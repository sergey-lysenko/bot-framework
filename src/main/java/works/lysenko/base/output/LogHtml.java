package works.lysenko.base.output;

import works.lysenko.base.core.Routines;
import works.lysenko.util.func.type.Files;
import works.lysenko.util.spec.Layout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.core;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_HTML_;

@SuppressWarnings({"UtilityClass", "MethodWithMultipleLoops", "NestedMethodCall", "ClassWithoutLogger", "OverlyLongMethod"})
public final class LogHtml {

    private static final String TEMPLATE_RESOURCE = "works/lysenko/base/output/log-report-template.html";

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\s*\\]\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^(?:\\[\\s*\\])?\\[\\s*(\\d+)\\s*\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern PATH_LINE_RE = Pattern.compile("\\[\\s*(\\d+)\\s*\\]\\s*\\[\\s*([\\d,]+)\\s*\\]\\s*→\\s*(.*)");
    private static final Pattern SCEN_LINE_RE = Pattern.compile("([▷◼◆●])\\s*([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)[.\\s]+(\\[[^\\]]+\\]|\\([^)]+\\)\\s*→\\s*\\[[^\\]]+\\])\\s*(\\d+(?::\\d+)?)(?:\\s*\\(([^)]+)\\))?");
    private static final Pattern TEST_TIME_RE = Pattern.compile("Test time (.*)");
    private static final Pattern TEST_RUN_SUMMARY_RE = Pattern.compile(".*\\b\\d+\\s+tests?\\s+of\\s+.+\\s+done\\s+in\\s+.*");
    private static final Pattern SPAN_BRACKET_RE = Pattern.compile("(\\[[0-9.]+\\](?:<[^>]+>)*)(\\[\\s*\\d+\\s*\\])");
    private static final Pattern ALL_LEAF_COMPLETION_RE = Pattern.compile("\\[ALL_LEAF_COMPLETION]\\s+(\\d+)(?:\\s+(\\d+))?\\s+(.+)$");

    private LogHtml() {
    }

    // -------------------------------------------------------------------------
    // Template loading
    // -------------------------------------------------------------------------

    private static String loadTemplate() {
        try (final InputStream is = LogHtml.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (is == null) {
                logEvent(S2, "log-report-template.html not found on classpath: " + TEMPLATE_RESOURCE);
                return "";
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                final StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (final IOException e) {
            logEvent(S2, "Failed to load log-report-template.html: " + e.getMessage());
            return "";
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void logStats() {
        try {
            if (null != core && null != core.getLogger() && null != core.getLogger().getLogWriter()) {
                if (core.getLogger().getLogWriter() instanceof java.io.Flushable) {
                    ((java.io.Flushable) core.getLogger().getLogWriter()).flush();
                }
            }
        } catch (final Exception e) {
            logEvent(S2, "Failed to flush run log before report generation: " + e.getMessage());
        }
    }

    public static void generateReport(final File logFile, final File outFile) {
        generateReport(
                logFile,
                outFile,
                ProgressionSettings.current().scenarioEnabled(),
                ProgressionSettings.current().treeEnabled(),
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled());
    }

    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled) {
        generateReport(
                logFile,
                outFile,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                ProgressionSettings.current().scenarioMp4Enabled(),
                ProgressionSettings.current().treeMp4Enabled());
    }

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

    // -------------------------------------------------------------------------
    // HTML assembly — replaces template placeholders with rendered fragments
    // -------------------------------------------------------------------------

    private static String buildHtml(
            final BufferedReader reader,
            final File logFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final LineStore lineStore) throws IOException {
        final List<ArtifactItem> runArtifacts = loadRunArtifacts(logFile);
        final List<Double> telemetryCpu = loadTelemetryCpu(logFile);
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
                        currentSec = new LogSection("test", "Test #" + tNum, "passed", lineStore);
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
                    sec.title = "Test #" + sec.prevTestNum + " ➔ #" + nextSec.testNum;
                    chartLabel = sec.prevTestNum + "➔" + nextSec.testNum;
                } else {
                    sec.title = "Test #" + sec.prevTestNum + " ➔ Teardown";
                    chartLabel = sec.prevTestNum + "➔End";
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
                final TelemetryItem item = new TelemetryItem("#" + sec.testNum, sec.title, delta, delta * 1000.0, sec.status, false, sec.testNum);
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
        final File gifFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".progression.gif") : null;
        final String progressionLink = (scenarioProgressionEnabled && null != gifFile && gifFile.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (10 fps)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression</button></a>", escapeHtml(gifFile.getName()))
                : "";
        final File webpFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".progression.webp") : null;
        final String progressionWebpLink = (scenarioProgressionEnabled && null != webpFile && webpFile.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (WebP)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression WebP</button></a>", escapeHtml(webpFile.getName()))
                : "";
        final File mp4File = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".progression.mp4") : null;
        final String progressionMp4Link = (scenarioProgressionEnabled && scenarioMp4Enabled && null != mp4File && mp4File.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Coverage Progression Animation (MP4)\"><button type=\"button\" style=\"border-color: #38bdf8; color: #38bdf8;\">Progression MP4</button></a>", escapeHtml(mp4File.getName()))
                : "";

        final File treeGifFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".tree.progression.gif") : null;
        final String treeProgressionLink = (treeProgressionEnabled && null != treeGifFile && treeGifFile.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Tree Progression Animation (10 fps)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree Progression</button></a>", escapeHtml(treeGifFile.getName()))
                : "";
        final File treeWebpFile = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".tree.progression.webp") : null;
        final String treeProgressionWebpLink = (treeProgressionEnabled && null != treeWebpFile && treeWebpFile.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Tree Progression Animation (WebP)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree WebP</button></a>", escapeHtml(treeWebpFile.getName()))
                : "";
        final File treeMp4File = (null != logFile.getParentFile()) ? new File(logFile.getParentFile(), basePrefix + ".tree.progression.mp4") : null;
        final String treeProgressionMp4Link = (treeProgressionEnabled && treeMp4Enabled && null != treeMp4File && treeMp4File.exists())
                ? String.format("<a class=\"btn-link\" href=\"%s\" target=\"_blank\" title=\"Scenario Tree Progression Animation (MP4)\"><button type=\"button\" style=\"border-color: #34d399; color: #34d399;\">Tree MP4</button></a>", escapeHtml(treeMp4File.getName()))
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
                "    <div class=\"header-timestamp\">🕒 " + timeStr
                        + "<span class=\"ts-val\">(" + basePrefix + ")</span></div>";

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
        final List<LeafCompletion> completions = loadLeafCompletions(logFile);
        final String lgScriptData = renderLgScriptData(testData, limboByPrevTest, completions, tAvg, tMax, lMax);
        final String commonPath = renderCommonPath(commonPathSteps);
        final String pathsRows = renderPathsRows(testPaths);
        final String scenSubtitle = buildScenSubtitle(pathsPossibleStr, pathsChanceStr, pathsExecutedStr);
        final String scenRows = renderScenRows(scenStats);
        final String sectionsHtml = renderSections(sections, runArtifacts, telemetryCpu, currOpGlobalIdx);
        final String leafCompletionGraph = renderLeafCompletionGraph(completions);

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
        replacements.put("{{TREE_LINK}}", escapeHtml(basePrefix + ".tree.html"));
        replacements.put("{{JSON_LINK}}", escapeHtml(basePrefix + ".run.json"));
        replacements.put("{{RAW_LINK}}", escapeHtml(basePrefix + ".run.log"));
        replacements.put("{{TELEM_LINK}}", escapeHtml(basePrefix + ".telemetry.log"));
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
        return replaceTemplate(loadTemplate(), replacements);
    }

    private static String replaceTemplate(final String template, final Map<String, String> replacements) {
        final long capacity = (long) template.length() + replacements.values().stream()
                .mapToLong(String::length).sum();
        final StringBuilder output = new StringBuilder((int) Math.min(Integer.MAX_VALUE - 8L, capacity));
        int cursor = 0;
        while (cursor < template.length()) {
            final int placeholderStart = template.indexOf("{{", cursor);
            if (placeholderStart < 0) break;
            final int placeholderEnd = template.indexOf("}}", placeholderStart + 2);
            if (placeholderEnd < 0) break;
            output.append(template, cursor, placeholderStart);
            final String placeholder = template.substring(placeholderStart, placeholderEnd + 2);
            final String replacement = replacements.get(placeholder);
            if (null == replacement) {
                output.append(placeholder);
            } else {
                output.append(replacement);
            }
            cursor = placeholderEnd + 2;
        }
        output.append(template, cursor, template.length());
        return output.toString();
    }

    // -------------------------------------------------------------------------
    // Fragment renderers
    // -------------------------------------------------------------------------

    private static String renderResultPlaque(final String status, final String message) {
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

    private static String renderStatsStrip(
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

    private static String renderCommonPath(final List<String> commonPathSteps) {
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

    private static String renderPathsRows(final List<PathEntry> testPaths) {
        final StringBuilder sb = new StringBuilder();
        for (final PathEntry p : testPaths) {
            sb.append(s("<div class=\"path-row\" onclick=\"focusSection('Test #", p.num, "')\">",
                    "<div class=\"path-meta\">",
                    "<span class=\"path-num\">Test #", p.num, "</span>",
                    "<span class=\"path-dur\">⏱ ", p.durStr, "</span>",
                    "</div><div class=\"path-chain\">"));
            for (int sIdx = 0; sIdx < p.steps.size(); sIdx++) {
                if (sIdx > 0) sb.append(" <span class=\"step-arrow\">→</span> ");
                sb.append(s("<span class=\"path-step\">", escapeHtml(p.steps.get(sIdx)), "</span>"));
            }
            sb.append("</div></div>");
        }
        return sb.toString();
    }

    private static String buildScenSubtitle(final String pathsPossibleStr, final String pathsChanceStr, final String pathsExecutedStr) {
        final List<String> parts = new ArrayList<>(3);
        if (!pathsPossibleStr.isEmpty()) parts.add(escapeHtml(pathsPossibleStr));
        if (!pathsChanceStr.isEmpty()) parts.add(escapeHtml(pathsChanceStr));
        if (!pathsExecutedStr.isEmpty()) parts.add(escapeHtml(pathsExecutedStr));
        return String.join("<br>", parts);
    }

    private static String renderScenRows(final List<ScenEntry> scenStats) {
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
                    "<td><span class=\"", symClass, "\">", sc.sym, "</span> <b>", escapeHtml(sc.name), "</b></td>",
                    "<td class=\"cell-mono text-muted\">", escapeHtml(sc.weight), "</td>",
                    "<td class=\"cell-mono text-right\"><b>", sc.count, "</b>", evtBadge, "</td>",
                    "</tr>"));
        }
        return sb.toString();
    }

    private static String renderLeafCompletionGraph(final List<LeafCompletion> completions) {

        if (completions.isEmpty()) return "";

        final double width = 1000.0;
        final double height = 240.0;
        final double marginLeft = 55.0;
        final double marginRight = 25.0;
        final double marginTop = 26.0;
        final double marginBottom = 34.0;
        final double plotW = width - marginLeft - marginRight;
        final double plotH = height - marginTop - marginBottom;

        long maxValMs = 1L;
        long previous = 0L;
        final long[] intervals = new long[completions.size()];
        final long[] etas = new long[completions.size()];

        for (int index = 0; index < completions.size(); index++) {
            final LeafCompletion completion = completions.get(index);
            final long interval = Math.max(0L, completion.atMillis - previous);
            intervals[index] = interval;
            etas[index] = completion.etaMs;
            previous = completion.atMillis;
            maxValMs = Math.max(maxValMs, Math.max(interval, completion.etaMs));
        }

        final double maxValSec = maxValMs / 1000.0;
        final double ceilSec = getNiceCeil(maxValSec > 0 ? maxValSec : 1.0);

        final StringBuilder sb = new StringBuilder();
        sb.append("<section id=\"leafCompletionChart\" class=\"leaf-completion-chart\"><div class=\"chart-card\">")
                .append("<div class=\"card-title\"><span>All-Leaf Completion Intervals</span>")
                .append("<span class=\"card-subtitle\">Y: Time since previous completion (first: run start); X: completion order</span>")
                .append("</div><div class=\"line-graph-legend\">")
                .append("<span class=\"lg-legend-item\"><span class=\"lg-line-sample leaf-line\"></span>")
                .append("Time between completions</span>")
                .append("<span class=\"lg-legend-item\"><span class=\"lg-line-sample leaf-eta-line\"></span>")
                .append("ETA</span></div>")
                .append("<div class=\"line-graph-svg-wrap\"><svg id=\"leafCompletionSvg\" class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">")
                .append("  <defs>\n")
                .append("    <linearGradient id=\"leafAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n")
                .append("      <stop offset=\"0%\" stop-color=\"#a855f7\" stop-opacity=\"0.35\"/>\n")
                .append("      <stop offset=\"100%\" stop-color=\"#a855f7\" stop-opacity=\"0.02\"/>\n")
                .append("    </linearGradient>\n")
                .append("  </defs>\n");

        for (int k = 0; k <= 4; k++) {
            final double ratio = (double) k / 4.0;
            final double y = (marginTop + plotH) - ratio * plotH;
            final double valSec = ratio * ceilSec;
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.06)\" stroke-dasharray=\"3,3\" />\n",
                    marginLeft, y, marginLeft + plotW, y));
            final String labelStr;
            if (ceilSec < 1.0) {
                labelStr = Math.round(valSec * 1000.0) + "ms";
            } else {
                labelStr = String.format(Locale.ROOT, "%.1fs", valSec);
            }
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">%s</text>\n",
                    marginLeft - 8, y + 3.5, labelStr));
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
            sb.append(String.format(Locale.ROOT,
                    "  <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"rgba(255,255,255,0.15)\" />\n",
                    x, marginTop + plotH, x, marginTop + plotH + 4));
            sb.append(String.format(Locale.ROOT,
                    "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" fill=\"#64748b\" font-size=\"10\" font-family=\"ui-monospace, monospace\">#%d</text>\n",
                    x, height - 10, idx + 1));
        }

        final StringBuilder leafPath = new StringBuilder();
        final StringBuilder leafArea = new StringBuilder();
        final StringBuilder etaPath = new StringBuilder();

        for (int i = 0; i < N; i++) {
            final double intervalSec = intervals[i] / 1000.0;
            final double etaSec = etas[i] / 1000.0;
            final double x = marginLeft + (N > 1 ? (double) i / (N - 1) * plotW : plotW / 2.0);
            final double yInterval = (marginTop + plotH) - Math.min(plotH, (intervalSec / ceilSec) * plotH);
            final double yEta = (marginTop + plotH) - Math.min(plotH, (etaSec / ceilSec) * plotH);

            if (i == 0) {
                leafPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yInterval));
                leafArea.append(String.format(Locale.ROOT, "M %.2f %.2f L %.2f %.2f", x, marginTop + plotH, x, yInterval));
                etaPath.append(String.format(Locale.ROOT, "M %.2f %.2f", x, yEta));
            } else {
                leafPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yInterval));
                leafArea.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yInterval));
                etaPath.append(String.format(Locale.ROOT, " L %.2f %.2f", x, yEta));
            }
            if (i == N - 1) {
                leafArea.append(String.format(Locale.ROOT, " L %.2f %.2f Z", x, marginTop + plotH));
            }
        }

        sb.append("  <path d=\"").append(leafArea).append("\" fill=\"url(#leafAreaGrad)\" />\n");
        sb.append("  <path d=\"").append(leafPath).append("\" class=\"leaf-chart-line\" fill=\"none\" stroke=\"#a855f7\" stroke-width=\"2\" />\n");
        sb.append("  <path d=\"").append(etaPath).append("\" class=\"leaf-eta-chart-line\" fill=\"none\" stroke=\"#38bdf8\" stroke-width=\"2\" stroke-dasharray=\"4,3\" />\n");

        sb.append(String.format(Locale.ROOT,
                "  <line id=\"leafCrosshair\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-dasharray=\"2,2\" stroke-width=\"1\" opacity=\"0\" pointer-events=\"none\" />\n",
                marginTop, marginTop + plotH));
        sb.append("  <circle id=\"leafDot\" cx=\"0\" cy=\"0\" r=\"5\" fill=\"#a855f7\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n");
        sb.append("  <circle id=\"leafEtaDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#38bdf8\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n");
        sb.append(String.format(Locale.ROOT,
                "  <rect id=\"leafOverlay\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"transparent\" style=\"cursor: crosshair;\" onmousemove=\"onLeafHover(event)\" onmouseleave=\"onLeafLeave()\" />\n",
                marginLeft, marginTop, plotW, plotH));
        sb.append("</svg>\n");
        sb.append("<div id=\"leafTooltip\" class=\"lg-tooltip hidden\"></div>\n");
        sb.append("</div></div></section>");

        return sb.toString();
    }

    private static List<LeafCompletion> loadLeafCompletions(final File runLogFile) {

        final File completionFile = allLeafCompletionsFile(runLogFile);
        if (!completionFile.isFile()) return List.of();

        final List<LeafCompletion> completions = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(completionFile), StandardCharsets.UTF_8))) {
            String line;
            while (null != (line = reader.readLine())) {
                final Matcher matcher = ALL_LEAF_COMPLETION_RE.matcher(line.trim());
                if (matcher.matches()) {
                    final long atMillis = Long.parseLong(matcher.group(1));
                    final long etaMs = (null != matcher.group(2) && !matcher.group(2).isEmpty())
                            ? Long.parseLong(matcher.group(2)) : 0L;
                    final String leaf = matcher.group(3).trim();
                    completions.add(new LeafCompletion(atMillis, etaMs, leaf));
                }
            }
        } catch (final IOException | NumberFormatException e) {
            logEvent(S2, "Failed to read all-leaf completion log " + completionFile + ": " + e.getMessage());
        }
        return completions;
    }

    private static File allLeafCompletionsFile(final File runLogFile) {

        final String suffix = ".run.log";
        final String name = runLogFile.getName();
        final String prefix = name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
        return new File(runLogFile.getParentFile(), prefix + ".all-leaf-completions.log");
    }

    private static int completionX(
            final int index, final int completionCount, final int left, final int plotWidth) {

        if (completionCount <= 1) return left + (plotWidth / 2);
        return left + (int) Math.round((index / (double) (completionCount - 1)) * plotWidth);
    }

    private static String formatCompletionDuration(final long milliseconds) {

        return milliseconds < 1_000L ? milliseconds + "ms"
                : String.format(Locale.ROOT, "%.2fs", milliseconds / 1_000.0);
    }

    private static String formatCompletionMoment(final long milliseconds) {

        final long minutes = milliseconds / 60_000L;
        final long seconds = (milliseconds / 1_000L) % 60L;
        return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, milliseconds % 1_000L);
    }

    private static String renderSections(
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

            sb.append(s("<details id=\"", secId, "\" class=\"log-section ", sec.type, " ", sec.status, "\">\n"));
            sb.append("  <summary>\n    <div class=\"summary-left\">\n");
            if (!opRange.isEmpty()) sb.append(s("      <span class=\"op-range\">", opRange, "</span>\n"));
            sb.append(s("      <span class=\"sec-title\">", escapeHtml(sec.title), "</span>\n"));
            sb.append(s("      <span class=\"badge ", badgeClass, "\">", badgeText, "</span>\n"));
            if (sec.durationStr != null && !sec.durationStr.isEmpty()) {
                final String durClass = isLimbo ? " limbo-duration" : "";
                sb.append(s("      <span class=\"duration", durClass, "\">", sec.durationStr, "</span>\n"));
            }
            sb.append("    </div>\n    <div class=\"summary-right\">\n");
            if (!timeRange.isEmpty()) sb.append(s("      <span class=\"time-range\">", timeRange, "</span>\n"));
            sb.append(s("      <span class=\"line-count\">", sec.lines.size(), " lines</span>\n    </div>\n  </summary>\n"));

            if ("test".equals(sec.type) && !sec.scenarios.isEmpty()) {
                sb.append("  <div class=\"section-breadcrumb\">\n    <span class=\"breadcrumb-label\">Scenario:</span>\n");
                for (final String sc : sec.scenarios) {
                    sb.append(s("    <span class=\"pill\">", escapeHtml(sc), "</span>\n"));
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
                        final java.util.regex.Matcher mTs = java.util.regex.Pattern.compile("\\[([0-9]+\\.[0-9]+)\\]").matcher(cleanRaw);
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
                    final java.util.regex.Matcher mTs = java.util.regex.Pattern.compile("\\[([0-9]+\\.[0-9]+)\\]").matcher(cleanRaw);
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
                            cpuRows.append(String.format(Locale.ROOT, s("<div class=\"st-row%s\" title=\"%.2f%%\"><div class=\"st-wave\" style=\"width: %dpx;\"></div></div>"), cClass, cpuVal, cpuW));
                        }
                    } else {
                        cpuRows.append("<div class=\"st-row rate-white\" title=\"-\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                    }
                }

                sb.append("  <div class=\"log-section-body\">\n");
                sb.append("    <div class=\"soundtrack-column\" title=\"35mm Optical Track (Operation Duration)\"><div class=\"soundtrack-track\">").append(stRows).append("</div></div>\n");
                sb.append("    <div class=\"log-content\">");
                appendSectionLines(sb, sec, causativeArtifacts, secArtifacts);
                sb.append("</div>\n");
                sb.append("    <div class=\"soundtrack-column right-track\" title=\"35mm Optical Track (CPU Load)\"><div class=\"soundtrack-track\">").append(cpuRows).append("</div></div>\n");
                sb.append("  </div>\n</details>\n");
            } else {
                sb.append("  <div class=\"log-content\">");
                appendSectionLines(sb, sec, causativeArtifacts, secArtifacts);
                sb.append("</div>\n</details>\n");
            }
        }
        return sb.toString();
    }

    private static void appendSectionLines(
            final StringBuilder output,
            final LogSection section,
            final Map<Integer, List<ArtifactItem>> causativeArtifacts,
            final Map<Integer, List<ArtifactItem>> sectionArtifacts) {
        for (int lineIndex = 0; lineIndex < section.lines.size(); lineIndex++) {
            String parsedLine = parseAnsi(section.lines.get(lineIndex));
            final StringBuilder badgeHtml = new StringBuilder();

            final List<ArtifactItem> causative = causativeArtifacts.get(lineIndex);
            if (null != causative && !causative.isEmpty()) {
                final ArtifactItem firstArtifact = causative.get(0);
                final String tip = "Artifact created during this operation: " + firstArtifact.rel();
                final java.util.regex.Matcher spanMatcher = SPAN_BRACKET_RE.matcher(parsedLine);
                if (spanMatcher.find()) {
                    final String prefix = spanMatcher.group(1);
                    final String bracket = spanMatcher.group(2);
                    final String linkedBracket = prefix + "<a class=\"log-artifact-link\" href=\""
                            + firstArtifact.rel() + "\" target=\"_blank\" title=\"" + tip + "\">"
                            + bracket + "</a>";
                    parsedLine = spanMatcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(linkedBracket));
                }
            }

            final List<ArtifactItem> matched = sectionArtifacts.get(lineIndex);
            if (null != matched) {
                for (final ArtifactItem artifact : matched) {
                    if (".properties".equals(artifact.ext()) && parsedLine.contains("snapshot of test data")
                            && !parsedLine.contains("log-artifact-link")) {
                        parsedLine = parsedLine.replaceAll(
                                "(Made\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+test\\s+data)",
                                "<a class=\"log-artifact-link\" href=\"" + artifact.rel()
                                        + "\" target=\"_blank\" title=\"Open artifact: " + artifact.rel() + "\">$1</a>");
                    } else if (".xml".equals(artifact.ext()) && parsedLine.contains("snapshot of page code")
                            && !parsedLine.contains("log-artifact-link")) {
                        parsedLine = parsedLine.replaceAll(
                                "(Making\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+page\\s+code)",
                                "<a class=\"log-artifact-link\" href=\"" + artifact.rel()
                                        + "\" target=\"_blank\" title=\"Open artifact: " + artifact.rel() + "\">$1</a>");
                    }
                    if (!parsedLine.contains(artifact.rel())) {
                        final String icon = ".properties".equals(artifact.ext()) ? "💾"
                                : ".xml".equals(artifact.ext()) ? "📄" : "📸";
                        final String type = ".properties".equals(artifact.ext()) ? "badge-data"
                                : ".xml".equals(artifact.ext()) ? "badge-xml" : "badge-img";
                        badgeHtml.append("<a class=\"gutter-artifact-badge ").append(type)
                                .append("\" href=\"").append(artifact.rel())
                                .append("\" target=\"_blank\" title=\"Open ")
                                .append(artifact.ext().isEmpty() ? "" : artifact.ext().substring(1))
                                .append(": ").append(artifact.rel()).append("\">").append(icon).append("</a>");
                    }
                }
            }

            if (badgeHtml.length() > 0) parsedLine = parsedLine + "  " + badgeHtml;
            output.append("<div class=\"log-line-entry\">").append(parsedLine).append("</div>");
        }
    }

    // -------------------------------------------------------------------------
    // Timeline bar chart
    // -------------------------------------------------------------------------

    private static String renderTimeline(
            final TelemetryItem configItem, final TelemetryItem preflightItem,
            final List<TelemetryItem> testData, final Map<Integer, TelemetryItem> limboByPrevTest,
            final TelemetryItem postflightItem, final double tMax, final double lMax, final double maxSec) {
        final StringBuilder sb = new StringBuilder();

        sb.append(renderPhaseBar(configItem, "config", maxSec));
        sb.append(renderPhaseBar(preflightItem, "preflight", maxSec));

        for (final TelemetryItem test : testData) {
            sb.append("<div class=\"bar-pair\">\n");

            final int tPct = tMax > 0 ? Math.max(6, (int) Math.round((test.sec / tMax) * 100)) : 6;
            final String tValStr = String.format(Locale.ROOT, "%.2fs", test.sec);
            final String tColor = "passed".equals(test.status) ? "#22c55e" : "warning".equals(test.status) ? "#f59e0b" : "#ef4444";
            sb.append("  <div class=\"bar-col test\" onclick=\"focusSection('").append(test.title).append("')\" title=\"").append(test.title).append(": ").append(tValStr).append("\">");
            sb.append("<div class=\"bar-val\">").append(tValStr).append("</div>");
            sb.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ").append(tPct).append("%; background: ").append(tColor).append(";\"></div></div>");
            sb.append("<div class=\"bar-lbl\">").append(test.label).append("</div></div>\n");

            final TelemetryItem limbo = limboByPrevTest.get(test.testNum);
            if (null != limbo) {
                final int lPct = lMax > 0 ? Math.max(6, (int) Math.round((limbo.ms / lMax) * 100)) : 6;
                final String lValStr = Math.round(limbo.ms) + "ms";
                final String lColor = ("passed".equals(limbo.status) || "neutral".equals(limbo.status)) ? "#64748b" : ("warning".equals(limbo.status) ? "#f59e0b" : "#ef4444");
                sb.append("  <div class=\"bar-col limbo\" onclick=\"focusSection('").append(limbo.title).append("')\" title=\"").append(limbo.title).append(": ").append(lValStr).append("\">");
                sb.append("<div class=\"bar-val\">").append(lValStr).append("</div>");
                sb.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ").append(lPct).append("%; background: ").append(lColor).append(";\"></div></div>");
                sb.append("<div class=\"bar-lbl\">").append(limbo.label).append("</div></div>\n");
            }

            sb.append("</div>\n");
        }

        sb.append(renderPhaseBar(postflightItem, "postflight", maxSec));

        return sb.toString();
    }

    private static String renderPhaseBar(final TelemetryItem item, final String extraClass, final double maxSec) {
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
        final StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"bar-col phase ").append(extraClass)
                .append("\" onclick=\"focusSection('").append(item.title).append("')\" title=\"")
                .append(item.title).append(": ").append(valStr).append("\">");
        sb.append("<div class=\"bar-val\">").append(valStr).append("</div>");
        sb.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ").append(pct).append("%; background: ").append(color).append(";\"></div></div>");
        sb.append("<div class=\"bar-lbl\">").append(item.label).append("</div></div>\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // ANSI → HTML colour conversion
    // -------------------------------------------------------------------------

    private static String parseAnsi(final String text) {
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
                }
            }
            if (styles.length() > 0) {
                sb.append("<span style=\"").append(styles).append("\">");
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

    private static double getNiceCeil(final double max) {
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

    private static String escapeJson(final String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static String renderTimelineToggle(final boolean isLineDefault) {
        final String activeLine = isLineDefault ? " active" : "";
        final String activeBars = isLineDefault ? "" : " active";
        return s(
                "<div class=\"chart-view-toggle\">\n",
                "  <button type=\"button\" id=\"btnViewLine\" class=\"chart-toggle-btn", activeLine, "\" onclick=\"switchTimelineView('line')\">Line</button>\n",
                "  <button type=\"button\" id=\"btnViewBars\" class=\"chart-toggle-btn", activeBars, "\" onclick=\"switchTimelineView('bars')\">Bars</button>\n",
                "</div>\n"
        );
    }

    private static String renderLgScriptData(
            final List<TelemetryItem> testData,
            final Map<Integer, TelemetryItem> limboByPrevTest,
            final List<LeafCompletion> completions,
            final double tAvg, final double tMax, final double lMax) {
        if (testData.isEmpty() && (completions == null || completions.isEmpty())) return "";

        final StringBuilder sb = new StringBuilder();
        if (!testData.isEmpty()) {
            final boolean hasLimbo = lMax > 0 && !limboByPrevTest.isEmpty();
            final double tCeil = getNiceCeil(tMax > 0 ? tMax : 1.0);
            final double lCeil = hasLimbo ? getNiceCeil(lMax) : 1.0;

            sb.append("  window.lgTCeil = ").append(String.format(Locale.ROOT, "%.2f", tCeil)).append(";\n");
            sb.append("  window.lgLCeil = ").append(String.format(Locale.ROOT, "%.2f", lCeil)).append(";\n");
            sb.append("  window.lgTAvg = ").append(String.format(Locale.ROOT, "%.2f", tAvg)).append(";\n");
            sb.append("  window.lgHasLimbo = ").append(hasLimbo).append(";\n");
            sb.append("  window.lgData = [");
            for (int i = 0; i < testData.size(); i++) {
                if (i > 0) sb.append(",");
                final TelemetryItem t = testData.get(i);
                final TelemetryItem l = limboByPrevTest.get(t.testNum);
                final long lMs = l != null ? Math.round(l.ms) : 0;
                sb.append("{\"n\":").append(t.testNum)
                        .append(",\"t\":\"").append(escapeJson(t.title)).append("\"")
                        .append(String.format(Locale.ROOT, ",\"s\":%.3f", t.sec))
                        .append(",\"ms\":").append(Math.round(t.ms))
                        .append(",\"l\":").append(lMs)
                        .append(",\"st\":\"").append(escapeJson(t.status)).append("\"}");
            }
            sb.append("];\n");
        }

        if (null != completions && !completions.isEmpty()) {
            long maxVal = 1L;
            long previous = 0L;
            for (final LeafCompletion c : completions) {
                final long interval = Math.max(0L, c.atMillis - previous);
                previous = c.atMillis;
                maxVal = Math.max(maxVal, Math.max(interval, c.etaMs));
            }
            final double maxValSec = maxVal / 1000.0;
            final double ceilSec = getNiceCeil(maxValSec > 0 ? maxValSec : 1.0);

            sb.append("  window.leafCeil = ").append(String.format(Locale.ROOT, "%.3f", ceilSec)).append(";\n");
            sb.append("  window.leafData = [");
            long pAt = 0L;
            for (int i = 0; i < completions.size(); i++) {
                if (i > 0) sb.append(",");
                final LeafCompletion c = completions.get(i);
                final long intervalMs = Math.max(0L, c.atMillis - pAt);
                pAt = c.atMillis;
                final String intervalStr = formatCompletionDuration(intervalMs);
                final String etaStr = formatCompletionDuration(c.etaMs);
                final String momentStr = formatCompletionMoment(c.atMillis);
                final String fullLabel = (c.etaMs > 0L)
                        ? c.leaf + " @ " + momentStr + " (+" + intervalStr + ", ETA: " + etaStr + ")"
                        : c.leaf + " @ " + momentStr + " (+" + intervalStr + ")";
                sb.append("{\"n\":").append(i + 1)
                        .append(",\"leaf\":\"").append(escapeJson(c.leaf)).append("\"")
                        .append(",\"ms\":").append(intervalMs)
                        .append(",\"etaMs\":").append(c.etaMs)
                        .append(",\"eta\":\"").append(escapeJson(etaStr)).append("\"")
                        .append(",\"at\":").append(c.atMillis)
                        .append(",\"interval\":\"").append(escapeJson(intervalStr)).append("\"")
                        .append(",\"moment\":\"").append(escapeJson(momentStr)).append("\"")
                        .append(",\"label\":\"").append(escapeJson(fullLabel)).append("\"}");
            }
            sb.append("];\n");
        }
        return sb.toString();
    }

    private static String renderLineGraph(
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
        sb.append("<div class=\"line-graph-legend\">\n");
        sb.append("  <span class=\"lg-legend-item\"><span class=\"lg-line-sample test-line\"></span> Test Time (s)</span>\n");
        if (hasLimbo) {
            sb.append("  <span class=\"lg-legend-item lg-legend-limbo\"><span class=\"lg-line-sample limbo-line\"></span> Limbo (ms)</span>\n");
        }
        if (tAvg > 0) {
            sb.append(String.format(Locale.ROOT,
                    "  <span class=\"lg-legend-item\"><span class=\"lg-dash-sample\"></span> Avg: %.2fs</span>\n", tAvg));
        }
        sb.append("  <span class=\"lg-legend-item\"><span class=\"lg-dot-sample fail\"></span> Failed</span>\n");
        sb.append("  <span class=\"lg-legend-item\"><span class=\"lg-dot-sample warn\"></span> Warning</span>\n");
        sb.append("</div>\n");

        sb.append("<div class=\"line-graph-svg-wrap\">\n");
        sb.append("<svg class=\"timeline-line-svg\" viewBox=\"0 0 1000 240\">\n");
        sb.append("  <defs>\n");
        sb.append("    <linearGradient id=\"testAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n");
        sb.append("      <stop offset=\"0%\" stop-color=\"#38bdf8\" stop-opacity=\"0.35\"/>\n");
        sb.append("      <stop offset=\"100%\" stop-color=\"#38bdf8\" stop-opacity=\"0.02\"/>\n");
        sb.append("    </linearGradient>\n");
        sb.append("    <linearGradient id=\"limboAreaGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">\n");
        sb.append("      <stop offset=\"0%\" stop-color=\"#a855f7\" stop-opacity=\"0.25\"/>\n");
        sb.append("      <stop offset=\"100%\" stop-color=\"#a855f7\" stop-opacity=\"0.02\"/>\n");
        sb.append("    </linearGradient>\n");
        sb.append("  </defs>\n");

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
            sb.append("  <g class=\"lg-limbo-series\">\n");
            sb.append("    <path d=\"").append(limboArea).append("\" fill=\"url(#limboAreaGrad)\" />\n");
            sb.append("    <path d=\"").append(limboPath).append("\" fill=\"none\" stroke=\"#a855f7\" stroke-width=\"1.5\" opacity=\"0.7\" />\n");
            sb.append("  </g>\n");
        }
        sb.append("  <path d=\"").append(testArea).append("\" fill=\"url(#testAreaGrad)\" />\n");
        sb.append("  <path d=\"").append(testPath).append("\" fill=\"none\" stroke=\"#38bdf8\" stroke-width=\"2\" />\n");
        sb.append(markers);

        // Crosshair & interactive elements
        sb.append(String.format(Locale.ROOT,
                "  <line id=\"lgCrosshair\" x1=\"0\" y1=\"%.1f\" x2=\"0\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-dasharray=\"2,2\" stroke-width=\"1\" opacity=\"0\" pointer-events=\"none\" />\n",
                marginTop, marginTop + plotH));
        sb.append("  <circle id=\"lgTestDot\" cx=\"0\" cy=\"0\" r=\"5\" fill=\"#38bdf8\" stroke=\"#ffffff\" stroke-width=\"2\" opacity=\"0\" pointer-events=\"none\" />\n");
        if (hasLimbo) {
            sb.append("  <circle id=\"lgLimboDot\" cx=\"0\" cy=\"0\" r=\"4\" fill=\"#a855f7\" stroke=\"#ffffff\" stroke-width=\"1.5\" opacity=\"0\" pointer-events=\"none\" />\n");
        }
        sb.append(String.format(Locale.ROOT,
                "  <rect id=\"lgOverlay\" x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"transparent\" style=\"cursor: crosshair;\" onmousemove=\"onLgHover(event)\" onmouseleave=\"onLgLeave()\" onclick=\"onLgClick()\" />\n",
                marginLeft, marginTop, plotW, plotH));

        sb.append("</svg>\n");
        sb.append("<div id=\"lgTooltip\" class=\"lg-tooltip hidden\"></div>\n");
        sb.append("</div>\n");

        return sb.toString();
    }

    private static String escapeHtml(final String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // -------------------------------------------------------------------------
    // Artifact helpers
    // -------------------------------------------------------------------------

    private record ArtifactItem(String rel, int test, int ms, double sec, String ext, String name) {}

    private static final java.util.regex.Pattern ARTIFACT_PAT = java.util.regex.Pattern.compile(
            "(target/runs/[^/\\s\"&<>]+/[^/\\s\"&<>]+/)?((?:snapshots|data)/[^\"&<>\\r\\n]+?\\.(?:png|properties|log|json|html|txt))");
    private static final java.util.regex.Pattern SIBLING_REPORT_PAT = java.util.regex.Pattern.compile(
            "(?<![/a-zA-Z0-9])(\\d{13}\\.(?:tree|run)\\.html)");

    private static String linkifyArtifacts(final String htmlText) {
        final java.util.regex.Matcher m = ARTIFACT_PAT.matcher(htmlText);
        final StringBuilder sb = new StringBuilder();
        while (m.find()) {
            final String prefix = (null != m.group(1)) ? m.group(1) : "";
            final String rel = m.group(2);
            final String replacement = prefix + "<a class=\"log-artifact-link\" href=\"" + rel + "\" target=\"_blank\" title=\"Open artifact: " + rel + "\">" + rel + "</a>";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return SIBLING_REPORT_PAT.matcher(sb.toString()).replaceAll(
                "<a class=\"log-artifact-link\" href=\"$1\" target=\"_blank\" title=\"Open report: $1\">$1</a>");
    }

    private static List<ArtifactItem> loadRunArtifacts(final File runLogFile) {
        final List<ArtifactItem> artifacts = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return artifacts;
        final File runDir = runLogFile.getParentFile();
        final java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(.+)$");
        final String[] subdirs = new String[]{"snapshots", "data"};
        for (final String sub : subdirs) {
            final File dir = new File(runDir, sub);
            if (dir.exists() && dir.isDirectory()) {
                final File[] files = dir.listFiles();
                if (null != files) {
                    for (final File f : files) {
                        final String fname = f.getName();
                        final java.util.regex.Matcher m = p.matcher(fname);
                        if (m.matches()) {
                            final int testNum = Integer.parseInt(m.group(2));
                            final int ms = Integer.parseInt(m.group(3));
                            final double sec = ms / 1000.0;
                            final String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
                            artifacts.add(new ArtifactItem(sub + "/" + fname, testNum, ms, sec, ext, m.group(4)));
                        }
                    }
                }
            }
        }
        return artifacts;
    }

    // -------------------------------------------------------------------------
    // Log-line helpers
    // -------------------------------------------------------------------------

    private static int extractLineSpan(final String line) {
        final String clean = ANSI_PATTERN.matcher(line).replaceAll("").trim();
        final java.util.regex.Matcher m1 = LINE_WITH_TEST_RE.matcher(clean);
        if (m1.matches()) {
            final String s = m1.group(4);
            if (null != s && s.trim().matches("\\d+")) return Integer.parseInt(s.trim());
            return 0;
        }
        final java.util.regex.Matcher m2 = LINE_NO_TEST_RE.matcher(clean);
        if (m2.matches()) {
            final String s = m2.group(3);
            if (null != s && s.trim().matches("\\d+")) return Integer.parseInt(s.trim());
            return 0;
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Telemetry helpers
    // -------------------------------------------------------------------------

    private static List<Double> loadTelemetryCpu(final File runLogFile) {
        final List<Double> cpuLoads = new ArrayList<>();
        if (null == runLogFile || null == runLogFile.getParentFile()) return cpuLoads;
        final String runLogName = runLogFile.getName();
        final String telemName = runLogName.replace(".run.log", ".telemetry.log");
        final File telemFile = new File(runLogFile.getParentFile(), telemName);
        if (!telemFile.exists()) return cpuLoads;

        double lastVal = 0.0;
        try (final BufferedReader br = new BufferedReader(new FileReader(telemFile))) {
            String line;
            while (null != (line = br.readLine())) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                final String[] parts = trimmed.split(",");
                if (parts.length >= 3) {
                    final String valStr = parts[2].trim();
                    if (!"-".equals(valStr)) {
                        try {
                            lastVal = Double.parseDouble(valStr);
                        } catch (final NumberFormatException ignored) {
                        }
                    }
                    cpuLoads.add(lastVal);
                }
            }
        } catch (final IOException ignored) {
        }
        return cpuLoads;
    }

    // -------------------------------------------------------------------------
    // Time/duration helpers
    // -------------------------------------------------------------------------

    private static double calculateBootSpentSeconds(final List<String> lines) {
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

    private static double toSeconds(final String s) {
        if (s == null) return 0.0;
        final String clean = s.trim();
        try {
            return Double.parseDouble(clean.startsWith(".") ? "0" + clean : clean);
        } catch (final NumberFormatException e) {
            return 0.0;
        }
    }

    private static String formatDeltaTime(final double sec) {
        if (sec < 1.0) {
            return Math.round(sec * 1000) + " ms";
        }
        final int s = (int) sec;
        final int ms = (int) Math.round((sec - s) * 1000);
        return ms > 0 ? String.format(Locale.ROOT, "%d s %03d ms", s, ms) : s + " s";
    }

    // -------------------------------------------------------------------------
    // Domain model (inner classes)
    // -------------------------------------------------------------------------

    private static final class LogSection {
        final String type;
        String title;
        int prevTestNum;
        String status;
        int testNum;
        Integer startOp;
        Integer endOp;
        String startTime;
        String endTime;
        String durationStr;
        double durationSec;
        final List<String> scenarios = new ArrayList<>();
        final List<String> lines;

        LogSection(final String type, final String title, final String status, final LineStore lineStore) {
            this.type = type;
            this.title = title;
            this.status = status;
            lines = new SpoolingLines(lineStore);
        }
    }

    private static final class SpoolingLines extends AbstractList<String> {
        private final LineStore store;
        private final List<Long> positions = new ArrayList<>();

        private SpoolingLines(final LineStore store) {
            this.store = store;
        }

        @Override
        public String get(final int index) {
            try {
                return store.read(positions.get(index));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public int size() {
            return positions.size();
        }

        @Override
        public boolean add(final String line) {
            try {
                positions.add(store.append(line));
                return true;
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static final class LineStore implements AutoCloseable {
        private final Path path;
        private final RandomAccessFile file;

        private LineStore() throws IOException {
            path = java.nio.file.Files.createTempFile("run-report-lines-", ".tmp");
            file = new RandomAccessFile(path.toFile(), "rw");
        }

        private synchronized long append(final String line) throws IOException {
            final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
            final long position = file.length();
            file.seek(position);
            file.writeInt(bytes.length);
            file.write(bytes);
            return position;
        }

        private synchronized String read(final long position) throws IOException {
            file.seek(position);
            final int length = file.readInt();
            final byte[] bytes = new byte[length];
            file.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public void close() throws IOException {
            try {
                file.close();
            } finally {
                java.nio.file.Files.deleteIfExists(path);
            }
        }
    }

    private static final class PathEntry {
        final String num;
        final String durStr;
        final List<String> steps;

        PathEntry(final String num, final String durStr, final List<String> steps) {
            this.num = num;
            this.durStr = durStr;
            this.steps = steps;
        }
    }

    private static final class LeafCompletion {
        final long atMillis;
        final long etaMs;
        final String leaf;

        LeafCompletion(final long atMillis, final long etaMs, final String leaf) {
            this.atMillis = atMillis;
            this.etaMs = etaMs;
            this.leaf = leaf;
        }
    }

    private static final class ScenEntry {
        final String sym;
        final String name;
        final String weight;
        final String count;
        final String events;

        ScenEntry(final String sym, final String name, final String weight, final String count, final String events) {
            this.sym = sym;
            this.name = name;
            this.weight = weight;
            this.count = count;
            this.events = events;
        }
    }

    private static final class TelemetryItem {
        final String label;
        final String title;
        final double sec;
        final double ms;
        final String status;
        final boolean isLimbo;
        final int testNum;

        TelemetryItem(final String label, final String title, final double sec, final double ms,
                      final String status, final boolean isLimbo, final int testNum) {
            this.label = label;
            this.title = title;
            this.sec = sec;
            this.ms = ms;
            this.status = status;
            this.isLimbo = isLimbo;
            this.testNum = testNum;
        }
    }

    // -------------------------------------------------------------------------
    // Browser launcher
    // -------------------------------------------------------------------------

    public static void openInBrowser() {
        final String outFilePath = name(RUN_LOG_HTML_);
        openInBrowser(new File(outFilePath));
    }

    public static void openInBrowser(final File htmlFile) {
        try {
            if (null == htmlFile || !htmlFile.exists()) {
                return;
            }
            if (java.awt.GraphicsEnvironment.isHeadless() || Routines.isInsideCI() || Routines.isInsideDocker()) {
                return;
            }
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(htmlFile.toURI());
                return;
            }
            // Fallback for macOS / Linux CLI
            final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("mac")) {
                new ProcessBuilder("open", htmlFile.getAbsolutePath()).start();
            } else if (os.contains("nix") || os.contains("nux")) {
                new ProcessBuilder("xdg-open", htmlFile.getAbsolutePath()).start();
            }
        } catch (final Exception e) {
            logEvent(S2, "Unable to auto-open report in browser: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // CLI entry point
    // -------------------------------------------------------------------------

    public static void main(final String[] args) {
        if (args.length >= 2) {
            generateReport(new File(args[0]), new File(args[1]));
        } else if (args.length == 1) {
            final File logFile = new File(args[0]);
            final String outName = logFile.getAbsolutePath().endsWith(".log")
                    ? logFile.getAbsolutePath() + ".html"
                    : logFile.getAbsolutePath() + ".run.log.html";
            generateReport(logFile, new File(outName));
        }
    }
}
