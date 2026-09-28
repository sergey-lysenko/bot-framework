package works.lysenko.base.util;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.properties._Result;
import works.lysenko.util.data.records.RelativeOrAbsoluteFraction;
import works.lysenko.util.lang.word.P;

import java.awt.Color;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.BY;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.TYPE;
import static works.lysenko.util.data.enums.Severity.S0;
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
     * Retrieves the property value as the specified type.
     *
     * @return the property value casted to the appropriate type T, based on the class type of the property
     */
    public T result() {

        if (isNull(source)) return null;
        if (aType.getSimpleName().equals(Boolean.class.getSimpleName())) return (T) ((Boolean) Boolean.parseBoolean(source));
        if (aType.getSimpleName().equals(Character.class.getSimpleName())) return (T) source;
        if (aType.getSimpleName().equals(Color.class.getSimpleName())) return (T) (getColor(source));
        if (aType.getSimpleName().equals(Double.class.getSimpleName())) return (T) ((Double) Double.parseDouble(source));
        if (aType.getSimpleName().equals(Float.class.getSimpleName())) return (T) ((Float) Float.parseFloat(source));
        if (aType.getSimpleName().equals(Fraction.class.getSimpleName())) return (T) fr(source);
        if (aType.getSimpleName().equals(Integer.class.getSimpleName())) return (T) ((Integer) Integer.parseInt(source));
        if (aType.getSimpleName().equals(Long.class.getSimpleName())) return (T) ((Long) Long.parseLong(source));
        if (aType.getSimpleName().equals(RelativeOrAbsoluteFraction.class.getSimpleName()))
            return (T) RelativeOrAbsoluteFraction.raf(source);
        if (aType.getSimpleName().equals(String.class.getSimpleName())) return (T) source;
        logEvent(S0, b(c(TYPE), aType.getName(), OF, q(source), IS, NOT, SUPPORTED, BY, STRING, P.PARSER));
        return null;
    }
}
