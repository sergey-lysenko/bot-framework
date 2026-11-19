package works.lysenko.util.grid.validation;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceableTest {

    @Test
    void testTraceableRecord() {
        List<KeyValue<String, String>> list = List.of(KeyValue.kv("key1", "val1"), KeyValue.kv("key2", "val2"));
        Traceable traceable = new Traceable(list);

        assertEquals(2, traceable.size());
        assertEquals("key1", traceable.name(0));
        assertEquals("val1", traceable.value(0));
        assertEquals("key2", traceable.name(1));
        assertEquals("val2", traceable.value(1));
    }
}
