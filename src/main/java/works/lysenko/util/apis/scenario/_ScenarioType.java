package works.lysenko.util.apis.scenario;

/**
 * Provides tag per Scenario type
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _ScenarioType {

    /**
     * @return tag
     */
    String tag();
}
