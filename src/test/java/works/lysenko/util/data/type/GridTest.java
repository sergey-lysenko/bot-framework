package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Shape;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Resolution;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GridTest {

    @Test
    void testGridConstructionAndAccessors() {
        final BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        final Geometry geometry = new Geometry(new Location(50f, 50f), 20f, 1f);
        final Resolution resolution = new Resolution(2, 2, Fraction.ONE, Shape.RECTANGLE);

        final Request request = new Request("test", img, null, Fraction.ONE, resolution, Collections.emptyList(), null);

        final Grid grid = new Grid(img, geometry, request);

        assertNotNull(grid.getGrid());
        assertNotNull(grid.calculator());
        assertNotNull(grid.getCalculator());
        assertNotNull(grid.processor());
        assertNotNull(grid.getProcessor());
        assertNotNull(grid.render());

        final Collection<Point> samples = grid.getSamplePoints();
        assertNotNull(samples);
        assertFalse(samples.isEmpty());
    }
}
