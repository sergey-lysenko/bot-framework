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

import static works.lysenko.util.data.enums.Severity.SK;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings("NestedMethodCall")
public class Logs implements _Logs {

    private final _LogsProcessor processor;

    /**
     * @param logsToRead set of browser logs to read and include
     */
    @SuppressWarnings("SameParameterValue")
    Logs(final Set<String> logsToRead) {

        processor = new Processor(Objects.requireNonNullElseGet(logsToRead, HashSet::new));
    }

    public final Closeable getLogWriter() {

        return processor.getLogWriter();
    }

    public final int getSpanLength() {

        return processor.getSpanLength();
    }

    public final Closeable getTelemetryWriter() {

        return processor.getTelemetryWriter();
    }

    public final void log(final String message) {

        log(Level.plain, message);
    }

    public final void log(final int level, final String message) {

        log(level, message, null);
    }

    public final void log(final int level, final String message, final Long redefinedTime) {

        processor.log(level, message, redefinedTime);
    }

    public final void logEmptyLine() {

        processor.logEmptyLine();
    }

    public final void logEvent(final Severity severity, final String message, final String shortStackTrace) {

        processor.logEvent(severity, message, shortStackTrace);
    }

    public final void logKnownIssue(final String s) {

        logEvent(SK, s, Stacktrace.getShort(Thread.currentThread().getStackTrace()));
    }
}