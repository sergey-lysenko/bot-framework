package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ITest {

    @Test
    void testInstantiation() {
        I instance = new I();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(I.IGNORE);
        assertFalse(I.IGNORE.isEmpty());
        assertNotNull(I.IGNORED);
        assertFalse(I.IGNORED.isEmpty());
        assertNotNull(I.IMAGE);
        assertFalse(I.IMAGE.isEmpty());
        assertNotNull(I.IMMINENT);
        assertFalse(I.IMMINENT.isEmpty());
        assertNotNull(I.IMPLEMENTED);
        assertFalse(I.IMPLEMENTED.isEmpty());
        assertNotNull(I.INACCESSIBLE);
        assertFalse(I.INACCESSIBLE.isEmpty());
        assertNotNull(I.INCLUDE);
        assertFalse(I.INCLUDE.isEmpty());
        assertNotNull(I.INCORRECT);
        assertFalse(I.INCORRECT.isEmpty());
        assertNotNull(I.INCORRECTLY);
        assertFalse(I.INCORRECTLY.isEmpty());
        assertNotNull(I.INDEX);
        assertFalse(I.INDEX.isEmpty());
        assertNotNull(I.INDUCED);
        assertFalse(I.INDUCED.isEmpty());
        assertNotNull(I.INITIAL);
        assertFalse(I.INITIAL.isEmpty());
        assertNotNull(I.INITIALISED);
        assertFalse(I.INITIALISED.isEmpty());
        assertNotNull(I.INITIALIZATION);
        assertFalse(I.INITIALIZATION.isEmpty());
        assertNotNull(I.INITIALIZING);
        assertFalse(I.INITIALIZING.isEmpty());
        assertNotNull(I.INNER);
        assertFalse(I.INNER.isEmpty());
        assertNotNull(I.INSTANCE);
        assertFalse(I.INSTANCE.isEmpty());
        assertNotNull(I.INSTANTIATE);
        assertFalse(I.INSTANTIATE.isEmpty());
        assertNotNull(I.INSTEAD);
        assertFalse(I.INSTEAD.isEmpty());
        assertNotNull(I.INTEGER);
        assertFalse(I.INTEGER.isEmpty());
        assertNotNull(I.INTERNAL);
        assertFalse(I.INTERNAL.isEmpty());
        assertNotNull(I.INTERRUPTED);
        assertFalse(I.INTERRUPTED.isEmpty());
        assertNotNull(I.INVALID);
        assertFalse(I.INVALID.isEmpty());
        assertNotNull(I.INVISIBILITY);
        assertFalse(I.INVISIBILITY.isEmpty());
        assertNotNull(I.ISSUE);
        assertFalse(I.ISSUE.isEmpty());
        assertNotNull(I.ISSUES);
        assertFalse(I.ISSUES.isEmpty());
    }
}
