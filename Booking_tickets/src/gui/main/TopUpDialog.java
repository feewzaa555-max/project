//TopUpDialog <- กล่องเติมเงินที่เด้งขึ้นมาตอนกดปุ่ม Top-up ในแถบบน
/*
    หน้าตา:
        ┌──────────────── Wallet Top-up ────────────────┐
        │            Current Balance: 0 THB               │
        │     Select or enter amount (1 - 5,000 THB):     │
        │   [+100] [+200] [+300] [+500] [+1000]           │  ← ปุ่มลัด: ใส่ตัวเลขนั้นลงช่อง (ไม่ได้บวกเพิ่ม)
        │               [     100     ]                    │  ← ช่องพิมพ์จำนวนเงิน
        │        [Confirm Top-up]   [Cancel]               │
        └─────────────────────────────────────────────────┘

    กด Confirm / Enter ในช่องพิมพ์ (confirm()):
        พิมพ์ไม่ใช่ตัวเลข (abc, 12.5)  → "Please enter a valid integer amount"
        นอกช่วง (0, 6000)              → Account ตรวจแล้ว throw → "Top-up amount must be between 1 and 5,000 THB"
        สำเร็จ                          → accounts.topUp บันทึกไฟล์ → ปิดกล่อง → onDone.run() → "Top-up successful ..."

    กฎช่วงเงิน (1 – 5,000) อยู่ที่ Account ที่เดียว กล่องนี้ไม่ตรวจเอง
        ตัวเลขในข้อความอ่านจาก Account.MIN_TOP_UP / MAX_TOP_UP เปลี่ยนกฎที่ Account → ข้อความเปลี่ยนตาม

    ใช้ยังไง (ใน MainFrame):
        new TopUpDialog(this, app.accounts(), user, () -> refreshTopBar()).setVisible(true);
        // อ่านบัญชีไม่ได้ → constructor throw IOException → MainFrame โชว์ error

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 560–692 (showTopUpDialog)
    ต่างจากเดิม:
        เดิมตรวจ 1–5000 เองซ้ำกับ Account + เขียน "1 - 5,000" ตายตัว 2 ที่ → ใหม่ให้ Account ตรวจ ข้อความอ่านจากค่ากฎ
        เดิม catch (Exception) → ใหม่จับ NumberFormatException / IllegalArgumentException / IOException แยกกัน (SC2 หน้า 11)
        เดิมเรียก refreshAccountBar() ตรง ๆ → ใหม่ผ่าน onDone / เดิม setSize(420, 320) → ใหม่ pack()
        เพิ่ม: กด Enter ในช่องพิมพ์ = กด Confirm

    คำที่ใช้บ่อยในไฟล์นี้:
        JDialog(เจ้าของ, true) = กล่องที่เด้งทับหน้าต่างเจ้าของ / true = modal (ต้องปิดกล่องก่อนถึงกดหน้าหลังได้)
        Integer.parseInt("500") = แปลงข้อความเป็นตัวเลข 500 / ไม่ใช่ตัวเลข → throw NumberFormatException
 */

package gui.main;

import java.awt.*;                 // Component, Dimension, FlowLayout, Cursor, Color, Font
import java.io.IOException;        // error ตอนอ่าน/เขียน accounts.csv ไม่ได้
import java.util.Locale;           // Locale.US = เขียนตัวเลขมีลูกน้ำแบบ 5,000 เสมอ ไม่ขึ้นกับภาษาของเครื่อง
import javax.swing.*;              // JDialog, JPanel, JLabel, JButton, JTextField = ช่องพิมพ์, JOptionPane, Box, BoxLayout
import javax.swing.border.*;       // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Account;              // บัญชีลูกค้า + MIN_TOP_UP / MAX_TOP_UP (กฎช่วงเงิน)
import model.User;                 // ลูกค้าที่ login อยู่
import service.AccountService;     // อ่านบัญชี + เติมเงิน (topUp)

final class TopUpDialog extends JDialog {

    private static final int[] QUICK_AMOUNTS = {100, 200, 300, 500, 1000};   // ปุ่มลัด (เท่าของเดิม)
    private static final String DEFAULT_AMOUNT = "100";                      // ตัวเลขในช่องตอนเปิดกล่อง

    private final AccountService accounts;   // ใช้เติมเงินตอนกด Confirm
    private final User user;                 // ลูกค้าคนนี้
    private final Runnable onDone;           // สิ่งที่ MainFrame ให้ทำหลังเติมสำเร็จ เช่น () -> refreshTopBar()
    private final JTextField amountField;    // ช่องพิมพ์จำนวนเงิน

