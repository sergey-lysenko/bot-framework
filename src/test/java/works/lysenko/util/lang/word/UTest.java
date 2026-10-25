package works.lysenko.util.lang.word;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UTest {

    @Test
    void testInstantiation() {
        U instance = new U();
        assertNotNull(instance);
    }

    @Test
    void testConstants() {
        assertNotNull(U.ULTIMATE);
        assertFalse(U.ULTIMATE.isEmpty());
        assertNotNull(U.UNABLE);
        assertFalse(U.UNABLE.isEmpty());
        assertNotNull(U.UNCAUGHT);
        assertFalse(U.UNCAUGHT.isEmpty());
        assertNotNull(U.UNCHANGED);
        assertFalse(U.UNCHANGED.isEmpty());
        assertNotNull(U.UNDEFINED);
        assertFalse(U.UNDEFINED.isEmpty());
        assertNotNull(U.UNEXPECTED);
        assertFalse(U.UNEXPECTED.isEmpty());
        assertNotNull(U.UNKNOWN);
        assertFalse(U.UNKNOWN.isEmpty());
        assertNotNull(U.UNMET);
        assertFalse(U.UNMET.isEmpty());
        assertNotNull(U.UNNAMED);
        assertFalse(U.UNNAMED.isEmpty());
        assertNotNull(U.UNREACHABLE);
        assertFalse(U.UNREACHABLE.isEmpty());
        assertNotNull(U.UNRESOLVED);
        assertFalse(U.UNRESOLVED.isEmpty());
        assertNotNull(U.UNTIL);
        assertFalse(U.UNTIL.isEmpty());
        assertNotNull(U.UPDATE);
        assertFalse(U.UPDATE.isEmpty());
        assertNotNull(U.UPLOADING);
        assertFalse(U.UPLOADING.isEmpty());
        assertNotNull(U.UPPER);
        assertFalse(U.UPPER.isEmpty());
        assertNotNull(U.UPSTREAM);
        assertFalse(U.UPSTREAM.isEmpty());
        assertNotNull(U.USERS);
        assertFalse(U.USERS.isEmpty());
        assertNotNull(U.USING);
        assertFalse(U.USING.isEmpty());
        assertNotNull(U.UPDATING);
        assertFalse(U.UPDATING.isEmpty());
    }
}
