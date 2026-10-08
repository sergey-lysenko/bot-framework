package works.lysenko.base.parameters;

import works.lysenko.util.func.core.Clipboard;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ToolTipManager;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Utility class for building GridBagLayout panels in the parameter GUI.
 */
public final class GuiLayoutBuilder {

    static {
        try {
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        } catch (final Exception ignored) {
        }
    }

    private GuiLayoutBuilder() {
    }

    /**
     * Adds a labeled row to a GridBagLayout container with automatic tooltip help text
     * and a click-to-copy parameter name handler.
     *
     * @param label     the label for the component
     * @param comp      the component to add
     * @param container the container to add the label and component to
     * @param row       the grid row index
     */
    public static void addRow(final String label, final Component comp, final JPanel container, final int row) {

        final JLabel l = new JLabel(label);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                Clipboard.writeClipboard(label);
                if (comp instanceof JCheckBox cb) {
                    cb.setSelected(!cb.isSelected());
                }
            }
        });

        final String help = PropertyHelp.findHelp(label);
        if (isNotNull(help) && !help.isBlank()) {
            final String toolTip = "<html><body style='width: 320px; padding: 2px;'>" + help + "</body></html>";
            l.setToolTipText(toolTip);
            if (comp instanceof JComponent jComp) {
                jComp.setToolTipText(toolTip);
            }
        }

        final GridBagConstraints cLabel = new GridBagConstraints();
        cLabel.gridx = 0;
        cLabel.gridy = row;
        cLabel.anchor = GridBagConstraints.LINE_START;
        cLabel.insets = new Insets(3, 4, 3, 12);
        container.add(l, cLabel);

        final GridBagConstraints cComp = new GridBagConstraints();
        cComp.gridx = 1;
        cComp.gridy = row;
        cComp.weightx = 1.0;
        cComp.anchor = GridBagConstraints.LINE_START;
        if (comp instanceof JButton) {
            cComp.fill = GridBagConstraints.NONE;
        } else {
            cComp.fill = GridBagConstraints.HORIZONTAL;
        }
        cComp.insets = new Insets(3, 0, 3, 4);
        container.add(comp, cComp);
    }

    /**
     * Adds a labeled row to a GridBagLayout container if the component is non-null.
     *
     * @param label     the label for the component
     * @param comp      the component to add, or null
     * @param container the container to add the label and component to
     * @param row       the current grid row index
     * @return the updated grid row index (incremented if component was added)
     */
    public static int addRowOptional(final String label, final Component comp, final JPanel container, final int row) {

        if (isNotNull(comp)) {
            addRow(label, comp, container, row);
            return row + 1;
        }
        return row;
    }

    /**
     * Adds a horizontal separator divider spanning across the GridBagLayout columns.
     *
     * @param container the container panel
     * @param row       the current grid row index
     * @return the updated grid row index (row + 1)
     */
    public static int addDivider(final JPanel container, final int row) {

        final JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 4);
        container.add(separator, c);
        return row + 1;
    }
}
