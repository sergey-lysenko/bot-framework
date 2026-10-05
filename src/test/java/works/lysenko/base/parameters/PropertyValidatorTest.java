package works.lysenko.base.parameters;

import org.junit.jupiter.api.Test;
import works.lysenko.util.spec.PropEnum;

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
    void sonificationAcceptsOnlyKnownModes() {
        final String name = ".progression.tree.sonification";
        for (final String ok : new String[]{"none", "copilot", "claude", "Claude", "gemini", "Gemini", ""}) {
            assertNull(PropertyValidator.validate(name, ok), ok);
        }
        for (final String bad : new String[]{"true", "false"}) {
            assertNotNull(PropertyValidator.validate(name, bad), bad);
        }
    }

    @Test
    void exposesFiniteKnownValuesForGuiSelection() {
        assertEquals(
                java.util.List.of("none", "copilot", "claude", "gemini"),
                PropertyValidator.validValues(".progression.tree.sonification"));
        assertEquals(java.util.List.of("true", "false"), PropertyValidator.validValues(".progression.tree"));
        assertEquals(java.util.List.of(), PropertyValidator.validValues(".progression.max.frames"));
        assertEquals(java.util.List.of(), PropertyValidator.validValues("custom.property"));
    }

    @Test
    void validatesPrimitiveTypes() {
        assertNull(PropertyValidator.validate(".progression.max.frames", "100"));
        assertNotNull(PropertyValidator.validate(".progression.max.frames", "abc"));
        assertNotNull(PropertyValidator.validate(".progression.max.frames", " 10"));
        assertNull(PropertyValidator.validate(".progression.tree", "TRUE"));
        assertNotNull(PropertyValidator.validate(".progression.tree", "yes"));
    }

    @Test
    void validatesColoursAndFractions() {
        assertNull(PropertyValidator.validate(".grid.action.marker.colour", "255,128,0,128"));
        assertNotNull(PropertyValidator.validate(".grid.action.marker.colour", "255,128,0"));
        assertNotNull(PropertyValidator.validate(".grid.action.marker.colour", "256,0,0,0"));
        assertNull(PropertyValidator.validate(".tree.completion.weight", "1/2"));
        assertNotNull(PropertyValidator.validate(".tree.completion.weight", "1 2"));
    }

    @Test
    void customPropertiesAreFreeForm() {
        assertNull(PropertyValidator.validate("my.custom.key", "anything"));
    }
}
