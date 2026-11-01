package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class STest {

    @Test
    void testInstantiation() {
        S instance = new S();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(S.SCENARIOS_STACK);
        assertFalse(S.SCENARIOS_STACK.isEmpty());
        assertNotNull(S.SCENARIO_EXECUTION_FAILED_DUE_TO);
        assertFalse(S.SCENARIO_EXECUTION_FAILED_DUE_TO.isEmpty());
        assertNotNull(S.SHRINKING_WAS_DISABLED_BY_CONFIGURATION);
        assertFalse(S.SHRINKING_WAS_DISABLED_BY_CONFIGURATION.isEmpty());
        assertNotNull(S.SHRINKING_WAS_DISABLED_BY_REQUEST);
        assertFalse(S.SHRINKING_WAS_DISABLED_BY_REQUEST.isEmpty());
        assertNotNull(S.SNAPSHOT_OF_PAGE_CODE);
        assertFalse(S.SNAPSHOT_OF_PAGE_CODE.isEmpty());
        assertNotNull(S.S_E_R_EXCEPTION);
        assertFalse(S.S_E_R_EXCEPTION.isEmpty());
        assertEquals("Android SDK root folder does not exist: check ANDROID_HOME / ANDROID_SDK_ROOT", S.SDK_ROOT_CHECK);
        assertEquals("Android SDK folder does not exist: check ANDROID_HOME / ANDROID_SDK_ROOT", S.SDK_FOLDER_CHECK);
        assertEquals(S.SDK_ROOT_CHECK, S.MSG_SDK_ROOT_CHECK);
        assertEquals(S.SDK_FOLDER_CHECK, S.MSG_SDK_FOLDER_CHECK);
    }
}
