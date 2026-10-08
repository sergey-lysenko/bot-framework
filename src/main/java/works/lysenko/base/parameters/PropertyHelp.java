package works.lysenko.base.parameters;

import works.lysenko.util.spec.PropEnum;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import static works.lysenko.util.func.type.Objects.isNotNull;

final class PropertyHelp {

    private static final String RESOURCE = "/property-help.properties";
    private static final Properties DESCRIPTIONS = loadDescriptions();

    private PropertyHelp() {
    }

    static String getDescription(final String propertyName) {

        return DESCRIPTIONS.getProperty(propertyName);
    }

    static Set<String> getPropertyNames() {

        return Collections.unmodifiableSet(DESCRIPTIONS.stringPropertyNames());
    }

    /**
     * Resolves help text description for a given parameter label or property name.
     *
     * @param label parameter label or property name
     * @return description from property help if found, null otherwise
     */
    static String findHelp(final String label) {

        if (null == label || label.isBlank()) {
            return null;
        }

        final String direct = getDescription(label);
        if (isNotNull(direct) && !direct.isBlank()) {
            return direct;
        }

        for (final PropEnum prop : PropEnum.values()) {
            final String name = prop.name();
            final String subName = name.startsWith("_") ? name.substring(1) : name;

            if (label.equalsIgnoreCase(subName)
                    || prop.getPropertyName().equalsIgnoreCase(label)
                    || (subName.startsWith("TEST_") && label.equalsIgnoreCase(subName.substring(5)))
                    || (subName.startsWith("TREE_") && label.equalsIgnoreCase(subName.substring(5)))) {
                final String desc = getDescription(prop.getPropertyName());
                if (isNotNull(desc) && !desc.isBlank()) {
                    return desc;
                }
            }
        }

        return null;
    }

    private static Properties loadDescriptions() {

        final Properties descriptions = new Properties();
        try (InputStream stream = PropertyHelp.class.getResourceAsStream(RESOURCE)) {
            if (null == stream) {
                throw new IllegalStateException("Missing property help resource: " + RESOURCE);
            }
            descriptions.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to load property help resource: " + RESOURCE, e);
        }
        return descriptions;
    }
}
