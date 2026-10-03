package works.lysenko.base.output;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Writes animated WebP by muxing ImageIO-encoded WebP frames into a RIFF container.
 */
final class AnimatedWebP {

    private static final int MAX_DIMENSION = (1 << 14) - 1;
    private static final int MAX_CHUNK_SIZE = 0xFFFFFF;

    private AnimatedWebP() {
    }

    static void write(
            final List<BufferedImage> images,
            final File outputFile,
            final int frameDelayMillis,
            final int finalFrameDelayMillis) throws IOException {
        if (images.isEmpty()) throw new IllegalArgumentException("At least one WebP frame is required");

        final int width = images.get(0).getWidth();
        final int height = images.get(0).getHeight();
        validateDimensions(width, height);

        final ByteArrayOutputStream chunks = new ByteArrayOutputStream();
        final byte[] vp8x = new byte[10];
        vp8x[0] = 0x02;
        write24(vp8x, 4, width - 1);
        write24(vp8x, 7, height - 1);
        writeChunk(chunks, "VP8X", vp8x);

        final byte[] animation = new byte[6];
        writeChunk(chunks, "ANIM", animation);

        for (int i = 0; i < images.size(); i++) {
            final BufferedImage image = images.get(i);
            if (image.getWidth() != width || image.getHeight() != height) {
                throw new IllegalArgumentException("All WebP frames must have identical dimensions");
            }
            final int delay = (i == images.size() - 1) ? finalFrameDelayMillis : frameDelayMillis;
            if (delay < 0 || delay > MAX_CHUNK_SIZE) {
                throw new IllegalArgumentException("WebP frame delay must be between 0 and 16777215 ms");
            }
            writeFrame(chunks, image, width, height, delay);
        }

        final byte[] webpChunks = chunks.toByteArray();
        final ByteArrayOutputStream webp = new ByteArrayOutputStream(webpChunks.length + 12);
        webp.write("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        write32(webp, webpChunks.length + 4L);
        webp.write("WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        webp.write(webpChunks);
        Files.write(outputFile.toPath(), webp.toByteArray());
    }

    private static void validateDimensions(final int width, final int height) {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException("WebP dimensions must be between 1 and 16383 pixels");
        }
    }

    private static void writeFrame(
            final ByteArrayOutputStream output,
            final BufferedImage image,
            final int width,
            final int height,
            final int duration) throws IOException {
        final ByteArrayOutputStream frame = new ByteArrayOutputStream();
        final byte[] frameHeader = new byte[16];
        write24(frameHeader, 6, width - 1);
        write24(frameHeader, 9, height - 1);
        write24(frameHeader, 12, duration);
        frameHeader[15] = 0x02;
        frame.write(frameHeader);
        appendImageChunks(frame, encodeFrame(image));
        writeChunk(output, "ANMF", frame.toByteArray());
    }

    private static byte[] encodeFrame(final BufferedImage image) throws IOException {
        final ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "webp", encoded)) {
            throw new IOException("No WebP ImageIO writer is available");
        }
        final byte[] data = encoded.toByteArray();
        if (data.length < 20 || !matches(data, 0, "RIFF") || !matches(data, 8, "WEBP")) {
            throw new IOException("WebP ImageIO writer produced an invalid RIFF image");
        }
        return data;
    }

    private static void appendImageChunks(final ByteArrayOutputStream frame, final byte[] image) throws IOException {
        int offset = 12;
        boolean foundImageData = false;
        while (offset + 8 <= image.length) {
            final String type = new String(image, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            final long size = read32(image, offset + 4);
            final long end = offset + 8L + size + (size & 1L);
            if (end > image.length) throw new IOException("Invalid WebP image chunk length");
            if ("ALPH".equals(type) || "VP8 ".equals(type) || "VP8L".equals(type)) {
                frame.write(image, offset, (int) (end - offset));
                foundImageData |= !"ALPH".equals(type);
            }
            offset = (int) end;
        }
        if (!foundImageData) throw new IOException("WebP image contains no VP8/VP8L frame data");
    }

    private static void writeChunk(
            final ByteArrayOutputStream output,
            final String type,
            final byte[] data) throws IOException {
        output.write(type.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        write32(output, data.length);
        output.write(data);
        if ((data.length & 1) != 0) output.write(0);
    }

    private static void write24(final byte[] output, final int offset, final int value) {
        output[offset] = (byte) value;
        output[offset + 1] = (byte) (value >>> 8);
        output[offset + 2] = (byte) (value >>> 16);
    }

    private static void write32(final ByteArrayOutputStream output, final long value) {
        output.write((int) value);
        output.write((int) (value >>> 8));
        output.write((int) (value >>> 16));
        output.write((int) (value >>> 24));
    }

    private static long read32(final byte[] input, final int offset) {
        return (input[offset] & 0xFFL)
                | ((input[offset + 1] & 0xFFL) << 8)
                | ((input[offset + 2] & 0xFFL) << 16)
                | ((input[offset + 3] & 0xFFL) << 24);
    }

    private static boolean matches(final byte[] input, final int offset, final String value) {
        for (int i = 0; i < value.length(); i++) {
            if (input[offset + i] != value.charAt(i)) return false;
        }
        return true;
    }
}
