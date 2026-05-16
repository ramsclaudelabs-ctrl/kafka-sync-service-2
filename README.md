# kafka-contacts-service

Real-time Avro field-projection for the Contacts domain.

```
contacts.raw.v1  ──▶  [this service]  ──▶  contacts.projected.v1
                              │
                              └─ on error ──▶  contacts.projected.v1.DLQ
```

See [`../ARCHITECTURE.md`](../ARCHITECTURE.md) for the full design and
[`../docs/perf-tuning.md`](../docs/perf-tuning.md) for performance guidance.

## Build

```bash
./mvnw clean verify          # generates Avro sources, compiles, runs unit + embedded tests
./mvnw -Pjmh clean package   # builds target/benchmarks.jar
```

## Run locally

```bash
docker compose -f ../docs/local-kafka-compose.yml up -d
./mvnw spring-boot:run
```

## Run JMH

```bash
./mvnw -Pjmh clean package
java -jar target/benchmarks.jar -i 5 -wi 3 -f 1
```

## Deploy to AKS

```bash
docker build -t myacr.azurecr.io/kafka-contacts-service:1.0.0 .
docker push  myacr.azurecr.io/kafka-contacts-service:1.0.0
helm upgrade --install contacts ./helm -n kafka-platform -f helm/values.yaml
```

## Field projection

Input fields (`ContactRaw`) → output fields (`ContactProjected`):

| Output field        | Source                                               |
|---------------------|------------------------------------------------------|
| `contactId`         | `contactId`                                          |
| `accountId`         | `accountId`                                          |
| `fullName`          | `firstName + ' ' + lastName` (collapses on missing)  |
| `title`             | `title`                                              |
| `email`             | `email`                                              |
| `countryCode`       | `address.countryCode` (null if no address)           |
| `preferredLanguage` | `preferredLanguage`                                  |
| `marketingOptIn`    | `marketingOptIn`                                     |
| `updatedAt`         | `updatedAt`                                          |
| `sourceSystem`      | `sourceSystem`                                       |

PII fields (`phone`, `mobile`, `dateOfBirth`, `secondaryEmail`, `middleName`, `salutation`) are
intentionally dropped. `email` is kept because downstream MDM joins on it; mask if your governance
policy differs.
