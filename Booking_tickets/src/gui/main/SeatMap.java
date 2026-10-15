//SeatMap <- ผังที่นั่งในหน้าเลือกที่นั่ง (SeatFrame) กดที่นั่งเพื่อเลือก / กดซ้ำเพื่อยกเลิก
/*
    หน้าตา:
        ┌───────────────────────────────────────────────┐
        │   [=============== SCREEN ================]   │  ① ป้ายจอ
        │   A [1][2][3][4][5][6][7][8][9][10] A         │  ② ที่นั่ง 1 แถว = 1 บรรทัด (อ่านจาก HallLayout)
        │   ...                                         │
        │   F [1][2][3][4][5][6][7][8][9][10] F         │     แถว VIP = เหลือง
        │   □ Standard (160 THB)  □ VIP (200 THB)       │  ③ คำอธิบายสี (ราคาอ่านจาก PriceCalculator)
        │   ■ Selected  □ Booked                        │
        └───────────────────────────────────────────────┘

    ที่นั่ง 1 ที่มี 4 แบบ:
        ปกติ      = พื้นขาว กรอบเทา          กดได้
        VIP       = พื้นเหลือง กรอบส้ม        กดได้
        เลือกอยู่   = พื้นฟ้า                  กดซ้ำ = ยกเลิก
        จองแล้ว    = พื้นเทา ตัวเลขเทา          กดไม่ได้

    ผังนี้รู้แค่ "ที่นั่ง" ไม่รู้เรื่องเงิน / รอบฉาย / การจอง
        กดที่นั่ง → บอก SeatFrame ผ่าน onChange แล้ว SeatFrame ถาม selected() ไปคิดราคาเอง

    ใช้ยังไง (ใน SeatFrame):
        SeatMap map = new SeatMap(app.layout(), app.bookings().bookedSeats(รอบ), () -> refreshSummary());
        map.selected()   // ที่นั่งที่เลือกอยู่ เรียงตามผัง เช่น [A3, F7] / ยังไม่เลือก → []

    ย้ายมาจาก gui/SeatFrame.java เดิม บรรทัด 283–460 (createHallPanel, createSeatButton,
        toggleSeatSelection, updateSeatButtonStyle, createLegendItem) ต่างจากเดิม:
        ป้าย SCREEN ใช้ JLabel ธรรมดา (เดิม override paintComponent วาดเอง 30 บรรทัด)
        ราคาในคำอธิบายสีอ่านจาก PriceCalculator (เดิมเขียน "160 THB" / "200 THB" ตายตัว)
        สีแถว VIP ดูจาก SeatType ของที่นั่ง (เดิมเขียน row == 'F' ตายตัว)
        ไม่ใส่ราคาใน tooltip (เดิมเขียน "VIP (200 THB / Member 180 THB)" ตายตัว) ราคาไปดูที่ OrderSummary
        สีและฟอนต์จาก MainTheme

    คำที่ใช้บ่อยในไฟล์นี้:
        BorderLayout NORTH / CENTER / SOUTH = บน / กลาง (กินที่ที่เหลือ) / ล่าง
        FlowLayout(CENTER, 6, 3) = วางเรียงซ้ายไปขวา อยู่กลาง ห่างกันแนวนอน 6 px แนวตั้ง 3 px
        Map<String, JButton> = จับคู่ รหัสที่นั่ง → ปุ่ม เช่น "A3" → ปุ่มเลข 3 แถว A
 */

package gui.main;

import java.awt.*;                  // BorderLayout, FlowLayout, Dimension, Cursor, Color, Component
import java.util.ArrayList;         // สร้างลิสต์ที่นั่งที่เลือกตอนคืนค่า
import java.util.HashMap;           // เก็บคู่ รหัสที่นั่ง → ปุ่ม
import java.util.HashSet;           // เก็บที่นั่งที่เลือกอยู่ (ไม่ซ้ำ)
import java.util.LinkedHashMap;     // เก็บที่นั่งตัวอย่างของแต่ละประเภท เรียงตามลำดับที่เจอ (ทำคำอธิบายสี)
import java.util.List;              // รายการที่นั่ง (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import java.util.Map;               // ชนิดของ HashMap / LinkedHashMap
import java.util.Set;               // ชนิดของ HashSet / ที่นั่งที่จองแล้วที่ส่งเข้ามา
import javax.swing.*;               // JPanel, JButton = ที่นั่ง, JLabel = ป้าย, SwingConstants
import javax.swing.border.*;        // LineBorder = เส้นรอบ, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import model.HallLayout;            // ผังโรง: แถวอะไรบ้าง แต่ละแถวมีที่นั่งไหน
import model.Seat;                  // ที่นั่ง 1 ที่ เช่น F7 (แถว F เลข 7 VIP)
import model.SeatType;              // ประเภทที่นั่ง STANDARD / VIP
import service.PriceCalculator;     // ราคาที่นั่ง ใช้ทำคำอธิบายสี "Standard (160 THB)"

