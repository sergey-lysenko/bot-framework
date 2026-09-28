package works.lysenko.util.data.records.grid;

import works.lysenko.util.data.enums.ScaledPropertiesSilent;

/**
 * This class represents options for scaling properties. These options include the silent mode
 * for log messages, whether scaling is prohibited, and if the creation of new properties is allowed.
 */
public record ScaledPropertiesOptions(ScaledPropertiesSilent silent,
                                      boolean noScales,
                                      boolean allowNew) {


}
