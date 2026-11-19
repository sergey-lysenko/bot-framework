package works.lysenko.base.output.svg;

import works.lysenko.base.eta.EtaCalculator;
import works.lysenko.base.eta.model.EtaSnapshot;
import works.lysenko.base.eta.model.EtaState;

import java.util.Locale;

import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Shared dark-slate visual styling, SVG chrome, translucent bordered header badges,
 * and timing/ETA formatters unified across Tree Progression and Scenario Progression frames.
 */
public final class ProgressionChrome {

    public static final int HEADER_HEIGHT_BASE = 60;
    public static final int BADGE_HEIGHT = 24;
    public static final int BADGE_GAP = 8;

    public static final String BG_CANVAS = "#0F172A";
    public static final String DOT_GRID_COLOR = "#1E293B";
    public static final String HEADER_BG = "#1E293B";
    public static final String HEADER_BORDER = "#334155";

    public static final String ACCENT_SKY = "#38BDF8";
    public static final String ACCENT_SKY_TEXT = "#7DD3FC";
    public static final String ACCENT_GREEN = "#22C55E";
    public static final String ACCENT_GREEN_TEXT = "#4ADE80";
    public static final String ACCENT_AMBER = "#F59E0B";
    public static final String ACCENT_AMBER_TEXT = "#FBBF24";
    public static final String ACCENT_RED = "#EF4444";
    public static final String ACCENT_RED_TEXT = "#FCA5A5";
    public static final String ACCENT_CYAN = "#06B6D4";
    public static final String ACCENT_CYAN_TEXT = "#67E8F9";
    public static final String ACCENT_INDIGO = "#818CF8";
    public static final String ACCENT_INDIGO_TEXT = "#A5B4FC";

    public static final String SLATE_FILL = "#334155";
    public static final String SLATE_BORDER = "#475569";

    public static final String TEXT_PRIMARY = "#F8FAFC";
    public static final String TEXT_SECONDARY = "#CBD5E1";
    public static final String TEXT_MUTED = "#94A3B8";
    public static final String TEXT_DIM = "#64748B";

    /**
     * Visual tokens for a translucent bordered header badge pill.
     *
     * @param fill          background fill hex
     * @param fillOpacity   background fill opacity
     * @param stroke        border stroke hex
     * @param strokeOpacity border stroke opacity
     * @param textColor     foreground label hex
     */
    public record BadgeStyle(
            String fill,
            double fillOpacity,
            String stroke,
            double strokeOpacity,
            String textColor) {
    }

    /**
     * Resolved ETA badge label and semantic style.
     *
     * @param text  badge display text (e.g. {@code "ETA: 03:45"}, {@code "ETA: Done"}, {@code "ETA: --:--"})
     * @param style visual badge style
     */
    public record EtaBadge(String text, BadgeStyle style) {
    }

    public static final BadgeStyle BADGE_SKY = new BadgeStyle(ACCENT_SKY, 0.14, ACCENT_SKY, 0.65, ACCENT_SKY_TEXT);
    public static final BadgeStyle BADGE_GREEN = new BadgeStyle(ACCENT_GREEN, 0.14, ACCENT_GREEN, 0.65, ACCENT_GREEN_TEXT);
    public static final BadgeStyle BADGE_AMBER = new BadgeStyle(ACCENT_AMBER, 0.14, ACCENT_AMBER, 0.65, ACCENT_AMBER_TEXT);
    public static final BadgeStyle BADGE_INDIGO = new BadgeStyle(ACCENT_INDIGO, 0.14, ACCENT_INDIGO, 0.65, ACCENT_INDIGO_TEXT);
    public static final BadgeStyle BADGE_SLATE = new BadgeStyle(SLATE_FILL, 0.45, SLATE_BORDER, 0.85, TEXT_SECONDARY);

    private ProgressionChrome() {
    }

    /**
     * Appends the shared {@code #dot-grid} SVG pattern definition.
     *
     * @param sb      target SVG builder
     * @param headerY vertical offset matching the bottom of the header bar
     */
    public static void appendDotGridPattern(final StringBuilder sb, final int headerY) {
        sb.append(String.format(Locale.US,
                "    <pattern id=\"dot-grid\" width=\"24\" height=\"24\" patternUnits=\"userSpaceOnUse\" y=\"%d\">\n",
                headerY));
        sb.append("      <rect width=\"24\" height=\"24\" fill=\"").append(BG_CANVAS).append("\" />\n");
        sb.append("      <circle cx=\"1\" cy=\"1\" r=\"1\" fill=\"").append(DOT_GRID_COLOR).append("\" opacity=\"0.7\" />\n");
        sb.append("    </pattern>\n");
    }

    /**
     * Appends the full-bleed dark canvas background and dot-grid overlay beneath the header bar.
     *
     * @param sb      target SVG builder
     * @param width   canvas width
     * @param height  canvas height
     * @param headerH header height in canvas coordinates
     */
    public static void appendCanvasBackground(final StringBuilder sb, final int width, final int height, final int headerH) {
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"").append(BG_CANVAS).append("\" />\n");
        sb.append(String.format(Locale.US,
                "  <rect x=\"0\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"url(#dot-grid)\" />\n",
                headerH, width, Math.max(0, height - headerH)));
    }

    /**
     * Appends the full-width top header bar background and bottom divider line.
     *
     * @param sb     target SVG builder
     * @param localW width in HUD coordinate space
     */
    public static void appendHeaderBar(final StringBuilder sb, final int localW) {
        sb.append(String.format(Locale.US,
                "    <rect x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" fill=\"%s\" />\n",
                localW, HEADER_HEIGHT_BASE, HEADER_BG));
        sb.append(String.format(Locale.US,
                "    <line x1=\"0\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\" stroke-width=\"1\" />\n",
                HEADER_HEIGHT_BASE, localW, HEADER_HEIGHT_BASE, HEADER_BORDER));
    }

