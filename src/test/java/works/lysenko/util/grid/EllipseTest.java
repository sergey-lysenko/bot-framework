package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.grid.record.gsrc.Geometry;

import static org.junit.jupiter.api.Assertions.*;

class EllipseTest {

    @Test
    void ellipseCalculation() {
        Geometry geometry = new Geometry(new Location(0, 0), 10, 1.0f);
        Ellipse ellipse = new Ellipse(geometry, 5.0);
        assertEquals(5, ellipse.rX);
        assertEquals(5, ellipse.rY);
    }
}
