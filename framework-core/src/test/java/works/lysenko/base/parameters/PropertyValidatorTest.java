package works.lysenko.base.parameters;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.prop.PropEnum;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyValidatorTest {

    @Test
    void everyDefaultValueIsValid() {
        for (final PropEnum property : PropEnum.values()) {
            assertNull(PropertyValidator.validate(property.getPropertyName(), property.defaultValue()),
                    property.getPropertyName() + " default '" + property.defaultValue() + "'");
        }
    }

    @Test
    void sonificationAcceptsBooleanValues() {
        final String name = ".test.report.progression.tree.sonification";
        for (final String ok : new String[]{"true", "false", "True", "FALSE", ""}) {
            assertNull(PropertyValidator.validate(name, ok), ok);
        }
        for (final String bad : new String[]{"none", "dnb", "neon", "1", "0"}) {
            assertNotNull(PropertyValidator.validate(name, bad), bad);
        }
    }

    @Test
    void sonificationsListAcceptsCommaSeparatedText() {
        final String name = ".test.report.progression.tree.sonifications";
        assertNull(PropertyValidator.validate(name, "dnb, neon, jmj, carbon"));
        assertNull(PropertyValidator.validate(name, "dnb"));
        assertNull(PropertyValidator.validate(name, ""));
    }

    @Test
    void exposesFiniteKnownValuesForGuiSelection() {
        assertEquals(java.util.List.of("true", "false"), PropertyValidator.validValues(".test.report.progression.tree.sonification"));
        assertEquals(java.util.List.of("true", "false"), PropertyValidator.validValues(".test.report.progression.tree"));
        assertEquals(
                java.util.Arrays.stream(Severity.values()).map(Enum::name).toList(),
                PropertyValidator.validValues(".test.assertions.severity"));
        assertEquals(java.util.List.of(), PropertyValidator.validValues(".test.report.progression.tree.sonifications"));
        assertEquals(java.util.List.of(), PropertyValidator.validValues(".test.report.progression.max.frames"));
        assertEquals(java.util.List.of(), PropertyValidator.validValues("custom.property"));
    }

    @Test
    void validatesPrimitiveTypes() {
        assertNull(PropertyValidator.validate(".test.report.progression.max.frames", "100"));
        assertNotNull(PropertyValidator.validate(".test.report.progression.max.frames", "abc"));
        assertNotNull(PropertyValidator.validate(".test.report.progression.max.frames", " 10"));
        assertNull(PropertyValidator.validate(".test.report.progression.max.frame.pixels", "168M"));
        assertNull(PropertyValidator.validate(".test.report.progression.max.total.pixels", "2000M"));
        assertNull(PropertyValidator.validate(".test.report.progression.max.total.pixels", "2G"));
        assertNull(PropertyValidator.validate(".test.report.progression.tree", "TRUE"));
        assertNotNull(PropertyValidator.validate(".test.report.progression.tree", "yes"));
        assertNull(PropertyValidator.validate(".test.assertions.severity", "S0"));
        assertNotNull(PropertyValidator.validate(".test.assertions.severity", "invalid"));
    }

    @Test
    void validatesColoursAndFractions() {
        assertNull(PropertyValidator.validate(".grid.action.marker.colour", "255,128,0,128"));
        assertNotNull(PropertyValidator.validate(".grid.action.marker.colour", "255,128,0"));
        assertNotNull(PropertyValidator.validate(".grid.action.marker.colour", "256,0,0,0"));
        assertNull(PropertyValidator.validate(".tree.completion.weight", "1/2"));
        assertNotNull(PropertyValidator.validate(".tree.completion.weight", "1 2"));
        assertNull(PropertyValidator.validate(".tree.progression.movement.speed", "1"));
        assertNull(PropertyValidator.validate(".tree.progression.movement.speed", "1/2"));
        assertNull(PropertyValidator.validate(".tree.progression.movement.speed", "2.5"));
        assertNull(PropertyValidator.validate(".tree.progression.movement.speed", "5"));
        assertNotNull(PropertyValidator.validate(".tree.progression.movement.speed", "0"));
        assertNotNull(PropertyValidator.validate(".tree.progression.movement.speed", "-1/2"));
        assertNotNull(PropertyValidator.validate(".tree.progression.movement.speed", "fast"));
    }

    @Test
    void customPropertiesAreFreeForm() {
        assertNull(PropertyValidator.validate("my.custom.key", "anything"));
    }

    @Test
    void classFileScannerDiscoversDirectAndTransitiveSubtypes() {
        final var propEnums = ClasspathSubtypeScanner.findSubTypesOf(
                works.lysenko.util.apis._PropEnum.class,
                java.util.List.of("works.lysenko"),
                true);
        org.junit.jupiter.api.Assertions.assertTrue(
                propEnums.contains(PropEnum.class),
                "Class-File API scanner must discover PropEnum implementing _PropEnum");

        final var scenarios = ClasspathSubtypeScanner.findSubTypesOf(
                java.util.List.of(works.lysenko.tree.base.Node.class, works.lysenko.tree.base.Leaf.class),
                java.util.List.of("works.lysenko.tree"),
                false);
        org.junit.jupiter.api.Assertions.assertTrue(
                scenarios.get(works.lysenko.tree.base.Leaf.class).contains(works.lysenko.tree.base.Mono.class),
                "Class-File API scanner must discover Mono extending Leaf");
    }
}
