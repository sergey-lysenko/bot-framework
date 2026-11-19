package works.lysenko.base.eta.model;

/**
 * High-level lifecycle state of a runtime ETA estimate.
 */
public enum EtaState {
    INITIALIZING,
    ESTIMATED,
    DONE,
    UNAVAILABLE
}
