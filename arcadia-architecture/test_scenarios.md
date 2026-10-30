# Connected Vehicle Platform — Test Scenarios & QA Matrix

This document defines the core functional, integration, edge, and performance test scenarios for the **Connected Vehicle Platform**, covering the Modular Monolith pilot phase through to the distributed microservices target.

---

## 1. Module 1: Remote Operations (`/operations`)

### TS-OPS-01: Initiate Asynchronous Vehicle Command (Happy Path)
* **Objective:** Verify that dispatching a valid vehicle command (e.g., `DOOR_LOCK`) returns an immediate asynchronous tracking response.
* **Pre-conditions:** Vehicle with VIN `MAKZ1234567890` is provisioned and in an idle, connected state.
* **Request:**
  * **Method & Path:** `POST /api/v1/vehicles/MAKZ1234567890/commands`
  * **Headers:** `Content-Type: application/json`, `X-Request-Id: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`
  * **Payload:**
    ```json
    {
      "command": "DOOR_LOCK",
      "source": "MOBILE_APP",
      "requestedBy": "user-1042",
      "params": { "doors": "ALL" },
      "timestamp": "2026-10-08T10:15:00Z"
    }
    ```
* **Expected Result:**
  * HTTP Status: `202 Accepted`
  * Response Body contains `commandId` (e.g., `cmd-8f21a`), `status: "IN_PROGRESS"`, `acceptedAt`, and `pollUrl`.
  * Redis key `cmd:cmd-8f21a` is initialized with status `IN_PROGRESS` and a TTL of 60 seconds.
  * Audit row inserted into `operations.remote_commands` table.

---

### TS-OPS-02: Rapid Command Polling via Redis Cache
* **Objective:** Verify that client polling reads directly from the low-latency Redis cache without putting load on PostgreSQL.
* **Pre-conditions:** Command `cmd-8f21a` is actively cached in Redis.
* **Request:**
  * **Method & Path:** `GET /api/v1/commands/cmd-8f21a`
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Response Body contains latest execution status (`IN_PROGRESS` or `SUCCESS`).
  * Response time < 5ms (cache hit).
  * No queries executed against PostgreSQL `operations.remote_commands`.

---

### TS-OPS-03: Vehicle Edge Actuation & Acknowledgment Callback
* **Objective:** Verify edge/simulator callback marks the command as completed and updates live state.
* **Pre-conditions:** `cmd-8f21a` is in `IN_PROGRESS` state.
* **Request:**
  * **Method & Path:** `POST /api/v1/vehicles/MAKZ1234567890/commands/cmd-8f21a/ack`
  * **Payload:**
    ```json
    {
      "status": "SUCCESS",
      "executedAt": "2026-10-08T10:15:03Z",
      "errorMessage": null
    }
    ```
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Redis key `cmd:cmd-8f21a` status updated to `SUCCESS`.
  * `operations.remote_commands` record updated with completed timestamp.
  * Live vehicle status cache `vehicle:telemetry:MAKZ1234567890` updated to reflect `doorLockStatus: "LOCKED"`.

---

### TS-OPS-04: Command Execution Timeout (Vehicle Edge Unreachable)
* **Objective:** Ensure commands that receive no edge acknowledgment within the configured timeout window transition gracefully to `TIMED_OUT`.
* **Pre-conditions:** Command dispatched, but vehicle TCU is offline/unreachable.
* **Action:** Wait for timeout expiry (or expire the TTL state).
* **Request:**
  * **Method & Path:** `GET /api/v1/commands/cmd-8f21a`
* **Expected Result:**
  * HTTP Status: `200 OK` (or `408/504` depending on client configuration).
  * Payload indicates `status: "TIMED_OUT"` and `errorMessage: "Vehicle did not respond within the configured timeout."`.

---

### TS-OPS-05: Concurrent Actuation Conflict Protection
* **Objective:** Verify that duplicate or overlapping conflicting commands on the same vehicle are blocked by distributed locks.
* **Pre-conditions:** A `DOOR_LOCK` command is actively in-flight for VIN `MAKZ1234567890`.
* **Action:** Rapidly submit a secondary `DOOR_UNLOCK` command within < 100ms.
* **Expected Result:**
  * HTTP Status: `409 Conflict`
  * Response: `errorCode: "COMMAND_CONFLICT"`, message indicating an active command is already in progress.

---

## 2. Module 2: Eco Challenge (`/ecodrive`)

### TS-ECO-01: Ingest Completed Trip Telemetry & Recalculate Score
* **Objective:** Verify telemetry ingestion triggers scoring evaluation, updates trip logs, and updates Redis leaderboards.
* **Pre-conditions:** Driver `user-1042` is registered.
* **Request:**
  * **Method & Path:** `POST /api/v1/telemetry/trips`
  * **Payload:**
    ```json
    {
      "driverId": "user-1042",
      "tripId": "trip-5567",
      "distanceKm": 42.6,
      "harshBraking": 0,
      "idleMinutes": 2,
      "accessoryHealth": {
        "tyrePressure": "OK",
        "batteryHealth": "GOOD"
      }
    }
    ```
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Trip record saved in `ecodrive.trip_logs`.
  * Driver's aggregate eco-score recalculated (e.g., score increases due to zero harsh braking).
  * Driver's score updated in Redis Sorted Set (`leaderboard:2026-W38`).
  * In-memory `TripCompletedEvent` emitted to trigger downstream component wear checks.

---

### TS-ECO-02: Achievement Badge Evaluation
* **Objective:** Verify that qualifying trips award the proper milestone badges (e.g., `SMOOTH_BRAKING`).
* **Pre-conditions:** Driver has 5 consecutive trips with zero harsh braking events.
* **Request:**
  * **Method & Path:** `GET /api/v1/drivers/user-1042/eco-score?period=WEEKLY`
