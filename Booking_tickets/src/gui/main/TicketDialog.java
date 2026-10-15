//TicketDialog <- ตั๋วที่เด้งขึ้นหลังจองสำเร็จ (หน้าเลือกที่นั่ง SeatFrame เปิด) กด Done → กลับหน้าเลือกหนัง
/*
    หน้าตา:
              KU CINEMA - E-TICKET
           Payment Successful (PAID)
                Booking ID: B12
        ─────────────────────────────────
        Movie:          Spider-Man: Brand New Day
        Date:           14/10/2026
        Time:           11:45 - 14:10 (Hall 3)
        Seats:          A3, F7 (2 seats)          ← ยาวเกินขึ้นบรรทัดใหม่เอง
        Total Price:    324 THB
        Customer:       tester9
        Booking Time:   14/10/2026 10:00
        ─────────────────────────────────
             [ Done (Return to Main) ]

    กล่องนี้แค่ "โชว์" การจองที่ทำเสร็จแล้ว ไม่อ่าน / เขียนไฟล์
        กด Done หรือกด ✕ ปิดกล่อง → onDone.run() (SeatFrame ส่งมา เช่น กลับหน้า MainFrame)

    ใช้ยังไง (ใน SeatFrame หลัง app.bookings().book(...) สำเร็จ):
        new TicketDialog(this, booking, showtime, () -> backToMain()).setVisible(true);

    ย้ายมาจาก gui/SeatFrame.java เดิม บรรทัด 723–812 (showETicketDialog, addTicketRow) ต่างจากเดิม:
        กด ✕ ปิด = เหมือนกด Done (เดิมกด ✕ แล้วค้างหน้าเลือกที่นั่ง ที่นั่งที่เพิ่งจองยังไม่เทา กดจองซ้ำได้)
        pack() ให้พอดีของข้างใน (เดิม setSize 440 x 480 ตายตัว) / ที่นั่งเยอะขึ้นบรรทัดใหม่ ไม่ล้นกล่อง
        ตรวจว่า booking กับ showtime เป็นรอบเดียวกัน
        สีและฟอนต์จาก MainTheme

    คำที่ใช้บ่อยในไฟล์นี้:
        modal (true ใน super) = เปิดกล่องแล้วกดหน้าข้างหลังไม่ได้ จนกว่าจะปิดกล่อง
        BorderLayout WEST / EAST = ชิดซ้าย / ชิดขวา (หัวข้ออยู่ซ้าย ค่าอยู่ขวา)
        WindowAdapter.windowClosing = สิ่งที่ทำตอนกด ✕ มุมกล่อง
 */

package gui.main;

import java.awt.*;                          // BorderLayout, Dimension, Cursor, Component, Color, Font
import java.awt.event.WindowAdapter;        // ตัวรับเหตุการณ์ของหน้าต่าง ใช้ดักตอนกด ✕
import java.awt.event.WindowEvent;          // ข้อมูลเหตุการณ์ของหน้าต่าง (windowClosing รับเป็นพารามิเตอร์)
import java.time.format.DateTimeFormatter;  // แปลงวันเวลาเป็นข้อความ "14/10/2026" / "14/10/2026 10:00"
import javax.swing.*;                       // JDialog, JPanel, JLabel, JButton, JSeparator, Box, BoxLayout
import javax.swing.border.*;                // LineBorder = เส้นรอบ, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import model.Booking;                       // การจอง 1 ครั้ง: รหัส, คนจอง, ที่นั่ง, ราคา, เวลาที่จอง
import model.Showtime;                      // รอบที่จอง: หนัง, โรง, วันที่, เวลาเริ่ม-จบ

