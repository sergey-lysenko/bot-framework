package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompressorTest {

    @Test
    void testCompressAndDecompress() {
        final String original = "Hello World! Compression Test 1234567890";
        final String compressed = Compressor.compress(original);
        final String decompressed = Compressor.decompress(compressed);

        assertEquals(original, decompressed);
    }

    @Test
    void testNullAndEmptyHandling() {
        assertNull(Compressor.compress(null));
        assertEquals("", Compressor.compress(""));
        assertNull(Compressor.decompress(null));
        assertEquals("", Compressor.decompress(""));
    }
}
