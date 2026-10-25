package works.lysenko.util.grid.validation;

import org.junit.jupiter.api.Test;
import works.lysenko.util.grid.record.Request;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class CheckTest {

    @Test
    void testWithoutGrid() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Request request = Check.withoutGrid(image, null, null, null, null, null);
        assertNotNull(request);
        assertEquals(image, request.image());
    }

    @Test
    void testCheckClassInstantiation() {
        Check check = new Check();
        assertNotNull(check);
    }
}
