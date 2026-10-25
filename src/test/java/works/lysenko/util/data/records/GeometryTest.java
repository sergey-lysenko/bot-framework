package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeometryTest {

    @Test
    void testGeometryRecord() {
        Rectangle rectangle = new Rectangle(0, 0, 10, 10);
        Point center = new Point(5, 5);
        Geometry geometry = new Geometry(null, rectangle, center);
        assertNotNull(geometry);
        assertEquals(rectangle, geometry.rectangle());
        assertEquals(center, geometry.center());
    }
}
