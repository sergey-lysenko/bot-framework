package works.lysenko.util.func.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FilesTest {

    @Test
    void testFilesInstantiation() {
        Files files = new Files();
        assertNotNull(files);
    }
}
