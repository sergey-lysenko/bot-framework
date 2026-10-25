package works.lysenko.util.func.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Parameters;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UsersTest {

    private Parameters previousParameters;

    @BeforeEach
    void setUp() {
        previousParameters = Base.parameters;
        Base.parameters = new Parameters(new Properties());
    }

    @AfterEach
    void tearDown() {
        Base.parameters = previousParameters;
    }

    @Test
    void testUsersInstantiation() {
        Users users = new Users();
        assertNotNull(users);
    }

    @Test
    void testListAndPick() {
        assertDoesNotThrow(() -> Users.list());
    }
}
