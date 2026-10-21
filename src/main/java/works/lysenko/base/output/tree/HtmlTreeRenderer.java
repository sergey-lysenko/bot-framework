package works.lysenko.base.output.tree;

import java.util.List;
import java.util.Locale;

public class HtmlTreeRenderer {

    private final TreeTheme theme;

    public HtmlTreeRenderer(TreeTheme theme) {
        this.theme = theme;
    }

    public String renderNodes(List<VisualNode> nodes) {
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

            String safeLabel = node.label().replace("<", "&lt;").replace(">", "&gt;");
            int pillWidth = node.badgeText().length() * 7 + 12;
            int pillX = theme.cardWidth() - pillWidth - 8;

            int maxChars = Math.max(4, (pillX - 18) / 8);
            if (safeLabel.length() > maxChars) {
                safeLabel = safeLabel.substring(0, maxChars - 3) + "...";
            }

            String progressBar = "";
            if (node.fillRatio() >= 0) {
                int barW = theme.cardWidth() - 16;
                int filledW = Math.max(4, (int) Math.round(barW * node.fillRatio()));
                progressBar = String.format(Locale.US,
                    "  <rect class=\"pb\" x=\"8\" y=\"%d\" width=\"%d\" height=\"4\" />\n",
                    theme.cardHeight() - 8, filledW);
            }

            String hatchOverlay = node.hasKnownIssues() ?
                String.format(Locale.US, "  <rect class=\"hatch-overlay\" width=\"%d\" height=\"%d\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />\n",
                theme.cardWidth(), theme.cardHeight()) : "";

            sb.append(String.format(
                Locale.US,
                "<g id=\"%s\" class=\"node-card %s\" transform=\"translate(%d, %d)\" onclick=\"showNodeDetails('%s')\">\n" +
                "  <title>%s (%s)</title>\n" +
                "  <use href=\"#cb\" />\n" +
                "%s" +
                "%s" +
                "  <use href=\"#pt\" x=\"%d\" />\n" +
                "  <text class=\"tx1\" x=\"%d\">%s</text>\n" +
                "  <text class=\"tx2\">%s</text>\n" +
                "</g>\n",
                node.id(), cssClass, (int) node.x(), (int) node.y(), node.id(),
                safeLabel, titleStatus,
                hatchOverlay,
                progressBar,
                pillX,
                pillX + 6, node.badgeText(),
                safeLabel
            ));
        }

        return sb.toString();
    }
}
