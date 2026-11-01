package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WTest {

    @Test
    void testInstantiation() {
        W instance = new W();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(W.WAITING_FOR);
        assertFalse(W.WAITING_FOR.isEmpty());
        assertNotNull(W.WAITING_FOR_INVISIBILITY_OF);
        assertFalse(W.WAITING_FOR_INVISIBILITY_OF.isEmpty());
        assertNotNull(W.WAITING_FOR_VISIBILITY_OF);
        assertFalse(W.WAITING_FOR_VISIBILITY_OF.isEmpty());
        assertNotNull(W.WAITING_WHILE);
        assertFalse(W.WAITING_WHILE.isEmpty());
        assertNotNull(W.WAS_NOT_REACHABLE);
        assertFalse(W.WAS_NOT_REACHABLE.isEmpty());
        assertNotNull(W.WD_HUB);
        assertFalse(W.WD_HUB.isEmpty());
        assertNotNull(W.WERE_REPRODUCED);
        assertFalse(W.WERE_REPRODUCED.isEmpty());
        assertNotNull(W.WHILE_TRYING_TO);
        assertFalse(W.WHILE_TRYING_TO.isEmpty());
        assertNotNull(W.WHILE_TRYING_TO_CLICK_);
        assertFalse(W.WHILE_TRYING_TO_CLICK_.isEmpty());
        assertNotNull(W.WIDTH_IS_STILL);
        assertFalse(W.WIDTH_IS_STILL.isEmpty());
        assertNotNull(W.WIDTH_WAS_REDUCED_BY___);
        assertFalse(W.WIDTH_WAS_REDUCED_BY___.isEmpty());
        assertNotNull(W.WITH_NO_DEFINED_RANGES);
        assertFalse(W.WITH_NO_DEFINED_RANGES.isEmpty());
        assertNotNull(W.WRITING_FULL_SCREENSHOT);
        assertFalse(W.WRITING_FULL_SCREENSHOT.isEmpty());
        assertNotNull(W.WRITING_CROPPED_IMAGE);
        assertFalse(W.WRITING_CROPPED_IMAGE.isEmpty());
        assertNotNull(W.WRITING_FULLSCREEN_IMAGE);
        assertFalse(W.WRITING_FULLSCREEN_IMAGE.isEmpty());
        assertNotNull(W.WEB_DRIVER);
        assertFalse(W.WEB_DRIVER.isEmpty());
        assertEquals("**[WARNING]** ", W.WARNING_MARKER);
    }
}
