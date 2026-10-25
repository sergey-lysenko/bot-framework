package works.lysenko.util.apis.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_String Functional Interface Tests")
class _StringTest {

    @Test
    @DisplayName("Verify _String lambda execution")
    void testGetString() {
        _String supplier = () -> "test";
        assertEquals("test", supplier.getString());
    }
}
