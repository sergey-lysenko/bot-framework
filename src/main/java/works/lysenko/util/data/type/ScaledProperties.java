package works.lysenko.util.data.type;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._ScaledProperties;
import works.lysenko.util.data.enums.Brackets;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.data.records.grid.ScaledPropertiesOptions;
import works.lysenko.util.spec.Numbers;

import java.util.*;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.FIND;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs.____.REAL;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.ScaledPropertiesSilent.IF_NOT_SCALED;
import static works.lysenko.util.data.enums.ScaledPropertiesSilent.NO;
import static works.lysenko.util.data.enums.ScaledPropertiesSilent.YES;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.records.RangedMargin.rm;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.bn;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Properties.getPropertiesFromFile;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.A.AMBIGUOUS;
import static works.lysenko.util.lang.word.C.CANDIDATES;
import static works.lysenko.util.lang.word.E.EXISTENT;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.S.SEARCH;
import static works.lysenko.util.lang.word.U.UPDATE;
import static works.lysenko.util.spec.Symbols.*;

/**
 * ScaledProperties class represents a set of properties associated with scale compensation and aspect ratio.
 */
public class ScaledProperties implements _ScaledProperties {

    private static final String TAG = s(c(P), _DOT_, GET);
    private final Properties properties;
    private final ScaledPropertiesOptions options;

    /**
     * Constructs a ScaledProperties object by reading properties from the specified file path.
     *
     * @param path    the path to the properties file
     * @param options options for configuring the behavior of the ScaledProperties instance
     */
    public ScaledProperties(final String path, final ScaledPropertiesOptions options) {

        this.options = options;
        properties = getPropertiesFromFile(path, NO != options.silent());
    }

    /**
     * Retrieves the aspect of the execution by getting the current scale from the scaler.
     *
     * @return The Fraction representing the aspect of the execution.
     */
    private static Fraction getAspect() {

        return exec.getScaler().getCurrent();
    }

    /**
     * Retrieves the scale compensation.
     *
     * @return The Fraction representing the scale compensation.
     */
    private static Fraction getCompensation() {

        return exec.getScaler().getCompensation();
    }

    @SuppressWarnings({"CallToSuspiciousStringMethod", "MethodWithMultipleReturnPoints"})
    private static String getFieldDescriptor(final String key, final String realKey) {

        if (isNull(realKey) || key.equals(realKey)) return key;
        return bn(key, s(RGT_DAR), realKey);
    }

    /**
     * Retrieves a ScaledProperties object initialized with the specified file path and options.
     *
     * @param path    the path to the properties file
     * @param options options for configuring the behavior of the ScaledProperties instance
     * @return a ScaledProperties object with the properties loaded from the specified file path
     */
    @SuppressWarnings("unused")
    public static ScaledProperties getScaledProperties(final String path, final ScaledPropertiesOptions options) {

        return new ScaledProperties(path, options);
    }

    /**
     * Retrieves a ScaledProperties object initialized with the specified file path and default options.
     *
     * @param path the path to the properties file
     * @return a ScaledProperties object with the properties loaded from the specified file path
     */
    @SuppressWarnings("unused")
    public static ScaledProperties getScaledProperties(final String path) {

        return new ScaledProperties(path, new ScaledPropertiesOptions(IF_NOT_SCALED, false, false));
    }

    /**
     * Checks if a given string matches a default value.
     * If the input string is null, it evaluates it against the default value using a helper method.
     *
     * @param o   the input string to be checked
     * @param def the default value to be compared against
     * @return true if the input string matches the default value, false otherwise
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static boolean isDefault(final String o, final String def) {

        return n(EMPTY, o).equals(def);
    }

    /**
     * Checks if a given object matches a default value.
     * This method converts both the object and the default value to their string representations
     * and compares them using a helper method.
     *
     * @param o   the input object to be checked
     * @param def the default value to be compared against
     * @return true if the input object matches the default value, false otherwise
     */
    public static boolean isDefault(final Object o, final Object def) {

        return isDefault(o.toString(), def.toString());
    }

    @SuppressWarnings("CallToSuspiciousStringMethod")
    private static boolean isScaled(final String key, final String realKey) {

        return !isNull(realKey) && !realKey.equals(key);
    }

    @Override
    public final Properties get() {

        return properties;
    }

    public final String getAspectString() {

        return ts(getAspect(), false);
    }

    @Override
    public final KeyValue<String, String> getKV(final String key, final String def) {

        final String realKey = getRealKey(key, true);
        final String value = isNull(realKey) ? def : properties.getProperty(realKey, def);
        getLog(getFieldDescriptor(key, realKey), value, def, isScaled(key, realKey));
        return kv(realKey, value);
    }

    @Override
    public final KeyValue<String, String> getKV(final String key, final boolean isNullAllowed) {

        final String realKey = getRealKey(key, isNullAllowed);
        final KeyValue<String, String> kv = getKV(realKey, (String) null);
        if (!isNullAllowed && isNull(kv.v()))
            logEvent(S0, b(c(VALUE), FOR, KEY, kv.k(), IS, NULL));
        return kv;
    }

