package works.lysenko.util.func.logs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OutOfBoundsTest {

    @Test
    void testOutOfBoundsInstantiation() {
        OutOfBounds outOfBounds = new OutOfBounds();
        assertNotNull(outOfBounds);
    }
}
