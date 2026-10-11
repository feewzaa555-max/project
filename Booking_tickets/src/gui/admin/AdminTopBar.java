//AdminTopBar <- แถบบนของหน้า admin: ซ้าย KU CINEMA / ขวา ชื่อ admin กับปุ่ม Logout
/*
    หน้าตา:
        ┌──────────────────────────────────────────────┐
        │ KU CINEMA                 Admin: admin [Logout]│
        └──────────────────────────────────────────────┘
    ไม่มี Balance, Top-up, History แบบหน้าลูกค้า เพราะ admin ไม่ได้จองตั๋ว

    ปุ่ม Logout: แถบนี้แค่บอกว่า "ถูกกดแล้ว" ผ่าน onLogout ที่ส่งเข้ามา
    ส่วนการปิดหน้าต่างและกลับหน้า Login เป็นงานของ AdminFrame
    แถบบนจะได้ไม่ต้องรู้จัก LoginFrame หรือ AppServices

    ใช้ยังไง (ใน AdminFrame):
        new AdminTopBar("admin", () -> handleLogout())

    คำที่ใช้บ่อยในไฟล์นี้:
        Border      = "ขอบ" ของกล่องหรือปุ่ม คือสิ่งที่วาดรอบ ๆ ด้านนอก ต้อง new สร้างก่อน แล้วค่อยใส่ด้วย setBorder
        setBorder   = สั่งกล่อง/ปุ่มว่า "ใช้ขอบอันนี้" ใส่ได้ทีละ 1 อัน
        ตัวเลข 4 ตัวในขอบ เรียงเป็น (บน, ซ้าย, ล่าง, ขวา) เสมอ หน่วย px
 */

package gui.admin;

import java.awt.*;                 // BorderLayout = แบ่งซ้าย/ขวา, FlowLayout = เรียงของด้านขวา, Color, Cursor
import javax.swing.*;              // JPanel = แถบ, JLabel = ข้อความ, JButton = ปุ่ม Logout
import javax.swing.border.*;       // EmptyBorder = ที่ว่าง, MatteBorder = เส้นบางด้าน, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ

final class AdminTopBar extends JPanel {

