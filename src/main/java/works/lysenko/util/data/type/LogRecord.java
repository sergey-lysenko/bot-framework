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
     * Renders the complete log record string including stamp, test info, and message.
     *
     * @param totalTests   total number of tests
     * @param spanLength   the length of the span string formatting
     * @param previousId   the previous log record ID
     * @return the rendered log record string
     */
    public final String render(final Integer totalTests, final int spanLength, final Long previousId) {

        final String message = data.render();
        final String test = renderTest(totalTests);
        final boolean gray = isNull(this.test) || EMPTY.equals(message);
        final boolean yellow = isNotNull(previousId) && previousId + 1 != data.id();
        final String stamp = gray(gray, s(test, yb(yellow, renderId()), renderTime(spanLength)));
        return b(stamp, message);
    }

    /**
     * Renders the test indicator with dynamic width based on total or active tests.
     *
     * @param totalTests total number of tests
     * @return the rendered test string enclosed in square brackets
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final String renderTest(final Integer totalTests) {

        final int currentTests = isNotNull(core) ? core.getTestsCount() : 0;
        final int currentWidth = (0 == currentTests) ? 0 : s(currentTests).length();
        final int totalWidth = (null == totalTests) ? 0 : s(totalTests).length();
        final int testWidth = (null == test) ? 0 : s(test).length();
        int width = max(max(totalWidth, currentWidth), testWidth);
        if (0 == width && isNotNull(core)) {
            final int paths = core.getActiveScenarioPaths();
            if (0 < paths) {
                width = s(paths).length();
            }
        }
        if (0 == width) {
            width = 2;
        }
        if (isNull(test)) return e(SQUARE, SPACE.repeat(width));
        final String testN = s(test);
        final String prefix = SPACE.repeat(max(0, width - testN.length()));
        return e(SQUARE, s(prefix, testN));
    }

    /**
     * Renders the log data ID enclosed in square brackets.
     *
     * @return the rendered ID string
     */
    public final String renderId() {

        return e(SQUARE, s(data.id()));
    }

    /**
     * Renders the timestamp and time span with appropriate color coding.
     *
     * @param width the width formatting parameter for span
     * @return the rendered time and span string
     */
    @SuppressWarnings({"OverlyLongLambda", "ConstantOnWrongSideOfComparison"})
    public final String renderTime(final int width) {

        final String renderedTime = e(SQUARE, new DecimalFormat(stamp).format(time / 1000.0));
        final String stringedSpan = (span == -1L) ? EMPTY : s(span);
        final String renderedSpan = EMPTY.equals(stringedSpan)
                ? SPACE.repeat(width + 2)
                : e(SQUARE, s(SPACE.repeat(width - stringedSpan.length()), stringedSpan));

        final Function<String, String> spanColor = rendered -> {
            if (span > red) return rb(renderedSpan);
            if (span > yellow) return yb(renderedSpan);
            if (span > green) return gb(renderedSpan);
            if (span > blue) return bb(renderedSpan);
            return renderedSpan;
        };

        final String coloredSpan = spanColor.apply(renderedSpan);
        return s(renderedTime, coloredSpan);
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
     * Returns the test number.
     *
     * @return the test integer
     */
    public final int test() {

        return test;
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
