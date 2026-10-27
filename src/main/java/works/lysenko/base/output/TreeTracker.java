package works.lysenko.base.output;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.base.output.TreeHtml.TreeLayout;
import works.lysenko.base.output.sonifier.TreeProgressionAudio;
import works.lysenko.base.output.sonifier.TreeSonifier;
import works.lysenko.base.output.tree.SvgTreeRenderer;
import works.lysenko.base.output.tree.TreeCameraTransform;
import works.lysenko.base.output.tree.TreeImageRenderer;
import works.lysenko.base.output.tree.TreeProgressMetrics;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.prop.PropEnum;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.log;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Paths._RUNS_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;

/**
 * Tracks scenario tree progression across test cycles during limbo periods,
 * rendering the scenario hierarchy into PNG frames with visual execution
 * indicators,
 * and compiling them into an animated GIF upon test completion.
 */
@SuppressWarnings({ "ClassWithoutLogger", "MagicNumber", "ClassWithTooManyFields" })
public final class TreeTracker {

    private static final int COL_WIDTH = 270;
    private static final int ROW_HEIGHT = 60;
    private static final int HEADER_HEIGHT_BASE = 60;

    private static int headerHeight(final int w) {
        return (int) Math.round(HEADER_HEIGHT_BASE * Math.max(1.0, w / 1200.0));
    }

    private static final int PADDING_X = 40;
    private static final int PADDING_Y = 32;
    private static final int BOTTOM_PADDING = 32;

    private static final int FPS = 10;
    private static final int DELAY_CENTISECONDS = 100 / FPS; // 100ms
    private static final int FINAL_FRAME_DELAY_CENTISECONDS = 150; // 1.5s on final frame

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

    private static String lastBadgeText = null;

    private static volatile TexturePaint cachedDotPaint = null;

    public record Transform(double zoom, double translateX, double translateY) {
        public static Transform from(final TreeCameraTransform.Transform tx) {
            return (null == tx) ? null : new Transform(tx.zoom(), tx.translateX(), tx.translateY());
        }

        public TreeCameraTransform.Transform toCamera() {
            return new TreeCameraTransform.Transform(zoom, translateX, translateY);
        }
    }

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
        TreeCameraTransform.reset();
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

    public static Color getProgressColor(final int execs, final int target) {
        return TreeProgressMetrics.getProgressColor(execs, target);
    }

    static int getTargetExecutions(final NodeData node, final int leafTarget) {
        return TreeProgressMetrics.getTargetExecutions(node, leafTarget);
    }

    static int getOverExecutionPercent(final int execs, final int target) {
        return TreeProgressMetrics.getOverExecutionPercent(execs, target);
    }

    static Color getOverExecutionColor(final double overRatio, final double maxOverRatio) {
        return TreeProgressMetrics.getOverExecutionColor(overRatio, maxOverRatio);
    }

    static String getExecutionBadgeText(final NodeData node, final int execs, final int leafTarget) {
        return TreeProgressMetrics.getExecutionBadgeText(node, execs, leafTarget);
    }

