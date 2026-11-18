package works.lysenko.base.output.scenario;

import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.Locale;

import static works.lysenko.Base.core;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Generates SVG document strings for scenario execution progression graphs.
 */
public final class ScenarioGraphSvgRenderer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 680;

    private ScenarioGraphSvgRenderer() {
    }

    public static String renderProgressionGraphSvg(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {

        final int cardX = 24, cardY = 20, cardW = WIDTH - 48, cardH = HEIGHT - 40;
        int completedCount = 0;
        int maxExecs = target;
        final int[] execsArray = new int[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            final _Scenario sc = scenarios.get(i);
            final int scTarget = (sc instanceof Mono) ? 1 : target;
            final int execs = (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getExecutions(sc) : 0;
            execsArray[i] = execs;
            if (execs >= scTarget) completedCount++;
            if (execs > maxExecs) maxExecs = execs;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                WIDTH, HEIGHT, WIDTH, HEIGHT));
        sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"#090D16\" />\n");
        sb.append(String.format(Locale.US,
                "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"12\" ry=\"12\" fill=\"#111827\" stroke=\"#1E293B\" stroke-width=\"1\" />\n",
                cardX, cardY, cardW, cardH));
        sb.append(String.format(Locale.US,
                "  <text x=\"%d\" y=\"%d\" fill=\"#F8FAFC\" font-family=\"sans-serif\" font-weight=\"bold\" font-size=\"15\">SCENARIO EXECUTION PROGRESSION</text>\n",
                cardX + 24, cardY + 36));

        final int badgeY = cardY + 22;
        int badgeX = cardX + cardW - 24;

        final long elapsedMillis = (isNotNull(timer)) ? Math.max(0L, System.currentTimeMillis() - timer.startedAt()) : 0L;
        final String elapsedStr = (isNotNull(timer)) ? "ELAPSED: " + ScenarioGraphImageRenderer.formatElapsedTime(elapsedMillis) : "ELAPSED: --:--:--";
        badgeX = appendBadgeSvg(sb, elapsedStr, badgeX, badgeY, "#1E293B", "#94A3B8");

        final String targetStr = String.format("TARGET: %d", target);
        badgeX = appendBadgeSvg(sb, targetStr, badgeX, badgeY, "#1E293B", "#94A3B8");

        final int totalScenarios = scenarios.size();
        final int pct = totalScenarios > 0 ? (completedCount * 100 / totalScenarios) : 0;
        final String completedStr = String.format("COMPLETED: %d/%d (%d%%)", completedCount, totalScenarios, pct);
        final String compColor = (completedCount == totalScenarios) ? "#22C55E" : "#F59E0B";
        badgeX = appendBadgeSvg(sb, completedStr, badgeX, badgeY, compColor + "28", compColor);

        final String cycleStr = String.format("TEST #%d", currentCycle);
        appendBadgeSvg(sb, cycleStr, badgeX, badgeY, "#38BDF828", "#38BDF8");

        final int chartX = cardX + 54;
        final int chartY = cardY + 68;
        final int chartW = cardW - 74;
        final int chartH = cardH - 300;
        final int maxY = Math.max(target + 1, maxExecs + 1);

        final int ySteps = Math.min(maxY, 8);
        final int stepVal = Math.max(1, (int) Math.ceil((double) maxY / ySteps));

        for (int v = 0; v <= maxY; v += stepVal) {
            final int yPos = chartY + chartH - (int) Math.round(((double) v / maxY) * chartH);
            sb.append(String.format(Locale.US,
                    "  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#1E293B\" stroke-opacity=\"0.5\" stroke-width=\"1\" />\n",
                    chartX, yPos, chartX + chartW, yPos));
            sb.append(String.format(Locale.US,
                    "  <text x=\"%d\" y=\"%d\" fill=\"#64748B\" font-family=\"monospace\" font-size=\"10\" text-anchor=\"end\">%d</text>\n",
                    chartX - 8, yPos + 4, v));
        }

        if (target <= maxY) {
            final int targetY = chartY + chartH - (int) Math.round(((double) target / maxY) * chartH);
            sb.append(String.format(Locale.US,
                    "  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#38BDF8\" stroke-opacity=\"0.8\" stroke-width=\"1.5\" stroke-dasharray=\"6 4\" />\n",
                    chartX, targetY, chartX + chartW, targetY));
            final String tag = String.format("TARGET: %d", target);
            final int tagW = tag.length() * 6 + 12;
            sb.append(String.format(Locale.US,
                    "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"13\" fill=\"#111827\" />\n" +
                    "  <text x=\"%d\" y=\"%d\" fill=\"#38BDF8\" font-family=\"sans-serif\" font-weight=\"bold\" font-size=\"9\">%s</text>\n",
                    chartX + chartW - tagW - 4, targetY - 14, tagW, chartX + chartW - tagW, targetY - 4, tag));
        }

        final int count = scenarios.size();
        final double slotW = (double) chartW / count;
        final int barW = Math.max(4, Math.min(26, (int) (slotW * 0.72)));
        final double labelAngle = -Math.PI / 3.27;
        final double cosA = Math.cos(Math.abs(labelAngle));
        final double sinA = Math.sin(Math.abs(labelAngle));

        for (int i = 0; i < count; i++) {
            final _Scenario sc = scenarios.get(i);
            final int execs = execsArray[i];
            final int centerX = (int) Math.round(chartX + i * slotW + slotW / 2.0);
            final int barLeft = centerX - barW / 2;

            sb.append(String.format(Locale.US,
                    "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"#1E293B\" fill-opacity=\"0.35\" />\n",
                    barLeft, chartY, barW, chartH));

            final int scTarget = (sc instanceof Mono) ? 1 : target;
            if (execs > 0) {
                final int barH = Math.max(3, (int) Math.round(((double) execs / maxY) * chartH));
                final int barTop = chartY + chartH - barH;
                final String barColor = (execs >= scTarget) ? "#22C55E" : "#38BDF8";
                sb.append(String.format(Locale.US,
                        "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" fill=\"%s\" />\n",
                        barLeft, barTop, barW, barH, barColor));
            } else {
                sb.append(String.format(Locale.US,
                        "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"2\" fill=\"#334155\" />\n",
                        barLeft, chartY + chartH - 2, barW));
            }

            final String valStr = String.valueOf(execs);
            final int valY = (execs > 0)
                    ? (chartY + chartH - (int) Math.round(((double) execs / maxY) * chartH) - 4)
                    : (chartY + chartH - 6);
            final String valColor = (execs >= scTarget) ? "#4ADE80" : (execs > 0 ? "#38BDF8" : "#64748B");
            sb.append(String.format(Locale.US,
                    "  <text x=\"%d\" y=\"%d\" fill=\"%s\" font-family=\"monospace\" font-weight=\"bold\" font-size=\"10\" text-anchor=\"middle\">%s</text>\n",
                    centerX, Math.max(chartY + 12, valY), valColor, valStr));

            final String rawName = sc.getShortName();
            final String nameColor = (execs >= scTarget) ? "#F8FAFC" : (execs > 0 ? "#94A3B8" : "#64748B");
            final int maxW_X = (int) Math.floor((centerX - (cardX + 8)) / cosA);
            final int maxW_Y = (int) Math.floor(((cardY + cardH - 12) - (chartY + chartH + 10)) / sinA);
            final int maxAllowedChars = Math.max(4, Math.min(maxW_X, maxW_Y) / 7);

            String displayName = rawName;
            if (displayName.length() > maxAllowedChars) {
                displayName = "..." + displayName.substring(displayName.length() - maxAllowedChars + 3);
            }
            final String safeName = displayName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            sb.append(String.format(Locale.US,
                    "  <text x=\"%d\" y=\"%d\" fill=\"%s\" font-family=\"sans-serif\" font-size=\"10\" transform=\"rotate(-55, %d, %d)\" text-anchor=\"end\">%s</text>\n",
                    centerX - 2, chartY + chartH + 10, nameColor, centerX - 2, chartY + chartH + 10, safeName));
        }

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static int appendBadgeSvg(
            final StringBuilder sb,
            final String text,
            final int rightX,
            final int topY,
            final String bg,
            final String fg) {
        final int badgeW = text.length() * 7 + 16;
        final int leftX = rightX - badgeW;
        sb.append(String.format(Locale.US,
                "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"20\" rx=\"6\" ry=\"6\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1\" />\n" +
                "  <text x=\"%d\" y=\"%d\" fill=\"%s\" font-family=\"monospace\" font-weight=\"bold\" font-size=\"10\">%s</text>\n",
                leftX, topY, badgeW, bg, fg, leftX + 8, topY + 14, fg, text));
        return leftX - 8;
    }
}
