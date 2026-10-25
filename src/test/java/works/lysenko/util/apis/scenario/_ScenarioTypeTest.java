package works.lysenko.util.apis.scenario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_ScenarioType Functional Interface Tests")
class _ScenarioTypeTest {

    @Test
    @DisplayName("Verify _ScenarioType tag execution")
    void testTag() {
        _ScenarioType type = () -> "LEAF";
        assertEquals("LEAF", type.tag());
    }
}
