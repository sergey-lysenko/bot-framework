package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BTest {

    @Test
    void testInstantiation() {
        B instance = new B();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(B.BACKGROUND);
        assertFalse(B.BACKGROUND.isEmpty());
        assertNotNull(B.BEFORE);
        assertFalse(B.BEFORE.isEmpty());
        assertNotNull(B.BETTER);
        assertFalse(B.BETTER.isEmpty());
        assertNotNull(B.BIGGER);
        assertFalse(B.BIGGER.isEmpty());
        assertNotNull(B.BINDING);
        assertFalse(B.BINDING.isEmpty());
        assertNotNull(B.BOOTING);
        assertFalse(B.BOOTING.isEmpty());
        assertNotNull(B.BORDER);
        assertFalse(B.BORDER.isEmpty());
        assertNotNull(B.BOUNDARIES);
        assertFalse(B.BOUNDARIES.isEmpty());
        assertNotNull(B.BOUNDS);
        assertFalse(B.BOUNDS.isEmpty());
        assertNotNull(B.BRIGHTNESS);
        assertFalse(B.BRIGHTNESS.isEmpty());
        assertNotNull(B.BROWSER);
        assertFalse(B.BROWSER.isEmpty());
        assertNotNull(B.BUILDING);
        assertFalse(B.BUILDING.isEmpty());
        assertNotNull(B.BUNDLE);
        assertFalse(B.BUNDLE.isEmpty());
        assertNotNull(B.BUTTON);
        assertFalse(B.BUTTON.isEmpty());
    }
}
