package gui.MainFrame;

import gui.LoginFrame.LoginFrame;
import gui.SeatFrame.SeatFrame;
import model.Account;
import model.Movie;
import model.Showtime;
import model.User;
import service.AccountService;
import service.AppServices;
import service.AuthService;
import service.BookingService;
import service.MovieService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * หน้าต่างหลัก GUI สำหรับเลือกรอบภาพยนตร์ (Main Dashboard)
 * ทำหน้าที่เชื่อมโยงระหว่างข้อมูลภาพยนตร์, รอบฉาย, และคอมโพเนนต์ย่อยต่างๆ:
 * - MainTopBar: แถบเมนูด้านบน
 * - MovieCard / PosterBox: การ์ดแสดงภาพยนตร์
 * - MembershipDialog: หน้าต่างจัดการสมาชิก
 * - TopUpDialog: หน้าต่างเติมเงิน
 * - HistoryDialog: หน้าต่างประวัติการจอง
 */
public class MainFrame extends JFrame {

    // --- สีและฟอนต์มาตรฐานของระบบ ---
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue #2563EB
    public static final Color COLOR_BG = new Color(245, 247, 250);          // Gray #F5F7FA
    public static final Color COLOR_CARD_BORDER = new Color(229, 231, 235); // Border #E5E7EB
    public static final Color COLOR_INPUT_BORDER = new Color(209, 213, 219);// Border #D1D5DB
    public static final Color COLOR_TEXT_TITLE = new Color(17, 24, 39);     // Dark #111827
    public static final Color COLOR_TEXT_LABEL = new Color(55, 65, 81);     // Dark #374151
    public static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);  // Muted #6B7280

    public static final Font FONT_HEADER = new Font("Tahoma", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Tahoma", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Tahoma", Font.BOLD, 12);
    public static final Font FONT_TEXT = new Font("Tahoma", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Tahoma", Font.PLAIN, 11);

    // --- Services และข้อมูล ---
    private final User currentUser;
    private final AppServices appServices;
    private final MovieService movieService;
    private final AuthService authService;
    private final AccountService accountService;
    private final BookingService bookingService;

    private List<Movie> movieList = new ArrayList<>();
    private List<LocalDate> bookableDates = new ArrayList<>();

    // --- สถานะที่เลือกในปัจจุบัน ---
    private LocalDate selectedDate;
    private Movie selectedMovie;
    private int carouselStartIndex = 0;

    // --- UI Components ---
    private MainTopBar topBar;
    private JPanel dateButtonsPanel;
    private JPanel moviesPanel;
    private JPanel dotsPanel;
    private JLabel showtimesSectionLabel;
    private JPanel showtimesListPanel;

    /**
     * คอนสตรัคเตอร์หลักสำหรับเปิดหน้าจอหลัก โดยรับข้อมูลผู้ใช้และ AppServices
     *
     * @param user ผู้ใช้งานที่ล็อกอินเข้ามา
     * @param appServices เซอร์วิสส่วนกลางของระบบ
     */
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

    /**
     * คอนสตรัคเตอร์รองรับการส่ง MovieService และ AuthService แยกต่างหาก
     *
     * @param user ผู้ใช้งาน
     * @param movieService เซอร์วิสข้อมูลภาพยนตร์และรอบฉาย
     * @param authService เซอร์วิสระบบยืนยันตัวตน
     */
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

    /**
     * คอนสตรัคเตอร์สำหรับผู้ใช้งานและ MovieService
     *
     * @param user ผู้ใช้งาน
     * @param movieService เซอร์วิสข้อมูลภาพยนตร์
     */
    public MainFrame(User user, MovieService movieService) {
        this(user, movieService, null);
    }

    /**
     * คอนสตรัคเตอร์สำหรับผู้ใช้งาน โดยดึงเซอร์วิสอัตโนมัติ
     *
     * @param user ผู้ใช้งาน
     */
    public MainFrame(User user) {
        this(user, null, null);
    }

    /**
     * คอนสตรัคเตอร์แบบไม่มีพารามิเตอร์ ใช้ผู้ใช้ทดสอบเริ่มต้น
     */
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
     * ประกอบหน้าจอหลักด้วยการ์ดเนื้อหาตรงกลางและแถบด้านบน
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

        // 1. แถบด้านบน (MainTopBar)
        topBar = new MainTopBar(
                currentUser,
                this::showMembershipDialog,
                this::showTopUpDialog,
                this::showHistoryDialog,
                this::handleLogout
        );
        rootPanel.add(topBar, BorderLayout.NORTH);

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
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(COLOR_TEXT_TITLE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Welcome " + currentUser.username() + " | Please select a movie and showtime");
        subtitleLabel.setFont(FONT_SUBTITLE);
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- ส่วนที่ 1: เลือกวันที่ (Date Section) ---
        JLabel dateSectionLabel = new JLabel("1. Date");
        dateSectionLabel.setFont(FONT_LABEL);
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
        movieSectionLabel.setFont(FONT_LABEL);
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
        showtimesSectionLabel.setFont(FONT_LABEL);
        showtimesSectionLabel.setForeground(COLOR_TEXT_LABEL);
        showtimesSectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(showtimesSectionLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        showtimesListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        showtimesListPanel.setOpaque(false);
        showtimesListPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        showtimesListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        cardPanel.add(showtimesListPanel);

        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel guideLabel = new JLabel("← Click on a showtime to select seats");
        guideLabel.setFont(FONT_SMALL);
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
     * อัปเดตยอดเงินและปุ่มสมาชิกบนแถบด้านบน
     */
    private void refreshAccountBar() {
        if (topBar == null) return;
        if (accountService == null) {
            topBar.updateAccount(null, false);
            return;
        }

        try {
            Account account = accountService.accountOf(currentUser);
            boolean isMember = accountService.isMember(currentUser);
            topBar.updateAccount(account, isMember);
        } catch (Exception e) {
            topBar.updateAccount(null, false);
        }
    }

    /**
     * เปิดหน้าต่างจัดการสมาชิก
     */
    private void showMembershipDialog() {
        MembershipDialog dialog = new MembershipDialog(
                this,
                currentUser,
                accountService,
                this::refreshAccountBar,
                this::showTopUpDialog
        );
        dialog.setVisible(true);
    }

    /**
     * เปิดหน้าต่างเติมเงิน
     */
    private void showTopUpDialog() {
        TopUpDialog dialog = new TopUpDialog(
                this,
                currentUser,
                accountService,
                this::refreshAccountBar
        );
        dialog.setVisible(true);
    }

    /**
     * เปิดหน้าต่างประวัติการจองตั๋ว
     */
    private void showHistoryDialog() {
        HistoryDialog dialog = new HistoryDialog(
                this,
                currentUser,
                bookingService
        );
        dialog.setVisible(true);
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
                MovieCard movieCard = new MovieCard(movie, movieIndex, isSelected, () -> {
                    selectedMovie = movie;
                    refreshMovies();
                    refreshShowtimes();
                });
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
                emptyLbl.setFont(FONT_SMALL);
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
            errLbl.setFont(FONT_SMALL);
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
            User defaultUser = new User("-", "-");

            MainFrame frame = new MainFrame(defaultUser, appServices);
            frame.setVisible(true);
        });
    }
}
