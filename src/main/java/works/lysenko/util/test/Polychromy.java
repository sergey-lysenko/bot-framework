package works.lysenko.util.test;

import works.lysenko.util.grid.record.gsrc.Resolution;

import java.util.Properties;

import static java.util.Objects.isNull;
import static works.lysenko.Base.section;
import static works.lysenko.util.apis.grid.d._Resolution.readResolution;
import static works.lysenko.util.chrs.____.DATA;
import static works.lysenko.util.data.enums.PolychromyMethod.EUCLIDEAN_DISTANCE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.inn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.imgs.Analyser.getMaximumEuclideanDispersion;
import static works.lysenko.util.func.type.Files.getFilesList;
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
     * Calculates the maximum polychromies.
     * <p>
     * This method walks through the files in the '_MATRICES_' directory (recursively), filters the files to include only
     * regular files with the
     * extension '.grid', and calculates the maximum polychromy for each file.
     * To perform this calculation, it first adds a section to the execution output using the 'section' method. Then, it
     * iterates over each file
     * path and calls the 'calculateMaxPolychromy' method of the 'Polychromy' class.
     * If an IOException occurs during the process, it prints the stack trace.
     */
    public static void calculateMaxPolychromies() {

        section(b(c(UPDATING), POLYCHROMY, DATA));
        getFilesList(_GRIDS_, GRID_EXTENSION).forEach(Polychromy::calculateMaxPolychromy);
    }

    /**
     * Calculates the maximum polychromy for a given file specified by its path.
     * This method reads the properties from the given file, checks the polychromy and method,
     * and if applicable, computes the maximum Euclidean dispersion.
     *
     * @param s the file path as a String from which to read properties and calculate the maximum polychromy
     */
    private static void calculateMaxPolychromy(final String s) {

        final Properties file = getPropertiesFromFile(s, true);
        final String polychromy = file.getProperty(POLYCHROMY);
        final String method = file.getProperty(s(PDM));

        if (!isNull(polychromy) && !isNull(method) && (EUCLIDEAN_DISTANCE.ordinal() == inn(method))) {
            final String rawText = getPropertiesFromFile(s, true).getProperty(RESOLUTION);
            final Resolution resolution = readResolution(rawText, true);
            if (null != resolution) getMaximumEuclideanDispersion(resolution.size(), true);
        }
    }
}
