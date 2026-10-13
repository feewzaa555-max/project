//AdminFrame <- หน้าต่างของ admin: เอาแถบบน (AdminTopBar) กับการ์ดกลาง (LineupPanel) มาประกอบเป็นหน้าเดียว
/*
    หน้าตา (เลขตรงกับ ①–⑤ ในโค้ดข้างล่าง):
        ┌──────────────────────────────────────────────┐  ① หน้าต่าง 920 x 730 ชื่อ "KU CINEMA - Admin"
        │ AdminTopBar  (KU CINEMA / Admin / Logout)     │  ③ โซนบน (NORTH)
        ├──────────────────────────────────────────────┤
        │  พื้นเทา                                      │  ② พื้นที่กลาง (CENTER) วางการ์ดไว้ตรงกลาง
        │          ┌──── LineupPanel ────┐             │
        │          └─────────────────────┘             │
        └──────────────────────────────────────────────┘
        กด Logout → ⑤ ถามยืนยัน → ปิดหน้านี้ → เปิดหน้า Login

    ไฟล์นี้ทำแค่ 2 อย่าง:
      1. ประกอบหน้าต่าง (ขนาด, พื้นหลัง, วางแถบบนกับการ์ดกลาง)
      2. ปุ่ม Logout: ถามยืนยัน → ปิดหน้านี้ → กลับหน้า Login
    ส่วนหน้าตาของแถบบนอยู่ใน AdminTopBar / การเลือกหนังอยู่ใน LineupPanel

    เปิดหน้านี้ยังไง (LoginFrame หลัง login แล้วเป็น admin):
        new AdminFrame(user, services).setVisible(true);

    คำที่ใช้บ่อยในไฟล์นี้:
        JFrame          = หน้าต่าง 1 บาน (มีแถบชื่อ ปุ่มปิด) ข้างในใส่ JPanel
        BorderLayout    = แบ่งพื้นที่เป็นโซน NORTH (บน) / CENTER (กลาง กินที่ที่เหลือทั้งหมด)
        GridBagLayout   = ตัวจัดวางที่ถ้าใส่ของชิ้นเดียว จะวางไว้ "ตรงกลาง" ตามขนาดของมัน ไม่ยืด
        setContentPane  = เอา JPanel มาเป็นเนื้อหาทั้งหมดของหน้าต่าง
 */

package gui.admin;

import gui.login.LoginFrame;  // หน้า Login ตัวใหม่ (โฟลเดอร์ gui/login) กด Logout แล้วกลับไปหน้านี้ อยู่คนละ package เลยต้อง import
import java.awt.*;            // BorderLayout = แบ่งบน/กลาง, GridBagLayout = วางการ์ดไว้กลางจอ
import javax.swing.*;         // JFrame = หน้าต่าง, JPanel = กล่อง, JOptionPane = กล่องถามยืนยัน Logout
import model.User;            // admin ที่ login อยู่
import service.AppServices;   // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

public final class AdminFrame extends JFrame {

    private final AppServices services;   // เก็บไว้ส่งกลับให้ LoginFrame ตอน Logout (นาฬิกาและไฟล์ชุดเดิม)

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

        // ① ตั้งค่าหน้าต่าง
        setTitle("KU CINEMA - Admin");                     // ข้อความบนแถบชื่อหน้าต่าง
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // กดกากบาทปิดหน้าต่าง = ปิดโปรแกรม
        setSize(920, 730);                                 // กว้าง 920 สูง 730 px เท่ากับ MainFrame
        setLocationRelativeTo(null);                       // null = เปิดกลางจอ
        setResizable(false);                               // ห้ามลากยืดหน้าต่าง หน้าตาจะได้ไม่เพี้ยน

        // ② พื้นที่กลาง: พื้นเทา + การ์ด LineupPanel อยู่ตรงกลาง
        //    GridBagLayout ใส่ของชิ้นเดียว → วางชิ้นนั้นไว้กลางพื้นที่ ตามขนาดของมัน (560 x 420) ไม่ยืดเต็ม
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AdminTheme.BACKGROUND);
        center.add(new LineupPanel(admin, services));

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
}