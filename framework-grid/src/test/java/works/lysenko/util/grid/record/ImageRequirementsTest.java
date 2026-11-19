package works.lysenko.util.grid.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageRequirementsTest {

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
