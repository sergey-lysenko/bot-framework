package works.lysenko.base.output;

import works.lysenko.base.Parameters;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.plaque.PlaqueHtmlRenderer;
import works.lysenko.base.output.plaque.PlaqueMetricsCalculator;
import works.lysenko.base.output.plaque.RunConfigParser;

import java.util.List;

/**
 * Record data model and entry point for summary statistics plaques.
 */
public record SummaryPlaque(
        String suite,
        String pool,
        String domain,
        String mode,
        String pathsSummary,
        String origin,
        String tag,
        int total,
        int executable,
        int executed,
        int passed,
        int warning,
        int upset,
        int failed,
        int knownIssues,
        boolean perNode
) {

    public SummaryPlaque(
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int failed
    ) {
        this(null, null, null, null, null, null, null, total, executable, executed, passed, warning, 0, failed, 0, false);
    }

    public SummaryPlaque(
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int upset,
            final int failed
    ) {
        this(null, null, null, null, null, null, null, total, executable, executed, passed, warning, upset, failed, 0, true);
    }

    public SummaryPlaque(
            final String suite,
            final String pool,
            final String domain,
            final String mode,
            final String pathsSummary,
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int failed
    ) {
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, 0, failed, 0, false);
    }

    public SummaryPlaque(
            final String suite,
            final String pool,
            final String domain,
            final String mode,
            final String pathsSummary,
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int failed,
            final int knownIssues
    ) {
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, 0, failed, knownIssues, false);
    }

    public SummaryPlaque(
            final String suite,
            final String pool,
            final String domain,
            final String mode,
            final String pathsSummary,
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int upset,
            final int failed,
            final int knownIssues
    ) {
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, upset, failed, knownIssues, true);
    }

    public record RunConfig(String suite, String pool, String domain, String mode, String pathsSummary, String origin, String tag) {
        public RunConfig(final String suite, final String pool, final String domain, final String mode, final String pathsSummary) {
            this(suite, pool, domain, mode, pathsSummary, null, null);
        }

        public RunConfig(final String suite, final String pool, final String domain, final String mode) {
            this(suite, pool, domain, mode, null);
        }

        public static RunConfig fromParameters(final Parameters parameters) {
            return RunConfigParser.fromParameters(parameters);
        }

        public static RunConfig parse(final String logLine, final String pathsSummary) {
            return RunConfigParser.parse(logLine, pathsSummary);
        }

        public static RunConfig parse(final String logLine) {
            return RunConfigParser.parse(logLine);
        }
    }

    public static String cleanPathsSummary(final String pathsExecutedStr, final String pathsChanceStr, final String pathsPossibleStr) {
        return RunConfigParser.cleanPathsSummary(pathsExecutedStr, pathsChanceStr, pathsPossibleStr);
    }

    public static String formatPct(final int val, final int base) {
        return PlaqueMetricsCalculator.formatPct(val, base);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes) {
        return PlaqueMetricsCalculator.computeFromNodes(nodes);
    }

    public static SummaryPlaque computeFromNodes(final List<TreeHtml.NodeData> nodes, final RunConfig config) {
        return PlaqueMetricsCalculator.computeFromNodes(nodes, config);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections) {
        return PlaqueMetricsCalculator.computeFromSections(sections);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections, final String execConfigStr) {
        return PlaqueMetricsCalculator.computeFromSections(sections, execConfigStr);
    }

    public static SummaryPlaque computeFromSections(final List<LogSection> sections, final String execConfigStr, final String pathsSummary) {
        return PlaqueMetricsCalculator.computeFromSections(sections, execConfigStr, pathsSummary);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries) {
        return PlaqueMetricsCalculator.computeFromScenEntries(entries);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr) {
        return PlaqueMetricsCalculator.computeFromScenEntries(entries, execConfigStr);
    }

    public static SummaryPlaque computeFromScenEntries(final List<ScenEntry> entries, final String execConfigStr, final String pathsSummary) {
        return PlaqueMetricsCalculator.computeFromScenEntries(entries, execConfigStr, pathsSummary);
    }

    public String renderHtml() {
        return PlaqueHtmlRenderer.renderHtml(this);
    }

    public static String css() {
        return PlaqueHtmlRenderer.css();
    }
}
