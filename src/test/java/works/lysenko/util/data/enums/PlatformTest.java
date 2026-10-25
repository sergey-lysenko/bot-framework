package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformTest {

    @Test
    void testGetAndGetString() {
        assertEquals(Platform.FIREFOX, Platform.get("Firefox"));
        assertEquals(Platform.EDGE, Platform.get("Edge"));
        assertEquals(Platform.SAFARI, Platform.get("Safari"));
        assertEquals(Platform.ANDROID, Platform.get("Android"));
        assertEquals(Platform.CHROME, Platform.get("Other"));

        assertEquals("Chrome", Platform.CHROME.getString());
    }
}
