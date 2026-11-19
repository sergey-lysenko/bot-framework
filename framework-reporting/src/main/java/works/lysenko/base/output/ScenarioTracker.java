package works.lysenko.base.output;

import works.lysenko.base.output.scenario.AnimatedGifWriter;
import works.lysenko.base.output.scenario.ScenarioGraphSvgRenderer;
import works.lysenko.base.output.svg.ProgressionChrome;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.PropEnum;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

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
 * Tracks scenario execution progression across test cycles during limbo periods,
 * generating visual status charts and compiling them into animated GIFs, WebP, and MP4.
 */
public final class ScenarioTracker {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 680;
    private static final int FPS = 10;
    private static final int DELAY_CENTISECONDS = 100 / FPS; // 100ms
    private static final int FINAL_FRAME_DELAY_CENTISECONDS = 150; // 1.5s on final frame

    private static final List<File> capturedFrames = new ArrayList<>();
    private static final Map<String, Integer> previousExecutions = new HashMap<>();
    private static final Set<String> lastRecentScenarioKeys = new HashSet<>();
    private static File customOutputDir = null;

    private ScenarioTracker() {
    }

    public static void setCustomOutputDir(final File dir) {
        customOutputDir = dir;
    }

    public static void reset() {
        ProgressionFrames.awaitCompletion();
        capturedFrames.clear();
        previousExecutions.clear();
        lastRecentScenarioKeys.clear();
        customOutputDir = null;
    }

    public static List<File> getCapturedFrames() {
        ProgressionFrames.awaitCompletion();
        return new ArrayList<>(capturedFrames);
    }

