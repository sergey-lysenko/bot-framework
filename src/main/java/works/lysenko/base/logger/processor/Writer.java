package works.lysenko.base.logger.processor;

import works.lysenko.util.apis.log._LogsWriter;
import works.lysenko.util.spec.Layout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static works.lysenko.util.func.type.Objects.isNotNull;

import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.LOG;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.OPEN;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITING;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_MD_TEST_;
import static works.lysenko.util.spec.Layout.Templates.TELEMETRY_;

@SuppressWarnings({"NestedMethodCall", "MissingJavadoc"})
/**
 * The Writer class.
 */
public class Writer implements _LogsWriter {

    private final BufferedWriter logWriter;
    private final BufferedWriter markdownWriter;
    private final BufferedWriter telemetryWriter;
    private final Map<String, BufferedWriter> markdownTestWriters = new ConcurrentHashMap<>();

    /**
     * Constructs a new Writer.
     */
    public Writer() {

        logWriter = createFileWriter(RUN_LOG_);
        markdownWriter = createFileWriter(RUN_LOG_MD_);
        writeIndexHeader(markdownWriter);
        telemetryWriter = createFileWriter(TELEMETRY_);
        works.lysenko.base.core.BootCapture.attach(logWriter);
    }

    private static void writeIndexHeader(final BufferedWriter writer) {

        try {
            writer.write("# Test Execution Log - Index" + System.lineSeparator() + System.lineSeparator());
            writer.write("> **Context & Schema**:" + System.lineSeparator());
            writer.write("> - **Sections (`### Title`)**: Execution phases are structured as Markdown headings in per-test log files." + System.lineSeparator());
            writer.write("> - **Log Message**: Operational events, UI interactions, assertions, property reads/writes within each section." + System.lineSeparator());
            writer.write("> - **Stack Trace**: Call site references formatted as `ID:[Class.method:line]`. The full mapping is stored in `<run_id>.stacktraces.properties` in the run directory." + System.lineSeparator());
            writer.write("> - **Duration (ms)**: Operation execution time in milliseconds." + System.lineSeparator() + System.lineSeparator());
            writer.write("## Segmented Log Files" + System.lineSeparator() + System.lineSeparator());
            writer.flush();
        } catch (final IOException ignored) {
        }
    }

    private static void writeTestHeader(final BufferedWriter writer, final int testNumber) {

        try {
            final String headerTitle = (testNumber == 0) ? "Test #0 (Setup / Preflight)" : ("Test #" + testNumber);
            writer.write("# Test Execution Log - " + headerTitle + System.lineSeparator() + System.lineSeparator());
            writer.write("> **Context & Schema**:" + System.lineSeparator());
            writer.write("> - **Sections (`### Title`)**: Execution phases (Preflight, Scenario selection/execution, Data operations) are structured as Markdown headings with call site IDs and section timing." + System.lineSeparator());
            writer.write("> - **Log Message**: Operational events, UI interactions, assertions, property reads/writes, and state changes within each section." + System.lineSeparator());
            writer.write("> - **Stack Trace**: Call site references formatted as `ID:[Class.method:line]` upon first declaration. Bracketed IDs (`[ID]`) or empty cells refer to call site IDs defined earlier in the trace." + System.lineSeparator());
            writer.write("> - **Duration (ms)**: Operation execution time in milliseconds." + System.lineSeparator() + System.lineSeparator());
            writer.flush();
        } catch (final IOException ignored) {
        }
    }

    private static void writeSummaryHeader(final BufferedWriter writer) {

        try {
            writer.write("# Test Execution Log - Summary / Postflight" + System.lineSeparator() + System.lineSeparator());
            writer.write("> **Context & Schema**:" + System.lineSeparator());
            writer.write("> - **Sections (`### Title`)**: Execution summary phases (Scenario statistics, Result markers, Tree reports, Postflight) are structured as Markdown headings with call site IDs and section timing." + System.lineSeparator());
            writer.write("> - **Log Message**: Operational events, UI interactions, assertions, property reads/writes, and state changes within each section." + System.lineSeparator());
            writer.write("> - **Stack Trace**: Call site references formatted as `ID:[Class.method:line]`. The full mapping is stored in `<run_id>.stacktraces.properties` in the run directory." + System.lineSeparator());
            writer.write("> - **Duration (ms)**: Operation execution time in milliseconds." + System.lineSeparator() + System.lineSeparator());
            writer.flush();
        } catch (final IOException ignored) {
        }
    }

    private static BufferedWriter createFileWriter(final String fileName) {

        try {
            return new BufferedWriter(new FileWriter(Layout.Files.name(fileName), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(b(c(UNABLE), TO, OPEN, LOG, FILE, q(fileName), FOR, WRITING), e);
        }
    }

    private static BufferedWriter createTestFileWriter(final String template, final String testNumberStr) {

        try {
            final File file = new File(Layout.Files.name(template, testNumberStr));
            if (isNotNull(file.getParentFile())) {
                file.getParentFile().mkdirs();
            }
            return new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException(b(c(UNABLE), TO, OPEN, LOG, FILE, q(template), FOR, WRITING), e);
        }
    }

    @Override
    public final BufferedWriter getLogWriter() {

        return logWriter;
    }

    @Override
    public final BufferedWriter getMarkdownWriter() {

        return markdownWriter;
    }

    private int maxTestNumber = 0;

    @Override
    public final synchronized BufferedWriter getMarkdownWriter(final int testNumber) {

        final String fileKey;
        final String title;

        if (testNumber > maxTestNumber) {
            maxTestNumber = testNumber;
            fileKey = String.valueOf(testNumber);
            title = "Test #" + testNumber;
        } else if (testNumber == 0 && maxTestNumber > 0) {
            fileKey = "summary";
            title = "Summary / Postflight";
        } else {
            fileKey = String.valueOf(testNumber);
            title = (testNumber == 0) ? "Test #0 (Setup / Preflight)" : ("Test #" + testNumber);
        }

        return markdownTestWriters.computeIfAbsent(fileKey, fk -> {
            final BufferedWriter testWriter = createTestFileWriter(RUN_LOG_MD_TEST_, fk);
            if ("summary".equals(fk)) {
                writeSummaryHeader(testWriter);
            } else {
                writeTestHeader(testWriter, Integer.parseInt(fk));
            }
            try {
                markdownWriter.write("- [" + title + "](md/" + fk + ".md)" + System.lineSeparator());
                markdownWriter.flush();
            } catch (final IOException ignored) {
            }
            return testWriter;
        });
    }

    @Override
    public final BufferedWriter getTelemetryWriter() {

        return telemetryWriter;
    }
}
