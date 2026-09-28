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
     * Returns the BufferedWriter object used for writing telemetry data.
     *
     * @return The BufferedWriter object used for writing telemetry data.
     */
    BufferedWriter getTelemetryWriter();
}
