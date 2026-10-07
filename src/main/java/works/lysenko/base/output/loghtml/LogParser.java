package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.base.output.loghtml.LogModels.LineStore;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.PathEntry;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.loghtml.LogModels.TelemetryItem;
import works.lysenko.util.func.type.Files;
import works.lysenko.util.spec.PropEnum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.logEvent;
import static works.lysenko.base.output.loghtml.LogFormatting.calculateBootSpentSeconds;
import static works.lysenko.base.output.loghtml.LogFormatting.escapeHtml;
import static works.lysenko.base.output.loghtml.LogFormatting.formatDeltaTime;
import static works.lysenko.base.output.loghtml.LogFormatting.toSeconds;
import static works.lysenko.base.output.loghtml.SectionRenderer.buildScenSubtitle;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderCommonPath;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderPathsRows;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderResultPlaque;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderScenRows;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderSections;
import static works.lysenko.base.output.loghtml.SectionRenderer.renderStatsStrip;
import static works.lysenko.base.output.loghtml.TimelineRenderer.renderLgScriptData;
import static works.lysenko.base.output.loghtml.TimelineRenderer.renderLineGraph;
import static works.lysenko.base.output.loghtml.TimelineRenderer.renderTimeline;
import static works.lysenko.base.output.loghtml.TimelineRenderer.renderTimelineToggle;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * Log section parsing engine and report document assembler.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "NestedMethodCall", "OverlyLongMethod", "MethodWithTooManyParameters"})
public final class LogParser {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern PATH_LINE_RE = Pattern.compile("\\[\\s*(\\d+)\\s*\\]\\s*\\[\\s*([\\d,]+)\\s*\\]\\s*→\\s*(.*)");
    private static final Pattern SCEN_LINE_RE = Pattern.compile("([▷◼◆●])\\s*([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)[.\\s]+(\\[[^\\]]+\\]|\\([^)]+\\)\\s*→\\s*\\[[^\\]]+\\])\\s*(\\d+(?::\\d+)?)(?:\\s*\\(([^)]+)\\))?");
    private static final Pattern TEST_RUN_SUMMARY_RE = Pattern.compile(".*\\b\\d+\\s+tests?\\s+of\\s+.+\\s+done\\s+in\\s+.*");

    private LogParser() {
    }

