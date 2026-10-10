package works.lysenko.base.output.loghtml.parser;

import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.TelemetryItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static works.lysenko.base.output.loghtml.LogFormatting.calculateBootSpentSeconds;
import static works.lysenko.base.output.loghtml.LogFormatting.formatDeltaTime;
import static works.lysenko.base.output.loghtml.LogFormatting.toSeconds;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.data.strs.Swap.s;

@SuppressWarnings({"MethodWithMultipleLoops", "OverlyLongMethod", "ClassWithoutLogger", "CyclomaticComplexity", "NPathComplexity", "UtilityClass", "PublicInnerClass", "MagicNumber", "ObjectAllocationInLoop", "DuplicateStringLiteralInspection", "MultipleStringLiterals", "UtilityClassCanBeEnum"})
public final class LogMetrics {

    /**
     * Represents the result of metrics computation.
     */
    public static class MetricsResult {
        /** Config item. */
        public TelemetryItem configItem = null;
        /** Preflight item. */
        public TelemetryItem preflightItem = null;
        /** Postflight item. */
        public TelemetryItem postflightItem = null;
        /** Test data. */
        public final List<TelemetryItem> testData = new ArrayList<>(16);
        /** Limbo data. */
        public final List<TelemetryItem> limboData = new ArrayList<>(16);
        /** Limbo mapped by previous test. */
        public final Map<Integer, TelemetryItem> limboByPrevTest = new LinkedHashMap<>(16);
        
        /** Test min duration. */
        public double tMin = 0;
        /** Test max duration. */
        public double tMax = 0;
        /** Test total duration. */
        public double tTotal = 0;
        /** Test average duration. */
        public double tAvg = 0;

        /** Limbo min duration. */
        public double lMin = 0;
        /** Limbo max duration. */
        public double lMax = 0;
        /** Limbo total duration. */
        public double lTotal = 0;
        /** Limbo average duration. */
        public double lAvg = 0;

        /** Max duration in seconds. */
        public double maxSec = 1.0;
        
        /** Min test operation. */
        public int minTestOp = Integer.MAX_VALUE;
        /** Max test operation. */
        public int maxTestOp = Integer.MIN_VALUE;
    }

    private LogMetrics() {
    }

    /**
     * Computes metrics for the parsed sections.
     *
     * @param sections the list of parsed log sections
     * @return the computed metrics
     */
    public static MetricsResult compute(final List<LogSection> sections) {
        final MetricsResult r = new MetricsResult();

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
                r.limboData.add(item);
                r.limboByPrevTest.put(sec.prevTestNum, item);
            } else if (TEST.equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                final TelemetryItem item = new TelemetryItem(s("#", sec.testNum), sec.title, delta, delta * 1000.0, sec.status, false, sec.testNum);
                r.testData.add(item);
            } else if ("booting".equals(sec.type)) {
                final double bootSec = calculateBootSpentSeconds(sec.lines);
                sec.durationSec = bootSec;
                sec.durationStr = formatDeltaTime(bootSec);
            } else if ("configuring".equals(sec.type)) {
                final double confSec = calculateBootSpentSeconds(sec.lines);
                sec.durationSec = confSec;
                sec.durationStr = formatDeltaTime(confSec);
                r.configItem = new TelemetryItem("Config", "Configuring", confSec, confSec * 1000.0, sec.status, true, 0);
            } else if ("preflight".equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                r.preflightItem = new TelemetryItem("Preflight", "Preflight", delta, delta * 1000.0, sec.status, true, 0);
            } else if ("postflight".equals(sec.type)) {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.001, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
                r.postflightItem = new TelemetryItem("Postflight", "Postflight", delta, delta * 1000.0, sec.status, true, 0);
            } else {
                final double t0 = toSeconds(sec.startTime);
                final double t1 = toSeconds(sec.endTime);
                final double delta = Math.max(0.0, t1 - t0);
                sec.durationSec = delta;
                sec.durationStr = formatDeltaTime(delta);
            }
            
            if (TEST.equals(sec.type) || "limbo".equals(sec.type)) {
                if (sec.startOp != null && sec.startOp < r.minTestOp) r.minTestOp = sec.startOp;
                if (sec.endOp != null && sec.endOp > r.maxTestOp) r.maxTestOp = sec.endOp;
            }
        }

        r.tMin = r.testData.isEmpty() ? 0 : 999999;
        for (final TelemetryItem item : r.testData) {
            r.tMin = Math.min(r.tMin, item.sec);
            r.tMax = Math.max(r.tMax, item.sec);
            r.tTotal += item.sec;
        }
        if (r.testData.isEmpty()) r.tMin = 0;
        r.tAvg = r.testData.isEmpty() ? 0 : r.tTotal / r.testData.size();

        r.lMin = r.limboData.isEmpty() ? 0 : 999999;
        for (final TelemetryItem item : r.limboData) {
            r.lMin = Math.min(r.lMin, item.ms);
            r.lMax = Math.max(r.lMax, item.ms);
            r.lTotal += item.ms;
        }
        if (r.limboData.isEmpty()) r.lMin = 0;
        r.lAvg = r.limboData.isEmpty() ? 0 : r.lTotal / r.limboData.size();

        r.maxSec = r.tMax;
        if (null != r.configItem && r.configItem.sec > r.maxSec) r.maxSec = r.configItem.sec;
        if (null != r.preflightItem && r.preflightItem.sec > r.maxSec) r.maxSec = r.preflightItem.sec;
        if (null != r.postflightItem && r.postflightItem.sec > r.maxSec) r.maxSec = r.postflightItem.sec;
        if (r.maxSec <= 0.0) r.maxSec = 1.0;

        return r;
    }
}