    public static String formatElapsedTime(final long elapsedMillis) {
        return TreeProgressMetrics.formatElapsedTime(elapsedMillis);
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

        updateSessionDimensions(layout);

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

            final TreeCameraTransform.Transform cameraTx = TreeCameraTransform.computeSmoothTransform(layout, recentNodeKeys, target, sessionBaseW, sessionBaseH);
            image = TreeImageRenderer.renderTreeProgression(
                    layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, cameraTx, cachedExecutable, sessionBaseW, sessionBaseH, cachedDotPaint, paint -> cachedDotPaint = paint);
            svgContent = SvgTreeRenderer.renderTreeProgressionSvg(
                    layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, cameraTx, cachedExecutable, sessionBaseW, sessionBaseH);
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

    private static void updateSessionDimensions(final TreeLayout layout) {
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
        if (!TreeCameraTransform.isInitialized() || isNull(core) || isNull(core.getResults())) return true;
        final TreeMap<String, Result> sorted = core.getResults().getSortedStrings(false);
        if (sorted.isEmpty()) return true;
        final TreeLayout layout = TreeHtml.computeLayout(sorted);
        if (layout.nodes().isEmpty()) return true;
        final Set<String> recentNodeKeys = computeRecentNodeKeys(layout);
        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        return TreeCameraTransform.isTransformSettled(layout, recentNodeKeys, target, sessionBaseW, sessionBaseH);
    }

    /**
     * Triggers the generation of additional cooldown frames at the end of test execution
     * to exhaust any leftover camera movement and zoom momentum until the view transform settles.
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

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress) {
        processFrames(runDir, prefix, progress, ProgressionSettings.current().treeEnabled());
    }

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
        if (!enabled || isNull(runDir)) {
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

    static Set<_Scenario> getAccessibleScenarios() {
        Set<_Scenario> scenarios = cachedAccessibleScenarios;
        if (null == scenarios) {
            scenarios = TreeHtml.getAccessibleScenarios();
            cachedAccessibleScenarios = scenarios;
            cachedAccessibleContext = new TreeHtml.AccessibleContext(scenarios);
        }
        return scenarios;
    }

    static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios) {
        return TreeHtml.isTheoreticallyExecutable(n, accessibleScenarios);
    }

    static boolean isTheoreticallyExecutable(final NodeData n, final Set<_Scenario> accessibleScenarios, final Map<NodeData, Boolean> cache) {
        return TreeHtml.isTheoreticallyExecutable(n, accessibleScenarios, cache);
    }

    static boolean isTheoreticallyExecutable(final NodeData n) {
        return TreeHtml.isTheoreticallyExecutable(n);
    }

    public static Transform computeZoomTransform(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {
        final TreeCameraTransform.Transform tx = TreeCameraTransform.computeZoomTransform(layout, recentNodeKeys, target, baseW, baseH);
        return Transform.from(tx);
    }

    public static Transform computeSmoothTransform(
            final TreeLayout layout,
            final Set<String> recentNodeKeys,
            final int target,
            final int baseW,
            final int baseH) {
        final TreeCameraTransform.Transform tx = TreeCameraTransform.computeSmoothTransform(layout, recentNodeKeys, target, baseW, baseH);
        return Transform.from(tx);
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {
        return renderTreeProgressionSvg(layout, target, "Test #" + currentCycle, Collections.emptySet(), getAccessibleScenarios());
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final int currentCycle,
            final Set<String> recentNodeKeys) {
        return renderTreeProgressionSvg(layout, target, "Test #" + currentCycle, recentNodeKeys);
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, getAccessibleScenarios());
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, accessibleScenarios, (null != cachedAccessibleContext) ? cachedAccessibleContext : new TreeHtml.AccessibleContext(accessibleScenarios));
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx) {
        return renderTreeProgressionSvg(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, null);
    }

    public static String renderTreeProgressionSvg(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Transform overrideTx) {

        final TreeCameraTransform.Transform cameraTx = (null != overrideTx) ? overrideTx.toCamera() : null;
        if (null == cachedExecutable) {
            cachedExecutable = new HashMap<>();
        }
        return SvgTreeRenderer.renderTreeProgressionSvg(
                layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, cameraTx, cachedExecutable, sessionBaseW, sessionBaseH);
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle) {
        return renderTreeProgression(layout, target, "Test #" + currentCycle, Collections.emptySet(), getAccessibleScenarios());
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final int currentCycle,
            final Set<String> recentNodeKeys) {
        return renderTreeProgression(layout, target, "Test #" + currentCycle, recentNodeKeys);
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys) {
        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, getAccessibleScenarios());
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios) {
        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, (null != cachedAccessibleContext) ? cachedAccessibleContext : new TreeHtml.AccessibleContext(accessibleScenarios));
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx) {
        return renderTreeProgression(layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, null);
    }

    public static BufferedImage renderTreeProgression(
            final TreeLayout layout,
            final int target,
            final String badgeText,
            final Set<String> recentNodeKeys,
            final Set<_Scenario> accessibleScenarios,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Transform overrideTx) {

        final TreeCameraTransform.Transform cameraTx = (null != overrideTx) ? overrideTx.toCamera() : null;
        if (null == cachedExecutable) {
            cachedExecutable = new HashMap<>();
        }
        return TreeImageRenderer.renderTreeProgression(
                layout, target, badgeText, recentNodeKeys, accessibleScenarios, accessibleCtx, cameraTx, cachedExecutable, sessionBaseW, sessionBaseH, cachedDotPaint, paint -> cachedDotPaint = paint);
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

    private static long evenDimension(final long dimension) {
        if (dimension >= 16384L)
            return dimension;
        return (dimension & 1L) == 0L ? dimension : dimension + 1L;
    }
}
