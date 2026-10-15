//MainTopBar <- แถบบนของหน้าลูกค้า: ซ้าย KU CINEMA / ขวา ชื่อ, Membership, ยอดเงิน, Top-up, History, Logout
/*
    หน้าตา:
        ┌───────────────────────────────────────────────────────────────────────────────────────┐
        │ KU CINEMA     User: tester9 [Membership] Balance: 0 THB [Top-up] [History] [Logout]   │
        └───────────────────────────────────────────────────────────────────────────────────────┘
        เป็นสมาชิกแล้ว → ปุ่ม Membership เปลี่ยนเป็น [Member (until 13/11)] สีเหลืองส้ม

    ปุ่ม 4 ปุ่ม: แถบนี้แค่บอกว่า "ถูกกดแล้ว" ผ่าน Runnable ที่ส่งเข้ามา (แบบเดียวกับปุ่ม Logout ใน AdminTopBar)
        ส่วนการเปิดกล่องสมัครสมาชิก / เติมเงิน / ประวัติ / กลับหน้า Login เป็นงานของ MainFrame
        แถบบนจะได้ไม่ต้องรู้จัก AccountService หรือกล่องที่เด้งขึ้นมา

    ยอดเงิน + สถานะสมาชิก: MainFrame อ่านบัญชีมาแล้วส่งให้ showAccount(...)
        ใช้ account.isMemberAt(now) ของ Account (model) กฎเดียวกับ AccountService.isMember กฎอยู่ที่เดียว
        อ่านบัญชีไม่ได้ → MainFrame เรียก showAccountError() → "Balance: -"

    ใช้ยังไง (ใน MainFrame):
        MainTopBar bar = new MainTopBar(user.username(),
                () -> openMembership(), () -> openTopUp(), () -> openHistory(), () -> handleLogout());
        bar.showAccount(app.accounts().accountOf(user), app.clock().now());   // ตอนเปิดหน้า และหลังเติมเงิน / สมัครสมาชิก

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 303–383 (createTopBar) และ 391–425 (refreshAccountBar)
    ต่างจากเดิม:
        เดิมกดปุ่มแล้วเปิดกล่องในไฟล์เดียวกัน → ใหม่แค่บอก MainFrame
        เดิมแถบบนถาม AccountService เองและ catch (Exception e) เงียบ ๆ → ใหม่ MainFrame ส่ง Account มาให้ ไม่ต้อง catch
        สีและฟอนต์ใช้จาก MainTheme แทนเลขสีในโค้ด

    คำที่ใช้บ่อยในไฟล์นี้:
        Border      = "ขอบ" ของกล่องหรือปุ่ม ต้อง new สร้างก่อน แล้วค่อยใส่ด้วย setBorder
        ตัวเลข 4 ตัวในขอบ เรียงเป็น (บน, ซ้าย, ล่าง, ขวา) เสมอ หน่วย px
 */

package gui.main;

import java.awt.*;                          // BorderLayout = แบ่งซ้าย/ขวา, FlowLayout = เรียงของด้านขวา, Color, Cursor
import java.time.LocalDateTime;             // เวลาตอนนี้ของโปรแกรม ใช้ถามว่ายังเป็นสมาชิกอยู่ไหม
import java.time.format.DateTimeFormatter;  // แปลงวันหมดสมาชิกเป็นข้อความ "13/11"
import javax.swing.*;                       // JPanel = แถบ, JLabel = ข้อความ, JButton = ปุ่ม
import javax.swing.border.*;                // EmptyBorder = ที่ว่าง, MatteBorder = เส้นบางด้าน, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Account;                       // บัญชีลูกค้า: ยอดเงิน (balance) + วันหมดสมาชิก (memberUntil)

final class MainTopBar extends JPanel {

    private static final DateTimeFormatter MEMBER_DATE = DateTimeFormatter.ofPattern("dd/MM");   // วันหมดสมาชิก เช่น 13/11

    private final JButton membershipButton;   // ปุ่ม Membership เปลี่ยนข้อความ/สีตามสถานะสมาชิก (showAccount)
    private final JLabel balanceLabel;        // ป้าย "Balance: 500 THB" เปลี่ยนตามยอดเงิน (showAccount)

