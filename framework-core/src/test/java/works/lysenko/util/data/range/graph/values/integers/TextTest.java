package works.lysenko.util.data.range.graph.values.integers;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.func.grid.Renderers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextTest {

    @Test
    void testTextFromQuotas() {
        final AbstractQuotas quotas = new AbstractQuotas(Collections.emptyList()) {
            @Override
            public Fraction logExpected(final int width, final Renderers renderers) {
                return Fraction.ZERO;
            }

            @Override
            public boolean isWithinPrecision(final Object value, final works.lysenko.util.apis.grid.q._Quota quota) {
                return true;
            }

            @Override
            public Slack slack() {
                return new Slack(Fraction.ZERO);
            }
        };

        final Text t = Text.text(quotas, true, 1.2);
        assertNotNull(t);
        assertNotNull(t.value());
        assertNotNull(t.max());
    }

    @Test
    void testTextFromResultsMap() {
        final Map<Integer, _ValuedRangeResult> results = new HashMap<>();
        final Text t = Text.text(results, false, 0.5);
        assertNotNull(t);
        assertNotNull(t.value());
        assertNotNull(t.max());
    }
}
