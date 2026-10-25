package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TextTest {

    @Test
    void testTextFromMap() {
        final Map<Fraction, _ValuedRangeResult> shares = new HashMap<>();
        final Limit li = new Limit(false, 0.5);

        final Text t = Text.text(shares, 10, li);
        assertNotNull(t);
        assertNotNull(t.value());
        assertNotNull(t.max());
    }

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

        final Limit li = new Limit(true, 1.2);
        final Text t = Text.text(quotas, 10, li);
        assertNotNull(t);
    }
}
