package com.mongodb.course.m18.ecommerce;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.Instant;
import java.util.Date;

@ReadingConverter
public class CustomerReadConverter implements Converter<Document, Customer> {

    @Override
    public Customer convert(Document source) {
        int version = source.getInteger("schemaVersion", 1);

        if (version < 2) {
            migrateV1toV2(source);
        }
        if (version < 3) {
            migrateV2toV3(source);
        }

        return mapToCustomer(source);
    }

    private void migrateV1toV2(Document source) {
        var address = new Document()
                .append("street", source.getString("street"))
                .append("city", source.getString("city"))
                .append("zipCode", source.getString("zipCode"))
                .append("country", source.getString("country"));
        source.put("address", address);
        source.remove("street");
        source.remove("city");
        source.remove("zipCode");
        source.remove("country");
        source.put("schemaVersion", 2);
    }

    private void migrateV2toV3(Document source) {
        source.putIfAbsent("loyaltyTier", "BRONZE");
        source.putIfAbsent("registeredAt", Date.from(Instant.EPOCH));
        source.put("schemaVersion", 3);
    }

    private Customer mapToCustomer(Document source) {
        Document addrDoc = source.get("address", Document.class);
        Address address = addrDoc != null
                ? new Address(
                        addrDoc.getString("street"),
                        addrDoc.getString("city"),
                        addrDoc.getString("zipCode"),
                        addrDoc.getString("country"))
                : null;

        Object rawRegisteredAt = source.get("registeredAt");
        Instant registeredAt;
        if (rawRegisteredAt instanceof Date date) {
            registeredAt = date.toInstant();
        } else if (rawRegisteredAt instanceof Instant instant) {
            registeredAt = instant;
        } else {
            registeredAt = Instant.EPOCH;
        }

        return new Customer(
                source.getObjectId("_id") != null ? source.getObjectId("_id").toHexString() : null,
                source.getString("name"),
                source.getString("email"),
                source.getString("phone"),
                address,
                source.getString("loyaltyTier"),
                registeredAt,
                source.getInteger("schemaVersion", 3)
        );
    }
}
