package works.lysenko.util.data.range;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractQuotasTest {

    private static class TestQuotas extends AbstractQuotas {

        public TestQuotas(final List<_Quota<?>> quotas) {
            super(quotas);
        }

        public TestQuotas(final String source, final String origin, final String type, final boolean silent) {
            super(source, origin, type, silent);
        }

        @Override
        public Fraction logExpected(final int width, final Renderers renderers) {
            return Fraction.ZERO;
        }

        @Override
        public boolean isWithinPrecision(final Object value, final _Quota quota) {
            return true;
        }

        @Override
        public Slack slack() {
            return null;
        }
    }

    @Test
    void testConstructorWithList() {
        final List<_Quota<?>> list = new ArrayList<>();
        final Quota<Integer> q = Quota.shareOfInteger(1, Fraction.ONE_HALF);
        list.add(q);

        final TestQuotas quotas = new TestQuotas(list);
        assertEquals(1, quotas.get().size());
        assertEquals(q, quotas.getByKey(1));
        assertNull(quotas.getByKey(99));
    }

    @Test
    void testSetQuotasAndNullSource() {
        final TestQuotas nullQuotas = new TestQuotas((String) null, "origin", "type", true);
        assertTrue(nullQuotas.get().isEmpty());

        final List<_Quota<?>> list = new ArrayList<>();
        nullQuotas.setQuotas(list);
        assertEquals(list, nullQuotas.get());
    }
}
