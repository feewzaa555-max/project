package gui.LoginFrame;

import exception.AuthField;
import exception.AuthFormException;
import service.AppServices;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * หน้าต่าง GUI สำหรับ Login / Sign up ของระบบจองตั๋ว (Booking Tickets)
 * ทำหน้าที่เป็น View หลักในการจัดวางหน้าจอ และส่งต่อ Action ให้กับ LoginController
 */
public class LoginFrame extends JFrame {

    private final LoginController controller;
    private boolean isLoginMode = true; // true = Login, false = Sign up

    // UI Components
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private AuthInputField usernameField;
    private AuthInputField passwordField;
    private JButton submitButton;
    private JButton toggleModeButton;

    /**
     * คอนสตรัคเตอร์เริ่มต้นหน้าต่าง LoginFrame โดยสร้าง AppServices ให้อัตโนมัติ
     */
    public LoginFrame() {
        this(AppServices.create());
    }

    /**
     * คอนสตรัคเตอร์เริ่มต้นหน้าต่าง LoginFrame โดยรับ AppServices
     *
     * @param appServices เซอร์วิสส่วนกลางของแอปพลิเคชัน
     */
    public LoginFrame(AppServices appServices) {
        this.controller = new LoginController(this, appServices);
        initUI();
    }

    /**
     * คอนสตรัคเตอร์เริ่มต้นหน้าต่าง LoginFrame โดยรับ AuthService
     *
     * @param authService เซอร์วิสยืนยันตัวตน
     */
    public LoginFrame(AuthService authService) {
        this.controller = new LoginController(this, authService);
        initUI();
    }

    /**
     * กำหนดการแสดงผล UI ของหน้าจอ Login / Sign up ประกอบด้วยการ์ดกลาง, ช่องกรอก, และปุ่มกด
     */
    private void initUI() {
        setTitle("Booking Tickets - Sign In");
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
        Font fontButton = new Font("Tahoma", Font.BOLD, 14);

        // --- Header Section ---
        titleLabel = new JLabel("Sign In");
        titleLabel.setFont(fontHeader);
        titleLabel.setForeground(new Color(17, 24, 39));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel = new JLabel("Welcome to Booking Tickets system");
        subtitleLabel.setFont(fontSubtitle);
        subtitleLabel.setForeground(new Color(107, 114, 128));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // --- Username Section (AuthInputField) ---
        usernameField = new AuthInputField("Username", false);
        cardPanel.add(usernameField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // --- Password Section (AuthInputField) ---
        passwordField = new AuthInputField("Password", true);
        cardPanel.add(passwordField);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- Submit Button ---
        submitButton = new JButton("Sign In");
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
                "<html>Don't have an account? <font color='#2563EB'><b>Sign Up</b></font></html>");
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
    public void toggleMode() {
        isLoginMode = !isLoginMode;
        clearErrors();

        if (isLoginMode) {
            setTitle("Booking Tickets - Sign In");
            titleLabel.setText("Sign In");
            subtitleLabel.setText("Welcome to Booking Tickets system");
            submitButton.setText("Sign In");
            submitButton.setBackground(new Color(37, 99, 235));
            toggleModeButton
                    .setText("<html>Don't have an account? <font color='#2563EB'><b>Sign Up</b></font></html>");
        } else {
            setTitle("Booking Tickets - Sign Up");
            titleLabel.setText("Sign Up");
            subtitleLabel.setText("Create an account to start booking tickets");
            submitButton.setText("Create Account");
            submitButton.setBackground(new Color(16, 185, 129)); // Green
            toggleModeButton
                    .setText("<html>Already have an account? <font color='#10B981'><b>Sign In</b></font></html>");
        }
    }

    /**
     * ล้างข้อความ Error ทั้งหมดบนหน้าจอ
     */
    public void clearErrors() {
        usernameField.clearError();
        passwordField.clearError();
    }

    /**
     * แสดงข้อความ Error ตามที่ AuthFormException ระบุ
     */
    public void displayFormErrors(AuthFormException ex) {
        usernameField.setErrors(ex.messagesFor(AuthField.USERNAME));
        passwordField.setErrors(ex.messagesFor(AuthField.PASSWORD));
    }

    /**
     * ทำงานหลังจากสมัครสมาชิกสำเร็จ
     */
    public void onRegisterSuccess() {
        toggleMode();
        passwordField.clear();
    }

    /**
     * ดำเนินการส่งข้อมูลฟอร์มไปยัง Controller
     */
    private void handleSubmit() {
        controller.handleSubmit(isLoginMode, usernameField.getText(), passwordField.getText());
    }

    /**
     * ค้นหาตำแหน่งไฟล์ข้อมูล โดยรองรับทั้งการรันจาก root directory และ subfolder
     */
    public static Path resolveDataPath(String fileName) {
        Path pathUnderSubfolder = Path.of("data", fileName);
        Path pathFromRoot = Path.of("Booking_tickets", "data", fileName);

        if (Files.exists(pathFromRoot)) {
            return pathFromRoot;
        }
        if (Files.exists(pathUnderSubfolder)) {
            return pathUnderSubfolder;
        }
        if (Files.exists(Path.of("Booking_tickets"))) {
            return pathFromRoot;
        }
        return pathUnderSubfolder;
    }

    /**
     * จุดเริ่มต้นโปรแกรม (Entry Point) สำหรับเปิดหน้าต่าง Login
     * ตั้งค่า Look and Feel และเปิดหน้าต่าง LoginFrame ผ่าน Event Dispatch Thread
     *
     * @param args อาร์กิวเมนต์จาก Command line (ไม่ได้ใช้งาน)
     */
    public static void main(String[] args) {
        // ใช้ System Look and Feel เพื่อให้ UI สวยงามตามระบบปฏิบัติการ
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // เปิดหน้าต่าง GUI
        SwingUtilities.invokeLater(() -> {
            AppServices appServices = AppServices.create();
            LoginFrame frame = new LoginFrame(appServices);
            frame.setVisible(true);
        });
    }
}
