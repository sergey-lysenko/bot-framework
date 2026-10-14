package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.base.output.sonifier.TreeSonifier;
import works.lysenko.base.output.sonifier.TreeProgressionAudio;
import works.lysenko.util.prop.PropEnum;
import java.awt.Stroke;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
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
    private static final Color RED_FAILED = new Color(0xEF, 0x44, 0x44);
    private static final Color RED_CARD_FILL = new Color(0x45, 0x0A, 0x0A);
    private static final Color GREEN_DONE = new Color(0x22, 0xC5, 0x5E);
    private static final Color TEXT_DARK = new Color(0x0F, 0x17, 0x2A);
    static final Color INDIGO_UPSET = new Color(0x81, 0x8C, 0xF8);
    static final Color UPSET_CARD_FILL = new Color(0x1E, 0x1B, 0x4B);
    static final Color EXCLUDED_CARD = new Color(0x13, 0x1A, 0x2A);
    static final Color EXCLUDED_BORDER = new Color(0x28, 0x35, 0x48);
    static final Color EXCLUDED_EDGE = new Color(0x24, 0x30, 0x44, 100);
    static final Color TEXT_EXCLUDED = new Color(0x47, 0x55, 0x69);
    static final Stroke DASHED_BORDER_STROKE = new BasicStroke(
            1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 4.0f}, 0.0f);
    static final Stroke DASHED_EDGE_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
    static final Stroke DASHED_UPSET_STROKE = new BasicStroke(
            1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);

    private static final List<File> capturedFrames = new ArrayList<>();
    private static File customOutputDir = null;
    private static volatile BufferedImage latestFrameImage = null;
    private static final Map<String, Integer> previousExecutions = new HashMap<>();
    private static volatile Set<_Scenario> cachedAccessibleScenarios = null;
    private static volatile TreeHtml.AccessibleContext cachedAccessibleContext = null;
    private static volatile Map<TreeHtml.NodeData, Boolean> cachedExecutable = null;

    private static volatile java.awt.TexturePaint cachedDotPaint = null;

    private TreeTracker() {
    }

    /**
     * Resets internal state for tests.
     */
    public static void reset() {
        ProgressionFrames.awaitCompletion();
        capturedFrames.clear();
        customOutputDir = null;
        latestFrameImage = null;
        previousExecutions.clear();
        invalidateCache();
    }

    public static void invalidateCache() {
        cachedAccessibleScenarios = null;
        cachedAccessibleContext = null;
        cachedExecutable = null;
        cachedDotPaint = null;
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
        ProgressionFrames.awaitCompletion();
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
        final boolean isAllLeafs = (null != works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.isAllLeafs() : (null != PropEnum._TEST_ALL_LEAFS.get() && (Boolean) PropEnum._TEST_ALL_LEAFS.get());
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.NODE == type || ScenarioType.CORE == type || !isAllLeafs) {
            return execs + ((1 == execs) ? " exec" : " execs");
        }

        final int target = getTargetExecutions(node, leafTarget);
        if (execs > target && ScenarioType.LEAF == type) {
            return execs + "/" + target + " +" + getOverExecutionPercent(execs, target) + "%";
        }
        if (execs >= target && 0 < execs) {
            return "✓ " + execs + "/" + target;
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
        captureFrame(testNumber);
    }

    /**
     * Hook called whenever a scenario node is executed.
     */
    public static void onNode() {
        if (!ProgressionSettings.current().treeEnabled() || !ProgressionSettings.current().treePerNodeEnabled())
            return;
        captureFrame(null);
    }

    private static synchronized void captureFrame(final Integer testNumber) {

        logDebug("captureFrame start");

        if (isNull(core) || isNull(core.getResults()))
            return;
        final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
        if (sorted.isEmpty())
            return;
        final long t0 = System.currentTimeMillis();
        final TreeLayout layout = TreeHtml.computeLayout(sorted);
        if (layout.nodes().isEmpty())
            return;
        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsCount()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_COUNT.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_COUNT.get()) : 1);
        final String badgeText = isNotNull(testNumber)
                ? "Cycle #" + testNumber
                : "Step #" + (capturedFrames.size() + 1);

        final File runDir = resolveRunDirectory();
        if (isNull(runDir))
            return;

        final BufferedImage image;
        try {
            final Set<String> recentNodeKeys = computeRecentNodeKeys(layout);
            logDebug(String.format("Canvas (%d nodes, %d edges)...", layout.nodes().size(), layout.edges().size()));
            final boolean isCold = (null == cachedAccessibleScenarios);
            final long tAccStart = System.currentTimeMillis();
            if (isCold) {
                logDebug("Collecting accessible scenarios...");
            }
            final Set<_Scenario> accessibleScenarios = getAccessibleScenarios();
            final TreeHtml.AccessibleContext accessibleCtx = cachedAccessibleContext;
            if (isCold) {
                logDebug(String.format("Collected %d accessible scenarios in %d ms", accessibleScenarios.size(), System.currentTimeMillis() - tAccStart));
            }

            image = renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx);
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

        final int frameIndex = capturedFrames.size() + 1;
        final File frameFile = new File(progressionDir, String.format(Locale.US, "frame_%04d.png", frameIndex));
        if (!capturedFrames.contains(frameFile)) {
            capturedFrames.add(frameFile);
        }

        ProgressionFrames.saveFrameAsync(image, frameFile, () -> {
            if (ProgressionSettings.current().treeMp4Enabled()) {
                final java.util.Optional<TreeSonifier> sonifier = ProgressionSettings.current().treeSonifier();
                if (sonifier.isPresent()) {
                    try {
                        sonifier.get().capture(frameFile, layout, target);
                    } catch (final IOException e) {
                        System.err.println("Failed tree sonification: " + e.getMessage());
                    }
                }
            }
            final long duration = System.currentTimeMillis() - t0;
            logDebug(String.format("Frame #%d (%dx%d) in %d ms", frameIndex, image.getWidth(), image.getHeight(), duration));
        });
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
        ProgressionFrames.awaitCompletion();
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
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                ProgressionSettings.current().treeGifEnabled(),
                ProgressionSettings.current().treeWebpEnabled(),
                mp4Enabled,
                ffmpeg,
                treeSonification);
    }

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final int maxTotalPixels,
            final boolean gifEnabled,
            final boolean webpEnabled,
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

        if (gifEnabled) {
            final File gifFile = new File(runDir, prefix + ".tree.progression.gif");
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
        } else {
            progress.skipped(gifTask);
        }

        if (webpEnabled) {
            final File webpFile = new File(runDir, prefix + ".tree.progression.webp");
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
        } else {
            progress.skipped(webpTask);
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
    static Set<_Scenario> getAccessibleScenarios() {
        Set<_Scenario> scenarios = cachedAccessibleScenarios;
        if (null == scenarios) {
            scenarios = TreeHtml.getAccessibleScenarios();
            cachedAccessibleScenarios = scenarios;
            cachedAccessibleContext = new TreeHtml.AccessibleContext(scenarios);
        }
        return scenarios;
    }

    /**
     * Determines whether a scenario node is theoretically to be executed during the current test session.
     *
     * @param n                   node to evaluate
     * @param accessibleScenarios set of scenarios accessible in the current session
     * @return true if theoretically executable, false if excluded by configuration
     */
    static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios) {
        return TreeHtml.isTheoreticallyExecutable(n, accessibleScenarios);
    }

    static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios, final Map<NodeData, Boolean> cache) {
        return TreeHtml.isTheoreticallyExecutable(n, accessibleScenarios, cache);
    }

    static boolean isTheoreticallyExecutable(final NodeData n) {
        return TreeHtml.isTheoreticallyExecutable(n);
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {

        return renderTreeProgression(layout, target, "Cycle #" + currentCycle, Collections.emptySet(), getAccessibleScenarios());
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

        return renderTreeProgression(layout, target, "Cycle #" + currentCycle, recentNodeKeys);
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage with a custom badge label,
     * highlighting recently changed nodes.
     *
     * @param layout         computed tree layout
     * @param target         target executions count
     * @param badgeText      text for the frame badge (e.g. "Cycle #1", "Step #42")
     * @param recentNodeKeys set of node keys recently traversed or changed
     * @return rendered BufferedImage
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys) {

        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, getAccessibleScenarios());
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage with a custom badge label,
     * highlighting recently changed nodes and differentiating executable vs excluded nodes.
     *
     * @param layout              computed tree layout
     * @param target              target executions count
     * @param badgeText           text for the frame badge (e.g. "Cycle #1", "Step #42")
     * @param recentNodeKeys      set of node keys recently traversed or changed
     * @param accessibleScenarios set of scenarios accessible in the current session
     * @return rendered BufferedImage
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios) {

        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, (null != cachedAccessibleContext) ? cachedAccessibleContext : new TreeHtml.AccessibleContext(accessibleScenarios));
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage with a custom badge label,
     * highlighting recently changed nodes and differentiating executable vs excluded nodes.
     *
     * @param layout              computed tree layout
     * @param target              target executions count
     * @param badgeText           text for the frame badge (e.g. "Cycle #1", "Step #42")
     * @param recentNodeKeys      set of node keys recently traversed or changed
     * @param accessibleScenarios set of scenarios accessible in the current session
     * @param accessibleCtx       cached accessible scenarios context
     * @return rendered BufferedImage
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx) {

        final long tRenderStart = System.currentTimeMillis();
        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx) ? accessibleCtx : new TreeHtml.AccessibleContext(accessibleScenarios);
        
        Map<NodeData, Boolean> execCache = cachedExecutable;
        if (execCache == null) {
            execCache = new HashMap<>();
            cachedExecutable = execCache;
        }
        final Map<NodeData, Boolean> executableCache = execCache;
        
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

        int maxCol = 0;
        double maxRow = 0.0;
        int totalLeafs = 0;
        int coveredLeafs = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        final int totalStatsNodes = layout.nodes().size();
        final Set<Integer> statsMilestones = new HashSet<>();
        if (totalStatsNodes > 0) {
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.25)));
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.50)));
            statsMilestones.add(Math.max(1, (int) Math.round(totalStatsNodes * 0.75)));
            statsMilestones.add(totalStatsNodes);
        }

        for (int i = 0; i < totalStatsNodes; i++) {
            final NodeData n = layout.nodes().get(i);
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
                if (TreeHtml.isTheoreticallyExecutable(n, ctx, executableCache) || execs > 0) {
                    totalLeafs++;
                    if (execs >= getTargetExecutions(n, target))
                        coveredLeafs++;
                }
            }
            final int current = i + 1;
            if (statsMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalStatsNodes);
                logTrace(String.format("stats: %d/%d (%d%%)", current, totalStatsNodes, percent));
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

        java.awt.TexturePaint dotPaint = cachedDotPaint;
        if (dotPaint == null) {
            // Dot grid pattern using TexturePaint tile
            final BufferedImage dotTile = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
            final Graphics2D tileG = dotTile.createGraphics();
            tileG.setColor(BG_COLOR);
            tileG.fillRect(0, 0, 24, 24);
            tileG.setColor(GRID_DOT);
            tileG.fillRect(0, 0, 1, 1);
            tileG.dispose();

            dotPaint = new java.awt.TexturePaint(dotTile,
                    new java.awt.geom.Rectangle2D.Double(0, HEADER_HEIGHT, 24, 24));
            cachedDotPaint = dotPaint;
        }
        g.setPaint(dotPaint);
        g.fillRect(0, HEADER_HEIGHT, canvasW, canvasH - HEADER_HEIGHT);

        final long tInit = System.currentTimeMillis() - tRenderStart;

        // Draw edges
        final long tEdgeStart = System.currentTimeMillis();
        drawEdges(g, layout.edges(), target, maxLeafOverRatio, recentNodeKeys, accessibleScenarios, childFailureCache);
        final long tEdges = System.currentTimeMillis() - tEdgeStart;

        // Draw nodes
        final long tNodeStart = System.currentTimeMillis();
        final boolean isAllLeafs = (null != works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.isAllLeafs() : false;
        final works.lysenko.base.output.tree.VisualNodeResolver nodeResolver =
                new works.lysenko.base.output.tree.VisualNodeResolver(works.lysenko.base.output.tree.TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, true);
        final List<works.lysenko.base.output.tree.VisualNode> visualNodes = new ArrayList<>();
        final int totalNodes = layout.nodes().size();
        final Set<Integer> nodeMilestones = new HashSet<>();
        if (totalNodes > 0) {
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.25)));
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.50)));
            nodeMilestones.add(Math.max(1, (int) Math.round(totalNodes * 0.75)));
            nodeMilestones.add(totalNodes);
        }

        for (int i = 0; i < totalNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
            final int current = i + 1;
            if (nodeMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalNodes);
                logTrace(String.format("nodes: %d/%d (%d%%)", current, totalNodes, percent));
            }
        }

        new works.lysenko.base.output.tree.GraphicsTreeRenderer(works.lysenko.base.output.tree.TreeTheme.DEFAULT).renderNodes(g, visualNodes);
        final long tNodes = System.currentTimeMillis() - tNodeStart;

        // Draw header
        drawHeader(g, canvasW, badgeText, target, coveredLeafs, totalLeafs, totalExecs);

        g.dispose();
        final long tTotal = System.currentTimeMillis() - tRenderStart;
        logDebug(String.format("Canvas (%dx%d) in %d ms (init: %d ms, edges: %d ms, nodes: %d ms)",
                canvasW, canvasH, tTotal, tInit, tEdges, tNodes));
        return img;
    }

    private static void drawEdges(
            final Graphics2D g,
            final List<Edge> edges,
            final int target,
            final double maxLeafOverRatio,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final Map<NodeData, Boolean> childFailureCache) {

        final List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        final int totalEdges = sortedEdges.size();
        final Set<Integer> edgeMilestones = new HashSet<>();
        if (totalEdges > 0) {
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.25)));
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.50)));
            edgeMilestones.add(Math.max(1, (int) Math.round(totalEdges * 0.75)));
            edgeMilestones.add(totalEdges);
        }

        for (int i = 0; i < totalEdges; i++) {
            final Edge e = sortedEdges.get(i);
            final int current = i + 1;
            if (edgeMilestones.contains(current)) {
                final int percent = (int) Math.round((current * 100.0) / totalEdges);
                logTrace(String.format("edges: %d/%d (%d%%)", current, totalEdges, percent));
            }
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
            } else if (TreeHtml.hasFailure(e.to())) {
                g.setColor(RED_FAILED);
                g.setStroke(new BasicStroke(2.2f));
                g.draw(path);
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                g.setColor(INDIGO_UPSET);
                g.setStroke(DASHED_UPSET_STROKE);
                g.draw(path);
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toEvents > 0) {
                    g.setColor(WARNING_AMBER);
                } else {
                    g.setColor(getScenarioProgressColor(e.to(), target, maxLeafOverRatio));
                }
                g.setStroke(new BasicStroke(2.2f));
                g.draw(path);
            } else if (!isTheoreticallyExecutable(e.to(), accessibleScenarios)) {
                g.setColor(EXCLUDED_EDGE);
                g.setStroke(DASHED_EDGE_STROKE);
                g.draw(path);
            } else {
                g.setColor(UNVISITED_EDGE);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(path);
            }
        }
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
        final boolean isAllLeafs = (null != works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.isAllLeafs() : (null != PropEnum._TEST_ALL_LEAFS.get() && (Boolean) PropEnum._TEST_ALL_LEAFS.get());
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.LEAF == type && isAllLeafs) {
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
            final String badgeText,
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

        drawBadge(g, badgeText, rightX, topY, new Color(2, 132, 199), Color.WHITE);
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
