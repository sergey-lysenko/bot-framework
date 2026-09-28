package works.lysenko.util.grid;

import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.data.type.ScaledProperties;
import works.lysenko.util.data.records.grid.ScaledPropertiesOptions;

import static works.lysenko.util.data.enums.ScaledPropertiesSilent.NO;
import static works.lysenko.util.data.enums.ScaledPropertiesSilent.YES;

/**
 * Represents properties associated with a grid, providing methods to retrieve, update, and handle grid properties.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public final class GridProperties extends ScaledProperties implements _GridProperties {

    /**
     * Constructs a GridProperties instance.
     *
     * @param name   the name or path of the grid
     * @param isPath specifies whether the provided name represents a direct path (true) or a grid name (false)
     */
    public GridProperties(final String name, final boolean isPath) {

        super(isPath ? name : _GridProperties.getGridPath(name), new ScaledPropertiesOptions(isPath ? YES : NO, false, false));
    }

    @Override
    public void updateProperty(final String key, final String value) {

        super.updateProperty(key, value, false);
    }
}
