package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.func.grid.Renderers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValuesTest {

    @Test
    void testGetMinimalMaximumValue() {
        assertEquals(0.015, Values.getMinimalMaximumValue(0.001), 1e-9);
        assertEquals(0.5, Values.getMinimalMaximumValue(0.5), 1e-9);
    }

    @Test
    void testGraphExpectedSortedRanges() {
        final AbstractQuotas quotas = new AbstractQuotas(List.of(
                new Quota<>(1, null, null, Fraction.ZERO, Fraction.ONE_HALF)
        )) {
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
                return null;
            }
        };

        final Renderers renderers = Renderers.r((req, q) -> false, (data, quota, binner) -> ".", quota -> "row");
        final Fraction edge = Values.graphExpectedSortedRanges(quotas, 10, renderers, 1.0, new Slack(Fraction.ZERO));
        assertNotNull(edge);
    }
}
