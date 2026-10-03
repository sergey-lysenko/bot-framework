package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WrapTest {

    @Test
    void spacesAroundText() {

        assertEquals(" a ", Wrap.e("a"));
        assertEquals(" a", Wrap.e(true, "a", false));
        assertEquals("a ", Wrap.e("a", true));
        assertEquals(" a", Wrap.e(true, "a"));
    }

    @Test
    void fenceAroundText() {

        assertEquals("-x-", Wrap.e('-', "x"));
        assertEquals("-x-", Wrap.e('-', 'x'));
    }

    @Test
    void quotesPreferSingleUnlessTextHasOne() {

        assertEquals("'ab'", Wrap.q("ab"));
        assertEquals("\"it's\"", Wrap.q("it's"));
        assertEquals("'7'", Wrap.q(7));
    }

    @Test
    void conditionalQuotes() {

        assertEquals("'a'", Wrap.q(true, "a"));
        assertEquals("a", Wrap.q(false, "a"));
    }
}