final class TicketDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");             // 14/10/2026
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");                  // 11:45
    private static final DateTimeFormatter BOOKED_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");    // 14/10/2026 10:00
    private static final int VALUE_WIDTH = 220;   // ความกว้างช่องค่าทางขวา เกินนี้ขึ้นบรรทัดใหม่ (เช่น ที่นั่ง 10 ที่)

    private final Runnable onDone;   // สิ่งที่ต้องทำหลังปิดตั๋ว (SeatFrame ส่งมา) เช่น () -> backToMain()

    /**
     * สร้างกล่องตั๋ว (ยังไม่โชว์ ต้องเรียก setVisible(true))
     *
     * @param owner    หน้าต่างแม่ เช่น SeatFrame (this) กล่องจะเด้งกลางหน้านี้
     * @param booking  การจองที่เพิ่งสำเร็จ เช่น app.bookings().book(...) ห้าม null
     * @param showtime รอบของการจองนี้ ห้าม null และต้องเป็นรอบเดียวกับ booking
     * @param onDone   คำสั่งที่ทำหลังกด Done หรือ ✕ เช่น () -> backToMain() ห้าม null
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onDone.run()
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null หรือ booking ไม่ใช่ของรอบนี้
     */
    TicketDialog(JFrame owner, Booking booking, Showtime showtime, Runnable onDone) {
        super(owner, "Electronic Ticket (E-Ticket)", true);   // true = modal

        // ① ตรวจของที่ส่งเข้ามา (SC2 หน้า 9 Defensive Programming)
        if (booking == null || showtime == null || onDone == null) {
            throw new IllegalArgumentException("booking, showtime and onDone must not be null");
        }
        // showtime.id() = รหัสรอบ เช่น "2026-10-14_H3_1145" ต้องตรงกับรอบที่เก็บใน booking
        if (!booking.showtimeId().equals(showtime.id())) {
            throw new IllegalArgumentException("booking " + booking.id() + " is not for showtime " + showtime.id());
        }
        this.onDone = onDone;

        // ② ตั้งค่ากล่อง: กด ✕ → ไม่ปิดเอง แต่ไปทำ finish() เหมือนกด Done
        setResizable(false);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {   // ทำงานตอนกด ✕ มุมกล่อง
                finish();
            }
        });

        // ③ ตัวตั๋ว: พื้นขาว เรียงบนลงล่าง เว้นขอบ บน 20 ซ้าย 24 ล่าง 20 ขวา 24 (เท่าของเดิม)
        JPanel ticket = new JPanel();
        ticket.setLayout(new BoxLayout(ticket, BoxLayout.Y_AXIS));
        ticket.setBackground(MainTheme.CARD);
        ticket.setBorder(new EmptyBorder(20, 24, 20, 24));

        ticket.add(centered("KU CINEMA - E-TICKET", MainTheme.FONT_DIALOG_TITLE, MainTheme.PRIMARY));
        ticket.add(gap(4));
        ticket.add(centered("Payment Successful (PAID)", MainTheme.FONT_LABEL, MainTheme.SUCCESS));
        ticket.add(gap(10));
        ticket.add(centered("Booking ID: " + booking.id(), MainTheme.FONT_PRICE, MainTheme.TEXT_TITLE));
        ticket.add(gap(12));
        ticket.add(divider());
        ticket.add(gap(12));

        // ④ ข้อมูลการจอง หัวข้อซ้าย ค่าขวา
        //    String.join(", ", ...) = ต่อรหัสคั่นด้วย ", " เช่น [A3, F7] → "A3, F7"
        ticket.add(row("Movie:", showtime.movie().title()));
        ticket.add(row("Date:", showtime.date().format(DATE_FORMAT)));
        ticket.add(row("Time:", showtime.start().format(TIME_FORMAT) + " - " + showtime.end().format(TIME_FORMAT)
                + " (Hall " + showtime.hall() + ")"));
        ticket.add(row("Seats:", String.join(", ", booking.seatCodes()) + " (" + booking.seatCodes().size() + " seats)"));
        ticket.add(row("Total Price:", booking.totalPrice() + " THB"));
        ticket.add(row("Customer:", booking.username()));
        ticket.add(row("Booking Time:", booking.bookedAt().format(BOOKED_FORMAT)));

        ticket.add(gap(8));
        ticket.add(divider());
        ticket.add(gap(14));

        // ⑤ ปุ่ม Done พื้นฟ้า อยู่กลาง
        JButton done = new JButton("Done (Return to Main)");
        done.setFont(MainTheme.FONT_TEXT.deriveFont(Font.BOLD));   // ตัวหนาขนาด 13 เท่าของเดิม
        done.setBackground(MainTheme.PRIMARY);
        done.setForeground(MainTheme.BUTTON_TEXT);
        done.setFocusPainted(false);
        done.setCursor(new Cursor(Cursor.HAND_CURSOR));
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.PRIMARY, 1, true),
                new EmptyBorder(8, 20, 8, 20)));
        done.addActionListener(e -> finish());
        ticket.add(done);

        setContentPane(ticket);
        pack();                          // ขนาดพอดีของข้างใน
        setLocationRelativeTo(owner);    // เด้งกลางหน้าต่างแม่
    }

    // ปิดตั๋ว แล้วทำคำสั่งที่ SeatFrame ส่งมา (กด Done และกด ✕ มาที่นี่ทั้งคู่)
    private void finish() {
        dispose();
        onDone.run();
    }

    // แถว "หัวข้อ ... ค่า" เช่น Movie: ... Spider-Man
    //   ค่าใช้ <table width=220> ยาวเกินขึ้นบรรทัดใหม่ และชิดขวา (align=right)
    private static JPanel row(String title, String value) {
        JLabel left = new JLabel(title);
        left.setFont(MainTheme.FONT_SMALL);
        left.setForeground(MainTheme.TEXT_MUTED);
        left.setVerticalAlignment(SwingConstants.TOP);   // ค่าหลายบรรทัด หัวข้ออยู่บรรทัดแรก

        JLabel right = new JLabel("<html><table width=" + VALUE_WIDTH + " cellpadding=0 cellspacing=0><tr>"
                + "<td align=right>" + value + "</td></tr></table></html>");
        right.setFont(MainTheme.FONT_LABEL);
        right.setForeground(MainTheme.TEXT_TITLE);

        JPanel row = new JPanel(new BorderLayout(8, 0));   // หัวข้อกับค่าห่างกันอย่างน้อย 8 px
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));        // เว้นบนล่างแถวละ 2 px
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ข้อความอยู่กลาง
    private static JLabel centered(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // เส้นคั่นบาง ๆ กว้างเต็มกล่อง
    private static JSeparator divider() {
        JSeparator line = new JSeparator();
        line.setForeground(MainTheme.CARD_BORDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        return line;
    }

    // ก้อนว่างแนวตั้ง สูง height px
    private static Component gap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}