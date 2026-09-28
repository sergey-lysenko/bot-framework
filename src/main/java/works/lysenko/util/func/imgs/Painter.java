package works.lysenko.util.func.imgs;

import org.apache.commons.math3.util.FastMath;
import org.openqa.selenium.Rectangle;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.t._BitPlane;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Location;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.records.Size;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.misc.DrawGridResult;
import works.lysenko.util.prop.grid.Marker;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;

import static works.lysenko.Base.log;
import static works.lysenko.util.apis.grid.d._Ellipse.isPartOfEllipse;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.HAS;
import static works.lysenko.util.chrs.___.OUT;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.THIS;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Shape.ELLIPCE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.imgs.BufferedImages.getCopyOf;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.grid.record.gsrc.Routines.getGridElementCoordinates;
import static works.lysenko.util.grid.record.gsrc.Routines.isWithin;
import static works.lysenko.util.lang.G.GRID_IS_TOO_DENSE___;
import static works.lysenko.util.lang.word.D.DENSITY;
import static works.lysenko.util.lang.word.P.POSSIBLE;
import static works.lysenko.util.lang.word.S.SAMPLES;
import static works.lysenko.util.spec.Symbols.*;

/**
 *
 */
@SuppressWarnings({"WeakerAccess", "StaticMethodOnlyUsedInOneClass", "MethodWithMultipleLoops", "unused"})
public record Painter() {

    /**
     * Generate readable description of provided {@link Rectangle}
     *
     * @param rectangle to describe
     * @return description
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static String describe(final Rectangle rectangle) {

        return b(s(H, rectangle.height), s(W, rectangle.width),
                s(_AT_SGN), s(X, rectangle.x), s(Y, rectangle.y));
    }

    /**
     * Draws a marked area on the provided image based on the given region information.
     *
     * @param image  the image to draw on
     * @param colour the colour to mark the area with
     * @param region the region information to determine the area to mark
     * @return the modified image with the marked area
     */
    public static BufferedImage drawArea(final BufferedImage image, final Paint colour, final _Region region) {

        final int width = i(region.scale().doubleValue() * image.getWidth());
        final int height = i(width / region.aspect().doubleValue());

        final int x = i(region.position().h() * image.getWidth() - (width / 2)) - 1;
        final int y = i(region.position().v() * image.getHeight() - (height / 2)) - 1;

        final Rectangle rect = new Rectangle(x, y, height, width);

        return drawArea(image, colour, rect);
    }

    /**
     * @param image  to mark
     * @param colour to mark area by
     * @param rect   to mark
     * @return marked image
     */
    public static BufferedImage drawArea(final BufferedImage image, final Paint colour, final Rectangle rect) {

        final Graphics2D g2d = image.createGraphics();
        g2d.setPaint(colour);
        g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2d.dispose();
        return image;
    }

    private static BufferedImage drawDraft(final BufferedImage image, final Paint color, final Location... points) {

        final int n = points.length;
        for (int i = 0; i < n; i++)
            drawLineOn(image, color, points[i], points[(i + 1) % n]);
        return image;
    }

    /**
     * Draws a grid on the provided BufferedImage based on the specified geometry, resolution, and exclusion region.
     *
     * @param image    the BufferedImage to draw the grid on
     * @param geometry the geometry of the grid with centre point, radius, and ratio
     * @param request  the validation request determining the resolution and exclusion region
     * @param grid     the grid containing sample points
     * @return the DrawGridResult containing the modified image, the bit buffer representation, and the sample centers
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "DataFlowIssue"})
    public static DrawGridResult drawGrid(final BufferedImage image, final _Geometry geometry, final Request request,
                                          final _Grid grid) {

        final int steps = FastMath.max(request.resolution().x(), request.resolution().y());
        final Size size = new Size(image.getWidth(), image.getHeight());
        final _BitPlane buffer = new BitPlane(size);
        final Collection<Point> centers = grid.getSamplePoints();
        final _Resolution resolution = request.resolution();

        final BufferedImage result = getCopyOf(image);
        fillBuffer(size, buffer, centers, geometry, resolution, steps, request.exclude());

        for (int y = 0; y < size.height(); y++)
            for (int x = 0; x < size.width(); x++)
                if (buffer.get(x, y)) drawPointOn(result, Marker.colour, new Location(x, y), 1);

        return getDrawGridResult(result, buffer, centers);
    }

    /**
     * Draws a grid on the provided BufferedImage based on the specified geometry, resolution, and exclusion region.
     *
     * @param image      the BufferedImage to draw the grid on
     * @param geometry   the geometry of the grid with centre point, radius, and ratio
     * @param resolution the resolution determining the number of steps for drawing the grid
     * @param exclude    the region to exclude while drawing the grid
     * @return the DrawGridResult containing the modified image, number of samples, and area covered
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "DataFlowIssue", "ObjectAllocationInLoop"})
    public static DrawGridResult drawGrid(final BufferedImage image, final _Geometry geometry, final _Resolution resolution,
                                          final Iterable<? extends _Region> exclude) {

        final int steps = FastMath.max(resolution.x(), resolution.y());
        final Size size = new Size(image.getWidth(), image.getHeight());
        final _BitPlane buffer = new BitPlane(size);
        final Collection<Point> centers = getCenterPoints(geometry, resolution, steps);

        final BufferedImage result = getCopyOf(image);
        fillBuffer(size, buffer, centers, geometry, resolution, steps, exclude);

        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                if (buffer.get(x, y)) drawPointOn(result, Marker.colour, new Location(x, y), 1);

        return getDrawGridResult(result, buffer, centers);
    }

    /**
     * Draws a line on the provided BufferedImage from one location to another with the specified paint color.
     *
     * @param image  the BufferedImage to draw the line on
     * @param colour the paint color of the line
     * @param from   the starting location of the line
     * @param to     the ending location of the line
     */
    public static void drawLineOn(final BufferedImage image, final Paint colour, final _Location from, final _Location to) {

        final Graphics2D g2d = image.createGraphics();
        g2d.setPaint(colour);
        g2d.drawLine(from.X(), from.Y(), to.X(), to.Y());
        g2d.dispose();
    }

