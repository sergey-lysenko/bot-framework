package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Location;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.records.Size;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.grid.record.Request;

import java.awt.image.RenderedImage;

import static java.lang.StrictMath.max;
import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.min;
import static works.lysenko.util.data.records.Location.point;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Routines class contains static utility methods for performing various calculations and checks related to matrices.
 */
public record Routines() {


    /**
     * Calculates the coordinates of a grid element based on the given parameters.
     *
     * @param geometry the geometry representing the center point and radius of the grid
     * @param steps    the number of steps in each direction from the center
     * @param iX       the X index of the grid element
     * @param iY       the Y index of the grid element
     * @return a Location object representing the coordinates of the grid element
     */
    public static Location getGridElementCoordinates(final _Geometry geometry, final int steps, final int iX, final int iY) {

        final float distance = geometry.radius() / steps;
        final float dX = iX * distance;
        final float dY = iY * distance;
        final float x = geometry.center().x() + dX;
        final float y = geometry.center().y() + dY;
        return new Location(x, y);
    }

    /**
     * Returns the GridLocation representing the requested grid location based on the given Request and BufferedImage.
     *
     * @param vr The Request object containing the necessary parameters for calculating the requested grid location.
     * @param bi The BufferedImage object from which the grid location is determined.
     * @return The GridLocation object representing the requested grid location.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static Geometry getRequestedGridLocation(final Request vr, final RenderedImage bi) {

        if (isAnyNull(vr.position(), vr.scale())) return null;
        final double scale = (ZERO == vr.scale().doubleValue()) ? getStretchedScale(vr).doubleValue() : vr.scale().doubleValue();
        final float ratio = vr.resolution().ratio();
        return getRequestedGridLocation(bi.getWidth(), bi.getHeight(), vr.position().h(), vr.position().v(), scale, ratio);
    }

    /**
     * Returns the Geometry object representing the requested grid location based on the given parameters.
     *
     * @param width      the width of the image or grid
     * @param height     the height of the image or grid
     * @param horizontal the horizontal position of the grid center (0.0 to 1.0)
     * @param vertical   the vertical position of the grid center (0.0 to 1.0)
     * @param scale      the scale factor of the grid
     * @param ratio      the ratio of the grid
     * @return the Geometry object representing the requested grid location
     */
    @SuppressWarnings({"MethodWithTooManyParameters", "StaticMethodOnlyUsedInOneClass", "NumericCastThatLosesPrecision"})
    public static Geometry getRequestedGridLocation(final int width, final int height, final double horizontal,
                                                    final double vertical, final double scale, final float ratio) {

        Trace.log(traceable(width, height, horizontal, vertical, scale, ratio));
        final Location center = point(width * horizontal, height * vertical);
        final float radius = (float) ((min(width, height) - 1) * (scale * 0.5));
        Trace.log(traceable(center, radius, ratio));
        return new Geometry(center, radius, ratio);
    }

    /**
     * Calculates the stretched scale factor as a Fraction based on the height-to-width ratio of the image
     * in the given Request object.
     *
     * @param request the Request object containing the image data used to calculate the stretched scale
     * @return the stretched scale as a Fraction, based on the height-to-width ratio of the image
     */
    public static Fraction getStretchedScale(final Request request) {

        final float num = max(request.image().getRaster().getHeight(), request.image().getRaster().getWidth());
        final float den = min(request.image().getRaster().getHeight(), request.image().getRaster().getWidth());
        return fr(num / den);
    }

    /**
     * Determines whether a given location is within the specified regions.
     *
     * @param exclude  Iterable of Region objects representing regions to exclude
     * @param location Location object representing the point to check
     * @param in       Size object representing the dimensions
     * @return true if the location is within any of the regions, false otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean isWithin(final Iterable<? extends _Region> exclude, final _Location location, final Size in) {

        if (isNull(exclude)) return false;

        for (final _Region region : exclude) {
            final int width = i(region.scale().doubleValue() * in.width());
            final int height = i((width / (region.aspect().doubleValue())));
            final int x = i(region.position().h() * in.width()) - (width / 2) - 1;
            final int y = i(region.position().v() * in.height()) - (height / 2) - 1;
            final boolean withinX = (location.X() >= x) && (location.X() <= (x + width));
            final boolean withinY = (location.Y() >= y) && (location.Y() <= (y + height));
            if (withinX && withinY) return true;
        }
        return false;
    }
}
