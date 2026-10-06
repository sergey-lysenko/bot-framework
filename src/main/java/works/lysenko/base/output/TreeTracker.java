package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.spec.PropEnum;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Objects.isNull;
import static works.lysenko.Base.*;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;

/**
 * Tracks scenario tree progression across test cycles during limbo periods,
 * rendering the scenario hierarchy into PNG frames with visual execution
 * indicators,
 * and compiling them into an animated GIF upon test completion.
 */
@SuppressWarnings({ "ClassWithoutLogger", "MagicNumber", "NestedMethodCall", "OverlyComplexMethod",
        "MethodWithMultipleLoops", "ClassWithTooManyFields" })
public final class TreeTracker {

    private static final int COL_WIDTH = 270;
    private static final int ROW_HEIGHT = 60;
    private static final int CARD_WIDTH = 220;
    private static final int CARD_HEIGHT = 46;
    private static final int HEADER_HEIGHT = 60;
    private static final int PADDING_X = 40;
    private static final int PADDING_Y = 24;
    private static final int BOTTOM_PADDING = 40;

    private static final int FPS = 10;
    private static final int DELAY_CENTISECONDS = 100 / FPS; // 100ms
    private static final int FINAL_FRAME_DELAY_CENTISECONDS = 150; // 1.5s on final frame

    // Theme colors matching HTML report and TreeHtml
    private static final Color BG_COLOR = new Color(0x0F, 0x17, 0x2A);
    private static final Color HEADER_BG = new Color(0x1E, 0x29, 0x3B);
    private static final Color HEADER_BORDER = new Color(0x33, 0x41, 0x55);
    private static final Color GRID_DOT = new Color(0x1E, 0x29, 0x3B, 180);
    private static final Color TEXT_WHITE = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
    private static final Color ACCENT_CYAN = new Color(0x38, 0xBD, 0xF8);
    private static final Color UNVISITED_CARD = new Color(0x1E, 0x29, 0x3B);
    private static final Color UNVISITED_BORDER = new Color(0x33, 0x41, 0x55);
    private static final Color UNVISITED_EDGE = new Color(0x33, 0x41, 0x55, 140);
    private static final Color WARNING_AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color GREEN_DONE = new Color(0x22, 0xC5, 0x5E);
    private static final Color TEXT_DARK = new Color(0x0F, 0x17, 0x2A);

    private static final List<File> capturedFrames = new ArrayList<>();
    private static File customOutputDir = null;
    private static volatile BufferedImage latestFrameImage = null;
    private static final Map<String, Integer> previousExecutions = new HashMap<>();

    private TreeTracker() {
    }

    /**
     * Resets internal state for tests.
     */
    public static void reset() {
        capturedFrames.clear();
        customOutputDir = null;
        latestFrameImage = null;
        previousExecutions.clear();
    }

    /**
     * Retrieves the most recent frame image of tree progression.
     *
     * @return the most recent rendered BufferedImage, or null if none available
     */
    public static BufferedImage getLatestFrameImage() {
        return latestFrameImage;
    }

