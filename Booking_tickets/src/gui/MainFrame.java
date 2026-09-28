package gui;

import model.Movie;
import model.Showtime;
import model.User;
import repository.CsvMovieRepository;
import repository.CsvUserRepository;
import repository.MovieRepository;
import repository.UserRepository;
import service.AppClock;
import service.AuthService;
import service.MovieService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
 * ออกแบบในสไตล์เรียบง่าย (Clean Card Style) ถอดแบบโครงสร้างและโทนสีเดียวกับ LoginFrame
 * เพื่อให้โค้ดเข้าใจง่าย เป็นระเบียบ และพร้อมสำหรับการนำไปออกแบบต่อยอดในอนาคต
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final MovieService movieService;
    private final AuthService authService;

    // ข้อมูลภาพยนตร์และวันที่จากระบบหลังบ้าน
    private List<Movie> movieList = new ArrayList<>();
    private List<LocalDate> bookableDates = new ArrayList<>();

    // สถานะที่เลือกในปัจจุบัน
    private LocalDate selectedDate;
    private Movie selectedMovie;
    private int carouselStartIndex = 0; // ลำดับภาพยนตร์ตัวแรกใน 3 เรื่องที่มองเห็น

    // UI Components
    private JPanel dateButtonsPanel;
    private JPanel moviesPanel;
    private JPanel dotsPanel;
    private JLabel showtimesSectionLabel;
    private JPanel showtimesListPanel;

    // สีและฟอนต์มาตรฐานชุดเดียวกับ LoginFrame
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);      // Blue #2563EB (สีหลักแบบ LoginFrame)
    private static final Color COLOR_BG = new Color(245, 247, 250);          // พื้นหลังสีเทาอ่อน #F5F7FA
    private static final Color COLOR_CARD_BORDER = new Color(229, 231, 235); // สีกรอบการ์ด #E5E7EB
    private static final Color COLOR_INPUT_BORDER = new Color(209, 213, 219);// สีกรอบปุ่ม/ช่องข้อมูล #D1D5DB
    private static final Color COLOR_TEXT_TITLE = new Color(17, 24, 39);     // สีหัวข้อหลัก #111827
    private static final Color COLOR_TEXT_LABEL = new Color(55, 65, 81);     // สีป้ายชื่อฟิลด์ #374151
    private static final Color COLOR_TEXT_MUTED = new Color(107, 114, 128);  // สีข้อความรอง #6B7280

    private final Font fontHeader = new Font("Tahoma", Font.BOLD, 20);
    private final Font fontSubtitle = new Font("Tahoma", Font.PLAIN, 12);
    private final Font fontLabel = new Font("Tahoma", Font.BOLD, 12);
    private final Font fontText = new Font("Tahoma", Font.PLAIN, 13);
    private final Font fontSmall = new Font("Tahoma", Font.PLAIN, 11);

    public MainFrame(User user, MovieService movieService, AuthService authService) {
        this.currentUser = (user != null) ? user : new User("thanawat", "password123");
        this.movieService = movieService;
        this.authService = authService;

        loadData();
        initUI();
    }

    public MainFrame(User user, MovieService movieService) {
        this(user, movieService, null);
    }

    public MainFrame(User user) {
        this(user, createDefaultMovieService(), null);
    }

    public MainFrame() {
        this(new User("thanawat", "password123"), createDefaultMovieService(), null);
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
                        "เกิดข้อผิดพลาดในการโหลดรายการภาพยนตร์: " + e.getMessage(),
                        "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
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
     * สร้างหน้าจอหลักในรูปแบบการ์ดสีขาวตรงกลาง สไตล์เดียวกับ LoginFrame
     */
    private void initUI() {
        setTitle("ระบบจองตั๋ว - เลือกรอบภาพยนตร์");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 720);
        setLocationRelativeTo(null);
        setResizable(false);

        // 1. พื้นหลังหลัก (Main Background Panel สีเทาอ่อนเหมือน LoginFrame)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        // 2. การ์ดสีขาวตรงกลาง (Card Container)
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                new LineBorder(COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(20, 28, 20, 28)));

        // --- ส่วนหัว (Header Section) ---
        JLabel titleLabel = new JLabel("KU CINEMA - เลือกรอบภาพยนตร์");
        titleLabel.setFont(fontHeader);
        titleLabel.setForeground(COLOR_TEXT_TITLE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("ยินดีต้อนรับคุณ " + currentUser.username() + " | กรุณาเลือกภาพยนตร์และรอบฉาย");
        subtitleLabel.setFont(fontSubtitle);
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- ส่วนที่ 1: เลือกวันที่ (Date Section) ---
        JLabel dateSectionLabel = new JLabel("1. วันที่เข้าชม (Date)");
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
        JLabel movieSectionLabel = new JLabel("2. ภาพยนตร์ (Movie)");
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
        showtimesSectionLabel = new JLabel("3. รอบฉาย (Showtimes)");
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

        JLabel guideLabel = new JLabel("← คลิกที่รอบฉายเพื่อไปหน้าเลือกที่นั่ง");
        guideLabel.setFont(fontSmall);
        guideLabel.setForeground(COLOR_TEXT_MUTED);
        guideLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(guideLabel);

        cardPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        // --- ส่วนท้าย: ปุ่ม Logout ในสไตล์ Link แบบเดียวกับ toggleModeButton ใน LoginFrame ---
        JButton logoutButton = new JButton(
                "<html>เข้าสู่ระบบด้วยบัญชีอื่น? <font color='#2563EB'><b>ออกจากระบบ (Logout)</b></font></html>");
        logoutButton.setFont(fontSubtitle);
        logoutButton.setForeground(new Color(75, 85, 99));
        logoutButton.setBorderPainted(false);
        logoutButton.setContentAreaFilled(false);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.addActionListener(e -> handleLogout());

        cardPanel.add(logoutButton);

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        // อัปเดตข้อมูลครั้งแรก
        refreshDateButtons();
        refreshMovies();
        refreshShowtimes();
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
                    ? "วันนี้ " + date.getDayOfMonth() + "/" + date.getMonthValue()
                    : date.getDayOfMonth() + "/" + date.getMonthValue();

            JButton dateBtn = new JButton(labelText);
            dateBtn.setFont(new Font("Tahoma", isSelected ? Font.BOLD : Font.PLAIN, 12));
            dateBtn.setFocusPainted(false);
            dateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (isSelected) {
                dateBtn.setBackground(COLOR_PRIMARY);
                dateBtn.setForeground(Color.WHITE);
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
            moviesPanel.add(new JLabel("ไม่มีข้อมูลภาพยนตร์"));
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
        JLabel durLbl = new JLabel(movie.durationMinutes() + " นาที", SwingConstants.CENTER);
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
            showtimesSectionLabel.setText("3. รอบฉาย (Showtimes)");
            showtimesListPanel.removeAll();
            showtimesListPanel.revalidate();
            showtimesListPanel.repaint();
            return;
        }

        String dateText = (!bookableDates.isEmpty() && selectedDate.equals(bookableDates.get(0)))
                ? "วันนี้ " + selectedDate.getDayOfMonth() + "/" + selectedDate.getMonthValue()
                : selectedDate.getDayOfMonth() + "/" + selectedDate.getMonthValue();
        showtimesSectionLabel.setText("3. รอบฉาย: \"" + selectedMovie.title() + "\"  ·  " + dateText);

        showtimesListPanel.removeAll();

        try {
            List<Showtime> showtimes = movieService.showtimesOf(selectedMovie, selectedDate);
            if (showtimes.isEmpty()) {
                JLabel emptyLbl = new JLabel("ไม่มีรอบฉายสำหรับวันที่เลือก");
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
            JLabel errLbl = new JLabel("ไม่สามารถโหลดรอบฉายได้: " + e.getMessage());
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
        String subStr = canBook ? "โรง " + st.hall() : "โรง " + st.hall() + " · เลยเวลาแล้ว";

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
                // รอระบบเลือกที่นั่งและการจอง (ข้อ 9)
                JOptionPane.showMessageDialog(this,
                        "คุณเลือกรอบเวลา " + timeStr + " (โรง " + st.hall() + ") ของเรื่อง \"" + selectedMovie.title() + "\"\n\n" +
                                "(ระบบเลือกที่นั่งและการจองกำลังอยู่ระหว่างการพัฒนา รอระบบที่นั่งและการจอง)",
                        "เลือกรอบภาพยนตร์", JOptionPane.INFORMATION_MESSAGE);
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
                "คุณต้องการออกจากระบบหรือไม่?",
                "ยืนยันออกจากระบบ",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            this.dispose();
            AuthService auth = this.authService;
            if (auth == null) {
                Path userPath = resolveDataPath("users.csv");
                UserRepository userRepo = new CsvUserRepository(userPath);
                auth = new AuthService(userRepo);
            }
            LoginFrame loginFrame = new LoginFrame(auth);
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

            // กรอบเส้นสีน้ำเงินเมื่อถูกเลือก (แบบ LoginFrame focus)
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

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Tahoma", Font.BOLD, 13));
                FontMetrics fm1 = g2.getFontMetrics();
                String text1 = "โปสเตอร์";
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
     * สร้าง MovieService เริ่มต้นสำหรับกรณีเปิดใช้งานโดยตรง
     */
    private static MovieService createDefaultMovieService() {
        try {
            Path moviesPath = resolveDataPath("movies.csv");
            Path schedulePath = resolveDataPath("schedule.csv");
            MovieRepository repo = new CsvMovieRepository(moviesPath, schedulePath);
            AppClock clock = new AppClock();
            return new MovieService(repo, clock);
        } catch (Exception e) {
            return null;
        }
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
            Path moviesPath = resolveDataPath("movies.csv");
            Path schedulePath = resolveDataPath("schedule.csv");
            Path usersPath = resolveDataPath("users.csv");

            MovieRepository movieRepo = new CsvMovieRepository(moviesPath, schedulePath);
            AppClock clock = new AppClock();
            MovieService movieService = new MovieService(movieRepo, clock);

            UserRepository userRepo = new CsvUserRepository(usersPath);
            AuthService authService = new AuthService(userRepo);

            User defaultUser = new User("thanawat", "password123");

            MainFrame frame = new MainFrame(defaultUser, movieService, authService);
            frame.setVisible(true);
        });
    }
}
