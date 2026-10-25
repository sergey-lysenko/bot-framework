package works.lysenko.util.data.records.grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("grid.BuildRequest Record Tests")
class BuildRequestTest {

    @Test
    @DisplayName("Should store and retrieve fields correctly")
    void testRecordFields() {
        final BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        final BuildRequest request = new BuildRequest(image, null, null, true);

        assertEquals(image, request.image());
        assertNull(request.geometry());
        assertNull(request.resolution());
        assertTrue(request.ignoreOOB());
    }

    @Test
    @DisplayName("Should handle null components")
    void testNullFields() {
        final BuildRequest request = new BuildRequest(null, null, null, false);

        assertNull(request.image());
        assertNull(request.geometry());
        assertNull(request.resolution());
        assertFalse(request.ignoreOOB());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final BuildRequest r1 = new BuildRequest(null, null, null, true);
        final BuildRequest r2 = new BuildRequest(null, null, null, true);
        final BuildRequest r3 = new BuildRequest(null, null, null, false);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final BuildRequest request = new BuildRequest(null, null, null, true);
        assertNotNull(request.toString());
    }
}
