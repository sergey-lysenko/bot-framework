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
            String fill = theme.hex(node.backgroundColor());
            String stroke = theme.hex(node.borderColor());
            String textCol = theme.hex(node.textColor());
            String badgeTextCol = theme.hex(node.badgeTextColor());
            
            String safeLabel = node.label().replace("<", "&lt;").replace(">", "&gt;");
            String dash = node.isDashed() ? "stroke-dasharray=\"3 3\"" : "";
            
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
                    "  <rect class=\"progress-track\" x=\"8\" y=\"%d\" width=\"%d\" height=\"4\" fill=\"#0f172a\" opacity=\"0.8\" rx=\"2\" ry=\"2\" />\n" +
                    "  <rect class=\"progress-fill\" x=\"8\" y=\"%d\" width=\"%d\" height=\"4\" fill=\"%s\" rx=\"2\" ry=\"2\" />\n",
                    theme.cardHeight() - 8, barW,
                    theme.cardHeight() - 8, filledW, stroke);
            }

            String indicatorDot = "";
            if (node.indicatorColor() != null) {
                indicatorDot = String.format(Locale.US,
                    "  <circle cx=\"%d\" cy=\"18\" r=\"3\" fill=\"%s\" />\n",
                    pillX - 7, theme.hex(node.indicatorColor()));
            }

            String hatchOverlay = node.hasKnownIssues() ? 
                String.format(Locale.US, "  <rect class=\"hatch-overlay\" width=\"%d\" height=\"%d\" fill=\"url(#known-issue-hatch)\" rx=\"8\" ry=\"8\" pointer-events=\"none\" />\n", 
                theme.cardWidth(), theme.cardHeight()) : "";

            sb.append(String.format(
                Locale.US,
                "<g id=\"%s\" class=\"node-card %s\" transform=\"translate(%d, %d)\" onclick=\"showNodeDetails('%s')\">\n" +
                "  <title>%s (%s)</title>\n" +
                "  <rect class=\"card-bg\" width=\"%d\" height=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%.1f\" %s rx=\"8\" ry=\"8\" />\n" +
                "%s" +
                "%s" +
                "  <rect class=\"pill-bg\" x=\"%d\" y=\"10\" width=\"%d\" height=\"18\" fill=\"#0f172a\" opacity=\"0.8\" stroke=\"%s\" rx=\"6\" ry=\"6\" />\n" +
                "  <text x=\"%d\" y=\"23\" fill=\"%s\" font-family=\"monospace\" font-size=\"10\" font-weight=\"bold\">%s</text>\n" +
                "%s" +
                "  <text x=\"12\" y=\"24\" fill=\"%s\" font-size=\"12\">%s</text>\n" +
                "</g>\n",
                node.id(), cssClass, (int) node.x(), (int) node.y(), node.id(),
                safeLabel, titleStatus,
                theme.cardWidth(), theme.cardHeight(), fill, stroke, node.strokeWidth(), dash,
                hatchOverlay,
                progressBar,
                pillX, pillWidth, stroke,
                pillX + 6, badgeTextCol, node.badgeText(),
                indicatorDot,
                textCol, safeLabel
            ));
        }
        
        return sb.toString();
    }
}
