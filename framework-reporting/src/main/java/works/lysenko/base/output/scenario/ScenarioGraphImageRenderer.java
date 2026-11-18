package works.lysenko.base.output.scenario;

import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Scenario;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import static works.lysenko.Base.core;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Renders scenario execution status bar charts to {@link BufferedImage} instances.
 */
public final class ScenarioGraphImageRenderer {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 680;

    private static final Color BG_COLOR = new Color(0x09, 0x0D, 0x16);
    private static final Color CARD_BG = new Color(0x11, 0x18, 0x27);
    private static final Color CARD_BORDER = new Color(0x1E, 0x29, 0x3B);
    private static final Color GRID_COLOR = new Color(30, 41, 59, 120);
    private static final Color ACCENT_BLUE = new Color(0x38, 0xBD, 0xF8);
    private static final Color GREEN_BAR = new Color(0x22, 0xC5, 0x5E);
    private static final Color AMBER_BAR = new Color(0xF5, 0x9E, 0x0B);
    private static final Color SLATE_BAR = new Color(0x33, 0x41, 0x55);
    private static final Color BAR_TRACK = new Color(30, 41, 59, 90);
    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
    private static final Color TEXT_DIM = new Color(0x64, 0x74, 0x8B);

    private ScenarioGraphImageRenderer() {
    }

