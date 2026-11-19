package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.t._AbsoluteRegion;
import works.lysenko.util.data.records.Location;

import static org.junit.jupiter.api.Assertions.*;

class AbsoluteRegionTest {

    @Test
    void createAndExpandRegion() {
        _AbsoluteRegion region = AbsoluteRegion.create(10, 20);
        assertNotNull(region);
        assertEquals(new Location(10, 20), region.min());
        assertEquals(new Location(11, 21), region.max());

        region.expand(5, 25);
        assertEquals(new Location(5, 20), region.min());
        assertEquals(new Location(11, 26), region.max());
    }

    @Test
    void includesCoordinates() {
        _AbsoluteRegion region = AbsoluteRegion.create(10, 10);
        assertTrue(region.includes(10, 10, null));
        assertTrue(region.includes(10, 10, 5));
        assertFalse(region.includes(20, 20, 1));
    }

    @Test
    void distanceCalculation() {
        double dist = AbsoluteRegion.distance(0, 0, new org.openqa.selenium.Point(3, 4));
        assertEquals(5.0, dist, 0.001);
    }
}
