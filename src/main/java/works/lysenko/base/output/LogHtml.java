package works.lysenko.base.output;

import works.lysenko.base.core.Routines;
import works.lysenko.base.output.loghtml.LogParser;

import java.io.File;
import java.util.Locale;

import static works.lysenko.Base.core;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_HTML_;

/**
 * HTML report generator for execution run logs.
 * <p>
 * Serves as the public façade delegating report parsing and rendering tasks to
 * modular components in {@link works.lysenko.base.output.loghtml}.
 */
@SuppressWarnings({"UtilityClass", "ClassWithoutLogger"})
public final class LogHtml {

    private LogHtml() {
    }

    /**
     * Flushes the active logger's log writer prior to report generation.
     */
    public static void logStats() {
        try {
            if (null != core && null != core.getLogger() && null != core.getLogger().getLogWriter()) {
                if (core.getLogger().getLogWriter() instanceof java.io.Flushable) {
                    ((java.io.Flushable) core.getLogger().getLogWriter()).flush();
                }
            }
        } catch (final Exception e) {
            logEvent(S2, "Failed to flush run log before report generation: " + e.getMessage());
        }
    }

    /**
     * Generates an HTML report from a run log file using default progression settings.
     *
     * @param logFile input raw run log file
     * @param outFile target output HTML report file
     */
    public static void generateReport(final File logFile, final File outFile) {
        generateReport(
                logFile,
                outFile,
                ProgressionSettings.current().scenarioEnabled(),
                ProgressionSettings.current().treeEnabled(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled());
    }

    /**
     * Generates an HTML report from a run log file with specified progression animation flags.
     *
     * @param logFile                   input raw run log file
     * @param outFile                   target output HTML report file
     * @param scenarioProgressionEnabled whether scenario progression media links should be included
     * @param treeProgressionEnabled     whether scenario tree progression media links should be included
     */
    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled) {
        generateReport(
                logFile,
                outFile,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled());
    }

    /**
     * Generates an HTML report from a run log file with full progression parameters.
     *
     * @param logFile                   input raw run log file
     * @param outFile                   target output HTML report file
     * @param scenarioProgressionEnabled whether scenario progression media links should be included
     * @param treeProgressionEnabled     whether scenario tree progression media links should be included
     * @param scenarioMp4Enabled         whether scenario MP4 video links should be included
     * @param treeMp4Enabled             whether scenario tree MP4 video links should be included
     */
    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled) {
        LogParser.generateReport(
                logFile,
                outFile,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                scenarioMp4Enabled,
                treeMp4Enabled);
    }

    /**
     * Opens the default generated HTML run report in the system default web browser.
     */
    public static void openInBrowser() {
        final String outFilePath = name(RUN_LOG_HTML_);
        openInBrowser(new File(outFilePath));
    }

    /**
     * Opens the specified HTML report file in the default browser if running interactively.
     *
     * @param htmlFile target HTML report file
     */
    public static void openInBrowser(final File htmlFile) {
        try {
            if (null == htmlFile || !htmlFile.exists()) {
                return;
            }
            if (java.awt.GraphicsEnvironment.isHeadless() || Routines.isInsideCI() || Routines.isInsideDocker()) {
                return;
            }
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(htmlFile.toURI());
                return;
            }
            // Fallback for macOS / Linux CLI
            final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                new ProcessBuilder("open", htmlFile.getAbsolutePath()).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", htmlFile.getAbsolutePath()).start();
            }
        } catch (final Exception e) {
            logEvent(S2, "Unable to auto-open report in browser: " + e.getMessage());
        }
    }

    /**
     * CLI entry point for standalone HTML report generation.
     *
     * @param args command-line arguments: [0] input log path, [1] optional output HTML path
     */
    public static void main(final String[] args) {
        if (args.length >= 2) {
            generateReport(new File(args[0]), new File(args[1]));
        } else if (args.length == 1) {
            final File logFile = new File(args[0]);
            final String outName = logFile.getAbsolutePath().endsWith(".log")
                    ? logFile.getAbsolutePath() + ".html"
                    : logFile.getAbsolutePath() + ".run.log.html";
            generateReport(logFile, new File(outName));
        }
    }
}
