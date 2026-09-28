package works.lysenko.util.data.strs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwapTest {

    @Test
    @DisplayName("Verify string concatenation via Swap.s")
    void testSwapConcat() {
        String result = Swap.s("Hello", " ", "World");
        assertNotNull(result);
        assertEquals("Hello World", result);
    }
}