    /**
     * สร้างแถบบน เริ่มต้นแสดง "Balance: 0 THB" และปุ่ม "Membership" จนกว่า MainFrame จะเรียก showAccount
     *
     * @param username     ชื่อลูกค้าที่ login อยู่ เช่น "tester9"
     * @param onMembership สิ่งที่ต้องทำตอนกด Membership (MainFrame ส่งมา) เช่น () -> openMembership()
     * @param onTopUp      สิ่งที่ต้องทำตอนกด Top-up เช่น () -> openTopUp()
     * @param onHistory    สิ่งที่ต้องทำตอนกด History เช่น () -> openHistory()
     * @param onLogout     สิ่งที่ต้องทำตอนกด Logout เช่น () -> handleLogout()
     *                     Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนกดปุ่ม (.run())
     * @throws IllegalArgumentException ถ้าค่าใดเป็น null
     */
    MainTopBar(String username, Runnable onMembership, Runnable onTopUp, Runnable onHistory, Runnable onLogout) {
        // ① แบ่งแถบเป็นซ้าย (WEST) / ขวา (EAST)
        //    super(...) = เรียก constructor ของ JPanel (ตัวแม่) ให้ใช้ BorderLayout
        super(new BorderLayout());

        // ② ตรวจของที่ส่งเข้ามา ผิดแล้ว throw เลย (SC2 หน้า 9 Defensive Programming)
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        if (onMembership == null || onTopUp == null || onHistory == null || onLogout == null) {
            throw new IllegalArgumentException("button actions must not be null");
        }

        // ③ ตัวแถบ: พื้นขาว + เส้นเทาใต้แถบ + ที่ว่างข้างใน (แบบเดียวกับ AdminTopBar)
        //    MatteBorder(0, 0, 1, 0, สี) = เส้นแค่ด้านล่าง หนา 1 px / EmptyBorder(10, 20, 10, 20) = เว้นบน 10 ซ้าย 20 ล่าง 10 ขวา 20
        setBackground(MainTheme.CARD);
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, MainTheme.CARD_BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ④ ซ้าย: โลโก้ KU CINEMA
        JLabel brand = new JLabel("KU CINEMA");
        brand.setFont(MainTheme.FONT_BRAND);       // Tahoma หนา 18
        brand.setForeground(MainTheme.PRIMARY);    // ฟ้า

        // ⑤ ขวา: 6 ชิ้นเรียงตามลำดับ
        //    1. ชื่อลูกค้า
        JLabel name = new JLabel("User: " + username);
        name.setFont(MainTheme.FONT_LABEL);
        name.setForeground(MainTheme.TEXT_TITLE);

        //    2. ปุ่ม Membership (ข้อความ/สีจริงใส่ใน showMembership ข้างล่าง)
        membershipButton = button("Membership", onMembership);

        //    3. ยอดเงิน สีเขียวเข้ม
        balanceLabel = new JLabel();
        balanceLabel.setFont(MainTheme.FONT_LABEL);
        balanceLabel.setForeground(MainTheme.BALANCE);

        //    4. ปุ่ม Top-up พื้นเขียว ตัวหนังสือดำ
        JButton topUp = button("Top-up", onTopUp);
        style(topUp, MainTheme.SUCCESS, MainTheme.BUTTON_TEXT, MainTheme.SUCCESS);

        //    5. ปุ่ม History พื้นขาว กรอบเทา
        JButton history = button("History", onHistory);
        style(history, MainTheme.CARD, MainTheme.TEXT_LABEL, MainTheme.INPUT_BORDER);

        //    6. ปุ่ม Logout พื้นแดงอ่อน ตัวหนังสือแดง กรอบแดงกลาง
        JButton logout = button("Logout", onLogout);
        style(logout, MainTheme.LOGOUT_BACKGROUND, MainTheme.ERROR, MainTheme.LOGOUT_BORDER);

        //    กล่องฝั่งขวา: BorderLayout วางได้โซนละ 1 ชิ้น แต่ฝั่งขวามี 6 ชิ้น เลยใส่กล่องเดียวกันก่อน
        //    FlowLayout(FlowLayout.RIGHT, 10, 0) = เรียงชิดขวา ห่างกันแนวนอน 10 px
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);   // โปร่งใส ให้เห็นพื้นขาวของแถบ
        right.add(name);
        right.add(membershipButton);
        right.add(balanceLabel);
        right.add(topUp);
        right.add(history);
        right.add(logout);

        // ⑥ วางของลงโซน
        add(brand, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        // ⑦ ค่าเริ่มต้นก่อนรู้ข้อมูลบัญชี
        balanceLabel.setText("Balance: 0 THB");
        showMembership(false, null);
    }

