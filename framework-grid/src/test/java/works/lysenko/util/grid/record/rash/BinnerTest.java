package works.lysenko.util.grid.record.rash;

import org.apache.commons.math3.fraction.Fraction;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.range.Quota;

import static org.junit.jupiter.api.Assertions.*;

class BinnerTest {

    @Test
    void testGetRanges() {
        SharesData<String> data = new SharesData<>("test", 0, 100.0, 200);
        Quota<Fraction> quota = new Quota<>(Fraction.ONE, null, null, Fraction.ZERO, Fraction.ONE);
        Binner binner = Binner.getRanges(data, quota, null);
        assertNotNull(binner);
    }
}
