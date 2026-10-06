package works.lysenko.base.output.loghtml;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.util.data.strs.Swap.s;

/**
 * Text formatting, string escaping, and ANSI sequence conversion utilities.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall"})
public final class LogFormatting {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern ARTIFACT_PAT = Pattern.compile(
            "(target/runs/[^/\\s\"&<>]+/[^/\\s\"&<>]+/)?((?:snapshots|data)/[^\"&<>\\r\\n]+?\\.(?:png|properties|log|json|html|txt))");
    private static final Pattern SIBLING_REPORT_PAT = Pattern.compile(
            "(?<![/a-zA-Z0-9])(\\d{13}\\.(?:tree|run)\\.html)");

    private LogFormatting() {
    }

    /**
     * Converts ANSI terminal color escape codes in a raw log line into styled HTML {@code <span>} elements.
     *
     * @param text raw text containing ANSI control sequences
     * @return HTML formatted string with inline CSS styling
     */
    public static String parseAnsi(final String text) {
        final StringBuilder sb = new StringBuilder();
        int openSpans = 0;
        int i = 0;
        while (i < text.length()) {
            final int esc = text.indexOf("\u001B[", i);
            if (esc == -1) {
                sb.append(escapeHtml(text.substring(i)));
                break;
            }
            sb.append(escapeHtml(text.substring(i, esc)));
            final int end = text.indexOf('m', esc + 2);
            if (end == -1) {
                sb.append(escapeHtml(text.substring(esc)));
                break;
            }
            final String codeStr = text.substring(esc + 2, end);
            final String[] codes = codeStr.split(";");
            boolean reset = false;
            for (final String c : codes) {
                if ("0".equals(c) || c.isEmpty()) reset = true;
            }
            if (reset) {
                while (openSpans > 0) {
                    sb.append("</span>");
                    openSpans--;
                }
            }
            final StringBuilder styles = new StringBuilder();
            for (final String c : codes) {
                switch (c) {
                    case "30": styles.append("color:#1e293b;"); break;
                    case "31": styles.append("color:#ef4444;"); break;
                    case "32": styles.append("color:#22c55e;"); break;
                    case "33": styles.append("color:#eab308;"); break;
                    case "34": styles.append("color:#3b82f6;"); break;
                    case "35": styles.append("color:#d946ef;"); break;
                    case "36": styles.append("color:#06b6d4;"); break;
                    case "37": styles.append("color:#e2e8f0;"); break;
                    case "90": styles.append("color:#64748b;"); break;
                    case "91": styles.append("color:#f87171;"); break;
                    case "92": styles.append("color:#4ade80;"); break;
                    case "93": styles.append("color:#facc15;"); break;
                    case "94": styles.append("color:#60a5fa;"); break;
                    case "95": styles.append("color:#e879f9;"); break;
                    case "96": styles.append("color:#22d3ee;"); break;
                    case "97": styles.append("color:#f8fafc;"); break;
                    case "40": styles.append("background-color:#1e293b;"); break;
                    case "41": styles.append("background-color:#dc2626; color:#ffffff;"); break;
                    case "42": styles.append("background-color:#16a34a; color:#000000; font-weight:bold; padding: 1px 0;"); break;
                    case "43": styles.append("background-color:#ca8a04; color:#000000;"); break;
                    case "44": styles.append("background-color:#2563eb; color:#ffffff;"); break;
                    case "45": styles.append("background-color:#c026d3; color:#ffffff;"); break;
                    case "46": styles.append("background-color:#0891b2; color:#ffffff;"); break;
                    case "47": styles.append("background-color:#f1f5f9; color:#0f172a;"); break;
                    case "100": styles.append("background-color:#475569;"); break;
                    case "101": styles.append("background-color:#ef4444;"); break;
                    case "102": styles.append("background-color:#22c55e;"); break;
                    case "103": styles.append("background-color:#eab308;"); break;
                    case "104": styles.append("background-color:#3b82f6;"); break;
                    case "105": styles.append("background-color:#d946ef;"); break;
                    case "106": styles.append("background-color:#06b6d4;"); break;
                    case "107": styles.append("background-color:#ffffff;"); break;
                    case "1": styles.append("font-weight:bold;"); break;
                    case "4": styles.append("text-decoration:underline;"); break;
                    default: break;
                }
            }
            if (styles.length() > 0) {
                sb.append(s("<span style=\"", styles, "\">"));
                openSpans++;
            }
            i = end + 1;
        }
        while (openSpans > 0) {
            sb.append("</span>");
            openSpans--;
        }
        return linkifyArtifacts(sb.toString());
    }

    /**
     * Converts artifact paths and sibling report filenames in text into hyperlinked HTML anchors.
     *
     * @param htmlText raw HTML or log text containing relative artifact references
     * @return text with clickable HTML artifact links
     */
    public static String linkifyArtifacts(final String htmlText) {
        final Matcher m = ARTIFACT_PAT.matcher(htmlText);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            final String prefix = (null != m.group(1)) ? m.group(1) : "";
            final String rel = m.group(2);
            final String replacement = s(prefix, "<a class=\"log-artifact-link\" href=\"", rel, "\" target=\"_blank\" title=\"Open artifact: ", rel, "\">", rel, "</a>");
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return SIBLING_REPORT_PAT.matcher(sb.toString()).replaceAll(
                "<a class=\"log-artifact-link\" href=\"$1\" target=\"_blank\" title=\"Open report: $1\">$1</a>");
    }

    /**
     * Extracts operation duration span in milliseconds from a formatted log line.
     *
     * @param line raw log line
     * @return extracted duration in milliseconds, or 0 if unparseable
     */
    public static int extractLineSpan(final String line) {
        final String clean = ANSI_PATTERN.matcher(line).replaceAll("").trim();
        final Matcher m1 = LINE_WITH_TEST_RE.matcher(clean);
        if (m1.matches()) {
            final String s = m1.group(4);
            if (null != s && s.trim().matches("\\d+")) return Integer.parseInt(s.trim());
            return 0;
        }
        final Matcher m2 = LINE_NO_TEST_RE.matcher(clean);
        if (m2.matches()) {
            final String s = m2.group(3);
            if (null != s && s.trim().matches("\\d+")) return Integer.parseInt(s.trim());
            return 0;
        }
        return 0;
    }

    /**
     * Escapes HTML control characters in a string.
     *
     * @param s string to escape
     * @return HTML-safe escaped string
     */
    public static String escapeHtml(final String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Escapes special JSON character sequences in a string for embedded script literal serialization.
     *
     * @param s string to escape
     * @return JSON-escaped string
     */
    public static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /**
     * Formats a duration in seconds into a human-readable string (e.g., "12 s 345 ms" or "500 ms").
     *
     * @param sec duration in seconds
     * @return formatted duration string
     */
    public static String formatDeltaTime(final double sec) {
        if (sec < 1.0) {
            return s(Math.round(sec * 1000), " ms");
        }
        final int s = (int) sec;
        final int ms = (int) Math.round((sec - s) * 1000);
        return ms > 0 ? String.format(Locale.ROOT, "%d s %03d ms", s, ms) : s(s, " s");
    }

    /**
     * Formats a duration in milliseconds into a concise string (e.g., "250ms" or "2.50s").
     *
     * @param milliseconds duration in milliseconds
     * @return formatted duration string
     */
    public static String formatCompletionDuration(final long milliseconds) {
        return milliseconds < 1_000L ? s(milliseconds, "ms")
                : String.format(Locale.ROOT, "%.2fs", milliseconds / 1_000.0);
    }

    /**
     * Formats an elapsed timestamp in milliseconds into a minute:second.millisecond moment string (e.g. "1:23.456").
     *
     * @param milliseconds elapsed time in milliseconds
     * @return formatted moment string
     */
    public static String formatCompletionMoment(final long milliseconds) {
        final long minutes = milliseconds / 60_000L;
        final long seconds = (milliseconds / 1_000L) % 60L;
        return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, milliseconds % 1_000L);
    }

    /**
     * Computes a rounded ceiling value for chart Y-axis tick intervals.
     *
     * @param max maximum value to scale
     * @return rounded ceiling value
     */
    public static double getNiceCeil(final double max) {
        if (max <= 0.0) return 1.0;
        final double mag = Math.pow(10, Math.floor(Math.log10(max)));
        final double norm = max / mag;
        final double factor;
        if (norm <= 1.0) factor = 1.0;
        else if (norm <= 2.0) factor = 2.0;
        else if (norm <= 2.5) factor = 2.5;
        else if (norm <= 5.0) factor = 5.0;
        else factor = 10.0;
        return factor * mag;
    }

    /**
     * Calculates logarithmic Y-coordinate for plot scaling.
     *
     * @param val       metric value
     * @param maxVal    maximum range value
     * @param marginTop top margin offset in pixels
     * @param plotH     plot height in pixels
     * @return Y-coordinate pixel value
     */
    public static double logScaleY(final double val, final double maxVal, final double marginTop, final double plotH) {
        if (val <= 0.0) return marginTop + plotH;
        final double logMax = Math.log10(Math.max(1.0, maxVal) + 1.0);
        final double logVal = Math.log10(Math.min(maxVal, val) + 1.0);
        final double norm = Math.min(1.0, Math.max(0.0, logVal / logMax));
        return (marginTop + plotH) - norm * plotH;
    }

    /**
     * Calculates the horizontal X coordinate for a completion point in a chart.
     *
     * @param index           zero-based completion point index
     * @param completionCount total number of completion points
     * @param left            left plot margin in pixels
     * @param plotWidth       plot width in pixels
     * @return calculated X coordinate
     */
    public static int completionX(
            final int index, final int completionCount, final int left, final int plotWidth) {
        if (completionCount <= 1) return left + (plotWidth / 2);
        return left + (int) Math.round((index / (double) (completionCount - 1)) * plotWidth);
    }

    /**
     * Calculates total elapsed duration in seconds for boot and configuration log statements.
     *
     * @param lines list of boot or configuration log lines
     * @return elapsed time in seconds
     */
    public static double calculateBootSpentSeconds(final List<String> lines) {
        long totalMs = 0;
        final Pattern p = Pattern.compile("spent\\s+(?:(\\d+)\\s*s\\s*)?(\\d+)\\s*ms");
        for (final String l : lines) {
            if (l.contains("spent less than millisecond")) {
                totalMs += 1;
            } else {
                final Matcher m = p.matcher(l);
                if (m.find()) {
                    final long s = (null != m.group(1)) ? Long.parseLong(m.group(1)) : 0L;
                    final long ms = Long.parseLong(m.group(2));
                    totalMs += (s * 1000L) + ms;
                }
            }
        }
        return totalMs / 1000.0;
    }

    /**
     * Converts a string representation of timestamp seconds to a double value.
     *
     * @param s timestamp string
     * @return duration in seconds
     */
    public static double toSeconds(final String s) {
        if (s == null) return 0.0;
        final String clean = s.trim();
        try {
            return Double.parseDouble(clean.startsWith(".") ? s("0", clean) : clean);
        } catch (final NumberFormatException e) {
            return 0.0;
        }
    }
}
