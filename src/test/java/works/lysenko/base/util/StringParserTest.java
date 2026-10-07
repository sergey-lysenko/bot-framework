package works.lysenko.base.util;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringParserTest {

    @Test
    void parsesEnumValuesByTheirConstantName() {

        assertEquals(Severity.S0, StringParser.create("S0", Severity.class).result());
        assertEquals(Severity.S0, StringParser.create("s0", Severity.class).result());
    }
}
