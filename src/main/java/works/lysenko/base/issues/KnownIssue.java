package works.lysenko.base.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a known issue entry associated with a scenario or pattern.
 *
 * @param scenario          the target scenario pattern
 * @param title             the issue title
 * @param description       the issue description
 * @param link              the issue tracking link
 * @param pattern           the log record matching pattern
 * @param decreaseToWarning whether matching failure events should be decreased to warning severity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnownIssue(
        @JsonProperty("scenario") String scenario,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("link") String link,
        @JsonProperty("pattern") String pattern,
        @JsonProperty("decrease-to-warning") boolean decreaseToWarning
) {
    /**
     * Compact constructor normalizing null fields to empty strings.
     */
    public KnownIssue {
        if (null == title) title = "";
        if (null == description) description = "";
        if (null == link) link = "";
        if (null == pattern) pattern = "";
    }

    /**
     * Constructs a KnownIssue that decreases matching failures to warnings by default.
     *
     * @param scenario    the target scenario pattern
     * @param title       the issue title
     * @param description the issue description
     * @param link        the issue tracking link
     * @param pattern     the log record matching pattern
     */
    public KnownIssue(
            final String scenario,
            final String title,
            final String description,
            final String link,
            final String pattern) {
        this(scenario, title, description, link, pattern, true);
    }
}
