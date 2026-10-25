package works.lysenko.util.apis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("_isOk Functional Interface Tests")
class _isOkTest {

    @Test
    @DisplayName("Verify _isOk lambda execution")
    void testIsOk() {
        _isOk checker = () -> true;
        assertTrue(checker.isOk());
    }
}
