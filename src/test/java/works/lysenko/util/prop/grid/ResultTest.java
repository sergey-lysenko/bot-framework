package works.lysenko.util.prop.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.grid.Result.*;

class ResultTest {
    @Test
    void testDefaults() {
        assertNotNull(nullSeverity);
        assertNotNull(emptySeverity);
        assertNotNull(new Result());
    }
}

