package works.lysenko.base.output;

import static works.lysenko.Base.log;

import works.lysenko.base.issues.KnownIssuesLoader;
import works.lysenko.base.output.postprocessor.DetachedProcessLauncher;
import works.lysenko.base.output.postprocessor.PostProcessorProgressUI;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Detached post-processor that waits for the test execution JVM to terminate
 * before generating run reports and progression animations.
 */
public class PostProcessor {

    private static final String REPORT_TASK = "Run timeline report";
    public static final String SCENARIO_PNG_TASK = "Scenario progression PNG frames";
    public static final String TREE_PNG_TASK = "Tree progression PNG frames";

    static List<String> enabledTasks(
            final boolean scenarioEnabled,
            final boolean scenarioGifEnabled,
            final boolean scenarioWebpEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeEnabled,
            final boolean treeGifEnabled,
            final boolean treeWebpEnabled,
            final boolean treeMp4Enabled) {
        return DetachedProcessLauncher.enabledTasks(
                scenarioEnabled,
                scenarioGifEnabled,
                scenarioWebpEnabled,
                scenarioMp4Enabled,
                treeEnabled,
                treeGifEnabled,
                treeWebpEnabled,
                treeMp4Enabled);
    }

    static String resolveClassPath() {
        return DetachedProcessLauncher.resolveClassPath();
    }

    public static void launchDetached(final File runLogFile, final File outHtmlFile, final boolean openBrowser) {
        DetachedProcessLauncher.launchDetached(runLogFile, outHtmlFile, openBrowser);
    }

    public static void launchDetached(
            final File runLogFile,
            final File outHtmlFile,
            final boolean openBrowser,
            final boolean showProgressWindow) {
        DetachedProcessLauncher.launchDetached(runLogFile, outHtmlFile, openBrowser, showProgressWindow);
    }

    static String[] detachedCommand(
            final String javaBin,
            final String classPath,
            final long parentPid,
            final String runLogPath,
            final String htmlPath,
            final boolean openBrowser) {
        return DetachedProcessLauncher.detachedCommand(javaBin, classPath, parentPid, runLogPath, htmlPath, openBrowser);
    }

    static String[] detachedCommand(
            final String javaBin,
            final String classPath,
            final long parentPid,
            final String runLogPath,
            final String htmlPath,
            final boolean showProgressWindow,
            final boolean openBrowser) {
        return DetachedProcessLauncher.detachedCommand(javaBin, classPath, parentPid, runLogPath, htmlPath, showProgressWindow, openBrowser);
    }

    static File postProcessorLogFile(final File runLogFile) {
        return DetachedProcessLauncher.postProcessorLogFile(runLogFile);
    }

