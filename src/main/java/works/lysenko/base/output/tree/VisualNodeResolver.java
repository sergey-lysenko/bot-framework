package works.lysenko.base.output.tree;

import works.lysenko.base.output.TreeHtml;
import works.lysenko.base.output.TreeHtml.NodeData;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.ExecutionStatus;
import works.lysenko.util.data.enums.ScenarioType;

public class VisualNodeResolver {
    
    private final TreeTheme theme;
    private final Set<_Scenario> accessibleScenarios;
    private final boolean isAllLeafs;
    private final int target;
    private final double maxLeafOverRatio;
    private final boolean isRecent;
    private final boolean includeHeaderOffset;
    private final double minRow;

    public VisualNodeResolver(TreeTheme theme, Set<_Scenario> accessibleScenarios, boolean isAllLeafs) {
        this(theme, accessibleScenarios, isAllLeafs, 1, 0.0, false, false);
    }

    public VisualNodeResolver(TreeTheme theme, Set<_Scenario> accessibleScenarios, boolean isAllLeafs, int target, double maxLeafOverRatio) {
        this(theme, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, false, false);
    }

    public VisualNodeResolver(TreeTheme theme, Set<_Scenario> accessibleScenarios, boolean isAllLeafs, int target, double maxLeafOverRatio, boolean isRecent, boolean includeHeaderOffset) {
        this(theme, accessibleScenarios, isAllLeafs, target, maxLeafOverRatio, isRecent, includeHeaderOffset, 0.0);
    }

    public VisualNodeResolver(TreeTheme theme, Set<_Scenario> accessibleScenarios, boolean isAllLeafs, int target, double maxLeafOverRatio, boolean isRecent, boolean includeHeaderOffset, double minRow) {
        this.theme = theme;
        this.accessibleScenarios = accessibleScenarios;
        this.isAllLeafs = isAllLeafs;
        this.target = target;
        this.maxLeafOverRatio = maxLeafOverRatio;
        this.isRecent = isRecent;
        this.includeHeaderOffset = includeHeaderOffset;
        this.minRow = minRow;
    }
    
    public VisualNode resolve(NodeData node) {
        return resolve(node, this.isRecent);
    }

    private final Map<NodeData, Boolean> childFailureCache = new HashMap<>();
    private final Map<NodeData, Boolean> executableCache = new HashMap<>();

    public VisualNode resolve(NodeData node, boolean isRecentNode) {
        return resolve(node, isRecentNode, this.childFailureCache, this.executableCache);
    }

