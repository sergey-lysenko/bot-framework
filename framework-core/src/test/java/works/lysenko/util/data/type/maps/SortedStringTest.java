package works.lysenko.util.data.type.maps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortedStringTest {

    @Test
    void testCaseInsensitiveKey() {
        final SortedString map = new SortedString();
        map.put("Key", "Value");
        assertEquals("Value", map.get("key"));
    }
}
