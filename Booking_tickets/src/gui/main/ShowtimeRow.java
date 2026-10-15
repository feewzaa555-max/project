//ShowtimeRow <- ปุ่มรอบฉายของหนังเรื่องที่เลือกในหน้าลูกค้า กดรอบไหน = จะจองรอบนั้น
/*
    หน้าตา (แถวละ 5 ปุ่ม):
        [  08:45          ] [  11:45   ] [  14:45   ]
        [ Hall 1 · Ended  ] [  Hall 2  ] [  Hall 3  ]
          รอบที่เริ่มไปแล้ว     รอบที่ยังจองได้ = พื้นขาว กรอบ/ตัวหนังสือฟ้า กดได้
          = เทา กดไม่ได้

    วางปุ่มด้วย GridLayout แถวละ 5 ปุ่ม: 1–5 รอบ = 1 แถว / 6–10 รอบ = 2 แถว
        รองรับได้ถึง 10 รอบ เกินกว่านั้นต้องเพิ่มแถบเลื่อน (โรงหนังจริงมี 4–7 รอบต่อเรื่องต่อวัน)

    "เริ่มไปแล้วหรือยัง" ใช้ showtime.hasStarted(now) ของ Showtime (model) กฎเดียวกับที่ MovieService.canBook
    และ BookingService.book ใช้ หน้าจอกับตอนจองจริงจะตัดสินตรงกันเสมอ
    now มาจากนาฬิกาของโปรแกรม (app.clock().now()) admin ตั้งเวลาไว้เท่าไร ปุ่มก็ตัดสินตามเวลานั้น

    แถวนี้แค่บอกว่า "ลูกค้ากดรอบแล้ว" ผ่าน onPick ที่ส่งเข้ามา
    ส่วนการเปิดหน้าเลือกที่นั่ง (SeatFrame) เป็นงานของ MainFrame แถวนี้จะได้ไม่ต้องรู้จัก SeatFrame

    ใช้ยังไง (ใน MainFrame):
        ShowtimeRow row = new ShowtimeRow(app.movies().showtimesOf(หนัง, วันที่), app.clock().now(), () -> openSeats());
        row.picked()   // รอบที่ลูกค้ากด เช่น 11:45 Hall 2 / ยังไม่กด → null

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 1008–1086 (refreshShowtimes, createShowtimeButton)
    ต่างจากเดิม:
        เดิมกดปุ่มแล้วเปิด SeatFrame ในปุ่มเลย → ใหม่แค่บอก MainFrame ผ่าน onPick
        ป้าย "3. Showtimes: ..." และข้อความ error ตอนอ่านไฟล์ไม่ได้ ให้ MainFrame ทำ (แบบเดียวกับป้าย "1. Date")
        สีและฟอนต์ใช้จาก MainTheme แทนเลขสีในโค้ด

    คำที่ใช้บ่อยในไฟล์นี้:
        GridLayout(0, 5, 10, 10) = ตารางแถวละ 5 ช่อง (0 = จำนวนแถวเพิ่มเองตามจำนวนปุ่ม) ห่างกัน 10 px ทั้งแนวนอนและแนวตั้ง
        setEnabled(false) = ปุ่มกดไม่ได้ (กดแล้วไม่มีอะไรเกิดขึ้น)
 */

package gui.main;

import java.awt.*;                          // GridLayout, FlowLayout, Dimension, Cursor, Color
import java.time.LocalDateTime;             // เวลาตอนนี้ของโปรแกรม ใช้ตัดสินว่ารอบไหนเริ่มไปแล้ว
import java.time.format.DateTimeFormatter;  // แปลงเวลาเริ่มเป็นข้อความ "08:45"
import java.util.List;                      // รายการรอบฉาย (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.swing.*;                       // JPanel, JButton = ปุ่มรอบ, JLabel = ข้อความตอนไม่มีรอบ
import javax.swing.border.*;                // LineBorder = เส้นรอบปุ่ม, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import model.Showtime;                      // รอบฉาย 1 รอบ: หนัง, โรง (hall), วันที่, เวลาเริ่ม

final class ShowtimeRow extends JPanel {

    private static final int COLUMNS = 5;                                            // ปุ่มต่อแถว
    private static final Dimension BUTTON_SIZE = new Dimension(130, 44);              // ขนาดปุ่มรอบ เท่าของเดิม
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");   // 08:45