    /**
     * สร้างแถบบน
     *
     * @param adminName ชื่อ admin ที่จะโชว์ เช่น "admin"
     * @param onLogout  คำสั่ง run method นั้น
     *                  สิ่งที่ต้องทำตอนกด Logout (AdminFrame ส่งมา) เช่น () -> handleLogout()
     *                  Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onLogout.run()
     */
    AdminTopBar(String adminName, Runnable onLogout) {
        // ① แบ่งแถบเป็นโซน ซ้าย (WEST) กับ ขวา (EAST) ไว้วางของ ตอนนี้แค่จองที่ ของวางทีหลังในข้อ ⑧
        //    super(...) = เรียก constructor ของ JPanel (ตัวแม่) ให้ใช้ BorderLayout
        super(new BorderLayout());

        // ② พื้นแถบสีขาว ตัดกับพื้นเทาของหน้าต่าง
        setBackground(Color.WHITE);

        // ③ ขอบของแถบ มี 2 ชั้นซ้อนกัน
        //    ชั้นนอก MatteBorder(บน, ซ้าย, ล่าง, ขวา, สี) = เส้นทึบ กำหนดความหนาแยกแต่ละด้านได้
        //            (0, 0, 1, 0) = มีเส้นแค่ด้านล่าง หนา 1 px → เส้นเทาใต้แถบ คั่นแถบกับพื้นข้างล่าง
        //    ชั้นใน EmptyBorder(บน, ซ้าย, ล่าง, ขวา) = ที่ว่างข้างใน ไม่มีสี มองไม่เห็น
        //            (10, 20, 10, 20) = เว้นบน 10 ซ้าย 20 ล่าง 10 ขวา 20 → โลโก้กับปุ่มไม่ชิดขอบ
        //    CompoundBorder(ตัวนอก, ตัวใน) = เอา 2 ขอบมาซ้อนเป็นอันเดียว เพราะ setBorder ใส่ได้ทีละ 1 อัน
        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, AdminTheme.BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ④ ซ้าย: ข้อความโลโก้ KU CINEMA
        //    JLabel = ข้อความบนจอ ผู้ใช้แก้ไม่ได้
        JLabel brand = new JLabel("KU CINEMA");
        brand.setFont(AdminTheme.FONT_BRAND);       // ฟอนต์ Tahoma หนา 18
        brand.setForeground(AdminTheme.PRIMARY);    // สีตัวหนังสือ ฟ้า

        // ⑤ ขวา: ข้อความชื่อ admin เช่น "Admin: admin" (admin มาจาก adminName ที่ AdminFrame ส่งมา)
        JLabel name = new JLabel("Admin: " + adminName);
        name.setFont(AdminTheme.FONT_LABEL);        // ฟอนต์ Tahoma หนา 12
        name.setForeground(AdminTheme.TEXT_DARK);   // สีตัวหนังสือ เกือบดำ

        // ⑥ ขวา: ปุ่ม Logout สีแดง
        JButton logout = new JButton("Logout");
        logout.setFont(AdminTheme.FONT_LABEL);                  // ฟอนต์ Tahoma หนา 12
        logout.setBackground(AdminTheme.LOGOUT_BACKGROUND);     // พื้นปุ่ม แดงอ่อน
        logout.setForeground(AdminTheme.LOGOUT_TEXT);           // ตัวหนังสือ แดง
        logout.setFocusPainted(false);                          // กดแล้วไม่วาดกรอบประรอบคำ
        logout.setCursor(new Cursor(Cursor.HAND_CURSOR));       // ชี้เมาส์แล้วเป็นรูปมือ บอกว่ากดได้
        // ขอบปุ่ม มี 2 ชั้นซ้อนกัน (เหมือนกรอบรูป: กรอบไม้ + กระดาษรองขาว)
        //    ชั้นนอก LineBorder(สี, ความหนา, มุมโค้ง) = เส้นรอบปุ่มครบ 4 ด้าน
        //            (LOGOUT_BORDER, 1, true) = สีชมพู หนา 1 px มุมโค้งมน (false = มุมเหลี่ยม)
        //    ชั้นใน EmptyBorder(บน, ซ้าย, ล่าง, ขวา) = ที่ว่างระหว่างเส้นกับคำว่า Logout
        //            (5, 12, 5, 12) = เว้นบน 5 ซ้าย 12 ล่าง 5 ขวา 12 → คำไม่ชิดเส้น
        //    CompoundBorder(ตัวนอก, ตัวใน) = รวม 2 ชั้นเป็นขอบเดียว แล้ว setBorder ใส่ให้ปุ่ม
        logout.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.LOGOUT_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        // กดปุ่ม → onLogout.run() = ทำคำสั่งที่ AdminFrame ส่งมา (ถามยืนยัน แล้วกลับหน้า Login)
        //    e -> ... = lambda รับเหตุการณ์กดปุ่ม (e) แล้วทำสิ่งที่อยู่หลังลูกศร
        logout.addActionListener(e -> onLogout.run());

        // ⑦ กล่องเล็กฝั่งขวา เอาชื่อกับปุ่มมาเรียงต่อกัน
        //    ทำไมต้องมี: BorderLayout วางได้โซนละ 1 ชิ้น แต่ฝั่งขวามี 2 ชิ้น (ชื่อ + ปุ่ม) เลยใส่กล่องเดียวกันก่อน
        //    FlowLayout(FlowLayout.RIGHT, 10, 0) = เรียงชิดขวา ห่างกันแนวนอน 10 px แนวตั้ง 0
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);   // โปร่งใส ให้เห็นพื้นขาวของแถบ (ไม่งั้นกล่องนี้เป็นสีเทาโผล่มา)
        right.add(name);
        right.add(logout);

        // ⑧ วางของลงโซนที่จองไว้ในข้อ ①: โลโก้ไปซ้าย กล่อง right ไปขวา
        add(brand, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }
}