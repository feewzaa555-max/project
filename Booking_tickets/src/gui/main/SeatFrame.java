//SeatFrame <- หน้าเลือกที่นั่งของรอบที่ลูกค้ากด: เอาชิ้นส่วนในโฟลเดอร์ main มาประกอบ (เลือกที่นั่ง → ดูราคา → จอง → ตั๋ว)
/*
    หน้าตา (เลขตรงกับ ①–⑤ ในโค้ดข้างล่าง):
        ┌───────────────────────────────────────────────────────────────┐  ① หน้าต่าง 980 x 750
        │ MainTopBar (KU CINEMA / User / Membership / Balance / ...)     │  ② แถบบน (ตัวเดียวกับหน้า MainFrame)
        ├───────────────────────────────────────────────────────────────┤
        │ [← Back]  Spider-Man: Brand New Day                           │  ③ หัวรอบ (อยู่ในไฟล์นี้)
        │           Hall 3 · Date: 14/10/2026 · Time: 11:45 - 14:10      │
        │ ┌────────────── SeatMap ─────────────┐ ┌── OrderSummary ──┐   │  ④ ผังที่นั่ง + การ์ดสรุป
        │ │  SCREEN / A–F / คำอธิบายสี          │ │  ราคา + ปุ่มจอง   │   │
        │ └────────────────────────────────────┘ └──────────────────┘   │
        └───────────────────────────────────────────────────────────────┘

    ไฟล์นี้ทำแค่ 2 อย่าง:
      1. ประกอบหน้า: วางชิ้นส่วน แล้วส่ง "สิ่งที่ต้องทำ" (Runnable) ให้แต่ละชิ้น
      2. ทำงานที่ชิ้นส่วนบอกมา (⑤):
           SeatMap กดที่นั่ง                     → refresh()        อ่านบัญชี แล้วอัปเดตแถบบน + การ์ดสรุป
           OrderSummary กดปุ่มจอง                → handleBooking()  ถามยืนยัน → จองจริง → โชว์ตั๋ว
           TicketDialog กด Done / ✕              → backToMain()     กลับหน้าเลือกหนัง
           แถบบนกด Membership / Top-up / History / Logout → เปิดกล่อง / กลับหน้า Login
           กล่องสมัคร / เติมเงินเสร็จ               → refresh()        ยอดเงิน + ราคาใหม่ (สมัครแล้วลดทันที)
    หน้าตาของแต่ละส่วนอยู่ในไฟล์ของตัวเอง (MainTopBar, SeatMap, OrderSummary, TicketDialog, ...Dialog)

    เปิดหน้านี้ยังไง (MainFrame ตอนลูกค้ากดรอบ):
        new SeatFrame(user, app, รอบ).setVisible(true);   // อ่านที่นั่งที่จองแล้วไม่ได้ → throw IOException

    แทน gui/SeatFrame.java เดิม (982 บรรทัด) ต่างจากเดิม:
        แต่ละส่วนย้ายไปอยู่ไฟล์ของตัวเอง / ตัด main() และค่าปลอมตอน user / app เป็น null (เดิมสร้าง user "thanawat")
        ตรวจ user / app / showtime ไม่เป็น null และ user ต้องไม่ใช่ admin (แบบเดียวกับ MainFrame)
        อ่านที่นั่งที่จองแล้วไม่ได้ → throw ให้ MainFrame แจ้ง (เดิมทำเหมือนไม่มีใครจอง ที่นั่งที่จองแล้วไม่เทา)
        แถบบนใช้ MainTopBar (มี Membership / History / Logout เพิ่ม) ปุ่ม Back ย้ายมาอยู่หัวรอบ
        เดิม catch (Exception) แล้วเงียบ → ใหม่จับแค่ IOException ตอนอ่านไฟล์ แล้วแจ้ง (SC2 หน้า 11)

    คำที่ใช้บ่อยในไฟล์นี้:
        BorderLayout NORTH / CENTER / EAST = บน / กลาง (กินที่ที่เหลือ) / ขวา
        this::refresh = ส่งเมธอด refresh ไปให้ชิ้นส่วนเรียกทีหลัง (:: เพราะเมธอดไม่รับพารามิเตอร์)
 */

