package works.lysenko.util.grid.record.gsrc;

import works.lysenko.util.apis.grid.t._Region;
import works.lysenko.util.apis.grid.v._ValidationResult;

import java.util.Collection;

import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.P.PASSED;

/**
 * Represents the result of a colour search process.
 */
public record ColoursSearchResult(Collection<_Region> regions) implements _ValidationResult {

    @Override
    public boolean isPassed() {

        return (isNotNull(regions));
    }

    @Override
    public String name() {

        return getClass().getSimpleName();
    }

    @Override
    public String render() {

        return isPassed() ? gb(PASSED) : gb(FAILED);
    }
}