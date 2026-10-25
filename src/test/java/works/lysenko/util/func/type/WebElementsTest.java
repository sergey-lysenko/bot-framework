package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebElementsTest {

    @Test
    void testWebElementsInstantiation() {
        WebElements webElements = new WebElements();
        assertNotNull(webElements);
    }
}