    // Abstraction Function (SC4 หน้า 10):
    //   AF(showtimes, now, picked) = ปุ่ม 1 ปุ่มต่อรอบใน showtimes เรียงตามลำดับ
    //                                รอบที่เริ่มก่อน now = กดไม่ได้ / picked = รอบที่ลูกค้ากดล่าสุด (null = ยังไม่กด)
    // Rep Invariant:
    //   showtimes ไม่เป็น null และไม่มี null, now / onPick ไม่เป็น null
    //   picked เป็น null หรือเป็นรอบหนึ่งใน showtimes ที่ยังไม่เริ่ม (กดรอบที่เริ่มแล้วไม่ได้)
    // Safety from rep exposure (SC4 หน้า 14):
    //   showtimes เก็บสำเนาแบบแก้ไม่ได้ (List.copyOf) / Showtime เป็น record แก้ค่าไม่ได้ คืน picked ตัวจริงได้
    private final List<Showtime> showtimes;   // รอบของหนังเรื่องที่เลือกในวันที่เลือก เช่น [08:45 Hall 1, 11:45 Hall 2, ...]
    private final LocalDateTime now;          // เวลาตอนนี้ของโปรแกรม ตอนสร้างแถว
    private final Runnable onPick;            // สิ่งที่ต้องทำหลังกดรอบ (MainFrame ส่งมา) เช่น () -> openSeats()
    private Showtime picked;                  // รอบที่ลูกค้ากด เริ่มต้น null

