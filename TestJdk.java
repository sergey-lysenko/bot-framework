import javax.swing.*;
public class TestJdk {
    public static void main(String[] args) {
        JComboBox box = new JComboBox();
        DefaultCellEditor ed = new DefaultCellEditor(box);
        System.out.println(box.getClientProperty("JComboBox.isTableCellEditor"));
    }
}
