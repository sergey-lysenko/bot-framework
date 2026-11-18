package works.lysenko.util.func.imgs;

import org.openqa.selenium.Rectangle;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.prop.core.Size;
import works.lysenko.util.prop.grid.Marker;

import java.awt.Point;
import java.awt.image.BufferedImage;

import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Painter.drawPointOn;

/**
 * Classic / Legacy image annotation utility.
 * Implements the original simple solid rectangle fill and point markers using Marker.colour.
 */
public class ClassicMarker implements OperationMarker {

    public static final String MODE = "classic";

    @Override
    public String mode() {
        return MODE;
    }

    @Override
    public String description() {
        return "Classic solid rectangle marker using configured Marker.colour";
    }

    @Override
    public BufferedImage markElement(final BufferedImage image, final Rectangle rect,
                                     final String label, final String action) {
        if (null == image || null == rect) return image;
        drawArea(image, Marker.colour, rect);
        final java.awt.Graphics2D g2d = image.createGraphics();
        Painter.drawCrossHatch(g2d, rect, new java.awt.Color(255, 128, 0, 120), 12, 1.5f);
        g2d.dispose();
        return image;
    }

    @Override
    public BufferedImage markPoint(final BufferedImage image, final Point point, final String action) {
        if (null == image || null == point) return image;
        drawPointOn(image, Marker.colour, new Location(point.x, point.y), Size.longClicksMarker);
        return image;
    }

    @Override
    public BufferedImage markSwipe(final BufferedImage image, final Point start, final Point end) {
        if (null == image || null == start || null == end) return image;
        Painter.drawLineOn(image, Marker.colour, new Location(start.x, start.y), new Location(end.x, end.y));
        return image;
    }

    @Override
    public BufferedImage stampStatus(final BufferedImage image, final String status) {
        return image; // no status stamp in classic mode
    }
}
