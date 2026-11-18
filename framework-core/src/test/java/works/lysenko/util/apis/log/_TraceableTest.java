package works.lysenko.util.apis.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("_Traceable Static Methods Tests")
class _TraceableTest {

    @Test
    @DisplayName("Verify key static method creates KeyValue pair")
    void testKey() {
        var kv = _Traceable.key("k1", "v1");
        assertNotNull(kv);
        assertEquals("k1", kv.k());
        assertEquals("v1", kv.v());
    }

    @Test
    @DisplayName("Verify create static factory creates _Traceable instance")
    void testCreate() {
        var traceable = _Traceable.create(_Traceable.key("k1", "v1"), _Traceable.key("k2", "v2"));
        assertNotNull(traceable);
        assertEquals(2, traceable.size());
        assertEquals("k1", traceable.name(0));
        assertEquals("v1", traceable.value(0));
    }
}