final class SeatMap extends JPanel {

    private static final Dimension SEAT_SIZE = new Dimension(38, 34);         // ขนาดปุ่มที่นั่ง เท่าของเดิม
    private static final Dimension ROW_LABEL_SIZE = new Dimension(20, 34);    // ขนาดป้ายแถว A, B ... เท่าของเดิม
    private static final PriceCalculator PRICES = new PriceCalculator();      // ตัวคิดราคา (ไม่มีค่าข้างในเปลี่ยน ใช้ตัวเดียวได้)

    // Abstraction Function (SC4 หน้า 10):
    //   AF(layout, booked, chosen) = ผังที่นั่งตาม layout
    //                                ที่นั่งที่รหัสอยู่ใน booked = จองแล้ว กดไม่ได้
    //                                ที่นั่งใน chosen = ที่ลูกค้าเลือกอยู่ตอนนี้
    // Rep Invariant:
    //   layout / booked / onChange / chosen / buttons ไม่เป็น null
    //   ทุกที่นั่งใน chosen อยู่ใน layout และรหัสไม่อยู่ใน booked (เลือกที่นั่งที่จองแล้วไม่ได้)
    // Safety from rep exposure (SC4 หน้า 14):
    //   booked เก็บสำเนาแบบแก้ไม่ได้ (Set.copyOf) / chosen กับ buttons เป็น private ไม่คืนออกไปตรง ๆ
    //   selected() คืนลิสต์ใหม่ทุกครั้ง / Seat เป็น record แก้ค่าไม่ได้
    private final HallLayout layout;                          // ผังโรง (ตัวเดียวกับทั้งโปรแกรม app.layout())
    private final Set<String> booked;                         // รหัสที่นั่งที่จองแล้วของรอบนี้ เช่น {A1, F7}
    private final Runnable onChange;                          // สิ่งที่ต้องทำหลังกดที่นั่ง (SeatFrame ส่งมา)
    private final Set<Seat> chosen = new HashSet<>();         // ที่นั่งที่เลือกอยู่
    private final Map<String, JButton> buttons = new HashMap<>();   // รหัสที่นั่ง → ปุ่ม ใช้เปลี่ยนสีปุ่มตอนกด