package gui.main;

import gui.login.LoginFrame;                // หน้า Login ตัวใหม่ เปิดตอนกด Logout
import java.awt.*;                          // BorderLayout, GridBagLayout, Cursor
import java.io.IOException;                 // error ตอนอ่าน / เขียนไฟล์บัญชี การจอง
import java.time.format.DateTimeFormatter;  // แปลงวันเวลาเป็นข้อความ "14/10/2026" / "11:45"
import java.util.ArrayList;                 // สร้างลิสต์รหัสที่นั่งในกล่องยืนยัน
import java.util.List;                      // รายการที่นั่ง (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import java.util.Set;                       // รหัสที่นั่งที่จองแล้ว เช่น {A1, F7}
import javax.swing.*;                       // JFrame, JPanel, JLabel, JButton, JOptionPane, Box, BoxLayout
import javax.swing.border.*;                // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Account;                       // บัญชีลูกค้า: เงิน + สมาชิกถึงวันไหน
import model.Booking;                       // การจองที่เพิ่งสำเร็จ ส่งให้ TicketDialog
import model.Seat;                          // ที่นั่ง 1 ที่ เช่น F7
import model.Showtime;                      // รอบที่ลูกค้ากดมา
import model.User;                          // ลูกค้าที่ login อยู่
import service.AppServices;                 // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

