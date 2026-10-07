package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.base.output.loghtml.LogModels.TelemetryItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static works.lysenko.base.output.loghtml.LogFormatting.escapeJson;
import static works.lysenko.base.output.loghtml.LogFormatting.formatCompletionDuration;
import static works.lysenko.base.output.loghtml.LogFormatting.formatCompletionMoment;
import static works.lysenko.base.output.loghtml.LogFormatting.getNiceCeil;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Renderer for timeline bar charts, interactive SVG line graphs, and client-side telemetry scripts.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall", "OverlyLongMethod"})
public final class TimelineRenderer {

    private TimelineRenderer() {
    }

    /**
     * Renders timeline bar chart segments for execution configuration, preflight, tests, limbo transitions, and postflight.
     *
     * @param configItem      configuration phase telemetry item
     * @param preflightItem   preflight phase telemetry item
     * @param testData        test execution telemetry items
     * @param limboByPrevTest map of limbo items indexed by previous test number
     * @param postflightItem  postflight phase telemetry item
     * @param tMax            maximum test duration in seconds
     * @param lMax            maximum limbo duration in milliseconds
     * @param maxSec          overall maximum duration in seconds
     * @return HTML timeline bar chart snippet
     */
    public static String renderTimeline(
            final TelemetryItem configItem, final TelemetryItem preflightItem,
            final List<TelemetryItem> testData, final Map<Integer, TelemetryItem> limboByPrevTest,
            final TelemetryItem postflightItem, final double tMax, final double lMax, final double maxSec) {
        final StringBuilder sb = new StringBuilder();

        sb.append(renderPhaseBar(configItem, "config", maxSec));
        sb.append(renderPhaseBar(preflightItem, "preflight", maxSec));

        for (final TelemetryItem test : testData) {
            final int tPct = tMax > 0 ? Math.max(6, (int) Math.round((test.sec / tMax) * 100)) : 6;
            final String tValStr = String.format(Locale.ROOT, "%.2fs", test.sec);
            final String tColor = "passed".equals(test.status) ? "#22c55e" : "warning".equals(test.status) ? "#f59e0b" : "#ef4444";

            final String limboHtml;
            final TelemetryItem limbo = limboByPrevTest.get(test.testNum);
            if (null != limbo) {
                final int lPct = lMax > 0 ? Math.max(6, (int) Math.round((limbo.ms / lMax) * 100)) : 6;
                final String lValStr = Math.round(limbo.ms) + "ms";
                final String lColor = ("passed".equals(limbo.status) || "neutral".equals(limbo.status)) ? "#64748b" : ("warning".equals(limbo.status) ? "#f59e0b" : "#ef4444");
                limboHtml = s(
                        "  <div class=\"bar-col limbo\" onclick=\"focusSection('", limbo.title, "')\" title=\"", limbo.title, ": ", lValStr, "\">",
                        "<div class=\"bar-val\">", lValStr, "</div>",
                        "<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ", lPct, "%; background: ", lColor, ";\"></div></div>",
                        "<div class=\"bar-lbl\">", limbo.label, "</div></div>\n"
                );
            } else {
                limboHtml = "";
            }

            sb.append(s(
                    "<div class=\"bar-pair\">\n",
                    "  <div class=\"bar-col test\" onclick=\"focusSection('", test.title, "')\" title=\"", test.title, ": ", tValStr, "\">",
                    "<div class=\"bar-val\">", tValStr, "</div>",
                    "<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ", tPct, "%; background: ", tColor, ";\"></div></div>",
                    "<div class=\"bar-lbl\">", test.label, "</div></div>\n",
                    limboHtml,
                    "</div>\n"
            ));
        }

        sb.append(renderPhaseBar(postflightItem, "postflight", maxSec));

        return sb.toString();
    }

