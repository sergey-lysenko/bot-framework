package works.lysenko.base.ui;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static works.lysenko.util.chrs.__.CO;
import static works.lysenko.util.chrs.__.CU;
import static works.lysenko.util.chrs.__.PA;
import static works.lysenko.util.chrs.__.ST;
import static works.lysenko.util.chrs.___.ALL;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.word.S.SELECT;
import static works.lysenko.util.spec.Symbols.E;
import static works.lysenko.util.spec.Symbols.P;
import static works.lysenko.util.spec.Symbols.T;
import static works.lysenko.util.spec.Symbols.Y;

/**
 * Utility for attaching right-click context menus to Swing text components.
 */
enum ContextMenu {
    ;

    private static final String CUT_LABEL = c(s(CU, T));
    private static final String COPY_LABEL = c(s(CO, P, Y));
    private static final String PASTE_LABEL = c(s(PA, ST, E));
    private static final String SELECT_ALL_LABEL = b(c(SELECT), c(ALL));

    /**
     * Attaches a right-click context menu (Cut, Copy, Paste, Select All) to the specified text components.
     *
     * @param components text components to attach the context menu to
     */
    static void attach(final JTextComponent... components) {
        if (null != components) {
            for (final JTextComponent component : components) {
                if (null != component) {
                    attachToComponent(component);
                }
            }
        }
    }

    private static void attachToComponent(final JTextComponent component) {
        final JPopupMenu menu = new JPopupMenu();
        final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        final JMenuItem cutItem = new JMenuItem(CUT_LABEL);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, mask));
        cutItem.addActionListener(_ -> component.cut());

        final JMenuItem copyItem = new JMenuItem(COPY_LABEL);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, mask));
        copyItem.addActionListener(_ -> component.copy());

        final JMenuItem pasteItem = new JMenuItem(PASTE_LABEL);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, mask));
        pasteItem.addActionListener(_ -> component.paste());

        final JMenuItem selectAllItem = new JMenuItem(SELECT_ALL_LABEL);
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, mask));
        selectAllItem.addActionListener(_ -> component.selectAll());

        menu.add(cutItem);
        menu.add(copyItem);
        menu.add(pasteItem);
        menu.addSeparator();
        menu.add(selectAllItem);

        menu.addPopupMenuListener(new TextPopupMenuListener(component, cutItem, copyItem, pasteItem, selectAllItem));

        component.setComponentPopupMenu(menu);
        component.addMouseListener(new PopupTriggerMouseListener(component));
    }

    private record TextPopupMenuListener(JTextComponent component, JMenuItem cutItem, JMenuItem copyItem,
                                         JMenuItem pasteItem, JMenuItem selectAllItem) implements PopupMenuListener {

        @Override
        public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
            component.requestFocusInWindow();
            final boolean hasSelection = component.getSelectionStart() != component.getSelectionEnd();
            final boolean isEditable = component.isEditable() && component.isEnabled();
            cutItem.setEnabled(isEditable && hasSelection);
            copyItem.setEnabled(hasSelection);
            pasteItem.setEnabled(isEditable && isClipboardStringAvailable());
            final String text = component.getText();
            selectAllItem.setEnabled(null != text && !text.isEmpty());
        }

        @Override
        public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(final PopupMenuEvent e) {
        }

        private static boolean isClipboardStringAvailable() {
            boolean available = true;
            try {
                available = Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor);
            } catch (final HeadlessException | IllegalStateException | SecurityException _) {
                // Fall back to enabling Paste when the system clipboard cannot be queried
            }
            return available;
        }
    }

    private static final class PopupTriggerMouseListener extends MouseAdapter {

        private final JTextComponent component;

        private PopupTriggerMouseListener(final JTextComponent component) {
            this.component = component;
        }

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
                if (0 <= pos) {
                    final int selStart = component.getSelectionStart();
                    final int selEnd = component.getSelectionEnd();
                    if (selStart == selEnd || pos < selStart || pos > selEnd) {
                        component.setCaretPosition(pos);
                    }
                }
            }
        }
    }
}
