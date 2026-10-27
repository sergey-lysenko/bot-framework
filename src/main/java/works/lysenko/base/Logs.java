package works.lysenko.base;

import works.lysenko.base.logger.Processor;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.log._LogsProcessor;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.func.core.Stacktrace;
import works.lysenko.util.spec.Level;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The Logs class manages log processing and recording operations.
 * Implements the _Logs interface.
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings("NestedMethodCall")
public class Logs implements _Logs {

    private final _LogsProcessor processor;

    /**
     * Constructs a new Logs instance with the specified set of logs to read.
     *
     * @param logsToRead set of browser logs to read and include
     */
    @SuppressWarnings("SameParameterValue")
    Logs(final Set<String> logsToRead) {

        processor = new Processor(Objects.requireNonNullElseGet(logsToRead, HashSet::new));
    }

    /**
     * Returns the log writer closeable resource.
     *
     * @return log writer closeable
     */
    public final Closeable getLogWriter() {

        return processor.getLogWriter();
    }

    /**
     * Returns the span length for logging operations.
     *
     * @return span length
     */
    public final int getSpanLength() {

        return processor.getSpanLength();
    }

    /**
     * Returns the telemetry writer closeable resource.
     *
     * @return telemetry writer closeable
     */
    public final Closeable getTelemetryWriter() {

        return processor.getTelemetryWriter();
    }

    /**
     * Logs a plain message.
     *
     * @param message the message to log
     */
    public final void log(final String message) {

        log(Level.plain, message);
    }

    /**
     * Logs a message at the specified log level.
     *
     * @param level   the log level
     * @param message the message to log
     */
    public final void log(final int level, final String message) {

        log(level, message, null);
    }

    /**
     * Logs a message at the specified log level with a redefined timestamp.
     *
     * @param level         the log level
     * @param message       the message to log
     * @param redefinedTime optional redefined timestamp
     */
    public final void log(final int level, final String message, final Long redefinedTime) {

        processor.log(level, message, null, redefinedTime);
    }

    /**
     * Logs a message at the specified log level with stacktrace and optional redefined timestamp.
     *
     * @param level         the log level
     * @param message       the message to log
     * @param stacktrace    the stack trace
     * @param redefinedTime optional redefined timestamp
     */
    public final void log(final int level, final String message, final String stacktrace, final Long redefinedTime) {

        processor.log(level, message, stacktrace, redefinedTime);
    }

    /**
     * Logs a section header.
     * TODO: redeem author's stupidity
     *
     * @param title      section title
     * @param stacktrace optional stacktrace
     */
    public final void logSection(final String title, final String stacktrace) {

        processor.logSection(title, stacktrace);
    }

    /**
     * Logs a section header.
     * TODO: redeem author's stupidity
     *
     * @param title section title
     */
    public final void logSection(final String title) {

        processor.logSection(title, null);
    }

    /**
     * Logs an empty line.
     */
    public final void logEmptyLine() {

        processor.logEmptyLine();
    }

    /**
     * Logs an event with severity and stack trace.
     *
     * @param severity        event severity
     * @param message         event message
     * @param shortStackTrace short stack trace string
     */
    public final void logEvent(final Severity severity, final String message, final String shortStackTrace) {

        processor.logEvent(severity, message, shortStackTrace);
    }

    /**
     * Flushes the underlying processor.
     */
    public final void flush() {

        processor.flush();
    }
}
