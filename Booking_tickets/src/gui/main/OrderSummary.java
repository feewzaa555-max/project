//OrderSummary <- การ์ดสรุปราคาด้านขวาของหน้าเลือกที่นั่ง (SeatFrame) + ปุ่มยืนยันการจอง
/*
    หน้าตา (ตัวอย่าง: เลือก A3, F7 / เป็นสมาชิก / มีเงิน 401):
        Order Summary
        ─────────────
        Quantity
        Seats: 2                       ← จำนวนที่นั่ง
        Seats: A3, F7                  ← รหัสที่นั่ง (ยาวเกินขึ้นบรรทัดใหม่เอง)
        • Standard: 1 (160 THB)        ← ราคาปกติแยกตามประเภท
        • VIP: 1 (200 THB)
        ─────────────
        Price Breakdown
        Regular Price: 360 THB         ← ราคาเต็ม
        Status: Member (discount applied)
        Member Discount: -36 THB       ← ส่วนลด = ราคาเต็ม - ราคาที่ต้องจ่าย
        Total Amount: 324 THB          ← ราคาที่ต้องจ่ายจริง
        ─────────────
        Wallet Balance: 401 THB
        Balance After Payment: 77 THB
        Need 50 THB more (Please top up)   ← โชว์เฉพาะตอนเงินไม่พอ (สีแดง)

        [ Confirm Booking (324 THB) ]  ← ปุ่มจอง

    ปุ่มจองมี 3 แบบ:
        ยังไม่เลือกที่นั่ง → "Please select seat(s)"       กดไม่ได้
        เงินไม่พอ        → "Insufficient Balance"         กดไม่ได้
        เงินพอ          → "Confirm Booking (324 THB)"    กดได้ → onBook.run()

    การ์ดนี้แค่ "โชว์ผล" ไม่อ่านไฟล์ ไม่จองเอง
        ราคาคิดด้วย PriceCalculator / สมาชิกไหมถาม account.isMemberAt(now) (กฎอยู่ที่ model / service ที่เดียว)
        กดปุ่มจอง → บอก SeatFrame ผ่าน onBook แล้ว SeatFrame ถามยืนยัน + จองจริง

    ใช้ยังไง (ใน SeatFrame):
        OrderSummary summary = new OrderSummary(() -> handleBooking());
        summary.show(map.selected(), app.accounts().accountOf(user), app.clock().now());   // เรียกทุกครั้งที่กดที่นั่ง

    ย้ายมาจาก gui/SeatFrame.java เดิม บรรทัด 462–679 (createSummaryPanel, createDivider, updateSummary) ต่างจากเดิม:
        ราคาแยกประเภทถาม PriceCalculator (เดิมเขียน standardCount * 160 / vipCount * 200 ตายตัว)
        ไม่เขียน "10%" ตายตัวในสถานะสมาชิก
        ไม่อ่านบัญชีเอง (เดิม catch (Exception ignored) แล้วเงียบ) → SeatFrame อ่านแล้วส่ง Account มาให้
        ทุกอย่างชิดซ้ายเท่ากัน (เดิมปุ่มอยู่กลางแต่ป้ายชิดซ้าย BoxLayout เลยดันป้ายไปเริ่มกลางการ์ด ข้อความยาวโดนตัดเป็น "...")
        สีและฟอนต์จาก MainTheme

    คำที่ใช้บ่อยในไฟล์นี้:
        BoxLayout(..., Y_AXIS) = เรียงของจากบนลงล่างตามลำดับที่ add
        Box.createVerticalGlue() = ก้อนว่างยืดได้ ดันปุ่มจองลงไปชิดล่างการ์ด
        setAlignmentX(LEFT_ALIGNMENT) = ชิดซ้าย (ใน BoxLayout ทุกชิ้นต้องตั้งเท่ากัน ไม่งั้นตำแหน่งเพี้ยน)
 */

package gui.main;

import java.awt.*;                  // Dimension, Cursor, Component, Color, Font
import java.time.LocalDateTime;     // เวลาตอนนี้ของโปรแกรม ใช้ถามว่ายังเป็นสมาชิกอยู่ไหม
import java.util.ArrayList;         // สร้างลิสต์รหัสที่นั่ง
import java.util.List;              // รายการที่นั่ง (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.swing.*;               // JPanel, JLabel, JButton, JSeparator = เส้นคั่น, Box, BoxLayout
import javax.swing.border.*;        // LineBorder = เส้นรอบ, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import model.Account;               // บัญชีลูกค้า: เงินคงเหลือ + สมาชิกถึงวันไหน
import model.Seat;                  // ที่นั่ง 1 ที่ เช่น F7
import model.SeatType;              // ประเภทที่นั่ง STANDARD / VIP
import service.PriceCalculator;     // ตัวคิดราคา (กฎราคาอยู่ที่นี่ที่เดียว)

