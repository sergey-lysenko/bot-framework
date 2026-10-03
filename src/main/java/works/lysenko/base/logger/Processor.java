package works.lysenko.base.logger;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import works.lysenko.base.core.Routines;
import works.lysenko.base.logger.processor.Queue;
import works.lysenko.base.logger.processor.Writer;
import works.lysenko.base.util.Telemetry;
import works.lysenko.util.apis._LimitedDeque;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._LogsProcessor;
import works.lysenko.util.apis.log._LogsWriter;
import works.lysenko.util.apis.log._Queue;
import works.lysenko.util.apis.util._Severity;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.LimitedDeque;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.logr.AbstractLogData;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.LineFeed;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.func.core.Stacktrace;
import works.lysenko.util.prop.core.Similarity;
import works.lysenko.util.prop.logs.Severities;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.base.core.Routines.msSinceStart;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.data.enums.Ansi.removeANSICodes;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.EventType.KNOWN_ISSUE;
import static works.lysenko.util.data.enums.Platform.CHROME;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.u;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.A.ADJACENT___;
import static works.lysenko.util.lang.D.DUPLICATED___;
import static works.lysenko.util.lang.L.LOG_WRITER_IS_NOT___;
import static works.lysenko.util.lang.T.TELEMETRY_WRITER_IS_NOT___;
import static works.lysenko.util.lang.word.B.BROWSER;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.prop.core.Similarity.exceptions;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.SEM_CLN;
import static works.lysenko.util.spec.Symbols._LFD_;

/**
 * The Processor class is a central component that manages logging, log record processing,
 * and categorisation of log events. It handles browser logs, external log entries,
 * and telemetry data for use in monitoring and debugging applications.
 * <p>
 * The class processes and stores log data in a way that allows for structured event
 * categorisation, similarity checks, and custom logging functionality.
 * It also provides features to calculate timestamp offsets, assess log similarity,
 * and output logs to specified destinations.
 * <p>
 * Fields:
 * - counter: Tracks the number of processed records or operations.
 * - queue: The log queue that stores pending log records for processing.
 * - similarity: A collection used for similarity checks between logs.
 * - writer: Used to write logs to a specified output.
 * - logsToRead: Stores the browser logs to be read during processing.
 * - previousLogRecord: Stores the last processed log record for comparison or reference.
 * - driverLogsAvailable: A flag indicating if driver logs are available for processing.
 * - spanLength: Tracks the duration or span of events for certain types of logs.
 * - upperLogRecord: An associated higher-priority or grouped log record.
 */
@SuppressWarnings({"CallToSuspiciousStringMethod", "MethodWithMultipleReturnPoints", "CallToPrintStackTrace"})
public class Processor implements _LogsProcessor {

    private static long counter = ZERO;
    private final _Queue queue = new Queue();
    private final _LimitedDeque<_LogRecord> similarity = new LimitedDeque<>(Similarity.depth);
    private final _LogsWriter writer = new Writer();
    private final Set<String> logsToRead;
    private _LogRecord previousLogRecord = null;
    private Boolean driverLogsAvailable = null;
    private int spanLength = 0;
    private _LogRecord upperLogRecord = null;

    /**
     * Constructor for the Processor class that initialises the logs to be read.
     *
     * @param logsToRead the set of log types to be read and processed by the Processor
     */
    public Processor(final Set<String> logsToRead) {

        this.logsToRead = logsToRead;
    }

    /**
     * Creates a new log event based on the provided parameters. The method evaluates the severity,
     * text, and other attributes to determine the type of event and generates a new {@code LogRecord}.
     *
     * @param time       The timestamp when the event occurred.
     * @param depth      The depth level of the event in the execution stack.
     * @param severity   The severity of the event, represented as an instance of {@code _Severity}.
     * @param text       The text message associated with the event.
     * @param stacktrace The stack trace information relevant to the event, if any.
     * @return A {@code LogRecord} object representing the newly created event.
     */
    @SuppressWarnings({"IfStatementWithTooManyBranches", "ValueOfIncrementOrDecrementUsed"})
    private static LogRecord createEvent(final long time, final int depth, final _Severity severity, final String text,
                                         final String stacktrace) {

        final LogRecord newLogRecord;
        final Set<String> ki = (null == exec) ? null : exec.issues().getKnownIssueFor(text);
        final Integer test = core.getCurrentTestNumber();
        if (isNotNull(ki) && !ki.isEmpty())
            newLogRecord = new LogRecord(test, time, new Event(++counter, KNOWN_ISSUE, depth, s(KNOWN_ISSUE.getString(),
                    SEM_CLN, ki, SEM_CLN, text), stacktrace));
        else if (isNotNull(severity)) {
            newLogRecord = new LogRecord(test, time, new Event(++counter, severity.type(), depth, text, stacktrace));
            if (isNotNull(exec)) exec.issues().add(newLogRecord);
        } else if (text.contains(KNOWN_ISSUE.getString()))
            newLogRecord = new LogRecord(test, time, new Event(++counter, KNOWN_ISSUE, depth, text, stacktrace));
        else newLogRecord = new LogRecord(test, time, new Event(++counter, EventType.UNDEFINED, depth, text, stacktrace));
        core.getResults().addEvent(newLogRecord);
        return newLogRecord;
    }

