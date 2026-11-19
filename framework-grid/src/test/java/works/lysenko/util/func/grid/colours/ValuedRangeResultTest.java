package works.lysenko.util.func.grid.colours;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.RangeResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ValuedRangeResultTest {

    @Test
    void testValuedRangeResultRecord() {
        ValuedRangeResult result = new ValuedRangeResult(Fraction.ONE, "stamp");
        assertNotNull(result);
        assertEquals(Fraction.ONE, result.value());
        assertEquals("stamp", result.stamp());

        result.result(RangeResult.OK);
        assertEquals(RangeResult.OK, result.result());
    }
}