    public static BufferedImage renderProgressionGraph(
            final List<_Scenario> scenarios,
            final int target,
            final int currentCycle) {

        final BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Canvas Background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Card Panel
        final int cardX = 24, cardY = 20, cardW = WIDTH - 48, cardH = HEIGHT - 40;
        g.setColor(CARD_BG);
        g.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
        g.setColor(CARD_BORDER);
        g.setStroke(new BasicStroke(1.0f));
        g.drawRoundRect(cardX, cardY, cardW, cardH, 12, 12);

        // Scenario Execution stats
        int completedCount = 0;
        int maxExecs = target;
        final int[] execsArray = new int[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            final _Scenario sc = scenarios.get(i);
            final int scTarget = (sc instanceof Mono) ? 1 : target;
            final int execs = (isNotNull(core) && isNotNull(core.getResults())) ? core.getResults().getExecutions(sc)
                    : 0;
            execsArray[i] = execs;
            if (execs >= scTarget)
                completedCount++;
            if (execs > maxExecs)
                maxExecs = execs;
        }

        // Header Title
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.setColor(TEXT_PRIMARY);
        g.drawString("SCENARIO EXECUTION PROGRESSION", cardX + 24, cardY + 36);

        // Header Badges
        final int badgeY = cardY + 22;
        int badgeX = cardX + cardW - 24;

        final long elapsedMillis = (isNotNull(timer))
                ? Math.max(0L, System.currentTimeMillis() - timer.startedAt())
                : 0L;
        final String elapsedStr = (isNotNull(timer))
                ? "ELAPSED: " + formatElapsedTime(elapsedMillis)
                : "ELAPSED: --:--:--";
        badgeX -= drawBadge(g, elapsedStr, badgeX, badgeY, CARD_BORDER, TEXT_MUTED) + 8;

        final String targetStr = String.format("TARGET: %d", target);
        badgeX -= drawBadge(g, targetStr, badgeX, badgeY, CARD_BORDER, TEXT_MUTED) + 8;

        final int totalScenarios = scenarios.size();
        final int pct = totalScenarios > 0 ? (completedCount * 100 / totalScenarios) : 0;
        final String completedStr = String.format("COMPLETED: %d/%d (%d%%)", completedCount, totalScenarios, pct);
        final Color compColor = (completedCount == totalScenarios) ? GREEN_BAR : AMBER_BAR;
        badgeX -= drawBadge(g, completedStr, badgeX, badgeY,
                new Color(compColor.getRed(), compColor.getGreen(), compColor.getBlue(), 40), compColor) + 8;

        final String cycleStr = String.format("TEST #%d", currentCycle);
        drawBadge(g, cycleStr, badgeX, badgeY, new Color(56, 189, 248, 40), ACCENT_BLUE);

        // Chart plotting area
        final int chartX = cardX + 54;
        final int chartY = cardY + 68;
        final int chartW = cardW - 74;
        final int chartH = cardH - 300;
        final int maxY = Math.max(target + 1, maxExecs + 1);

        // Horizontal Grid & Y-Ticks
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        final int ySteps = Math.min(maxY, 8);
        final int stepVal = Math.max(1, (int) Math.ceil((double) maxY / ySteps));

        for (int v = 0; v <= maxY; v += stepVal) {
            final int yPos = chartY + chartH - (int) Math.round(((double) v / maxY) * chartH);
            g.setColor(GRID_COLOR);
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine(chartX, yPos, chartX + chartW, yPos);

            g.setColor(TEXT_DIM);
            final String tick = String.valueOf(v);
            final FontMetrics fm = g.getFontMetrics();
            g.drawString(tick, chartX - fm.stringWidth(tick) - 8, yPos + 4);
        }

        // Target Line (dashed sky blue)
        if (target <= maxY) {
            final int targetY = chartY + chartH - (int) Math.round(((double) target / maxY) * chartH);
            final float[] dash = { 6.0f, 4.0f };
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g.setColor(new Color(56, 189, 248, 200));
            g.drawLine(chartX, targetY, chartX + chartW, targetY);

            // Target Tag
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            final String tag = String.format("TARGET: %d", target);
            final FontMetrics fmTag = g.getFontMetrics();
            final int tagW = fmTag.stringWidth(tag) + 8;
            g.setColor(CARD_BG);
            g.fillRect(chartX + chartW - tagW - 4, targetY - 14, tagW, 13);
            g.setColor(ACCENT_BLUE);
            g.drawString(tag, chartX + chartW - tagW, targetY - 4);
        }

        // Draw Bars
        final int count = scenarios.size();
        final double slotW = (double) chartW / count;
        final int barW = Math.max(4, Math.min(26, (int) (slotW * 0.72)));
        final double labelAngle = -Math.PI / 3.27;
        final double cosA = Math.cos(Math.abs(labelAngle));
        final double sinA = Math.sin(Math.abs(labelAngle));

        for (int i = 0; i < count; i++) {
            final _Scenario sc = scenarios.get(i);
            final int execs = execsArray[i];
            final int centerX = (int) Math.round(chartX + i * slotW + slotW / 2.0);
            final int barLeft = centerX - barW / 2;

            g.setColor(BAR_TRACK);
            g.fillRoundRect(barLeft, chartY, barW, chartH, 4, 4);

            if (execs > 0) {
                final int barH = Math.max(3, (int) Math.round(((double) execs / maxY) * chartH));
                final int barTop = chartY + chartH - barH;
                final int scTarget = (sc instanceof Mono) ? 1 : target;
                final Color barColor = (execs >= scTarget) ? GREEN_BAR : ACCENT_BLUE;
                g.setColor(barColor);
                g.fillRoundRect(barLeft, barTop, barW, barH, 4, 4);
                if (barH > 4) {
                    g.fillRect(barLeft, chartY + chartH - 4, barW, 4);
                }
            } else {
                g.setColor(SLATE_BAR);
                g.fillRect(barLeft, chartY + chartH - 2, barW, 2);
            }

            final int scTarget = (sc instanceof Mono) ? 1 : target;
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
            final String valStr = String.valueOf(execs);
            final FontMetrics fmVal = g.getFontMetrics();
            final int valW = fmVal.stringWidth(valStr);
            final int valY = (execs > 0)
                    ? (chartY + chartH - (int) Math.round(((double) execs / maxY) * chartH) - 4)
                    : (chartY + chartH - 6);
            final Color valColor = (execs >= scTarget) ? new Color(0x4A, 0xDE, 0x80)
                    : (execs > 0 ? ACCENT_BLUE : TEXT_DIM);
            g.setColor(valColor);
            g.drawString(valStr, centerX - valW / 2, Math.max(chartY + 12, valY));

            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            final String rawName = sc.getShortName();
            final Color nameColor = (execs >= scTarget) ? TEXT_PRIMARY : (execs > 0 ? TEXT_MUTED : TEXT_DIM);
            g.setColor(nameColor);

            final FontMetrics fm = g.getFontMetrics();

            final int maxW_X = (int) Math.floor((centerX - (cardX + 8)) / cosA);
            final int maxW_Y = (int) Math.floor(((cardY + cardH - 12) - (chartY + chartH + 10)) / sinA);
            final int maxAllowedW = Math.max(30, Math.min(maxW_X, maxW_Y));

            String displayName = rawName;
            if (fm.stringWidth(displayName) > maxAllowedW) {
                while (displayName.length() > 3 && fm.stringWidth("..." + displayName) > maxAllowedW) {
                    displayName = displayName.substring(1);
                }
                displayName = "..." + displayName;
            }

            final int textW = fm.stringWidth(displayName);
            final AffineTransform orig = g.getTransform();
            g.translate(centerX - 2, chartY + chartH + 10);
            g.rotate(labelAngle);
            g.drawString(displayName, -textW, 0);
            g.setTransform(orig);
        }

        g.dispose();
        return img;
    }

    public static String formatElapsedTime(final long elapsedMillis) {
        final long totalSeconds = Math.max(0L, elapsedMillis) / 1000;
        return String.format(
                java.util.Locale.ROOT,
                "%02d:%02d:%02d",
                totalSeconds / 3600,
                (totalSeconds / 60) % 60,
                totalSeconds % 60);
    }

    private static int drawBadge(
            final Graphics2D g,
            final String text,
            final int rightX,
            final int topY,
            final Color bg,
            final Color fg) {

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
        final FontMetrics fm = g.getFontMetrics();
        final int textW = fm.stringWidth(text);
        final int badgeW = textW + 16;
        final int badgeH = 20;
        final int leftX = rightX - badgeW;

        g.setColor(bg);
        g.fillRoundRect(leftX, topY, badgeW, badgeH, 6, 6);
        g.setColor(fg);
        g.drawString(text, leftX + 8, topY + 14);
        return badgeW;
    }
}
