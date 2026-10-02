package works.lysenko.base.output;

import works.lysenko.base.core.Routines;

import works.lysenko.util.func.type.Files;
import works.lysenko.util.spec.Layout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static works.lysenko.Base.core;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_;
import static works.lysenko.util.spec.Layout.Templates.RUN_LOG_HTML_;

@SuppressWarnings({"UtilityClass", "MethodWithMultipleLoops", "NestedMethodCall", "ClassWithoutLogger", "OverlyLongMethod"})
public final class LogHtml {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1b\\[[0-9;]*m");
    private static final Pattern LINE_WITH_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\]\\[\\s*(\\d+)\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern LINE_NO_TEST_RE = Pattern.compile("^\\[\\s*(\\d+)\\]\\[([^\\]]+)\\](?:\\[([^\\]]*)\\])?(.*)$");
    private static final Pattern PATH_LINE_RE = Pattern.compile("\\[(\\d+)\\]\\s*\\[\\s*([\\d,]+)\\s*\\]\\s*→\\s*(.*)");
    private static final Pattern SCEN_LINE_RE = Pattern.compile("([▷◼◆●])\\s*([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)*)[.\\s]+(\\[[^\\]]+\\]|\\([^)]+\\)\\s*→\\s*\\[[^\\]]+\\])\\s*(\\d+(?::\\d+)?)(?:\\s*\\(([^)]+)\\))?");
    private static final Pattern TEST_TIME_RE = Pattern.compile("Test time (.*)");
    private static final Pattern SPAN_BRACKET_RE = Pattern.compile("(\\[[0-9.]+\\](?:<[^>]+>)*)(\\[\\s*\\d+\\s*\\])");

    private LogHtml() {
    }

    public static void logStats() {
        try {
            if (null != core && null != core.getLogger() && null != core.getLogger().getLogWriter()) {
                if (core.getLogger().getLogWriter() instanceof java.io.Flushable) {
                    ((java.io.Flushable) core.getLogger().getLogWriter()).flush();
                }
            }
            final String logFilePath = name(RUN_LOG_);
            final File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                return;
            }
            final String outFilePath = name(RUN_LOG_HTML_);
            generateReport(logFile, new File(outFilePath));
        } catch (final Exception e) {
            logEvent(S2, "Failed to generate log.html: " + e.getMessage());
        }
    }

    public static void generateReport(final File logFile, final File outFile) {
        try {
            if (!logFile.exists()) return;
            final List<String> lines = new ArrayList<>();
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            final String html = buildHtml(lines, logFile);
            Files.writeToFile(html, outFile.getAbsolutePath());
        } catch (final Exception e) {
            logEvent(S2, "Failed to generate log.html: " + e.getMessage());
        }
    }

