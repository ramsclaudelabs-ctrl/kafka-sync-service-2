package com.example.contacts.config;

import com.example.contacts.avro.ContactProjected;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final KafkaProperties bootProps;

    public KafkaProducerConfig(KafkaProperties bootProps) {
        this.bootProps = bootProps;
    }

    @Bean
    public ProducerFactory<String, ContactProjected> producerFactory() {
        Map<String, Object> props = new HashMap<>(bootProps.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, ContactProjected> kafkaTemplate(
            ProducerFactory<String, ContactProjected> pf) {
        KafkaTemplate<String, ContactProjected> tpl = new KafkaTemplate<>(pf);
        tpl.setObservationEnabled(true);
        return tpl;
    }
}
