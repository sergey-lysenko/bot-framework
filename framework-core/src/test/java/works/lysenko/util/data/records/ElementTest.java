package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Element Record Tests")
class ElementTest {

    @Test
    @DisplayName("Should create Element via static factory method e()")
    void testFactoryMethod() {
        final Element element = Element.e(5, 15, 0x00FF00);

        assertEquals(5, element.x());
        assertEquals(15, element.y());
        assertEquals(0x00FF00, element.colour());
    }
}
