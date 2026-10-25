package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitsTest {

    @Test
    void testWaitValuesAndRanges() {
        assertEquals(111, Waits.SHORT_WAIT_FROM);
        assertEquals(333, Waits.SHORT_WAIT_TO);
        assertEquals(333, Waits.MID_WAIT_FROM);
        assertEquals(555, Waits.MID_WAIT_TO);
        assertEquals(555, Waits.LONG_WAIT_FROM);
        assertEquals(777, Waits.LONG_WAIT_TO);

        assertTrue(Waits.SHORT_WAIT_FROM < Waits.SHORT_WAIT_TO);
        assertEquals(Waits.SHORT_WAIT_TO, Waits.MID_WAIT_FROM);
        assertTrue(Waits.MID_WAIT_FROM < Waits.MID_WAIT_TO);
        assertEquals(Waits.MID_WAIT_TO, Waits.LONG_WAIT_FROM);
        assertTrue(Waits.LONG_WAIT_FROM < Waits.LONG_WAIT_TO);
    }
}
