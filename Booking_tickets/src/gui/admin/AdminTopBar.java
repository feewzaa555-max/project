//AdminTopBar <- แถบด้านบนของหน้า admin แสดงโลโก้ KU CINEMA, ชื่อผู้ดูแล และปุ่ม Logout
/*
    หน้าที่:
        1. แสดงโลโก้แบรนด์ KU CINEMA ด้านซ้ายมือ
        2. แสดงป้ายชื่อ Admin ปัจจุบัน ด้านขวามือ
        3. มีปุ่ม Logout สีแดงอ่อน เมื่อกดแล้วจะเรียก callback ให้กลับไปหน้า Login

    ใช้ร่วมกับ:
        AdminTheme สำหรับชุดสีและฟอนต์มาตรฐาน
        AdminFrame เป็นคนสร้างแถบนี้แล้วส่ง listener สำหรับ Logout มาให้
 */

package gui.admin;

import model.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * แถบด้านบนของหน้าจอ Admin
 * ออกแบบให้เข้ากับแถบบนของ MainFrame โดยใช้ชุดสีและฟอนต์จาก AdminTheme
 */
final class AdminTopBar extends JPanel {

    AdminTopBar(User admin, Runnable onLogout) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, AdminTheme.BORDER),
                new EmptyBorder(10, 24, 10, 24)));

        // ด้านซ้าย: โลโก้แบรนด์ KU CINEMA
        JLabel brandLabel = new JLabel("KU CINEMA");
        brandLabel.setFont(AdminTheme.FONT_BRAND);
        brandLabel.setForeground(AdminTheme.PRIMARY);
        add(brandLabel, BorderLayout.WEST);

        // ด้านขวา: ข้อมูล Admin และปุ่ม Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        String username = (admin != null) ? admin.username() : "admin";
        JLabel adminLabel = new JLabel("Admin: " + username);
        adminLabel.setFont(AdminTheme.FONT_LABEL);
        adminLabel.setForeground(AdminTheme.TEXT_DARK);
        adminLabel.setBorder(new EmptyBorder(5, 2, 5, 2));

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(AdminTheme.FONT_LABEL);
        logoutButton.setBackground(AdminTheme.LOGOUT_BACKGROUND);
        logoutButton.setForeground(AdminTheme.LOGOUT_TEXT);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.LOGOUT_BORDER, 1, true),
                new EmptyBorder(5, 14, 5, 14)));
        logoutButton.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });

        rightPanel.add(adminLabel);
        rightPanel.add(logoutButton);
        add(rightPanel, BorderLayout.EAST);
    }
}
