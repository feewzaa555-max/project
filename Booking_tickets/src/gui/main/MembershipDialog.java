//MembershipDialog <- กล่องสมัคร / ต่ออายุสมาชิก ที่เด้งขึ้นมาตอนกดปุ่ม Membership ในแถบบน
/*
    หน้าตา (ยังไม่เป็นสมาชิก):
        ┌──────── KU CINEMA Membership Subscription ────────┐
        │   Benefits: 10% discount on all seats and showtimes │  ① ข้อความ 5 บรรทัด
        │   Fee: 99 THB / 30 Days                              │
        │   Status: Non-member                                 │
        │   Wallet Balance: 0 THB                              │
        │   [Confirm Subscription (99 THB)]  [Close]           │  ② ปุ่ม
        └──────────────────────────────────────────────────────┘
    เป็นสมาชิกแล้ว: หัวข้อ "KU CINEMA Membership Status", Status: Member (Expires 13/11/2026 10:00) สีเขียว,
                   ปุ่ม "Renew Membership (99 THB)" (ต่ออายุ +30 วัน ต่อจากวันหมดเดิม)

    กดปุ่มยืนยัน (③ confirm):
        เงินไม่ถึง 99 → ถาม "top up now?" → Yes = ปิดกล่องนี้ แล้วสั่ง onTopUp.run() ให้ MainFrame เปิดกล่องเติมเงิน
        เงินพอ        → accounts.subscribe(user) ตัดเงิน + ต่อวันสมาชิก บันทึกไฟล์
                        → ปิดกล่อง → onDone.run() ให้ MainFrame อัปเดตแถบบน → ข้อความสำเร็จ

    กล่องนี้ไม่รู้จัก MainFrame: อยากให้ MainFrame ทำอะไร บอกผ่าน onDone / onTopUp ที่ส่งเข้ามา

    ใช้ยังไง (ใน MainFrame):
        new MembershipDialog(this, app.accounts(), user, () -> refreshTopBar(), () -> openTopUp()).setVisible(true);
        // อ่านบัญชีไม่ได้ → constructor throw IOException → MainFrame โชว์ error

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 430–556 (showMembershipDialog)
    ต่างจากเดิม:
        "99 THB" / "30 Days" เดิมเขียนตายตัว → ใหม่อ่านจาก AccountService.MEMBERSHIP_PRICE / Account.MEMBERSHIP_DAYS
        เดิม catch (Exception) 2 ที่ → ใหม่จับ IOException กับ IllegalArgumentException แยกกัน (SC2 หน้า 11)
        เดิม setSize(400, 300) → ใหม่ pack() กล่องขยายพอดีข้อความเอง
        เดิมเรียก showTopUpDialog() / refreshAccountBar() ตรง ๆ → ใหม่ผ่าน onTopUp / onDone

    คำที่ใช้บ่อยในไฟล์นี้:
        JDialog(เจ้าของ, หัวกล่อง, true) = กล่องที่เด้งทับหน้าต่างเจ้าของ / true = modal (ต้องปิดกล่องก่อนถึงกดหน้าหลังได้)
        pack()    = ให้กล่องมีขนาดพอดีของข้างใน
        dispose() = ปิดกล่องและคืนหน่วยความจำ
 */

package gui.main;

import java.awt.*;                          // Component, Dimension, FlowLayout, Cursor, Color
import java.io.IOException;                 // error ตอนอ่าน/เขียน accounts.csv ไม่ได้
import java.time.format.DateTimeFormatter;  // แปลงวันหมดสมาชิกเป็นข้อความ "13/11/2026 10:00"
import javax.swing.*;                       // JDialog = กล่องเด้ง, JPanel, JLabel, JButton, JOptionPane = กล่องข้อความ, Box, BoxLayout
import javax.swing.border.*;                // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Account;                       // บัญชีลูกค้า (ยอดเงิน, วันหมดสมาชิก) + MEMBERSHIP_DAYS
import model.User;                          // ลูกค้าที่ login อยู่
import service.AccountService;              // อ่านบัญชี, เช็กสมาชิก, สมัคร (subscribe) + MEMBERSHIP_PRICE

final class MembershipDialog extends JDialog {

    private static final DateTimeFormatter EXPIRE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");  // 13/11/2026 10:00

