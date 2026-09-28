package works.lysenko.util.data.type.sets;

import works.lysenko.util.apis.grid.v._ValidationResult;
import works.lysenko.util.apis.grid.v._ValidationResults;
import works.lysenko.util.data.enums.GridValidationResult;
import works.lysenko.util.prop.grid.Result;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.U.UNABLE_TO_ADD_OK___;
import static works.lysenko.util.lang.U.UNABLE_TO_PROCESS_EMPTY___;
import static works.lysenko.util.lang.U.UNABLE_TO_PROCESS_NULL___;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.prop.grid.Allowed.noValidation;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a collection of GridValidationResult objects.
 */
@SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
public class GridValidationResults implements _ValidationResults, _ValidationResult {

    private Set<_ValidationResult> set = null;

    /**
     * Logs an event with empty severity and a specific message, then returns false.
     *
     * @return false, indicating that the result is empty and processing cannot continue
     */
    private static boolean emptyResult() {

        logEvent(Result.emptySeverity, UNABLE_TO_PROCESS_EMPTY___);
        return false;
    }

    /**
     * Executes the Grid Probe Result method with the given GridValidationResult object.
     *
     * @param result the GridValidationResult object to be added
     * @return the GridValidationResults object containing the added result
     */
    public static GridValidationResults mvr(final _ValidationResult result) {

        final GridValidationResults mvr = new GridValidationResults();
        mvr.add(result);
        return mvr;
    }

    /**
     * Evaluates whether the current validation can process a null result.
     * Logs appropriate messages based on the validation state.
     *
     * @return true if null results are allowed, false otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static boolean nullResult() {

        if (noValidation) {
            logDebug(b(c(EMPTY), GRID, VALIDATION, RESULT, ALLOWED));
            return true;
        } else {
            logEvent(Result.nullSeverity, UNABLE_TO_PROCESS_NULL___);
            return false;
        }
    }

    @SuppressWarnings("RedundantCollectionOperation")
    public final void add(final _ValidationResult result) {

        if (isNull(set)) set = new HashSet<>(1);
        if ((set.size() > ZERO) && set.contains(GridValidationResult.OK)) set.remove(GridValidationResult.OK);
        if ((!set.isEmpty()) && result.equals(GridValidationResult.OK)) fail(UNABLE_TO_ADD_OK___);
        set.add(result);
    }

    @Override
    public final boolean isEmpty() {

        return isNull(set) || set.isEmpty();
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public final boolean isPassed() {

        if (isNull(set)) return nullResult();
        if (set.isEmpty()) return emptyResult();
        for (final _ValidationResult result : set) if (!result.isPassed()) return false;
        return true;
    }

    @Override
    public final String name() {

        return names();
    }

    @Override
    public final String names() {

        return isNull(set) ? null : (set.stream()
                .map(_ValidationResult::name)
                .collect(Collectors.joining(COMMA_SPACE)));
    }

    public final String render() {

        return isPassed() ? gb(names()) : rb(names());
    }

    @Override
    public final long size() {

        return set.size();
    }
}
