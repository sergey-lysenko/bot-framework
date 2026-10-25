package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LineFeedTest {

    @Test
    void testLineFeed() {
        final LineFeed lf = new LineFeed(100L);
        assertEquals(100L, lf.id());
        assertEquals(0, lf.depth());
        assertEquals("", lf.render());
        assertNotNull(lf.toString());
    }
}
