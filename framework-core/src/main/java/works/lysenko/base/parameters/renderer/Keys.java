package works.lysenko.base.parameters.renderer;

import works.lysenko.base.Parameters;
import works.lysenko.util.data.records.ParametersMeta;

import java.util.Map;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.spec.Symbols._ASTRS_;
import static works.lysenko.util.spec.Symbols._BULLT_;
import static works.lysenko.util.spec.Symbols._EQUAL_;

/**
 * Formats individual execution parameter key-value pairs with source markers and ANSI styling.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints"})
record Keys() {

    static final String FILE_MARKER = s(_BULLT_);
    static final String ENV_MARKER = s(_ASTRS_);

    private static String processUnknown(final Map.Entry<String, String> entry, final ParametersMeta meta) {

        final String key = entry.getKey();
        meta.unknownKeys().add(key);
        return rb(s(key, _EQUAL_, entry.getValue()));
    }

    static String renderKeyValue(final Parameters parameters,
                                 final Map.Entry<String, String> entry,
                                 final ParametersMeta meta) {

        final String key = s(entry.getKey());
        if (!meta.defaults().containsKey(key)) return processUnknown(entry, meta);
        final String def = meta.defaults().get(key);
        final String rawValue = parameters.getProperty(key, entry.getValue());
        final String displayValue = entry.getValue();
        final boolean defaultValue = displayValue.equalsIgnoreCase(def);
        meta.defaults().remove(key);

        final boolean isEnv = parameters.isEnvValue(key, rawValue);
        final boolean isFile = parameters.isFileValue(key, rawValue);
        final boolean isAdditional = parameters.isAdditionalParameter(key);

        final String marker = isEnv ? ENV_MARKER : (isFile ? FILE_MARKER : EMPTY);
        final String renderedKey = isEnv
                ? mb(entry.getKey())
                : (isAdditional ? gb(entry.getKey()) : bb(!defaultValue, entry.getKey()));
        final String equals = gb(isNull(def), s(_EQUAL_));
        final String join = isNull(def) ? e(equals) : equals;
        final String renderedValue = isEnv ? mb(displayValue) : yb(!defaultValue, displayValue);
        return s(marker, renderedKey, join, renderedValue);
    }

    static String renderKeyValue(final Map.Entry<String, String> entry) {

        return b(s(entry.getKey()), s(_EQUAL_), n(NULL, entry.getValue()));
    }
}
