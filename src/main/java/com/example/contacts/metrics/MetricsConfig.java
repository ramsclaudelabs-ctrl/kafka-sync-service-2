package com.example.contacts.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterFilter commonTags(@Value("${app.env:local}") String env) {
        return MeterFilter.commonTags(java.util.List.of(
                io.micrometer.core.instrument.Tag.of("service", "kafka-contacts-service"),
                io.micrometer.core.instrument.Tag.of("env", env)
        ));
    }

    @Bean
    public MeterRegistryCustomizerHolder meterRegistryCustomizerHolder(MeterRegistry registry) {
        return new MeterRegistryCustomizerHolder(registry);
    }

    public record MeterRegistryCustomizerHolder(MeterRegistry registry) {}
}
