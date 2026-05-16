package com.example.contacts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Entry point for the Contacts field-projection service.
 *
 * <p>This service reads {@code contacts.raw.v1}, projects a subset of fields
 * (see {@link com.example.contacts.mapper.ContactMapper}), and writes
 * {@code contacts.projected.v1}. Errors are routed to {@code contacts.projected.v1.DLQ}.
 */
@SpringBootApplication
@EnableKafka
public class ContactsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContactsServiceApplication.class, args);
    }
}
