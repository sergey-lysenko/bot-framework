package works.lysenko.tree;

import org.junit.jupiter.api.Test;
import works.lysenko.tree.inheritance.IndirectlyLinkedNode;
import works.lysenko.tree.inheritance.LinkedNode;
import works.lysenko.tree.inheritance.parent.Child;
import works.lysenko.util.apis.scenario._Scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodeInheritanceTest {

    @Test
    void inheritedNodeUsesItsParentChildPackage() {

        assertContainsParentChild(new LinkedNode());
    }

    @Test
    void indirectlyInheritedNodeUsesItsNearestNodeOwnerChildPackage() {

        assertContainsParentChild(new IndirectlyLinkedNode());
    }

    private static void assertContainsParentChild(final works.lysenko.tree.base.Node node) {

        assertEquals(1, node.getPool().getSortedSet().size());
        final _Scenario child = node.getPool().getSortedSet().iterator().next();
        assertTrue(child instanceof Child);
    }
}
