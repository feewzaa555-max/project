//AdminFrame <- หน้าต่างของ admin: เอาแถบบน (AdminTopBar) การ์ดเลือกหนัง (LineupPanel) และการ์ดตั้งเวลา (ClockPanel) มาประกอบเป็นหน้าเดียว
/*
    หน้าตา (เลขตรงกับ ①–⑥ ในโค้ดข้างล่าง):
        ┌──────────────────────────────────────────────┐  ① หน้าต่าง 920 x 730 ชื่อ "KU CINEMA - Admin"
        │ AdminTopBar  (KU CINEMA / Admin / Logout)     │  ③ โซนบน (NORTH)
        ├──────────────────────────────────────────────┤
        │  พื้นเทา                                      │  ② พื้นที่กลาง (CENTER) วางคอลัมน์ไว้ตรงกลาง
        │          ┌──── LineupPanel ────┐             │     ในคอลัมน์: lineupBox (ใส่ LineupPanel)
        │          └─────────────────────┘             │                ช่องว่าง 16 px
        │          ┌──── ClockPanel ─────┐             │                ClockPanel
        │          └─────────────────────┘             │
        └──────────────────────────────────────────────┘
        กด Logout → ⑤ ถามยืนยัน → ปิดหน้านี้ → เปิดหน้า Login
        กด Set / Reset ใน ClockPanel → ClockPanel บอกว่าเวลาเปลี่ยนแล้ว → ⑥ showLineup() ใส่ LineupPanel ตัวใหม่
                                      → Showing on เป็นวันนี้ (ตามเวลาใหม่) + 1

    ไฟล์นี้ทำแค่ 3 อย่าง:
      1. ประกอบหน้าต่าง (ขนาด, พื้นหลัง, วางแถบบน การ์ดเลือกหนัง การ์ดตั้งเวลา)
      2. ปุ่ม Logout: ถามยืนยัน → ปิดหน้านี้ → กลับหน้า Login
      3. เวลาเปลี่ยน → ใส่ LineupPanel ตัวใหม่ ข้อความ Showing on จะได้ตรงกับวันที่ที่ Save จะบันทึก
    ส่วนหน้าตาของแถบบนอยู่ใน AdminTopBar / การเลือกหนังอยู่ใน LineupPanel / การตั้งเวลาอยู่ใน ClockPanel

    เปิดหน้านี้ยังไง (LoginFrame หลัง login แล้วเป็น admin):
        new AdminFrame(user, services).setVisible(true);

    คำที่ใช้บ่อยในไฟล์นี้:
        JFrame          = หน้าต่าง 1 บาน (มีแถบชื่อ ปุ่มปิด) ข้างในใส่ JPanel
        BorderLayout    = แบ่งพื้นที่เป็นโซน NORTH (บน) / CENTER (กลาง กินที่ที่เหลือทั้งหมด)
        GridBagLayout   = ตัวจัดวางที่ถ้าใส่ของชิ้นเดียว จะวางไว้ "ตรงกลาง" ตามขนาดของมัน ไม่ยืด
        BoxLayout(..., Y_AXIS) = เรียงของจากบนลงล่างทีละชิ้น ตามลำดับที่ add
        setContentPane  = เอา JPanel มาเป็นเนื้อหาทั้งหมดของหน้าต่าง
 */

package gui.admin;

import gui.login.LoginFrame;  // หน้า Login ตัวใหม่ (โฟลเดอร์ gui/login) กด Logout แล้วกลับไปหน้านี้ อยู่คนละ package เลยต้อง import
import java.awt.*;            // BorderLayout = แบ่งบน/กลาง, GridBagLayout = วางการ์ดไว้กลางจอ, Dimension = ขนาดช่องว่าง
import javax.swing.*;         // JFrame = หน้าต่าง, JPanel = กล่อง, JOptionPane = กล่องถามยืนยัน Logout, Box/BoxLayout = เรียงการ์ดบนลงล่าง
import model.User;            // admin ที่ login อยู่
import service.AppServices;   // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

