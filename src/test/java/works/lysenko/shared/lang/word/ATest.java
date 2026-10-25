package works.lysenko.shared.lang.word;

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
        assertNotNull(A.ADMIN);
        assertFalse(A.ADMIN.isEmpty());
        assertNotNull(A.AUDIT);
        assertFalse(A.AUDIT.isEmpty());
        assertNotNull(A.ADD);
        assertFalse(A.ADD.isEmpty());
        assertNotNull(A.APPLY);
        assertFalse(A.APPLY.isEmpty());
        assertNotNull(A.ATTEMPTS);
        assertFalse(A.ATTEMPTS.isEmpty());
        assertNotNull(A.ALERT);
        assertFalse(A.ALERT.isEmpty());
    }
}
