package works.lysenko.base.output.plaque;

import works.lysenko.Base;
import works.lysenko.base.output.SummaryPlaque;
import works.lysenko.base.output.SummaryPlaque.RunConfig;
import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.tree.NodeEventClassifier;
import works.lysenko.util.apis.data._Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        final TreeHtml.AccessibleContext accessibleCtx = new TreeHtml.AccessibleContext(TreeHtml.getAccessibleScenarios());
        return computeFromNodes(nodes, config, accessibleCtx, new HashMap<>(), new HashMap<>());
    }

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Map<TreeHtml.NodeData, Boolean> executableCache,
            final Map<TreeHtml.NodeData, Boolean> childFailureCache) {
        return computeFromNodes(nodes, config, accessibleCtx, executableCache, childFailureCache, Collections.emptySet());
    }

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Map<TreeHtml.NodeData, Boolean> executableCache,
            final Map<TreeHtml.NodeData, Boolean> childFailureCache,
            final int hiddenCount) {
        final SummaryPlaque base = computeFromNodes(nodes, config, accessibleCtx, executableCache, childFailureCache, Collections.emptySet());
        if (hiddenCount <= 0) {
            return base;
        }
        final int actualHidden = Math.min(hiddenCount, base.passed());
        return new SummaryPlaque(
                base.suite(), base.pool(), base.domain(), base.mode(), base.pathsSummary(), base.origin(), base.tag(),
                base.total(), base.executable(), base.executed(),
                base.passed() - actualHidden, base.warning(), base.upset(), base.failed(),
                base.knownIssues(), base.off(), actualHidden, base.notice(), base.notExecuted(),
                base.regression(), base.regressionWarning(), true);
    }

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final Map<TreeHtml.NodeData, Boolean> executableCache,
            final Map<TreeHtml.NodeData, Boolean> childFailureCache,
            final Set<TreeHtml.NodeData> hiddenNodes) {
        final int total = nodes.size();
        int executable = 0;
        int executed = 0;
        int off = 0;
        int notExecuted = 0;
        int hidden = 0;
        int passed = 0;
        int notice = 0;
        int warning = 0;
        int regressionWarning = 0;
        int upset = 0;
        int failed = 0;
        int regression = 0;
        int knownIssues = 0;

        final TreeHtml.AccessibleContext ctx = (null != accessibleCtx)
                ? accessibleCtx : new TreeHtml.AccessibleContext(TreeHtml.getAccessibleScenarios());
        final Map<TreeHtml.NodeData, Boolean> execCache = (null != executableCache) ? executableCache : new HashMap<>();
        final Map<TreeHtml.NodeData, Boolean> childFailCache = (null != childFailureCache) ? childFailureCache : new HashMap<>();
        final Set<TreeHtml.NodeData> hiddenSet = (null != hiddenNodes) ? hiddenNodes : Collections.emptySet();

        for (final TreeHtml.NodeData n : nodes) {
            if (!n.knownIssues().isEmpty()) {
                knownIssues++;
            }

            final NodeEventClassifier.Classification cls = NodeEventClassifier.classify(n);
            final boolean failure = cls.hasFailure();
            final boolean childFailure = !failure && TreeHtml.hasChildFailure(n, childFailCache);
            final _Result res = n.result();
            final int execs = (null != res) ? res.getExecutions() : 0;

            if (cls.hasRegression()) {
                regression++;
                executable++;
                if (execs > 0) executed++;
            } else if (failure) {
                failed++;
                executable++;
                if (execs > 0) executed++;
            } else if (childFailure) {
                upset++;
                executable++;
                if (execs > 0) executed++;
            } else if (execs > 0) {
                executable++;
                executed++;
                if (cls.hasRegressionWarning()) {
                    regressionWarning++;
                } else if (cls.hasWarning()) {
                    warning++;
                } else if (cls.hasNotice()) {
                    notice++;
                } else if (hiddenSet.contains(n)) {
                    hidden++;
                } else {
                    passed++;
                }
            } else {
                final boolean isExec = TreeHtml.isTheoreticallyExecutable(n, ctx, execCache);
                if (isExec) {
                    executable++;
                    notExecuted++;
                } else {
                    off++;
                }
            }
        }

        return new SummaryPlaque(
                config.suite(), config.pool(), config.domain(), config.mode(), config.pathsSummary(), config.origin(), config.tag(),
                total, executable, executed, passed, warning, upset, failed, knownIssues, off, hidden, notice, notExecuted,
                regression, regressionWarning, true);
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
        int regressionWarning = 0;
        int failed = 0;
        int regression = 0;

        if (null != sections) {
            for (final LogSection sec : sections) {
                if ("test".equals(sec.type)) {
                    total++;
                    executable++;
                    executed++;
                    final String st = (null != sec.status) ? sec.status.toLowerCase(Locale.ROOT) : "";
                    if ("regression".equals(st)) {
                        regression++;
                    } else if (st.contains("fail") || st.contains("error") || st.contains("severe")) {
                        failed++;
                    } else if ("regression_warning".equals(st)) {
                        regressionWarning++;
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
                total, executable, executed, passed, warning, 0, failed, 0, (total - executable), 0, 0, 0,
                regression, regressionWarning, false);
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
            } catch (final Exception _) {
            }

            boolean isExec = true;
            if (null != e.weight) {
                final String cleanWeight = e.weight.replaceAll("[\\[\\]()]", "").trim();
                try {
                    isExec = Double.parseDouble(cleanWeight) != 0.0;
                } catch (final Exception _) {
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
                total, executable, executed, passed, warning, 0, failed, 0, (total - executable), 0, false);
    }
}
