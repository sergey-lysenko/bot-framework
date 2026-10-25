package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NTest {

    @Test
    void testInstantiation() {
        N instance = new N();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(N.NEW_COLOURS_PROPERTY___);
        assertFalse(N.NEW_COLOURS_PROPERTY___.isEmpty());
        assertNotNull(N.NOT_ENOUGH);
        assertFalse(N.NOT_ENOUGH.isEmpty());
        assertNotNull(N.NOT_EQUAL_TO_EXPECTED);
        assertFalse(N.NOT_EQUAL_TO_EXPECTED.isEmpty());
        assertNotNull(N.NOT_FOUND);
        assertFalse(N.NOT_FOUND.isEmpty());
        assertNotNull(N.NULL_IS_NOT_ACCEPTABLE);
        assertFalse(N.NULL_IS_NOT_ACCEPTABLE.isEmpty());
        assertNotNull(N.NOTHING_TO_DO_NOW);
        assertFalse(N.NOTHING_TO_DO_NOW.isEmpty());
        assertNotNull(N.NO_TESTS_WERE_PERFORMED);
        assertFalse(N.NO_TESTS_WERE_PERFORMED.isEmpty());
        assertNotNull(N.NOT_EQUAL);
        assertFalse(N.NOT_EQUAL.isEmpty());
        assertNotNull(N.NEITHER_ACTUAL_NOR___);
        assertFalse(N.NEITHER_ACTUAL_NOR___.isEmpty());
    }
}
