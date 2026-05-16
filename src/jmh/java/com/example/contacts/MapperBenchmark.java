package com.example.contacts;

import com.example.contacts.avro.ContactAddress;
import com.example.contacts.avro.ContactRaw;
import com.example.contacts.mapper.ContactMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 5)
public class MapperBenchmark {

    private ContactRaw raw;

    @Setup
    public void setup() {
        raw = ContactRaw.newBuilder()
                .setContactId("c-bench")
                .setAccountId("acc-bench")
                .setFirstName("Jane").setLastName("Doe")
                .setEmail("jane.doe@example.com")
                .setPhone("+1 555 0000")
                .setMarketingOptIn(true)
                .setPreferredLanguage("en")
                .setAddress(ContactAddress.newBuilder()
                        .setLine1("1 Market St").setCity("SF").setPostalCode("94105").setCountryCode("US")
                        .build())
                .setCreatedAt(Instant.now().toEpochMilli())
                .setUpdatedAt(Instant.now().toEpochMilli())
                .setSourceSystem("SFDC")
                .build();
    }

    @Benchmark
    public void project(Blackhole bh) {
        bh.consume(ContactMapper.toProjected(raw));
    }
}
