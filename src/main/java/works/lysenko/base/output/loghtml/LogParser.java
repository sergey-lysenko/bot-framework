package works.lysenko.base.output.loghtml;

import works.lysenko.base.output.ProcessingProgress;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.loghtml.parser.*;
import works.lysenko.base.output.loghtml.parser.LogFileParser.ParseResult;
import works.lysenko.base.output.loghtml.parser.LogMediaLinks.MediaLinksResult;
import works.lysenko.base.output.loghtml.parser.LogMetrics.MetricsResult;
import works.lysenko.base.output.loghtml.LogModels.ArtifactItem;
import works.lysenko.base.output.loghtml.LogModels.EtaDebugItem;
import works.lysenko.base.output.loghtml.LogModels.LeafCompletion;
import works.lysenko.base.output.loghtml.LogModels.SystemResourceItem;
import works.lysenko.base.output.loghtml.LogModels.LineStore;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
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
import java.util.Map;

import static works.lysenko.Base.logEvent;
import static works.lysenko.base.output.loghtml.LogFormatting.escapeHtml;
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
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.R.REPORT;
import static works.lysenko.util.lang.word.R.RUN;
import static works.lysenko.util.lang.word.T.TIMELINE;
import static works.lysenko.util.chrs.____.TEST;

/**
 * Log section parsing engine and report document assembler.
 */
@SuppressWarnings({"ClassWithoutLogger", "MethodWithMultipleLoops", "OverlyLongMethod", "MethodWithTooManyParameters", "UtilityClass", "PublicInnerClass", "ObjectAllocationInLoop", "DynamicRegexReplaceableByCompiledPattern"})
public final class LogParser {

    private LogParser() {
    }

    /**
     * Builds HTML from reader.
     *
     * @param reader                     the reader
     * @param logFile                    the log file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     * @param lineStore                  the line store
     * @return HTML string
     * @throws IOException on error
     */
    public static String buildHtml(
            final BufferedReader reader,
            final File logFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final LineStore lineStore) throws IOException {
        return buildHtml(
                reader,
                logFile,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                scenarioMp4Enabled,
                treeMp4Enabled,
                lineStore,
                ProcessingProgress.NONE);
    }

