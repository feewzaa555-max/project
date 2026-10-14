package gui.MainFrame;

import model.Account;
import model.User;
import service.AccountService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * หน้าต่างเติมเงินในกระเป๋า (Wallet Top-up)
 */
public class TopUpDialog extends JDialog {

    /**
     * คอนสตรัคเตอร์สร้างหน้าต่างเติมเงินเข้ากระเป๋าเงิน (Wallet Top-up)
     * มีปุ่มลัดเลือกจำนวนเงินด่วน (+100, +200, +300, +500, +1000) และช่องป้อนจำนวนเงินที่ต้องการ (1 - 5,000 บาท)
     *
     * @param owner หน้าต่างหลักที่เป็นเจ้าของ Dialog
     * @param currentUser ผู้ใช้ปัจจุบัน
     * @param accountService เซอร์วิสจัดการบัญชีและเติมเงิน
     * @param onAccountUpdated Callback อัปเดตข้อมูลบนหน้าต่างหลักหลังเติมเงินสำเร็จ
     */
    public TopUpDialog(Frame owner,
                       User currentUser,
                       AccountService accountService,
                       Runnable onAccountUpdated) {
        super(owner, "Wallet Top-up", true);

        if (accountService == null) {
            JOptionPane.showMessageDialog(owner, "AccountService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Account account = accountService.accountOf(currentUser);

            setSize(420, 320);
            setLocationRelativeTo(owner);
            setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel title = new JLabel("Wallet Top-up");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(MainFrame.COLOR_TEXT_TITLE);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel curBalance = new JLabel("Current Balance: " + account.balance() + " THB");
            curBalance.setFont(MainFrame.FONT_SUBTITLE);
            curBalance.setForeground(MainFrame.COLOR_TEXT_MUTED);
            curBalance.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel promptLabel = new JLabel("Select or enter amount (1 - 5,000 THB):");
            promptLabel.setFont(MainFrame.FONT_LABEL);
            promptLabel.setForeground(MainFrame.COLOR_TEXT_LABEL);
            promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // ปุ่มลัดเลือกจำนวนเงิน
            JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            quickPanel.setOpaque(false);
            int[] quickAmounts = {100, 200, 300, 500, 1000};
            JTextField amountField = new JTextField("100", 12);
            amountField.setFont(new Font("Tahoma", Font.PLAIN, 14));
            amountField.setHorizontalAlignment(JTextField.CENTER);
            amountField.setMaximumSize(new Dimension(200, 32));

            for (int amt : quickAmounts) {
                JButton qBtn = new JButton("+" + amt);
                qBtn.setFont(MainFrame.FONT_SMALL);
                qBtn.setBackground(new Color(243, 244, 246));
                qBtn.setForeground(MainFrame.COLOR_TEXT_LABEL);
                qBtn.setFocusPainted(false);
                qBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                qBtn.setBorder(new CompoundBorder(
                        new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                        new EmptyBorder(4, 8, 4, 8)));
                qBtn.addActionListener(e -> amountField.setText(String.valueOf(amt)));
                quickPanel.add(qBtn);
            }

            panel.add(title);
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(curBalance);
            panel.add(Box.createRigidArea(new Dimension(0, 14)));
            panel.add(promptLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(quickPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(amountField);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnPanel.setOpaque(false);

            JButton confirmBtn = new JButton("Confirm Top-up");
            confirmBtn.setFont(MainFrame.FONT_LABEL);
            confirmBtn.setBackground(new Color(16, 185, 129)); // Emerald Green
            confirmBtn.setForeground(Color.BLACK);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            confirmBtn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(16, 185, 129), 1, true),
                    new EmptyBorder(7, 16, 7, 16)));

            confirmBtn.addActionListener(e -> {
                String text = amountField.getText().trim();
                int amt;
                try {
                    amt = Integer.parseInt(text);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid integer amount", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (amt < 1 || amt > 5000) {
                    JOptionPane.showMessageDialog(this, "Top-up amount must be between 1 and 5,000 THB", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Account updated = accountService.topUp(currentUser, amt);
                    dispose();
                    if (onAccountUpdated != null) onAccountUpdated.run();
                    JOptionPane.showMessageDialog(
                            owner,
                            "Top-up successful: " + amt + " THB\nCurrent Balance: " + updated.balance() + " THB",
                            "Top-up Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error during top-up: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.setFont(MainFrame.FONT_LABEL);
            cancelBtn.setBackground(Color.WHITE);
            cancelBtn.setForeground(MainFrame.COLOR_TEXT_LABEL);
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.setBorder(new CompoundBorder(
                    new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));
            cancelBtn.addActionListener(e -> dispose());

            btnPanel.add(confirmBtn);
            btnPanel.add(cancelBtn);
            panel.add(btnPanel);

            setContentPane(panel);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Cannot retrieve account data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