public final class AdminFrame extends JFrame {

    private final AppServices services;   // เก็บไว้ส่งกลับให้ LoginFrame ตอน Logout (นาฬิกาและไฟล์ชุดเดิม) และส่งให้ LineupPanel ตัวใหม่
    private final User admin;             // คนที่ login อยู่ ใช้ตอนสร้าง LineupPanel ตัวใหม่ใน ⑥ showLineup()
    private final JPanel lineupBox;       // กล่องที่ใส่ LineupPanel ไว้ 1 ตัว เวลาเปลี่ยน → เอาตัวเก่าออก ใส่ตัวใหม่ในกล่องนี้

    /**
     * เปิดหน้า admin
     * public เพราะ LoginFrame (อยู่ package gui.login ไม่ใช่ gui.admin) ต้อง new หน้านี้
     *
     * @param admin    คนที่ login แล้ว ห้าม null และต้องเป็น ADMIN
     * @param services ของหลังบ้านชุดเดียวกับทั้งโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า admin หรือ services เป็น null หรือ admin ไม่ใช่ ADMIN
     */
    public AdminFrame(User admin, AppServices services) {
        // ตรวจของที่ส่งเข้ามาก่อน ผิดตั้งแต่ต้น → หยุดเลย ไม่เปิดหน้าครึ่ง ๆ (SC2 fail-fast)
        if (admin == null) {
            throw new IllegalArgumentException("admin must not be null");
        }
        if (!admin.isAdmin()) {   // ลูกค้าธรรมดาเข้าหน้านี้ไม่ได้ (ถ้าเกิด แปลว่าโค้ดที่เรียกเปิดผิดหน้า)
            throw new IllegalArgumentException("user is not admin: " + admin.username());
        }
        if (services == null) {
            throw new IllegalArgumentException("services must not be null");
        }
        this.services = services;
        this.admin = admin;

        // ① ตั้งค่าหน้าต่าง
        setTitle("KU CINEMA - Admin");                     // ข้อความบนแถบชื่อหน้าต่าง
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // กดกากบาทปิดหน้าต่าง = ปิดโปรแกรม
        setSize(920, 730);                                 // กว้าง 920 สูง 730 px เท่ากับ MainFrame
        setLocationRelativeTo(null);                       // null = เปิดกลางจอ
        setResizable(false);                               // ห้ามลากยืดหน้าต่าง หน้าตาจะได้ไม่เพี้ยน

        // ② พื้นที่กลาง: พื้นเทา + คอลัมน์การ์ด 2 ใบอยู่ตรงกลาง
        //    lineupBox = กล่องใส่ LineupPanel (BorderLayout ใส่ชิ้นเดียว → ชิ้นนั้นเต็มกล่องพอดี)
        //    setOpaque(false) = โปร่งใส ไม่ทาสีพื้นของกล่อง ให้เห็นพื้นเทาข้างหลัง
        lineupBox = new JPanel(new BorderLayout());
        lineupBox.setOpaque(false);
        showLineup();   // ⑥ ใส่ LineupPanel ตัวแรกลงกล่อง (ใช้เมธอดเดียวกับตอนเวลาเปลี่ยน ไม่เขียนซ้ำ)

        //    column = เรียงบนลงล่าง: lineupBox (560 x 420) → ช่องว่าง 16 px → ClockPanel (560 x 130)
        //    Box.createRigidArea(new Dimension(0, 16)) = ก้อนว่าง กว้าง 0 สูง 16 px
        //    new ClockPanel(นาฬิกา, สิ่งที่ต้องทำหลังเวลาเปลี่ยน)
        //      services.clock()  = นาฬิกาตัวเดียวของทั้งโปรแกรม (AppServices.java บรรทัด 231)
        //      this::showLineup  = ส่งเมธอด showLineup (⑥) ให้ ClockPanel เก็บไว้ กด Set / Reset สำเร็จ → ClockPanel เรียก onChange.run() → showLineup() ทำงาน
        //                          ใช้ :: เพราะ showLineup ไม่รับพารามิเตอร์ (เหมือน this::handleLogout ข้างล่าง)
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.add(lineupBox);
        column.add(Box.createRigidArea(new Dimension(0, 16)));
        column.add(new ClockPanel(services.clock(), this::showLineup));

        //    GridBagLayout ใส่ของชิ้นเดียว (column) → วางไว้กลางพื้นที่ ตามขนาดของมัน (560 x 566) ไม่ยืดเต็ม
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AdminTheme.BACKGROUND);
        center.add(column);

