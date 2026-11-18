package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.properties.renderer.Routines;
import works.lysenko.util.data.enums.Ansi;
import works.lysenko.util.data.records.PropertiesMeta;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies properties renderer summary and validation messages use "property"/"properties".
 */
class PropertiesRendererTest {

    private TestProperties previousProperties;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {

        previousProperties = Base.properties;
        Base.properties = new TestProperties();
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {

        System.setOut(originalOut);
        Base.properties = previousProperties;
    }

    private String cleanOutput() {

        return Ansi.removeANSICodes(capturedOut.toString(StandardCharsets.UTF_8));
    }

    @Test
    void validateOutputsSingularPropertyForOneClassReference() {

        final int totalDefaults = Base.properties.getDefaultsSize();
        final Map<String, String> remainingDefaults = new LinkedHashMap<>();
        for (int i = 0; i < totalDefaults - 1; i++) {
            remainingDefaults.put(".prop." + i, "val");
        }
        final PropertiesMeta meta = new PropertiesMeta(remainingDefaults, List.of("java.lang.String"));

        Routines.validate(meta, List.of(".changed.one", "java.lang.String"));

        final String clean = cleanOutput();
        assertTrue(clean.contains("Values of 2 properties were redefined, " + (totalDefaults - 1) + " have default values"),
                "Should report redefined properties count, actual:\n" + clean);
        assertTrue(clean.contains("one property with a class reference"),
                "Should report singular property with a class reference, actual:\n" + clean);
        assertFalse(clean.contains("parameter"), "Properties summary should not refer to parameters, actual:\n" + clean);
    }

    @Test
    void validateOutputsPluralPropertiesForMultipleClassReferencesAndMismatchException() {

        final int totalDefaults = Base.properties.getDefaultsSize();
        final Map<String, String> remainingDefaults = new LinkedHashMap<>();
        for (int i = 0; i < totalDefaults; i++) {
            remainingDefaults.put(".prop." + i, "val");
        }
        final PropertiesMeta meta = new PropertiesMeta(remainingDefaults, List.of("java.lang.String", "java.lang.Integer"));

        Routines.validate(meta, List.of("java.lang.String", "java.lang.Integer"));

        final String clean = cleanOutput();
        assertTrue(clean.contains("There are 2 properties with a class reference"),
                "Should report plural properties with a class reference, actual:\n" + clean);

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Routines.validate(meta, List.of("only.one")));
        assertTrue(ex.getMessage().contains("properties is"),
                "Exception message should mention properties, actual: " + ex.getMessage());
    }
}
