package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataTest {

    private final Options go = new Options(10, Fraction.ONE_HALF, Fraction.ONE_HALF, new Slack(Fraction.ZERO), Fraction.ZERO);

    @Test
    void testDataFromSharesMap() {
        final Map<Fraction, _ValuedRangeResult> map = new HashMap<>();
        final Data d = Data.data(map, go, 10);

        assertNotNull(d);
        assertNotNull(d.gp());
        assertNotNull(d.value());
        assertNotNull(d.max());
    }

    @Test
    void testDataFromQuotas() {
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

        final Data d = Data.data(new IntegerRange(1, 10), quotas, go, 10);
        assertNotNull(d);
        assertNotNull(d.gp());
    }
}
