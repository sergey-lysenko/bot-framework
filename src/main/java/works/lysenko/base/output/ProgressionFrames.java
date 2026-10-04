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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProgressionFrames {

    private static final Pattern FRAME_NAME = Pattern.compile("frame_(\\d+)\\.png");
    static final int MAX_DIMENSION = (1 << 14) - 2;
    private static final int MAX_FRAMES = 10_000;
    private static final int MAX_FRAME_PIXELS = 33_554_432;

    private ProgressionFrames() {
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
        long frameCount = 0;
        long minFrame = Long.MAX_VALUE;
        long maxFrame = Long.MIN_VALUE;
        File firstFrame = null;
        File lastFrame = null;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory.toPath(), "*.png")) {
            for (final Path path : paths) {
                final Matcher matcher = FRAME_NAME.matcher(path.getFileName().toString());
                if (!matcher.matches()) continue;
                final long frameNumber = Long.parseLong(matcher.group(1));
                frameCount++;
                if (frameNumber < minFrame) {
                    minFrame = frameNumber;
                    firstFrame = path.toFile();
                }
                if (frameNumber > maxFrame) {
                    maxFrame = frameNumber;
                    lastFrame = path.toFile();
                }
            }
        }
        if (frameCount == 0) return List.of();
        final List<File> selected = new ArrayList<>(Math.min(maxFrames, (int) Math.min(frameCount, Integer.MAX_VALUE)));
        if (frameCount > maxFrames && maxFrames == 1) {
            selected.add(lastFrame);
        } else if (frameCount > maxFrames) {
            selected.add(firstFrame);
        }
        final int interiorLimit = frameCount > maxFrames && maxFrames > 2 ? maxFrames - 2 : 0;
        final List<File> interiorSample = new ArrayList<>(Math.max(0, interiorLimit));
        final Random random = new Random(0x5EEDL);
        long interiorCount = 0;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory.toPath(), "*.png")) {
            for (final Path path : paths) {
                final Matcher matcher = FRAME_NAME.matcher(path.getFileName().toString());
                if (!matcher.matches()) continue;
                final File frame = path.toFile();
                if (frameCount <= maxFrames) {
                    selected.add(frame);
                } else if (!frame.equals(firstFrame) && !frame.equals(lastFrame) && interiorLimit > 0) {
                    interiorCount++;
                    if (interiorSample.size() < interiorLimit) {
                        interiorSample.add(frame);
                    } else {
                        final long replacement = random.nextLong(interiorCount);
                        if (replacement < interiorLimit) interiorSample.set((int) replacement, frame);
                    }
                }
            }
        }
        if (frameCount > maxFrames) {
            selected.addAll(interiorSample);
            if (maxFrames > 1) selected.add(lastFrame);
        }
        selected.sort(Comparator.comparing(File::getName));
        int maxWidth = 0;
        int maxHeight = 0;
        for (final File frame : selected) {
            final int[] dimensions = dimensions(frame, configuredMaxFramePixels);
            maxWidth = Math.max(maxWidth, dimensions[0]);
            maxHeight = Math.max(maxHeight, dimensions[1]);
        }
        final int frameLimit = frameLimit(
                maxFrames,
                configuredMaxTotalPixels,
                maxWidth,
                maxHeight);
        if (selected.size() <= frameLimit) return selected;
        if (frameLimit == 1) return List.of(selected.get(selected.size() - 1));
        final List<File> sampled = new ArrayList<>(frameLimit);
        for (int i = 0; i < frameLimit; i++) {
            final int selectedIndex = (int) ((long) i * (selected.size() - 1) / (frameLimit - 1));
            sampled.add(selected.get(selectedIndex));
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