    /**
     * Draws a point on a BufferedImage at the specified location with the given diameter and color.
     *
     * @param image    the BufferedImage to draw the point on
     * @param colour   the Paint color of the point
     * @param point    the Location of the point to be drawn
     * @param diameter the diameter of the point to draw
     */
    public static void drawPointOn(final BufferedImage image, final Paint colour, final _Location point, final int diameter) {

        final Graphics2D g2d = image.createGraphics();
        g2d.setPaint(colour);
        if (1 == diameter) {
            g2d.drawLine(point.X(), point.Y(), point.X(), point.Y());
            g2d.dispose();
        } else {
            final int r = diameter / 2;
            g2d.fillOval(point.X() - r, point.Y() - r, diameter, diameter);
        }
        g2d.dispose();
    }

    /**
     * Draws a rectangle on the provided BufferedImage using the specified colour and two corner points.
     *
     * @param img    the BufferedImage to draw the rectangle on
     * @param colour the colour of the rectangle
     * @param point1 the location of the first corner of the rectangle
     * @param point2 the location of the second corner of the rectangle
     * @return the modified image with the drawn rectangle
     */
    private static BufferedImage drawRectangle(final BufferedImage img, final Paint colour, final Location point1,
                                               final Location point2) {
        // Create the other two points of rectangle
        final Location point3 = new Location(point1.x(), point2.y());
        final Location point4 = new Location(point2.x(), point1.y());

        // Draw the rectangle using drawShape function
        return drawShape(img, colour, point1, point3, point2, point4);
    }

    /**
     * Draw a shape on the provided image with the specified colour and points.
     *
     * @param image  the BufferedImage to draw the shape on
     * @param colour the colour of the shape
     * @param points the array of Location points defining the shape
     * @return the modified image with the drawn shape
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    private static BufferedImage drawShape(final BufferedImage image, final Paint colour, final Location origin,
                                           final Location... points) {

        final Collection<Location> deDup = new HashSet<>(0);

        final int n = points.length;
        for (int i = 0; i < n; i++) {
            final Location p1 = points[i];
            final Location p2 = points[(i + 1) % n];

            if (p1.equals(p2)) {
                if (!deDup.contains(p1)) {
                    deDup.add(p1);
                    drawPointOn(image, colour, p1, 1);
                }
            } else {
                // Calculate direction vector from p1 to p2.
                final Location np2 = getLocation(p2, p1);

                drawLineOn(image, colour, p1, np2);
            }
        }
        return image;
    }

    /**
     * Draws a square on the provided BufferedImage with the specified colour and side length, centered position the given
     * Location.
     *
     * @param img        the BufferedImage to draw the square on
     * @param colour     the colour of the square
     * @param center     the centre Location of the square
     * @param sideLength the length of each side of the square
     * @return the modified BufferedImage with the drawn square
     */
    @SuppressWarnings({"SameParameterValue", "unused"})
    public static BufferedImage drawSquare(final BufferedImage img, final Paint colour, final Location center,
                                           final int sideLength) {
        // Calculate half-side length
        final int halfSideLength = sideLength / 2;

        // Calculate the top-left and bottom-right points of the square
        final Location topLeft = new Location(center.x() - halfSideLength, center.y() - halfSideLength);
        final Location bottomRight = new Location(center.x() + halfSideLength, center.y() + halfSideLength);

        // Draw the square using drawRectangle method
        return drawRectangle(img, colour, topLeft, bottomRight);
    }

