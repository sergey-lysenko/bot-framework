package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class STest {

    @Test
    void testInstantiation() {
        final S instance = new S();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(S.SIGN_IN);
        assertFalse(S.SIGN_IN.isEmpty());
    }
}
