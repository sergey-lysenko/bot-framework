package works.lysenko.util.func.logs.poster;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.data.enums.Severity;

/**
 * Represents a failure event with information about severity, data type, description, actual value, expected value, and
 * allowed deviation.
 */
public record Failure(
        Severity severity,
        String dataType,
        String description,
        _Value<?> actual,
        _Value<?> expected,
        Fraction allowed_dV) {}
