package works.lysenko.util.func.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PercentsTest {

    @Test
    void parsesPercentStrings() {

        assertEquals(50, Percents.intPercentsFromString("50%"));
        assertEquals(50, Percents.intPercentsFromString(" 5 0 % "));
        assertEquals(100, Percents.intPercentsFromString("100%"));
    }

    @Test
    void nonPercentStringsGiveNull() {

        assertNull(Percents.intPercentsFromString("50"));
        assertNull(Percents.intPercentsFromString("%"));
        assertNull(Percents.intPercentsFromString(""));
    }

    @Test
    void rendersIntegerPercents() {

        assertEquals("25%", Percents.percentString(1, 4));
        assertEquals("50%", Percents.percentString(1, 2));
        assertEquals("100%", Percents.percentString(7, 7));
        assertEquals("0%", Percents.percentString(0, 7));
    }
}
