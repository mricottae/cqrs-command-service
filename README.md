# cqrs-command-service

Write side of the Product CQRS setup. Handles product creation and updates, persisted in PostgreSQL.

## Stack
Java 21 · Spring Boot · Spring Data JPA · PostgreSQL · MapStruct · Lombok

## Run
```bash
docker compose up -d
./mvnw spring-boot:run
```
Service listens on `http://localhost:8081`.

## Endpoints
| Method | Path                 | Description      | Success |
|--------|----------------------|------------------|---------|
| POST   | `/v1/products`       | Create a product | 201     |
| PUT    | `/v1/products/{id}`  | Update a product | 200     |

```bash
curl -X POST localhost:8081/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mouse","description":"Wireless mouse","price":19.99,"stock":10}'
```

## Tests
```bash
./mvnw clean verify
```
