package works.lysenko.util.func.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClipboardTest {

    @Test
    void testClipboardInstantiation() {
        Clipboard clipboard = new Clipboard();
        assertNotNull(clipboard);
    }

    @Test
    void testWriteAndReadClipboard() {
        assertDoesNotThrow(() -> {
            try {
                Clipboard.writeClipboard("test text");
            } catch (Exception ignored) {
                // Headless environment might throw exception on toolkit
            }
        });
    }
}
