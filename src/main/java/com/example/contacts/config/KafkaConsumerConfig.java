package com.example.contacts.config;

import com.example.contacts.avro.ContactRaw;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * See {@code com.example.accounts.config.KafkaConsumerConfig} for the rationale — this class
 * is the Contacts mirror.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private final KafkaProperties bootProps;

    @Value("${spring.kafka.listener.concurrency:4}")
    private int concurrency;

    public KafkaConsumerConfig(KafkaProperties bootProps) {
        this.bootProps = bootProps;
    }

    @Bean
    public ConsumerFactory<String, ContactRaw> consumerFactory() {
        Map<String, Object> props = new HashMap<>(bootProps.buildConsumerProperties(null));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "contactKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, ContactRaw>
    contactKafkaListenerContainerFactory(ConsumerFactory<String, ContactRaw> cf) {
        ConcurrentKafkaListenerContainerFactory<String, ContactRaw> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }
}
