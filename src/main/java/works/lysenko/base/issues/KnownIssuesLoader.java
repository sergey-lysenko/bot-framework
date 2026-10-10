package works.lysenko.base.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES;
import static works.lysenko.util.spec.Layout.Files.KNOWN_ISSUES_;
import static works.lysenko.util.spec.Symbols._SLASH_;

/**
 * Utility class for loading known issues from configuration streams.
 */
@SuppressWarnings("all")
public final class KnownIssuesLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RESOURCE = s(_SLASH_, KNOWN_ISSUES);
    private static final String LEGACY_RESOURCE = "known/issues";
    private static final String LEGACY_RESOURCE_ALT = "/known/issues";
    private static final String LEGACY_FILE_PATH = "src/main/resources/known/issues";

    private KnownIssuesLoader() {
        // Utility class
    }

    /**
     * Finds the default configuration input stream.
     *
     * @return the input stream, or null if not found
     */
    public static InputStream findInputStream() {
        InputStream is = null;
        try {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (isNotNull(contextClassLoader)) {
                is = contextClassLoader.getResourceAsStream(KNOWN_ISSUES);
                if (null == is) is = contextClassLoader.getResourceAsStream(LEGACY_RESOURCE);
            }
            if (null == is) is = KnownIssuesLoader.class.getResourceAsStream(DEFAULT_RESOURCE);
            if (null == is) is = KnownIssuesLoader.class.getResourceAsStream(LEGACY_RESOURCE_ALT);
            if (null == is) {
                final java.io.File f1 = new java.io.File(KNOWN_ISSUES_);
                if (f1.exists()) is = new java.io.FileInputStream(f1);
                else {
                    final java.io.File f2 = new java.io.File(KNOWN_ISSUES);
                    if (f2.exists()) is = new java.io.FileInputStream(f2);
                    else {
                        final java.io.File f3 = new java.io.File(LEGACY_FILE_PATH);
                        if (f3.exists()) is = new java.io.FileInputStream(f3);
                        else {
                            final java.io.File f4 = new java.io.File(LEGACY_RESOURCE);
                            if (f4.exists()) is = new java.io.FileInputStream(f4);
                        }
                    }
                }
            }
        } catch (final SecurityException | IOException ignored) {
        }
        return is;
    }

    /**
     * Loads known issues from the default location.
     *
     * @return a list of known issues
     */
    public static List<KnownIssue> loadDefault() {
        List<KnownIssue> issues = Collections.emptyList();
        try (final InputStream is = findInputStream()) {
            if (isNotNull(is)) issues = loadFromStream(is);
        } catch (final IOException ignored) {
        }
        return issues;
    }

    /**
     * Loads known issues from an input stream.
     *
     * @param is the input stream
     * @return a list of known issues
     */
    public static List<KnownIssue> loadFromStream(final InputStream is) {
        final List<KnownIssue> issues = new ArrayList<>();
        if (null == is) return issues;
        byte[] bytes;
        try {
            bytes = is.readAllBytes();
        } catch (final IOException e) {
            return issues;
        }

        try {
            final KnownIssuesContainer container = MAPPER.readValue(bytes, KnownIssuesContainer.class);
            if (isNotNull(container) && isNotNull(container.issues())) {
                issues.addAll(container.issues());
                return issues;
            }
        } catch (final IOException ignored) {
        }

        // Fallback: parse as Properties / Key-Value lines
        try {
            final Properties props = new Properties();
            props.load(new ByteArrayInputStream(bytes));
            for (final String key : props.stringPropertyNames()) {
                final String val = props.getProperty(key);
                if (isNotNull(key) && !key.isBlank() && isNotNull(val) && !val.isBlank()) {
                    if ("XXX".equalsIgnoreCase(key.trim()) && "YYY".equalsIgnoreCase(val.trim())) {
                        continue; // Skip dummy placeholder entries
                    }
                    issues.add(new KnownIssue(key.trim(), key.trim(), val.trim(), "", val.trim()));
                }
            }
        } catch (final IOException ignored) {
        }

        return issues;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KnownIssuesContainer(List<KnownIssue> issues) {}
}

