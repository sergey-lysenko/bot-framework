package works.lysenko.tree.inheritance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.TestProperties;
import works.lysenko.tree.inheritance.outer.Alias;
import works.lysenko.util.prop.tree.Traverse;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.type.fractions.Factory.fr;

class ExtensionWeightTraversalTest {

    private final TestProperties previousProperties = Base.properties;

    @AfterEach
    void tearDown() {

        Base.properties = previousProperties;
        Traverse.refresh();
    }

    @Test
    void usesConcreteScenarioWeightWhenConfigured() {

        configure(
                "works.lysenko.tree.inheritance.LinkedNode", "3.0",
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "true"
        );

        assertEquals(fr(3.0), new LinkedNode().weightConfigured());
    }

    @Test
    void usesInheritedScenarioWeightWhenExtensionTraversalIsEnabled() {

        configure(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "true"
        );

        assertEquals(fr(2.0), new LinkedNode().weightConfigured());
    }

    @Test
    void ignoresInheritedScenarioWeightWhenExtensionTraversalIsDisabled() {

        configure(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "false"
        );

        assertEquals(fr(0.0), new LinkedNode().weightConfigured());
    }

    @Test
    void retainsTraversalSettingUntilConfigurationIsRefreshed() {

        configure(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "true"
        );
        assertEquals(fr(2.0), new LinkedNode().weightConfigured());

        install(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "false"
        );

        assertEquals(fr(2.0), new LinkedNode().weightConfigured());
    }

    @Test
    void propagatesExtensionParentWeightToSharedChildPackage() {

        configure(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "true"
        );
        final LinkedNode parent = new LinkedNode();
        final var child = parent.getPool().getSortedSet().iterator().next();

        assertEquals(fr(2.0), downstreamWeight(child, parent));
    }

    @Test
    void retainsExtensionTraversalForAnActiveTreeAfterTheGlobalCacheRefreshes() {

        configure(
                "works.lysenko.tree.inheritance.Parent", "2.0",
                ".tree.traverse.extensions", "true"
        );
        final LinkedNode parent = new LinkedNode();
        final var child = parent.getPool().getPairList().get(0).k();

        Traverse.freeze();
        try {
            install(
                    "works.lysenko.tree.inheritance.Parent", "2.0",
                    ".tree.traverse.extensions", "false"
            );
            Traverse.refresh();

            assertEquals(fr(2.0), downstreamWeight(child, parent));
        } finally {
            Traverse.unfreeze();
        }
    }

    @Test
    void propagatesAncestorWeightAcrossExtensionBoundary() {

        final String outerName = new Outer().getShortName();
        configure(
                outerName, "1.0",
                ".tree.traverse.extensions", "true"
        );
        final Alias alias = (Alias) new Outer().getPool().getSortedSet().iterator().next();
        final var child = alias.getPool().getSortedSet().iterator().next();

        assertEquals(fr(1.0), downstreamWeight(child, alias));
    }

    @Test
    void propagatesWeightThroughMultipleExtensionAliases() {

        final AliasRoot root = new AliasRoot();
        configure(
                AliasRoot.class.getName(), "1.0",
                ".tree.traverse.extensions", "true"
        );
        assertEquals(fr(1.0), root.weightConfigured());
        final Alias alias = (Alias) root.getPool().getPairList().get(0).k();
        final var child = alias.getPool().getPairList().get(0).k();

        assertEquals(fr(1.0), downstreamWeight(child, alias));
    }

    private static void configure(final String... values) {

        install(values);
        Traverse.refresh();
    }

    private static void install(final String... values) {

        final Properties properties = new Properties();
        for (int i = 0; i < values.length; i += 2) properties.setProperty(values[i], values[i + 1]);
        try {
            final TestProperties testProperties = new TestProperties();
            final Field field = TestProperties.class.getDeclaredField("the");
            field.setAccessible(true);
            field.set(testProperties, properties);
            Base.properties = testProperties;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
