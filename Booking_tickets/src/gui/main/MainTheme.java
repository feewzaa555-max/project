//MainTheme <- ธีมของหน้าลูกค้า (MainFrame) สี ฟอนต์ เก็บไว้ที่เดียว ทุกชิ้นในโฟลเดอร์ main หยิบไปใช้
/*
    ทำไมต้องมี:
        MainFrame เดิม (gui/MainFrame.java ของวรัชญ์) เขียนสีตรง ๆ ในโค้ด 49 จุด เช่น new Color(16, 185, 129) ซ้ำ 6 ที่
        พอแยกหน้าเป็น 9 ไฟล์ (แถบบน, กล่องเติมเงิน, แถวโปสเตอร์, ปุ่มรอบฉาย ...) ถ้าต่างคนต่างเขียนสีเอง
        วันหลังเปลี่ยนสีเขียวต้องไล่แก้หลายไฟล์ และพลาดง่าย
        เก็บที่นี่ที่เดียว แก้ที่นี่ทีเดียว ทุกไฟล์เปลี่ยนตาม (SC2 หน้า 8 DRY)

    ค่าทุกตัวเอามาจาก gui/MainFrame.java เดิม (ไม่ได้คิดใหม่) วงเล็บท้ายคอมเมนต์ = บรรทัดในไฟล์เดิม
    PRIMARY / BACKGROUND / CARD_BORDER / TEXT_TITLE / TEXT_MUTED ค่าเดียวกับ LoginTheme และ AdminTheme ทุกหน้าเลยเข้าชุดกัน

    ใช้ยังไง (ในไฟล์อื่นของโฟลเดอร์ main):
        topBar.setBackground(MainTheme.CARD);          → แถบบนพื้นขาว
        balanceLabel.setForeground(MainTheme.BALANCE); → "Balance: 500 THB" สีเขียวเข้ม
        title.setFont(MainTheme.FONT_HEADER);          → "Select Showtime" ตัวหนา 20

    ไม่มี public ทั้ง class และค่าข้างใน: ใช้แค่ในโฟลเดอร์ main แบบเดียวกับ LoginTheme และ AdminTheme
 */

package gui.main;

import java.awt.*;        // Color = สี (แดง, เขียว, น้ำเงิน แต่ละตัว 0–255), Font = ฟอนต์ (ชื่อ, หนา/บาง, ขนาด)
import java.util.List;    // รายการสีพื้นโปสเตอร์สำรอง (POSTER_COLORS) แบบแก้ไม่ได้

final class MainTheme {   // final = ห้ามมี class อื่นมา extends

    // ===== สีหลักของหน้า: new Color(แดง, เขียว, น้ำเงิน) แต่ละค่า 0–255 =====
    static final Color PRIMARY = new Color(37, 99, 235);         // ฟ้าหลัก: KU CINEMA, ปุ่มวันที่ที่เลือก, กรอบและชื่อหนังที่เลือก, ปุ่มรอบฉาย (73)
    static final Color BACKGROUND = new Color(245, 247, 250);    // พื้นเทาอ่อนของหน้าต่าง รอบการ์ดกลาง (74)
    static final Color CARD = new Color(255, 255, 255);          // ขาว: การ์ดกลาง, แถบบน, กล่องที่เด้ง, ปุ่มพื้นขาว (เดิม Color.WHITE บรรทัด 180, 305, 353 ...)
    static final Color CARD_BORDER = new Color(229, 231, 235);   // เส้นขอบการ์ด และกรอบปุ่มรอบฉายที่เลยเวลาแล้ว (75)
    static final Color INPUT_BORDER = new Color(209, 213, 219);  // กรอบปุ่มพื้นขาว เช่น History, Close, วันที่ที่ไม่ได้เลือก (76)

    // ===== สีตัวหนังสือ =====
    static final Color TEXT_TITLE = new Color(17, 24, 39);       // หัวข้อ เช่น "Select Showtime", ชื่อหนังที่ไม่ได้เลือก (77)
    static final Color TEXT_LABEL = new Color(55, 65, 81);       // ป้าย เช่น "1. Date", "User: tester9", ตัวหนังสือปุ่มวันที่ที่ไม่ได้เลือก (78)
    static final Color TEXT_MUTED = new Color(107, 114, 128);    // ข้อความรอง เช่น "145 min", "Welcome tester9 | ..." (79)
    static final Color BUTTON_TEXT = new Color(0, 0, 0);         // ตัวหนังสือบนปุ่มสี เช่น Top-up, Confirm, วันที่ที่เลือก (เดิม Color.BLACK บรรทัด 342, 495, 850)

    // ===== สีบอกความหมาย =====
    static final Color SUCCESS = new Color(16, 185, 129);        // เขียว: ปุ่ม Top-up, ปุ่มยืนยันเติมเงิน, สถานะสมาชิก, ราคาในประวัติ (341, 633, 470, 780)
    static final Color BALANCE = new Color(5, 150, 105);         // เขียวเข้ม: "Balance: 500 THB" ในแถบบน (335)
    static final Color ERROR = new Color(220, 38, 38);           // แดง: คำ Logout, ข้อความ error (366, 1043)
    static final Color LOGOUT_BACKGROUND = new Color(254, 242, 242); // พื้นปุ่ม Logout แดงอ่อน (365)
    static final Color LOGOUT_BORDER = new Color(252, 165, 165);     // กรอบปุ่ม Logout แดงกลาง (370)

