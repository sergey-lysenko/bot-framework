package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TTest {

    @Test
    void testInstantiation() {
        final T instance = new T();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(T.TIMESTAMP);
        assertFalse(T.TIMESTAMP.isEmpty());
        assertNotNull(T.TIMESTAMPS);
        assertFalse(T.TIMESTAMPS.isEmpty());
    }
}
