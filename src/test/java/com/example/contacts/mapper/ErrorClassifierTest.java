package com.example.contacts.mapper;

import com.example.contacts.error.ErrorClassifier;
import com.example.contacts.error.ErrorClassifier.Kind;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.DeserializationException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorClassifierTest {

    private final ErrorClassifier sut = new ErrorClassifier();

    @Test
    void deserialization_error_is_poison_pill() {
        assertThat(sut.classify(new DeserializationException("bad bytes", new byte[]{}, false, new RuntimeException())))
                .isEqualTo(Kind.POISON_PILL);
    }

    @Test
    void retriable_kafka_error_is_retryable() {
        assertThat(sut.classify(new TimeoutException("broker slow"))).isEqualTo(Kind.RETRYABLE);
    }

    @Test
    void unknown_error_is_bug() {
        assertThat(sut.classify(new IllegalStateException("oops"))).isEqualTo(Kind.BUG);
    }

    @Test
    void unwraps_causes() {
        Throwable t = new RuntimeException("wrap", new TimeoutException("inner"));
        assertThat(sut.classify(t)).isEqualTo(Kind.RETRYABLE);
    }
}
