package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Rectangle;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OperationMarkersTest {

    @Test
    void testAvailableModes() {
        List<String> modes = OperationMarkers.availableModes();
        assertTrue(modes.contains("neutral"));
        assertTrue(modes.contains("military"));
        assertTrue(modes.contains("classic"));
    }

    @Test
    void testFindMarker() {
        assertTrue(OperationMarkers.find("military").orElseThrow() instanceof MilitaryMarker);
        assertTrue(OperationMarkers.find("neutral").orElseThrow() instanceof NeutralMarker);
        assertTrue(OperationMarkers.find("classic").orElseThrow() instanceof ClassicMarker);
        assertTrue(OperationMarkers.find("unknown").isEmpty());
    }

    @Test
    void testMarkingExecution() {
        BufferedImage img = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Rectangle rect = new Rectangle(50, 50, 100, 40);

        for (String mode : OperationMarkers.availableModes()) {
            OperationMarker marker = OperationMarkers.find(mode).orElseThrow();
            assertNotNull(marker.markElement(img, rect, "TestLabel", "CLICK"));
            assertNotNull(marker.markPoint(img, new Point(100, 100), "TAP"));
            assertNotNull(marker.markSwipe(img, new Point(50, 50), new Point(50, 200)));
            assertNotNull(marker.stampStatus(img, "TEST STATUS"));
        }
    }
}
