package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PixelsTest {

    @Test
    void testPixelsInstantiation() {
        Pixels pixels = new Pixels();
        assertNotNull(pixels);
    }
}
