package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.chrs.___;
import works.lysenko.util.data.enums.Brackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * The Text record holds two string values: value and max.
 * @param max
 * @param value
 */
public record Text(String value, String max) {

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private static String getPropertyValueFraction(final Map<Fraction, _ValuedRangeResult> shares, final int fences) {

        final Collection<String> result = new ArrayList<>(ZERO);
        for (final Map.Entry<Fraction, _ValuedRangeResult> value : shares.entrySet())
            result.add(b(L0, s((int) (value.getKey().doubleValue() * fences)), ts(value.getValue().value())));
        return (StringUtils.join(result, COMMA_SPACE));
    }

    /**
     * Constructs a Text object containing a maximum value and a fraction value
     * based on the provided shares and limit.
     *
     * @param shares the map of fractions representing shares
     * @param fences an integer representing the number of fences
     * @param li     the limit object representing overflow status and maximum value
     * @return a new Text object containing the constructed maximum and fraction values
     */
    public static Text text(final Map<Fraction, _ValuedRangeResult> shares, final int fences, final Limit li) {

        final String max = rb(li.overflow(), a(kv(s(___.MAX), s(ts(fr(li.max())), L0, li.max()))));
        final String value = e(Brackets.CURLY, getPropertyValueFraction(shares, fences));
        return new Text(max, value);
    }

    /**
     * Constructs a Text object containing a maximum value and a property value
     * based on the provided shares, fences, and limit.
     *
     * @param shares the _Quotas object representing shares
     * @param fences an integer representing the number of fences
     * @param li     the limit object representing overflow status and maximum value
     * @return a new Text object containing the constructed maximum and property values
     */
    public static Text text(final _Quotas<?> shares, final int fences, final Limit li) {

        final String max = rb(li.overflow(), a(kv(s(___.MAX), s(ts(fr(li.max())), L0, li.max()))));
        final String value = e(Brackets.CURLY, shares.getPropertyValue(fences));
        return new Text(max, value);
    }

}
