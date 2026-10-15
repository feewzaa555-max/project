//MainFrame <- หน้าลูกค้า: เอาชิ้นส่วนในโฟลเดอร์ main มาประกอบเป็นหน้าเดียว (เลือกวัน → เลือกหนัง → เลือกรอบ)
/*
    หน้าตา (เลขตรงกับ ①–⑤ ในโค้ดข้างล่าง):
        ┌──────────────────────────────────────────────────────────────┐  ① หน้าต่าง 920 x 730
        │ MainTopBar (KU CINEMA / User / Membership / Balance / ...)    │  ② แถบบน
        ├──────────────────────────────────────────────────────────────┤
        │  ┌──────────────────── Select Showtime ───────────────────┐  │  ③ การ์ดกลาง
        │  │   Welcome tester9 | Please select a movie and showtime  │  │
        │  │   1. Date            DateBar     [Today 14/10]          │  │
        │  │   2. Movie           MovieRow    [โปสเตอร์ 5 เรื่อง]      │  │
        │  │   3. Showtimes: "Spider-Man..." · Today 14/10           │  │
        │  │                      ShowtimeRow [08:45][11:45][17:45]  │  │
        │  │         ← Click on a showtime to select seats           │  │
        │  └─────────────────────────────────────────────────────────┘  │
        └──────────────────────────────────────────────────────────────┘

    ไฟล์นี้ทำแค่ 2 อย่าง:
      1. ประกอบหน้า: วางชิ้นส่วน แล้วส่ง "สิ่งที่ต้องทำ" (Runnable) ให้แต่ละชิ้น
      2. ทำงานที่ชิ้นส่วนบอกมา (④ ⑤):
           DateBar / MovieRow เปลี่ยน     → refreshShowtimes()  โหลดรอบของวัน + หนังที่เลือก
           ShowtimeRow กดรอบ              → openSeats()         ไปหน้าเลือกที่นั่ง
           แถบบนกด Membership / Top-up / History / Logout → เปิดกล่อง / กลับหน้า Login
           กล่องสมัคร / เติมเงินเสร็จ        → refreshTopBar()     ยอดเงิน + สถานะสมาชิกใหม่
    หน้าตาของแต่ละส่วนอยู่ในไฟล์ของตัวเอง (MainTopBar, DateBar, MovieRow, ShowtimeRow, ...Dialog)

    เปิดหน้านี้ยังไง (LoginFrame หลังลูกค้า login / SeatFrame ตอนกด Back):
        new MainFrame(user, app).setVisible(true);

    แทน gui/MainFrame.java เดิม (1,241 บรรทัด) ต่างจากเดิม:
        แต่ละส่วนย้ายไปอยู่ไฟล์ของตัวเอง 8 ไฟล์ / ตัด constructor ที่ไม่มีใครใช้ 2 ตัว, main(), resolveDataPath
        ตรวจ user / app ไม่เป็น null และ user ต้องไม่ใช่ admin (แบบเดียวกับ AdminFrame ห้ามลูกค้า)
        เดิม catch (Exception) หลายที่ → ใหม่จับแค่ IOException ตอนอ่านไฟล์ (SC2 หน้า 11)
        Logout กลับหน้า Login ตัวใหม่ (gui/login/LoginFrame)

    คำที่ใช้บ่อยในไฟล์นี้:
        BorderLayout NORTH / CENTER = บน / กลาง (กินที่ที่เหลือ)
        BoxLayout(..., Y_AXIS)       = เรียงของจากบนลงล่างตามลำดับที่ add
        removeAll → add → revalidate → repaint = เปลี่ยนของในกล่อง (แบบเดียวกับ showLineup ใน AdminFrame)
 */

package gui.main;

