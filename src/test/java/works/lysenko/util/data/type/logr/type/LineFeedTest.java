package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineFeedTest {
    @Test
    void testLineFeed() {
        LineFeed lf = new LineFeed(1L);
        assertEquals("", lf.message());
    }
}
