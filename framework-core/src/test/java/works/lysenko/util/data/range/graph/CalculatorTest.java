package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.range.Quota;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculatorTest {

    @Test
    void testMaxMapValueNull() {
        assertNull(Calculator.maxMapValue(null));
    }

    @Test
    void testMaxShareNull() {
        assertNull(Calculator.maxShare(null));
    }

    @Test
    void testMaxShareList() {
        final Quota<Integer> q1 = new Quota<>(1, null, null, Fraction.ZERO, Fraction.ONE_QUARTER);
        final Quota<Integer> q2 = new Quota<>(2, null, null, Fraction.ZERO, Fraction.ONE_HALF);

        assertEquals(Fraction.ONE_HALF, Calculator.maxShare(List.of(q1, q2)));
    }
}
