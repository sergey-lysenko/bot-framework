package works.lysenko.base.output.scenario;

import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.output.svg.ProgressionChrome;
import works.lysenko.base.output.tree.TreeProgressMetrics;
import works.lysenko.base.output.tree.TreeTheme;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.data._Event;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.Result;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static works.lysenko.Base.core;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Generates unified dark-slate SVG frames for scenario execution progression graphs,
 * sharing header chrome, bordered badges, ETA readout, and status palette with Tree Progression.
 */
public final class ScenarioGraphSvgRenderer {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 680;
    static final int DENSE_SCENARIO_THRESHOLD = 45;
    private static final int MAX_HIERARCHICAL_GROUPS = 28;
    private static final int AUTO_MIN_GROUPS = 5;

    private enum BarStatus {
        UNVISITED("Pending", ProgressionChrome.SLATE_FILL, ProgressionChrome.TEXT_DIM),
        IN_PROGRESS("Partial", "#2B834A", "#6EE7B7"),
        PASSED("Passed", ProgressionChrome.ACCENT_GREEN, ProgressionChrome.ACCENT_GREEN_TEXT),
        NOTICE("Notice", ProgressionChrome.ACCENT_CYAN, ProgressionChrome.ACCENT_CYAN_TEXT),
        WARNING("Warning", ProgressionChrome.ACCENT_AMBER, ProgressionChrome.ACCENT_AMBER_TEXT),
        FAILED("Failed", ProgressionChrome.ACCENT_RED, ProgressionChrome.ACCENT_RED_TEXT);

        private final String label;
        private final String barHex;
        private final String textHex;

        BarStatus(final String label, final String barHex, final String textHex) {
            this.label = label;
            this.barHex = barHex;
            this.textHex = textHex;
        }
    }

    private record BranchGroup(
            String label,
            int startIdx,
            int endIdx,
            int completed,
            int total,
            boolean hasFailure,
            boolean hasWarning) {
    }

    private ScenarioGraphSvgRenderer() {
    }

