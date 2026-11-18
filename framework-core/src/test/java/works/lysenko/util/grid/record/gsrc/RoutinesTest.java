package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.records.Size;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.grid.record.Request;

import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RoutinesTest {

    @Test
    void testGetGridElementCoordinates() {
        Geometry geom = new Geometry(new Location(10f, 10f), 10f, 1.0f);
        Location coords = Routines.getGridElementCoordinates(geom, 2, 1, 1);
        assertNotNull(coords);
        assertEquals(15f, coords.x());
        assertEquals(15f, coords.y());
    }

    @Test
    void testGetRequestedGridLocationWithNulls() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Request req = new Request("req", img, null, Fraction.ONE, new Resolution(1, 1), null, null);
        Geometry geom = Routines.getRequestedGridLocation(req, img);
        assertNull(geom);
    }

    @Test
    void testGetRequestedGridLocationDimensions() {
        Geometry geom = Routines.getRequestedGridLocation(100, 200, 0.5, 0.5, 1.0, 1.0f);
        assertNotNull(geom);
        assertEquals(50f, geom.center().x());
        assertEquals(100f, geom.center().y());
    }

    @Test
    void testIsWithinNullExclude() {
        assertFalse(Routines.isWithin(null, new Location(0, 0), new Size(100, 100)));
    }

    @Test
    void testIsWithinWithRegion() {
        Region reg = new Region(new RelativePosition(Fraction.ZERO, Fraction.ZERO), Fraction.ONE, Fraction.ONE);
        assertFalse(Routines.isWithin(Collections.singletonList(reg), new Location(50, 50), new Size(100, 100)));
    }
}