    /**
     * Builds HTML from reader.
     *
     * @param reader                     the reader
     * @param logFile                    the log file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     * @param lineStore                  the line store
     * @return HTML string
     * @throws IOException on error
     */
    /**
     * Builds HTML from reader with progress.
     *
     * @param reader                     the reader
     * @param logFile                    the log file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     * @param lineStore                  the line store
     * @param progress                   the progress tracker
     * @return HTML string
     * @throws IOException on error
     */
    public static String buildHtml(
            final BufferedReader reader,
            final File logFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final LineStore lineStore,
            final ProcessingProgress progress) throws IOException {

        final ParseResult pRes = LogFileParser.parse(reader, logFile, lineStore, progress);
        final MetricsResult mRes = LogMetrics.compute(pRes.sections);
        final LogMediaLinks.MediaLinksConfig config = new LogMediaLinks.MediaLinksConfig(
                scenarioProgressionEnabled, treeProgressionEnabled,
                scenarioMp4Enabled, treeMp4Enabled);
        final MediaLinksResult mlRes = LogMediaLinks.build(logFile, config);

        final List<ArtifactItem> runArtifacts = SidecarLoader.loadRunArtifacts(logFile);
        final List<Double> telemetryCpu = SidecarLoader.loadTelemetryCpu(logFile);

        if (null == pRes.plaqueStatus) {
            boolean hasFailed = false;
            boolean hasTest = false;
            for (final LogSection sec : pRes.sections) {
                if ("failed".equals(sec.status)) {
                    hasFailed = true;
                    break;
                }
                if (TEST.equals(sec.type)) {
                    hasTest = true;
                }
            }
            if (hasFailed) {
                pRes.plaqueStatus = "failed";
                pRes.plaqueMessage = "= [FAILURE] Execution failed =";
            } else if (hasTest) {
                pRes.plaqueStatus = "passed";
                pRes.plaqueMessage = "= Execution passed successfully =";
            } else {
                pRes.plaqueStatus = "neutral";
                pRes.plaqueMessage = "= No test results =";
            }
        }
        final String resultPlaque = renderResultPlaque(pRes.plaqueStatus, pRes.plaqueMessage);

        final String statsStrip = renderStatsStrip(mRes.testData, mRes.limboData, mRes.tMin, mRes.tAvg, mRes.tMax, mRes.tTotal, mRes.lMin, mRes.lAvg, mRes.lMax, mRes.lTotal);
        final String timelineBars = renderTimeline(mRes.configItem, mRes.preflightItem, mRes.testData, mRes.limboByPrevTest, mRes.postflightItem, mRes.tMax, mRes.lMax, mRes.maxSec);
        final boolean isLineDefault = mRes.testData.size() > 50;
        final String lineHidden = isLineDefault ? "" : " hidden";
        final String barsHidden = isLineDefault ? " hidden" : "";
        final String timelineToggle = mRes.testData.isEmpty() ? "" : renderTimelineToggle(isLineDefault);
        final String timelineLineGraph = renderLineGraph(mRes.testData, mRes.limboByPrevTest, mRes.tAvg, mRes.tMax, mRes.lMax);
        final boolean addAllLeaf = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_ALL_LEAF.get());
        final boolean addEtaDebug = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_ETA_DEBUG.get());
        final boolean addCpuDebug = Boolean.TRUE.equals(PropEnum._TEST_REPORT_ADD_CPU_DEBUG.get());

        final List<LeafCompletion> rawCompletions = addAllLeaf ? SidecarLoader.loadLeafCompletions(logFile) : List.of();
        final List<EtaDebugItem> rawEtaDebugItems = addEtaDebug ? SidecarLoader.loadEtaDebugItems(logFile) : List.of();
        
        final List<LeafCompletion> completions = new ArrayList<>(16);
        final List<EtaDebugItem> etaDebugItems = new ArrayList<>(16);
        if (!mRes.testData.isEmpty()) {
            final java.util.Set<Integer> validTestNums = new java.util.LinkedHashSet<>();
            for (final TelemetryItem item : mRes.testData) {
                validTestNums.add(item.testNum);
            }
            if (!rawCompletions.isEmpty()) {
                final java.util.Map<Integer, LeafCompletion> compMap = new java.util.LinkedHashMap<>();
                for (final LeafCompletion c : rawCompletions) {
                    if (validTestNums.contains(c.testNum)) {
                        compMap.put(c.testNum, c);
                    }
                }
                for (final TelemetryItem item : mRes.testData) {
                    if (!compMap.containsKey(item.testNum)) {
                        final long durMs = Math.round(item.ms);
                        compMap.put(item.testNum, new LeafCompletion(item.testNum, durMs, 0L, 0, "n/a"));
                    }
                }
                for (final TelemetryItem item : mRes.testData) {
                    final LeafCompletion c = compMap.get(item.testNum);
                    if (null != c) completions.add(c);
                }
            }

            if (!rawEtaDebugItems.isEmpty()) {
                final java.util.Map<Integer, EtaDebugItem> etaMap = new java.util.LinkedHashMap<>();
                for (final EtaDebugItem e : rawEtaDebugItems) {
                    if (validTestNums.contains(e.testNum)) {
                        etaMap.put(e.testNum, e);
                    }
                }
                for (final TelemetryItem item : mRes.testData) {
                    if (!etaMap.containsKey(item.testNum)) {
                        final long durMs = Math.round(item.ms);
                        etaMap.put(item.testNum, new EtaDebugItem(item.testNum, durMs, durMs, 0L, durMs, "Test #" + item.testNum));
                    }
                }
                for (final TelemetryItem item : mRes.testData) {
                    final EtaDebugItem e = etaMap.get(item.testNum);
                    if (null != e) etaDebugItems.add(e);
                }
            }
        } else {
            completions.addAll(rawCompletions);
            etaDebugItems.addAll(rawEtaDebugItems);
        }

        final List<SystemResourceItem> resourceItems = addCpuDebug
                ? SidecarLoader.loadTelemetryResources(logFile, mRes.testData.size(), mRes.minTestOp, mRes.maxTestOp)
                : List.of();
        final List<Double> telemetryCpuList = addCpuDebug ? telemetryCpu : List.of();

        final String lgScriptData = renderLgScriptData(mRes.testData, mRes.limboByPrevTest, completions, etaDebugItems, resourceItems, mRes.tAvg, mRes.tMax, mRes.lMax);
        final String commonPath = renderCommonPath(pRes.commonPathSteps);
        final String pathsRows = renderPathsRows(pRes.testPaths, pRes.sections);
        final String scenSubtitle = buildScenSubtitle(pRes.pathsPossibleStr, pRes.pathsChanceStr, pRes.pathsExecutedStr);
        final String scenRows = renderScenRows(pRes.scenStats);
        final String sectionsHtml = renderSections(pRes.sections, runArtifacts, telemetryCpuList, 0); // Note: currOpGlobalIdx is 0 in original too
        final String leafCompletionGraph = addAllLeaf ? ChartsRenderer.renderLeafCompletionGraph(completions) : "";
        final String etaDebugGraph = addEtaDebug ? ChartsRenderer.renderEtaDebugGraph(etaDebugItems) : "";
        final String systemResourcesGraph = addCpuDebug ? ChartsRenderer.renderResourceStatsGraph(resourceItems) : "";

        final String pathsSummary = SummaryPlaque.cleanPathsSummary(pRes.pathsExecutedStr, pRes.pathsChanceStr, pRes.pathsPossibleStr);
        final SummaryPlaque summaryPlaque = SummaryPlaque.computeFromSections(pRes.sections, pRes.execConfigStr, pathsSummary);

        // ---- populate template ----

        final Map<String, String> replacements = new LinkedHashMap<>(16);
        replacements.put("{{TIMESTAMP_BLOCK}}", mlRes.getTimestampBlock());
        replacements.put("{{RESULT_PLAQUE}}", resultPlaque);
        replacements.put("{{SUMMARY_PLAQUE}}", summaryPlaque.renderHtml());
        replacements.put("{{SUMMARY_PLAQUE_CSS}}", SummaryPlaque.css());
        replacements.put("{{PROGRESSION_LINK}}", mlRes.getProgressionLink());
        replacements.put("{{PROGRESSION_WEBP_LINK}}", mlRes.getProgressionWebpLink());
        replacements.put("{{PROGRESSION_MP4_LINK}}", mlRes.getProgressionMp4Link());
        replacements.put("{{TREE_PROGRESSION_LINK}}", mlRes.getTreeProgressionLink());
        replacements.put("{{TREE_PROGRESSION_WEBP_LINK}}", mlRes.getTreeProgressionWebpLink());
        replacements.put("{{TREE_PROGRESSION_MP4_LINK}}", mlRes.getTreeProgressionMp4Link());
        replacements.put("{{TREE_LINK}}", escapeHtml(s(mlRes.getBasePrefix(), ".tree.html")));
        replacements.put("{{JSON_LINK}}", escapeHtml(s(mlRes.getBasePrefix(), ".run.json")));
        replacements.put("{{RAW_LINK}}", escapeHtml(s(mlRes.getBasePrefix(), ".run.log")));
        replacements.put("{{TELEM_LINK}}", escapeHtml(s(mlRes.getBasePrefix(), ".telemetry.log")));
        replacements.put("{{TIMELINE_TOGGLE}}", timelineToggle);
        replacements.put("{{STATS_STRIP}}", statsStrip);
        replacements.put("{{LINE_HIDDEN}}", lineHidden);
        replacements.put("{{BARS_HIDDEN}}", barsHidden);
        replacements.put("{{TIMELINE_LINE_GRAPH}}", timelineLineGraph);
        replacements.put("{{TIMELINE_BARS}}", timelineBars);
        replacements.put("{{LG_SCRIPT_DATA}}", lgScriptData);
        replacements.put("{{PATHS_COUNT}}", String.valueOf(pRes.testPaths.size()));
        replacements.put("{{COMMON_PATH}}", commonPath);
        replacements.put("{{PATH_STYLES}}", works.lysenko.base.output.tree.TreeTheme.DEFAULT.getExecutionPathStyles());
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
     * Generates the report.
     *
     * @param logFile                    the log file
     * @param outFile                    the out file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     */
    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled) {
        generateReport(
                logFile,
                outFile,
                scenarioProgressionEnabled,
                treeProgressionEnabled,
                scenarioMp4Enabled,
                treeMp4Enabled,
                ProcessingProgress.NONE);
    }

    /**
     * Generates the report.
     *
     * @param logFile                    the log file
     * @param outFile                    the out file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     */
    /**
     * Generates the report with progress.
     *
     * @param logFile                    the log file
     * @param outFile                    the out file
     * @param scenarioProgressionEnabled scenario flag
     * @param treeProgressionEnabled     tree flag
     * @param scenarioMp4Enabled         scenario mp4 flag
     * @param treeMp4Enabled             tree mp4 flag
     * @param progress                   the progress tracker
     */
    public static void generateReport(
            final File logFile,
            final File outFile,
            final boolean scenarioProgressionEnabled,
            final boolean treeProgressionEnabled,
            final boolean scenarioMp4Enabled,
            final boolean treeMp4Enabled,
            final ProcessingProgress progress) {
        final String task = b(c(RUN), TIMELINE, REPORT);
        final ProcessingProgress prg = null != progress ? progress : ProcessingProgress.NONE;
        try {
            if (!logFile.exists()) {
                prg.skipped(task);
                return;
            }
            prg.update(task, 0);
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
                        lineStore,
                        prg);
            }
            Files.writeToFile(html, outFile.getAbsolutePath());
            prg.complete(task);
        } catch (final Exception e) {
            prg.failed(task, e);
            logEvent(S2, "Failed to generate log.html: " + e.getMessage());
        }
    }
}
