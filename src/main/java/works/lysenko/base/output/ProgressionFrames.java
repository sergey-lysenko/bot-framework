package works.lysenko.base.output;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProgressionFrames {

    private static final Pattern FRAME_NAME = Pattern.compile("frame_(\\d+)\\.png");
    static final int MAX_DIMENSION = (1 << 14) - 2;
    private static final int MAX_FRAMES = 10_000;
    private static final int MAX_FRAME_PIXELS = 33_554_432;
    private static final int MAX_PENDING_FRAMES = 4;
    private static final Semaphore WRITER_SEMAPHORE = new Semaphore(MAX_PENDING_FRAMES);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "progression-frame-writer");
        t.setDaemon(true);
        return t;
    });

    private ProgressionFrames() {
    }

    static void saveFrameAsync(final BufferedImage image, final File frameFile, final Runnable postSave) {
        try {
            WRITER_SEMAPHORE.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        EXECUTOR.submit(() -> {
            try {
                ImageIO.write(image, "png", frameFile);
                if (null != postSave) {
                    try {
                        postSave.run();
                    } catch (final Exception e) {
                        System.err.println("Failed post-save hook: " + e.getMessage());
                    }
                }
            } catch (final IOException e) {
                System.err.println("Failed to save progression frame: " + e.getMessage());
            } finally {
                WRITER_SEMAPHORE.release();
            }
        });
    }

    static void awaitCompletion() {
        try {
            WRITER_SEMAPHORE.acquire(MAX_PENDING_FRAMES);
            WRITER_SEMAPHORE.release(MAX_PENDING_FRAMES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long parseFrameNumber(final File file) {
        final Matcher matcher = FRAME_NAME.matcher(file.getName());
        if (matcher.matches()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (final NumberFormatException ignored) {
            }
        }
        return Long.MAX_VALUE;
    }

    static List<File> select(final File directory, final int configuredMaxFrames) throws IOException {
        return select(directory, configuredMaxFrames, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    static List<File> select(
            final File directory,
            final int configuredMaxFrames,
            final int configuredMaxFramePixels,
            final int configuredMaxTotalPixels) throws IOException {
        if (!directory.isDirectory()) return List.of();
        final int maxFrames = maxFrames(configuredMaxFrames);

        final List<File> allFrames = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory.toPath(), "*.png")) {
            for (final Path path : paths) {
                final Matcher matcher = FRAME_NAME.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    allFrames.add(path.toFile());
                }
            }
        }
        if (allFrames.isEmpty()) return List.of();

        allFrames.sort(Comparator.comparingLong(ProgressionFrames::parseFrameNumber));

        int maxWidth = 0;
        int maxHeight = 0;
        for (final File frame : allFrames) {
            final int[] dimensions = dimensions(frame, configuredMaxFramePixels);
            maxWidth = Math.max(maxWidth, dimensions[0]);
            maxHeight = Math.max(maxHeight, dimensions[1]);
        }

        final int frameLimit = frameLimit(
                maxFrames,
                configuredMaxTotalPixels,
                maxWidth,
                maxHeight);

        if (allFrames.size() <= frameLimit) return allFrames;
        if (frameLimit == 1) return List.of(allFrames.get(allFrames.size() - 1));

        final List<File> sampled = new ArrayList<>(frameLimit);
        for (int i = 0; i < frameLimit; i++) {
            final int selectedIndex = (int) ((long) i * (allFrames.size() - 1) / (frameLimit - 1));
            sampled.add(allFrames.get(selectedIndex));
        }
        return sampled;
    }

    static int maxFrames(final int configuredMaxFrames) {
        return Math.max(1, Math.min(MAX_FRAMES, configuredMaxFrames));
    }

    static int maxFramePixels(final int configuredMaxPixels) {
        return Math.max(1, Math.min(MAX_FRAME_PIXELS, configuredMaxPixels));
    }

    static int frameLimit(
            final int configuredMaxFrames,
            final int configuredMaxTotalPixels,
            final int width,
            final int height) {
        final long totalPixelLimit = Math.max(1L, Math.min(2_000_000_000L, configuredMaxTotalPixels));
        final long pixelsPerFrame = Math.max(1L, (long) width * height);
        final int totalPixelFrames = (int) Math.max(1L, totalPixelLimit / pixelsPerFrame);
        return Math.min(maxFrames(configuredMaxFrames), totalPixelFrames);
    }

    static BufferedImage read(final File file, final int maxFramePixels) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (null == input) throw new IOException("Unable to open progression frame: " + file);
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("No image reader for progression frame: " + file);
            final ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                validateDimensions(file, reader.getWidth(0), reader.getHeight(0), maxFramePixels);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static int[] dimensions(final File file, final int maxFramePixels) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (null == input) throw new IOException("Unable to open progression frame: " + file);
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("No image reader for progression frame: " + file);
            final ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                final int width = reader.getWidth(0);
                final int height = reader.getHeight(0);
                validateDimensions(file, width, height, maxFramePixels);
                return new int[]{width, height};
            } finally {
                reader.dispose();
            }
        }
    }

    private static void validateDimensions(
            final File file,
            final int width,
            final int height,
            final int configuredMaxPixels) throws IOException {
        if (width <= 0 || height <= 0
                || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > maxFramePixels(configuredMaxPixels)) {
            throw new IOException("Progression frame exceeds configured dimensions/pixel limit: "
                    + file.getName() + " (" + width + "x" + height + ")");
        }
    }
}
