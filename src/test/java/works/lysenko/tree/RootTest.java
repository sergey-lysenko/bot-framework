package works.lysenko.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RootTest {

    private static class DummyRoot extends Root {}

    @Test
    void testGenerateUUID() {
        final String uuid1 = Root.generateUUID();
        final String uuid2 = Root.generateUUID();

        assertNotNull(uuid1);
        assertNotNull(uuid2);
        assertNotEquals(uuid1, uuid2);
        assertEquals(36, uuid1.length(), "UUID string length should be 36");
    }

    @Test
    void testStatsFormat() {
        final String stats = Root.stats("Items", 5, 10);
        assertNotNull(stats);
        assertTrue(stats.contains("Items"));
        assertTrue(stats.contains("5/10"));
    }

    @Test
    void testDataAndSessionInitialization() {
        final DummyRoot root = new DummyRoot();
        assertNotNull(root.data, "Data storage instance should be initialized");
        assertNotNull(root.session, "Session storage instance should be initialized");
    }
}