* **Expected Result:**
  * HTTP Status: `200 OK`
  * `badges` array includes `SMOOTH_BRAKING`.
  * Badge record persisted in `ecodrive.driver_badges`.

---

### TS-ECO-03: Real-Time Leaderboard Retrieval ($O(\log N)$ ZSET)
* **Objective:** Verify that the top 10 driver leaderboard is returned in correct rank order from Redis without querying the database.
* **Request:**
  * **Method & Path:** `GET /api/v1/eco-challenge/leaderboard?top=10`
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Returns ranked list from rank 1 to 10 sorted in descending order of `score`.
  * Response served from Redis Sorted Set (`ZREVRANGE`).

---

### TS-ECO-04: Invalid Telemetry Data Rejection
* **Objective:** Verify validation prevents negative or impossible sensor values from corrupting score aggregates.
* **Request:**
  * **Method & Path:** `POST /api/v1/telemetry/trips`
  * **Payload:**
    ```json
    {
      "driverId": "user-1042",
      "tripId": "trip-invalid",
      "distanceKm": -15.5,
      "harshBraking": -1,
      "idleMinutes": 999999
    }
    ```
* **Expected Result:**
  * HTTP Status: `400 Bad Request`
  * Response: `errorCode: "INVALID_TELEMETRY"`, detail list indicating validation constraints on numeric ranges.

---

## 3. Module 3: Predictive Maintenance (`/maintenance`)

### TS-MAINT-01: Component Health Checklist Fetch
* **Objective:** Ensure mobile client retrieves live status for battery, brake pads, tyres, and fluids.
* **Request:**
  * **Method & Path:** `GET /api/v1/vehicles/MAKZ1234567890/health`
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Response contains `overallStatus` (e.g., `ATTENTION_REQUIRED`) and items for `BATTERY`, `BRAKE_PAD_FRONT`, `TYRE_FRONT_LEFT`, `COOLANT` with individual metrics and status flags (`OK`, `WARNING`, `CRITICAL`).

---

### TS-MAINT-02: Rule Engine Alert Trigger on Degradation
* **Objective:** Verify that trip telemetry indicating abnormal wear triggers a diagnostic alert.
* **Action:** Fire `TripCompletedEvent` with front brake pad thickness at 12%.
* **Internal Process:**
  * `BrakePadDegradationRule` fires.
  * Alert entity generated in `maintenance.alerts` table with `severity: "HIGH"`, `predictedFailureIn: "12 days"`, and `recommendation: "Book service"`.
* **Verification Request:**
  * `GET /api/v1/vehicles/MAKZ1234567890/alerts?status=OPEN`
* **Expected Result:**
  * HTTP Status: `200 OK`
  * New alert `alt-3391` present with status `OPEN`.

---

### TS-MAINT-03: Register Mobile Push Notification Token
* **Objective:** Verify mobile app can register an FCM / APNs token for alert delivery.
* **Request:**
  * **Method & Path:** `POST /api/v1/devices/register`
  * **Payload:**
    ```json
    {
      "vin": "MAKZ1234567890",
      "deviceToken": "fcm-Ab12Cd34SampleToken",
      "platform": "ANDROID",
      "topics": ["MAINTENANCE_ALERTS"]
    }
    ```
* **Expected Result:**
  * HTTP Status: `200 OK`
  * Response indicates `status: "REGISTERED"`.
  * Record stored in `maintenance.device_registrations`.

---

### TS-MAINT-04: Service Appointment Booking from Alert (Call-to-Action)
* **Objective:** Verify booking hand-off creates an appointment and links to the alert.
* **Request:**
  * **Method & Path:** `POST /api/v1/vehicles/MAKZ1234567890/service-bookings`
  * **Payload:**
    ```json
    {
      "alertId": "alt-3391",
      "preferredDate": "2026-10-25",
      "serviceCenterId": "sc-bangalore-east-01"
    }
    ```
* **Expected Result:**
  * HTTP Status: `201 Created`
  * Response contains `bookingId`, `status: "CONFIRMED"` (or `PENDING_CONFIRMATION`), and scheduled date.
  * Associated alert `alt-3391` status updated or linked to the booking ID.

---

## 4. Cross-Cutting & Security (`/auth` & Infrastructure)

### TS-AUTH-01: Authentication & Token Issuance
* **Objective:** Valid credentials return signed JWT access and refresh tokens.
* **Request:** `POST /api/v1/auth/login`
* **Expected Result:** HTTP Status `200 OK`, returns `accessToken`, `refreshToken`, and expiry metadata.

---

### TS-AUTH-02: Instant Token Revocation via Redis Blacklist
* **Objective:** Verify that logging out immediately invalidates the access token across instances.
* **Action:**
  1. Call `POST /api/v1/auth/logout` with Bearer token.
  2. Attempt to call `GET /api/v1/vehicles/MAKZ1234567890/health` using the revoked token.
* **Expected Result:**
  1. Logout returns `200 OK`; token `jti` written to Redis key `auth:token:{jti}`.
  2. Subsequent request returns `401 Unauthorized` with `errorCode: "UNAUTHORIZED"`.

---

### TS-INT-01: Asynchronous Event Decoupling (Eco $\rightarrow$ Maintenance)
* **Objective:** Confirm that trip ingestion in `ecodrive` triggers maintenance degradation checks asynchronously without blocking the trip submission response.
* **Verification:**
  * Telemetry submission returns `200 OK` in < 50ms.
  * Inspection of logs shows `TripCompletedEventListener` executing on a separate worker thread (`cv-async-worker-*`).
  * No cross-schema SQL joins executed between `ecodrive` and `maintenance`.