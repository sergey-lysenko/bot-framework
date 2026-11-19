package works.lysenko.base.eta.calc;

import works.lysenko.base.eta.model.EtaSnapshot;

import java.util.Locale;

/**
 * Stateless formatting and JSON serialization utilities for ETA durations, patience metrics, and snapshots.
 */
public record EtaFormatter() {

    /**
     * Placeholder duration string when ETA is unavailable or initializing.
     */
    public static final String PLACEHOLDER_DURATION = "--:--";

    /**
     * Terminal duration text when execution has completed.
     */
    public static final String DONE_TEXT = "Done";

    /**
     * Compact GUI/CLI label when ETA is unavailable.
     */
    public static final String LABEL_UNAVAILABLE = "(ETA --:--)";

    /**
     * Compact GUI/CLI label when execution has completed.
     */
    public static final String LABEL_DONE = "(ETA Done)";

    /**
     * Formats milliseconds as {@code MM:SS} (under 1 hour) or {@code HH:MM:SS}.
     *
     * @param ms duration in milliseconds
     * @return formatted duration string
     */
    public static String formatMs(final long ms) {
        final long totalSec = Math.max(0L, ms / 1000L);
        final long hours = totalSec / 3600L;
        final long minutes = (totalSec % 3600L) / 60L;
        final long seconds = totalSec % 60L;

        if (hours > 0L) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    /**
     * Formats a wandering patience percentage trimming trailing decimal zeros.
     *
     * @param patiencePercentage wandering patience percentage {@code [0..100]}
     * @return compact numeric string representation
     */
    public static String formatPatience(final double patiencePercentage) {
        final double rounded = Math.round(patiencePercentage * 100.0) / 100.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-6) {
            return Long.toString(Math.round(rounded));
        }
        final String formatted = String.format(Locale.ROOT, "%.2f", rounded);
        return formatted.endsWith("0") ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    /**
     * Serializes an {@link EtaSnapshot} as a compact single-line JSON object for {@code dash/eta}.
     *
     * @param snapshot snapshot to serialize
     * @param title    current dashboard title string (may be {@code null})
     * @return single-line JSON representation
     */
    public static String toJson(final EtaSnapshot snapshot, final String title) {
        final StringBuilder sb = new StringBuilder(384);
        sb.append('{');
        sb.append("\"state\":\"").append(snapshot.state().name()).append("\",");
        sb.append("\"mode\":\"").append(snapshot.mode().name()).append("\",");
        sb.append("\"runId\":").append(snapshot.runId()).append(',');
        sb.append("\"updatedAtEpochMs\":").append(snapshot.updatedAtEpochMs()).append(',');
        sb.append("\"elapsedMs\":").append(snapshot.elapsedMs()).append(',');
        sb.append("\"remainingMs\":").append(snapshot.remainingMs()).append(',');
        sb.append("\"correctionMs\":").append(snapshot.correctionMs()).append(',');
        sb.append("\"correctedRemainingMs\":").append(snapshot.correctedRemainingMs()).append(',');
        sb.append("\"currentTest\":").append(snapshot.currentTest()).append(',');
        sb.append("\"completedTests\":").append(snapshot.completedTests()).append(',');
        sb.append("\"totalTests\":").append(snapshot.totalTests()).append(',');
        sb.append("\"executed\":").append(snapshot.executed()).append(',');
        sb.append("\"total\":").append(snapshot.total()).append(',');
        sb.append("\"progressPercent\":").append(round2(snapshot.progressPercent())).append(',');
        sb.append("\"patiencePercent\":").append(round2(snapshot.patiencePercent())).append(',');
        sb.append("\"formattedRemaining\":\"").append(escapeJson(snapshot.formattedRemaining())).append("\",");
        sb.append("\"formattedCorrectedRemaining\":\"").append(escapeJson(snapshot.formattedCorrectedRemaining())).append("\",");
        sb.append("\"label\":\"").append(escapeJson(snapshot.label())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(null == title ? "" : title)).append('\"');
        sb.append('}');
        return sb.toString();
    }

    private static String round2(final double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0.0";
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String escapeJson(final String s) {
        if (null == s || s.isEmpty()) return "";
        final StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
