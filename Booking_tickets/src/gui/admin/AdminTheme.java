//AdminTheme <- ธีมของหน้า admin (ชุดสีกับฟอนต์) เก็บไว้ที่เดียว ทุกชิ้นในโฟลเดอร์ admin หยิบไปใช้
/*
    ทำไมต้องมี:
        แถบบน (AdminTopBar) กับการ์ดกลาง (LineupPanel) ใช้สีฟ้า / ฟอนต์ Tahoma ชุดเดียวกัน
        ถ้าต่างคนต่างประกาศเอง วันหลังเปลี่ยนสีต้องไล่แก้หลายไฟล์ และอาจเปลี่ยนไม่ครบ
        เก็บที่นี่ที่เดียว แก้ทีเดียวทุกชิ้นเปลี่ยนตาม

    ใช้ยังไง (ในไฟล์อื่นของโฟลเดอร์ admin):
        label.setFont(AdminTheme.FONT_LABEL);
        button.setBackground(AdminTheme.PRIMARY);

    เป็นของหน้า admin อย่างเดียว (ไม่มี public) หน้าอื่นมองไม่เห็น
 */

package gui.admin;

import java.awt.*;   // Color = สี, Font = ฟอนต์

final class AdminTheme {

    // ===== สี (ค่าเดียวกับ MainFrame ให้หน้าตาเข้ากัน) =====
    static final Color PRIMARY = new Color(37, 99, 235);        // ฟ้าหลัก: โลโก้ KU CINEMA, ปุ่ม Save
    static final Color BACKGROUND = new Color(245, 247, 250);   // พื้นเทาของหน้าต่าง
    static final Color BORDER = new Color(229, 231, 235);       // เส้นขอบการ์ด และเส้นใต้แถบบน
    static final Color TEXT_DARK = new Color(17, 24, 39);       // ตัวหนังสือเข้ม: หัวข้อ, ชื่อ admin
    static final Color TEXT_MUTED = new Color(107, 114, 128);   // ตัวหนังสือจาง: วันที่, ข้อความเล็กใต้ปุ่ม
    static final Color LOGOUT_BACKGROUND = new Color(254, 242, 242); // พื้นปุ่ม Logout (แดงอ่อน)
    static final Color LOGOUT_TEXT = new Color(220, 38, 38);         // ตัวหนังสือปุ่ม Logout (แดง)
    static final Color LOGOUT_BORDER = new Color(252, 165, 165);     // ขอบปุ่ม Logout

    // ===== ฟอนต์ (Tahoma ทั้งหมด) =====
    static final Font FONT_BRAND = new Font("Tahoma", Font.BOLD, 18);     // โลโก้ KU CINEMA
    static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 20);    // หัวข้อ Tomorrow's Lineup
    static final Font FONT_SUBTITLE = new Font("Tahoma", Font.PLAIN, 12); // บรรทัดวันที่ใต้หัวข้อ
    static final Font FONT_LABEL = new Font("Tahoma", Font.BOLD, 12);     // ป้าย "Movie 1:", ชื่อ admin, ปุ่ม Logout
    static final Font FONT_TEXT = new Font("Tahoma", Font.PLAIN, 13);     // ชื่อหนังในช่องเลือก
    static final Font FONT_BUTTON = new Font("Tahoma", Font.BOLD, 14);    // ปุ่ม Save
    static final Font FONT_SMALL = new Font("Tahoma", Font.PLAIN, 11);    // ข้อความเล็กใต้ปุ่ม Save

    // ไม่ให้ใคร new AdminTheme() เพราะมีแต่ค่าคงที่ ไม่มีอะไรให้สร้าง
    private AdminTheme() {
    }
}