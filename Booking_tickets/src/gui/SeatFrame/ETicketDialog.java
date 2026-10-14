package gui.SeatFrame;

import gui.MainFrame.MainFrame;
import model.Booking;
import model.Showtime;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * หน้าต่างแสดงตั๋วภาพยนตร์จำลอง (Simulated E-Ticket) หลังชำระเงินสำเร็จ
 */
public class ETicketDialog extends JDialog {

    /**
     * คอนสตรัคเตอร์สร้างหน้าต่างตั๋วอิเล็กทรอนิกส์ (E-Ticket)
     * แสดงข้อมูลสรุปการจอง: รหัสการจอง, ภาพยนตร์, วันที่, รอบเวลา, โรงภาพยนตร์, ที่นั่ง, ยอดชำระ, และชื่อลูกค้า
     *
     * @param owner หน้าต่างหลักที่เป็นเจ้าของ Dialog
     * @param booking ข้อมูลการจองที่สร้างสำเร็จ
     * @param showtime ข้อมูลรอบฉายที่จอง
     * @param onDone Callback ทำงานเมื่อผู้ใช้กดปุ่ม Done เพื่อปิด Dialog และกลับสู่หน้าต่างหลัก
     */
    public ETicketDialog(Frame owner, Booking booking, Showtime showtime, Runnable onDone) {
        super(owner, "Electronic Ticket (E-Ticket)", true);
        setSize(440, 480);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel ticketPanel = new JPanel();
        ticketPanel.setLayout(new BoxLayout(ticketPanel, BoxLayout.Y_AXIS));
        ticketPanel.setBackground(Color.WHITE);
        ticketPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel brand = new JLabel("KU CINEMA - E-TICKET");
        brand.setFont(new Font("Tahoma", Font.BOLD, 16));
        brand.setForeground(MainFrame.COLOR_PRIMARY);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusBadge = new JLabel("Payment Successful (PAID)");
        statusBadge.setFont(MainFrame.FONT_LABEL);
        statusBadge.setForeground(new Color(16, 185, 129));
        statusBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLabel = new JLabel("Booking ID: " + booking.id());
        idLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        idLabel.setForeground(MainFrame.COLOR_TEXT_TITLE);
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
        doneBtn.setBackground(MainFrame.COLOR_PRIMARY);
        doneBtn.setForeground(Color.BLACK);
        doneBtn.setFocusPainted(false);
        doneBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        doneBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        doneBtn.setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_PRIMARY, 1, true),
                new EmptyBorder(8, 20, 8, 20)));
        doneBtn.addActionListener(e -> {
            dispose();
            if (onDone != null) onDone.run();
        });

        ticketPanel.add(doneBtn);

        setContentPane(ticketPanel);
    }

    /**
     * เมธอดเสริมสำหรับเพิ่มแถวข้อมูลตั๋ว 1 บรรทัด (Label ทางซ้าย และค่า Value ทางขวา)
     *
     * @param panel แผงที่จะเพิ่มแถวเข้าไป
     * @param label ข้อความหัวข้อด้านซ้าย
     * @param value ข้อความค่าข้อมูลด้านขวา
     */
    private void addTicketRow(JPanel panel, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(MainFrame.FONT_SMALL);
        lbl.setForeground(MainFrame.COLOR_TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(MainFrame.FONT_LABEL);
        val.setForeground(MainFrame.COLOR_TEXT_TITLE);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);

        panel.add(row);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    /**
     * สร้างเส้นแบ่งแนวนอน (Divider) สไตล์มินิมอล
     */
    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(MainFrame.COLOR_CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }
}
