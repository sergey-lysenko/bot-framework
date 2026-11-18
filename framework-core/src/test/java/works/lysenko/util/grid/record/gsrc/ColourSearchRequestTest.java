package works.lysenko.util.grid.record.gsrc;

import org.junit.jupiter.api.Test;
import works.lysenko.util.grid.ColourSpread;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ColourSearchRequestTest {

    @Test
    void testCsrFactories() {
        ColourSearchRequest csr1 = ColourSearchRequest.csr(10, Collections.emptyList());
        assertNotNull(csr1);
        assertEquals(10, csr1.normal());
        assertTrue(csr1.spreads().isEmpty());

        ColourSearchRequest csr3 = ColourSearchRequest.csr(null, Collections.emptyList());
        assertNotNull(csr3);
        assertNull(csr3.normal());
        assertTrue(csr3.spreads().isEmpty());

        assertNull(ColourSearchRequest.csr(null));
        assertNull(ColourSearchRequest.csr(""));
    }

    @Test
    void testFitsAndUnknownColours() {
        ColourSearchRequest csr = ColourSearchRequest.csr(null, Collections.emptyList());
        assertTrue(csr.getUnknownColours().isEmpty());
    }
}
