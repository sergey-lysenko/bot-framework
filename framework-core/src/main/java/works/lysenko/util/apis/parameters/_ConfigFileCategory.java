package works.lysenko.util.apis.parameters;

/**
 * Scope descriptor for configuration and suite files validated by {@code PropertyValidator}.
 */
@FunctionalInterface
public interface _ConfigFileCategory {

    /**
     * Returns {@code true} when only dot-prefixed global/configuration keys are permitted in this file category.
     *
     * @return true if scenario keys without a leading dot are disallowed
     */
    boolean isConfigOnlyScope();
}