    private static String buildHtml(final List<String> lines, final File logFile) {
        final List<ArtifactItem> runArtifacts = loadRunArtifacts(logFile);
        final List<Double> telemetryCpu = loadTelemetryCpu(logFile);
        int currOpGlobalIdx = 0;
        final List<LogSection> sections = new ArrayList<>();
        LogSection currentSec = new LogSection("booting", "Booting", "neutral");

        int prevTestNum = 0;
        boolean inPostflight = false;

        final List<PathEntry> testPaths = new ArrayList<>();
        final List<ScenEntry> scenStats = new ArrayList<>();
        String pathsChanceStr = "";

        for (final String line : lines) {
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

            if (clean.contains("had a chance to be executed")) {
                pathsChanceStr = clean.contains("]") ? clean.substring(clean.lastIndexOf(']') + 1).trim() : clean;
            }

            if (clean.contains("# Applied test configuration") && "booting".equals(currentSec.type)) {
                if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                currentSec = new LogSection("configuring", "Configuring", "neutral");
            }

            if (clean.contains("Closing test service") || clean.contains("Events summary") || clean.contains("Postflight") || clean.contains("Test session completed")) {
                if (!inPostflight) {
                    inPostflight = true;
                    sections.add(currentSec);
                    currentSec = new LogSection("postflight", "Postflight", "neutral");
                }
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
                        currentSec = new LogSection("test", "Test #" + tNum, "passed");
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
                        currentSec = new LogSection("preflight", "Preflight", "neutral");
                        currentSec.startOp = opNum;
                        currentSec.startTime = ts;
                    } else if (prevTestNum > 0 && !"limbo".equals(currentSec.type)) {
                        if (!currentSec.lines.isEmpty()) sections.add(currentSec);
                        currentSec = new LogSection("limbo", "Limbo", "neutral");
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
        }
        if (!currentSec.lines.isEmpty()) sections.add(currentSec);

        TelemetryItem configItem = null;
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
                configItem = new TelemetryItem("Config", "Configuring", confSec, confSec * 1000.0, "neutral", true, 0);
                limboData.add(configItem);
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

        final StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<title>Execution Timeline & Telemetry</title>\n<style>\n");
        sb.append(":root { --bg: #090d16; --card-bg: #111827; --limbo-bg: #0d121f; --border: #1e293b; --text: #e2e8f0; --accent: #38bdf8; }\n");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; }\n");
        sb.append("body { background: var(--bg); color: var(--text); font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 13px; line-height: 1.5; padding: 24px 36px; }\n");
        sb.append("header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }\n");
        sb.append("h1 { font-size: 18px; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; display: flex; align-items: center; gap: 10px; }\n");
        sb.append(".controls { display: flex; gap: 10px; align-items: center; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n");
        sb.append("button { background: #1e293b; color: #f8fafc; border: 1px solid #334155; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; transition: all 0.15s; }\n");
        sb.append("button:hover { background: #334155; border-color: #475569; }\n");
        sb.append("button.active { background: #0284c7; border-color: #38bdf8; }\n");
        sb.append("input[type=\"text\"] { background: #1e293b; color: #f8fafc; border: 1px solid #334155; padding: 6px 12px; border-radius: 6px; font-size: 13px; outline: none; width: 220px; }\n");
        sb.append("input[type=\"text\"]:focus { border-color: var(--accent); }\n");
        sb.append("#charts-container { display: flex; margin-bottom: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n");
        sb.append("#summary-cards-container { display: flex; gap: 20px; margin-bottom: 20px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n");
        sb.append(".chart-card { flex: 1; min-width: 0; background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 16px 20px; }\n");
        sb.append(".summary-card { flex: 1; min-width: 0; background: var(--card-bg); border: 1px solid var(--border); border-radius: 8px; padding: 16px 20px; }\n");
        sb.append(".card-title { font-size: 14px; font-weight: 600; color: #f8fafc; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }\n");
        sb.append(".card-subtitle { font-size: 12px; color: #94a3b8; font-weight: normal; }\n");
        sb.append(".stats-strip { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; font-family: ui-monospace, monospace; font-size: 11px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }\n");
        sb.append(".stat-group { display: flex; align-items: center; gap: 12px; }\n");
        sb.append(".stat-group-label { color: #94a3b8; font-weight: 600; text-transform: uppercase; font-size: 10px; letter-spacing: 0.05em; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n");
        sb.append(".stat-divider { width: 1px; height: 14px; background: var(--border); }\n");
        sb.append(".stat-item { color: #64748b; }\n");
        sb.append(".stat-item b { color: #38bdf8; font-weight: 600; }\n");
        sb.append(".stat-group-limbo .stat-item b { color: #94a3b8; }\n");
        sb.append(".chart-scroll-wrap { overflow-x: auto; overflow-y: hidden; padding-bottom: 6px; }\n");
        sb.append(".chart-scroll-wrap::-webkit-scrollbar { height: 6px; }\n");
        sb.append(".chart-scroll-wrap::-webkit-scrollbar-track { background: rgba(15, 23, 42, 0.6); border-radius: 3px; }\n");
        sb.append(".chart-scroll-wrap::-webkit-scrollbar-thumb { background: #334155; border-radius: 3px; }\n");
        sb.append(".chart-scroll-wrap::-webkit-scrollbar-thumb:hover { background: #475569; }\n");
        sb.append(".bars-flex { display: flex; align-items: stretch; gap: 8px; padding: 0 4px; width: max-content; min-width: 100%; }\n");
        sb.append(".bar-col.config { display: flex; flex-direction: column; align-items: center; min-width: 44px; cursor: pointer; user-select: none; }\n");
        sb.append(".bar-col.config .bar-val { font-size: 11px; color: #94a3b8; font-family: ui-monospace, monospace; margin-bottom: 4px; white-space: nowrap; }\n");
        sb.append(".bar-col.config .bar-track { flex: 1; width: 100%; max-width: 34px; display: flex; align-items: flex-end; background: rgba(30, 41, 59, 0.25); border-radius: 4px; overflow: hidden; }\n");
        sb.append(".bar-col.config .bar-fill { width: 100%; background: #64748b; border-radius: 4px 4px 0 0; transition: height 0.2s; }\n");
        sb.append(".bar-col.config .bar-lbl { font-size: 10px; color: #64748b; font-family: ui-monospace, monospace; margin-top: 5px; white-space: nowrap; }\n");
        sb.append(".bar-pair { display: flex; flex-direction: column; align-items: center; justify-content: space-between; min-width: 44px; gap: 8px; }\n");
        sb.append(".bar-col.test { display: flex; flex-direction: column; align-items: center; width: 100%; height: 85px; cursor: pointer; user-select: none; }\n");
        sb.append(".bar-col.test .bar-val { font-size: 11px; color: #cbd5e1; font-family: ui-monospace, monospace; margin-bottom: 4px; white-space: nowrap; }\n");
        sb.append(".bar-col.test .bar-track { flex: 1; width: 100%; max-width: 34px; display: flex; align-items: flex-end; background: rgba(30, 41, 59, 0.35); border-radius: 4px; overflow: hidden; }\n");
        sb.append(".bar-col.test .bar-fill { width: 100%; border-radius: 4px 4px 0 0; transition: height 0.2s; }\n");
        sb.append(".bar-col.test .bar-lbl { font-size: 11px; color: #94a3b8; font-family: ui-monospace, monospace; font-weight: 600; margin-top: 5px; white-space: nowrap; }\n");
        sb.append(".bar-col.limbo { display: flex; flex-direction: column; align-items: center; width: 100%; height: 52px; cursor: pointer; user-select: none; }\n");
        sb.append(".bar-col.limbo .bar-val { font-size: 10px; color: #94a3b8; font-family: ui-monospace, monospace; margin-bottom: 3px; white-space: nowrap; }\n");
        sb.append(".bar-col.limbo .bar-track { flex: 1; width: 100%; max-width: 34px; display: flex; align-items: flex-end; background: rgba(30, 41, 59, 0.2); border-radius: 4px; overflow: hidden; }\n");
        sb.append(".bar-col.limbo .bar-fill { width: 100%; border-radius: 4px 4px 0 0; transition: height 0.2s; }\n");
        sb.append(".bar-col.limbo .bar-lbl { font-size: 10px; color: #64748b; font-family: ui-monospace, monospace; margin-top: 4px; white-space: nowrap; }\n");
        sb.append(".bar-col:hover .bar-fill { filter: brightness(1.25); }\n");
        sb.append(".bar-col:hover .bar-val { color: #38bdf8; }\n");
        sb.append(".bar-col:hover .bar-lbl { color: #f8fafc; }\n");
        sb.append("body.hide-limbo .bar-col.limbo { display: none !important; }\n");
        sb.append("body.hide-limbo .bar-pair { gap: 0; }\n");
        sb.append("body.hide-limbo .bar-col.config { height: 85px; }\n");
        sb.append("body.hide-limbo .stat-group-limbo, body.hide-limbo .stat-divider { display: none !important; }\n");
        sb.append(".path-row { display: flex; align-items: center; gap: 14px; padding: 7px 10px; border-radius: 6px; cursor: pointer; transition: background 0.15s; border-bottom: 1px solid rgba(255, 255, 255, 0.03); }\n");
        sb.append(".path-row:hover { background: rgba(56, 189, 248, 0.08); }\n");
        sb.append(".path-meta { display: flex; align-items: center; gap: 8px; min-width: 140px; }\n");
        sb.append(".path-num { font-size: 12px; font-weight: 600; color: #f8fafc; font-family: ui-monospace, monospace; }\n");
        sb.append(".path-dur { font-size: 11px; color: #38bdf8; font-family: ui-monospace, monospace; }\n");
        sb.append(".path-chain { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-family: ui-monospace, monospace; font-size: 12px; }\n");
        sb.append(".path-step { background: #1e293b; color: #e2e8f0; padding: 2px 8px; border-radius: 4px; border: 1px solid #334155; transition: all 0.2s ease; cursor: pointer; }\n");
        sb.append(".path-step:hover { background: #334155; border-color: #38bdf8; color: #38bdf8; transform: translateY(-1px); box-shadow: 0 0 10px rgba(56, 189, 248, 0.4); }\n");
        sb.append("a.log-artifact-link { color: #38bdf8; text-decoration: underline; text-underline-offset: 3px; font-weight: 500; transition: all 0.15s ease; }\n");
        sb.append("a.log-artifact-link:hover { color: #7dd3fc; background: rgba(56, 189, 248, 0.18); border-radius: 2px; }\n");
        sb.append(".gutter-artifact-badge { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; margin-left: 6px; border-radius: 4px; text-decoration: none; vertical-align: middle; transition: transform 0.15s ease, box-shadow 0.15s ease; cursor: pointer; }\n");
        sb.append(".gutter-artifact-badge:hover { transform: scale(1.25); box-shadow: 0 0 8px rgba(56, 189, 248, 0.6); }\n");
        sb.append(".badge-data { background: rgba(59, 130, 246, 0.2); border: 1px solid rgba(59, 130, 246, 0.4); }\n");
        sb.append(".badge-img { background: rgba(168, 85, 247, 0.2); border: 1px solid rgba(168, 85, 247, 0.4); }\n");
        sb.append(".badge-xml { background: rgba(34, 197, 94, 0.2); border: 1px solid rgba(34, 197, 94, 0.4); }\n");
        sb.append(".log-line-entry.step-highlight { background: rgba(56, 189, 248, 0.14) !important; border-left: 4px solid #38bdf8 !important; padding-left: 6px; }\n");
        sb.append(".log-line-entry.step-highlight.step-highlight-first { border-top: 1px dashed rgba(56, 189, 248, 0.5) !important; border-top-left-radius: 4px; }\n");
        sb.append(".log-line-entry.step-highlight.step-highlight-last { border-bottom: 1px dashed rgba(56, 189, 248, 0.5) !important; border-bottom-left-radius: 4px; }\n");
        sb.append(".log-line-entry.step-highlight-flash { animation: stepAreaFlash 3s cubic-bezier(0.16, 1, 0.3, 1); }\n");
        sb.append("@keyframes stepAreaFlash { 0% { background: rgba(56, 189, 248, 0.65) !important; box-shadow: inset 0 0 16px rgba(56, 189, 248, 0.5); } 30% { background: rgba(56, 189, 248, 0.35); } 100% { background: rgba(56, 189, 248, 0.14); } }\n");
        sb.append(".step-arrow { color: #64748b; font-size: 11px; }\n");
        sb.append(".scen-table { width: 100%; border-collapse: collapse; font-size: 12px; }\n");
        sb.append(".scen-table th { text-align: left; padding: 6px 8px; color: #64748b; font-weight: 500; border-bottom: 1px solid var(--border); font-size: 11px; text-transform: uppercase; }\n");
        sb.append(".scen-table td { padding: 6px 8px; border-bottom: 1px solid rgba(255, 255, 255, 0.03); }\n");
        sb.append(".cell-mono { font-family: ui-monospace, monospace; }\n");
        sb.append(".text-right { text-align: right; }\n");
        sb.append(".text-muted { color: #64748b; }\n");
        sb.append(".sym-node { color: #38bdf8; }\n");
        sb.append(".sym-leaf { color: #a855f7; }\n");
        sb.append(".sym-fork { color: #22c55e; }\n");
        sb.append(".sym-other { color: #f59e0b; }\n");
        sb.append("#sectionsContainer { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 10px; align-items: start; }\n");
        sb.append("details.log-section { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; transition: all 0.15s ease-out; }\n");
        sb.append("details.log-section.preflight, details.log-section.postflight { grid-column: 1 / -1; background: var(--card-bg); border-left: 4px solid #38bdf8; }\n");
        sb.append("details.log-section.test { grid-column: 1; background: var(--card-bg); border-left: 4px solid #22c55e; }\n");
        sb.append("details.log-section.booting { grid-column: 1; background: var(--card-bg); border-left: 4px solid #38bdf8; }\n");
        sb.append("details.log-section.configuring { grid-column: 2; background: var(--limbo-bg); border-left: 4px solid #475569; opacity: 0.85; font-size: 0.92em; }\n");
        sb.append("details.log-section.configuring:hover { opacity: 1; }\n");
        sb.append("body.hide-limbo details.log-section.test { grid-column: 1 / -1; }\n");
        sb.append("body.hide-limbo details.log-section.booting { grid-column: 1 / -1; }\n");
        sb.append("body.hide-limbo details.log-section.configuring { display: none !important; }\n");
        sb.append("details.log-section.test.warning { border-left-color: #f59e0b; }\n");
        sb.append("details.log-section.test.failed { border-left-color: #ef4444; }\n");
        sb.append("details.log-section.limbo { grid-column: 2; background: var(--limbo-bg); border-left: 4px solid #475569; opacity: 0.85; }\n");
        sb.append("details.log-section.limbo:hover { opacity: 1; }\n");
        sb.append("details.log-section[open] { grid-column: 1 / -1 !important; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5); z-index: 2; }\n");
        sb.append("summary { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; cursor: pointer; user-select: none; background: rgba(30, 41, 59, 0.4); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; font-weight: 500; font-size: 13px; }\n");
        sb.append("summary:hover { background: rgba(51, 65, 85, 0.5); }\n");
        sb.append("summary::-webkit-details-marker { display: none; }\n");
        sb.append(".summary-left { display: flex; align-items: center; gap: 8px; min-width: 0; flex-shrink: 0; }\n");
        sb.append(".sec-title { white-space: nowrap; font-weight: 600; flex-shrink: 0; }\n");
        sb.append(".op-range { font-family: ui-monospace, monospace; font-size: 11px; color: #64748b; background: rgba(15, 23, 42, 0.6); padding: 2px 6px; border-radius: 4px; border: 1px solid #1e293b; white-space: nowrap; flex-shrink: 0; }\n");
        sb.append(".badge { font-size: 11px; padding: 2px 8px; border-radius: 12px; font-weight: 600; text-transform: uppercase; white-space: nowrap; flex-shrink: 0; }\n");
        sb.append(".badge.passed { background: #14532d; color: #4ade80; border: 1px solid #22c55e; }\n");
        sb.append(".badge.warning { background: #713f12; color: #fde047; border: 1px solid #f59e0b; }\n");
        sb.append(".badge.failed { background: #7f1d1d; color: #fca5a5; border: 1px solid #ef4444; }\n");
        sb.append(".badge.neutral { background: #1e293b; color: #94a3b8; border: 1px solid #334155; }\n");
        sb.append(".badge.limbo { background: #1e293b; color: #94a3b8; border: 1px dashed #64748b; font-size: 10px; }\n");
        sb.append(".duration { color: #38bdf8; font-size: 12px; font-weight: 600; margin-left: 6px; font-family: ui-monospace, monospace; white-space: nowrap; flex-shrink: 0; }\n");
        sb.append(".duration.limbo-duration { color: #94a3b8; font-weight: normal; }\n");
        sb.append(".summary-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; margin-left: auto; }\n");
        sb.append(".time-range { color: #64748b; font-size: 12px; font-family: ui-monospace, monospace; white-space: nowrap; flex-shrink: 0; }\n");
        sb.append(".line-count { color: #475569; font-size: 11px; white-space: nowrap; flex-shrink: 0; }\n");
        sb.append(".section-breadcrumb { display: flex; align-items: center; gap: 8px; padding: 7px 16px; background: #080c14; border-top: 1px solid var(--border); font-size: 12px; font-family: ui-monospace, monospace; flex-wrap: wrap; }\n");
        sb.append(".breadcrumb-label { color: #475569; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }\n");
        sb.append(".pill { background: #1e293b; padding: 1px 7px; border-radius: 4px; color: #cbd5e1; font-size: 11px; white-space: nowrap; border: 1px solid #334155; }\n");
        sb.append("@media (max-width: 960px) { #sectionsContainer { grid-template-columns: 1fr; } }\n");
        sb.append("@media (max-width: 600px) { .time-range { display: none; } }\n");
        sb.append(".log-section-body { display: flex; flex-direction: row; border-top: 1px solid var(--border); background: #070a10; }\n");
        sb.append(".soundtrack-column { width: 56px; flex-shrink: 0; background: #05070c; border-right: 1px solid #1a2234; padding: 12px 0; user-select: none; display: flex; flex-direction: column; position: relative; }\n");
        sb.append(".soundtrack-column::before { content: ''; position: absolute; top: 0; bottom: 0; left: 8px; width: 6px; background-image: radial-gradient(circle, #334155 35%, transparent 40%); background-size: 6px 19.2px; opacity: 0.5; }\n");
        sb.append(".soundtrack-track { margin-left: 18px; width: 34px; height: 100%; display: flex; flex-direction: column; }\n");
        sb.append(".soundtrack-column.right-track { border-right: none; border-left: 1px solid #1a2234; }\n");
        sb.append(".soundtrack-column.right-track::before { left: auto; right: 8px; }\n");
        sb.append(".soundtrack-column.right-track .soundtrack-track { margin-left: 0; margin-right: 18px; }\n");
        sb.append(".st-row { height: 19.2px; display: flex; align-items: center; justify-content: center; position: relative; }\n");
        sb.append(".st-wave { height: 13px; border-radius: 1px; opacity: 0.88; transition: all 0.1s ease; }\n");
        sb.append(".st-row:hover .st-wave { opacity: 1; filter: brightness(1.35); }\n");
        sb.append(".st-row.rate-white .st-wave { background: #cbd5e1; box-shadow: 0 0 4px rgba(203, 213, 225, 0.4); }\n");
        sb.append(".st-row.rate-blue .st-wave { background: #38bdf8; box-shadow: 0 0 6px rgba(56, 189, 248, 0.6); }\n");
        sb.append(".st-row.rate-green .st-wave { background: #22c55e; box-shadow: 0 0 7px rgba(34, 197, 94, 0.7); }\n");
        sb.append(".st-row.rate-yellow .st-wave { background: #eab308; box-shadow: 0 0 8px rgba(234, 179, 8, 0.8); }\n");
        sb.append(".st-row.rate-red .st-wave { background: #ef4444; box-shadow: 0 0 10px rgba(239, 68, 68, 0.9); }\n");
        sb.append(".log-content { flex: 1; padding: 12px 18px; overflow-x: auto; background: transparent; font-size: 12px; line-height: 19.2px; white-space: pre; word-break: normal; border-top: none; }\n");
        sb.append(".log-line-entry { height: 19.2px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: pre; }\n");
        sb.append(".hidden { display: none !important; }\n");
        sb.append(".highlight { outline: 2px solid var(--accent); }\n");
        sb.append("</style>\n</head>\n<body>\n");

        final String runLogName = (null != logFile) ? logFile.getName() : "";
        final String basePrefix = runLogName.replace(".run.log", "");
        final String treeLink = basePrefix + ".tree.html";
        final String jsonLink = basePrefix + ".run.json";
        final String rawLink = basePrefix + ".run.log";
        final String telemLink = basePrefix + ".telemetry.log";

        String timeStr = "";
        try {
            final long epoch = Long.parseLong(basePrefix);
            final java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
            timeStr = dtf.format(java.time.Instant.ofEpochMilli(epoch));
        } catch (final Exception ignored) {}

        sb.append("<header>\n  <div class=\"header-title-box\">\n    <h1>⚡ Test Execution Timeline & Telemetry</h1>\n");
        if (!timeStr.isEmpty()) {
            sb.append("    <div class=\"header-timestamp\">🕒 ").append(timeStr).append("<span class=\"ts-val\">(").append(basePrefix).append(")</span></div>\n");
        }
        sb.append("  </div>\n");
        sb.append("  <div class=\"controls\">\n");
        sb.append("    <input type=\"text\" id=\"filterInput\" placeholder=\"Filter operations...\" oninput=\"filterLogs()\">\n");
        sb.append("    <div class=\"controls-grid\">\n");
        sb.append("      <button type=\"button\" onclick=\"expandAll()\">Expand All</button>\n");
        sb.append("      <button type=\"button\" onclick=\"collapseAll()\">Collapse All</button>\n");
        sb.append("      <button type=\"button\" id=\"limboBtn\" onclick=\"toggleLimbo()\">Hide Limbo</button>\n");
        sb.append("      <button type=\"button\" id=\"issuesBtn\" onclick=\"toggleOnlyIssues()\">Only Issues</button>\n");
        sb.append("      <a class=\"btn-link\" href=\"").append(treeLink).append("\" target=\"_blank\" title=\"Interactive Scenario Graph\"><button type=\"button\">Tree</button></a>\n");
        sb.append("      <a class=\"btn-link\" href=\"").append(jsonLink).append("\" target=\"_blank\" title=\"Standard Indented JSON Report\"><button type=\"button\">JSON</button></a>\n");
        sb.append("      <a class=\"btn-link\" href=\"").append(rawLink).append("\" target=\"_blank\" title=\"Unprocessed ANSI Log\"><button type=\"button\">Raw Log</button></a>\n");
        sb.append("      <a class=\"btn-link\" href=\"").append(telemLink).append("\" target=\"_blank\" title=\"System CPU/Memory Telemetry\"><button type=\"button\">Telemetry</button></a>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n</header>\n");

        sb.append("<div id=\"charts-container\">\n");
        sb.append("  <div class=\"chart-card\">\n    <div class=\"card-title\">Test Executions &amp; Limbo Durations</div>\n");
        sb.append("    <div class=\"stats-strip\">\n");
        if (!testData.isEmpty()) {
            sb.append(String.format(Locale.ROOT, "      <div class=\"stat-group\"><span class=\"stat-group-label\">Tests:</span> <div class=\"stat-item\">min: <b>%.2fs</b></div><div class=\"stat-item\">avg: <b>%.2fs</b></div><div class=\"stat-item\">max: <b>%.2fs</b></div><div class=\"stat-item\">total: <b>%.2fs</b></div></div>\n", tMin, tAvg, tMax, tTotal));
        }
        if (!limboData.isEmpty()) {
            if (!testData.isEmpty()) {
                sb.append("      <div class=\"stat-divider\"></div>\n");
            }
            sb.append(String.format(Locale.ROOT, "      <div class=\"stat-group stat-group-limbo\"><span class=\"stat-group-label\">Limbo:</span> <div class=\"stat-item\">min: <b>%dms</b></div><div class=\"stat-item\">avg: <b>%dms</b></div><div class=\"stat-item\">max: <b>%dms</b></div><div class=\"stat-item\">total: <b>%dms</b></div></div>\n", Math.round(lMin), Math.round(lAvg), Math.round(lMax), Math.round(lTotal)));
        }
        sb.append("    </div>\n");
        sb.append("    <div class=\"chart-scroll-wrap\">\n");
        sb.append("      <div class=\"bars-flex\">").append(renderTimeline(configItem, testData, limboByPrevTest, tMax, lMax)).append("</div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n</div>\n");

        sb.append("<div id=\"summary-cards-container\">\n");
        sb.append("  <div class=\"summary-card\">\n    <div class=\"card-title\">Execution Paths <span class=\"card-subtitle\">").append(testPaths.size()).append(" Tests Executed</span></div>\n    <div>");
        for (final PathEntry p : testPaths) {
            sb.append("<div class=\"path-row\" onclick=\"focusSection('Test #").append(p.num).append("')\"><div class=\"path-meta\"><span class=\"path-num\">Test #").append(p.num).append("</span><span class=\"path-dur\">⏱ ").append(p.durStr).append("</span></div><div class=\"path-chain\">");
            for (int sIdx = 0; sIdx < p.steps.size(); sIdx++) {
                if (sIdx > 0) sb.append(" <span class=\"step-arrow\">→</span> ");
                sb.append("<span class=\"path-step\">").append(escapeHtml(p.steps.get(sIdx))).append("</span>");
            }
            sb.append("</div></div>");
        }
        sb.append("</div>\n  </div>\n");

        sb.append("  <div class=\"summary-card\">\n    <div class=\"card-title\">Scenario Statistics <span class=\"card-subtitle\">").append(escapeHtml(pathsChanceStr)).append("</span></div>\n");
        sb.append("    <table class=\"scen-table\"><thead><tr><th>Scenario</th><th>Weight Flow</th><th class=\"text-right\">Hits</th></tr></thead><tbody>");
        for (final ScenEntry sc : scenStats) {
            final String symClass = "▷".equals(sc.sym) ? "sym-node" : "◼".equals(sc.sym) ? "sym-leaf" : "◆".equals(sc.sym) ? "sym-fork" : "sym-other";
            final String evtBadge = !sc.events.isEmpty() ? "<span class=\"badge warning\" style=\"margin-left: 8px;\">" + escapeHtml(sc.events) + "</span>" : "";
            sb.append("<tr><td><span class=\"").append(symClass).append("\">").append(sc.sym).append("</span> <b>").append(escapeHtml(sc.name)).append("</b></td><td class=\"cell-mono text-muted\">").append(escapeHtml(sc.weight)).append("</td><td class=\"cell-mono text-right\"><b>").append(sc.count).append("</b>").append(evtBadge).append("</td></tr>");
        }
        sb.append("</tbody></table>\n  </div>\n</div>\n");

        sb.append("<main id=\"sectionsContainer\">\n");
        for (final LogSection sec : sections) {
            final boolean isTest = "test".equals(sec.type);
            final boolean isLimbo = "limbo".equals(sec.type) || "configuring".equals(sec.type);
            final String badgeClass = isTest ? sec.status : isLimbo ? "limbo" : "neutral";
            final String badgeText = isTest ? sec.status : "limbo".equals(sec.type) ? "limbo" : "config";
            final String opRange = sec.startOp != null ? "[" + sec.startOp + ".." + sec.endOp + "]" : "";
            final String timeRange = (sec.startTime != null && sec.endTime != null) ? sec.startTime + "s ➔ " + sec.endTime + "s" : "";
            final String secId = "sec_" + sec.title.replace(" ", "_").replace("#", "").replace("➔", "to");

            sb.append("<details id=\"").append(secId).append("\" class=\"log-section ").append(sec.type).append(" ").append(sec.status).append("\">\n");
            sb.append("  <summary>\n    <div class=\"summary-left\">\n");
            if (!opRange.isEmpty()) sb.append("      <span class=\"op-range\">").append(opRange).append("</span>\n");
            sb.append("      <span class=\"sec-title\">").append(escapeHtml(sec.title)).append("</span>\n");
            sb.append("      <span class=\"badge ").append(badgeClass).append("\">").append(badgeText).append("</span>\n");
            if (sec.durationStr != null && !sec.durationStr.isEmpty()) {
                final String durClass = isLimbo ? "duration limbo-duration" : "duration";
                sb.append("      <span class=\"").append(durClass).append("\">⏱ ").append(escapeHtml(sec.durationStr)).append("</span>\n");
            }
            sb.append("    </div>\n    <div class=\"summary-right\">\n");
            if (!timeRange.isEmpty()) sb.append("      <span class=\"time-range\">").append(timeRange).append("</span>\n");
            sb.append("      <span class=\"line-count\">").append(sec.lines.size()).append(" lines</span>\n    </div>\n  </summary>\n");
            if ("test".equals(sec.type) && !sec.scenarios.isEmpty()) {
                sb.append("  <div class=\"section-breadcrumb\">\n    <span class=\"breadcrumb-label\">Scenario:</span>\n");
                for (final String sc : sec.scenarios) {
                    sb.append("    <span class=\"pill\">").append(escapeHtml(sc)).append("</span>\n");
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

            final List<String> parsedLines = new ArrayList<>();
            for (final String l : sec.lines) {
                parsedLines.add(parseAnsi(l));
            }

            // Map artifacts to lines in this section:
            // 1) Announcement lines (direct name/rel mention or snapshot text)
            // 2) Causative lines (where duration span [span] encompassed the creation)
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

                // Check for causative line (operation whose span encompassed artifact creation)
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

            final List<String> linesDivs = new ArrayList<>();
            for (int lIdx = 0; lIdx < parsedLines.size(); lIdx++) {
                String pl = parsedLines.get(lIdx);
                final StringBuilder badgeHtml = new StringBuilder();

                // 1) Link duration bracket on causative line
                final List<ArtifactItem> caus = causativeArtifacts.get(lIdx);
                if (null != caus && !caus.isEmpty()) {
                    final ArtifactItem firstA = caus.get(0);
                    final String tip = "Artifact created during this operation: " + firstA.rel();
                    final java.util.regex.Matcher mSpan = SPAN_BRACKET_RE.matcher(pl);
                    if (mSpan.find()) {
                        final String prefix = mSpan.group(1);
                        final String bracket = mSpan.group(2);
                        final String linkedBracket = prefix + "<a class=\"log-artifact-link\" href=\"" + firstA.rel() + "\" target=\"_blank\" title=\"" + tip + "\">" + bracket + "</a>";
                        pl = mSpan.replaceFirst(java.util.regex.Matcher.quoteReplacement(linkedBracket));
                    }
                }

                // 2) Link announcement line text
                final List<ArtifactItem> matched = secArtifacts.get(lIdx);
                if (null != matched) {
                    for (final ArtifactItem a : matched) {
                        if (".properties".equals(a.ext()) && pl.contains("snapshot of test data") && !pl.contains("log-artifact-link")) {
                            pl = pl.replaceAll("(Made\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+test\\s+data)", "<a class=\"log-artifact-link\" href=\"" + a.rel() + "\" target=\"_blank\" title=\"Open artifact: " + a.rel() + "\">$1</a>");
                        } else if (".xml".equals(a.ext()) && pl.contains("snapshot of page code") && !pl.contains("log-artifact-link")) {
                            pl = pl.replaceAll("(Making\\s+(?:'|&#x27;|&quot;)[^<&]+(?:'|&#x27;|&quot;)\\s+snapshot\\s+of\\s+page\\s+code)", "<a class=\"log-artifact-link\" href=\"" + a.rel() + "\" target=\"_blank\" title=\"Open artifact: " + a.rel() + "\">$1</a>");
                        }

                        // Only add an icon badge if the artifact path is not already referenced as a link in the line
                        if (!pl.contains(a.rel())) {
                            final String bIcon = ".properties".equals(a.ext()) ? "💾" : ".xml".equals(a.ext()) ? "📄" : "📸";
                            final String bType = ".properties".equals(a.ext()) ? "badge-data" : ".xml".equals(a.ext()) ? "badge-xml" : "badge-img";
                            badgeHtml.append("<a class=\"gutter-artifact-badge ").append(bType).append("\" href=\"").append(a.rel()).append("\" target=\"_blank\" title=\"Open ").append(a.ext().isEmpty() ? "" : a.ext().substring(1)).append(": ").append(a.rel()).append("\">").append(bIcon).append("</a>");
                        }
                    }
                }

                if (badgeHtml.length() > 0) {
                    pl = pl + "  " + badgeHtml;
                }

                linesDivs.add("<div class=\"log-line-entry\">" + pl + "</div>");
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
                        stRows.append("<div class=\"st-row").append(rateClass).append("\" title=\"").append(sp).append(" ms\"><div class=\"st-wave\" style=\"width: ").append(wPx).append("px;\"></div></div>");
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
                            cpuRows.append(String.format(Locale.ROOT, "<div class=\"st-row%s\" title=\"%.2f%%\"><div class=\"st-wave\" style=\"width: %dpx;\"></div></div>", cClass, cpuVal, cpuW));
                        }
                    } else {
                        cpuRows.append("<div class=\"st-row rate-white\" title=\"-\"><div class=\"st-wave\" style=\"width: 1px; opacity: 0.15;\"></div></div>");
                    }
                }

                sb.append("  <div class=\"log-section-body\">\n");
                sb.append("    <div class=\"soundtrack-column\" title=\"35mm Optical Track (Operation Duration)\"><div class=\"soundtrack-track\">").append(stRows).append("</div></div>\n");
                sb.append("    <div class=\"log-content\">");
                for (final String div : linesDivs) sb.append(div);
                sb.append("</div>\n");
                sb.append("    <div class=\"soundtrack-column right-track\" title=\"35mm Optical Track (CPU Load)\"><div class=\"soundtrack-track\">").append(cpuRows).append("</div></div>\n");
                sb.append("  </div>\n</details>\n");
            } else {
                sb.append("  <div class=\"log-content\">");
                for (final String div : linesDivs) sb.append(div);
                sb.append("</div>\n</details>\n");
            }
        }
        sb.append("</main>\n");

