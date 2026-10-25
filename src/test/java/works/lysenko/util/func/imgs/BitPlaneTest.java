package works.lysenko.util.func.imgs;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.records.Size;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BitPlaneTest {

    @Test
    void testBitPlaneInstantiation() {
        BitPlane bitPlane = new BitPlane(new Size(10, 10));
        assertNotNull(bitPlane);
    }
}
