package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NTest {

    @Test
    void testInstantiation() {
        N instance = new N();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(N.NAMED);
        assertFalse(N.NAMED.isEmpty());
        assertNotNull(N.NATIVE);
        assertFalse(N.NATIVE.isEmpty());
        assertNotNull(N.NEGATIVE);
        assertFalse(N.NEGATIVE.isEmpty());
        assertNotNull(N.NEITHER);
        assertFalse(N.NEITHER.isEmpty());
        assertNotNull(N.NESTED);
        assertFalse(N.NESTED.isEmpty());
        assertNotNull(N.NEUTRAL);
        assertFalse(N.NEUTRAL.isEmpty());
        assertNotNull(N.NONE);
        assertFalse(N.NONE.isEmpty());
        assertNotNull(N.NOTHING);
        assertFalse(N.NOTHING.isEmpty());
        assertNotNull(N.NOTICE);
        assertFalse(N.NOTICE.isEmpty());
        assertNotNull(N.NOTICES);
        assertFalse(N.NOTICES.isEmpty());
    }
}
