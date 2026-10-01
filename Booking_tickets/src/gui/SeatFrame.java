package gui;

import model.Account;
import model.Booking;
import model.HallLayout;
import model.Movie;
import model.Seat;
import model.SeatType;
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
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * หน้าต่าง GUI สำหรับเลือกที่นั่ง (Seat Selection) และสรุปราคา (Order Summary)
 * ออกแบบตามผังโรงและกติกาใน README:
 * - ผัง 6 แถว (A ถึง F) แถวละ 10 ที่นั่ง (แถว A–E ธรรมดา 160 บาท, แถว F VIP 200 บาท)
 * - ทางเดินอยู่หลังที่นั่งหมายเลข 5
 * - สมาชิกลด 10% (ธรรมดา 144 บาท, VIP 180 บาท)
 * - แสดงจำนวนที่นั่งที่เลือก, รายละเอียดราคา, ส่วนลดสมาชิก, ยอดสุทธิ และสถานะเงินในกระเป๋า
 * - จองสำเร็จแสดงตั๋วจำลอง (Simulated E-Ticket) พร้อมตัดเงินและบันทึกลง bookings.csv
 */
public class SeatFrame extends JFrame {

    private final User currentUser;
    private final AppServices appServices;
    private final Showtime showtime;

    private final HallLayout hallLayout;
    private final BookingService bookingService;
    private final AccountService accountService;
    private final PriceCalculator priceCalculator;

    // ข้อมูลที่นั่ง
    private Set<String> bookedSeatCodes = new HashSet<>();
    private final List<Seat> selectedSeats = new ArrayList<>();
    private final Map<String, JButton> seatButtons = new HashMap<>();

    // UI Components แถบบน
    private JLabel topBalanceLabel;
    private JLabel memberBadgeLabel;

    // UI Components สรุปรายการขวา (Order Summary)
    private JLabel selectedSeatsTextLabel;
    private JLabel seatCountLabel;
    private JLabel standardCountLabel;
    private JLabel vipCountLabel;
    private JLabel regularTotalLabel;
    private JLabel memberStatusLabel;
    private JLabel discountLabel;
    private JLabel netTotalLabel;
    private JLabel currentBalanceLabel;
    private JLabel balanceAfterLabel;
    private JLabel balanceWarningLabel;
    private JButton confirmBookingButton;