    /**
     * Renders an individual timeline bar chart segment for execution phases (e.g. config, preflight, postflight).
     *
     * @param item       phase telemetry item
     * @param extraClass extra CSS class modifier
     * @param maxSec     maximum duration scale in seconds
     * @return HTML snippet for phase bar
     */
    public static String renderPhaseBar(final TelemetryItem item, final String extraClass, final double maxSec) {
        if (null == item) return "";
        final int pct = maxSec > 0 ? Math.max(6, Math.min(100, (int) Math.round((item.sec / maxSec) * 100))) : 6;
        final String valStr = (item.sec >= 1.0)
                ? String.format(Locale.ROOT, "%.2fs", item.sec)
                : Math.round(item.ms) + "ms";
        final String color = "failed".equals(item.status) ? "#ef4444"
                : "warning".equals(item.status) ? "#f59e0b"
                : "passed".equals(item.status) ? "#22c55e"
                : "config".equals(extraClass) ? "#64748b"
                : "#38bdf8";
        return s(
                "<div class=\"bar-col phase ", extraClass, "\" onclick=\"focusSection('", item.title, "')\" title=\"",
                item.title, ": ", valStr, "\">",
                "<div class=\"bar-val\">", valStr, "</div>",
                "<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ", pct, "%; background: ", color, ";\"></div></div>",
                "<div class=\"bar-lbl\">", item.label, "</div></div>\n"
        );
    }

    /**
     * Renders the UI toggle buttons for switching between timeline line graph and bar views.
     *
     * @param isLineDefault whether line view is selected by default
     * @return HTML snippet for view toggle
     */
    public static String renderTimelineToggle(final boolean isLineDefault) {
        final String activeLine = isLineDefault ? " active" : "";
        final String activeBars = isLineDefault ? "" : " active";
        return s(
                "<div class=\"chart-view-toggle\">\n",
                "  <button type=\"button\" id=\"btnViewLine\" class=\"chart-toggle-btn", activeLine, "\" onclick=\"switchTimelineView('line')\">Line</button>\n",
                "  <button type=\"button\" id=\"btnViewBars\" class=\"chart-toggle-btn", activeBars, "\" onclick=\"switchTimelineView('bars')\">Bars</button>\n",
                "</div>\n"
        );
    }

