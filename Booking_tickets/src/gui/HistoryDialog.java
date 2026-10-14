package gui;

import model.Booking;
import model.Showtime;
import model.User;
import service.BookingService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * หน้าต่างแสดงประวัติการจองตั๋วภาพยนตร์
 */
public class HistoryDialog extends JDialog {

    public HistoryDialog(Frame owner, User currentUser, BookingService bookingService) {
        super(owner, "Booking History", true);

        if (bookingService == null) {
            JOptionPane.showMessageDialog(owner, "BookingService not available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<Booking> history = bookingService.historyOf(currentUser);

            setSize(620, 480);
            setLocationRelativeTo(owner);

            JPanel mainDialogPanel = new JPanel(new BorderLayout());
            mainDialogPanel.setBackground(MainFrame.COLOR_BG);
            mainDialogPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

            // ส่วนหัว Dialog
            JLabel title = new JLabel("Booking History (" + (currentUser != null ? currentUser.username() : "") + ")");
            title.setFont(new Font("Tahoma", Font.BOLD, 16));
            title.setForeground(MainFrame.COLOR_TEXT_TITLE);
            title.setBorder(new EmptyBorder(0, 0, 12, 0));
            mainDialogPanel.add(title, BorderLayout.NORTH);

            if (history.isEmpty()) {
                JPanel emptyPanel = new JPanel(new GridBagLayout());
                emptyPanel.setBackground(Color.WHITE);
                emptyPanel.setBorder(new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true));

                JLabel emptyLabel = new JLabel("No booking history yet");
                emptyLabel.setFont(MainFrame.FONT_TEXT);
                emptyLabel.setForeground(MainFrame.COLOR_TEXT_MUTED);
                emptyPanel.add(emptyLabel);

                mainDialogPanel.add(emptyPanel, BorderLayout.CENTER);
            } else {
                JPanel listPanel = new JPanel();
                listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
                listPanel.setBackground(MainFrame.COLOR_BG);

                DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (Booking b : history) {
                    JPanel itemCard = new JPanel(new BorderLayout(8, 6));
                    itemCard.setBackground(Color.WHITE);
                    itemCard.setBorder(new CompoundBorder(
                            new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true),
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
                    movieLabel.setFont(MainFrame.FONT_LABEL);
                    movieLabel.setForeground(MainFrame.COLOR_PRIMARY);

                    JLabel timeLabel = new JLabel(showtimeText);
                    timeLabel.setFont(MainFrame.FONT_SUBTITLE);
                    timeLabel.setForeground(MainFrame.COLOR_TEXT_TITLE);

                    JLabel seatLabel = new JLabel("Seats: " + String.join(", ", b.seatCodes()) + " | Booked: " + b.bookedAt().format(dtFormat));
                    seatLabel.setFont(MainFrame.FONT_SMALL);
                    seatLabel.setForeground(MainFrame.COLOR_TEXT_MUTED);

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
            closeBtn.setFont(MainFrame.FONT_LABEL);
            closeBtn.setBackground(Color.WHITE);
            closeBtn.setForeground(MainFrame.COLOR_TEXT_LABEL);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.setBorder(new CompoundBorder(
                    new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true),
                    new EmptyBorder(6, 16, 6, 16)));
            closeBtn.addActionListener(e -> dispose());
            bottomPanel.add(closeBtn);

            mainDialogPanel.add(bottomPanel, BorderLayout.SOUTH);

            setContentPane(mainDialogPanel);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, "Cannot retrieve booking history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
