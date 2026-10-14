package gui.SeatFrame;

import gui.MainFrame.MainFrame;
import gui.MainFrame.TopUpDialog;
import model.Booking;
import model.Movie;
import model.Seat;
import model.Showtime;
import model.User;
import service.AccountService;
import service.AppServices;
import service.BookingService;
import service.PriceCalculator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * หน้าต่าง GUI สำหรับเลือกที่นั่ง (Seat Selection) และสรุปราคา (Order Summary)
 * ทำหน้าที่เชื่อมโยงคอมโพเนนต์ย่อยต่างๆ:
 * - HallPanel: ผังที่นั่งโรงภาพยนตร์ (จอ SCREEN, ปุ่มที่นั่ง A ถึง F, Legend)
 * - OrderSummaryPanel: การ์ดสรุปรายการสั่งซื้อ จำนวนที่นั่ง ราคาสุทธิ และปุ่มยืนยัน
 * - TopUpDialog: หน้าต่างเติมเงินเข้ากระเป๋า
 * - ETicketDialog: หน้าต่างแสดงตั๋วอิเล็กทรอนิกส์จำลองหลังการจองสำเร็จ
 */
public class SeatFrame extends JFrame {

    // --- สีเฉพาะสำหรับ VIP Row F ---
    public static final Color COLOR_VIP_BG = new Color(254, 243, 199);      // Amber #FEF3C7
    public static final Color COLOR_VIP_BORDER = new Color(245, 158, 11);   // Amber #F59E0B
    public static final Color COLOR_VIP_TEXT = new Color(180, 83, 9);       // Amber #B45309

    private final User currentUser;
    private final AppServices appServices;
    private final Showtime showtime;

    private final BookingService bookingService;
    private final AccountService accountService;
    private final PriceCalculator priceCalculator;

    // ข้อมูลที่นั่งที่ถูกจองแล้ว
    private Set<String> bookedSeatCodes = new HashSet<>();

    // UI Components
    private JLabel topBalanceLabel;
    private JLabel memberBadgeLabel;
    private HallPanel hallPanel;
    private OrderSummaryPanel summaryPanel;

    /**
     * คอนสตรัคเตอร์เริ่มต้นหน้าต่าง SeatFrame
     * ดึงเซอร์วิสต่างๆ โหลดข้อมูลที่นั่งที่ถูกจองแล้ว สร้างหน้าจอ และอัปเดตสรุปราคา
     *
     * @param user ผู้ใช้งานปัจจุบัน
     * @param appServices เซอร์วิสส่วนกลางของระบบ
     * @param showtime ข้อมูลรอบฉายที่ถูกเลือกมาจากหน้าหลัก
     */
    public SeatFrame(User user, AppServices appServices, Showtime showtime) {
        this.currentUser = (user != null) ? user : new User("thanawat", "password123");
        this.appServices = (appServices != null) ? appServices : AppServices.create();
        this.showtime = showtime;

        this.bookingService = this.appServices.bookings();
        this.accountService = this.appServices.accounts();
        this.priceCalculator = new PriceCalculator();

        loadBookedSeats();
        initUI();
        updateSummary();
    }