    public VisualNode resolve(NodeData node, boolean isRecentNode, Map<NodeData, Boolean> childFailureCache, Map<NodeData, Boolean> executableCache) {
        final int execs = (node.result() != null) ? node.result().getExecutions() : 0;
        final int eventCount = (node.result() != null && node.result().getEvents() != null) ? node.result().getEvents().size() : 0;
        final int scenarioTarget = getTargetExecutions(node, target);
        final int safeTarget = Math.max(1, scenarioTarget);
        final ScenarioType type = getScenarioType(node);
        final double overRatio = (ScenarioType.LEAF == type) ? getLeafOverExecutionRatio(node, target) : 0.0;

        final boolean failure = TreeHtml.hasFailure(node);
        final boolean childFailure = !failure && TreeHtml.hasChildFailure(node, childFailureCache);
        final Color progressColor = failure ? theme.failedBorder() : (childFailure ? theme.upsetBorder() : getScenarioProgressColor(node, target, maxLeafOverRatio));
        boolean executable = TreeHtml.isTheoreticallyExecutable(node, accessibleScenarios, executableCache);

        Color cardFill;
        Color borderColor;
        float strokeWidth = 1.8f;
        boolean isDashed = false;
        String statusName;

        final boolean warning = (node.result() != null && node.result().status() == ExecutionStatus.WARNING);

        if (failure) {
            cardFill = theme.failedBg();
            borderColor = theme.failedBorder();
            strokeWidth = 2.2f;
            statusName = "FAILED";
        } else if (isRecentNode) {
            cardFill = (overRatio > 0.0) ? progressColor : new Color(0x16, 0x3A, 0x58);
            borderColor = new Color(0x38, 0xBD, 0xF8); // ACCENT_CYAN
            strokeWidth = 2.8f;
            statusName = childFailure ? "UPSET" : ((execs > 0) ? (warning ? "WARNING" : "PASSED") : (executable ? "UNVISITED" : "NON_EXECUTABLE"));
        } else if (childFailure) {
            cardFill = theme.upsetBg();
            borderColor = theme.upsetBorder();
            isDashed = true;
            statusName = "UPSET";
        } else if (execs == 0) {
            if (!executable) {
                cardFill = theme.nonExecutableBg();
                borderColor = theme.nonExecutableBorder();
                isDashed = true;
                statusName = "NON_EXECUTABLE";
            } else {
                cardFill = theme.unvisitedBg();
                borderColor = theme.unvisitedBorder();
                strokeWidth = 1.2f;
                statusName = "UNVISITED";
            }
        } else if (overRatio > 0.0) {
            cardFill = progressColor;
            borderColor = warning ? theme.warningBorder() : progressColor;
            strokeWidth = 1.8f;
            statusName = warning ? "WARNING" : "PASSED";
        } else {
            final int r = (int) (theme.unvisitedBg().getRed() * 0.7 + progressColor.getRed() * 0.3);
            final int gr = (int) (theme.unvisitedBg().getGreen() * 0.7 + progressColor.getGreen() * 0.3);
            final int b = (int) (theme.unvisitedBg().getBlue() * 0.7 + progressColor.getBlue() * 0.3);
            cardFill = new Color(r, gr, b);
            borderColor = warning ? theme.warningBorder() : progressColor;
            strokeWidth = 1.8f;
            statusName = warning ? "WARNING" : "PASSED";
        }

        boolean hasKnownIssues = node.knownIssues() != null && !node.knownIssues().isEmpty();
        if (hasKnownIssues) {
            borderColor = new Color(251, 191, 36); // Amber #fbbf24 for known issues border
        }

        // Fill ratio for progress bar (ONLY in all_leafs mode, and ONLY for LEAF and MONO nodes)
        double fillRatio = -1.0;
        if (isAllLeafs && (execs > 0 || failure) && (ScenarioType.LEAF == type || ScenarioType.MONO == type)) {
            if (execs == 0 && failure) {
                fillRatio = 0.5; // Partial fill for failed / aborted test run before 1st completion
            } else {
                fillRatio = (0 == scenarioTarget) ? 1.0 : Math.min(1.0, (double) execs / (double) safeTarget);
            }
        }

        // Pill badge text and badge foreground text color
        String badgeText = (execs == 0 && !executable) ? "OFF" : getExecutionBadgeText(node, execs, target);
        Color badgeFg = (execs == 0 && !executable) 
            ? theme.nonExecutableText() 
            : (childFailure ? theme.upsetBorder() : ((execs == 0) ? theme.unvisitedText() : progressColor));

        // Circle indicator color
        Color indicatorColor = null;
        if (failure) {
            indicatorColor = theme.failedBorder();
        } else if (childFailure) {
            indicatorColor = theme.upsetBorder();
        } else if (eventCount > 0) {
            indicatorColor = theme.warningBorder();
        }

        // Node label text color
        Color textColor = (overRatio > 0.0) 
            ? new Color(0x0F, 0x17, 0x2A) 
            : ((execs > 0) ? new Color(0xF8, 0xFA, 0xFC) : ((!executable) ? theme.nonExecutableText() : theme.unvisitedText()));

        // Coordinates calculation
        double x, y;
        if (includeHeaderOffset) {
            x = node.col() * 270 + 40;
            y = (node.row() - minRow) * 60 + 60 + 32;
        } else {
            x = node.col() * 260 + 40;
            y = (node.row() - minRow) * 56 + 20;
        }

        return new VisualNode(
            node.id(),
            node.label(),
            node.group(),
            statusName,
            badgeText,
            cardFill,
            borderColor,
            textColor,
            badgeFg,
            indicatorColor,
            fillRatio,
            strokeWidth,
            hasKnownIssues,
            isDashed,
            x,
            y,
            node.col(),
            node.row()
        );
    }

