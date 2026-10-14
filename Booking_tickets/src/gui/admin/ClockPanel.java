//ClockPanel <- การ์ดตั้งเวลาของโปรแกรมในหน้า admin: ตั้งได้ว่า "วันนี้" คือวันอะไร เวลาเท่าไร (ไว้สาธิต)
/*
    หน้าตา (เลขตรงกับ ①–⑤ ในโค้ดข้างล่าง):
        ┌─────────────────────────────────────────────── ① การ์ดขาว มีเส้นขอบ กว้างเท่า LineupPanel
        │                 Program Clock                  ② หัวข้อ
        │             Now: 14/10/2026 10:00              ③ เวลาตอนนี้ของโปรแกรม
        │ Date [14/10/2026] Time [10:00] [Set] [Reset to real time]   ④ ช่องพิมพ์ + ⑤ ปุ่ม
        └───────────────────────────────────────────────

    ทำงานยังไง:
        ทุกหน้าใช้นาฬิกาตัวเดียวกัน (AppClock ใน AppServices) ตั้งที่นี่ → ทุกหน้าเห็นพร้อมกัน
        admin ตั้ง 20/10/2026 09:00 กด Set
            → หน้า admin: Showing on เปลี่ยนเป็น 21/10/2026 (วันนี้ + 1)  ← AdminFrame วาด LineupPanel ใหม่ผ่าน onChange
            → logout ให้ลูกค้า login: ปุ่มวันที่ขึ้น [Today 20/10] และจองได้แค่รอบของ 20/10
        กด Reset to real time → กลับเป็นเวลาจริงของเครื่อง
        ปิดโปรแกรม → เวลาที่ตั้งไว้หาย กลับเป็นเวลาจริง (AppClock เก็บไว้ในหน่วยความจำ ไม่ได้เขียนลงไฟล์)

    การ์ดนี้แค่บอกว่า "เปลี่ยนเวลาแล้ว" ผ่าน onChange ที่ส่งเข้ามา
    ส่วนการวาด LineupPanel ใหม่เป็นงานของ AdminFrame การ์ดนี้จะได้ไม่ต้องรู้จัก LineupPanel

    ใช้ยังไง (ใน AdminFrame):
        new ClockPanel(services.clock(), () -> showLineup())

    คำที่ใช้บ่อยในไฟล์นี้:
        JTextField  = ช่องให้พิมพ์ข้อความ 1 บรรทัด / getText() = อ่านสิ่งที่พิมพ์ / setText(...) = ใส่ข้อความลงช่อง
        DateTimeFormatter = "แม่แบบ" วันเวลา ใช้แปลงไปกลับระหว่างข้อความ "20/10/2026 09:00" กับวันเวลาจริง
 */

package gui.admin;

import java.awt.*;                              // Color = สี, Dimension = ขนาด, Cursor = รูปเมาส์, FlowLayout = เรียงของในแถว
import java.time.LocalDateTime;                 // วันที่ + เวลา เช่น 2026-10-20T09:00
import java.time.format.DateTimeFormatter;      // แม่แบบแปลงวันเวลา <-> ข้อความ
import java.time.format.DateTimeParseException; // error ตอนข้อความไม่ตรงแม่แบบ เช่น พิมพ์ 32/10/2026
import java.time.format.ResolverStyle;          // STRICT = ตรวจเข้ม วันที่ไม่มีจริง (31/02) ถือว่าผิด ไม่แอบปัดเป็น 28/02
import javax.swing.*;                           // JPanel, JLabel, JTextField = ช่องพิมพ์, JButton, JOptionPane = กล่องเด้ง, Box, BoxLayout
import javax.swing.border.*;                    // LineBorder = เส้นรอบ, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import service.AppClock;                        // นาฬิกาของโปรแกรม มี now() / setTo(...) / reset()

final class ClockPanel extends JPanel {

    // แม่แบบวันเวลา 3 ตัว (แก้ค่าไม่ได้ ใช้ร่วมกันได้ทั้งโปรแกรม)
    //   dd = วัน 2 หลัก, MM = เดือน 2 หลัก, uuuu = ปี 4 หลัก, HH = ชั่วโมง 00–23, mm = นาที 2 หลัก
    //   ใช้ uuuu แทน yyyy เพราะโหมด STRICT ต้องใช้ uuuu (yyyy = ปีแบบมีศักราช ใน STRICT จะแปลงไม่ผ่านทุกวัน)
    //   withResolverStyle(ResolverStyle.STRICT) = ห้ามแอบปัดค่า เช่น 31/02/2026 → error (ปกติจะปัดเป็น 28/02 เงียบ ๆ)
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");        // ช่อง Date เช่น 20/10/2026
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");             // ช่อง Time เช่น 09:00
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")  // ตอนอ่าน 2 ช่องรวมกัน
            .withResolverStyle(ResolverStyle.STRICT);

