package works.lysenko.util.func.core;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.TestPropertiesDescriptor;

import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestPropertiesTest {

    @Test
    void testTestPropertiesInstantiation() {
        TestProperties tp = new TestProperties();
        assertNotNull(tp);
    }

    @Test
    void testReadTestPropertiesFromFileNonExistentThrows() {
        TestPropertiesDescriptor desc = new TestPropertiesDescriptor("src/main/resources/config", "nonexistent", ".properties");
        assertThrows(UncheckedIOException.class, () -> TestProperties.readTestPropertiesFromFile(desc));
    }
}
