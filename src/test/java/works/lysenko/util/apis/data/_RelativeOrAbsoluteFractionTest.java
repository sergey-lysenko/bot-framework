package works.lysenko.util.apis.data;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.t._Range;

import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("_RelativeOrAbsoluteFraction Interface Tests")
class _RelativeOrAbsoluteFractionTest {

    private static class DummyFraction implements _RelativeOrAbsoluteFraction {
        @Override
        public String data() {
            return "1/2";
        }

        @Override
        public Double doubleValue(_Range<Fraction> base, Fraction reference) {
            return 0.5;
        }

        @Override
        public Double doubleValue(Fraction base) {
            return 0.5;
        }

        @Override
        public String render(boolean full, boolean scripts) {
            return "0.5";
        }

        @Override
        public String render() {
            return "0.5";
        }
    }

    @Test
    @DisplayName("Verify default type method returns null")
    void testDefaultType() {
        DummyFraction df = new DummyFraction();
        assertNull(df.type());
    }
}
