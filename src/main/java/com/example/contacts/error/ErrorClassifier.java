package com.example.contacts.error;

import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.support.serializer.DeserializationException;

/** Classifies Kafka pipeline failures so the right policy is applied. */
public class ErrorClassifier {

    public enum Kind { RETRYABLE, POISON_PILL, BUG }

    public Kind classify(Throwable t) {
        if (t == null) return Kind.BUG;
        if (t instanceof DeserializationException) return Kind.POISON_PILL;
        if (t instanceof RetriableException) return Kind.RETRYABLE;
        Throwable cause = t.getCause();
        if (cause != null && cause != t) return classify(cause);
        return Kind.BUG;
    }
}