    /**
     * Appends the header accent indicator dot and Title Case frame title.
     *
     * @param sb        target SVG builder
     * @param leftX     left margin coordinate
     * @param titleText Title Case heading text
     */
    public static void appendHeaderTitle(final StringBuilder sb, final int leftX, final String titleText) {
        if (null == titleText || titleText.isEmpty()) return;
        final int cy = HEADER_HEIGHT_BASE / 2;
        sb.append(String.format(Locale.US,
                "    <circle cx=\"%d\" cy=\"%d\" r=\"8\" fill=\"%s\" fill-opacity=\"0.18\" />\n",
                leftX + 6, cy, ACCENT_SKY));
        sb.append(String.format(Locale.US,
                "    <circle cx=\"%d\" cy=\"%d\" r=\"4.5\" fill=\"%s\" />\n",
                leftX + 6, cy, ACCENT_SKY));
        sb.append(String.format(Locale.US,
                "    <text class=\"tx4\" x=\"%d\" y=\"%d\">%s</text>\n",
                leftX + 22, cy + 5, escapeXml(titleText)));
    }

    /**
     * Appends a right-aligned translucent bordered badge pill and returns the next {@code rightX} cursor.
     *
     * @param sb     target SVG builder
     * @param text   badge text (Title Case)
     * @param rightX right edge coordinate for this badge
     * @param topY   top edge coordinate for this badge
     * @param style  translucent bordered badge style
     * @return next {@code rightX} coordinate to the left of this badge
     */
    public static int appendBorderedBadge(
            final StringBuilder sb,
            final String text,
            final int rightX,
            final int topY,
            final BadgeStyle style) {
        final String safe = (null == text) ? "" : text;
        final int badgeW = (int) Math.ceil(safe.length() * 6.8) + 18;
        final int leftX = rightX - badgeW;
        sb.append(String.format(Locale.US,
                "    <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"6\" ry=\"6\" " +
                        "fill=\"%s\" fill-opacity=\"%.2f\" stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"1\" />\n",
                leftX, topY, badgeW, BADGE_HEIGHT,
                style.fill(), style.fillOpacity(), style.stroke(), style.strokeOpacity()));
        sb.append(String.format(Locale.US,
                "    <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
                leftX + (badgeW / 2), topY + 16, style.textColor(), escapeXml(safe)));
        return leftX - BADGE_GAP;
    }

    /**
     * Formats leaf completion badge text with completion percentage.
     *
     * @param covered number of covered leaf scenarios
     * @param total   total accessible leaf scenarios
     * @return formatted Title Case badge text (e.g. {@code "Leafs: 36/253 (14%)"})
     */
    public static String formatLeafsBadge(final int covered, final int total) {
        if (total <= 0) {
            return "Leafs: " + covered + "/" + total;
        }
        final int pct = Math.min(100, (covered * 100) / total);
        return String.format(Locale.US, "Leafs: %d/%d (%d%%)", covered, total, pct);
    }

    /**
     * Resolves semantic badge style for leaf coverage progress.
     *
     * @param covered number of covered leaf scenarios
     * @param total   total accessible leaf scenarios
     * @return green when complete, amber when in progress, neutral slate when no leafs are tracked
     */
    public static BadgeStyle leafsBadgeStyle(final int covered, final int total) {
        if (total <= 0) return BADGE_SLATE;
        return (covered >= total) ? BADGE_GREEN : BADGE_AMBER;
    }

    /**
     * Formats the elapsed execution time badge label using the active global run timer.
     *
     * @return {@code "Elapsed: HH:MM:SS"} or {@code "Elapsed: --:--:--"}
     */
    public static String formatElapsedBadge() {
        return isNotNull(timer)
                ? "Elapsed: " + formatElapsedTime(Math.max(0L, System.currentTimeMillis() - timer.startedAt()))
                : "Elapsed: --:--:--";
    }

    /**
     * Captures a read-only ETA snapshot and returns the formatted badge text and semantic style.
     *
     * @return resolved {@link EtaBadge}
     */
    public static EtaBadge resolveEtaBadge() {
        try {
            final EtaSnapshot snapshot = EtaCalculator.capture(true);
            if (null == snapshot) {
                return new EtaBadge("ETA: --:--", BADGE_SLATE);
            }
            if (EtaState.DONE == snapshot.state()) {
                return new EtaBadge("ETA: Done", BADGE_GREEN);
            }
            if (EtaState.ESTIMATED == snapshot.state()) {
                final String remaining = (snapshot.correctionMs() != 0L)
                        ? snapshot.formattedCorrectedRemaining()
                        : snapshot.formattedRemaining();
                return new EtaBadge("ETA: " + remaining, BADGE_INDIGO);
            }
        } catch (final RuntimeException ignored) {
            // Best-effort HUD badge; never interrupt frame rendering
        }
        return new EtaBadge("ETA: --:--", BADGE_SLATE);
    }

    /**
     * Formats elapsed duration milliseconds as {@code HH:MM:SS}.
     *
     * @param elapsedMillis duration in milliseconds
     * @return formatted {@code HH:MM:SS} string
     */
    public static String formatElapsedTime(final long elapsedMillis) {
        final long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
        return String.format(
                Locale.ROOT,
                "%02d:%02d:%02d",
                totalSeconds / 3600L,
                (totalSeconds / 60L) % 60L,
                totalSeconds % 60L);
    }

    /**
     * Escapes XML special characters for safe SVG text insertion.
     *
     * @param text raw string (may be {@code null})
     * @return XML-safe string
     */
    public static String escapeXml(final String text) {
        if (null == text || text.isEmpty()) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
