package works.lysenko.util.apis.util;

/**
 * Special button with behavior of Elevator buttons
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _BotButton {

    /**
     * turn highlighting on
     */
    void activate();

    /**
     * turn highlighting off
     */
    void deactivate();

    /**
     * @return true if button's function was requested
     */
    boolean isActive();
}
