//HistoryDialog <- กล่องประวัติการจองที่เด้งขึ้นมาตอนกดปุ่ม History ในแถบบน (ดูอย่างเดียว ไม่แก้อะไร)
/*
    หน้าตา:
        ┌────────────── Booking History (tester9) ──────────────┐
        │ ┌──────────────────────────────────────────────────┐  │  ① หัวข้อ
        │ │ B3 - Spider-Man: Brand New Day          320 THB   │  │  ② การ์ด 1 ใบต่อ 1 การจอง ใหม่สุดอยู่บน
        │ │ Date: 14/10/2026 Time: 11:45 (Hall 3)             │  │     บรรทัด 1 รหัส + ชื่อหนัง (ฟ้า) / ขวา ราคา (เขียว)
        │ │ Seats: C5, C6 | Booked: 14/10/2026 10:05          │  │     บรรทัด 2 วัน เวลา โรง / บรรทัด 3 ที่นั่ง + เวลาที่จอง
        │ └──────────────────────────────────────────────────┘  │
        │   (หลายรายการ → เลื่อนลงได้)                   [Close]  │  ③ ปุ่มปิด
        └────────────────────────────────────────────────────────┘
        ยังไม่เคยจอง → กล่องขาวข้อความ "No booking history yet"
        หารอบฉายของการจองไม่เจอ (เช่นหนังถูกลบจากไฟล์) → บรรทัด 1 เป็น "Showtime ID: 2026-10-14_1145_3" แทนชื่อหนัง

    ใช้ยังไง (ใน MainFrame):
        new HistoryDialog(this, app.bookings(), user).setVisible(true);
        // อ่านไฟล์ไม่ได้ → constructor throw IOException → MainFrame โชว์ error

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 696–820 (showHistoryDialog)
    ต่างจากเดิม:
        เดิม catch (Exception) → ใหม่ IOException ออกไปให้ MainFrame (SC2 หน้า 11)
        เดิม setSize(620, 480) → ใหม่ pack() แต่สูงไม่เกิน 480 (ส่วนเกินเลื่อนดู)
        สีและฟอนต์ใช้จาก MainTheme แทนเลขสีในโค้ด

    คำที่ใช้บ่อยในไฟล์นี้:
        JScrollPane(ของข้างใน) = กล่องที่มีแถบเลื่อน ของข้างในยาวกว่ากล่อง → เลื่อนดูได้
        BorderLayout NORTH / CENTER / SOUTH = บน / กลาง (กินที่ที่เหลือ) / ล่าง
 */

package gui.main;

import java.awt.*;                          // BorderLayout, GridBagLayout, FlowLayout, Dimension, Cursor, Color, Font, Component
import java.io.IOException;                 // error ตอนอ่าน bookings.csv / movies.csv / schedule.csv ไม่ได้
import java.time.format.DateTimeFormatter;  // แปลงวันเวลาเป็นข้อความ "14/10/2026" / "14/10/2026 10:05"
import java.util.List;                      // รายการการจอง (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.swing.*;                       // JDialog, JPanel, JLabel, JButton, JScrollPane = กล่องเลื่อน, Box, BoxLayout
import javax.swing.border.*;                // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Booking;                       // การจอง 1 ครั้ง: รหัส, รอบ, ที่นั่ง, ราคา, เวลาที่จอง
import model.Showtime;                      // รอบฉายของการจอง: หนัง, วัน, เวลา, โรง
import model.User;                          // ลูกค้าที่ login อยู่
import service.BookingService;              // historyOf = ประวัติการจอง / showtimeOf = รอบของการจองนั้น

