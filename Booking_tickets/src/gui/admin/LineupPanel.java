//LineupPanel <- การ์ดกลางของหน้า admin: เลือกหนัง 5 เรื่องที่จะฉายพรุ่งนี้ แล้วกด Save
/*
    หน้าตา (เลขตรงกับ ①–⑥ ในโค้ดข้างล่าง):
        ┌─────────────────────────────────────────────── ① การ์ดขาว มีเส้นขอบ
        │              Tomorrow's Lineup                 ② หัวข้อ
        │  Showing on 12/10/2026 | Select 5 movies ...   ③ วันที่ของพรุ่งนี้
        │  Movie 1: [Spider-Man: Brand New Day ▼]        ④ แถวเลือกหนัง 5 แถว
        │  ...
        │  Movie 5: [Blade Runner 2049         ▼]
        │  [              Save Lineup              ]     ⑤ ปุ่ม Save
        │  Today's lineup is locked (...)                ⑥ ข้อความเล็กใต้ปุ่ม
        └───────────────────────────────────────────────

    หน้านี้ไม่เช็กกฎเอง (ครบ 5 เรื่อง, ห้ามซ้ำ, ต้องเป็น admin)
    ส่งให้ AdminService เช็ก แล้วเอาข้อความ error มาโชว์ กฎจะได้อยู่ที่เดียว

    ใช้ของจาก AppServices:
      services.movies().movies()               → หนังทั้ง 15 เรื่อง ใส่เป็นตัวเลือกของทุกช่อง
      services.admin().tomorrowLineup()        → ชุดที่ตั้งไว้ให้พรุ่งนี้ ใส่เป็นค่าเริ่มต้นตอนเปิด (⑦)
      services.admin().tomorrow()              → วันที่ของพรุ่งนี้ เขียน "Showing on 12/10/2026" (③)
      services.admin().setTomorrowLineup(...)  → ตอนกดปุ่ม Save (⑧)

    ใช้ยังไง (ใน AdminFrame):
        center.add(new LineupPanel(admin, services));

    คำที่ใช้บ่อยในไฟล์นี้:
        BoxLayout(..., Y_AXIS)  = ตัวจัดวางที่เรียงของ "จากบนลงล่าง" ทีละชิ้น ตามลำดับที่ add
        setAlignmentX(CENTER_ALIGNMENT) = ใน BoxLayout แนวตั้ง ให้ชิ้นนี้อยู่กลางแนวนอน
        Box.createRigidArea(new Dimension(0, 20)) = "ก้อนว่าง" กว้าง 0 สูง 20 px ใส่คั่นระหว่างของ
        Dimension(กว้าง, สูง)    = ขนาด หน่วย px
        JComboBox<Movie>        = ช่องเลือกแบบกดแล้วมีรายการเด้งลงมา ในช่องเก็บ Movie ตัวจริง
        JOptionPane.showMessageDialog(หน้าที่เด้งทับ, ข้อความ, หัวกล่อง, ชนิดไอคอน) = กล่องข้อความเด้ง
 */

package gui.admin;

import java.awt.*;                          // Color = สี, Dimension = ขนาด, Cursor = รูปเมาส์, BorderLayout = แถวป้าย+ช่องเลือก
import java.io.IOException;                 // error ตอนอ่าน/เขียนไฟล์ CSV ไม่ได้
import java.time.format.DateTimeFormatter;  // แปลงวันที่เป็นข้อความ 12/10/2026
import java.util.ArrayList;                 // สร้างรายการใหม่: ช่องเลือก 5 ช่อง / หนังที่เลือกตอนกด Save
import java.util.List;                      // ชนิดของรายการ (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.swing.*;                       // JPanel, JLabel, JButton, JComboBox = ช่องเลือก, JOptionPane = กล่องเด้ง, Box, BoxLayout
import javax.swing.border.*;                // LineBorder = เส้นรอบ, EmptyBorder = ที่ว่าง, CompoundBorder = ซ้อน 2 ขอบ
import model.Movie;                         // หนัง 1 เรื่อง เก็บในช่องเลือก
import model.User;                          // admin ที่ login อยู่ ส่งให้ AdminService เช็กสิทธิ์
import service.AdminService;                // ใช้ LINEUP_SIZE (5) กำหนดจำนวนช่องเลือก ให้ตรงกับกฎ
import service.AppServices;                 // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

final class LineupPanel extends JPanel {

