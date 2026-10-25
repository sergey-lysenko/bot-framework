package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToStringTest {

    @Test
    void constructorAcceptsGrid() {
        ToString toString = new ToString(null);
        assertNotNull(toString);
    }
}
