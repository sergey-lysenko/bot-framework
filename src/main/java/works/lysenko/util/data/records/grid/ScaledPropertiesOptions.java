package works.lysenko.util.data.records.grid;

import works.lysenko.util.data.enums.ScaledPropertiesSilent;

/**
 * Represents options for scaling properties.
 *
 * @param silent   the silent mode for log messages
 * @param noScales whether scaling is prohibited
 * @param allowNew whether creation of new properties is allowed
 */
public record ScaledPropertiesOptions(ScaledPropertiesSilent silent,
                                      boolean noScales,
                                      boolean allowNew) {
}