    // ===== ปุ่ม Membership ในแถบบน (สีเปลี่ยนตามว่าเป็นสมาชิกหรือยัง) =====
    static final Color MEMBER_BACKGROUND = new Color(254, 243, 199); // เป็นสมาชิกแล้ว: พื้นเหลืองอ่อน (408)
    static final Color MEMBER_TEXT = new Color(180, 83, 9);          // เป็นสมาชิกแล้ว: ตัวหนังสือส้มเข้ม (409)
    static final Color MEMBER_BORDER = new Color(245, 158, 11);      // เป็นสมาชิกแล้ว: กรอบส้ม (411)
    static final Color JOIN_BACKGROUND = new Color(238, 242, 255);   // ยังไม่เป็นสมาชิก: พื้นม่วงอ่อน ตัวหนังสือใช้ PRIMARY (415)
    static final Color JOIN_BORDER = new Color(199, 210, 254);       // ยังไม่เป็นสมาชิก: กรอบม่วงกลาง (418)

    // ===== ของที่กดไม่ได้ / ปุ่มเล็ก =====
    static final Color LIGHT_GRAY = new Color(243, 244, 246);    // พื้นเทาอ่อน: ปุ่มจำนวนเงินด่วนในกล่องเติมเงิน, ปุ่มรอบฉายที่เลยเวลา (606, 1068)
    static final Color DISABLED_TEXT = new Color(156, 163, 175); // ตัวหนังสือเทา: ปุ่มรอบฉายที่เลยเวลา "08:45 Hall 1 · Ended" (1069)

    // ===== หน้าเลือกที่นั่ง (SeatFrame) วงเล็บ = บรรทัดใน gui/SeatFrame.java เดิม =====
    //   ที่นั่งที่เลือกใช้ PRIMARY / ที่นั่งที่จองแล้วใช้ LIGHT_GRAY + DISABLED_TEXT (มีข้างบนแล้ว)
    static final Color VIP_BACKGROUND = new Color(254, 243, 199);    // ที่นั่ง VIP แถว F: พื้นเหลืองอ่อน (84)
    static final Color VIP_TEXT = new Color(180, 83, 9);             // ที่นั่ง VIP: ตัวเลขส้มเข้ม (86)
    static final Color VIP_BORDER = new Color(245, 158, 11);         // ที่นั่ง VIP: กรอบส้ม (85)
    static final Color SCREEN = new Color(239, 246, 255);            // พื้นป้าย SCREEN ฟ้าอ่อน บนผังที่นั่ง (303)

    // ===== สีพื้นโปสเตอร์สำรอง (ใช้ตอนหาไฟล์รูปโปสเตอร์ไม่เจอ) =====
    //   หนังเรื่องที่ 1 ได้สีแรก เรื่องที่ 2 ได้สีที่สอง ... เรื่องที่ 6 วนกลับมาสีแรก (1184–1188)
    //   List.of(...) = รายการที่แก้ไม่ได้ ไฟล์อื่นเรียก add / set แล้วพังทันที สีจะไม่ถูกเปลี่ยนโดยไม่ตั้งใจ (SC5 หน้า 9 ข้อ 6)
    static final List<Color> POSTER_COLORS = List.of(
            new Color(71, 85, 105),    // น้ำเงินอมเทา
            new Color(109, 40, 217),   // ม่วง
            new Color(13, 148, 136),   // เขียวอมฟ้า
            new Color(217, 119, 6),    // ส้ม
            new Color(37, 99, 235));   // ฟ้า

    // ===== ฟอนต์: new Font(ชื่อฟอนต์, หนา/ปกติ, ขนาด) =====
    //   Font.BOLD = ตัวหนา, Font.PLAIN = ตัวปกติ, ขนาดหน่วย pt (ตัวเลขมาก = ตัวใหญ่)
    static final Font FONT_BRAND = new Font("Tahoma", Font.BOLD, 18);         // KU CINEMA ในแถบบน (312)
    static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 20);        // "Select Showtime" (81)
    static final Font FONT_DIALOG_TITLE = new Font("Tahoma", Font.BOLD, 16);  // หัวข้อกล่องที่เด้ง เช่น "Wallet Top-up" (451, 580, 715)
    static final Font FONT_SUBTITLE = new Font("Tahoma", Font.PLAIN, 12);     // ข้อความรอง, ปุ่มวันที่ที่ไม่ได้เลือก (82, 844)
    static final Font FONT_LABEL = new Font("Tahoma", Font.BOLD, 12);         // ป้าย "1. Date", ปุ่มวันที่ที่เลือก (83, 844)
    static final Font FONT_TEXT = new Font("Tahoma", Font.PLAIN, 13);         // ข้อความทั่วไปในกล่องที่เด้ง (84)
    static final Font FONT_SMALL = new Font("Tahoma", Font.PLAIN, 11);        // ตัวเล็ก เช่น "145 min", ชื่อหนังที่ไม่ได้เลือก (85, 977)
    static final Font FONT_SMALL_BOLD = new Font("Tahoma", Font.BOLD, 11);    // ชื่อหนังที่เลือกอยู่ (977)
    static final Font FONT_INPUT = new Font("Tahoma", Font.PLAIN, 14);        // ช่องพิมพ์จำนวนเงินในกล่องเติมเงิน (599)
    static final Font FONT_PRICE = new Font("Tahoma", Font.BOLD, 15);         // ราคาในกล่องประวัติ เช่น "320 THB" (779)
    static final Font FONT_POSTER_LABEL = new Font("Tahoma", Font.BOLD, 13);  // คำ "Poster" บนโปสเตอร์สำรอง (1195)
    static final Font FONT_POSTER_LETTER = new Font("Tahoma", Font.BOLD, 36); // ตัวอักษรใหญ่ A, B, C บนโปสเตอร์สำรอง (1201)

    /**
     * ไม่ให้ใคร new MainTheme() เพราะมีแต่ค่าคงที่ ไม่มีอะไรให้สร้าง
     */
    private MainTheme() {
    }
}