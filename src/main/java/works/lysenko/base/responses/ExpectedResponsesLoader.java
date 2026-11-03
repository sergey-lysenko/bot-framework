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

    private static final String[] CLASSPATH_RESOURCES = {
        "expected/responses.json",
        "known/expected.responses.json",
        "expected.responses.json"
    };

    private static final String[] FILE_PATHS = {
        "src/main/resources/expected/responses.json",
        "expected/responses.json",
        "src/main/resources/known/expected.responses.json",
        "src/main/resources/expected.responses.json"
    };

    private ExpectedResponsesLoader() {
        // Utility class
    }

    /**
     * Finds the input stream for expected/responses.json (or legacy fallbacks).
     *
     * @return the input stream or null if not found
     */
    public static InputStream findInputStream() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                for (final String res : CLASSPATH_RESOURCES) {
                    is = contextClassLoader.getResourceAsStream(res);
                    if (isNotNull(is)) break;
                }
            }
            if (null == is) {
                for (final String res : CLASSPATH_RESOURCES) {
                    is = ExpectedResponsesLoader.class.getResourceAsStream("/" + res);
                    if (isNotNull(is)) break;
                }
            }
            if (null == is) {
                for (final String path : FILE_PATHS) {
                    final java.io.File file = new java.io.File(path);
                    if (file.exists() && file.isFile()) {
                        is = new java.io.FileInputStream(file);
                        break;
                    }
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
     * Loads expected responses from a file if it exists.
     *
     * @param file the JSON file to load
     * @return list of expected responses
     */
    public static List<ExpectedResponse> loadFromFile(final java.io.File file) {
        if (null == file || !file.exists() || !file.isFile()) return Collections.emptyList();
        try (final InputStream is = new java.io.FileInputStream(file)) {
            return loadFromStream(is);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Saves the current expected/responses.json content into a specific file.
     *
     * @param outputFile the destination file
     */
    public static void saveRunExpectedResponses(final java.io.File outputFile) {
        if (null == outputFile) return;
        try (final InputStream is = findInputStream()) {
            if (isNotNull(is)) {
                final byte[] bytes = is.readAllBytes();
                if (bytes.length > 0) {
                    if (isNotNull(outputFile.getParentFile())) {
                        outputFile.getParentFile().mkdirs();
                    }
                    java.nio.file.Files.write(outputFile.toPath(), bytes);
                    works.lysenko.Base.logDebug("Saved expected responses dataset to " + outputFile.getAbsolutePath());
                }
            }
        } catch (final Throwable e) {
            works.lysenko.Base.logDebug("Unable to save run expected responses dataset: " + e.getMessage());
        }
    }

    /**
     * Saves the current expected/responses.json content into the run result dataset as {runId}.expected.responses.json.
     */
    public static void saveRunExpectedResponses() {
        saveRunExpectedResponses(new java.io.File(works.lysenko.util.spec.Layout.Files.name(works.lysenko.util.spec.Layout.Templates.RUN_EXPECTED_RESPONSES_)));
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
                        final String reason = (null != raw.reason()) ? raw.reason().trim() : "";
                        final String pattern = raw.pattern().trim();
                        if (isNotNull(raw.scenarios()) && !raw.scenarios().isEmpty()) {
                            for (final String sc : raw.scenarios()) {
                                if (isNotNull(sc) && !sc.isBlank()) {
                                    list.add(new ExpectedResponse(sc.trim(), pattern, reason));
                                }
                            }
                        } else {
                            final String scenario = (null != raw.scenario() && !raw.scenario().isBlank()) ? raw.scenario().trim() : "*";
                            list.add(new ExpectedResponse(scenario, pattern, reason));
                        }
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
        @JsonProperty("scenarios") List<String> scenarios,
        @JsonProperty("pattern") String pattern,
        @JsonProperty("reason") String reason
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExpectedResponsesContainer(
        @JsonProperty("responses") List<ExpectedResponseRaw> responses
    ) {}
}
