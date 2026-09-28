package works.lysenko.base.properties.renderer;

import works.lysenko.util.data.records.PropertiesMeta;
import works.lysenko.util.prop.tree.Scenario;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.properties;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Symbols._BULLT_;
import static works.lysenko.util.spec.Symbols._DOT_;
import static works.lysenko.util.spec.Symbols._EQUAL_;

@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints",
        "BooleanMethodNameMustStartWithQuestion"})
record Keys() {

    @SuppressWarnings("WeakerAccess")
    static final String COMMON_MARKER = s(_BULLT_);

    private static String processUnknown(final Map.Entry<String, String> entry,
                                         final Collection<? super String> keysWithValidClasses) {

        final String key = entry.getKey();
        final String cl = searchClassReference(key);
        if (isNotNull(cl)) keysWithValidClasses.add(cl);
        return isNull(cl) ? rb(s(key, _EQUAL_, entry.getValue())) : gb(s(key, _EQUAL_, entry.getValue()));
    }

    @SuppressWarnings({"BooleanVariableAlwaysNegated", "CallToSuspiciousStringMethod", "MethodWithMoreThanThreeNegations",
            "MethodWithMultipleReturnPoints"})
    static String renderKeyValue(final Map.Entry<String, String> entry, final PropertiesMeta meta) {

        final String key = s(entry.getKey());
        if (!meta.defaults().containsKey(key)) return processUnknown(entry, meta.keysWithValidClasses());
        final String def = meta.defaults().get(key);
        final String value = entry.getValue();
        final boolean defaultValue = value.equals(def);
        if (!defaultValue) meta.defaults().remove(key);
        final String commonMarker = properties.isCommonValue(key, value) ? COMMON_MARKER : EMPTY;
        final String renderedKey = bb(!defaultValue, entry.getKey());
        final String equals = gb(isNull(def), s(_EQUAL_));
        final String join = isNull(def) ? e(equals) : equals;
        final String renderedValue = yb(!defaultValue, value);
        return s(commonMarker, renderedKey, join, renderedValue);
    }

    /**
     * Renders a key-value pair by applying various formatting based on certain conditions.
     *
     * @param entry the key-value pair to be rendered
     * @return the rendered key-value pair as a string
     */
    static String renderKeyValue(final Map.Entry<String, String> entry) {

        if (entry.getKey().startsWith(s(_DOT_))) return b(s(entry.getKey()), s(_EQUAL_), n(NULL, entry.getValue()));
        return b(entry.getKey(), s(_EQUAL_), entry.getValue());
    }

    private static String searchClassReference(final String key) {

        String className;
        if (isNull(key) || key.isEmpty()) throw new IllegalArgumentException("Key cannot be null or empty");

        final Pattern classNamePattern = Pattern.compile("(([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[A-Za-z_$][a-zA-Z\\d_$]*)");
        final Matcher matcher = classNamePattern.matcher(key);
        if (matcher.find()) {
            className = matcher.group(1);
            if (validateClassName(className)) logDebug(b(yb(className), IN, q(key)));
            else className = null;
        } else className = null;
        return className;
    }

    private static boolean validateClassName(final String className) {

        try {
            Class.forName(className); // Ensures the class exists in runtime
            return true;
        } catch (final ClassNotFoundException e) {
            try {
                Class.forName(s(Scenario.root, _DOT_, className)); // Ensures the class exists in runtime
                return true;
            } catch (final ClassNotFoundException e1) {
                return false; // Class not found
            }
        }
    }
}
