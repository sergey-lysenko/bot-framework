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

    private final Integer test;
    private final _LogData data;
    private final long time;
    private int span;

    /**
     * Default constructor
     *
     * @param test test
     * @param time timestamp
     * @param data {@link AbstractLogData}
     */
    public LogRecord(final Integer test, final long time, final _LogData data) {

        this.test = test;
        this.data = data;
        this.time = time;
        span = -1;
    }

    public final _LogData data() {

        return data;
    }

    public final String render(final Integer totalTests, final int spanLength, final Long previousId) {

        final String message = data.render();
        final String test = renderTest(totalTests);
        final boolean gray = isNull(this.test) || EMPTY.equals(message);
        final boolean yellow = isNotNull(previousId) && previousId + 1 != data.id();
        final String stamp = gray(gray, s(test, yb(yellow, renderId()), renderTime(spanLength)));
        return b(stamp, message);
    }

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

    public final String renderId() {

        return e(SQUARE, s(data.id()));
    }

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

    @SuppressWarnings("NumericCastThatLosesPrecision")
    public final int setSpan(final long currentTime) {

        span = (int) (currentTime - time);
        if (span > threshold) logEvent(severity, b(t(span), IS_TOO_MUCH_TIME___));
        return span;
    }

    public final int test() {

        return test;
    }

    public final String text() {

        return data.message();
    }

    public final Long time() {

        return time;
    }
}
