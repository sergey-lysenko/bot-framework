package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangedMarginTypeTest {

    @Test
    void testValues() {
        assertEquals(2, RangedMarginType.values().length);
    }
}