public final class SeatFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");   // 14/10/2026
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");        // 11:45

    private final User user;                 // ลูกค้าที่ login อยู่
    private final AppServices app;           // ของหลังบ้าน: accounts() / bookings() / layout() / clock()
    private final Showtime showtime;         // รอบที่กำลังเลือกที่นั่ง
    private final MainTopBar topBar;         // แถบบน (อัปเดตด้วย refresh)
    private final SeatMap seatMap;           // ผังที่นั่ง (ถาม selected() ว่าเลือกที่ไหนอยู่)
    private final OrderSummary summary;      // การ์ดสรุปราคา + ปุ่มจอง

    /**
     * เปิดหน้าเลือกที่นั่ง
     * public เพราะเรียกจากที่อื่นได้ในอนาคต (ตอนนี้ MainFrame ใน package เดียวกันเป็นคนเปิด)
     *
     * @param user     ลูกค้าที่ login แล้ว ห้าม null และต้องไม่ใช่ ADMIN
     * @param app      ของหลังบ้านชุดเดียวกับทั้งโปรแกรม ห้าม null
     * @param showtime รอบที่ลูกค้ากด เช่น 11:45 Hall 3 ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null หรือ user เป็น admin
     * @throws IOException ถ้าอ่านไฟล์การจอง (ที่นั่งที่จองแล้ว) ไม่ได้
     */
    public SeatFrame(User user, AppServices app, Showtime showtime) throws IOException {
        // ตรวจของที่ส่งเข้ามาก่อน ผิดตั้งแต่ต้น → หยุดเลย ไม่เปิดหน้าครึ่ง ๆ (SC2 หน้า 9)
        if (user == null || app == null || showtime == null) {
            throw new IllegalArgumentException("user, app and showtime must not be null");
        }
        if (user.isAdmin()) {   // admin ไม่ได้จองตั๋ว ถ้าเกิด แปลว่าโค้ดที่เรียกเปิดผิดหน้า
            throw new IllegalArgumentException("admin cannot open seat page: " + user.username());
        }
        this.user = user;
        this.app = app;
        this.showtime = showtime;

        // ① ตั้งค่าหน้าต่าง (เท่าของเดิม)
        setTitle("KU CINEMA - Seat Selection & Order Summary");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // กดกากบาท = ปิดโปรแกรม
        setSize(980, 750);
        setLocationRelativeTo(null);                       // เปิดกลางจอ
        setResizable(false);

        // ② แถบบน: ปุ่ม 4 ปุ่มส่งเมธอดของหน้านี้ไปให้
        topBar = new MainTopBar(user.username(), this::openMembership, this::openTopUp, this::openHistory, this::handleLogout);

        // ④ ผังที่นั่ง + การ์ดสรุป
        //    bookedSeats(รอบ) = รหัสที่นั่งที่จองแล้วของรอบนี้ เช่น {A1, F7} อ่านไม่ได้ → IOException ออกไปให้คนเปิดหน้า
        Set<String> booked = app.bookings().bookedSeats(showtime);
        seatMap = new SeatMap(app.layout(), booked, this::refresh);
        summary = new OrderSummary(this::handleBooking);

        // ประกอบ: หัวรอบอยู่บน / ผังกลาง / การ์ดสรุปขวา เว้นขอบ บน 12 ซ้าย 18 ล่าง 14 ขวา 18 (เท่าของเดิม)
        JPanel content = new JPanel(new BorderLayout(16, 12));   // ห่างกันแนวนอน 16 แนวตั้ง 12
        content.setBackground(MainTheme.BACKGROUND);
        content.setBorder(new EmptyBorder(12, 18, 14, 18));
        content.add(makeBanner(), BorderLayout.NORTH);
        content.add(seatMap, BorderLayout.CENTER);
        content.add(summary, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(MainTheme.BACKGROUND);
        root.add(topBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        refresh();   // ข้อมูลครั้งแรก: ยอดเงิน + การ์ดสรุป (ยังไม่เลือกที่นั่ง)
    }

    /**
     * ③ หัวรอบ: ปุ่ม Back ซ้าย / ชื่อหนัง + โรง วัน เวลา ความยาว
     *   เช่น "Hall 3  ·  Date: 14/10/2026  ·  Time: 11:45 - 14:10  (145 min)"
     *   ไม่รับพารามิเตอร์ ใช้ showtime ของหน้านี้
     *
     * @return JPanel แถบหัวรอบ = ปุ่ม ← Back + ชื่อหนัง + "Hall 3 · Date: 14/10/2026 · Time: 11:45 - 14:10 (145 min)"
     */
    private JPanel makeBanner() {
        JButton back = new JButton("← Back");
        back.setFont(MainTheme.FONT_LABEL);
        back.setForeground(MainTheme.PRIMARY);
        back.setBackground(MainTheme.CARD);
        back.setFocusPainted(false);
        back.setCursor(new Cursor(Cursor.HAND_CURSOR));
        back.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.INPUT_BORDER, 1, true),
                new EmptyBorder(6, 12, 6, 12)));
        back.addActionListener(e -> backToMain());

        JLabel title = new JLabel(showtime.movie().title());
        title.setFont(MainTheme.FONT_DIALOG_TITLE);
        title.setForeground(MainTheme.TEXT_TITLE);
        JLabel info = new JLabel("Hall " + showtime.hall()
                + "  ·  Date: " + showtime.date().format(DATE_FORMAT)
                + "  ·  Time: " + showtime.start().format(TIME_FORMAT) + " - " + showtime.end().format(TIME_FORMAT)
                + "  (" + showtime.movie().durationMinutes() + " min)");
        info.setFont(MainTheme.FONT_SMALL);
        info.setForeground(MainTheme.TEXT_MUTED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));   // ชื่อหนังบน / รายละเอียดล่าง
        text.setOpaque(false);
        text.add(title);
        text.add(Box.createVerticalStrut(3));
        text.add(info);

        JPanel banner = new JPanel(new BorderLayout(14, 0));     // ปุ่มกับข้อความห่างกัน 14 px
        banner.setBackground(MainTheme.CARD);
        banner.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.CARD_BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)));
        JPanel backHolder = new JPanel(new GridBagLayout());     // GridBagLayout เปล่า = ปุ่มอยู่กลางแนวตั้ง ไม่ยืดสูง
        backHolder.setOpaque(false);
        backHolder.add(back);
        banner.add(backHolder, BorderLayout.WEST);
        banner.add(text, BorderLayout.CENTER);
        return banner;
    }

    /**
     * ⑤ อ่านบัญชี 1 ครั้ง แล้วอัปเดตแถบบน + การ์ดสรุป
     * เรียกตอนเปิดหน้า, ทุกครั้งที่กดที่นั่ง, หลังเติมเงิน / สมัครสมาชิก
     * อ่านไม่ได้ → แถบบน "Balance: -" และบอก error (ปุ่มจองคงสถานะเดิม จองจริงก็ตรวจซ้ำอีกที)
     */
    private void refresh() {
        try {
            Account account = app.accounts().accountOf(user);
            topBar.showAccount(account, app.clock().now());
            summary.show(seatMap.selected(), account, app.clock().now());
        } catch (IOException e) {
            topBar.showAccountError();
            showError("Cannot retrieve account data: " + e.getMessage());
        }
    }

    /**
     * ⑤ กดปุ่มจอง: ถามยืนยัน → จองจริง → โชว์ตั๋ว (ปุ่มจองกดได้เฉพาะตอนเลือกแล้วและเงินพอ)
     */
    private void handleBooking() {
        List<Seat> seats = seatMap.selected();
        List<String> codes = new ArrayList<>();
        for (Seat seat : seats) {
            codes.add(seat.code());    // เช่น "A3"
        }
        try {
            int total = app.bookings().totalFor(user, seats);   // ราคาที่ต้องจ่าย (สมาชิกลดแล้ว) เช่น 324
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Confirm movie ticket booking?\n"
                            + "Movie: " + showtime.movie().title() + "\n"
                            + "Showtime: " + showtime.start().format(TIME_FORMAT) + " (Hall " + showtime.hall() + ")\n"
                            + "Seats: " + String.join(", ", codes) + " (" + seats.size() + " seats)\n"
                            + "Total Amount: " + total + " THB\n"
                            + "(Payment will be deducted immediately from your wallet and cannot be cancelled)",
                    "Confirm Booking", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {   // No / ปิดกล่อง = ยังไม่จอง อยู่หน้าเดิม
                return;
            }
            // book(...) = ตรวจกฎ (รอบยังไม่เริ่ม / ที่นั่งว่าง / เงินพอ) → บันทึกการจอง → ตัดเงิน → คืน Booking
            Booking booking = app.bookings().book(user, showtime, seats);
            new TicketDialog(this, booking, showtime, this::backToMain).setVisible(true);
        } catch (IllegalArgumentException e) {
            // ผิดกฎการจอง เช่น มีคนจองที่นั่งนี้ตัดหน้า / รอบเริ่มไปแล้ว (ข้อความจาก BookingService)
            JOptionPane.showMessageDialog(this, e.getMessage(), "Booking Failed", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            showError("System error connecting to data files: " + e.getMessage());
        }
    }

    /**
     * กลับหน้าเลือกหนัง (กด Back / ปิดตั๋ว)
     */
    private void backToMain() {
        new MainFrame(user, app).setVisible(true);
        dispose();
    }

    /**
     * ⑤ กล่องสมัครสมาชิก: เสร็จ → refresh (ราคาลดทันที) / เงินไม่พอแล้วกด Yes → openTopUp
     */
    private void openMembership() {
        try {
            new MembershipDialog(this, app.accounts(), user, this::refresh, this::openTopUp).setVisible(true);
        } catch (IOException e) {
            showError("Cannot retrieve membership info: " + e.getMessage());
        }
    }

    /**
     * ⑤ กล่องเติมเงิน: เสร็จ → refresh (ปุ่มจองอาจกดได้แล้ว)
     */
    private void openTopUp() {
        try {
            new TopUpDialog(this, app.accounts(), user, this::refresh).setVisible(true);
        } catch (IOException e) {
            showError("Cannot retrieve account data: " + e.getMessage());
        }
    }

    /**
     * ⑤ กล่องประวัติการจอง (ดูอย่างเดียว)
     */
    private void openHistory() {
        try {
            new HistoryDialog(this, app.bookings(), user).setVisible(true);
        } catch (IOException e) {
            showError("Cannot retrieve booking history: " + e.getMessage());
        }
    }

    /**
     * ⑤ Logout: ถามยืนยัน → ปิดหน้านี้ → หน้า Login ด้วย app ตัวเดิม
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame(app).setVisible(true);
        }
    }

    /**
     * กล่อง error (ไอคอน X สีแดง)
     *   ผลที่ได้: เด้งกล่อง error อย่างเดียว
     *
     * @param message ข้อความ error เช่น "Cannot retrieve account data: ..."
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}