        sb.append("<script>\n");
        sb.append("  function expandAll() { document.querySelectorAll('details.log-section').forEach(d => d.open = true); }\n");
        sb.append("  function collapseAll() { document.querySelectorAll('details.log-section').forEach(d => d.open = false); }\n");
        sb.append("  let hideLimbo = false;\n");
        sb.append("  function toggleLimbo() {\n    hideLimbo = !hideLimbo;\n    document.body.classList.toggle('hide-limbo', hideLimbo);\n    document.getElementById('limboBtn').classList.toggle('active', hideLimbo);\n    document.querySelectorAll('details.log-section.limbo').forEach(d => d.classList.toggle('hidden', hideLimbo));\n  }\n");
        sb.append("  let onlyIssues = false;\n");
        sb.append("  function toggleOnlyIssues() {\n    onlyIssues = !onlyIssues;\n    document.getElementById('issuesBtn').classList.toggle('active', onlyIssues);\n    document.querySelectorAll('details.log-section').forEach(d => {\n      if (onlyIssues) {\n        if (d.classList.contains('failed') || d.classList.contains('warning')) { d.classList.remove('hidden'); d.open = true; } else { d.classList.add('hidden'); }\n      } else { if (!hideLimbo || !d.classList.contains('limbo')) d.classList.remove('hidden'); }\n    });\n  }\n");
        sb.append("  function filterLogs() {\n    const q = document.getElementById('filterInput').value.toLowerCase();\n    document.querySelectorAll('details.log-section').forEach(d => {\n      const content = d.querySelector('.log-content').innerText.toLowerCase();\n      const title = d.querySelector('summary').innerText.toLowerCase();\n      if (!q || content.includes(q) || title.includes(q)) { d.classList.remove('hidden'); if (q) d.open = true; } else { d.classList.add('hidden'); }\n    });\n  }\n");
        sb.append("  function focusSection(title) {\n    const id = 'sec_' + title.replace(/ /g, '_').replace(/#/g, '').replace(/➔/g, 'to');\n    const el = document.getElementById(id);\n    if (el) { el.classList.remove('hidden'); el.open = true; el.scrollIntoView({ behavior: 'smooth', block: 'center' }); el.classList.add('highlight'); setTimeout(() => el.classList.remove('highlight'), 1500); }\n  }\n");
        sb.append("</script>\n</body>\n</html>");

