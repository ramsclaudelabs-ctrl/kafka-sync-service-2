package com.example.contacts.mapper;

import com.example.contacts.avro.ContactAddress;
import com.example.contacts.avro.ContactRaw;
import com.example.contacts.avro.ContactProjected;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ContactMapperTest {

    @Test
    void projects_required_fields_and_drops_pii() {
        ContactRaw raw = ContactRaw.newBuilder()
                .setContactId("c-1")
                .setAccountId("acc-1")
                .setFirstName("Jane")
                .setLastName("Doe")
                .setTitle("Director")
                .setEmail("jane.doe@example.com")
                .setSecondaryEmail("jane@personal.com")
                .setPhone("+1 555 0001")
                .setMobile("+1 555 0002")
                .setMarketingOptIn(true)
                .setPreferredLanguage("en")
                .setAddress(ContactAddress.newBuilder()
                        .setLine1("1 Market St")
                        .setCity("SF")
                        .setPostalCode("94105")
                        .setCountryCode("US")
                        .build())
                .setCreatedAt(Instant.now().toEpochMilli())
                .setUpdatedAt(Instant.now().toEpochMilli())
                .setSourceSystem("SFDC")
                .build();

        ContactProjected out = ContactMapper.toProjected(raw);

        assertThat(out.getContactId()).isEqualTo("c-1");
        assertThat(out.getAccountId()).isEqualTo("acc-1");
        assertThat(out.getFullName()).isEqualTo("Jane Doe");
        assertThat(out.getTitle()).isEqualTo("Director");
        assertThat(out.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(out.getCountryCode()).isEqualTo("US");
        assertThat(out.getPreferredLanguage()).isEqualTo("en");
        assertThat(out.getMarketingOptIn()).isTrue();
        // No accessor for phone, mobile, dateOfBirth, secondaryEmail — they're not in the projected schema.
    }

    @Test
    void full_name_collapses_when_missing_parts() {
        ContactRaw rawNoLast = baseRaw().setFirstName("Jane").setLastName("").build();
        assertThat(ContactMapper.toProjected(rawNoLast).getFullName()).isEqualTo("Jane");

        ContactRaw rawNoFirst = baseRaw().setFirstName("").setLastName("Doe").build();
        assertThat(ContactMapper.toProjected(rawNoFirst).getFullName()).isEqualTo("Doe");
    }

    @Test
    void country_is_null_when_no_address() {
        ContactRaw raw = baseRaw().setAddress(null).build();
        assertThat(ContactMapper.toProjected(raw).getCountryCode()).isNull();
    }

    @Test
    void throws_when_email_is_missing() {
        // Avro builder will throw because email is required (non-null in schema).
        assertThatThrownBy(() ->
                ContactRaw.newBuilder()
                        .setContactId("x")
                        .setFirstName("a").setLastName("b")
                        .setMarketingOptIn(false)
                        .setCreatedAt(0L).setUpdatedAt(0L).setSourceSystem("S")
                        .build()
        ).isInstanceOf(org.apache.avro.AvroRuntimeException.class);
    }

    private ContactRaw.Builder baseRaw() {
        return ContactRaw.newBuilder()
                .setContactId("c-base")
                .setFirstName("Jane").setLastName("Doe")
                .setEmail("jane@example.com")
                .setMarketingOptIn(false)
                .setCreatedAt(0L).setUpdatedAt(0L).setSourceSystem("SFDC");
    }
}
