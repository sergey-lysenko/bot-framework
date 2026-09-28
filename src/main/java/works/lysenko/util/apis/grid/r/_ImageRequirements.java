package works.lysenko.util.apis.grid.r;

import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.grid.record.ImageRequirements;

import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.DUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.fail;
import static works.lysenko.util.grid.reader.CPP.getColours;
import static works.lysenko.util.grid.reader.CPP.getPalette;
import static works.lysenko.util.grid.reader.CPP.getPolychromy;
import static works.lysenko.util.grid.reader.HSB.getBrightness;
import static works.lysenko.util.grid.reader.HSB.getHue;
import static works.lysenko.util.grid.reader.HSB.getSaturation;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.R.READING;
import static works.lysenko.util.spec.Symbols._COLON_;

/**
 * The _ImageRequirements interface defines a set of methods to retrieve various image-related requirements.
 * This interface serves as a contract for getting specific quotas and configurations associated with image attributes
 * such as brightness, colours, hue, palette, polychromy, and saturation.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ImageRequirements {

    /**
     * Reads the image requirements from properties and returns an ImageRequirements object.
     *
     * @param origin The origin of the requirements.
     * @param grid   The property object containing the requirements.
     * @return An ImageRequirements object with the read requirements.
     */
    static _ImageRequirements read(final String origin, final _GridProperties grid) {

        return read(origin, grid, null, false);
    }

    /**
     * Reads the image requirements from properties and returns an ImageRequirements object.
     *
     * @param origin     The origin of the requirements.
     * @param grid       The property object containing the requirements.
     * @param predefined The predefined ImageRequirements object.
     * @param silent     A1 flag indicating whether to run silently.
     * @return An ImageRequirements object with the read requirements.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    static _ImageRequirements read(final String origin, final _GridProperties grid, final _ImageRequirements predefined,
                                   final boolean silent) {

        try {
            final _Palette palette = getPalette(grid, predefined);
            final _ColoursQuotas colours = getColours(grid, predefined, origin, silent);
            final _Polychromy polychromy = getPolychromy(grid, predefined);
            final _FractionQuotas hue = getHue(grid, predefined);
            final _FractionQuotas saturation = getSaturation(grid, predefined);
            final _FractionQuotas brightness = getBrightness(grid, predefined);
            return new ImageRequirements(colours, palette, polychromy, hue, saturation, brightness);
        } catch (final IllegalArgumentException e) {
            fail(b(c(READING), OF, origin, FAILED, DUE, s(TO, _COLON_), e.getMessage()));
            return null;
        }
    }

    /**
     * Retrieves the _FractionQuotas instance associated with the brightness requirements.
     *
     * @return the _FractionQuotas instance representing brightness quotas and related properties
     */
    _FractionQuotas brightness();

    /**
     * Retrieves the _ColoursQuotas instance associated with the image requirements.
     *
     * @return the _ColoursQuotas instance representing color quotas and related properties
     */
    _ColoursQuotas colours();

    /**
     * Retrieves the _FractionQuotas instance associated with the hue requirements.
     *
     * @return the _FractionQuotas instance representing hue quotas and related properties
     */
    _FractionQuotas hue();

    /**
     * Retrieves the _Palette instance associated with the image requirements.
     *
     * @return the _Palette instance representing the palette configuration.
     */
    _Palette palette();

    /**
     * Retrieves the _Polychromy instance associated with the image requirements.
     *
     * @return the _Polychromy instance representing the polychromy configuration
     */
    _Polychromy polychromy();

    /**
     * Retrieves the _FractionQuotas instance associated with the saturation requirements.
     *
     * @return the _FractionQuotas instance representing saturation quotas and related properties
     */
    _FractionQuotas saturation();

}
