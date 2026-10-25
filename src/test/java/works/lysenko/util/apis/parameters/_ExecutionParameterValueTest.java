package works.lysenko.util.apis.parameters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.ExecutionParameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("_ExecutionParameterValue Functional Interface Tests")
class _ExecutionParameterValueTest {

    @Test
    @DisplayName("Verify _ExecutionParameterValue getValue execution")
    void testGetValue() {
        _ExecutionParameterValue paramVal = param -> "val_" + param.name();
        assertEquals("val_" + ExecutionParameter.HEADLESS.name(), paramVal.getValue(ExecutionParameter.HEADLESS));
    }
}