    /**
     * สร้างผังที่นั่ง
     *
     * @param layout   ผังโรง เช่น app.layout() ห้าม null
     * @param booked   รหัสที่นั่งที่จองแล้ว เช่น app.bookings().bookedSeats(รอบ) → {A1, F7} ห้าม null / ห้ามมี null (ว่างได้)
     * @param onChange คำสั่งที่ทำทุกครั้งที่ลูกค้ากดที่นั่ง เช่น () -> refreshSummary() ห้าม null
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onChange.run()
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null หรือ booked มี null ข้างใน
     */
    SeatMap(HallLayout layout, Set<String> booked, Runnable onChange) {
        // ① ตรวจของที่ส่งเข้ามา ผิดแล้ว throw เลย (SC2 หน้า 9 Defensive Programming)
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (booked == null) {
            throw new IllegalArgumentException("booked must not be null");
        }
        for (String code : booked) {   // ไล่ดูทีละรหัส (ไม่ใช้ booked.contains(null) เพราะ Set.of(...) ถามหา null แล้วพัง)
            if (code == null) {
                throw new IllegalArgumentException("booked must not contain null");
            }
        }
        if (onChange == null) {
            throw new IllegalArgumentException("onChange must not be null");
        }

        // ② เก็บของ
        //    Set.copyOf(booked) = สำเนาแบบแก้ไม่ได้ ตัวนอกถูกแก้ทีหลังก็ไม่กระทบ (SC4 หน้า 14)
        this.layout = layout;
        this.booked = Set.copyOf(booked);
        this.onChange = onChange;

        // ③ ตัวการ์ด: พื้นขาว เส้นขอบเทา เว้นข้างใน บน 14 ซ้าย 16 ล่าง 14 ขวา 16 (เท่าของเดิม)
        setLayout(new BorderLayout(0, 10));   // ของแต่ละส่วน (บน / กลาง / ล่าง) ห่างกัน 10 px
        setBackground(MainTheme.CARD);
        setBorder(new CompoundBorder(
                new LineBorder(MainTheme.CARD_BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        add(makeScreen(), BorderLayout.NORTH);
        add(makeSeats(), BorderLayout.CENTER);
        add(makeLegend(), BorderLayout.SOUTH);
        checkRep();
    }

    /**
     * ที่นั่งที่เลือกอยู่ เรียงตามผัง (แถว A ก่อน เลขน้อยก่อน)
     *
     * @return ลิสต์ใหม่ทุกครั้ง แก้ได้ไม่กระทบผัง เช่น กด F7 แล้วกด A3 → [A3, F7] / ยังไม่เลือก → []
     */
    List<Seat> selected() {
        List<Seat> result = new ArrayList<>();
        for (char row : layout.rows()) {               // ไล่ตามผัง จะได้เรียงเหมือนกันทุกครั้ง ไม่ขึ้นกับลำดับที่กด
            for (Seat seat : layout.seatsInRow(row)) {
                if (chosen.contains(seat)) {
                    result.add(seat);
                }
            }
        }
        return result;
    }

    /**
     * ① ป้าย SCREEN: พื้นฟ้าอ่อน กรอบฟ้า ตัวหนังสือฟ้าอยู่กลาง เว้นซ้ายขวา 30 px (เท่าของเดิม)
     *
     * @return JComponent กล่องที่มีป้าย SCREEN พื้นฟ้าอ่อน กรอบฟ้า (เว้นซ้ายขวา 30 px)
     */
    private JComponent makeScreen() {
        JLabel screen = new JLabel("SCREEN", SwingConstants.CENTER);
        screen.setFont(MainTheme.FONT_SMALL_BOLD);
        screen.setForeground(MainTheme.PRIMARY);
        screen.setOpaque(true);                        // ต้องเปิด ไม่งั้น JLabel ไม่ระบายพื้น
        screen.setBackground(MainTheme.SCREEN);
        screen.setBorder(new LineBorder(MainTheme.PRIMARY, 2, true));
        screen.setPreferredSize(new Dimension(0, 20)); // สูง 20 ส่วนความกว้างให้ BorderLayout ยืดเต็ม

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setBorder(new EmptyBorder(4, 30, 6, 30));   // เว้นรอบป้าย บน 4 ซ้าย 30 ล่าง 6 ขวา 30
        holder.add(screen, BorderLayout.CENTER);
        return holder;
    }

    /**
     * ② ที่นั่งทุกแถว เรียงบนลงล่าง: ป้ายแถว + ที่นั่ง + ป้ายแถว
     *   ไม่รับพารามิเตอร์ อ่านแถวและที่นั่งจาก layout
     *
     * @return JComponent ที่นั่งทุกแถว A–F เรียงบนลงล่าง (และจำปุ่มทุกปุ่มไว้ใน buttons)
     */
    private JComponent makeSeats() {
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));   // เรียงแถวบนลงล่าง
        rows.setOpaque(false);

        // layout.rows() = แถวทั้งหมด เช่น [A, B, C, D, E, F]
        for (char row : layout.rows()) {
            List<Seat> seats = layout.seatsInRow(row);    // ที่นั่งของแถวนี้ เช่น [A1 ... A10]
            // แถวนี้เป็น VIP ไหม ดูจากประเภทของที่นั่งตัวแรก (เดิมเขียน row == 'F')
            boolean vipRow = !seats.isEmpty() && seats.get(0).type() == SeatType.VIP;

            JPanel line = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
            line.setOpaque(false);
            line.add(rowLabel(row, vipRow));              // ป้ายแถวซ้าย
            for (Seat seat : seats) {
                JButton button = makeSeat(seat);
                buttons.put(seat.code(), button);         // จำไว้ เปลี่ยนสีตอนกด
                line.add(button);
            }
            line.add(rowLabel(row, vipRow));              // ป้ายแถวขวา
            rows.add(line);
        }
        return rows;
    }

    /**
     * ป้ายแถว เช่น "A" สีเทา / แถว VIP สีส้ม
     *
     * @param row ชื่อแถว เช่น 'A'
     * @param vipRow true ถ้าแถวนี้เป็น VIP (ป้ายสีส้ม) false = สีเทา
     * @return JLabel ป้ายแถว เช่น "A" ขนาด 20 x 34
     */
    private static JLabel rowLabel(char row, boolean vipRow) {
        JLabel label = new JLabel(String.valueOf(row), SwingConstants.CENTER);
        label.setFont(MainTheme.FONT_LABEL);
        label.setForeground(vipRow ? MainTheme.VIP_TEXT : MainTheme.TEXT_MUTED);
        label.setPreferredSize(ROW_LABEL_SIZE);
        return label;
    }

    /**
     * ปุ่มที่นั่ง 1 ที่ / จองแล้ว → เทา กดไม่ได้ / ยังว่าง → กดเพื่อเลือก
     *
     * @param seat ที่นั่ง 1 ที่ เช่น F7 (แถว F เลข 7 VIP)
     * @return JButton ปุ่มที่นั่ง เลข "7" (จองแล้ว = เทา กดไม่ได้ / ว่าง = กดแล้ว toggle(ที่นั่งนี้))
     */
    private JButton makeSeat(Seat seat) {
        JButton button = new JButton(String.valueOf(seat.number()));   // ตัวเลขบนปุ่ม เช่น "7"
        button.setFont(MainTheme.FONT_SMALL_BOLD);
        button.setPreferredSize(SEAT_SIZE);
        button.setMargin(new Insets(0, 0, 0, 0));   // ไม่เว้นขอบใน เลข "10" จะได้ไม่กลายเป็น "..."
        button.setFocusPainted(false);              // กดแล้วไม่วาดกรอบประรอบเลข

        if (booked.contains(seat.code())) {
            button.setEnabled(false);               // กดไม่ได้
            button.setBackground(MainTheme.LIGHT_GRAY);
            button.setForeground(MainTheme.DISABLED_TEXT);
            button.setBorder(new LineBorder(MainTheme.CARD_BORDER, 1, true));
            button.setToolTipText(seat.code() + " (Booked)");                         // ชี้ค้างแล้วขึ้น "F7 (Booked)"
        } else {
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));                         // ชี้แล้วเป็นรูปมือ
            button.setToolTipText(seat.code() + " " + seat.type().displayName());     // ชี้ค้างแล้วขึ้น "F7 VIP"
            colorSeat(button, seat, false);
            // กดปุ่ม → toggle(ที่นั่งนี้) / แต่ละปุ่มจำที่นั่งของตัวเองไว้
            button.addActionListener(e -> toggle(seat));
        }
        return button;
    }

    /**
     * ลูกค้ากดที่นั่ง → เลือกอยู่แล้วก็เอาออก / ยังไม่เลือกก็ใส่ แล้วบอก SeatFrame
     *   ผลที่ได้: ใส่/เอาออกจาก chosen, เปลี่ยนสีปุ่ม, เรียก onChange
     *
     * @param seat ที่นั่งที่ลูกค้ากด เช่น A3
     */
    private void toggle(Seat seat) {
        boolean nowChosen;
        if (chosen.contains(seat)) {
            chosen.remove(seat);
            nowChosen = false;
        } else {
            chosen.add(seat);
            nowChosen = true;
        }
        colorSeat(buttons.get(seat.code()), seat, nowChosen);   // เปลี่ยนสีปุ่มนั้นปุ่มเดียว
        checkRep();          // ตรวจ RI (SC4 หน้า 12 เรียกท้ายทุกเมธอดที่แก้ rep)
        onChange.run();      // ทำคำสั่งที่ SeatFrame ส่งมา เช่น refreshSummary() คิดราคาใหม่
    }

    /**
     * ระบายสีปุ่มที่ยังไม่ถูกจอง: เลือกอยู่ = ฟ้า / VIP = เหลือง / ปกติ = ขาว
     *   ผลที่ได้: เปลี่ยนสีพื้น ตัวเลข และกรอบของปุ่มที่ส่งมา
     *
     * @param button ปุ่มที่จะระบายสี
     * @param seat ที่นั่งของปุ่มนั้น (ใช้ดูว่า VIP ไหม)
     * @param isChosen true ถ้าเลือกอยู่
     */
    private static void colorSeat(JButton button, Seat seat, boolean isChosen) {
        Color background;
        Color text;
        Color border;
        if (isChosen) {
            background = MainTheme.PRIMARY;
            text = MainTheme.BUTTON_TEXT;
            border = MainTheme.PRIMARY;
        } else if (seat.type() == SeatType.VIP) {
            background = MainTheme.VIP_BACKGROUND;
            text = MainTheme.VIP_TEXT;
            border = MainTheme.VIP_BORDER;
        } else {
            background = MainTheme.CARD;
            text = MainTheme.TEXT_TITLE;
            border = MainTheme.INPUT_BORDER;
        }
        button.setBackground(background);
        button.setForeground(text);
        button.setBorder(new LineBorder(border, isChosen ? 2 : 1, true));   // เลือกอยู่ = กรอบหนา 2
    }

    /**
     * ③ คำอธิบายสี: ประเภทที่นั่งทุกแบบที่มีในผัง (พร้อมราคา) + Selected + Booked
     *
     * @return JComponent แถบคำอธิบายสี เช่น □ Standard (160 THB)  □ VIP (200 THB)  ■ Selected  □ Booked
     */
    private JComponent makeLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(8, 0, 4, 0));

        // หาที่นั่งตัวอย่าง 1 ที่ต่อประเภท เช่น {STANDARD=A1, VIP=F1} เอาไปถามราคา
        //   putIfAbsent = ใส่เฉพาะตอนยังไม่มีประเภทนี้ (เก็บตัวแรกที่เจอ)
        Map<SeatType, Seat> sample = new LinkedHashMap<>();
        for (char row : layout.rows()) {
            for (Seat seat : layout.seatsInRow(row)) {
                sample.putIfAbsent(seat.type(), seat);
            }
        }
        for (Seat seat : sample.values()) {
            // priceOf(ที่นั่ง, false) = ราคาปกติ (ไม่ใช่สมาชิก) เช่น A1 → 160, F1 → 200
            String text = seat.type().displayName() + " (" + PRICES.priceOf(seat, false) + " THB)";
            if (seat.type() == SeatType.VIP) {
                legend.add(legendItem(MainTheme.VIP_BACKGROUND, MainTheme.VIP_BORDER, text));
            } else {
                legend.add(legendItem(MainTheme.CARD, MainTheme.INPUT_BORDER, text));
            }
        }
        legend.add(legendItem(MainTheme.PRIMARY, MainTheme.PRIMARY, "Selected"));
        legend.add(legendItem(MainTheme.LIGHT_GRAY, MainTheme.CARD_BORDER, "Booked"));
        return legend;
    }

    /**
     * คำอธิบาย 1 อัน: กล่องสี 16x16 + ข้อความ
     *
     * @param background สีพื้นกล่องตัวอย่าง
     * @param border สีกรอบ
     * @param text ข้อความ เช่น "Booked"
     * @return JPanel คำอธิบาย 1 อัน = กล่องสี 16x16 + ข้อความ
     */
    private static JPanel legendItem(Color background, Color border, String text) {
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(16, 16));
        box.setBackground(background);
        box.setBorder(new LineBorder(border, 1, true));

        JLabel label = new JLabel(text);
        label.setFont(MainTheme.FONT_SMALL);
        label.setForeground(MainTheme.TEXT_LABEL);

        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        item.add(box);
        item.add(label);
        return item;
    }

    /**
     * ตรวจ Rep Invariant ทุกข้อ ทำงานเฉพาะตอนรันด้วย java -ea (SC4 หน้า 12)
     */
    private void checkRep() {
        assert layout != null && booked != null && onChange != null && chosen != null && buttons != null
                : "field ต้องไม่เป็น null";
        for (Seat seat : chosen) {
            assert buttons.containsKey(seat.code()) : "ที่นั่งที่เลือกต้องอยู่ในผัง: " + seat.code();
            assert !booked.contains(seat.code()) : "เลือกที่นั่งที่จองแล้วไม่ได้: " + seat.code();
        }
    }
}