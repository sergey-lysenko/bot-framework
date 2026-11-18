package works.lysenko.util.func.type.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FractionsRenderTest {

    @Test
    void testTs() {
        assertEquals("0.5", Render.ts(new Fraction(1, 2)));
    }
}
