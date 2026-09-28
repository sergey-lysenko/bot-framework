package works.lysenko.util.data.type;

import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.apis.grid.d._Ellipse;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.g._GridCalculator;
import works.lysenko.util.apis.grid.g._GridProcessor;
import works.lysenko.util.apis.grid.g._GridToString;
import works.lysenko.util.apis.grid.t._Geometry;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.v._ValidationRequest;
import works.lysenko.util.data.enums.Shape;
import works.lysenko.util.data.records.Element;
import works.lysenko.util.data.records.Location;
import works.lysenko.util.data.records.Size;
import works.lysenko.util.data.records.grid.BuildRequest;
import works.lysenko.util.data.records.grid.OutOfGridBounds;
import works.lysenko.util.grid.Calculator;
import works.lysenko.util.grid.Processor;
import works.lysenko.util.grid.ToString;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.grid.record.gsrc.Routines;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Shape.ELLIPCE;
import static works.lysenko.util.data.enums.Shape.RECTANGLE;
import static works.lysenko.util.data.records.Element.e;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.imgs.BufferedImages.getCopyOf;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.grid.record.gsrc.Routines.isWithin;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.G.GRID_IS_TOO_DENSE___;
import static works.lysenko.util.lang.word.C.CAUGHT;
import static works.lysenko.util.lang.word.C.CENTER;
import static works.lysenko.util.lang.word.E.EXCEPTION;
import static works.lysenko.util.lang.word.L.LOCATION;
import static works.lysenko.util.lang.word.R.RADIUS;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols.I;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;

/**
 * Represents a pixel grid.
 */
@SuppressWarnings({"unused", "WeakerAccess", "MethodWithMultipleReturnPoints", "ThisEscapedInObjectConstruction",
        "MethodWithMultipleLoops"})
public class Grid implements _Grid {

    private final Element[][] grid;
    private final _GridCalculator calculator = new Calculator(this);
    private final _GridProcessor processor = new Processor(calculator);
    private final _GridToString toString = new ToString(this);

    /**
     * Constructs a Grid based on the provided BufferedImage, Geometry, and InternalRequest.
     *
     * @param image    the BufferedImage to build the grid from
     * @param geometry the Geometry defining centre point, radius, and ratio of the grid
     * @param request  the InternalRequest containing resolution and regions to exclude
     */
    public Grid(final BufferedImage image, final _Geometry geometry, final _ValidationRequest request) {

        final boolean ignoreOOB = (request.scale().doubleValue() == ZERO);
        final BufferedImage copy = getCopyOf(image);
        final BuildRequest buildRequest = new BuildRequest(copy, geometry, request.resolution(), ignoreOOB);
        grid = build(buildRequest, request.exclude());
    }

    private static void addOutOfBounds(final Element[][] grid, final Geometry at, final ArrayIndexOutOfBoundsException e,
                                       final int iX, final int iY, final Location location) {

        final String info = a(List.of(kv(s(I, c(X)), iX), kv(s(I, c(Y)), iY), kv(s(X), location.x()), kv(s(Y), location.y())
                , kv(RADIUS, at.radius()), kv(CENTER, s(at.center()))), COMMA_SPACE);
        logEvent(S0, b(c(EXCEPTION), q(e.toString()), CAUGHT, WITH, info));
    }

