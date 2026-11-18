package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("Location Record Tests")
class LocationTest {

    @Test
    @DisplayName("Should create Location via point() with int arguments")
    void testPointInt() {
        final Location loc = Location.point(10, 20);

        assertEquals(10.0f, loc.x());
        assertEquals(20.0f, loc.y());
    }

    @Test
    @DisplayName("Should create Location via point() with double arguments")
    void testPointDouble() {
        final Location loc = Location.point(10.5d, 20.25d);

        assertEquals(10.5f, loc.x());
        assertEquals(20.25f, loc.y());
    }

    @Test
    @DisplayName("Should create Location via point() with float arguments")
    void testPointFloat() {
        final Location loc = Location.point(12.3f, 45.6f);

        assertEquals(12.3f, loc.x());
        assertEquals(45.6f, loc.y());
    }

    @Test
    @DisplayName("Should round x and y to integer via X() and Y()")
    void testRoundedCoordinates() {
        final Location loc1 = Location.point(10.6f, 20.4f);

        assertEquals(10, loc1.X());
        assertEquals(20, loc1.Y());

        final Location loc2 = Location.point(0.0f, 0.0f);
        assertEquals(0, loc2.X());
        assertEquals(0, loc2.Y());
    }

    @Test
    @DisplayName("Should correctly compare locations via custom equals method")
    void testCustomEquals() {
        final Location loc1 = Location.point(5.0f, 10.0f);
        final Location loc2 = Location.point(5.0f, 10.0f);
        final Location loc3 = Location.point(5.1f, 10.0f);

        assertEquals(loc1, loc1);
        assertEquals(loc1, loc2);
        assertNotEquals(loc1, loc3);
        assertFalse(loc1.equals(null));
        assertFalse(loc1.equals("Not A Location"));
    }
}
