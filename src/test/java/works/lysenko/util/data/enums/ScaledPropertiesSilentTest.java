package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScaledPropertiesSilentTest {

    @Test
    void testValues() {
        assertEquals(3, ScaledPropertiesSilent.values().length);
    }
}
