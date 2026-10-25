package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Parameters;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FromSharesTest {

    @Test
    void testGetGraphParametersNullMaxThrows() {
        final FromShares fromShares = new FromShares(null, null, null);
        assertThrows(IllegalArgumentException.class, () -> fromShares.getGraphParameters(new Slack(Fraction.ZERO)));
    }

    @Test
    void testGetGraphParametersSuccess() {
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
                return null;
            }
        };

        final FromShares fs1 = new FromShares(new IntegerRange(1, 10), quotas, Fraction.ONE);
        final Parameters p1 = fs1.getGraphParameters(new Slack(Fraction.ZERO));
        assertNotNull(p1);

        final FromShares fs2 = new FromShares(null, quotas, Fraction.ONE);
        final Parameters p2 = fs2.getGraphParameters(new Slack(Fraction.ZERO));
        assertNotNull(p2);
    }
}
