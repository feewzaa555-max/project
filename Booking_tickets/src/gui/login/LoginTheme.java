//LoginTheme <- ธีมของหน้า login (ชื่อโรงหนัง ชุดสี ฟอนต์) เก็บไว้ที่เดียว ทุกชิ้นของหน้า login หยิบไปใช้
/*
    ทำไมต้องมี:
        หน้า login มีหลายชิ้น: หน้าต่าง (LoginFrame), การ์ด Sign In, การ์ด Sign Up, ช่องกรอก (FormField)
        ทุกชิ้นใช้สีฟ้า / ฟอนต์ Tahoma ชุดเดียวกัน ถ้าต่างคนต่างประกาศเอง วันหลังเปลี่ยนสีต้องไล่แก้หลายไฟล์
        เก็บที่นี่ที่เดียว แก้ทีเดียวทุกชิ้นเปลี่ยนตาม

    สีทุกตัวเอามาจาก LoginFrame ตัวเดิม (ไม่ได้คิดใหม่) ยกเว้น BUTTON_TEXT ที่เปลี่ยนจากดำเป็นขาว
    PRIMARY / BACKGROUND / BORDER / TEXT_DARK / TEXT_MUTED ค่าเดียวกับ AdminTheme หน้า login กับหน้า admin เลยเข้าชุดกัน

    เลข ①–⑫ ตรงกับภาพในสไลด์ "12 สีของหน้า login"

    ใช้ยังไง:
        title.setForeground(LoginTheme.TEXT_DARK);
        title.setFont(LoginTheme.FONT_HEADER);
        signUpButton.setForeground(LoginTheme.PRIMARY);   → คำ "Sign Up" สีฟ้า (เป็นปุ่มแยก ไม่ใช้ HTML จึงใส่ Color ได้ตรง ๆ)

    เป็น public เพราะ LoginFrame อยู่คนละโฟลเดอร์ (package gui) แต่ต้องใช้สีพื้นหน้าต่าง
 */

package gui.login;

import java.awt.*;   // Color = สี (แดง, เขียว, น้ำเงิน แต่ละตัว 0–255), Font = ฟอนต์ (ชื่อ, หนา/บาง, ขนาด)

public final class LoginTheme {   // final = ห้ามมี class อื่นมา extends

    // ===== ชื่อโรงหนัง =====
    public static final String BRAND = "KU CINEMA";   // ใช้ที่ชื่อหน้าต่าง, คำสีฟ้าเหนือหัวข้อ, ข้อความรอง

    // ===== สี: new Color(แดง, เขียว, น้ำเงิน) แต่ละค่า 0–255 =====
    public static final Color BACKGROUND = new Color(245, 247, 250);   // ① พื้นหน้าต่างรอบการ์ด (เทาอ่อน)
    public static final Color CARD = new Color(255, 255, 255);         // ② พื้นการ์ด (ขาว)
    public static final Color BORDER = new Color(229, 231, 235);       // ③ เส้นขอบการ์ด
    public static final Color PRIMARY = new Color(37, 99, 235);        // ④ ฟ้าหลัก: KU CINEMA, ปุ่ม Sign In, ปุ่มคำ "Sign Up"
    public static final Color TEXT_DARK = new Color(17, 24, 39);       // ⑤ หัวข้อ Sign In / Sign Up
    public static final Color TEXT_MUTED = new Color(107, 114, 128);   // ⑥ ข้อความรองใต้หัวข้อ
    public static final Color LABEL = new Color(55, 65, 81);           // ⑦ ป้าย Username / Password
    public static final Color FIELD_BORDER = new Color(209, 213, 219); // ⑧ เส้นขอบช่องกรอก
    public static final Color ERROR = new Color(220, 38, 38);          // ⑨ ข้อความ error ใต้ช่อง (แดง)
    public static final Color BUTTON_TEXT = new Color(255, 255, 255);  // ⑩ ตัวหนังสือบนปุ่ม (ขาว เดิมเป็นดำ อ่านยาก)
    public static final Color LINK_TEXT = new Color(75, 85, 99);       // ⑪ "Don't have an account?" (เทาเข้ม)
    public static final Color SUCCESS = new Color(16, 185, 129);       // ⑫ เขียว: ปุ่ม Create Account, ปุ่มคำ "Sign In"

    // ===== ฟอนต์: new Font(ชื่อฟอนต์, หนา/ปกติ, ขนาด) =====
    //   Font.BOLD = ตัวหนา, Font.PLAIN = ตัวปกติ, ขนาดหน่วย pt (ตัวเลขมาก = ตัวใหญ่)
    public static final Font FONT_BRAND = new Font("Tahoma", Font.BOLD, 18);      // KU CINEMA สีฟ้า (ขนาดเดียวกับโลโก้หน้า admin)
    public static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 20);     // หัวข้อ Sign In / Sign Up
    public static final Font FONT_SUBTITLE = new Font("Tahoma", Font.PLAIN, 12);  // ข้อความรอง และ "Don't have an account?"
    public static final Font FONT_LABEL = new Font("Tahoma", Font.BOLD, 12);      // ป้าย Username / Password
    public static final Font FONT_INPUT = new Font("Tahoma", Font.PLAIN, 13);     // ตัวหนังสือที่พิมพ์ในช่อง
    public static final Font FONT_ERROR = new Font("Tahoma", Font.PLAIN, 11);     // ข้อความ error ใต้ช่อง
    public static final Font FONT_BUTTON = new Font("Tahoma", Font.BOLD, 14);     // ปุ่ม Sign In / Create Account
    public static final Font FONT_LINK = new Font("Tahoma", Font.BOLD, 12);       // ปุ่มคำ "Sign Up" / "Sign In" ใต้ปุ่มหลัก

    // ไม่ให้ใคร new LoginTheme() เพราะมีแต่ค่าคงที่ ไม่มีอะไรให้สร้าง
    private LoginTheme() {
    }
}