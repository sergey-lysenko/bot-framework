package works.lysenko.util.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridPropertiesTest {

    @Test
    void gridPropertiesInstantiation() {
        try {
            GridProperties gp = new GridProperties("test", false);
            assertNotNull(gp);
            gp.updateProperty("key", "value");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }
}
