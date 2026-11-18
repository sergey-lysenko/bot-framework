package works.lysenko.base.parameters;

import org.junit.jupiter.api.Test;
import works.lysenko.util.prop.PropEnum;

import static org.junit.jupiter.api.Assertions.*;

class PropertyHelpTest {

    @Test
    void testGetDescription() {
        final String desc = PropertyHelp.getDescription(PropEnum._TEST_HEADLESS.getPropertyName());
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void testFindHelp() {
        final String help = PropertyHelp.findHelp("HEADLESS");
        assertNotNull(help);
        assertFalse(help.isBlank());

        final String helpByProp = PropertyHelp.findHelp(PropEnum._TEST_HEADLESS.getPropertyName());
        assertNotNull(helpByProp);
        assertFalse(helpByProp.isBlank());

        assertNull(PropertyHelp.findHelp("non.existent.property.key"));
        assertNull(PropertyHelp.findHelp(""));
        assertNull(PropertyHelp.findHelp(null));
    }

    @Test
    void testGetPropertyNames() {
        assertFalse(PropertyHelp.getPropertyNames().isEmpty());
    }
}