    /**
     * Renders JavaScript JSON payload consumed by interactive line chart widgets.
     *
     * @param testData        test telemetry items
     * @param limboByPrevTest limbo telemetry items indexed by previous test number
     * @param completions     leaf completion records
     * @param etaDebugItems   ETA debug records
     * @param tAvg            average test duration in seconds
     * @param tMax            maximum test duration in seconds
     * @param lMax            maximum limbo duration in milliseconds
     * @return JavaScript snippet defining global data objects
     */
    public static String renderLgScriptData(
            final List<TelemetryItem> testData,
            final Map<Integer, TelemetryItem> limboByPrevTest,
            final List<LeafCompletion> completions,
            final List<EtaDebugItem> etaDebugItems,
            final List<SystemResourceItem> resourceItems,
            final double tAvg, final double tMax, final double lMax) {
        if (testData.isEmpty() && (completions == null || completions.isEmpty()) && (etaDebugItems == null || etaDebugItems.isEmpty()) && (resourceItems == null || resourceItems.isEmpty())) return "";

        final StringBuilder sb = new StringBuilder();
        if (!testData.isEmpty()) {
            final boolean hasLimbo = lMax > 0 && !limboByPrevTest.isEmpty();
            final double tCeil = getNiceCeil(tMax > 0 ? tMax : 1.0);
            final double lCeil = hasLimbo ? getNiceCeil(lMax) : 1.0;

            sb.append(s("  window.lgTCeil = ", String.format(Locale.ROOT, "%.2f", tCeil), ";\n"));
            sb.append(s("  window.lgLCeil = ", String.format(Locale.ROOT, "%.2f", lCeil), ";\n"));
            sb.append(s("  window.lgTAvg = ", String.format(Locale.ROOT, "%.2f", tAvg), ";\n"));
            sb.append(s("  window.lgHasLimbo = ", hasLimbo, ";\n"));
            sb.append("  window.lgData = [");
            for (int i = 0; i < testData.size(); i++) {
                if (i > 0) sb.append(",");
                final TelemetryItem t = testData.get(i);
                final TelemetryItem l = limboByPrevTest.get(t.testNum);
                final long lMs = l != null ? Math.round(l.ms) : 0;
                sb.append(s(
                        "{\"n\":", t.testNum,
                        ",\"t\":\"", escapeJson(t.title), "\"",
                        String.format(Locale.ROOT, ",\"s\":%.3f", t.sec),
                        ",\"ms\":", Math.round(t.ms),
                        ",\"l\":", lMs,
                        ",\"st\":\"", escapeJson(t.status), "\"}"
                ));
            }
            sb.append("];\n");
        }

        if (null != completions && !completions.isEmpty()) {
            long maxValMs = 1L;
            long maxIntervalMs = 1L;
            long maxProjectedMs = 1L;
            long lastRoundAtMillis = 0L;
            for (final LeafCompletion c : completions) {
                final long projTotal = c.atMillis + c.etaMs;
                maxProjectedMs = Math.max(maxProjectedMs, projTotal);
                maxValMs = Math.max(maxValMs, projTotal);
                if (c.roundIndex > 0) {
                    final long roundInterval = Math.max(0L, c.atMillis - lastRoundAtMillis);
                    lastRoundAtMillis = c.atMillis;
                    maxIntervalMs = Math.max(maxIntervalMs, roundInterval);
                    maxValMs = Math.max(maxValMs, roundInterval);
                }
            }
            final double ceilSec = getNiceCeil(maxValMs > 0 ? maxValMs / 1000.0 : 1.0);
            final double ceilIntervalSec = getNiceCeil(maxIntervalMs > 0 ? maxIntervalMs / 1000.0 : 1.0);
            final double ceilProjectedSec = getNiceCeil(maxProjectedMs > 0 ? maxProjectedMs / 1000.0 : 1.0);

            sb.append(s("  window.leafCeil = ", String.format(Locale.ROOT, "%.3f", ceilSec), ";\n"));
            sb.append(s("  window.leafCeilInterval = ", String.format(Locale.ROOT, "%.3f", ceilIntervalSec), ";\n"));
            sb.append(s("  window.leafCeilProjected = ", String.format(Locale.ROOT, "%.3f", ceilProjectedSec), ";\n"));
            sb.append("  window.leafData = [");
            long pAt = 0L;
            long pRoundAt = 0L;
            for (int i = 0; i < completions.size(); i++) {
                if (i > 0) sb.append(",");
                final LeafCompletion c = completions.get(i);
                final long intervalMs = Math.max(0L, c.atMillis - pAt);
                pAt = c.atMillis;
                long roundIntervalMs = -1L;
                if (c.roundIndex > 0) {
                    roundIntervalMs = Math.max(0L, c.atMillis - pRoundAt);
                    pRoundAt = c.atMillis;
                }

                final String intervalStr = formatCompletionDuration(intervalMs);
                final String roundIntervalStr = (roundIntervalMs >= 0L) ? formatCompletionDuration(roundIntervalMs) : "";
                final String etaStr = formatCompletionDuration(c.etaMs);
                final long rawProjMs = c.atMillis + c.etaMs;
                final long corrMs = works.lysenko.base.output.EtaCorrection.getEstimationCorrection(c.atMillis);
                final long corrEtaMs = Math.max(0L, c.etaMs - corrMs);
                final long corrProjMs = c.atMillis + corrEtaMs;
                final String projStr = formatCompletionDuration(rawProjMs);
                final String corrProjStr = formatCompletionDuration(corrProjMs);
                final String momentStr = formatCompletionMoment(c.atMillis);
                final String fullLabel = (c.etaMs > 0L)
                        ? c.leaf + " @ " + momentStr + " (+" + intervalStr + ", Projected: " + projStr + ")"
                        : c.leaf + " @ " + momentStr + " (+" + intervalStr + ")";
                sb.append(s(
                        "{\"n\":", i + 1,
                        ",\"leaf\":\"", escapeJson(c.leaf), "\"",
                        ",\"ms\":", intervalMs,
                        ",\"round\":", c.roundIndex,
                        ",\"roundIntervalMs\":", roundIntervalMs,
                        ",\"roundInterval\":\"", escapeJson(roundIntervalStr), "\"",
                        ",\"etaMs\":", c.etaMs,
                        ",\"projMs\":", rawProjMs,
                        ",\"corrProjMs\":", corrProjMs,
                        ",\"eta\":\"", escapeJson(etaStr), "\"",
                        ",\"proj\":\"", escapeJson(projStr), "\"",
                        ",\"corrProj\":\"", escapeJson(corrProjStr), "\"",
                        ",\"at\":", c.atMillis,
                        ",\"interval\":\"", escapeJson(intervalStr), "\"",
                        ",\"moment\":\"", escapeJson(momentStr), "\"",
                        ",\"label\":\"", escapeJson(fullLabel), "\"}"
                ));
            }
            sb.append("];\n");
        }

        if (null != etaDebugItems && !etaDebugItems.isEmpty()) {
            double maxInstabilityPct = 1.0;
            double maxErrorPct = 1.0;
            for (int i = 0; i < etaDebugItems.size(); i++) {
                final EtaDebugItem item = etaDebugItems.get(i);
                maxInstabilityPct = Math.max(maxInstabilityPct, item.instabilityPct());
                if (i > 0 || etaDebugItems.size() == 1) {
                    maxErrorPct = Math.max(maxErrorPct, item.errorPct());
                }
            }
            final double ceilInstabilityPct = getNiceCeil(maxInstabilityPct > 0 ? maxInstabilityPct : 10.0);
            final double ceilErrorPct = Math.min(100000.0, getNiceCeil(maxErrorPct > 0 ? maxErrorPct : 10.0));

            sb.append(s("  window.etaDebugCeilInstability = ", String.format(Locale.ROOT, "%.3f", ceilInstabilityPct), ";\n"));
            sb.append(s("  window.etaDebugCeilError = ", String.format(Locale.ROOT, "%.3f", ceilErrorPct), ";\n"));
            sb.append(s("  window.etaDebugCeil = ", String.format(Locale.ROOT, "%.3f", Math.max(ceilInstabilityPct, ceilErrorPct)), ";\n"));
            sb.append("  window.etaDebugData = [");
            for (int i = 0; i < etaDebugItems.size(); i++) {
                if (i > 0) sb.append(",");
                final EtaDebugItem item = etaDebugItems.get(i);
                final String realStr = formatCompletionDuration(item.realMs);
                final String meanStr = formatCompletionDuration(item.meanEtaMs);
                final String stdDevStr = formatCompletionDuration(item.stdDevEtaMs);
                final String predStr = formatCompletionDuration(item.predictedMs);

                sb.append(s(
                        "{\"n\":", item.testNum,
                        ",\"test\":\"", escapeJson(item.testName), "\"",
                        String.format(Locale.ROOT, ",\"instability\":%.2f", item.instabilityPct()),
                        String.format(Locale.ROOT, ",\"error\":%.2f", item.errorPct()),
                        ",\"realStr\":\"", escapeJson(realStr), "\"",
                        ",\"meanStr\":\"", escapeJson(meanStr), "\"",
                        ",\"stdDevStr\":\"", escapeJson(stdDevStr), "\"",
                        ",\"predStr\":\"", escapeJson(predStr), "\"}"
                ));
            }
            sb.append("];\n");
        }

        if (null != resourceItems && !resourceItems.isEmpty()) {
            sb.append("  window.resourceData = [");
            for (int i = 0; i < resourceItems.size(); i++) {
                if (i > 0) sb.append(",");
                final SystemResourceItem item = resourceItems.get(i);
                sb.append(s(
                        "{\"n\":", item.sampleNum,
                        ",\"t\":", item.testNum,
                        String.format(Locale.ROOT, ",\"cpu\":%.2f", item.cpuPct),
                        String.format(Locale.ROOT, ",\"cpuMax\":%.2f", item.maxCpuPct),
                        String.format(Locale.ROOT, ",\"ramUsed\":%.1f", item.usedRamMb),
                        String.format(Locale.ROOT, ",\"ramTotal\":%.1f", item.totalRamMb),
                        ",\"threads\":", item.threads, "}"
                ));
            }
            sb.append("];\n");
        }

        return sb.toString();
    }

