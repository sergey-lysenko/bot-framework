package works.lysenko.util.grid.record.rash;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.range.Quota;

import static org.junit.jupiter.api.Assertions.*;

class BinnerTest {

    @Test
    void testBinnerInstantiation() {
        Binner binner = new Binner(10L, 1L, 12L, 0L);
        assertEquals(10L, binner.max());
        assertEquals(1L, binner.min());
        assertEquals(12L, binner.maxS());
        assertEquals(0L, binner.minS());
    }

    @Test
    void testGetRanges() {
        SharesData<String> data = new SharesData<>("test", 0, 100.0, 200);
        Quota<Fraction> quota = new Quota<>(Fraction.ONE, null, null, Fraction.ZERO, Fraction.ONE);
        Binner binner = Binner.getRanges(data, quota, null);
        assertNotNull(binner);
    }
}
