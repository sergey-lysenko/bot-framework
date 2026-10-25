package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QTest {

    @Test
    void testInstantiation() {
        Q instance = new Q();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(Q.QUOTA);
        assertFalse(Q.QUOTA.isEmpty());
        assertNotNull(Q.QUOTAS);
        assertFalse(Q.QUOTAS.isEmpty());
    }
}