    /**
     * Overrides output directory (for testing).
     *
     * @param dir target directory
     */
    public static void setCustomOutputDir(final File dir) {
        customOutputDir = dir;
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
     * Calculates the visual progress color based on executions and target count.
     * Smoothly interpolates from the non-started grayish-dark-blue color (UNVISITED_BORDER)
     * to the completion emerald green (GREEN_DONE).
     *
     * @param execs  number of executions
     * @param target target executions count
     * @return Color indicating progress
     */
    public static Color getProgressColor(final int execs, final int target) {
        if (execs <= 0) {
            return UNVISITED_BORDER;
        }
        final int safeTarget = Math.max(1, target);
        if (execs >= safeTarget) {
            return GREEN_DONE;
        }
        final double ratio = (double) execs / (double) safeTarget;
        final int r = (int) Math.round(UNVISITED_BORDER.getRed() + ratio * (GREEN_DONE.getRed() - UNVISITED_BORDER.getRed()));
        final int g = (int) Math.round(UNVISITED_BORDER.getGreen() + ratio * (GREEN_DONE.getGreen() - UNVISITED_BORDER.getGreen()));
        final int b = (int) Math.round(UNVISITED_BORDER.getBlue() + ratio * (GREEN_DONE.getBlue() - UNVISITED_BORDER.getBlue()));
        return new Color(r, g, b);
    }

    static int getTargetExecutions(final NodeData node, final int leafTarget) {
        return switch (getScenarioType(node)) {
            case MONO -> 1;
            case LEAF -> Math.max(1, leafTarget);
            default -> 0;
        };
    }

    static int getOverExecutionPercent(final int execs, final int target) {
        final int safeTarget = Math.max(1, target);
        return (execs > safeTarget) ? (int) Math.ceil((execs - safeTarget) * 100.0 / safeTarget) : 0;
    }

    static Color getOverExecutionColor(final double overRatio, final double maxOverRatio) {
        if (overRatio <= 0.0 || maxOverRatio <= 0.0)
            return GREEN_DONE;

        final double progress = Math.min(1.0, overRatio / maxOverRatio);
        return new Color(
                interpolate(GREEN_DONE.getRed(), TEXT_WHITE.getRed(), progress),
                interpolate(GREEN_DONE.getGreen(), TEXT_WHITE.getGreen(), progress),
                interpolate(GREEN_DONE.getBlue(), TEXT_WHITE.getBlue(), progress));
    }

    static String getExecutionBadgeText(final NodeData node, final int execs, final int leafTarget) {
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.NODE == type || ScenarioType.CORE == type) {
            return execs + ((1 == execs) ? " exec" : " execs");
        }

        final int target = getTargetExecutions(node, leafTarget);
        if (execs > target && ScenarioType.LEAF == type) {
            return execs + "/" + target + " +" + getOverExecutionPercent(execs, target) + "%";
        }
        if (execs >= target && 0 < execs) {
            return "\u2713 " + execs + "/" + target;
        }
        return execs + "/" + target;
    }

