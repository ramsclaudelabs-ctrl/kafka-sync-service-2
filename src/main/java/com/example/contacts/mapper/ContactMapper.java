package com.example.contacts.mapper;

import com.example.contacts.avro.ContactAddress;
import com.example.contacts.avro.ContactProjected;
import com.example.contacts.avro.ContactRaw;

import java.util.Objects;

/**
 * Pure field-projection: ContactRaw -> ContactProjected.
 *
 * <p>Drops PII fields ({@code phone}, {@code mobile}, {@code dateOfBirth}, {@code secondaryEmail}).
 * Concatenates {@code firstName} + {@code lastName} into {@code fullName}.
 */
public final class ContactMapper {

    private ContactMapper() {}

    public static ContactProjected toProjected(ContactRaw src) {
        Objects.requireNonNull(src, "ContactRaw must not be null");
        Objects.requireNonNull(src.getContactId(), "contactId is required");
        Objects.requireNonNull(src.getEmail(), "email is required");

        String fullName = buildFullName(src.getFirstName(), src.getLastName());

        String countryCode = null;
        ContactAddress addr = src.getAddress();
        if (addr != null) {
            countryCode = addr.getCountryCode();
        }

        return ContactProjected.newBuilder()
                .setContactId(src.getContactId())
                .setAccountId(src.getAccountId())
                .setFullName(fullName)
                .setTitle(src.getTitle())
                .setEmail(src.getEmail())
                .setCountryCode(countryCode)
                .setPreferredLanguage(src.getPreferredLanguage())
                .setMarketingOptIn(src.getMarketingOptIn())
                .setUpdatedAt(src.getUpdatedAt())
                .setSourceSystem(src.getSourceSystem())
                .build();
    }

    private static String buildFullName(CharSequence first, CharSequence last) {
        String f = first == null ? "" : first.toString().trim();
        String l = last == null ? "" : last.toString().trim();
        if (f.isEmpty()) return l;
        if (l.isEmpty()) return f;
        return f + " " + l;
    }
}
