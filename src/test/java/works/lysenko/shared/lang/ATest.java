package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ATest {

    @Test
    void testInstantiation() {
        final A instance = new A();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(A.ATTEMPTING_TO_SIGN_IN_WITH);
        assertFalse(A.ATTEMPTING_TO_SIGN_IN_WITH.isEmpty());
        assertNotNull(A.AND_WRONG_PASSWORD);
        assertFalse(A.AND_WRONG_PASSWORD.isEmpty());
        assertNotNull(A.AND_CORRECT_PASSWORD);
        assertFalse(A.AND_CORRECT_PASSWORD.isEmpty());
    }
}
