package works.lysenko.util.spec;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.util.StringParser;
import works.lysenko.util.apis._PropEnum;

import java.awt.Color;
import java.util.Locale;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.properties;
import static works.lysenko.util.chrs.___._128;
import static works.lysenko.util.chrs.___._255;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Strings.NULL;
import static works.lysenko.util.lang.word.F.FALSE;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The {@code PropEnum} class is an enumeration that implements the {@code _PropEnum} interface.
 * It is used to define various constant properties, each associated with specific types,
 * default values, and optional behaviour flags.
 * <p>
 * Each enum constant is associated with a specific property descriptor, including:
 * <ul>
 *   <li>A {@code Class<?>} type that represents the type of the property value.</li>
 *   <li>A default value represented as a {@code String}.</li>
 *   <li>(Optionally) A silent flag indicating if the property is quiet in its behaviour.</li>
 * </ul>
 *
 * The {@code PropEnum} class provides utility methods to retrieve property values,
 * property names, and associated metadata.
 * <p>
 * This enumeration supports handling of various data types such as {@code Integer},
 * {@code Boolean}, {@code String}, and others, and is designed to manage configuration properties
 * in a structured and type-safe manner.
 */
@SuppressWarnings({"MissingJavadoc", "OverloadedVarargsMethod"})
public enum PropEnum implements _PropEnum {

