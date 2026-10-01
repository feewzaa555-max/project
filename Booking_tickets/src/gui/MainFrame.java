package gui;

import model.Account;
import model.Booking;
import model.Movie;
import model.Showtime;
import model.User;
import repository.CsvMovieRepository;
import repository.CsvUserRepository;
import repository.MovieRepository;
import repository.UserRepository;
import service.AccountService;
import service.AppClock;
import service.AppServices;
import service.AuthService;
import service.BookingService;
import service.MovieService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * หน้าต่างหลัก GUI สำหรับเลือกรอบภาพยนตร์ (Main Dashboard)
 * มีแถบด้านบน (Top Bar) แสดงชื่อ KU CINEMA ทางซ้ายสุด
 * และทางขวาสุดแสดง: ชื่อผู้ใช้, ปุ่มสมัครสมาชิก, เงินคงเหลือ, ปุ่มเติมเงิน, ประวัติ, ปุ่มlogout เรียงตามลำดับ
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final AppServices appServices;
    private final MovieService movieService;
    private final AuthService authService;
    private final AccountService accountService;
    private final BookingService bookingService;

    // ข้อมูลภาพยนตร์และวันที่จากระบบหลังบ้าน
    private List<Movie> movieList = new ArrayList<>();
    private List<LocalDate> bookableDates = new ArrayList<>();

    // สถานะที่เลือกในปัจจุบัน
    private LocalDate selectedDate;
    private Movie selectedMovie;
    private int carouselStartIndex = 0; // ลำดับภาพยนตร์ตัวแรกใน 3 เรื่องที่มองเห็น

    // UI Components แถบบน (Top Bar)
    private JLabel balanceLabel;
    private JButton membershipButton;

    // UI Components เนื้อหาการ์ด
    private JPanel dateButtonsPanel;
    private JPanel moviesPanel;
    private JPanel dotsPanel;
    private JLabel showtimesSectionLabel;
    private JPanel showtimesListPanel;

    // สีและฟอนต์มาตรฐานชุดเดียวกับ LoginFrame
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue #2563EB
    private static final Color COLOR_BG = new Color(245, 247, 250);          // Gray #F5F7FA
    private static final Color COLOR_CARD_BORDER = new Color(229, 231, 235); // Border #E5E7EB
    private static final Color COLOR_INPUT_BORDER = new Color(209, 213, 219);// Border #D1D5DB
    private static final Color COLOR_TEXT_TITLE = new Color(17, 24, 39);     // Dark #111827
    private static final Color COLOR_TEXT_LABEL = new Color(55, 65, 81);     // Dark #374151
    private static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);  // Muted #6B7280

    private final Font fontHeader = new Font("Tahoma", Font.BOLD, 20);
    private final Font fontSubtitle = new Font("Tahoma", Font.PLAIN, 12);
    private final Font fontLabel = new Font("Tahoma", Font.BOLD, 12);
    private final Font fontText = new Font("Tahoma", Font.PLAIN, 13);
    private final Font fontSmall = new Font("Tahoma", Font.PLAIN, 11);

    public MainFrame(User user, AppServices appServices) {
        this.currentUser = (user != null) ? user : new User("thanawat", "password123");
        this.appServices = (appServices != null) ? appServices : AppServices.create();
        this.movieService = this.appServices.movies();
        this.authService = this.appServices.auth();
        this.accountService = this.appServices.accounts();
        this.bookingService = this.appServices.bookings();

        loadData();
        initUI();
    }

    public MainFrame(User user, MovieService movieService, AuthService authService) {
        this.currentUser = (user != null) ? user : new User("thanawat", "password123");
        AppServices services = null;
        try {
            services = AppServices.create();
        } catch (Exception ignored) {
        }
        this.appServices = services;
        this.movieService = (movieService != null) ? movieService : (services != null ? services.movies() : null);
        this.authService = (authService != null) ? authService : (services != null ? services.auth() : null);
        this.accountService = (services != null) ? services.accounts() : null;
        this.bookingService = (services != null) ? services.bookings() : null;

        loadData();
        initUI();
    }

    public MainFrame(User user, MovieService movieService) {
        this(user, movieService, null);
    }

    public MainFrame(User user) {
        this(user, null, null);
    }

    public MainFrame() {
        this(new User("thanawat", "password123"), null, null);
    }

    /**
     * ดึงข้อมูลภาพยนตร์และวันที่ที่จองได้จาก MovieService
     */
    private void loadData() {
        if (movieService != null) {
            try {
                this.movieList = movieService.movies();
            } catch (IOException e) {
                this.movieList = new ArrayList<>();
                JOptionPane.showMessageDialog(this,
                        "Error loading movie list: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            this.bookableDates = movieService.bookableDates();
        }

        if (bookableDates != null && !bookableDates.isEmpty()) {
            this.selectedDate = bookableDates.get(0);
        } else {
            this.selectedDate = LocalDate.now();
        }

        if (movieList != null && !movieList.isEmpty()) {
            this.selectedMovie = movieList.get(0);
        }
    }

    /**
     * สร้างหน้าจอหลักพร้อมแถบด้านบน (Top Bar) และการ์ดเนื้อหาตรงกลาง
     */
    private void initUI() {
        setTitle("KU CINEMA - Movie Ticket Booking");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 730);
        setLocationRelativeTo(null);
        setResizable(false);

        // Root Container
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(COLOR_BG);

        // 1. แถบด้านบน (Top Bar)
        rootPanel.add(createTopBar(), BorderLayout.NORTH);

        // 2. พื้นที่ตรงกลาง (Main Content Panel)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(new EmptyBorder(14, 24, 16, 24));

        // การ์ดสีขาวตรงกลาง (Card Container)
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(16, 28, 16, 28)));

        // --- ส่วนหัว (Header Section) ---
        JLabel titleLabel = new JLabel("Select Showtime");
        titleLabel.setFont(fontHeader);
        titleLabel.setForeground(COLOR_TEXT_TITLE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Welcome " + currentUser.username() + " | Please select a movie and showtime");
        subtitleLabel.setFont(fontSubtitle);
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- ส่วนที่ 1: เลือกวันที่ (Date Section) ---
        JLabel dateSectionLabel = new JLabel("1. Date");
        dateSectionLabel.setFont(fontLabel);
        dateSectionLabel.setForeground(COLOR_TEXT_LABEL);
        dateSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(dateSectionLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        dateButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        dateButtonsPanel.setOpaque(false);
        dateButtonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dateButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cardPanel.add(dateButtonsPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- ส่วนที่ 2: เลือกภาพยนตร์ (Movie Section) ---
        JLabel movieSectionLabel = new JLabel("2. Movie");
        movieSectionLabel.setFont(fontLabel);
        movieSectionLabel.setForeground(COLOR_TEXT_LABEL);
        movieSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(movieSectionLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // คอนเทนเนอร์แสดงการ์ดหนัง 3 เรื่องพร้อมปุ่มเลื่อน < >
        JPanel carouselContainer = new JPanel(new BorderLayout(12, 0));
        carouselContainer.setOpaque(false);
        carouselContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        carouselContainer.setMaximumSize(new Dimension(740, 260));

        JButton prevBtn = createNavButton("<");
        prevBtn.addActionListener(e -> scrollMovies(-1));
        JPanel prevWrap = new JPanel(new GridBagLayout());
        prevWrap.setOpaque(false);
        prevWrap.add(prevBtn);
        carouselContainer.add(prevWrap, BorderLayout.WEST);

        moviesPanel = new JPanel(new GridLayout(1, 3, 14, 0));
        moviesPanel.setOpaque(false);
        carouselContainer.add(moviesPanel, BorderLayout.CENTER);

        JButton nextBtn = createNavButton(">");
        nextBtn.addActionListener(e -> scrollMovies(1));
        JPanel nextWrap = new JPanel(new GridBagLayout());
        nextWrap.setOpaque(false);
        nextWrap.add(nextBtn);
        carouselContainer.add(nextWrap, BorderLayout.EAST);

        cardPanel.add(carouselContainer);

        // จุด Indicator แสดงตำแหน่งหนัง
        dotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        dotsPanel.setOpaque(false);
        dotsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(dotsPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- ส่วนที่ 3: เลือกรอบฉาย (Showtimes Section) ---
        showtimesSectionLabel = new JLabel("3. Showtimes");
        showtimesSectionLabel.setFont(fontLabel);
        showtimesSectionLabel.setForeground(COLOR_TEXT_LABEL);
        showtimesSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(showtimesSectionLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // แถวแสดงปุ่มรอบฉาย
        showtimesListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        showtimesListPanel.setOpaque(false);
        showtimesListPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        showtimesListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        cardPanel.add(showtimesListPanel);

        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel guideLabel = new JLabel("← Click on a showtime to select seats");
        guideLabel.setFont(fontSmall);
        guideLabel.setForeground(COLOR_TEXT_MUTED);
        guideLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(guideLabel);

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        rootPanel.add(mainPanel, BorderLayout.CENTER);
        setContentPane(rootPanel);

        // อัปเดตข้อมูลครั้งแรก
        refreshAccountBar();
        refreshDateButtons();
        refreshMovies();
        refreshShowtimes();
    }

    /**
     * สร้างแถบด้านบน (Top Bar):
     * ซ้ายสุด: KU CINEMA
     * นอกนั้นชิดขวา:
     *   1. ชื่อผู้ใช้
     *   2. ปุ่มสมัครสมาชิก
     *   3. เงินคงเหลือ
     *   4. ปุ่มเติมเงิน
     *   5. ประวัติ
     *   6. ปุ่มlogout
     * เรียงตามลำดับ
     */
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER),
                new EmptyBorder(10, 20, 10, 20)));

        // ซ้ายสุด: KU CINEMA
        JLabel brandLabel = new JLabel("KU CINEMA");
        brandLabel.setFont(new Font("Tahoma", Font.BOLD, 18));
        brandLabel.setForeground(COLOR_PRIMARY);

        // ด้านขวา: จัดเรียงตามลำดับที่กำหนด ชิดขวา
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // 1. ชื่อผู้ใช้
        JLabel usernameLabel = new JLabel("User: " + currentUser.username());
        usernameLabel.setFont(fontLabel);
        usernameLabel.setForeground(COLOR_TEXT_TITLE);
        usernameLabel.setBorder(new EmptyBorder(5, 2, 5, 2));

        // 2. ปุ่มสมัครสมาชิก
        membershipButton = new JButton("Membership");
        membershipButton.setFont(fontLabel);
        membershipButton.setFocusPainted(false);
        membershipButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        membershipButton.addActionListener(e -> showMembershipDialog());

        // 3. เงินคงเหลือ
        balanceLabel = new JLabel("Balance: 0 THB");
        balanceLabel.setFont(fontLabel);
        balanceLabel.setForeground(new Color(5, 150, 105)); // Green #059669
        balanceLabel.setBorder(new EmptyBorder(5, 2, 5, 2));

        // 4. ปุ่มเติมเงิน
        JButton topUpButton = new JButton("Top-up");
        topUpButton.setFont(fontLabel);
        topUpButton.setBackground(new Color(16, 185, 129)); // Emerald Green #10B981
        topUpButton.setForeground(Color.BLACK);
        topUpButton.setFocusPainted(false);
        topUpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        topUpButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(16, 185, 129), 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        topUpButton.addActionListener(e -> showTopUpDialog());

        // 5. ประวัติ
        JButton historyButton = new JButton("History");
        historyButton.setFont(fontLabel);
        historyButton.setBackground(Color.WHITE);
        historyButton.setForeground(COLOR_TEXT_LABEL);
        historyButton.setFocusPainted(false);
        historyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        historyButton.setBorder(new CompoundBorder(
                new LineBorder(COLOR_INPUT_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        historyButton.addActionListener(e -> showHistoryDialog());

        // 6. ปุ่ม logout
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(fontLabel);
        logoutButton.setBackground(new Color(254, 242, 242));
        logoutButton.setForeground(new Color(220, 38, 38)); // Red #DC2626
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(252, 165, 165), 1, true),
                new EmptyBorder(5, 12, 5, 12)));
        logoutButton.addActionListener(e -> handleLogout());

        // เพิ่มเข้า rightPanel เรียงตามลำดับที่กำหนด
        rightPanel.add(usernameLabel);
        rightPanel.add(membershipButton);
        rightPanel.add(balanceLabel);
        rightPanel.add(topUpButton);
        rightPanel.add(historyButton);
        rightPanel.add(logoutButton);

        topBar.add(brandLabel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        return topBar;
    }

    /**
     * อัปเดตข้อมูลแถบด้านบน (ยอดเงินคงเหลือ และสถานะปุ่มสมาชิก)
     */
    private void refreshAccountBar() {
        if (balanceLabel == null || membershipButton == null) return;

        if (accountService == null) {
            balanceLabel.setText("Balance: 0 THB");
            membershipButton.setText("Membership");
            return;
        }

        try {
            Account account = accountService.accountOf(currentUser);
            balanceLabel.setText("Balance: " + account.balance() + " THB");

            boolean isMember = accountService.isMember(currentUser);
            if (isMember && account.memberUntil() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
                membershipButton.setText("Member (until " + account.memberUntil().format(formatter) + ")");
                membershipButton.setBackground(new Color(254, 243, 199)); // Amber light
                membershipButton.setForeground(new Color(180, 83, 9));    // Amber dark
                membershipButton.setBorder(new CompoundBorder(
                        new LineBorder(new Color(245, 158, 11), 1, true),
                        new EmptyBorder(5, 12, 5, 12)));
            } else {
                membershipButton.setText("Membership");
                membershipButton.setBackground(new Color(238, 242, 255)); // Indigo light
                membershipButton.setForeground(COLOR_PRIMARY);
                membershipButton.setBorder(new CompoundBorder(
                        new LineBorder(new Color(199, 210, 254), 1, true),
                        new EmptyBorder(5, 12, 5, 12)));
            }
        } catch (Exception e) {
            balanceLabel.setText("Balance: -");
            membershipButton.setText("Membership");
        }
    }

    /**
     * เปิดหน้าต่างสมัครสมาชิก / ต่ออายุสมาชิก
     */
    private void showMembershipDialog() {
        if (accountService == null) {
            JOptionPane.showMessageDialog(this, "AccountService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Account account = accountService.accountOf(currentUser);
            boolean isMember = accountService.isMember(currentUser);

            JDialog dialog = new JDialog(this, isMember ? "KU CINEMA Membership Status" : "KU CINEMA Membership Subscription", true);
            dialog.setSize(400, 300);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(20, 24, 20, 24));

            JLabel title = new JLabel(isMember ? "KU CINEMA Membership Status" : "KU CINEMA Membership Subscription");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(COLOR_TEXT_TITLE);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel benefit = new JLabel("Benefits: 10% discount on all seats and showtimes");
            benefit.setFont(fontSubtitle);
            benefit.setForeground(COLOR_PRIMARY);
            benefit.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel feeLabel = new JLabel("Fee: " + AccountService.MEMBERSHIP_PRICE + " THB / 30 Days");
            feeLabel.setFont(fontText);
            feeLabel.setForeground(COLOR_TEXT_LABEL);
            feeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            String statusStr = isMember && account.memberUntil() != null
                    ? "Status: Member (Expires " + account.memberUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")"
                    : "Status: Non-member";
            JLabel statusLabel = new JLabel(statusStr);
            statusLabel.setFont(fontText);
            statusLabel.setForeground(isMember ? new Color(16, 185, 129) : COLOR_TEXT_MUTED);
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel currentBalanceLabel = new JLabel("Wallet Balance: " + account.balance() + " THB");
            currentBalanceLabel.setFont(fontLabel);
            currentBalanceLabel.setForeground(COLOR_TEXT_TITLE);
            currentBalanceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(title);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(benefit);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
            panel.add(feeLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
            panel.add(statusLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
            panel.add(currentBalanceLabel);
            panel.add(Box.createRigidArea(new Dimension(0, 18)));

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            btnPanel.setOpaque(false);

            JButton actionBtn = new JButton(isMember ? "Renew Membership (99 THB)" : "Confirm Subscription (99 THB)");
            actionBtn.setFont(fontLabel);
            actionBtn.setBackground(COLOR_PRIMARY);
            actionBtn.setForeground(Color.BLACK);
            actionBtn.setFocusPainted(false);
            actionBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            actionBtn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_PRIMARY, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));

            actionBtn.addActionListener(e -> {
                if (account.balance() < AccountService.MEMBERSHIP_PRICE) {
                    int opt = JOptionPane.showConfirmDialog(
                            dialog,
                            "Insufficient balance (Requires " + AccountService.MEMBERSHIP_PRICE + " THB, Available " + account.balance() + " THB)\nWould you like to top up now?",
                            "Insufficient Balance",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION) {
                        dialog.dispose();
                        showTopUpDialog();
                    }
                    return;
                }

                try {
                    Account updated = accountService.subscribe(currentUser);
                    dialog.dispose();
                    refreshAccountBar();
                    String untilStr = updated.memberUntil() != null
                            ? updated.memberUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "";
                    JOptionPane.showMessageDialog(
                            this,
                            (isMember ? "Membership renewed successfully!" : "Membership subscribed successfully!") + "\nMember until: " + untilStr + "\nRemaining balance: " + updated.balance() + " THB",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton closeBtn = new JButton("Close");
            closeBtn.setFont(fontLabel);
            closeBtn.setBackground(Color.WHITE);
            closeBtn.setForeground(COLOR_TEXT_LABEL);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(7, 16, 7, 16)));
            closeBtn.addActionListener(e -> dialog.dispose());

            btnPanel.add(actionBtn);
            btnPanel.add(closeBtn);
            panel.add(btnPanel);

            dialog.setContentPane(panel);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot retrieve membership info: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * เปิดหน้าต่างเติมเงิน (Top-up)
     */
    private void showTopUpDialog() {
        if (accountService == null) {
            JOptionPane.showMessageDialog(this, "AccountService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
            curBalance.setFont(fontSubtitle);
            curBalance.setForeground(COLOR_TEXT_MUTED);
            curBalance.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel promptLabel = new JLabel("Select or enter amount (1 - 5,000 THB):");
            promptLabel.setFont(fontLabel);
            promptLabel.setForeground(COLOR_TEXT_LABEL);
            promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // ปุ่มลัดเลือกจำนวนเงิน
            JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            quickPanel.setOpaque(false);
            int[] quickAmounts = {100, 200, 300, 500, 1000};
            JTextField amountField = new JTextField("100", 12);
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
            confirmBtn.setBackground(new Color(16, 185, 129)); // Emerald Green
            confirmBtn.setForeground(Color.BLACK);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            confirmBtn.setBorder(new CompoundBorder(
                    new LineBorder(new Color(16, 185, 129), 1, true),
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
                    refreshAccountBar();
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
     * เปิดหน้าต่างประวัติการจองตั๋วภาพยนตร์
     */
    private void showHistoryDialog() {
        if (bookingService == null) {
            JOptionPane.showMessageDialog(this, "BookingService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<Booking> history = bookingService.historyOf(currentUser);

            JDialog dialog = new JDialog(this, "Booking History", true);
            dialog.setSize(620, 480);
            dialog.setLocationRelativeTo(this);

            JPanel mainDialogPanel = new JPanel(new BorderLayout());
            mainDialogPanel.setBackground(COLOR_BG);
            mainDialogPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

            // ส่วนหัว Dialog
            JLabel title = new JLabel("Booking History (" + currentUser.username() + ")");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(COLOR_TEXT_TITLE);
            title.setBorder(new EmptyBorder(0, 0, 12, 0));
            mainDialogPanel.add(title, BorderLayout.NORTH);

            if (history.isEmpty()) {
                JPanel emptyPanel = new JPanel(new GridBagLayout());
                emptyPanel.setBackground(Color.WHITE);
                emptyPanel.setBorder(new LineBorder(COLOR_CARD_BORDER, 1, true));

                JLabel emptyLabel = new JLabel("No booking history yet");
                emptyLabel.setFont(fontText);
                emptyLabel.setForeground(COLOR_TEXT_MUTED);
                emptyPanel.add(emptyLabel);

                mainDialogPanel.add(emptyPanel, BorderLayout.CENTER);
            } else {
                JPanel listPanel = new JPanel();
                listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
                listPanel.setBackground(COLOR_BG);

                DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (Booking b : history) {
                    JPanel itemCard = new JPanel(new BorderLayout(8, 6));
                    itemCard.setBackground(Color.WHITE);
                    itemCard.setBorder(new CompoundBorder(
                            new LineBorder(COLOR_CARD_BORDER, 1, true),
                            new EmptyBorder(12, 14, 12, 14)));
                    itemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

                    // ข้อมูลฝั่งซ้าย
                    Showtime st = bookingService.showtimeOf(b);
                    String movieTitle = (st != null && st.movie() != null) ? st.movie().title() : "Showtime ID: " + b.showtimeId();
                    String showtimeText = (st != null)
                            ? "Date: " + st.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " Time: " + st.start() + " (Hall " + st.hall() + ")"
                            : "Showtime: " + b.showtimeId();

                    JPanel leftInfo = new JPanel();
                    leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
                    leftInfo.setOpaque(false);

                    JLabel movieLabel = new JLabel(b.id() + " - " + movieTitle);
                    movieLabel.setFont(fontLabel);
                    movieLabel.setForeground(COLOR_PRIMARY);

                    JLabel timeLabel = new JLabel(showtimeText);
                    timeLabel.setFont(fontSubtitle);
                    timeLabel.setForeground(COLOR_TEXT_TITLE);

                    JLabel seatLabel = new JLabel("Seats: " + String.join(", ", b.seatCodes()) + " | Booked: " + b.bookedAt().format(dtFormat));
                    seatLabel.setFont(fontSmall);
                    seatLabel.setForeground(COLOR_TEXT_MUTED);

                    leftInfo.add(movieLabel);
                    leftInfo.add(Box.createRigidArea(new Dimension(0, 3)));
                    leftInfo.add(timeLabel);
                    leftInfo.add(Box.createRigidArea(new Dimension(0, 3)));
                    leftInfo.add(seatLabel);

                    // ยอดเงินฝั่งขวา
                    JPanel rightPrice = new JPanel(new GridBagLayout());
                    rightPrice.setOpaque(false);
                    JLabel priceLabel = new JLabel(b.totalPrice() + " THB");
                    priceLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
                    priceLabel.setForeground(new Color(16, 185, 129));
                    rightPrice.add(priceLabel);

                    itemCard.add(leftInfo, BorderLayout.CENTER);
                    itemCard.add(rightPrice, BorderLayout.EAST);

                    listPanel.add(itemCard);
                    listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }

                JScrollPane scrollPane = new JScrollPane(listPanel);
                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.setOpaque(false);
                scrollPane.getViewport().setOpaque(false);
                scrollPane.getVerticalScrollBar().setUnitIncrement(12);

                mainDialogPanel.add(scrollPane, BorderLayout.CENTER);
            }

            // ปุ่มปิดด้านล่าง
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
            bottomPanel.setOpaque(false);
            JButton closeBtn = new JButton("Close");
            closeBtn.setFont(fontLabel);
            closeBtn.setBackground(Color.WHITE);
            closeBtn.setForeground(COLOR_TEXT_LABEL);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(6, 16, 6, 16)));
            closeBtn.addActionListener(e -> dialog.dispose());
            bottomPanel.add(closeBtn);

            mainDialogPanel.add(bottomPanel, BorderLayout.SOUTH);

            dialog.setContentPane(mainDialogPanel);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot retrieve booking history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * วาดปุ่มวันที่ตาม bookableDates (วันนี้ / พรุ่งนี้ / มะรืน)
     */
    private void refreshDateButtons() {
        dateButtonsPanel.removeAll();
        if (bookableDates == null || bookableDates.isEmpty()) {
            dateButtonsPanel.revalidate();
            dateButtonsPanel.repaint();
            return;
        }

        for (int i = 0; i < bookableDates.size(); i++) {
            LocalDate date = bookableDates.get(i);
            boolean isSelected = date.equals(selectedDate);

            String labelText = (i == 0)
                    ? "Today " + date.getDayOfMonth() + "/" + date.getMonthValue()
                    : date.getDayOfMonth() + "/" + date.getMonthValue();

            JButton dateBtn = new JButton(labelText);
            dateBtn.setFont(new Font("Tahoma", isSelected ? Font.BOLD : Font.PLAIN, 12));
            dateBtn.setFocusPainted(false);
            dateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (isSelected) {
                dateBtn.setBackground(COLOR_PRIMARY);
                dateBtn.setForeground(Color.BLACK);
                dateBtn.setBorder(new CompoundBorder(
                        new LineBorder(COLOR_PRIMARY, 1, true),
                        new EmptyBorder(5, 14, 5, 14)));
            } else {
                dateBtn.setBackground(Color.WHITE);
                dateBtn.setForeground(COLOR_TEXT_LABEL);
                dateBtn.setBorder(new CompoundBorder(
                        new LineBorder(COLOR_INPUT_BORDER, 1, true),
                        new EmptyBorder(5, 14, 5, 14)));
            }

            dateBtn.addActionListener(e -> {
                selectedDate = date;
                refreshDateButtons();
                refreshShowtimes();
            });

            dateButtonsPanel.add(dateBtn);
        }

        dateButtonsPanel.revalidate();
        dateButtonsPanel.repaint();
    }

    /**
     * เลื่อนภาพยนตร์ ซ้าย (-1) หรือ ขวา (+1)
     */
    private void scrollMovies(int direction) {
        if (movieList == null || movieList.size() <= 3) return;

        int maxStart = movieList.size() - 3;
        int newIndex = carouselStartIndex + direction;

        if (newIndex < 0) {
            newIndex = maxStart;
        } else if (newIndex > maxStart) {
            newIndex = 0;
        }

        carouselStartIndex = newIndex;

        // ถ้าเรื่องที่เลือกอยู่นอกจอ ให้เลือกเรื่องแรกของหน้าจอใหม่
        boolean isVisible = false;
        for (int i = 0; i < 3; i++) {
            if (movieList.get(carouselStartIndex + i).equals(selectedMovie)) {
                isVisible = true;
                break;
            }
        }
        if (!isVisible) {
            selectedMovie = movieList.get(carouselStartIndex);
        }

        refreshMovies();
        refreshShowtimes();
    }

    /**
     * วาดการ์ดภาพยนตร์ 3 เรื่อง และจุด Indicator
     */
    private void refreshMovies() {
        moviesPanel.removeAll();
        dotsPanel.removeAll();

        if (movieList == null || movieList.isEmpty()) {
            moviesPanel.add(new JLabel("No movie data available"));
            moviesPanel.revalidate();
            moviesPanel.repaint();
            return;
        }

        int displayCount = Math.min(3, movieList.size());
        for (int i = 0; i < displayCount; i++) {
            int movieIndex = carouselStartIndex + i;
            if (movieIndex < movieList.size()) {
                Movie movie = movieList.get(movieIndex);
                boolean isSelected = movie.equals(selectedMovie);
                JPanel movieCard = createSingleMovieCard(movie, movieIndex, isSelected);
                moviesPanel.add(movieCard);
            }
        }

        // วาดจุด Indicator แสดงตำแหน่ง
        for (int i = 0; i < movieList.size(); i++) {
            boolean isInView = (i >= carouselStartIndex && i < carouselStartIndex + 3);
            JPanel dot = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isInView ? COLOR_PRIMARY : COLOR_INPUT_BORDER);
                    g2.fillOval(0, 0, 7, 7);
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(7, 7));
            dot.setOpaque(false);
            dotsPanel.add(dot);
        }

        moviesPanel.revalidate();
        moviesPanel.repaint();
        dotsPanel.revalidate();
        dotsPanel.repaint();
    }

    /**
     * สร้างการ์ดแสดงภาพยนตร์เดี่ยว 1 เรื่อง ในสไตล์มินิมอลสะอาดตา
     */
    private JPanel createSingleMovieCard(Movie movie, int index, boolean isSelected) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // กล่องรูปโปสเตอร์
        PosterBox posterBox = new PosterBox(movie, index, isSelected);
        posterBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(posterBox);

        card.add(Box.createRigidArea(new Dimension(0, 6)));

        // ชื่อภาพยนตร์
        JLabel titleLbl = new JLabel("<html><div style='text-align: center; font-family: Tahoma;'>"
                + movie.title() + "</div></html>", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Tahoma", isSelected ? Font.BOLD : Font.PLAIN, 11));
        titleLbl.setForeground(isSelected ? COLOR_PRIMARY : COLOR_TEXT_TITLE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLbl.setPreferredSize(new Dimension(170, 34));
        titleLbl.setMaximumSize(new Dimension(170, 34));
        card.add(titleLbl);

        card.add(Box.createRigidArea(new Dimension(0, 2)));

        // ความยาวภาพยนตร์
        JLabel durLbl = new JLabel(movie.durationMinutes() + " min", SwingConstants.CENTER);
        durLbl.setFont(fontSmall);
        durLbl.setForeground(COLOR_TEXT_MUTED);
        durLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(durLbl);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedMovie = movie;
                refreshMovies();
                refreshShowtimes();
            }
        });

        return card;
    }

    /**
     * ดึงรอบฉายของภาพยนตร์และวันที่เลือก มาแสดงเป็นปุ่ม
     */
    private void refreshShowtimes() {
        if (showtimesSectionLabel == null || showtimesListPanel == null) return;

        if (selectedMovie == null || selectedDate == null || movieService == null) {
            showtimesSectionLabel.setText("3. Showtimes");
            showtimesListPanel.removeAll();
            showtimesListPanel.revalidate();
            showtimesListPanel.repaint();
            return;
        }

        String dateText = (!bookableDates.isEmpty() && selectedDate.equals(bookableDates.get(0)))
                ? "Today " + selectedDate.getDayOfMonth() + "/" + selectedDate.getMonthValue()
                : selectedDate.getDayOfMonth() + "/" + selectedDate.getMonthValue();
        showtimesSectionLabel.setText("3. Showtimes: \"" + selectedMovie.title() + "\"  ·  " + dateText);

        showtimesListPanel.removeAll();

        try {
            List<Showtime> showtimes = movieService.showtimesOf(selectedMovie, selectedDate);
            if (showtimes.isEmpty()) {
                JLabel emptyLbl = new JLabel("No showtimes available for the selected date");
                emptyLbl.setFont(fontSmall);
                emptyLbl.setForeground(COLOR_TEXT_MUTED);
                showtimesListPanel.add(emptyLbl);
            } else {
                for (Showtime st : showtimes) {
                    boolean canBook = movieService.canBook(st);
                    JButton roundBtn = createShowtimeButton(st, canBook);
                    showtimesListPanel.add(roundBtn);
                }
            }
        } catch (IOException e) {
            JLabel errLbl = new JLabel("Cannot load showtimes: " + e.getMessage());
            errLbl.setFont(fontSmall);
            errLbl.setForeground(new Color(220, 38, 38));
            showtimesListPanel.add(errLbl);
        }

        showtimesListPanel.revalidate();
        showtimesListPanel.repaint();
    }

    /**
     * สร้างปุ่มรอบฉาย 1 ปุ่ม (รองรับสถานะ เลยเวลาแล้ว / พร้อมจอง)
     */
    private JButton createShowtimeButton(Showtime st, boolean canBook) {
        String timeStr = st.start().format(DateTimeFormatter.ofPattern("HH:mm"));
        String subStr = canBook ? "Hall " + st.hall() : "Hall " + st.hall() + " · Ended";

        JButton btn = new JButton("<html><div style='text-align: center; font-family: Tahoma;'>"
                + "<b>" + timeStr + "</b><br>"
                + "<span style='font-size: 10px;'>" + subStr + "</span>"
                + "</div></html>");
        btn.setPreferredSize(new Dimension(130, 44));
        btn.setFocusPainted(false);

        if (!canBook) {
            // รอบที่เลยเวลาแล้ว: สีเทาอ่อน ไม่สามารถคลิกได้
            btn.setEnabled(false);
            btn.setBackground(new Color(243, 244, 246));
            btn.setForeground(new Color(156, 163, 175));
            btn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_CARD_BORDER, 1, true),
                    new EmptyBorder(4, 6, 4, 6)));
        } else {
            // รอบที่พร้อมจอง: พื้นขาว กรอบสีหลัก คลิกได้
            btn.setEnabled(true);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBackground(Color.WHITE);
            btn.setForeground(COLOR_PRIMARY);
            btn.setBorder(new CompoundBorder(
                    new LineBorder(COLOR_PRIMARY, 1, true),
                    new EmptyBorder(4, 6, 4, 6)));

            btn.addActionListener(e -> {
                SeatFrame seatFrame = new SeatFrame(currentUser, appServices, st);
                seatFrame.setVisible(true);
                this.dispose();
            });
        }

        return btn;
    }

    /**
     * ปุ่มนำทาง < หรือ > สไตล์เรียบง่าย
     */
    private JButton createNavButton(String arrow) {
        JButton btn = new JButton(arrow);
        btn.setFont(new Font("Tahoma", Font.BOLD, 14));
        btn.setForeground(COLOR_TEXT_LABEL);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBorder(new CompoundBorder(
                new LineBorder(COLOR_INPUT_BORDER, 1, true),
                new EmptyBorder(2, 4, 2, 4)));
        return btn;
    }

    /**
     * จัดการ Logout ปิดหน้านี้แล้วเปิด LoginFrame
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            LoginFrame loginFrame = (appServices != null)
                    ? new LoginFrame(appServices)
                    : new LoginFrame(authService);
            loginFrame.setVisible(true);
        }
    }

    /**
     * กล่องภาพโปสเตอร์สไตล์เรียบง่าย รองรับทั้งไฟล์ภาพจริง และการ์ดสี A, B, C
     */
    private class PosterBox extends JPanel {
        private final Movie movie;
        private final int index;
        private final boolean isSelected;
        private BufferedImage image;

        public PosterBox(Movie movie, int index, boolean isSelected) {
            this.movie = movie;
            this.index = index;
            this.isSelected = isSelected;
            setPreferredSize(new Dimension(150, 200));
            setMaximumSize(new Dimension(150, 200));
            setOpaque(false);

            if (movie.posterPath() != null && !movie.posterPath().isBlank()) {
                try {
                    Path p = resolveDataPath(movie.posterPath());
                    if (Files.exists(p)) {
                        this.image = ImageIO.read(p.toFile());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = 2, y = 2;
            int w = getWidth() - 4;
            int h = getHeight() - 4;
            int arc = 10;

            // กรอบเส้นสีน้ำเงินเมื่อถูกเลือก
            if (isSelected) {
                g2.setColor(COLOR_PRIMARY);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(x - 1, y - 1, w + 2, h + 2, arc + 2, arc + 2);
            }

            // ถ้ามีรูปโปสเตอร์จริง ให้วาดรูปภาพ
            if (image != null) {
                g2.setClip(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
                g2.drawImage(image, x, y, w, h, null);
            } else {
                // ถ้าไม่มีรูปภาพ ให้แสดงการ์ดสีมินิมอล A, B, C...
                Color[] softColors = {
                        new Color(71, 85, 105),  // Slate Blue
                        new Color(109, 40, 217), // Violet
                        new Color(13, 148, 136), // Teal
                        new Color(217, 119, 6),  // Amber
                        new Color(37, 99, 235)   // Blue
                };
                Color color = softColors[index % softColors.length];
                g2.setColor(color);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Tahoma", Font.BOLD, 13));
                FontMetrics fm1 = g2.getFontMetrics();
                String text1 = "Poster";
                g2.drawString(text1, x + (w - fm1.stringWidth(text1)) / 2, y + (h / 2) - 10);

                char letter = (char) ('A' + (index % 26));
                g2.setFont(new Font("Tahoma", Font.BOLD, 36));
                FontMetrics fm2 = g2.getFontMetrics();
                String text2 = String.valueOf(letter);
                g2.drawString(text2, x + (w - fm2.stringWidth(text2)) / 2, y + (h / 2) + 28);
            }

            g2.dispose();
        }
    }

    /**
     * ค้นหาตำแหน่งไฟล์ข้อมูล รองรับทั้ง root directory และ subfolder
     */
    public static Path resolveDataPath(String fileName) {
        Path pathUnderSubfolder = Path.of("data", fileName);
        Path pathFromRoot = Path.of("Booking_tickets", "data", fileName);

        if (Files.exists(pathFromRoot)) return pathFromRoot;
        if (Files.exists(pathUnderSubfolder)) return pathUnderSubfolder;
        if (Files.exists(Path.of("Booking_tickets"))) return pathFromRoot;
        return pathUnderSubfolder;
    }

    /**
     * เมธอด main ให้สามารถทดสอบรันหน้าต่าง MainFrame นี้ได้โดยตรง
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            AppServices appServices = AppServices.create();
            User defaultUser = new User("thanawat", "password123");

            MainFrame frame = new MainFrame(defaultUser, appServices);
            frame.setVisible(true);
        });
    }
}