    public static void main(final String[] args) {
        if (args.length < 4) return;
        try {
            final long parentPid = Long.parseLong(args[0]);
            final File logFile = new File(args[1]);
            final File htmlFile = new File(args[2]);
            try (RunLocks.LockHandle ignored = RunLocks.acquirePostprocessLock()) {
                ProcessHandle.of(parentPid).ifPresent(handle -> {
                    try {
                        handle.onExit().get(60, TimeUnit.SECONDS);
                    } catch (final Exception e) {
                        System.err.println("Timed out waiting for test process to exit: " + e.getMessage());
                    }
                });
                Thread.sleep(150);
                if (args.length < 12) {
                    process(logFile, htmlFile, Boolean.parseBoolean(args[3]));
                } else {
                    final boolean openBrowser = 13 <= args.length
                            ? Boolean.parseBoolean(args[12])
                            : Boolean.parseBoolean(args[3]);
                    final String treeSonification = 14 <= args.length
                            ? args[13]
                            : ProgressionSettings.current().treeSonification();
                    final boolean scenarioGifEnabled = 18 <= args.length
                            ? Boolean.parseBoolean(args[14])
                            : ProgressionSettings.current().scenarioGifEnabled();
                    final boolean scenarioWebpEnabled = 18 <= args.length
                            ? Boolean.parseBoolean(args[15])
                            : ProgressionSettings.current().scenarioWebpEnabled();
                    final boolean treeGifEnabled = 18 <= args.length
                            ? Boolean.parseBoolean(args[16])
                            : ProgressionSettings.current().treeGifEnabled();
                    final boolean treeWebpEnabled = 18 <= args.length
                            ? Boolean.parseBoolean(args[17])
                            : ProgressionSettings.current().treeWebpEnabled();
                    process(
                            logFile,
                            htmlFile,
                            Boolean.parseBoolean(args[3]),
                            Boolean.parseBoolean(args[4]),
                            scenarioGifEnabled,
                            scenarioWebpEnabled,
                            Boolean.parseBoolean(args[9]),
                            Boolean.parseBoolean(args[5]),
                            treeGifEnabled,
                            treeWebpEnabled,
                            Boolean.parseBoolean(args[10]),
                            works.lysenko.base.util.StringParser.parseSuffixedInt(args[6]),
                            works.lysenko.base.util.StringParser.parseSuffixedInt(args[7]),
                            works.lysenko.base.util.StringParser.parseSuffixedLong(args[8]),
                            args[11],
                            openBrowser,
                            treeSonification);
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Post-processing was interrupted");
        } catch (final Exception e) {
            System.err.println("Post-processing failed: " + e.getMessage());
        }
    }

    static void process(final File logFile, final File htmlFile, final boolean showProgressWindow) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                ProgressionSettings.current().scenarioEnabled(),
                ProgressionSettings.current().treeEnabled(),
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxFramePixels(),
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxFramePixels(),
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean scenarioGifEnabled,
            final boolean scenarioWebpEnabled,
            final boolean treeProgressionEnabled,
            final boolean treeGifEnabled,
            final boolean treeWebpEnabled) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                scenarioGifEnabled,
                scenarioWebpEnabled,
                ProgressionSettings.current().scenarioMp4Enabled(),
                treeProgressionEnabled,
                treeGifEnabled,
                treeWebpEnabled,
                ProgressionSettings.current().treeMp4Enabled(),
                ProgressionSettings.current().maxFrames(),
                ProgressionSettings.current().maxFramePixels(),
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().ffmpeg(),
                showProgressWindow,
                ProgressionSettings.current().treeSonification());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final int maxFrames,
            final int maxFramePixels) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                maxFrames,
                maxFramePixels,
                ProgressionSettings.current().maxTotalPixels(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled(),
                ProgressionSettings.current().ffmpeg());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final String ffmpeg) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                scenarioMp4Enabled,
                treeMp4Enabled,
                ffmpeg,
                showProgressWindow);
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final String ffmpeg,
            final boolean openBrowser) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                scenarioMp4Enabled,
                treeMp4Enabled,
                ffmpeg,
                openBrowser,
                ProgressionSettings.current().treeSonification());
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final String ffmpeg,
            final boolean openBrowser,
            final String treeSonification) {
        process(
                logFile,
                htmlFile,
                showProgressWindow,
                scenarioProgressionEnabled,
                ProgressionSettings.current().scenarioGifEnabled(),
                ProgressionSettings.current().scenarioWebpEnabled(),
                scenarioMp4Enabled,
                treeProgressionEnabled,
                ProgressionSettings.current().treeGifEnabled(),
                ProgressionSettings.current().treeWebpEnabled(),
                treeMp4Enabled,
                maxFrames,
                maxFramePixels,
                maxTotalPixels,
                ffmpeg,
                openBrowser,
                treeSonification);
    }

    static void process(
            final File logFile,
            final File htmlFile,
            final boolean showProgressWindow,
            final boolean scenarioProgressionEnabled,
            final boolean scenarioGifEnabled,
            final boolean scenarioWebpEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeProgressionEnabled,
            final boolean treeGifEnabled,
            final boolean treeWebpEnabled,
            final boolean treeMp4Enabled,
            final int maxFrames,
            final int maxFramePixels,
            final long maxTotalPixels,
            final String ffmpeg,
            final boolean openBrowser,
            final String treeSonification) {
        final File runDir = logFile.getAbsoluteFile().getParentFile();
        final String prefix = runPrefix(logFile);
        log("Post-processing started for run: " + runDir.getAbsolutePath());
        final List<String> tasks = enabledTasks(
                scenarioProgressionEnabled,
                scenarioGifEnabled,
                scenarioWebpEnabled,
                scenarioMp4Enabled,
                treeProgressionEnabled,
                treeGifEnabled,
                treeWebpEnabled,
                treeMp4Enabled);
        final ProcessingProgress progress = PostProcessorProgressUI.create(showProgressWindow, tasks);
        try {
            ScenarioTracker.processFrames(
                    runDir,
                    prefix,
                    progress,
                    scenarioProgressionEnabled,
                    maxFrames,
                    maxFramePixels,
                    maxTotalPixels,
                    scenarioGifEnabled,
                    scenarioWebpEnabled,
                    scenarioMp4Enabled,
                    ffmpeg);
            TreeTracker.processFrames(
                    runDir,
                    prefix,
                    progress,
                    treeProgressionEnabled,
                    maxFrames,
                    maxFramePixels,
                    maxTotalPixels,
                    treeGifEnabled,
                    treeWebpEnabled,
                    treeMp4Enabled,
                    ffmpeg,
                    treeSonification);

            progress.update(REPORT_TASK, 0);
            KnownIssuesLoader.saveRunKnownIssues(new File(runDir, prefix + ".known.issues.json"));
            LogHtml.generateReport(
                    logFile,
                    htmlFile,
                    scenarioProgressionEnabled,
                    treeProgressionEnabled,
                    scenarioMp4Enabled,
                    treeMp4Enabled,
                    progress);
            progress.complete(REPORT_TASK);
            log("Run timeline report generated: " + htmlFile.getAbsolutePath());
            if (openBrowser) LogHtml.openInBrowser(htmlFile);
            log("Post-processing completed successfully.");
        } finally {
            progress.close();
        }
    }

    private static String runPrefix(final File logFile) {
        final String name = logFile.getName();
        final String suffix = ".run.log";
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : "run";
    }
}
