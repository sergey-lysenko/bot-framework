package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VTest {

    @Test
    void testInstantiation() {
        V instance = new V();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(V.VALID);
        assertFalse(V.VALID.isEmpty());
        assertNotNull(V.VALIDATE);
        assertFalse(V.VALIDATE.isEmpty());
        assertNotNull(V.VALIDATING);
        assertFalse(V.VALIDATING.isEmpty());
        assertNotNull(V.VALIDATION);
        assertFalse(V.VALIDATION.isEmpty());
        assertNotNull(V.VALIDATOR);
        assertFalse(V.VALIDATOR.isEmpty());
        assertNotNull(V.VALUES);
        assertFalse(V.VALUES.isEmpty());
        assertNotNull(V.VERIFICATION);
        assertFalse(V.VERIFICATION.isEmpty());
        assertNotNull(V.VERIFYING);
        assertFalse(V.VERIFYING.isEmpty());
        assertNotNull(V.VERSION);
        assertFalse(V.VERSION.isEmpty());
        assertNotNull(V.VERTICAL);
        assertFalse(V.VERTICAL.isEmpty());
        assertNotNull(V.VERTICES);
        assertFalse(V.VERTICES.isEmpty());
        assertNotNull(V.VISIBILITY);
        assertFalse(V.VISIBILITY.isEmpty());
        assertNotNull(V.VERIFIED);
        assertFalse(V.VERIFIED.isEmpty());
        assertNotNull(V.VERIFY);
        assertFalse(V.VERIFY.isEmpty());
    }
}
