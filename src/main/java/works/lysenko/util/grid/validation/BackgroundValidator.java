package works.lysenko.util.grid.validation;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.sets.GridValidationResults;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.func.logs.trace.Data;
import works.lysenko.util.grid.expected.ColoursQuotas;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.ColourSearchRequest;
import works.lysenko.util.grid.record.gsrc.Resolution;
import works.lysenko.util.prop.grid.Background;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.isNull;
import static works.lysenko.Base.find;
import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.section;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.qinn;
import static works.lysenko.util.data.type.RelativePosition.CENTRE;
import static works.lysenko.util.func.imgs.Grid.test;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.grid.record.gsrc.Region.search;
import static works.lysenko.util.grid.validation.Check.byScaleResolutionExclude;
import static works.lysenko.util.grid.validation.Check.withoutGrid;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.word.B.BACKGROUND;
import static works.lysenko.util.lang.word.C.CHECK;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.spec.Numbers.ONE;

/**
 * The BackgroundValidator class provides methods to validate page elements and properties.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "SameParameterValue"})
public record BackgroundValidator(String locator, String gridName, ColoursQuotas expected, BufferedImage inImage,
                                  ColourSearchRequest foreground,
                                  Severity failureSeverity) {

    private static final double SCALE_FACTOR = 0.98; // TODO: get rid of this

    private static BackgroundValidator bv(final String locator, final String gridName, final ColoursQuotas expected,
                                          final BufferedImage inImage,
                                          final ColourSearchRequest foreground, final Severity failureSeverity) {

        return new BackgroundValidator(locator, gridName, expected, inImage, foreground, failureSeverity);

    }

    /**
     * Constructs a new BackgroundValidator with the provided parameters and delegates
     * to an internal overloaded method with additional default parameters.
     *
     * @param locator         The locator string used to identify the grid or element for validation.
     * @param expected        The expected ColoursQuotas object for validation.
     * @param foreground      The ColourSearchRequest object representing the foreground colours to search for.
     * @param failureSeverity The Severity object indicating the severity level of validation failures.
     * @return A BackgroundValidator object initialized with the provided parameters and defaults for omitted fields.
     */
    public static BackgroundValidator bv(final String locator, final ColoursQuotas expected,
                                         final ColourSearchRequest foreground,
                                         final Severity failureSeverity) {

        return bv(locator, null, expected, null, foreground, failureSeverity);
    }

    /**
     * Constructs a new BackgroundValidator with the provided parameters and delegates
     * to an internal overloaded method with additional default parameters.
     *
     * @param locator         The locator string used to identify the grid or element for validation.
     * @param expected        The expected ColoursQuotas object for validation.
     * @param foreground      The ColourSearchRequest object representing the foreground colours to search for.
     * @param inImage         The BufferedImage object on which the validation is performed.
     * @param failureSeverity The Severity object indicating the severity level of validation failures.
     * @return A BackgroundValidator object initialized with the given parameters.
     */
    public static BackgroundValidator bv(final String locator, final ColoursQuotas expected,
                                         final ColourSearchRequest foreground,
                                         final BufferedImage inImage, final Severity failureSeverity) {

        return bv(locator, null, expected, inImage, foreground, failureSeverity);
    }

    /**
     * Constructs a new BackgroundValidator with the provided parameters and delegates
     * to an overloaded internal method with additional default parameters.
     *
     * @param locator         The locator string used to identify the grid or element for validation.
     * @param gridName        The name of the grid to be used for validation.
     * @param foreground      The ColourSearchRequest object representing the foreground colours to search for.
     * @param inImage         The BufferedImage object on which the validation is performed.
     * @param failureSeverity The Severity object indicating the severity level of validation failures.
     * @return A BackgroundValidator object initialized with the given parameters.
     */
    public static BackgroundValidator bv(final String locator, final String gridName, final ColourSearchRequest foreground,
                                         final BufferedImage inImage, final Severity failureSeverity) {

        return bv(locator, gridName, null, inImage, foreground, failureSeverity);
    }

    /**
     * Constructs a new BackgroundValidator with the provided parameters and delegates to an internal overloaded method
     * with additional default parameters.
     *
     * @param locator         The locator string used to identify the grid or element for validation.
     * @param gridName        The name of the grid to be used for validation.
     * @param foreground      The ColourSearchRequest object representing the foreground colours to search for.
     * @param failureSeverity The Severity object indicating the severity level of validation failures.
     * @return A BackgroundValidator object initialized with the provided parameters and defaults for omitted fields.
     */
    public static BackgroundValidator bv(final String locator, final String gridName, final ColourSearchRequest foreground,
                                         final Severity failureSeverity) {

        return bv(locator, gridName, null, null, foreground, failureSeverity);
    }

    /**
     * Verifies the background validation process.
     * If the background validation flag is set to true, this method performs various validation steps
     * based on the provided image and parameters.
     * It checks if the image is centered, calculates the aspect ratio, sets the resolution,
     * creates a Request object, and verifies the grid using verifyGrid method.
     * If the validation fails, it logs the failure event.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public void doVerify() {

        if (Background.validation) {

            BufferedImage image = inImage;
            section(b(c(VALIDATION), OF, qinn(gridName, GRID), BACKGROUND));
            if (isNull(image))
                image = isNull(locator) ? makeScreenshot(d(gridName, BACKGROUND))
                        : makeScreenshot(find(locator), d(gridName, BACKGROUND));
            final Collection<_Region> exclude = isNull(foreground) ? null :
                    Objects.requireNonNull(search(foreground, image)).regions();
            final float aspect = (float) image.getHeight() / image.getWidth();
            final int x = Background.density;
            final int y = i((double) (x * aspect));
            final Resolution resolution = new Resolution(x, y);
            Trace.log(Data.traceable(aspect, resolution));
            final Fraction scale = fr(((ONE > aspect) ? 1 / aspect : aspect) * SCALE_FACTOR);
            final Request vr = isNotNull(gridName) ?
                    byScaleResolutionExclude(image, gridName, scale, resolution, exclude) :
                    withoutGrid(image, expected, CENTRE, fr(aspect * SCALE_FACTOR), resolution, exclude);
            final GridValidationResults result = test(vr, failureSeverity);
            if (!result.isPassed())
                logEvent(failureSeverity, b(qinn(gridName, c(GRID)), BACKGROUND, CHECK, FAILED, DUE_TO, result.names()));
        }
    }

}