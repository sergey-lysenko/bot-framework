package works.lysenko.util.data.range;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.func.grid.Renderers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GraphTest {

    @Test
    void testGraphActualFactoryAndGetters() {

        final Map<Integer, _ValuedRangeResult> results = new HashMap<>();
        final Renderers renderers = Renderers.r((req, q) -> false, (shares, q, b) -> "", q -> "");

        final Graph<Integer> graph = Graph.graphActual(results, null, renderers, 10);

        assertEquals("Actual", graph.title());
        assertNull(graph.amount());
        assertNull(graph.quotas());
        assertEquals(results, graph.results());
        assertNull(graph.go());
        assertEquals(renderers, graph.renderers());
        assertEquals(10, graph.fences());
    }
}
