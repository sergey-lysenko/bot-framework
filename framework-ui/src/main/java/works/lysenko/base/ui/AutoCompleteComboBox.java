package works.lysenko.base.ui;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The AutoCompleteComboBox class.
 */
public final class AutoCompleteComboBox extends JComboBox<String> {

    /**
     * Constructs a new AutoCompleteComboBox.
     *
     * @param items the items
     */
    AutoCompleteComboBox(final List<String> items) {
        super(items.toArray(String[]::new));
        setEditable(true);

        final JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();
        editor.addKeyListener(new FilterKeyAdapter(editor, new ArrayList<>(items)));
    }

    /**
     * Performs get text.
     *
     * @return the get text result
     */
    String getText() {
        return ((JTextComponent) getEditor().getEditorComponent()).getText();
    }

    /**
     * Performs set text.
     *
     * @param text the text
     */
    void setText(final String text) {
        ((JTextComponent) getEditor().getEditorComponent()).setText(text);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        getEditor().getEditorComponent().setEnabled(enabled);
        if (getEditor().getEditorComponent() instanceof final JTextComponent textComp) {
            textComp.setEditable(enabled);
        }
    }

    private final class FilterKeyAdapter extends KeyAdapter {

        private final JTextComponent editor;
        private final List<String> items;

        private FilterKeyAdapter(final JTextComponent editor, final List<String> items) {
            this.editor = editor;
            this.items = items;
        }

        private static boolean isIgnoredKey(final KeyEvent e) {
            final char ch = e.getKeyChar();
            return KeyEvent.VK_ENTER == ch || KeyEvent.VK_ESCAPE == ch || isArrowKey(e.getKeyCode());
        }

        private static boolean isArrowKey(final int code) {
            return isVerticalArrow(code) || isHorizontalArrow(code);
        }

        private static boolean isVerticalArrow(final int code) {
            return KeyEvent.VK_UP == code || KeyEvent.VK_DOWN == code;
        }

        private static boolean isHorizontalArrow(final int code) {
            return KeyEvent.VK_LEFT == code || KeyEvent.VK_RIGHT == code;
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            if (!isIgnoredKey(e)) {
                SwingUtilities.invokeLater(this::updateFilteredModel);
            }
        }

        private void updateFilteredModel() {
            final String text = editor.getText();
            if (text.isEmpty()) {
                hidePopup();
                setModel(new DefaultComboBoxModel<>(items.toArray(String[]::new)));
                editor.setText(text);
            } else {
                final List<String> filtered = filterItems(text);
                if (filtered.isEmpty()) {
                    hidePopup();
                } else {
                    setModel(new DefaultComboBoxModel<>(filtered.toArray(String[]::new)));
                    editor.setText(text);
                    showPopup();
                }
            }
        }

        private List<String> filterItems(final String text) {
            final String query = text.toLowerCase(Locale.ROOT);
            final List<String> filtered = new ArrayList<>(items.size());
            for (final String item : items) {
                if (item.toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(item);
                }
            }
            return filtered;
        }
    }
}
