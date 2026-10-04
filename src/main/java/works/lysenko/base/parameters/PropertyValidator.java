package works.lysenko.base.parameters;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.util.spec.PropEnum;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Validates property values entered in {@link PropertiesDialog} against the type declared in {@link PropEnum}
 * (plus per-property constraints). Properties unknown to {@link PropEnum} are free-form and always valid.
 * An empty value means "unset" and is always valid.
 */
public final class PropertyValidator {

    /**
     * Per-property constraints on string values (compared case-insensitively).
     */
    private static final Map<PropEnum, List<String>> ALLOWED_VALUES = Map.of(
            PropEnum._PROGRESSION_TREE_SONIFICATION, ProgressionSettings.treeSonificationValues());

    private PropertyValidator() {
    }

    /**
     * Validates a value for a property.
     *
     * @param propertyName property key, e.g. {@code .progression.tree.sonification}
     * @param value        proposed value
     * @return human-readable error, or {@code null} if the value is valid
     */
    public static String validate(final String propertyName, final String value) {

        if (null == value || value.isEmpty()) return null;
        final PropEnum property = find(propertyName);
        if (null == property) return null;

        final Class<?> type = property.type();
        final boolean isText = String.class == type || Character.class == type;
        if (!isText && !value.equals(value.strip())) {
            return "Value must not start or end with whitespace";
        }
        try {
            if (Boolean.class == type) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    return "Expected a boolean: true or false";
                }
            } else if (Integer.class == type) {
                Integer.parseInt(value);
            } else if (Long.class == type) {
                Long.parseLong(value);
            } else if (Double.class == type) {
                finite(Double.parseDouble(value));
            } else if (Float.class == type) {
                finite(Float.parseFloat(value));
            } else if (Color.class == type) {
                return validateColor(value, delimiter());
            } else if (Fraction.class == type) {
                if (null == fr(value, true)) return "Expected a fraction (e.g. 1/2) or a decimal number";
            } else if (works.lysenko.util.data.records.RelativeOrAbsoluteFraction.class == type) {
                return validateRelativeOrAbsolute(value);
            } else if (Character.class == type && 1 != value.length()) {
                return "Expected a single character";
            }
        } catch (final NumberFormatException e) {
            return "Expected " + describe(type) + ": '" + value + "' is not valid";
        } catch (final RuntimeException e) {
            return "Value is not valid for " + describe(type);
        }
        final List<String> allowed = ALLOWED_VALUES.get(property);
        if (null != allowed && allowed.stream().noneMatch(a -> a.equalsIgnoreCase(value.strip()))) {
            return "Allowed values: " + String.join(", ", allowed);
        }
        return null;
    }

    /**
     * Returns the known valid values for a property, or an empty list when it has no finite value set.
     *
     * @param propertyName property key, e.g. {@code .progression.tree.sonification}
     * @return known valid values
     */
    public static List<String> validValues(final String propertyName) {

        final PropEnum property = find(propertyName);
        if (null == property) return List.of();
        if (Boolean.class == property.type()) return List.of("true", "false");
        return ALLOWED_VALUES.getOrDefault(property, List.of());
    }

    private static PropEnum find(final String propertyName) {

        if (null == propertyName) return null;
        for (final PropEnum property : PropEnum.values()) {
            if (property.getPropertyName().equals(propertyName)) return property;
        }
        return null;
    }

    private static void finite(final double d) {

        if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException("not finite");
    }

    private static String delimiter() {

        final String l1 = PropEnum._DELIMITER_L1.get();
        return (null == l1 || l1.isEmpty()) ? "," : l1.substring(0, 1);
    }

    private static String validateColor(final String value, final String delimiter) {

        final String[] parts = value.split(java.util.regex.Pattern.quote(delimiter), -1);
        if (4 != parts.length) {
            return "Expected a colour as R" + delimiter + "G" + delimiter + "B" + delimiter + "A (4 integers 0-255)";
        }
        for (final String part : parts) {
            try {
                final int component = Integer.parseInt(part.strip());
                if (component < 0 || component > 255) return "Colour components must be in the range 0-255";
            } catch (final NumberFormatException e) {
                return "Colour component '" + part + "' is not an integer";
            }
        }
        return null;
    }

    private static String validateRelativeOrAbsolute(final String value) {

        final char marker = value.charAt(0);
        if ('0' == marker) return (1 == value.length()) ? null : "'0' must stand alone";
        if ('a' != marker && 'r' != marker) {
            return "Expected 'a' (absolute) or 'r' (relative) followed by a fraction, e.g. r1/2, or '0'";
        }
        return (null == fr(value.substring(1), true)) ? "Invalid fraction after '" + marker + "'" : null;
    }

    private static String describe(final Class<?> type) {

        return type.getSimpleName().toLowerCase(Locale.ROOT);
    }
}
