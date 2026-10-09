//TimeSimulatorPanel <- แผงควบคุมจำลองเวลาของโปรแกรมสำหรับหน้า Admin
/*
    หน้าที่:
        1. แสดงเวลาปัจจุบันของระบบ (AppClock) ว่าเป็นเวลาจริงหรือเวลาจำลอง
        2. มีปุ่มลัดเลื่อนเวลาไปวันพรุ่งนี้ (+1 วัน) เพื่อดูว่าชุดหนังของพรุ่งนี้เปลี่ยนมาเป็นหนังของวันนี้จริงไหม
        3. มีปุ่มเลื่อนเวลาทีละชั่วโมง (+1 ชม.) และปุ่มตั้งวันที่เองได้ตามต้องการ
        4. มีปุ่ม Reset เพื่อคืนค่ากลับสู่เวลาจริงของเครื่องคอมพิวเตอร์
        5. มีปุ่มเปิดหน้าต่างมุมมองลูกค้า (Customer View) เพื่อดูว่าหน้าจองตั๋วแสดงหนังเรื่องไหนตามเวลาจำลอง
 */

package gui.admin;

import service.AppClock;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * แผงจำลองเวลา (Time Simulator) สำหรับทดสอบระบบตารางฉายหนัง
 */
final class TimeSimulatorPanel extends JPanel {

    private final AppClock clock;
    private final Runnable onTimeChanged;
    private final Runnable onPreviewCustomer;

