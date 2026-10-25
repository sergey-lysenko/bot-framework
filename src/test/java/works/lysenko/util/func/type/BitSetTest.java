package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BitSetTest {
    @Test
    void testInstantiation() {
        works.lysenko.util.func.type.BitSet bs = works.lysenko.util.func.type.BitSet.random(5);
        assertNotNull(bs);
        assertEquals(5, bs.length());
    }
}
