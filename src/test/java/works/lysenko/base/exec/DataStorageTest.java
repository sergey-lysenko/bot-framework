package works.lysenko.base.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DataStorage}.
 */
class DataStorageTest {

    @Test
    void testDataStorageOperations() {
        DataStorage storage = new DataStorage(false);
        storage.put("key1", "val1");
        assertEquals("val1", storage.get("key1"));

        assertTrue(storage.keySet().contains("key1"));

        storage.remove("key1");
        assertNull(storage.get("key1"));
    }
}
