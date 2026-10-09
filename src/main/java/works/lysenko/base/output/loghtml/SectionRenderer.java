package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.PathEntry;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.loghtml.LogModels.TelemetryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    "<a class=\"path-step common\" href=\"#", escapeHtml(step),
                    "\" onclick=\"focusScenario(event, null, '", escapeHtml(step),
                    "')\" title=\"Jump to ", escapeHtml(step), " in log\">",
                    escapeHtml(step), "</a>"));
        }
        sb.append("</div></div>\n");
        return sb.toString();
    }

    /**
     * Renders HTML rows for recorded test paths.
     *
     * @param testPaths recorded test paths with execution steps
     * @return HTML snippet containing path rows
     */
    public static String renderPathsRows(final List<PathEntry> testPaths) {
        final StringBuilder sb = new StringBuilder();
        for (final PathEntry p : testPaths) {
            sb.append(s("<div class=\"path-row\" onclick=\"focusSection('Test #", p.num, "')\">",
                    "<div class=\"path-meta\">",
                    "<span class=\"path-num\">Test #", p.num, "</span>",
                    "<span class=\"path-dur\">⏱ ", p.durStr, "</span>",
                    "</div><div class=\"path-chain\">"));
            for (int sIdx = 0; sIdx < p.steps.size(); sIdx++) {
                if (sIdx > 0) sb.append(" <span class=\"step-arrow\">→</span> ");
                final String stepName = p.steps.get(sIdx);
                sb.append(s("<a class=\"path-step\" href=\"#test-", p.num, "-", escapeHtml(stepName),
                        "\" onclick=\"focusScenario(event, '", p.num, "', '", escapeHtml(stepName),
                        "')\" title=\"Jump to ", escapeHtml(stepName), " in Test #", p.num, "\">",
                        escapeHtml(stepName), "</a>"));
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
            final String evtBadge = !sc.events.isEmpty()
                    ? s("<span class=\"badge warning\" style=\"margin-left: 8px;\">", escapeHtml(sc.events), "</span>")
                    : "";
            sb.append(s("<tr>",
                    "<td><span class=\"", symClass, "\">", sc.sym,
                    "</span> <a class=\"scen-stat-link\" href=\"#", escapeHtml(sc.name),
                    "\" onclick=\"focusScenario(event, null, '", escapeHtml(sc.name),
                    "')\" title=\"Jump to ", escapeHtml(sc.name), " in log\"><b>",
                    escapeHtml(sc.name), "</b></a></td>",
                    "<td class=\"cell-mono text-muted\">", escapeHtml(sc.weight), "</td>",
                    "<td class=\"cell-mono text-right\"><b>", sc.count, "</b>", evtBadge, "</td>",
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
        final StringBuilder sb = new StringBuilder();
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
            final String timeRange = (sec.startTime != null && sec.endTime != null) ? s(sec.startTime, "s ➔ ", sec.endTime, "s") : "";
            final String secId = s("sec_", sec.title.replace(" ", "_").replace("#", "").replace("➔", "to"));

            sb.append(s(
                    "<details id=\"", secId, "\" class=\"log-section ", sec.type, " ", sec.status, "\">\n",
                    "  <summary>\n    <div class=\"summary-left\">\n",
                    opRange.isEmpty() ? "" : s("      <span class=\"op-range\">", opRange, "</span>\n"),
                    "      <span class=\"sec-title\">", escapeHtml(sec.title), "</span>\n",
                    "      <span class=\"badge ", badgeClass, "\">", badgeText, "</span>\n",
                    (sec.durationStr != null && !sec.durationStr.isEmpty()) ? s("      <span class=\"duration", isLimbo ? " limbo-duration" : "", "\">", sec.durationStr, "</span>\n") : "",
                    "    </div>\n    <div class=\"summary-right\">\n",
                    timeRange.isEmpty() ? "" : s("      <span class=\"time-range\">", timeRange, "</span>\n"),
                    "      <span class=\"line-count\">", sec.lines.size(), " lines</span>\n    </div>\n  </summary>\n"
            ));

            if ("test".equals(sec.type) && !sec.scenarios.isEmpty()) {
                sb.append("  <div class=\"section-breadcrumb\">\n    <span class=\"breadcrumb-label\">Scenario:</span>\n");
                for (final String sc : sec.scenarios) {
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
                    final boolean isOpLine = LINE_WITH_TEST_RE.matcher(cleanL).matches() || LINE_NO_TEST_RE.matcher(cleanL).matches();
                    if (isOpLine && currOpGlobalIdx < telemetryCpu.size()) {
                        final double cpuVal = telemetryCpu.get(currOpGlobalIdx++);
                        if (cpuVal <= 0.0 || cpuVal > 100.0) {
                            cpuRows.append("<div class=\"st-row rate-white\" title=\"-\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                        } else {
                            final String cClass = (cpuVal > 47.0) ? " rate-red" : (cpuVal > 44.0) ? " rate-yellow" : (cpuVal > 38.0) ? " rate-green" : (cpuVal > 25.0) ? " rate-blue" : " rate-white";
                            final double cpuPct = Math.min(1.0, Math.max(0.0, cpuVal / 50.0));
                            final int cpuW = Math.max(2, Math.min(32, (int) Math.round(cpuPct * 32)));
                            cpuRows.append(String.format(Locale.ROOT, s("<div class=\"st-row%s\" title=\"%.2f%%\\\"><div class=\"st-wave\" style=\"width: %dpx;\"></div></div>"), cClass, cpuVal, cpuW));
                        }
                    } else {
                        cpuRows.append("<div class=\"st-row rate-white\" title=\"-\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
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
}
