package works.lysenko.util.apis.grid;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class _GridProviderTest {

    @Test
    void standaloneCoreFailsClosedOnVerifyImageWithoutProvider() {
        assertTrue(_GridProvider.find().isEmpty());
        assertFalse(_GridProvider.verifyImage(null, "MissingProviderScenario"));
        assertEquals(_GridProvider.DEFAULT_MARKER_MODES, _GridProvider.availableMarkerModes());
    }

    @Test
    void describeRectangleFormatsDimensionsAndCoordinates() {
        final String desc = _GridProvider.describeRectangle(new Rectangle(10, 20, 30, 40));
        assertTrue(desc.contains("30"));
        assertTrue(desc.contains("40"));
        assertTrue(desc.contains("10"));
        assertTrue(desc.contains("20"));
    }
}
