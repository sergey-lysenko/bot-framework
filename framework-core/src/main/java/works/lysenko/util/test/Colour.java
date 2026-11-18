package works.lysenko.util.test;

import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.prop.grid.Verify;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.section;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Files.getFilesList;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.D.DEFINITIONS;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VALIDATING;
import static works.lysenko.util.spec.Layout.Files.knownColours;
import static works.lysenko.util.spec.Layout.Parts.GRID_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;

/**
 * Utility record representing colours and providing methods to verify undefined
 * colour definitions across grid configuration files.
 */
@SuppressWarnings("WeakerAccess")
public record Colour() {

    /**
     * Verifies undefined colours within a specific grid file given its path,
     * adding any unknown colour values to the provided collection.
     *
     * @param path           the file path of the grid properties
     * @param unknownColours collection accumulating unknown colour integer values
     */
    private static void verifyGridUndefinedColours(final String path, final Collection<Integer> unknownColours) {

        final _GridProperties gridProperties = _GridProperties.getGridPropertiesByPath(path);
        if (isNotNull(gridProperties.getKV(COLOURS, true).v())) {
            final _ImageRequirements imageRequirements = _ImageRequirements.read(path, gridProperties, null, true);
            if (isNotNull(imageRequirements) && isNotNull(imageRequirements.colours())) {
                final List<_Quota<?>> ranges = imageRequirements.colours().get();
                if (isNotNull(ranges)) {
                    for (final _Quota<?> range : ranges) {
                        if (isNotNull(range.value()) && !knownColours.containsValue(s(range.value()))) {
                            unknownColours.add((Integer) range.value());
                        }
                    }
                }
            }
        }
    }

    /**
     * Verifies undefined colour definitions across all grid configuration files located
     * in the directory defined by {@code _GRIDS_}.
     * <p>
     * If colour verification is enabled ({@code Verify.colours}), a section is logged,
     * and each matching grid file is checked for unknown colours that are not present
     * in {@code knownColours}. Any discovered unknown colours are logged as events with severity {@code S2}.
     */
    public static void verifyUndefinedColours() {

        if (Verify.colours) {
            section(b(c(VALIDATING), COLOUR, DEFINITIONS));
            final List<String> files = getFilesList(_GRIDS_, GRID_EXTENSION);
            if (isNotNull(files)) {
                final Set<Integer> unknownColours = new HashSet<>(0);
                files.forEach(path -> verifyGridUndefinedColours(path, unknownColours));
                for (final Integer unknownColour : unknownColours) {
                    logEvent(S2, b(RGB24.shortRgb(unknownColour), COLOUR, IS, UNKNOWN));
                }
            }
        }
    }
}
