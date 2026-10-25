package works.lysenko.base.test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Stat}.
 */
class StatTest {

    @Test
    void testStatOperations() {
        Stat stat = new Stat();
        assertNotNull(stat.history());
        assertNotNull(stat.times());
        assertNull(stat.previous());
        assertTrue(stat.summary().isEmpty());
    }
}
