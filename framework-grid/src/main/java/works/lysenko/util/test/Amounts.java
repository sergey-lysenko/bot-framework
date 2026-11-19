package works.lysenko.util.test;

import java.util.List;
import java.util.Properties;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.Base.section;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Files.getFilesList;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.Properties.getPropertiesFromFile;
import static works.lysenko.util.grid.Fields.CDA;
import static works.lysenko.util.grid.Fields.CDI;
import static works.lysenko.util.lang.A.ALLOWED___;
import static works.lysenko.util.lang.B.BUT___;
import static works.lysenko.util.lang.word.A.AMOUNTS;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.O.ORDER;
import static works.lysenko.util.lang.word.R.REDEFINED;
import static works.lysenko.util.lang.word.V.VERIFYING;
import static works.lysenko.util.spec.Layout.Parts.GRID_EXTENSION;
import static works.lysenko.util.spec.Layout.Paths._GRIDS_;

/**
 * The Amounts record provides methods for verifying and logging redefined colour
 * properties across grid files.
 */
@SuppressWarnings("WeakerAccess")
public record Amounts() {

    /**
     * Verifies redefined amounts for colour properties across all grid files in the directory defined by {@code _GRIDS_}.
     * <p>
     * A titled section is added to the execution output to indicate the verification process.
     * Each file matching the {@code GRID_EXTENSION} extension is processed by
     * {@link Amounts#verifyRedefinedColoursAmounts(String)}.
     */
    public static void verifyRedefinedAmounts() {

        section(b(c(VERIFYING), REDEFINED, COLOUR, AMOUNTS));
        final List<String> files = getFilesList(_GRIDS_, GRID_EXTENSION);
        if (isNotNull(files)) files.forEach(Amounts::verifyRedefinedColoursAmounts);
    }

    /**
     * Verifies and processes redefined colour amounts from the specified properties file.
     * If the property associated with CDA is defined and the value linked to CDI does not
     * contain the ORDER keyword, an event is logged.
     *
     * @param path the file path of the properties file to be processed
     */
    private static void verifyRedefinedColoursAmounts(final String path) {

        final Properties file = getPropertiesFromFile(path, true);

        if (isNotNull(file) && isNotNull(file.getProperty(CDA))) {
            final String cdi = file.getProperty(CDI);
            if (isNull(cdi) || !cdi.contains(ORDER)) {
                logEvent(S2, b(ALLOWED___, q(path), BUT___, q(CDI)));
            }
        }
    }
}
