package com.example.contacts.consumer;

import com.example.contacts.avro.ContactProjected;
import com.example.contacts.avro.ContactRaw;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 2,
        topics = {"contacts.raw.v1", "contacts.projected.v1", "contacts.projected.v1.DLQ"})
@TestPropertySource(properties = {
        "spring.kafka.consumer.group-id=contacts-test",
        "spring.kafka.listener.concurrency=2",
        "app.kafka.topics.source=contacts.raw.v1",
        "app.kafka.topics.sink=contacts.projected.v1",
        "app.kafka.topics.dlq=contacts.projected.v1.DLQ"
})
class ContactListenerEmbeddedTest {

    private static final String SR_URL = "mock://test-contact-listener";

    @DynamicPropertySource
    static void registryProps(DynamicPropertyRegistry r) {
        r.add("spring.kafka.properties.schema.registry.url", () -> SR_URL);
    }

    @Autowired
    private org.springframework.kafka.test.EmbeddedKafkaBroker broker;

    @Test
    void end_to_end_projection() {
        Map<String, Object> p = KafkaTestUtils.producerProps(broker);
        p.put("key.serializer", StringSerializer.class);
        p.put("value.serializer", KafkaAvroSerializer.class);
        p.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SR_URL);
        KafkaTemplate<String, ContactRaw> rawProducer =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(p));

        ContactRaw raw = ContactRaw.newBuilder()
                .setContactId("c-it-1")
                .setAccountId("acc-it-1")
                .setFirstName("Jane").setLastName("Doe")
                .setEmail("jane.doe@example.com")
                .setMarketingOptIn(true)
                .setPreferredLanguage("en")
                .setCreatedAt(Instant.now().toEpochMilli())
                .setUpdatedAt(Instant.now().toEpochMilli())
                .setSourceSystem("SFDC")
                .build();
        rawProducer.send(new ProducerRecord<>("contacts.raw.v1", raw.getContactId().toString(), raw));
        rawProducer.flush();

        Map<String, Object> c = KafkaTestUtils.consumerProps("verifier", "true", broker);
        c.put("key.deserializer", StringDeserializer.class);
        c.put("value.deserializer", KafkaAvroDeserializer.class);
        c.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SR_URL);
        c.put("specific.avro.reader", true);
        Consumer<String, ContactProjected> verifier =
                new org.apache.kafka.clients.consumer.KafkaConsumer<>(c);
        verifier.subscribe(java.util.List.of("contacts.projected.v1"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            ConsumerRecords<String, ContactProjected> records = verifier.poll(Duration.ofMillis(500));
            assertThat(records).isNotEmpty();
            ConsumerRecord<String, ContactProjected> rec = records.iterator().next();
            assertThat(rec.value().getContactId()).hasToString("c-it-1");
            assertThat(rec.value().getFullName()).hasToString("Jane Doe");
            assertThat(rec.value().getEmail()).hasToString("jane.doe@example.com");
            assertThat(rec.value().getMarketingOptIn()).isTrue();
        });

        verifier.close();
        MockSchemaRegistry.dropScope("test-contact-listener");
    }
}
