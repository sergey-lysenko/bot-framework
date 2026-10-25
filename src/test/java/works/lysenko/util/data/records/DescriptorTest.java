package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Descriptor Record Tests")
class DescriptorTest {

    @Test
    @DisplayName("Should correctly store and retrieve values")
    void testRecordFields() {
        final Descriptor descriptor = new Descriptor("com.example", "/com/example", 2);

        assertEquals("com.example", descriptor.pack());
        assertEquals("/com/example", descriptor.path());
        assertEquals(2, descriptor.depth());
    }

    @Test
    @DisplayName("Should handle null and edge cases")
    void testNullAndEdgeCases() {
        final Descriptor descriptor = new Descriptor(null, null, -1);

        assertNull(descriptor.pack());
        assertNull(descriptor.path());
        assertEquals(-1, descriptor.depth());
    }

    @Test
    @DisplayName("Should satisfy equality and hashCode contract")
    void testEqualsAndHashCode() {
        final Descriptor d1 = new Descriptor("com.example", "/com/example", 2);
        final Descriptor d2 = new Descriptor("com.example", "/com/example", 2);
        final Descriptor d3 = new Descriptor("com.other", "/com/other", 1);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1, d3);
    }

    @Test
    @DisplayName("Should generate non-null toString representation")
    void testToString() {
        final Descriptor descriptor = new Descriptor("com.example", "/com/example", 2);
        final String str = descriptor.toString();

        assertNotNull(str);
        assertNotEquals("", str);
    }
}
