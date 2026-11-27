package works.lysenko.base.output;

import works.lysenko.base.Parameters;
import works.lysenko.base.output.loghtml.LogModels.LogSection;
import works.lysenko.base.output.loghtml.LogModels.ScenEntry;
import works.lysenko.base.output.plaque.PlaqueHtmlRenderer;
import works.lysenko.base.output.plaque.PlaqueMetricsCalculator;
import works.lysenko.base.output.plaque.RunConfigParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        int off,
        int hidden,
        int notice,
        int notExecuted,
        int regression,
        int regressionWarning,
        boolean perNode
) {

    /**
     * A single visible statistics column for rendering in HTML or SVG plaques.
     */
    public record MetricColumn(
            String label,
            String headerClass,
            String cellClass,
            String colorHex,
            String title,
            int amount,
            String percentage
    ) {}

    public SummaryPlaque(
            final String suite,
            final String pool,
            final String domain,
            final String mode,
            final String pathsSummary,
            final String origin,
            final String tag,
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int upset,
            final int failed,
            final int knownIssues,
            final int off,
            final int hidden,
            final int notice,
            final int notExecuted,
            final boolean perNode
    ) {
        this(suite, pool, domain, mode, pathsSummary, origin, tag,
                total, executable, executed, passed, warning, upset, failed, knownIssues,
                off, hidden, notice, notExecuted, 0, 0, perNode);
    }

    public SummaryPlaque(
            final String suite,
            final String pool,
            final String domain,
            final String mode,
            final String pathsSummary,
            final String origin,
            final String tag,
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int upset,
            final int failed,
            final int knownIssues,
            final int off,
            final int hidden,
            final boolean perNode
    ) {
        this(suite, pool, domain, mode, pathsSummary, origin, tag,
                total, executable, executed, passed, warning, upset, failed, knownIssues,
                off, hidden, 0, Math.max(0, total - off - hidden - passed - warning - upset - failed), 0, 0, perNode);
    }

    public SummaryPlaque(
            final int total,
            final int executable,
            final int executed,
            final int passed,
            final int warning,
            final int failed
    ) {
        this(null, null, null, null, null, null, null, total, executable, executed, passed, warning, 0, failed, 0, Math.max(0, total - executable), 0, 0, Math.max(0, executable - executed), 0, 0, false);
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
        this(null, null, null, null, null, null, null, total, executable, executed, passed, warning, upset, failed, 0, Math.max(0, total - executable), 0, 0, Math.max(0, executable - executed), 0, 0, true);
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
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, 0, failed, 0, Math.max(0, total - executable), 0, 0, Math.max(0, executable - executed), 0, 0, false);
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
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, 0, failed, knownIssues, Math.max(0, total - executable), 0, 0, Math.max(0, executable - executed), 0, 0, false);
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
        this(suite, pool, domain, mode, pathsSummary, null, null, total, executable, executed, passed, warning, upset, failed, knownIssues, Math.max(0, total - executable), 0, 0, Math.max(0, executable - executed), 0, 0, true);
    }

    public int totalPassed() {
        return passed + hidden;
    }

    /**
     * Returns the visible columns for per-node tree plaques (static HTML and progression SVG).
     * {@code Total} is always included; the remaining columns partition {@code Total} into mutually
     * exclusive node states and are omitted whenever their value is {@code 0}.
     */
    public List<MetricColumn> visibleTreeColumns() {
        final List<MetricColumn> cols = new ArrayList<>(11);
        cols.add(new MetricColumn("Total", "col-total", "col-total", "#38BDF8", null, total, formatPct(total, total)));
        addIfPositive(cols, "Not Executable", "col-off", "col-off", "#64748B",
                "Scenarios that cannot be executed (weight 0 or unreachable)", off);
        addIfPositive(cols, "Not Executed", "col-not-executed", "col-not-executed", "#94A3B8",
                "Executable scenarios that have not been executed yet", notExecuted);
        addIfPositive(cols, "Hidden Leaves", "col-hidden", "col-hidden", "#6EE7B7",
                "Passed leaf scenarios hidden from the tree view", hidden);
        addIfPositive(cols, "Passed Nodes", "col-passed", "stat-passed", "#22C55E",
                "Visible scenario nodes that passed cleanly", passed);
        addIfPositive(cols, "Notice", "col-notice", "stat-notice", "#06B6D4",
                "Scenarios executed with notice events", notice);
        addIfPositive(cols, "Warning", "col-warning", "stat-warning", "#F59E0B",
                null, warning);
        addIfPositive(cols, "Reg. Warn", "col-regression-warning", "stat-regression-warning", "#F472B6",
                "Scenarios with warning-level regressions matching fixed.issues.json", regressionWarning);
        addIfPositive(cols, "Upset", "col-upset", "stat-upset", "#818CF8",
                "Scenarios whose execution passed, but a child failed downstream", upset);
        addIfPositive(cols, "Failed", "col-failed", "stat-failed", "#EF4444",
                null, failed);
        addIfPositive(cols, "Regression", "col-regression", "stat-regression", "#E879F9",
                "Scenarios that regressed on a previously fixed issue (fixed.issues.json)", regression);
        return cols;
    }

    private void addIfPositive(
            final List<MetricColumn> cols,
            final String label,
            final String headerClass,
            final String cellClass,
            final String colorHex,
            final String title,
            final int amount) {
        if (amount > 0) {
            cols.add(new MetricColumn(label, headerClass, cellClass, colorHex, title, amount, formatPct(amount, total)));
        }
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

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final java.util.Map<TreeHtml.NodeData, Boolean> executableCache,
            final java.util.Map<TreeHtml.NodeData, Boolean> childFailureCache) {
        return PlaqueMetricsCalculator.computeFromNodes(nodes, config, accessibleCtx, executableCache, childFailureCache);
    }

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final java.util.Map<TreeHtml.NodeData, Boolean> executableCache,
            final java.util.Map<TreeHtml.NodeData, Boolean> childFailureCache,
            final int hiddenCount) {
        return PlaqueMetricsCalculator.computeFromNodes(nodes, config, accessibleCtx, executableCache, childFailureCache, hiddenCount);
    }

    public static SummaryPlaque computeFromNodes(
            final List<TreeHtml.NodeData> nodes,
            final RunConfig config,
            final TreeHtml.AccessibleContext accessibleCtx,
            final java.util.Map<TreeHtml.NodeData, Boolean> executableCache,
            final java.util.Map<TreeHtml.NodeData, Boolean> childFailureCache,
            final Set<TreeHtml.NodeData> hiddenNodes) {
        return PlaqueMetricsCalculator.computeFromNodes(nodes, config, accessibleCtx, executableCache, childFailureCache, hiddenNodes);
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
