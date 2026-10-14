package gui;

import model.Account;
import model.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * แถบด้านบน (Top Bar) ของหน้าต่างหลัก
 * แสดงแบรนด์ KU CINEMA ทางซ้าย และเมนูผู้ใช้ทางขวา:
 * 1. ชื่อผู้ใช้
 * 2. ปุ่มสมัครสมาชิก
 * 3. เงินคงเหลือ
 * 4. ปุ่มเติมเงิน
 * 5. ประวัติการจอง
 * 6. ปุ่ม logout
 */
public class MainTopBar extends JPanel {

    private final JLabel usernameLabel;
    private final JButton membershipButton;
    private final JLabel balanceLabel;
    private final JButton topUpButton;
    private final JButton historyButton;
    private final JButton logoutButton;

    public MainTopBar(User user,
                      Runnable onMembership,
                      Runnable onTopUp,
                      Runnable onHistory,
                      Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, MainFrame.COLOR_CARD_BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ซ้ายสุด: KU CINEMA
        JLabel brandLabel = new JLabel("KU CINEMA");
        brandLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        brandLabel.setForeground(MainFrame.COLOR_PRIMARY);

        // ด้านขวา: จัดเรียงตามลำดับ ชิดขวา
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // 1. ชื่อผู้ใช้
        usernameLabel = new JLabel("User: " + (user != null ? user.username() : ""));
        usernameLabel.setFont(MainFrame.FONT_LABEL);
        usernameLabel.setForeground(MainFrame.COLOR_TEXT_TITLE);
        usernameLabel.setBorder(new EmptyBorder(5, 2, 5, 2));

        // 2. ปุ่มสมัครสมาชิก
        membershipButton = new JButton("Membership");
        membershipButton.setFont(MainFrame.FONT_LABEL);
        membershipButton.setFocusPainted(false);
        membershipButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        membershipButton.addActionListener(e -> {
            if (onMembership != null) onMembership.run();
        });

        // 3. เงินคงเหลือ
        balanceLabel = new JLabel("Balance: 0 THB");
        balanceLabel.setFont(MainFrame.FONT_LABEL);
        balanceLabel.setForeground(new Color(5, 150, 105)); // Green #059669
        balanceLabel.setBorder(new EmptyBorder(5, 2, 5, 2));

        // 4. ปุ่มเติมเงิน
        topUpButton = new JButton("Top-up");
        topUpButton.setFont(MainFrame.FONT_LABEL);
        topUpButton.setBackground(new Color(16, 185, 129)); // Emerald Green #10B981
        topUpButton.setForeground(Color.BLACK);
        topUpButton.setFocusPainted(false);
        topUpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topUpButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(16, 185, 129), 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        topUpButton.addActionListener(e -> {
            if (onTopUp != null) onTopUp.run();
        });

        // 5. ประวัติ
        historyButton = new JButton("History");
        historyButton.setFont(MainFrame.FONT_LABEL);
        historyButton.setBackground(Color.WHITE);
        historyButton.setForeground(MainFrame.COLOR_TEXT_LABEL);
        historyButton.setFocusPainted(false);
        historyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        historyButton.setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        historyButton.addActionListener(e -> {
            if (onHistory != null) onHistory.run();
        });

        // 6. ปุ่ม logout
        logoutButton = new JButton("Logout");
        logoutButton.setFont(MainFrame.FONT_LABEL);
        logoutButton.setBackground(new Color(254, 242, 242));
        logoutButton.setForeground(new Color(220, 38, 38)); // Red #DC2626
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(252, 165, 165), 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        logoutButton.addActionListener(e -> {
            if (onLogout != null) onLogout.run();
        });

        // เพิ่มเข้า rightPanel เรียงตามลำดับที่กำหนด
        rightPanel.add(usernameLabel);
        rightPanel.add(membershipButton);
        rightPanel.add(balanceLabel);
        rightPanel.add(topUpButton);
        rightPanel.add(historyButton);
        rightPanel.add(logoutButton);

        add(brandLabel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * อัปเดตข้อมูลแถบด้านบน (ยอดเงินคงเหลือ และสถานะปุ่มสมาชิก)
     */
    public void updateAccount(Account account, boolean isMember) {
        if (account == null) {
            balanceLabel.setText("Balance: -");
            membershipButton.setText("Membership");
            return;
        }

        balanceLabel.setText("Balance: " + account.balance() + " THB");

        if (isMember && account.memberUntil() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            membershipButton.setText("Member (until " + account.memberUntil().format(formatter) + ")");
            membershipButton.setBackground(new Color(254, 243, 199)); // Amber light
            membershipButton.setForeground(new Color(180, 83, 9));    // Amber dark
            membershipButton.setBorder(new CompoundBorder(
                    new LineBorder(new Color(245, 158, 11), 1, true),
                    new EmptyBorder(5, 12, 5, 12)));
        } else {
            membershipButton.setText("Membership");
            membershipButton.setBackground(new Color(238, 242, 255)); // Indigo light
            membershipButton.setForeground(MainFrame.COLOR_PRIMARY);
            membershipButton.setBorder(new CompoundBorder(
                    new LineBorder(new Color(199, 210, 254), 1, true),
                    new EmptyBorder(5, 12, 5, 12)));
        }
    }
}
