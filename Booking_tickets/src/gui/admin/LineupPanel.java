//LineupPanel <- การ์ดกลางของหน้า admin สำหรับเลือกหนัง 5 เรื่องที่จะฉายในวันพรุ่งนี้
/*
    หน้าที่:
        1. โหลดรายชื่อหนังทั้งหมดจาก MovieService มาใส่ใน JComboBox ทั้ง 5 ช่อง
        2. แสดงรูปโปสเตอร์ของหนังที่กำลังฉายวันนี้ (Now Showing Today) ให้เห็นชัดเจน
        3. แสดงรูปโปสเตอร์พรีวิวข้างช่องเลือกหนังของวันพรุ่งนี้ อัปเดตแบบเรียลไทม์เมื่อเปลี่ยนเรื่อง
        4. ใช้ MovieRenderer แสดงชื่อหนังและความยาว พร้อมไอคอนรูปใน Dropdown
        5. ตรวจสอบกฎความถูกต้องก่อนบันทึก: เลือกครบ 5 เรื่อง และไม่มีเรื่องที่เลือกซ้ำกัน
        6. บันทึกชุดหนังลงไฟล์ผ่าน AdminService.setTomorrowLineup()
        7. มีเมธอด refreshLineup() ให้ TimeSimulatorPanel เรียกเพื่ออัปเดตหน้าจอเมื่อเวลาเปลี่ยน
 */

package gui.admin;