    /**
     * ดึงข้อมูลที่นั่งที่ถูกจองแล้วจาก BookingService
     */
    private void loadBookedSeats() {
        if (bookingService != null && showtime != null) {
            try {
                this.bookedSeatCodes = bookingService.bookedSeats(showtime);
            } catch (IOException e) {
                this.bookedSeatCodes = new HashSet<>();
                JOptionPane.showMessageDialog(this,
                        "Error loading booked seats: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * สร้างหน้าจอหลักสำหรับเลือกที่นั่ง
     */
    private void initUI() {
        setTitle("KU CINEMA - Seat Selection & Order Summary");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 750);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(MainFrame.COLOR_BG);

        // 1. แถบด้านบน (Top Bar)
        rootPanel.add(createTopBar(), BorderLayout.NORTH);

        // 2. พื้นที่เนื้อหาหลัก แบ่งซ้าย (ผังที่นั่ง) และขวา (สรุปราคาและจำนวน)
        JPanel contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setBackground(MainFrame.COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(12, 18, 14, 18));

        // 2.1 แบนเนอร์ข้อมูลรอบฉายด้านบนสุดของเนื้อหา
        contentPanel.add(createMovieBanner(), BorderLayout.NORTH);

        // 2.2 ฝั่งซ้าย: ผังที่นั่งในโรง (HallPanel)
        hallPanel = new HallPanel(
                appServices.layout(),
                bookedSeatCodes,
                this::updateSummary
        );
        contentPanel.add(hallPanel, BorderLayout.CENTER);

        // 2.3 ฝั่งขวา: สรุปราคาและจำนวน (OrderSummaryPanel)
        summaryPanel = new OrderSummaryPanel(
                priceCalculator,
                this::handleBooking
        );
        contentPanel.add(summaryPanel, BorderLayout.EAST);

        rootPanel.add(contentPanel, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    /**
     * แถบด้านบน: โลโก้, ปุ่มย้อนกลับ, ผู้ใช้, สถานะสมาชิก, เงินคงเหลือ, ปุ่มเติมเงิน
     */
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, MainFrame.COLOR_CARD_BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ซ้าย: KU CINEMA พร้อมปุ่มย้อนกลับ
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JButton backBtn = new JButton("← Back");
        backBtn.setFont(MainFrame.FONT_LABEL);
        backBtn.setBackground(Color.WHITE);
        backBtn.setForeground(MainFrame.COLOR_PRIMARY);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        backBtn.addActionListener(e -> returnToMain());

        JLabel brandLabel = new JLabel("KU CINEMA");
        brandLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        brandLabel.setForeground(MainFrame.COLOR_PRIMARY);

        leftPanel.add(backBtn);
        leftPanel.add(brandLabel);

        // ขวา: ข้อมูลผู้ใช้, สถานะสมาชิก, เงินคงเหลือ, เติมเงิน
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel userLabel = new JLabel("User: " + currentUser.username());
        userLabel.setFont(MainFrame.FONT_LABEL);
        userLabel.setForeground(MainFrame.COLOR_TEXT_TITLE);

        boolean isMember = false;
        try {
            isMember = (accountService != null && accountService.isMember(currentUser));
        } catch (Exception ignored) {
        }

        memberBadgeLabel = new JLabel(isMember ? "Member (10% off)" : "Standard");
        memberBadgeLabel.setFont(MainFrame.FONT_SMALL);
        memberBadgeLabel.setForeground(isMember ? COLOR_VIP_TEXT : MainFrame.COLOR_TEXT_MUTED);
        memberBadgeLabel.setOpaque(true);
        memberBadgeLabel.setBackground(isMember ? COLOR_VIP_BG : new Color(243, 244, 246));
        memberBadgeLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        int balance = 0;
        try {
            if (accountService != null) balance = accountService.accountOf(currentUser).balance();
        } catch (Exception ignored) {
        }

        topBalanceLabel = new JLabel("Balance: " + balance + " THB");
        topBalanceLabel.setFont(MainFrame.FONT_LABEL);
        topBalanceLabel.setForeground(new Color(5, 150, 105));

        JButton topUpBtn = new JButton("+ Top-up");
        topUpBtn.setFont(MainFrame.FONT_SMALL);
        topUpBtn.setBackground(new Color(16, 185, 129));
        topUpBtn.setForeground(Color.BLACK);
        topUpBtn.setFocusPainted(false);
        topUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topUpBtn.setBorder(new CompoundBorder(
                new LineBorder(new Color(16, 185, 129), 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        topUpBtn.addActionListener(e -> showTopUpDialog());

        rightPanel.add(userLabel);
        rightPanel.add(memberBadgeLabel);
        rightPanel.add(topBalanceLabel);
        rightPanel.add(topUpBtn);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    /**
     * แถบข้อมูลภาพยนตร์และรอบฉาย
     */
    private JPanel createMovieBanner() {
        JPanel banner = new JPanel(new BorderLayout(14, 0));
        banner.setBackground(Color.WHITE);
        banner.setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(10, 16, 10, 16)));

        Movie m = showtime.movie();
        String title = (m != null) ? m.title() : "Movie";
        int duration = (m != null) ? m.durationMinutes() : 0;
        String dateStr = showtime.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeStr = showtime.start() + " - " + showtime.end();

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(MainFrame.FONT_SUBTITLE);
        titleLbl.setForeground(MainFrame.COLOR_TEXT_TITLE);

        JLabel infoLbl = new JLabel("Hall " + showtime.hall() + "  ·  Date: " + dateStr + "  ·  Time: " + timeStr + "  (" + duration + " min)");
        infoLbl.setFont(MainFrame.FONT_SMALL);
        infoLbl.setForeground(MainFrame.COLOR_TEXT_MUTED);

        left.add(titleLbl);
        left.add(Box.createRigidArea(new Dimension(0, 3)));
        left.add(infoLbl);

        banner.add(left, BorderLayout.CENTER);
        return banner;
    }

    /**
     * อัปเดตข้อมูลสรุปราคาและกระเป๋าเงิน
     */
    private void updateSummary() {
        if (summaryPanel == null || hallPanel == null) return;

        List<Seat> selectedSeats = hallPanel.getSelectedSeats();

        boolean isMember = false;
        int currentBalance = 0;
        try {
            if (accountService != null) {
                isMember = accountService.isMember(currentUser);
                currentBalance = accountService.accountOf(currentUser).balance();
            }
        } catch (Exception ignored) {
        }

        if (topBalanceLabel != null) {
            topBalanceLabel.setText("Balance: " + currentBalance + " THB");
        }

        summaryPanel.updateSummary(selectedSeats, currentBalance, isMember);
    }

    /**
     * จัดการขั้นตอนการยืนยันการจองตั๋วภาพยนตร์
     */
    private void handleBooking() {
        List<Seat> selectedSeats = hallPanel.getSelectedSeats();
        if (selectedSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least 1 seat", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isMember = false;
        try {
            if (accountService != null) isMember = accountService.isMember(currentUser);
        } catch (Exception ignored) {
        }
        int netTotal = priceCalculator.totalOf(selectedSeats, isMember);

        List<String> codes = selectedSeats.stream().map(Seat::code).toList();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm movie ticket booking?\n"
                        + "Movie: " + showtime.movie().title() + "\n"
                        + "Showtime: " + showtime.start() + " (Hall " + showtime.hall() + ")\n"
                        + "Seats: " + String.join(", ", codes) + " (" + selectedSeats.size() + " seats)\n"
                        + "Total Amount: " + netTotal + " THB\n"
                        + "(Payment will be deducted immediately from your wallet and cannot be cancelled)",
                "Confirm Booking",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            Booking booking = bookingService.book(currentUser, showtime, selectedSeats);
            ETicketDialog ticketDialog = new ETicketDialog(this, booking, showtime, this::returnToMain);
            ticketDialog.setVisible(true);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking Failed", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "System error connecting to data files: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * เปิดหน้าต่างเติมเงิน (ใช้ TopUpDialog ร่วมกับหน้าหลัก)
     */
    private void showTopUpDialog() {
        if (accountService == null) return;
        TopUpDialog dialog = new TopUpDialog(this, currentUser, accountService, this::updateSummary);
        dialog.setVisible(true);
    }

    /**
     * ปิดหน้านี้แล้วกลับไปยังหน้าต่างหลัก MainFrame
     */
    private void returnToMain() {
        this.dispose();
        MainFrame mainFrame = new MainFrame(currentUser, appServices);
        mainFrame.setVisible(true);
    }

    /**
     * เมธอด main ให้สามารถทดสอบรันหน้าต่าง SeatFrame นี้ได้โดยตรง
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                AppServices services = AppServices.create();
                User user = new User("thanawat", "password123");
                List<Movie> movies = services.movies().movies();
                if (!movies.isEmpty()) {
                    Movie m = movies.get(0);
                    List<Showtime> sts = services.movies().showtimesOf(m, services.movies().bookableDates().get(0));
                    if (!sts.isEmpty()) {
                        SeatFrame frame = new SeatFrame(user, services, sts.get(0));
                        frame.setVisible(true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
