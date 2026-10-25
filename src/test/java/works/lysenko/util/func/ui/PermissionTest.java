package works.lysenko.util.func.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PermissionTest {

    @BeforeEach
    void setUp() {
        Base.exec = null;
    }

    @AfterEach
    void tearDown() {
        Base.exec = null;
    }

    @Test
    void testPermissionInstantiation() {
        Permission permission = new Permission();
        assertNotNull(permission);
    }

    @Test
    void testProcessOptionalPermissionsWithNullExec() {
        assertDoesNotThrow(() -> {
            if (null != Base.exec) {
                Permission.processOptionalPermissions();
            }
        });
    }
}
