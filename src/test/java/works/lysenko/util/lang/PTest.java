package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PTest {

    @Test
    void testInstantiation() {
        P instance = new P();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(P.PROCESSING_EXCEPTIONAL_STATE);
        assertFalse(P.PROCESSING_EXCEPTIONAL_STATE.isEmpty());
        assertNotNull(P.PROVIDED_LOCATOR);
        assertFalse(P.PROVIDED_LOCATOR.isEmpty());
    }
}
