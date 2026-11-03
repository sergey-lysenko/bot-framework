package works.lysenko.base.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Loader utility for expected browser responses configuration.
 */
public final class ExpectedResponsesLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExpectedResponsesLoader() {
        // Utility class
    }

    /**
     * Finds the input stream for expected.responses.json.
     *
     * @return the input stream or null if not found
     */
    public static InputStream findInputStream() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                is = contextClassLoader.getResourceAsStream("known/expected.responses.json");
                if (null == is) is = contextClassLoader.getResourceAsStream("expected.responses.json");
            }
            if (null == is) {
                final java.io.File f1 = new java.io.File("src/main/resources/known/expected.responses.json");
                if (f1.exists()) is = new java.io.FileInputStream(f1);
                else {
                    final java.io.File f2 = new java.io.File("src/main/resources/expected.responses.json");
                    if (f2.exists()) is = new java.io.FileInputStream(f2);
                }
            }
        } catch (final Exception ignored) {
        }
        return is;
    }

    /**
     * Loads default expected responses.
     *
     * @return list of expected responses
     */
    public static List<ExpectedResponse> loadDefault() {
        try (final InputStream is = findInputStream()) {
            if (isNotNull(is)) return loadFromStream(is);
        } catch (final Exception ignored) {
        }
        return Collections.emptyList();
    }

    /**
     * Loads expected responses from an input stream.
     *
     * @param is input stream
     * @return list of expected responses
     */
    public static List<ExpectedResponse> loadFromStream(final InputStream is) {
        final List<ExpectedResponse> list = new ArrayList<>();
        if (null == is) return list;
        try {
            final byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) return list;
            final ExpectedResponsesContainer container = MAPPER.readValue(bytes, ExpectedResponsesContainer.class);
            if (isNotNull(container) && isNotNull(container.responses())) {
                for (final ExpectedResponseRaw raw : container.responses()) {
                    if (isNotNull(raw) && isNotNull(raw.pattern()) && !raw.pattern().isBlank()) {
                        final String scenario = (null != raw.scenario()) ? raw.scenario().trim() : "*";
                        final String reason = (null != raw.reason()) ? raw.reason().trim() : "";
                        list.add(new ExpectedResponse(scenario, raw.pattern().trim(), reason));
                    }
                }
            }
        } catch (final Exception ignored) {
        }
        return list;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpectedResponseRaw(
        @JsonProperty("scenario") String scenario,
        @JsonProperty("pattern") String pattern,
        @JsonProperty("reason") String reason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpectedResponsesContainer(
        @JsonProperty("responses") List<ExpectedResponseRaw> responses
    ) {}
}
