package works.lysenko.util.apis.grid.g;

import works.lysenko.util.data.records.diff.Pair;
import works.lysenko.util.grid.record.rgbc.HSB;

import java.util.Map;

/**
 * Represents a grid calculator that performs various calculations on a grid.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _GridCalculator {

    /**
     * This method counts the HSB color representation grid of the RGB values in the provided grid.
     *
     * @return a grid of HSB color representations for the RGB values in the grid
     */
    HSB[][] countHSBgrid();

    /**
     * Counts the number of pixels by brightness level based on the provided fences value.
     *
     * @param fences an integer value representing the level of fences for the brightness levels
     * @return a map containing the brightness levels as keys and the count of pixels for each brightness level as values
     */
    Map<Integer, Pair<Integer, String>> countPixelsByBrightness(int fences);

    /**
     * Retrieves the count of pixels by color in the pixel grid.
     *
     * @return a map containing the count of pixels for each color
     */
    Map<Integer, Pair<Integer, String>> countPixelsByColor();

    /**
     * Counts the number of pixels by hue level based on the provided fences value.
     *
     * @param fences an integer value representing the level of fences for the hue levels
     * @return a map containing the hue levels as keys and the count of pixels for each hue level as values
     */
    Map<Integer, Pair<Integer, String>> countPixelsByHue(int fences);

    /**
     * Counts the number of pixels by saturation level based on the provided fences value.
     *
     * @param fences an integer value representing the level of fences for the saturation levels
     * @return a map containing the saturation levels as keys and the count of pixels for each saturation level as values
     */
    Map<Integer, Pair<Integer, String>> countPixelsBySaturation(int fences);

}
