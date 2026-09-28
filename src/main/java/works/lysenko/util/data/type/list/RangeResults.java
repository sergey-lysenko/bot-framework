package works.lysenko.util.data.type.list;

import works.lysenko.util.apis.grid.v._ValidationResult;
import works.lysenko.util.apis.grid.v._ValidationResults;
import works.lysenko.util.data.enums.RangeResult;
import works.lysenko.util.data.enums.RangerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.spec.Numbers.ONE;

/**
 * Represents the results of a range validation process. Implements interfaces _ValidationResults and _ValidationResult.
 */
public class RangeResults implements _ValidationResults, _ValidationResult {

    private List<RangeResult> list = null;

    @Override
    public final void add(final _ValidationResult result) {

        if (isNull(list)) list = new ArrayList<>(1);
        list.add((RangeResult) result);
    }

    /**
     * Determines the worst result from the list of RangeResults and returns
     * a RangerResults object corresponding to that result.
     *
     * @return a RangerResults object representing the worst result in the list.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public final RangerResults basedOn() {

        int worst = -ONE;
        for (final RangeResult result : list) {
            if (isNotNull(result) && result.ordinal() > worst) worst = result.ordinal();
        }
        return new RangerResults((-ONE == worst) ? RangerResult.OK : RangerResult.values()[worst]);
    }

    @Override
    public final boolean isEmpty() {

        return isNull(list) || list.isEmpty();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public final boolean isPassed() {

        for (final _ValidationResult result : list) {
            if (!result.isPassed()) return false;
        }
        return true;
    }

    @Override
    public final String name() {

        return names();
    }

    @Override
    public final String names() {

        return (list.stream()
                .map(_ValidationResult::name)
                .collect(Collectors.joining(COMMA_SPACE)));
    }

    @Override
    public final String render() {

        return isPassed() ? gb(names()) : rb(names());
    }

    @Override
    public final long size() {

        return list.size();
    }
}
