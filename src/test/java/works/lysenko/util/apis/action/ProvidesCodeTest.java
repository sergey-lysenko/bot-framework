package works.lysenko.util.apis.action;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ProvidesCode Functional Interface Tests")
class ProvidesCodeTest {

    @Test
    @DisplayName("Verify ProvidesCode getCode execution")
    void testGetCode() {
        ProvidesCode provider = () -> 200;
        assertEquals(200, provider.getCode());
    }
}
