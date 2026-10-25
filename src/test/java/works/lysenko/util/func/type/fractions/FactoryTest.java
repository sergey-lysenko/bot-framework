package works.lysenko.util.func.type.fractions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FactoryTest {
    @Test
    void testInstantiation() {
        Factory f = new Factory();
        assertNotNull(f);
    }
}
