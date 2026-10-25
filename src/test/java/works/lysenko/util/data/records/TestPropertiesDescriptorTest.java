package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPropertiesDescriptorTest {

    @Test
    void testRecordFields() {
        final TestPropertiesDescriptor desc = new TestPropertiesDescriptor("config", "test", "properties");
        assertEquals("config", desc.directory());
        assertEquals("test", desc.name());
        assertEquals("properties", desc.extension());
    }
}
