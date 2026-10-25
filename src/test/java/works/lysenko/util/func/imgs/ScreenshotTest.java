package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenshotTest {
    @Test
    void testInstantiation() {
        Screenshot s = new Screenshot();
        assertNotNull(s);
    }
}