import gui.login.LoginFrame;         // หน้า Login ตัวใหม่ เปิดตอนกด Logout
import java.awt.*;                   // BorderLayout, Component, Dimension, Font, Color
import java.io.IOException;          // error ตอนอ่านไฟล์หนัง / รอบฉาย / บัญชีไม่ได้
import java.time.LocalDate;          // วันที่ เช่น 2026-10-14
import java.util.List;               // รายการหนัง / รอบฉาย (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.swing.*;                // JFrame, JPanel, JLabel, JOptionPane, Box, BoxLayout
import javax.swing.border.*;         // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import model.Movie;                  // หนัง 1 เรื่อง
import model.Showtime;               // รอบฉาย 1 รอบ
import model.User;                   // ลูกค้าที่ login อยู่
import service.AppServices;          // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

public final class MainFrame extends JFrame {

    private final User user;                // ลูกค้าที่ login อยู่
    private final AppServices app;          // ของหลังบ้าน: movies() / accounts() / bookings() / clock() / dataFolder()
    private final MainTopBar topBar;        // แถบบน (อัปเดตยอดเงินด้วย refreshTopBar)
    private final DateBar dateBar;          // ปุ่มวันที่ (ถาม selected() ว่าวันไหน)
    private final MovieRow movieRow;        // แถวโปสเตอร์ (ถาม selected() ว่าเรื่องไหน)
    private final JLabel showtimesLabel;    // ป้าย "3. Showtimes: ..." เปลี่ยนตามวัน/หนังที่เลือก
    private final JPanel showtimeBox;       // กล่องใส่ ShowtimeRow 1 ตัว เปลี่ยนวัน/หนัง → ใส่ตัวใหม่
    private ShowtimeRow showtimeRow;        // แถวรอบฉายตัวที่โชว์อยู่ (ถาม picked() ตอนกดรอบ)

