package works.lysenko.util.grid.record;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;

import java.awt.image.BufferedImage;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testInstantiationAndAccessors() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        _RelativePosition pos = new works.lysenko.util.data.type.RelativePosition(Fraction.ZERO, Fraction.ONE);
        _Resolution res = new works.lysenko.util.grid.record.gsrc.Resolution(2, 2);
        Iterable<_Region> exclude = Collections.emptyList();
        _ImageRequirements irq = new ImageRequirements(null);

        Request request = new Request("testReq", img, pos, Fraction.ONE, res, exclude, irq);

        assertEquals("testReq", request.name());
        assertEquals(img, request.image());
        assertEquals(pos, request.position());
        assertEquals(Fraction.ONE, request.scale());
        assertEquals(res, request.resolution());
        assertEquals(exclude, request.exclude());
        assertEquals(irq, request.irq());
    }

    @Test
    void testNullValues() {
        Request request = new Request(null, null, null, null, null, null, null);
        assertNull(request.name());
        assertNull(request.image());
        assertNull(request.position());
        assertNull(request.scale());
        assertNull(request.resolution());
        assertNull(request.exclude());
        assertNull(request.irq());
    }
}
