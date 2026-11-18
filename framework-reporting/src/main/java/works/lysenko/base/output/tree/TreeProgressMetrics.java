package works.lysenko.base.output.tree;

import works.lysenko.base.output.TreeHtml.NodeData;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.prop.PropEnum;

import java.awt.Color;
import java.util.Locale;

import static works.lysenko.Base.parameters;

/**
 * Utility calculations for scenario tree progression metrics, progress colors,
 * execution targets, and formatted timing.
 */
public final class TreeProgressMetrics {

    private static final Color UNVISITED_BORDER = new Color(0x33, 0x41, 0x55);
    private static final Color GREEN_DONE = new Color(0x22, 0xC5, 0x5E);
    private static final Color TEXT_WHITE = new Color(0xF8, 0xFA, 0xFC);

    private TreeProgressMetrics() {
    }

    /**
     * Calculates the visual progress color based on executions and target count.
     * Smoothly interpolates from the non-started grayish-dark-blue color (UNVISITED_BORDER)
     * to the completion emerald green (GREEN_DONE).
     *
     * @param execs  number of executions
     * @param target target executions count
     * @return Color indicating progress
     */
    public static Color getProgressColor(final int execs, final int target) {
        if (execs <= 0) {
            return UNVISITED_BORDER;
        }
        final int safeTarget = Math.max(1, target);
        if (execs >= safeTarget) {
            return GREEN_DONE;
        }
        final double ratio = (double) execs / (double) safeTarget;
        final int r = (int) Math.round(UNVISITED_BORDER.getRed() + ratio * (GREEN_DONE.getRed() - UNVISITED_BORDER.getRed()));
        final int g = (int) Math.round(UNVISITED_BORDER.getGreen() + ratio * (GREEN_DONE.getGreen() - UNVISITED_BORDER.getGreen()));
        final int b = (int) Math.round(UNVISITED_BORDER.getBlue() + ratio * (GREEN_DONE.getBlue() - UNVISITED_BORDER.getBlue()));
        return new Color(r, g, b);
    }

    public static int getTargetExecutions(final NodeData node, final int leafTarget) {
        return switch (getScenarioType(node)) {
            case MONO -> 1;
            case LEAF -> Math.max(1, leafTarget);
            default -> 0;
        };
    }

    public static int getOverExecutionPercent(final int execs, final int target) {
        final int safeTarget = Math.max(1, target);
        return (execs > safeTarget) ? (int) Math.ceil((execs - safeTarget) * 100.0 / safeTarget) : 0;
    }

    public static Color getOverExecutionColor(final double overRatio, final double maxOverRatio) {
        if (overRatio <= 0.0 || maxOverRatio <= 0.0)
            return GREEN_DONE;

        final double progress = Math.min(1.0, overRatio / maxOverRatio);
        return new Color(
                interpolate(GREEN_DONE.getRed(), TEXT_WHITE.getRed(), progress),
                interpolate(GREEN_DONE.getGreen(), TEXT_WHITE.getGreen(), progress),
                interpolate(GREEN_DONE.getBlue(), TEXT_WHITE.getBlue(), progress));
    }

    public static String getExecutionBadgeText(final NodeData node, final int execs, final int leafTarget) {
        final boolean isAllLeafs = (null != parameters) ? parameters.isAllLeafs() : (null != PropEnum._TEST_ALL_LEAFS.get() && (Boolean) PropEnum._TEST_ALL_LEAFS.get());
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

    public static ScenarioType getScenarioType(final NodeData node) {
        if (null != node.result() && null != node.result().getScenarioType()) {
            return node.result().getScenarioType();
        }
        return node.children().isEmpty() ? ScenarioType.LEAF : ScenarioType.NODE;
    }

    public static boolean isTerminalScenario(final NodeData node) {
        final ScenarioType type = getScenarioType(node);
        return ScenarioType.LEAF == type || ScenarioType.MONO == type;
    }

    public static double getLeafOverExecutionRatio(final NodeData node, final int target) {
        final int safeTarget = Math.max(1, target);
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        return (execs > safeTarget) ? (double) (execs - safeTarget) / safeTarget : 0.0;
    }

    public static Color getScenarioProgressColor(
            final NodeData node,
            final int target,
            final double maxLeafOverRatio) {
        final int execs = (null != node.result()) ? node.result().getExecutions() : 0;
        final boolean isAllLeafs = (null != parameters) ? parameters.isAllLeafs() : (null != PropEnum._TEST_ALL_LEAFS.get() && (Boolean) PropEnum._TEST_ALL_LEAFS.get());
        final ScenarioType type = getScenarioType(node);
        if (ScenarioType.LEAF == type && isAllLeafs) {
            final double overRatio = getLeafOverExecutionRatio(node, target);
            return (overRatio > 0.0)
                    ? getOverExecutionColor(overRatio, maxLeafOverRatio)
                    : getProgressColor(execs, target);
        }
        return getProgressColor(execs, 1);
    }

    public static int interpolate(final int start, final int end, final double progress) {
        return (int) Math.round(start + ((end - start) * progress));
    }

    public static String formatElapsedTime(final long elapsedMillis) {
        final long totalSeconds = Math.max(0L, elapsedMillis) / 1000;
        return String.format(
                Locale.ROOT,
                "%02d:%02d:%02d",
                totalSeconds / 3600,
                (totalSeconds / 60) % 60,
                totalSeconds % 60);
    }
}