    /**
     * สร้างกล่องเติมเงิน (ยังไม่โชว์ เรียก setVisible(true) เอง)
     *
     * @param owner    หน้าต่างที่กล่องนี้เด้งทับ (MainFrame) / null ได้ = กลางจอ
     * @param accounts AccountService ของโปรแกรม เช่น app.accounts()
     * @param user     ลูกค้าที่ login อยู่
     * @param onDone   คำสั่ง run method นั้น: สิ่งที่ทำหลังเติมเงินสำเร็จ เช่น () -> refreshTopBar()
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onDone.run()
     * @throws IOException ถ้าอ่าน accounts.csv ไม่ได้ (MainFrame เอา getMessage() ไปโชว์)
     * @throws IllegalArgumentException ถ้า accounts, user หรือ onDone เป็น null
     */
    TopUpDialog(JFrame owner, AccountService accounts, User user, Runnable onDone) throws IOException {
        super(owner, "Wallet Top-up", true);   // super(เจ้าของ, หัวกล่อง, modal)

        // ① ตรวจของที่ส่งเข้ามา (SC2 หน้า 9)
        if (accounts == null || user == null || onDone == null) {
            throw new IllegalArgumentException("accounts, user and onDone must not be null");
        }
        this.accounts = accounts;
        this.user = user;
        this.onDone = onDone;

        // ② อ่านยอดเงินตอนนี้ (อ่านไม่ได้ → IOException ออกไปให้ MainFrame)
        int balance = accounts.accountOf(user).balance();

        // ③ เนื้อหา
        setResizable(false);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));   // เรียงบนลงล่าง
        panel.setBackground(MainTheme.CARD);                       // พื้นขาว
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));          // เว้นขอบ บน 20 ซ้าย 24 ล่าง 20 ขวา 24

        panel.add(line("Wallet Top-up", MainTheme.FONT_DIALOG_TITLE, MainTheme.TEXT_TITLE));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(line("Current Balance: " + balance + " THB", MainTheme.FONT_SUBTITLE, MainTheme.TEXT_MUTED));
        panel.add(Box.createRigidArea(new Dimension(0, 14)));
        panel.add(line("Select or enter amount (" + rangeText() + "):", MainTheme.FONT_LABEL, MainTheme.TEXT_LABEL));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        //    ช่องพิมพ์ต้องสร้างก่อนปุ่มลัด เพราะปุ่มลัดเขียนตัวเลขลงช่องนี้
        //    JTextField(ข้อความเริ่มต้น, ความกว้าง 12 ตัวอักษร)
        amountField = new JTextField(DEFAULT_AMOUNT, 12);
        amountField.setFont(MainTheme.FONT_INPUT);
        amountField.setHorizontalAlignment(JTextField.CENTER);     // ตัวเลขอยู่กลางช่อง
        amountField.setMaximumSize(new Dimension(200, 32));        // BoxLayout ยืดช่องได้ไม่เกิน 200 x 32
        amountField.addActionListener(e -> confirm());             // กด Enter ในช่อง = กด Confirm

        //    ปุ่มลัด 5 ปุ่ม เรียงกลาง ห่างกัน 6 px / กด → ใส่ตัวเลขนั้นลงช่อง (ไม่ได้บวกเพิ่ม เหมือนของเดิม)
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        quick.setOpaque(false);
        quick.setAlignmentX(Component.CENTER_ALIGNMENT);
        for (int amount : QUICK_AMOUNTS) {
            JButton q = button("+" + amount, MainTheme.FONT_SMALL, MainTheme.LIGHT_GRAY, MainTheme.TEXT_LABEL, MainTheme.INPUT_BORDER, 4, 8);
            q.addActionListener(e -> amountField.setText(String.valueOf(amount)));   // String.valueOf(500) = "500"
            quick.add(q);
        }
        panel.add(quick);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(amountField);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        //    ปุ่ม Confirm (เขียว ตัวดำ) / Cancel (ขาว กรอบเทา)
        JButton confirm = button("Confirm Top-up", MainTheme.FONT_LABEL, MainTheme.SUCCESS, MainTheme.BUTTON_TEXT, MainTheme.SUCCESS, 7, 16);
        confirm.addActionListener(e -> confirm());          // กด → ④ confirm()
        JButton cancel = button("Cancel", MainTheme.FONT_LABEL, MainTheme.CARD, MainTheme.TEXT_LABEL, MainTheme.INPUT_BORDER, 7, 16);
        cancel.addActionListener(e -> dispose());            // กด → ปิดกล่อง ไม่เติมเงิน

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.add(confirm);
        buttons.add(cancel);
        panel.add(buttons);

        setContentPane(panel);
        pack();                          // ขนาดพอดีของข้างใน
        setLocationRelativeTo(owner);    // เปิดกลางหน้าต่างเจ้าของ
    }

    /**
     * ④ กด Confirm / Enter
     */
    private void confirm() {
        String text = amountField.getText().trim();   // trim() = ตัดช่องว่างหน้า/หลัง
        int amount;
        try {
            amount = Integer.parseInt(text);          // "500" → 500 / "abc" → NumberFormatException
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid integer amount", "Error", JOptionPane.ERROR_MESSAGE);
            return;                                   // กล่องยังเปิดอยู่ ให้แก้ตัวเลข
        }
        try {
            // topUp(user, amount) = เติมเงินแล้วบันทึก accounts.csv คืนบัญชีใหม่ (AccountService.java)
            //   นอกช่วง 1–5,000 → Account throw IllegalArgumentException (กฎอยู่ที่ Account ที่เดียว)
            Account updated = accounts.topUp(user, amount);
            dispose();         // ปิดกล่องก่อน
            onDone.run();      // ให้ MainFrame อัปเดตแถบบน (ยอดเงินใหม่)
            JOptionPane.showMessageDialog(getOwner(),
                    "Top-up successful: " + amount + " THB\nCurrent Balance: " + updated.balance() + " THB",
                    "Top-up Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException e) {
            // ผิดกฎช่วงเงิน → บอกช่วงที่ถูก (ตัวเลขจาก Account) กล่องยังเปิดอยู่
            //   เช่น "Top-up amount must be between 1 and 5,000 THB" (ข้อความเดียวกับของเดิม แต่ตัวเลขอ่านจาก Account)
            JOptionPane.showMessageDialog(this,
                    String.format(Locale.US, "Top-up amount must be between %,d and %,d THB", Account.MIN_TOP_UP, Account.MAX_TOP_UP),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            // อ่าน/เขียนไฟล์ไม่ได้ → ไม่ได้เติม
            JOptionPane.showMessageDialog(this, "Cannot save top-up: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ข้อความช่วงเงิน เช่น "1 - 5,000 THB" อ่านจากกฎของ Account
     *   String.format(Locale.US, "%,d", 5000) = "5,000" (%,d = ตัวเลขมีลูกน้ำ)
     *   ไม่รับพารามิเตอร์ อ่าน Account.MIN_TOP_UP / MAX_TOP_UP
     *
     * @return String ข้อความช่วงเงิน เช่น "1 - 5,000 THB"
     */
    private static String rangeText() {
        return String.format(Locale.US, "%,d - %,d THB", Account.MIN_TOP_UP, Account.MAX_TOP_UP);
    }

    /**
     * ป้ายข้อความ 1 บรรทัด จัดกลาง
     *
     * @param text ข้อความ เช่น "Wallet Top-up"
     * @param font ฟอนต์
     * @param color สีตัวหนังสือ
     * @return JLabel ป้าย 1 บรรทัดที่อยู่กลางกล่อง
     */
    private static JLabel line(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);   // อยู่กลางกล่อง (กล่องเรียงแนวตั้งด้วย BoxLayout)
        return label;
    }

    /**
     * ปุ่มของกล่อง (ปุ่มลัด +100 ... และปุ่ม Confirm Top-up ใช้ตัวนี้ทั้งคู่)
     *
     * @param text       คำบนปุ่ม เช่น "+500" หรือ "Confirm Top-up"
     * @param font       ฟอนต์ เช่น MainTheme.FONT_LABEL
     * @param background สีพื้น เช่น MainTheme.SUCCESS (เขียว)
     * @param foreground สีตัวหนังสือ เช่น MainTheme.BUTTON_TEXT (ดำ)
     * @param border     สีกรอบ
     * @param padV       ที่ว่างระหว่างกรอบกับคำ ด้านบน-ล่าง px (ปุ่มลัด 4 / ปุ่มหลัก 7)
     * @param padH       ที่ว่างระหว่างกรอบกับคำ ด้านซ้าย-ขวา px (ปุ่มลัด 8 / ปุ่มหลัก 16)
     * @return JButton ปุ่มที่ทาสีและเว้นขอบแล้ว (ยังไม่ได้ผูกว่ากดแล้วทำอะไร คนเรียกผูกเอง)
     */
    private static JButton button(String text, Font font, Color background, Color foreground, Color border, int padV, int padH) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);                       // กดแล้วไม่วาดกรอบประ
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));    // ชี้แล้วเป็นรูปมือ
        button.setBorder(new CompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(padV, padH, padV, padH)));
        return button;
    }
}