package works.lysenko.util.apis.log;

import works.lysenko.util.data.enums.Severity;

import java.io.Closeable;

/**
 * Logs Interface
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _Logs {

    /**
     * Retrieves the log writer that can be used to write logs.
     *
     * @return a Closeable object that represents the log writer
     */
    Closeable getLogWriter();

    /**
     * Retrieves the length of the span used in the logger.
     *
     * @return the length of the span
     */
    int getSpanLength();

    /**
     * Retrieves the telemetry writer to log telemetry data.
     *
     * @return a Closeable object that represents the telemetry writer
     */
    Closeable getTelemetryWriter();

    /**
     * Write a string of text into the test execution log with logical level 1, for
     * example:
     * <p>
     * • This is the level 1 record
     *
     * @param message string of text to be written to log
     */
    void log(String message);

    /**
     * Write a string of text to test execution log with defined logical level as
     * visual queues, for example:
     * <p>
     * This is the level 0 record
     * <p>
     * • This is the level 1 record
     * <p>
     * •• This is the level 2 record
     *
     * @param level   log level, 0 is for no markers, 1-n is for number of '•' markers
     *                before a string
     * @param message string of text to be written to log
     */
    void log(int level, String message);

    /**
     * Logs a message with the specified level, call stack, and optional redefined time.
     *
     * @param level         the log level
     * @param message       the message to be logged
     * @param callstack     the call stack string
     * @param redefinedTime an optional parameter to redefine the timestamp of the log message
     */
    default void log(final int level, final String message, final String callstack, final Long redefinedTime) {
        log(level, message, redefinedTime);
    }

    /**
     * Logs a message with the specified level and optional redefined time.
     *
     * @param level         the log level, where 0 is for no markers and 1-n is for the number of '•' markers before the
     *                      message
     * @param message       the message to be logged
     * @param redefinedTime an optional parameter to redefine the timestamp of the log message
     */
    void log(final int level, final String message, final Long redefinedTime);

    /**
     * Logs a section header.
     * TODO: redeem author's stupidity
     *
     * @param title     the section title
     * @param callstack optional call stack
     */
    default void logSection(final String title, final String callstack) {
        log(0, title, callstack, null);
    }

    /**
     * Logs a section header.
     * TODO: redeem author's stupidity
     *
     * @param title the section title
     */
    default void logSection(final String title) {
        logSection(title, null);
    }

    /**
     * Logs an empty line into the test execution log.
     */
    void logEmptyLine();

    /**
     * Logs an event with the given severity, message, and short call stack.
     *
     * @param severity       the severity of the event
     * @param message        the message of the event
     * @param shortCallstack the short call stack of the event
     */
    void logEvent(Severity severity, String message, String shortCallstack);

    /**
     * Flushes the underlying logs processor.
     */
    void flush();

    /**
     * Flushes and closes all underlying log writers.
     */
    default void close() {
    }
}
