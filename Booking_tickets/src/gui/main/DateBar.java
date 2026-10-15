//DateBar <- แถวปุ่มวันที่ใต้ป้าย "1. Date" ในหน้าลูกค้า กดปุ่มไหน วันนั้นถูกเลือก
/*
    หน้าตา (ตอนนี้จองได้แค่วันนี้ เลยมีปุ่มเดียว):
                    [ Today 14/10 ]
    ถ้าวันหลังให้จองได้ 3 วัน (แก้ BOOKABLE_DAYS ใน MovieService) ปุ่มขึ้นเอง ไม่ต้องแก้ไฟล์นี้:
            [ Today 14/10 ] [ 15/10 ] [ 16/10 ]
        ปุ่มที่เลือกอยู่ = พื้นฟ้า ตัวหนา / ปุ่มอื่น = พื้นขาว กรอบเทา

    "วันนี้" มาจากนาฬิกาของโปรแกรม (app.clock()) ไม่ได้ถามจากเครื่อง
        app.clock().setTo(20/10 09:00) → วันนี้ = 20/10 → ปุ่มขึ้น [ Today 20/10 ]
        ปุ่มไหนเป็นวันเดียวกับ today ถึงจะมีคำว่า Today (ไม่ได้ดูว่าเป็นปุ่มแรก)

    แถวนี้แค่บอกว่า "เปลี่ยนวันแล้ว" ผ่าน onChange ที่ส่งเข้ามา
    ส่วนการโหลดรอบฉายของวันใหม่เป็นงานของ MainFrame แถวนี้จะได้ไม่ต้องรู้จัก MovieService

    ใช้ยังไง (ใน MainFrame):
        LocalDate today = app.clock().now().toLocalDate();                           // วันนี้ของโปรแกรม เช่น 2026-10-14
        DateBar dateBar = new DateBar(app.movies().bookableDates(), today, () -> refreshShowtimes());
        dateBar.selected()                                                          // วันที่เลือกอยู่ เช่น 2026-10-14

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 209–213 (กล่องปุ่ม) และ 827–873 (refreshDateButtons)
    ต่างจากเดิม:
        เดิมปุ่มแรกได้คำว่า Today เสมอ (i == 0) → ใหม่เทียบกับ today จริง
        เดิมลิสต์วันที่ว่างแล้วแอบใช้ LocalDate.now() → ใหม่ throw ทันที (SC2 หน้า 9)

    คำที่ใช้บ่อยในไฟล์นี้:
        Border      = "ขอบ" ของปุ่ม คือสิ่งที่วาดรอบ ๆ ด้านนอก ต้อง new สร้างก่อน แล้วค่อยใส่ด้วย setBorder
        ตัวเลข 4 ตัวในขอบ เรียงเป็น (บน, ซ้าย, ล่าง, ขวา) เสมอ หน่วย px
 */

package gui.main;

import java.awt.*;                 // FlowLayout = เรียงปุ่มต่อกันเป็นแถว, Dimension = ขนาด, Cursor = รูปเมาส์
import java.time.LocalDate;        // วันที่อย่างเดียว ไม่มีเวลา เช่น 2026-10-14
import java.util.List;             // รายการวันที่ที่จองได้
import javax.swing.*;              // JPanel = แถว, JButton = ปุ่มวันที่
import javax.swing.border.*;       // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ

final class DateBar extends JPanel {

    // Abstraction Function (SC4 หน้า 10):
    //   AF(dates, today, selected) = แถวปุ่มวันที่ 1 ปุ่มต่อ 1 วันใน dates เรียงตามลำดับ
    //                                ปุ่มที่ตรงกับ today มีคำว่า Today / ปุ่มของ selected คือวันที่ลูกค้าเลือกอยู่
    // Rep Invariant:
    //   dates ไม่ว่าง และไม่มี null, today ไม่ใช่ null, onChange ไม่ใช่ null
    //   selected ต้องเป็นวันหนึ่งใน dates เสมอ (เลือกวันที่ไม่มีปุ่มไม่ได้)
    // Safety from rep exposure (SC4 หน้า 14):
    //   dates เก็บสำเนาแบบแก้ไม่ได้ (List.copyOf) คนส่งลิสต์มาแก้ลิสต์ของตัวเองทีหลัง ปุ่มก็ไม่เปลี่ยน
    //   LocalDate แก้ค่าไม่ได้ คืน selected ตัวจริงออกไปได้เลย
    private final List<LocalDate> dates;   // วันที่ที่มีปุ่ม เช่น [2026-10-14]
    private final LocalDate today;         // วันนี้ของโปรแกรม ใช้ตัดสินว่าปุ่มไหนเขียน Today
    private final Runnable onChange;       // สิ่งที่ต้องทำหลังเปลี่ยนวัน (MainFrame ส่งมา) เช่น () -> refreshShowtimes()
    private LocalDate selected;            // วันที่เลือกอยู่ตอนนี้ เปลี่ยนเมื่อกดปุ่ม

