# Arcadia Connected Vehicle Platform

> An enterprise connected vehicle platform powering remote operations, eco-driving gamification, and predictive maintenance diagnostics. Engineered with Java 21, Spring Boot 3, PostgreSQL, and Redis, using a modular, event-driven design built for seamless evolution from a modular monolith to distributed microservices.

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 📑 Table of Contents
1. [Platform Overview & Core Modules](#-platform-overview--core-modules)
2. [Technology Stack & Architectural Patterns](#️-technology-stack--architectural-patterns)
3. [Centralized Error & Fault Handling](#-centralized-error--fault-handling)

---

## 🚗 Platform Overview & Core Modules

The platform is engineered around three primary vehicle-facing domains alongside a core identity provider:

### 1. Remote Operations (`/operations`)
* **Asynchronous Actuation:** Dispatches vehicular commands (`DOOR_LOCK`, `DOOR_UNLOCK`, `CLIMATE_START`, `CLIMATE_STOP`) and returns an immediate `202 Accepted` tracking receipt with polling instructions.
* **Low-Latency Polling:** Command execution states (`IN_PROGRESS`, `SUCCESS`, `FAILED`, `TIMED_OUT`) are tracked in Redis keys (`cmd:{commandId}`) with automated 60-second TTL expirations.
* **Live Telemetry & Status:** Supplies real-time door lock state, battery level, and climate activity for the mobile dashboard card.
* **Edge Callback Ingress:** Exposes a callback interface (`/ack`) for the physical vehicle TCU agent or software simulator to report actuation results.

### 2. Eco Challenge (`/ecodrive`)
* **Trip Telemetry Ingestion:** Collects driving logs (total distance, harsh braking counts, idle minutes, auxiliary loads) upon completion of each trip.
* **Deterministic Scoring Engine:** Evaluates behavioral telemetry using rule-based strategies to produce normalized scores from 0 to 100.
* **Real-Time Leaderboard:** Employs Redis Sorted Sets (`ZSET`) to achieve $O(\log N)$ real-time rank queries without running intensive SQL table aggregations.
* **Gamification Badges:** Awards driver milestones such as `SMOOTH_BRAKING`, `LOW_IDLE`, and `ECO_CHAMPION`.

### 3. Predictive Maintenance (`/maintenance`)
* **Component Health Checklist:** Tracks physical degradation metrics across battery condition, brake pad thickness, tyre pressure, and fluid states.
* **Threshold Diagnostics:** Compares telemetry against wear safety margins to raise severity-coded alerts (`LOW`, `MEDIUM`, `HIGH`).
* **Proactive Push Alerts:** Registers FCM and APNs push notification device tokens to broadcast critical warning alerts.
* **Service Booking Hand-off:** Converts maintenance alerts into service appointment bookings with preferred service centers.

### 4. Identity & Authentication (`/auth`)
* **Stateless JWT Tokens:** Issues, parses, and signs cryptographic JWT access and refresh tokens.
* **Distributed Session Invalidation:** Tracks revoked token identifiers (`auth:token:{jti}`) in Redis for distributed logout support.

---

## 🛠️ Technology Stack & Architectural Patterns

### 1. Core Platform & Runtime
* **Java 21 (LTS):** Primary execution language leveraging virtual threads, immutable records for DTOs, and pattern matching.
* **Spring Boot 3.x:** Foundational framework for dependency injection, auto-configuration, and application bootstrapping.
* **Apache Maven:** Project build lifecycle and dependency management using the included `./mvnw` wrapper.

---

### 2. Ingress, Networking & Real-Time Communication
* **Spring Web MVC:** Synchronous REST controller dispatching, `@Valid` bean payload validation, and HTTP serialization.
* **Spring Cloud Gateway:** Centralized edge reverse proxy handling routing, distributed rate limiting, and TLS termination in the distributed target architecture.
* **OpenAPI 3.0 / Swagger UI:** Standardized contract specifications and interactive browser testing.
* **WebSockets (STOMP):** Bi-directional channel for streaming live actuation progress and emergency diagnostic alerts to mobile clients.
* **Webhooks:** Ingress endpoints for asynchronous callbacks from third-party dealer scheduling and payment systems.

---

### 3. Security, Authentication & Session Management
* **Spring Security 6.x:** Filter-chain security orchestrating authentication and Role-Based Access Control (RBAC).
* **Stateless JWT (JSON Web Tokens):** Cryptographically signed access and refresh tokens carrying driver claims and permissions.
* **Redis Token Blacklist (`auth:token:{jti}`):** Distributed in-memory store for instantaneous session invalidation and token revocation across running instances.
* **Azure Entra ID:** Enterprise cloud identity provider integration for cloud-native pilot environments.

---

### 4. Persistence & Database Architecture
* **PostgreSQL 16:** ACID-compliant relational data storage.
  * *Monolith Phase:* Single database instance maintaining logical schema separation (`auth`, `fleet`, `operations`, `ecodrive`, `maintenance`) with strict zero cross-schema SQL joins.
  * *Microservices Phase:* Strict **Database-per-Service** ownership model ensuring total service data autonomy.
* **Spring Data JPA / Hibernate 6:** Object-relational mapping, custom repository abstractions, and managed transactions.
* **Flyway:** Automated, version-controlled database schema migrations running at application bootstrap.

---

### 5. Caching, In-Memory State & Concurrency Control
* **Redis 7 / Azure Cache for Redis:** Central high-throughput, low-latency memory store.
* **Transient Polling Cache:** Ephemeral vehicle actuation state tracking (`cmd:{commandId}`) with 60-second TTLs to prevent database load during client polling bursts.
* **Redis Sorted Sets (`ZSET`):** Real-time leaderboard engine enabling $O(\log N)$ driver rank lookups without relational table scans.
* **Read-Through / Cache-Aside Store:** Live vehicle telemetry cards (`vehicle:telemetry:{vin}`) and component wear evaluations.
* **Redis Distributed Locks:** Prevents concurrent duplicate actuation triggers across distributed application nodes.

---

### 6. Event-Driven Messaging & Distributed Workflows
* **Spring `ApplicationEventPublisher`:** In-memory asynchronous event bus decoupling internal monolith modules (e.g., trip submission triggering maintenance wear checks).
* **Apache Kafka / Azure Event Hubs:** Distributed event-streaming backbone for cross-microservice communication.
* **Transactional Outbox Pattern:** Guarantees database state changes and event message emissions occur in a single atomic transaction, preventing dual-write anomalies.
* **Idempotent Consumers:** Deduplication layer preventing duplicated processing caused by at-least-once message delivery guarantees.
* **Saga Pattern (Orchestrator):** State machine managing distributed transactions across multi-service workflows (e.g., service booking with dealer slot reservations) with automated compensating transactions on failure.

---

### 7. Service Discovery, Routing & Load Balancing
* **Netflix Eureka:** Dynamic service registry managing instance registration and heartbeat monitoring.
* **Spring Cloud LoadBalancer:** Client-side load-balancing algorithm resolving instances dynamically from the Eureka registry.

---

### 8. Fault Tolerance & System Resilience
* **Resilience4j Circuit Breakers:** Protects services from cascading outages by failing fast when downstream dependencies fail.
* **Resilience4j Retries & Time Limiters:** Automated exponential backoff and timeout thresholds for outbound integrations.
* **Bulkhead Isolation:** Restricts concurrent resource usage across outbound execution threads.

---

### 9. Observability, Distributed Tracing & Metrics
* **Micrometer Tracing + Zipkin:** Distributed trace context propagation passing `traceId` and `spanId` across HTTP calls and Kafka boundaries.
* **Spring Boot Actuator + Prometheus:** Automated operational endpoints for health probes, garbage collection, and custom performance metrics.
* **SLF4J + Logback:** Structured JSON log outputs tagged with distributed correlation IDs.

---

### 10. External Integrations & Vehicle Edge Protocols
* **Azure Notification Hubs / FCM / APNs:** Multi-platform push notification delivery for high-severity diagnostic warnings.
* **OBD-II / CAN Telematics Simulator:** Edge agent simulating physical vehicle telemetry streams and executing `/ack` command callbacks.

---

### 11. Testing & Performance Verification
* **JUnit 5 & Mockito:** Unit testing for isolated domain business logic.
* **Spring Boot Test & Testcontainers:** Integration test slices running ephemeral PostgreSQL and Redis Docker containers during CI builds.
* **Apache JMeter / Gatling:** Stress testing suites simulating telematics ingestion bursts and high-concurrency client polling loads.

---

## 🛡️ Centralized Error & Fault Handling

All API errors return a uniform, machine-readable JSON structure matching the OpenAPI contracts:

```json
{
  "transactionId": "11446809-cc98-4642-b173-ad26d567ed38"
  "errorCode": "VEHICLE_TIMEOUT",
  "message": "Vehicle did not acknowledge command within the configured timeout window.",
  "timestamp": "2026-10-08T10:15:00Z"
}
