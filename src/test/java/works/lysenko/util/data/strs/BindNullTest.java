package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BindNullTest {

    @Test
    void bindsWithSpace() {

        assertEquals("a b", Bind.b("a", "b"));
        assertEquals("a", Bind.b("a"));
    }

    @Test
    void bindsWithCustomBinderAndMixedTypes() {

        assertEquals("1-2-x", Bind.b('-', 1, 2, "x"));
    }

    @Test
    void emptyElementsAreOptional() {

        assertEquals("a  b", Bind.b(true, "a", "", "b"));
        assertEquals("a b", Bind.b(false, "a", "", "b"));
    }

    @Test
    void dashBinder() {

        assertEquals("a-b", Bind.d("a", "b"));
    }

    @Test
    void nullsAreRenderedByBn() {

        assertEquals("a null", Bind.bn("a", null));
    }

    @Test
    void nullReplacement() {

        assertEquals("x", Null.n("x", null));
        assertEquals("y", Null.n("x", "y"));
    }

    @Test
    void nullAwareStringification() {

        assertEquals("null", Null.sn((Object) null));
        assertEquals("5", Null.sn(5));
        assertEquals("d", Null.sn("d", null));
        assertEquals("v", Null.sn("d", "v"));
    }
}
