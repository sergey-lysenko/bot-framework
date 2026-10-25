package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelativeOrAbsoluteTest {

    @Test
    void testValues() {
        assertEquals(2, RelativeOrAbsolute.values().length);
    }
}
