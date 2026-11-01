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
        assertEquals("Patience at '%s%%' (%d of %s)", P.PATIENCE_AT_S_D_OF_S);
        assertEquals("Processing exceptional state", P.PROCESSING_EXCEPTIONAL_STATE);
        assertEquals("Provided locator", P.PROVIDED_LOCATOR);
        assertEquals("platform-tools", P.DIR_PLATFORM_TOOLS);
        assertEquals("platforms", P.DIR_PLATFORMS);
        assertEquals("Port conflict: port 4723 is already in use (EADDRINUSE)", P.PORT_CONFLICT_4723);
        assertEquals("Port conflict (EADDRINUSE): another Appium instance or process is using this port", P.PORT_CONFLICT_GENERIC);
        assertEquals("/Library/Android/sdk", P.PATH_MACOS_SDK);
        assertEquals("/Android/Sdk", P.PATH_ANDROID_SDK);
        assertEquals("/sdk", P.PATH_SDK);
        assertEquals("/opt/android-sdk", P.PATH_OPT_SDK);
        assertEquals("/usr/local/share/android-sdk", P.PATH_USR_LOCAL_SDK);
        assertEquals(P.PORT_CONFLICT_4723, P.MSG_PORT_CONFLICT_4723);
        assertEquals(P.PORT_CONFLICT_GENERIC, P.MSG_PORT_CONFLICT_GENERIC);
    }
}