    /**
     * สร้างแถวปุ่มวันที่ เริ่มต้นเลือกวันแรกของลิสต์
     *
     * @param dates    วันที่ที่จองได้ เช่น app.movies().bookableDates() → [2026-10-14]
     *                 ต้องไม่ใช่ null ไม่ว่าง และไม่มี null ข้างใน
     * @param today    วันนี้ของโปรแกรม เช่น app.clock().now().toLocalDate() → 2026-10-14
     * @param onChange คำสั่ง run method นั้น
     *                 สิ่งที่ต้องทำหลังกดเปลี่ยนวัน (MainFrame ส่งมา) เช่น () -> refreshShowtimes()
     *                 Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onChange.run()
     * @throws IllegalArgumentException ถ้า dates เป็น null / ว่าง / มี null ข้างใน หรือ today / onChange เป็น null
     */
    DateBar(List<LocalDate> dates, LocalDate today, Runnable onChange) {
        // ① เรียงปุ่มต่อกันเป็นแถว อยู่กลาง
        //    super(...) = เรียก constructor ของ JPanel (ตัวแม่) ให้ใช้ FlowLayout
        //    FlowLayout(FlowLayout.CENTER, 8, 0) = จัดกลาง ปุ่มห่างกันแนวนอน 8 px แนวตั้ง 0
        super(new FlowLayout(FlowLayout.CENTER, 8, 0));

        // ② ตรวจของที่ส่งเข้ามา ผิดแล้ว throw เลย ไม่แอบใช้ค่าอื่นแทน (SC2 หน้า 9 Defensive Programming)
        //    ใช้ exception ไม่ใช่ assert เพราะเป็นของที่คนอื่นส่งมา assert ไว้ตรวจของในตัวเอง (SC4 หน้า 20)
        if (dates == null || dates.isEmpty()) {
            throw new IllegalArgumentException("dates must not be empty");
        }
        for (LocalDate date : dates) {   // ไล่ดูทีละวัน มี null ปนมา → ทำปุ่มไม่ได้ (null ไม่มีวัน/เดือน)
            if (date == null) {          // ไม่ใช้ dates.contains(null) เพราะลิสต์จาก List.of(...) ถามหา null แล้วพัง (NullPointerException)
                throw new IllegalArgumentException("dates must not contain null");
            }
        }
        if (today == null) {
            throw new IllegalArgumentException("today must not be null");
        }
        if (onChange == null) {
            throw new IllegalArgumentException("onChange must not be null");
        }

        // ③ เก็บของไว้ใช้ตอนวาดปุ่มและตอนกด
        //    List.copyOf(dates) = ทำสำเนาใหม่แบบแก้ไม่ได้ ลิสต์ตัวนอกถูกแก้ทีหลังก็ไม่กระทบ (SC4 หน้า 14)
        this.dates = List.copyOf(dates);
        this.today = today;
        this.onChange = onChange;
        this.selected = this.dates.get(0);   // เริ่มต้นเลือกวันแรก เหมือนเดิม (bookableDates วันแรก = วันนี้)

        // ④ ตั้งค่าตัวแถว (เดิมอยู่ MainFrame บรรทัด 210–212)
        setOpaque(false);                                       // โปร่งใส ให้เห็นพื้นขาวของการ์ด
        setAlignmentX(Component.CENTER_ALIGNMENT);              // อยู่กลางการ์ด (การ์ดเรียงของแนวตั้งด้วย BoxLayout)
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));   // กว้างได้เต็มการ์ด แต่สูงไม่เกิน 40 px ไม่ยืดกินที่ส่วนอื่น

        // ⑤ วาดปุ่มครั้งแรก
        drawButtons();
        checkRep();
    }

    /**
     * วันที่ที่ลูกค้าเลือกอยู่ตอนนี้
     *
     * @return เช่น 2026-10-14 (ไม่เป็น null และเป็นวันหนึ่งที่มีปุ่มเสมอ)
     */
    LocalDate selected() {
        return selected;
    }

    /**
     * วาดปุ่มใหม่ทั้งแถว ใช้ตอนเริ่มและทุกครั้งที่เปลี่ยนวัน เพื่อให้สีปุ่มตรงกับวันที่เลือก
     */
    private void drawButtons() {
        removeAll();                     // ลบปุ่มเก่าทิ้งก่อน ไม่งั้นปุ่มซ้อนเพิ่มทุกครั้งที่กด
        for (LocalDate date : dates) {   // 1 วัน = 1 ปุ่ม ตามลำดับในลิสต์
            add(makeButton(date));
        }
        revalidate();                    // บอก Swing ว่าของข้างในเปลี่ยน ให้จัดตำแหน่งใหม่
        repaint();                       // วาดใหม่บนจอ
    }

    /**
     * สร้างปุ่มของวันเดียว
     *
     * @param date วันของปุ่มนี้ เช่น 2026-10-14 (มาจากรายการ dates ที่ส่งเข้า DateBar)
     * @return JButton ปุ่มวันที่ 1 ปุ่ม เช่น [Today 14/10] กดแล้ว pick(วันนั้น)
     */
    private JButton makeButton(LocalDate date) {
        boolean isSelected = date.equals(selected);   // ปุ่มนี้คือวันที่เลือกอยู่ไหม

        // ข้อความบนปุ่ม: วัน/เดือน เช่น 14/10, 1/11
        //   getDayOfMonth() = วันที่ในเดือน เช่น 14 / getMonthValue() = เดือนเป็นเลข เช่น 10
        String text = date.getDayOfMonth() + "/" + date.getMonthValue();
        if (date.equals(today)) {     // เป็นวันนี้ของโปรแกรม → เติมคำว่า Today ข้างหน้า
            text = "Today " + text;   // เช่น "Today 14/10"
        }

        JButton button = new JButton(text);
        button.setFocusPainted(false);                      // กดแล้วไม่วาดกรอบประรอบคำ
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));   // ชี้เมาส์แล้วเป็นรูปมือ บอกว่ากดได้

        // สีกับฟอนต์ต่างกันตามว่าเลือกอยู่ไหม
        //   ขอบ LineBorder(สี, ความหนา, มุมโค้ง) = เส้นรอบปุ่ม (true = มุมโค้งมน)
        //   ขอบ EmptyBorder(5, 14, 5, 14) = ที่ว่างระหว่างเส้นกับตัวหนังสือ บน 5 ซ้าย 14 ล่าง 5 ขวา 14
        //   CompoundBorder(ตัวนอก, ตัวใน) = รวม 2 ชั้นเป็นขอบเดียว เพราะ setBorder ใส่ได้ทีละ 1 อัน
        Color borderColor;
        if (isSelected) {
            button.setFont(MainTheme.FONT_LABEL);            // Tahoma หนา 12
            button.setBackground(MainTheme.PRIMARY);         // พื้นฟ้า
            button.setForeground(MainTheme.BUTTON_TEXT);     // ตัวหนังสือดำ
            borderColor = MainTheme.PRIMARY;                 // กรอบฟ้า
        } else {
            button.setFont(MainTheme.FONT_SUBTITLE);         // Tahoma ปกติ 12
            button.setBackground(MainTheme.CARD);            // พื้นขาว
            button.setForeground(MainTheme.TEXT_LABEL);      // ตัวหนังสือเทาเข้ม
            borderColor = MainTheme.INPUT_BORDER;            // กรอบเทา
        }
        button.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(5, 14, 5, 14)));

        // กดปุ่ม → pick(date) = เปลี่ยนเป็นวันของปุ่มนี้
        //   e -> ... = lambda รับเหตุการณ์กดปุ่ม (e) แล้วทำสิ่งที่อยู่หลังลูกศร
        //   date ในวงเล็บคือวันของปุ่มนี้ แต่ละปุ่มจำวันของตัวเองไว้
        button.addActionListener(e -> pick(date));
        return button;
    }

    /**
     * เปลี่ยนวันที่เลือก แล้วบอก MainFrame
     *
     * @param date วันของปุ่มที่ถูกกด
     */
    private void pick(LocalDate date) {
        selected = date;     // จำวันใหม่
        drawButtons();       // วาดปุ่มใหม่ ปุ่มที่กดกลายเป็นสีฟ้า ปุ่มเก่ากลับเป็นสีขาว
        checkRep();          // ตรวจว่า selected ยังเป็นวันที่มีปุ่ม (SC4 หน้า 12 เรียกท้ายทุกเมธอดที่แก้ rep)
        onChange.run();      // ทำคำสั่งที่ MainFrame ส่งมา เช่น refreshShowtimes() โหลดรอบฉายของวันใหม่
    }

    /**
     * ตรวจ Rep Invariant ทุกข้อ ทำงานเฉพาะตอนรันด้วย java -ea (SC4 หน้า 12)
     */
    private void checkRep() {
        assert dates != null && !dates.isEmpty() : "dates ต้องไม่ว่าง";
        assert today != null && onChange != null : "today และ onChange ต้องไม่เป็น null";
        assert dates.contains(selected) : "selected ต้องเป็นวันที่มีปุ่ม";
    }
}