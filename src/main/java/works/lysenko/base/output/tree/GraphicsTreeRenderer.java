package works.lysenko.base.output.tree;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * The GraphicsTreeRenderer class.
 */
public class GraphicsTreeRenderer {

    private static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font FONT_BADGE = new Font(Font.MONOSPACED, Font.BOLD, 10);
    private static final BasicStroke DASHED_STROKE = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);

    private final TreeTheme theme;

    /**
     * Constructs a new GraphicsTreeRenderer.
     * @param theme the theme
     */
    public GraphicsTreeRenderer(TreeTheme theme) {
        this.theme = theme;
    }

    /**
     * Performs render nodes.
     * @param g2d the g2d
     * @param nodes the nodes
     */
    public void renderNodes(Graphics2D g2d, List<VisualNode> nodes) {
        for (VisualNode node : nodes) {
            int x = (int) node.x();
            int y = (int) node.y();

            // 1. Draw card background
            RoundRectangle2D.Float rect = new RoundRectangle2D.Float(x, y, theme.cardWidth(), theme.cardHeight(), 10, 10);
            g2d.setColor(node.backgroundColor());
            g2d.fill(rect);

            // 1b. Known Issues Cross-Hatch Overlay
            if (node.hasKnownIssues()) {
                java.awt.Shape oldClip = g2d.getClip();
                g2d.clip(rect);

                g2d.setColor(new Color(251, 191, 36, 120)); // Amber cross-hatch #fbbf24
                g2d.setStroke(new BasicStroke(1.8f));

                int width = theme.cardWidth();
                int height = theme.cardHeight();
                int spacing = 10;

                for (int offset = -height; offset < width + height; offset += spacing) {
                    // Lines going top-left to bottom-right \
                    g2d.drawLine(x + offset, y, x + offset + height, y + height);
                    // Lines going bottom-left to top-right /
                    g2d.drawLine(x + offset, y + height, x + offset + height, y);
                }

                g2d.setClip(oldClip);
            }

            // 2. Draw card border
            if (node.isDashed()) {
                g2d.setStroke(DASHED_STROKE);
            } else {
                g2d.setStroke(new BasicStroke(node.strokeWidth()));
            }
            g2d.setColor(node.borderColor());
            g2d.draw(rect);

            // 3. Progress bar along bottom of card
            if (node.fillRatio() >= 0) {
                int barX = x + 8;
                int barY = y + theme.cardHeight() - 8;
                int barW = theme.cardWidth() - 16;
                int barH = 4;

                // Track background
                g2d.setColor(new Color(15, 23, 42, 220));
                g2d.fillRoundRect(barX, barY, barW, barH, 2, 2);

                // Fill
                int filledW = Math.max(4, (int) Math.round(barW * node.fillRatio()));
                g2d.setColor(node.borderColor());
                g2d.fillRoundRect(barX, barY, filledW, barH, 2, 2);
            }

            // 4. Right pill badge
            g2d.setFont(FONT_BADGE);
            FontMetrics fmBadge = g2d.getFontMetrics();
            int badgeTextW = fmBadge.stringWidth(node.badgeText());
            int pillW = badgeTextW + 12;
            int pillH = 18;
            int pillX = x + theme.cardWidth() - pillW - 8;
            int pillY = y + 10;

            // Pill bg
            g2d.setColor(new Color(15, 23, 42, "OFF".equals(node.badgeText()) ? 140 : 220));
            g2d.fillRoundRect(pillX, pillY, pillW, pillH, 6, 6);
            g2d.setStroke(node.isDashed() ? DASHED_STROKE : new BasicStroke(1.0f));
            g2d.setColor(node.borderColor());
            g2d.drawRoundRect(pillX, pillY, pillW, pillH, 6, 6);

            // Pill text
            g2d.setColor(node.badgeTextColor());
            g2d.drawString(node.badgeText(), pillX + 6, pillY + 13);

            // 5. Warning / Failure indicator dot
            if (node.indicatorColor() != null) {
                g2d.setColor(node.indicatorColor());
                g2d.fillOval(pillX - 10, y + 15, 6, 6);
            }

            // 6. Node label
            g2d.setFont(FONT_LABEL);
            FontMetrics fmLabel = g2d.getFontMetrics();
            int maxLabelW = pillX - x - 18;
            String label = node.label();
            if (fmLabel.stringWidth(label) > maxLabelW) {
                while (label.length() > 3 && fmLabel.stringWidth(label + "...") > maxLabelW) {
                    label = label.substring(0, label.length() - 1);
                }
                label = label + "...";
            }
            g2d.setColor(node.textColor());
            g2d.drawString(label, x + 12, y + 24);
        }
    }
}
