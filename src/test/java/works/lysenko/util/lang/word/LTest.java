package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LTest {

    @Test
    void testInstantiation() {
        L instance = new L();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(L.LAUNCHER);
        assertFalse(L.LAUNCHER.isEmpty());
        assertNotNull(L.LAYOUT);
        assertFalse(L.LAYOUT.isEmpty());
        assertNotNull(L.LEVEL);
        assertFalse(L.LEVEL.isEmpty());
        assertNotNull(L.LIMBO);
        assertFalse(L.LIMBO.isEmpty());
        assertNotNull(L.LINEAR);
        assertFalse(L.LINEAR.isEmpty());
        assertNotNull(L.LINES);
        assertFalse(L.LINES.isEmpty());
        assertNotNull(L.LOADING);
        assertFalse(L.LOADING.isEmpty());
        assertNotNull(L.LOCATED);
        assertFalse(L.LOCATED.isEmpty());
        assertNotNull(L.LOCATING);
        assertFalse(L.LOCATING.isEmpty());
        assertNotNull(L.LOCATION);
        assertFalse(L.LOCATION.isEmpty());
        assertNotNull(L.LOCATOR);
        assertFalse(L.LOCATOR.isEmpty());
        assertNotNull(L.LOCATORS);
        assertFalse(L.LOCATORS.isEmpty());
        assertNotNull(L.LOGGER);
        assertFalse(L.LOGGER.isEmpty());
        assertNotNull(L.LOWER);
        assertFalse(L.LOWER.isEmpty());
    }
}
