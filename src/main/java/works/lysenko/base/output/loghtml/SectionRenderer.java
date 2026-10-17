package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.PathEntry;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.loghtml.LogModels.TelemetryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.base.output.loghtml.LogFormatting.toSeconds;
import static works.lysenko.base.output.loghtml.LogFormatting.SCEN_TOKEN_RE;
import static works.lysenko.base.output.loghtml.LogFormatting.escapeHtml;
import static works.lysenko.base.output.loghtml.LogFormatting.extractLineSpan;
import static works.lysenko.base.output.loghtml.LogFormatting.linkifyArtifacts;
import static works.lysenko.base.output.loghtml.LogFormatting.parseAnsi;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Renderer for HTML section blocks, summary plaques, statistics strips, paths tables, and soundtrack optical tracks.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall", "OverlyLongMethod", "MethodWithTooManyParameters"})
public final class SectionRenderer {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern SPAN_BRACKET_RE = Pattern.compile("(\\[[0-9.]+\\](?:<[^>]+>)*)(\\[\\s*\\d+\\s*\\])");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");

    private SectionRenderer() {
    }

    /**
     * Renders the top status result plaque (passed/failed/neutral banner).
     *
     * @param status  execution status ("passed", "failed", or "neutral")
     * @param message summary plaque message
     * @return HTML result plaque snippet
     */
    public static String renderResultPlaque(final String status, final String message) {
        if (null == message || message.isEmpty()) return "";
        final String escaped = escapeHtml(message);
        final String formatted = linkifyArtifacts(escaped.replace("• ", "•").replace("•", "<br>&nbsp;&nbsp;• "));
        return s(
                "<div class=\"result-plaque-wrap\">\n",
                "  <div class=\"result-plaque ", status, "\">",
                formatted,
                "</div>\n",
                "</div>\n"
        );
    }

    /**
     * Renders the execution statistics summary strip displaying min/avg/max/total times.
     *
     * @param testData  list of test telemetry items
     * @param limboData list of limbo transition telemetry items
     * @param tMin      minimum test time in seconds
     * @param tAvg      average test time in seconds
     * @param tMax      maximum test time in seconds
     * @param tTotal    total test time in seconds
     * @param lMin      minimum limbo time in milliseconds
     * @param lAvg      average limbo time in milliseconds
     * @param lMax      maximum limbo time in milliseconds
     * @param lTotal    total limbo time in milliseconds
     * @return HTML statistics strip snippet
     */
    public static String renderStatsStrip(
            final List<TelemetryItem> testData, final List<TelemetryItem> limboData,
            final double tMin, final double tAvg, final double tMax, final double tTotal,
            final double lMin, final double lAvg, final double lMax, final double lTotal) {
        final StringBuilder sb = new StringBuilder();
        if (!testData.isEmpty()) {
            sb.append(String.format(Locale.ROOT,
                    s("      <div class=\"stat-group\"><span class=\"stat-group-label\">Tests:</span> ",
                            "<div class=\"stat-item\">min: <b>%.2fs</b></div>",
                            "<div class=\"stat-item\">avg: <b>%.2fs</b></div>",
                            "<div class=\"stat-item\">max: <b>%.2fs</b></div>",
                            "<div class=\"stat-item\">total: <b>%.2fs</b></div></div>\n"),
                    tMin, tAvg, tMax, tTotal));
        }
        if (!limboData.isEmpty()) {
            if (!testData.isEmpty()) {
                sb.append("      <div class=\"stat-divider\"></div>\n");
            }
            sb.append(String.format(Locale.ROOT,
                    s("      <div class=\"stat-group stat-group-limbo\"><span class=\"stat-group-label\">Limbo:</span> ",
                            "<div class=\"stat-item\">min: <b>%dms</b></div>",
                            "<div class=\"stat-item\">avg: <b>%dms</b></div>",
                            "<div class=\"stat-item\">max: <b>%dms</b></div>",
                            "<div class=\"stat-item\">total: <b>%dms</b></div></div>\n"),
                    Math.round(lMin), Math.round(lAvg), Math.round(lMax), Math.round(lTotal)));
        }
        return sb.toString();
    }

    private static boolean isStandIn(final String step) {
        if (null == step) return false;
        final String t = step.trim();
        return "-".equals(t) || "[-]".equals(t) || "–".equals(t) || "—".equals(t)
                || (t.startsWith("[") && t.endsWith("]") && "-".equals(t.substring(1, t.length() - 1).trim()));
    }

