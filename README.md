# Fair Queue — TicketMaster-style fair queuing system

A backend system simulating a fair waiting-room queue for high-traffic ticket/drop
sales (think: concert ticket sales, sneaker drops). Built as a portfolio project to
demonstrate event-driven microservices, high-concurrency handling, and modern Java/
Spring practices.

Instead of everyone hammering a "buy" endpoint at once, users join a queue, see their
live position, and get offered a purchase slot in order — with rate limiting, bot/abuse
protection in mind, and an event-driven backend that can scale each concern
independently.

## Why this project

Flash-sale traffic spikes are a well-known, concrete backend engineering problem
(Ticketmaster-style outages are a recurring industry story). This project builds a
scaled-down version of that system to practice and demonstrate:

- Handling large numbers of concurrent connections
- Event-driven communication between services (Kafka)
- Real-time updates to clients (WebSocket)
- Rate limiting and abuse protection (Redis)
- Testing strategy across unit, controller, and integration layers

## Architecture

```
TicketQueue/
├── queue-service/       # queue management, rate limiting, live position, Kafka producer
├── purchase-service/    # (in progress) consumes slot offers, purchase window, timeouts
├── docker-compose.yml   # Redis + Kafka (KRaft mode) + services
└── README.md
```

Each service is an independent Gradle project — no shared build, no shared database.
Services communicate exclusively through Kafka events, not direct REST-to-REST calls.
This is a monorepo (single git repository) for convenience, not a shared build.

### High-level flow

1. User joins the queue for an event → position tracked in Redis (Sorted Set)
2. Live position pushed to the client over WebSocket as the queue moves
3. A scheduler periodically releases the next N users from the queue
4. Releasing a user removes them from Redis **and** publishes a `SlotOfferedEvent` to
   Kafka, keyed by `eventId`
5. *(planned)* `purchase-service` consumes that event, opens a time-boxed purchase
   window, and either completes or times out the purchase

## Tech stack

| Concern | Technology |
|---|---|
| Language / runtime | Java 25, Spring Boot 4.1 |
| Queue state | Redis (Sorted Set, reactive client) |
| Real-time updates | WebSocket / STOMP |
| Event streaming | Apache Kafka (KRaft mode, no Zookeeper) |
| Build | Gradle (Kotlin DSL) |
| Testing | JUnit 5, Mockito, Reactor `StepVerifier`, Testcontainers, MockMvc |
| Infra (local) | Docker Compose |

## `queue-service`

### Responsibilities

- Accept users into a per-event waiting queue (`POST /events/{eventId}/queue/join`)
- Track and expose live queue position (REST + WebSocket)
- Rate-limit repeated join attempts per user
- Periodically release the next batch of users from the queue
- Publish a Kafka event when a user is released, so downstream services can react

### Redis data model

| Key pattern | Structure | Purpose |
|---|---|---|
| `queue:{eventId}` | Sorted Set (score = join timestamp) | Ordered waiting queue |
| `ratelimit:{eventId}:{userId}` | String, TTL 3s | Prevents rapid repeated join calls |

Sorted Set was chosen because it gives O(log n) insertion, rank lookup, and range
queries "for free" — exactly the operations a fair queue needs (join, check position,
release the first N).

### REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/events/{eventId}/queue/join` | Join the queue for an event |
| `GET` | `/events/{eventId}/queue/position/{userId}` | Check current position |
| `DELETE` | `/events/{eventId}/queue/leave/{userId}` | Leave the queue |

Returns `429 Too Many Requests` if rate-limited, `404` if the user isn't in the queue.

### Real-time updates

A scheduled job periodically re-broadcasts every waiting user's current position over
WebSocket/STOMP, to a per-user topic: `/topic/queue/{eventId}/{userId}`. Clients
subscribe only to their own topic, so they never see other users' updates.

A minimal HTML/JS test client (SockJS + STOMP.js) is included for manual
verification of the join → live position → release flow without a full frontend.

### Kafka events

| Topic | Key | Payload | Published when |
|---|---|---|---|
| `slot-offered` | `eventId` | `SlotOfferedEvent` (uuid, eventId, userId, offeredAt) | A user is released from the queue |

