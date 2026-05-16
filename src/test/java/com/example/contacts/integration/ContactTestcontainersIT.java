package com.example.contacts.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/** CI-only integration test against a real broker. See accounts service for full notes. */
@Tag("tc")
@Testcontainers
@SpringBootTest
class ContactTestcontainersIT {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Test
    void broker_is_reachable() {
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotBlank();
    }
}
