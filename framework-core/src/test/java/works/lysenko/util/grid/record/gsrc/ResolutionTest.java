package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Shape;

import static org.junit.jupiter.api.Assertions.*;

class ResolutionTest {

    @Test
    void testConstructorsAndAccessors() {
        Resolution res1 = new Resolution(3, 4);
        assertEquals(3, res1.x());
        assertEquals(4, res1.y());
        assertEquals(Fraction.ONE, res1.fill());
        assertEquals(Shape.RECTANGLE, res1.shape());

        Resolution res2 = new Resolution(2, 2, Fraction.ONE_HALF, Shape.ELLIPCE);
        assertEquals(2, res2.x());
        assertEquals(2, res2.y());
        assertEquals(Fraction.ONE_HALF, res2.fill());
        assertEquals(Shape.ELLIPCE, res2.shape());
    }

    @Test
    void testFillHandling() {
        Resolution resNullFill = new Resolution(2, 2, null, Shape.RECTANGLE);
        assertEquals(Fraction.ONE, resNullFill.fill());

        Resolution resLargeFill = new Resolution(2, 2, Fraction.TWO, Shape.RECTANGLE);
        assertEquals(Fraction.ONE, resLargeFill.fill());
    }

    @Test
    void testRatioAndSizeAndText() {
        Resolution res = new Resolution(2, 4);
        assertEquals(2.0f, res.ratio(), 0.001);
        assertNotNull(res.size());
        assertNotNull(res.text());
    }
}
