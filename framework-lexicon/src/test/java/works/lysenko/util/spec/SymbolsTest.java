package works.lysenko.util.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SymbolsTest {

    @Test
    void decodesBitwiseSymbolCharactersAcrossBitWidths() {
        assertEquals('\n', Symbols._LFD_);
        assertEquals(' ', Symbols._SPACE_);
        assertEquals('!', Symbols._EXCL_);
        assertEquals('0', Symbols._0_);
        assertEquals('9', Symbols._9_);
        assertEquals('a', Symbols.A);
        assertEquals('z', Symbols.Z);
        assertEquals('®', Symbols.CPY_RHT);
        assertEquals('•', Symbols._BULLT_);
        assertEquals('●', Symbols.FAT_BUL);
    }
}

