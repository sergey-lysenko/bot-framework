package works.lysenko.util.grid.record.gsrc;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._AbsoluteRegion;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.data.type.RelativePosition;
import works.lysenko.util.grid.AbsoluteRegion;
import works.lysenko.util.prop.core.Screenshot;
import works.lysenko.util.prop.grid.Marker;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.imgs.BufferedImages.getCopyOf;
import static works.lysenko.util.func.imgs.Painter.drawArea;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.lang.word.B.BINDING;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.F.FOUND;
import static works.lysenko.util.lang.word.R.REGION;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Represents a region within an image defined by a relative position, scale, and aspect ratio.
 */
public record Region(RelativePosition position, Fraction scale, Fraction aspect) implements _Region {

    /**
     * Binds regions within an image that fit a given colour search request.
     *
     * @param request the ColourSearchRequest defining the criteria for selecting regions
     * @param image   the BufferedImage to search for regions in
     * @return an Iterable of Region objects representing the found regions
     */
    @SuppressWarnings({"MethodWithMultipleLoops", "BreakStatement", "ObjectAllocationInLoop",
            "StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints"})
    public static ColoursSearchResult search(final ColourSearchRequest request, final BufferedImage image) {

        if (isNull(request)) return null;

        final Collection<_AbsoluteRegion> regions = new ArrayList<>(ONE);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int colour = image.getRGB(x, y);
                if (request.fits(colour)) {
                    boolean addRegion = true;
                    for (final _AbsoluteRegion region : regions) {
                        if (region.includes(x, y, request.normal())) {
                            region.expand(x, y);
                            addRegion = false;
                            break;
                        }
                    }
                    if (addRegion) regions.add(AbsoluteRegion.create(x, y));
                }
            }
        }
        logDebug(b(s1(regions.size(), REGION), FOUND));

        final Collection<_Region> result = new ArrayList<>(0);
        for (final _AbsoluteRegion region : regions) {

            final float centerX = (region.min().x() + region.max().x()) / 2;
            final float centerY = (region.min().y() + region.max().y()) / 2;

            final Fraction horizontal = fr(centerX / image.getWidth());
            final Fraction vertical = fr(centerY / image.getHeight());
            final RelativePosition at = new RelativePosition(horizontal, vertical);

            logTrace(s(region.min().x(), _COMMA_, region.min().y(), _COMMA_, region.max().x(), _COMMA_, region.max().y()));
            final Fraction scale = fr((region.max().x() - region.min().x()) / image.getWidth());
            final Fraction aspect = fr((region.max().x() - region.min().x()) / (region.max().y() - region.min().y()));

            result.add(new Region(at, scale, aspect));
        }

        if (Screenshot.binds) {
            BufferedImage img = getCopyOf(image);
            for (final _Region region : result) {
                img = drawArea(image, Marker.colour, region);
            }
            writeScreenshot(img, false, b(c(BINDING), OF, s1(result.size(), REGION), OF, s1(request.spreads().size(),
                    COLOUR)));
        }

        return new ColoursSearchResult(result);
    }
}
