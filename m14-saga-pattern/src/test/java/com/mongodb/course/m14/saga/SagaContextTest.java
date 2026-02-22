package com.mongodb.course.m14.saga;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaContextTest {

    @Test
    void put_and_get_roundTrip() {
        var context = new SagaContext();
        context.put("orderId", "ORD-001");
        context.put("amount", 5000L);

        assertThat(context.get("orderId", String.class)).isEqualTo("ORD-001");
        assertThat(context.get("amount", Long.class)).isEqualTo(5000L);
        assertThat(context.get("missing", String.class)).isNull();
    }

    @Test
    void get_withType_castsCorrectly() {
        var context = new SagaContext();
        context.put("count", 42);
        context.put("name", "test");
        context.put("flag", true);

        assertThat(context.get("count", Integer.class)).isEqualTo(42);
        assertThat(context.get("name", String.class)).isEqualTo("test");
        assertThat(context.get("flag", Boolean.class)).isTrue();
    }

    @Test
    void toMap_returnsImmutableCopy() {
        var context = new SagaContext();
        context.put("key1", "value1");

        Map<String, Object> snapshot = context.toMap();
        assertThat(snapshot).containsEntry("key1", "value1");

        // Modifying context after snapshot should not affect snapshot
        context.put("key2", "value2");
        assertThat(snapshot).doesNotContainKey("key2");

        // Snapshot itself should be immutable
        assertThatThrownBy(() -> snapshot.put("key3", "value3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
