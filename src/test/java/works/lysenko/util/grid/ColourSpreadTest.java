package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ColourSpreadTest {

    @Test
    void colourSpreadCreationAndMethods() {
        ColourSpread cs = ColourSpread.cs("black", 10);
        assertNotNull(cs);
        assertEquals("black", cs.name());
        assertNotNull(cs.toString());

        ColourSpread cs0 = ColourSpread.cs("white");
        assertEquals("white", cs0.name());

        Collection<ColourSpread> collection = ColourSpread.cs(cs, cs0);
        assertEquals(2, collection.size());
    }

    @Test
    void fitsCheck() {
        ColourSpread cs = ColourSpread.cs("red", 50);
        assertNotNull(cs.toString());
    }
}