import gui.MainFrame;
import model.Movie;
import model.User;
import service.AdminService;
import service.MovieService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * พาเนลการ์ดจัดการชุดภาพยนตร์ที่จะฉายในวันพรุ่งนี้ (Tomorrow's Lineup)
 * พร้อมแสดงรูปภาพโปสเตอร์ทั้งของวันนี้และวันพรุ่งนี้แบบอินเตอร์แอคทีฟ
 */
final class LineupPanel extends JPanel {

    private final User admin;
    private final AdminService adminService;
    private final MovieService movieService;

    private final List<JComboBox<Movie>> movieBoxes = new ArrayList<>();
    private final List<JLabel> slotPosterLabels = new ArrayList<>();
    private List<Movie> allMovies = new ArrayList<>();

    private JLabel subtitleLabel;
    private JLabel smallNoteLabel;
    private JLabel todayHeaderLabel;
    private JPanel todayMoviesPillPanel;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, ImageIcon> POSTER_CACHE = new HashMap<>();

    LineupPanel(User admin, AdminService adminService, MovieService movieService) {
        this.admin = admin;
        this.adminService = adminService;
        this.movieService = movieService;

        loadData();
        initUI();
    }

    private void loadData() {
        try {
            if (movieService != null) {
                this.allMovies = movieService.movies();
            }
        } catch (IOException ex) {
            this.allMovies = new ArrayList<>();
            JOptionPane.showMessageDialog(this,
                    "Failed to load movie list: " + ex.getMessage(),
                    "Data Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(16, 28, 16, 28)));
        setMaximumSize(new Dimension(720, 680));
        setPreferredSize(new Dimension(680, 640));

        // ==========================================
        // 1. ส่วนแสดงรูปหนังที่ฉายวันนี้ (Now Showing Today)
        // ==========================================
        JPanel todayCard = new JPanel(new BorderLayout(8, 6));
        todayCard.setBackground(new Color(248, 250, 252));
        todayCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        todayCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        todayHeaderLabel = new JLabel("Now Showing Today");
        todayHeaderLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
        todayHeaderLabel.setForeground(new Color(30, 41, 59));

        todayMoviesPillPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        todayMoviesPillPanel.setOpaque(false);

        todayCard.add(todayHeaderLabel, BorderLayout.NORTH);
        todayCard.add(todayMoviesPillPanel, BorderLayout.CENTER);

        add(todayCard);
        add(Box.createRigidArea(new Dimension(0, 12)));

        // ==========================================
        // 2. ส่วนหัวจัดตารางหนังพรุ่งนี้ (Tomorrow's Lineup)
        // ==========================================
        JLabel headerLabel = new JLabel("Tomorrow's Lineup");
        headerLabel.setFont(AdminTheme.FONT_HEADER);
        headerLabel.setForeground(AdminTheme.TEXT_DARK);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        LocalDate tomorrow = (adminService != null) ? adminService.tomorrow() : LocalDate.now().plusDays(1);
        String dateStr = tomorrow.format(DATE_FORMATTER);

        subtitleLabel = new JLabel("Showing on " + dateStr);
        subtitleLabel.setFont(AdminTheme.FONT_SUBTITLE);
        subtitleLabel.setForeground(AdminTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(headerLabel);
        add(Box.createRigidArea(new Dimension(0, 2)));
        add(subtitleLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));

        // ==========================================
        // 3. ฟอร์มเลือกหนัง 5 เรื่อง พร้อมรูปโปสเตอร์พรีวิว
        // ==========================================
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        List<Movie> currentLineup = getCurrentTomorrowLineup();

        for (int i = 0; i < AdminService.LINEUP_SIZE; i++) {
            final int slotIndex = i;

            // ป้าย "Movie 1:"
            JLabel slotLabel = new JLabel("Movie " + (i + 1) + ":");
            slotLabel.setFont(AdminTheme.FONT_LABEL);
            slotLabel.setForeground(AdminTheme.TEXT_DARK);
            slotLabel.setPreferredSize(new Dimension(65, 36));

            // ช่องรูปโปสเตอร์พรีวิวขนาดเล็กข้าง Dropdown
            JLabel posterPreview = new JLabel();
            posterPreview.setPreferredSize(new Dimension(28, 38));
            posterPreview.setBorder(new LineBorder(new Color(229, 231, 235), 1, true));
            posterPreview.setHorizontalAlignment(SwingConstants.CENTER);
            slotPosterLabels.add(posterPreview);

            // Dropdown เลือกหนัง
            JComboBox<Movie> box = new JComboBox<>(new DefaultComboBoxModel<>(allMovies.toArray(new Movie[0])));
            box.setFont(AdminTheme.FONT_TEXT);
            box.setRenderer(new MovieRenderer());
            box.setBackground(Color.WHITE);
            box.setPreferredSize(new Dimension(380, 38));

            // เลือกค่าเริ่มต้นตาม lineup ของวันพรุ่งนี้
            if (currentLineup != null && i < currentLineup.size()) {
                Movie currentMovie = currentLineup.get(i);
                for (int j = 0; j < box.getItemCount(); j++) {
                    Movie item = box.getItemAt(j);
                    if (item != null && item.id().equals(currentMovie.id())) {
                        box.setSelectedIndex(j);
                        break;
                    }
                }
            } else if (i < allMovies.size()) {
                box.setSelectedIndex(i);
            }

            // เมื่อเปลี่ยนเรื่อง ให้รูปโปสเตอร์พรีวิวอัปเดตตามทันที
            box.addActionListener(e -> updateSlotPosterPreview(slotIndex));
            movieBoxes.add(box);

            // อัปเดตรูปเริ่มต้น
            updateSlotPosterPreview(slotIndex);

            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            formPanel.add(slotLabel, gbc);

            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.weightx = 0;
            formPanel.add(posterPreview, gbc);

            gbc.gridx = 2;
            gbc.gridy = i;
            gbc.weightx = 1.0;
            formPanel.add(box, gbc);
        }

        add(formPanel);
        add(Box.createRigidArea(new Dimension(0, 12)));

        // ==========================================
        // 4. ปุ่ม Actions (Save & Reset)
        // ==========================================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveButton = new JButton("Save Lineup");
        saveButton.setFont(AdminTheme.FONT_BUTTON);
        saveButton.setBackground(AdminTheme.PRIMARY);
        saveButton.setForeground(Color.BLACK);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.setPreferredSize(new Dimension(160, 40));
        saveButton.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.PRIMARY, 1, true),
                new EmptyBorder(8, 20, 8, 20)));

        saveButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                saveButton.setBackground(new Color(29, 78, 216));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                saveButton.setBackground(AdminTheme.PRIMARY);
            }
        });

        saveButton.addActionListener(e -> handleSave());

        JButton resetButton = new JButton("Reset");
        resetButton.setFont(AdminTheme.FONT_BUTTON);
        resetButton.setBackground(Color.WHITE);
        resetButton.setForeground(AdminTheme.TEXT_DARK);
        resetButton.setFocusPainted(false);
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetButton.setPreferredSize(new Dimension(100, 40));
        resetButton.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        resetButton.addActionListener(e -> resetSelection());

        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);
        add(buttonPanel);

        add(Box.createRigidArea(new Dimension(0, 8)));

        // คำอธิบายตัวเล็กใต้ปุ่ม Save
        smallNoteLabel = new JLabel("* Changes will take effect on tomorrow's schedule (" + dateStr + ")");
        smallNoteLabel.setFont(AdminTheme.FONT_SMALL);
        smallNoteLabel.setForeground(AdminTheme.TEXT_MUTED);
        smallNoteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(smallNoteLabel);

        // โหลดรูปหนังที่ฉายวันนี้
        updateTodayMoviesDisplay();
    }

    /**
     * อัปเดตรูปโปสเตอร์พรีวิวข้าง Dropdown แต่ละช่อง
     */
    private void updateSlotPosterPreview(int index) {
        if (index >= 0 && index < movieBoxes.size() && index < slotPosterLabels.size()) {
            Movie selected = (Movie) movieBoxes.get(index).getSelectedItem();
            JLabel lbl = slotPosterLabels.get(index);
            if (selected != null && selected.posterPath() != null) {
                ImageIcon icon = loadScaledPoster(selected.posterPath(), 26, 36);
                lbl.setIcon(icon);
            } else {
                lbl.setIcon(null);
            }
        }
    }

    /**
     * อัปเดตการแสดงผลการ์ดรูปภาพโปสเตอร์ของหนังที่กำลังฉายวันนี้
     */
    private void updateTodayMoviesDisplay() {
        if (todayMoviesPillPanel == null) return;
        todayMoviesPillPanel.removeAll();

        List<Movie> todayList = List.of();
        try {
            if (movieService != null) {
                todayList = movieService.moviesToday();
            }
        } catch (IOException ignored) {
        }

        LocalDate todayDate = (adminService != null) ? adminService.tomorrow().minusDays(1) : LocalDate.now();
        todayHeaderLabel.setText("Now Showing Today (" + todayDate.format(DATE_FORMATTER) + ") - หนังที่ฉายวันนี้:");

        if (todayList.isEmpty()) {
            JLabel emptyLbl = new JLabel("No movies currently scheduled for today.");
            emptyLbl.setFont(AdminTheme.FONT_SMALL);
            emptyLbl.setForeground(AdminTheme.TEXT_MUTED);
            todayMoviesPillPanel.add(emptyLbl);
        } else {
            for (int i = 0; i < todayList.size(); i++) {
                Movie m = todayList.get(i);
                JPanel card = new JPanel(new BorderLayout(0, 3));
                card.setOpaque(false);
                card.setPreferredSize(new Dimension(80, 110));

                ImageIcon icon = loadScaledPoster(m.posterPath(), 58, 80);
                JLabel imgLabel;
                if (icon != null) {
                    imgLabel = new JLabel(icon);
                } else {
                    imgLabel = new JLabel("No Image", SwingConstants.CENTER);
                    imgLabel.setFont(new Font("Tahoma", Font.PLAIN, 10));
                    imgLabel.setPreferredSize(new Dimension(58, 80));
                    imgLabel.setBackground(new Color(226, 232, 240));
                    imgLabel.setOpaque(true);
                }
                imgLabel.setBorder(new LineBorder(new Color(203, 213, 225), 1, true));
                imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

                // ชื่อหนังตัดให้พอดีกับการ์ด
                String shortTitle = m.title().length() > 10 ? m.title().substring(0, 9) + "..." : m.title();
                JLabel titleLabel = new JLabel("#" + (i + 1) + " " + shortTitle, SwingConstants.CENTER);
                titleLabel.setFont(new Font("Tahoma", Font.BOLD, 10));
                titleLabel.setForeground(new Color(30, 41, 59));
                titleLabel.setToolTipText(m.title() + " (" + m.durationMinutes() + " min)");

                card.add(imgLabel, BorderLayout.CENTER);
                card.add(titleLabel, BorderLayout.SOUTH);
                todayMoviesPillPanel.add(card);
            }
        }

        todayMoviesPillPanel.revalidate();
        todayMoviesPillPanel.repaint();
    }

    /**
     * โหลดรูปภาพโปสเตอร์และย่อขนาดตามที่ต้องการ พร้อมแคชไว้ในหน่วยความจำ
     */
    static ImageIcon loadScaledPoster(String posterPath, int width, int height) {
        if (posterPath == null || posterPath.isBlank()) return null;
        String cacheKey = posterPath + "@" + width + "x" + height;
        return POSTER_CACHE.computeIfAbsent(cacheKey, k -> {
            try {
                Path p = MainFrame.resolveDataPath(posterPath);
                if (Files.exists(p)) {
                    BufferedImage img = ImageIO.read(p.toFile());
                    if (img != null) {
                        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    /**
     * รีเฟรชทั้งพาเนลเมื่อเวลาในระบบ (AppClock) มีการเปลี่ยนแปลง
     */
    public void refreshLineup() {
        updateTodayMoviesDisplay();

        LocalDate tomorrow = (adminService != null) ? adminService.tomorrow() : LocalDate.now().plusDays(1);
        String dateStr = tomorrow.format(DATE_FORMATTER);

        if (subtitleLabel != null) {
            subtitleLabel.setText("Showing on " + dateStr);
        }
        if (smallNoteLabel != null) {
            smallNoteLabel.setText("* Changes will take effect on tomorrow's schedule (" + dateStr + ")");
        }

        resetSelection();
    }

    private List<Movie> getCurrentTomorrowLineup() {
        if (adminService == null) return List.of();
        try {
            return adminService.tomorrowLineup();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private void resetSelection() {
        List<Movie> currentLineup = getCurrentTomorrowLineup();
        for (int i = 0; i < movieBoxes.size(); i++) {
            JComboBox<Movie> box = movieBoxes.get(i);
            if (currentLineup != null && i < currentLineup.size()) {
                Movie currentMovie = currentLineup.get(i);
                for (int j = 0; j < box.getItemCount(); j++) {
                    Movie item = box.getItemAt(j);
                    if (item != null && item.id().equals(currentMovie.id())) {
                        box.setSelectedIndex(j);
                        break;
                    }
                }
            } else if (i < allMovies.size()) {
                box.setSelectedIndex(i);
            }
            updateSlotPosterPreview(i);
        }
    }

    private void handleSave() {
        if (adminService == null) {
            JOptionPane.showMessageDialog(this,
                    "AdminService is not available.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Movie> selected = new ArrayList<>();
        for (JComboBox<Movie> box : movieBoxes) {
            Movie m = (Movie) box.getSelectedItem();
            if (m != null) {
                selected.add(m);
            }
        }

        // ตรวจสอบว่าเลือกครบ 5 เรื่องไหม
        if (selected.size() != AdminService.LINEUP_SIZE) {
            JOptionPane.showMessageDialog(this,
                    "Please select exactly " + AdminService.LINEUP_SIZE + " movies.",
                    "Incomplete Selection",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ตรวจสอบเรื่องซ้ำ
        Set<String> seenIds = new HashSet<>();
        for (Movie m : selected) {
            if (!seenIds.add(m.id())) {
                JOptionPane.showMessageDialog(this,
                        "Movie \"" + m.title() + "\" is selected more than once.\nPlease select " + AdminService.LINEUP_SIZE + " distinct movies.",
                        "Duplicate Movie",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // บันทึกผ่าน AdminService
        try {
            adminService.setTomorrowLineup(admin, selected);
            String dateStr = adminService.tomorrow().format(DATE_FORMATTER);
            JOptionPane.showMessageDialog(this,
                    "Tomorrow's lineup (" + dateStr + ") has been saved successfully!\n\n"
                            + "Tip: You can use 'Advance +1 Day' in the Time Simulator above\n"
                            + "to verify that these movies become today's showing movies.",
                    "Saved Successfully",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error saving lineup: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
