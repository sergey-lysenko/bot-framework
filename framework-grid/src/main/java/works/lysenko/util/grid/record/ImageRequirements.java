package works.lysenko.util.grid.record;

import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.apis.grid.r._Palette;
import works.lysenko.util.apis.grid.r._Polychromy;

/**
 * Represents a set of expectations for an image analysis.
 *
 * @param colours    the colour quotas expected in the image
 * @param palette    the palette requirements
 * @param polychromy the polychromy requirements
 * @param hue        the hue fraction quotas
 * @param saturation the saturation fraction quotas
 * @param brightness the brightness fraction quotas
 */
public record ImageRequirements(_ColoursQuotas colours,
                                _Palette palette,
                                _Polychromy polychromy,
                                _FractionQuotas hue,
                                _FractionQuotas saturation,
                                _FractionQuotas brightness) implements _ImageRequirements {

    /**
     * Initialises an ImageRequirements object with only Colours requirement defined.
     *
     * @param colours The Colours object representing the expected colour ranges.
     */
    public ImageRequirements(final _ColoursQuotas colours) {

        this(colours, null, null, null, null, null);
    }
}
