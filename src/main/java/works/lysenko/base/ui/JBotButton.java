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
    private final String inactiveText;
    private String activeText = null;

    /**
     * @param text of button
     */
    @SuppressWarnings("PublicConstructor")
    public JBotButton(final String text) {

        super(text);
        this.inactiveText = text;
        addActionListener(e -> {
            active = !active;
            actualizeUI();
        });
    }

    /**
     * @param text       inactive text of button
     * @param activeText active text of button
     */
    @SuppressWarnings("PublicConstructor")
    public JBotButton(final String text, final String activeText) {

        this(text);
        this.activeText = activeText;
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

    public void setActiveText(final String activeText) {

        this.activeText = activeText;
        actualizeUI();
    }

    private void actualizeUI() {

        if (active) {
            setForeground(new Color(0xDC, 0x26, 0x26));
            if (null != activeText) setText(activeText);
        } else {
            setForeground(new Color(0x0F, 0x17, 0x2A));
            if (null != inactiveText) setText(inactiveText);
        }
    }
}
