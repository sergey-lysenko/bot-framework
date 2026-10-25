package works.lysenko.util.apis.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_Result Interface Tests")
class _ResultTest {

    private static class DummyResult implements _Result<String> {
        @Override
        public String result() {
            return "res";
        }
    }

    @Test
    @DisplayName("Verify _Result result return value")
    void testResult() {
        _Result<String> res = new DummyResult();
        assertEquals("res", res.result());
    }
}
