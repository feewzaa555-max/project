package gui;

import model.HallLayout;
import model.Seat;
import model.SeatType;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * แผงผังที่นั่งในโรงภาพยนตร์ (Cinema Hall Panel)
 * ประกอบด้วย:
 * 1. จอภาพ (SCREEN) โค้งด้านบน
 * 2. ผังที่นั่ง 6 แถว (A ถึง F) แถวละ 10 ที่นั่ง
 * 3. แถบคำอธิบายสัญลักษณ์ประเภทที่นั่ง (Legend)
 */
public class HallPanel extends JPanel {

    private final HallLayout hallLayout;
    private final Set<String> bookedSeatCodes;
    private final List<Seat> selectedSeats = new ArrayList<>();
    private final Map<String, JButton> seatButtons = new HashMap<>();
    private final Runnable onSelectionChanged;

    public HallPanel(HallLayout hallLayout, Set<String> bookedSeatCodes, Runnable onSelectionChanged) {
        super(new BorderLayout(0, 10));
        this.hallLayout = hallLayout;
        this.bookedSeatCodes = bookedSeatCodes;
        this.onSelectionChanged = onSelectionChanged;

        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        initUI();
    }

    private void initUI() {
        // 1. จอภาพ (SCREEN) โค้งด้านบน
        JPanel screenBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth() - 60;
                int h = 18;
                int x = 30;
                int y = 4;

                g2.setColor(new Color(239, 246, 255)); // Light Blue
                g2.fillRoundRect(x, y, w, h, 10, 10);
                g2.setColor(MainFrame.COLOR_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(x, y, w, h, 10, 10);

                g2.setColor(MainFrame.COLOR_PRIMARY);
                g2.setFont(new Font("Tahoma", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                String text = "SCREEN";
                int tx = (getWidth() - fm.stringWidth(text)) / 2;
                int ty = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        screenBar.setPreferredSize(new Dimension(540, 30));
        screenBar.setOpaque(false);
        add(screenBar, BorderLayout.NORTH);

        // 2. ผังที่นั่ง 6 แถว (A ถึง F) เรียงติดกันไม่มีเว้นตรงกลาง
        JPanel gridContainer = new JPanel();
        gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
        gridContainer.setOpaque(false);

        for (char row : hallLayout.rows()) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
            rowPanel.setOpaque(false);

            // ป้ายชื่อแถวฝั่งซ้าย
            JLabel leftRowLbl = new JLabel(String.valueOf(row), SwingConstants.CENTER);
            leftRowLbl.setFont(MainFrame.FONT_LABEL);
            leftRowLbl.setForeground(row == 'F' ? SeatFrame.COLOR_VIP_TEXT : MainFrame.COLOR_TEXT_MUTED);
            leftRowLbl.setPreferredSize(new Dimension(20, 34));
            rowPanel.add(leftRowLbl);

            for (Seat seat : hallLayout.seatsInRow(row)) {
                JButton seatBtn = createSeatButton(seat);
                seatButtons.put(seat.code(), seatBtn);
                rowPanel.add(seatBtn);
            }

            // ป้ายชื่อแถวฝั่งขวา
            JLabel rightRowLbl = new JLabel(String.valueOf(row), SwingConstants.CENTER);
            rightRowLbl.setFont(MainFrame.FONT_LABEL);
            rightRowLbl.setForeground(row == 'F' ? SeatFrame.COLOR_VIP_TEXT : MainFrame.COLOR_TEXT_MUTED);
            rightRowLbl.setPreferredSize(new Dimension(20, 34));
            rowPanel.add(rightRowLbl);

            gridContainer.add(rowPanel);
        }

        add(gridContainer, BorderLayout.CENTER);

        // 3. คำอธิบายสัญลักษณ์ที่นั่ง (Legend) ด้านล่าง
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

        legendPanel.add(createLegendItem(Color.WHITE, MainFrame.COLOR_INPUT_BORDER, MainFrame.COLOR_TEXT_TITLE, "Standard (160 THB)"));
        legendPanel.add(createLegendItem(SeatFrame.COLOR_VIP_BG, SeatFrame.COLOR_VIP_BORDER, SeatFrame.COLOR_VIP_TEXT, "VIP Row F (200 THB)"));
        legendPanel.add(createLegendItem(MainFrame.COLOR_PRIMARY, MainFrame.COLOR_PRIMARY, Color.BLACK, "Selected"));
        legendPanel.add(createLegendItem(new Color(243, 244, 246), MainFrame.COLOR_CARD_BORDER, new Color(156, 163, 175), "Booked"));

        add(legendPanel, BorderLayout.SOUTH);
    }

    private JButton createSeatButton(Seat seat) {
        String code = seat.code();
        boolean isBooked = bookedSeatCodes != null && bookedSeatCodes.contains(code);
        boolean isVIP = (seat.type() == SeatType.VIP);

        JButton btn = new JButton(String.valueOf(seat.number()));
        btn.setFont(new Font("Tahoma", Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(38, 34));
        btn.setFocusPainted(false);

        if (isBooked) {
            btn.setEnabled(false);
            btn.setBackground(new Color(243, 244, 246));
            btn.setForeground(new Color(156, 163, 175));
            btn.setBorder(new LineBorder(MainFrame.COLOR_CARD_BORDER, 1, true));
            btn.setToolTipText(code + " (Booked)");
        } else {
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(code + " - " + (isVIP ? "VIP (200 THB / Member 180 THB)" : "Standard (160 THB / Member 144 THB)"));
            updateSeatButtonStyle(btn, seat, false);

            btn.addActionListener(e -> toggleSeatSelection(seat, btn));
        }

        return btn;
    }

    private void toggleSeatSelection(Seat seat, JButton btn) {
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat);
            updateSeatButtonStyle(btn, seat, false);
        } else {
            selectedSeats.add(seat);
            updateSeatButtonStyle(btn, seat, true);
        }
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    private void updateSeatButtonStyle(JButton btn, Seat seat, boolean isSelected) {
        boolean isVIP = (seat.type() == SeatType.VIP);

        if (isSelected) {
            btn.setBackground(MainFrame.COLOR_PRIMARY);
            btn.setForeground(Color.BLACK);
            btn.setBorder(new LineBorder(MainFrame.COLOR_PRIMARY, 2, true));
        } else if (isVIP) {
            btn.setBackground(SeatFrame.COLOR_VIP_BG);
            btn.setForeground(SeatFrame.COLOR_VIP_TEXT);
            btn.setBorder(new LineBorder(SeatFrame.COLOR_VIP_BORDER, 1, true));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(MainFrame.COLOR_TEXT_TITLE);
            btn.setBorder(new LineBorder(MainFrame.COLOR_INPUT_BORDER, 1, true));
        }
    }

    private JPanel createLegendItem(Color bg, Color border, Color text, String label) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(16, 16));
        box.setBackground(bg);
        box.setBorder(new LineBorder(border, 1, true));

        JLabel lbl = new JLabel(label);
        lbl.setFont(MainFrame.FONT_SMALL);
        lbl.setForeground(MainFrame.COLOR_TEXT_LABEL);

        item.add(box);
        item.add(lbl);
        return item;
    }

    public List<Seat> getSelectedSeats() {
        return new ArrayList<>(selectedSeats);
    }
}
