package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TTest {

    @Test
    void testInstantiation() {
        T instance = new T();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(T.TABLE);
        assertFalse(T.TABLE.isEmpty());
        assertNotNull(T.TARGET);
        assertFalse(T.TARGET.isEmpty());
        assertNotNull(T.TARGETS);
        assertFalse(T.TARGETS.isEmpty());
        assertNotNull(T.TELEMETRY);
        assertFalse(T.TELEMETRY.isEmpty());
        assertNotNull(T.TERMINATING);
        assertFalse(T.TERMINATING.isEmpty());
        assertNotNull(T.TESTING);
        assertFalse(T.TESTING.isEmpty());
        assertNotNull(T.TESTS);
        assertFalse(T.TESTS.isEmpty());
        assertNotNull(T.THESE);
        assertFalse(T.THESE.isEmpty());
        assertNotNull(T.THREAD);
        assertFalse(T.THREAD.isEmpty());
        assertNotNull(T.THRESHOLD);
        assertFalse(T.THRESHOLD.isEmpty());
        assertNotNull(T.THROUGH);
        assertFalse(T.THROUGH.isEmpty());
        assertNotNull(T.THUMBNAIL);
        assertFalse(T.THUMBNAIL.isEmpty());
        assertNotNull(T.TIMELINE);
        assertFalse(T.TIMELINE.isEmpty());
        assertNotNull(T.TIMEOUT);
        assertFalse(T.TIMEOUT.isEmpty());
        assertNotNull(T.TIMES);
        assertFalse(T.TIMES.isEmpty());
        assertNotNull(T.TITLE);
        assertFalse(T.TITLE.isEmpty());
        assertNotNull(T.TOOK);
        assertFalse(T.TOOK.isEmpty());
        assertNotNull(T.TOOLBAR);
        assertFalse(T.TOOLBAR.isEmpty());
        assertNotNull(T.TOUCHING);
        assertFalse(T.TOUCHING.isEmpty());
        assertNotNull(T.TRACE);
        assertFalse(T.TRACE.isEmpty());
        assertNotNull(T.TRANSFORM);
        assertFalse(T.TRANSFORM.isEmpty());
        assertNotNull(T.TREE);
        assertFalse(T.TREE.isEmpty());
        assertNotNull(T.TRYING);
        assertFalse(T.TRYING.isEmpty());
        assertNotNull(T.TYPES);
        assertFalse(T.TYPES.isEmpty());
        assertNotNull(T.TYPING);
        assertFalse(T.TYPING.isEmpty());
    }
}
