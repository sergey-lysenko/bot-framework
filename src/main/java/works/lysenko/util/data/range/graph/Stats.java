package works.lysenko.util.data.range.graph;

import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.prop.grid.Ranges;

import java.util.*;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.ARE;
import static works.lysenko.util.chrs.___.THE;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.range.AbstractQuotas.UPPER_LIMIT;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.lang.word.C.CONSIDER;
import static works.lysenko.util.lang.word.O.OVERSTRETCHED;
import static works.lysenko.util.lang.word.R.RANGES;
import static works.lysenko.util.lang.word.R.REFORMULATING;
import static works.lysenko.util.lang.word.R.REQUIREMENTS;
import static works.lysenko.util.lang.word.S.SPAN;
import static works.lysenko.util.lang.word.S.STRETCH;
import static works.lysenko.util.lang.word.T.THESE;
import static works.lysenko.util.spec.Symbols._COMMA_;
import static works.lysenko.util.spec.Symbols._DOT_;

record Stats() {

    private static final double OVERSTRETCH = 1.1;

    /**
     * Calculates statistics based on the ranges.
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static void stats(final _Quotas<?> quotas) {

        final List<Quota<Number>> list = quotas.get().stream()
                .filter(Objects::nonNull)
                .map(share -> (Quota<Number>) share)
                .toList();

        final double minSum = list.stream().mapToDouble(range -> range.min().doubleValue()).sum();
        final double maxSum = list.stream().mapToDouble(range -> range.max().doubleValue()).sum();
        statsRow(minSum, maxSum);
    }

    static void stats(final Map<?, ? extends _ValuedRangeResult> results) {

        final Collection<_ValuedRangeResult> list = new ArrayList<>(results.values());
        final double value = list.stream().mapToDouble(_ValuedRangeResult::doubleValue).sum();
        statsRow(value);
    }

    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    private static void statsRow(final double value) {

        statsRow(value, value);
    }

    private static void statsRow(final double min, final double max) {

        if (Ranges.logOverstretch)
            if (UPPER_LIMIT.doubleValue() * OVERSTRETCH < max)
                logEvent(S2, b(c(THESE), RANGES, ARE, s(OVERSTRETCHED, _DOT_), c(CONSIDER), REFORMULATING, OF, THE,
                        REQUIREMENTS));
        Values.logNoLevelLine(b(c(STRETCH), s(percentString(max, UPPER_LIMIT.doubleValue()), _COMMA_), SPAN,
                s(percentString(max - min, UPPER_LIMIT.doubleValue()))));
    }
}
