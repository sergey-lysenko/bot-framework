package works.lysenko.util.data.range.graph.values.integers;

import org.apache.commons.lang3.StringUtils;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.Brackets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static works.lysenko.util.chrs.___.MAX;
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
 * Represents a text object with a value and a maximum representation.
 * The `Text` class is implemented as a record, providing a concise and immutable abstraction.
 * @param max
 * @param value
 */
@SuppressWarnings("WeakerAccess")
public record Text(String value, String max) {

    private static String getPropertyValueInteger(final Map<Integer, _ValuedRangeResult> results) {

        final Collection<String> result = new ArrayList<>(ZERO);
        for (final Map.Entry<Integer, _ValuedRangeResult> value : results.entrySet())
            result.add(b(L0, s(value.getKey()), ts(value.getValue().value())));
        return (StringUtils.join(result, COMMA_SPACE));
    }

    static Text text(final _Quotas<?> quotas, final boolean overflow, final double max) {

        final String value = e(Brackets.CURLY, quotas.getPropertyValue());
        final String maxS = rb(overflow, a(kv(s(MAX), s(ts(fr(max)), L0, max))));
        return new Text(value, maxS);
    }

    static Text text(final Map<Integer, _ValuedRangeResult> results, final boolean overflow, final double max) {

        final String value = e(Brackets.CURLY, getPropertyValueInteger(results));
        final String maxS = rb(overflow, a(kv(s(MAX), s(ts(fr(max)), L0, max))));
        return new Text(value, maxS);
    }
}
