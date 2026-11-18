package works.lysenko.util.grid.record.gsrc;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.grid.t._Region;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColoursSearchResultTest {

    @Test
    void testColoursSearchResultMethods() {
        List<_Region> regions = Collections.emptyList();
        ColoursSearchResult result = new ColoursSearchResult(regions);
        assertEquals(regions, result.regions());
        assertTrue(result.isPassed());
        assertEquals("ColoursSearchResult", result.name());
        assertNotNull(result.render());

        ColoursSearchResult nullResult = new ColoursSearchResult(null);
        assertFalse(nullResult.isPassed());
        assertNotNull(nullResult.render());
    }
}
