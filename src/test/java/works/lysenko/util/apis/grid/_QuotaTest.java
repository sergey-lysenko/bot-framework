package works.lysenko.util.apis.grid;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.t._Value;

import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("_Quota Interface Default Methods Tests")
class _QuotaTest {

    private static class DummyQuota implements _Quota<String> {
        @Override
        public Fraction actualAbsoluteMargin(Fraction actualValue) {
            return Fraction.ZERO;
        }

        @Override
        public double median() {
            return 0.5;
        }

        @Override
        public String percentage() {
            return "50%";
        }

        @Override
        public String render() {
            return "50%";
        }

        @Override
        public _Value<String> share() {
            return null;
        }

        @Override
        public String value() {
            return "val";
        }

        @Override
        public String precision() {
            return "prec";
        }

        @Override
        public String stamp() {
            return "stamp";
        }

        @Override
        public double width() {
            return 1.0;
        }
    }

    @Test
    @DisplayName("Verify default max and min methods return null")
    void testDefaults() {
        DummyQuota quota = new DummyQuota();
        assertNull(quota.max());
        assertNull(quota.min());
    }
}
