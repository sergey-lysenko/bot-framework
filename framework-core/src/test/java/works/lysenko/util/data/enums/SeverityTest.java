package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SeverityTest {

    @Test
    void testByCodeByColorByTypeGreaterSeverity() {
        assertEquals(Severity.S0, Severity.byCode(0));
        assertEquals(Severity.S1, Severity.byColor(Ansi.RED));
        assertNull(Severity.byColor(Ansi.BLUE));
        assertEquals(Severity.S2, Severity.byType(EventType.WARNING));
        assertNull(Severity.byType(EventType.UNDEFINED));

        assertEquals(Severity.S0, Severity.greaterSeverity(Severity.S0, Severity.S2));
        assertEquals(Severity.S1, Severity.greaterSeverity(null, Severity.S1));
        assertEquals(Severity.S2, Severity.greaterSeverity(Severity.S2, null));
    }
}