    /**
     * Renders an interactive SVG line chart visualizing test durations, limbo transition times, and failure markers.
     *
     * @param testData        list of test telemetry items
     * @param limboByPrevTest map of limbo telemetry items indexed by previous test number
     * @param tAvg            average test duration in seconds
     * @param tMax            maximum test duration in seconds
     * @param lMax            maximum limbo duration in milliseconds
     * @return HTML snippet containing interactive SVG line graph
     */
    public static String renderLineGraph(
            final List<TelemetryItem> testData,
            final Map<Integer, TelemetryItem> limboByPrevTest,
            final double tAvg, final double tMax, final double lMax) {
        if (testData.isEmpty()) return "";

        final int N = testData.size();
        final double width = 1000.0;
        final double height = 240.0;
        final boolean hasLimbo = lMax > 0 && !limboByPrevTest.isEmpty();
        final double marginLeft = 55.0;
        final double marginRight = hasLimbo ? 55.0 : 25.0;
        final double marginTop = 26.0;
        final double marginBottom = 34.0;
        final double plotW = width - marginLeft - marginRight;
        final double plotH = height - marginTop - marginBottom;

        final double tCeil = getNiceCeil(tMax > 0 ? tMax : 1.0);
        final double lCeil = hasLimbo ? getNiceCeil(lMax) : 1.0;

        final StringBuilder sb = new StringBuilder();

        // Legend
        sb.append(s(
                "<div class=\"line-graph-legend\">\n",
                "  <span class=\"lg-legend-item\"><span class=\"lg-line-sample test-line\"></span> Test Time (s)</span>\n",
                hasLimbo ? "  <span class=\"lg-legend-item lg-legend-limbo\"><span class=\"lg-line-sample limbo-line\"></span> Limbo (ms)</span>\n" : "",
                tAvg > 0 ? String.format(Locale.ROOT, "  <span class=\"lg-legend-item\"><span class=\"lg-dash-sample\"></span> Avg: %.2fs</span>\n", tAvg) : "",
                "  <span class=\"lg-legend-item\"><span class=\"lg-dot-sample fail\"></span> Failed</span>\n",
                "  <span class=\"lg-legend-item\"><span class=\"lg-dot-sample warn\"></span> Warning</span>\n",
                "</div>\n",
                "<div class=\"line-graph-svg-wrap\">\n",
                "<svg class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">\n",
                "  <defs>\n",
                "    <linearGradient id=\"testAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n",
                "      <stop offset=\"0%\" stop-color=\"#38bdf8\" stop-opacity=\"0.35\"/>\n",
                "      <stop offset=\"100%\" stop-color=\"#38bdf8\" stop-opacity=\"0.02\"/>\n",
                "    </linearGradient>\n",
                "    <linearGradient id=\"limboAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n",
                "      <stop offset=\"0%\" stop-color=\"#a855f7\" stop-opacity=\"0.25\"/>\n",
                "      <stop offset=\"100%\" stop-color=\"#a855f7\" stop-opacity=\"0.02\"/>\n",
                "    </linearGradient>\n",
                "  </defs>\n"
        ));

        // Horizontal grid lines and Y-axis labels
        for (int k = 0; k <= 4; k++) {
            final double ratio = (double) k / 4.0;
            final double y = (marginTop + plotH) - ratio * plotH;
            final double tVal = ratio * tCeil;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.06)\" stroke-dasharray=\"3,3\" />\n",
                    marginLeft, y, marginLeft + plotW, y));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%.1fs</text>\n",
                    marginLeft - 8, y + 3.5, tVal));
            if (hasLimbo) {
                final double lVal = ratio * lCeil;
                sb.append(String.format(Locale.ROOT,
                        "  <text class=\"lg-limbo-series\" x=\"%.1f\" y=\"%.1f\" text-anchor=\"start\" fill=\"#c084fc\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%dms</text>\n",
                        marginLeft + plotW + 8, y + 3.5, Math.round(lVal)));
            }
        }

        // X-axis ticks and labels
        final int tickCount = Math.min(8, N);
        final Set<Integer> tickIndices = new LinkedHashSet<>();
        if (tickCount <= 1 || N <= 1) {
            tickIndices.add(0);
        } else {
            for (int k = 0; k < tickCount - 1; k++) {
                tickIndices.add((int) Math.round((double) k * (N - 1) / (tickCount - 1)));
            }
            tickIndices.add(N - 1);
        }

        for (final int idx : tickIndices) {
            final TelemetryItem item = testData.get(idx);
            final double x = marginLeft + (N > 1 ? (double) idx / (N - 1) * plotW : plotW / 2.0);
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.15)\" />\n",
                    x, marginTop + plotH, x, marginTop + plotH + 4));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    x, height - 10, item.label));
        }

        // Average reference line
        if (tAvg > 0 && tAvg <= tCeil) {
            final double yAvg = (marginTop + plotH) - (tAvg / tCeil) * plotH;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#0284c7\" stroke-dasharray=\"5,4\" stroke-width=\"1.2\" opacity=\"0.75\" />\n",
                    marginLeft, yAvg, marginLeft + plotW, yAvg));
        }

        // Build Paths
        final StringBuilder testPath = new StringBuilder();
        final StringBuilder testArea = new StringBuilder();
        final StringBuilder limboPath = new StringBuilder();
        final StringBuilder limboArea = new StringBuilder();
        final StringBuilder markers = new StringBuilder();

        for (int i = 0; i < N; i++) {
            final TelemetryItem test = testData.get(i);
            final double x = marginLeft + (N > 1 ? (double) i / (N - 1) * plotW : plotW / 2.0);
            final double yT = (marginTop + plotH) - Math.min(plotH, (test.sec / tCeil) * plotH);

            if (i == 0) {
                testPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yT));
                testArea.append(String.format(Locale.ROOT, "M %.2f %.2f L %.2f %.2f", x, marginTop + plotH, x, yT));
            } else {
                testPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yT));
                testArea.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yT));
            }

            if (hasLimbo) {
                final TelemetryItem limbo = limboByPrevTest.get(test.testNum);
                final double lMs = limbo != null ? limbo.ms : 0.0;
                final double yL = (marginTop + plotH) - Math.min(plotH, (lMs / lCeil) * plotH);
                if (i == 0) {
                    limboPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yL));
                    limboArea.append(String.format(Locale.ROOT, "M %.2f %.2f L %.2f %.2f", x, marginTop + plotH, x, yL));
                } else {
                    limboPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yL));
                    limboArea.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yL));
                }
                if (i == N - 1) {
                    limboArea.append(String.format(Locale.ROOT, " L %.2f %.2f Z", x, marginTop + plotH));
                }
            }

            if (i == N - 1) {
                testArea.append(String.format(Locale.ROOT, " L %.2f %.2f Z", x, marginTop + plotH));
            }

            if ("failed".equals(test.status)) {
                markers.append(String.format(Locale.ROOT,
                        "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"5\" fill=\"#ef4444\" stroke=\"#ffffff\" stroke-width=\"1.5\" />\n",
                        x, yT));
                markers.append(String.format(Locale.ROOT,
                        "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"8\" fill=\"none\" stroke=\"#ef4444\" stroke-width=\"1\" opacity=\"0.6\" />\n",
                        x, yT));
            } else if ("warning".equals(test.status)) {
                markers.append(String.format(Locale.ROOT,
                        "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"4\" fill=\"#f59e0b\" stroke=\"#ffffff\" stroke-width=\"1.5\" />\n",
                        x, yT));
            }
        }

        if (hasLimbo) {
            sb.append(s(
                    "  <g class=\"lg-limbo-series\">\n",
                    "    <path d=\"", limboArea, "\" fill=\"url(#limboAreaGrad)\" />\n",
                    "    <path d=\"", limboPath, "\" fill=\"none\" stroke=\"#a855f7\" stroke-width=\"1.5\" opacity=\"0.7\" />\n",
                    "  </g>\n"
            ));
        }
        sb.append(s(
                "  <path d=\"", testArea, "\" fill=\"url(#testAreaGrad)\" />\n",
                "  <path d=\"", testPath, "\" fill=\"none\" stroke=\"#38bdf8\" stroke-width=\"2\" />\n",
                markers,
                String.format(Locale.ROOT,
                        "  <line id=\"lgCrosshair\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-dasharray=\"2,2\" stroke-width=\"1\" opacity=\"0\" pointer-events=\"none\" />\n",
                        marginTop, marginTop + plotH),
                "  <circle id=\"lgTestDot\" cx=\"0\" cy=\"0\" r=\"5\" fill=\"#38bdf8\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n",
                hasLimbo ? "  <circle id=\"lgLimboDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#a855f7\" stroke=\"#ffffff\" stroke-width=\"1.5\" opacity=\"0\" pointer-events=\"none\" />\n" : "",
                String.format(Locale.ROOT,
                        "  <rect id=\"lgOverlay\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"transparent\" style=\"cursor: crosshair;\" onmousemove=\"onLgHover(event)\" onmouseleave=\"onLgLeave()\" onclick=\"onLgClick()\" />\n",
                        marginLeft, marginTop, plotW, plotH),
                "</svg>\n",
                "<div id=\"lgTooltip\" class=\"lg-tooltip hidden\"></div>\n",
                "</div>\n"
        ));

        return sb.toString();
    }
}
