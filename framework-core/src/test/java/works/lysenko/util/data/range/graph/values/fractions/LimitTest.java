package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.grid.record.graph.Options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitTest {

    @Test
    void testLimitCalculation() {
        final Options go = new Options(10, new Fraction(1, 2), new Fraction(3, 4), new Slack(Fraction.ZERO), Fraction.ZERO);
        final Limit limit = Limit.limit(go);

        assertTrue(limit.overflow());
        assertEquals(0.75, limit.max(), 1e-9);
    }
}
