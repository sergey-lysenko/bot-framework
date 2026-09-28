package works.lysenko.util.data.type.list;

import works.lysenko.util.apis.grid.v._ValidationResult;
import works.lysenko.util.apis.grid.v._ValidationResults;
import works.lysenko.util.data.enums.ColoursValidationResult;
import works.lysenko.util.data.enums.RangerResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.lang.C.COMMA_SPACE;

/**
 * The RangerResults class implements _ValidationResults and _ValidationResult interfaces
 * to manage a collection of RangerResult instances and provide validation results.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public class RangerResults implements _ValidationResults, _ValidationResult {

    private final List<RangerResult> list;

    /**
     * Constructs a new RangerResults object and initializes it with the provided RangerResult instances.
     * If no instances are provided, an empty list will be created.
     *
     * @param toAdd the RangerResult instances to be added to this object
     */
    public RangerResults(final RangerResult... toAdd) {
        // This will handle zero elements correctly by creating an empty list.
        list = Arrays.stream(toAdd).collect(Collectors.toList());
    }

    @Override
    public final void add(final _ValidationResult result) {

        list.add((RangerResult) result);
    }

    /**
     * Determines the worst validation result from a list of RangerResult instances
     * and returns the corresponding ColoursValidationResult.
     *
     * @return a ColoursValidationResult representing the worst result in the list or RANGER_FAILED if any failure exists.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public final ColoursValidationResult basedOn() {

        int worst = -1;
        for (final RangerResult result : list) {
            if (result.ordinal() > worst) worst = result.ordinal();
        }
        if (FAILED < worst) return ColoursValidationResult.RANGER_FAILED;
        else return ColoursValidationResult.values()[worst];
    }

    @Override
    public final boolean isEmpty() {

        return isNull(list) || list.isEmpty();
    }

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
