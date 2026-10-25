package works.lysenko.util.grid;

import works.lysenko.util.apis.grid.d._Ellipse;
import works.lysenko.util.apis.grid.t._Geometry;

/**
 * The Ellipse class represents an ellipse shape in a two-dimensional space.
 */
@SuppressWarnings("WeakerAccess")
public final class Ellipse implements _Ellipse {

    /** Radius along the X-axis. */
    public final int rX;

    /** Radius along the Y-axis. */
    public final int rY;

    /**
     * Constructs an Ellipse with the specified geometry and radius.
     *
     * @the geometry providing ratio details
     * @r the base radius
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public Ellipse(final _Geometry at, final double r) {

        final float mX = (1 < at.ratio()) ? 1 / at.ratio() : 1.0F;
        final float mY = (1 > at.ratio()) ? at.ratio() : 1.0F;
        rX = (int) (r * mX);
        rY = (int) (r * mY);
    }
}

