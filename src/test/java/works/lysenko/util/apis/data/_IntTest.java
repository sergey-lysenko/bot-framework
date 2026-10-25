package works.lysenko.util.apis.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_Int Functional Interface Tests")
class _IntTest {

    @Test
    @DisplayName("Verify _Int lambda execution")
    void testGetInt() {
        _Int supplier = () -> 42;
        assertEquals(42, supplier.getInt());
    }
}
