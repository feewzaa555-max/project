package gui.MainFrame;

import model.Account;
import model.User;
import service.AccountService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * หน้าต่างแสดงสถานะและการสมัคร/ต่ออายุสมาชิก KU CINEMA
 */
public class MembershipDialog extends JDialog {

    /**
     * คอนสตรัคเตอร์สร้างหน้าต่างแสดงสถานะและการสมัคร/ต่ออายุสมาชิก
     * ตรวจสอบสถานะการเป็นสมาชิก ยอดเงินคงเหลือ สิทธิประโยชน์ส่วนลด 10%
     * และมีปุ่มสำหรับสมัคร/ต่ออายุ หรือแนะนำให้เติมเงินหากยอดเงินไม่เพียงพอ
     *
     * @param owner หน้าต่างหลักที่เป็นเจ้าของ Dialog
     * @param currentUser ผู้ใช้ปัจจุบัน
     * @param accountService เซอร์วิสจัดการบัญชีและสมาชิก
     * @param onAccountUpdated Callback อัปเดตข้อมูลบนหน้าต่างหลักหลังสมัคร/ต่ออายุสำเร็จ
     * @param onOpenTopUp Callback เปิดหน้าต่างเติมเงินหากเงินไม่พอ
     */
    public MembershipDialog(Frame owner,
                            User currentUser,
                            AccountService accountService,
                            Runnable onAccountUpdated,
                            Runnable onOpenTopUp) {
        super(owner, true);

        if (accountService == null) {
            JOptionPane.showMessageDialog(owner, "AccountService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Account account = accountService.accountOf(currentUser);
            boolean isMember = accountService.isMember(currentUser);

            setTitle(isMember ? "KU CINEMA Membership Status" : "KU CINEMA Membership Subscription");
            setSize(400, 300);
            setLocationRelativeTo(owner);
            setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel title = new JLabel(isMember ? "KU CINEMA Membership Status" : "KU CINEMA Membership Subscription");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(MainFrame.COLOR_TEXT_TITLE);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel benefit = new JLabel("Benefits: 10% discount on all seats and showtimes");
            benefit.setFont(MainFrame.FONT_SUBTITLE);
            benefit.setForeground(MainFrame.COLOR_PRIMARY);
            benefit.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel feeLabel = new JLabel("Fee: " + AccountService.MEMBERSHIP_PRICE + " THB / 30 Days");
            feeLabel.setFont(MainFrame.FONT_TEXT);
            feeLabel.setForeground(MainFrame.COLOR_TEXT_LABEL);
            feeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            String statusStr = isMember && account.memberUntil() != null
                    ? "Status: Member (Expires " + account.memberUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")"
                    : "Status: Non-member";
            JLabel statusLabel = new JLabel(statusStr);
            statusLabel.setFont(MainFrame.FONT_TEXT);
            statusLabel.setForeground(isMember ? new Color(16, 185, 129) : MainFrame.COLOR_TEXT_MUTED);
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel currentBalanceLabel = new JLabel("Wallet Balance: " + account.balance() + " THB");
            currentBalanceLabel.setFont(MainFrame.FONT_LABEL);
            currentBalanceLabel.setForeground(MainFrame.COLOR_TEXT_TITLE);
            currentBalanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(title);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(benefit);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
            panel.add(feeLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(statusLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
            panel.add(currentBalanceLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 18)));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnPanel.setOpaque(false);

            JButton actionBtn = new JButton(isMember ? "Renew Membership (99 THB)" : "Confirm Subscription (99 THB)");
            actionBtn.setFont(MainFrame.FONT_LABEL);
            actionBtn.setBackground(MainFrame.COLOR_PRIMARY);
            actionBtn.setForeground(Color.BLACK);
            actionBtn.setFocusPainted(false);
            actionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            actionBtn.setBorder(new CompoundBorder(
                    new LineBorder(MainFrame.COLOR_PRIMARY, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));

            actionBtn.addActionListener(e -> {
                if (account.balance() < AccountService.MEMBERSHIP_PRICE) {
                    int opt = JOptionPane.showConfirmDialog(
                            this,
                            "Insufficient balance (Requires " + AccountService.MEMBERSHIP_PRICE + " THB, Available " + account.balance() + " THB)\nWould you like to top up now?",
                            "Insufficient Balance",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION) {
                        dispose();
                        if (onOpenTopUp != null) onOpenTopUp.run();
                    }
                    return;
                }

                try {
                    Account updated = accountService.subscribe(currentUser);
                    dispose();
                    if (onAccountUpdated != null) onAccountUpdated.run();
                    String untilStr = updated.memberUntil() != null
                            ? updated.memberUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "";
                    JOptionPane.showMessageDialog(
                            owner,
                            (isMember ? "Membership renewed successfully!" : "Membership subscribed successfully!") + "\nMember until: " + untilStr + "\nRemaining balance: " + updated.balance() + " THB",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton closeBtn = new JButton("Close");
            closeBtn.setFont(MainFrame.FONT_LABEL);
            closeBtn.setBackground(Color.WHITE);
            closeBtn.setForeground(MainFrame.COLOR_TEXT_LABEL);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setBorder(new CompoundBorder(
                    new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));
            closeBtn.addActionListener(e -> dispose());

            btnPanel.add(actionBtn);
            btnPanel.add(closeBtn);
            panel.add(btnPanel);

            setContentPane(panel);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Cannot retrieve membership info: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
