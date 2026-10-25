package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MTest {

    @Test
    void testInstantiation() {
        M instance = new M();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(M.MAIN_ACTIVITY);
        assertFalse(M.MAIN_ACTIVITY.isEmpty());
        assertNotNull(M.MAKING_SCREENSHOT);
        assertFalse(M.MAKING_SCREENSHOT.isEmpty());
        assertNotNull(M.MANUALLY_REQUESTED_TEST_STOP_PERFORMED);
        assertFalse(M.MANUALLY_REQUESTED_TEST_STOP_PERFORMED.isEmpty());
        assertNotNull(M.MAXIMUM_RETRIES_AMOUNT___);
        assertFalse(M.MAXIMUM_RETRIES_AMOUNT___.isEmpty());
        assertNotNull(M.MERGING_ACTUAL_VALUE);
        assertFalse(M.MERGING_ACTUAL_VALUE.isEmpty());
    }
}