    private ScenarioType getScenarioType(final NodeData node) {
        if (null != node.result() && null != node.result().getScenarioType()) {
            return node.result().getScenarioType();
        }
        return node.children().isEmpty() ? ScenarioType.LEAF : ScenarioType.NODE;
    }

    private int getTargetExecutions(final NodeData node, final int leafTarget) {
        return switch (getScenarioType(node)) {
            case MONO -> 1;
            case LEAF -> Math.max(1, leafTarget);
            default -> 0;
        };
    }

    private double getLeafOverExecutionRatio(final NodeData node, final int target) {
        final int safeTarget = Math.max(1, getTargetExecutions(node, target));
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        return (execs > safeTarget) ? (double) (execs - safeTarget) / safeTarget : 0.0;
    }

    private int getOverExecutionPercent(final int execs, final int target) {
        final int safeTarget = Math.max(1, target);
        return (execs > safeTarget) ? (int) Math.ceil((execs - safeTarget) * 100.0 / safeTarget) : 0;
    }

    private Color getOverExecutionColor(final double overRatio, final double maxOverRatio) {
        if (overRatio <= 0.0 || maxOverRatio <= 0.0)
            return theme.passedBorder();
        
        final double progress = Math.min(1.0, overRatio / maxOverRatio);
        final Color targetColor = new Color(0xF8, 0xFA, 0xFC); // TEXT_WHITE
        return new Color(
            interpolate(theme.passedBorder().getRed(), targetColor.getRed(), progress),
            interpolate(theme.passedBorder().getGreen(), targetColor.getGreen(), progress),
            interpolate(theme.passedBorder().getBlue(), targetColor.getBlue(), progress));
    }

    private Color getProgressColor(final int execs, final int target) {
        if (execs <= 0) {
            return theme.unvisitedBorder();
        }
        final int safeTarget = Math.max(1, target);
        if (execs >= safeTarget) {
            return theme.passedBorder();
        }
        final double ratio = (double) execs / (double) safeTarget;
        final int r = interpolate(theme.unvisitedBorder().getRed(), theme.passedBorder().getRed(), ratio);
        final int g = interpolate(theme.unvisitedBorder().getGreen(), theme.passedBorder().getGreen(), ratio);
        final int b = interpolate(theme.unvisitedBorder().getBlue(), theme.passedBorder().getBlue(), ratio);
        return new Color(r, g, b);
    }

    private Color getScenarioProgressColor(
            final NodeData node,
            final int target,
            final double maxLeafOverRatio) {
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.LEAF == type && isAllLeafs) {
            final double overRatio = getLeafOverExecutionRatio(node, target);
            return (overRatio > 0.0)
                    ? getOverExecutionColor(overRatio, maxLeafOverRatio)
                    : getProgressColor(execs, target);
        }
        return getProgressColor(execs, 1);
    }

    private int interpolate(final int start, final int end, final double progress) {
        return (int) Math.round(start + ((end - start) * progress));
    }

    private String getExecutionBadgeText(final NodeData node, final int execs, final int leafTarget) {
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.NODE == type || ScenarioType.CORE == type || !isAllLeafs) {
            return execs + ((1 == execs) ? " exec" : " execs");
        }

        final int target = getTargetExecutions(node, leafTarget);
        if (execs > target && ScenarioType.LEAF == type) {
            return execs + "/" + target + " +" + getOverExecutionPercent(execs, target) + "%";
        }
        if (execs >= target && 0 < execs) {
            return "✓ " + execs + "/" + target;
        }
        return execs + "/" + target;
    }
}