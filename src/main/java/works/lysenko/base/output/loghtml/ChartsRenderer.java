package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static works.lysenko.base.output.loghtml.LogFormatting.getNiceCeil;
import static works.lysenko.base.output.loghtml.LogFormatting.logScaleY;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * SVG chart renderers for Leaf Completion intervals and ETA Debug metrics.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall", "OverlyLongMethod"})
public final class ChartsRenderer {

    private ChartsRenderer() {
    }

    /**
     * Renders an SVG chart displaying scenario tree leaf completion durations and projected total times.
     *
     * @param completions list of leaf completion records
     * @return HTML snippet containing the leaf completion SVG chart
     */
    public static String renderLeafCompletionGraph(final List<LeafCompletion> completions) {
        if (completions.isEmpty()) return "";

        final double width = 1000.0;
        final double height = 240.0;
        final double marginLeft = 55.0;
        final double marginRight = 55.0;
        final double marginTop = 26.0;
        final double marginBottom = 34.0;
        final double plotW = width - marginLeft - marginRight;
        final double plotH = height - marginTop - marginBottom;

        long maxIntervalMs = 1L;
        long maxProjectedMs = 1L;
        long lastRoundAtMillis = 0L;
        final long[] roundIntervals = new long[completions.size()];
        final long[] projectedTotals = new long[completions.size()];
        final long[] correctedTotals = new long[completions.size()];

        boolean hasCorrectionData = false;
        for (int index = 0; index < completions.size(); index++) {
            final LeafCompletion c = completions.get(index);
            final long projTotal = c.atMillis + c.etaMs;
            projectedTotals[index] = projTotal;
            maxProjectedMs = Math.max(maxProjectedMs, projTotal);

            final long corrMs = works.lysenko.base.output.EtaCorrection.getEstimationCorrection(c.atMillis);
            if (corrMs != 0L) hasCorrectionData = true;
            final long corrEtaMs = Math.max(0L, c.etaMs - corrMs);
            final long corrTotal = c.atMillis + corrEtaMs;
            correctedTotals[index] = corrTotal;
            maxProjectedMs = Math.max(maxProjectedMs, corrTotal);

            if (c.roundIndex > 0) {
                final long roundInterval = Math.max(0L, c.atMillis - lastRoundAtMillis);
                roundIntervals[index] = roundInterval;
                lastRoundAtMillis = c.atMillis;
                maxIntervalMs = Math.max(maxIntervalMs, roundInterval);
            } else {
                roundIntervals[index] = -1L;
            }
        }

        final double maxIntervalSec = maxIntervalMs / 1000.0;
        final double maxProjectedSec = maxProjectedMs / 1000.0;
        final double ceilIntervalSec = getNiceCeil(maxIntervalSec > 0 ? maxIntervalSec : 1.0);
        final double ceilProjectedSec = getNiceCeil(maxProjectedSec > 0 ? maxProjectedSec : 1.0);

        final StringBuilder sb = new StringBuilder();
        sb.append(s(
                "<section id=\"leafCompletionChart\" class=\"leaf-completion-chart\"><div class=\"chart-card\">",
                "<div class=\"card-title\"><span>All-Leaf Completion Intervals</span>",
                "<span class=\"card-subtitle\">Y: Duration &amp; Projected Total (s); X: test cycle</span></div>",
                "<div class=\"chart-controls-bar\">",
                "<div class=\"series-controls left-series-controls\">",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample leaf-line\"></span>Leaf executions</span>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnLeafLeftLog\" class=\"chart-toggle-btn active\" onclick=\"switchLeafLeftScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnLeafLeftLinear\" class=\"chart-toggle-btn\" onclick=\"switchLeafLeftScale('linear')\">Linear</button>",
                "</div>",
                "<select id=\"selectLeafLeftCutoff\" class=\"chart-select\" onchange=\"setLeafLeftCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"10\">Cutoff: 10s</option>",
                "<option value=\"30\">Cutoff: 30s</option>",
                "<option value=\"100\">Cutoff: 100s</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "</div>",
                "<div class=\"series-controls right-series-controls\">",
                "<select id=\"selectLeafRightCutoff\" class=\"chart-select\" onchange=\"setLeafRightCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"10\">Cutoff: 10s</option>",
                "<option value=\"30\">Cutoff: 30s</option>",
                "<option value=\"100\">Cutoff: 100s</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnLeafRightLog\" class=\"chart-toggle-btn active\" onclick=\"switchLeafRightScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnLeafRightLinear\" class=\"chart-toggle-btn\" onclick=\"switchLeafRightScale('linear')\">Linear</button>",
                "</div>",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample leaf-eta-line\"></span>Projected Total (Passed + ETA)</span>",
                hasCorrectionData ? "<span class=\"lg-legend-item\"><span class=\"lg-line-sample leaf-corr-eta-line\" style=\"background:#10b981;\"></span>Corrected Total</span>" : "",
                "</div>",
                "</div>",
                "<div class=\"line-graph-svg-wrap\"><svg id=\"leafCompletionSvg\" class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">",
                "  <defs>\n",
                "    <linearGradient id=\"leafAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n",
                "      <stop offset=\"0%\" stop-color=\"#a855f7\" stop-opacity=\"0.35\"/>\n",
                "      <stop offset=\"100%\" stop-color=\"#a855f7\" stop-opacity=\"0.02\"/>\n",
                "    </linearGradient>\n",
                "  </defs>\n"
        ));

        final double logMaxInterval = Math.log10(ceilIntervalSec + 1.0);
        final double logMaxProjected = Math.log10(ceilProjectedSec + 1.0);

        for (int k = 0; k <= 4; k++) {
            final double ratio = (double) k / 4.0;
            final double y = (marginTop + plotH) - ratio * plotH;
            final double valIntervalSec = (ratio == 0.0) ? 0.0 : Math.pow(10, ratio * logMaxInterval) - 1.0;
            final double valProjectedSec = (ratio == 0.0) ? 0.0 : Math.pow(10, ratio * logMaxProjected) - 1.0;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.06)\" stroke-dasharray=\"3,3\" />\n",
                    marginLeft, y, marginLeft + plotW, y));

            final String leftLabel = (valIntervalSec < 1.0)
                    ? Math.round(valIntervalSec * 1000.0) + "ms"
                    : (valIntervalSec < 10.0)
                    ? String.format(Locale.ROOT, "%.1fs", valIntervalSec)
                    : String.format(Locale.ROOT, "%.0fs", valIntervalSec);
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" fill=\"#a855f7\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    marginLeft - 8, y + 3.5, leftLabel));

            final String rightLabel = (valProjectedSec < 1.0)
                    ? Math.round(valProjectedSec * 1000.0) + "ms"
                    : (valProjectedSec < 10.0)
                    ? String.format(Locale.ROOT, "%.1fs", valProjectedSec)
                    : String.format(Locale.ROOT, "%.0fs", valProjectedSec);
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"start\" fill=\"#38bdf8\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    marginLeft + plotW + 8, y + 3.5, rightLabel));
        }

        final int N = completions.size();
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
            final double x = marginLeft + (N > 1 ? (double) idx / (N - 1) * plotW : plotW / 2.0);
            final int tNum = completions.get(idx).testNum;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.15)\" />\n",
                    x, marginTop + plotH, x, marginTop + plotH + 4));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">#%d</text>\n",
                    x, height - 10, tNum));
        }

        final StringBuilder leafPath = new StringBuilder();
        final StringBuilder etaPath = new StringBuilder();
        final StringBuilder corrEtaPath = new StringBuilder();

        boolean firstRoundPt = true;
        for (int i = 0; i < N; i++) {
            final double x = marginLeft + (N > 1 ? (double) i / (N - 1) * plotW : plotW / 2.0);
            final double projSec = projectedTotals[i] / 1000.0;
            final double yEta = logScaleY(projSec, ceilProjectedSec, marginTop, plotH);

            final double corrProjSec = correctedTotals[i] / 1000.0;
            final double yCorrEta = logScaleY(corrProjSec, ceilProjectedSec, marginTop, plotH);

            if (i == 0) {
                etaPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yEta));
                corrEtaPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yCorrEta));
            } else {
                etaPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yEta));
                corrEtaPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yCorrEta));
            }

            if (roundIntervals[i] >= 0L) {
                final double intervalSec = roundIntervals[i] / 1000.0;
                final double yInterval = logScaleY(intervalSec, ceilIntervalSec, marginTop, plotH);
                if (firstRoundPt) {
                    leafPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yInterval));
                    firstRoundPt = false;
                } else {
                    leafPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yInterval));
                }
            }
        }

        sb.append(s(
                "  <path d=\"", leafPath, "\" class=\"leaf-chart-line\" fill=\"none\" stroke=\"#a855f7\" stroke-width=\"2\" />\n",
                "  <path d=\"", etaPath, "\" class=\"leaf-eta-chart-line\" fill=\"none\" stroke=\"#38bdf8\" stroke-width=\"2\" stroke-dasharray=\"4,3\" />\n",
                hasCorrectionData ? s("  <path d=\"", corrEtaPath, "\" class=\"leaf-corr-eta-chart-line\" fill=\"none\" stroke=\"#10b981\" stroke-width=\"2\" />\n") : ""
        ));

        for (int i = 0; i < N; i++) {
            if (roundIntervals[i] >= 0L) {
                final double x = marginLeft + (N > 1 ? (double) i / (N - 1) * plotW : plotW / 2.0);
                final double intervalSec = roundIntervals[i] / 1000.0;
                final double yInterval = logScaleY(intervalSec, ceilIntervalSec, marginTop, plotH);
                sb.append(String.format(Locale.ROOT,
                        "  <circle cx=\"%.1f\" cy=\"%.1f\" r=\"4\" fill=\"#a855f7\" stroke=\"#ffffff\" stroke-width=\"1.5\" />\n",
                        x, yInterval));
            }
        }

        sb.append(s(
                String.format(Locale.ROOT,
                        "  <line id=\"leafCrosshair\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-dasharray=\"2,2\" stroke-width=\"1\" opacity=\"0\" pointer-events=\"none\" />\n",
                        marginTop, marginTop + plotH),
                "  <circle id=\"leafDot\" cx=\"0\" cy=\"0\" r=\"5\" fill=\"#a855f7\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n",
                "  <circle id=\"leafEtaDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#38bdf8\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n",
                String.format(Locale.ROOT,
                        "  <rect id=\"leafOverlay\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"transparent\" style=\"cursor: crosshair;\" onmousemove=\"onLeafHover(event)\" onmouseleave=\"onLeafLeave()\" />\n",
                        marginLeft, marginTop, plotW, plotH),
                "</svg>\n",
                "<div id=\"leafTooltip\" class=\"lg-tooltip hidden\"></div>\n",
                "</div></div></section>"
        ));

        return sb.toString();
    }

    /**
     * Renders an SVG chart visualizing ETA prediction accuracy and instability metrics over test cycles.
     *
     * @param items list of ETA debug records
     * @return HTML snippet containing the ETA debug SVG chart
     */
    public static String renderEtaDebugGraph(final List<EtaDebugItem> items) {
        if (items.isEmpty()) return "";

        final double width = 1000.0;
        final double height = 240.0;
        final double marginLeft = 55.0;
        final double marginRight = 55.0;
        final double marginTop = 26.0;
        final double marginBottom = 34.0;
        final double plotW = width - marginLeft - marginRight;
        final double plotH = height - marginTop - marginBottom;

        double maxInstabilityPct = 1.0;
        double maxErrorPct = 1.0;
        for (int i = 0; i < items.size(); i++) {
            final EtaDebugItem item = items.get(i);
            maxInstabilityPct = Math.max(maxInstabilityPct, item.instabilityPct());
            if (i > 0 || items.size() == 1) {
                maxErrorPct = Math.max(maxErrorPct, item.errorPct());
            }
        }

        final double ceilInstabilityPct = getNiceCeil(maxInstabilityPct > 0 ? maxInstabilityPct : 10.0);
        final double ceilErrorPct = Math.min(100000.0, getNiceCeil(maxErrorPct > 0 ? maxErrorPct : 10.0));

        final StringBuilder sb = new StringBuilder();
        sb.append(s(
                "<section id=\"etaDebugChart\" class=\"eta-debug-chart\"><div class=\"chart-card\">",
                "<div class=\"card-title\"><span>ETA Debug</span>",
                "<span class=\"card-subtitle\">Y: Deviation &amp; Prediction Error (%); X: test cycle</span></div>",
                "<div class=\"chart-controls-bar\">",
                "<div class=\"series-controls left-series-controls\">",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample eta-instability-line\"></span>Instability (sample deviation)</span>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnEtaLeftLog\" class=\"chart-toggle-btn active\" onclick=\"switchEtaLeftScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnEtaLeftLinear\" class=\"chart-toggle-btn\" onclick=\"switchEtaLeftScale('linear')\">Linear</button>",
                "</div>",
                "<select id=\"selectEtaLeftCutoff\" class=\"chart-select\" onchange=\"setEtaLeftCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"50\">Cutoff: 50%</option>",
                "<option value=\"100\">Cutoff: 100%</option>",
                "<option value=\"500\">Cutoff: 500%</option>",
                "<option value=\"1000\">Cutoff: 1000%</option>",
                "<option value=\"10000\">Cutoff: 10000%</option>",
                "<option value=\"100000\">Cutoff: 100000%</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "</div>",
                "<div class=\"series-controls right-series-controls\">",
                "<select id=\"selectEtaRightCutoff\" class=\"chart-select\" onchange=\"setEtaRightCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"50\">Cutoff: 50%</option>",
                "<option value=\"100\">Cutoff: 100%</option>",
                "<option value=\"500\">Cutoff: 500%</option>",
                "<option value=\"1000\">Cutoff: 1000%</option>",
                "<option value=\"10000\">Cutoff: 10000%</option>",
                "<option value=\"100000\">Cutoff: 100000%</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnEtaRightLog\" class=\"chart-toggle-btn active\" onclick=\"switchEtaRightScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnEtaRightLinear\" class=\"chart-toggle-btn\" onclick=\"switchEtaRightScale('linear')\">Linear</button>",
                "</div>",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample eta-error-line\"></span>Prediction Error (vs real time)</span>",
                "</div>",
                "</div>",
                "<div class=\"line-graph-svg-wrap\"><svg id=\"etaDebugSvg\" class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">"
        ));

        final double logMaxInst = Math.log10(ceilInstabilityPct + 1.0);
        final double logMaxErr = Math.log10(ceilErrorPct + 1.0);

        for (int k = 0; k <= 4; k++) {
            final double ratio = (double) k / 4.0;
            final double y = (marginTop + plotH) - ratio * plotH;
            final double instPct = (ratio == 0.0) ? 0.0 : Math.pow(10, ratio * logMaxInst) - 1.0;
            final double errPct = (ratio == 0.0) ? 0.0 : Math.pow(10, ratio * logMaxErr) - 1.0;

            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.06)\" stroke-dasharray=\"3,3\" />\n",
                    marginLeft, y, marginLeft + plotW, y));

            final String leftLabel = (instPct < 10.0)
                    ? String.format(Locale.ROOT, "%.1f%%", instPct)
                    : String.format(Locale.ROOT, "%.0f%%", instPct);
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" fill=\"#ec4899\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    marginLeft - 8, y + 3.5, leftLabel));

            final String rightLabel = (errPct < 10.0)
                    ? String.format(Locale.ROOT, "%.1f%%", errPct)
                    : String.format(Locale.ROOT, "%.0f%%", errPct);
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"start\" fill=\"#f59e0b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    marginLeft + plotW + 8, y + 3.5, rightLabel));
        }

        final int N = items.size();
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
            final double x = marginLeft + (N > 1 ? (double) idx / (N - 1) * plotW : plotW / 2.0);
            final int tNum = items.get(idx).testNum;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.15)\" />\n",
                    x, marginTop + plotH, x, marginTop + plotH + 4));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">#%d</text>\n",
                    x, height - 10, tNum));
        }

        final StringBuilder instabilityPath = new StringBuilder();
        final StringBuilder errorPath = new StringBuilder();

        for (int i = 0; i < N; i++) {
            final EtaDebugItem item = items.get(i);
            final double x = marginLeft + (N > 1 ? (double) i / (N - 1) * plotW : plotW / 2.0);
            final double yInst = logScaleY(item.instabilityPct(), ceilInstabilityPct, marginTop, plotH);
            final double yErr = logScaleY(item.errorPct(), ceilErrorPct, marginTop, plotH);

            if (i == 0) {
                instabilityPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yInst));
                errorPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yErr));
            } else {
                instabilityPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yInst));
                errorPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yErr));
            }
        }

        sb.append(s(
                "  <path d=\"", instabilityPath, "\" class=\"eta-instability-chart-line\" fill=\"none\" stroke=\"#ec4899\" stroke-width=\"2\" />\n",
                "  <path d=\"", errorPath, "\" class=\"eta-error-chart-line\" fill=\"none\" stroke=\"#f59e0b\" stroke-width=\"2\" stroke-dasharray=\"4,3\" />\n",
                String.format(Locale.ROOT,
                        "  <line id=\"etaDebugCrosshair\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-dasharray=\"2,2\" stroke-width=\"1\" opacity=\"0\" pointer-events=\"none\" />\n",
                        marginTop, marginTop + plotH),
                "  <circle id=\"etaInstabilityDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#ec4899\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n",
                "  <circle id=\"etaErrorDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#f59e0b\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n",
                String.format(Locale.ROOT,
                        "  <rect id=\"etaDebugOverlay\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"transparent\" style=\"cursor: crosshair;\" onmousemove=\"onEtaDebugHover(event)\" onmouseleave=\"onEtaDebugLeave()\" />\n",
                        marginLeft, marginTop, plotW, plotH),
                "</svg>\n",
                "<div id=\"etaDebugTooltip\" class=\"lg-tooltip hidden\"></div>\n",
                "</div></div></section>"
        ));

        return sb.toString();
    }

    /**
     * Renders SVG card and script container for System Resources (CPU & RAM) graph.
     *
     * @param items list of SystemResourceItem records
     * @return HTML string for system resources chart card
     */
    public static String renderResourceStatsGraph(final List<SystemResourceItem> items) {
        if (null == items || items.isEmpty()) return "";

        final StringBuilder sb = new StringBuilder();
        sb.append(s(
                "<section id=\"resourceStatsChart\" class=\"resource-stats-chart\"><div class=\"chart-card\">",
                "<div class=\"card-title\"><span>System Resources (CPU &amp; RAM)</span>",
                "<span class=\"card-subtitle\">Y: CPU (%) &amp; Heap RAM (MB); X: sample</span></div>",
                "<div class=\"chart-controls-bar\">",
                "<div class=\"series-controls left-series-controls\">",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample res-cpu-line\"></span>CPU Usage (%)</span>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnResLeftLog\" class=\"chart-toggle-btn active\" onclick=\"switchResLeftScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnResLeftLinear\" class=\"chart-toggle-btn\" onclick=\"switchResLeftScale('linear')\">Linear</button>",
                "</div>",
                "<select id=\"selectResLeftCutoff\" class=\"chart-select\" onchange=\"setResLeftCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"100\">Cutoff: 100%</option>",
                "<option value=\"500\">Cutoff: 500%</option>",
                "<option value=\"1000\">Cutoff: 1000%</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "</div>",
                "<div class=\"series-controls right-series-controls\">",
                "<select id=\"selectResRightCutoff\" class=\"chart-select\" onchange=\"setResRightCutoff(this.value)\">",
                "<option value=\"auto\" selected>Cutoff: Auto</option>",
                "<option value=\"100\">Cutoff: 100MB</option>",
                "<option value=\"500\">Cutoff: 500MB</option>",
                "<option value=\"1000\">Cutoff: 1000MB</option>",
                "<option value=\"max\">Cutoff: Max</option>",
                "</select>",
                "<div class=\"chart-view-toggle\">",
                "<button type=\"button\" id=\"btnResRightLog\" class=\"chart-toggle-btn active\" onclick=\"switchResRightScale('log')\">Log</button>",
                "<button type=\"button\" id=\"btnResRightLinear\" class=\"chart-toggle-btn\" onclick=\"switchResRightScale('linear')\">Linear</button>",
                "</div>",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample res-ram-line\"></span>Used Heap RAM (MB)</span>",
                "<span class=\"lg-legend-item\"><span class=\"lg-line-sample res-total-ram-line\"></span>Total Heap RAM (MB)</span>",
                "</div>",
                "</div>",
                "<div class=\"line-graph-svg-wrap\"><svg id=\"resourceStatsSvg\" class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">",
                "  <text x=\"500\" y=\"120\" fill=\"#94a3b8\" text-anchor=\"middle\" font-size=\"14\">Loading resource statistics...</text>",
                "</svg></div></div></section>\n"
        ));
        return sb.toString();
    }
}
