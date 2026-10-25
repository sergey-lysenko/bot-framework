package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GTest {

    @Test
    void testInstantiation() {
        G instance = new G();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(G.GETTING_FULLSCREEN_IMAGE);
        assertFalse(G.GETTING_FULLSCREEN_IMAGE.isEmpty());
        assertNotNull(G.GRID_IS_TOO_DENSE___);
        assertFalse(G.GRID_IS_TOO_DENSE___.isEmpty());
        assertNotNull(G.GETTING_PARTIAL_SCREENSHOT_OF);
        assertFalse(G.GETTING_PARTIAL_SCREENSHOT_OF.isEmpty());
    }
}
