package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GTest {

    @Test
    void testInstantiation() {
        G instance = new G();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(G.GETTING);
        assertFalse(G.GETTING.isEmpty());
        assertNotNull(G.GIVEN);
        assertFalse(G.GIVEN.isEmpty());
        assertNotNull(G.GRAPH);
        assertFalse(G.GRAPH.isEmpty());
        assertNotNull(G.GREEN);
        assertFalse(G.GREEN.isEmpty());
        assertNotNull(G.GRIDS);
        assertFalse(G.GRIDS.isEmpty());
        assertNotNull(G.GROUND);
        assertFalse(G.GROUND.isEmpty());
        assertNotNull(G.GROUP);
        assertFalse(G.GROUP.isEmpty());
    }
}
