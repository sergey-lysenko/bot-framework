package works.lysenko.base.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextMenuTest {

    @org.junit.jupiter.api.BeforeAll
    static void setUpAll() {
        ControlPanel.initializeLookAndFeel();
    }

    @Test
    void testAttachContextMenu() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final JTextField textField = new JTextField("Sample text");
            final JTextArea textArea = new JTextArea("Sample area text");

            ContextMenu.attach(textField, textArea);

            final JPopupMenu tfPopup = textField.getComponentPopupMenu();
            assertNotNull(tfPopup, "JTextField should have a component popup menu");

            final JPopupMenu taPopup = textArea.getComponentPopupMenu();
            assertNotNull(taPopup, "JTextArea should have a component popup menu");

            // Check menu items
            assertEquals(5, tfPopup.getComponentCount(), "Popup menu should have 4 items and 1 separator");
            assertTrue(tfPopup.getComponent(0) instanceof JMenuItem);
            final JMenuItem cutItem = (JMenuItem) tfPopup.getComponent(0);
            final JMenuItem copyItem = (JMenuItem) tfPopup.getComponent(1);
            final JMenuItem pasteItem = (JMenuItem) tfPopup.getComponent(2);
            final JMenuItem selectAllItem = (JMenuItem) tfPopup.getComponent(4);

            assertEquals("Cut", cutItem.getText());
            assertEquals("Copy", copyItem.getText());
            assertEquals("Paste", pasteItem.getText());
            assertEquals("Select All", selectAllItem.getText());

            final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask), cutItem.getAccelerator());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask), copyItem.getAccelerator());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask), pasteItem.getAccelerator());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask), selectAllItem.getAccelerator());

            // Fire popupMenuWillBecomeVisible to verify dynamic enable/disable states
            for (final PopupMenuListener listener : tfPopup.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeVisible(new PopupMenuEvent(tfPopup));
            }

            // Without selection, cut and copy should be disabled
            assertFalse(cutItem.isEnabled(), "Cut should be disabled without selection");
            assertFalse(copyItem.isEnabled(), "Copy should be disabled without selection");
            assertTrue(selectAllItem.isEnabled(), "Select All should be enabled when text is present");

            // With selection
            textField.selectAll();
            for (final PopupMenuListener listener : tfPopup.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeVisible(new PopupMenuEvent(tfPopup));
            }
            assertTrue(cutItem.isEnabled(), "Cut should be enabled with selection");
            assertTrue(copyItem.isEnabled(), "Copy should be enabled with selection");

            // With non-editable text field
            textField.setEditable(false);
            for (final PopupMenuListener listener : tfPopup.getPopupMenuListeners()) {
                listener.popupMenuWillBecomeVisible(new PopupMenuEvent(tfPopup));
            }
            assertFalse(cutItem.isEnabled(), "Cut should be disabled for non-editable field");
            assertTrue(copyItem.isEnabled(), "Copy should be enabled even if non-editable");
            assertFalse(pasteItem.isEnabled(), "Paste should be disabled for non-editable field");
        });
    }

    @Test
    void testMenuShortcutKeyMaskApplied() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ControlPanel.initializeLookAndFeel();
            final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            final JTextField tf = new JTextField();
            assertEquals("paste-from-clipboard", tf.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask)));
            assertEquals("copy-to-clipboard", tf.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask)));
            assertEquals("cut-to-clipboard", tf.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask)));
            assertEquals("select-all", tf.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask)));
        });
    }
}
