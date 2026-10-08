package works.lysenko.base.issues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a known issue entry associated with a scenario or pattern.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KnownIssue(
        @JsonProperty("scenario") String scenario,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("link") String link,
        @JsonProperty("pattern") String pattern
) {
    public KnownIssue {
        if (null == title) title = "";
        if (null == description) description = "";
        if (null == link) link = "";
        if (null == pattern) pattern = "";
    }
}
