package works.lysenko.util.apis.log;

import java.io.BufferedWriter;

/**
 * The LogsWriter interface represents a contract for classes that write logs and telemetry data to files.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogsWriter {

    /**
     * Returns the BufferedWriter object used for writing logs.
     *
     * @return The BufferedWriter object used for writing logs.
     */
    BufferedWriter getLogWriter();

    /**
     * Returns the BufferedWriter object used for writing Markdown logs for a specific test number.
     *
     * @param testNumber the test number integer
     * @return The BufferedWriter object used for writing per-test Markdown logs.
     */
    default BufferedWriter getMarkdownWriter(final int testNumber) {
        return getMarkdownWriter();
    }

    /**
     * Returns the BufferedWriter object used for writing Markdown logs.
     *
     * @return The BufferedWriter object used for writing Markdown logs.
     */
    BufferedWriter getMarkdownWriter();

    /**
     * Returns the BufferedWriter object used for writing telemetry data.
     *
     * @return The BufferedWriter object used for writing telemetry data.
     */
    BufferedWriter getTelemetryWriter();
}
