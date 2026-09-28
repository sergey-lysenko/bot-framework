package works.lysenko.util.apis.grid.g;

import works.lysenko.util.data.records.Element;

import java.awt.Point;
import java.util.Collection;

/**
 * Represents a pixel grid.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _Grid {

    /**
     * Calculates various statistics and properties for a given pixel grid.
     *
     * @return an instance of _PixelGridCalculator
     */
    _GridCalculator calculator();

    /**
     * Retrieves the calculator instance for the grid.
     *
     * @return an instance of _GridCalculator
     */
    _GridCalculator getCalculator();

    /**
     * Retrieves the pixel grid.
     *
     * @return the pixel grid as a 2D array of Integers
     */
    Element[][] getGrid();

    /**
     * Retrieves the processor instance for the grid.
     *
     * @return an instance of _GridProcessor
     */
    _GridProcessor getProcessor();

    /**
     * Retrieves a collection of sample points.
     *
     * @return a collection of Point objects representing sample points
     */
    Collection<Point> getSamplePoints();

    /**
     * Process pixel grid data and provide various statistics and properties.
     *
     * @return an instance of _PixelGridProcessor
     */
    _GridProcessor processor();

    /**
     * Renders the pixel grid and returns an instance of _PixelGridToString.
     *
     * @return an instance of _PixelGridToString representing the rendered pixel grid
     */
    _GridToString render();
}
