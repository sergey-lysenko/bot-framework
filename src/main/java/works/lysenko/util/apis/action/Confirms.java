package works.lysenko.util.apis.action;

/**
 * Confirms interface represents an action that can be confirmed.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface Confirms {

    /**
     * Confirm
     */
    void confirm();

    /**
     * @return the text of the confirm button.
     */
    String getConfirmButtonText();
}
