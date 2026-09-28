package works.lysenko.util.grid;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.g._Grid;
import works.lysenko.util.apis.grid.v._Validation;
import works.lysenko.util.data.enums.ColoursValidationResult;
import works.lysenko.util.data.enums.GridValidationResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.Grid;
import works.lysenko.util.data.type.sets.GridValidationResults;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.gsrc.Geometry;
import works.lysenko.util.prop.core.Screenshot;
import works.lysenko.util.prop.grid.Ranges;

import java.awt.image.BufferedImage;

import static java.util.Objects.isNull;
import static org.apache.commons.math3.util.FastMath.min;
import static works.lysenko.Base.isDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.section;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.HAD;
import static works.lysenko.util.chrs.____.BEEN;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.data.enums.GridValidationResult.*;
import static works.lysenko.util.data.enums.GridValidationResult.OK;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.qinn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.data.type.sets.GridValidationResults.mvr;
import static works.lysenko.util.func.Logs.logGrid;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.grid.colours.Routines.areColoursOk;
import static works.lysenko.util.func.grid.hsb.Routines.isBrightnessOk;
import static works.lysenko.util.func.grid.hsb.Routines.isHueOk;
import static works.lysenko.util.func.grid.hsb.Routines.isSaturationOk;
import static works.lysenko.util.func.grid.palette.Routines.isPaletteOk;
import static works.lysenko.util.func.grid.polychromy.Routines.isPolychromyOk;
import static works.lysenko.util.func.imgs.Painter.drawGrid;
import static works.lysenko.util.func.imgs.Screenshot.makeScreenshot;
import static works.lysenko.util.func.imgs.Screenshot.writeScreenshot;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.grid.record.gsrc.Routines.getRequestedGridLocation;
import static works.lysenko.util.grid.record.gsrc.Routines.getStretchedScale;
import static works.lysenko.util.grid.validation.Routines.*;
import static works.lysenko.util.lang.word.A.ALREADY;
import static works.lysenko.util.lang.word.B.BUILDING;
import static works.lysenko.util.lang.word.C.CALCULATING;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DEFAULT;
import static works.lysenko.util.lang.word.F.FULLSCREEN;
import static works.lysenko.util.lang.word.I.INVALID;
import static works.lysenko.util.lang.word.O.OTHER;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.P.PERFORMED;
import static works.lysenko.util.lang.word.R.RADIUS;
import static works.lysenko.util.lang.word.S.SCALING;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.U.UNNAMED;
import static works.lysenko.util.lang.word.V.*;
import static works.lysenko.util.spec.Numbers.TWO;
import static works.lysenko.util.spec.Symbols.X;
import static works.lysenko.util.spec.Symbols.Y;

/**
 * Probe class for validating pixel grid based on a Request object.
 */
public final class Probe implements _Validation {

    private final Request request;
    private Severity severity = S0;
    private GridValidationResults results = null;
    private _Grid grid = null;
    private BufferedImage image = null;
    private Geometry geometry = null;

    /**
     * Constructor for creating a new Probe object with the provided Request.
     *
     * @param request the Request object containing pixel grid information
     */
    private Probe(final Request request) {

        this.request = request;
    }

    /**
     * Constructor for creating a new Probe object with the provided Request, Tolerances, and Severity.
     *
     * @param request  the Request object containing pixel grid information
     * @param severity the severity level for the validation
     */
    private Probe(final Request request, final Severity severity) {

        this.request = request;
        this.severity = severity;
    }

    /**
     * Creates a new Probe object with the given Request and default Severity.
     *
     * @param request the validation request containing the pixel grid information
     * @return a new Probe object with the provided request and default severity
     */
    public static _Validation create(final Request request) {

        return new Probe(request);
    }

    /**
     * Creates a new Probe object with the given Request and Severity.
     *
     * @param request  the validation request containing the pixel grid information
     * @param severity the severity level of the validation
     * @return a new Probe object with the provided request and severity
     */
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static _Validation create(final Request request, final Severity severity) {

        return new Probe(request, severity);
    }

