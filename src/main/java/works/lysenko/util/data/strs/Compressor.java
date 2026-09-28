package works.lysenko.util.data.strs;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static java.util.Objects.isNull;
import static works.lysenko.util.lang.I.INVALID_COMPRESSED_DATA;

/**
 * A utility class providing methods to compress and decompress strings.
 * The compression is performed using the Deflater algorithm with the best
 * compression level, and the resulting compressed data is encoded in Base64
 * without padding for safe transmission or storage. The decompression method
 * reverses this process to return the original string.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints", "CheckForOutOfMemoryOnLargeArrayAllocation"})
public record Compressor() {

    private static final int BUFFER_LENGTH = 8192;

    /**
     * Compresses a given string using the Deflater algorithm with the best compression level
     * and encodes the resulting compressed data into a Base64 string without padding.
     *
     * @param input the string to be compressed; if null or empty, the original input is returned
     * @return the compressed and Base64-encoded string; or the original input if it is null or empty
     */
    public static String compress(final String input) {
        if (isNull(input) || input.isEmpty()) return input;

        final byte[] data = input.getBytes(StandardCharsets.UTF_8);
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        final ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        final byte[] buffer = new byte[BUFFER_LENGTH];
        while (!deflater.finished()) {
            final int count = deflater.deflate(buffer);
            os.write(buffer, 0, count);
        }
        deflater.end();

        return Base64.getUrlEncoder().withoutPadding().encodeToString(os.toByteArray());
    }

    /**
     * Decompresses a given Base64-encoded and compressed string using the Inflater algorithm.
     *
     * @param compressed the string to be decompressed, encoded in Base64; if null or empty, the original input is returned
     * @return the decompressed string in UTF-8 encoding, or the original input if it is null or empty
     * @throws IllegalArgumentException if the provided input is not valid compressed data
     */
    @SuppressWarnings("unused")
    public static String decompress(final String compressed) {
        if (isNull(compressed)|| compressed.isEmpty()) return compressed;

        final byte[] data = Base64.getUrlDecoder().decode(compressed);
        final Inflater inflater = new Inflater();
        inflater.setInput(data);

        final ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        final byte[] buffer = new byte[BUFFER_LENGTH];
        try {
            while (!inflater.finished()) {
                final int count = inflater.inflate(buffer);
                os.write(buffer, 0, count);
            }
        } catch (final DataFormatException e) {
            throw new IllegalArgumentException(INVALID_COMPRESSED_DATA, e);
        } finally {
            inflater.end();
        }

        return os.toString(StandardCharsets.UTF_8);
    }
}
