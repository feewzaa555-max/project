package gui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyListener;
import java.util.List;

/**
 * Component สำหรับช่องกรอกข้อมูลในหน้าฟอร์ม (Form Input Field)
 * รวม Label หัวข้อ, ช่องกรอกข้อมูล (JTextField / JPasswordField) และ Label แสดง Error สีแดงไว้ด้วยกัน
 */
public class AuthInputField extends JPanel {

    private final JLabel label;
    private final JTextField inputField;
    private final JLabel errorLabel;

    public AuthInputField(String labelText, boolean isPassword) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);

        Font fontLabel = new Font("Tahoma", Font.BOLD, 12);
        Font fontInput = new Font("Tahoma", Font.PLAIN, 13);
        Font fontError = new Font("Tahoma", Font.PLAIN, 11);

        // --- Label หัวข้อ ---
        label = new JLabel(labelText);
        label.setFont(fontLabel);
        label.setForeground(new Color(55, 65, 81));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Input Field ---
        inputField = isPassword ? new JPasswordField() : new JTextField();
        inputField.setFont(fontInput);
        inputField.setPreferredSize(new Dimension(320, 36));
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        inputField.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputField.setBorder(new CompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        // --- Error Label ---
        errorLabel = new JLabel("");
        errorLabel.setFont(fontError);
        errorLabel.setForeground(new Color(220, 38, 38));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        errorLabel.setHorizontalAlignment(SwingConstants.LEFT);
        errorLabel.setVisible(false);

        // จัดวางตามลำดับ
        add(label);
        add(Box.createRigidArea(new Dimension(0, 6)));
        add(inputField);
        add(Box.createRigidArea(new Dimension(0, 3)));
        add(errorLabel);
    }

    /**
     * ดึงข้อความจากช่องกรอกข้อมูล
     */
    public String getText() {
        if (inputField instanceof JPasswordField passwordField) {
            return new String(passwordField.getPassword());
        }
        return inputField.getText();
    }

    /**
     * กำหนดข้อความในช่องกรอก
     */
    public void setText(String text) {
        inputField.setText(text);
    }

    /**
     * ล้างข้อความในช่องกรอก
     */
    public void clear() {
        inputField.setText("");
    }

    /**
     * แสดงข้อความ Error สีแดงใต้ช่องกรอก
     */
    public void setErrors(List<String> errors) {
        if (errors != null && !errors.isEmpty()) {
            errorLabel.setText("<html>• " + String.join("<br>• ", errors) + "</html>");
            errorLabel.setVisible(true);
        } else {
            clearError();
        }
        revalidate();
        repaint();
    }

    /**
     * ล้างข้อความ Error
     */
    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        revalidate();
        repaint();
    }

    /**
     * ตรวจสอบว่าช่องนี้กำลังแสดง Error อยู่หรือไม่
     */
    public boolean hasError() {
        return errorLabel.isVisible();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void addKeyListener(KeyListener l) {
        inputField.addKeyListener(l);
    }

    @Override
    public boolean requestFocusInWindow() {
        return inputField.requestFocusInWindow();
    }

    public JTextField getInputField() {
        return inputField;
    }

    public JLabel getLabel() {
        return label;
    }
}
