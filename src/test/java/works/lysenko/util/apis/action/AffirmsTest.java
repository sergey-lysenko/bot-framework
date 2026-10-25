package works.lysenko.util.apis.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Affirms Functional Interface Tests")
class AffirmsTest {

    @Test
    @DisplayName("Verify Affirms lambda execution")
    void testAffirm() {
        AtomicBoolean called = new AtomicBoolean(false);
        Affirms affirms = () -> called.set(true);
        affirms.affirm();
        assertTrue(called.get());
    }
}