    /**
     * สร้างปุ่มรอบฉาย
     *
     * @param showtimes รอบฉาย เช่น app.movies().showtimesOf(หนัง, วันที่) (ว่างได้ = ไม่มีรอบ)
     * @param now       เวลาตอนนี้ของโปรแกรม เช่น app.clock().now() → 2026-10-14T10:00
     * @param onPick    คำสั่ง run method นั้น
     *                  สิ่งที่ต้องทำหลังลูกค้ากดรอบ (MainFrame ส่งมา) เช่น () -> openSeats()
     *                  Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onPick.run()
     * @throws IllegalArgumentException ถ้า showtimes เป็น null / มี null ข้างใน หรือ now / onPick เป็น null
     */
    ShowtimeRow(List<Showtime> showtimes, LocalDateTime now, Runnable onPick) {
        // ① ตรวจของที่ส่งเข้ามา ผิดแล้ว throw เลย (SC2 หน้า 9 Defensive Programming)
        if (showtimes == null) {
            throw new IllegalArgumentException("showtimes must not be null");
        }
        for (Showtime showtime : showtimes) {   // ไล่ดูทีละรอบ มี null ปน → ทำปุ่มไม่ได้
            if (showtime == null) {             // ไม่ใช้ showtimes.contains(null) เพราะลิสต์จาก List.of(...) ถามหา null แล้วพัง
                throw new IllegalArgumentException("showtimes must not contain null");
            }
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (onPick == null) {
            throw new IllegalArgumentException("onPick must not be null");
        }

        // ② เก็บของ
        //    List.copyOf(showtimes) = สำเนาแบบแก้ไม่ได้ ลิสต์ตัวนอกถูกแก้ทีหลังก็ไม่กระทบ (SC4 หน้า 14)
        this.showtimes = List.copyOf(showtimes);
        this.now = now;
        this.onPick = onPick;
        this.picked = null;    // ยังไม่มีใครกด

        // ③ ตั้งค่าตัวแถว
        setOpaque(false);                            // โปร่งใส ให้เห็นพื้นขาวของการ์ด
        setAlignmentX(Component.CENTER_ALIGNMENT);   // อยู่กลางการ์ด (การ์ดเรียงของแนวตั้งด้วย BoxLayout)

        // ④ ใส่ปุ่ม (สร้างครั้งเดียว กดรอบแล้วไม่ต้องวาดใหม่ เพราะหน้าจะเปลี่ยนไปหน้าเลือกที่นั่ง)
        if (this.showtimes.isEmpty()) {
            setLayout(new FlowLayout(FlowLayout.CENTER));
            JLabel empty = new JLabel("No showtimes available for the selected date");
            empty.setFont(MainTheme.FONT_SMALL);
            empty.setForeground(MainTheme.TEXT_MUTED);
            add(empty);
        } else {
            // GridLayout(0, COLUMNS, 10, 10) = แถวละ 5 ปุ่ม แถวเพิ่มเองตามจำนวนรอบ ห่างกัน 10 px
            //   ปุ่มน้อยกว่า 5 → ใช้จำนวนปุ่มเป็นจำนวนช่อง (3 รอบ = 3 ช่อง) แถวจะได้ไม่มีช่องว่างทางขวา
            setLayout(new GridLayout(0, Math.min(COLUMNS, this.showtimes.size()), 10, 10));
            for (Showtime showtime : this.showtimes) {
                add(makeButton(showtime));
            }
        }
        checkRep();
    }

    /**
     * รอบที่ลูกค้ากดล่าสุด
     *
     * @return เช่น Showtime[movie=Spider-Man..., hall=2, date=2026-10-14, start=11:45] / ยังไม่กด → null
     */
    Showtime picked() {
        return picked;
    }

    // สร้างปุ่มของรอบเดียว
    //   showtime = รอบของปุ่มนี้ เช่น 11:45 Hall 2
    private JButton makeButton(Showtime showtime) {
        // hasStarted(now) = รอบนี้เริ่มไปแล้วหรือยัง (Showtime.java) เช่น now 10:00 → รอบ 08:45 true, 11:45 false
        boolean ended = showtime.hasStarted(now);

        // ข้อความ 2 บรรทัด: เวลาเริ่ม (ตัวหนา) / โรง (+ Ended ถ้าเริ่มไปแล้ว)
        //   <html> ทำให้ปุ่มมีหลายบรรทัดได้ / <b> = ตัวหนา / <br> = ขึ้นบรรทัดใหม่
        String hall = ended ? "Hall " + showtime.hall() + " · Ended" : "Hall " + showtime.hall();
        JButton button = new JButton("<html><center><b>" + showtime.start().format(TIME_FORMAT)
                + "</b><br>" + hall + "</center></html>");
        button.setFont(MainTheme.FONT_SMALL);
        button.setPreferredSize(BUTTON_SIZE);
        button.setFocusPainted(false);    // กดแล้วไม่วาดกรอบประรอบคำ

        // สีต่างกันตามว่าเริ่มไปแล้วไหม
        //   ขอบ LineBorder(สี, 1, true) = เส้นรอบปุ่ม มุมโค้ง / EmptyBorder(4, 6, 4, 6) = ที่ว่างข้างใน
        Color borderColor;
        if (ended) {
            button.setEnabled(false);                         // กดไม่ได้
            button.setBackground(MainTheme.LIGHT_GRAY);       // พื้นเทาอ่อน
            button.setForeground(MainTheme.DISABLED_TEXT);    // ตัวหนังสือเทา
            borderColor = MainTheme.CARD_BORDER;              // กรอบเทาอ่อน
        } else {
            button.setCursor(new Cursor(Cursor.HAND_CURSOR)); // ชี้แล้วเป็นรูปมือ
            button.setBackground(MainTheme.CARD);             // พื้นขาว
            button.setForeground(MainTheme.PRIMARY);          // ตัวหนังสือฟ้า
            borderColor = MainTheme.PRIMARY;                  // กรอบฟ้า
            // กดปุ่ม → pick(รอบนี้) / แต่ละปุ่มจำรอบของตัวเองไว้ (ปุ่มที่ setEnabled(false) กดไม่ได้อยู่แล้ว ไม่ต้องใส่)
            button.addActionListener(e -> pick(showtime));
        }
        button.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(4, 6, 4, 6)));
        return button;
    }

    // ลูกค้ากดรอบ → จำรอบ แล้วบอก MainFrame
    //   showtime = รอบของปุ่มที่ถูกกด
    private void pick(Showtime showtime) {
        picked = showtime;   // จำรอบที่กด
        checkRep();          // ตรวจ RI (SC4 หน้า 12 เรียกท้ายทุกเมธอดที่แก้ rep)
        onPick.run();        // ทำคำสั่งที่ MainFrame ส่งมา เช่น openSeats() เปิดหน้าเลือกที่นั่งของรอบนี้
    }

    // ตรวจ Rep Invariant ทุกข้อ ทำงานเฉพาะตอนรันด้วย java -ea (SC4 หน้า 12)
    private void checkRep() {
        assert showtimes != null && now != null && onPick != null : "field ต้องไม่เป็น null";
        assert picked == null || (showtimes.contains(picked) && !picked.hasStarted(now))
                : "รอบที่กดต้องอยู่ในแถวและยังไม่เริ่ม";
    }
}