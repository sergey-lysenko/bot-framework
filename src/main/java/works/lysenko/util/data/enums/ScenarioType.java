package works.lysenko.util.data.enums;

import works.lysenko.util.apis.scenario._ScenarioType;

/**
 * Enumeration that represents different types of scenarios.
 */
@SuppressWarnings("EnumClass")
public enum ScenarioType implements _ScenarioType {

    /**
     * Leaf
     */
    LEAF("◆"),

    /**
     * Node
     */
    NODE("▷"),

    /**
     * Mono
     */
    MONO("◼"),

    /**
     * CORE
     */
    CORE("●");

    private final String tag;

    ScenarioType(final String tag) {

        this.tag = tag;
    }

    /**
     * @return tag of this Type
     */
    public String tag() {

        return tag;
    }
}
