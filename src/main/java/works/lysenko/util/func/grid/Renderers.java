package works.lysenko.util.func.grid;

import org.apache.commons.lang3.function.TriFunction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a set of rendering functions for validation requests and shares data.
 */
public record Renderers(BiFunction<Request, _Quota<?>, Boolean> ignore,
                        TriFunction<SharesData<?>, _Quota<?>, Binner, String> point,
                        Function<_Quota<?>, String> row) {

    /**
     * Creates a Renderers instance with the specified functions.
     *
     * @param ignore The function to determine if a validation request should be ignored or not
     * @param point  The function to calculate a string representation of shares data at a specific point
     * @param row    The function to calculate a string representation of a single share at a specific point
     * @return A1 new Renderers instance with the provided functions
     */
    public static Renderers r(final BiFunction<Request, _Quota<?>, Boolean> ignore, final TriFunction<SharesData<?>, _Quota<
            ?>, Binner, String> point, final Function<_Quota<?>, String> row) {

        return new Renderers(ignore, point, row);
    }
}
