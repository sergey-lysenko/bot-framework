package works.lysenko.util.test;

import works.lysenko.util.grid.record.gsrc.Resolution;

import java.util.List;
import java.util.Properties;

import static works.lysenko.Base.section;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.chrs.____.DATA;
import static works.lysenko.util.data.enums.PolychromyMethod.EUCLIDEAN_DISTANCE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.func.imgs.Analyser.getMaximumEuclideanDispersion;
import static works.lysenko.util.func.type.Files.getFilesList;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Properties.getPropertiesFromFile;
import static works.lysenko.util.grid.Fields.PDM;
import static works.lysenko.util.lang.word.P.POLYCHROMY;
import static works.lysenko.util.lang.word.R.RESOLUTION;
import static works.lysenko.util.lang.word.U.UPDATING;
import static works.lysenko.util.spec.Layout.Parts.GRID_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;

/**
 * The {@code Polychromy} class represents a record of polychromies.
 * It contains a static method for calculating the maximum polychromies.
 */
@SuppressWarnings("WeakerAccess")
public record Polychromy() {

    /**
     * Calculates the maximum polychromies across all grid files in the directory defined by {@code _GRIDS_}.
     * <p>
     * A titled section is added to the execution output to indicate the calculation process.
     * Each file matching the {@code GRID_EXTENSION} extension is processed by
     * {@link Polychromy#calculateMaxPolychromy(String)}.
     */
    public static void calculateMaxPolychromies() {

        section(b(c(UPDATING), POLYCHROMY, DATA));
        final List<String> files = getFilesList(_GRIDS_, GRID_EXTENSION);
        if (isNotNull(files)) files.forEach(Polychromy::calculateMaxPolychromy);
    }

    /**
     * Calculates the maximum polychromy for a given file specified by its path.
     * This method reads the properties from the given file, checks the polychromy and method,
     * and if applicable, computes the maximum Euclidean dispersion.
     *
     * @param path the file path as a String from which to read properties and calculate the maximum polychromy
     */
    private static void calculateMaxPolychromy(final String path) {

        final Properties file = getPropertiesFromFile(path, true);
        final String polychromy = file.getProperty(POLYCHROMY);
        final String method = file.getProperty(PDM);

        if (isNotNull(polychromy) && isNotNull(method) && (EUCLIDEAN_DISTANCE.ordinal() == inn(method))) {
            final String rawText = file.getProperty(RESOLUTION);
            final Resolution resolution = readResolution(rawText, true);
            if (isNotNull(resolution)) getMaximumEuclideanDispersion(resolution.size(), true);
        }
    }
}
