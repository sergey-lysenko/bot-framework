package works.lysenko.base.logger.renderer;

import org.jetbrains.annotations.NonNls;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.LineFeed;

import java.text.DecimalFormat;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.E.*;
import static works.lysenko.util.lang.W.*;
import static works.lysenko.util.lang.word.J.*;
import static works.lysenko.util.prop.logs.Md.*;
import static works.lysenko.util.prop.logs.Time.stamp;
import static works.lysenko.util.spec.Symbols.*;

public class MarkdownLogRenderer implements LogRenderer {

    @NonNls
    public static final String S_S = " _(%s, %s)_";
    @NonNls
    public static final String S1 = " _(%s)_";
    private static final double THOUSAND = 1000.0;

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public final String render(final _LogRecord record, final Integer totalTests, final int spanLength, final Long previousId) {
        final String message = renderData(record.data());
        final String mdStamp = renderMdStamp(record);
        if (mdStamp.isEmpty()) {
            return message;
        }
        return b(message, mdStamp);
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "ChainOfInstanceofChecks"})
    private static String renderData(final _LogData data) {
        if (data instanceof LineFeed) return EMPTY;
        else if (data instanceof final Event e) {
            final String severityMarker = switch (e.severity()) {
                case S1 -> WARNING_MARKER;
                case S2 -> ERROR_MARKER;
                case S0 -> EMPTY;
                default -> EMPTY;
            };
            String logText = s(SPACE.repeat(e.depth()), "- ", severityMarker, e.message());
            if (e.stacktrace() != null && !e.stacktrace().isEmpty()) logText += s(JAVA_START, e.stacktrace(), JAVA_END);
            return logText;
        } else if (data instanceof final Log ld) return s(SPACE.repeat(ld.depth()), "- ", ld.message());
        return data.message();
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "MethodWithMultipleReturnPoints"})
    private static String renderMdStamp(final _LogRecord record) {

        String stampPart = EMPTY;
        String durationPart = EMPTY;
        if (timestamps) stampPart = s(new DecimalFormat(stamp).format(record.time() / THOUSAND), "s");
        if (durations && -1 != record.span()) durationPart = s(_PLUS_, record.span(), MS);
        if (stampPart.isEmpty() && durationPart.isEmpty()) return EMPTY;
        if (!stampPart.isEmpty() && !durationPart.isEmpty()) return String.format(S_S, stampPart, durationPart);
        else if (!stampPart.isEmpty()) return String.format(S1, stampPart);
        else return String.format(S1, durationPart);
    }
}
