package works.lysenko.util.test;

import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.r._ImageRequirements;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.prop.grid.Verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.section;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isAnyNull;
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
 * Represents a Colour.
 */
@SuppressWarnings("StaticCollection")
public record Colour() {

    private static final Collection<Integer> unknownColours = new HashSet<>(0);

    /**
     * Verifies the undefined colours in a grid.
     *
     * @param path the path of the file containing the properties
     */
    private static void verifyGridUndefinedColours(final String path) {

        final _GridProperties gridProperties = _GridProperties.getGridPropertiesByPath(path);
        if (isNotNull(gridProperties.getKV(COLOURS, true).v())) {
            final _ImageRequirements imageRequirements = _ImageRequirements.read(path, gridProperties, null, true);
            if (isNotNull(imageRequirements) && !isAnyNull(imageRequirements, imageRequirements.colours())) {
                final List<_Quota<?>> ranges = imageRequirements.colours().get();
                for (final _Quota<?> range : ranges)
                    if (!knownColours.containsValue(s(range.value())))
                        unknownColours.add((Integer) range.value());
            }
        }
    }

    /**
     * Verifies the undefined colours in the matrices.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    static void verifyUndefinedColours() {

        if (Verify.colours) {
            section(b(c(VALIDATING), COLOUR, DEFINITIONS));
            try (final Stream<Path> walk = Files.walk(Paths.get(_GRIDS_))) {
                final List<String> result = walk.filter(Files::isRegularFile)
                        .map(Path::toString)
                        .filter(fi -> fi.endsWith(GRID_EXTENSION))
                        .toList();
                result.forEach(Colour::verifyGridUndefinedColours);
                for (final Integer unknownColour : unknownColours)
                    logEvent(S2, b(RGB24.shortRgb(unknownColour), COLOUR, IS, UNKNOWN));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}