    /**
     * Hook called during limbo period between test cycles.
     *
     * @param testNumber the test cycle number just completed
     */
    public static void onLimbo(final Integer testNumber) {
        if (!ProgressionSettings.current().treeEnabled())
            return;
        if (isNull(core) || isNull(core.getResults()))
            return;
        final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
        if (sorted.isEmpty())
            return;

        final TreeLayout layout = TreeHtml.computeLayout(sorted);
        if (layout.nodes().isEmpty())
            return;

        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsCount()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_COUNT.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_COUNT.get()) : 1);
        final int currentCycle = isNotNull(testNumber) ? testNumber : capturedFrames.size() + 1;

        final File runDir = resolveRunDirectory();
        if (isNull(runDir))
            return;

        final BufferedImage image;
        try {
            final Set<String> recentNodeKeys = computeRecentNodeKeys(layout);
            image = renderTreeProgression(layout, target, currentCycle, recentNodeKeys);
            updatePreviousExecutions(layout);
        } catch (final IllegalArgumentException e) {
            System.err.println("Skipped tree progression frame: " + e.getMessage());
            return;
        }
        latestFrameImage = image;
        if (isNotNull(core) && isNotNull(core.getDashboard())) {
            core.getDashboard().setTreeProgression(image);
        }

        final File progressionDir = new File(runDir, "tree_progression");
        if (!progressionDir.exists() && !progressionDir.mkdirs())
            return;

        final int maxFrames = ProgressionFrames.frameLimit(
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxTotalPixels(),
                image.getWidth(),
                image.getHeight());
        final int frameIndex = Math.min(currentCycle, maxFrames);
        final File frameFile = new File(progressionDir, String.format(Locale.US, "frame_%04d.png", frameIndex));
        try {
            ImageIO.write(image, "png", frameFile);
            if (ProgressionSettings.current().treeMp4Enabled()) {
                final java.util.Optional<TreeSonifier> sonifier = ProgressionSettings.current().treeSonifier();
                if (sonifier.isPresent()) {
                    sonifier.get().capture(frameFile, layout, target);
                }
            }
            if (!capturedFrames.contains(frameFile) && capturedFrames.size() < maxFrames) {
                capturedFrames.add(frameFile);
            }
        } catch (final IOException e) {
            System.err.println("Failed to save tree progression frame: " + e.getMessage());
        }
    }

    private static Set<String> computeRecentNodeKeys(final TreeLayout layout) {
        final Set<String> recent = new HashSet<>();
        for (final NodeData n : layout.nodes()) {
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            final int prev = previousExecutions.getOrDefault(n.id(), 0);
            if (execs > prev) {
                recent.add(n.id());
            }
        }
        return recent;
    }

    private static void updatePreviousExecutions(final TreeLayout layout) {
        for (final NodeData n : layout.nodes()) {
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            previousExecutions.put(n.id(), execs);
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
     * Compiles the captured tree frames for a completed run.
     *
     * @param runDir   run output directory
     * @param prefix   run-specific artifact prefix
     * @param progress post-processing progress reporter
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress) {

        processFrames(runDir, prefix, progress, ProgressionSettings.current().treeEnabled());
    }

    /**
     * Compiles captured tree frames when tree progression is enabled.
     *
     * @param runDir   run output directory
     * @param prefix   run-specific artifact prefix
     * @param progress post-processing progress reporter
     * @param enabled  whether tree progression is enabled
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
                ProgressionSettings.current().treeMp4Enabled(),
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
                ProgressionSettings.current().treeMp4Enabled(),
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
                ProgressionSettings.current().treeMp4Enabled(),
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
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                mp4Enabled,
                ffmpeg,
                ProgressionSettings.current().treeSonification());
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
            final String ffmpeg,
            final String treeSonification) {
        if (!enabled) {
            progress.skipped("Tree progression GIF");
            progress.skipped("Tree progression WebP");
            progress.skipped("Tree progression MP4");
            return;
        }
        if (isNull(runDir)) {
            progress.skipped("Tree progression GIF");
            progress.skipped("Tree progression WebP");
            progress.skipped("Tree progression MP4");
            return;
        }
        final String gifTask = "Tree progression GIF";
        final String webpTask = "Tree progression WebP";
        final String mp4Task = "Tree progression MP4";
        final File progressionDir = new File(runDir, "tree_progression");
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

        final File gifFile = new File(runDir, prefix + ".tree.progression.gif");
        final File webpFile = new File(runDir, prefix + ".tree.progression.webp");
        try {
            ScenarioTracker.writeAnimatedGifFiles(
                    framesToProcess,
                    gifFile,
                    DELAY_CENTISECONDS,
                    FINAL_FRAME_DELAY_CENTISECONDS,
                    maxFramePixels,
                    percentage -> progress.update(gifTask, 10 + (percentage * 45 / 100)));
            progress.complete(gifTask);
            log("Tree progression GIF generated: " + gifFile.getAbsolutePath());
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
            log("Tree progression WebP generated: " + webpFile.getAbsolutePath());
        } catch (final Exception e) {
            progress.failed(webpTask, e);
        }
        if (!mp4Enabled) {
            progress.skipped(mp4Task);
            return;
        }
        final File mp4File = new File(runDir, prefix + ".tree.progression.mp4");
        File audioFile = null;
        try {
            final java.util.Optional<TreeSonifier> sonifier = TreeProgressionAudio.find(treeSonification);
            if (sonifier.isPresent() && sonifier.get().canSonify(framesToProcess)) {
                audioFile = File.createTempFile("tree-progression-audio-", ".wav", runDir);
                sonifier.get().writeWav(framesToProcess, audioFile, FPS);
            }
            ProgressionMp4.write(
                    framesToProcess,
                    mp4File,
                    ffmpeg,
                    FPS,
                    maxFramePixels,
                    audioFile,
                    percentage -> progress.update(mp4Task, percentage));
            progress.complete(mp4Task);
            log("Tree progression MP4 generated: " + mp4File.getAbsolutePath());
        } catch (final Exception e) {
            progress.failed(mp4Task, e);
        } finally {
            if (null != audioFile && !audioFile.delete()) {
                log("Unable to remove temporary tree progression audio: " + audioFile.getAbsolutePath());
            }
        }
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage.
     *
     * @param layout       computed tree layout
     * @param target       target executions count
     * @param currentCycle current cycle index
     * @return rendered BufferedImage
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {

        return renderTreeProgression(layout, target, currentCycle, Collections.emptySet());
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage, highlighting recently changed nodes.
     *
     * @param layout         computed tree layout
     * @param target         target executions count
     * @param currentCycle   current cycle index
     * @param recentNodeKeys set of node keys recently traversed or changed
     * @return rendered BufferedImage
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle,
            final Set<String> recentNodeKeys) {

        int maxCol = 0;
        double maxRow = 0.0;
        int totalLeafs = 0;
        int coveredLeafs = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        for (final NodeData n : layout.nodes()) {
            if (n.col() > maxCol)
                maxCol = n.col();
            if (n.row() > maxRow)
                maxRow = n.row();
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            totalExecs += execs;
            if (ScenarioType.LEAF == getScenarioType(n)) {
                maxLeafOverRatio = Math.max(maxLeafOverRatio, getLeafOverExecutionRatio(n, target));
            }
            if (n.children().isEmpty() && isTerminalScenario(n)) {
                totalLeafs++;
                if (execs >= getTargetExecutions(n, target))
                    coveredLeafs++;
            }
        }

        final long canvasWidth = evenDimension(Math.max(1280L, ((long) maxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
        final long canvasHeight = evenDimension(Math.max(
                720L,
                (long) Math.ceil((maxRow + 1.0) * ROW_HEIGHT + HEADER_HEIGHT + PADDING_Y + BOTTOM_PADDING)));
        final long maxPixels = ProgressionFrames.maxFramePixels(ProgressionSettings.current().maxFramePixels());
        if (canvasWidth > ProgressionFrames.MAX_DIMENSION
                || canvasHeight > ProgressionFrames.MAX_DIMENSION
                || canvasWidth * canvasHeight > maxPixels) {
            throw new IllegalArgumentException("Rendered tree frame would exceed the configured limit ("
                    + canvasWidth + "x" + canvasHeight + ", max pixels " + maxPixels + ")");
        }
        final int canvasW = (int) canvasWidth;
        final int canvasH = (int) canvasHeight;

        final BufferedImage img = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, canvasW, canvasH);

        // Dot grid pattern
        g.setColor(GRID_DOT);
        for (int x = 0; x < canvasW; x += 24) {
            for (int y = HEADER_HEIGHT; y < canvasH; y += 24) {
                g.fillRect(x, y, 1, 1);
            }
        }

        // Draw edges
        drawEdges(g, layout.edges(), target, maxLeafOverRatio, recentNodeKeys);

        // Draw nodes
        final Font fontLabel = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        final Font fontBadge = new Font(Font.MONOSPACED, Font.BOLD, 10);
        for (final NodeData n : layout.nodes()) {
            drawNode(g, n, target, maxLeafOverRatio, fontLabel, fontBadge, recentNodeKeys.contains(n.id()));
        }

        // Draw header
        drawHeader(g, canvasW, currentCycle, target, coveredLeafs, totalLeafs, totalExecs);

        g.dispose();
        return img;
    }

    private static void drawEdges(
            final Graphics2D g,
            final List<Edge> edges,
            final int target,
            final double maxLeafOverRatio,
            final Set<String> recentNodeKeys) {

        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (final Edge e : sortedEdges) {
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + PADDING_X;
            final int startY = (int) Math
                    .round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + HEADER_HEIGHT + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math
                    .round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + HEADER_HEIGHT + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final int toEvents = (null != e.to().result() && null != e.to().result().getEvents())
                    ? e.to().result().getEvents().size()
                    : 0;

            final Path2D.Double path = new Path2D.Double();
            path.moveTo(startX, startY);
            path.curveTo(cX, startY, cX, endY, endX, endY);

            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            if (isRecent) {
                // Outer glowing aura for recently changed branch edge
                g.setColor(new Color(0x38, 0xBD, 0xF8, 90));
                g.setStroke(new BasicStroke(5.0f));
                g.draw(path);

                g.setColor(ACCENT_CYAN);
                g.setStroke(new BasicStroke(3.0f));
                g.draw(path);
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toEvents > 0) {
                    g.setColor(WARNING_AMBER);
                } else {
                    g.setColor(getScenarioProgressColor(e.to(), target, maxLeafOverRatio));
                }
                g.setStroke(new BasicStroke(2.2f));
                g.draw(path);
            } else {
                g.setColor(UNVISITED_EDGE);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(path);
            }
        }
    }

    private static void drawNode(
            final Graphics2D g,
            final NodeData n,
            final int target,
            final double maxLeafOverRatio,
            final Font fontLabel,
            final Font fontBadge,
            final boolean isRecent) {

        final int x = n.col() * COL_WIDTH + PADDING_X;
        final int y = (int) Math.round(n.row() * ROW_HEIGHT + HEADER_HEIGHT + PADDING_Y);
        final _Result res = n.result();
        final int execs = (null != res) ? res.getExecutions() : 0;
        final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
        final int scenarioTarget = getTargetExecutions(n, target);
        final int safeTarget = Math.max(1, scenarioTarget);
        final double overRatio = (ScenarioType.LEAF == getScenarioType(n))
                ? getLeafOverExecutionRatio(n, target)
                : 0.0;

        final Color progressColor = getScenarioProgressColor(n, target, maxLeafOverRatio);
        final Color cardFill;
        final Color borderColor;
        final float strokeW;

        if (isRecent) {
            cardFill = (overRatio > 0.0) ? progressColor : new Color(0x16, 0x3A, 0x58);
            borderColor = ACCENT_CYAN;
            strokeW = 2.8f;
        } else if (execs == 0) {
            cardFill = UNVISITED_CARD;
            borderColor = UNVISITED_BORDER;
            strokeW = 1.2f;
        } else if (overRatio > 0.0) {
            cardFill = progressColor;
            borderColor = (eventCount > 0) ? WARNING_AMBER : progressColor;
            strokeW = 1.8f;
        } else {
            final int r = (int) (UNVISITED_CARD.getRed() * 0.7 + progressColor.getRed() * 0.3);
            final int gr = (int) (UNVISITED_CARD.getGreen() * 0.7 + progressColor.getGreen() * 0.3);
            final int b = (int) (UNVISITED_CARD.getBlue() * 0.7 + progressColor.getBlue() * 0.3);
            cardFill = new Color(r, gr, b);
            borderColor = (eventCount > 0) ? WARNING_AMBER : progressColor;
            strokeW = 1.8f;
        }

        // Draw card background
        final RoundRectangle2D.Float rect = new RoundRectangle2D.Float(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
        g.setColor(cardFill);
        g.fill(rect);

        // Draw card border
        g.setStroke(new BasicStroke(strokeW));
        g.setColor(borderColor);
        g.draw(rect);

        // Progress bar along bottom of card (if visited)
        if (execs > 0) {
            final int barX = x + 8;
            final int barY = y + CARD_HEIGHT - 6;
            final int barW = CARD_WIDTH - 16;
            final int barH = 3;

            // Track background
            g.setColor(new Color(51, 65, 85, 160));
            g.fillRoundRect(barX, barY, barW, barH, 2, 2);

            // Fill
            final double fillRatio = (0 == scenarioTarget)
                    ? 1.0
                    : Math.min(1.0, (double) execs / (double) safeTarget);
            final int filledW = Math.max(4, (int) Math.round(barW * fillRatio));
            g.setColor(progressColor);
            g.fillRoundRect(barX, barY, filledW, barH, 2, 2);
        }

        // Right pill badge: e.g. "0/5", "3/5", "✓ 5/5"
        g.setFont(fontBadge);
        final FontMetrics fmBadge = g.getFontMetrics();
        final String badgeText = getExecutionBadgeText(n, execs, target);
        final Color badgeFg = (execs == 0) ? TEXT_MUTED : progressColor;

        final int badgeTextW = fmBadge.stringWidth(badgeText);
        final int pillW = badgeTextW + 12;
        final int pillH = 18;
        final int pillX = x + CARD_WIDTH - pillW - 8;
        final int pillY = y + 10;

        // Pill bg
        g.setColor(new Color(15, 23, 42, 220));
        g.fillRoundRect(pillX, pillY, pillW, pillH, 6, 6);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(borderColor);
        g.drawRoundRect(pillX, pillY, pillW, pillH, 6, 6);

        // Pill text
        g.setColor(badgeFg);
        g.drawString(badgeText, pillX + 6, pillY + 13);

        // Warning indicator if events > 0
        if (eventCount > 0) {
            g.setColor(WARNING_AMBER);
            g.fillOval(pillX - 10, y + 15, 6, 6);
        }

        // Node label
        g.setFont(fontLabel);
        final FontMetrics fmLabel = g.getFontMetrics();
        final int maxLabelW = pillX - x - 18;
        String label = n.label();
        if (fmLabel.stringWidth(label) > maxLabelW) {
            while (label.length() > 3 && fmLabel.stringWidth(label + "...") > maxLabelW) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "...";
        }
        g.setColor((overRatio > 0.0) ? TEXT_DARK : (execs > 0) ? TEXT_WHITE : TEXT_MUTED);
        g.drawString(label, x + 12, y + 24);
    }

    private static ScenarioType getScenarioType(final NodeData node) {
        if (null != node.result() && null != node.result().getScenarioType()) {
            return node.result().getScenarioType();
        }
        return node.children().isEmpty() ? ScenarioType.LEAF : ScenarioType.NODE;
    }

    private static boolean isTerminalScenario(final NodeData node) {
        final ScenarioType type = getScenarioType(node);
        return ScenarioType.LEAF == type || ScenarioType.MONO == type;
    }

    private static double getLeafOverExecutionRatio(final NodeData node, final int target) {
        final int safeTarget = Math.max(1, target);
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        return (execs > safeTarget) ? (double) (execs - safeTarget) / safeTarget : 0.0;
    }

    private static Color getScenarioProgressColor(
            final NodeData node,
            final int target,
            final double maxLeafOverRatio) {
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.LEAF == type) {
            final double overRatio = getLeafOverExecutionRatio(node, target);
            return (overRatio > 0.0)
                    ? getOverExecutionColor(overRatio, maxLeafOverRatio)
                    : getProgressColor(execs, target);
        }
        return getProgressColor(execs, 1);
    }

    private static int interpolate(final int start, final int end, final double progress) {
        return (int) Math.round(start + ((end - start) * progress));
    }

    private static long evenDimension(final long dimension) {
        if (dimension >= ProgressionFrames.MAX_DIMENSION)
            return dimension;
        return (dimension & 1L) == 0L ? dimension : dimension + 1L;
    }

    static String formatElapsedTime(final long elapsedMillis) {
        final long totalSeconds = Math.max(0L, elapsedMillis) / 1000;
        return String.format(
                Locale.ROOT,
                "%02d:%02d:%02d",
                totalSeconds / 3600,
                (totalSeconds / 60) % 60,
                totalSeconds % 60);
    }

    private static void drawHeader(
            final Graphics2D g,
            final int canvasW,
            final int currentCycle,
            final int target,
            final int coveredLeafs,
            final int totalLeafs,
            final int totalExecs) {

        g.setColor(HEADER_BG);
        g.fillRect(0, 0, canvasW, HEADER_HEIGHT);
        g.setColor(HEADER_BORDER);
        g.drawLine(0, HEADER_HEIGHT, canvasW, HEADER_HEIGHT);

        g.setColor(ACCENT_CYAN);
        g.fillOval(PADDING_X, (HEADER_HEIGHT - 12) / 2, 12, 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT_WHITE);
        g.drawString("Scenario Execution Tree Progression", PADDING_X + 22, (HEADER_HEIGHT / 2) + 5);

        int rightX = canvasW - PADDING_X;
        final int topY = (HEADER_HEIGHT - 22) / 2;

        final String elapsedText = (isNotNull(timer))
                ? "Elapsed: " + formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
        rightX -= drawBadge(g, elapsedText, rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        rightX -= drawBadge(g, totalExecs + " total execs", rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        final Color covBg = (coveredLeafs == totalLeafs && totalLeafs > 0) ? new Color(22, 101, 52)
                : new Color(180, 83, 9);
        rightX -= drawBadge(g, "Leafs: " + coveredLeafs + "/" + totalLeafs, rightX, topY, covBg, TEXT_WHITE) + 10;

        rightX -= drawBadge(g, "Target: " + target, rightX, topY, new Color(51, 65, 85), new Color(203, 213, 225)) + 10;

        drawBadge(g, "Cycle #" + currentCycle, rightX, topY, new Color(2, 132, 199), Color.WHITE);
    }

    private static int drawBadge(
            final Graphics2D g,
            final String text,
            final int rightX,
            final int topY,
            final Color bg,
            final Color fg) {

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        final FontMetrics fm = g.getFontMetrics();
        final int textW = fm.stringWidth(text);
        final int badgeW = textW + 16;
        final int badgeH = 22;
        final int leftX = rightX - badgeW;

        g.setColor(bg);
        g.fillRoundRect(leftX, topY, badgeW, badgeH, 6, 6);
        g.setColor(fg);
        g.drawString(text, leftX + 8, topY + 15);
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
}
