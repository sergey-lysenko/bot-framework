package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Brackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrapTest {

    @Test
    void spacesAroundText() {

        assertEquals(" a ", Wrap.e("a"));
        assertEquals(" a", Wrap.e(true, "a", false));
        assertEquals("a ", Wrap.e("a", true));
        assertEquals(" a", Wrap.e(true, "a"));
        assertEquals("a", Wrap.e(false, "a", false));
    }

    @Test
    void fenceAroundText() {

        assertEquals("-x-", Wrap.e('-', "x"));
        assertEquals("-x-", Wrap.e('-', 'x'));
    }

    @Test
    void bracketsAroundValues() {

        assertEquals("(abc)", Wrap.e(Brackets.ROUND, "abc"));
        assertEquals("[123]", Wrap.e(Brackets.SQUARE, 123));
        assertEquals("{456}", Wrap.e(Brackets.CURLY, 456L));
        assertEquals("{7.5}", Wrap.e(Brackets.CURLY, 7.5));
        assertEquals("(z)", Wrap.e(Brackets.ROUND, 'z'));
    }

    @Test
    void quotesPreferSingleUnlessTextHasOne() {

        assertEquals("'ab'", Wrap.q("ab"));
        assertEquals("\"it's\"", Wrap.q("it's"));
        assertEquals("'7'", Wrap.q(7));
        assertEquals("'hello'", Wrap.q((Object) "hello"));
        assertEquals("\"don't\"", Wrap.q((Object) "don't"));
    }

    @Test
    void conditionalQuotes() {

        assertEquals("'a'", Wrap.q(true, "a"));
        assertEquals("a", Wrap.q(false, "a"));
        assertEquals("\"it's\"", Wrap.q(true, "it's"));
        assertEquals("it's", Wrap.q(false, "it's"));
    }
}