    /** รูปแบบวันที่ที่โชว์บนจอ dd = วัน 2 หลัก, MM = เดือน 2 หลัก, yyyy = ปี 4 หลัก เช่น 12/10/2026 */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final User admin;                    // คนที่ login อยู่ (role ADMIN) ส่งต่อให้ AdminService เช็กสิทธิ์ตอน Save
    private final AppServices services;          // ของหลังบ้านตัวเดียวกับทั้งโปรแกรม ใช้เรียก movies() / admin()
    private final List<Movie> allMovies;         // หนังทั้งหมด (15 เรื่อง) = ตัวเลือกของทุกช่อง
    private final List<JComboBox<Movie>> movieBoxes = new ArrayList<>(); // ช่องเลือก Movie 1–5 เรียงตามลำดับ ตอน Save หยิบออกมาตามลำดับนี้

    /**
     * สร้างการ์ดกลาง แล้วใส่ชุดหนังที่ตั้งไว้ให้พรุ่งนี้ลงช่องเลือก
     *
     * @param admin    คนที่ login อยู่ (AdminFrame ส่งมา)
     * @param services ของหลังบ้านชุดเดียวกับทั้งโปรแกรม (AdminFrame ส่งมา)
     */
    LineupPanel(User admin, AppServices services) {
        this.admin = admin;
        this.services = services;
        this.allMovies = loadAllMovies();   // ต้องโหลดหนังก่อน เพราะ ④ ใช้ allMovies สร้างช่องเลือก

        // ① ตัวการ์ด
        //    BoxLayout(this, Y_AXIS) = เรียงของในการ์ดนี้ (this) จากบนลงล่าง ตามลำดับที่ add
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);   // พื้นการ์ดสีขาว ตัดกับพื้นเทาของหน้าต่าง
        //    ขอบการ์ด 2 ชั้น (อ่านแบบเดียวกับ AdminTopBar)
        //    ชั้นนอก LineBorder(สี, หนา 1 px, true = มุมโค้ง) = เส้นเทาอ่อนรอบการ์ด
        //    ชั้นใน EmptyBorder(บน 24, ซ้าย 40, ล่าง 24, ขวา 40) = ที่ว่างข้างในการ์ด ของไม่ชิดเส้น
        setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(24, 40, 24, 40)));
        //    ขนาดการ์ด กว้าง 560 สูง 420 px (AdminFrame วางการ์ดไว้กลางจอตามขนาดนี้)
        setPreferredSize(new Dimension(560, 420));

        addTitle();            // ② ③
        addMovieRows();        // ④
        addSaveButton();       // ⑤ ⑥
        showTomorrowLineup();  // ⑦ ต้องทำหลัง ④ เพราะต้องมีช่องเลือกก่อนถึงจะใส่ค่าลงไปได้
    }

    /**
     * หนังทั้งหมด ใช้เป็นตัวเลือกของทุกช่อง
     * อ่านไม่ได้ → โชว์ error แล้วคืนรายการว่าง (ช่องเลือกจะว่าง และปุ่ม Save ถูกปิดใน ⑤)
     */
    private List<Movie> loadAllMovies() {
        try {
            return services.movies().movies();
        } catch (IOException e) {
            showError("Cannot load movies: " + e.getMessage());   // e.getMessage() = สาเหตุ เช่น movies.csv line 3: ...
            return new ArrayList<>();
        }
    }

    /** ② หัวข้อ "Tomorrow's Lineup" + ③ บรรทัดวันที่ */
    private void addTitle() {
        // ② หัวข้อ ตัวหนา 20 สีเกือบดำ อยู่กลาง
        JLabel header = new JLabel("Tomorrow's Lineup");
        header.setFont(AdminTheme.FONT_HEADER);
        header.setForeground(AdminTheme.TEXT_DARK);
        header.setAlignmentX(CENTER_ALIGNMENT); 
        add(header);

        // ③ บรรทัดวันที่ ตัวเล็ก 12 สีเทา อยู่กลาง
        //    services.admin().tomorrow() = วันพรุ่งนี้ตามนาฬิกาของโปรแกรม (วันเดียวกับที่ Save จะบันทึก)
        //    .format(DATE_FORMAT)        = แปลง 2026-10-12 เป็นข้อความ "12/10/2026"
        String tomorrow = services.admin().tomorrow().format(DATE_FORMAT);
        JLabel subtitle = new JLabel("Showing on " + tomorrow
                + "  |  Select 5 movies (Movie 1 = slot 1 in schedule)");
        subtitle.setFont(AdminTheme.FONT_SUBTITLE);
        subtitle.setForeground(AdminTheme.TEXT_MUTED);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        add(subtitle);
        add(Box.createRigidArea(new Dimension(0, 20)));   // ที่ว่าง 20 px ระหว่างวันที่กับแถว Movie 1
    }

    /**
     * ④ แถวเลือกหนัง Movie 1–5 (5 แถว ตาม AdminService.LINEUP_SIZE)
     * ลำดับมีผล: ช่องที่ 1 = เรื่องที่ 1 ใน schedule.csv
     */
    private void addMovieRows() {
        for (int slot = 1; slot <= AdminService.LINEUP_SIZE; slot++) {   // slot = 1, 2, 3, 4, 5
            add(createMovieRow(slot));
            add(Box.createRigidArea(new Dimension(0, 10)));   // ที่ว่าง 10 px ระหว่างแถว
        }
        add(Box.createRigidArea(new Dimension(0, 14)));       // ที่ว่างเพิ่มอีก 14 px ก่อนปุ่ม Save
    }

    /**
     * ④ แถว 1 แถว: ป้าย "Movie 1:" ทางซ้าย + ช่องเลือกหนังกินที่ที่เหลือ
     * ช่องเลือกเก็บไว้ใน movieBoxes ตามลำดับ ตอนกด Save จะได้หยิบออกมาเรียงถูก
     *
     * @param slot เลขช่อง 1–5
     * @return แถวที่ประกอบเสร็จแล้ว ให้ addMovieRows เอาไป add ลงการ์ด
     */
    private JPanel createMovieRow(int slot) {
        // ป้ายซ้าย เช่น "Movie 1:" ตัวหนา 12 กว้างตายตัว 70 px ทุกแถว ช่องเลือกจะได้เริ่มตรงกัน
        JLabel label = new JLabel("Movie " + slot + ":");
        label.setFont(AdminTheme.FONT_LABEL);
        label.setPreferredSize(new Dimension(70, 30));

        // ช่องเลือกหนัง
        //    allMovies.toArray(new Movie[0]) = แปลงรายการหนังเป็น array เพราะ JComboBox รับ array
        //    ในช่องเก็บ Movie ตัวจริง (ไม่ใช่แค่ชื่อ) ตอน Save จะได้ส่ง Movie ให้ AdminService ได้เลย
        JComboBox<Movie> box = new JComboBox<>(allMovies.toArray(new Movie[0]));
        box.setFont(AdminTheme.FONT_TEXT);
        box.setBackground(Color.WHITE);
        box.setRenderer(new MovieRenderer());   // บอกว่าแต่ละเรื่องโชว์เป็น "Inception  (148 min)"
        movieBoxes.add(box);                    // จำไว้ตามลำดับ (ช่องที่ 1 อยู่ตำแหน่ง 0)

        // ประกอบแถว: BorderLayout(ห่างแนวนอน 10, แนวตั้ง 0) ป้ายไป WEST (ซ้าย) ช่องเลือกไป CENTER (กินที่ที่เหลือ)
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);                         // โปร่งใส ให้เห็นพื้นขาวของการ์ด
        row.setMaximumSize(new Dimension(460, 36));   // BoxLayout จะยืดแถวได้ไม่เกิน กว้าง 460 สูง 36
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.add(label, BorderLayout.WEST);
        row.add(box, BorderLayout.CENTER);
        return row;
    }

    /** ⑤ ปุ่ม Save + ⑥ ข้อความเล็กบอกว่าแก้ของวันนี้ไม่ได้ */
    private void addSaveButton() {
        // ⑤ ปุ่ม Save สีฟ้า ตัวหนังสือขาว
        JButton save = new JButton("Save Lineup");
        save.setFont(AdminTheme.FONT_BUTTON);
        save.setBackground(AdminTheme.PRIMARY);
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);                       // กดแล้วไม่วาดกรอบประรอบคำ
        save.setCursor(new Cursor(Cursor.HAND_CURSOR));    // ชี้แล้วเป็นรูปมือ
        save.setAlignmentX(CENTER_ALIGNMENT);
        save.setMaximumSize(new Dimension(460, 40));       // กว้างเท่าแถวเลือกหนัง สูง 40
        save.setEnabled(!allMovies.isEmpty());             // โหลดหนังไม่ได้ (รายการว่าง) → ปุ่มเป็นสีจาง กดไม่ได้
        save.addActionListener(e -> handleSave());         // กดปุ่ม → ทำ ⑧ handleSave()
        add(save);
        add(Box.createRigidArea(new Dimension(0, 10)));    // ที่ว่าง 10 px ระหว่างปุ่มกับข้อความเล็ก

        // ⑥ ข้อความเล็กสีเทา บอกว่าทำไมแก้ได้แค่พรุ่งนี้
        JLabel note = new JLabel("Today's lineup is locked (customers may already have tickets).");
        note.setFont(AdminTheme.FONT_SMALL);
        note.setForeground(AdminTheme.TEXT_MUTED);
        note.setAlignmentX(CENTER_ALIGNMENT);
        add(note);
    }

    /**
     * ⑦ ใส่ชุดหนังที่ตั้งไว้ให้พรุ่งนี้ลงช่องเลือก admin จะเห็นว่าตอนนี้ตั้งไว้เป็นอะไร
     * ยังไม่เคยตั้งของพรุ่งนี้ → ได้ชุดล่าสุดก่อนหน้า (ชุดที่พรุ่งนี้จะฉายจริงถ้าไม่เปลี่ยน)
     * เช่น ได้ [M1, M2, M3, M4, M5] → Movie 1 = Spider-Man, ..., Movie 5 = Blade Runner 2049
     */
    private void showTomorrowLineup() {
        // โหลดหนังไม่ได้ตั้งแต่ต้น (ช่องเลือกว่าง) → ไม่ต้องอ่านต่อ ไม่งั้น error เด้งซ้ำเป็นกล่องที่ 2
        if (allMovies.isEmpty()) {
            return;
        }
        List<Movie> lineup;
        try {
            lineup = services.admin().tomorrowLineup();
        } catch (IOException e) {
            showError("Cannot load tomorrow's lineup: " + e.getMessage());
            return;   // ช่องเลือกยังใช้ได้ แค่ไม่มีค่าเริ่มต้นจากไฟล์ (ทุกช่องเป็นเรื่องแรกของรายการ)
        }
        // ไล่ทีละช่อง i = 0..4 (ไม่เกินจำนวนช่อง และไม่เกินจำนวนเรื่องที่ได้มา)
        // setSelectedItem(หนัง) = ให้ช่องนั้นแสดงหนังเรื่องนี้เป็นตัวที่เลือกอยู่
        for (int i = 0; i < movieBoxes.size() && i < lineup.size(); i++) {
            movieBoxes.get(i).setSelectedItem(lineup.get(i));
        }
    }

    /**
     * ⑧ ปุ่ม Save: เอาหนังจาก 5 ช่องเรียงตามลำดับ ส่งให้ AdminService เช็กและบันทึก
     * สำเร็จ → กล่อง "Saved lineup for 12/10/2026"
     * ผิดกฎ (เช่นเลือกซ้ำ) → กล่องเตือนพร้อมข้อความจาก AdminService ตรง ๆ เช่น "Movie M6 is selected more than once."
     * เขียนไฟล์ไม่ได้ → กล่อง error "Cannot save: ..."
     */
    private void handleSave() {
        // หยิบหนังจากช่อง 1 → 5 ใส่รายการตามลำดับ
        List<Movie> chosen = new ArrayList<>();
        for (JComboBox<Movie> box : movieBoxes) {
            // getSelectedItem() คืนชนิด Object → (Movie) แปลงกลับเป็น Movie (ในช่องมีแต่ Movie อยู่แล้ว)
            chosen.add((Movie) box.getSelectedItem());
        }
        try {
            // AdminService เช็ก: เป็น admin ไหม, ครบ 5 ไหม, ซ้ำไหม, มีหนังจริงไหม → ผ่านค่อยเขียน lineup.csv
            services.admin().setTomorrowLineup(admin, chosen);
            JOptionPane.showMessageDialog(this,
                    "Saved lineup for " + services.admin().tomorrow().format(DATE_FORMAT),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);    // ไอคอน i สีฟ้า
        } catch (IllegalArgumentException e) {
            // ผิดกฎของ AdminService → บอก admin ให้แก้แล้วกด Save ใหม่ (ไฟล์ไม่ถูกแก้)
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot save", JOptionPane.WARNING_MESSAGE);   // ไอคอน ! สีเหลือง
        } catch (IOException e) {
            showError("Cannot save: " + e.getMessage());
        }
    }

    /** ⑨ กล่อง error (ไอคอน X สีแดง) ใช้ตอนอ่าน/เขียนไฟล์ไม่ได้ */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}