package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Element Record Tests")
class ElementTest {

    @Test
    @DisplayName("Should correctly store and retrieve coordinates and colour")
    void testRecordFields() {
        final Element element = new Element(10, 20, 0xFF0000);

        assertEquals(10, element.x());
        assertEquals(20, element.y());
        assertEquals(0xFF0000, element.colour());
    }

    @Test
    @DisplayName("Should create Element via static factory method e()")
    void testFactoryMethod() {
        final Element element = Element.e(5, 15, 0x00FF00);

        assertEquals(5, element.x());
        assertEquals(15, element.y());
        assertEquals(0x00FF00, element.colour());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Element e1 = Element.e(1, 2, 3);
        final Element e2 = new Element(1, 2, 3);
        final Element e3 = Element.e(1, 2, 4);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
    }

    @Test
    @DisplayName("Should generate non-null toString representation")
    void testToString() {
        final Element element = Element.e(10, 20, 30);
        assertNotNull(element.toString());
    }
}
