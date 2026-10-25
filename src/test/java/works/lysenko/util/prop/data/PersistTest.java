package works.lysenko.util.prop.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static works.lysenko.util.prop.data.Persist.*;

class PersistTest {
    @Test
    void testDefaults() {
        assertNotNull(data);
        assertNotNull(new Persist());
    }
}

