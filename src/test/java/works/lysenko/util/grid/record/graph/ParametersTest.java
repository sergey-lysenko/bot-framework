package works.lysenko.util.grid.record.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParametersTest {

    @Test
    void testParametersInstantiation() {
        Parameters params = new Parameters(10.5, "amount", null);
        assertEquals(10.5, params.minimalMaximumValue());
        assertEquals("amount", params.amountS());
        assertNull(params.slack());
    }
}
