package works.lysenko.base.logger.renderer;

import org.jetbrains.annotations.NonNls;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.data.type.logr.type.Event;
import works.lysenko.util.data.type.logr.type.Log;
import works.lysenko.util.data.type.logr.type.LineFeed;
import works.lysenko.util.data.type.logr.type.Section;
import works.lysenko.util.func.type.Fractions;

import java.text.DecimalFormat;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.data.enums.Ansi.removeANSICodes;
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
    private static final double THOUSAND = 1000.0;
    @NonNls
    private static final String TABLE_HEADER = "| Log Message | Stack Trace | Duration (ms) |" + System.lineSeparator() + "| :--- | :--- | :--- |";

    private boolean needsTableHeader = true;
    private String pendingSectionHeader = null;
    private int lastTestNumber = -1;

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public final synchronized String render(final _LogRecord record, final Integer totalTests, final int spanLength, final Long previousId) {
        if (record.test() != lastTestNumber) {
            lastTestNumber = record.test();
            needsTableHeader = true;
            pendingSectionHeader = null;
        }

        final _LogData data = record.data();
        if (data instanceof LineFeed) {
            needsTableHeader = true;
            return EMPTY;
        }

        if (data instanceof final Section sec) {
            final String title = Fractions.deUnicode(removeANSICodes(sec.title()));
            final String cleanStack = formatStacktrace(sec.stacktrace());
            return processSectionHeader(sec.depth(), title, cleanStack);
        }

        final String cleanMsg = formatMessage(data);
        final String cleanStack = formatStacktrace(data.stacktrace());
        final String stamp = renderMdStamp(record);

        if (isHeaderMessage(cleanMsg)) {
            final String title = extractHeaderTitle(cleanMsg);
            return processSectionHeader(data.depth(), title, cleanStack);
        }

        final StringBuilder out = new StringBuilder();
        if (pendingSectionHeader != null) {
            out.append(pendingSectionHeader).append(System.lineSeparator()).append(TABLE_HEADER);
            pendingSectionHeader = null;
            needsTableHeader = false;
        } else if (needsTableHeader) {
            out.append(System.lineSeparator()).append(TABLE_HEADER);
            needsTableHeader = false;
        }

        if (out.length() > 0) {
            out.append(System.lineSeparator());
        }

        out.append(String.format("| %s | %s | %s |",
                escapePipe(cleanMsg),
                cleanStack.isEmpty() ? EMPTY : "`" + escapePipe(cleanStack) + "`",
                stamp));

        return out.toString();
    }

    private static boolean isHeaderMessage(final String msg) {
        if (msg == null) return false;
        final String trimmed = msg.trim();
        if (trimmed.length() >= 3 && trimmed.startsWith("=") && trimmed.endsWith("=")) {
            return true;
        }
        return trimmed.startsWith("▷ ") || trimmed.startsWith("◆ ") || trimmed.startsWith("◼ ");
    }

    private static String extractHeaderTitle(final String msg) {
        if (msg == null) return EMPTY;
        String trimmed = msg.trim();
        while (trimmed.startsWith("=")) {
            trimmed = trimmed.substring(1).trim();
        }
        while (trimmed.endsWith("=")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String processSectionHeader(final int depth, final String title, final String cleanStack) {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append(getHeadingPrefix(depth)).append(title);
        if (!cleanStack.isEmpty()) {
            sb.append(" `").append(escapePipe(cleanStack)).append("`");
        }
        pendingSectionHeader = sb.toString();
        needsTableHeader = true;
        return EMPTY;
    }

    private static String getHeadingPrefix(final int depth) {
        final int level = Math.min(6, Math.max(3, 3 + (depth > 0 ? depth - 1 : 0)));
        return "#".repeat(level) + " ";
    }

    private static String formatMessage(final _LogData data) {
        String msg;
        if (data instanceof final Event e) {
            final String severityMarker = switch (e.severity()) {
                case S1 -> WARNING_MARKER;
                case S2 -> ERROR_MARKER;
                case S0 -> EMPTY;
                default -> EMPTY;
            };
            final String clean = removeANSICodes(e.message());
            msg = s(SPACE.repeat(e.depth()), severityMarker, clean);
        } else if (data instanceof final Log ld) {
            msg = s(SPACE.repeat(ld.depth()), removeANSICodes(ld.message()));
        } else {
            msg = removeANSICodes(data.message());
        }
        msg = Fractions.deUnicode(msg);
        return replaceLeadingSpacesWithNbsp(msg);
    }

    private static String replaceLeadingSpacesWithNbsp(final String str) {
        if (str == null || str.isEmpty()) return EMPTY;
        int count = 0;
        while (count < str.length() && str.charAt(count) == ' ') {
            count++;
        }
        if (count == 0) return str;
        return "&nbsp;".repeat(count) + str.substring(count);
    }

    private static String formatStacktrace(final String stacktrace) {
        if (stacktrace == null || stacktrace.isEmpty()) return EMPTY;
        return removeANSICodes(stacktrace).replace("\n", "<br>").replace("\r", "");
    }

    private static String escapePipe(final String input) {
        if (input == null) return EMPTY;
        return input.replace("|", "\\|");
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "MethodWithMultipleReturnPoints"})
    private static String renderMdStamp(final _LogRecord record) {

        String stampPart = EMPTY;
        String durationPart = EMPTY;
        if (timestamps) stampPart = s(new DecimalFormat(stamp).format(record.time() / THOUSAND), "s");
        if (durations && -1 != record.span()) durationPart = s(record.span());
        if (stampPart.isEmpty() && durationPart.isEmpty()) return EMPTY;
        if (!stampPart.isEmpty() && !durationPart.isEmpty()) return s(stampPart, ", ", durationPart);
        else if (!stampPart.isEmpty()) return stampPart;
        else return durationPart;
    }
}
