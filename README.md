# Bilibili-like Video Streaming Platform

This is the core, multi-module Bilibili application. It contains three Maven subprojects:

1. **bilibili-api**  
   - Exposes all REST endpoints and API controllers under `com.example.bilibili.api`.  
   - Contains aspects for rate-limiting and data-limiting (`ApiLimitedRoleAspect`, `DataLimitedAspect`).  
   - Hosts all `*Api` classes (e.g. `UserApi`, `VideoApi`, `DemoApi`, etc.) and the main `BilibiliApp` entry point.

2. **bilibili-dao**  
   - Defines domain entities under `com.example.bilibili.domain`.  
   - Contains MyBatis mapper XML files in `src/main/resources/mapper`.  
   - Provides DAO interfaces (`*Dao` and `*Repository`) for database access and CRUD operations.

3. **bilibili-service**  
   - Implements all business logic in `com.example.bilibili.service`.  
   - Includes service classes (`*Service`, e.g. `UserService`, `VideoService`, `DemoService`).  
   - Configures cross-cutting concerns in `config` (Elasticsearch, RocketMQ, FastJSON, WebSocket).  
   - Declares Feign clients (e.g. `MsDeclareService`) and exception handlers.

---

## Standalone Platform Services

The following supporting components were introduced to extend the core Bilibili application:

- **bilibili-eureka**  
  A dedicated Netflix Eureka server (port 15006) that acts as the central registry for all services. It enables load-balancing and fail-over for every microservice in the platform.

- **bilibili-demo-service**  
  A lightweight Spring Boot microservice exposing `/demos` endpoints for testing purposes. It implements Eureka, with inter-service calls via OpenFeign, and uses Resilience4j to circuit-break on slow or failing requests.

- **bilibili-api-gateway**  
  A Spring Cloud Gateway application that routes all `/bilibili-api/**` traffic to downstream services.
