package works.lysenko.util.func.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClassLoaderTest {

    @Test
    void testClassLoaderInstantiation() {
        ClassLoader classLoader = new ClassLoader();
        assertNotNull(classLoader);
    }

    @Test
    void testFindAllClassesInPackage() {
        assertDoesNotThrow(() -> ClassLoader.findAll("works.lysenko.util.func.core", true));
    }
}
