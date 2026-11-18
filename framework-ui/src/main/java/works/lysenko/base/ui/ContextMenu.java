package works.lysenko.base.ui;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utility for attaching right-click context menus to Swing text components.
 */
public final class ContextMenu {

    private ContextMenu() {
    }

    /**
     * Attaches a right-click context menu (Cut, Copy, Paste, Select All) to the specified text components.
     *
     * @param components text components to attach the context menu to
     */
    public static void attach(final JTextComponent... components) {
        if (components != null) {
            for (final JTextComponent component : components) {
                if (component != null) {
                    attachToComponent(component);
                }
            }
        }
    }

    private static void attachToComponent(final JTextComponent component) {
        final JPopupMenu menu = new JPopupMenu();
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        final JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask));
        cutItem.addActionListener(e -> component.cut());

        final JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask));
        copyItem.addActionListener(e -> component.copy());

        final JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask));
        pasteItem.addActionListener(e -> component.paste());

        final JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask));
        selectAllItem.addActionListener(e -> component.selectAll());

        menu.add(cutItem);
        menu.add(copyItem);
        menu.add(pasteItem);
        menu.addSeparator();
        menu.add(selectAllItem);

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                component.requestFocusInWindow();
                final boolean hasSelection = component.getSelectionStart() != component.getSelectionEnd();
                final boolean isEditable = component.isEditable() && component.isEnabled();
                cutItem.setEnabled(isEditable && hasSelection);
                copyItem.setEnabled(hasSelection);
                pasteItem.setEnabled(isEditable && isClipboardStringAvailable());
                final String text = component.getText();
                selectAllItem.setEnabled(text != null && !text.isEmpty());
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {}
        });

        component.setComponentPopupMenu(menu);

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                handlePopupTrigger(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                handlePopupTrigger(e);
            }

            private void handlePopupTrigger(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    component.requestFocusInWindow();
                    final int pos = component.viewToModel2D(e.getPoint());
                    if (pos >= 0) {
                        final int selStart = component.getSelectionStart();
                        final int selEnd = component.getSelectionEnd();
                        if (selStart == selEnd || pos < selStart || pos > selEnd) {
                            component.setCaretPosition(pos);
                        }
                    }
                }
            }
        });
    }

    private static boolean isClipboardStringAvailable() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (final Exception ignored) {
            return true;
        }
    }
}
