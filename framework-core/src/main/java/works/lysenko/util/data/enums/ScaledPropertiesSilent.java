package works.lysenko.util.data.enums;

/**
 * Enumeration representing the silent mode for log messages in the ScaledProperties class.
 */
public enum ScaledPropertiesSilent {

    /**
     * Not silent: log messages will be displayed.
     */
    NO,

    /**
     * Silent unless scaling is performed.
     */
    IF_NOT_SCALED,

    /**
     * Fully silent: log messages will be suppressed.
     */
    YES
}
