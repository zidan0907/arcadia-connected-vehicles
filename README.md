# Arcadia Connected Vehicle Platform (Pilot)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Arcadia is a high-performance, modular monolith backend powering the core features of a connected vehicle platform. Built using Java 21 and Spring Boot 3, it drives three prototype user experiences: **Remote Operations**, **Eco Challenge**, and **Predictive Maintenance**.

The application enforces Domain-Driven Design (DDD) bounded contexts with isolated PostgreSQL schemas and an in-memory Spring event bus. This zero-leakage modularity enables simple local development today while providing a friction-free path to microservices decomposition (via Apache Kafka, Spring Cloud Gateway, and Netflix Eureka) when scaling fleet workloads.

---

## 🚀 Core Feature Modules

### 1. Remote Operations (`/operations`)
* **Asynchronous Actuation:** Dispatches vehicular commands (`DOOR_LOCK`, `DOOR_UNLOCK`, `CLIMATE_START`, `CLIMATE_STOP`) returning immediate `202 Accepted` tracking receipts.
* **Low-Latency Polling:** Tracks command execution state (`IN_PROGRESS`, `SUCCESS`, `FAILED`, `TIMED_OUT`) via Redis keys (`cmd:{commandId}`) with automatic 60-second TTLs.
* **Live Vehicle Telemetry:** Delivers current door lock, battery level, and climate state for the mobile vehicle status card.
* **Edge Callback Ingress:** Receives telematics actuation acknowledgments (`/ack`) from the onboard TCU / Vehicle Simulator.

### 2. Eco Challenge (`/ecodrive`)
* **Trip Ingestion:** Ingests trip telemetry (distance, harsh braking instances, idle minutes, accessory loads) at driving session completion.
* **Deterministic Scoring Engine:** Converts driving behaviors into explainable weekly eco scores normalized from 0 to 100.
* **Dynamic Leaderboard:** Delivers $O(\log N)$ real-time rankings powered by Redis Sorted Sets (`ZSET`).
* **Driver Achievements:** Automatically unlocks milestone badges (`SMOOTH_BRAKING`, `LOW_IDLE`, `ECO_CHAMPION`).

### 3. Predictive Maintenance (`/maintenance`)
* **Component Health Checklist:** Monitors real-time wear indicators across engine, battery, brake pads, tyres, and fluid levels.
* **Rule-Based Diagnostics:** Evaluates sensor thresholds against degradation limits to raise severity-coded alerts (`LOW`, `MEDIUM`, `HIGH`).
* **Push Alert Bridge:** Registers mobile device tokens (FCM / APNs) for proactive notification push.
* **Service Booking Action:** Provides an actionable dealer service reservation hand-off directly from diagnostic alert cards.

### 4. Identity & Authentication (`/auth`)
* **Stateless Token Management:** Generates and validates signed JWT access and refresh tokens.
* **Distributed Session Revocation:** Intercepts tokens against a Redis-backed token blacklist (`auth:token:{jti}`) for immediate logout invalidation.

---

## 🛠️ Technology Stack

| Category | Component / Library |
| :--- | :--- |
| **Language & Runtime** | Java 21 (Microsoft OpenJDK / Temurin) |
| **Framework** | Spring Boot 3.x (Web, Data JPA, Validation, Actuator) |
| **Primary Database** | PostgreSQL 16 (Logical schema isolation: `auth`, `fleet`, `operations`, `ecodrive`, `maintenance`) |
| **Database Migrations** | Flyway |
| **In-Memory Cache & State** | Redis 7 (Command Polling TTLs, Leaderboard `ZSET`, Token Blacklists) |
| **Security & Contracts** | Spring Security (Stateless JWT / RBAC), OpenAPI 3.0 / Swagger UI |
| **Testing** | JUnit 5, Mockito, Testcontainers |

---