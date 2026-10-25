package works.lysenko.util.apis.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_Default Functional Interface Tests")
class _DefaultTest {

    @Test
    @DisplayName("Verify _Default def execution")
    void testDef() {
        _Default supplier = () -> "default_val";
        assertEquals("default_val", supplier.def());
    }
}
