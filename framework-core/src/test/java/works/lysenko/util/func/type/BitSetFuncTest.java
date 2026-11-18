package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BitSetFuncTest {

    @Test
    void testBitSetInstantiation() {
        final BitSet bitSet = BitSet.random(10);
        assertNotNull(bitSet);
        assertEquals(10, bitSet.length());
        assertNotNull(bitSet.switches());
    }
}
