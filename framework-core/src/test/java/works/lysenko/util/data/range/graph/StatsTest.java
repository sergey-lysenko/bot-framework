package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.Quota;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StatsTest {

    @Test
    void testStatsWithQuotasAndResults() {

        final AbstractQuotas quotas = new AbstractQuotas(List.of(
                new Quota<>(1, null, null, Fraction.ZERO, Fraction.ONE)
        )) {

            @Override
            public Fraction logExpected(final int width, final works.lysenko.util.func.grid.Renderers renderers) {
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

        assertDoesNotThrow(() -> Stats.stats(quotas));

        final Map<String, _ValuedRangeResult> results = new HashMap<>();
        results.put("key", new _ValuedRangeResult() {

            @Override
            public Fraction value() {
                return Fraction.ONE_HALF;
            }

            @Override
            public String stamp() {
                return null;
            }

            @Override
            public double doubleValue() {
                return 0.5;
            }

            @Override
            public RangeResult result() {
                return RangeResult.OK;
            }

            @Override
            public void result(RangeResult result) {}
        });

        assertDoesNotThrow(() -> Stats.stats(results));
    }
}
