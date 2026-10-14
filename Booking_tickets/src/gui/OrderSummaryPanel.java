package gui;

import model.Seat;
import model.SeatType;
import service.PriceCalculator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * แผงสรุปคำสั่งซื้อและยอดเงิน (Order Summary Panel)
 * ประกอบด้วย:
 * 1. จำนวนที่นั่งที่เลือก (Standard / VIP)
 * 2. การแจกแจงราคาและส่วนลดสมาชิก (10%)
 * 3. สถานะเงินในกระเป๋า (ก่อนและหลังหัก)
 * 4. ปุ่มยืนยันการจองตั๋ว
 */
public class OrderSummaryPanel extends JPanel {

    private final PriceCalculator priceCalculator;
    private final Runnable onConfirmBooking;

    private JLabel seatCountLabel;
    private JLabel selectedSeatsTextLabel;
    private JLabel standardCountLabel;
    private JLabel vipCountLabel;
    private JLabel regularTotalLabel;
    private JLabel memberStatusLabel;
    private JLabel discountLabel;
    private JLabel netTotalLabel;
    private JLabel currentBalanceLabel;
    private JLabel balanceAfterLabel;
    private JLabel balanceWarningLabel;
    private JButton confirmBookingButton;

    public OrderSummaryPanel(PriceCalculator priceCalculator, Runnable onConfirmBooking) {
        this.priceCalculator = priceCalculator;
        this.onConfirmBooking = onConfirmBooking;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        setPreferredSize(new Dimension(320, 520));

        initUI();
    }

    private void initUI() {
        // 1. หัวข้อ
        JLabel heading = new JLabel("Order Summary");
        heading.setFont(MainFrame.FONT_HEADER);
        heading.setForeground(MainFrame.COLOR_TEXT_TITLE);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(heading);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createDivider());
        add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. หมวดจำนวนที่นั่ง (Quantity)
        JLabel qtyTitle = new JLabel("Quantity");
        qtyTitle.setFont(MainFrame.FONT_LABEL);
        qtyTitle.setForeground(MainFrame.COLOR_TEXT_TITLE);
        qtyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(qtyTitle);
        add(Box.createRigidArea(new Dimension(0, 6)));

