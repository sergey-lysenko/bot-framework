package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ITest {

    @Test
    void testInstantiation() {
        I instance = new I();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(I.INITIAL_SETBACK_MESSAGE);
        assertFalse(I.INITIAL_SETBACK_MESSAGE.isEmpty());
        assertNotNull(I.INVALID_COMPRESSED_DATA);
        assertFalse(I.INVALID_COMPRESSED_DATA.isEmpty());
        assertNotNull(I.IS_ALREADY_ON_EDGE_OF);
        assertFalse(I.IS_ALREADY_ON_EDGE_OF.isEmpty());
        assertNotNull(I.IS_BIGGER_THAN_ALLOWED);
        assertFalse(I.IS_BIGGER_THAN_ALLOWED.isEmpty());
        assertNotNull(I.IS_DEFINED_MORE_THAN___);
        assertFalse(I.IS_DEFINED_MORE_THAN___.isEmpty());
        assertNotNull(I.IS_NOT_AN_INTEGER_IN___);
        assertFalse(I.IS_NOT_AN_INTEGER_IN___.isEmpty());
        assertNotNull(I.IS_TOO_CLOSE_TO___);
        assertFalse(I.IS_TOO_CLOSE_TO___.isEmpty());
        assertNotNull(I.IS_TOO_MUCH_TIME___);
        assertFalse(I.IS_TOO_MUCH_TIME___.isEmpty());
        assertNotNull(I.IS_TO_BE_EXECUTED);
        assertFalse(I.IS_TO_BE_EXECUTED.isEmpty());
        assertNotNull(I.IS_WIDER_THAN);
        assertFalse(I.IS_WIDER_THAN.isEmpty());
    }
}
