package works.lysenko.util.apis.grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.d._Ellipse;
import works.lysenko.util.apis.grid.t._Location;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.grid.record.gsrc.Geometry;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("_Ellipse Interface Static Methods Tests")
class _EllipseTest {

    @Test
    @DisplayName("Verify point inside ellipse check")
    void testIsInEllipse() {
        _Location point = new Location(0f, 0f);
        _Location center = new Location(0f, 0f);
        Geometry geometry = new Geometry(new Location(0f, 0f), 5f, 1f);
        works.lysenko.util.grid.Ellipse ellipse = new works.lysenko.util.grid.Ellipse(geometry, 5.0);
        assertTrue(_Ellipse.isInEllipse(point, center, ellipse));
    }
}
