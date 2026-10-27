package works.lysenko.base.logger.renderer;

import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.LineFeed;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.enums.Ansi;

import java.text.DecimalFormat;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.Base.core;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.prop.logs.Rate.blue;
import static works.lysenko.util.prop.logs.Rate.green;
import static works.lysenko.util.prop.logs.Rate.red;
import static works.lysenko.util.prop.logs.Rate.yellow;
import static works.lysenko.util.prop.logs.Time.stamp;

public class AnsiLogRenderer implements LogRenderer {

    @Override
    public String render(_LogRecord record, Integer totalTests, int spanLength, Long previousId) {
        final _LogData data = record.data();
        final String message = renderData(data);
        final String testStr = renderTest(record, totalTests);
        final boolean isGray = (record.test() == 0) || EMPTY.equals(message);
        final boolean isYellow = isNotNull(previousId) && previousId + 1 != data.id();
        
        final String stampStr = gray(isGray, s(testStr, yb(isYellow, renderId(record)), renderTime(record, spanLength)));
        return b(stampStr, message);
    }

    private String renderData(_LogData data) {
        if (data instanceof LineFeed) {
            return EMPTY;
        } else if (data instanceof Event e) {
            final Severity severity = e.severity();
            if (isNotNull(severity)) {
                return b(s(SPACE.repeat(e.depth()), Ansi.ansi(e.message(), severity.color())), gray(e.stacktrace()));
            }
            return null;
        } else if (data instanceof Log l) {
            final String baseMsg = (0 == l.level()) ? l.message() : b(s(works.lysenko.util.spec.Symbols._BULLT_).repeat(l.level()), l.message());
            final String indented = s(SPACE.repeat(l.depth()), baseMsg);
            if (isNotNull(l.stacktrace()) && !l.stacktrace().isEmpty()) {
                return b(indented, gray(l.stacktrace()));
            }
            return indented;
        } else if (data instanceof works.lysenko.util.data.type.logr.type.Section s) {
            final String formattedMsg = bb(e(works.lysenko.util.spec.Symbols._EQUAL_, e(s.title())));
            final String indented = s(SPACE.repeat(s.depth()), formattedMsg);
            if (isNotNull(s.stacktrace()) && !s.stacktrace().isEmpty()) {
                return b(indented, gray(s.stacktrace()));
            }
            return indented;
        }
        return data.message();
    }

    private String renderTest(_LogRecord record, Integer totalTests) {
        final int currentTests = isNotNull(core) ? core.getTestsCount() : 0;
        final int currentWidth = (0 == currentTests) ? 0 : s(currentTests).length();
        final int totalWidth = (null == totalTests) ? 0 : s(totalTests).length();
        final int testWidth = (record.test() == 0) ? 0 : s(record.test()).length();
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
        if (record.test() == 0) return e(SQUARE, SPACE.repeat(width));
        final String testN = s(record.test());
        final String prefix = SPACE.repeat(max(0, width - testN.length()));
        return e(SQUARE, s(prefix, testN));
    }

    private String renderId(_LogRecord record) {
        return e(SQUARE, s(record.data().id()));
    }

    @SuppressWarnings({"OverlyLongLambda", "ConstantOnWrongSideOfComparison"})
    private String renderTime(_LogRecord record, int width) {
        final String renderedTime = e(SQUARE, new DecimalFormat(stamp).format(record.time() / 1000.0));
        final int span = record.span();
        final String stringedSpan = (span == -1) ? EMPTY : s(span);
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
}
