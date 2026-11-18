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

    @Test
    void parsesSuffixedIntegersAndLongs() {
        assertEquals(2_000_000_000L, StringParser.create("2000M", Long.class).result());
        assertEquals(2_000_000L, StringParser.create("2mpx", Long.class).result());
        assertEquals(2_000_000_000L, StringParser.create("2000mpx", Long.class).result());
        assertEquals(2_000_000_000L, StringParser.create("2G", Long.class).result());
        assertEquals(2_000_000_000L, StringParser.create("2gpx", Long.class).result());
        assertEquals(168_000_000, StringParser.create("168M", Integer.class).result());
        assertEquals(168_000_000, StringParser.create("168mpx", Integer.class).result());
        assertEquals(167_770_000, StringParser.create("167.77M", Integer.class).result());
        assertEquals(1_000_000, StringParser.create("1000K", Integer.class).result());
        assertEquals(2_000_000_000L, StringParser.create("2000000000", Long.class).result());
        assertEquals(167772160, StringParser.create("167772160", Integer.class).result());
    }
}