    private static Element[][] build(final BuildRequest r, final Iterable<? extends _Region> exclude) {

        final int steps = FastMath.max(r.resolution().x(), r.resolution().y());
        if (RECTANGLE == r.resolution().shape()) return buildRectangle(new InternalRequest(r, steps), exclude);
        if (ELLIPCE == r.resolution().shape()) return buildEllipce(new InternalRequest(r, steps));
        return null;
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ObjectAllocationInLoop", "ProhibitedExceptionCaught"})
    private static Element[][] buildEllipce(final InternalRequest r) {

        final Element[][] grid = new Element[(r.r().resolution().y() << 1) + 1][(r.r().resolution().x() << 1) + 1];
        final OutOfGridBounds outOfBounds = new OutOfGridBounds();

        for (int iX = -r.r().resolution().x(); iX <= r.r().resolution().x(); iX++)
            for (int iY = -r.r().resolution().y(); iY <= r.r().resolution().y(); iY++) {
                Location location = null;
                Integer value = null;
                try {
                    location = Routines.getGridElementCoordinates(r.r().geometry(), r.steps(), iX, iY);
                    if (_Ellipse.isPartOfEllipse(location, r.r().geometry(), r.r().resolution()))
                        value = r.r().image().getRGB(location.X(), location.Y());
                    if (isNotNull(value))
                        grid[iY + r.r().resolution().y()][iX + r.r().resolution().x()] = e(location.X(), location.Y(), value);
                } catch (final ArrayIndexOutOfBoundsException e) {
                    if (!r.r().ignoreOOB()) outOfBounds.add(location, iX, iY);
                }
            }
        if (outOfBounds.isNotEmpty()) outOfBounds.summary();
        return grid;
    }

    @SuppressWarnings({"ProhibitedExceptionCaught", "ObjectAllocationInLoop"})
    private static Element[][] buildRectangle(final InternalRequest r, final Iterable<? extends _Region> exclude) {

        final Element[][] grid = new Element[(r.r().resolution().y() << 1) + 1][(r.r().resolution().x() << 1) + 1];
        final Size size = new Size(r.r().image().getWidth(), r.r().image().getHeight());
        final OutOfGridBounds outOfBounds = new OutOfGridBounds();


        for (int iX = -r.r().resolution().x(); iX <= r.r().resolution().x(); iX++)
            for (int iY = -r.r().resolution().y(); iY <= r.r().resolution().y(); iY++) {
                Location location = null;
                Integer value = null;
                try {
                    location = Routines.getGridElementCoordinates(r.r().geometry(), r.steps(), iX, iY);
                    if (!isWithin(exclude, location, size)) value = r.r().image().getRGB(location.X(), location.Y());
                    if (isNotNull(value))
                        grid[iY + r.r().resolution().y()][iX + r.r().resolution().x()] = e(location.X(), location.Y(), value);
                } catch (final ArrayIndexOutOfBoundsException e) {
                    if (!r.r().ignoreOOB()) outOfBounds.add(location, iX, iY);
                }
            }
        if (outOfBounds.isNotEmpty()) outOfBounds.summary();
        return grid;
    }

    private static void outOfBounds(final Element[][] grid, final Shape shape, final Geometry geometry,
                                    final ArrayIndexOutOfBoundsException e, final Location location, final int iX,
                                    final int iY) {

        if (isNull(location)) {
            fail(b(LOCATION, OF, shape.name(), IS, UNKNOWN));
        } else {
            addOutOfBounds(grid, geometry, e, iX, iY, location);
        }
    }

    @Override
    public final _GridCalculator calculator() {

        return calculator;
    }

    public final _GridCalculator getCalculator() {

        return calculator;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public final Element[][] getGrid() {

        return grid;
    }

    public final _GridProcessor getProcessor() {

        return processor;
    }

    @SuppressWarnings({"ObjectAllocationInLoop", "ArrayLengthInLoopCondition"})
    public final Collection<Point> getSamplePoints() {

        final Collection<Point> centers = new HashSet<>(0);
        for (int iX = 0; iX < grid[0].length; iX++)
            for (final Element[] elements : grid) {
                if (isNotNull(elements[iX])) {
                    final int x = elements[iX].x();
                    final int y = elements[iX].y();
                    final Point point = (new Point(x, y));
                    if (!centers.add(point)) {
                        throw new IllegalArgumentException(GRID_IS_TOO_DENSE___);
                    }
                }
            }
        return centers;
    }

    @Override
    public final _GridProcessor processor() {

        return processor;
    }

    public final _GridToString render() {

        return toString;
    }

    private record InternalRequest(BuildRequest r, int steps) {}
}
