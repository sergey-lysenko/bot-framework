package works.lysenko.base.output.plaque;

import works.lysenko.Base;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.SummaryPlaque.RunConfig;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.util.apis.data._Result;

import java.util.List;
import java.util.Locale;

/**
 * Computes summary plaque statistics from scenario node trees, log sections,
 * or scenario entries.
 */
public final class PlaqueMetricsCalculator {

    private PlaqueMetricsCalculator() {
    }

    public static String formatPct(final int val, final int base) {
        if (0 == base) return "0%";
        final double pct = (val * 100.0) / base;
        if (pct == 0.0) return "0%";
        if (pct == 100.0) return "100%";
        return String.format(Locale.US, "%.1f%%", pct);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes) {
        final RunConfig config = RunConfigParser.fromParameters(Base.parameters);
        return computeFromNodes(nodes, config);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes, final RunConfig config) {
        int total = nodes.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int upset = 0;
        int failed = 0;
        int knownIssues = 0;

        final java.util.Set<works.lysenko.util.apis.scenario._Scenario> accessible = TreeHtml.getAccessibleScenarios();

        for (final TreeHtml.NodeData n : nodes) {
            if (!n.knownIssues().isEmpty()) {
                knownIssues++;
            }
            final boolean isExec = TreeHtml.isTheoreticallyExecutable(n, accessible);
            if (isExec) executable++;

            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;
            final int eventCount = (null != res && null != res.getEvents()) ? res.getEvents().size() : 0;
            final boolean failure = TreeHtml.hasFailure(n);
            final boolean childFailure = TreeHtml.hasChildFailure(n);

            if (failure) {
                failed++;
                if (execs > 0) executed++;
            } else if (childFailure) {
                upset++;
                if (execs > 0) executed++;
            } else if (execs > 0) {
                executed++;
                if (eventCount > 0) {
                    warning++;
                } else {
                    passed++;
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(), config.origin(), config.tag(),
                total, executable, executed, passed, warning, upset, failed, knownIssues, true);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections) {
        return computeFromSections(sections, null, null);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections, final String execConfigStr) {
        return computeFromSections(sections, execConfigStr, null);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections, final String execConfigStr, final String pathsSummary) {
        final RunConfig config = RunConfigParser.parse(execConfigStr, pathsSummary);
        int total = 0;
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int failed = 0;

        if (null != sections) {
            for (final LogSection sec : sections) {
                if ("test".equals(sec.type)) {
                    total++;
                    executable++;
                    executed++;
                    final String st = (null != sec.status) ? sec.status.toLowerCase(Locale.ROOT) : "";
                    if (st.contains("fail") || st.contains("error") || st.contains("severe")) {
                        failed++;
                    } else if (st.contains("warn") || st.contains("issue")) {
                        warning++;
                    } else {
                        passed++;
                    }
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(), config.origin(), config.tag(),
                total, executable, executed, passed, warning, 0, failed, 0, false);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries) {
        return computeFromScenEntries(entries, null, null);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr) {
        return computeFromScenEntries(entries, execConfigStr, null);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr, final String pathsSummary) {
        final RunConfig config = RunConfigParser.parse(execConfigStr, pathsSummary);
        int total = entries.size();
        int executable = 0;
        int executed = 0;
        int passed = 0;
        int warning = 0;
        int failed = 0;

        for (final ScenEntry e : entries) {
            int count = 0;
            try {
                final String cStr = e.count.contains(":") ? e.count.split(":")[0] : e.count;
                count = Integer.parseInt(cStr.trim());
            } catch (final Exception ignored) {
            }

            boolean isExec = true;
            if (null != e.weight) {
                final String cleanWeight = e.weight.replaceAll("[\\[\\]()]", "").trim();
                try {
                    isExec = Double.parseDouble(cleanWeight) != 0.0;
                } catch (final Exception ignored) {
                    isExec = !("0".equals(cleanWeight) || "0.0".equals(cleanWeight));
                }
            }
            if (isExec || count > 0) executable++;

            final String ev = (null != e.events) ? e.events.toLowerCase(Locale.ROOT) : "";
            final boolean isFail = ev.contains("fail") || ev.contains("error") || ev.contains("exception") || ev.contains("child_failed") || "child_failed".equalsIgnoreCase(e.sym);
            final boolean isWarn = ev.contains("event") || ev.contains("warn") || ev.contains("issue") || (!ev.isEmpty() && !isFail);

            if (isFail) {
                failed++;
                if (count > 0) executed++;
            } else if (count > 0) {
                executed++;
                if (isWarn) {
                    warning++;
                } else {
                    passed++;
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(), config.origin(), config.tag(),
                total, executable, executed, passed, warning, 0, failed, 0, false);
    }
}