    private final AccountService accounts;   // ใช้สมัครสมาชิกตอนกดยืนยัน
    private final User user;                 // ลูกค้าคนนี้
    private final Account account;           // บัญชีตอนเปิดกล่อง (ยอดเงินใช้เช็กว่าพอ 99 ไหม)
    private final boolean member;            // ตอนเปิดกล่องเป็นสมาชิกอยู่ไหม (เลือกข้อความสมัคร / ต่ออายุ)
    private final Runnable onDone;           // สิ่งที่ MainFrame ให้ทำหลังสมัครสำเร็จ เช่น () -> refreshTopBar()
    private final Runnable onTopUp;          // สิ่งที่ MainFrame ให้ทำเมื่อเงินไม่พอแล้วลูกค้าอยากเติม เช่น () -> openTopUp()

    /**
     * สร้างกล่องสมัครสมาชิก (ยังไม่โชว์ เรียก setVisible(true) เอง)
     *
     * @param owner    หน้าต่างที่กล่องนี้เด้งทับ (MainFrame) / null ได้ = กลางจอ
     * @param accounts AccountService ของโปรแกรม เช่น app.accounts()
     * @param user     ลูกค้าที่ login อยู่
     * @param onDone   คำสั่ง run method นั้น: สิ่งที่ทำหลังสมัครสำเร็จ เช่น () -> refreshTopBar()
     * @param onTopUp  คำสั่ง run method นั้น: สิ่งที่ทำเมื่อเงินไม่พอและลูกค้ากด Yes เช่น () -> openTopUp()
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง .run()
     * @throws IOException ถ้าอ่าน accounts.csv ไม่ได้ (MainFrame เอา getMessage() ไปโชว์)
     * @throws IllegalArgumentException ถ้า accounts, user, onDone หรือ onTopUp เป็น null
     */
    MembershipDialog(JFrame owner, AccountService accounts, User user, Runnable onDone, Runnable onTopUp) throws IOException {
        // super(เจ้าของ, หัวกล่อง, modal) / หัวกล่องจริงใส่ทีหลังด้วย setTitle เพราะต้องรู้ก่อนว่าเป็นสมาชิกไหม
        super(owner, true);

        // ① ตรวจของที่ส่งเข้ามา (SC2 หน้า 9)
        if (accounts == null || user == null) {
            throw new IllegalArgumentException("accounts and user must not be null");
        }
        if (onDone == null || onTopUp == null) {
            throw new IllegalArgumentException("onDone and onTopUp must not be null");
        }
        this.accounts = accounts;
        this.user = user;
        this.onDone = onDone;
        this.onTopUp = onTopUp;

        // ② อ่านบัญชี (อ่านไม่ได้ → IOException ออกไปให้ MainFrame)
        //    accountOf(user) = บัญชีของคนนี้ เช่น Account[tester9, 0, null] (AccountService.java)
        //    isMember(user)  = ตอนนี้เป็นสมาชิกไหม (ใช้เวลาของนาฬิกาโปรแกรม)
        this.account = accounts.accountOf(user);
        this.member = accounts.isMember(user);

        // ③ หัวกล่อง + เนื้อหา
        String title = member ? "KU CINEMA Membership Status" : "KU CINEMA Membership Subscription";
        setTitle(title);
        setResizable(false);   // ห้ามลากยืด

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));   // เรียงบนลงล่าง
        panel.setBackground(MainTheme.CARD);                       // พื้นขาว
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));          // เว้นขอบ บน 20 ซ้าย 24 ล่าง 20 ขวา 24

        //    ข้อความ 5 บรรทัด: line(ข้อความ, ฟอนต์, สี) สร้างป้ายจัดกลาง
        panel.add(line(title, MainTheme.FONT_DIALOG_TITLE, MainTheme.TEXT_TITLE));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(line("Benefits: 10% discount on all seats and showtimes", MainTheme.FONT_SUBTITLE, MainTheme.PRIMARY));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        //    ราคาและจำนวนวันอ่านจากค่ากฎจริง เปลี่ยนกฎที่ service / model → ข้อความเปลี่ยนตาม
        panel.add(line("Fee: " + AccountService.MEMBERSHIP_PRICE + " THB / " + Account.MEMBERSHIP_DAYS + " Days",
                MainTheme.FONT_TEXT, MainTheme.TEXT_LABEL));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(member
                ? line("Status: Member (Expires " + account.memberUntil().format(EXPIRE_FORMAT) + ")", MainTheme.FONT_TEXT, MainTheme.SUCCESS)
                : line("Status: Non-member", MainTheme.FONT_TEXT, MainTheme.TEXT_MUTED));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(line("Wallet Balance: " + account.balance() + " THB", MainTheme.FONT_LABEL, MainTheme.TEXT_TITLE));
        panel.add(Box.createRigidArea(new Dimension(0, 18)));

        //    ปุ่ม 2 ปุ่ม: ยืนยัน (ฟ้า ตัวดำ) / Close (ขาว กรอบเทา)
        String action = (member ? "Renew Membership (" : "Confirm Subscription (") + AccountService.MEMBERSHIP_PRICE + " THB)";
        JButton confirm = button(action, MainTheme.PRIMARY, MainTheme.BUTTON_TEXT, MainTheme.PRIMARY);
        confirm.addActionListener(e -> confirm());           // กด → ④ confirm()
        JButton close = button("Close", MainTheme.CARD, MainTheme.TEXT_LABEL, MainTheme.INPUT_BORDER);
        close.addActionListener(e -> dispose());              // กด → ปิดกล่อง ไม่มีอะไรเปลี่ยน

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));   // เรียงปุ่มกลาง ห่างกัน 10 px
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(confirm);
        buttons.add(close);
        panel.add(buttons);

        setContentPane(panel);
        pack();                          // ขนาดพอดีของข้างใน
        setLocationRelativeTo(owner);    // เปิดกลางหน้าต่างเจ้าของ
    }

    // ④ กดปุ่มยืนยัน
    private void confirm() {
        // เงินไม่ถึงค่าสมาชิก → ถามว่าจะเติมเงินไหม (เช็กก่อน ไม่ต้องรอให้ subscribe throw)
        if (account.balance() < AccountService.MEMBERSHIP_PRICE) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Insufficient balance (Requires " + AccountService.MEMBERSHIP_PRICE + " THB, Available "
                            + account.balance() + " THB)\nWould you like to top up now?",
                    "Insufficient Balance", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.YES_OPTION) {   // Yes → ปิดกล่องนี้ แล้วให้ MainFrame เปิดกล่องเติมเงิน
                dispose();
                onTopUp.run();
            }
            return;                                   // No / ปิดกล่องถาม → อยู่กล่องเดิม
        }
        try {
            // subscribe(user) = ตัดเงิน 99 + ต่อวันสมาชิก 30 วัน แล้วบันทึก accounts.csv คืนบัญชีใหม่ (AccountService.java บรรทัด 220)
            Account updated = accounts.subscribe(user);
            dispose();         // ปิดกล่องก่อน
            onDone.run();      // ให้ MainFrame อัปเดตแถบบน (ยอดเงินลด / ปุ่มเป็น Member)
            JOptionPane.showMessageDialog(getOwner(),
                    (member ? "Membership renewed successfully!" : "Membership subscribed successfully!")
                            + "\nMember until: " + updated.memberUntil().format(EXPIRE_FORMAT)
                            + "\nRemaining balance: " + updated.balance() + " THB",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            // ผิดกฎ เช่นเงินไม่พอ (บัญชีถูกแก้ระหว่างเปิดกล่อง) → บอกข้อความของ service ตรง ๆ กล่องยังเปิดอยู่
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot subscribe", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            // อ่าน/เขียนไฟล์ไม่ได้ → ไม่ได้ตัดเงิน
            JOptionPane.showMessageDialog(this, "Cannot save membership: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ป้ายข้อความ 1 บรรทัด จัดกลาง
    //   text = ข้อความ / font = ฟอนต์ / color = สีตัวหนังสือ
    private static JLabel line(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);   // อยู่กลางกล่อง (กล่องเรียงแนวตั้งด้วย BoxLayout)
        return label;
    }

    // ปุ่มของกล่อง: พื้น / ตัวหนังสือ / กรอบ
    //   ขอบ LineBorder(สี, 1, true) = เส้นรอบมุมโค้ง + EmptyBorder(7, 16, 7, 16) = ที่ว่างระหว่างเส้นกับคำ
    private static JButton button(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text);
        button.setFont(MainTheme.FONT_LABEL);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);                       // กดแล้วไม่วาดกรอบประ
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));    // ชี้แล้วเป็นรูปมือ
        button.setBorder(new CompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(7, 16, 7, 16)));
        return button;
    }
}