    /**
     * แสดงยอดเงินและสถานะสมาชิกของบัญชีนี้
     * เช่น Account[tester9, 500, null] → "Balance: 500 THB" + ปุ่ม "Membership"
     *      Account[member1, 401, 2026-11-13T10:00] ที่ now = 14/10 → "Balance: 401 THB" + ปุ่ม "Member (until 13/11)"
     *
     * @param account บัญชีของลูกค้า (MainFrame อ่านมาจาก AccountService)
     * @param now     เวลาตอนนี้ของโปรแกรม เช่น app.clock().now()
     * @throws IllegalArgumentException ถ้า account หรือ now เป็น null
     */
    void showAccount(Account account, LocalDateTime now) {
        if (account == null || now == null) {
            throw new IllegalArgumentException("account and now must not be null");
        }
        balanceLabel.setText("Balance: " + account.balance() + " THB");
        // isMemberAt(now) = ตอนนี้ยังเป็นสมาชิกไหม (Account.java บรรทัด 147) กฎเดียวกับ AccountService.isMember
        showMembership(account.isMemberAt(now), account.memberUntil());
    }

    /**
     * อ่านบัญชีไม่ได้ (MainFrame จับ IOException ได้) → "Balance: -" และปุ่ม "Membership" ธรรมดา
     */
    void showAccountError() {
        balanceLabel.setText("Balance: -");
        showMembership(false, null);
    }

    /**
     * ปุ่ม Membership 2 แบบ
     *
     * @param member เป็นสมาชิกอยู่ไหม
     * @param until วันเวลาหมดสมาชิก (ใช้ตอน member เป็น true เท่านั้น)
     */
    private void showMembership(boolean member, LocalDateTime until) {
        if (member) {
            // เป็นสมาชิก: "Member (until 13/11)" พื้นเหลืองอ่อน ตัวหนังสือส้มเข้ม กรอบส้ม
            membershipButton.setText("Member (until " + until.format(MEMBER_DATE) + ")");
            style(membershipButton, MainTheme.MEMBER_BACKGROUND, MainTheme.MEMBER_TEXT, MainTheme.MEMBER_BORDER);
        } else {
            // ยังไม่เป็น: "Membership" พื้นม่วงอ่อน ตัวหนังสือฟ้า กรอบม่วงกลาง
            membershipButton.setText("Membership");
            style(membershipButton, MainTheme.JOIN_BACKGROUND, MainTheme.PRIMARY, MainTheme.JOIN_BORDER);
        }
    }

    /**
     * สร้างปุ่มพื้นฐานของแถบ: ฟอนต์, เมาส์รูปมือ, กดแล้วทำ action
     *
     * @param text คำบนปุ่ม เช่น "Top-up"
     * @param action สิ่งที่ทำตอนกด เช่น this::openTopUp ของ MainFrame
     * @return JButton ปุ่มที่ตั้งฟอนต์ เมาส์รูปมือ และผูก action แล้ว (ยังไม่ได้ทาสี ทาด้วย style)
     */
    private static JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(MainTheme.FONT_LABEL);                 // Tahoma หนา 12
        button.setFocusPainted(false);                        // กดแล้วไม่วาดกรอบประรอบคำ
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));     // ชี้แล้วเป็นรูปมือ
        button.addActionListener(e -> action.run());          // กดปุ่ม → ทำสิ่งที่ MainFrame ฝากไว้
        return button;
    }

    /**
     * ทาสีปุ่ม: พื้น / ตัวหนังสือ / กรอบ
     *   ขอบ: LineBorder(สี, 1, true) = เส้นรอบมุมโค้ง + EmptyBorder(5, 12, 5, 12) = ที่ว่างระหว่างเส้นกับคำ
     *   ใช้กับ 4 ปุ่ม เขียนที่เดียว (SC2 หน้า 8 DRY) เดิมเขียนซ้ำทุกปุ่ม
     *        เช่น style(ปุ่ม Logout, LOGOUT_BACKGROUND, ERROR, LOGOUT_BORDER) → ปุ่มแดงอ่อน
     *   ผลที่ได้: เปลี่ยนสีปุ่มที่ส่งมาตรง ๆ
     *
     * @param button ปุ่มที่จะทาสี
     * @param background สีพื้น
     * @param text สีตัวหนังสือ
     * @param border สีกรอบ
     */
    private static void style(JButton button, Color background, Color text, Color border) {
        button.setBackground(background);
        button.setForeground(text);
        button.setBorder(new CompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(5, 12, 5, 12)));
    }
}