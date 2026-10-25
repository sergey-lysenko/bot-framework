package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CollectorTest {

    @Test
    void testCollectorInstantiation() {
        Collector collector = new Collector();
        assertNotNull(collector);
    }

    @Test
    void testSelectOneOf() {
        String selected = Collector.selectOneOf(List.of("a", "b", "c"));
        assertNotNull(selected);
    }
}
