package works.lysenko.util.apis.parameters;

/**
 * Represents an interface for displaying a Graphical User Interface (GUI).
 * The implementing class should provide a method to display the GUI for parameter input and user interaction.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _GUI {

    /**
     * Displays the graphical user interface for parameter input and user interaction.
     * This method adds standard parameters, adds additional parameters, displays a dialogue box,
     * propagates user input, and allows the user to exit the program.
     */
    void display();
}
