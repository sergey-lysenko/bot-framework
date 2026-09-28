package works.lysenko.util.apis.action;

/**
 * ProvidesInfo is a functional interface that represents an object which can provide information about the current Scenario
 * execution.
 */
@FunctionalInterface
public interface ProvidesInfo {

    /**
     * @return information about current Scenario execution
     */
    String info();
}
