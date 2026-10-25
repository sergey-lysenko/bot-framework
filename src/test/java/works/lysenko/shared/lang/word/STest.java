package works.lysenko.shared.lang.word;

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
        assertNotNull(S.SIGN);
        assertFalse(S.SIGN.isEmpty());
        assertNotNull(S.SIGNING);
        assertFalse(S.SIGNING.isEmpty());
        assertNotNull(S.SETTINGS);
        assertFalse(S.SETTINGS.isEmpty());
        assertNotNull(S.SAVE);
        assertFalse(S.SAVE.isEmpty());
    }
}
