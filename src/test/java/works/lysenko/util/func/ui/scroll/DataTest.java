package works.lysenko.util.func.ui.scroll;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataTest {

    @Test
    void testInstantiation() {
        Data data = new Data(null, 0, 0, null, null);
        assertNotNull(data);
        assertNull(data.scroller());
        assertEquals(0, data.height());
        assertEquals(0, data.width());
        assertNull(data.finger());
        assertNull(data.q());
    }
}
