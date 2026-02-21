package com.mongodb.course.m18.ecommerce;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.Date;

@WritingConverter
public class CustomerWriteConverter implements Converter<Customer, Document> {

    @Override
    public Document convert(Customer source) {
        var doc = new Document();
        if (source.id() != null) {
            if (ObjectId.isValid(source.id())) {
                doc.put("_id", new ObjectId(source.id()));
            } else {
                doc.put("_id", source.id());
            }
        }
        doc.put("name", source.name());
        doc.put("email", source.email());
        doc.put("phone", source.phone());

        if (source.address() != null) {
            var addrDoc = new Document()
                    .append("street", source.address().street())
                    .append("city", source.address().city())
                    .append("zipCode", source.address().zipCode())
                    .append("country", source.address().country());
            doc.put("address", addrDoc);
        }

        doc.put("loyaltyTier", source.loyaltyTier() != null ? source.loyaltyTier() : "BRONZE");
        doc.put("registeredAt", source.registeredAt() != null ? Date.from(source.registeredAt()) : Date.from(java.time.Instant.EPOCH));
        doc.put("schemaVersion", 3);
        return doc;
    }
}
