//AdminFrame <- หน้าต่างหลักของผู้ดูแลระบบ (Admin Panel) พร้อมระบบจำลองเวลา
/*
    หน้าที่:
        1. ประกอบด้วยแถบบน (AdminTopBar), แผงจำลองเวลา (TimeSimulatorPanel) และการ์ดจัดตารางหนัง (LineupPanel)
        2. มีระบบจำลองเวลาให้ Admin กดเลื่อนไปวันพรุ่งนี้ (+1 วัน) เพื่อตรวจสอบว่าหนังฉายวันนี้เปลี่ยนตามที่ตั้งไว้หรือไม่
        3. มีปุ่มเปิดหน้าต่างจองตั๋วของลูกค้า (Customer View) เพื่อดูผลลัพธ์จริงบนหน้าจอของผู้ใช้
        4. มีระบบยืนยัน Logout เพื่อปิดหน้าต่างนี้แล้วกลับไปยัง LoginFrame
        5. มี main() method เพื่อให้สามารถรันและทดสอบหน้าจอ Admin ได้โดยตรง
 */

package gui.admin;

import gui.LoginFrame;
import gui.MainFrame;
import model.Role;
import model.User;
import service.AdminService;
import service.AppClock;
import service.AppServices;
import service.AuthService;
import service.MovieService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * หน้าต่างหลัก GUI สำหรับผู้ดูแลระบบ (Admin Dashboard)
 * ใช้สำหรับจัดตารางภาพยนตร์ 5 เรื่องของวันพรุ่งนี้ และจำลองเวลาเพื่อทดสอบระบบ
 */
public class AdminFrame extends JFrame {

    private final User currentUser;
    private final AppServices appServices;
    private final AdminService adminService;
    private final MovieService movieService;
    private final AuthService authService;
    private final AppClock clock;

    private LineupPanel lineupPanel;
    private TimeSimulatorPanel timeSimulatorPanel;

    public AdminFrame(User user, AppServices appServices) {
        this.currentUser = (user != null) ? user : new User("admin", "admin1", Role.ADMIN);
        this.appServices = (appServices != null) ? appServices : AppServices.create();
        this.adminService = this.appServices.admin();
        this.movieService = this.appServices.movies();
        this.authService = this.appServices.auth();
        this.clock = this.appServices.clock();

        initUI();
    }

    public AdminFrame(User user, AdminService adminService, MovieService movieService, AuthService authService, AppClock clock) {
        this.currentUser = (user != null) ? user : new User("admin", "admin1", Role.ADMIN);
        this.appServices = null;
        this.adminService = adminService;
        this.movieService = movieService;
        this.authService = authService;
        this.clock = clock;

        initUI();
    }

    public AdminFrame(User user, AdminService adminService, MovieService movieService, AuthService authService) {
        this(user, adminService, movieService, authService, (adminService != null) ? new AppClock() : null);
    }

    private void initUI() {
        setTitle("KU CINEMA - Admin Panel");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 780);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(AdminTheme.BACKGROUND);

        // 1. แถบด้านบน (Top Bar)
        rootPanel.add(new AdminTopBar(currentUser, this::handleLogout), BorderLayout.NORTH);

        // 2. พื้นที่เนื้อหาตรงกลาง (Center Content)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(AdminTheme.BACKGROUND);
        contentPanel.setBorder(new EmptyBorder(14, 20, 16, 20));

        // 2.1 แผงจำลองเวลา (Time Simulator Panel)
        timeSimulatorPanel = new TimeSimulatorPanel(clock, this::handleTimeChanged, this::openCustomerPreview);
        timeSimulatorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeSimulatorPanel.setMaximumSize(new Dimension(680, 85));
        contentPanel.add(timeSimulatorPanel);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // 2.2 การ์ดจัดตารางหนัง (LineupPanel)
        lineupPanel = new LineupPanel(currentUser, adminService, movieService);
        lineupPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lineupPanel);

        // ใช้ ScrollPane เพื่อความยืดหยุ่นในทุกขนาดจอ
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        rootPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    /**
     * ดำเนินการเมื่อเวลาในระบบ AppClock มีการเปลี่ยนแปลง
     */
    private void handleTimeChanged() {
        if (lineupPanel != null) {
            lineupPanel.refreshLineup();
        }
    }

    /**
     * เปิดหน้าต่างมุมมองลูกค้า (MainFrame) เพื่อทดสอบดูผลลัพธ์ว่ารอบและหนังเปลี่ยนจริงหรือไม่
     */
    private void openCustomerPreview() {
        try {
            AppServices services = (this.appServices != null) ? this.appServices : AppServices.create();
            MainFrame customerFrame = new MainFrame(currentUser, services);
            customerFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            customerFrame.setTitle("KU CINEMA - Customer View (Simulation Preview)");
            customerFrame.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot open customer view: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * จัดการ Logout ยืนยันแล้วกลับหน้า LoginFrame
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            AppServices services = AppServices.create();
            User admin = new User("admin", "admin1", Role.ADMIN);
            AdminFrame frame = new AdminFrame(admin, services);
            frame.setVisible(true);
        });
    }
}
