package works.lysenko.base.output;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProgressionFrames {

    private static final Pattern FRAME_NAME = Pattern.compile("frame_(\\d+)\\.(svg|png)");
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

    static void saveFrameSvgAsync(final String svgContent, final File frameFile, final Runnable postSave) {
        try {
            WRITER_SEMAPHORE.acquire();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        EXECUTOR.submit(() -> {
            try {
                Files.writeString(frameFile.toPath(), svgContent, StandardCharsets.UTF_8);
                if (null != postSave) {
                    try {
                        postSave.run();
                    } catch (final Exception e) {
                        System.err.println("Failed post-save hook: " + e.getMessage());
                    }
                }
            } catch (final IOException e) {
                System.err.println("Failed to save progression SVG frame: " + e.getMessage());
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

        final Map<Long, File> frameMap = new HashMap<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory.toPath())) {
            for (final Path path : paths) {
                final File file = path.toFile();
                final Matcher matcher = FRAME_NAME.matcher(file.getName());
                if (matcher.matches()) {
                    final long num = Long.parseLong(matcher.group(1));
                    if (!frameMap.containsKey(num) || file.getName().endsWith(".svg")) {
                        frameMap.put(num, file);
                    }
                }
            }
        }
        if (frameMap.isEmpty()) return List.of();

        final List<File> allFrames = new ArrayList<>(frameMap.values());
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
        if (file.getName().endsWith(".svg")) {
            return renderSvg(file, maxFramePixels);
        }
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

    static BufferedImage renderSvg(final File file, final int maxFramePixels) throws IOException {
        try {
            final SVGLoader loader = new SVGLoader();
            final SVGDocument doc = loader.load(file.toURI().toURL());
            if (null == doc) throw new IOException("Unable to parse SVG file: " + file);
            final FloatSize size = doc.size();
            final int width = (int) Math.ceil(size.width);
            final int height = (int) Math.ceil(size.height);
            validateDimensions(file, width, height, maxFramePixels);
            final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0x0F, 0x17, 0x2A));
            g.fillRect(0, 0, width, height);
            doc.render(null, g);
            g.dispose();
            return img;
        } catch (final LinkageError e) {
            throw new IOException("JSVG library is missing from classpath. Please reload Maven dependencies in your IDE: " + e.getMessage(), e);
        }
    }

    private static int[] dimensions(final File file, final int maxFramePixels) throws IOException {
        if (file.getName().endsWith(".svg")) {
            try {
                final SVGLoader loader = new SVGLoader();
                final SVGDocument doc = loader.load(file.toURI().toURL());
                if (null == doc) throw new IOException("Unable to parse SVG file: " + file);
                final FloatSize size = doc.size();
                final int width = (int) Math.ceil(size.width);
                final int height = (int) Math.ceil(size.height);
                validateDimensions(file, width, height, maxFramePixels);
                return new int[]{width, height};
            } catch (final LinkageError e) {
                throw new IOException("JSVG library is missing from classpath. Please reload Maven dependencies in your IDE: " + e.getMessage(), e);
            }
        }
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

    static List<File> rasterize(
            final List<File> frames,
            final File tmpDir,
            final String taskName,
            final ProcessingProgress progress,
            final int maxFramePixels) throws IOException {
        if (frames.isEmpty()) {
            progress.skipped(taskName);
            return List.of();
        }
        boolean hasSvg = false;
        for (final File f : frames) {
            if (f.getName().endsWith(".svg")) {
                hasSvg = true;
                break;
            }
        }
        if (!hasSvg) {
            progress.complete(taskName);
            return frames;
        }

        if (!tmpDir.exists() && !tmpDir.mkdirs()) {
            throw new IOException("Failed to create temporary raster directory: " + tmpDir);
        }

        final List<File> pngFrames = new ArrayList<>(frames.size());
        final int total = frames.size();
        progress.update(taskName, 0);

        for (int i = 0; i < total; i++) {
            final File src = frames.get(i);
            final File dstPng = new File(tmpDir, String.format(Locale.US, "frame_%04d.png", i + 1));
            if (src.getName().endsWith(".svg")) {
                final BufferedImage img = renderSvg(src, maxFramePixels);
                ImageIO.write(img, "png", dstPng);
                pngFrames.add(dstPng);
            } else {
                pngFrames.add(src);
            }
            final int percent = (int) Math.round((double) (i + 1) * 100.0 / total);
            progress.update(taskName, percent);
        }
        progress.complete(taskName);
        return pngFrames;
    }

    static void cleanup(final File directory) {
        if (null == directory || !directory.exists()) return;
        try {
            final File[] files = directory.listFiles();
            if (null != files) {
                for (final File f : files) {
                    if (f.isDirectory()) cleanup(f);
                    else f.delete();
                }
            }
            directory.delete();
        } catch (final Exception ignored) {
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
