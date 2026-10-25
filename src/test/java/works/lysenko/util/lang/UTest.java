package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UTest {

    @Test
    void testInstantiation() {
        U instance = new U();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(U.UI_AUTOMATOR_2);
        assertFalse(U.UI_AUTOMATOR_2.isEmpty());
        assertNotNull(U.UI_VERIFICATION_FAILED_IN);
        assertFalse(U.UI_VERIFICATION_FAILED_IN.isEmpty());
        assertNotNull(U.UNABLE_TO);
        assertFalse(U.UNABLE_TO.isEmpty());
        assertNotNull(U.UNABLE_TO_ADD_OK___);
        assertFalse(U.UNABLE_TO_ADD_OK___.isEmpty());
        assertNotNull(U.UNABLE_TO_DEFINE___);
        assertFalse(U.UNABLE_TO_DEFINE___.isEmpty());
        assertNotNull(U.UNABLE_TO_GET_EXTERNAL___);
        assertFalse(U.UNABLE_TO_GET_EXTERNAL___.isEmpty());
        assertNotNull(U.UNABLE_TO_LOAD);
        assertFalse(U.UNABLE_TO_LOAD.isEmpty());
        assertNotNull(U.UNABLE_TO_OPEN);
        assertFalse(U.UNABLE_TO_OPEN.isEmpty());
        assertNotNull(U.UNABLE_TO_PARCE_RANGEDMARGIN___);
        assertFalse(U.UNABLE_TO_PARCE_RANGEDMARGIN___.isEmpty());
        assertNotNull(U.UNABLE_TO_PROCEED);
        assertFalse(U.UNABLE_TO_PROCEED.isEmpty());
        assertNotNull(U.UNABLE_TO_PROCESS_EMPTY___);
        assertFalse(U.UNABLE_TO_PROCESS_EMPTY___.isEmpty());
        assertNotNull(U.UNABLE_TO_PROCESS_NULL___);
        assertFalse(U.UNABLE_TO_PROCESS_NULL___.isEmpty());
        assertNotNull(U.UNABLE_TO_READ_CLIPBOARD);
        assertFalse(U.UNABLE_TO_READ_CLIPBOARD.isEmpty());
        assertNotNull(U.UNABLE_TO_READ_LINES_FROM);
        assertFalse(U.UNABLE_TO_READ_LINES_FROM.isEmpty());
        assertNotNull(U.UNABLE_TO_READ_SCREENSHOT___);
        assertFalse(U.UNABLE_TO_READ_SCREENSHOT___.isEmpty());
        assertNotNull(U.UNABLE_TO_READ___);
        assertFalse(U.UNABLE_TO_READ___.isEmpty());
        assertNotNull(U.UNABLE_TO_SELECT_A_RANDOM_ELEMENT_OF_AN_EMPTY_SET);
        assertFalse(U.UNABLE_TO_SELECT_A_RANDOM_ELEMENT_OF_AN_EMPTY_SET.isEmpty());
        assertNotNull(U.UNABLE_TO_SELECT_A_SCENARIO___);
        assertFalse(U.UNABLE_TO_SELECT_A_SCENARIO___.isEmpty());
        assertNotNull(U.UNABLE_TO_START_TEST___);
        assertFalse(U.UNABLE_TO_START_TEST___.isEmpty());
        assertNotNull(U.UNABLE_TO_TRANSFORM___);
        assertFalse(U.UNABLE_TO_TRANSFORM___.isEmpty());
        assertNotNull(U.UNABLE_TO_VALIDATE_INCORRECTLY_CONFIGURED);
        assertFalse(U.UNABLE_TO_VALIDATE_INCORRECTLY_CONFIGURED.isEmpty());
        assertNotNull(U.UNABLE_TO_WRITE);
        assertFalse(U.UNABLE_TO_WRITE.isEmpty());
        assertNotNull(U.UNABLE_TO_WRITE_CODE___);
        assertFalse(U.UNABLE_TO_WRITE_CODE___.isEmpty());
        assertNotNull(U.UNABLE_TO_WRITE_DATA_TO);
        assertFalse(U.UNABLE_TO_WRITE_DATA_TO.isEmpty());
        assertNotNull(U.UNABLE_TO_WRITE_INTO_FILE);
        assertFalse(U.UNABLE_TO_WRITE_INTO_FILE.isEmpty());
        assertNotNull(U.UNABLE_TO_WRITE_SCREENSHOT);
        assertFalse(U.UNABLE_TO_WRITE_SCREENSHOT.isEmpty());
        assertNotNull(U.UNCAUGHT_EXCEPTION);
        assertFalse(U.UNCAUGHT_EXCEPTION.isEmpty());
        assertNotNull(U.UNEXPECTED_NULL_VALUE_AMONG_);
        assertFalse(U.UNEXPECTED_NULL_VALUE_AMONG_.isEmpty());
        assertNotNull(U.UNRESOLVED___);
        assertFalse(U.UNRESOLVED___.isEmpty());
        assertNotNull(U.USER_DIR);
        assertFalse(U.USER_DIR.isEmpty());
        assertNotNull(U.U_B_EXCEPTION);
        assertFalse(U.U_B_EXCEPTION.isEmpty());
    }
}
