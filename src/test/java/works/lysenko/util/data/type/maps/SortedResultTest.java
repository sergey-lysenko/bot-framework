package works.lysenko.util.data.type.maps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SortedResultTest {

    @Test
    void testInstantiation() {
        final SortedResult map = new SortedResult();
        assertNotNull(map.comparator());
    }
}
