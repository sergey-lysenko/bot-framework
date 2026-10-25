package works.lysenko.util.data.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExecutionParameterTest {

    @Test
    void testByNameAndDef() {
        assertEquals(ExecutionParameter.SUITE, ExecutionParameter.byName("TEST"));
        assertEquals(ExecutionParameter.ALL_LEAFS, ExecutionParameter.byName("all_leafs"));
        assertNull(ExecutionParameter.byName(null));
        assertNull(ExecutionParameter.byName("NON_EXISTENT"));

        assertEquals("false", ExecutionParameter.ALL_LEAFS.def());
    }
}
