package works.lysenko.base.util;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StringParserTest {

    @Test
    void parsesEnumValuesByTheirConstantName() {
        assertEquals(Severity.S0, StringParser.create("S0", Severity.class).result());
        assertEquals(Severity.S0, StringParser.create("s0", Severity.class).result());
    }

    @Test
    void parsesSeverityByCodeOrString() {
        assertEquals(Severity.S0, StringParser.create("0", Severity.class).result());
        assertEquals(Severity.S1, StringParser.create("1", Severity.class).result());
        assertEquals(Severity.S2, StringParser.create("2", Severity.class).result());
        assertEquals(Severity.S3, StringParser.create("3", Severity.class).result());
    }

    @Test
    void parsesIntegerWithSeverityPrefixTolerantly() {
        assertEquals(0, StringParser.create("s0", Integer.class).result());
        assertEquals(1, StringParser.create("s1", Integer.class).result());
        assertEquals(99, StringParser.create("99", Integer.class).result());
        assertNull(StringParser.create("invalid", Integer.class).result());
    }
}
