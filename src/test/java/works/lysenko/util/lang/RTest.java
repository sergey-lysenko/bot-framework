package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RTest {

    @Test
    void testInstantiation() {
        R instance = new R();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(R.READING_TEXT);
        assertFalse(R.READING_TEXT.isEmpty());
        assertNotNull(R.READING_VALUE);
        assertFalse(R.READING_VALUE.isEmpty());
        assertNotNull(R.REDEFINED_EXPECTED);
        assertFalse(R.REDEFINED_EXPECTED.isEmpty());
        assertNotNull(R.REDEFINED_EXPECTED_MINIMUM_IS);
        assertFalse(R.REDEFINED_EXPECTED_MINIMUM_IS.isEmpty());
        assertNotNull(R.RANGES_IN);
        assertFalse(R.RANGES_IN.isEmpty());
        assertNotNull(R.RANGER_RESULT);
        assertFalse(R.RANGER_RESULT.isEmpty());
    }
}
