# Rate Limiter — Spring Cloud Gateway + Redis

A Spring Boot application that demonstrates **API rate limiting** using **Spring Cloud Gateway** and **Redis** (token bucket algorithm).

---

## Architecture

```
Client
  │
  ▼
Spring Cloud Gateway  (port 8080)
  │  - Matches /hello route
  │  - Applies RequestRateLimiter filter (Redis token bucket)
  │  - 429 if limit exceeded, else proxies to backend
  ▼
Backend Server  (port 8081)  ← embedded Netty (BackendServerConfig)
  │
  ▼
GET /hello → "Hello from Spring Boot + Redis!"
```

---

## How It Works

- The **gateway** on port `8080` intercepts all `/hello` requests.
- Before forwarding, it checks Redis for available tokens using the **token bucket algorithm**:
  - `replenishRate = 2` — 2 tokens added per second
  - `burstCapacity = 2` — max 2 tokens at any time
- If tokens are available → request is forwarded to the **backend** on port `8081` → `200 OK`
- If no tokens → request is rejected → `429 Too Many Requests`
- The rate limit key is resolved per **client IP address** (`RateLimitConfig.userKeyResolver`)

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.2.3 |
| Gateway | Spring Cloud Gateway 2023.0.0 |
| Rate Limiter | Redis token bucket (`RedisRateLimiter`) |
| Redis Client | Spring Data Redis |
| Reactive Runtime | Project Reactor / Netty |
| Build Tool | Gradle |
| Java | 17 |

---

## Prerequisites

- Java 17+
- Redis running on `localhost:6379`

---

## Running the Application

**1. Start Redis**

```bash
# If installed locally
redis-server

# Or via Docker
docker run -d -p 6379:6379 redis
```

**2. Start the Application**

```bash
./gradlew bootRun
```

The gateway starts on port `8080` and the backend starts on port `8081` automatically.

---

## Testing the Rate Limiter

Send rapid requests using curl:

```bash
for i in {1..10}; do
  printf "Request %2d: " $i
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/hello
done
```

Expected output:

```
Request  1: 200
Request  2: 200
Request  3: 429
Request  4: 429
...
Request 10: 429
```

### Rate Limit Response Headers

Every response includes these headers:

| Header | Description |
|--------|-------------|
| `X-RateLimit-Remaining` | Tokens remaining in the current window |
| `X-RateLimit-Burst-Capacity` | Max burst size |
| `X-RateLimit-Replenish-Rate` | Tokens added per second |
| `X-RateLimit-Requested-Tokens` | Tokens consumed by this request |

---

## Configuration

`src/main/resources/application.properties`

```properties
server.port=8080

spring.data.redis.host=localhost
spring.data.redis.port=6379

# Rate limiter settings
spring.cloud.gateway.routes[1].filters[0].args.redis-rate-limiter.replenishRate=2
spring.cloud.gateway.routes[1].filters[0].args.redis-rate-limiter.burstCapacity=2
```

To make the limit harder/easier to trigger, adjust `replenishRate` and `burstCapacity`.

---

## Project Structure

```
src/main/java/com/example/ratelimiter/
├── RateLimiterApplication.java          # Spring Boot entry point
├── config/
│   ├── BackendServerConfig.java         # Embedded Netty backend on port 8081
│   ├── RateLimitConfig.java             # KeyResolver bean (resolves by client IP)
│   └── RedisConfig.java                 # StringRedisTemplate bean
└── controller/
    └── HelloController.java             # Disabled (backend served via BackendServerConfig)
```
