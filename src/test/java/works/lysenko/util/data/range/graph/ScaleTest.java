package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.grid.record.graph.Options;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ScaleTest {

    @Test
    void testScale() {
        final Options go = new Options(20, new Fraction(1, 2), new Fraction(1, 2), new Slack(new Fraction(1, 10)), Fraction.ZERO);
        assertDoesNotThrow(() -> Scale.scale(go, new Fraction(1, 2)));
    }
}