    /**
     * Creates a new {@code LogRecord} instance based on the provided time and log data.
     *
     * @param time The timestamp for the log record, representing the time the log event occurred.
     * @param data The {@code AbstractLogData} object containing the details of the log event.
     * @return A {@code LogRecord} object containing the generated log information.
     */
    private static LogRecord createLogRecord(final long time, final AbstractLogData data) {

        return new LogRecord(core.getCurrentTestNumber(), time, data);
    }

    /**
     * Defines a specific time based on a redefined time value or the time since a
     * defined starting point.
     *
     * @param redefinedTime an optional time value in milliseconds to be used instead of
     *                      the default time since a defined starting point. If null, the
     *                      method will use the time since start.
     * @return the computed time value based on the input redefinedTime or the
     * time since the defined starting point.
     */
    private static long defineTime(final Long redefinedTime) {

        return n(msSinceStart(), redefinedTime);
    }

    /**
     * Calculates and returns the current depth of execution.
     * <p>
     * This method evaluates whether the `exec` object is null. If `exec` is null,
     * the depth is considered to be 0. Otherwise, it retrieves the current depth
     * from the `exec` object.
     *
     * @return the current depth as an integer, or 0 if the `exec` object is null
     */
    private static int getDepth() {

        return (null == exec) ? 0 : exec.scenarios().depth();
    }

    /**
     * Determines if the given text contains any exception pattern from the predefined list.
     * Iterates through a collection of exception patterns and checks if the provided text
     * contains any of them, ignoring null or empty patterns.
     *
     * @param text The string to be checked for exception patterns.
     * @return true if the text contains any matching exception pattern, false otherwise.
     */
    private static boolean isException(final String text) {

        for (final String exception : exceptions) {
            if (isNull(exception)) continue;
            if (exception.isEmpty()) continue;
            if (text.contains(exception)) return true;
        }
        return false;
    }

