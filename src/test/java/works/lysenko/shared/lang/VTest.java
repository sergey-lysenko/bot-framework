package works.lysenko.shared.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VTest {

    @Test
    void testInstantiation() {
        final V instance = new V();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(V.VERIFYING_EXPECTED_MESSAGE);
        assertFalse(V.VERIFYING_EXPECTED_MESSAGE.isEmpty());
        assertNotNull(V.VERIFYING_SUCCESSFUL_LOGIN);
        assertFalse(V.VERIFYING_SUCCESSFUL_LOGIN.isEmpty());
        assertNotNull(V.VERIFYING_SUCCESSFUL_LOGOUT);
        assertFalse(V.VERIFYING_SUCCESSFUL_LOGOUT.isEmpty());
    }
}
