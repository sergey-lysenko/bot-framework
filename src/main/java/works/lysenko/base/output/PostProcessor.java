package works.lysenko.base.output;

import works.lysenko.base.core.Routines;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Detached post-processor that waits for the test execution JVM to fully terminate
 * before generating the comprehensive timeline log HTML report and opening the browser.
 */
public class PostProcessor {

    public static void launchDetached(final File runLogFile, final File outHtmlFile, final boolean openBrowser) {
        try {
            if (null == runLogFile || !runLogFile.exists()) {
                return;
            }
            final String javaBin = ProcessHandle.current().info().command().orElse(
                    System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            );
            final String cp = System.getProperty("java.class.path");
            final long parentPid = ProcessHandle.current().pid();

            new ProcessBuilder(
                    javaBin,
                    "-cp",
                    cp,
                    PostProcessor.class.getName(),
                    String.valueOf(parentPid),
                    runLogFile.getAbsolutePath(),
                    outHtmlFile.getAbsolutePath(),
                    String.valueOf(openBrowser)
            ).start();
        } catch (final Exception ignored) {
        }
    }

    public static void main(final String[] args) {
        if (args.length < 4) {
            return;
        }
        try {
            final long parentPid = Long.parseLong(args[0]);
            ProcessHandle.of(parentPid).ifPresent(h -> {
                try {
                    h.onExit().get(20, TimeUnit.SECONDS);
                } catch (final Exception ignored) {
                }
            });

            // Brief pause to allow OS file system buffers to settle
            try {
                Thread.sleep(150);
            } catch (final InterruptedException ignored) {
            }

            final File logFile = new File(args[1]);
            final File htmlFile = new File(args[2]);
            final boolean openBrowser = Boolean.parseBoolean(args[3]);

            LogHtml.generateReport(logFile, htmlFile);

            if (openBrowser) {
                LogHtml.openInBrowser(htmlFile);
            }
        } catch (final Exception ignored) {
        }
    }
}
