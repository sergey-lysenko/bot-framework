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
import java.util.List;
import java.util.Random;

/**
 * Military Map &amp; Tactical HUD style image annotation utility.
 * Adds tactical coordinates, corner brackets, crosshairs, and fun military callouts/stamps to screenshots.
 */
public class MilitaryMarker implements OperationMarker {

    public static final String MODE = "military";

    // Tactical HUD Color Palette
    private static final Color HUD_GREEN = new Color(0, 255, 65);
    private static final Color HUD_GREEN_BG = new Color(0, 30, 10, 190);
    private static final Color RADAR_AMBER = new Color(255, 176, 0);
    private static final Color RADAR_AMBER_BG = new Color(40, 25, 0, 190);
    private static final Color THREAT_RED = new Color(255, 50, 50);
    private static final Color THREAT_RED_BG = new Color(50, 0, 0, 200);

    private static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);
    private static final Font HEADER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 14);

    private static final Random RANDOM = new Random();

    private static final List<String> FAN_SERVICE_CALLOUTS = List.of(
            "TARGET"
    );

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public String description() {
        return "Military Map & Tactical HUD style annotations with coordinates and callouts";
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

        // 1. Semi-transparent element highlight & cross-hatch grid
        g2d.setColor(new Color(0, 255, 65, 30));
        g2d.fillRect(x, y, w, h);

        // Tactical cross-hatch grid
        Painter.drawCrossHatch(g2d, rect, new Color(0, 255, 65, 70), 14, 1.2f);

        // 2. Corner Brackets (Military Framing: ┌ ┐ └ ┘)
        g2d.setColor(HUD_GREEN);
        g2d.setStroke(new BasicStroke(2.5f));
        final int bracketLen = Math.min(Math.min(w, h) / 3, 20);

        // Top-Left
        g2d.drawLine(x, y, x + bracketLen, y);
        g2d.drawLine(x, y, x, y + bracketLen);
        // Top-Right
        g2d.drawLine(x + w, y, x + w - bracketLen, y);
        g2d.drawLine(x + w, y, x + w, y + bracketLen);
        // Bottom-Left
        g2d.drawLine(x, y + h, x + bracketLen, y + h);
        g2d.drawLine(x, y + h, x, y + h - bracketLen);
        // Bottom-Right
        g2d.drawLine(x + w, y + h, x + w - bracketLen, y + h);
        g2d.drawLine(x + w, y + h, x + w, y + h - bracketLen);

        // 3. Center Crosshair
        final int centerX = x + w / 2;
        final int centerY = y + h / 2;
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawLine(centerX - 8, centerY, centerX + 8, centerY);
        g2d.drawLine(centerX, centerY - 8, centerX, centerY + 8);
        g2d.drawOval(centerX - 5, centerY - 5, 10, 10);

        // 4. Tactical Callout HUD Box
        drawHudPanel(g2d, image.getWidth(), x, y, w, h, label, action, centerX, centerY);

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

        // Concentric Tactical Target Rings
        g2d.setColor(RADAR_AMBER);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(px - 25, py - 25, 50, 50);

        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(px - 15, py - 15, 30, 30);
        g2d.drawOval(px - 5, py - 5, 10, 10);

        // Crosshair reticle lines extending outward
        g2d.drawLine(px - 40, py, px - 10, py);
        g2d.drawLine(px + 10, py, px + 40, py);
        g2d.drawLine(px, py - 40, px, py - 10);
        g2d.drawLine(px, py + 10, px, py + 40);

        // Coordinates Label
        final String callout = getRandomCallout();
        final String text = String.format("[%s] GRID: (%d, %d)", action.toUpperCase(), px, py);
        final String sub = String.format("STATUS: %s", callout);

        g2d.setFont(MONO_FONT);
        final FontMetrics fm = g2d.getFontMetrics();
        final int textW = Math.max(fm.stringWidth(text), fm.stringWidth(sub)) + 16;
        final int textH = 36;

        int badgeX = px + 30;
        int badgeY = py - 40;
        if (badgeX + textW > image.getWidth() - 10) badgeX = px - textW - 30;
        if (badgeY < 10) badgeY = py + 20;

        g2d.setColor(RADAR_AMBER_BG);
        g2d.fillRect(badgeX, badgeY, textW, textH);
        g2d.setColor(RADAR_AMBER);
        g2d.drawRect(badgeX, badgeY, textW, textH);

        g2d.drawString(text, badgeX + 8, badgeY + 16);
        g2d.setColor(new Color(255, 220, 130));
        g2d.drawString(sub, badgeX + 8, badgeY + 30);

        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage markSwipe(final BufferedImage image, final Point start, final Point end) {
        if (null == image || null == start || null == end) return image;

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        // Vector line
        g2d.setColor(HUD_GREEN);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawLine(start.x, start.y, end.x, end.y);

        // Start indicator (Alpha circle)
        g2d.setColor(HUD_GREEN);
        g2d.fillOval(start.x - 8, start.y - 8, 16, 16);

        // End indicator (Arrowhead)
        final double angle = Math.atan2(end.y - start.y, end.x - start.x);
        final int arrowLen = 18;
        final int x1 = (int) (end.x - arrowLen * Math.cos(angle - Math.PI / 6));
        final int y1 = (int) (end.y - arrowLen * Math.sin(angle - Math.PI / 6));
        final int x2 = (int) (end.x - arrowLen * Math.cos(angle + Math.PI / 6));
        final int y2 = (int) (end.y - arrowLen * Math.sin(angle + Math.PI / 6));

        final int[] xPoints = {end.x, x1, x2};
        final int[] yPoints = {end.y, y1, y2};
        g2d.fillPolygon(xPoints, yPoints, 3);

        // Distance & Coordinates Overlay
        final double dist = Math.hypot(end.x - start.x, end.y - start.y);
        final String info = String.format("[SWIPE] DIST: %.0fpx | VECTOR (%d,%d)➔(%d,%d)",
                dist, start.x, start.y, end.x, end.y);

        g2d.setFont(MONO_FONT);
        final FontMetrics fm = g2d.getFontMetrics();
        final int badgeW = fm.stringWidth(info) + 16;
        final int badgeX = Math.max(10, (start.x + end.x) / 2 - badgeW / 2);
        final int badgeY = Math.max(20, (start.y + end.y) / 2 - 15);

        g2d.setColor(HUD_GREEN_BG);
        g2d.fillRect(badgeX, badgeY, badgeW, 22);
        g2d.setColor(HUD_GREEN);
        g2d.drawRect(badgeX, badgeY, badgeW, 22);
        g2d.drawString(info, badgeX + 8, badgeY + 15);

        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage stampStatus(final BufferedImage image, final String status) {
        if (null == image) return image;

        final Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        final String stampText = String.format("★ REPORT: %s ★",
                (null == status || status.isBlank()) ? "ISSUE" : status.toUpperCase());

        g2d.setFont(HEADER_FONT);
        final FontMetrics fm = g2d.getFontMetrics();
        final int textW = fm.stringWidth(stampText) + 24;
        final int textH = 28;

        final int x = image.getWidth() - textW - 15;
        final int y = image.getHeight() - textH - 15;

        g2d.setColor(THREAT_RED_BG);
        g2d.fillRect(x, y, textW, textH);

        g2d.setColor(THREAT_RED);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRect(x, y, textW, textH);

        g2d.drawString(stampText, x + 12, y + 19);

        g2d.dispose();
        return image;
    }

    private static void drawHudPanel(final Graphics2D g2d, final int imgW, final int x, final int y,
                                     final int w, final int h, final String label, final String action,
                                     final int centerX, final int centerY) {
        final String line1 = String.format("[%s] %s",
                (null == action || action.isBlank()) ? "TARGET" : action.toUpperCase(),
                getRandomCallout());
        final String line2 = String.format("GRID: (%d, %d) | SIZE: %dx%d", centerX, centerY, w, h);
        final String line3 = (null != label && !label.isBlank()) ? String.format("LABEL: \"%s\"", label) : null;

        g2d.setFont(MONO_FONT);
        final FontMetrics fm = g2d.getFontMetrics();

        int maxW = Math.max(fm.stringWidth(line1), fm.stringWidth(line2));
        if (null != line3) maxW = Math.max(maxW, fm.stringWidth(line3));
        final int boxW = maxW + 20;
        final int boxH = (null == line3) ? 42 : 58;

        int boxX = x;
        int boxY = y - boxH - 6;

        if (boxY < 5) boxY = y + h + 6;
        if (boxX + boxW > imgW - 5) boxX = imgW - boxW - 5;
        if (boxX < 5) boxX = 5;

        g2d.setColor(HUD_GREEN_BG);
        g2d.fillRect(boxX, boxY, boxW, boxH);

        g2d.setColor(HUD_GREEN);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRect(boxX, boxY, boxW, boxH);

        g2d.drawLine(boxX, boxY + boxH / 2, x, y);

        g2d.drawString(line1, boxX + 10, boxY + 16);
        g2d.setColor(new Color(180, 255, 200));
        g2d.drawString(line2, boxX + 10, boxY + 32);

        if (null != line3) {
            g2d.setColor(new Color(255, 255, 180));
            g2d.drawString(line3, boxX + 10, boxY + 48);
        }
    }

    private static void setupGraphics(final Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static String getRandomCallout() {
        return FAN_SERVICE_CALLOUTS.get(RANDOM.nextInt(FAN_SERVICE_CALLOUTS.size()));
    }
}
