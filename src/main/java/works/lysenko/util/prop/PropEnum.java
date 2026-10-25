package works.lysenko.util.prop;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.data.enums.*;

import java.awt.Color;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.properties;
import static works.lysenko.base.util.StringParser.create;
import static works.lysenko.util.Constants.*;
import static works.lysenko.util.chrs.___._128;
import static works.lysenko.util.chrs.___._255;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Strings.NULL;
import static works.lysenko.util.lang.word.F.*;
import static works.lysenko.util.lang.word.N.*;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The {@code PropEnum} class is an enumeration that implements the
 * {@code _PropEnum} interface.
 * It is used to define various constant properties, each associated with
 * specific types,
 * default values, and optional behaviour flags.
 * <p>
 * Each enum constant is associated with a specific property descriptor,
 * including:
 * <ul>
 * <li>A {@code Class<?>} type that represents the type of the property
 * value.</li>
 * <li>A default value represented as a {@code String}.</li>
 * <li>(Optionally) A silent flag indicating if the property is quiet in its
 * behaviour.</li>
 * <li>(Optionally) An execution-parameter flag indicating if the property is
 * exposed as an execution parameter.</li>
 * </ul>
 *
 * The {@code PropEnum} class provides utility methods to retrieve property
 * values,
 * property names, and associated metadata.
 * <p>
 * This enumeration supports handling of various data types such as
 * {@code Integer},
 * {@code Boolean}, {@code String}, and others, and is designed to manage
 * configuration properties
 * in a structured and type-safe manner.
 */
public enum PropEnum implements _PropEnum {

