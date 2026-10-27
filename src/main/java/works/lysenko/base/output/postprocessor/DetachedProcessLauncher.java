package works.lysenko.base.output.postprocessor;

import works.lysenko.base.output.PostProcessor;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.RunLocks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Handles classpath resolution, command construction, and background JVM process
 * launching for detached post-processing.
 */
public final class DetachedProcessLauncher {

    private static final String REPORT_TASK = "Run timeline report";

    private DetachedProcessLauncher() {
    }

    public static List<String> enabledTasks(
            final boolean scenarioEnabled,
            final boolean scenarioGifEnabled,
            final boolean scenarioWebpEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeEnabled,
            final boolean treeGifEnabled,
            final boolean treeWebpEnabled,
            final boolean treeMp4Enabled) {
        final List<String> tasks = new ArrayList<>();
        tasks.add(REPORT_TASK);
        if (scenarioEnabled) {
            if (scenarioGifEnabled || scenarioWebpEnabled || scenarioMp4Enabled) tasks.add(PostProcessor.SCENARIO_PNG_TASK);
            if (scenarioGifEnabled) tasks.add("Scenario progression GIF");
            if (scenarioWebpEnabled) tasks.add("Scenario progression WebP");
            if (scenarioMp4Enabled) tasks.add("Scenario progression MP4");
        }
        if (treeEnabled) {
            if (treeGifEnabled || treeWebpEnabled || treeMp4Enabled) tasks.add(PostProcessor.TREE_PNG_TASK);
            if (treeGifEnabled) tasks.add("Tree progression GIF");
            if (treeWebpEnabled) tasks.add("Tree progression WebP");
            if (treeMp4Enabled) tasks.add("Tree progression MP4");
        }
        return tasks;
    }

    public static String resolveClassPath() {
        final String baseCp = System.getProperty("java.class.path");
        final StringBuilder sb = new StringBuilder(baseCp);
        final Set<String> paths = new HashSet<>();
        if (null != baseCp) {
            for (final String p : baseCp.split(File.pathSeparator)) {
                if (!p.isBlank()) paths.add(new File(p).getAbsolutePath());
            }
        }

        final String[] keyClassNames = {
                "works.lysenko.base.output.PostProcessor",
                "com.fasterxml.jackson.databind.ObjectMapper",
                "org.apache.commons.math3.fraction.Fraction",
                "org.apache.commons.lang3.StringUtils",
                "com.github.weisj.jsvg.parser.SVGLoader"
        };
        for (final String className : keyClassNames) {
            try {
                final Class<?> cls = Class.forName(className);
                final java.security.ProtectionDomain pd = cls.getProtectionDomain();
                if (null != pd && null != pd.getCodeSource() && null != pd.getCodeSource().getLocation()) {
                    final String locPath = new File(pd.getCodeSource().getLocation().toURI()).getAbsolutePath();
                    if (paths.add(locPath)) {
                        sb.append(File.pathSeparator).append(locPath);
                    }
                }
            } catch (final Throwable ignored) {
            }
        }

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof java.net.URLClassLoader ucl) {
            for (final java.net.URL url : ucl.getURLs()) {
                try {
                    final String urlPath = new File(url.toURI()).getAbsolutePath();
                    if (paths.add(urlPath)) {
                        sb.append(File.pathSeparator).append(urlPath);
                    }
                } catch (final Exception ignored) {
                }
            }
        }
        return sb.toString();
    }

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
            final String cp = resolveClassPath();
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
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
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

    public static String[] detachedCommand(
            final String javaBin,
            final String classPath,
            final long parentPid,
            final String runLogPath,
            final String htmlPath,
            final boolean openBrowser) {
        return detachedCommand(javaBin, classPath, parentPid, runLogPath, htmlPath, openBrowser, openBrowser);
    }

    public static String[] detachedCommand(
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
                ProgressionSettings.current().treeSonification(),
                String.valueOf((Object) ProgressionSettings.current().scenarioGifEnabled()),
                String.valueOf((Object) ProgressionSettings.current().scenarioWebpEnabled()),
                String.valueOf((Object) ProgressionSettings.current().treeGifEnabled()),
                String.valueOf((Object) ProgressionSettings.current().treeWebpEnabled())
        };
    }

    public static File postProcessorLogFile(final File runLogFile) {
        if (null == runLogFile) return new File("post-processor.log");
        final String name = runLogFile.getName();
        final String suffix = ".run.log";
        final String logName = name.endsWith(suffix)
                ? name.substring(0, name.length() - suffix.length()) + ".post-processing.log"
                : name + ".post-processing.log";
        return new File(runLogFile.getAbsoluteFile().getParentFile(), logName);
    }
}
