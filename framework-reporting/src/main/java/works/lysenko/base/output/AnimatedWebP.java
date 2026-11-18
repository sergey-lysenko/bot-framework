package works.lysenko.base.output;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

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
        write(images, outputFile, frameDelayMillis, finalFrameDelayMillis, percentage -> { });
    }

    static void write(
            final List<BufferedImage> images,
            final File outputFile,
            final int frameDelayMillis,
            final int finalFrameDelayMillis,
            final IntConsumer progress) throws IOException {
        writeImages(images, outputFile, frameDelayMillis, finalFrameDelayMillis, Integer.MAX_VALUE, progress);
    }

    static void write(
            final List<File> frames,
            final File outputFile,
            final int frameDelayMillis,
            final int finalFrameDelayMillis,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        if (frames.isEmpty()) throw new IllegalArgumentException("At least one WebP frame is required");
        writeAnimation(
                frames.size(),
                index -> ProgressionFrames.read(frames.get(index), maxFramePixels),
                outputFile,
                frameDelayMillis,
                finalFrameDelayMillis,
                maxFramePixels,
                progress);
    }

    private static void writeImages(
            final List<BufferedImage> images,
            final File outputFile,
            final int frameDelayMillis,
            final int finalFrameDelayMillis,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        if (images.isEmpty()) throw new IllegalArgumentException("At least one WebP frame is required");
        writeAnimation(
                images.size(),
                images::get,
                outputFile,
                frameDelayMillis,
                finalFrameDelayMillis,
                maxFramePixels,
                progress);
    }

    private static void writeAnimation(
            final int frameCount,
            final FrameLoader frames,
            final File outputFile,
            final int frameDelayMillis,
            final int finalFrameDelayMillis,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        final BufferedImage first = frames.load(0);
        final int width = first.getWidth();
        final int height = first.getHeight();
        validateDimensions(width, height, maxFramePixels);

        final Path parent = (null == outputFile.getAbsoluteFile().getParentFile())
                ? new File(".").toPath() : outputFile.getAbsoluteFile().getParentFile().toPath();
        Files.createDirectories(parent);
        final Path chunks = Files.createTempFile(parent, "animated-webp-", ".chunks");
        try {
            try (OutputStream chunkOutput = new BufferedOutputStream(Files.newOutputStream(chunks))) {
                final byte[] vp8x = new byte[10];
                vp8x[0] = 0x02;
                writeChunk(chunkOutput, "VP8X", vp8x, width, height);
                writeChunk(chunkOutput, "ANIM", new byte[6]);

                for (int i = 0; i < frameCount; i++) {
                    final BufferedImage image = (i == 0) ? first : frames.load(i);
                    if (image.getWidth() != width || image.getHeight() != height) {
                        throw new IllegalArgumentException("All WebP frames must have identical dimensions");
                    }
                    validateDimensions(width, height, maxFramePixels);
                    final int delay = (i == frameCount - 1) ? finalFrameDelayMillis : frameDelayMillis;
                    if (delay < 0 || delay > MAX_CHUNK_SIZE) {
                        throw new IllegalArgumentException("WebP frame delay must be between 0 and 16777215 ms");
                    }
                    writeFrame(chunkOutput, image, width, height, delay);
                    progress.accept((int) (((i + 1L) * 100) / frameCount));
                }
            }

            final long chunkSize = Files.size(chunks);
            if (chunkSize > 0xFFFFFFFFL - 4L) {
                throw new IOException("Animated WebP exceeds the maximum RIFF file size");
            }
            try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))) {
                output.write("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                write32(output, chunkSize + 4L);
                output.write("WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                Files.copy(chunks, output);
            }
        } finally {
            Files.deleteIfExists(chunks);
        }
    }

    private static void validateDimensions(final int width, final int height, final int maxFramePixels) {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException("WebP dimensions must be between 1 and 16383 pixels");
        }
        if ((long) width * height > ProgressionFrames.maxFramePixels(maxFramePixels)) {
            throw new IllegalArgumentException("WebP frame exceeds configured pixel limit: "
                    + width + "x" + height);
        }
    }

    private static void writeFrame(
            final OutputStream output,
            final BufferedImage image,
            final int width,
            final int height,
            final int duration) throws IOException {
        final byte[] encoded = encodeFrame(image);
        final List<ChunkRange> imageChunks = imageDataChunks(encoded);
        long frameSize = 16;
        for (final ChunkRange chunk : imageChunks) frameSize += chunk.length();
        if (frameSize > MAX_CHUNK_SIZE) throw new IOException("Encoded WebP frame exceeds maximum chunk size");

        output.write("ANMF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        write32(output, frameSize);
        final byte[] frameHeader = new byte[16];
        write24(frameHeader, 6, width - 1);
        write24(frameHeader, 9, height - 1);
        write24(frameHeader, 12, duration);
        frameHeader[15] = 0x02;
        output.write(frameHeader);
        for (final ChunkRange chunk : imageChunks) output.write(encoded, chunk.offset(), chunk.length());
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

    private static List<ChunkRange> imageDataChunks(final byte[] image) throws IOException {
        int offset = 12;
        boolean foundImageData = false;
        final List<ChunkRange> imageChunks = new ArrayList<>(2);
        while (offset + 8 <= image.length) {
            final String type = new String(image, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            final long size = read32(image, offset + 4);
            final long end = offset + 8L + size + (size & 1L);
            if (end > image.length) throw new IOException("Invalid WebP image chunk length");
            if ("ALPH".equals(type) || "VP8 ".equals(type) || "VP8L".equals(type)) {
                imageChunks.add(new ChunkRange(offset, (int) (end - offset)));
                foundImageData |= !"ALPH".equals(type);
            }
            offset = (int) end;
        }
        if (!foundImageData) throw new IOException("WebP image contains no VP8/VP8L frame data");
        return imageChunks;
    }

    private static void writeChunk(
            final OutputStream output,
            final String type,
            final byte[] data,
            final int width,
            final int height) throws IOException {
        final byte[] vp8x = data.clone();
        write24(vp8x, 4, width - 1);
        write24(vp8x, 7, height - 1);
        writeChunk(output, type, vp8x);
    }

    private static void writeChunk(
            final OutputStream output,
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

    private static void write32(final OutputStream output, final long value) throws IOException {
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

    @FunctionalInterface
    private interface FrameLoader {
        BufferedImage load(int index) throws IOException;
    }

    private record ChunkRange(int offset, int length) { }
}