    /**
     * Renders the common execution path breadcrumb element.
     *
     * @param commonPathSteps list of scenario step names common across tests
     * @return HTML snippet representing the common path
     */
    public static String renderCommonPath(final List<String> commonPathSteps) {
        if (commonPathSteps.isEmpty()) return "";
        final StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"common-path-box\"><span class=\"common-path-label\">Common path:</span><div class=\"path-chain\">");
        for (final String step : commonPathSteps) {
            sb.append(s(" <span class=\"step-arrow\">→</span> ",
                    "<span class=\"path-step common\">", escapeHtml(step), "</span>"));
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    /**
     * Renders HTML rows for recorded test paths.
     *
     * @param testPaths recorded test paths with execution steps
     * @param sections  parsed log sections for status lookup
     * @return HTML snippet containing path rows
     */
    public static String renderPathsRows(final List<PathEntry> testPaths, final List<LogSection> sections) {
        return renderPathsRows(testPaths, sections, List.of());
    }

    /**
     * Renders HTML rows for recorded test paths with common path resolution.
     *
     * @param testPaths       recorded test paths with execution steps
     * @param sections        parsed log sections for status lookup
     * @param commonPathSteps common path scenario step names
     * @return HTML snippet containing path rows
     */
    public static String renderPathsRows(final List<PathEntry> testPaths, final List<LogSection> sections, final List<String> commonPathSteps) {
        final StringBuilder sb = new StringBuilder();
        final int commonCount = (null != commonPathSteps) ? commonPathSteps.size() : 0;

        for (final PathEntry p : testPaths) {
            String testStatus = "neutral";
            for (final LogSection sec : sections) {
                if ("test".equals(sec.type) && String.valueOf(sec.testNum).equals(p.num)) {
                    testStatus = sec.status;
                    break;
                }
            }

            sb.append(s("<div class=\"path-row\" onclick=\"focusSection('Test #", p.num, "')\">",
                    "<div class=\"path-meta\">",
                    "<span class=\"path-num\">Test #", p.num, "</span>",
                    "<span class=\"path-dur\">⏱ ", p.durStr, "</span>",
                    "</div><div class=\"path-chain\">"));

            int matchedCommon = 0;
            if (commonCount > 0) {
                for (int i = 0; i < commonCount && i < p.steps.size(); i++) {
                    final String stepName = p.steps.get(i);
                    final String commonName = commonPathSteps.get(i);
                    if (stepName.equals(commonName) || isStandIn(stepName)) {
                        matchedCommon++;
                    } else {
                        break;
                    }
                }
            }

            for (int sIdx = 0; sIdx < p.steps.size(); sIdx++) {
                if (sIdx > 0) sb.append(" <span class=\"step-arrow\">→</span> ");
                final String stepName = p.steps.get(sIdx);
                final String targetScen = (null != commonPathSteps && sIdx < commonPathSteps.size() && isStandIn(stepName))
                        ? commonPathSteps.get(sIdx)
                        : stepName;

                if (sIdx < matchedCommon) {
                    sb.append(s("<a class=\"path-step common-dash\" href=\"#test-", p.num, "-", escapeHtml(targetScen),
                            "\" onclick=\"focusScenario(event, '", p.num, "', '", escapeHtml(targetScen),
                            "')\" title=\"Jump to ", escapeHtml(targetScen), " in Test #", p.num, "\">-</a>"));
                } else {
                    String nodeCssClass = "passed";
                    if (sIdx == p.steps.size() - 1) {
                        if ("failed".equals(testStatus)) nodeCssClass = "failed";
                        else if ("warning".equals(testStatus)) nodeCssClass = "warning";
                    }

                    sb.append(s("<a class=\"path-step ", nodeCssClass, "\" href=\"#test-", p.num, "-", escapeHtml(targetScen),
                            "\" onclick=\"focusScenario(event, '", p.num, "', '", escapeHtml(targetScen),
                            "')\" title=\"Jump to ", escapeHtml(targetScen), " in Test #", p.num, "\">",
                            escapeHtml(targetScen), "</a>"));
                }
            }
            sb.append("</div></div>");
        }
        return sb.toString();
    }

    /**
     * Builds the subtitle text for scenario coverage statistics.
     *
     * @param pathsPossibleStr possible paths summary text
     * @param pathsChanceStr   paths chance summary text
     * @param pathsExecutedStr executed paths summary text
     * @return HTML formatted subtitle string
     */
    public static String buildScenSubtitle(final String pathsPossibleStr, final String pathsChanceStr, final String pathsExecutedStr) {
        final List<String> parts = new ArrayList<>(3);
        if (!pathsPossibleStr.isEmpty()) parts.add(escapeHtml(pathsPossibleStr));
        if (!pathsChanceStr.isEmpty()) parts.add(escapeHtml(pathsChanceStr));
        if (!pathsExecutedStr.isEmpty()) parts.add(escapeHtml(pathsExecutedStr));
        return String.join("<br>", parts);
    }

    /**
     * Renders HTML table rows for scenario execution statistics.
     *
     * @param scenStats scenario execution statistics entries
     * @return HTML snippet containing scenario rows
     */
    public static String renderScenRows(final List<ScenEntry> scenStats) {
        final StringBuilder sb = new StringBuilder();
        for (final ScenEntry sc : scenStats) {
            final String symClass = "▷".equals(sc.sym) ? "sym-node"
                    : "◼".equals(sc.sym) ? "sym-leaf"
                    : "◆".equals(sc.sym) ? "sym-fork"
                    : "sym-other";

            final String ev = (null != sc.events) ? sc.events.toLowerCase(Locale.ROOT) : "";
            final boolean isFail = ev.contains("fail") || ev.contains("error") || ev.contains("exception") || ev.contains("child_failed");
            final boolean isWarn = !isFail && (ev.contains("event") || ev.contains("warn") || ev.contains("issue") || !ev.isEmpty());

            final String countRaw = (null != sc.count) ? sc.count.trim() : "";
            final String hitsStr;
            final String instancesStr;
            final boolean isSpecial;

            if (countRaw.contains(":")) {
                final String[] parts = countRaw.split(":");
                hitsStr = parts[0];
                instancesStr = (parts.length > 1) ? parts[1] : "";
                isSpecial = true;
            } else {
                hitsStr = countRaw;
                instancesStr = "";
                isSpecial = false;
            }

            final List<String> classes = new ArrayList<>(2);
            if (isFail) {
                classes.add("scen-row-failed");
            } else if (isWarn) {
                classes.add("scen-row-warning");
            }
            if (isSpecial) {
                classes.add("scen-row-special");
            }

            final String rowCssClass = !classes.isEmpty() ? s(" class=\"", String.join(" ", classes), "\"") : "";

            String eventsCountStr = "";
            if (!ev.isEmpty()) {
                final String digits = sc.events.replaceAll("[^0-9]", "");
                final String val = !digits.isEmpty() ? digits : "1";
                if (!"0".equals(val)) {
                    eventsCountStr = val;
                }
            }

            sb.append(s("<tr", rowCssClass, ">",
                    "<td><span class=\"", symClass, "\">", sc.sym,
                    "</span> <a class=\"scen-stat-link\" href=\"#", escapeHtml(sc.name),
                    "\" onclick=\"showScenarioExecutionsMenu(event, '", escapeHtml(sc.name),
                    "')\" title=\"View executions of ", escapeHtml(sc.name), "\"><b>",
                    escapeHtml(sc.name), "</b></a></td>",
                    "<td class=\"cell-mono text-muted\">", escapeHtml(sc.weight), "</td>",
                    "<td class=\"cell-mono text-right\"><b>", hitsStr, "</b></td>",
                    "<td class=\"cell-mono text-right\">",
                    instancesStr.isEmpty() ? "" : s("<b>", instancesStr, "</b>"),
                    "</td>",
                    "<td class=\"cell-mono text-right\">",
                    eventsCountStr.isEmpty() ? "" : s("<b>", eventsCountStr, "</b>"),
                    "</td>",
                    "</tr>"));
        }
        return sb.toString();
    }

    /**
     * Renders HTML collapsible sections for all log sections, including inline artifacts and soundtrack optical tracks.
     *
     * @param sections        parsed log sections
     * @param runArtifacts    list of run artifact items
     * @param telemetryCpu    list of CPU telemetry measurements
     * @param currOpGlobalIdx global operation index for CPU telemetry alignment
     * @return HTML snippet containing log sections
     */
    public static String renderSections(
            final List<LogSection> sections,
            final List<ArtifactItem> runArtifacts,
            final List<Double> telemetryCpu,
            int currOpGlobalIdx) {
        return renderSections(sections, runArtifacts, Map.of(), telemetryCpu, currOpGlobalIdx);
    }

    /**
     * Renders HTML collapsible sections for all log sections, including inline artifacts and soundtrack optical tracks.
     *
     * @param sections        parsed log sections
     * @param runArtifacts    list of run artifact items
     * @param telemetryCpuMap map of operation number to CPU telemetry measurement
     * @param telemetryCpuList list of CPU telemetry measurements
     * @param currOpGlobalIdx global operation index for CPU telemetry alignment
     * @return HTML snippet containing log sections
     */
    public static String renderSections(
            final List<LogSection> sections,
            final List<ArtifactItem> runArtifacts,
            final Map<Integer, Double> telemetryCpuMap,
            final List<Double> telemetryCpuList,
            int currOpGlobalIdx) {
        double minCpu = Double.MAX_VALUE;
        double maxCpu = -Double.MAX_VALUE;
        final List<Double> allCpuVals = new ArrayList<>();
        if (null != telemetryCpuMap && !telemetryCpuMap.isEmpty()) {
            for (final Double val : telemetryCpuMap.values()) {
                if (null != val && val > 0.0 && val <= 100.0) {
                    allCpuVals.add(val);
                    if (val < minCpu) minCpu = val;
                    if (val > maxCpu) maxCpu = val;
                }
            }
        } else if (null != telemetryCpuList) {
            for (final Double val : telemetryCpuList) {
                if (null != val && val > 0.0 && val <= 100.0) {
                    allCpuVals.add(val);
                    if (val < minCpu) minCpu = val;
                    if (val > maxCpu) maxCpu = val;
                }
            }
        }
        Collections.sort(allCpuVals);
        final boolean hasCpuRange = (minCpu < maxCpu) && (maxCpu > 0.0);

        double lastSeenCpu = 0.0;
        final StringBuilder sb = new StringBuilder();
        sb.append(renderLegendSection(hasCpuRange, minCpu, maxCpu));
        for (final LogSection sec : sections) {
            final boolean isTest = "test".equals(sec.type);
            final boolean isLimbo = "limbo".equals(sec.type) || "configuring".equals(sec.type);
            final String badgeClass = ("warning".equals(sec.status) || "failed".equals(sec.status)) ? sec.status
                    : isTest ? sec.status : isLimbo ? "limbo" : "neutral";
            final String badgeText = isTest ? sec.status
                    : "limbo".equals(sec.type) ? "limbo"
                    : "configuring".equals(sec.type) ? "config"
                    : "preflight".equals(sec.type) ? "preflight"
                    : "postflight".equals(sec.type) ? "postflight"
                    : "booting".equals(sec.type) ? "boot"
                    : sec.type;
            final String opRange = sec.startOp != null ? s("[", sec.startOp, "..", sec.endOp, "]") : "";
            final String fullTimeRange = (sec.startTime != null && sec.endTime != null) ? s(sec.startTime, "s ➔ ", sec.endTime, "s") : "";
            final String timeRange;
            if (isLimbo && sec.startTime != null && sec.endTime != null) {
                timeRange = String.format(Locale.ROOT, "%.1fs ➔ %.1fs", toSeconds(sec.startTime), toSeconds(sec.endTime));
            } else {
                timeRange = fullTimeRange;
            }
            final String secId = s("sec_", sec.title.replace(" ", "_").replace("#", "").replace("➔", "to"));

            sb.append(s(
                    "<details id=\"", secId, "\" class=\"log-section ", sec.type, " ", sec.status, "\">\n",
                    "  <summary>\n    <div class=\"summary-left\">\n",
                    opRange.isEmpty() ? "" : s("      <span class=\"op-range\">", opRange, "</span>\n"),
                    "      <span class=\"sec-title\">", escapeHtml(sec.title), "</span>\n",
                    "      <span class=\"badge ", badgeClass, "\">", badgeText, "</span>\n",
                    (sec.durationStr != null && !sec.durationStr.isEmpty()) ? s("      <span class=\"duration", isLimbo ? " limbo-duration" : "", "\">", sec.durationStr, "</span>\n") : "",
                    "    </div>\n    <div class=\"summary-right\">\n",
                    timeRange.isEmpty() ? "" : s("      <span class=\"time-range\" title=\"", fullTimeRange, "\">", timeRange, "</span>\n"),
                    "      <span class=\"line-count\">", sec.lines.size(), " lines</span>\n    </div>\n  </summary>\n"
            ));

            if ("test".equals(sec.type) && !sec.scenarios.isEmpty()) {
                sb.append("  <div class=\"section-breadcrumb\">\n    <span class=\"breadcrumb-label\">Scenario:</span>\n");
                for (final String sc : sec.scenarios) {
                    if (sc.equalsIgnoreCase("Specifics") || sc.toLowerCase(java.util.Locale.ROOT).endsWith(".specifics")) continue;
                    sb.append(s("    <a class=\"pill scen-pill\" href=\"#test-", sec.testNum, "-", escapeHtml(sc),
                            "\" onclick=\"focusScenario(event, '", sec.testNum, "', '", escapeHtml(sc),
                            "')\" title=\"Jump to ", escapeHtml(sc), "\">", escapeHtml(sc), "</a>\n"));
                }
                sb.append("  </div>\n");
            }

            final boolean hasOps = sec.startOp != null;
            final List<Integer> lineSpans = new ArrayList<>();
            int maxS = 0;
            if (hasOps) {
                for (final String l : sec.lines) {
                    final int sp = extractLineSpan(l);
                    lineSpans.add(sp);
                    if (sp > maxS) maxS = sp;
                }
            }

            // Map artifacts to lines in this section
            final Map<Integer, List<ArtifactItem>> secArtifacts = new HashMap<>();
            final Map<Integer, List<ArtifactItem>> causativeArtifacts = new HashMap<>();
            for (final ArtifactItem a : runArtifacts) {
                if (sec.testNum > 0 && a.test() != sec.testNum) continue;
                if (sec.testNum == 0 && a.test() != 0) continue;

                Integer bestIdx = null;
                for (int lIdx = 0; lIdx < sec.lines.size(); lIdx++) {
                    final String rawL = sec.lines.get(lIdx);
                    if (rawL.contains(a.rel())) {
                        bestIdx = lIdx;
                        break;
                    }
                }
                if (bestIdx == null) {
                    for (int lIdx = 0; lIdx < sec.lines.size(); lIdx++) {
                        final String rawL = sec.lines.get(lIdx);
                        final String cleanRaw = ANSI_PATTERN.matcher(rawL).replaceAll("").trim();
                        final Matcher mTs = Pattern.compile("\\[([0-9]+\\.[0-9]+)\\]").matcher(cleanRaw);
                        if (mTs.find()) {
                            try {
                                final double lineSec = Double.parseDouble(mTs.group(1));
                                if (Math.abs(lineSec - a.sec()) < 0.05) {
                                    if (".properties".equals(a.ext()) && rawL.contains("Made '") && rawL.contains("snapshot of test data")) {
                                        bestIdx = lIdx;
                                        break;
                                    } else if (".xml".equals(a.ext()) && rawL.contains("snapshot of page code")) {
                                        bestIdx = lIdx;
                                        break;
                                    }
                                }
                            } catch (final NumberFormatException ignored) {}
                        }
                    }
                }
                if (bestIdx != null) {
                    secArtifacts.computeIfAbsent(bestIdx, k -> new ArrayList<>()).add(a);
                }

                Integer causativeIdx = null;
                for (int lIdx = 0; lIdx < sec.lines.size(); lIdx++) {
                    if (bestIdx != null && lIdx == bestIdx) continue;
                    final String rawL = sec.lines.get(lIdx);
                    final String cleanRaw = ANSI_PATTERN.matcher(rawL).replaceAll("").trim();
                    final Matcher mTs = Pattern.compile("\\[([0-9]+\\.[0-9]+)\\]").matcher(cleanRaw);
                    if (mTs.find()) {
                        try {
                            final double lineSec = Double.parseDouble(mTs.group(1));
                            final int spanMs = extractLineSpan(rawL);
                            if (spanMs > 0) {
                                final double lineEndSec = lineSec + (spanMs / 1000.0);
                                if (lineSec - 0.002 <= a.sec() && a.sec() <= lineEndSec + 0.005) {
                                    causativeIdx = lIdx;
                                    break;
                                }
                            }
                        } catch (final NumberFormatException ignored) {}
                    }
                }
                if (causativeIdx != null) {
                    causativeArtifacts.computeIfAbsent(causativeIdx, k -> new ArrayList<>()).add(a);
                }
            }

            if (hasOps && maxS > 0) {
                final StringBuilder stRows = new StringBuilder();
                final StringBuilder cpuRows = new StringBuilder();
                final double logScale = Math.log10(Math.max(15000, maxS) + 1);

                for (int lIdx = 0; lIdx < lineSpans.size(); lIdx++) {
                    final int sp = lineSpans.get(lIdx);
                    if (sp > 0) {
                        final double pct = Math.log10(sp + 1) / logScale;
                        final int wPx = Math.max(2, Math.min(32, (int) Math.round(pct * 32)));
                        final String rateClass = (sp > 999) ? " rate-red" : (sp > 99) ? " rate-yellow" : (sp > 49) ? " rate-green" : (sp > 19) ? " rate-blue" : " rate-white";
                        stRows.append(s("<div class=\"st-row", rateClass, "\" title=\"", sp, " ms\"><div class=\"st-wave\" style=\"width: ", wPx, "px;\"></div></div>"));
                    } else {
                        stRows.append("<div class=\"st-row rate-white\" title=\"0 ms\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.25;\"></div></div>");
                    }

                    final String lineText = sec.lines.get(lIdx);
                    final String cleanL = ANSI_PATTERN.matcher(lineText).replaceAll("").trim();
                    final Matcher mTest = LINE_WITH_TEST_RE.matcher(cleanL);
                    final Matcher mNoTest = mTest.matches() ? null : LINE_NO_TEST_RE.matcher(cleanL);
                    final boolean isOpLine = mTest.matches() || (null != mNoTest && mNoTest.matches());

                    Integer opNum = null;
                    if (mTest.matches()) {
                        try { opNum = Integer.parseInt(mTest.group(2).trim()); } catch (final NumberFormatException ignored) {}
                    } else if (null != mNoTest && mNoTest.matches()) {
                        try { opNum = Integer.parseInt(mNoTest.group(1).trim()); } catch (final NumberFormatException ignored) {}
                    }

                    if (null != opNum && null != telemetryCpuMap && telemetryCpuMap.containsKey(opNum)) {
                        lastSeenCpu = telemetryCpuMap.get(opNum);
                    } else if (isOpLine && null != telemetryCpuList && currOpGlobalIdx < telemetryCpuList.size()) {
                        lastSeenCpu = telemetryCpuList.get(currOpGlobalIdx++);
                    }

                    if (isOpLine && lastSeenCpu > 0.0 && lastSeenCpu <= 100.0) {
                        final double cpuVal = lastSeenCpu;
                        final double rel;
                        if (hasCpuRange && (maxCpu - minCpu) > 0.001) {
                            final double valRel = Math.min(1.0, Math.max(0.0, (cpuVal - minCpu) / (maxCpu - minCpu)));
                            final double rankRel = getPercentileRank(allCpuVals, cpuVal);
                            rel = Math.min(1.0, Math.max(0.0, 0.5 * valRel + 0.5 * rankRel));
                        } else {
                            rel = Math.min(1.0, Math.max(0.0, cpuVal / 50.0));
                        }
                        final String cClass = (rel > 0.80) ? " rate-red" : (rel > 0.60) ? " rate-yellow" : (rel > 0.40) ? " rate-green" : (rel > 0.20) ? " rate-blue" : " rate-white";
                        final int cpuW = Math.max(2, Math.min(32, (int) Math.round(rel * 32)));
                        final String tooltip = hasCpuRange
                                ? String.format(Locale.ROOT, "%.2f%% (range: %.1f%%-%.1f%%)", cpuVal, minCpu, maxCpu)
                                : String.format(Locale.ROOT, "%.2f%%", cpuVal);
                        cpuRows.append(String.format(Locale.ROOT, s("<div class=\"st-row%s\" title=\"%s\"><div class=\"st-wave\" style=\"width: %dpx;\"></div></div>"), cClass, tooltip, cpuW));
                    } else {
                        cpuRows.append("<div class=\"st-row rate-white\" title=\"-\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                    }

                    final int extraLines = countExtraNewlines(lineText);
                    for (int k = 0; k < extraLines; k++) {
                        stRows.append("<div class=\"st-row rate-white\" title=\"(multiline continuation)\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                        cpuRows.append("<div class=\"st-row rate-white\" title=\"(multiline continuation)\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                    }
                }

                sb.append(s(
                        "  <div class=\"log-section-body\">\n",
                        "    <div class=\"soundtrack-column\" title=\"35mm Optical Track (Operation Duration)\"><div class=\"soundtrack-track\">", stRows, "</div></div>\n",
                        "    <div class=\"log-content\">"
                ));
                appendSectionLines(sb, sec, causativeArtifacts, secArtifacts);
                sb.append(s(
                        "</div>\n",
                        "    <div class=\"soundtrack-column right-track\" title=\"35mm Optical Track (CPU Load)\"><div class=\"soundtrack-track\">", cpuRows, "</div></div>\n",
                        "  </div>\n</details>\n"
                ));
            } else {
                sb.append("  <div class=\"log-content\">");
                appendSectionLines(sb, sec, causativeArtifacts, secArtifacts);
                sb.append("</div>\n</details>\n");
            }
        }
        return sb.toString();
    }

    /**
     * Appends formatted log lines to a section output, injecting artifact links and line status badges.
     *
     * @param output             string builder output buffer
     * @param section            log section being rendered
     * @param causativeArtifacts artifacts created during specific log operations
     * @param sectionArtifacts   artifacts associated with specific section lines
     */
    public static void appendSectionLines(
            final StringBuilder output,
            final LogSection section,
            final Map<Integer, List<ArtifactItem>> causativeArtifacts,
            final Map<Integer, List<ArtifactItem>> sectionArtifacts) {
        final Map<String, Integer> scenCounts = new HashMap<>();
        final Set<String> seenOps = new HashSet<>();
        for (int lineIndex = 0; lineIndex < section.lines.size(); lineIndex++) {
            final String rawLine = section.lines.get(lineIndex);
            final String cleanL = ANSI_PATTERN.matcher(rawLine).replaceAll("").trim();
            final Matcher mTest = LINE_WITH_TEST_RE.matcher(cleanL);
            final Matcher mNoTest = mTest.matches() ? null : LINE_NO_TEST_RE.matcher(cleanL);

            String opNum = null;
            int tNum = section.testNum;
            if (mTest.matches()) {
                tNum = Integer.parseInt(mTest.group(1).trim());
                opNum = mTest.group(2).trim();
            } else if (null != mNoTest && mNoTest.matches()) {
                opNum = mNoTest.group(1).trim();
            }

            String scenName = null;
            if (cleanL.contains("▷") || cleanL.contains("◆") || cleanL.contains("◼") || cleanL.contains("●")) {
                final Matcher mScen = SCEN_TOKEN_RE.matcher(cleanL);
                if (mScen.find()) {
                    final String cand = mScen.group(2).trim();
                    if (!cand.isEmpty() && !"scenario".equalsIgnoreCase(cand)) {
                        scenName = cand;
                    }
                }
            }

            String parsedLine = parseAnsi(rawLine);
            final StringBuilder badgeHtml = new StringBuilder();

            final List<ArtifactItem> causative = causativeArtifacts.get(lineIndex);
            if (null != causative && !causative.isEmpty()) {
                final ArtifactItem firstArtifact = causative.get(0);
                final String tip = s("Artifact created during this operation: ", firstArtifact.rel());
                final Matcher spanMatcher = SPAN_BRACKET_RE.matcher(parsedLine);
                if (spanMatcher.find()) {
                    final String prefix = spanMatcher.group(1);
                    final String bracket = spanMatcher.group(2);
                    final String linkedBracket = s(
                            prefix, "<a class=\"log-artifact-link\" href=\"",
                            firstArtifact.rel(), "\" target=\"_blank\" title=\"", tip, "\">",
                            bracket, "</a>"
                    );
                    parsedLine = spanMatcher.replaceFirst(Matcher.quoteReplacement(linkedBracket));
                }
            }

            final List<ArtifactItem> matched = sectionArtifacts.get(lineIndex);
            if (null != matched) {
                for (final ArtifactItem artifact : matched) {
                    if (".properties".equals(artifact.ext()) && parsedLine.contains("snapshot of test data")
                            && !parsedLine.contains("log-artifact-link")) {
                        parsedLine = parsedLine.replaceAll(
                                "(Made\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+test\\s+data)",
                                s("<a class=\"log-artifact-link\" href=\"", artifact.rel(),
                                        "\" target=\"_blank\" title=\"Open artifact: ", artifact.rel(), "\">", "$1</a>"));
                    } else if (".xml".equals(artifact.ext()) && parsedLine.contains("snapshot of page code")
                            && !parsedLine.contains("log-artifact-link")) {
                        parsedLine = parsedLine.replaceAll(
                                "(Making\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+page\\s+code)",
                                s("<a class=\"log-artifact-link\" href=\"", artifact.rel(),
                                        "\" target=\"_blank\" title=\"Open artifact: ", artifact.rel(), "\">", "$1</a>"));
                    }
                    if (!parsedLine.contains(artifact.rel())) {
                        final String icon = ".properties".equals(artifact.ext()) ? "💾"
                                : ".xml".equals(artifact.ext()) ? "📄" : "📸";
                        final String type = ".properties".equals(artifact.ext()) ? "badge-data"
                                : ".xml".equals(artifact.ext()) ? "badge-xml" : "badge-img";
                        badgeHtml.append(s(
                                "<a class=\"gutter-artifact-badge ", type,
                                "\" href=\"", artifact.rel(),
                                "\" target=\"_blank\" title=\"Open ",
                                artifact.ext().isEmpty() ? "" : artifact.ext().substring(1),
                                ": ", artifact.rel(), "\">", icon, "</a>"
                        ));
                    }
                }
            }

            final StringBuilder divOpen = new StringBuilder("<div class=\"log-line-entry");
            final StringBuilder anchorsHtml = new StringBuilder();

            if (null != scenName) {
                divOpen.append(" is-scenario");
            }
            if (null != opNum && !seenOps.contains(opNum)) {
                seenOps.add(opNum);
                divOpen.append(" id=\"op_").append(opNum).append("\" data-op=\"").append(opNum).append("\"");
            }
            if (tNum > 0) {
                divOpen.append(" data-test=\"").append(tNum).append("\"");
            }

            if (null != scenName) {
                divOpen.append(" data-scenario=\"").append(escapeHtml(scenName)).append("\"");
                final int occ = scenCounts.merge(scenName, 1, Integer::sum);
                final String canonicalHash;
                if (tNum > 0) {
                    canonicalHash = s("test-", tNum, "-", scenName);
                    anchorsHtml.append(s("<a id=\"test-", tNum, "-", scenName, "\" class=\"scenario-anchor\"></a>"));
                    anchorsHtml.append(s("<a id=\"test_", tNum, "_", scenName, "\" class=\"scenario-anchor\"></a>"));
                    if (occ > 1) {
                        anchorsHtml.append(s("<a id=\"test-", tNum, "-", scenName, "-", occ, "\" class=\"scenario-anchor\"></a>"));
                        anchorsHtml.append(s("<a id=\"test_", tNum, "_", scenName, "_", occ, "\" class=\"scenario-anchor\"></a>"));
                    }
                    if (null != opNum) {
                        anchorsHtml.append(s("<a id=\"test-", tNum, "-op-", opNum, "\" class=\"scenario-anchor\"></a>"));
                        anchorsHtml.append(s("<a id=\"test_", tNum, "_op_", opNum, "\" class=\"scenario-anchor\"></a>"));
                    }
                } else if (null != opNum) {
                    canonicalHash = s("op_", opNum);
                } else {
                    canonicalHash = scenName;
                }
                if (1 == occ) {
                    anchorsHtml.append(s("<a id=\"", escapeHtml(scenName), "\" class=\"scenario-anchor\"></a>"));
                }
                badgeHtml.append(s(
                        "<a class=\"scen-link-btn\" href=\"#", canonicalHash,
                        "\" onclick=\"onScenarioLinkClick(event, '", canonicalHash, "')\" title=\"Copy link to this scenario (#",
                        canonicalHash, ")\">🔗</a>"
                ));
            }

            divOpen.append(">");
            if (badgeHtml.length() > 0) parsedLine = s(parsedLine, "  ", badgeHtml);
            output.append(divOpen).append(anchorsHtml).append(parsedLine).append("</div>");
        }
    }

    /**
     * Calculates the percentile rank (0.0 to 1.0) of a value within a sorted list.
     * If the value appears multiple times, the average rank of those occurrences is used.
     *
     * @param sortedValues non-null sorted list of values
     * @param val          value to search for
     * @return percentile rank between 0.0 and 1.0
     */
    private static double getPercentileRank(final List<Double> sortedValues, final double val) {
        if (null == sortedValues || sortedValues.isEmpty()) return 0.5;
        final int n = sortedValues.size();
        if (n <= 1) return 0.5;
        int first = -1;
        int last = -1;
        for (int i = 0; i < n; i++) {
            if (Math.abs(sortedValues.get(i) - val) < 1e-6) {
                if (first == -1) first = i;
                last = i;
            }
        }
        if (first == -1) {
            int pos = Collections.binarySearch(sortedValues, val);
            if (pos < 0) pos = -pos - 1;
            return Math.min(1.0, Math.max(0.0, (double) pos / (n - 1)));
        }
        final double avgIdx = (first + last) / 2.0;
        return Math.min(1.0, Math.max(0.0, avgIdx / (n - 1)));
    }

    /**
     * Renders the soundtrack optical track legend header section.
     *
     * @return HTML snippet for the soundtrack legend header
     */
    public static String renderLegendSection(final boolean hasCpuRange, final double minCpu, final double maxCpu) {
        final String cpu0, cpu20, cpu40, cpu60, cpu80, cpu100;
        if (hasCpuRange && (maxCpu - minCpu) > 0.001) {
            final double range = maxCpu - minCpu;
            cpu0 = String.format(Locale.ROOT, "%.1f", minCpu);
            cpu20 = String.format(Locale.ROOT, "%.1f", minCpu + 0.2 * range);
            cpu40 = String.format(Locale.ROOT, "%.1f", minCpu + 0.4 * range);
            cpu60 = String.format(Locale.ROOT, "%.1f", minCpu + 0.6 * range);
            cpu80 = String.format(Locale.ROOT, "%.1f", minCpu + 0.8 * range);
            cpu100 = String.format(Locale.ROOT, "%.1f", maxCpu);
        } else {
            // When no valid range, rel = cpuVal / 50.0 (maxes at 50%)
            cpu0 = "0";
            cpu20 = "10";
            cpu40 = "20";
            cpu60 = "30";
            cpu80 = "40";
            cpu100 = "≥50";
        }

        return s(
                "  <div class=\"log-legend-section\">\n",
                "    <div class=\"lg-title-group\">\n",
                "      <span class=\"badge neutral lg-badge\">LEGEND</span>\n",
                "    </div>\n",
                "    <div class=\"lg-items-group\">\n",
                "      <div class=\"lg-item\">\n",
                "        <span class=\"lg-st-sample rate-white\"></span>\n",
                "        <div class=\"lg-item-text\">\n",
                "          <span class=\"lg-val rate-white\">&lt;20 ms</span>\n",
                "          <span class=\"lg-sub rate-white\">", cpu0, "–", cpu20, "% CPU</span>\n",
                "        </div>\n",
                "      </div>\n",
                "      <div class=\"lg-item\">\n",
                "        <span class=\"lg-st-sample rate-blue\"></span>\n",
                "        <div class=\"lg-item-text\">\n",
                "          <span class=\"lg-val rate-blue\">20–49 ms</span>\n",
                "          <span class=\"lg-sub rate-blue\">", cpu20, "–", cpu40, "% CPU</span>\n",
                "        </div>\n",
                "      </div>\n",
                "      <div class=\"lg-item\">\n",
                "        <span class=\"lg-st-sample rate-green\"></span>\n",
                "        <div class=\"lg-item-text\">\n",
                "          <span class=\"lg-val rate-green\">50–99 ms</span>\n",
                "          <span class=\"lg-sub rate-green\">", cpu40, "–", cpu60, "% CPU</span>\n",
                "        </div>\n",
                "      </div>\n",
                "      <div class=\"lg-item\">\n",
                "        <span class=\"lg-st-sample rate-yellow\"></span>\n",
                "        <div class=\"lg-item-text\">\n",
                "          <span class=\"lg-val rate-yellow\">100–999 ms</span>\n",
                "          <span class=\"lg-sub rate-yellow\">", cpu60, "–", cpu80, "% CPU</span>\n",
                "        </div>\n",
                "      </div>\n",
                "      <div class=\"lg-item\">\n",
                "        <span class=\"lg-st-sample rate-red\"></span>\n",
                "        <div class=\"lg-item-text\">\n",
                "          <span class=\"lg-val rate-red\">≥1000 ms</span>\n",
                "          <span class=\"lg-sub rate-red\">", cpu80, "–", cpu100, "% CPU</span>\n",
                "        </div>\n",
                "      </div>\n",
                "    </div>\n",
                "    <div class=\"lg-track-labels\">\n",
                "      <span class=\"lg-track-tag\">Left: Duration</span>\n",
                "      <span class=\"lg-track-tag\">Right: CPU Load</span>\n",
                "    </div>\n",
                "  </div>\n"
        );
    }

    private static int countExtraNewlines(final String text) {
        if (null == text || text.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
