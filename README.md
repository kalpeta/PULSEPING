# PulsePing

Waitlist + Campaign Notifications Platform (event-driven).

## Local development (baseline)
- App: Spring Boot (Java 21)
- DB: Postgres 16 (Docker) [later]
- Kafka: KRaft (Docker) [later]

## Quick start (for Slice B1.1)
From `app/`:
```bash
./mvnw spring-boot:run

Health:

curl -s localhost:8080/actuator/health