package works.lysenko.base.output.tree;

import java.awt.Color;

/**
 * The VisualNode record.
 */
public record VisualNode(
    String id,
    String label,
    String group,
    String statusName,
    String badgeText,
    Color backgroundColor,
    Color borderColor,
    Color textColor,
    Color badgeTextColor,
    Color indicatorColor,
    double fillRatio,
    float strokeWidth,
    boolean hasKnownIssues,
    boolean isDashed,
    double x,
    double y,
    int col,
    double row
) {
}