    /**
     * เปิดหน้าลูกค้า
     * public เพราะ LoginFrame (gui.login) อยู่คนละ package ต้อง new หน้านี้ (SeatFrame อยู่ gui.main เดียวกัน)
     *
     * @param user ลูกค้าที่ login แล้ว ห้าม null และต้องไม่ใช่ ADMIN
     * @param app  ของหลังบ้านชุดเดียวกับทั้งโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า user / app เป็น null หรือ user เป็น admin
     */
    public MainFrame(User user, AppServices app) {
        // ตรวจของที่ส่งเข้ามาก่อน ผิดตั้งแต่ต้น → หยุดเลย ไม่เปิดหน้าครึ่ง ๆ (SC2 หน้า 9)
        if (user == null || app == null) {
            throw new IllegalArgumentException("user and app must not be null");
        }
        if (user.isAdmin()) {   // admin มีหน้าของตัวเอง (AdminFrame) ถ้าเกิด แปลว่าโค้ดที่เรียกเปิดผิดหน้า
            throw new IllegalArgumentException("admin cannot open customer page: " + user.username());
        }
        this.user = user;
        this.app = app;

        // ① ตั้งค่าหน้าต่าง (เท่าของเดิม)
        setTitle("KU CINEMA - Movie Ticket Booking");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // กดกากบาท = ปิดโปรแกรม
        setSize(920, 730);
        setLocationRelativeTo(null);                       // เปิดกลางจอ
        setResizable(false);

        // ② แถบบน: ปุ่ม 4 ปุ่มส่งเมธอดของหน้านี้ไปให้ (:: เพราะเมธอดไม่รับพารามิเตอร์)
        topBar = new MainTopBar(user.username(), this::openMembership, this::openTopUp, this::openHistory, this::handleLogout);

        // ③ ชิ้นส่วนในการ์ดกลาง
        //    วันนี้ของนาฬิกาโปรแกรม (admin ตั้งเวลาไว้ → ใช้เวลานั้น)
        LocalDate today = app.clock().now().toLocalDate();
        dateBar = new DateBar(app.movies().bookableDates(), today, this::refreshShowtimes);
        movieRow = new MovieRow(loadMoviesToday(), app.dataFolder(), this::refreshShowtimes);

        showtimesLabel = sectionLabel("3. Showtimes");
        showtimeBox = new JPanel(new BorderLayout());      // BorderLayout ใส่ชิ้นเดียว → เต็มกล่อง
        showtimeBox.setOpaque(false);
        showtimeBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));   // เรียงบนลงล่าง
        card.setBackground(MainTheme.CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.CARD_BORDER, 1, true),
                new EmptyBorder(16, 28, 16, 28)));
        card.add(text("Select Showtime", MainTheme.FONT_HEADER, MainTheme.TEXT_TITLE));
        card.add(gap(4));
        card.add(text("Welcome " + user.username() + " | Please select a movie and showtime", MainTheme.FONT_SUBTITLE, MainTheme.TEXT_MUTED));
        card.add(gap(14));
        card.add(sectionLabel("1. Date"));
        card.add(gap(6));
        card.add(dateBar);
        card.add(gap(14));
        card.add(sectionLabel("2. Movie"));
        card.add(gap(6));
        card.add(movieRow);
        card.add(gap(14));
        card.add(showtimesLabel);
        card.add(gap(6));
        card.add(showtimeBox);
        card.add(gap(6));
        card.add(text("← Click on a showtime to select seats", MainTheme.FONT_SMALL, MainTheme.TEXT_MUTED));

        //    พื้นเทารอบการ์ด เว้นขอบ บน 14 ซ้าย 24 ล่าง 16 ขวา 24 (เท่าของเดิม)
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(MainTheme.BACKGROUND);
        center.setBorder(new EmptyBorder(14, 24, 16, 24));
        center.add(card, BorderLayout.NORTH);              // NORTH = การ์ดสูงตามของข้างใน ไม่ยืดลงล่าง

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(MainTheme.BACKGROUND);
        root.add(topBar, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        // ข้อมูลครั้งแรก
        refreshTopBar();
        refreshShowtimes();
    }

    /**
     * หนังวันนี้ / อ่านไฟล์ไม่ได้ → บอก error แล้วใช้รายการว่าง (MovieRow โชว์ "No movies today")
     *   ไม่รับพารามิเตอร์ ถาม app.movies().moviesToday
     *
     * @return List<Movie> หนังวันนี้ เช่น [Spider-Man, Pacific Rim, ...] / อ่านไฟล์ไม่ได้ → List ว่าง (แจ้ง error ก่อน)
     */
    private List<Movie> loadMoviesToday() {
        try {
            return app.movies().moviesToday();
        } catch (IOException e) {
            showError("Cannot load movies: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * ④ แถบบน: อ่านบัญชีแล้วโชว์ยอดเงิน + สถานะสมาชิก / อ่านไม่ได้ → "Balance: -"
     * เรียกตอนเปิดหน้า และหลังกล่องสมัคร / เติมเงินทำเสร็จ (ส่งไปเป็น this::refreshTopBar)
     */
    private void refreshTopBar() {
        try {
            topBar.showAccount(app.accounts().accountOf(user), app.clock().now());
        } catch (IOException e) {
            topBar.showAccountError();
        }
    }

    /**
     * ④ ป้าย + ปุ่มรอบฉาย ของวันและหนังที่เลือกอยู่
     * เรียกตอนเปิดหน้า และทุกครั้งที่ DateBar / MovieRow บอกว่าเปลี่ยน
     * เช่น เลือก Spider-Man วันนี้ 14/10 → "3. Showtimes: "Spider-Man: Brand New Day"  ·  Today 14/10" + ปุ่ม 3 รอบ
     */
    private void refreshShowtimes() {
        Movie movie = movieRow.selected();      // ไม่มีหนัง → null
        LocalDate date = dateBar.selected();
        showtimeBox.removeAll();                // เอาแถวรอบเก่าออก
        if (movie == null) {
            showtimesLabel.setText("3. Showtimes");
            showtimeRow = null;
        } else {
            //    วันเดียวกับวันนี้ของโปรแกรม → "Today 14/10" / วันอื่น → "15/10"
            String day = date.getDayOfMonth() + "/" + date.getMonthValue();
            if (date.equals(app.clock().now().toLocalDate())) {
                day = "Today " + day;
            }
            showtimesLabel.setText("3. Showtimes: \"" + movie.title() + "\"  ·  " + day);
            try {
                // showtimesOf(หนัง, วันที่) = รอบของหนังเรื่องนี้ในวันนั้น / now = ใช้ตัดสินว่ารอบไหนเริ่มแล้ว
                List<Showtime> showtimes = app.movies().showtimesOf(movie, date);
                showtimeRow = new ShowtimeRow(showtimes, app.clock().now(), this::openSeats);
                showtimeBox.add(showtimeRow);
            } catch (IOException e) {
                showtimeRow = null;
                showtimeBox.add(text("Cannot load showtimes: " + e.getMessage(), MainTheme.FONT_SMALL, MainTheme.ERROR));
            }
        }
        showtimeBox.revalidate();               // ของใหม่เกิดมา 0x0 → จัดขนาดให้
        showtimeBox.repaint();
    }

    /**
     * ⑤ ลูกค้ากดรอบ → เปิดหน้าเลือกที่นั่งของรอบนั้น แล้วปิดหน้านี้ (เหมือนของเดิม)
     *   SeatFrame อยู่ gui.main เดียวกัน ไม่ต้อง import / อ่านที่นั่งที่จองแล้วไม่ได้ → บอก error อยู่หน้าเดิม
     */
    private void openSeats() {
        try {
            new SeatFrame(user, app, showtimeRow.picked()).setVisible(true);
            dispose();
        } catch (IOException e) {
            showError("Cannot load booked seats: " + e.getMessage());
        }
    }

    /**
     * ⑤ กล่องสมัครสมาชิก: เสร็จ → refreshTopBar / เงินไม่พอแล้วกด Yes → openTopUp
     */
    private void openMembership() {
        try {
            new MembershipDialog(this, app.accounts(), user, this::refreshTopBar, this::openTopUp).setVisible(true);
        } catch (IOException e) {
            showError("Cannot retrieve membership info: " + e.getMessage());
        }
    }

    /**
     * ⑤ กล่องเติมเงิน: เสร็จ → refreshTopBar
     */
    private void openTopUp() {
        try {
            new TopUpDialog(this, app.accounts(), user, this::refreshTopBar).setVisible(true);
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
     * ⑤ Logout: ถามยืนยัน → ปิดหน้านี้ → หน้า Login ตัวใหม่ด้วย app ตัวเดิม (นาฬิกาและไฟล์ชุดเดิม)
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {    // No / ปิดกล่อง = อยู่หน้าเดิม
            dispose();
            new LoginFrame(app).setVisible(true);
        }
    }

    /**
     * กล่อง error (ไอคอน X สีแดง)
     *   ผลที่ได้: เด้งกล่อง error อย่างเดียว
     *
     * @param message ข้อความ error เช่น "Cannot load movies: movies.csv line 3: ..."
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * ป้ายหัวข้อส่วน เช่น "1. Date" ตัวหนา 12 สีเทาเข้ม อยู่กลาง
     *
     * @param text หัวข้อส่วน เช่น "1. Date"
     * @return JLabel ป้ายหัวข้อ ตัวหนา 12 สีเทาเข้ม อยู่กลาง
     */
    private static JLabel sectionLabel(String text) {
        return text(text, MainTheme.FONT_LABEL, MainTheme.TEXT_LABEL);
    }

    /**
     * ป้ายข้อความ อยู่กลางการ์ด
     *
     * @param text ข้อความ เช่น "Select Showtime"
     * @param font ฟอนต์
     * @param color สีตัวหนังสือ
     * @return JLabel ป้ายที่อยู่กลางการ์ด
     */
    private static JLabel text(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    /**
     * ก้อนว่างแนวตั้ง สูง height px
     *
     * @param height ความสูงช่องว่างเป็น px เช่น 14
     * @return Component ก้อนว่างกว้าง 0 สูง height (ใส่คั่นระหว่างของในการ์ด)
     */
    private static Component gap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}