# cqrs-command-service

Write side of the Product CQRS setup. Stores product creates and updates in PostgreSQL and publishes product events to Kafka through a **transactional outbox**.

## How it works
```
POST / PUT /v1/products
  └─ ProductServiceImpl ─ one DB transaction ─┬─ products        (row saved, @Version incremented)
                                              └─ outbox_events   (ProductCreated / ProductUpdated, full state as JSON)

OutboxRelay (@Scheduled, every 1 s)
  └─ SELECT … WHERE published_at IS NULL ORDER BY id FOR UPDATE SKIP LOCKED
       └─ send to catalog.product.events (key = product id) ─► mark published_at
```

- **Why an outbox.** The product change and its event commit together, so no event is lost and none is published for a rolled-back write. Delivery is at-least-once, and consumers skip duplicates using `aggregateVersion`.
- **If Kafka is down,** writes still succeed. Events wait in `outbox_events` (see `attempts` / `last_error`) and are published once the broker is back. Publishing stops at the first failure, so each product's events keep their order.
- **Cleanup:** published rows are deleted after `app.outbox.retention` (7 days).

## Stack
Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Spring for Apache Kafka · MapStruct · Lombok

## Run
Kafka comes from the sibling [`cqrs-infra`](../cqrs-infra) repo.
```bash
(cd ../cqrs-infra && docker compose up -d)
docker compose up -d
./mvnw spring-boot:run
```
The service listens on `http://localhost:8081`. On startup it creates the topic `catalog.product.events` (3 partitions, `cleanup.policy=compact`) if it doesn't exist.

CORS allows `POST`/`PUT` from `http://localhost:5173`, configurable via `app.cors.allowed-origins`.

## Endpoints
| Method | Path                 | Description      | Success |
|--------|----------------------|------------------|---------|
| POST   | `/v1/products`       | Create a product | 201     |
| PUT    | `/v1/products/{id}`  | Update a product | 200     |

```bash
curl -i -X POST localhost:8081/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mouse","description":"Wireless mouse","price":19.99,"stock":10}'
```

## Event contract
Topic `catalog.product.events`, key = product id, headers `eventType` and `eventId`.
```json
{
  "eventId": "6f1c1d2e-8f7a-4a51-9c63-2b0d5e7c9a10",
  "eventType": "ProductUpdated",
  "occurredAt": "2026-09-14T10:00:00Z",
  "aggregateId": 1,
  "aggregateVersion": 1,
  "payload": {
    "id": 1, "name": "Mouse", "description": "Wireless mouse", "price": 19.99, "stock": 10,
    "createdAt": "2026-09-14T09:00:00Z", "updatedAt": "2026-09-14T10:00:00Z"
  }
}
```
Every event carries the full product state. Only add fields in backward-compatible ways; consumers ignore unknown fields.

## Configuration
| Property                        | Default                  | Meaning                                      |
|---------------------------------|--------------------------|----------------------------------------------|
| `app.kafka.topics.product-events` | `catalog.product.events` | Topic this service owns and publishes to     |
| `app.outbox.poll-interval-ms`   | `1000`                   | Delay between relay runs                      |
| `app.outbox.batch-size`         | `100`                    | Max events published per run                  |
| `app.outbox.send-timeout`       | `10s`                    | Max wait for a Kafka acknowledgement          |
| `app.outbox.retention`          | `7d`                     | How long published rows are kept              |
| `app.outbox.cleanup-cron`       | `0 0 3 * * *`            | When published rows are purged                |

## Inspect the outbox
```bash
docker exec cqrs-command-postgres psql -U postgres -d products_command -c \
  'select id, event_type, aggregate_id, aggregate_version, published_at, attempts, last_error from outbox_events order by id;'
```

## Tests
```bash
./mvnw clean verify
```
