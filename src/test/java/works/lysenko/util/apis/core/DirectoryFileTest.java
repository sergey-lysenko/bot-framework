package works.lysenko.util.apis.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("DirectoryFile Record Tests")
class DirectoryFileTest {

    @Test
    @DisplayName("Verify df helper method splits path into directory and filename")
    void testDf() {
        DirectoryFile df = DirectoryFile.df("/path/to/file.txt");
        assertNotNull(df);
        assertEquals("/path/to", df.d());
        assertEquals("file.txt", df.f());
        assertEquals("/path/to", df.directory());
        assertEquals("file.txt", df.filename());
    }
}