    private boolean isSimulated = false;
    private JLabel timeDisplayLabel;
    private JLabel statusBadgeLabel;
    private JButton resetBtn;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    TimeSimulatorPanel(AppClock clock, Runnable onTimeChanged, Runnable onPreviewCustomer) {
        this.clock = clock;
        this.onTimeChanged = onTimeChanged;
        this.onPreviewCustomer = onPreviewCustomer;

        initUI();
        updateTimeDisplay();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 10));
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(new Color(219, 234, 254), 1, true), // Blue tint border
                new EmptyBorder(12, 20, 12, 20)));

        // --- แถวด้านบน: หัวข้อ + แสดงเวลาปัจจุบัน ---
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        // ซ้าย: ป้ายหัวข้อและสถานะ
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Time Simulator (จำลองเวลา)");
        titleLabel.setFont(AdminTheme.FONT_LABEL);
        titleLabel.setForeground(AdminTheme.TEXT_DARK);

        statusBadgeLabel = new JLabel("(Real Time)");
        statusBadgeLabel.setFont(AdminTheme.FONT_SMALL);
        statusBadgeLabel.setForeground(AdminTheme.TEXT_MUTED);

        titlePanel.add(titleLabel);
        titlePanel.add(statusBadgeLabel);

        // ขวา: แสดงเวลาปัจจุบันของโปรแกรม
        timeDisplayLabel = new JLabel("Clock: --/--/---- --:--");
        timeDisplayLabel.setFont(new Font("Tahoma", Font.BOLD, 13));
        timeDisplayLabel.setForeground(AdminTheme.PRIMARY);

        topRow.add(titlePanel, BorderLayout.WEST);
        topRow.add(timeDisplayLabel, BorderLayout.EAST);
        add(topRow, BorderLayout.NORTH);

        // --- แถวล่าง: ปุ่มควบคุมการเปลี่ยนเวลา ---
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);

        // ปุ่ม 1: +1 วัน (ไปวันพรุ่งนี้) - ปุ่มเด่นสุดสำหรับทดสอบ
        JButton plusOneDayBtn = createButton("Advance +1 Day (To Tomorrow)", AdminTheme.PRIMARY, Color.BLACK);
        plusOneDayBtn.setToolTipText("Advance clock by 1 day to simulate tomorrow");
        plusOneDayBtn.addActionListener(e -> advanceTimeDays(1));

        // ปุ่ม 2: +1 ชั่วโมง
        JButton plusOneHourBtn = createButton("+1 Hour", Color.WHITE, AdminTheme.TEXT_DARK);
        plusOneHourBtn.addActionListener(e -> advanceTimeHours(1));

        // ปุ่ม 3: กำหนดวันที่เอง
        JButton setCustomBtn = createButton("Set Date...", Color.WHITE, AdminTheme.TEXT_DARK);
        setCustomBtn.addActionListener(e -> showCustomDateDialog());

        // ปุ่ม 4: Reset กลับเวลาจริง
        resetBtn = createButton("Reset Time", AdminTheme.LOGOUT_BACKGROUND, AdminTheme.LOGOUT_TEXT);
        resetBtn.addActionListener(e -> resetTimeToReal());

        // ปุ่ม 5: ดูหน้าลูกค้า (Customer Preview)
        JButton previewCustomerBtn = createButton("Customer View", new Color(236, 253, 245), new Color(5, 150, 105));
        previewCustomerBtn.setToolTipText("Open customer screen to see today's movies and showtimes");
        previewCustomerBtn.addActionListener(e -> {
            if (onPreviewCustomer != null) {
                onPreviewCustomer.run();
            }
        });

        buttonRow.add(plusOneDayBtn);
        buttonRow.add(plusOneHourBtn);
        buttonRow.add(setCustomBtn);
        buttonRow.add(resetBtn);
        buttonRow.add(previewCustomerBtn);

        add(buttonRow, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(AdminTheme.FONT_LABEL);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(6, 12, 6, 12)));
        return btn;
    }

    /**
     * เลื่อนเวลาไปข้างหน้าตามจำนวนวันที่ระบุ
     */
    private void advanceTimeDays(int days) {
        if (clock == null) return;
        LocalDateTime newTime = clock.now().plusDays(days);
        clock.setTo(newTime);
        isSimulated = true;
        updateTimeDisplay();
        notifyTimeChanged();
    }

    /**
     * เลื่อนเวลาไปข้างหน้าตามจำนวนชั่วโมงที่ระบุ
     */
    private void advanceTimeHours(int hours) {
        if (clock == null) return;
        LocalDateTime newTime = clock.now().plusHours(hours);
        clock.setTo(newTime);
        isSimulated = true;
        updateTimeDisplay();
        notifyTimeChanged();
    }

    /**
     * รีเซ็ตกลับเป็นเวลาจริงของเครื่อง
     */
    private void resetTimeToReal() {
        if (clock == null) return;
        clock.reset();
        isSimulated = false;
        updateTimeDisplay();
        notifyTimeChanged();
    }

    /**
     * กล่องเลือกกำหนดวันเวลาจำลองเอง
     */
    private void showCustomDateDialog() {
        if (clock == null) return;

        LocalDateTime current = clock.now();
        JTextField dateField = new JTextField(current.toLocalDate().toString(), 10);
        JTextField timeField = new JTextField(current.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), 6);

        JPanel dialogPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        dialogPanel.add(new JLabel("Date (yyyy-MM-dd):"));
        dialogPanel.add(dateField);
        dialogPanel.add(new JLabel("Time (HH:mm):"));
        dialogPanel.add(timeField);

        int result = JOptionPane.showConfirmDialog(
                this,
                dialogPanel,
                "Set Simulated Date & Time",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDate date = LocalDate.parse(dateField.getText().trim());
                LocalTime time = LocalTime.parse(timeField.getText().trim());
                LocalDateTime custom = LocalDateTime.of(date, time);
                clock.setTo(custom);
                isSimulated = true;
                updateTimeDisplay();
                notifyTimeChanged();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid date or time format!\nPlease use yyyy-MM-dd (e.g. 2026-10-10) and HH:mm (e.g. 14:00)",
                        "Format Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * อัปเดตข้อความแสดงเวลาบนหน้าจอ
     */
    public void updateTimeDisplay() {
        if (clock == null) {
            timeDisplayLabel.setText("Clock: Unavailable");
            return;
        }

        LocalDateTime now = clock.now();
        timeDisplayLabel.setText("Clock: " + now.format(TIME_FORMATTER));

        if (isSimulated) {
            statusBadgeLabel.setText("(Simulated)");
            statusBadgeLabel.setForeground(new Color(217, 119, 6)); // Amber #D97706
            resetBtn.setEnabled(true);
        } else {
            statusBadgeLabel.setText("(Real Time)");
            statusBadgeLabel.setForeground(AdminTheme.TEXT_MUTED);
            resetBtn.setEnabled(false);
        }
    }

    private void notifyTimeChanged() {
        if (onTimeChanged != null) {
            onTimeChanged.run();
        }
    }
}