`eventId` is used as the partition key so that all events for a given ticketed event
stay in order relative to each other — see the trade-off note below.

## Testing strategy

Three layers, each testing a different thing:

- **Unit tests (`QueueServiceTest`)** — Redis client fully mocked
  (`ReactiveRedisTemplate` / `ReactiveZSetOperations` / `ReactiveValueOperations`).
  Verifies `QueueService`'s own logic and reaction to different Redis outcomes,
  independent of whether Redis actually behaves as assumed.
- **Controller tests (`QueueControllerTest`)** — `@WebMvcTest` + `MockMvc`, with
  `QueueService` mocked via `@MockitoBean`. Verifies HTTP status codes and response
  bodies for every branch (200/404/429/204), without touching Redis at all.
- **Integration tests (`QueueServiceIntegrationTest`)** — `@SpringBootTest` +
  Testcontainers, running against a real, ephemeral Redis container. No mocks — verifies
  that Redis actually behaves the way the unit tests assumed (ordering, rate-limit TTL
  behavior, release semantics). State is flushed before every test to avoid leakage
  between tests sharing the same container.

## Architectural decisions & trade-offs

Documenting these because they were deliberate engineering calls, not oversights.

### Servlet stack (Tomcat) over full WebFlux

This service uses Reactive Redis (`ReactiveRedisTemplate`, `Mono`/`Flux`) throughout,
which would ideally run on a fully non-blocking stack (WebFlux + Netty).

However, Spring's STOMP-based WebSocket support (`@EnableWebSocketMessageBroker`) is
built on the Servlet API and requires a servlet container — it's incompatible with a
pure WebFlux/Netty runtime. Since `spring-boot-starter-websocket` is present, Spring
Boot resolves the application as servlet-based (Tomcat) even alongside
`spring-boot-starter-webflux`. Forcing `spring.main.web-application-type=reactive`
breaks the WebSocket handshake entirely (confirmed experimentally).

**Decision:** kept the servlet stack (Tomcat). REST endpoints remain written in
reactive style (`Mono`/`Flux`) for consistency with the reactive Redis client, but are
ultimately subscribed to by the servlet container rather than running on a fully
non-blocking event loop. At this project's scale, Redis and application logic are the
realistic bottlenecks, not the HTTP thread model — so this trade-off was accepted
rather than rewriting the WebSocket layer on WebFlux's lower-level, non-STOMP reactive
WebSocket API (which would require reimplementing per-user message routing from
scratch, since that API has no built-in topic/subscription concept).

### `eventId` as the Kafka partition key

Using `eventId` as the key guarantees ordering of events *within* a single ticketed
event, which matters for correctness (e.g. slot offers shouldn't be processed out of
order for the same sale). The trade-off: all traffic for one very popular event goes
through a single partition, capping parallelism for that specific key regardless of
how many partitions the topic has. Acceptable at this project's scale; would need
revisiting (e.g. a composite key, or accepting relaxed ordering) if handling
genuinely massive single-event traffic.

### Redis Sorted Set for the queue

Chosen over a List or a database table because it gives ordering, O(log n)
rank/insert/range operations, and automatic de-duplication (re-joining updates score
instead of creating a duplicate entry) without any extra bookkeeping.

## Running locally

```bash
docker-compose up -d        # starts Redis + Kafka (KRaft mode)
cd queue-service
./gradlew bootRun
```

Redis: `localhost:6379`. Kafka: `localhost:9092` (host) / `kafka:29092` (inter-container).

## Roadmap

- [x] Redis-backed fair queue with rate limiting
- [x] Live position updates over WebSocket
- [x] Kafka producer for slot-release events
- [x] Unit, controller, and integration test coverage
- [ ] `purchase-service`: Kafka consumer, time-boxed purchase window, timeout handling
- [ ] `fraud-insight-service`: AI agent observing the event stream for anomaly detection
- [ ] Kubernetes manifests + CI/CD (GitHub Actions)
- [x] Load testing (k6/Gatling) demonstrating virtual threads under high concurrency
