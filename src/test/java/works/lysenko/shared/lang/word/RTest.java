package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RTest {

    @Test
    void testInstantiation() {
        final R instance = new R();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(R.REPORT);
        assertFalse(R.REPORT.isEmpty());
        assertNotNull(R.REPORTS);
        assertFalse(R.REPORTS.isEmpty());
        assertNotNull(R.RULES);
        assertFalse(R.RULES.isEmpty());
    }
}
