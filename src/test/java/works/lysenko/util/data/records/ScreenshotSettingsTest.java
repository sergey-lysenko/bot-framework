package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotSettingsTest {

    @Test
    void testRecordFields() {
        final ScreenshotSettings settings = new ScreenshotSettings(true, false, true, null, true);
        assertTrue(settings.silent());
        assertFalse(settings.storeFullscreen());
        assertTrue(settings.storeCropped());
        assertNull(settings.element());
        assertTrue(settings.root());
    }
}
