package works.lysenko.base.output.loghtml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * HTML template resource loader and token placeholder replacement engine.
 */
@SuppressWarnings({"ClassWithoutLogger", "NestedMethodCall"})
public final class TemplateEngine {

    private static final String TEMPLATE_RESOURCE = "works/lysenko/base/output/log-report-template.html";

    private TemplateEngine() {
    }

    /**
     * Loads the HTML report template resource from the classpath.
     *
     * @return template string content, or an empty string if loading fails
     */
    public static String loadTemplate() {
        try (final InputStream is = TemplateEngine.class.getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (is == null) {
                logEvent(S2, s("log-report-template.html not found on classpath: ", TEMPLATE_RESOURCE));
                return "";
            }
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                final StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(s(line, "\n"));
                }
                return sb.toString();
            }
        } catch (final IOException e) {
            logEvent(S2, s("Failed to load log-report-template.html: ", e.getMessage()));
            return "";
        }
    }

    /**
     * Replaces placeholders in the HTML template matching {@code {{KEY}}} with corresponding map values.
     *
     * @param template    raw HTML template string
     * @param replacements map of template placeholders to replacement values
     * @return template populated with dynamic values
     */
    public static String replaceTemplate(final String template, final Map<String, String> replacements) {
        final long capacity = (long) template.length() + replacements.values().stream()
                .mapToLong(String::length).sum();
        final StringBuilder output = new StringBuilder((int) Math.min(Integer.MAX_VALUE - 8L, capacity));
        int cursor = 0;
        while (cursor < template.length()) {
            final int placeholderStart = template.indexOf("{{", cursor);
            if (placeholderStart < 0) break;
            final int placeholderEnd = template.indexOf("}}", placeholderStart + 2);
            if (placeholderEnd < 0) break;
            output.append(template, cursor, placeholderStart);
            final String placeholder = template.substring(placeholderStart, placeholderEnd + 2);
            final String replacement = replacements.get(placeholder);
            if (null == replacement) {
                output.append(placeholder);
            } else {
                output.append(replacement);
            }
            cursor = placeholderEnd + 2;
        }
        output.append(template, cursor, template.length());
        return output.toString();
    }
}
