package works.lysenko.base.ui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class AutoCompleteComboBox extends JComboBox<String> {

    private final List<String> allItems;

    public AutoCompleteComboBox(List<String> items) {
        super(items.toArray(new String[0]));
        this.allItems = new ArrayList<>(items);
        setEditable(true);

        final JTextComponent editor = (JTextComponent) getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                char ch = e.getKeyChar();
                if (ch == KeyEvent.VK_ENTER || ch == KeyEvent.VK_ESCAPE ||
                        e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();
                    if (text.isEmpty()) {
                        hidePopup();
                        setModel(new DefaultComboBoxModel<>(allItems.toArray(new String[0])));
                        editor.setText(text);
                        return;
                    }

                    List<String> filtered = new ArrayList<>();
                    for (String item : allItems) {
                        if (item.toLowerCase().contains(text.toLowerCase())) {
                            filtered.add(item);
                        }
                    }

                    if (filtered.isEmpty()) {
                        hidePopup();
                    } else {
                        setModel(new DefaultComboBoxModel<>(filtered.toArray(new String[0])));
                        editor.setText(text);
                        showPopup();
                    }
                });
            }
        });
    }

    public String getText() {
        return ((JTextComponent) getEditor().getEditorComponent()).getText();
    }

    public void setText(String text) {
        ((JTextComponent) getEditor().getEditorComponent()).setText(text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        getEditor().getEditorComponent().setEnabled(enabled);
        if (getEditor().getEditorComponent() instanceof JTextComponent textComp) {
            textComp.setEditable(enabled);
        }
    }
}