        return sb.toString();
    }

    private static String renderTimeline(final TelemetryItem configItem, final List<TelemetryItem> testData, final Map<Integer, TelemetryItem> limboByPrevTest, final double tMax, final double lMax) {
        final StringBuilder sb = new StringBuilder();

        if (null != configItem) {
            final int pct = lMax > 0 ? Math.max(6, (int) Math.round((configItem.ms / lMax) * 100)) : 6;
            final String valStr = Math.round(configItem.ms) + "ms";
            sb.append("<div class=\"bar-col config\" onclick=\"focusSection('").append(configItem.title).append("')\" title=\"").append(configItem.title).append(": ").append(valStr).append("\">");
            sb.append("<div class=\"bar-val\">").append(valStr).append("</div>");
            sb.append("<div class=\"bar-track\"><div class=\"bar-fill\" style=\"height: ").append(pct).append("%; background: #64748b;\"></div></div>");
            sb.append("<div class=\"bar-lbl\">").append(configItem.label).append("</div></div>\n");
        }

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

        return sb.toString();
    }

    private record ArtifactItem(String rel, int test, int ms, double sec, String ext, String name) {}


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

    private static final java.util.regex.Pattern ARTIFACT_PAT = java.util.regex.Pattern.compile("(target/runs/[^/\\s\"&<>]+/[^/\\s\"&<>]+/)?((?:snapshots|data)/[^\"&<>\\r\\n]+?\\.(?:png|properties|log|json|html|txt))");
    private static final java.util.regex.Pattern SIBLING_REPORT_PAT = java.util.regex.Pattern.compile("(?<![/a-zA-Z0-9])(\\d{13}\\.(?:tree|run)\\.html)");

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
        return SIBLING_REPORT_PAT.matcher(sb.toString()).replaceAll("<a class=\"log-artifact-link\" href=\"$1\" target=\"_blank\" title=\"Open report: $1\">$1</a>");
    }

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

    private static String escapeHtml(final String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

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

    private static int extractLineSpan(final String line, final Pattern pTest, final Pattern pNoTest) {
        final String clean = ANSI_PATTERN.matcher(line).replaceAll("").trim();
        final Matcher m1 = pTest.matcher(clean);
        if (m1.matches()) {
            final String s = (null != m1.group(4)) ? m1.group(4).trim() : "";
            if (!s.isEmpty() && s.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(s);
            }
            return 0;
        }
        final Matcher m2 = pNoTest.matcher(clean);
        if (m2.matches()) {
            final String s = (null != m2.group(3)) ? m2.group(3).trim() : "";
            if (!s.isEmpty() && s.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(s);
            }
            return 0;
        }
        return 0;
    }

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
        final List<String> lines = new ArrayList<>();

        LogSection(final String type, final String title, final String status) {
            this.type = type;
            this.title = title;
            this.status = status;
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

        TelemetryItem(final String label, final String title, final double sec, final double ms, final String status, final boolean isLimbo, final int testNum) {
            this.label = label;
            this.title = title;
            this.sec = sec;
            this.ms = ms;
            this.status = status;
            this.isLimbo = isLimbo;
            this.testNum = testNum;
        }
    }

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