final class OrderSummary extends JPanel {

    private static final Dimension SIZE = new Dimension(320, 520);         // ขนาดการ์ด เท่าของเดิม
    private static final int TEXT_WIDTH = 270;                             // ความกว้างข้อความที่ให้ขึ้นบรรทัดใหม่ (การ์ด 320 - ขอบ)
    private static final PriceCalculator PRICES = new PriceCalculator();   // ตัวคิดราคา (ไม่มีค่าข้างในเปลี่ยน ใช้ตัวเดียวได้)

    // Abstraction Function (SC4 หน้า 10):
    //   AF(seatCount, total, balance, ป้ายทั้งหมด) = การ์ดสรุปของการเลือกล่าสุดที่ส่งเข้า show()
    //                                               ที่นั่ง seatCount ที่ ต้องจ่าย total บาท มีเงิน balance บาท
    // Rep Invariant:
    //   onBook และป้าย / ปุ่มทุกตัวไม่เป็น null, seatCount >= 0, total >= 0, balance >= 0
    //   ปุ่มจองกดได้ ก็ต่อเมื่อ seatCount > 0 และ balance >= total
    // Safety from rep exposure (SC4 หน้า 14):
    //   field ทุกตัว private ไม่มีเมธอดคืนป้าย / ปุ่มออกไป / show() อ่านค่าจากที่นั่งแล้วไม่เก็บลิสต์ไว้
    private final Runnable onBook;            // สิ่งที่ต้องทำตอนกดปุ่มจอง (SeatFrame ส่งมา) เช่น () -> handleBooking()
    private int seatCount;                    // จำนวนที่นั่งที่เลือกล่าสุด
    private int total;                        // ราคาที่ต้องจ่ายล่าสุด
    private int balance;                      // เงินคงเหลือล่าสุด

    private final JLabel countLabel = label("Seats: 0", MainTheme.FONT_PRICE, MainTheme.PRIMARY);
    private final JLabel seatsLabel = label("Seats: (None)", MainTheme.FONT_TEXT, MainTheme.TEXT_LABEL);
    private final JLabel standardLabel = label("• Standard: 0", MainTheme.FONT_SMALL, MainTheme.TEXT_MUTED);
    private final JLabel vipLabel = label("• VIP: 0", MainTheme.FONT_SMALL, MainTheme.VIP_TEXT);
    private final JLabel regularLabel = label("Regular Price: 0 THB", MainTheme.FONT_TEXT, MainTheme.TEXT_LABEL);
    private final JLabel statusLabel = label("Status: -", MainTheme.FONT_SMALL, MainTheme.TEXT_MUTED);
    private final JLabel discountLabel = label("Member Discount: -0 THB", MainTheme.FONT_TEXT, MainTheme.SUCCESS);
    private final JLabel totalLabel = label("Total Amount: 0 THB", MainTheme.FONT_DIALOG_TITLE, MainTheme.PRIMARY);
    private final JLabel walletLabel = label("Wallet Balance: 0 THB", MainTheme.FONT_TEXT, MainTheme.TEXT_LABEL);
    private final JLabel afterLabel = label("Balance After Payment: 0 THB", MainTheme.FONT_TEXT, MainTheme.TEXT_MUTED);
    private final JLabel warningLabel = label("", MainTheme.FONT_SMALL, MainTheme.ERROR);
    private final JButton bookButton = new JButton("Please select seat(s)");

