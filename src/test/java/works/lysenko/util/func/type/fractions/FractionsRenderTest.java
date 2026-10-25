package works.lysenko.util.func.type.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FractionsRenderTest {

    @Test
    void testRenderInstantiation() {
        Render render = new Render();
        assertNotNull(render);
    }

    @Test
    void testTs() {
        assertEquals("0.5", Render.ts(new Fraction(1, 2)));
    }
}
