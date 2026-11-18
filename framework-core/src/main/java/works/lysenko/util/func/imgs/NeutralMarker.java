package works.lysenko.util.func.imgs;

import org.openqa.selenium.Rectangle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Neutral / Boring image annotation utility.
 * Renders clean, professional, non-intrusive annotations with rounded rectangles, coordinates, and simple pill badges.
 */
public class NeutralMarker implements OperationMarker {

    public static final String MODE = "neutral";

    // Clean Professional Palette (Sky Blue / Slate / Emerald)
    private static final Color ACCENT_BLUE = new Color(2, 132, 199);
    private static final Color ACCENT_BLUE_FILL = new Color(2, 132, 199, 35);
    private static final Color BADGE_BG = new Color(15, 23, 42, 210);
    private static final Color TEXT_WHITE = new Color(248, 250, 252);
    private static final Color TEXT_MUTED = new Color(148, 163, 184);

    private static final Font SANS_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font SANS_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public String description() {
        return "Neutral & clean professional screenshot annotations with coordinates and pill badges";
    }

    @Override
    public BufferedImage markElement(final BufferedImage image, final Rectangle rect,
                                     final String label, final String action) {
        if (null == image || null == rect || rect.width <= 0 || rect.height <= 0) {
            return image;
        }

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        final int x = rect.x;
        final int y = rect.y;
        final int w = rect.width;
        final int h = rect.height;

        // 1. Semi-transparent rounded rectangle highlight
        g2d.setColor(ACCENT_BLUE_FILL);
        g2d.fillRoundRect(x, y, w, h, 8, 8);

        // Cross-hatch overlay for clear distinction from UI elements
        Painter.drawCrossHatch(g2d, rect, new Color(2, 132, 199, 80), 12, 1.2f);

        // 2. Clean 2px border
        g2d.setColor(ACCENT_BLUE);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // 3. Compact Pill Badge with Coordinates and Action
        final int centerX = x + w / 2;
        final int centerY = y + h / 2;

        final String actStr = (null == action || action.isBlank()) ? "ACTION" : action.toUpperCase();
        final String infoText = String.format("%s (%d, %d) %dx%d%s",
                actStr, centerX, centerY, w, h,
                (null != label && !label.isBlank()) ? String.format(" | \"%s\"", label) : "");

        g2d.setFont(SANS_BOLD);
        final FontMetrics fm = g2d.getFontMetrics();
        final int badgeW = fm.stringWidth(infoText) + 16;
        final int badgeH = 22;

        int badgeX = x;
        int badgeY = y - badgeH - 4;
        if (badgeY < 4) badgeY = y + h + 4;
        if (badgeX + badgeW > image.getWidth() - 4) badgeX = image.getWidth() - badgeW - 4;
        if (badgeX < 4) badgeX = 4;

        g2d.setColor(BADGE_BG);
        g2d.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
        g2d.setColor(ACCENT_BLUE);
        g2d.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

        g2d.setColor(TEXT_WHITE);
        g2d.drawString(infoText, badgeX + 8, badgeY + 15);

        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage markPoint(final BufferedImage image, final Point point, final String action) {
        if (null == image || null == point) return image;

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        final int px = point.x;
        final int py = point.y;

        // Touch circle
        g2d.setColor(ACCENT_BLUE_FILL);
        g2d.fillOval(px - 18, py - 18, 36, 36);

        g2d.setColor(ACCENT_BLUE);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(px - 18, py - 18, 36, 36);
        g2d.drawOval(px - 4, py - 4, 8, 8);

        // Coordinates badge
        final String text = String.format("%s at (%d, %d)", action.toUpperCase(), px, py);
        g2d.setFont(SANS_FONT);
        final FontMetrics fm = g2d.getFontMetrics();
        final int badgeW = fm.stringWidth(text) + 14;
        final int badgeH = 20;

        int badgeX = px + 24;
        int badgeY = py - 10;
        if (badgeX + badgeW > image.getWidth() - 5) badgeX = px - badgeW - 24;

        g2d.setColor(BADGE_BG);
        g2d.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
        g2d.setColor(ACCENT_BLUE);
        g2d.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

        g2d.setColor(TEXT_WHITE);
        g2d.drawString(text, badgeX + 7, badgeY + 14);

        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage markSwipe(final BufferedImage image, final Point start, final Point end) {
        if (null == image || null == start || null == end) return image;

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        // Line & Arrow
        g2d.setColor(ACCENT_BLUE);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawLine(start.x, start.y, end.x, end.y);

        g2d.fillOval(start.x - 6, start.y - 6, 12, 12);

        final double angle = Math.atan2(end.y - start.y, end.x - start.x);
        final int arrowLen = 14;
        final int x1 = (int) (end.x - arrowLen * Math.cos(angle - Math.PI / 6));
        final int y1 = (int) (end.y - arrowLen * Math.sin(angle - Math.PI / 6));
        final int x2 = (int) (end.x - arrowLen * Math.cos(angle + Math.PI / 6));
        final int y2 = (int) (end.y - arrowLen * Math.sin(angle + Math.PI / 6));

        g2d.fillPolygon(new int[]{end.x, x1, x2}, new int[]{end.y, y1, y2}, 3);

        // Info pill
        final double dist = Math.hypot(end.x - start.x, end.y - start.y);
        final String info = String.format("SWIPE %.0fpx | (%d,%d) ➔ (%d,%d)", dist, start.x, start.y, end.x, end.y);

        g2d.setFont(SANS_FONT);
        final FontMetrics fm = g2d.getFontMetrics();
        final int badgeW = fm.stringWidth(info) + 14;
        final int badgeX = Math.max(10, (start.x + end.x) / 2 - badgeW / 2);
        final int badgeY = Math.max(15, (start.y + end.y) / 2 - 10);

        g2d.setColor(BADGE_BG);
        g2d.fillRoundRect(badgeX, badgeY, badgeW, 20, 6, 6);
        g2d.setColor(ACCENT_BLUE);
        g2d.drawRoundRect(badgeX, badgeY, badgeW, 20, 6, 6);
        g2d.setColor(TEXT_WHITE);
        g2d.drawString(info, badgeX + 7, badgeY + 14);

        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage stampStatus(final BufferedImage image, final String status) {
        if (null == image) return image;

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        final String stampText = String.format("STATUS: %s",
                (null == status || status.isBlank()) ? "FAILURE" : status.toUpperCase());

        g2d.setFont(SANS_BOLD);
        final FontMetrics fm = g2d.getFontMetrics();
        final int textW = fm.stringWidth(stampText) + 20;
        final int textH = 24;

        final int x = image.getWidth() - textW - 10;
        final int y = image.getHeight() - textH - 10;

        g2d.setColor(BADGE_BG);
        g2d.fillRoundRect(x, y, textW, textH, 6, 6);

        g2d.setColor(new Color(239, 68, 68)); // red border
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(x, y, textW, textH, 6, 6);

        g2d.setColor(TEXT_WHITE);
        g2d.drawString(stampText, x + 10, y + 16);

        g2d.dispose();
        return image;
    }

    private static void setupGraphics(final Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
