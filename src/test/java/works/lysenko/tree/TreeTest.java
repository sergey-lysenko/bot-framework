package works.lysenko.tree;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TreeTest {

    @Test
    void testTreeInitializationAndGetters() {
        final String testPackage = "works.lysenko.tree.inheritance";
        final Tree tree = new Tree(testPackage);

        assertEquals(testPackage, tree.getRootPackage(), "Root package should match initialized value");

        final Set<_Scenario> branches = tree.getBranches();
        assertNotNull(branches, "Branches set should not be null");
    }
}
