package works.lysenko.util.grid.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FactoryTest {

    @Test
    void testFactoryInstantiation() {
        Factory factory = new Factory();
        assertNotNull(factory);
    }
}
