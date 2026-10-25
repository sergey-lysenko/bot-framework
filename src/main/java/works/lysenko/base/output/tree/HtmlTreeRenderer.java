package works.lysenko.base.output.tree;

import java.util.List;
import java.util.Locale;

/**
 * The HtmlTreeRenderer class.
 */
public class HtmlTreeRenderer {

    private final TreeTheme theme;

    /**
     * Constructs a new HtmlTreeRenderer.
     * @param theme the theme
     */
    public HtmlTreeRenderer(TreeTheme theme) {
        this.theme = (null != theme) ? theme : TreeTheme.DEFAULT;
    }

    /**
     * Performs render nodes.
     * @param nodes the nodes
     * @return the render nodes result
     */
    public String renderNodes(List<VisualNode> nodes) {
        if (null == nodes || nodes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        for (VisualNode node : nodes) {
            String cssClass = switch (node.statusName()) {
                case "FAILED" -> "failed";
                case "UPSET" -> "child_failed";
                case "PASSED" -> "passed";
                case "WARNING" -> "warning";
                case "NON_EXECUTABLE" -> "non_executable";
                default -> "unvisited";
            };

            String titleStatus = switch (node.statusName()) {
                case "FAILED" -> "Failed";
                case "UPSET" -> "Upset";
                case "PASSED" -> "Passed";
                case "WARNING" -> "Warning";
                case "NON_EXECUTABLE" -> "Non-executable";
                default -> "Unvisited";
            };

            if (node.hasKnownIssues()) {
                cssClass += " known-issue";
                titleStatus += " (Known Issue)";
            }

            int cardW = theme.cardWidth();
            int cardH = theme.cardHeight();

            String bgHex = (null != node.backgroundColor()) ? theme.hex(node.backgroundColor()) : theme.hex(theme.unvisitedBg());
            String borderHex = (null != node.borderColor()) ? theme.hex(node.borderColor()) : theme.hex(theme.unvisitedBorder());
            float strokeW = (node.strokeWidth() > 0) ? node.strokeWidth() : 1.5f;
            String dashAttr = node.isDashed() ? " stroke-dasharray=\"4 4\"" : "";

            boolean hasIndicator = (null != node.indicatorColor());
            int labelX = hasIndicator ? 24 : 12;

            String badgeStr = (null != node.badgeText()) ? node.badgeText() : "";
            int pillW = Math.max(32, badgeStr.length() * 7 + 10);
            int pillX = cardW - pillW - 8;
            int pillY = (cardH - 18) / 2;

            String rawLabel = (null != node.label()) ? node.label() : "";
            String safeLabel = rawLabel.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
            int availLabelW = pillX - labelX - 6;
            int maxChars = Math.max(4, availLabelW / 7);
            if (safeLabel.length() > maxChars) {
                safeLabel = safeLabel.substring(0, Math.max(1, maxChars - 3)) + "...";
            }

            String textHex = (null != node.textColor()) ? theme.hex(node.textColor()) : "#f8fafc";
            String badgeTextHex = (null != node.badgeTextColor()) ? theme.hex(node.badgeTextColor()) : "#38bdf8";

            String hatchOverlay = node.hasKnownIssues()
                ? String.format(Locale.US, "  <rect class=\"hatch-overlay\" width=\"%d\" height=\"%d\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />\n", cardW, cardH)
                : "";

            StringBuilder pbSb = new StringBuilder();
            if (node.fillRatio() >= 0) {
                int pbY = cardH - 6;
                int pbTotalW = cardW - 16;
                int pbFilledW = Math.max(3, (int) Math.round(pbTotalW * Math.min(1.0, node.fillRatio())));
                pbSb.append(String.format(Locale.US, "  <rect class=\"pb-track\" x=\"8\" y=\"%d\" width=\"%d\" height=\"3\" fill=\"#0f172a\" opacity=\"0.7\" rx=\"1.5\" ry=\"1.5\" />\n", pbY, pbTotalW));
                pbSb.append(String.format(Locale.US, "  <rect class=\"pb-fill\" x=\"8\" y=\"%d\" width=\"%d\" height=\"3\" fill=\"%s\" rx=\"1.5\" ry=\"1.5\" />\n", pbY, pbFilledW, borderHex));
            }

            String indicatorSvg = hasIndicator
                ? String.format(Locale.US, "  <circle cx=\"12\" cy=\"%d\" r=\"4\" fill=\"%s\" />\n", cardH / 2, theme.hex(node.indicatorColor()))
                : "";

            sb.append(String.format(
                Locale.US,
                "<g id=\"%s\" class=\"node-card %s\" transform=\"translate(%d, %d)\" onclick=\"showNodeDetails('%s')\">\n" +
                "  <title>%s (%s)</title>\n" +
                "  <rect class=\"card-bg\" width=\"%d\" height=\"%d\" rx=\"8\" ry=\"8\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%.1f\"%s />\n" +
                "%s" +
                "%s" +
                "%s" +
                "  <rect class=\"pill-bg\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"18\" rx=\"9\" ry=\"9\" fill=\"#0f172a\" fill-opacity=\"0.75\" stroke=\"%s\" stroke-width=\"1\" />\n" +
                "  <text class=\"tx1\" x=\"%d\" y=\"%d\" text-anchor=\"middle\" fill=\"%s\">%s</text>\n" +
                "  <text class=\"tx2\" x=\"%d\" y=\"%d\" fill=\"%s\">%s</text>\n" +
                "</g>\n",
                node.id(), cssClass, (int) Math.round(node.x()), (int) Math.round(node.y()), node.id(),
                safeLabel, titleStatus,
                cardW, cardH, bgHex, borderHex, strokeW, dashAttr,
                hatchOverlay,
                pbSb.toString(),
                indicatorSvg,
                pillX, pillY, pillW, badgeTextHex,
                pillX + pillW / 2, pillY + 13, badgeTextHex, badgeStr,
                labelX, cardH / 2 + 4, textHex, safeLabel
            ));
        }

        return sb.toString();
    }
}