    public static void onLimbo(final Integer testNumber) {
        if (!ProgressionSettings.current().scenarioEnabled())
            return;
        if (isNull(core))
            return;
        final Set<_Scenario> leafs = core.getAccessibleLeafs();
        if (leafs.isEmpty())
            return;

        final long t0 = System.currentTimeMillis();
        final List<_Scenario> scenarios = new ArrayList<>(leafs);
        scenarios.sort(Comparator.comparing(_Scenario::getShortName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(_Scenario::getName, String.CASE_INSENSITIVE_ORDER));

        final int target = (isNotNull(parameters)) ? parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        final int currentCycle = isNotNull(testNumber) ? testNumber : capturedFrames.size() + 1;

        logDebug(String.format("Rendering scenario progression graph (%d scenarios)...", scenarios.size()));
        final File runDir = resolveRunDirectory();
        if (isNull(runDir))
            return;

        final File progressionDir = new File(runDir, "progression");
        if (!progressionDir.exists() && !progressionDir.mkdirs())
            return;

        final int frameIndex = currentCycle;
        final File frameFile = new File(progressionDir, String.format("frame_%04d.svg", frameIndex));
        if (!capturedFrames.contains(frameFile)) {
            capturedFrames.add(frameFile);
        }
        final Set<String> recentKeys = computeRecentScenarioKeys(scenarios);
        final ScenarioGraphSvgRenderer.FrameSnapshot snapshot =
                ScenarioGraphSvgRenderer.captureSnapshot(scenarios, target, currentCycle, recentKeys);
        updatePreviousExecutions(scenarios);
        ProgressionFrames.submitAsync(() -> {
            final String svgContent = ScenarioGraphSvgRenderer.renderProgressionGraphSvg(snapshot);
            try {
                Files.writeString(frameFile.toPath(), svgContent, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                System.err.println("Failed to save progression SVG frame: " + e.getMessage());
                return;
            }
            final long duration = System.currentTimeMillis() - t0;
            logDebug(String.format("Captured scenario progression frame #%d (%dx%d) in %d ms", frameIndex, WIDTH, HEIGHT, duration));
        });
    }

    public static void onComplete() {
        ProgressionFrames.awaitCompletion();
    }

    public static void processFrames(
            final File runDir,
            final String prefix,
            final ProcessingProgress progress) {
        processFrames(runDir, prefix, progress, ProgressionSettings.current().scenarioEnabled());
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
            final long maxTotalPixels) {
        processFrames(
                runDir,
                prefix,
                progress,
                enabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                ProgressionSettings.current().scenarioGifEnabled(),
                ProgressionSettings.current().scenarioWebpEnabled(),
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
                ProgressionSettings.current().scenarioGifEnabled(),
                ProgressionSettings.current().scenarioWebpEnabled(),
                mp4Enabled,
                ffmpeg);
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
            final String ffmpeg) {
        if (!enabled || isNull(runDir)) {
            progress.skipped("Scenario progression GIF");
            progress.skipped("Scenario progression WebP");
            progress.skipped("Scenario progression MP4");
            return;
        }
        final String pngTask = PostProcessor.SCENARIO_PNG_TASK;
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

        final boolean needsRaster = gifEnabled || webpEnabled || mp4Enabled;
        final File tmpDir = new File(runDir, ".tmp_raster_scenario");
        final List<File> rasterizedFrames;
        try {
            if (needsRaster) {
                rasterizedFrames = ProgressionFrames.rasterize(framesToProcess, tmpDir, pngTask, progress, maxFramePixels);
            } else {
                progress.skipped(pngTask);
                rasterizedFrames = framesToProcess;
            }

            final List<Runnable> encoderTasks = new ArrayList<>(3);
            if (gifEnabled) {
                encoderTasks.add(() -> {
                    final File gifFile = new File(runDir, prefix + ".progression.gif");
                    try {
                        writeAnimatedGifFiles(
                                rasterizedFrames,
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
                });
            } else {
                progress.skipped(gifTask);
            }

            if (webpEnabled) {
                encoderTasks.add(() -> {
                    final File webpFile = new File(runDir, prefix + ".progression.webp");
                    try {
                        AnimatedWebP.write(
                                rasterizedFrames,
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
                });
            } else {
                progress.skipped(webpTask);
            }

            if (mp4Enabled) {
                encoderTasks.add(() -> {
                    try {
                        logDebug("Generating scenario progression MP4 from " + rasterizedFrames.size() + " frames...");
                        ProgressionMp4.write(
                                rasterizedFrames,
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
                });
            } else {
                progress.skipped(mp4Task);
            }
            ProgressionFrames.runAllConcurrently(encoderTasks);
        } catch (final Exception e) {
            progress.failed(pngTask, e);
        } finally {
            ProgressionFrames.cleanup(tmpDir);
        }
    }

    public static String renderProgressionGraphSvg(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {
        return ScenarioGraphSvgRenderer.renderProgressionGraphSvg(scenarios, target, currentCycle);
    }

    public static String renderProgressionGraphSvg(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle,
            final Set<String> recentScenarioKeys) {
        return ScenarioGraphSvgRenderer.renderProgressionGraphSvg(scenarios, target, currentCycle, recentScenarioKeys);
    }

    public static BufferedImage renderProgressionGraph(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {
        final String svg = renderProgressionGraphSvg(scenarios, target, currentCycle);
        try {
            return ProgressionFrames.renderSvg(svg, ProgressionSettings.current().maxFramePixels());
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to rasterize scenario progression SVG", e);
        }
    }

    public static String formatElapsedTime(final long elapsedMillis) {
        return ProgressionChrome.formatElapsedTime(elapsedMillis);
    }

    private static Set<String> computeRecentScenarioKeys(final List<_Scenario> scenarios) {
        final Set<String> recent = new HashSet<>();
        if (isNull(core) || isNull(core.getResults())) return recent;
        for (final _Scenario sc : scenarios) {
            final String key = scenarioKey(sc);
            final int execs = core.getResults().getExecutions(sc);
            final int prev = previousExecutions.getOrDefault(key, 0);
            if (execs > prev) {
                recent.add(key);
            }
        }
        if (!recent.isEmpty()) {
            lastRecentScenarioKeys.clear();
            lastRecentScenarioKeys.addAll(recent);
            return recent;
        }
        return new HashSet<>(lastRecentScenarioKeys);
    }

    private static void updatePreviousExecutions(final List<_Scenario> scenarios) {
        if (isNull(core) || isNull(core.getResults())) return;
        for (final _Scenario sc : scenarios) {
            previousExecutions.put(scenarioKey(sc), core.getResults().getExecutions(sc));
        }
    }

    private static String scenarioKey(final _Scenario sc) {
        if (isNull(sc)) return "";
        final String name = sc.getName();
        return (isNotNull(name) && !name.isBlank()) ? name : sc.getShortName();
    }

    public static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds) throws IOException {
        AnimatedGifWriter.writeAnimatedGif(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds);
    }

    public static void writeAnimatedGif(
            final List<BufferedImage> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {
        AnimatedGifWriter.writeAnimatedGif(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds, progress);
    }

    public static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final IntConsumer progress) throws IOException {
        AnimatedGifWriter.writeAnimatedGifFiles(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds, progress);
    }

    public static void writeAnimatedGifFiles(
            final List<File> frames,
            final File outFile,
            final int delayCentiseconds,
            final int finalFrameDelayCentiseconds,
            final int maxFramePixels,
            final IntConsumer progress) throws IOException {
        AnimatedGifWriter.writeAnimatedGifFiles(frames, outFile, delayCentiseconds, finalFrameDelayCentiseconds, maxFramePixels, progress);
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
