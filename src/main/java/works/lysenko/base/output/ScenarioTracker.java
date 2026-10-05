package works.lysenko.base.output;

import org.w3c.dom.NodeList;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.spec.PropEnum;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;

/**
 * Tracks scenario execution progression across test cycles during limbo
 * periods,
 * generating visual status charts and compiling them into an animated GIF.
 */
public final class ScenarioTracker {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 680;
    private static final int FPS = 10;
    private static final int DELAY_CENTISECONDS = 100 / FPS; // 10 centiseconds = 100ms
    private static final int FINAL_FRAME_DELAY_CENTISECONDS = 150; // 1.5s on final frame

    // Theme colors matching HTML report
    private static final Color BG_COLOR = new Color(0x09, 0x0D, 0x16);
    private static final Color CARD_BG = new Color(0x11, 0x18, 0x27);
    private static final Color CARD_BORDER = new Color(0x1E, 0x29, 0x3B);
    private static final Color GRID_COLOR = new Color(30, 41, 59, 120);
    private static final Color ACCENT_BLUE = new Color(0x38, 0xBD, 0xF8);
    private static final Color GREEN_BAR = new Color(0x22, 0xC5, 0x5E);
    private static final Color AMBER_BAR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color SLATE_BAR = new Color(0x33, 0x41, 0x55);
    private static final Color BAR_TRACK = new Color(30, 41, 59, 90);
    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
    private static final Color TEXT_DIM = new Color(0x64, 0x74, 0x8B);

    private static final List<File> capturedFrames = new ArrayList<>();
    private static File customOutputDir = null;

    private ScenarioTracker() {
    }

    /**
     * Sets a custom output directory (mainly for testing).
     *
     * @param dir the directory to output frames and gif to
     */
    public static void setCustomOutputDir(final File dir) {
        customOutputDir = dir;
    }

    /**
     * Clears tracked frames list.
     */
    public static void reset() {
        capturedFrames.clear();
        customOutputDir = null;
    }

    /**
     * Retrieves the list of captured frame files.
     *
     * @return list of captured frame files
     */
    public static List<File> getCapturedFrames() {
        return new ArrayList<>(capturedFrames);
    }

