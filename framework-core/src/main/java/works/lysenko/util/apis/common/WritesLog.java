package works.lysenko.util.apis.common;

import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;

/**
 * Interface for writing logs.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface WritesLog {

    /**
     * Generates the log for the specified field and object.
     *
     * @param field the field for which the log is retrieved
     * @param o     the object for which the log is retrieved
     */
    void getLog(Object field, Object o);

    /**
     * Logs a message.
     *
     * @param message the message to be logged
     */
    void log(String message);

    /**
     * Writes a log message at the specified log level.
     *
     * @param level    the log level
     * @param message  the message to be logged
     * @param addStack indicates whether to add a stack trace to the log message
     */
    void log(int level, String message, boolean addStack);

    /**
     * Logs a message with the given log level, message, and additional options.
     *
     * @param level           the log level indicating the severity of the log message
     * @param message         the message to be logged
     * @param addStack        indicates whether to add a stack trace to the log message
     * @param forceUnfiltered indicates whether to log the message even with filtering enabled
     */
    void log(int level, String message, boolean addStack, boolean forceUnfiltered);

    /**
     * Logs a debug message.
     *
     * @param message the debug message to be logged
     */
    void logDebug(String message);

    /**
     * Logs a debug message with key-value pairs.
     *
     * @param keys The key-value pairs to be logged.
     */
    @SuppressWarnings("rawtypes")
    void logDebug(final KeyValue... keys);

    /**
     * Logs a debug message.
     *
     * @param message  the debug message to be logged
     * @param addStack a boolean flag indicating whether to add stack trace to the log message
     */
    void logDebug(String message, boolean addStack);

    /**
     * Logs a debug message with the given message, addStack, and forceUnfiltered options.
     *
     * @param message         the debug message to be logged
     * @param addStack        a flag indicating whether to add a stack trace to the log message
     * @param forceUnfiltered a flag indicating whether to log the message even with filtering enabled
     */
    void logDebug(String message, boolean addStack, boolean forceUnfiltered);

    /**
     * Writes an empty line to the log.
     */
    void logEmptyLine();

    /**
     * Logs an empty line to the log.
     *
     * @param addStack indicates whether to add a stack trace to the log message
     */
    void logEmptyLine(boolean addStack);

    /**
     * Logs an empty line to the log.
     *
     * @param addStack        indicates whether to add a stack trace to the log message
     * @param forceUnfiltered indicates whether to log the empty line even with filtering enabled
     */
    void logEmptyLine(boolean addStack, boolean forceUnfiltered);

    /**
     * @param severity of an event
     * @param message  description of an event
     */
    void logEvent(Severity severity, String message);

    /**
     * Logs a trace message.
     *
     * @param message the trace message to be logged
     */
    void logTrace(String message);

    /**
     * Logs a trace message.
     *
     * @param message  the trace message to be logged
     * @param addStack a boolean flag indicating whether to add stack trace to the log message
     */
    void logTrace(String message, boolean addStack);

    /**
     * Logs a trace message with the given message, addStack, and forceUnfiltered options.
     *
     * @param message         the trace message to be logged
     * @param addStack        a flag indicating whether to add a stack trace to the log message
     * @param forceUnfiltered a flag indicating whether to log the message even with filtering enabled
     */
    void logTrace(String message, boolean addStack, boolean forceUnfiltered);
}
