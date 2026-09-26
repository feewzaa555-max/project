package gui;

import exception.AuthField;
import exception.AuthFormException;
import model.User;
import repository.CsvUserRepository;
import repository.UserRepository;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * หน้าต่าง GUI สำหรับ Login / Sign up ของระบบจองตั๋ว (Booking Tickets)
 * ทำงานร่วมกับ AuthService, UserRepository, AuthField และ AuthFormException
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private boolean isLoginMode = true; // true = Login, false = Sign up

    // UI Components
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel usernameErrorLabel;
    private JLabel passwordErrorLabel;
    private JButton submitButton;
    private JButton toggleModeButton;

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setTitle("ระบบจองตั๋ว - เข้าสู่ระบบ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Background Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Card Container
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(229, 231, 235), 1, true),
                new EmptyBorder(25, 25, 25, 25)));

        Font fontHeader = new Font("Tahoma", Font.BOLD, 20);
        Font fontSubtitle = new Font("Tahoma", Font.PLAIN, 12);
        Font fontLabel = new Font("Tahoma", Font.BOLD, 12);
        Font fontInput = new Font("Tahoma", Font.PLAIN, 13);
        Font fontError = new Font("Tahoma", Font.PLAIN, 11);
        Font fontButton = new Font("Tahoma", Font.BOLD, 14);

        // --- Header Section ---
        titleLabel = new JLabel("เข้าสู่ระบบ");
        titleLabel.setFont(fontHeader);
        titleLabel.setForeground(new Color(17, 24, 39));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel = new JLabel("ยินดีต้อนรับสู่ระบบจองตั๋ว Booking Tickets");
        subtitleLabel.setFont(fontSubtitle);
        subtitleLabel.setForeground(new Color(107, 114, 128));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // --- Username Section ---
        JLabel userLabel = new JLabel("ชื่อผู้ใช้ (Username)");
        userLabel.setFont(fontLabel);
        userLabel.setForeground(new Color(55, 65, 81));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField = new JTextField();
        usernameField.setFont(fontInput);
        usernameField.setPreferredSize(new Dimension(320, 36));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setBorder(new CompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        usernameErrorLabel = new JLabel("");
        usernameErrorLabel.setFont(fontError);
        usernameErrorLabel.setForeground(new Color(220, 38, 38));
        usernameErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameErrorLabel.setVisible(false);

        cardPanel.add(userLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        cardPanel.add(usernameErrorLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- Password Section ---
        JLabel passLabel = new JLabel("รหัสผ่าน (Password)");
        passLabel.setFont(fontLabel);
        passLabel.setForeground(new Color(55, 65, 81));
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(fontInput);
        passwordField.setPreferredSize(new Dimension(320, 36));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setBorder(new CompoundBorder(
                new LineBorder(new Color(209, 213, 219), 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        passwordErrorLabel = new JLabel("");
        passwordErrorLabel.setFont(fontError);
        passwordErrorLabel.setForeground(new Color(220, 38, 38));
        passwordErrorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordErrorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        passwordErrorLabel.setHorizontalAlignment(SwingConstants.LEFT);
        passwordErrorLabel.setVisible(false);

        cardPanel.add(passLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        cardPanel.add(passwordErrorLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Submit Button ---
        submitButton = new JButton("เข้าสู่ระบบ");
        submitButton.setFont(fontButton);
        submitButton.setForeground(Color.BLACK);
        submitButton.setBackground(new Color(37, 99, 235)); // Blue
        submitButton.setFocusPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.setPreferredSize(new Dimension(320, 40));
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        cardPanel.add(submitButton);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // --- Mode Toggle Button ---
        toggleModeButton = new JButton(
                "<html>ยังไม่มีบัญชีใช่หรือไม่? <font color='#2563EB'><b>สมัครสมาชิก</b></font></html>");
        toggleModeButton.setFont(fontSubtitle);
        toggleModeButton.setForeground(new Color(75, 85, 99));
        toggleModeButton.setBorderPainted(false);
        toggleModeButton.setContentAreaFilled(false);
        toggleModeButton.setFocusPainted(false);
        toggleModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleModeButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(toggleModeButton);

        mainPanel.add(cardPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        // --- Event Listeners ---
        submitButton.addActionListener(e -> handleSubmit());
        toggleModeButton.addActionListener(e -> toggleMode());

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSubmit();
                }
            }
        };
        usernameField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);
    }

    /**
     * สลับโหมดระหว่าง Login และ Sign up
     */
    private void toggleMode() {
        isLoginMode = !isLoginMode;
        clearErrors();

        if (isLoginMode) {
            setTitle("ระบบจองตั๋ว - เข้าสู่ระบบ");
            titleLabel.setText("เข้าสู่ระบบ");
            subtitleLabel.setText("ยินดีต้อนรับสู่ระบบจองตั๋ว Booking Tickets");
            submitButton.setText("เข้าสู่ระบบ");
            submitButton.setBackground(new Color(37, 99, 235));
            toggleModeButton
                    .setText("<html>ยังไม่มีบัญชีใช่หรือไม่? <font color='#2563EB'><b>สมัครสมาชิก</b></font></html>");
        } else {
            setTitle("ระบบจองตั๋ว - สมัครสมาชิก");
            titleLabel.setText("สมัครสมาชิกใหม่");
            subtitleLabel.setText("สร้างบัญชีเพื่อเริ่มต้นจองตั๋วเดินทาง");
            submitButton.setText("สร้างบัญชีผู้ใช้");
            submitButton.setBackground(new Color(16, 185, 129)); // Green
            toggleModeButton
                    .setText("<html>มีบัญชีอยู่แล้วใช่หรือไม่? <font color='#10B981'><b>เข้าสู่ระบบ</b></font></html>");
        }
    }

    /**
     * ล้างข้อความ Error ทั้งหมดบนหน้าจอ
     */
    private void clearErrors() {
        usernameErrorLabel.setText("");
        usernameErrorLabel.setVisible(false);
        passwordErrorLabel.setText("");
        passwordErrorLabel.setVisible(false);
    }

    /**
     * ดำเนินการส่งข้อมูลฟอร์มไปยัง AuthService
     */
    private void handleSubmit() {
        clearErrors();

        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            if (isLoginMode) {
                // เรียก Service ตรวจสอบ Login
                User user = authService.login(username, password);
                JOptionPane.showMessageDialog(
                        this,
                        "Login successful!\nWelcome: " + user.username(),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                // ในระบบจริงจะเปิดหน้าต่างหลัก (Main Booking Dashboard) ต่อไปที่นี่
            } else {
                // เรียก Service ตรวจสอบ Sign up
                User user = authService.register(username, password);
                JOptionPane.showMessageDialog(
                        this,
                        "Registration successful!\nUsername: " + user.username() + "\nPlease log in to use the service",
                        "Registration successful",
                        JOptionPane.INFORMATION_MESSAGE);
                // สลับกลับมาหน้า Login ให้อัตโนมัติ พร้อมคง username ไว้
                toggleMode();
                passwordField.setText("");
            }
        } catch (AuthFormException ex) {
            // ดึงข้อความแจ้งเตือนตาม Field ที่ AuthFormException เตรียมไว้
            List<String> usernameErrors = ex.messagesFor(AuthField.USERNAME);
            if (!usernameErrors.isEmpty()) {
                usernameErrorLabel.setText("<html>• " + String.join("<br>• ", usernameErrors) + "</html>");
                usernameErrorLabel.setVisible(true);
            }

            List<String> passwordErrors = ex.messagesFor(AuthField.PASSWORD);
            if (!passwordErrors.isEmpty()) {
                passwordErrorLabel.setText("<html>• " + String.join("<br>• ", passwordErrors) + "</html>");
                passwordErrorLabel.setVisible(true);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "เกิดข้อผิดพลาดในการเชื่อมต่อไฟล์ข้อมูล: " + ex.getMessage(),
                    "ข้อผิดพลาดระบบ",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ค้นหาตำแหน่งไฟล์ users.csv โดยรองรับทั้งการรันจาก root directory และ
     * subfolder
     */
    private static Path resolveDataPath() {
        Path pathUnderSubfolder = Path.of("data", "users.csv");
        Path pathFromRoot = Path.of("Booking_tickets", "data", "users.csv");

        if (Files.exists(pathFromRoot)) {
            return pathFromRoot;
        }
        if (Files.exists(pathUnderSubfolder)) {
            return pathUnderSubfolder;
        }
        // ถ้ายังไม่มีไฟล์ ให้ดูว่ารันจากโฟลเดอร์ไหน
        if (Files.exists(Path.of("Booking_tickets"))) {
            return pathFromRoot;
        }
        return pathUnderSubfolder;
    }

    public static void main(String[] args) {
        // ใช้ System Look and Feel เพื่อให้ UI สวยงามตามระบบปฏิบัติการ
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // เตรียม Repository และ Service
        Path csvPath = resolveDataPath();
        UserRepository userRepository = new CsvUserRepository(csvPath);
        AuthService authService = new AuthService(userRepository);

        // เปิดหน้าต่าง GUI
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame(authService);
            frame.setVisible(true);
        });
    }
}
