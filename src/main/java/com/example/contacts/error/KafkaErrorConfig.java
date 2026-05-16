package com.example.contacts.error;

import com.example.contacts.config.KafkaTopicsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorConfig {

    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            @SuppressWarnings("rawtypes") KafkaOperations kafkaOperations,
            KafkaTopicsProperties topics,
            MeterRegistry meterRegistry) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaOperations,
                        (ConsumerRecord<?, ?> cr, Exception e) ->
                                new TopicPartition(topics.dlq(), cr.partition()));

        ExponentialBackOff backOff = new ExponentialBackOff(200L, 4.0);
        backOff.setMaxInterval(5_000L);
        backOff.setMaxElapsedTime(10_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                SerializationException.class,
                IllegalArgumentException.class,
                NullPointerException.class);
        handler.addRetryableExceptions(RetriableException.class);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                meterRegistry.counter("contacts.consumer.retry",
                        "attempt", String.valueOf(deliveryAttempt)).increment());
        return handler;
    }

    @Bean
    public ErrorClassifier errorClassifier() {
        return new ErrorClassifier();
    }
}