    /**
     * Renders a scenario progression frame without active-cycle highlights.
     *
     * @param scenarios    ordered list of accessible leaf scenarios
     * @param target       target executions per repeatable leaf
     * @param currentCycle current 1-based test cycle number
     * @return complete SVG document string
     */
    public static String renderProgressionGraphSvg(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {
        return renderProgressionGraphSvg(scenarios, target, currentCycle, Collections.emptySet());
    }

    /**
     * Immutable snapshot of scenario execution progress captured on the main thread so
     * SVG rendering can execute safely on a background worker thread.
     */
    public static final class FrameSnapshot {
        private final int count;
        private final int safeTarget;
        private final int currentCycle;
        private final int completedCount;
        private final int totalExecs;
        private final int maxExecs;
        private final int passedCount;
        private final int warningCount;
        private final int failedCount;
        private final int noticeCount;
        private final int inProgressCount;
        private final int pendingCount;
        private final int[] execsArray;
        private final int[] targetsArray;
        private final BarStatus[] statuses;
        private final boolean[] activeFlags;
        private final String[] rawShortNames;
        private final String commonPrefix;
        private final String[] displayNames;
        private final int labelLevel;
        private final String elapsedBadge;
        private final ProgressionChrome.EtaBadge etaBadge;

        private FrameSnapshot(
                final int count,
                final int safeTarget,
                final int currentCycle,
                final int completedCount,
                final int totalExecs,
                final int maxExecs,
                final int passedCount,
                final int warningCount,
                final int failedCount,
                final int noticeCount,
                final int inProgressCount,
                final int pendingCount,
                final int[] execsArray,
                final int[] targetsArray,
                final BarStatus[] statuses,
                final boolean[] activeFlags,
                final String[] rawShortNames,
                final String commonPrefix,
                final String[] displayNames,
                final int labelLevel,
                final String elapsedBadge,
                final ProgressionChrome.EtaBadge etaBadge) {
            this.count = count;
            this.safeTarget = safeTarget;
            this.currentCycle = currentCycle;
            this.completedCount = completedCount;
            this.totalExecs = totalExecs;
            this.maxExecs = maxExecs;
            this.passedCount = passedCount;
            this.warningCount = warningCount;
            this.failedCount = failedCount;
            this.noticeCount = noticeCount;
            this.inProgressCount = inProgressCount;
            this.pendingCount = pendingCount;
            this.execsArray = execsArray;
            this.targetsArray = targetsArray;
            this.statuses = statuses;
            this.activeFlags = activeFlags;
            this.rawShortNames = rawShortNames;
            this.commonPrefix = commonPrefix;
            this.displayNames = displayNames;
            this.labelLevel = labelLevel;
            this.elapsedBadge = elapsedBadge;
            this.etaBadge = etaBadge;
        }
    }

    /**
     * Captures an immutable {@link FrameSnapshot} of current scenario execution metrics on the caller thread.
     */
    public static FrameSnapshot captureSnapshot(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle,
            final Set<String> recentScenarioKeys) {

        final Set<String> recentKeys = (null != recentScenarioKeys) ? recentScenarioKeys : Collections.emptySet();
        final int count = (null != scenarios) ? scenarios.size() : 0;
        final int safeTarget = Math.max(1, target);

        int completedCount = 0;
        int totalExecs = 0;
        int maxExecs = safeTarget;
        int passedCount = 0;
        int warningCount = 0;
        int failedCount = 0;
        int noticeCount = 0;
        int inProgressCount = 0;
        int pendingCount = 0;

        final int[] execsArray = new int[count];
        final int[] targetsArray = new int[count];
        final BarStatus[] statuses = new BarStatus[count];
        final boolean[] activeFlags = new boolean[count];
        final String[] rawShortNames = new String[count];

        for (int i = 0; i < count; i++) {
            final _Scenario sc = scenarios.get(i);
            final int scTarget = (sc instanceof Mono) ? 1 : safeTarget;
            targetsArray[i] = scTarget;
            rawShortNames[i] = safeShortName(sc);

            final Result res = (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getResult(sc) : null;
            final int execs = (null != res)
                    ? res.getExecutions()
                    : ((isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getExecutions(sc) : 0);
            execsArray[i] = execs;
            totalExecs += execs;
            if (execs >= scTarget) completedCount++;
            if (execs > maxExecs) maxExecs = execs;

            final BarStatus st = resolveBarStatus(res, execs, scTarget);
            statuses[i] = st;
            switch (st) {
                case FAILED -> failedCount++;
                case WARNING -> warningCount++;
                case NOTICE -> noticeCount++;
                case PASSED -> passedCount++;
                case IN_PROGRESS -> inProgressCount++;
                case UNVISITED -> pendingCount++;
            }

            activeFlags[i] = isRecent(sc, recentKeys);
        }

        final String commonPrefix = findCommonDotPrefix(rawShortNames);
        final String[] displayNames = new String[count];
        for (int i = 0; i < count; i++) {
            displayNames[i] = stripPrefix(rawShortNames[i], commonPrefix);
        }

        return new FrameSnapshot(
                count,
                safeTarget,
                currentCycle,
                completedCount,
                totalExecs,
                maxExecs,
                passedCount,
                warningCount,
                failedCount,
                noticeCount,
                inProgressCount,
                pendingCount,
                execsArray,
                targetsArray,
                statuses,
                activeFlags,
                rawShortNames,
                commonPrefix,
                displayNames,
                ProgressionSettings.current().scenarioLabelLevel(),
                ProgressionChrome.formatElapsedBadge(),
                ProgressionChrome.resolveEtaBadge());
    }

    /**
     * Renders a scenario progression frame with active-cycle highlights.
     *
     * @param scenarios          ordered list of accessible leaf scenarios
     * @param target             target executions per repeatable leaf
     * @param currentCycle       current 1-based test cycle number
     * @param recentScenarioKeys keys ({@code getName()} or {@code getShortName()}) executed in the current cycle
     * @return complete SVG document string
     */
    public static String renderProgressionGraphSvg(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle,
            final Set<String> recentScenarioKeys) {
        return renderProgressionGraphSvg(captureSnapshot(scenarios, target, currentCycle, recentScenarioKeys));
    }

    /**
     * Renders a scenario progression SVG from an already-captured immutable {@link FrameSnapshot}.
     *
     * @param snapshot immutable scenario frame snapshot
     * @return complete SVG document string
     */
    public static String renderProgressionGraphSvg(final FrameSnapshot snapshot) {
        final int count = snapshot.count;
        final int safeTarget = snapshot.safeTarget;
        final int currentCycle = snapshot.currentCycle;
        final int completedCount = snapshot.completedCount;
        final int totalExecs = snapshot.totalExecs;
        final int maxExecs = snapshot.maxExecs;
        final int passedCount = snapshot.passedCount;
        final int warningCount = snapshot.warningCount;
        final int failedCount = snapshot.failedCount;
        final int noticeCount = snapshot.noticeCount;
        final int inProgressCount = snapshot.inProgressCount;
        final int pendingCount = snapshot.pendingCount;
        final int[] execsArray = snapshot.execsArray;
        final int[] targetsArray = snapshot.targetsArray;
        final BarStatus[] statuses = snapshot.statuses;
        final boolean[] activeFlags = snapshot.activeFlags;
        final String[] rawShortNames = snapshot.rawShortNames;
        final String commonPrefix = snapshot.commonPrefix;
        final String[] displayNames = snapshot.displayNames;

        final boolean denseMode = count > DENSE_SCENARIO_THRESHOLD;
        final int headerH = ProgressionChrome.HEADER_HEIGHT_BASE;
        final int marginX = 24;

        final StringBuilder sb = new StringBuilder(Math.max(4096, count * 280));
        sb.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                WIDTH, HEIGHT, WIDTH, HEIGHT));
        sb.append("  <defs>\n");
        ProgressionChrome.appendDotGridPattern(sb, headerH);
        sb.append("  </defs>\n");
        sb.append("  <style>\n");
        sb.append("    .tx1 { font-family: monospace; font-size: 10px; font-weight: bold; }\n");
        sb.append("    .tx2 { font-family: sans-serif; font-size: 11px; }\n");
        sb.append("    .tx3 { font-family: monospace; font-weight: bold; font-size: 11px; }\n");
        sb.append("    .tx4 { font-family: sans-serif; font-weight: bold; font-size: 15px; fill: #F8FAFC; }\n");
        sb.append("    .tx-muted { font-family: sans-serif; font-size: 10px; fill: #94A3B8; }\n");
        sb.append("    .tx-branch { font-family: sans-serif; font-size: 10px; font-weight: bold; }\n");
        sb.append("    .tx-sub { font-family: monospace; font-size: 9px; fill: #64748B; }\n");
        sb.append("  </style>\n");

        ProgressionChrome.appendCanvasBackground(sb, WIDTH, HEIGHT, headerH);

        // Unified HUD Header Bar
        sb.append("  <g id=\"progression-header\">\n");
        ProgressionChrome.appendHeaderBar(sb, WIDTH);

        int rightX = WIDTH - marginX;
        final int topY = (headerH - ProgressionChrome.BADGE_HEIGHT) / 2;

        // Right-to-left placement -> left-to-right display:
        // Test #N | Leafs: X/Y (P%) | Target: T | M execs | ETA: MM:SS | Elapsed: HH:MM:SS
        rightX = ProgressionChrome.appendBorderedBadge(sb, snapshot.elapsedBadge, rightX, topY, ProgressionChrome.BADGE_SLATE);
        final ProgressionChrome.EtaBadge etaBadge = snapshot.etaBadge;
        rightX = ProgressionChrome.appendBorderedBadge(sb, etaBadge.text(), rightX, topY, etaBadge.style());
        rightX = ProgressionChrome.appendBorderedBadge(sb, totalExecs + " execs", rightX, topY, ProgressionChrome.BADGE_SLATE);
        rightX = ProgressionChrome.appendBorderedBadge(sb, "Target: " + safeTarget, rightX, topY, ProgressionChrome.BADGE_SLATE);
        rightX = ProgressionChrome.appendBorderedBadge(
                sb,
                ProgressionChrome.formatLeafsBadge(completedCount, count),
                rightX,
                topY,
                ProgressionChrome.leafsBadgeStyle(completedCount, count));
        ProgressionChrome.appendBorderedBadge(sb, "Test #" + currentCycle, rightX, topY, ProgressionChrome.BADGE_SKY);

        ProgressionChrome.appendHeaderTitle(sb, marginX, "Scenario Execution Progression");
        sb.append("  </g>\n");

        // Compact Status Legend Strip below Header (y = 70..94)
        final boolean hasActive = anyTrue(activeFlags);
        appendLegendStrip(sb, marginX, WIDTH - marginX, 82,
                passedCount, inProgressCount, noticeCount, warningCount, failedCount, pendingCount,
                hasActive, commonPrefix);

        if (count == 0) {
            sb.append(String.format(Locale.US,
                    "  <text class=\"tx2\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">No accessible leaf scenarios</text>\n",
                    WIDTH / 2, HEIGHT / 2, ProgressionChrome.TEXT_MUTED));
            sb.append("</svg>\n");
            return sb.toString();
        }

        // Chart Geometry
        final int chartX = 56;
        final int chartY = 104;
        final int chartW = WIDTH - chartX - marginX;
        final int footerY = 636;
        final int footerH = 30;
        final double slotW = (double) chartW / count;

        // Compute branch groups for dense mode and determine whether labels need 1 or 2 staggered rows
        final List<BranchGroup> branchGroups = denseMode
                ? computeBranchGroups(rawShortNames, commonPrefix, snapshot.labelLevel, execsArray, targetsArray, statuses)
                : Collections.emptyList();
        final boolean denseNeedsTwoRows = denseMode && requiresSecondLabelRow(branchGroups, chartX, chartW, slotW);
        final int chartH = denseMode ? (denseNeedsTwoRows ? 444 : 472) : 376;
        final int chartBottom = chartY + chartH;
        final int maxY = Math.max(safeTarget + 1, maxExecs + 1);

        // Subtle chart frame backdrop
        sb.append(String.format(Locale.US,
                "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"6\" ry=\"6\" fill=\"#0B1220\" fill-opacity=\"0.55\" stroke=\"#1E293B\" stroke-width=\"1\" />\n",
                chartX, chartY, chartW, chartH));

        // Branch vertical alternating bands (behind gridlines & bars)
        if (denseMode && !branchGroups.isEmpty()) {
            sb.append("  <g id=\"branch-bands\">\n");
            for (int g = 0; g < branchGroups.size(); g++) {
                final BranchGroup grp = branchGroups.get(g);
                final double gx1 = chartX + grp.startIdx() * slotW;
                final double gx2 = chartX + (grp.endIdx() + 1) * slotW;
                if (g % 2 == 1) {
                    sb.append(String.format(Locale.US,
                            "    <rect x=\"%.1f\" y=\"%d\" width=\"%.1f\" height=\"%d\" fill=\"#1E293B\" fill-opacity=\"0.24\" />\n",
                            gx1, chartY, Math.max(0.0, gx2 - gx1), chartH));
                }
                if (g > 0) {
                    sb.append(String.format(Locale.US,
                            "    <line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"#334155\" stroke-opacity=\"0.42\" stroke-width=\"1\" />\n",
                            gx1, chartY, gx1, chartBottom));
                }
            }
            sb.append("  </g>\n");
        }

        // Horizontal Y-Axis Gridlines & Ticks
        final int ySteps = Math.min(maxY, 8);
        final int stepVal = Math.max(1, (int) Math.ceil((double) maxY / ySteps));
        sb.append("  <g id=\"y-axis\">\n");
        for (int v = 0; v <= maxY; v += stepVal) {
            final int yPos = chartBottom - (int) Math.round(((double) v / maxY) * chartH);
            sb.append(String.format(Locale.US,
                    "    <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#1E293B\" stroke-opacity=\"0.75\" stroke-width=\"1\" />\n",
                    chartX, yPos, chartX + chartW, yPos));
            sb.append(String.format(Locale.US,
                    "    <text class=\"tx1\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"end\">%d</text>\n",
                    chartX - 10, yPos + 3, ProgressionChrome.TEXT_DIM, v));
        }
        sb.append("  </g>\n");

        // Target Dashed Line
        final int targetY = chartBottom - (int) Math.round(((double) safeTarget / maxY) * chartH);
        if (safeTarget <= maxY) {
            sb.append(String.format(Locale.US,
                    "  <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"%s\" stroke-opacity=\"0.75\" stroke-width=\"1.5\" stroke-dasharray=\"6 4\" />\n",
                    chartX, targetY, chartX + chartW, targetY, ProgressionChrome.ACCENT_SKY));
        }

        // Bars
        final int barW = Math.clamp((int) Math.floor(slotW * 0.72), 2, 28);
        final int barRx = (barW >= 6) ? 3 : 1;
        final boolean showAllCounts = !denseMode && barW >= 12;
        int lastLabeledBarCenterX = -100;

        sb.append("  <g id=\"scenario-bars\">\n");
        for (int i = 0; i < count; i++) {
            final int execs = execsArray[i];
            final int scTarget = targetsArray[i];
            final BarStatus st = statuses[i];
            final boolean active = activeFlags[i];

            final double centerXd = chartX + i * slotW + slotW / 2.0;
            final int centerX = (int) Math.round(centerXd);
            final int barLeft = centerX - (barW / 2);

            final String tooltip = String.format(Locale.US, "%s — %d/%d (%s)",
                    rawShortNames[i], execs, scTarget, st.label);

            sb.append("    <g>\n");
            sb.append("      <title>").append(ProgressionChrome.escapeXml(tooltip)).append("</title>\n");

            if (active) {
                final double glowW = Math.max(barW + 4.0, slotW);
                final double glowX = Math.max(chartX, centerXd - glowW / 2.0);
                sb.append(String.format(Locale.US,
                        "      <rect x=\"%.1f\" y=\"%d\" width=\"%.1f\" height=\"%d\" fill=\"%s\" fill-opacity=\"0.14\" />\n",
                        glowX, chartY, Math.min(glowW, chartX + chartW - glowX), chartH, ProgressionChrome.ACCENT_SKY));
            } else if (!denseMode) {
                sb.append(String.format(Locale.US,
                        "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"%d\" fill=\"#1E293B\" fill-opacity=\"0.35\" />\n",
                        barLeft, chartY, barW, chartH, barRx));
            }

            int barTop = chartBottom - 2;
            if (execs > 0) {
                final int barH = Math.max(4, (int) Math.round(((double) execs / maxY) * chartH));
                barTop = chartBottom - barH;
                final String fillHex = resolveBarFillHex(st, execs, scTarget);
                if (active) {
                    sb.append(String.format(Locale.US,
                            "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.2\" />\n",
                            barLeft, barTop, barW, barH, barRx, fillHex, ProgressionChrome.ACCENT_SKY));
                } else {
                    sb.append(String.format(Locale.US,
                            "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"%d\" fill=\"%s\" />\n",
                            barLeft, barTop, barW, barH, barRx, fillHex));
                }
            } else {
                sb.append(String.format(Locale.US,
                        "      <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"2\" fill=\"%s\" />\n",
                        barLeft, chartBottom - 2, barW, ProgressionChrome.SLATE_FILL));
            }

            if (active) {
                final int triY = Math.max(chartY + 8, barTop - 4);
                sb.append(String.format(Locale.US,
                        "      <polygon points=\"%d,%d %d,%d %d,%d\" fill=\"%s\" />\n",
                        centerX, triY, centerX - 4, triY - 6, centerX + 4, triY - 6, ProgressionChrome.ACCENT_SKY));
            }

            // Numeric counts: all bars in sparse mode; only active or over-executed in dense mode (collision-guarded)
            final boolean showCount = showAllCounts || (execs > 0 && (active || execs > scTarget));
            if (showCount && (showAllCounts || centerX - lastLabeledBarCenterX >= 18)) {
                final int labelOffset = active ? 13 : 5;
                final int valY = Math.max(chartY + 12, barTop - labelOffset);
                final String valColor = active ? ProgressionChrome.ACCENT_SKY_TEXT : st.textHex;
                sb.append(String.format(Locale.US,
                        "      <text class=\"tx1\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">%d</text>\n",
                        centerX, valY, valColor, execs));
                lastLabeledBarCenterX = centerX;
            }
            sb.append("    </g>\n");
        }
        sb.append("  </g>\n");

        // Target Pill Label (drawn above bars so nothing collides with it)
        if (safeTarget <= maxY) {
            final String tag = "Target: " + safeTarget;
            final int tagW = (int) Math.ceil(tag.length() * 6.2) + 14;
            final int tagH = 18;
            final int tagX = chartX + chartW - tagW - 8;
            final int tagY = Math.max(chartY + 4, targetY - tagH - 4);
            sb.append(String.format(Locale.US,
                    "  <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"4\" ry=\"4\" fill=\"%s\" fill-opacity=\"0.92\" stroke=\"%s\" stroke-opacity=\"0.65\" stroke-width=\"1\" />\n",
                    tagX, tagY, tagW, tagH, ProgressionChrome.BG_CANVAS, ProgressionChrome.ACCENT_SKY));
            sb.append(String.format(Locale.US,
                    "  <text class=\"tx1\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
                    tagX + (tagW / 2), tagY + 12, ProgressionChrome.ACCENT_SKY_TEXT, tag));
        }

        // Adaptive X-Axis: Branch Brackets (Dense) vs Clean Diagonal Labels (Sparse)
        if (denseMode) {
            appendBranchBracketsAxis(sb, branchGroups, chartX, chartW, chartBottom, slotW);
        } else {
            appendSparseDiagonalAxis(sb, displayNames, execsArray, targetsArray, activeFlags, chartX, chartBottom, footerY - 8, slotW);
        }

        // Active-Cycle Footer Strip
        appendActiveFooter(sb, marginX, footerY, WIDTH - (marginX * 2), footerH,
                displayNames, execsArray, targetsArray, statuses, activeFlags,
                count, branchGroups.size(), maxExecs, denseMode);

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static void appendLegendStrip(
            final StringBuilder sb,
            final int leftX,
            final int rightX,
            final int baselineY,
            final int passed,
            final int inProgress,
            final int notice,
            final int warning,
            final int failed,
            final int pending,
            final boolean hasActive,
            final String commonPrefix) {

        sb.append("  <g id=\"status-legend\">\n");
        int x = leftX;
        x = appendLegendItem(sb, x, baselineY, ProgressionChrome.ACCENT_GREEN, "Passed " + passed);
        if (inProgress > 0) {
            x = appendLegendItem(sb, x, baselineY, "#2B834A", "Partial " + inProgress);
        }
        if (notice > 0) {
            x = appendLegendItem(sb, x, baselineY, ProgressionChrome.ACCENT_CYAN, "Notice " + notice);
        }
        x = appendLegendItem(sb, x, baselineY, ProgressionChrome.ACCENT_AMBER, "Warning " + warning);
        x = appendLegendItem(sb, x, baselineY, ProgressionChrome.ACCENT_RED, "Failed " + failed);
        x = appendLegendItem(sb, x, baselineY, ProgressionChrome.SLATE_BORDER, "Pending " + pending);
        if (hasActive) {
            appendLegendItem(sb, x, baselineY, ProgressionChrome.ACCENT_SKY, "Active");
        }

        if (null != commonPrefix && !commonPrefix.isEmpty()) {
            final String rootDisplay = commonPrefix.endsWith(".")
                    ? commonPrefix.substring(0, commonPrefix.length() - 1)
                    : commonPrefix;
            sb.append(String.format(Locale.US,
                    "    <text class=\"tx1\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"end\">Root: %s</text>\n",
                    rightX, baselineY, ProgressionChrome.TEXT_DIM, ProgressionChrome.escapeXml(rootDisplay)));
        }
        sb.append("  </g>\n");
    }

    private static int appendLegendItem(
            final StringBuilder sb,
            final int x,
            final int baselineY,
            final String dotColor,
            final String label) {
        sb.append(String.format(Locale.US,
                "    <circle cx=\"%d\" cy=\"%d\" r=\"3.5\" fill=\"%s\" />\n",
                x + 4, baselineY - 3, dotColor));
        sb.append(String.format(Locale.US,
                "    <text class=\"tx-muted\" x=\"%d\" y=\"%d\">%s</text>\n",
                x + 12, baselineY, ProgressionChrome.escapeXml(label)));
        return x + 12 + (int) Math.ceil(label.length() * 6.0) + 16;
    }

    private static boolean requiresSecondLabelRow(
            final List<BranchGroup> groups,
            final int chartX,
            final int chartW,
            final double slotW) {
        double row0Right = chartX - 10.0;
        for (final BranchGroup grp : groups) {
            final double gx1 = chartX + grp.startIdx() * slotW + 2.0;
            final double gx2 = chartX + (grp.endIdx() + 1) * slotW - 2.0;
            final double groupW = Math.max(2.0, gx2 - gx1);
            final double midX = (gx1 + gx2) / 2.0;
            final String label = truncate(grp.label(), Math.clamp((int) Math.floor((groupW + 56.0) / 6.2), 10, 20));
            final String sub = grp.completed() + "/" + grp.total();
            final double halfPx = Math.max(label.length() * 6.0, sub.length() * 5.6) / 2.0;
            final double clampedMidX = Math.max(chartX + halfPx, Math.min(chartX + chartW - halfPx, midX));
            if (clampedMidX - halfPx < row0Right + 8.0) {
                return true;
            }
            row0Right = clampedMidX + halfPx;
        }
        return false;
    }

    private static void appendBranchBracketsAxis(
            final StringBuilder sb,
            final List<BranchGroup> groups,
            final int chartX,
            final int chartW,
            final int chartBottom,
            final double slotW) {

        sb.append("  <g id=\"branch-axis\">\n");
        final int bracketY = chartBottom + 10;
        final int[] rowTextY = {chartBottom + 27, chartBottom + 54};
        final int[] rowSubY = {chartBottom + 38, chartBottom + 65};
        final double[] rowRightEdge = {chartX - 10.0, chartX - 10.0};

        for (final BranchGroup grp : groups) {
            final double gx1 = chartX + grp.startIdx() * slotW + 2.0;
            final double gx2 = chartX + (grp.endIdx() + 1) * slotW - 2.0;
            final double groupW = Math.max(2.0, gx2 - gx1);
            final double midX = (gx1 + gx2) / 2.0;

            final String bracketColor;
            if (grp.hasFailure()) {
                bracketColor = ProgressionChrome.ACCENT_RED;
            } else if (grp.completed() == grp.total() && grp.total() > 0) {
                bracketColor = ProgressionChrome.ACCENT_GREEN;
            } else if (grp.hasWarning()) {
                bracketColor = ProgressionChrome.ACCENT_AMBER;
            } else {
                bracketColor = ProgressionChrome.SLATE_BORDER;
            }

            // Bracket span under group
            if (groupW >= 6.0) {
                sb.append(String.format(Locale.US,
                        "    <path d=\"M %.1f %d L %.1f %d L %.1f %d L %.1f %d\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.2\" stroke-opacity=\"0.85\" />\n",
                        gx1, bracketY - 4, gx1, bracketY, gx2, bracketY, gx2, bracketY - 4, bracketColor));
            } else {
                sb.append(String.format(Locale.US,
                        "    <line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"%s\" stroke-width=\"1.2\" />\n",
                        midX, bracketY - 4, midX, bracketY + 2, bracketColor));
            }

            // Pick a collision-free staggered horizontal label row
            final String label = truncate(grp.label(), Math.clamp((int) Math.floor((groupW + 56.0) / 6.2), 10, 20));
            final String sub = grp.completed() + "/" + grp.total();
            final double labelPx = Math.max(label.length() * 6.0, sub.length() * 5.6);
            final double halfPx = labelPx / 2.0;
            final double clampedMidX = Math.max(chartX + halfPx, Math.min(chartX + chartW - halfPx, midX));
            final double leftEdge = clampedMidX - halfPx;
            final double rightEdge = clampedMidX + halfPx;

            int chosenRow = -1;
            if (leftEdge >= rowRightEdge[0] + 8.0) {
                chosenRow = 0;
            } else if (leftEdge >= rowRightEdge[1] + 8.0) {
                chosenRow = 1;
            }

            if (chosenRow >= 0) {
                rowRightEdge[chosenRow] = rightEdge;
                if (chosenRow == 1) {
                    sb.append(String.format(Locale.US,
                            "    <line x1=\"%.1f\" y1=\"%d\" x2=\"%.1f\" y2=\"%d\" stroke=\"%s\" stroke-opacity=\"0.45\" stroke-width=\"1\" stroke-dasharray=\"2 2\" />\n",
                            midX, bracketY + 1, midX, rowTextY[1] - 11, bracketColor));
                }
                final String textFill = (grp.completed() == grp.total() && grp.total() > 0)
                        ? ProgressionChrome.ACCENT_GREEN_TEXT
                        : (grp.completed() > 0 ? ProgressionChrome.TEXT_PRIMARY : ProgressionChrome.TEXT_MUTED);
                sb.append(String.format(Locale.US,
                        "    <text class=\"tx-branch\" x=\"%.1f\" y=\"%d\" fill=\"%s\" text-anchor=\"middle\">%s</text>\n",
                        clampedMidX, rowTextY[chosenRow], textFill, ProgressionChrome.escapeXml(label)));
                sb.append(String.format(Locale.US,
                        "    <text class=\"tx-sub\" x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%s</text>\n",
                        clampedMidX, rowSubY[chosenRow], sub));
            }
        }
        sb.append("  </g>\n");
    }

    private static void appendSparseDiagonalAxis(
            final StringBuilder sb,
            final String[] displayNames,
            final int[] execsArray,
            final int[] targetsArray,
            final boolean[] activeFlags,
            final int chartX,
            final int chartBottom,
            final int maxBottomY,
            final double slotW) {

        sb.append("  <g id=\"scenario-labels\">\n");
        final double angleDeg = -45.0;
        final double rad = Math.toRadians(Math.abs(angleDeg));
        final double cosA = Math.cos(rad);
        final double sinA = Math.sin(rad);

        for (int i = 0; i < displayNames.length; i++) {
            final int centerX = (int) Math.round(chartX + i * slotW + slotW / 2.0);
            final int labelY = chartBottom + 12;
            final String nameColor = activeFlags[i]
                    ? ProgressionChrome.ACCENT_SKY_TEXT
                    : (execsArray[i] >= targetsArray[i]
                    ? ProgressionChrome.TEXT_PRIMARY
                    : (execsArray[i] > 0 ? ProgressionChrome.TEXT_SECONDARY : ProgressionChrome.TEXT_DIM));

            final int maxWX = (int) Math.floor((centerX - 16.0) / cosA);
            final int maxWY = (int) Math.floor((maxBottomY - labelY) / sinA);
            final int maxAllowedChars = Math.max(6, Math.min(maxWX, maxWY) / 6);

            String label = displayNames[i];
            if (label.length() > maxAllowedChars) {
                label = "…" + label.substring(label.length() - maxAllowedChars + 1);
            }
            sb.append(String.format(Locale.US,
                    "    <text class=\"tx-muted\" x=\"%d\" y=\"%d\" fill=\"%s\" transform=\"rotate(%.0f, %d, %d)\" text-anchor=\"end\">%s</text>\n",
                    centerX, labelY, nameColor, angleDeg, centerX, labelY, ProgressionChrome.escapeXml(label)));
        }
        sb.append("  </g>\n");
    }

    private static void appendActiveFooter(
            final StringBuilder sb,
            final int x,
            final int y,
            final int w,
            final int h,
            final String[] displayNames,
            final int[] execsArray,
            final int[] targetsArray,
            final BarStatus[] statuses,
            final boolean[] activeFlags,
            final int totalCount,
            final int branchCount,
            final int maxExecs,
            final boolean denseMode) {

        sb.append("  <g id=\"active-footer\">\n");
        sb.append(String.format(Locale.US,
                "    <rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" rx=\"6\" ry=\"6\" fill=\"#1E293B\" fill-opacity=\"0.72\" stroke=\"#334155\" stroke-width=\"1\" />\n",
                x, y, w, h));

        final List<Integer> activeIndices = new ArrayList<>();
        for (int i = 0; i < activeFlags.length; i++) {
            if (activeFlags[i]) activeIndices.add(i);
        }

        final int cy = y + (h / 2);
        final int textY = y + 19;
        if (!activeIndices.isEmpty()) {
            final int primary = activeIndices.getFirst();
            final String extra = (activeIndices.size() > 1) ? (" (+" + (activeIndices.size() - 1) + " more)") : "";
            final String activeText = String.format(Locale.US, "Active: %s (%d/%d · %s)%s",
                    truncate(displayNames[primary], 78),
                    execsArray[primary],
                    targetsArray[primary],
                    statuses[primary].label,
                    extra);
            sb.append(String.format(Locale.US,
                    "    <circle cx=\"%d\" cy=\"%d\" r=\"4\" fill=\"%s\" />\n",
                    x + 14, cy, ProgressionChrome.ACCENT_SKY));
            sb.append(String.format(Locale.US,
                    "    <text class=\"tx3\" x=\"%d\" y=\"%d\" fill=\"%s\">%s</text>\n",
                    x + 26, textY, ProgressionChrome.ACCENT_SKY_TEXT, ProgressionChrome.escapeXml(activeText)));
        } else {
            sb.append(String.format(Locale.US,
                    "    <circle cx=\"%d\" cy=\"%d\" r=\"3.5\" fill=\"%s\" />\n",
                    x + 14, cy, ProgressionChrome.SLATE_BORDER));
            sb.append(String.format(Locale.US,
                    "    <text class=\"tx-muted\" x=\"%d\" y=\"%d\">Awaiting scenario executions...</text>\n",
                    x + 26, textY));
        }

        final String summaryRight = denseMode
                ? String.format(Locale.US, "%d leaves · %d branches · Peak %dx", totalCount, branchCount, maxExecs)
                : String.format(Locale.US, "%d leaves · Peak %dx", totalCount, maxExecs);
        sb.append(String.format(Locale.US,
                "    <text class=\"tx1\" x=\"%d\" y=\"%d\" fill=\"%s\" text-anchor=\"end\">%s</text>\n",
                x + w - 14, textY, ProgressionChrome.TEXT_MUTED, ProgressionChrome.escapeXml(summaryRight)));
        sb.append("  </g>\n");
    }

    private static List<BranchGroup> computeBranchGroups(
            final String[] rawShortNames,
            final String commonPrefix,
            final int labelLevel,
            final int[] execsArray,
            final int[] targetsArray,
            final BarStatus[] statuses) {

        final int count = rawShortNames.length;
        if (count == 0) return Collections.emptyList();

        final int minLevel = segmentCount(commonPrefix) + 1;
        int maxDepth = minLevel;
        for (final String name : rawShortNames) {
            maxDepth = Math.max(maxDepth, segmentCount(name));
        }

        final int level;
        if (labelLevel > 0) {
            level = Math.clamp(labelLevel, minLevel, maxDepth);
        } else {
            level = autoLabelLevel(rawShortNames, minLevel, maxDepth);
        }
        final List<BranchGroup> raw = groupByLevel(rawShortNames, level, execsArray, targetsArray, statuses);

        // If names are flat (no hierarchy) or excessively fragmented, fall back to clean uniform buckets
        if (raw.size() > MAX_HIERARCHICAL_GROUPS && (double) count / raw.size() < 2.2) {
            final int bucketCount = Math.clamp(count / 20, 4, 12);
            final List<BranchGroup> buckets = new ArrayList<>(bucketCount);
            for (int b = 0; b < bucketCount; b++) {
                final int start = (b * count) / bucketCount;
                final int end = ((b + 1) * count) / bucketCount - 1;
                if (start > end) continue;
                int completed = 0;
                boolean hasFailure = false;
                boolean hasWarning = false;
                for (int k = start; k <= end; k++) {
                    if (execsArray[k] >= targetsArray[k]) completed++;
                    if (statuses[k] == BarStatus.FAILED) hasFailure = true;
                    if (statuses[k] == BarStatus.WARNING) hasWarning = true;
                }
                final String label = "#" + (start + 1) + "–" + (end + 1);
                buckets.add(new BranchGroup(label, start, end, completed, end - start + 1, hasFailure, hasWarning));
            }
            return buckets;
        }
        return raw;
    }

    /**
     * Picks the shallowest tree level (at or below the first level distinguishing scenarios) that yields at least
     * {@link #AUTO_MIN_GROUPS} groups, without exceeding {@link #MAX_HIERARCHICAL_GROUPS}.
     */
    private static int autoLabelLevel(final String[] rawShortNames, final int minLevel, final int maxDepth) {
        int candidate = minLevel;
        for (int level = minLevel; level <= maxDepth; level++) {
            final int groups = countGroups(rawShortNames, level);
            if (groups > MAX_HIERARCHICAL_GROUPS) break;
            candidate = level;
            if (groups >= AUTO_MIN_GROUPS) break;
        }
        return candidate;
    }

    private static int countGroups(final String[] rawShortNames, final int level) {
        int groups = 0;
        String previousKey = null;
        for (final String name : rawShortNames) {
            final String key = groupKey(name, level).toLowerCase(Locale.ROOT);
            if (!key.equals(previousKey)) {
                groups++;
                previousKey = key;
            }
        }
        return groups;
    }

    private static List<BranchGroup> groupByLevel(
            final String[] rawShortNames,
            final int level,
            final int[] execsArray,
            final int[] targetsArray,
            final BarStatus[] statuses) {

        final int count = rawShortNames.length;
        final List<BranchGroup> groups = new ArrayList<>();
        int i = 0;
        while (i < count) {
            final String key = groupKey(rawShortNames[i], level);
            final String keyLower = key.toLowerCase(Locale.ROOT);
            String bestLabel = lastSegment(key);
            boolean hasChildPath = segmentCount(rawShortNames[i]) > level;
            final int start = i;
            int completed = 0;
            boolean hasFailure = false;
            boolean hasWarning = false;

            while (i < count && groupKey(rawShortNames[i], level).toLowerCase(Locale.ROOT).equals(keyLower)) {
                if (!hasChildPath && segmentCount(rawShortNames[i]) > level) {
                    bestLabel = lastSegment(groupKey(rawShortNames[i], level));
                    hasChildPath = true;
                }
                if (execsArray[i] >= targetsArray[i]) completed++;
                if (statuses[i] == BarStatus.FAILED) hasFailure = true;
                if (statuses[i] == BarStatus.WARNING) hasWarning = true;
                i++;
            }
            groups.add(new BranchGroup(bestLabel, start, i - 1, completed, i - start, hasFailure, hasWarning));
        }
        return groups;
    }

    /** Returns the first {@code level} dot-separated segments of the name (the whole name if it is shallower). */
    private static String groupKey(final String name, final int level) {
        int idx = -1;
        for (int seg = 0; seg < level; seg++) {
            idx = name.indexOf('.', idx + 1);
            if (idx < 0) return name;
        }
        return name.substring(0, idx);
    }

    private static String lastSegment(final String key) {
        final int dot = key.lastIndexOf('.');
        return (dot >= 0) ? key.substring(dot + 1) : key;
    }

    private static int segmentCount(final String name) {
        if (null == name || name.isEmpty()) return 0;
        int segments = 1;
        for (int k = 0; k < name.length(); k++) {
            if (name.charAt(k) == '.' && k < name.length() - 1) segments++;
        }
        return segments;
    }


    private static BarStatus resolveBarStatus(final Result res, final int execs, final int target) {
        if (execs <= 0) return BarStatus.UNVISITED;
        if (null != res) {
            if (ExecutionStatus.FAILED == res.status()) return BarStatus.FAILED;
            if (ExecutionStatus.WARNING == res.status()) return BarStatus.WARNING;
            if (hasNoticeEvent(res)) return BarStatus.NOTICE;
        }
        return (execs >= target) ? BarStatus.PASSED : BarStatus.IN_PROGRESS;
    }

    private static boolean hasNoticeEvent(final Result res) {
        if (null == res.getEvents() || res.getEvents().isEmpty()) return false;
        for (final _LogRecord lr : res.getEvents()) {
            if (null != lr && lr.data() instanceof _Event ev && Severity.S3 == ev.severity()) {
                return true;
            }
        }
        return false;
    }

    private static String resolveBarFillHex(final BarStatus st, final int execs, final int target) {
        if (BarStatus.IN_PROGRESS == st) {
            final Color c = TreeProgressMetrics.getProgressColor(execs, target);
            return TreeTheme.DEFAULT.hex(c);
        }
        return st.barHex;
    }

    private static boolean isRecent(final _Scenario sc, final Set<String> recentKeys) {
        if (recentKeys.isEmpty() || null == sc) return false;
        final String name = sc.getName();
        final String shortName = sc.getShortName();
        return (null != name && recentKeys.contains(name))
                || (null != shortName && recentKeys.contains(shortName));
    }

    private static String safeShortName(final _Scenario sc) {
        if (null == sc) return "";
        final String sn = sc.getShortName();
        if (null != sn && !sn.isBlank()) return sn;
        final String name = sc.getName();
        return (null != name) ? name : "";
    }

    private static String findCommonDotPrefix(final String[] names) {
        if (names.length < 2) return "";
        final String[] first = names[0].split("\\.");
        int commonLen = first.length - 1;
        if (commonLen <= 0) return "";

        for (int i = 1; i < names.length; i++) {
            final String[] segs = names[i].split("\\.");
            commonLen = Math.min(commonLen, segs.length - 1);
            int match = 0;
            while (match < commonLen && first[match].equalsIgnoreCase(segs[match])) {
                match++;
            }
            commonLen = match;
            if (commonLen == 0) return "";
        }

        final StringBuilder prefix = new StringBuilder();
        for (int k = 0; k < commonLen; k++) {
            prefix.append(first[k]).append('.');
        }
        return prefix.toString();
    }

    private static String stripPrefix(final String name, final String commonPrefix) {
        if (commonPrefix.isEmpty() || name.length() <= commonPrefix.length()) return name;
        if (name.regionMatches(true, 0, commonPrefix, 0, commonPrefix.length())) {
            return name.substring(commonPrefix.length());
        }
        return name;
    }

    private static String truncate(final String text, final int maxLen) {
        if (null == text || text.length() <= maxLen) return (null == text) ? "" : text;
        if (maxLen <= 1) return "…";
        return text.substring(0, maxLen - 1) + "…";
    }

    private static boolean anyTrue(final boolean[] flags) {
        for (final boolean f : flags) {
            if (f) return true;
        }
        return false;
    }
}