    /**
     * สร้างการ์ดสรุป (เริ่มต้น: ยังไม่เลือกที่นั่ง ปุ่มจองกดไม่ได้)
     *
     * @param onBook คำสั่งที่ทำตอนลูกค้ากดปุ่มจอง เช่น () -> handleBooking() ห้าม null
     *               Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onBook.run()
     * @throws IllegalArgumentException ถ้า onBook เป็น null
     */
    OrderSummary(Runnable onBook) {
        // ① ตรวจของที่ส่งเข้ามา (SC2 หน้า 9 Defensive Programming)
        if (onBook == null) {
            throw new IllegalArgumentException("onBook must not be null");
        }
        this.onBook = onBook;

        // ② ตัวการ์ด: พื้นขาว เส้นขอบเทา เว้นข้างใน บน 16 ซ้าย 18 ล่าง 16 ขวา 18 (เท่าของเดิม)
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(MainTheme.CARD);
        setBorder(new CompoundBorder(
                new LineBorder(MainTheme.CARD_BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        setPreferredSize(SIZE);

        // ③ ปุ่มจอง: พื้นฟ้า กว้างเต็มการ์ด สูง 42 เริ่มต้นกดไม่ได้
        bookButton.setFont(MainTheme.FONT_PRICE);
        bookButton.setBackground(MainTheme.PRIMARY);
        bookButton.setForeground(MainTheme.BUTTON_TEXT);
        bookButton.setFocusPainted(false);
        bookButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bookButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));   // ยืดกว้างเต็ม สูงไม่เกิน 42
        bookButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        bookButton.setBorder(new CompoundBorder(
                new LineBorder(MainTheme.PRIMARY, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        bookButton.setEnabled(false);
        bookButton.addActionListener(e -> onBook.run());   // กดปุ่ม → ทำคำสั่งที่ SeatFrame ส่งมา
        warningLabel.setVisible(false);                     // ซ่อนไว้ก่อน โชว์เฉพาะตอนเงินไม่พอ

        // ④ วางของบนลงล่าง
        add(label("Order Summary", MainTheme.FONT_DIALOG_TITLE, MainTheme.TEXT_TITLE));
        addDivider();
        add(label("Quantity", MainTheme.FONT_LABEL, MainTheme.TEXT_TITLE));
        add(gap(6));
        add(countLabel);
        add(gap(4));
        add(seatsLabel);
        add(gap(4));
        add(standardLabel);
        add(vipLabel);
        addDivider();
        add(label("Price Breakdown", MainTheme.FONT_LABEL, MainTheme.TEXT_TITLE));
        add(gap(6));
        add(regularLabel);
        add(gap(3));
        add(statusLabel);
        add(gap(3));
        add(discountLabel);
        add(gap(6));
        add(totalLabel);
        addDivider();
        add(walletLabel);
        add(gap(3));
        add(afterLabel);
        add(gap(4));
        add(warningLabel);
        add(Box.createVerticalGlue());   // ดันปุ่มลงล่างสุด
        add(bookButton);
        checkRep();
    }

    /**
     * โชว์สรุปของที่นั่งที่เลือกตอนนี้ (เรียกทุกครั้งที่ลูกค้ากดที่นั่ง หรือเงินเปลี่ยน)
     *
     * ตัวอย่าง: show([A3, F7], บัญชีเงิน 401 เป็นสมาชิก, 14/10 10:00)
     *   → Seats: 2 / Regular 360 / Discount -36 / Total 324 / After 77 / ปุ่ม "Confirm Booking (324 THB)" กดได้
     *
     * @param seats   ที่นั่งที่เลือก เช่น map.selected() ห้าม null / ห้ามมี null (ว่างได้)
     * @param account บัญชีลูกค้า เช่น app.accounts().accountOf(user) ห้าม null
     * @param now     เวลาตอนนี้ของโปรแกรม เช่น app.clock().now() ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null หรือ seats มี null ข้างใน
     */
    void show(List<Seat> seats, Account account, LocalDateTime now) {
        // ① ตรวจของที่ส่งเข้ามา
        if (seats == null || account == null || now == null) {
            throw new IllegalArgumentException("seats, account and now must not be null");
        }
        for (Seat seat : seats) {
            if (seat == null) {
                throw new IllegalArgumentException("seats must not contain null");
            }
        }

        // ② คิดตัวเลข (กฎราคาอยู่ที่ PriceCalculator / กฎสมาชิกอยู่ที่ Account)
        boolean member = account.isMemberAt(now);              // เช่น สมาชิกถึง 13/11 ตอนนี้ 14/10 → true
        int regular = PRICES.totalOf(seats, false);            // ราคาเต็ม เช่น [A3, F7] → 160 + 200 = 360
        seatCount = seats.size();
        total = PRICES.totalOf(seats, member);                 // ราคาที่ต้องจ่าย เช่น สมาชิก → 324
        balance = account.balance();                           // เงินคงเหลือ เช่น 401

        // ③ ส่วน Quantity
        countLabel.setText("Seats: " + seatCount);
        if (seats.isEmpty()) {
            seatsLabel.setText("Seats: (None)");
            standardLabel.setText("• Standard: 0");
            vipLabel.setText("• VIP: 0");
        } else {
            List<String> codes = new ArrayList<>();
            List<Seat> standard = new ArrayList<>();
            List<Seat> vip = new ArrayList<>();
            for (Seat seat : seats) {
                codes.add(seat.code());                        // เช่น "A3"
                if (seat.type() == SeatType.VIP) {
                    vip.add(seat);
                } else {
                    standard.add(seat);
                }
            }
            // String.join(", ", codes) = ต่อรหัสคั่นด้วย ", " เช่น [A3, F7] → "A3, F7"
            seatsLabel.setText(wrap("Seats: " + String.join(", ", codes)));
            // totalOf(ที่นั่งกลุ่มนั้น, false) = ราคาปกติของกลุ่มนั้น เช่น VIP 1 ที่ → 200
            standardLabel.setText("• Standard: " + standard.size() + " (" + PRICES.totalOf(standard, false) + " THB)");
            vipLabel.setText("• VIP: " + vip.size() + " (" + PRICES.totalOf(vip, false) + " THB)");
        }

        // ④ ส่วน Price Breakdown
        regularLabel.setText("Regular Price: " + regular + " THB");
        statusLabel.setText(member ? "Status: Member (discount applied)" : "Status: Guest (no discount)");
        discountLabel.setText("Member Discount: -" + (regular - total) + " THB");
        totalLabel.setText("Total Amount: " + total + " THB");

        // ⑤ ส่วนเงิน + ปุ่มจอง
        walletLabel.setText("Wallet Balance: " + balance + " THB");
        afterLabel.setText("Balance After Payment: " + (balance - total) + " THB");   // ยังไม่เลือก total = 0 → เท่าเงินเดิม (เหมือนของเดิม)
        if (seatCount == 0) {
            bookButton.setEnabled(false);
            bookButton.setText("Please select seat(s)");
            warningLabel.setVisible(false);
        } else if (balance < total) {
            bookButton.setEnabled(false);
            bookButton.setText("Insufficient Balance");
            warningLabel.setText(wrap("Need " + (total - balance) + " THB more (Please top up)"));
            warningLabel.setVisible(true);
        } else {
            bookButton.setEnabled(true);
            bookButton.setText("Confirm Booking (" + total + " THB)");
            warningLabel.setVisible(false);
        }
        checkRep();   // ตรวจ RI (SC4 หน้า 12 เรียกท้ายทุกเมธอดที่แก้ rep)
    }

    /**
     * เส้นคั่นบาง ๆ มีที่ว่างบนล่าง 10 px
     */
    private void addDivider() {
        JSeparator line = new JSeparator();
        line.setForeground(MainTheme.CARD_BORDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));   // กว้างเต็ม สูง 1 px
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(gap(10));
        add(line);
        add(gap(10));
    }

    /**
     * ป้ายข้อความชิดซ้าย
     *
     * @param text ข้อความ เช่น "Seats: 0"
     * @param font ฟอนต์
     * @param color สีตัวหนังสือ
     * @return JLabel ป้ายที่ชิดซ้าย
     */
    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * ข้อความยาวให้ขึ้นบรรทัดใหม่เมื่อกว้างเกิน TEXT_WIDTH (เช่นเลือก 10 ที่ รหัสจะยาวเกินการ์ด)
     *   <table width=270> = กล่องกว้าง 270 px ข้อความเกินขึ้นบรรทัดใหม่เอง
     *
     * @param text ข้อความยาว เช่น "Seats: A1, A2, A3, ..."
     * @return String ข้อความเดิมห่อด้วย <html><table width=270> ให้ JLabel ตัดขึ้นบรรทัดใหม่ได้
     */
    private static String wrap(String text) {
        return "<html><table width=" + TEXT_WIDTH + " cellpadding=0 cellspacing=0><tr><td>"
                + text + "</td></tr></table></html>";
    }

    /**
     * ก้อนว่างแนวตั้ง สูง height px (ชิดซ้ายเหมือนของอื่น)
     *
     * @param height ความสูงช่องว่างเป็น px เช่น 6
     * @return Component ก้อนว่างกว้าง 0 สูง height (ชิดซ้าย)
     */
    private static Component gap(int height) {
        Component gap = Box.createRigidArea(new Dimension(0, height));
        ((JComponent) gap).setAlignmentX(Component.LEFT_ALIGNMENT);
        return gap;
    }

    /**
     * ตรวจ Rep Invariant ทุกข้อ ทำงานเฉพาะตอนรันด้วย java -ea (SC4 หน้า 12)
     */
    private void checkRep() {
        assert onBook != null && bookButton != null && warningLabel != null : "field ต้องไม่เป็น null";
        assert seatCount >= 0 && total >= 0 && balance >= 0 : "ตัวเลขต้องไม่ติดลบ";
        assert bookButton.isEnabled() == (seatCount > 0 && balance >= total) : "ปุ่มจองกดได้เฉพาะตอนเลือกแล้วและเงินพอ";
    }
}