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
     * Computes execution statuses for individual scenarios within a test section based on log line severe/warning indicators.
     *
     * @param sec the log section for a test (may be null)
     * @return map of scenario token names to their status ("passed", "warning", or "failed")
     */
    public static Map<String, String> computeScenarioStatuses(final LogSection sec) {
        final Map<String, String> statuses = new HashMap<>(8);
        if (null == sec || sec.lines.isEmpty()) return statuses;

        String currentScen = null;
        final Matcher mScenToken = SCEN_TOKEN_RE.matcher("");
        for (final String line : sec.lines) {
            final String clean = ANSI_PATTERN.matcher(line).replaceAll("").trim();
            mScenToken.reset(clean);
            if (mScenToken.find()) {
                final String sc = mScenToken.group(2).trim();
                if (!sc.isEmpty() && !"Scenario".equalsIgnoreCase(sc) && !"Specifics".equalsIgnoreCase(sc)
                        && !sc.toLowerCase(Locale.ROOT).endsWith(".specifics")) {
                    currentScen = sc;
                    statuses.putIfAbsent(currentScen, "passed");
                }
            }
            if (null != currentScen) {
                if (clean.contains("[SEVERE]") || clean.contains("[FAILURE]")) {
                    statuses.put(currentScen, "failed");
                } else if (clean.contains("[WARNING]") && !"failed".equals(statuses.get(currentScen))) {
                    statuses.put(currentScen, "warning");
                }
            }
        }
        return statuses;
    }

    /**
     * Resolves the execution status for a scenario step name.
     *
     * @param scenName         the scenario step name
     * @param sIdx             the step index (-1 if not in path row)
     * @param totalSteps       total steps in the path row (-1 if not in path row)
     * @param testStatus       overall section status ("passed", "warning", "failed")
     * @param scenarioStatuses map of scenario statuses for the test section
     * @return status string ("passed", "warning", or "failed")
     */
    public static String resolveScenarioStatus(
            final String scenName,
            final int sIdx,
            final int totalSteps,
            final String testStatus,
            final Map<String, String> scenarioStatuses) {
        if (null != scenName && null != scenarioStatuses && !scenarioStatuses.isEmpty()) {
            final String scenLower = scenName.toLowerCase(Locale.ROOT);
            for (final Map.Entry<String, String> entry : scenarioStatuses.entrySet()) {
                final String keyLower = entry.getKey().toLowerCase(Locale.ROOT);
                if (keyLower.equals(scenLower)
                        || keyLower.endsWith("." + scenLower)
                        || scenLower.endsWith("." + keyLower)) {
                    return entry.getValue();
                }
            }
        }
        if (sIdx >= 0 && sIdx == totalSteps - 1) {
            if ("failed".equals(testStatus)) return "failed";
            if ("warning".equals(testStatus)) return "warning";
        }
        return "passed";
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
            LogSection testSec = null;
            for (final LogSection sec : sections) {
                if ("test".equals(sec.type) && String.valueOf(sec.testNum).equals(p.num)) {
                    testStatus = sec.status;
                    testSec = sec;
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

            final Map<String, String> scenarioStatuses = computeScenarioStatuses(testSec);
            boolean hasFailedStep = false;

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
                    String nodeCssClass = resolveScenarioStatus(targetScen, sIdx, p.steps.size(), testStatus, scenarioStatuses);

                    if ("passed".equals(nodeCssClass) && sIdx == p.steps.size() - 1 && !hasFailedStep) {
                        if ("failed".equals(testStatus)) nodeCssClass = "failed";
                        else if ("warning".equals(testStatus)) nodeCssClass = "warning";
                    }

                    if ("failed".equals(nodeCssClass) || "warning".equals(nodeCssClass)) {
                        hasFailedStep = true;
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
                    "<details id=\"", secId, "\" class=\"log-section ", sec.type, " ", sec.status, "\" ontoggle=\"loadSectionLines(this)\" data-test=\"", sec.testNum, "\">\n",
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
                final Map<String, String> scenStatuses = computeScenarioStatuses(sec);
                sb.append("  <div class=\"section-breadcrumb\">\n    <span class=\"breadcrumb-label\">Scenario:</span>\n");
                for (final String sc : sec.scenarios) {
                    if (sc.equalsIgnoreCase("Specifics") || sc.toLowerCase(java.util.Locale.ROOT).endsWith(".specifics")) continue;
                    final String scStatus = resolveScenarioStatus(sc, -1, -1, sec.status, scenStatuses);
                    sb.append(s("    <a class=\"pill scen-pill ", scStatus, "\" href=\"#test-", sec.testNum, "-", escapeHtml(sc),
                            "\" onclick=\"focusScenario(event, '", sec.testNum, "', '", escapeHtml(sc),
                            "')\" title=\"Jump to ", escapeHtml(sc), "\">", escapeHtml(sc), "</a>\n"));
                }
                sb.append("  </div>\n");
            }

            sb.append("  <div class=\"log-section-body\">\n");
            appendSectionLines(sb, sec);
            sb.append("  </div>\n</details>\n");
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
            final LogSection section) {
        output.append("<div class=\"log-content\"><script type=\"text/plain\" class=\"raw-lines\">\n");
        for (int lineIndex = 0; lineIndex < section.lines.size(); lineIndex++) {
            final String rawLine = section.lines.get(lineIndex);
            output.append(escapeHtml(rawLine)).append('\n');
        }
        output.append("</script></div>\n");
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