    /**
     * Hook called during limbo period between test cycles.
     *
     * @param testNumber the test cycle number just completed
     */
    public static void onLimbo(final Integer testNumber) {
        if (!ProgressionSettings.current().scenarioEnabled())
            return;
        if (isNull(core))
            return;
        final Set<_Scenario> leafs = core.getAccessibleLeafs();
        if (leafs.isEmpty())
            return;

        final List<_Scenario> scenarios = new ArrayList<>(leafs);
        scenarios.sort(Comparator.comparing(_Scenario::getShortName).thenComparing(_Scenario::getName));

        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsCount()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_COUNT.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_COUNT.get()) : 1);
        final int currentCycle = isNotNull(testNumber) ? testNumber : capturedFrames.size() + 1;

        final BufferedImage image = renderProgressionGraph(scenarios, target, currentCycle);
        final File runDir = resolveRunDirectory();
        if (isNull(runDir))
            return;

        final File progressionDir = new File(runDir, "progression");
        if (!progressionDir.exists() && !progressionDir.mkdirs())
            return;

        final int maxFrames = ProgressionFrames.frameLimit(
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxTotalPixels(),
                WIDTH,
                HEIGHT);
        final int frameIndex = Math.min(currentCycle, maxFrames);
        final File frameFile = new File(progressionDir, String.format("frame_%04d.png", frameIndex));
        try {
            ImageIO.write(image, "png", frameFile);
            if (!capturedFrames.contains(frameFile) && capturedFrames.size() < maxFrames) {
                capturedFrames.add(frameFile);
            }
        } catch (final IOException e) {
            System.err.println("Failed to save progression frame: " + e.getMessage());
        }
    }

    /**
     * Hook called upon test completion to compile collected frames into animated
     * GIF and WebP.
     */
    public static void onComplete() {
        final String prefix = (isNotNull(timer)) ? String.valueOf(timer.startedAt()) : "run";
        processFrames(resolveRunDirectory(), prefix, ProcessingProgress.NONE);
    }

    /**
     * Compiles the captured frames for a completed run.
     *
     * @param runDir   run output directory
     * @param prefix   run-specific artifact prefix
     * @param progress post-processing progress reporter
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress) {

        processFrames(runDir, prefix, progress, ProgressionSettings.current().scenarioEnabled());
    }

    /**
     * Compiles captured frames when scenario progression is enabled.
     *
     * @param runDir   run output directory
     * @param prefix   run-specific artifact prefix
     * @param progress post-processing progress reporter
     * @param enabled  whether scenario progression is enabled
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled) {
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxFramePixels(),
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels) {
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                maxFrames,
                maxFramePixels,
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final int maxTotalPixels) {
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final int maxTotalPixels,
            final boolean mp4Enabled,
            final String ffmpeg) {
        if (!enabled) {
            progress.skipped("Scenario progression GIF");
            progress.skipped("Scenario progression WebP");
            progress.skipped("Scenario progression MP4");
            return;
        }
        if (isNull(runDir)) {
            progress.skipped("Scenario progression GIF");
            progress.skipped("Scenario progression WebP");
            progress.skipped("Scenario progression MP4");
            return;
        }
        final String gifTask = "Scenario progression GIF";
        final String webpTask = "Scenario progression WebP";
        final String mp4Task = "Scenario progression MP4";
        final File progressionDir = new File(runDir, "progression");
        final List<File> framesToProcess;
        try {
            framesToProcess = ProgressionFrames.select(
                    progressionDir,
                    maxFrames,
                    maxFramePixels,
                    maxTotalPixels);
        } catch (final Exception e) {
            progress.failed(gifTask, e);
            progress.failed(webpTask, e);
            progress.failed(mp4Task, e);
            return;
        }
        if (framesToProcess.isEmpty()) {
            progress.skipped(gifTask);
            progress.skipped(webpTask);
            progress.skipped(mp4Task);
            return;
        }

        final File gifFile = new File(runDir, prefix + ".progression.gif");
        final File webpFile = new File(runDir, prefix + ".progression.webp");
        try {
            writeAnimatedGifFiles(
                    framesToProcess,
                    gifFile,
                    DELAY_CENTISECONDS,
                    FINAL_FRAME_DELAY_CENTISECONDS,
                    maxFramePixels,
                    percentage -> progress.update(gifTask, 10 + (percentage * 45 / 100)));
            progress.complete(gifTask);
            log("Progression GIF generated: " + gifFile.getAbsolutePath());
        } catch (final Exception e) {
            progress.failed(gifTask, e);
        }
        try {
            AnimatedWebP.write(
                    framesToProcess,
                    webpFile,
                    DELAY_CENTISECONDS * 10,
                    FINAL_FRAME_DELAY_CENTISECONDS * 10,
                    maxFramePixels,
                    percentage -> progress.update(webpTask, 10 + (percentage * 90 / 100)));
            progress.complete(webpTask);
            log("Progression WebP generated: " + webpFile.getAbsolutePath());
        } catch (final Exception e) {
            progress.failed(webpTask, e);
        }
        if (!mp4Enabled) {
            progress.skipped(mp4Task);
            return;
        }
        try {
            ProgressionMp4.write(
                    framesToProcess,
                    new File(runDir, prefix + ".progression.mp4"),
                    ffmpeg,
                    FPS,
                    maxFramePixels,
                    percentage -> progress.update(mp4Task, percentage));
            progress.complete(mp4Task);
            log("Progression MP4 generated: " + new File(runDir, prefix + ".progression.mp4").getAbsolutePath());
        } catch (final Exception e) {
            progress.failed(mp4Task, e);
        }
    }

    /**
     * Renders progression chart as a BufferedImage.
     *
     * @param scenarios    ordered scenarios list
     * @param target       target executions count
     * @param currentCycle current cycle index
     * @return rendered BufferedImage
     */
    public static BufferedImage renderProgressionGraph(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {

        final BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Canvas Background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Card Panel
        final int cardX = 24, cardY = 20, cardW = WIDTH - 48, cardH = HEIGHT - 40;
        g.setColor(CARD_BG);
        g.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 12, 12);

        // Scenario Execution stats
        int completedCount = 0;
        int maxExecs = target;
        final int[] execsArray = new int[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            final _Scenario sc = scenarios.get(i);
            final int scTarget = (sc instanceof Mono) ? 1 : target;
            final int execs = (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getExecutions(sc)
                    : 0;
            execsArray[i] = execs;
            if (execs >= scTarget)
                completedCount++;
            if (execs > maxExecs)
                maxExecs = execs;
        }

        // Header Title
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT_PRIMARY);
        g.drawString("SCENARIO EXECUTION PROGRESSION", cardX + 24, cardY + 36);

        // Header Badges
        final int badgeY = cardY + 22;
        int badgeX = cardX + cardW - 24;

        final long elapsedMillis = (isNotNull(timer))
                ? Math.max(0L, System.currentTimeMillis() - timer.startedAt())
                : 0L;
        final String elapsedStr = (isNotNull(timer))
                ? "ELAPSED: " + formatElapsedTime(elapsedMillis)
                : "ELAPSED: --:--:--";
        badgeX -= drawBadge(g, elapsedStr, badgeX, badgeY, CARD_BORDER, TEXT_MUTED) + 8;

        // Badge 3: Target count
        final String targetStr = String.format("TARGET: %d", target);
        badgeX -= drawBadge(g, targetStr, badgeX, badgeY, CARD_BORDER, TEXT_MUTED) + 8;

        // Badge 2: Completed count & percent
        final int totalScenarios = scenarios.size();
        final int pct = totalScenarios > 0 ? (completedCount * 100 / totalScenarios) : 0;
        final String completedStr = String.format("COMPLETED: %d/%d (%d%%)", completedCount, totalScenarios, pct);
        final Color compColor = (completedCount == totalScenarios) ? GREEN_BAR : AMBER_BAR;
        badgeX -= drawBadge(g, completedStr, badgeX, badgeY,
                new Color(compColor.getRed(), compColor.getGreen(), compColor.getBlue(), 40), compColor) + 8;

        // Badge 1: Cycle number
        final String cycleStr = String.format("CYCLE #%d", currentCycle);
        drawBadge(g, cycleStr, badgeX, badgeY, new Color(56, 189, 248, 40), ACCENT_BLUE);

        // Chart plotting area
        final int chartX = cardX + 54;
        final int chartY = cardY + 68;
        final int chartW = cardW - 74;
        final int chartH = cardH - 300; // leaves ~230px for rotated bottom labels
        final int maxY = Math.max(target + 1, maxExecs + 1);

        // Horizontal Grid & Y-Ticks
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        final int ySteps = Math.min(maxY, 8);
        final int stepVal = Math.max(1, (int) Math.ceil((double) maxY / ySteps));

        for (int v = 0; v <= maxY; v += stepVal) {
            final int yPos = chartY + chartH - (int) Math.round(((double) v / maxY) * chartH);
            g.setColor(GRID_COLOR);
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine(chartX, yPos, chartX + chartW, yPos);

            g.setColor(TEXT_DIM);
            final String tick = String.valueOf(v);
            final FontMetrics fm = g.getFontMetrics();
            g.drawString(tick, chartX - fm.stringWidth(tick) - 8, yPos + 4);
        }

        // Target Line (dashed sky blue)
        if (target <= maxY) {
            final int targetY = chartY + chartH - (int) Math.round(((double) target / maxY) * chartH);
            final float[] dash = { 6.0f, 4.0f };
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g.setColor(new Color(56, 189, 248, 200));
            g.drawLine(chartX, targetY, chartX + chartW, targetY);

            // Target Tag
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            final String tag = String.format("TARGET: %d", target);
            final FontMetrics fmTag = g.getFontMetrics();
            final int tagW = fmTag.stringWidth(tag) + 8;
            g.setColor(CARD_BG);
            g.fillRect(chartX + chartW - tagW - 4, targetY - 14, tagW, 13);
            g.setColor(ACCENT_BLUE);
            g.drawString(tag, chartX + chartW - tagW, targetY - 4);
        }

        // Draw Bars
        final int count = scenarios.size();
        final double slotW = (double) chartW / count;
        final int barW = Math.max(4, Math.min(26, (int) (slotW * 0.72)));
        final double labelAngle = -Math.PI / 3.27; // ~ -55 degrees
        final double cosA = Math.cos(Math.abs(labelAngle));
        final double sinA = Math.sin(Math.abs(labelAngle));

        for (int i = 0; i < count; i++) {
            final _Scenario sc = scenarios.get(i);
            final int execs = execsArray[i];
            final int centerX = (int) Math.round(chartX + i * slotW + slotW / 2.0);
            final int barLeft = centerX - barW / 2;

            // Background track
            g.setColor(BAR_TRACK);
            g.fillRoundRect(barLeft, chartY, barW, chartH, 4, 4);

            // Bar fill
            if (execs > 0) {
                final int barH = Math.max(3, (int) Math.round(((double) execs / maxY) * chartH));
                final int barTop = chartY + chartH - barH;
                final int scTarget = (sc instanceof Mono) ? 1 : target;
                final Color barColor = (execs >= scTarget) ? GREEN_BAR : AMBER_BAR;
                g.setColor(barColor);
                g.fillRoundRect(barLeft, barTop, barW, barH, 4, 4);
                if (barH > 4) {
                    g.fillRect(barLeft, chartY + chartH - 4, barW, 4); // flatten bottom
                }
            } else {
                // Dim stub for 0
                g.setColor(SLATE_BAR);
                g.fillRect(barLeft, chartY + chartH - 2, barW, 2);
            }

            // Value text above bar
            final int scTarget = (sc instanceof Mono) ? 1 : target;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
            final String valStr = String.valueOf(execs);
            final FontMetrics fmVal = g.getFontMetrics();
            final int valW = fmVal.stringWidth(valStr);
            final int valY = (execs > 0)
                    ? (chartY + chartH - (int) Math.round(((double) execs / maxY) * chartH) - 4)
                    : (chartY + chartH - 6);
            final Color valColor = (execs >= scTarget) ? new Color(0x4A, 0xDE, 0x80)
                    : (execs > 0 ? new Color(0xFB, 0xBF, 0x24) : TEXT_DIM);
            g.setColor(valColor);
            g.drawString(valStr, centerX - valW / 2, Math.max(chartY + 12, valY));

            // Rotated Scenario Label below chart
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            final String rawName = sc.getShortName();
            final Color nameColor = (execs >= scTarget) ? TEXT_PRIMARY : (execs > 0 ? TEXT_MUTED : TEXT_DIM);
            g.setColor(nameColor);

            final FontMetrics fm = g.getFontMetrics();

            // Calculate max allowed length so label stays within card panel
            final int maxW_X = (int) Math.floor((centerX - (cardX + 8)) / cosA);
            final int maxW_Y = (int) Math.floor(((cardY + cardH - 12) - (chartY + chartH + 10)) / sinA);
            final int maxAllowedW = Math.max(30, Math.min(maxW_X, maxW_Y));

            String displayName = rawName;
            if (fm.stringWidth(displayName) > maxAllowedW) {
                while (displayName.length() > 3 && fm.stringWidth("..." + displayName) > maxAllowedW) {
                    displayName = displayName.substring(1);
                }
                displayName = "..." + displayName;
            }

            final int textW = fm.stringWidth(displayName);
            final AffineTransform orig = g.getTransform();
            g.translate(centerX - 2, chartY + chartH + 10);
            g.rotate(labelAngle);
            g.drawString(displayName, -textW, 0);
            g.setTransform(orig);
        }

        g.dispose();
        return img;
    }

    static String formatElapsedTime(final long elapsedMillis) {
        final long totalSeconds = Math.max(0L, elapsedMillis) / 1000;
        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d:%02d",
                totalSeconds / 3600,
                (totalSeconds / 60) % 60,
                totalSeconds % 60);
    }

    private static int drawBadge(
            final Graphics2D g,
            final String text,
            final int rightX,
            final int topY,
            final Color bg,
            final Color fg) {

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        final FontMetrics fm = g.getFontMetrics();
        final int textW = fm.stringWidth(text);
        final int badgeW = textW + 16;
        final int badgeH = 20;
        final int leftX = rightX - badgeW;

        g.setColor(bg);
        g.fillRoundRect(leftX, topY, badgeW, badgeH, 6, 6);
        g.setColor(fg);
        g.drawString(text, leftX + 8, topY + 14);
        return badgeW;
    }

    private static File resolveRunDirectory() {
        if (isNotNull(customOutputDir))
            return customOutputDir;
        if (isNotNull(parameters) && isNotNull(timer) && isNotNull(core)) {
            try {
                final String logFilePath = name(RUN_LOG_);
                final File logFile = new File(logFilePath);
                final File parent = logFile.getParentFile();
                if (isNotNull(parent))
                    return parent;
            } catch (final Exception ignored) {
            }
        }
        return new File("target/runs");
    }

    /**
     * Compiles a sequence of BufferedImages into an animated GIF file.
     *
     * @param frames                      images to encode
     * @param outFile                     output GIF file
     * @param delayCentiseconds           delay for intermediate frames (10 for
     *                                    10fps)
     * @param finalFrameDelayCentiseconds delay for final frame before looping
     * @throws IOException on write error
     */
    public static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds) throws IOException {

        writeAnimatedGif(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds, percentage -> {
        });
    }

    static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {

        if (frames.isEmpty())
            return;
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF ImageWriter available");
        }
        final ImageWriter writer = writers.next();

        if (isNotNull(outFile.getParentFile())) {
            outFile.getParentFile().mkdirs();
        }

        try (final ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);

            final ImageWriteParam params = writer.getDefaultWriteParam();

            for (int i = 0; i < frames.size(); i++) {
                final BufferedImage frame = frames.get(i);
                final int delay = (i == frames.size() - 1) ? finalFrameDelayCentiseconds : delayCentiseconds;
                final IIOMetadata metadata = configureGifMetadata(writer, frame, delay, i == 0);
                writer.writeToSequence(new IIOImage(frame, null, metadata), params);
                progress.accept((int) (((i + 1L) * 100) / frames.size()));
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {
        writeAnimatedGifFiles(
                frames,
                outFile,
                delayCentiseconds,
                finalFrameDelayCentiseconds,
                ProgressionSettings.current().maxFramePixels(),
                progress);
    }

    static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        if (frames.isEmpty())
            return;
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("gif");
        if (!writers.hasNext())
            throw new IOException("No GIF ImageWriter available");
        final ImageWriter writer = writers.next();
        if (isNotNull(outFile.getParentFile()) && !outFile.getParentFile().mkdirs()
                && !outFile.getParentFile().isDirectory()) {
            throw new IOException("Unable to create GIF output directory: " + outFile.getParent());
        }

        try (final ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.prepareWriteSequence(null);
            final ImageWriteParam params = writer.getDefaultWriteParam();
            for (int i = 0; i < frames.size(); i++) {
                final BufferedImage frame = ProgressionFrames.read(frames.get(i), maxFramePixels);
                final int delay = (i == frames.size() - 1) ? finalFrameDelayCentiseconds : delayCentiseconds;
                final IIOMetadata metadata = configureGifMetadata(writer, frame, delay, i == 0);
                writer.writeToSequence(new IIOImage(frame, null, metadata), params);
                progress.accept((int) (((i + 1L) * 100) / frames.size()));
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static IIOMetadata configureGifMetadata(
            final ImageWriter writer,
            final BufferedImage image,
            final int delayCentiseconds,
            final boolean isFirst) throws IOException {

        final ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        final IIOMetadata metadata = writer.getDefaultImageMetadata(type, null);
        final String nativeFormat = metadata.getNativeMetadataFormatName();
        final IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(nativeFormat);

        // GraphicControlExtension
        IIOMetadataNode gceNode = null;
        final NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if ("GraphicControlExtension".equalsIgnoreCase(children.item(i).getNodeName())) {
                gceNode = (IIOMetadataNode) children.item(i);
                break;
            }
        }
        if (gceNode == null) {
            gceNode = new IIOMetadataNode("GraphicControlExtension");
            root.appendChild(gceNode);
        }
        gceNode.setAttribute("disposalMethod", "none");
        gceNode.setAttribute("userInputFlag", "FALSE");
        gceNode.setAttribute("transparentColorFlag", "FALSE");
        gceNode.setAttribute("delayTime", Integer.toString(delayCentiseconds));
        gceNode.setAttribute("transparentColorIndex", "0");

        // Netscape 2.0 loop extension for infinite looping
        if (isFirst) {
            final IIOMetadataNode appExtensions = new IIOMetadataNode("ApplicationExtensions");
            final IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");
            appNode.setAttribute("applicationID", "NETSCAPE");
            appNode.setAttribute("authenticationCode", "2.0");
            appNode.setUserObject(new byte[] { 0x01, 0x00, 0x00 }); // 0 = loop forever
            appExtensions.appendChild(appNode);
            root.appendChild(appExtensions);
        }

        metadata.setFromTree(nativeFormat, root);
        return metadata;
    }
}