    public final KeyValue<String, RangedMargin> getKV(final String key, final RangedMargin def) {

        final String realKey = getRealKey(key, true);
        final RangedMargin value = isNull(realKey) ? def : rm(properties.getProperty(realKey, def.render()));
        getLog(getFieldDescriptor(key, realKey), value.render(), def.render(), isScaled(key, realKey));
        return kv(realKey, value);
    }

    @Override
    public final void updateProperty(final String key, final String value, final boolean allowNew) {

        if (allowNew || options.allowNew() || properties.containsKey(getScaled(key)) || properties.containsKey(key))
            updateAsScaled(key, value);
        else fail(b(UNABLE_TO, UPDATE, d(NON, EXISTENT), PROPERTY, KEY, getScaled(key)));
    }

    private Collection<String> getCandidates(final String key) {

        if (isNull(key)) throw new IllegalArgumentException(b(c(NULL), KEY, IS, NOT, ALLOWED, FOR, CANDIDATES, SEARCH));
        final Collection<String> candidates = new HashSet<>(1);
        candidates.add(getScaled(key));
        candidates.add(getScaledBack(key));
        if (getCompensation().equals(Fraction.ONE)) {
            candidates.add(s(key, _DOT_, getAspectString()));
            candidates.add(s(getAspectString(), _DOT_, key));
        }
        return candidates;
    }

    /**
     * Retrieves the log message based on the provided field and object, and logs a debug message if not silent.
     *
     * @param field the field to be included in the log message
     * @param o     the object to be included in the log message
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    private void getLog(final String field, final String o, final String def, final boolean isScaled) {

        if (isNotNull(field)) {
            final Fraction fraction = fr(o, true);
            final String hint = isNull(fraction) || o.equals(ts(false, fraction, false)) || o.equals(ts(fraction, true)) ?
                    EMPTY : gray(e(Brackets.ROUND, ts(true, fraction, false)));
            if (NO == options.silent() || (IF_NOT_SCALED == options.silent() && isScaled))
                logDebug(b(false, sn(TAG, e(ROUND, bb(field))),
                        gray(isDefault(o, def), RGT_DAR), yb(!isDefault(o, def), o), hint), true);
        }
    }

    /**
     * Retrieves the actual key matching the specified input key from the defined properties.
     * If multiple or no matching keys are found, an exception is thrown based on the parameters.
     *
     * @param key           the input key for which the real key needs to be determined
     * @param isNullAllowed a flag indicating whether a null value is allowed if no real key is found
     * @return the real key matching the input key from the properties, or null if allowed and no match is found
     * @throws IllegalArgumentException if null is not allowed and no matching key is found,
     *                                  or if multiple matching keys are found
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private String getRealKey(final String key, final boolean isNullAllowed) {

        if (!isNullAllowed || !isNull(key))
            return search(key, isNullAllowed);
        return key;
    }

    /**
     * Adds scale to key if the scale compensation is not equal to Fraction.ONE.
     * If the scale compensation is Fraction.ONE, returns the key as is.
     *
     * @param key The key to be scaled
     * @return The scaled key if scale compensation is not Fraction.ONE, otherwise the original key
     */
    private String getScaled(final String key) {

        return (options.noScales() || getCompensation().equals(Fraction.ONE)) ? key : s(getAspectString(), _DOT_, key);
    }

    private String getScaledBack(final String key) {

        return (options.noScales() || getCompensation().equals(Fraction.ONE)) ? key : s(key, _DOT_, getAspectString());
    }

    private String search(final String key, final boolean isNullAllowed) {

        String real = null;
        final Collection<String> candidates = getCandidates(key);
        final Collection<String> defined = new TreeSet<>();

        for (final String candidate : candidates) // search for defined scaled variant
            if (properties.containsKey(candidate)) {
                defined.add(candidate);
                real = candidate;
            }

        if (defined.isEmpty() && properties.containsKey(key)) { // attempt to fallback to non-scaled
            defined.add(key);
            real = key;
        }

        if (!isNullAllowed && defined.isEmpty())
            throw new IllegalArgumentException(b(UNABLE_TO, FIND, THE, REAL, KEY, FOR, q(key)));
        if (defined.size() > Numbers.ONE)
            throw new IllegalArgumentException(b(THERE_ARE, AMBIGUOUS, CANDIDATES, Arrays.toString(candidates.toArray()), OF
                    , REAL, KEY, FOR, q(key)));
        return real;
    }

    /**
     * Updates the property with the specified key as a scaled version.
     * The updated value is set in the property object and logged with debug information.
     *
     * @param key   the key of the property to update
     * @param value the new value to set
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")

    private void updateAsScaled(final String key, final String value) {

        final Object o = properties.setProperty(getScaled(key), value);
        final String info = ((null == o) || (null == value) || !o.toString().equals(value)) ? s(gb(value), RHT_ARR, rc(o)) :
                yb(o);
        if (YES != options.silent())
            logDebug(b(sn(UPDATE, e(ROUND, bb(getScaled(key))), e(s(RGT_DAR)), e(SQUARE, info))), true);
    }
}
