package works.lysenko.util.apis.log;

import works.lysenko.base.util.Telemetry;
import works.lysenko.util.data.enums.Severity;

import java.io.Closeable;

/**
 * _LogsProcessor is an interface that represents a log processor.
 * It provides methods for processing telemetry, logging messages and retrieving logs.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _LogsProcessor {

    /**
     * Retrieves all browser logs before proceeding with log entry addition.
     *
     * @param depth         The depth of the logs to retrieve.
     * @param redefinedTime The optional redefined time to filter logs. If null, all logs will be retrieved.
     */
    void getAllDriverLogs(int depth, Long redefinedTime);

    /**
     * Retrieves a Closeable object that can be used to write logs.
     *
     * @return A1 Closeable object representing the log writer.
     */
    Closeable getLogWriter();

    /**
     * Retrieves the length of the span.
     *
     * @return The length of the span.
     */
    int getSpanLength();

    /**
     * Returns a Closeable object that can be used to write telemetry data.
     *
     * @return A1 Closeable object that represents the telemetry writer.
     */
    Closeable getTelemetryWriter();

    /**
     * Logs a message with the specified log level and optional redefined time.
     *
     * @param level         The log level. Use an integer value representing the severity of the log message.
     * @param message       The log message to be logged.
     * @param redefinedTime The optional redefined time to filter logs. If null, all logs will be logged.
     */
    void log(int level, String message, Long redefinedTime);

    /**
     * Logs an empty line with a given short stack trace.
     *
     * @param shortStackTrace The short stack trace to append to the empty line.
     */
    void logEmptyLine(String shortStackTrace);

    /**
     * Logs an empty line.
     */
    void logEmptyLine();

    /**
     * Logs an event with the specified severity, message, and short stack trace.
     *
     * @param severity        The severity of the event.
     * @param message         The message to be logged.
     * @param shortStackTrace The short stack trace to be logged.
     */
    void logEvent(Severity severity, String message, String shortStackTrace);

    /**
     * Process the telemetry data.
     *
     * @param telemetry The telemetry data to be processed.
     */
    void process(Telemetry telemetry);
}
