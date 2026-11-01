package works.lysenko.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ATest {

    @Test
    void testInstantiation() {
        A instance = new A();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(A.ACTUAL_VALUES_ARE_NOT_VALID);
        assertFalse(A.ACTUAL_VALUES_ARE_NOT_VALID.isEmpty());
        assertNotNull(A.ADJACENT___);
        assertFalse(A.ADJACENT___.isEmpty());
        assertNotNull(A.ALLOWED___);
        assertFalse(A.ALLOWED___.isEmpty());
        assertEquals("All-leafs mode aborted: Wandering patience exceeded.", A.ALL_LEAFS_MODE_ABORTED_WANDERING_PATIENCE_EXCEEDED);
        assertNotNull(A.AMOUNT_OF);
        assertFalse(A.AMOUNT_OF.isEmpty());
        assertNotNull(A.ANDROID_LAUNCHER);
        assertFalse(A.ANDROID_LAUNCHER.isEmpty());
        assertNotNull(A.AN_ELEMENT___);
        assertFalse(A.AN_ELEMENT___.isEmpty());
        assertNotNull(A.APPIUM_AUTOMATION_NAME);
        assertFalse(A.APPIUM_AUTOMATION_NAME.isEmpty());
        assertNotNull(A.APPIUM_APP_PACKAGE);
        assertFalse(A.APPIUM_APP_PACKAGE.isEmpty());
        assertNotNull(A.APPIUM_APP_ACTIVITY);
        assertFalse(A.APPIUM_APP_ACTIVITY.isEmpty());
        assertNotNull(A.APPIUM_NO_RESET);
        assertFalse(A.APPIUM_NO_RESET.isEmpty());
        assertNotNull(A.APPIUM_UDID);
        assertFalse(A.APPIUM_UDID.isEmpty());
        assertNotNull(A.APPIUM_LOG);
        assertFalse(A.APPIUM_LOG.isEmpty());
        assertNotNull(A.ARE_TOO_SIMILAR);
        assertFalse(A.ARE_TOO_SIMILAR.isEmpty());
        assertNotNull(A.ASSERTING_THAT_ACTUAL);
        assertFalse(A.ASSERTING_THAT_ACTUAL.isEmpty());
        assertNotNull(A.ASSERTING_THAT_VALUE);
        assertFalse(A.ASSERTING_THAT_VALUE.isEmpty());
        assertNotNull(A.ATTEMPTING_COUNTER_ACTION);
        assertFalse(A.ATTEMPTING_COUNTER_ACTION.isEmpty());
        assertEquals("Appium server closed the connection unexpectedly (see ", A.APPIUM_SERVER_CLOSED);
        assertEquals(A.APPIUM_SERVER_CLOSED, A.MSG_APPIUM_CLOSED);
    }
}
