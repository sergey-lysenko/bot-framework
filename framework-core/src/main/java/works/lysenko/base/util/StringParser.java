package works.lysenko.base.util;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.properties._Result;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.RelativeOrAbsoluteFraction;
import works.lysenko.util.lang.word.P;

import java.awt.Color;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.TYPE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.lang.word.S.STRING;
import static works.lysenko.util.lang.word.S.SUPPORTED;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.spec.Numbers.*;

/**
 * Represents a typed property with a getter method to retrieve the property value.
 *
 * @param <T> the type of the property value
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod", "unchecked"})
public final class StringParser<T> implements _Result<T> {

    private final String source;
    private final Class<?> aType;

    /**
     * Constructs a String object with the given source, name, and target type.
     *
     * @param source the Properties object containing the property
     * @param toType the target type of the property
     */
    private StringParser(final String source, final Class<?> toType) {

        if (isAnyNull(source, toType)) {
            this.source = null;
            aType = null;
        } else {
            this.source = source;
            aType = toType;
        }
    }

    /**
     * Creates a new String object with the given source, name, and target type.
     *
     * @param source the Properties object containing the property
     * @param toType the target type of the property
     * @param <T>    the type of the property value
     * @return a String object representing the specified property
     */
    public static <T> StringParser<T> create(final String source, final Class<?> toType) {

        return new StringParser<>(source, toType);
    }

    @SuppressWarnings("DataFlowIssue")
    private static Color getColor(final String source) {

        final String[] split = source.split(s(L1));
        if (isAnyNull(split[ZERO], split[ONE], split[TWO], split[THREE])) return null;
        else return new Color(i(split[ZERO]), i(split[ONE]), i(split[TWO]), i(split[THREE]));
    }

    /**
     * Performs parse suffixed long.
     * @param text the text
     * @return the parse suffixed long result
     */
    public static long parseSuffixedLong(final String text) throws NumberFormatException {
        if (null == text || text.isBlank()) {
            throw new NumberFormatException("Empty input");
        }
        final String trimmed = text.trim();
        final String lower = trimmed.toLowerCase(java.util.Locale.ROOT);

        double multiplier = 1.0;
        String numberPart = trimmed;

        if (lower.endsWith("gpx") || lower.endsWith("gp")) {
            multiplier = 1_000_000_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - (lower.endsWith("gpx") ? 3 : 2));
        } else if (lower.endsWith("g")) {
            multiplier = 1_000_000_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - 1);
        } else if (lower.endsWith("mpx") || lower.endsWith("mp")) {
            multiplier = 1_000_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - (lower.endsWith("mpx") ? 3 : 2));
        } else if (lower.endsWith("m")) {
            multiplier = 1_000_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - 1);
        } else if (lower.endsWith("kpx") || lower.endsWith("kp")) {
            multiplier = 1_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - (lower.endsWith("kpx") ? 3 : 2));
        } else if (lower.endsWith("k")) {
            multiplier = 1_000.0;
            numberPart = trimmed.substring(0, trimmed.length() - 1);
        }

        final double value = Double.parseDouble(numberPart.trim());
        final double total = value * multiplier;
        if (Double.isNaN(total) || Double.isInfinite(total) || total < 0 || total > Long.MAX_VALUE) {
            throw new NumberFormatException("Out of range: " + text);
        }
        return Math.round(total);
    }

    /**
     * Performs parse suffixed int.
     * @param text the text
     * @return the parse suffixed int result
     */
    public static int parseSuffixedInt(final String text) throws NumberFormatException {
        final long val = parseSuffixedLong(text);
        if (val < Integer.MIN_VALUE || val > Integer.MAX_VALUE) {
            throw new NumberFormatException("Integer overflow: " + text);
        }
        return (int) val;
    }

    /**
     * Retrieves the property value as the specified type.
     *
     * @return the property value casted to the appropriate type T, based on the class type of the property
     */
    public T result() {

        if (isNull(source)) return null;
        final String trimmed = source.trim();
        if (aType.getSimpleName().equals(Boolean.class.getSimpleName())) return (T) ((Boolean) Boolean.parseBoolean(trimmed));
        if (aType.getSimpleName().equals(Character.class.getSimpleName())) return (T) trimmed;
        if (aType.getSimpleName().equals(Color.class.getSimpleName())) return (T) (getColor(trimmed));
        if (aType.getSimpleName().equals(Double.class.getSimpleName())) return (T) ((Double) Double.parseDouble(trimmed));
        if (aType.getSimpleName().equals(Float.class.getSimpleName())) return (T) ((Float) Float.parseFloat(trimmed));
        if (aType.getSimpleName().equals(Fraction.class.getSimpleName())) return (T) fr(trimmed);
        if (aType.getSimpleName().equals(Integer.class.getSimpleName())) {
            try {
                return (T) ((Integer) parseSuffixedInt(trimmed));
            } catch (final NumberFormatException e) {
                if (trimmed.length() > 1 && (trimmed.startsWith("s") || trimmed.startsWith("S"))) {
                    try {
                        return (T) ((Integer) Integer.parseInt(trimmed.substring(1)));
                    } catch (final NumberFormatException ignored) {
                    }
                }
                logDebug(b(c(TYPE), aType.getName(), OF, q(source), IS, NOT, SUPPORTED, BY, STRING, P.PARSER));
                return null;
            }
        }
        if (aType.getSimpleName().equals(Long.class.getSimpleName())) {
            try {
                return (T) ((Long) parseSuffixedLong(trimmed));
            } catch (final NumberFormatException e) {
                logDebug(b(c(TYPE), aType.getName(), OF, q(source), IS, NOT, SUPPORTED, BY, STRING, P.PARSER));
                return null;
            }
        }
        if (aType.getSimpleName().equals(RelativeOrAbsoluteFraction.class.getSimpleName()))
            return (T) RelativeOrAbsoluteFraction.raf(trimmed);
        if (aType.getSimpleName().equals(String.class.getSimpleName())) return (T) source;
        if (aType.isEnum()) {
            for (final Object value : aType.getEnumConstants()) {
                final Enum<?> enumValue = (Enum<?>) value;
                if (enumValue.name().equalsIgnoreCase(trimmed)) return (T) enumValue;
            }
            if (aType == Severity.class) {
                try {
                    final int code = Integer.parseInt(trimmed);
                    if (code >= 0 && code < Severity.values().length) {
                        return (T) Severity.byCode(code);
                    }
                } catch (final NumberFormatException ignored) {
                }
            }
            try {
                return (T) Enum.valueOf((Class) aType, trimmed);
            } catch (final IllegalArgumentException e) {
                logDebug(b(c(TYPE), aType.getName(), OF, q(source), IS, NOT, SUPPORTED, BY, STRING, P.PARSER));
                return null;
            }
        }
        logDebug(b(c(TYPE), aType.getName(), OF, q(source), IS, NOT, SUPPORTED, BY, STRING, P.PARSER));
        return null;
    }
}