    /**
     * Fills the given _BitBuffer with points based on the provided parameters.
     *
     * @param size       the size specifying the width and height dimensions
     * @param buffer     the _BitBuffer to be filled with points
     * @param centersSet the collection of centre Point objects to exclude from filling the buffer
     * @param geometry   the geometry representing the centre point, radius, and ratio of a grid
     * @param resolution the resolution determining the number of steps for filling the buffer
     * @param steps      the number of steps for the process
     * @param exclude    the region to exclude while filling the buffer
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "ObjectAllocationInLoop", "ContinueStatement"})
    private static void fillBuffer(final Size size, final _BitPlane buffer, final Collection<Point> centersSet,
                                   final _Geometry geometry, final _Resolution resolution, final int steps, final Iterable<?
                    extends _Region> exclude) {

        for (int iX = -resolution.x(); iX <= resolution.x(); iX++)
            for (int iY = -resolution.y(); iY <= resolution.y(); iY++) {
                final Location location = getGridElementCoordinates(geometry, steps, iX, iY);
                if ((ELLIPCE == resolution.shape() && !isPartOfEllipse(location, geometry, resolution)) || isWithin(exclude,
                        location, size))
                    continue;
                for (int dx = -1; 1 >= dx; dx++)
                    for (int dy = -1; 1 >= dy; dy++) {
                        if (0 == dx && 0 == dy) continue;
                        final int x = location.X() + dx;
                        final int y = location.Y() + dy;
                        if (!centersSet.contains(new java.awt.Point(x, y))) buffer.set(x, y);
                    }
            }
    }

    /**
     * Calculate the centre points within a specified size, geometry, resolution, and exclusion region.
     *
     * @param geometry   the geometry representing the centre point, radius, and ratio of a grid
     * @param resolution the resolution determining the number of steps for calculating the centre points
     * @param steps      the number of steps for drawing the grid
     * @return a collection of Point objects representing the calculated centre points
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    private static Collection<Point> getCenterPoints(final _Geometry geometry, final _Resolution resolution, final int steps) {

        final Collection<Point> centers = new HashSet<>(0);
        for (int iX = -resolution.x(); iX <= resolution.x(); iX++)
            for (int iY = -resolution.y(); iY <= resolution.y(); iY++) {
                final Location location = getGridElementCoordinates(geometry, steps, iX, iY);
                final java.awt.Point point = (new java.awt.Point(location.X(), location.Y()));
                if (!centers.add(point)) {
                    throw new IllegalArgumentException(GRID_IS_TOO_DENSE___);
                }
            }
        return centers;
    }

    /**
     * Retrieves the DrawGridResult based on the provided image, a bit buffer, and a set of centre points.
     *
     * @param image      the BufferedImage to generate the result for
     * @param buffer     the _BitBuffer representing the bit buffer
     * @param samplesSet the collection of centre Point objects
     * @return the DrawGridResult with the modified image, number of centre points, and total area covered
     */
    private static DrawGridResult getDrawGridResult(final BufferedImage image, final _BitPlane buffer,
                                                    final Collection<Point> samplesSet) {

        final Point upperLeft = buffer.nearBound();
        final Point lowerRight = buffer.farBound();
        final int area = (isAnyNull(upperLeft, lowerRight)) ? 0 :
                (lowerRight.x - upperLeft.x - 1) * (lowerRight.y - upperLeft.y - 1);
        final int samplesCount = samplesSet.size();
        log(b(c(THIS), GRID, HAS, yb(percentString(samplesCount, area)), DENSITY, WITH, yb(samplesCount), SAMPLES, OUT, OF,
                yb(area), POSSIBLE));

        return new DrawGridResult(image, samplesCount, area);
    }

    /**
     * Calculate the new location based on two input locations.
     *
     * @param p2 the ending location
     * @param p1 the starting location
     * @return the new location calculated based on the directions and distances between the two input locations
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static Location getLocation(final Location p2, final Location p1) {

        final float dx = p2.x() - p1.x();
        final float dy = p2.y() - p1.y();

        // Normalize the direction vector.
        final float len = (float) Math.sqrt(dx * dx + dy * dy);
        final float dirx = dx / len;
        final float diry = dy / len;

        // Move point 1 pixel away from the line end.
        return new Location((p2.x() - dirx), (p2.y() - diry));
    }
}
