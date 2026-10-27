package works.lysenko.util.data.type;

import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.data.type.logr.AbstractLogData;

import java.text.DecimalFormat;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.sn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Time.t;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.I.IS_TOO_MUCH_TIME___;
import static works.lysenko.util.prop.logs.LongOperation.severity;
import static works.lysenko.util.prop.logs.LongOperation.threshold;
import static works.lysenko.util.prop.logs.Rate.*;
import static works.lysenko.util.prop.logs.Time.stamp;

/**
 * Represents a log entry containing information about a specific point in time during an operation or process.
 * Encapsulates details such as the test in which the log was recorded, the timestamp, the log data, and the time span.
 */
@SuppressWarnings("CallToSuspiciousStringMethod")
public class LogRecord implements _LogRecord {

    /** The test identifier associated with the log record. */
    private final Integer test;
    /** The log data containing the message and metadata. */
    private final _LogData data;
    /** The timestamp when the log record was created. */
    private final long time;
    /** The time span between this record and a subsequent event. */
    private int span;

    /**
     * Constructs a new LogRecord with the specified test number, timestamp, and log data.
     *
     * @param test the test number
     * @param time the timestamp in milliseconds
     * @param data the log data object implementing {@link _LogData}
     */
    public LogRecord(final Integer test, final long time, final _LogData data) {

        this.test = test;
        this.data = data;
        this.time = time;
        span = -1;
    }

    /**
     * Returns the log data.
     *
     * @return the {@link _LogData} instance
     */
    public final _LogData data() {

        return data;
    }

    /**
     * Sets the time span relative to the current time and logs a warning if it exceeds the threshold.
     *
     * @param currentTime the current time in milliseconds
     * @return the calculated span value
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public final int setSpan(final long currentTime) {

        span = (int) (currentTime - time);
        if (span > threshold) logEvent(severity, b(t(span), IS_TOO_MUCH_TIME___));
        return span;
    }

    /**
     * Retrieves the time span.
     *
     * @return the time span
     */
    public final int span() {

        return span;
    }

    /**
     * Returns the test number.
     *
     * @return the test integer
     */
    public final int test() {

        return null == test ? 0 : test;
    }

    /**
     * Returns the log message text from data.
     *
     * @return the message string
     */
    public final String text() {

        return data.message();
    }

    /**
     * Returns the record timestamp.
     *
     * @return the timestamp long value
     */
    public final Long time() {

        return time;
    }
}