final class HistoryDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");             // 14/10/2026
    private static final DateTimeFormatter BOOKED_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");    // 14/10/2026 10:05
    private static final int MAX_HEIGHT = 480;    // สูงสุดของกล่อง (เท่าของเดิม) การจองเยอะกว่านี้ → เลื่อนดู
    private static final int MIN_WIDTH = 560;     // กว้างอย่างน้อย ไม่ให้กล่องแคบตอนไม่มีประวัติ

    /**
     * สร้างกล่องประวัติการจอง (ยังไม่โชว์ เรียก setVisible(true) เอง)
     *
     * @param owner    หน้าต่างที่กล่องนี้เด้งทับ (MainFrame) / null ได้ = กลางจอ
     * @param bookings BookingService ของโปรแกรม เช่น app.bookings()
     * @param user     ลูกค้าที่ login อยู่
     * @throws IOException ถ้าอ่านไฟล์การจอง / หนัง / ตารางฉายไม่ได้ (MainFrame เอา getMessage() ไปโชว์)
     * @throws IllegalArgumentException ถ้า bookings หรือ user เป็น null
     */
    HistoryDialog(JFrame owner, BookingService bookings, User user) throws IOException {
        super(owner, "Booking History", true);   // super(เจ้าของ, หัวกล่อง, modal)

        // ตรวจของที่ส่งเข้ามา (SC2 หน้า 9)
        if (bookings == null || user == null) {
            throw new IllegalArgumentException("bookings and user must not be null");
        }

        // historyOf(user) = การจองทั้งหมดของคนนี้ ใหม่สุดอยู่บน (BookingService.java บรรทัด 315)
        List<Booking> history = bookings.historyOf(user);

        // ทั้งกล่อง: BorderLayout บน = หัวข้อ / กลาง = รายการ / ล่าง = ปุ่ม Close
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MainTheme.BACKGROUND);                 // พื้นเทาอ่อน การ์ดขาวจะเด่น
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ① หัวข้อ เช่น "Booking History (tester9)"
        JLabel title = new JLabel("Booking History (" + user.username() + ")");
        title.setFont(MainTheme.FONT_DIALOG_TITLE);
        title.setForeground(MainTheme.TEXT_TITLE);
        title.setBorder(new EmptyBorder(0, 0, 12, 0));            // เว้นใต้หัวข้อ 12 px
        panel.add(title, BorderLayout.NORTH);

        // ② รายการ
        if (history.isEmpty()) {
            panel.add(emptyBox(), BorderLayout.CENTER);
        } else {
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));   // การ์ดเรียงบนลงล่าง
            list.setBackground(MainTheme.BACKGROUND);
            for (Booking booking : history) {
                // showtimeOf(booking) = รอบฉายของการจองนี้ / หาไม่เจอ → null (BookingService.java บรรทัด 348)
                list.add(card(booking, bookings.showtimeOf(booking)));
                list.add(Box.createRigidArea(new Dimension(0, 8)));   // เว้นระหว่างการ์ด 8 px
            }
            //    JScrollPane = กล่องเลื่อน: การจองเยอะจนสูงเกินกล่อง → เลื่อนลงดูได้
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createEmptyBorder());     // ไม่มีกรอบของกล่องเลื่อน
            scroll.getViewport().setBackground(MainTheme.BACKGROUND);
            scroll.getVerticalScrollBar().setUnitIncrement(12);      // หมุนเมาส์ 1 ครั้ง เลื่อน 12 px (ค่าปกติ 1 px ช้ามาก)
            panel.add(scroll, BorderLayout.CENTER);
        }

        // ③ ปุ่ม Close ชิดขวา (ขาว กรอบเทา)
        JButton close = new JButton("Close");
        close.setFont(MainTheme.FONT_LABEL);
        close.setBackground(MainTheme.CARD);
        close.setForeground(MainTheme.TEXT_LABEL);
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.INPUT_BORDER, 1, true),
                new EmptyBorder(6, 16, 6, 16)));
        close.addActionListener(e -> dispose());                    // กด → ปิดกล่อง
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        bottom.setOpaque(false);
        bottom.add(close);
        panel.add(bottom, BorderLayout.SOUTH);

        setContentPane(panel);
        pack();                                                       // ขนาดพอดีของข้างใน
        //    แต่กว้างไม่น้อยกว่า MIN_WIDTH และสูงไม่เกิน MAX_HEIGHT (ส่วนเกินเลื่อนดูในกล่องเลื่อน)
        setSize(Math.max(getWidth(), MIN_WIDTH), Math.min(getHeight(), MAX_HEIGHT));
        setLocationRelativeTo(owner);                                 // เปิดกลางหน้าต่างเจ้าของ
    }

    // ยังไม่เคยจอง → กล่องขาวมีข้อความกลางกล่อง
    private static JPanel emptyBox() {
        JPanel box = new JPanel(new GridBagLayout());                // GridBagLayout ใส่ชิ้นเดียว → อยู่กลาง
        box.setBackground(MainTheme.CARD);
        box.setBorder(new LineBorder(MainTheme.CARD_BORDER, 1, true));
        box.setPreferredSize(new Dimension(MIN_WIDTH - 40, 120));      // สูง 120 px ให้ดูเป็นกล่อง
        box.add(label("No booking history yet", MainTheme.FONT_TEXT, MainTheme.TEXT_MUTED));
        return box;
    }

    // การ์ดของการจอง 1 รายการ
    //   booking  = การจอง เช่น Booking[B3, tester9, 2026-10-14_1145_3, [C5, C6], 320, 2026-10-14T10:05]
    //   showtime = รอบของการจองนั้น / null = หาไม่เจอ
    private static JPanel card(Booking booking, Showtime showtime) {
        // ซ้าย 3 บรรทัด
        String movieText = showtime != null
                ? booking.id() + " - " + showtime.movie().title()                 // B3 - Spider-Man: Brand New Day
                : booking.id() + " - Showtime ID: " + booking.showtimeId();       // หาไม่เจอ → บอกรหัสรอบแทน
        String timeText = showtime != null
                ? "Date: " + showtime.date().format(DATE_FORMAT) + " Time: " + showtime.start() + " (Hall " + showtime.hall() + ")"
                : "Showtime: " + booking.showtimeId();
        //    String.join(", ", [C5, C6]) = "C5, C6"
        String seatText = "Seats: " + String.join(", ", booking.seatCodes()) + " | Booked: " + booking.bookedAt().format(BOOKED_FORMAT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));        // 3 บรรทัดเรียงบนลงล่าง
        left.setOpaque(false);
        left.add(label(movieText, MainTheme.FONT_LABEL, MainTheme.PRIMARY));
        left.add(Box.createRigidArea(new Dimension(0, 3)));
        left.add(label(timeText, MainTheme.FONT_SUBTITLE, MainTheme.TEXT_TITLE));
        left.add(Box.createRigidArea(new Dimension(0, 3)));
        left.add(label(seatText, MainTheme.FONT_SMALL, MainTheme.TEXT_MUTED));

        // ขวา: ราคาสีเขียว ตัวหนา 15 อยู่กลางแนวตั้ง (GridBagLayout ใส่ชิ้นเดียว → อยู่กลาง)
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(label(booking.totalPrice() + " THB", MainTheme.FONT_PRICE, MainTheme.SUCCESS));

        // ตัวการ์ด: BorderLayout(ห่างแนวนอน 8, แนวตั้ง 6) กลาง = ข้อมูล / ขวา = ราคา
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(MainTheme.CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.CARD_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));   // BoxLayout ยืดกว้างได้ แต่สูงไม่เกิน 100
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    // ป้ายข้อความ
    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
}