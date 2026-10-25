package works.lysenko.util.func.logs.poster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PosterValueTest {

    @Test
    void testValueRecord() {
        Value<Integer> value = new Value<>(10);
        assertNotNull(value);
        assertEquals(10, value.value());
    }
}
