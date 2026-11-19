package works.lysenko.util.apis.grid.d;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Location;
import works.lysenko.util.grid.Ellipse;

import static java.lang.StrictMath.pow;
import static works.lysenko.util.spec.Numbers.ONE;

/**
 * Interface defining methods for geometric operations related to ellipses, such as
 * checking if given points lie within filled or holed ellipses based on specified parameters.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Ellipse {

    /**
     * Checks if a given point is inside an ellipse.
     *
     * @param p the Location representing the point to be checked
     * @param c the Location representing the center of the ellipse
     * @param e the Ellipse representing the ellipse
     * @return true if the point is inside the ellipse, false otherwise
     */
    @SuppressWarnings("StandardVariableNames")
    static boolean isInEllipse(final _Location p, final _Location c, final Ellipse e) {

        return (pow(p.X() - c.x(), 2) / pow(e.rX, 2)) + (pow(p.Y() - c.y(), 2) / pow(e.rY, 2)) <= ONE;
    }

    /**
     * Checks if a given point is inside a filled ellipse.
     *
     * @param p  the Location representing the point to be checked
     * @param at the Geometry representing the centre point and radius of the filled ellipse
     * @param r  the radius of the filled ellipse
     * @param c  the Location representing the centre of the filled ellipse
     * @return true if the point is inside the filled ellipse, false otherwise
     */
    @SuppressWarnings("StandardVariableNames")
    static boolean isInFilledEllipse(final _Location p, final _Geometry at, final double r, final _Location c) {

        return isInEllipse(p, c, new Ellipse(at, r));
    }

    /**
     * Checks if a given point is inside a holed ellipse.
     *
     * @param p    the Location representing the point to be checked
     * @param at   the Geometry representing the centre point and radius of the holed ellipse
     * @param r    the radius of the holed ellipse
     * @param c    the Location representing the centre of the holed ellipse
     * @param fill the Fraction representing the fill factor of the holed ellipse
     * @return true if the point is inside the holed ellipse, false otherwise
     */
    @SuppressWarnings("StandardVariableNames")
    static boolean isInHoledEllipse(final _Location p, final _Geometry at, final double r, final _Location c,
                                    final Fraction fill) {

        final Ellipse eP = new Ellipse(at, r);
        final Ellipse eN = new Ellipse(at, r * (1 - fill.doubleValue()));
        return isInEllipse(p, c, eP) && !isInEllipse(p, c, eN);
    }

    /**
     * Checks if a given point is part of an ellipse based on the parameters.
     *
     * @param location   the Location representing the point to be checked
     * @param geometry   the Geometry representing the centre point and radius of the ellipse
     * @param resolution the Resolution representing the fill factor of the ellipse
     * @return true if the point is part of the ellipse, false otherwise
     */
    @SuppressWarnings("StandardVariableNames")
    static boolean isPartOfEllipse(final _Location location, final _Geometry geometry, final _Resolution resolution) {

        final _Location c = geometry.center();
        final double r = geometry.radius();
        final Fraction fill = resolution.fill();

        return (1 > resolution.fill().doubleValue()) ? isInHoledEllipse(location, geometry, r, c, fill) :
                isInFilledEllipse(location, geometry, r, c);

    }
}
