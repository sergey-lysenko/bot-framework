package works.lysenko.util.grid.record.rgbc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateTest {

    @Test
    void testDuplicateInstantiation() {
        List<String> names = List.of("red1", "red2");
        Duplicate dup = new Duplicate(0xFF0000, names);
        assertEquals(0xFF0000, dup.rgb());
        assertEquals(names, dup.names());
    }
}
