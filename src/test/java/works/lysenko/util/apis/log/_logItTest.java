package works.lysenko.util.apis.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("_logIt Functional Interface Tests")
class _logItTest {

    @Test
    @DisplayName("Verify _logIt lambda execution")
    void testLogIt() {
        AtomicBoolean called = new AtomicBoolean(false);
        _logIt logger = () -> called.set(true);
        logger.logIt();
        assertTrue(called.get());
    }
}