    // เก็บนาฬิกาตัวจริง (ไม่ทำสำเนา) ตั้งใจให้แก้นาฬิกาตัวเดียวกับทุกหน้า ถ้าทำสำเนา ตั้งไปก็ไม่มีหน้าไหนเห็น
    private final AppClock clock;
    private final Runnable onChange;       // สิ่งที่ต้องทำหลังเปลี่ยนเวลา (AdminFrame ส่งมา) เช่น () -> showLineup()
    private final JLabel nowLabel;         // ③ "Now: 14/10/2026 10:00" เปลี่ยนข้อความทุกครั้งที่กด Set / Reset
    private final JTextField dateField;    // ④ ช่องพิมพ์วันที่
    private final JTextField timeField;    // ④ ช่องพิมพ์เวลา

    /**
     * สร้างการ์ดตั้งเวลา ช่องพิมพ์ใส่เวลาตอนนี้ของโปรแกรมไว้ให้ก่อน
     *
     * @param clock    นาฬิกาของโปรแกรม ตัวเดียวกับทุกหน้า เช่น services.clock()
     * @param onChange คำสั่ง run method นั้น
     *                 สิ่งที่ต้องทำหลังกด Set / Reset สำเร็จ (AdminFrame ส่งมา) เช่น () -> showLineup()
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onChange.run()
     * @throws IllegalArgumentException ถ้า clock หรือ onChange เป็น null
     */
    ClockPanel(AppClock clock, Runnable onChange) {
        // ตรวจของที่ส่งเข้ามาก่อน ผิดแล้ว throw เลย (SC2 หน้า 9 Defensive Programming)
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (onChange == null) {
            throw new IllegalArgumentException("onChange must not be null");
        }
        this.clock = clock;
        this.onChange = onChange;

        // ① ตัวการ์ด (แบบเดียวกับ LineupPanel สองการ์ดจะได้เข้าชุดกัน)
        //    BoxLayout(this, Y_AXIS) = เรียงของในการ์ดนี้จากบนลงล่าง ตามลำดับที่ add
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);   // พื้นการ์ดสีขาว
        //    ชั้นนอก LineBorder(สี, หนา 1 px, true = มุมโค้ง) = เส้นเทาอ่อนรอบการ์ด
        //    ชั้นใน EmptyBorder(บน 16, ซ้าย 20, ล่าง 16, ขวา 20) = ที่ว่างข้างในการ์ด
        //            ซ้าย/ขวาแค่ 20 (LineupPanel ใช้ 40) เพราะแถว ④ มี 6 ชิ้น ต้องการที่กว้าง ~510 px ถ้าเว้น 40 เหลือ 480 ปุ่ม Reset จะตกบรรทัด
        setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(16, 20, 16, 20)));
        //    กว้าง 560 เท่า LineupPanel สูง 130 (420 + ช่องว่าง + 130 ยังพอดีหน้าต่างสูง 730)
        setPreferredSize(new Dimension(560, 130));

        // ② หัวข้อ ตัวหนา 20 สีเกือบดำ อยู่กลาง
        JLabel header = new JLabel("Program Clock");
        header.setFont(AdminTheme.FONT_HEADER);
        header.setForeground(AdminTheme.TEXT_DARK);
        header.setAlignmentX(CENTER_ALIGNMENT);
        add(header);
        add(Box.createRigidArea(new Dimension(0, 4)));    // ที่ว่าง 4 px

        // ③ บรรทัดเวลาตอนนี้ ตัวเล็ก 12 สีเทา อยู่กลาง (ข้อความใส่ใน showNow() ข้างล่าง)
        nowLabel = new JLabel();
        nowLabel.setFont(AdminTheme.FONT_SUBTITLE);
        nowLabel.setForeground(AdminTheme.TEXT_MUTED);
        nowLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(nowLabel);
        add(Box.createRigidArea(new Dimension(0, 12)));   // ที่ว่าง 12 px ก่อนแถวช่องพิมพ์

        // ④ ช่องพิมพ์ JTextField(จำนวนตัวอักษรที่กว้างพอ) → กำหนดความกว้างของช่อง
        dateField = new JTextField(8);   // พอสำหรับ 20/10/2026
        timeField = new JTextField(4);   // พอสำหรับ 09:00
        dateField.setFont(AdminTheme.FONT_TEXT);
        timeField.setFont(AdminTheme.FONT_TEXT);
        // กด Enter ในช่องพิมพ์ = กด Set (JTextField ส่งเหตุการณ์ตอนกด Enter มาที่ ActionListener)
        dateField.addActionListener(e -> handleSet());
        timeField.addActionListener(e -> handleSet());

        // ⑤ ปุ่ม Set สีฟ้า ตัวหนังสือขาว (แบบเดียวกับปุ่ม Save Lineup)
        JButton set = new JButton("Set");
        set.setFont(AdminTheme.FONT_LABEL);
        set.setBackground(AdminTheme.PRIMARY);
        set.setForeground(Color.WHITE);
        set.setFocusPainted(false);                       // กดแล้วไม่วาดกรอบประรอบคำ
        set.setCursor(new Cursor(Cursor.HAND_CURSOR));    // ชี้แล้วเป็นรูปมือ
        set.addActionListener(e -> handleSet());          // กดปุ่ม → handleSet()

        // ⑤ ปุ่ม Reset พื้นขาว กรอบเทา
        JButton reset = new JButton("Reset to real time");
        reset.setFont(AdminTheme.FONT_LABEL);
        reset.setBackground(Color.WHITE);
        reset.setForeground(AdminTheme.TEXT_DARK);
        reset.setFocusPainted(false);
        reset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        //    ขอบ: เส้นเทารอบปุ่ม + ที่ว่างข้างใน บน 5 ซ้าย 10 ล่าง 5 ขวา 10
        reset.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        reset.addActionListener(e -> handleReset());      // กดปุ่ม → handleReset()

        // แถวเดียว: Date [ช่อง] Time [ช่อง] [Set] [Reset]
        //    FlowLayout(FlowLayout.CENTER, 8, 0) = เรียงต่อกันจัดกลาง ห่างกันแนวนอน 8 px แนวตั้ง 0
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        row.setOpaque(false);                             // โปร่งใส ให้เห็นพื้นขาวของการ์ด
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(520, 34));       // BoxLayout ยืดแถวได้ไม่เกิน กว้าง 520 (560 - ขอบซ้ายขวา 20+20) สูง 34
        row.add(label("Date"));
        row.add(dateField);
        row.add(label("Time"));
        row.add(timeField);
        row.add(set);
        row.add(reset);
        add(row);

        // เริ่มต้น: ช่องพิมพ์ใส่เวลาตอนนี้ของโปรแกรม + บรรทัด Now
        fillFields();
        showNow();
    }

    // ป้ายหน้าช่องพิมพ์ "Date" / "Time" ตัวหนา 12 สีเกือบดำ
    //   text = คำบนป้าย
    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AdminTheme.FONT_LABEL);
        label.setForeground(AdminTheme.TEXT_DARK);
        return label;
    }

    // ใส่เวลาตอนนี้ของโปรแกรมลงช่องพิมพ์ เช่น [14/10/2026] [10:00]
    //   clock.now() = วันเวลาตอนนี้ของโปรแกรม (AppClock.java บรรทัด 96)
    //   .format(แม่แบบ) = แปลงเป็นข้อความตามแม่แบบ เช่น 2026-10-14T10:00 → "14/10/2026" และ "10:00"
    private void fillFields() {
        LocalDateTime now = clock.now();
        dateField.setText(now.format(DATE_FORMAT));
        timeField.setText(now.format(TIME_FORMAT));
    }

    // เขียนบรรทัด ③ ใหม่ เช่น "Now: 20/10/2026 09:00"
    private void showNow() {
        nowLabel.setText("Now: " + clock.now().format(INPUT_FORMAT));
    }

    /**
     * กด Set: อ่าน 2 ช่อง → ตั้งนาฬิกา → เขียนบรรทัด Now ใหม่ → บอก AdminFrame
     * พิมพ์ผิดแม่แบบ → กล่องเตือน นาฬิกาไม่เปลี่ยน
     * เช่น [20/10/2026] [09:00] → ตั้งเป็น 2026-10-20 09:00
     *      [32/10/2026] [09:00] → กล่องเตือน (ไม่มีวันที่ 32)
     */
    private void handleSet() {
        // trim() = ตัดช่องว่างหน้า/หลังทิ้ง เผลอเคาะเว้นวรรคก็ยังใช้ได้
        String text = dateField.getText().trim() + " " + timeField.getText().trim();   // เช่น "20/10/2026 09:00"
        LocalDateTime time;
        try {
            // LocalDateTime.parse(ข้อความ, แม่แบบ) = แปลงข้อความเป็นวันเวลา ไม่ตรงแม่แบบ → throw DateTimeParseException
            time = LocalDateTime.parse(text, INPUT_FORMAT);
        } catch (DateTimeParseException e) {
            // จับเฉพาะ error ของการแปลงข้อความ (SC2 หน้า 11) แล้วบอก admin ว่าต้องพิมพ์แบบไหน
            JOptionPane.showMessageDialog(this,
                    "Invalid date or time: \"" + text + "\"\nUse Date dd/MM/yyyy and Time HH:mm, e.g. 20/10/2026 and 09:00",
                    "Cannot set clock", JOptionPane.WARNING_MESSAGE);   // ไอคอน ! สีเหลือง
            return;   // ไม่ตั้งนาฬิกา ไม่เรียก onChange
        }
        clock.setTo(time);   // ตั้งนาฬิกาของโปรแกรม (AppClock.java บรรทัด 113) ทุกหน้าเห็นเวลานี้
        showNow();
        onChange.run();      // ทำคำสั่งที่ AdminFrame ส่งมา เช่น showLineup() → Showing on เป็นวันนี้ + 1
    }

    // กด Reset: กลับเวลาจริงของเครื่อง → ใส่เวลาจริงลงช่อง → เขียนบรรทัด Now ใหม่ → บอก AdminFrame
    private void handleReset() {
        clock.reset();       // ล้างเวลาที่ตั้งไว้ (AppClock.java บรรทัด 129)
        fillFields();
        showNow();
        onChange.run();
    }
}