        seatCountLabel = new JLabel("Seats: 0");
        seatCountLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
        seatCountLabel.setForeground(MainFrame.COLOR_PRIMARY);
        seatCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(seatCountLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));

        selectedSeatsTextLabel = new JLabel("Seats: (None)");
        selectedSeatsTextLabel.setFont(MainFrame.FONT_TEXT);
        selectedSeatsTextLabel.setForeground(MainFrame.COLOR_TEXT_LABEL);
        selectedSeatsTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(selectedSeatsTextLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));

        standardCountLabel = new JLabel("• Standard: 0");
        standardCountLabel.setFont(MainFrame.FONT_SMALL);
        standardCountLabel.setForeground(MainFrame.COLOR_TEXT_MUTED);
        standardCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(standardCountLabel);

        vipCountLabel = new JLabel("• VIP: 0");
        vipCountLabel.setFont(MainFrame.FONT_SMALL);
        vipCountLabel.setForeground(SeatFrame.COLOR_VIP_TEXT);
        vipCountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(vipCountLabel);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createDivider());
        add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. หมวดราคา (Price Breakdown)
        JLabel priceTitle = new JLabel("Price Breakdown");
        priceTitle.setFont(MainFrame.FONT_LABEL);
        priceTitle.setForeground(MainFrame.COLOR_TEXT_TITLE);
        priceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(priceTitle);
        add(Box.createRigidArea(new Dimension(0, 6)));

        regularTotalLabel = new JLabel("Regular Price: 0 THB");
        regularTotalLabel.setFont(MainFrame.FONT_TEXT);
        regularTotalLabel.setForeground(MainFrame.COLOR_TEXT_LABEL);
        regularTotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(regularTotalLabel);
        add(Box.createRigidArea(new Dimension(0, 3)));

        memberStatusLabel = new JLabel("Status: Checking");
        memberStatusLabel.setFont(MainFrame.FONT_SMALL);
        memberStatusLabel.setForeground(MainFrame.COLOR_TEXT_MUTED);
        memberStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(memberStatusLabel);
        add(Box.createRigidArea(new Dimension(0, 3)));

        discountLabel = new JLabel("Member Discount (10%): -0 THB");
        discountLabel.setFont(MainFrame.FONT_TEXT);
        discountLabel.setForeground(new Color(16, 185, 129));
        discountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(discountLabel);
        add(Box.createRigidArea(new Dimension(0, 6)));

        netTotalLabel = new JLabel("Total Amount: 0 THB");
        netTotalLabel.setFont(new Font("Tahoma", Font.BOLD, 17));
        netTotalLabel.setForeground(MainFrame.COLOR_PRIMARY);
        netTotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(netTotalLabel);

        add(Box.createRigidArea(new Dimension(0, 10)));
        add(createDivider());
        add(Box.createRigidArea(new Dimension(0, 10)));

        // 4. หมวดกระเป๋าเงิน (Wallet)
        currentBalanceLabel = new JLabel("Wallet Balance: 0 THB");
        currentBalanceLabel.setFont(MainFrame.FONT_TEXT);
        currentBalanceLabel.setForeground(MainFrame.COLOR_TEXT_LABEL);
        currentBalanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(currentBalanceLabel);
        add(Box.createRigidArea(new Dimension(0, 3)));

        balanceAfterLabel = new JLabel("Balance After Payment: 0 THB");
        balanceAfterLabel.setFont(MainFrame.FONT_TEXT);
        balanceAfterLabel.setForeground(MainFrame.COLOR_TEXT_MUTED);
        balanceAfterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(balanceAfterLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));

        balanceWarningLabel = new JLabel("");
        balanceWarningLabel.setFont(MainFrame.FONT_SMALL);
        balanceWarningLabel.setForeground(new Color(220, 38, 38));
        balanceWarningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        balanceWarningLabel.setVisible(false);
        add(balanceWarningLabel);

        add(Box.createVerticalGlue());

        // 5. ปุ่มยืนยันการจองตั๋ว
        confirmBookingButton = new JButton("Confirm Booking");
        confirmBookingButton.setFont(new Font("Tahoma", Font.BOLD, 14));
        confirmBookingButton.setBackground(MainFrame.COLOR_PRIMARY);
        confirmBookingButton.setForeground(Color.BLACK);
        confirmBookingButton.setFocusPainted(false);
        confirmBookingButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBookingButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        confirmBookingButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBookingButton.setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_PRIMARY, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        confirmBookingButton.setEnabled(false);
        confirmBookingButton.addActionListener(e -> {
            if (onConfirmBooking != null) {
                onConfirmBooking.run();
            }
        });

        add(confirmBookingButton);
    }

    /**
     * อัปเดตข้อมูลสรุปราคาและสถานะปุ่มยืนยัน
     */
    public void updateSummary(List<Seat> selectedSeats, int currentBalance, boolean isMember) {
        int count = selectedSeats.size();
        seatCountLabel.setText("Seats: " + count);

        if (count == 0) {
            selectedSeatsTextLabel.setText("Seats: (None)");
            standardCountLabel.setText("• Standard: 0");
            vipCountLabel.setText("• VIP: 0");
        } else {
            List<String> codes = selectedSeats.stream().map(Seat::code).toList();
            selectedSeatsTextLabel.setText("Seats: " + String.join(", ", codes));

            long standardCount = selectedSeats.stream().filter(s -> s.type() == SeatType.STANDARD).count();
            long vipCount = selectedSeats.stream().filter(s -> s.type() == SeatType.VIP).count();

            standardCountLabel.setText("• Standard: " + standardCount + " (" + (standardCount * 160) + " THB)");
            vipCountLabel.setText("• VIP: " + vipCount + " (" + (vipCount * 200) + " THB)");
        }

        int regularTotal = priceCalculator.totalOf(selectedSeats, false);
        int netTotal = priceCalculator.totalOf(selectedSeats, isMember);
        int discount = regularTotal - netTotal;

        regularTotalLabel.setText("Regular Price: " + regularTotal + " THB");
        memberStatusLabel.setText(isMember ? "Status: Member (10% discount applied)" : "Status: Guest (No discount)");
        discountLabel.setText("Member Discount: -" + discount + " THB");
        netTotalLabel.setText("Total Amount: " + netTotal + " THB");

        currentBalanceLabel.setText("Wallet Balance: " + currentBalance + " THB");

        int remainingBalance = currentBalance - netTotal;
        balanceAfterLabel.setText("Balance After Payment: " + (count > 0 ? remainingBalance : currentBalance) + " THB");

        // ตรวจสอบเงื่อนไขปุ่มยืนยัน
        if (count == 0) {
            confirmBookingButton.setEnabled(false);
            confirmBookingButton.setText("Please select seat(s)");
            balanceWarningLabel.setVisible(false);
        } else if (currentBalance < netTotal) {
            confirmBookingButton.setEnabled(false);
            confirmBookingButton.setText("Insufficient Balance");
            int needed = netTotal - currentBalance;
            balanceWarningLabel.setText("<html>Need " + needed + " THB more (Please top up)</html>");
            balanceWarningLabel.setVisible(true);
        } else {
            confirmBookingButton.setEnabled(true);
            confirmBookingButton.setText("Confirm Booking (" + netTotal + " THB)");
            balanceWarningLabel.setVisible(false);
        }
    }

    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(MainFrame.COLOR_CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }
}
