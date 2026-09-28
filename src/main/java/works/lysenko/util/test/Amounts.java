package works.lysenko.util.test;

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
 * The Amounts class contains methods and constants related to the management,
 * verification, and logging of redefined colour properties in a grid file system.
 * It provides functionality to process a predefined directory of grid files and
 * validate specific colour-related properties.
 */
@SuppressWarnings({"BooleanVariableAlwaysNegated", "WeakerAccess"})
public record Amounts() {

    /**
     * Verifies redefined amounts for colour properties across all grid files in a specified directory.
     * <p>
     * This method processes grid files in the directory defined by the `_GRIDS_` constant,
     * filtering for files that use the specified grid file extension (`GRID_EXTENSION`).
     * Each file is checked to validate redefinitions of colour amounts.
     * A titled section is added to the execution output to indicate the verification process.
     * If an error occurs while accessing the files, the exception stack trace is printed.
     * <p>
     * The verification logic for colour properties is delegated to
     * {@link Amounts#verifyRedefinedColoursAmounts(String)} for individual grid files.
     */
    public static void verifyRedefinedAmounts() {

        section(b(c(VERIFYING), REDEFINED, COLOUR, AMOUNTS));
        getFilesList(_GRIDS_, GRID_EXTENSION).forEach(Amounts::verifyRedefinedColoursAmounts);
    }

    /**
     * Verifies and processes redefined colour amounts from the specified properties file.
     * If the property associated with CDA is defined and the value linked to CDI does not
     * contain the IGNORE keyword, an event is logged.
     *
     * @param s the file path of the properties file to be processed
     */
    private static void verifyRedefinedColoursAmounts(final String s) {

        final Properties file = getPropertiesFromFile(s, true);

        if (!isNull(file.getProperty(CDA))) {
            final String cdi = file.getProperty(CDI);
            final boolean alreadyIgnored = isNotNull(cdi) && cdi.contains(ORDER);
            if (!alreadyIgnored) {
                logEvent(S2, b(ALLOWED___, q(s), BUT___, q(CDI)));
            }
        }
    }
}
