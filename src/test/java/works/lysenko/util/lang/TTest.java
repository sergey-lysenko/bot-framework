package works.lysenko.util.lang;

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
        assertNotNull(T.TELEMETRY_WRITER_IS_NOT___);
        assertFalse(T.TELEMETRY_WRITER_IS_NOT___.isEmpty());
        assertNotNull(T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY);
        assertFalse(T.TESTS_HAD_BEEN_STOPPED_PREEMPTIVELY.isEmpty());
        assertNotNull(T.TEST_EXECUTION);
        assertFalse(T.TEST_EXECUTION.isEmpty());
        assertNotNull(T.TEST_PROPERTIES_ARE___);
        assertFalse(T.TEST_PROPERTIES_ARE___.isEmpty());
        assertNotNull(T.TEST_SESSION_COMPLETED);
        assertFalse(T.TEST_SESSION_COMPLETED.isEmpty());
        assertNotNull(T.TEXT_IN);
        assertFalse(T.TEXT_IN.isEmpty());
        assertNotNull(T.THERE_ARE);
        assertFalse(T.THERE_ARE.isEmpty());
        assertNotNull(T.THE_SORTED___);
        assertFalse(T.THE_SORTED___.isEmpty());
        assertNotNull(T.THIS_SHOULD_NOT_HAPPEN);
        assertFalse(T.THIS_SHOULD_NOT_HAPPEN.isEmpty());
        assertNotNull(T.THRESHOLD_DEFINED_IN);
        assertFalse(T.THRESHOLD_DEFINED_IN.isEmpty());
        assertNotNull(T.THUMBNAIL_FOR);
        assertFalse(T.THUMBNAIL_FOR.isEmpty());
        assertNotNull(T.TIMEOUT_EXCEPTION);
        assertFalse(T.TIMEOUT_EXCEPTION.isEmpty());
        assertNotNull(T.TODO_);
        assertFalse(T.TODO_.isEmpty());
        assertNotNull(T.TOO_FEW_EXPECTED);
        assertFalse(T.TOO_FEW_EXPECTED.isEmpty());
        assertNotNull(T.TO_BE);
        assertFalse(T.TO_BE.isEmpty());
        assertEquals(" (tried for 30 second(s) with 500 milliseconds interval)", T.TRIED_FOR_30_SECONDS___);
    }
}