    _ACTION_MARKER_COLOUR(Color.class, b(_COMMA_, _255, _128, _0_, _128)),
    _ACTION_RETRIES(Integer.class, _3_),
    _ADEBUG(Boolean.class, FALSE),
    _ALLOWED_MARGIN_SEVERITY_REDUCTION(Integer.class, _2_),
    _ALLOWED_OTHER_SEVERITY_REDUCTION(Integer.class, _2_),
    _APP(String.class, EMPTY),
    _ASSERTIONS_PRODUCE_EXCEPTION(Boolean.class, TRUE),
    _BUNDLE_ID(String.class, NULL),
    _COLOUR_SIMILARITY_THRESHOLD(Double.class, _1_),
    _DATA_SNAPSHOTS_ALLOWED(Boolean.class, FALSE),
    _DATA_SNAPSHOTS_FULL(Boolean.class, FALSE),
    _DEBUG(Boolean.class, FALSE, true),
    _DEFAULT_COLOUR_RANGED_MARGIN(String.class, _0_),
    _DEFAULT_HSV_MARGIN(Fraction.class, _0_),
    _DEFAULT_POLYCHROMY_MARGIN(Fraction.class, _0_),
    _DEFAULT_SCENARIO_SUFFICIENCY_ATTEMPTS(Integer.class, _9_),
    _DEFAULT_WEIGHT(String.class, _0_, _DOT_, _0_),
    _DELIMITER_L0(String.class, VRT_BAR),
    _DELIMITER_L1(String.class, _COMMA_),
    _DELIMITER_L2(String.class, SEM_CLN),
    _EMPTY_GRID_VALIDATION_ALLOWED(Boolean.class, FALSE),
    _EMPTY_RESULT_SEVERITY(Boolean.class, FALSE),
    _EWAIT(Integer.class, _3_, _0_),
    _EXCEPTION_RETRIES(Integer.class, _3_),
    _FAIL_BY_EVENT_OF_SEVERITY(Integer.class, s(S1.ordinal())),
    _FITS_BY_DEFAULT(Boolean.class, FALSE),
    _FORCED_PLATFORM(String.class, NULL),
    _FRACTIONS_ACCURACY(Integer.class, _9_),
    _FRACTIONS_OUTPUT(Boolean.class, FALSE),
    _FULL_STACKTRACE(Boolean.class, FALSE),
    _GRID_DEFAULT_HORISONTAL(String.class, _1_, _SLASH_, _2_),
    _GRID_DEFAULT_RESOLUTION(String.class, _3_, _ASTRS_, _3_),
    _GRID_DEFAULT_SCALE(String.class, _1_),
    _GRID_DEFAULT_VERTICAL(String.class, _1_, _SLASH_, _2_),
    _GRID_LOG_LINE_MAX_LENGTH(Integer.class, _9_, _9_, _9_),
    _GRID_VALIDATION_DEFAULT_BORDER(String.class, _1_, _SLASH_, _9_, _9_, _9_),
    _GRID_VALIDATION_FENCES(String.class, _9_),
    _GRID_VALIDATION_HSB_THRESHOLD(String.class, _1_, _SLASH_, _9_, _9_),
    _GRID_VALIDATION_SCALE_EPSILON(Double.class, _0_, _DOT_, _0_, _0_, _0_, _1_),
    _IGNORED_IN_STACKTRACE(String.class, EMPTY),
    _IGNORE_FAILED_SCENARIO_SELECTION(Boolean.class, FALSE),
    _INCLUDE_DOWNSTREAM(Boolean.class, FALSE),
    _INCLUDE_UPSTREAM(Boolean.class, FALSE),
    _IWAIT(Integer.class, _1_, _0_, _0_, _0_),
    _LINE_NUMBER_IN_SHORT_STACKTRACE(Boolean.class, FALSE),
    _LOGS_TO_READ(String.class, EMPTY),
    _LOG_LINES_SIMILARITY_CHECK(Boolean.class, TRUE),
    _LOG_LINES_SIMILARITY_DELTA(Integer.class, _3_, _0_, _0_, _0_),
    _LOG_LINES_SIMILARITY_DEPTH(Integer.class, _1_),
    _LOG_LINES_SIMILARITY_EXCEPTIONS_PERMANENT(String.class, EMPTY),
    _LOG_LINES_SIMILARITY_EXCEPTIONS_TEMPORARY(String.class, EMPTY),
    _LOG_LINES_SIMILARITY_LIMIT(Integer.class, _5_),
    _LOG_RATE_1(Integer.class, _1_, _9_),
    _LOG_RATE_2(Integer.class, _4_, _9_),
    _LOG_RATE_3(Integer.class, _9_, _9_),
    _LOG_RATE_4(Integer.class, _9_, _9_, _9_),
    _LOG_TABLE_WIDTH(Integer.class, _9_, _9_),
    _LOG_TIME_STAMP_FORMAT(String.class, _DOT_, _0_, _0_, _0_),
    _LONG_OPERATION_SEVERITY(Integer.class, _0_),
    _LONG_OPERATION_THRESHOLD(Integer.class, _9_, _9_, _9_, _9_),
    _MAX_TYPING_DELAY(Integer.class, _5_, _0_),
    _NATIVE_X_RESOLUTION(Integer.class, _1_, _0_, _8_, _0_),
    _NATIVE_Y_RESOLUTION(Integer.class, _2_, _2_, _8_, _0_),
    _NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES(Boolean.class, TRUE),
    _NULL_RESULT_SEVERITY(Integer.class, FALSE),
    _PAUSE_LENGTH(Integer.class, _5_, _0_, _0_, _0_),
    _PERSIST(Boolean.class, FALSE),
    _RANGER_LOG_ABSENT_RANGE(Boolean.class, TRUE),
    _RANGER_LOG_BOUNDARIES(Boolean.class, TRUE),
    _RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS(Boolean.class, TRUE),
    _RANGER_LOG_ORDER_CHANGE(Boolean.class, TRUE),
    _RANGER_LOG_OVERSTRETCH(Boolean.class, TRUE),
    _RANGER_LOG_SCALE_CHECK(Boolean.class, TRUE),
    _RANGER_LOG_SCALE_DEBUG(Boolean.class, TRUE),
    _RANGER_LOG_SORT_CHECK(Boolean.class, TRUE),
    _RANGER_LOG_WIDE_RANGE(Boolean.class, TRUE),
    _RANGER_LOG_WIDTH(Integer.class, _9_, _6_),
    _RECREATE_DRIVER_AFTER_EXCEPTION(Boolean.class, TRUE),
    _ROOT(String.class, EMPTY),
    _SCENARIO_DEPTH_SAFEGUARD(Integer.class, _2_, _0_),
    _SCENARIO_DOES_NOT_FIT_NOTICE(Boolean.class, TRUE),
    _SCENARIO_HISTORY_DEPTH_SAFEGUARD(Integer.class, _2_, _0_),
    _SCENARIO_NOT_EXECUTABLE_NOTICE(Boolean.class, TRUE),
    _SCREENSHOTS_OF_BINDS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_CLICKS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_LONG_CLICKS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER(Integer.class, _5_, _0_),
    _SCREENSHOTS_OF_PIXELS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_SWIPES(Boolean.class, FALSE),
    _SCREEN_TO_SPAWN_DASHBOARD(Integer.class, _0_),
    _SHRINK_EXPECTATIONS(Boolean.class, FALSE),
    _SILENT_SLEEPING_THRESHOLD(Integer.class, _1_, _0_),
    _STACK_TRACE_REFERENCES(Boolean.class, FALSE),
    _STAMPS_COMPRESS(Boolean.class, TRUE),
    _STAMPS_DISPLAY(Boolean.class, TRUE),
    _STAMPS_PROCESS(Boolean.class, TRUE),
    _SVG_HEIGHT(Integer.class, _7_, _6_, _8_),
    _SVG_WIDTH(Integer.class, _1_, _0_, _2_, _4_),
    _SWIPE_LINE_MARKER_COLOUR(Color.class, b(_COMMA_, _0_, _0_, _0_, _128)),
    _SWIPE_START_MARKER_COLOUR(Color.class, b(_COMMA_, _0_, _255, _0_, _128)),
    _SWIPE_STOP_MARKER_COLOUR(Color.class, b(_COMMA_, _255, _0_, _0_, _128)),
    _SWIPES_BIG_REDUCTION(Float.class, _DASH_, _1_, _0_, _DOT_, _0_),
    _SWIPES_DEFAULT_REDUCTION(Float.class, _DASH_, _2_, _DOT_, _0_),
    _SWIPES_MEDIUM_REDUCTION(Float.class, _DASH_, _4_, _DOT_, _5_),
    _SWIPES_DEFAULT_WEIGHT(Float.class, _1_, _DOT_, _0_),
    _TESTS(Integer.class, NULL),
    _TEST_EXECUTION_TIME_SAFEGUARD(String.class, _2_, _0_, VRT_BAR, _2_, _0_, _0_, _0_),
    _TRACE(Boolean.class, FALSE),
    _UD_ID(String.class, EMPTY),
    _UPDATE_COLOUR_EXPECTATIONS(Boolean.class, FALSE),
    _UPDATE_HSV_EXPECTATIONS(Boolean.class, FALSE),
    _UPDATE_PASSED_EXPECTATIONS(Boolean.class, FALSE),
    _UPDATE_POLYCHROMY_EXPECTATIONS(Boolean.class, FALSE),
    _VERIFY_COLOURS(Boolean.class, FALSE),
    _WIDE_RANGE_THRESHOLD(Fraction.class, _1_, _SLASH_, _2_, _2_);

