package works.lysenko.util.grid.record;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.q._ColoursQuotas;

import static org.junit.jupiter.api.Assertions.*;

class ImageRequirementsTest {

    @Test
    void testPrimaryConstructor() {
        ImageRequirements irq = new ImageRequirements(null, null, null, null, null, null);
        assertNull(irq.colours());
        assertNull(irq.palette());
        assertNull(irq.polychromy());
        assertNull(irq.hue());
        assertNull(irq.saturation());
        assertNull(irq.brightness());
    }

    @Test
    void testSingleColoursConstructor() {
        ImageRequirements irq = new ImageRequirements(null);
        assertNull(irq.colours());
        assertNull(irq.palette());
        assertNull(irq.polychromy());
        assertNull(irq.hue());
        assertNull(irq.saturation());
        assertNull(irq.brightness());
    }
}
