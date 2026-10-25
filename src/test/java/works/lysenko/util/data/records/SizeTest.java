package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeTest {

    @Test
    void testRecordFields() {
        final Size size = new Size(1920, 1080);
        assertEquals(1920, size.width());
        assertEquals(1080, size.height());
    }
}