    _DATA_DELIMITER_L0(String.class, VRT_BAR),
    _DATA_DELIMITER_L1(String.class, _COMMA_),
    _DATA_DELIMITER_L2(String.class, SEM_CLN),
    _DATA_PERSIST(Boolean.class, FALSE),
    _DATA_SNAPSHOTS_ALLOWED(Boolean.class, FALSE),
    _DATA_SNAPSHOTS_FULL(Boolean.class, FALSE),
    _GRID_ACTION_MARKER_COLOUR(Color.class, b(_COMMA_, _255, _128, _0_, _128)),
    _GRID_ALLOWED_MARGIN_SEVERITY_REDUCTION(Integer.class, _2_),
    _GRID_ALLOWED_OTHER_SEVERITY_REDUCTION(Integer.class, _2_),
    _GRID_COLOUR_SIMILARITY_THRESHOLD(Double.class, _1_),
    _GRID_DEFAULT_COLOUR_RANGED_MARGIN(String.class, _0_),
    _GRID_DEFAULT_HORISONTAL(String.class, _1_, _SLASH_, _2_),
    _GRID_DEFAULT_HSV_MARGIN(Fraction.class, _0_),
    _GRID_DEFAULT_POLYCHROMY_MARGIN(Fraction.class, _0_),
    _GRID_DEFAULT_RESOLUTION(String.class, _3_, _ASTRS_, _3_),
    _GRID_DEFAULT_SCALE(String.class, _1_),
    _GRID_DEFAULT_VERTICAL(String.class, _1_, _SLASH_, _2_),
    _GRID_EMPTY_GRID_VALIDATION_ALLOWED(Boolean.class, FALSE),
    _GRID_EMPTY_RESULT_SEVERITY(Severity.class, _S0_),
    _GRID_LOG_LINE_MAX_LENGTH(Integer.class, _9_, _9_, _9_),
    _GRID_NULL_RESULT_SEVERITY(Severity.class, _S0_),
    _GRID_RANGER_LOG_ABSENT_RANGE(Boolean.class, TRUE),
    _GRID_RANGER_LOG_BOUNDARIES(Boolean.class, TRUE),
    _GRID_RANGER_LOG_NON_FAILING_OUT_OF_BOUNDS(Boolean.class, TRUE),
    _GRID_RANGER_LOG_ORDER_CHANGE(Boolean.class, TRUE),
    _GRID_RANGER_LOG_OVERSTRETCH(Boolean.class, TRUE),
    _GRID_RANGER_LOG_SCALE_CHECK(Boolean.class, TRUE),
    _GRID_RANGER_LOG_SCALE_DEBUG(Boolean.class, TRUE),
    _GRID_RANGER_LOG_SORT_CHECK(Boolean.class, TRUE),
    _GRID_RANGER_LOG_WIDE_RANGE(Boolean.class, TRUE),
    _GRID_RANGER_LOG_WIDTH(Integer.class, _9_, _6_),
    _GRID_SHRINK_EXPECTATIONS(Boolean.class, FALSE),
    _GRID_STAMPS_COMPRESS(Boolean.class, TRUE),
    _GRID_STAMPS_DISPLAY(Boolean.class, TRUE),
    _GRID_STAMPS_PROCESS(Boolean.class, TRUE),
    _GRID_UPDATE_COLOUR_EXPECTATIONS(Boolean.class, FALSE),
    _GRID_UPDATE_HSV_EXPECTATIONS(Boolean.class, FALSE),
    _GRID_UPDATE_PASSED_EXPECTATIONS(Boolean.class, FALSE),
    _GRID_UPDATE_POLYCHROMY_EXPECTATIONS(Boolean.class, FALSE),
    _GRID_VALIDATION_DEFAULT_BORDER(String.class, _1_, _SLASH_, _9_, _9_, _9_),
    _GRID_VALIDATION_FENCES(String.class, _9_),
    _GRID_VALIDATION_HSB_THRESHOLD(String.class, _1_, _SLASH_, _9_, _9_),
    _GRID_VALIDATION_SCALE_EPSILON(Double.class, _0_, _DOT_, _0_, _0_, _0_, _1_),
    _GRID_VERIFY_COLOURS(Boolean.class, FALSE),
    _GRID_WIDE_RANGE_THRESHOLD(Fraction.class, _1_, _SLASH_, _2_, _2_),
    _LOGS_ADEBUG(Boolean.class, FALSE),
    _LOGS_DEBUG(Boolean.class, FALSE, true),
    _LOGS_FULL_STACKTRACE(Boolean.class, FALSE),
    _LOGS_IGNORED_IN_STACKTRACE(String.class, EMPTY),
    _LOGS_LINES_SIMILARITY_CHECK(Boolean.class, TRUE),
    _LOGS_LINES_SIMILARITY_DELTA(Integer.class, _3_, _0_, _0_, _0_),
    _LOGS_LINES_SIMILARITY_DEPTH(Integer.class, _1_),
    _LOGS_LINES_SIMILARITY_EXCEPTIONS_PERMANENT(String.class, EMPTY),
    _LOGS_LINES_SIMILARITY_EXCEPTIONS_TEMPORARY(String.class, EMPTY),
    _LOGS_LINES_SIMILARITY_LIMIT(Integer.class, _5_),
    _LOGS_LINE_NUMBER_IN_SHORT_STACKTRACE(Boolean.class, FALSE),
    _LOGS_RATE_1(Integer.class, _1_, _9_),
    _LOGS_RATE_2(Integer.class, _4_, _9_),
    _LOGS_RATE_3(Integer.class, _9_, _9_),
    _LOGS_RATE_4(Integer.class, _9_, _9_, _9_),
    _LOGS_SILENT_SLEEPING_THRESHOLD(Integer.class, _1_, _0_),
    _LOGS_STACK_TRACE_REFERENCES(Boolean.class, TRUE),
    _LOGS_TABLE_WIDTH(Integer.class, _9_, _9_),
    _LOGS_TIME_STAMP_FORMAT(String.class, _DOT_, _0_, _0_, _0_),
    _LOGS_TO_READ(String.class, EMPTY),
    _LOGS_TRACE(Boolean.class, FALSE),
    _MATH_FRACTIONS_ACCURACY(Integer.class, _9_),
    _MATH_FRACTIONS_OUTPUT(Boolean.class, FALSE),
    _MOBI_NATIVE_X_RESOLUTION(Integer.class, _1_, _0_, _8_, _0_),
    _MOBI_NATIVE_Y_RESOLUTION(Integer.class, _2_, _2_, _8_, _0_),
    _MOBI_UD_ID(String.class, EMPTY),
    _SCREENSHOTS_MARKER_VARIANT(String.class, "neutral"),
    _SCREENSHOTS_OF_BINDS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_CLICKS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_LONG_CLICKS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_LONG_CLICKS_MARKER_DIAMETER(Integer.class, _5_, _0_),
    _SCREENSHOTS_OF_PIXELS(Boolean.class, FALSE),
    _SCREENSHOTS_OF_SWIPES(Boolean.class, FALSE),
    _SCREEN_TO_SPAWN_DASHBOARD(Integer.class, _0_),
    _SWIPES_BIG_REDUCTION(Float.class, _DASH_, _1_, _0_, _DOT_, _0_),
    _SWIPES_DEFAULT_REDUCTION(Float.class, _DASH_, _2_, _DOT_, _0_),
    _SWIPES_DEFAULT_WEIGHT(Float.class, _1_, _DOT_, _0_),
    _SWIPES_MEDIUM_REDUCTION(Float.class, _DASH_, _4_, _DOT_, _5_),
    _SWIPE_LINE_MARKER_COLOUR(Color.class, b(_COMMA_, _0_, _0_, _0_, _128)),
    _SWIPE_START_MARKER_COLOUR(Color.class, b(_COMMA_, _0_, _255, _0_, _128)),
    _SWIPE_STOP_MARKER_COLOUR(Color.class, b(_COMMA_, _255, _0_, _0_, _128)),
    _TEST_ACTION_RETRIES(Integer.class, _3_),
    _TEST_ALL_LEAFS(Boolean.class, FALSE, true, true),
    _TEST_ALL_LEAFS_EXECUTIONS(Integer.class, s(_1_), true, true),
    _TEST_APP(String.class, EMPTY),
    _TEST_ASSERTIONS_PRODUCE_EXCEPTION(Boolean.class, TRUE),
    _TEST_ASSERTIONS_SEVERITY(Severity.class, _S0_),
    _TEST_BUNDLE_ID(String.class, NULL),
    _TEST_EWAIT(Integer.class, _3_, _0_),
    _TEST_EXCEPTION_RETRIES(Integer.class, _3_),
    _TEST_EXECUTION_TIME_SAFEGUARD(String.class, _2_, _0_, VRT_BAR, _2_, _0_, _0_, _0_),
    _TEST_FAIL_BY_EVENT_OF_SEVERITY(Severity.class, _S1_, true),
    _TEST_HEADLESS(Boolean.class, FALSE, false, true),
    _TEST_KNOWN_ISSUES_FAILED_ONLY(Boolean.class, FALSE),
    _TEST_LONG_OPERATION_SEVERITY(Severity.class, _S2_),
    _TEST_LONG_OPERATION_THRESHOLD(Integer.class, _9_, _9_, _9_, _9_),
    _TEST_MAX_TYPING_DELAY(Integer.class, _5_, _0_),
    _TEST_PAUSE_LENGTH(Integer.class, _5_, _0_, _0_, _0_),
    _TEST_REPORT_ADD_ALL_LEAF(Boolean.class, TRUE),
    _TEST_REPORT_ADD_CPU_DEBUG(Boolean.class, TRUE),
    _TEST_REPORT_ADD_ETA_DEBUG(Boolean.class, TRUE),
    _TEST_REPORT_CPU_DENSITY(Integer.class, _5_, _0_, _0_, _0_),
    _TEST_REPORT_CPU_DENSITY_PER_TEST(Boolean.class, TRUE),
    _TEST_REPORT_LOG_SCALE_ALL_LEAF(Boolean.class, FALSE),
    _TEST_REPORT_LOG_SCALE_CPU_DEBUG(Boolean.class, FALSE),
    _TEST_REPORT_LOG_SCALE_ETA_DEBUG(Boolean.class, FALSE),
    _TEST_REPORT_LOG_SCALE_TIMELINE(Boolean.class, FALSE),
    _TEST_REPORT_OPEN_HTML(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_FFMPEG(String.class, FFMPEG),
    _TEST_REPORT_PROGRESSION_MAX_FRAMES(Integer.class, _5_, _0_, _0_, _0_),
    _TEST_REPORT_PROGRESSION_MAX_FRAME_PIXELS(Integer.class, _1_, _6_, _8_, M),
    _TEST_REPORT_PROGRESSION_MAX_TOTAL_PIXELS(Long.class, _1_, _0_, G),
    _TEST_REPORT_PROGRESSION_SCENARIO(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_SCENARIO_GIF(Boolean.class, FALSE),
    _TEST_REPORT_PROGRESSION_SCENARIO_MP4(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_SCENARIO_WEBP(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_TREE(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_TREE_GIF(Boolean.class, FALSE),
    _TEST_REPORT_PROGRESSION_TREE_MP4(Boolean.class, TRUE),
    _TEST_REPORT_PROGRESSION_TREE_PER_NODE(Boolean.class, FALSE),
    _TEST_REPORT_PROGRESSION_TREE_SONIFICATION(String.class, NONE),
    _TEST_REPORT_PROGRESSION_TREE_WEBP(Boolean.class, TRUE),
    _TEST_RESILIENT_MODE(Boolean.class, FALSE, true, true),
    _TEST_ROOT(String.class, EMPTY),
    _TEST_SCENARIO_SUFFICIENCY_ATTEMPTS(Integer.class, _9_),
    _TEST_SUITE_CHANGE_UPDATES_PARAMETERS(Boolean.class, TRUE),
    _TEST_TESTS(Integer.class, NULL, false, true),
    _TREE_BRANCH_STALE_PENALTY(Boolean.class, TRUE, false, true),
    _TREE_COMPLETION_WEIGHT(Fraction.class, s(_4_, _DOT_, _5_), false, true),
    _TREE_DEFAULT_WEIGHT(String.class, _0_, _DOT_, _0_),
    _TREE_FITS_BY_DEFAULT(Boolean.class, FALSE),
    _TREE_FORBID_OVEREXECUTION(Boolean.class, FALSE, false, true),
    _TREE_HIDE_OFF_BRANCHES(Boolean.class, FALSE),
    _TREE_IGNORE_FAILED_SCENARIO_SELECTION(Boolean.class, FALSE),
    _TREE_INCLUDE_DOWNSTREAM(Boolean.class, FALSE),
    _TREE_INCLUDE_UPSTREAM(Boolean.class, FALSE),
    _TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES(Boolean.class, TRUE),
    _TREE_PROGRESSION_OPTIMIZATION(Boolean.class, TRUE),
    _TREE_PROGRESSION_OPTIMIZATION_BANK_COLUMNS(Integer.class, _2_),
    _TREE_PROGRESSION_OPTIMIZATION_MAX_ASPECT_RATIO(Double.class, _0_, _DOT_, _8_, _5_),
    _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS(Integer.class, _2_),
    _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH(Integer.class, _1_, _2_),
    _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO(Double.class, _2_, _DOT_, _0_),
    _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD(Integer.class, _2_),
    _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD(Integer.class, _6_),
    _TREE_SCENARIO_DEPTH_SAFEGUARD(Integer.class, _2_, _0_),
    _TREE_SCENARIO_DOES_NOT_FIT_NOTICE(Boolean.class, TRUE),
    _TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD(Integer.class, _2_, _0_),
    _TREE_SCENARIO_NOT_EXECUTABLE_NOTICE(Boolean.class, TRUE),
    _TREE_SEQUENCE_MODE(SequenceMode.class, "SUGGESTION", false, true),
    _TREE_SEQUENCE_FAILURE_SEVERITY(Severity.class, _S0_, false, true),
    _TREE_TRAVERSE_EXTENSIONS(Boolean.class, FALSE, false, true),
    _TREE_UNREACHABLE_LEAF_PRUNING(Boolean.class, TRUE, false, true),
    _WEBD_FORCED_PLATFORM(String.class, NULL),
    _WEBD_IWAIT(Integer.class, _1_, _0_, _0_, _0_),
    _WEBD_RECREATE_DRIVER_AFTER_EXCEPTION(Boolean.class, TRUE);

    private final Class<?> type;
    private final String defaultValue;
    private final boolean silent;
    private final boolean executionParameter;

    /**
     * Initialises a property with all attributes.
     *
     * @param type               the class type which the property corresponds to
     * @param defaultValue       the default value for the property
     * @param silent             true if operations related to the property should be silent
     * @param executionParameter true if the property is also an execution parameter
     */
    PropEnum(final Class<?> type, final String defaultValue, final boolean silent,
            final boolean executionParameter) {

        this.type = type;
        this.defaultValue = defaultValue;
        this.silent = silent;
        this.executionParameter = executionParameter;
    }

    /**
     * Initialises a property with silent behaviour metadata.
     *
     * @param type         the class type which the property corresponds to
     * @param defaultValue the default value for the property
     * @param silent       true if operations related to the property should be silent
     */
    PropEnum(final Class<?> type, final String defaultValue, final boolean silent) {

        this(type, defaultValue, silent, false);
    }

    /**
     * Initialises a property with the specified type and default string value.
     *
     * @param type         the class type which the property corresponds to
     * @param defaultValue the default value for the property
     */
    PropEnum(final Class<?> type, final String defaultValue) {

        this(type, defaultValue, false);
    }

    /**
     * Initialises a property with the specified type and default character values.
     *
     * @param type         the class type which the property corresponds to
     * @param defaultChars the default character values for the property
     */
    PropEnum(final Class<?> type, final char... defaultChars) {

        this(type, new String(defaultChars));
    }

    @Override
    public String defaultValue() {

        return defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    /**
     * Performs get.
     * @return the get result
     */
    public <T> T get() {

        if (isNull(properties)) return (T) create(defaultValue, type).result();
        final T val = (T) properties.getEnum(this);
        return isNull(val) ? (T) create(defaultValue, type).result() : val;
    }

    @Override
    public String getPropertyName() {

        return switch (this) {
            case _SWIPE_LINE_MARKER_COLOUR -> ".swipes.marker.line.colour";
            case _SWIPE_START_MARKER_COLOUR -> ".swipes.marker.start.colour";
            case _SWIPE_STOP_MARKER_COLOUR -> ".swipes.marker.stop.colour";
            case _TREE_SEQUENCE_MODE -> ".tree.sequence.mode";
            case _TREE_SEQUENCE_FAILURE_SEVERITY -> ".tree.sequence.failure.severity";
            default -> _PropEnum.super.getPropertyName();
        };
    }

    @Override
    public boolean silent() {

        return silent;
    }

    @Override
    public boolean executionParameter() {

        return executionParameter;
    }

    @Override
    public Class<?> type() {

        return type;
    }
}
