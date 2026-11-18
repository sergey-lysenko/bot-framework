package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketsTest {

    @Test
    void testCharacters() {
        assertEquals('(', Brackets.ROUND.opening());
        assertEquals(')', Brackets.ROUND.closing());

        assertEquals('[', Brackets.SQUARE.opening());
        assertEquals(']', Brackets.SQUARE.closing());

        assertEquals('{', Brackets.CURLY.opening());
        assertEquals('}', Brackets.CURLY.closing());
    }
}
