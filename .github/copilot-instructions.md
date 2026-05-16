# GitHub Copilot instructions — kafka-contacts-service

Copilot, follow these conventions when generating code in this repo. They mirror the Accounts
service conventions intentionally — the two repos must be operationally similar.

## Stack & versions

- Java 21, Spring Boot 3.3.x, Spring Kafka 3.x, Maven 3.9.x.
- Avro 1.11.x with `SpecificRecord` only — never `GenericRecord`.
- Confluent `kafka-avro-serializer` + Confluent Cloud Schema Registry.
- Tests: JUnit 5 + AssertJ + `@EmbeddedKafka` for fast tests, Testcontainers for IT.
- JMH 1.37 for mapper micro-benchmarks under `src/jmh/java`.

## Code style

- Mappers are `final` classes with private constructors and a single public `static` method.
- Configuration is `@ConfigurationProperties` records with `jakarta.validation`.
- All Kafka listeners must:
  - Use a dedicated `ConcurrentKafkaListenerContainerFactory` named `contactKafkaListenerContainerFactory`.
  - Use `AckMode.MANUAL_IMMEDIATE` and commit the offset only after a successful producer ack.
  - Wrap downstream sends in `CompletableFuture.whenComplete(...)` and rethrow on failure.
- Producer configured with `acks=all`, `enable.idempotence=true`, `compression.type=zstd`,
  `linger.ms=20`, `batch.size=64KB`.
- All error handling lives in `error/KafkaErrorConfig.java`.

## Avro

- New fields on **input** topics must be `["null", "<type>"]` with `"default": null` — BACKWARD
  compatibility on the registry must not break.
- Never change a field's name or type in place — add a new field and migrate consumers first.
- PII fields (`phone`, `mobile`, `dateOfBirth`, `secondaryEmail`) must never be added to
  `ContactProjected.avsc`. If a new PII field needs to flow downstream, escalate to the data
  governance owners before changing the projection.
- Run `mvn generate-sources` after editing any `.avsc` file.

## Testing

- One unit test per mapper branch.
- Use `@EmbeddedKafka` + `MockSchemaRegistry` (mock URL) for listener-level tests.
- No sleeps — use `Awaitility`.
- Tag heavier tests with `@Tag("tc")`.

## Observability

- Every Spring bean must emit at least one Micrometer metric.
- Tag with `service` and `env` via the global `MeterFilter` only.

## Don'ts

- Don't use Kafka Streams or Flink.
- Don't add `@Transactional` to Kafka sends.
- Don't catch and swallow `KafkaException`.
- Don't use field injection — constructor inject everything.

## Files Copilot must not rewrite

- `src/main/avro/*.avsc` — schema changes require human review.
- `helm/templates/secretproviderclass.yaml` — touching this can break Key Vault binding.
- `pom.xml` Avro & Confluent versions.
