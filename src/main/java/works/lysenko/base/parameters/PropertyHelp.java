package works.lysenko.base.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

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
