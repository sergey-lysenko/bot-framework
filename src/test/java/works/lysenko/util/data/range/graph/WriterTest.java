package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WriterTest {

    private final Options go = new Options(10, Fraction.ONE_HALF, Fraction.ONE_HALF, new Slack(RangedMargin.rm(Fraction.ZERO)), Fraction.ZERO);
    private final Renderers renderers = Renderers.r(null, (data, quota, binner) -> ".", quota -> "row");

    @Test
    void testGraphActualInteger() {
        final Map<Integer, _ValuedRangeResult> results = new HashMap<>();
        results.put(1, new _ValuedRangeResult() {
            @Override
            public Fraction value() {
                return Fraction.ONE_HALF;
            }

            @Override
            public String stamp() {
                return "st";
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

        final Fraction result = Writer.graphActualInteger("Title", results, go, renderers);
        assertNotNull(result);
    }

    @Test
    void testGraphActualFraction() {
        final Map<Fraction, _ValuedRangeResult> results = new HashMap<>();
        results.put(Fraction.ONE_HALF, new _ValuedRangeResult() {
            @Override
            public Fraction value() {
                return Fraction.ONE_HALF;
            }

            @Override
            public String stamp() {
                return "st";
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

        final Fraction result = Writer.graphActualFraction("Title", results, go, renderers, 10);
        assertNotNull(result);
    }

    @Test
    void testGraphExpectedInteger() {
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
                return new Slack(RangedMargin.rm(Fraction.ZERO));
            }
        };

        final Fraction result = Writer.graphExpectedInteger("Title", new IntegerRange(1, 10), quotas, go, renderers);
        assertNotNull(result);
    }

    @Test
    void testGraphExpectedFraction() {
        final AbstractQuotas quotas = new AbstractQuotas(List.of(
                new Quota<>(Fraction.ONE_HALF, null, null, Fraction.ZERO, Fraction.ONE_HALF)
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
                return new Slack(RangedMargin.rm(Fraction.ZERO));
            }
        };

        final Fraction result = Writer.graphExpectedFraction("Title", new IntegerRange(1, 10), quotas, go, renderers, 10);
        assertNotNull(result);
    }
}
