package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SymbolsTest {

    @Test
    void testSymbolsConstants() {
        assertEquals('\n', Symbols._LFD_);
        assertEquals(' ', Symbols._SPACE_);
        assertEquals('!', Symbols._EXCL_);
        assertEquals('0', Symbols._0_);
        assertEquals('9', Symbols._9_);
        assertNotNull(Symbols.A);
        assertNotNull(Symbols.Z);
        assertEquals('•', Symbols._BULLT_);
        assertEquals('●', Symbols.FAT_BUL);
        assertEquals(Symbols.FAT_BUL, Symbols.DATA_POINT);
        assertNotNull(new Symbols());
    }
}