    private final Class<?> type;
    private final String defaultValue;
    private final boolean silent;

    /**
     * Initialises a new instance of the PropEnum class with the specified type and default string value.
     *
     * @param type the class type which the property corresponds to
     * @param defaultStringValue the default string value for the property
     */
    PropEnum(final Class<?> type, final String defaultStringValue) {

        this.type = type;
        defaultValue = defaultStringValue;
        silent = false;
    }

    /**
     * Initialises a new instance of the PropEnum class with the specified type and default character value.
     *
     * @param type the class type which the property corresponds to
     * @param defaultCharValue the default character value for the property
     */
    PropEnum(final Class<?> type, final char defaultCharValue) {

        this.type = type;
        defaultValue = s(defaultCharValue);
        silent = false;
    }

    /**
     * Initialises a new instance of the PropEnum class with the specified type and default characters.
     *
     * @param type the class type which the property corresponds to
     * @param defaultChars the default character values for the property
     */
    PropEnum(final Class<?> type, final char... defaultChars) {

        this.type = type;
        defaultValue = new String(defaultChars);
        silent = false;
    }

    /**
     * Initialises a new instance of the PropEnum class with the specified type, default string value,
     * and a flag indicating whether the operation is performed silently.
     *
     * @param type the class type which the property corresponds to
     * @param defaultStringValue the default string value for the property
     * @param silent a boolean flag indicating if the property should operate in silent mode
     */
    PropEnum(final Class<?> type, final String defaultStringValue, final boolean silent) {

        this.type = type;
        defaultValue = defaultStringValue;
        this.silent = silent;

    }

    public String defaultValue() {

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {

        return (isNull(properties)) ? (T) StringParser.create(defaultValue, type).result() : (T) properties.getEnum(this);
    }

    public String getPropertyName() {

        return s(name().toLowerCase(Locale.ROOT).replace('_', '.'));
    }

    @Override
    public boolean silent() {

        return silent;
    }

    public Class<?> type() {

        return type;
    }
}