    // สีและฟอนต์มาตรฐานชุดเดียวกับทั้งระบบ
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue #2563EB
    private static final Color COLOR_BG = new Color(245, 247, 250);          // Gray #F5F7FA
    private static final Color COLOR_CARD_BORDER = new Color(229, 231, 235); // Border #E5E7EB
    private static final Color COLOR_INPUT_BORDER = new Color(209, 213, 219);// Border #D1D5DB
    private static final Color COLOR_TEXT_TITLE = new Color(17, 24, 39);     // Dark #111827
    private static final Color COLOR_TEXT_LABEL = new Color(55, 65, 81);     // Dark #374151
    private static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);  // Muted #6B7280
    private static final Color COLOR_EMERALD = new Color(16, 185, 129);     // Green #10B981
    private static final Color COLOR_VIP_BG = new Color(254, 243, 199);      // Amber #FEF3C7
    private static final Color COLOR_VIP_BORDER = new Color(245, 158, 11);   // Amber #F59E0B
    private static final Color COLOR_VIP_TEXT = new Color(180, 83, 9);       // Amber #B45309

    private final Font fontHeader = new Font("Tahoma", Font.BOLD, 18);
    private final Font fontSubheader = new Font("Tahoma", Font.BOLD, 14);
    private final Font fontLabel = new Font("Tahoma", Font.BOLD, 12);
    private final Font fontText = new Font("Tahoma", Font.PLAIN, 12);
    private final Font fontSmall = new Font("Tahoma", Font.PLAIN, 11);

    public SeatFrame(User user, AppServices appServices, Showtime showtime) {
        this.currentUser = (user != null) ? user : new User("thanawat", "password123");
        this.appServices = (appServices != null) ? appServices : AppServices.create();
        this.showtime = showtime;

        this.hallLayout = this.appServices.layout();
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
        rootPanel.setBackground(COLOR_BG);

        // 1. แถบด้านบน (Top Bar)
        rootPanel.add(createTopBar(), BorderLayout.NORTH);

        // 2. พื้นที่เนื้อหาหลัก แบ่งซ้าย (ผังที่นั่ง) และขวา (สรุปราคาและจำนวน)
        JPanel contentPanel = new JPanel(new BorderLayout(16, 0));
        contentPanel.setBackground(COLOR_BG);
        contentPanel.setBorder(new EmptyBorder(12, 18, 14, 18));

        // 2.1 แบนเนอร์ข้อมูลรอบฉายด้านบนสุดของเนื้อหา
        contentPanel.add(createMovieBanner(), BorderLayout.NORTH);

        // 2.2 ฝั่งซ้าย: ผังที่นั่งในโรง
        contentPanel.add(createHallPanel(), BorderLayout.CENTER);

        // 2.3 ฝั่งขวา: สรุปราคาและจำนวน (Order Summary)
        contentPanel.add(createSummaryPanel(), BorderLayout.EAST);

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
                new MatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ซ้าย: KU CINEMA พร้อมปุ่มย้อนกลับ
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JButton backBtn = new JButton("← Back");
        backBtn.setFont(fontLabel);
        backBtn.setBackground(Color.WHITE);
        backBtn.setForeground(COLOR_PRIMARY);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(new CompoundBorder(
                new LineBorder(COLOR_INPUT_BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        backBtn.addActionListener(e -> returnToMain());

        JLabel brandLabel = new JLabel("KU CINEMA");
        brandLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        brandLabel.setForeground(COLOR_PRIMARY);

        leftPanel.add(backBtn);
        leftPanel.add(brandLabel);

        // ขวา: ข้อมูลผู้ใช้, สถานะสมาชิก, เงินคงเหลือ, เติมเงิน
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel userLabel = new JLabel("User: " + currentUser.username());
        userLabel.setFont(fontLabel);
        userLabel.setForeground(COLOR_TEXT_TITLE);

        boolean isMember = false;
        try {
            isMember = (accountService != null && accountService.isMember(currentUser));
        } catch (Exception ignored) {
        }

        memberBadgeLabel = new JLabel(isMember ? "Member (10% off)" : "Standard");
        memberBadgeLabel.setFont(fontSmall);
        memberBadgeLabel.setForeground(isMember ? COLOR_VIP_TEXT : COLOR_TEXT_MUTED);
        memberBadgeLabel.setOpaque(true);
        memberBadgeLabel.setBackground(isMember ? COLOR_VIP_BG : new Color(243, 244, 246));
        memberBadgeLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        int balance = 0;
        try {
            if (accountService != null) balance = accountService.accountOf(currentUser).balance();
        } catch (Exception ignored) {
        }

        topBalanceLabel = new JLabel("Balance: " + balance + " THB");
        topBalanceLabel.setFont(fontLabel);
        topBalanceLabel.setForeground(COLOR_EMERALD);

        JButton topUpBtn = new JButton("+ Top-up");
        topUpBtn.setFont(fontSmall);
        topUpBtn.setBackground(COLOR_EMERALD);
        topUpBtn.setForeground(Color.BLACK);
        topUpBtn.setFocusPainted(false);
        topUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topUpBtn.setBorder(new CompoundBorder(
                new LineBorder(COLOR_EMERALD, 1, true),
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
                new LineBorder(COLOR_CARD_BORDER, 1, true),
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
        titleLbl.setFont(fontSubheader);
        titleLbl.setForeground(COLOR_TEXT_TITLE);

        JLabel infoLbl = new JLabel("Hall " + showtime.hall() + "  ·  Date: " + dateStr + "  ·  Time: " + timeStr + "  (" + duration + " min)");
        infoLbl.setFont(fontSmall);
        infoLbl.setForeground(COLOR_TEXT_MUTED);

        left.add(titleLbl);
        left.add(Box.createRigidArea(new Dimension(0, 3)));
        left.add(infoLbl);

        banner.add(left, BorderLayout.CENTER);
        return banner;
    }

    /**
     * การ์ดผังที่นั่งโรงภาพยนตร์ (จอภาพ, แถว A ถึง F, ทางเดิน, สัญลักษณ์คำอธิบาย)
     */
    private JPanel createHallPanel() {
        JPanel hallCard = new JPanel(new BorderLayout(0, 10));
        hallCard.setBackground(Color.WHITE);
        hallCard.setBorder(new CompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        // 1. จอภาพ (SCREEN) โค้งด้านบน
        JPanel screenBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 60;
                int h = 18;
                int x = 30;
                int y = 4;

                g2.setColor(new Color(239, 246, 255)); // Light Blue
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setColor(COLOR_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(x, y, w, h, 10, 10);

                g2.setColor(COLOR_PRIMARY);
                g2.setFont(new Font("Tahoma", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String text = "SCREEN";
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        screenBar.setPreferredSize(new Dimension(540, 30));
        screenBar.setOpaque(false);
        hallCard.add(screenBar, BorderLayout.NORTH);

        // 2. ผังที่นั่ง 6 แถว (A ถึง F) เรียงติดกันไม่มีเว้นตรงกลาง
        JPanel gridContainer = new JPanel();
        gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
        gridContainer.setOpaque(false);

        for (char row : hallLayout.rows()) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
            rowPanel.setOpaque(false);

            // ป้ายชื่อแถวฝั่งซ้าย
            JLabel leftRowLbl = new JLabel(String.valueOf(row), SwingConstants.CENTER);
            leftRowLbl.setFont(fontLabel);
            leftRowLbl.setForeground(row == 'F' ? COLOR_VIP_TEXT : COLOR_TEXT_MUTED);
            leftRowLbl.setPreferredSize(new Dimension(20, 34));
            rowPanel.add(leftRowLbl);

            for (Seat seat : hallLayout.seatsInRow(row)) {
                JButton seatBtn = createSeatButton(seat);
                seatButtons.put(seat.code(), seatBtn);
                rowPanel.add(seatBtn);
            }

            // ป้ายชื่อแถวฝั่งขวา
            JLabel rightRowLbl = new JLabel(String.valueOf(row), SwingConstants.CENTER);
            rightRowLbl.setFont(fontLabel);
            rightRowLbl.setForeground(row == 'F' ? COLOR_VIP_TEXT : COLOR_TEXT_MUTED);
            rightRowLbl.setPreferredSize(new Dimension(20, 34));
            rowPanel.add(rightRowLbl);

            gridContainer.add(rowPanel);
        }

        hallCard.add(gridContainer, BorderLayout.CENTER);

        // 3. คำอธิบายสัญลักษณ์ที่นั่ง (Legend) ด้านล่าง
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

        legendPanel.add(createLegendItem(Color.WHITE, COLOR_INPUT_BORDER, COLOR_TEXT_TITLE, "Standard (160 THB)"));
        legendPanel.add(createLegendItem(COLOR_VIP_BG, COLOR_VIP_BORDER, COLOR_VIP_TEXT, "VIP Row F (200 THB)"));
        legendPanel.add(createLegendItem(COLOR_PRIMARY, COLOR_PRIMARY, Color.BLACK, "Selected"));
        legendPanel.add(createLegendItem(new Color(243, 244, 246), COLOR_CARD_BORDER, new Color(156, 163, 175), "Booked"));

        hallCard.add(legendPanel, BorderLayout.SOUTH);

        return hallCard;
    }

    /**
     * สร้างปุ่มที่นั่ง 1 ตัว พร้อมสถานะ ว่าง / เลือกอยู่ / ถูกจองแล้ว
     */
    private JButton createSeatButton(Seat seat) {
        String code = seat.code();
        boolean isBooked = bookedSeatCodes.contains(code);
        boolean isVIP = (seat.type() == SeatType.VIP);

        JButton btn = new JButton(String.valueOf(seat.number()));
        btn.setFont(new Font("Tahoma", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(38, 34));
        btn.setFocusPainted(false);

        if (isBooked) {
            btn.setEnabled(false);
            btn.setBackground(new Color(243, 244, 246));
            btn.setForeground(new Color(156, 163, 175));
            btn.setBorder(new LineBorder(COLOR_CARD_BORDER, 1, true));
            btn.setToolTipText(code + " (Booked)");
        } else {
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(code + " - " + (isVIP ? "VIP (200 THB / Member 180 THB)" : "Standard (160 THB / Member 144 THB)"));
            updateSeatButtonStyle(btn, seat, false);

            btn.addActionListener(e -> toggleSeatSelection(seat, btn));
        }

        return btn;
    }

    /**
     * สลับสถานะการเลือกที่นั่ง
     */
    private void toggleSeatSelection(Seat seat, JButton btn) {
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat);
            updateSeatButtonStyle(btn, seat, false);
        } else {
            selectedSeats.add(seat);
            updateSeatButtonStyle(btn, seat, true);
        }
        updateSummary();
    }

    /**
     * กำหนดสีและสไตล์ปุ่มที่นั่ง
     */
    private void updateSeatButtonStyle(JButton btn, Seat seat, boolean isSelected) {
        boolean isVIP = (seat.type() == SeatType.VIP);

        if (isSelected) {
            btn.setBackground(COLOR_PRIMARY);
            btn.setForeground(Color.BLACK);
            btn.setBorder(new LineBorder(COLOR_PRIMARY, 2, true));
        } else if (isVIP) {
            btn.setBackground(COLOR_VIP_BG);
            btn.setForeground(COLOR_VIP_TEXT);
            btn.setBorder(new LineBorder(COLOR_VIP_BORDER, 1, true));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(COLOR_TEXT_TITLE);
            btn.setBorder(new LineBorder(COLOR_INPUT_BORDER, 1, true));
        }
    }

    /**
     * ชิ้นส่วนคำอธิบายสัญลักษณ์
     */
    private JPanel createLegendItem(Color bg, Color border, Color text, String label) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(16, 16));
        box.setBackground(bg);
        box.setBorder(new LineBorder(border, 1, true));

        JLabel lbl = new JLabel(label);
        lbl.setFont(fontSmall);
        lbl.setForeground(COLOR_TEXT_LABEL);

        item.add(box);
        item.add(lbl);
        return item;
    }

    /**
     * การ์ดสรุปราคาและจำนวนที่นั่ง (Order Summary Panel)
     */
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        panel.setPreferredSize(new Dimension(320, 520));

        // 1. หัวข้อ
        JLabel heading = new JLabel("Order Summary");
        heading.setFont(fontHeader);
        heading.setForeground(COLOR_TEXT_TITLE);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createDivider());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. หมวดจำนวนที่นั่ง (Quantity)
        JLabel qtyTitle = new JLabel("Quantity");
        qtyTitle.setFont(fontLabel);
        qtyTitle.setForeground(COLOR_TEXT_TITLE);
        qtyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(qtyTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        seatCountLabel = new JLabel("Seats: 0");
        seatCountLabel.setFont(fontSubheader);
        seatCountLabel.setForeground(COLOR_PRIMARY);
        seatCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(seatCountLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        selectedSeatsTextLabel = new JLabel("Seats: (None)");
        selectedSeatsTextLabel.setFont(fontText);
        selectedSeatsTextLabel.setForeground(COLOR_TEXT_LABEL);
        selectedSeatsTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(selectedSeatsTextLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        standardCountLabel = new JLabel("• Standard: 0");
        standardCountLabel.setFont(fontSmall);
        standardCountLabel.setForeground(COLOR_TEXT_MUTED);
        standardCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(standardCountLabel);

        vipCountLabel = new JLabel("• VIP: 0");
        vipCountLabel.setFont(fontSmall);
        vipCountLabel.setForeground(COLOR_VIP_TEXT);
        vipCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(vipCountLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createDivider());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. หมวดราคา (Price Breakdown)
        JLabel priceTitle = new JLabel("Price Breakdown");
        priceTitle.setFont(fontLabel);
        priceTitle.setForeground(COLOR_TEXT_TITLE);
        priceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(priceTitle);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        regularTotalLabel = new JLabel("Regular Price: 0 THB");
        regularTotalLabel.setFont(fontText);
        regularTotalLabel.setForeground(COLOR_TEXT_LABEL);
        regularTotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(regularTotalLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        memberStatusLabel = new JLabel("Status: Checking");
        memberStatusLabel.setFont(fontSmall);
        memberStatusLabel.setForeground(COLOR_TEXT_MUTED);
        memberStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(memberStatusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        discountLabel = new JLabel("Member Discount (10%): -0 THB");
        discountLabel.setFont(fontText);
        discountLabel.setForeground(COLOR_EMERALD);
        discountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(discountLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        netTotalLabel = new JLabel("Total Amount: 0 THB");
        netTotalLabel.setFont(new Font("Tahoma", Font.BOLD, 17));
        netTotalLabel.setForeground(COLOR_PRIMARY);
        netTotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(netTotalLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createDivider());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 4. หมวดกระเป๋าเงิน (Wallet)
        currentBalanceLabel = new JLabel("Wallet Balance: 0 THB");
        currentBalanceLabel.setFont(fontText);
        currentBalanceLabel.setForeground(COLOR_TEXT_LABEL);
        currentBalanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(currentBalanceLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 3)));

        balanceAfterLabel = new JLabel("Balance After Payment: 0 THB");
        balanceAfterLabel.setFont(fontText);
        balanceAfterLabel.setForeground(COLOR_TEXT_MUTED);
        balanceAfterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(balanceAfterLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));

        balanceWarningLabel = new JLabel("");
        balanceWarningLabel.setFont(fontSmall);
        balanceWarningLabel.setForeground(new Color(220, 38, 38));
        balanceWarningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceWarningLabel.setVisible(false);
        panel.add(balanceWarningLabel);

        panel.add(Box.createVerticalGlue());

        // 5. ปุ่มยืนยันการจองตั๋ว
        confirmBookingButton = new JButton("Confirm Booking");
        confirmBookingButton.setFont(new Font("Tahoma", Font.BOLD, 14));
        confirmBookingButton.setBackground(COLOR_PRIMARY);
        confirmBookingButton.setForeground(Color.BLACK);
        confirmBookingButton.setFocusPainted(false);
        confirmBookingButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBookingButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        confirmBookingButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBookingButton.setBorder(new CompoundBorder(
                new LineBorder(COLOR_PRIMARY, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        confirmBookingButton.setEnabled(false);
        confirmBookingButton.addActionListener(e -> handleBooking());

        panel.add(confirmBookingButton);
        return panel;
    }

    /**
     * เส้นคั่นใน Summary Panel
     */
    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    /**
     * อัปเดตข้อมูลสรุปราคา จำนวนที่นั่ง และกระเป๋าเงิน
     */
    private void updateSummary() {
        int count = selectedSeats.size();
        seatCountLabel.setText("Seats: " + count);

        if (count == 0) {
            selectedSeatsTextLabel.setText("Seats: (None)");
            standardCountLabel.setText("• Standard: 0");
            vipCountLabel.setText("• VIP: 0");
        } else {
            List<String> codes = selectedSeats.stream().map(Seat::code).toList();
            selectedSeatsTextLabel.setText("Seats: " + String.join(", ", codes));

            long standardCount = selectedSeats.stream().filter(s -> s.type() == SeatType.STANDARD).count();
            long vipCount = selectedSeats.stream().filter(s -> s.type() == SeatType.VIP).count();

            standardCountLabel.setText("• Standard: " + standardCount + " (" + (standardCount * 160) + " THB)");
            vipCountLabel.setText("• VIP: " + vipCount + " (" + (vipCount * 200) + " THB)");
        }

        // คิดราคา
        boolean isMember = false;
        int currentBalance = 0;
        try {
            if (accountService != null) {
                isMember = accountService.isMember(currentUser);
                currentBalance = accountService.accountOf(currentUser).balance();
            }
        } catch (Exception ignored) {
        }

        int regularTotal = priceCalculator.totalOf(selectedSeats, false);
        int netTotal = priceCalculator.totalOf(selectedSeats, isMember);
        int discount = regularTotal - netTotal;

        regularTotalLabel.setText("Regular Price: " + regularTotal + " THB");
        memberStatusLabel.setText(isMember ? "Status: Member (10% discount applied)" : "Status: Guest (No discount)");
        discountLabel.setText("Member Discount: -" + discount + " THB");
        netTotalLabel.setText("Total Amount: " + netTotal + " THB");

        currentBalanceLabel.setText("Wallet Balance: " + currentBalance + " THB");
        topBalanceLabel.setText("Balance: " + currentBalance + " THB");

        int remainingBalance = currentBalance - netTotal;
        balanceAfterLabel.setText("Balance After Payment: " + (count > 0 ? remainingBalance : currentBalance) + " THB");

        // ตรวจสอบเงื่อนไขการจอง
        if (count == 0) {
            confirmBookingButton.setEnabled(false);
            confirmBookingButton.setText("Please select seat(s)");
            balanceWarningLabel.setVisible(false);
        } else if (currentBalance < netTotal) {
            confirmBookingButton.setEnabled(false);
            confirmBookingButton.setText("Insufficient Balance");
            int needed = netTotal - currentBalance;
            balanceWarningLabel.setText("<html>Need " + needed + " THB more (Please top up)</html>");
            balanceWarningLabel.setVisible(true);
        } else {
            confirmBookingButton.setEnabled(true);
            confirmBookingButton.setText("Confirm Booking (" + netTotal + " THB)");
            balanceWarningLabel.setVisible(false);
        }
    }

    /**
     * จัดการขั้นตอนการจองตั๋วภาพยนตร์
     */
    private void handleBooking() {
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

        // กล่องยืนยันการจอง
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
            showETicketDialog(booking);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Booking Failed", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "System error connecting to data files: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * แสดงตั๋วภาพยนตร์จำลอง (Simulated E-Ticket) หลังชำระเงินสำเร็จ
     */
    private void showETicketDialog(Booking booking) {
        JDialog dialog = new JDialog(this, "Electronic Ticket (E-Ticket)", true);
        dialog.setSize(440, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel ticketPanel = new JPanel();
        ticketPanel.setLayout(new BoxLayout(ticketPanel, BoxLayout.Y_AXIS));
        ticketPanel.setBackground(Color.WHITE);
        ticketPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel brand = new JLabel("KU CINEMA - E-TICKET");
        brand.setFont(new Font("Tahoma", Font.BOLD, 16));
        brand.setForeground(COLOR_PRIMARY);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusBadge = new JLabel("Payment Successful (PAID)");
        statusBadge.setFont(fontLabel);
        statusBadge.setForeground(COLOR_EMERALD);
        statusBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLabel = new JLabel("Booking ID: " + booking.id());
        idLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        idLabel.setForeground(COLOR_TEXT_TITLE);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ticketPanel.add(brand);
        ticketPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        ticketPanel.add(statusBadge);
        ticketPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ticketPanel.add(idLabel);
        ticketPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        ticketPanel.add(createDivider());
        ticketPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = showtime.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        addTicketRow(ticketPanel, "Movie:", showtime.movie().title());
        addTicketRow(ticketPanel, "Date:", dateStr);
        addTicketRow(ticketPanel, "Time:", showtime.start() + " - " + showtime.end() + " (Hall " + showtime.hall() + ")");
        addTicketRow(ticketPanel, "Seats:", String.join(", ", booking.seatCodes()) + " (" + booking.seatCodes().size() + " seats)");
        addTicketRow(ticketPanel, "Total Price:", booking.totalPrice() + " THB");
        addTicketRow(ticketPanel, "Customer:", booking.username());
        addTicketRow(ticketPanel, "Booking Time:", booking.bookedAt().format(dtFormat));

        ticketPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        ticketPanel.add(createDivider());
        ticketPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        JButton doneBtn = new JButton("Done (Return to Main)");
        doneBtn.setFont(new Font("Tahoma", Font.BOLD, 13));
        doneBtn.setBackground(COLOR_PRIMARY);
        doneBtn.setForeground(Color.BLACK);
        doneBtn.setFocusPainted(false);
        doneBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        doneBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        doneBtn.setBorder(new CompoundBorder(
                new LineBorder(COLOR_PRIMARY, 1, true),
                new EmptyBorder(8, 20, 8, 20)));
        doneBtn.addActionListener(e -> {
            dialog.dispose();
            returnToMain();
        });

        ticketPanel.add(doneBtn);

        dialog.setContentPane(ticketPanel);
        dialog.setVisible(true);
    }

    private void addTicketRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(fontSmall);
        lbl.setForeground(COLOR_TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(fontLabel);
        val.setForeground(COLOR_TEXT_TITLE);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);

        panel.add(row);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    /**
     * เปิดหน้าต่างเติมเงิน (Top-up)
     */
    private void showTopUpDialog() {
        if (accountService == null) return;

        try {
            Account account = accountService.accountOf(currentUser);

            JDialog dialog = new JDialog(this, "Wallet Top-up", true);
            dialog.setSize(420, 320);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel title = new JLabel("Wallet Top-up");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(COLOR_TEXT_TITLE);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel curBalance = new JLabel("Current Balance: " + account.balance() + " THB");
            curBalance.setFont(fontSmall);
            curBalance.setForeground(COLOR_TEXT_MUTED);
            curBalance.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel promptLabel = new JLabel("Select or enter amount (1 - 5,000 THB):");
            promptLabel.setFont(fontLabel);
            promptLabel.setForeground(COLOR_TEXT_LABEL);
            promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            quickPanel.setOpaque(false);
            int[] quickAmounts = {100, 200, 300, 500, 1000};
            JTextField amountField = new JTextField("200", 12);
            amountField.setFont(new Font("Tahoma", Font.PLAIN, 14));
            amountField.setHorizontalAlignment(JTextField.CENTER);
            amountField.setMaximumSize(new Dimension(200, 32));

            for (int amt : quickAmounts) {
                JButton qBtn = new JButton("+" + amt);
                qBtn.setFont(fontSmall);
                qBtn.setBackground(new Color(243, 244, 246));
                qBtn.setForeground(COLOR_TEXT_LABEL);
                qBtn.setFocusPainted(false);
                qBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                qBtn.setBorder(new CompoundBorder(
                        new LineBorder(COLOR_INPUT_BORDER, 1, true),
                        new EmptyBorder(4, 8, 4, 8)));
                qBtn.addActionListener(e -> amountField.setText(String.valueOf(amt)));
                quickPanel.add(qBtn);
            }

            panel.add(title);
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(curBalance);
            panel.add(Box.createRigidArea(new Dimension(0, 14)));
            panel.add(promptLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(quickPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 12)));
            panel.add(amountField);
            panel.add(Box.createRigidArea(new Dimension(0, 20)));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnPanel.setOpaque(false);

            JButton confirmBtn = new JButton("Confirm Top-up");
            confirmBtn.setFont(fontLabel);
            confirmBtn.setBackground(COLOR_EMERALD);
            confirmBtn.setForeground(Color.BLACK);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            confirmBtn.setBorder(new CompoundBorder(
                new LineBorder(COLOR_EMERALD, 1, true),
                new EmptyBorder(7, 16, 7, 16)));

            confirmBtn.addActionListener(e -> {
                String text = amountField.getText().trim();
                int amt;
                try {
                    amt = Integer.parseInt(text);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a valid integer amount", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (amt < 1 || amt > 5000) {
                    JOptionPane.showMessageDialog(dialog, "Top-up amount must be between 1 and 5,000 THB", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    Account updated = accountService.topUp(currentUser, amt);
                    dialog.dispose();
                    updateSummary();
                    JOptionPane.showMessageDialog(
                            this,
                            "Top-up successful: " + amt + " THB\nCurrent Balance: " + updated.balance() + " THB",
                            "Top-up Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error during top-up: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.setFont(fontLabel);
            cancelBtn.setBackground(Color.WHITE);
            cancelBtn.setForeground(COLOR_TEXT_LABEL);
            cancelBtn.setFocusPainted(false);
            cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelBtn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));
            cancelBtn.addActionListener(e -> dialog.dispose());

            btnPanel.add(confirmBtn);
            btnPanel.add(cancelBtn);
            panel.add(btnPanel);

            dialog.setContentPane(panel);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot retrieve account data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
