package works.lysenko.shared.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LTest {

    @Test
    void testInstantiation() {
        final L instance = new L();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(L.LOGGING);
        assertFalse(L.LOGGING.isEmpty());
        assertNotNull(L.LOGIN);
        assertFalse(L.LOGIN.isEmpty());
        assertNotNull(L.LOGOUT);
        assertFalse(L.LOGOUT.isEmpty());
        assertNotNull(L.LOGGED);
        assertFalse(L.LOGGED.isEmpty());
        assertNotNull(L.LOGS);
        assertFalse(L.LOGS.isEmpty());
    }
}