    /**
     * Reads the run log file, categorizes log entries into execution sections, calculates metrics,
     * renders charts and HTML fragments, and populates the template.
     *
     * @param reader                      buffered reader for the run log
     * @param logFile                     input run log file
     * @param scenarioProgressionEnabled scenario progression animation flag
     * @param treeProgressionEnabled     tree progression animation flag
     * @param scenarioMp4Enabled         scenario MP4 video flag
     * @param treeMp4Enabled             tree MP4 video flag
     * @param lineStore                   disk-backed storage for log section lines
     * @return fully rendered HTML report content
     * @throws IOException if reading the log file fails
     */
    public static String buildHtml(
            final BufferedReader reader,
            final File logFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final LineStore lineStore) throws IOException {
        final List<ArtifactItem> runArtifacts = SidecarLoader.loadRunArtifacts(logFile);
        final List<Double> telemetryCpu = SidecarLoader.loadTelemetryCpu(logFile);
        int currOpGlobalIdx = 0;
        final List<LogSection> sections = new ArrayList<>();
        LogSection currentSec = new LogSection("booting", "Booting", "neutral", lineStore);

        int prevTestNum = 0;
        boolean inPostflight = false;

        final List<PathEntry> testPaths = new ArrayList<>();
        final List<ScenEntry> scenStats = new ArrayList<>();
        String pathsPossibleStr = "";
        String pathsChanceStr = "";
        String pathsExecutedStr = "";
        final List<String> commonPathSteps = new ArrayList<>();
        String plaqueStatus = null;
        String plaqueMessage = null;

        String line;
        while (null != (line = reader.readLine())) {
            final String clean = ANSI_PATTERN.matcher(line).replaceAll("").trim();
            if (clean.isEmpty()) {
                currentSec.lines.add(line);
                continue;
            }

            final Matcher mPath = PATH_LINE_RE.matcher(clean);
            if (mPath.find()) {
                final String tNum = mPath.group(1);
                final String tDur = mPath.group(2).replace(",", "");
                final double durSec = tDur.matches("\\d+") ? Double.parseDouble(tDur) / 1000.0 : 0.0;
                final List<String> steps = new ArrayList<>();
                for (final String s : mPath.group(3).split("→")) {
                    final String stepTrim = s.trim();
                    if (!stepTrim.isEmpty()) steps.add(stepTrim);
                }
                testPaths.add(new PathEntry(tNum, String.format(Locale.ROOT, "%.3fs", durSec), steps));
            }

            final Matcher mScen = SCEN_LINE_RE.matcher(clean);
            if (mScen.find()) {
                final String sym = mScen.group(1);
                final String name = mScen.group(2).replaceAll("\\.+$", "").trim();
                final String weight = mScen.group(3).trim();
                final String count = mScen.group(4);
                final String events = mScen.group(5) != null ? mScen.group(5).trim() : "";
                scenStats.add(new ScenEntry(sym, name, weight, count, events));
            }

            if (clean.contains("possible with current set of Scenarios") || clean.contains("possible with current set of scenarios")) {
                pathsPossibleStr = clean.contains("]") ? clean.substring(clean.lastIndexOf(']') + 1).trim() : clean;
            }

            if (clean.contains("had a chance to be executed")) {
                pathsChanceStr = clean.contains("]") ? clean.substring(clean.lastIndexOf(']') + 1).trim() : clean;
            }

            if (clean.contains("were actually executed") || clean.contains("were executed")) {
                pathsExecutedStr = clean.contains("]") ? clean.substring(clean.lastIndexOf(']') + 1).trim() : clean;
            }

            if (clean.contains("Common path:")) {
                final String raw = clean.substring(clean.indexOf("Common path:") + "Common path:".length()).trim();
                commonPathSteps.clear();
                for (final String s : raw.split("→")) {
                    final String stepTrim = s.trim();
                    if (!stepTrim.isEmpty()) {
                        commonPathSteps.add(stepTrim);
                    }
                }
            }

            final int eqIdx = clean.indexOf("= ");
            if (eqIdx >= 0) {
                final String candidate = clean.substring(eqIdx).trim();
                if (candidate.startsWith("= [FAILURE]") || candidate.startsWith("= [ERROR]")) {
                    plaqueStatus = "failed";
                    plaqueMessage = candidate;
                } else if (candidate.startsWith("= Execution passed successfully")) {
                    plaqueStatus = "passed";
                    plaqueMessage = candidate;
                } else if (candidate.startsWith("= No test results")) {
                    plaqueStatus = "neutral";
                    plaqueMessage = candidate;
                }
            }

            if (clean.contains("# Applied test configuration") && "booting".equals(currentSec.type)) {
                if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                currentSec = new LogSection("configuring", "Configuring", "neutral", lineStore);
            }

            if (!inPostflight && (clean.contains("Closing test service") || clean.contains("Event summary") || clean.contains("Events summary") || clean.contains("Postflight") || clean.contains("Test session completed"))) {
                inPostflight = true;
                if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                currentSec = new LogSection("postflight", "Postflight", "neutral", lineStore);
            }

            final Matcher mTest = LINE_WITH_TEST_RE.matcher(clean);
            final Matcher mNoTest = mTest.matches() ? null : LINE_NO_TEST_RE.matcher(clean);

            if (inPostflight) {
                if (mNoTest != null && mNoTest.matches()) {
                    final int opNum = Integer.parseInt(mNoTest.group(1));
                    final String ts = mNoTest.group(2);
                    if (currentSec.startOp == null) {
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                }
            } else {
                if (mTest.matches()) {
                    final int tNum = Integer.parseInt(mTest.group(1));
                    final int opNum = Integer.parseInt(mTest.group(2));
                    final String ts = mTest.group(3);

                    if (!"test".equals(currentSec.type) || currentSec.testNum != tNum) {
                        if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                        currentSec = new LogSection("test", s("Test #", tNum), "passed", lineStore);
                        currentSec.testNum = tNum;
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                        prevTestNum = tNum;
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                } else if (mNoTest != null && mNoTest.matches()) {
                    final int opNum = Integer.parseInt(mNoTest.group(1));
                    final String ts = mNoTest.group(2);

                    if ("booting".equals(currentSec.type) || "configuring".equals(currentSec.type)) {
                        if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                        currentSec = new LogSection("preflight", "Preflight", "neutral", lineStore);
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    } else if (prevTestNum > 0 && !"limbo".equals(currentSec.type)) {
                        if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                        currentSec = new LogSection("limbo", "Limbo", "neutral", lineStore);
                        currentSec.prevTestNum = prevTestNum;
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    } else {
                        if (currentSec.startOp == null) {
                            currentSec.startOp = opNum;
                            currentSec.startTime = ts;
                        }
                    }
                    currentSec.endOp = opNum;
                    currentSec.endTime = ts;
                }
            }

            if (clean.contains("[WARNING]") && !"failed".equals(currentSec.status)) {
                currentSec.status = "warning";
            } else if (clean.contains("[SEVERE]") || clean.contains("[FAILURE]")) {
                currentSec.status = "failed";
            }

            if ("test".equals(currentSec.type)) {
                final Matcher mScenToken = Pattern.compile("[▷◆◼●]\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)\\s*:").matcher(clean);
                if (mScenToken.find()) {
                    final String sc = mScenToken.group(1).trim();
                    if (!sc.isEmpty() && !"scenario".equalsIgnoreCase(sc) && !currentSec.scenarios.contains(sc)) {
                        currentSec.scenarios.add(sc);
                    }
                }
            }

            currentSec.lines.add(line);

            if (!inPostflight && TEST_RUN_SUMMARY_RE.matcher(clean).matches()) {
                inPostflight = true;
                if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                currentSec = new LogSection("postflight", "Postflight", "neutral", lineStore);
            }
        }
        if (!currentSec.lines.isEmpty()) sections.add(currentSec);

        // ---- compute durations ----

        TelemetryItem configItem = null;
        TelemetryItem preflightItem = null;
        TelemetryItem postflightItem = null;
        final List<TelemetryItem> testData = new ArrayList<>();
        final List<TelemetryItem> limboData = new ArrayList<>();
        final Map<Integer, TelemetryItem> limboByPrevTest = new LinkedHashMap<>();

        for (int i = 0; i < sections.size(); i++) {
            final LogSection sec = sections.get(i);
            if ("limbo".equals(sec.type)) {
                final LogSection nextSec = (i + 1 < sections.size()) ? sections.get(i + 1) : null;
                final String chartLabel;
                if (null != nextSec && "test".equals(nextSec.type)) {
                    sec.title = s("Test #", sec.prevTestNum, " ➔ #", nextSec.testNum);
                    chartLabel = s(sec.prevTestNum, "➔", nextSec.testNum);
                } else {
                    sec.title = s("Test #", sec.prevTestNum, " ➔ Teardown");
                    chartLabel = s(sec.prevTestNum, "➔End");
                }
                final double t0 = toSeconds(sec.startTime);
                final double t1 = (i + 1 < sections.size() && sections.get(i + 1).startTime != null)
                        ? toSeconds(sections.get(i + 1).startTime) : toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                final TelemetryItem item = new TelemetryItem(chartLabel, sec.title, delta, delta * 1000.0, sec.status, true, sec.prevTestNum);
                limboData.add(item);
                limboByPrevTest.put(sec.prevTestNum, item);
            } else if ("test".equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                final TelemetryItem item = new TelemetryItem(s("#", sec.testNum), sec.title, delta, delta * 1000.0, sec.status, false, sec.testNum);
                testData.add(item);
            } else if ("booting".equals(sec.type)) {
                final double bootSec = calculateBootSpentSeconds(sec.lines);
                sec.durationSec = bootSec;
                sec.durationStr = formatDeltaTime(bootSec);
            } else if ("configuring".equals(sec.type)) {
                final double confSec = calculateBootSpentSeconds(sec.lines);
                sec.durationSec = confSec;
                sec.durationStr = formatDeltaTime(confSec);
                configItem = new TelemetryItem("Config", "Configuring", confSec, confSec * 1000.0, sec.status, true, 0);
            } else if ("preflight".equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                preflightItem = new TelemetryItem("Preflight", "Preflight", delta, delta * 1000.0, sec.status, true, 0);
            } else if ("postflight".equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                postflightItem = new TelemetryItem("Postflight", "Postflight", delta, delta * 1000.0, sec.status, true, 0);
            } else {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.0, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
            }
        }

        double tMin = testData.isEmpty() ? 0 : 999999, tMax = 0, tTotal = 0;
        for (final TelemetryItem item : testData) {
            tMin = Math.min(tMin, item.sec);
            tMax = Math.max(tMax, item.sec);
            tTotal += item.sec;
        }
        if (testData.isEmpty()) tMin = 0;
        final double tAvg = testData.isEmpty() ? 0 : tTotal / testData.size();

        double lMin = limboData.isEmpty() ? 0 : 999999, lMax = 0, lTotal = 0;
        for (final TelemetryItem item : limboData) {
            lMin = Math.min(lMin, item.ms);
            lMax = Math.max(lMax, item.ms);
            lTotal += item.ms;
        }
        if (limboData.isEmpty()) lMin = 0;
        final double lAvg = limboData.isEmpty() ? 0 : lTotal / limboData.size();

        double maxSec = tMax;
        if (null != configItem && configItem.sec > maxSec) maxSec = configItem.sec;
        if (null != preflightItem && preflightItem.sec > maxSec) maxSec = preflightItem.sec;
        if (null != postflightItem && postflightItem.sec > maxSec) maxSec = postflightItem.sec;
        if (maxSec <= 0.0) maxSec = 1.0;

        // ---- derive file-name links ----

        final String runLogName = (null != logFile) ? logFile.getName() : "";
        final String basePrefix = runLogName.replace(".run.log", "");
        final File gifFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".progression.gif")) : null;
        final String progressionLink = (scenarioProgressionEnabled && null != gifFile && gifFile.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(gifFile.getName()), "\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (10 fps)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression</button></a>")
                : "";
        final File webpFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".progression.webp")) : null;
        final String progressionWebpLink = (scenarioProgressionEnabled && null != webpFile && webpFile.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(webpFile.getName()), "\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (WebP)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression WebP</button></a>")
                : "";
        final File mp4File = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".progression.mp4")) : null;
        final String progressionMp4Link = (scenarioProgressionEnabled && scenarioMp4Enabled && null != mp4File && mp4File.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(mp4File.getName()), "\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (MP4)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression MP4</button></a>")
                : "";

        final File treeGifFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".tree.progression.gif")) : null;
        final String treeProgressionLink = (treeProgressionEnabled && null != treeGifFile && treeGifFile.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(treeGifFile.getName()), "\" target=\"_blank\" title=\"Scenario Tree Progression Animation (10 fps)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree Progression</button></a>")
                : "";
        final File treeWebpFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".tree.progression.webp")) : null;
        final String treeProgressionWebpLink = (treeProgressionEnabled && null != treeWebpFile && treeWebpFile.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(treeWebpFile.getName()), "\" target=\"_blank\" title=\"Scenario Tree Progression Animation (WebP)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree WebP</button></a>")
                : "";
        final File treeMp4File = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), s(basePrefix, ".tree.progression.mp4")) : null;
        final String treeProgressionMp4Link = (treeProgressionEnabled && treeMp4Enabled && null != treeMp4File && treeMp4File.exists())
                ? s("<a class=\"btn-link\" href=\"", escapeHtml(treeMp4File.getName()), "\" target=\"_blank\" title=\"Scenario Tree Progression Animation (MP4)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree MP4</button></a>")
                : "";

        String timeStr = "";
        try {
            final long epoch = Long.parseLong(basePrefix);
            final java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
            timeStr = dtf.format(java.time.Instant.ofEpochMilli(epoch));
        } catch (final Exception ignored) {}

        // ---- render dynamic fragments ----

        final String timestampBlock = timeStr.isEmpty() ? "" :
                s("    <div class=\"header-timestamp\">🕒 ", timeStr, "<span class=\"ts-val\">(", basePrefix, ")</span></div>");

        if (null == plaqueStatus) {
            boolean hasFailed = false;
            boolean hasTest = false;
            for (final LogSection sec : sections) {
                if ("failed".equals(sec.status)) {
                    hasFailed = true;
                    break;
                }
                if ("test".equals(sec.type)) {
                    hasTest = true;
                }
            }
            if (hasFailed) {
                plaqueStatus = "failed";
                plaqueMessage = "= [FAILURE] Execution failed =";
            } else if (hasTest) {
                plaqueStatus = "passed";
                plaqueMessage = "= Execution passed successfully =";
            } else {
                plaqueStatus = "neutral";
                plaqueMessage = "= No test results =";
            }
        }
        final String resultPlaque = renderResultPlaque(plaqueStatus, plaqueMessage);

        final String statsStrip = renderStatsStrip(testData, limboData, tMin, tAvg, tMax, tTotal, lMin, lAvg, lMax, lTotal);
        final String timelineBars = renderTimeline(configItem, preflightItem, testData, limboByPrevTest, postflightItem, tMax, lMax, maxSec);
        final boolean isLineDefault = testData.size() > 50;
        final String lineHidden = isLineDefault ? "" : " hidden";
        final String barsHidden = isLineDefault ? " hidden" : "";
        final String timelineToggle = testData.isEmpty() ? "" : renderTimelineToggle(isLineDefault);
        final String timelineLineGraph = renderLineGraph(testData, limboByPrevTest, tAvg, tMax, lMax);
        final boolean addAllLeaf = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_ALL_LEAF.get());
        final boolean addEtaDebug = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_ETA_DEBUG.get());
        final boolean addCpuDebug = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_CPU_DEBUG.get());

        final List<LeafCompletion> rawCompletions = addAllLeaf ? SidecarLoader.loadLeafCompletions(logFile) : List.of();
        final List<EtaDebugItem> rawEtaDebugItems = addEtaDebug ? SidecarLoader.loadEtaDebugItems(logFile) : List.of();
        
        final List<LeafCompletion> completions = new ArrayList<>();
        final List<EtaDebugItem> etaDebugItems = new ArrayList<>();
        if (!testData.isEmpty()) {
            final java.util.Set<Integer> validTestNums = new java.util.LinkedHashSet<>();
            for (final TelemetryItem item : testData) {
                validTestNums.add(item.testNum);
            }
            final java.util.Map<Integer, LeafCompletion> compMap = new java.util.LinkedHashMap<>();
            for (final LeafCompletion c : rawCompletions) {
                if (validTestNums.contains(c.testNum)) {
                    compMap.put(c.testNum, c);
                }
            }
            completions.addAll(compMap.values());

            final java.util.Map<Integer, EtaDebugItem> etaMap = new java.util.LinkedHashMap<>();
            for (final EtaDebugItem e : rawEtaDebugItems) {
                if (validTestNums.contains(e.testNum)) {
                    etaMap.put(e.testNum, e);
                }
            }
            etaDebugItems.addAll(etaMap.values());
        } else {
            completions.addAll(rawCompletions);
            etaDebugItems.addAll(rawEtaDebugItems);
        }
        
        int minTestOp = Integer.MAX_VALUE;
        int maxTestOp = Integer.MIN_VALUE;
        for (final LogSection sec : sections) {
            if ("test".equals(sec.type) || "limbo".equals(sec.type)) {
                if (sec.startOp != null && sec.startOp < minTestOp) minTestOp = sec.startOp;
                if (sec.endOp != null && sec.endOp > maxTestOp) maxTestOp = sec.endOp;
            }
        }

        final List<SystemResourceItem> resourceItems = addCpuDebug
                ? SidecarLoader.loadTelemetryResources(logFile, testData.size(), minTestOp, maxTestOp)
                : List.of();
        final List<Double> telemetryCpuList = addCpuDebug ? telemetryCpu : List.of();

        final String lgScriptData = renderLgScriptData(testData, limboByPrevTest, completions, etaDebugItems, resourceItems, tAvg, tMax, lMax);
        final String commonPath = renderCommonPath(commonPathSteps);
        final String pathsRows = renderPathsRows(testPaths);
        final String scenSubtitle = buildScenSubtitle(pathsPossibleStr, pathsChanceStr, pathsExecutedStr);
        final String scenRows = renderScenRows(scenStats);
        final String sectionsHtml = renderSections(sections, runArtifacts, telemetryCpuList, currOpGlobalIdx);
        final String leafCompletionGraph = addAllLeaf ? ChartsRenderer.renderLeafCompletionGraph(completions) : "";
        final String etaDebugGraph = addEtaDebug ? ChartsRenderer.renderEtaDebugGraph(etaDebugItems) : "";
        final String systemResourcesGraph = addCpuDebug ? ChartsRenderer.renderResourceStatsGraph(resourceItems) : "";

        // ---- populate template ----

        final Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("{{TIMESTAMP_BLOCK}}", timestampBlock);
        replacements.put("{{RESULT_PLAQUE}}", resultPlaque);
        replacements.put("{{PROGRESSION_LINK}}", progressionLink);
        replacements.put("{{PROGRESSION_WEBP_LINK}}", progressionWebpLink);
        replacements.put("{{PROGRESSION_MP4_LINK}}", progressionMp4Link);
        replacements.put("{{TREE_PROGRESSION_LINK}}", treeProgressionLink);
        replacements.put("{{TREE_PROGRESSION_WEBP_LINK}}", treeProgressionWebpLink);
        replacements.put("{{TREE_PROGRESSION_MP4_LINK}}", treeProgressionMp4Link);
        replacements.put("{{TREE_LINK}}", escapeHtml(s(basePrefix, ".tree.html")));
        replacements.put("{{JSON_LINK}}", escapeHtml(s(basePrefix, ".run.json")));
        replacements.put("{{RAW_LINK}}", escapeHtml(s(basePrefix, ".run.log")));
        replacements.put("{{TELEM_LINK}}", escapeHtml(s(basePrefix, ".telemetry.log")));
        replacements.put("{{TIMELINE_TOGGLE}}", timelineToggle);
        replacements.put("{{STATS_STRIP}}", statsStrip);
        replacements.put("{{LINE_HIDDEN}}", lineHidden);
        replacements.put("{{BARS_HIDDEN}}", barsHidden);
        replacements.put("{{TIMELINE_LINE_GRAPH}}", timelineLineGraph);
        replacements.put("{{TIMELINE_BARS}}", timelineBars);
        replacements.put("{{LG_SCRIPT_DATA}}", lgScriptData);
        replacements.put("{{PATHS_COUNT}}", String.valueOf(testPaths.size()));
        replacements.put("{{COMMON_PATH}}", commonPath);
        replacements.put("{{PATHS_ROWS}}", pathsRows);
        replacements.put("{{SCEN_SUBTITLE}}", scenSubtitle);
        replacements.put("{{SCEN_ROWS}}", scenRows);
        replacements.put("{{SECTIONS}}", sectionsHtml);
        replacements.put("{{LEAF_COMPLETION_GRAPH}}", leafCompletionGraph);
        replacements.put("{{ETA_DEBUG_GRAPH}}", etaDebugGraph);
        replacements.put("{{SYSTEM_RESOURCES_GRAPH}}", systemResourcesGraph);
        return TemplateEngine.replaceTemplate(TemplateEngine.loadTemplate(), replacements);
    }

    /**
     * Generates an HTML report from a run log file and writes it to disk.
     *
     * @param logFile                   input raw run log file
     * @param outFile                   target output HTML report file
     * @param scenarioProgressionEnabled whether scenario progression media links should be included
     * @param treeProgressionEnabled     whether scenario tree progression media links should be included
     * @param scenarioMp4Enabled         whether scenario MP4 video links should be included
     * @param treeMp4Enabled             tree MP4 video flag
     */
    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled) {
        try {
            if (!logFile.exists()) return;
            final String html;
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8));
                 final LineStore lineStore = new LineStore()) {
                html = buildHtml(
                        reader,
                        logFile,
                        scenarioProgressionEnabled,
                        treeProgressionEnabled,
                        scenarioMp4Enabled,
                        treeMp4Enabled,
                        lineStore);
            }
            Files.writeToFile(html, outFile.getAbsolutePath());
        } catch (final Exception e) {
            logEvent(S2, "Failed to generate log.html: " + e.getMessage());
        }
    }
}
