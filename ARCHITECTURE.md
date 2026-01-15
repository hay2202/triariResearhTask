# IoT Data Ingestion Platform Architecture

## 1. Overview
This document outlines the architecture of a real-time IoT data ingestion platform. The system is designed to handle high-volume sensor data, provide dynamic scaling recommendations for worker microservices, and offer a comprehensive API for data and worker management.

## 2. Tech Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.2 (with Spring Web, Data Redis, Validation)
- **Database**: Redis (for data storage, caching, and coordination)
- **Build Tool**: Maven
- **Core Dependencies**:
    - `spring-boot-starter-web`: For building RESTful APIs.
    - `spring-boot-starter-data-redis`: For Redis integration.
    - `spring-boot-starter-validation`: For request payload validation.
    - `jackson-datatype-jsr310`: For Java 8+ time type serialization.

## 3. Core Concepts & Components

### 3.1. Data Ingestion (`SensorController`, `SensorService`)
- **Functionality**: Provides REST endpoints for ingesting and querying sensor data.
- **Data Storage Strategy**:
    - **Time-Series Data**: Stored in Redis **Sorted Sets (ZSET)**.
        - **Key**: `sensor:{sensor_id}:data`
        - **Score**: Unix timestamp (epoch milliseconds) for chronological ordering.
        - **Value**: JSON-serialized `SensorData` object. This structure allows for efficient time-range queries (`ZRANGEBYSCORE`).
    - **Sensor Index**: A Redis **Set** (`sensors:all`) maintains a unique list of all sensor IDs for quick lookups.
- **Throughput Monitoring**: Each ingestion call increments a counter in Redis, enabling real-time monitoring of data velocity.

### 3.2. Worker Management (`WorkerController`, `WorkerService`)
- **Functionality**: Manages the lifecycle of worker microservices (registration, deregistration, health checks).
- **Data Storage Strategy**:
    - **Worker Data**: Each worker is stored as a separate key-value pair in Redis.
        - **Key**: `worker:{worker_id}`
        - **Value**: JSON-serialized `Worker` object.
    - **Worker Index**: A Redis **Set** (`workers:index`) stores all `worker_id`s, allowing for efficient retrieval of all registered workers.
- **Health Checks**: Workers periodically send heartbeats to `/api/v1/workers/{worker_id}/health`, updating their `last_heartbeat` timestamp and `processed_count`.

### 3.3. Dynamic Scaling (`WorkerService`, `ThroughputMonitor`)
- **Functionality**: Provides scaling recommendations based on real-time ingestion throughput.
- **Throughput Calculation**: `ThroughputMonitor` uses time-bucketed keys in Redis (e.g., `throughput:{timestamp}`) with a short TTL to calculate the number of messages ingested per second.
- **Scaling Logic**:
    - A configurable set of rules determines the scaling recommendation (`SCALE_UP`, `SCALE_DOWN`, `HOLD`).
    - **Scale Up**: Triggered if throughput exceeds the total capacity of active workers (e.g., `> 1500 msg/s` per worker).
    - **Scale Down**: Triggered if throughput is significantly below capacity (e.g., `< 1000 msg/s` per worker).
    - **Constraints**: The system respects configurable minimum (`minWorkers`) and maximum (`maxWorkers`) limits.

## 4. API Endpoints

### Sensor API
- `POST /api/v1/sensors/data`: Ingests a new sensor reading.
- `GET /api/v1/sensors`: Lists all registered sensor IDs.
- `GET /api/v1/sensors/{sensor_id}/data`: Retrieves the latest reading for a sensor.
- `GET /api/v1/sensors/{sensor_id}/data/range`: Retrieves readings within a specified time range.

### Worker & Metrics API
- `POST /api/v1/workers`: Registers a new worker (with optional ID in body).
- `GET /api/v1/workers`: Lists all registered workers and their status.
- `DELETE /api/v1/workers/{worker_id}`: Deregisters a worker.
- `PUT /api/v1/workers/{worker_id}/health`: Updates a worker's health status (heartbeat).
- `GET /api/v1/metrics/throughput`: Gets the current ingestion throughput.
- `GET /api/v1/scaling/recommendation`: Provides a scaling recommendation.

## 5. Error Handling
- A centralized `GlobalExceptionHandler` (`@ControllerAdvice`) intercepts exceptions.
- **Validation Errors**: Uses `@Valid` on request bodies. `MethodArgumentNotValidException` is handled to return a `400 Bad Request` with detailed validation messages.
- **Other Errors**: Catches general exceptions to provide a consistent JSON error response format (`ErrorResponse` object).

## 6. Configuration
Key parameters in `application.properties`:
- `spring.data.redis.*`: Connection settings for Redis.
- `scaling.worker.capacity`: Target messages per second per worker.
- `scaling.worker.min`: Minimum number of workers.
- `scaling.worker.max`: Maximum number of workers.

## 7. Future Improvements
- **Authentication & Authorization**: Secure endpoints using API Keys or OAuth2.
- **Decoupling with a Message Queue**: Introduce Kafka or RabbitMQ between the ingestion API and workers to improve resilience, handle backpressure, and allow for more complex processing workflows.
- **Data Archiving**: Implement a strategy to offload older data from Redis to a long-term data store (e.g., a time-series database like TimescaleDB or a data lake).
- **Automated Orchestration**: Integrate with a container orchestrator (like Kubernetes) to automatically act on the scaling recommendations.
