package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DTest {

    @Test
    void testInstantiation() {
        D instance = new D();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(D.DIDN_T_GET_OFF_THE_GROUND);
        assertFalse(D.DIDN_T_GET_OFF_THE_GROUND.isEmpty());
        assertNotNull(D.DIFFERENCE_IS);
        assertFalse(D.DIFFERENCE_IS.isEmpty());
        assertNotNull(D.DIFFERENT_AMOUNT_OF);
        assertFalse(D.DIFFERENT_AMOUNT_OF.isEmpty());
        assertNotNull(D.DOES_NOT_FIT);
        assertFalse(D.DOES_NOT_FIT.isEmpty());
        assertNotNull(D.DOT_CLASS);
        assertFalse(D.DOT_CLASS.isEmpty());
        assertNotNull(D.DUE_TO);
        assertFalse(D.DUE_TO.isEmpty());
        assertNotNull(D.DUPLICATED___);
        assertFalse(D.DUPLICATED___.isEmpty());
    }
}
