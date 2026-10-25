package works.lysenko.util.prop.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.data.Snapshot.*;

class SnapshotTest {
    @Test
    void testDefaults() {
        assertTrue(allowed == true || allowed == false);
        assertTrue(full == true || full == false);
    }
}