        // ③ แบ่งหน้าต่างเป็น บน (แถบบน) / กลาง (พื้นที่กลาง ②)
        //    new AdminTopBar(ชื่อ, คำสั่งตอนกด Logout)
        //      admin.username()   = ชื่อที่โชว์ "Admin: admin"
        //      this::handleLogout = ส่งเมธอด handleLogout (⑤) ของหน้านี้ให้แถบบนเก็บไว้ เรียกตอนกดปุ่ม Logout
        JPanel root = new JPanel(new BorderLayout());
        root.add(new AdminTopBar(admin.username(), this::handleLogout), BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        // ④ ใช้ root เป็นเนื้อหาทั้งหมดของหน้าต่าง
        setContentPane(root);
    }

    /**
     * ⑤ ปุ่ม Logout: ถามยืนยันก่อน แล้วกลับหน้า Login (แบบเดียวกับ MainFrame)
     * ส่ง services ตัวเดิมกลับไป นาฬิกาและไฟล์จะได้เป็นชุดเดียวกัน ไม่สร้างใหม่
     */
    private void handleLogout() {
        // showConfirmDialog(หน้าที่เด้งทับ, ข้อความ, หัวกล่อง, ปุ่ม Yes/No, ไอคอน ?) → คืนค่าปุ่มที่กด
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {          // กด Yes (กด No หรือปิดกล่อง = อยู่หน้าเดิม)
            dispose();                                     // ปิดหน้า admin
            new LoginFrame(services).setVisible(true);     // เปิดหน้า Login ด้วย services ตัวเดิม
        }
    }

    /**
     * ⑥ ใส่ LineupPanel ตัวใหม่ลง lineupBox
     * ใช้ 2 ที่: ตอนเปิดหน้า (②) และตอน ClockPanel บอกว่าเวลาเปลี่ยนแล้ว (ส่งไปเป็น this::showLineup)
     * ทำไมต้องสร้างใหม่: LineupPanel เขียน "Showing on 15/10/2026" ครั้งเดียวตอนสร้าง
     *   ไม่สร้างใหม่ → ตั้งเวลาเป็น 20/10 แล้วจอยังบอก 15/10 แต่กด Save จะบันทึกของ 21/10 (จอหลอก admin)
     *   สร้างใหม่    → ตัวใหม่ถาม tomorrow() ใหม่ → Showing on 21/10/2026 ตรงกับที่ Save จะบันทึก
     *   ถ้า admin ไม่จัดหนังของพรุ่งนี้ ก็ไม่ต้องกด Save หนังจะฉายชุดเดิมต่อไป
     */
    private void showLineup() {
        lineupBox.removeAll();                               // เอา LineupPanel ตัวเก่าออก ไม่งั้นมี 2 ตัวซ้อนกัน
        lineupBox.add(new LineupPanel(admin, services));     // ใส่ตัวใหม่ (ตอนสร้างจะอ่าน tomorrow() ตามเวลาตอนนี้)
        lineupBox.revalidate();                              // บอก Swing ว่าของข้างในเปลี่ยน ให้จัดตำแหน่งใหม่
        lineupBox.repaint();                                 // วาดใหม่บนจอ
    }
}