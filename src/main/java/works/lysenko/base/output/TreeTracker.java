package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.Edge;
import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.base.output.sonifier.TreeSonifier;
import works.lysenko.base.output.sonifier.TreeProgressionAudio;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.tree.*;

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
import static works.lysenko.util.prop.tree.Progression.*;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
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
    private static final int HEADER_HEIGHT_BASE = 60;
    private static int headerHeight(int w) { return (int) Math.round(HEADER_HEIGHT_BASE * Math.max(1.0, w / 1200.0)); }
    private static final int PADDING_X = 40;
    private static final int PADDING_Y = 32;
    private static final int BOTTOM_PADDING = 32;

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
    private static final Set<String> lastRecentNodeKeys = new HashSet<>();
    private static volatile Set<_Scenario> cachedAccessibleScenarios = null;
    private static volatile TreeHtml.AccessibleContext cachedAccessibleContext = null;
    private static volatile Map<TreeHtml.NodeData, Boolean> cachedExecutable = null;

    private static volatile int sessionBaseW = 0;
    private static volatile int sessionBaseH = 0;

    private static volatile double currentZoom = 1.0;
    private static volatile double currentTranslateX = 0.0;
    private static volatile double currentTranslateY = 0.0;
    private static volatile boolean transformInitialized = false;

    private static double debugDx = 0.0;
    private static double debugDy = 0.0;
    private static double debugDz = 0.0;
    private static double debugMaxZoom = 1.0;

    private static String lastBadgeText = null;

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
        lastRecentNodeKeys.clear();
        lastBadgeText = null;
        sessionBaseW = 0;
        sessionBaseH = 0;
        currentZoom = 1.0;
        currentTranslateX = 0.0;
        currentTranslateY = 0.0;
        transformInitialized = false;
        invalidateCache();
    }

    /**
     * Performs invalidate cache.
     */
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

        if (sessionBaseW <= 0 || sessionBaseH <= 0 || capturedFrames.isEmpty()) {
            int fullMaxCol = 0;
            double fullMaxRow = 0.0;
            for (final NodeData n : layout.nodes()) {
                if (n.col() > fullMaxCol) fullMaxCol = n.col();
                if (n.row() > fullMaxRow) fullMaxRow = n.row();
            }
            sessionBaseW = (int) evenDimension(Math.max(800L, ((long) fullMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            sessionBaseH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(sessionBaseW) + PADDING_Y + BOTTOM_PADDING)));
        } else {
            int currentMaxCol = 0;
            double currentMaxRow = 0.0;
            for (final NodeData n : layout.nodes()) {
                if (n.col() > currentMaxCol) currentMaxCol = n.col();
                if (n.row() > currentMaxRow) currentMaxRow = n.row();
            }
            final int currW = (int) evenDimension(Math.max(800L, ((long) currentMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            final int currH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((currentMaxRow + 1.0) * ROW_HEIGHT + headerHeight(currW) + PADDING_Y + BOTTOM_PADDING)));
            if (currW > sessionBaseW) sessionBaseW = currW;
            if (currH > sessionBaseH) sessionBaseH = currH;
        }

        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        final String badgeText;
        if (isNotNull(testNumber)) {
            badgeText = "Test #" + testNumber;
            lastBadgeText = badgeText;
        } else if (isNotNull(lastBadgeText)) {
            badgeText = lastBadgeText;
        } else {
            badgeText = "Step #" + (capturedFrames.size() + 1);
            lastBadgeText = badgeText;
        }

        final File runDir = resolveRunDirectory();
        if (isNull(runDir))
            return;

        final BufferedImage image;
        final String svgContent;
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

            final Transform tx = computeSmoothTransform(layout, recentNodeKeys, target, sessionBaseW, sessionBaseH);
            image = renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, tx);
            svgContent = renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, tx);
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
        final File frameFile = new File(progressionDir, String.format(Locale.US, "frame_%04d.svg", frameIndex));
        if (!capturedFrames.contains(frameFile)) {
            capturedFrames.add(frameFile);
        }

        ProgressionFrames.saveFrameSvgAsync(svgContent, frameFile, () -> {
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
        if (!recent.isEmpty()) {
            lastRecentNodeKeys.clear();
            lastRecentNodeKeys.addAll(recent);
            return recent;
        }
        return new HashSet<>(lastRecentNodeKeys);
    }

    private static void updatePreviousExecutions(final TreeLayout layout) {
        for (final NodeData n : layout.nodes()) {
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            previousExecutions.put(n.id(), execs);
        }
    }

    public static synchronized boolean isTransformSettled() {
        if (!transformInitialized || isNull(core) || isNull(core.getResults())) return true;
        final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
        if (sorted.isEmpty()) return true;
        final TreeLayout layout = TreeHtml.computeLayout(sorted);
        if (layout.nodes().isEmpty()) return true;
        final Set<String> recentNodeKeys = computeRecentNodeKeys(layout);
        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        final Transform targetTx = computeZoomTransform(layout, recentNodeKeys, target, sessionBaseW, sessionBaseH);

        final double epsilonZ = 0.005;
        final double epsilonT = 0.5;

        debugDx = targetTx.translateX() - currentTranslateX;
        debugDy = targetTx.translateY() - currentTranslateY;
        debugDz = targetTx.zoom() - currentZoom;

        return Math.abs(debugDz) < epsilonZ &&
               Math.abs(debugDx) < epsilonT &&
               Math.abs(debugDy) < epsilonT;
    }

    /**
     * Triggers the generation of additional cooldown frames at the end of test execution
     * to exhaust any leftover camera movement and zoom momentum (dx, dy, dz impulse)
     * until the view transform settles.
     */
    public static synchronized void triggerCooldownFrames() {
        if (!ProgressionSettings.current().treeEnabled()) return;
        int maxAdditionalFrames = 60;
        while (!isTransformSettled() && maxAdditionalFrames-- > 0) {
            captureFrame(null);
        }
        ProgressionFrames.awaitCompletion();
    }

    /**
     * Hook called upon test completion to compile collected frames into animated
     * GIF, WebP, and MP4.
     */
    public static void onComplete() {
        triggerCooldownFrames();
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

    /**
     * Performs process frames operation.
     */
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

    /**
     * Performs process frames operation.
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels) {
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

    /**
     * Performs process frames operation.
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
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

    /**
     * Performs process frames operation.
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
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

    /**
     * Performs process frames operation.
     */
    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress,
            final boolean enabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
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
        triggerCooldownFrames();
        final String pngTask = PostProcessor.TREE_PNG_TASK;
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
            progress.failed(pngTask, e);
            progress.failed(gifTask, e);
            progress.failed(webpTask, e);
            progress.failed(mp4Task, e);
            return;
        }
        if (framesToProcess.isEmpty()) {
            progress.skipped(pngTask);
            progress.skipped(gifTask);
            progress.skipped(webpTask);
            progress.skipped(mp4Task);
            return;
        }

        final boolean needsRaster = gifEnabled || webpEnabled;
        final File tmpDir = new File(runDir, ".tmp_raster_tree");
        final List<File> rasterizedFrames;
        try {
            if (needsRaster) {
                rasterizedFrames = ProgressionFrames.rasterize(framesToProcess, tmpDir, pngTask, progress, maxFramePixels);
            } else {
                progress.skipped(pngTask);
                rasterizedFrames = framesToProcess;
            }

            if (gifEnabled) {
                final File gifFile = new File(runDir, prefix + ".tree.progression.gif");
                try {
                    ScenarioTracker.writeAnimatedGifFiles(
                            rasterizedFrames,
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
                            rasterizedFrames,
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
                        rasterizedFrames,
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
        } catch (final Exception e) {
            progress.failed(pngTask, e);
        } finally {
            ProgressionFrames.cleanup(tmpDir);
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

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {
        return renderTreeProgressionSvg(layout, target, "Test #" + currentCycle, Collections.emptySet(), getAccessibleScenarios());
    }

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final int currentCycle,
            final Set<String> recentNodeKeys) {
        return renderTreeProgressionSvg(layout, target, "Test #" + currentCycle, recentNodeKeys);
    }

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, getAccessibleScenarios());
    }

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, accessibleScenarios, (null != cachedAccessibleContext) ? cachedAccessibleContext : new TreeHtml.AccessibleContext(accessibleScenarios));
    }

    private record FocusBounds(int minCol, int maxCol, double minRow, double maxRow) {}

    private static FocusBounds computeActiveFocusBounds(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target) {

        final Set<NodeData> focusNodes = new HashSet<>();

        for (final NodeData n : layout.nodes()) {
            if (recentNodeKeys.contains(n.id())) {
                focusNodes.add(n);
                if (null != n.parent()) focusNodes.add(n.parent());
            }
        }

        for (final NodeData n : layout.nodes()) {
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            if (n.children().isEmpty() && isTerminalScenario(n) && execs < getTargetExecutions(n, target)) {
                focusNodes.add(n);
                if (null != n.parent()) focusNodes.add(n.parent());
            }
        }

        if (focusNodes.isEmpty()) {
            focusNodes.addAll(layout.nodes());
        }

        double focusMinRow = Double.MAX_VALUE;
        double focusMaxRow = 0.0;
        int focusMaxCol = 0;

        for (final NodeData n : focusNodes) {
            if (n.col() > focusMaxCol) focusMaxCol = n.col();
            if (n.row() < focusMinRow) focusMinRow = n.row();
            if (n.row() > focusMaxRow) focusMaxRow = n.row();
        }

        if (focusMinRow > focusMaxRow) {
            focusMinRow = 0.0;
            focusMaxRow = 0.0;
        }

        if (focusMaxRow - focusMinRow < 2.0) {
            focusMaxRow = focusMinRow + 2.0;
        }

        return new FocusBounds(0, focusMaxCol, focusMinRow, focusMaxRow);
    }

    record Transform(double zoom, double translateX, double translateY) {}

    static Transform computeZoomTransform(final TreeLayout layout, final Set<String> recentNodeKeys, final int target, final int baseW, final int baseH) {
        final FocusBounds bounds = computeActiveFocusBounds(layout, recentNodeKeys, target);

        final double activeLeft = bounds.minCol() * COL_WIDTH + PADDING_X;
        final double activeRight = bounds.maxCol() * COL_WIDTH + CARD_WIDTH + PADDING_X;
        final double activeTop = bounds.minRow() * ROW_HEIGHT + headerHeight(baseW) + PADDING_Y;
        final double activeBottom = bounds.maxRow() * ROW_HEIGHT + CARD_HEIGHT + headerHeight(baseW) + PADDING_Y;

        final double activeW = Math.max(CARD_WIDTH, activeRight - activeLeft);
        final double activeH = Math.max(CARD_HEIGHT, activeBottom - activeTop);

        final double activeCenterX = (activeLeft + activeRight) / 2.0;
        final double activeCenterY = (activeTop + activeBottom) / 2.0;

        final double availW = baseW - PADDING_X * 2.0;
        final double availH = baseH - headerHeight(baseW) - PADDING_Y - BOTTOM_PADDING;

        final double scaleX = availW / activeW;
        final double scaleY = availH / activeH;
        final double maxSingleNodeZoom = Math.max(1.0, Math.min(availW / CARD_WIDTH, availH / CARD_HEIGHT));
        debugMaxZoom = maxSingleNodeZoom;
        final double zoom = Math.max(1.0, Math.min(Math.min(scaleX, scaleY), maxSingleNodeZoom));

        final double targetCenterX = baseW / 2.0;
        final double targetCenterY = headerHeight(baseW) + PADDING_Y + availH / 2.0;

        final double translateX = targetCenterX - activeCenterX * zoom;
        final double translateY = targetCenterY - activeCenterY * zoom;

        return new Transform(zoom, translateX, translateY);
    }

    static synchronized Transform computeSmoothTransform(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {

        final Transform targetTx = computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);

        debugDx = targetTx.translateX() - currentTranslateX;
        debugDy = targetTx.translateY() - currentTranslateY;
        debugDz = targetTx.zoom() - currentZoom;

        if (!transformInitialized) {
            currentZoom = targetTx.zoom();
            currentTranslateX = targetTx.translateX();
            currentTranslateY = targetTx.translateY();
            transformInitialized = true;
        } else {
            final double alpha = 0.20;
            currentZoom += alpha * debugDz;
            currentTranslateX += alpha * debugDx;
            currentTranslateY += alpha * debugDy;
        }

        return new Transform(currentZoom, currentTranslateX, currentTranslateY);
    }

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, null);
    }

    /**
     * Performs render tree progression svg operation.
     * @return result of render tree progression svg
     */
    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Transform overrideTx) {

        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx) ? accessibleCtx : new TreeHtml.AccessibleContext(accessibleScenarios);
        Map<NodeData, Boolean> execCache = cachedExecutable;
        if (execCache == null) {
            execCache = new HashMap<>();
            cachedExecutable = execCache;
        }
        final Map<NodeData, Boolean> executableCache = execCache;
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

        int totalLeafs = 0;
        int coveredLeafs = 0;
        int totalExecs = 0;
        double maxLeafOverRatio = 0.0;

        int fullMaxCol = 0;
        double fullMaxRow = 0.0;

        final int totalStatsNodes = layout.nodes().size();
        for (int i = 0; i < totalStatsNodes; i++) {
            final NodeData n = layout.nodes().get(i);
            if (n.col() > fullMaxCol) fullMaxCol = n.col();
            if (n.row() > fullMaxRow) fullMaxRow = n.row();
            final int execs = (null != n.result()) ? n.result().getExecutions() : 0;
            totalExecs += execs;
            if (ScenarioType.LEAF == getScenarioType(n)) {
                maxLeafOverRatio = Math.max(maxLeafOverRatio, getLeafOverExecutionRatio(n, target));
            }
            if (n.children().isEmpty() && isTerminalScenario(n)) {
                if (TreeHtml.isTheoreticallyExecutable(n, ctx, executableCache) || execs > 0) {
                    totalLeafs++;
                    if (execs >= getTargetExecutions(n, target)) coveredLeafs++;
                }
            }
        }

        final int baseW;
        final int baseH;
        if (sessionBaseW > 0 && sessionBaseH > 0) {
            baseW = sessionBaseW;
            baseH = sessionBaseH;
        } else {
            baseW = (int) evenDimension(Math.max(800L, ((long) fullMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            baseH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(sessionBaseW) + PADDING_Y + BOTTOM_PADDING)));
        }

        final Transform tx = (null != overrideTx) ? overrideTx : computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        final double zoom = tx.zoom();
        final double translateX = tx.translateX();
        final double translateY = tx.translateY();

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                baseW, baseH, baseW, baseH));
        sb.append("  <defs>\n");
        sb.append("    <pattern id=\"dot-grid\" width=\"24\" height=\"24\" patternUnits=\"userSpaceOnUse\" y=\"60\">\n");
        sb.append("      <rect width=\"24\" height=\"24\" fill=\"#0F172A\" />\n");
        sb.append("      <circle cx=\"1\" cy=\"1\" r=\"1\" fill=\"#1E293B\" opacity=\"0.7\" />\n");
        sb.append("    </pattern>\n");
        sb.append("    <pattern id=\"known-issue-hatch\" width=\"10\" height=\"10\" patternTransform=\"rotate(45 0 0)\" patternUnits=\"userSpaceOnUse\">\n");
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"0\" y2=\"10\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />\n");
        sb.append("      <line x1=\"0\" y1=\"0\" x2=\"10\" y2=\"0\" stroke=\"#fbbf24\" stroke-width=\"2\" stroke-opacity=\"0.45\" />\n");
        sb.append("    </pattern>\n");
        sb.append("    <rect id=\"cb\" width=\"" + CARD_WIDTH + "\" height=\"" + CARD_HEIGHT + "\" rx=\"8\" ry=\"8\" />\n");
        sb.append("    <rect id=\"pt\" x=\"8\" y=\"" + (CARD_HEIGHT - 8) + "\" width=\"" + (CARD_WIDTH - 16) + "\" height=\"4\" fill=\"#0f172a\" opacity=\"0.8\" rx=\"2\" ry=\"2\" />\n");
        sb.append("    <rect id=\"ho\" width=\"" + CARD_WIDTH + "\" height=\"" + CARD_HEIGHT + "\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />\n");
        sb.append("  </defs>\n");
        sb.append("  <style>\n");
        sb.append("    .tx1 { font-family: monospace; font-size: 10px; font-weight: bold; }\n");
        sb.append("    .tx2 { font-size: 12px; }\n");
        sb.append("    .tx3 { font-family: monospace; font-weight: bold; font-size: 11px; }\n");
        sb.append("    .tx4 { font-family: sans-serif; font-weight: bold; font-size: 15px; fill: #F8FAFC; }\n");
        sb.append("    .pb { fill: #0f172a; opacity: 0.8; }\n");
        sb.append("    .edge { fill: none; }\n");
        sb.append("  </style>\n");

        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"#0F172A\" />\n");
        sb.append(String.format(Locale.US,
                "  <rect x=\"0\" y=\"60\" width=\"%d\" height=\"%d\" fill=\"url(#dot-grid)\" />\n",
                baseW, baseH - 60));

        sb.append(String.format(Locale.US,
                "  <g transform=\"translate(%.2f, %.2f) scale(%.4f)\">\n",
                translateX, translateY, zoom));

        final List<Edge> sortedEdges = new ArrayList<>(layout.edges());
        sortedEdges.sort(Comparator.comparingInt(e -> {
            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            return (toExecs > 0 && fromExecs > 0) ? 1 : 0;
        }));

        for (final Edge e : sortedEdges) {
            final int startX = e.from().col() * COL_WIDTH + CARD_WIDTH + PADDING_X;
            final int startY = (int) Math.round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math.round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = (null != e.to().result() && e.to().result().status() == ExecutionStatus.WARNING);
            final boolean isRecent = recentNodeKeys.contains(e.to().id());

            String strokeColor = "#3341558C";
            float strokeW = 1.2f;
            String dash = "";

            if (isRecent) {
                strokeColor = "#38BDF8";
                strokeW = 3.0f;
            } else if (TreeHtml.hasFailure(e.to())) {
                strokeColor = "#EF4444";
                strokeW = 2.2f;
            } else if (TreeHtml.hasChildFailure(e.to(), childFailureCache)) {
                strokeColor = "#818CF8";
                strokeW = 1.2f;
                dash = "stroke-dasharray=\"3 3\" ";
            } else if (toExecs > 0 && fromExecs > 0) {
                if (toWarning) {
                    strokeColor = "#F59E0B";
                } else {
                    final Color col = getScenarioProgressColor(e.to(), target, maxLeafOverRatio);
                    strokeColor = works.lysenko.base.output.tree.TreeTheme.DEFAULT.hex(col);
                }
                strokeW = 2.2f;
            } else if (!isTheoreticallyExecutable(e.to(), accessibleScenarios)) {
                strokeColor = "#24304464";
                strokeW = 1.0f;
                dash = "stroke-dasharray=\"3 3\" ";
            }

            sb.append(String.format(Locale.US,
                    "  <path class=\"edge\" d=\"M %d %d C %d %d, %d %d, %d %d\" stroke=\"%s\" stroke-width=\"%.1f\" %s/>\n",
                    startX, startY, cX, startY, cX, endY, endX, endY, strokeColor, strokeW, dash));
        }

        final boolean isAllLeafs = (null != works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.isAllLeafs() : false;
        final works.lysenko.base.output.tree.VisualNodeResolver nodeResolver =
                new works.lysenko.base.output.tree.VisualNodeResolver(works.lysenko.base.output.tree.TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, true, 0.0, headerHeight(baseW));
        final List<works.lysenko.base.output.tree.VisualNode> visualNodes = new ArrayList<>();
        for (final NodeData n : layout.nodes()) {
            visualNodes.add(nodeResolver.resolve(n, recentNodeKeys.contains(n.id()), childFailureCache, executableCache));
        }
        sb.append(new works.lysenko.base.output.tree.HtmlTreeRenderer(works.lysenko.base.output.tree.TreeTheme.DEFAULT).renderNodes(visualNodes));

        sb.append("  </g>\n");

        final double scale = Math.max(1.0, baseW / 1200.0);
        final int localW = (int) Math.round(baseW / scale);

        sb.append(String.format(Locale.US, "  <g transform=\"scale(%.4f)\">\n", scale));
        sb.append(String.format(Locale.US,
                "    <rect x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"#1E293B\" stroke=\"#334155\" stroke-width=\"1\" />\n",
                localW, HEADER_HEIGHT_BASE));
        sb.append(String.format(Locale.US,
                "    <circle cx=\"%d\" cy=\"%d\" r=\"6\" fill=\"#38BDF8\" />\n",
                PADDING_X + 6, HEADER_HEIGHT_BASE / 2));
        sb.append(String.format(Locale.US,
                "    <text class=\"tx4\" x=\"%d\" y=\"%d\">Scenario Execution Tree Progression</text>\n",
                PADDING_X + 22, (HEADER_HEIGHT_BASE / 2) + 5));

        int rightX = localW - PADDING_X;
        final int topY = (HEADER_HEIGHT_BASE - 22) / 2;

        final String elapsedText = (isNotNull(timer))
                ? "Elapsed: " + formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
        rightX = appendHeaderBadgeSvg(sb, elapsedText, rightX, topY, "#334155", "#F8FAFC");
        rightX = appendHeaderBadgeSvg(sb, totalExecs + " total execs", rightX, topY, "#334155", "#F8FAFC");

        final String zoomStr = String.format(Locale.US, "Zoom: %.1fx", zoom);
        rightX = appendHeaderBadgeSvg(sb, zoomStr, rightX, topY, "#0284C7", "#FFFFFF");

        final String covBg = (coveredLeafs == totalLeafs && totalLeafs > 0) ? "#166534" : "#B45309";
        rightX = appendHeaderBadgeSvg(sb, "Leafs: " + coveredLeafs + "/" + totalLeafs, rightX, topY, covBg, "#F8FAFC");
        rightX = appendHeaderBadgeSvg(sb, "Target: " + target, rightX, topY, "#334155", "#CBD5E1");
        appendHeaderBadgeSvg(sb, badgeText, rightX, topY, "#0284C7", "#FFFFFF");

        if (Boolean.TRUE.equals(debug)) {
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.1fx", debugDx, debugDy, debugDz, debugMaxZoom);
            sb.append(String.format(Locale.US, "  <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"#64748B\">%s</text>\n", PADDING_X, HEADER_HEIGHT_BASE - 5, debugStr));

            // The debug movement indicator is drawn relative to the center of the viewport
            // but reversed since the transform shifts the *camera* not the *object*.
            final double ox = localW / 2.0;
            final double oy = (baseH / scale) / 2.0;
            sb.append(String.format(Locale.US, "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#F59E0B\" stroke-width=\"2\" />\n", ox, oy, ox - debugDx, oy - debugDy));
            sb.append(String.format(Locale.US, "  <circle cx=\"%.1f\" cy=\"%.1f\" r=\"3\" fill=\"#F59E0B\" />\n", ox, oy));
            sb.append(String.format(Locale.US, "  <circle cx=\"%.1f\" cy=\"%.1f\" r=\"2\" fill=\"#F8FAFC\" />\n", ox - debugDx, oy - debugDy));
        }

        sb.append("  </g>\n");

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static int appendHeaderBadgeSvg(
            final StringBuilder sb,
            final String text,
            final int rightX,
            final int topY,
            final String bg,
            final String fg) {
        final int badgeW = text.length() * 7 + 16;
        final int leftX = rightX - badgeW;
        sb.append(String.format(Locale.US,
                "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"22\" rx=\"6\" ry=\"6\" fill=\"%s\" />\n" +
                "  <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"%s\">%s</text>\n",
                leftX, topY, badgeW, bg, leftX + 8, topY + 15, fg, text));
        return leftX - 10;
    }

    /**
     * Performs render tree progression operation.
     * @return result of render tree progression
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {

        return renderTreeProgression(layout, target, "Test #" + currentCycle, Collections.emptySet(), getAccessibleScenarios());
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

        return renderTreeProgression(layout, target, "Test #" + currentCycle, recentNodeKeys);
    }

    /**
     * Renders scenario tree progression layout as a BufferedImage with a custom badge label,
     * highlighting recently changed nodes.
     *
     * @param layout         computed tree layout
     * @param target         target executions count
     * @param badgeText      text for the frame badge (e.g. "Test #1", "Step #42")
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
        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, null);
    }

    /**
     * Performs render tree progression operation.
     * @return result of render tree progression
     */
    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Transform overrideTx) {

        final long tRenderStart = System.currentTimeMillis();
        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx) ? accessibleCtx : new TreeHtml.AccessibleContext(accessibleScenarios);
        
        Map<NodeData, Boolean> execCache = cachedExecutable;
        if (execCache == null) {
            execCache = new HashMap<>();
            cachedExecutable = execCache;
        }
        final Map<NodeData, Boolean> executableCache = execCache;
        
        final Map<NodeData, Boolean> childFailureCache = new HashMap<>();

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

        final int baseW;
        final int baseH;
        if (sessionBaseW > 0 && sessionBaseH > 0) {
            baseW = sessionBaseW;
            baseH = sessionBaseH;
        } else {
            int fullMaxCol = 0;
            double fullMaxRow = 0.0;
            for (final NodeData n : layout.nodes()) {
                if (n.col() > fullMaxCol) fullMaxCol = n.col();
                if (n.row() > fullMaxRow) fullMaxRow = n.row();
            }
            baseW = (int) evenDimension(Math.max(800L, ((long) fullMaxCol + 1L) * COL_WIDTH + PADDING_X * 2L));
            baseH = (int) evenDimension(Math.max(
                    400L,
                    (long) Math.ceil((fullMaxRow + 1.0) * ROW_HEIGHT + headerHeight(sessionBaseW) + PADDING_Y + BOTTOM_PADDING)));
        }

        final Transform tx = (null != overrideTx) ? overrideTx : computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        final double zoom = tx.zoom();
        final double translateX = tx.translateX();
        final double translateY = tx.translateY();

        final long maxPixels = ProgressionFrames.maxFramePixels(ProgressionSettings.current().maxFramePixels());
        if (baseW > ProgressionFrames.MAX_DIMENSION
                || baseH > ProgressionFrames.MAX_DIMENSION
                || (long) baseW * baseH > maxPixels) {
            throw new IllegalArgumentException("Rendered tree frame would exceed the configured limit ("
                    + baseW + "x" + baseH + ", max pixels " + maxPixels + ")");
        }

        final BufferedImage img = new BufferedImage(baseW, baseH, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, baseW, baseH);

        java.awt.TexturePaint dotPaint = cachedDotPaint;
        if (dotPaint == null) {
            final BufferedImage dotTile = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
            final Graphics2D tileG = dotTile.createGraphics();
            tileG.setColor(BG_COLOR);
            tileG.fillRect(0, 0, 24, 24);
            tileG.setColor(GRID_DOT);
            tileG.fillRect(0, 0, 1, 1);
            tileG.dispose();

            dotPaint = new java.awt.TexturePaint(dotTile,
                    new java.awt.geom.Rectangle2D.Double(0, headerHeight(baseW), 24, 24));
            cachedDotPaint = dotPaint;
        }
        g.setPaint(dotPaint);
        g.fillRect(0, headerHeight(baseW), baseW, baseH - headerHeight(baseW));

        final long tInit = System.currentTimeMillis() - tRenderStart;

        // Apply zoom transform for edges & nodes
        final java.awt.geom.AffineTransform origTx = g.getTransform();
        g.translate(translateX, translateY);
        g.scale(zoom, zoom);

        // Draw edges
        final long tEdgeStart = System.currentTimeMillis();
        drawEdges(g, layout.edges(), target, maxLeafOverRatio, recentNodeKeys, accessibleScenarios, childFailureCache, baseW);
        final long tEdges = System.currentTimeMillis() - tEdgeStart;

        // Draw nodes
        final long tNodeStart = System.currentTimeMillis();
        final boolean isAllLeafs = (null != works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.isAllLeafs() : false;
        final works.lysenko.base.output.tree.VisualNodeResolver nodeResolver =
                new works.lysenko.base.output.tree.VisualNodeResolver(works.lysenko.base.output.tree.TreeTheme.DEFAULT, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, true, 0.0, headerHeight(baseW));
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

        // Restore transform for header
        g.setTransform(origTx);

        // Draw header
        drawHeader(g, baseW, badgeText, target, coveredLeafs, totalLeafs, totalExecs, zoom);

        g.dispose();
        final long tTotal = System.currentTimeMillis() - tRenderStart;
        logDebug(String.format("Canvas (%dx%d) in %d ms (init: %d ms, edges: %d ms, nodes: %d ms)",
                baseW, baseH, tTotal, tInit, tEdges, tNodes));
        return img;
    }

    private static void drawEdges(
            final Graphics2D g,
            final List<Edge> edges,
            final int target,
            final double maxLeafOverRatio,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final Map<NodeData, Boolean> childFailureCache,
            final int baseW) {

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
                    .round(e.from().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int endX = e.to().col() * COL_WIDTH + PADDING_X;
            final int endY = (int) Math
                    .round(e.to().row() * ROW_HEIGHT + (CARD_HEIGHT / 2.0) + headerHeight(baseW) + PADDING_Y);
            final int cX = (startX + endX) / 2;

            final int toExecs = (null != e.to().result()) ? e.to().result().getExecutions() : 0;
            final int fromExecs = (null != e.from().result()) ? e.from().result().getExecutions() : 0;
            final boolean toWarning = (null != e.to().result() && e.to().result().status() == ExecutionStatus.WARNING);

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
                if (toWarning) {
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
            final int totalExecs,
            final double zoom) {

        final double scale = Math.max(1.0, canvasW / 1200.0);
        final java.awt.geom.AffineTransform origTx = g.getTransform();
        g.scale(scale, scale);
        final int localCanvasW = (int) Math.round(canvasW / scale);

        g.setColor(HEADER_BG);
        g.fillRect(0, 0, localCanvasW, HEADER_HEIGHT_BASE);
        g.setColor(HEADER_BORDER);
        g.drawLine(0, HEADER_HEIGHT_BASE, localCanvasW, HEADER_HEIGHT_BASE);

        g.setColor(ACCENT_CYAN);
        g.fillOval(PADDING_X, (HEADER_HEIGHT_BASE - 12) / 2, 12, 12);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT_WHITE);
        g.drawString("Scenario Execution Tree Progression", PADDING_X + 22, (HEADER_HEIGHT_BASE / 2) + 5);

        int rightX = localCanvasW - PADDING_X;
        final int topY = (HEADER_HEIGHT_BASE - 22) / 2;

        final String elapsedText = (isNotNull(timer))
                ? "Elapsed: " + formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
        rightX -= drawBadge(g, elapsedText, rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        rightX -= drawBadge(g, totalExecs + " total execs", rightX, topY, new Color(51, 65, 85), TEXT_WHITE) + 10;

        final String zoomStr = String.format(Locale.US, "Zoom: %.1fx", zoom);
        rightX -= drawBadge(g, zoomStr, rightX, topY, new Color(2, 132, 199), Color.WHITE) + 10;

        final Color covBg = (coveredLeafs == totalLeafs && totalLeafs > 0) ? new Color(22, 101, 52)
                : new Color(180, 83, 9);
        rightX -= drawBadge(g, "Leafs: " + coveredLeafs + "/" + totalLeafs, rightX, topY, covBg, TEXT_WHITE) + 10;

        rightX -= drawBadge(g, "Target: " + target, rightX, topY, new Color(51, 65, 85), new Color(203, 213, 225)) + 10;

        drawBadge(g, badgeText, rightX, topY, new Color(2, 132, 199), Color.WHITE);

        if (debug) {
            final String debugStr = String.format(Locale.US, "dx:%.1f dy:%.1f dz:%.3f maxZ:%.1fx", debugDx, debugDy, debugDz, debugMaxZoom);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
            g.setColor(new Color(100, 116, 139));
            g.drawString(debugStr, PADDING_X, HEADER_HEIGHT_BASE - 5);

            final double ox = localCanvasW / 2.0;
            final double oy = (canvasW / scale) / 2.0; // Approximation since we don't pass baseH
            g.setColor(new Color(245, 158, 11));
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine((int) ox, (int) oy, (int) (ox - debugDx), (int) (oy - debugDy));
            g.fillOval((int) (ox - 3), (int) (oy - 3), 6, 6);
            g.setColor(new Color(248, 250, 252));
            g.fillOval((int) (ox - debugDx - 2), (int) (oy - debugDy - 2), 4, 4);
        }

        g.setTransform(origTx);
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
        return new File(_RUNS_);
    }
}
