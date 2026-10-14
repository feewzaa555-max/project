package gui.LoginFrame;

import exception.AuthFormException;
import gui.MainFrame.MainFrame;
import model.User;
import service.AppServices;
import service.AuthService;

import javax.swing.*;
import java.io.IOException;

/**
 * Controller สำหรับจัดการเหตุการณ์และ Business Logic ของหน้า Login / Sign up
 * ทำหน้าที่เชื่อมโยงระหว่าง LoginFrame (View) และ AuthService (Service)
 */
public class LoginController {

    private final LoginFrame view;
    private final AppServices appServices;
    private final AuthService authService;

    /**
     * คอนสตรัคเตอร์เริ่มต้น LoginController โดยรับ AppServices
     *
     * @param view หน้าต่าง LoginFrame (View) ที่คอนโทรลเลอร์นี้ดูแล
     * @param appServices เซอร์วิสส่วนกลางของระบบ เพื่อดึง AuthService มาใช้งาน
     */
    public LoginController(LoginFrame view, AppServices appServices) {
        this.view = view;
        this.appServices = appServices;
        this.authService = (appServices != null) ? appServices.auth() : null;
    }

    /**
     * คอนสตรัคเตอร์เริ่มต้น LoginController โดยรับ AuthService โดยตรง
     *
     * @param view หน้าต่าง LoginFrame (View)
     * @param authService เซอร์วิสจัดการระบบยืนยันตัวตน
     */
    public LoginController(LoginFrame view, AuthService authService) {
        this.view = view;
        this.appServices = null;
        this.authService = authService;
    }

    /**
     * ดำเนินการส่งข้อมูลฟอร์มไปยัง AuthService (Login หรือ Sign Up)
     *
     * @param isLoginMode โหมดปัจจุบัน (true = Login, false = Sign Up)
     * @param username    ชื่อผู้ใช้ที่กรอก
     * @param password    รหัสผ่านที่กรอก
     */
    public void handleSubmit(boolean isLoginMode, String username, String password) {
        view.clearErrors();

        if (authService == null) {
            JOptionPane.showMessageDialog(
                    view,
                    "AuthService is not available.",
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (isLoginMode) {
                // เรียก Service ตรวจสอบ Login
                User user = authService.login(username, password);
                JOptionPane.showMessageDialog(
                        view,
                        "Login successful!\nWelcome: " + user.username(),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                // เปิดหน้าต่างหลัก (MainFrame)
                openMainDashboard(user);
            } else {
                // เรียก Service ตรวจสอบ Sign up
                User user = authService.register(username, password);
                JOptionPane.showMessageDialog(
                        view,
                        "Registration successful!\nUsername: " + user.username() + "\nPlease log in to use the service",
                        "Registration successful",
                        JOptionPane.INFORMATION_MESSAGE);
                // แจ้งให้ View สลับกลับมาหน้า Login พร้อมล้างช่องรหัสผ่าน
                view.onRegisterSuccess();
            }
        } catch (AuthFormException ex) {
            // ส่งต่อข้อผิดพลาดไปให้ View แสดงผลใต้ช่องที่เกี่ยวข้อง
            view.displayFormErrors(ex);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    view,
                    "An error occurred connecting to data files: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * เปิดหน้าต่างหลัก MainFrame เมื่อเข้าสู่ระบบสำเร็จ
     *
     * @param user ผู้ใช้ที่ล็อกอินสำเร็จ
     */
    public void openMainDashboard(User user) {
        try {
            AppServices services = (this.appServices != null) ? this.appServices : AppServices.create();
            MainFrame mainFrame = new MainFrame(user, services);
            mainFrame.setVisible(true);
            view.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    view,
                    "Error loading data for main screen: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
