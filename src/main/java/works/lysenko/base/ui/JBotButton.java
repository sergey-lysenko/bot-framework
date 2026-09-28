package works.lysenko.base.ui;

import works.lysenko.util.apis.util._BotButton;

import javax.swing.JButton;
import java.awt.Color;

/**
 * Button with additional bot-related functionality
 */
@SuppressWarnings({"WeakerAccess", "ClassWithoutLogger", "ClassUnconnectedToPackage", "unused", "ClassWithoutNoArgConstructor",
        "ClassHasNoToStringMethod", "FinalClass", "ClassWithTooManyTransitiveDependents"})
public final class JBotButton extends JButton implements _BotButton {

    private boolean active = false;

    /**
     * @param text of button
     */
    @SuppressWarnings("PublicConstructor")
    public JBotButton(final String text) {

        super(text);
        addActionListener(e -> {
            active = !active;
            actualizeUI();
        });
    }

    /**
     * @param text    of button
     * @param initial state (true = pressed)
     */
    @SuppressWarnings({"PublicConstructor", "BooleanParameter"})
    public JBotButton(final String text, final boolean initial) {

        this(text);
        active = initial;
    }

    @Override
    public void activate() {

        active = true;
        actualizeUI();
    }

    public void deactivate() {

        active = false;
        actualizeUI();
    }

    public boolean isActive() {

        return active;
    }

    private void actualizeUI() {

        if (active) setForeground(Color.RED);
        else setForeground(Color.BLACK);
    }
}
