package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistributionTypeTest {

    @Test
    void testEnumValues() {
        assertEquals(3, DistributionType.values().length);
    }
}
