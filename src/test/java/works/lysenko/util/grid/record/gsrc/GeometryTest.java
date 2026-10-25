package works.lysenko.util.grid.record.gsrc;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Location;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {

    @Test
    void testGeometryInstantiation() {
        Location loc = new Location(10f, 20f);
        Geometry geometry = new Geometry(loc, 5.0f, 1.5f);
        assertEquals(loc, geometry.center());
        assertEquals(5.0f, geometry.radius());
        assertEquals(1.5f, geometry.ratio());
    }
}