    @Override
    public void perform() {

        if (isNull(results)) {
            final GridValidationResults invalidRequest = validateRequest(request);
            if (invalidRequest.isEmpty()) _1_inputs();
            else results = invalidRequest;
        } else fail(b(c(VALIDATION), OF, q(request.name()), HAD, BEEN, ALREADY, PERFORMED));
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public GridValidationResults results() {

        if (isNull(results)) return new GridValidationResults();
        return results;
    }

    /**
     * Handles the initial input processing for the operation.
     * Processes the request data, captures or assigns an image,
     * calculates the grid location, and validates the geometry.
     * <p>
     * Performs the following steps:
     * 1. Sets the section with provided inputs including calculated values and configuration settings.
     * 2. Checks and assigns an image either from the request or by creating a screenshot using predefined settings.
     * 3. Determines the geometry of the requested grid location using the image and request parameters.
     * 4. Validates the calculated geometry, adding the result to the list if invalid.
     * 5. Proceeds to the next operation if validation is successful.
     * <p>
     * Private method meant to be used internally to initialise the workflow.
     */
    private void _1_inputs() {

        section(b(c(CALCULATING), qinn(request.name(), null), GRID));
        image = (null == request.image()) ? makeScreenshot(d(DEFAULT, FULLSCREEN, BY, GRID, VALIDATOR)) : request.image();
        geometry = getRequestedGridLocation(request, image);
        if (isNull(geometry)) results.add(INVALID_GEOMETRY);
        else _2_scale();
    }

    /**
     * Handles the scaling operations as part of a larger process. This method
     * coordinates the initialisation of scaling-related parameters, performs
     * debug and validation checks (if enabled), and proceeds to grid processing.
     * <p>
     * The method performs the following actions:
     * - Sets up scaling with the required parameters using auxiliary functions.
     * - Logs debug and validation details related to scaling operations if the
     * corresponding debug flags are enabled.
     * - Invokes the grid processing step as the next part of the process.
     * <p>
     * This is an internal private method used within the implementation flow to
     * manage scaling functionalities effectively.
     */
    private void _2_scale() {

        section(b(c(SCALING), qinn(request.name(), null), GRID));
        if (Ranges.logScaleDebug) logScaleDebug(request, getScale());
        if (Ranges.logScaleCheck) logScaleCheck(request, getScale());
        _3_grid();
    }

    private Fraction getScale() {

        return (0 == request.scale().doubleValue())
                ? getStretchedScale(request)
                : fr((geometry.radius() * TWO) / min(image.getWidth(), image.getHeight()));
    }

    /**
     * Handles the grid creation, validation, and processing steps in the workflow.
     * <p>
     * This method starts by constructing a grid section based on the given request parameters.
     * It checks for any null values in the required attributes (image, geometry, resolution).
     * If any required attribute is null, the method sends an invalid grid response and terminates.
     * Otherwise, it proceeds to the next build phase by invoking the `_4_build` method.
     * <p>
     * After that, if the grid is still null after the build process, it logs an event indicating an
     * inability to validate the grid for the specific request and sends an invalid grid response.
     * If the grid is not null, it moves forward to the verification phase by invoking the `_5_verify` method.
     */
    private void _3_grid() {

        section(b(c(BUILDING), qinn(request.name(), null), GRID));
        if (isAnyNull(image, geometry, request.resolution())) mvr(INVALID_GRID);
        else _4_build();
        if (isNull(grid)) {
            logEvent(S0, b(c(UNABLE), TO, VALIDATE, INVALID, q(request.name()), GRID));
            mvr(INVALID_GRID);
        } else _5_verify();
    }

    /**
     * Builds the grid structure using the given image, geometry, and request parameters.
     * Optionally captures a screenshot of the grid if the `pixels` flag in the `Screenshot` class is true.
     * Logs debug information if debugging is enabled.
     * <p>
     * This method performs the following operations:
     * - Creates a grid instance with the supplied image, geometry, and request.
     * - If the `Screenshot.pixels` flag is true, renders the grid to a screenshot
     *   and writes it to a file with an appropriate name.
     * - If debugging is enabled, logs the grid and request details for debugging purposes.
     */
    private void _4_build() {

        grid = new Grid(image, geometry, request);
        if (Screenshot.pixels) {
            final BufferedImage screenshot = drawGrid(image, geometry, request, grid).image();
            final String name = getName();
            writeScreenshot(screenshot, false, name);
        }
        if (isDebug()) logGrid(grid, request);
    }

    /**
     * Verifies the current state and performs a series of validation and processing operations.
     * <p>
     * This method is part of a sequential process, invoked to validate request data,
     * execute additional operations, and log results. It leverages helper methods
     * and utilities to ensure the data and state are correctly processed.
     * <p>
     * Operations performed:
     * 1. Validates the provided request name and associated grid data by invoking
     *    the `section` method with specific parameters for validation.
     * 2. Calls the `_6_colours` method to process colour-related data.
     * 3. Invokes the `_7_other` method to handle additional operations.
     * 4. Logs the result of the request processing using the `logResult` method.
     * <p>
     * This method relies on the following external components:
     * - `section` function to validate and process the grid and other request details.
     * - `_6_colours` and `_7_other` methods to handle specific parts of the operation.
     * - `logResult` to record the results of the performed operations.
     */
    private void _5_verify() {

        section(b(c(VALIDATING), qinn(request.name(), null), GRID));
        _6_colours();
        _7_other();
        logResult(request.name(), results);
    }

    /**
     * Validates the colours of a given grid based on the request input and checks if they meet the required criteria.
     * If the validation passes, the result is stored as a successful validation result.
     * If validation fails, a corresponding issue result is stored.
     * <p>
     * The method performs the following steps:
     * 1. Checks if the colour information in the request is not null.
     * 2. If colours information is present, validates the colours against the grid and severity levels.
     * 3. Logs the verification process with relevant details if validation results are present.
     * 4. Updates the results with either a successful validation result or an issue indicating a problem with the colours.
     * <p>
     * Note: This method seems to involve specific domain logic related to grid colours and determines whether
     * the colours comply with certain predefined validation rules.
     */
    private void _6_colours() {

        ColoursValidationResult coloursResult = null;
        if (isNotNull(request.irq().colours())) coloursResult = areColoursOk(request, grid, severity);
        {
            if (!isNull(coloursResult)) {
                section(b(c(VERIFYING), qinn(request.name(), null), GRID, COLOURS));
                if (coloursResult.isPassed()) results = mvr(GridValidationResult.copyOrdinalFrom(coloursResult));
                else results = mvr(GridValidationResult.COLOURS_VALIDATION_ISSUE);
            }
        }
    }

    /**
     * Executes additional validation checks in the context of grid color analysis if
     * certain conditions are met regarding the provided results.
     * <p>
     * The method primarily validates aspects such as palette, polychromy, hue,
     * saturation, and brightness. If the results indicate no previous issues or
     * null conditions, it proceeds with the checks and aggregates potential outcomes.
     * <p>
     * Key processing logic:
     * - If the `results` object is either null or deemed passed, initiates further validations.
     * - Handles conditions where color checks were not performed by initializing an empty
     *   `GridValidationResults` object.
     * - Performs a set of validations including `palette`, `polychromy`, `hue`, `saturation`,
     *   and `brightness`.
     * - If no issues are detected during the process, marks the validation result as OK.
     * <p>
     * This method also updates a verification section with the current request's
     * parameters and status as part of its operations.
     */
    private void _7_other() {

        if (isNotNull(results) && results.isPassed() || isNull(results)) {
            section(b(c(VERIFYING), OTHER, qinn(request.name(), null), GRID, PARAMETERS));
            // if either colours are acceptable, or no colour checks had been performed, proceed with other checks
            if (isNull(results)) results = new GridValidationResults(); // there were no colour checks
            palette();
            polychromy();
            hue();
            saturation();
            brightness();
            if (results.isEmpty()) results.add(OK); // if nothing bad happened, then it's OK
        }
    }

    /**
     * Performs a brightness validation check for the current request and updates
     * the validation results if the brightness check fails.
     * <p>
     * This method evaluates the brightness values defined in the request's image
     * requirements (`_ImageRequirements`) against the configured grid and severity settings.
     * If the brightness values do not meet the expected criteria, the result
     * `BRIGHTNESS_FAILED` is added to the validation results set.
     * <p>
     * The following steps are performed during the validation:
     * - Checks if the brightness requirements object in the request is not null.
     * - Validates the brightness values using the `isBrightnessOk` method. This
     * method compares the actual brightness data from the grid to the expected
     * quotas defined in the request.
     * - If the validation fails (brightness is not within acceptable bounds),
     * the result `BRIGHTNESS_FAILED` is added to the results collection.
     */
    private void brightness() {

        if (isNotNull(request.irq().brightness()) && !isBrightnessOk(request, grid, severity))
            results.add(BRIGHTNESS_FAILED);
    }

    /**
     * Retrieves the name based on the given request and geometry information.
     *
     * @return the constructed name string based on the request's name, resolution, grid, radius, center, and geometry details
     */
    private String getName() {

        return b(n(UNNAMED, request.name()),
                s((request.resolution().x() << 1) + 1, X, (request.resolution().y() << 1) + 1), GRID, OF,
                s(geometry.radius()), RADIUS, AT, s(X, geometry.center().x()), s(Y, geometry.center().y()));
    }

    /**
     * Performs a validation check on the hue values associated with the current request.
     * <p>
     * The method evaluates whether the hue values meet the expected criteria defined in the
     * request's image requirements and severity settings. If the hue validation fails, the
     * corresponding failure result `HUE_FAILED` is added to the validation results set for
     * further processing.
     * <p>
     * The following steps are performed during the validation:
     * 1. Checks if the hue object in the request's image requirements is not null.
     * 2. Validates the hue values using the `isHueOk` method, which compares the actual
     * and expected hue quotas defined in the request.
     * 3. If the hue validation fails, the result `HUE_FAILED` is added to the validation results.
     */
    private void hue() {

        if (isNotNull(request.irq().hue()) && !isHueOk(request, grid, severity))
            results.add(HUE_FAILED);
    }

    /**
     * Validates the palette associated with the current request and updates the validation results if
     * the palette check fails.
     * <p>
     * This method performs the following actions:
     * - Checks if the palette object associated with the current request is not null.
     * - Ensures that the palette validation passes using the `isPaletteOk` method.
     * - If the palette validation fails, adds a `PALETTE_FAILED` result to the validation results.
     */
    private void palette() {

        if (isNotNull(request.irq().palette()) && !isPaletteOk(request, grid))
            results.add(PALETTE_FAILED);
    }

    /**
     * Performs a validation check on the polychromy of the grid within the provided request.
     * <p>
     * The method evaluates whether the polychromy configuration of the current request meets
     * the expected criteria. If the polychromy check fails, a corresponding failure result
     * is added to the validation results.
     * <p>
     * The following steps are performed:
     * 1. Checks if the polychromy object in the request's image requirements is not null.
     * 2. Validates the polychromy value of the current grid using the `isPolychromyOk` method.
     * 3. If the validation fails (result not passed), the result `POLYCHROMY_FAILED` is added to the results set.
     */
    private void polychromy() {

        if (isNotNull(request.irq().polychromy()) && !isPolychromyOk(request, grid).isPassed())
            results.add(POLYCHROMY_FAILED);
    }

    /**
     * Performs a saturation validation check for the current request and updates
     * the validation results if the saturation check fails.
     * <p>
     * This method evaluates the saturation values defined in the request's image
     * requirements (`_ImageRequirements`) against the configured grid and severity settings.
     * If the saturation values do not meet the expected criteria, the result
     * `SATURATION_FAILED` is added to the validation results set.
     * <p>
     * The following steps are performed during the validation:
     * - Checks if the saturation requirements object in the request is not null.
     * - Validates the saturation values using the `isSaturationOk` method. This
     * method compares the actual saturation data from the grid to the expected
     * quotas defined in the request.
     * - If the validation fails (saturation is not within acceptable bounds),
     * the result `SATURATION_FAILED` is added to the results collection.
     */
    private void saturation() {

        if (isNotNull(request.irq().saturation()) && !isSaturationOk(request, grid, severity))
            results.add(SATURATION_FAILED);
    }
}
