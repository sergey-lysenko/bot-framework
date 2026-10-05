package works.lysenko.base.output;


import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Detached post-processor that waits for the test execution JVM to terminate
 * before generating run reports and progression animations.
 */
public class PostProcessor {

    private static final String REPORT_TASK = "Run timeline report";
    private static final String[] MEDIA_TASKS = {
            "Scenario progression GIF",
            "Scenario progression WebP",
            "Scenario progression MP4",
            "Tree progression GIF",
            "Tree progression WebP",
            "Tree progression MP4"
    };

    public static void launchDetached(final File runLogFile, final File outHtmlFile, final boolean openBrowser) {
        launchDetached(runLogFile, outHtmlFile, openBrowser, openBrowser);
    }

    public static void launchDetached(
            final File runLogFile,
            final File outHtmlFile,
            final boolean openBrowser,
            final boolean showProgressWindow) {
        final File postProcessorLog = postProcessorLogFile(runLogFile);
        try {
            if (null == runLogFile || !runLogFile.exists()) {
                return;
            }
            final File parent = postProcessorLog.getAbsoluteFile().getParentFile();
            if (null != parent && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("Unable to create post-processor log directory: " + parent);
            }
            final String javaBin = ProcessHandle.current().info().command().orElse(
                    System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            );
            final String cp = System.getProperty("java.class.path");
            final long parentPid = ProcessHandle.current().pid();

            final Process postProcessor = new ProcessBuilder(detachedCommand(
                    javaBin,
                    cp,
                    parentPid,
                    runLogFile.getAbsolutePath(),
                    outHtmlFile.getAbsolutePath(),
                    showProgressWindow,
                    openBrowser)).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(postProcessorLog))
                    .start();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                if (RunLocks.isPostprocessLockHeld()) return;
                if (!postProcessor.isAlive()) return;
                Thread.sleep(25);
            }
            postProcessor.destroyForcibly();
            throw new IOException("Detached post-processor did not acquire run/.postprocess.lock");
        } catch (final Exception e) {
            final String message = "Unable to launch detached post-processor: " + e + System.lineSeparator();
            try {
                Files.writeString(
                        postProcessorLog.toPath(),
                        message,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (final IOException logFailure) {
                System.err.println(message.trim());
                System.err.println("Unable to write post-processor log: " + logFailure.getMessage());
            }
        }
    }

    static String[] detachedCommand(
            final String javaBin,
            final String classPath,
            final long parentPid,
            final String runLogPath,
            final String htmlPath,
            final boolean openBrowser) {
        return detachedCommand(javaBin, classPath, parentPid, runLogPath, htmlPath, openBrowser, openBrowser);
    }

    static String[] detachedCommand(
            final String javaBin,
            final String classPath,
            final long parentPid,
            final String runLogPath,
            final String htmlPath,
            final boolean showProgressWindow,
            final boolean openBrowser) {
        return new String[]{
                javaBin,
                "-cp",
                classPath,
                PostProcessor.class.getName(),
                String.valueOf(parentPid),
                runLogPath,
                htmlPath,
                String.valueOf(showProgressWindow),
                String.valueOf((Object) ProgressionSettings.current().scenarioEnabled()),
                String.valueOf((Object) ProgressionSettings.current().treeEnabled()),
                String.valueOf((Object) ProgressionSettings.current().maxFrames()),
                String.valueOf((Object) ProgressionSettings.current().maxFramePixels()),
                String.valueOf((Object) ProgressionSettings.current().maxTotalPixels()),
                String.valueOf((Object) ProgressionSettings.current().scenarioMp4Enabled()),
                String.valueOf((Object) ProgressionSettings.current().treeMp4Enabled()),
                ProgressionSettings.current().ffmpeg(),
                String.valueOf(openBrowser),
                ProgressionSettings.current().treeSonification()
        };
    }

    static File postProcessorLogFile(final File runLogFile) {
        if (null == runLogFile) return new File("post-processor.log");
        final String name = runLogFile.getName();
        final String suffix = ".run.log";
        final String logName = name.endsWith(suffix)
                ? name.substring(0, name.length() - suffix.length()) + ".post-processing.log"
                : name + ".post-processing.log";
        return new File(runLogFile.getAbsoluteFile().getParentFile(), logName);
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
                        handle.onExit().get(20, TimeUnit.SECONDS);
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
                    if (14 <= args.length) {
                        process(
                                logFile,
                                htmlFile,
                                Boolean.parseBoolean(args[3]),
                                Boolean.parseBoolean(args[4]),
                                Boolean.parseBoolean(args[5]),
                                Integer.parseInt(args[6]),
                                Integer.parseInt(args[7]),
                                Integer.parseInt(args[8]),
                                Boolean.parseBoolean(args[9]),
                                Boolean.parseBoolean(args[10]),
                                args[11],
                                openBrowser,
                                args[13]);
                    } else {
                        process(
                                logFile,
                                htmlFile,
                                Boolean.parseBoolean(args[3]),
                                Boolean.parseBoolean(args[4]),
                                Boolean.parseBoolean(args[5]),
                                Integer.parseInt(args[6]),
                                Integer.parseInt(args[7]),
                                Integer.parseInt(args[8]),
                                Boolean.parseBoolean(args[9]),
                                Boolean.parseBoolean(args[10]),
                                args[11],
                                openBrowser);
                    }
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
            final int maxTotalPixels) {
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
            final int maxTotalPixels,
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
            final int maxTotalPixels,
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
            final int maxTotalPixels,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final String ffmpeg,
            final boolean openBrowser,
            final String treeSonification) {
        final File runDir = logFile.getAbsoluteFile().getParentFile();
        final String prefix = runPrefix(logFile);
        final ProgressWindow progress = ProgressWindow.create(showProgressWindow);
        try {
            ScenarioTracker.processFrames(
                    runDir,
                    prefix,
                    progress,
                    scenarioProgressionEnabled,
                    maxFrames,
                    maxFramePixels,
                    maxTotalPixels,
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
                    treeMp4Enabled,
                    ffmpeg,
                    treeSonification);

            progress.update(REPORT_TASK, 0);
            LogHtml.generateReport(
                    logFile,
                    htmlFile,
                    scenarioProgressionEnabled,
                    treeProgressionEnabled,
                    scenarioMp4Enabled,
                    treeMp4Enabled);
            progress.complete(REPORT_TASK);
            if (openBrowser) LogHtml.openInBrowser(htmlFile);
        } finally {
            progress.close();
        }
    }

    private static String runPrefix(final File logFile) {
        final String name = logFile.getName();
        final String suffix = ".run.log";
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : "run";
    }

    private static final class ProgressWindow implements ProcessingProgress {

        private final JFrame frame;
        private final Map<String, JProgressBar> bars;

        private ProgressWindow(final JFrame frame, final Map<String, JProgressBar> bars) {
            this.frame = frame;
            this.bars = bars;
        }

        private static ProgressWindow create(final boolean show) {
            if (!show || GraphicsEnvironment.isHeadless()) return new ProgressWindow(null, Map.of());
            final Map<String, JProgressBar> bars = new LinkedHashMap<>();
            final JFrame[] frameRef = new JFrame[1];
            try {
                SwingUtilities.invokeAndWait(() -> {
                    final JFrame window = new JFrame("Test run post-processing");
                    window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    final JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
                    panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                    addTask(panel, bars, REPORT_TASK);
                    for (final String task : MEDIA_TASKS) addTask(panel, bars, task);
                    window.setContentPane(panel);
                    window.setMinimumSize(new Dimension(430, 220));
                    window.pack();
                    window.setLocationRelativeTo(null);
                    window.setVisible(true);
                    frameRef[0] = window;
                });
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while opening post-processing progress window");
            } catch (final InvocationTargetException e) {
                System.err.println("Unable to open post-processing progress window: " + e.getCause());
            }
            return new ProgressWindow(frameRef[0], bars);
        }

        private static void addTask(
                final JPanel panel,
                final Map<String, JProgressBar> bars,
                final String name) {
            panel.add(new JLabel(name));
            final JProgressBar bar = new JProgressBar(0, 100);
            bar.setStringPainted(true);
            bar.setString("Waiting");
            panel.add(bar);
            bars.put(name, bar);
        }

        @Override
        public void update(final String task, final int percentage) {
            final JProgressBar bar = bars.get(task);
            if (null == bar) return;
            final int value = Math.max(0, Math.min(100, percentage));
            SwingUtilities.invokeLater(() -> {
                bar.setValue(value);
                bar.setString(value + "%");
            });
        }

        @Override
        public void complete(final String task) {
            final JProgressBar bar = bars.get(task);
            if (null == bar) return;
            SwingUtilities.invokeLater(() -> {
                bar.setValue(100);
                bar.setString("Complete");
            });
        }

        @Override
        public void skipped(final String task) {
            final JProgressBar bar = bars.get(task);
            if (null == bar) return;
            SwingUtilities.invokeLater(() -> {
                bar.setValue(100);
                bar.setString("No frames");
            });
        }

        @Override
        public void failed(final String task, final Exception failure) {
            ProcessingProgress.super.failed(task, failure);
            final JProgressBar bar = bars.get(task);
            if (null != bar) SwingUtilities.invokeLater(() -> bar.setString("Failed"));
        }

        private void close() {
            if (null != frame) SwingUtilities.invokeLater(frame::dispose);
        }
    }
}
