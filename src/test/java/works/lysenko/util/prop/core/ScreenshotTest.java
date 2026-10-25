package works.lysenko.util.prop.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.core.Screenshot.*;

class ScreenshotTest {
    @Test
    void testDefaultsAndRefresh() {
        assertTrue(binds == true || binds == false);
        assertTrue(clicks == true || clicks == false);
        assertTrue(longClicks == true || longClicks == false);
        assertTrue(pixels == true || pixels == false);
        assertTrue(swipes == true || swipes == false);
        assertDoesNotThrow(Screenshot::refresh);
    }
}