    /**
     * Logs the provided string to the console using System.out.println.
     *
     * @param s the string to be printed to the console
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void logConsole(final String s) {

        System.out.println(s);
    }

    public final void getAllDriverLogs(final int depth, final Long redefinedTime) {

        if (isNotNull(exec)) {
            if (isNotNull(exec.wd())) {
                if (Routines.in(CHROME)) { // Old code
                    processLogType(LogType.BROWSER, depth, redefinedTime);
                    processLogType(LogType.CLIENT, depth, redefinedTime);
                    processLogType(LogType.DRIVER, depth, redefinedTime);
                } else readAllDriverLogs(depth, redefinedTime);
            }
        }
    }

    @Override
    public final Closeable getLogWriter() {

        return writer.getLogWriter();
    }

    @Override
    public final int getSpanLength() {

        return spanLength;
    }

    @Override
    public final Closeable getTelemetryWriter() {

        return writer.getTelemetryWriter();
    }

    /**
     * Logs a message with the given level and optional redefined time.
     *
     * @param level         The level of the log message.
     * @param message       The message to be logged.
     * @param redefinedTime The optional redefined time for the log.
     */
    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "AssignmentToStaticFieldFromInstanceMethod"})
    public final void log(final int level, final String message, final Long redefinedTime) {

        final int depth = getDepth(); // indentation of text
        getAllDriverLogs(depth, redefinedTime); // external logs

        final long time = defineTime(redefinedTime); // time for this record
        final AbstractLogData data = new Log(++counter, depth, level, message); // data for this log record
        final LogRecord logRecord = createLogRecord(time, data);
        final Telemetry telemetry = Telemetry.getNewTelemetry(time, (isNull(exec)) ? StringUtils.EMPTY :
                Stacktrace.getShort(Thread.currentThread().getStackTrace()));

        if (isSimilarityCheck(logRecord)) {
            processLogRecord(time, telemetry, logRecord);
            previousLogRecord = logRecord;
            process(telemetry);
        }
    }

    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "AssignmentToStaticFieldFromInstanceMethod"})
    public final void logEmptyLine() {

        core.sleep(1L, true);
        queue.add(new LogRecord(core.getCurrentTestNumber(), msSinceStart(), new LineFeed(++counter)));
        core.sleep(1L, true);
    }

    @SuppressWarnings({"ValueOfIncrementOrDecrementUsed", "AssignmentToStaticFieldFromInstanceMethod"})
    public final void logEmptyLine(final String shortStackTrace) {

        core.sleep(1L, true);
        final int depth = getDepth();
        queue.add(new LogRecord(core.getCurrentTestNumber(), msSinceStart(), new Log(++counter, depth, ZERO,
                shortStackTrace)));
        core.sleep(1L, true);
    }

    public final void logEvent(final Severity severity, final String message, final String shortStackTrace) {

        final int depth = getDepth();
        queue.add(createEvent(msSinceStart(), depth, severity, message, shortStackTrace));
    }

    /**
     * Processes the log records in the log queue.
     *
     * @param telemetry The telemetry object to be logged along with the log records.
     */
    public final void process(final Telemetry telemetry) {

        while (!queue.isEmpty()) {
            final _LogRecord r = queue.poll();
            final long previousId = isNull(upperLogRecord) ? ZERO : upperLogRecord.data().id();
            final String line = r.render(core.getTotalTests(), spanLength, previousId);
            logConsole(line); // render to Console
            logToFile(line); // render to File
            logTelemetry(telemetry); // persist Telemetry
            upperLogRecord = r;
        }
    }

    /**
     * Finds a log record that is similar to the provided log record based on specific criteria.
     * The method iterates through a collection of log records in reverse order and checks for similarity
     * in terms of timestamp and text content. If a matching log record is found, it is returned; otherwise, null is returned.
     *
     * @param next The log record to compare against the log records in the similarity collection.
     * @return The first log record from the similarity collection that matches the conditions or null if no match is found.
     */
    @SuppressWarnings({"DataFlowIssue", "BreakStatement"})
    private _LogRecord findSimilar(final _LogRecord next) {

        for (final Iterator<_LogRecord> it = similarity.descendingIterator(); it.hasNext(); ) {
            final _LogRecord logr = it.next();
            if (next.time() - logr.time() > Similarity.delta)
                break; // use of descendingIterator means there's no point to iterate further
            final String s0 = removeANSICodes(next.text()).trim();
            final String t0 = removeANSICodes(logr.text()).trim();
            final String s = s0.length() > Similarity.limit ? s0.substring(0, Similarity.limit) : null;
            final String t = t0.length() > Similarity.limit ? t0.substring(0, Similarity.limit) : null;
            if (!isAnyNull(s, t) && s.equals(t)) return logr;
        }
        return null;
    }

    /**
     * Determines if two log records are similar based on specific criteria.
     * The similarity is determined using processed forms of the text in the provided log records.
     * If the log records have identical processed text, the method will return false after logging the event.
     * If the log records are too similar with some overlapping textual content, an exception is thrown.
     *
     * @param logRecord The first log record to compare.
     * @param to        The second log record to compare against the first.
     * @return false if the log records have identical processed text. If the log records are too similar, an exception is
     * thrown.
     * @throws IllegalArgumentException If the log records are determined to be too similar with overlapping text content.
     */
    private boolean isSimilar(final _LogRecord logRecord, final _LogRecord to) {

        final String s0 = removeANSICodes(to.text()).substring(0, Similarity.limit);
        final String s1 = removeANSICodes(to.render(core.getTotalTests(), spanLength, null));
        final String s2 = removeANSICodes(logRecord.render(core.getTotalTests(), spanLength, null));
        final String s12 = s(s(_LFD_), q(s1), AND, s(_LFD_), q(s2));
        final String message = (s1.equals(s2)) ? b(DUPLICATED___, s12) : b(ADJACENT___, q(s0), IN, s12);
        final boolean result = !(s1.equals(s2));
        logEvent(S0, message, null);
        return result;
    }

    /**
     * Checks if the given log record meets the similarity criteria.
     * This method processes the log record to determine if it is unique enough or
     * if it is similar to an existing log record based on defined conditions.
     *
     * @param logRecord The log record to be assessed for similarity.
     * @return true if the log record is considered unique or processed successfully; false
     * if it is flagged as similar to an existing log record.
     */
    private boolean isSimilarityCheck(final _LogRecord logRecord) {

        if (Similarity.check && similarity.size() > ZERO) {
            final String next = (removeANSICodes(logRecord.text())).trim();
            if (next.length() > Similarity.limit) {
                if (!isException(next)) {
                    final _LogRecord to = findSimilar(logRecord);
                    if (isNotNull(to)) return isSimilar(logRecord, to);
                }
            }
        }
        similarity.add(logRecord);
        return true;
    }

    /**
     * Logs telemetry data using the provided telemetry writer. If the telemetry writer
     * is not available, logs an event instead.
     *
     * @param telemetry the telemetry object containing data to be logged
     */
    private void logTelemetry(final Telemetry telemetry) {

        try {
            if (isNull(writer.getTelemetryWriter()))
                logEvent(S0, Ansi.ansi(TELEMETRY_WRITER_IS_NOT___),
                        Stacktrace.getShort(Thread.currentThread().getStackTrace()));
            else writer.getTelemetryWriter().write(s(telemetry, System.lineSeparator()));
        } catch (final IOException e) {
            if (!"Stream closed".equals(e.getMessage())) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs the provided message to a file using the designated log writer.
     *
     * @param s the message to be logged
     */
    private void logToFile(final String s) {

        try {
            if (isNull(writer.getLogWriter()))
                logEvent(S0, Ansi.ansi(LOG_WRITER_IS_NOT___), Stacktrace.getShort(Thread.currentThread().getStackTrace()));
            else writer.getLogWriter().write(s(s, System.lineSeparator()));
        } catch (final IOException e) {
            if (!"Stream closed".equals(e.getMessage())) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Processes an external log entry and queues an event based on the log type and parameters.
     *
     * @param logEntry the log entry to process
     * @param logType  the type of the log entry, e.g. BROWSER or other types
     * @param depth    the depth value to associate with the log event
     * @param time     the timestamp to associate with the log event
     */
    private void processExternalLogEntry(final LogEntry logEntry, final String logType, final int depth, final long time) {

        if (logType.equals(LogType.BROWSER)) {
            final long timestamp = logEntry.getTimestamp() - Routines.startedAt(); // since test start
            final long difference = time - timestamp; // since capturing
            queue.add(createEvent(timestamp, depth, Severities.externalEvent, b(e(SQUARE, difference), e(SQUARE, u(BROWSER))
                    , s(logEntry)), EMPTY));
        } else queue.add(createEvent(time, depth, Severities.externalEvent, b(e(SQUARE, u(logType)), s(logEntry)), EMPTY));
    }

    /**
     * Processes a log record by setting its span, updating the maximum span length if applicable,
     * and adding the processed record to the queue. Also updates the dashboard with the log record's
     * text and telemetry information, if available.
     *
     * @param time      The time associated with the current log record.
     * @param telemetry The telemetry data to be logged or displayed.
     * @param logRecord The log record to be processed.
     */
    private void processLogRecord(final long time, final Telemetry telemetry, final _LogRecord logRecord) {

        if (isNotNull(previousLogRecord)) {
            final int span = previousLogRecord.setSpan(time);
            if (s(span).length() > spanLength) spanLength = s(span).length();
            queue.add((LogRecord) previousLogRecord);
        }
        if (isNotNull(exec) && isNotNull(core.getDashboard())) core.getDashboard().setInfo(logRecord.text(), telemetry);
    }

    /**
     * Processes a specific log type by reading log entries and processing them.
     *
     * @param logType       the type of the log to be processed
     * @param depth         the depth level used for processing the log entries
     * @param redefinedTime an optional redefined timestamp to be used in processing log entries
     */
    private void processLogType(final String logType, final int depth, final Long redefinedTime) {

        if (logsToRead.contains(logType)) {
            final WebDriver wd = exec.wd();
            if (null != wd) {
                final LogEntries ls = wd.manage().logs().get(logType);
                final long time = defineTime(redefinedTime);
                for (final LogEntry logEntry : ls) processExternalLogEntry(logEntry, logType, depth, time);
            }
        }
    }

    /**
     * Reads all available driver logs and processes each log type with the specified depth and redefined time.
     * Handles exceptions related to session or unsupported commands during log retrieval.
     *
     * @param depth         specifies the depth level to process the logs.
     * @param redefinedTime specifies a timestamp to reprocess logs based on a specific time.
     */
    private void readAllDriverLogs(final int depth, final Long redefinedTime) {

        if ((null == driverLogsAvailable) || driverLogsAvailable) {
            final WebDriver wd = exec.wd();
            if (null != wd) {
                try {
                    wd.manage().logs().getAvailableLogTypes().forEach(logType -> processLogType(logType, depth,
                            redefinedTime));
                } catch (final org.openqa.selenium.NoSuchSessionException e) {
                    driverLogsAvailable = null;
                } catch (final org.openqa.selenium.UnsupportedCommandException e) {
                    exec.logEvent(S2, works.lysenko.util.lang.U.UNABLE_TO_GET_EXTERNAL___);
                    driverLogsAvailable = false;
                }
            }
        }
    }
}
