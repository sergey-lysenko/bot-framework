package works.lysenko.util.grid.validation;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.d._Resolution;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.t._RelativePosition;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.grid.record.Request;

import java.awt.image.BufferedImage;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.max;
import static org.apache.commons.math3.util.FastMath.min;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.section;
import static works.lysenko.util.apis.grid.g._GridProperties.getGridPropertiesByName;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.data.type.RelativePosition.rp;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenAndCodeSnapshot;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.H.HORIZONTAL;
import static works.lysenko.util.lang.word.P.POSITION;
import static works.lysenko.util.lang.word.R.REQUEST;
import static works.lysenko.util.lang.word.R.REQUESTED;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.lang.word.V.VERTICAL;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Represents a Factory for creating and managing validation requests.
 */
public record Factory() {

    /**
     * Creates a Request object with the given name, BufferedImage, Point, and dimensions.
     *
     * @param name          the name of the validation request
     * @param bufferedImage the BufferedImage object, can be null
     * @param location      the Point object representing the position
     * @param dimensions    the dimensions value
     * @return a new Request object with the given name, BufferedImage, Point, and dimensions
     */
    @SuppressWarnings("FloatingPointEquality")
    public static Request vr( // 1 internal usage
                              final String name,
                              final BufferedImage bufferedImage,
                              final Location location,
                              final int dimensions) {

        final _GridProperties gridProperties = getGridPropertiesByName(name);

        final BufferedImage bi = (null == bufferedImage) ? makeScreenAndCodeSnapshot(d(DEFAULT, FULLSCREEN, BY, VALIDATION,
                REQUESTED)) : bufferedImage;
        final double width = bi.getWidth();
        final double height = bi.getHeight();
        final double x = ZERO == location.x() ? 0.01 : location.x();
        final double y = ZERO == location.y() ? 0.01 : location.y();
        final RelativePosition rp = rp(x / width, y / height, 0.5 / max(width, height));
        logDebug(b(c(POSITION), a(HORIZONTAL, yb(ts(rp.horizontal())), _COMMA_), a(VERTICAL, yb(ts(rp.vertical())))));
        final Fraction scale = fr(((dimensions << 1) + 1.0) / min(width, height));
        final _Resolution resolution = works.lysenko.util.grid.validation.Routines.getResolution(gridProperties);

        return new Request(name, bi, rp, scale, resolution, null, _ImageRequirements.read(name, gridProperties));
    }

    /**
     * Returns a new Request object with the given name and RelativePosition.
     *
     * @param name the name of the validation request
     * @param at   the RelativePosition object representing the position
     * @return a new Request object with the given name and RelativePosition
     */
    public static Request vr( // no usages
                              final String name,
                              final RelativePosition at) {

        return vr(name, null, at);
    }

    /**
     * Creates a Request object with the given name, BufferedImage, and RelativePosition.
     *
     * @param name          the name of the validation request
     * @param bufferedImage the BufferedImage object
     * @param at            the RelativePosition object representing the position
     * @return a new Request object with the given name, BufferedImage, and RelativePosition
     */
    @SuppressWarnings("UseOfConcreteClass")
    public static Request vr( // 1 internal usage
                              final String name,
                              final BufferedImage bufferedImage,
                              final RelativePosition at) {

        final _GridProperties gridProperties = getGridPropertiesByName(name);

        final BufferedImage bi = (null == bufferedImage) ? makeScreenAndCodeSnapshot(d(DEFAULT, FULLSCREEN, BY, VALIDATION,
                REQUESTED)) : bufferedImage;
        final Fraction scale = works.lysenko.util.grid.validation.Routines.getScale(gridProperties);
        final _Resolution resolution = works.lysenko.util.grid.validation.Routines.getResolution(gridProperties);

        return new Request(name, bi, at, scale, resolution, null, _ImageRequirements.read(name, gridProperties));
    }

    /**
     * Generates a Request object based on the provided parameters with default values if necessary.
     *
     * @param name       the name of the validation request
     * @param image      the BufferedImage object representing the image to be validated
     * @param position   the RelativePosition object representing the position of the image
     * @param scale      the Fraction object representing the scale of the image
     * @param resolution the Resolution object representing the resolution of the image
     * @param exclude    the Region object representing the region to be excluded from validation
     * @return a new Request object with the provided parameters or default values
     */
    @SuppressWarnings({"ReassignedVariable", "AssignmentToMethodParameter"})
    public static Request vr( // 1 internal usages
                              final String name,
                              final BufferedImage image,
                              _RelativePosition position,
                              Fraction scale,
                              _Resolution resolution,
                              final Iterable<_Region> exclude) {

        section(b(c(VALIDATION), REQUEST, q(name)));
        final _GridProperties gridProperties = getGridPropertiesByName(name);

        if (isNull(position)) position = works.lysenko.util.grid.validation.Routines.getRelativePosition(gridProperties);
        if (isNull(scale)) scale = Routines.getScale(gridProperties);
        if (isNull(resolution)) resolution = works.lysenko.util.grid.validation.Routines.getResolution(gridProperties);

        return new Request(name, image, position, scale, resolution, exclude, _ImageRequirements.read(name,
                gridProperties));
    }
}
