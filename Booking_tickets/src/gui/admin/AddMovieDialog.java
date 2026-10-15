//AddMovieDialog <- กล่องให้ admin กรอกหนังเรื่องใหม่ (ชื่อ, ความยาว, รูปโปสเตอร์) แล้วบันทึกลง movies.csv
/*
    หน้าตา:
        ┌──────────── Add New Movie ─────────────┐
        │             Add New Movie               │  ① หัวข้อ
        │   New movie will be saved to movies.csv │
        │   Title:     [ Dune: Part Two        ]  │  ② ช่องชื่อหนัง
        │   Duration:  [ 166 ]  min (1-180)       │  ③ ช่องความยาว
        │   Poster:    [Choose...]  Dune.JPG      │  ④ ปุ่มเลือกรูป + ชื่อไฟล์ที่เลือก
        │            [    Add Movie    ]         │  ⑤ ปุ่มบันทึก
        └─────────────────────────────────────────┘

    กด Add Movie (หรือกด Enter ในช่องชื่อ / ช่องความยาว):
        ความยาวไม่ใช่ตัวเลข     → กล่องเตือน "Duration must be a whole number ..." อยู่กล่องเดิม
        ผิดกฎ (AdminService)   → กล่องเตือน "Cannot add movie" + ข้อความจาก AdminService อยู่กล่องเดิม
                                 เช่น "This movie already exists: M2"
        อ่าน/เขียนไฟล์ไม่ได้      → กล่อง error อยู่กล่องเดิม
        สำเร็จ                 → กล่อง "Movie added: M16 ..." → ปิดกล่องนี้ → onDone.run()
                                 (AdminFrame ส่ง showLineup มา ช่อง Movie 1–5 จะมีเรื่องใหม่ให้เลือก)

    กล่องนี้แค่รับค่าแล้วส่งต่อ กฎทั้งหมด (ใครเพิ่มได้, ชื่อซ้ำ, ความยาว, ชนิดรูป, ออกรหัส, copy รูป)
    อยู่ที่ AdminService.addMovie ที่เดียว (แบบเดียวกับ LineupPanel ที่ส่งให้ setTomorrowLineup เช็ก)

    ใช้ยังไง (ใน AdminFrame):
        new AddMovieDialog(this, admin, services.admin(), () -> showLineup()).setVisible(true);

    คำที่ใช้บ่อยในไฟล์นี้:
        modal (true ใน super) = เปิดกล่องแล้วกดหน้า admin ข้างหลังไม่ได้ จนกว่าจะปิดกล่อง
        GridBagLayout = วางของเป็นตาราง แถว (gridy) / คอลัมน์ (gridx) ใช้ทำป้ายซ้าย ช่องกรอกขวา
        JFileChooser = กล่องเลือกไฟล์ของ Java (แบบ Open file ใน Windows)
        JOptionPane.showMessageDialog(หน้าที่เด้งทับ, ข้อความ, หัวกล่อง, ชนิดไอคอน) = กล่องแจ้งเตือน
 */

package gui.admin;

import java.awt.*;                          // GridBagLayout, GridBagConstraints, Insets, Dimension, Cursor, FlowLayout
import java.io.File;                        // ไฟล์ที่ได้จาก JFileChooser (getSelectedFile คืนเป็น File)
import java.io.IOException;                 // error ตอนอ่าน/เขียน movies.csv หรือ copy รูปไม่ได้
import java.nio.file.Path;                  // ส่งไฟล์รูปให้ AdminService (addMovie รับเป็น Path)
import javax.swing.*;                       // JDialog, JPanel, JLabel, JTextField, JButton, JFileChooser, JOptionPane
import javax.swing.border.*;                // EmptyBorder = ที่ว่าง, LineBorder = เส้นรอบ, CompoundBorder = ซ้อน 2 ขอบ
import javax.swing.filechooser.FileNameExtensionFilter;   // ให้กล่องเลือกไฟล์โชว์แค่รูป .jpg .jpeg .png
import model.Movie;                         // หนังเรื่องใหม่ที่ addMovie คืนมา / MAX_DURATION_MINUTES (180)
import model.User;                          // admin ที่ login อยู่ ส่งให้ AdminService เช็กว่าเป็น admin
import service.AdminService;                // ตัวเพิ่มหนังจริง (เช็กกฎ + บันทึก + copy รูป)

final class AddMovieDialog extends JDialog {

    private static final Dimension BUTTON_SIZE = new Dimension(320, 40);   // ขนาดปุ่ม Add Movie

    private final User admin;                   // admin ที่ login อยู่ (ส่งต่อให้ addMovie)
    private final AdminService adminService;    // ตัวเพิ่มหนัง (app.admin() ตัวเดียวกับทั้งโปรแกรม)
    private final Runnable onDone;              // สิ่งที่ทำหลังเพิ่มหนังสำเร็จ (AdminFrame ส่งมา) เช่น () -> showLineup()
    private final JTextField titleField = new JTextField(22);    // ช่องชื่อหนัง กว้างประมาณ 22 ตัวอักษร
    private final JTextField minutesField = new JTextField(5);   // ช่องความยาว (นาที) กว้างประมาณ 5 ตัวอักษร
    private final JLabel posterLabel = new JLabel("(no poster)"); // ชื่อไฟล์รูปที่เลือก ยังไม่เลือก = (no poster)
    private Path posterFile;                    // รูปที่ admin เลือก ยังไม่เลือก = null (addMovie รับ null ได้)

    /**
     * สร้างกล่องเพิ่มหนัง (ยังไม่โชว์ ต้องเรียก setVisible(true))
     *
     * @param owner        หน้าต่างแม่ เช่น AdminFrame (this) กล่องจะเด้งกลางหน้านี้
     * @param admin        admin ที่ login อยู่ ห้าม null
     * @param adminService ตัวเพิ่มหนัง เช่น services.admin() ห้าม null
     * @param onDone       คำสั่งที่ทำหลังเพิ่มหนังสำเร็จ เช่น () -> showLineup() ห้าม null
     *                     Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onDone.run()
     * @throws IllegalArgumentException ถ้า admin / adminService / onDone เป็น null
     */
    AddMovieDialog(JFrame owner, User admin, AdminService adminService, Runnable onDone) {
        super(owner, "Add New Movie", true);   // true = modal
        // ตรวจของที่ส่งเข้ามา (SC2 หน้า 9) ส่วน "เป็น admin ไหม" ให้ AdminService เช็ก (กฎอยู่ที่เดียว)
        if (admin == null || adminService == null || onDone == null) {
            throw new IllegalArgumentException("admin, adminService and onDone must not be null");
        }
        this.admin = admin;
        this.adminService = adminService;
        this.onDone = onDone;

        // ช่องกรอก: ตัวหนังสือขนาด 14 / กด Enter ในช่อง = กด Add Movie (ไม่ต้องเอื้อมไปกดปุ่ม)
        titleField.setFont(AdminTheme.FONT_TEXT.deriveFont(14f));
        minutesField.setFont(AdminTheme.FONT_TEXT.deriveFont(14f));
        titleField.addActionListener(e -> handleAdd());
        minutesField.addActionListener(e -> handleAdd());
        posterLabel.setFont(AdminTheme.FONT_SUBTITLE);
        posterLabel.setForeground(AdminTheme.TEXT_MUTED);

        // ตัวกล่อง: พื้นขาว วางเป็นตาราง เว้นขอบ บน 20 ซ้าย 24 ล่าง 20 ขวา 24
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // GridBagConstraints = "ที่วาง" ของชิ้นถัดไป: แถวไหน (gridy) คอลัมน์ไหน (gridx) กินกี่คอลัมน์ (gridwidth)
        //   insets = ระยะห่างรอบชิ้น บน 6 ซ้าย 4 ล่าง 6 ขวา 4 / anchor WEST = ชิดซ้ายของช่อง
        GridBagConstraints at = new GridBagConstraints();
        at.insets = new Insets(6, 4, 6, 4);

        // ① หัวข้อ 2 บรรทัด กิน 2 คอลัมน์ อยู่กลาง
        at.gridx = 0;
        at.gridwidth = 2;
        at.anchor = GridBagConstraints.CENTER;
        at.gridy = 0;
        panel.add(label("Add New Movie", AdminTheme.FONT_HEADER.deriveFont(18f), AdminTheme.TEXT_DARK), at);
        at.gridy = 1;
        panel.add(label("New movie will be saved to movies.csv", AdminTheme.FONT_SUBTITLE, AdminTheme.TEXT_MUTED), at);

        // ②③④ แถวกรอก: ป้ายคอลัมน์ 0 / ช่องคอลัมน์ 1 ชิดซ้าย
        at.gridwidth = 1;
        at.anchor = GridBagConstraints.WEST;
        addRow(panel, at, 2, "Title:", titleField);
        //    ความยาวสูงสุดอ่านจาก Movie.MAX_DURATION_MINUTES (180) ไม่เขียนเลขเอง → "min (1-180)"
        addRow(panel, at, 3, "Duration:", inline(minutesField,
                label("min (1-" + Movie.MAX_DURATION_MINUTES + ")", AdminTheme.FONT_SUBTITLE, AdminTheme.TEXT_MUTED)));
        JButton choose = whiteButton("Choose...");
        choose.addActionListener(e -> choosePoster());
        addRow(panel, at, 4, "Poster:", inline(choose, posterLabel));

        // ⑤ ปุ่ม Add Movie กิน 2 คอลัมน์ อยู่กลาง เว้นบนมากหน่อย (14) ให้แยกจากช่องกรอก
        JButton add = new JButton("Add Movie");
        add.setFont(AdminTheme.FONT_BUTTON);
        add.setBackground(AdminTheme.PRIMARY);
        add.setForeground(Color.BLACK);
        add.setFocusPainted(false);
        add.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add.setPreferredSize(BUTTON_SIZE);
        add.setBorder(new LineBorder(AdminTheme.PRIMARY, 1, true));
        add.addActionListener(e -> handleAdd());
        at.gridx = 0;
        at.gridy = 5;
        at.gridwidth = 2;
        at.anchor = GridBagConstraints.CENTER;
        at.insets = new Insets(14, 4, 0, 4);
        panel.add(add, at);

        setContentPane(panel);
        setResizable(false);
        pack();                          // ขนาดพอดีของข้างใน
        setLocationRelativeTo(owner);    // เด้งกลางหน้า admin
    }

    /**
     * ④ กด Choose... → เปิดกล่องเลือกไฟล์ (โชว์แค่รูป) → เลือกแล้วจำไฟล์ + โชว์ชื่อไฟล์
     *   เช่น เลือก C:\Users\me\Pictures\Dune.JPG → posterFile = ไฟล์นั้น, ป้ายขึ้น "Dune.JPG"
     *   กด Cancel ในกล่องเลือกไฟล์ → ไม่เปลี่ยนอะไร (ไฟล์ที่เลือกไว้ก่อนหน้ายังอยู่)
     */
    private void choosePoster() {
        JFileChooser chooser = new JFileChooser();
        // FileNameExtensionFilter(ชื่อที่โชว์, นามสกุลที่รับ...) = ซ่อนไฟล์อื่น เห็นแค่รูป
        //   (แค่ช่วยให้เลือกง่าย ถ้าพิมพ์ชื่อไฟล์อื่นเอง AdminService ก็ยังเช็กชนิดซ้ำอีกที)
        chooser.setFileFilter(new FileNameExtensionFilter("Images (jpg, jpeg, png)", "jpg", "jpeg", "png"));
        // showOpenDialog = เปิดกล่องเลือกไฟล์ รอจนกดปุ่ม → APPROVE_OPTION = กด Open / อื่น ๆ = Cancel หรือปิด
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();   // ไฟล์ที่เลือก เช่น C:\Users\me\Pictures\Dune.JPG
            posterFile = file.toPath();              // File → Path เพราะ addMovie รับ Path
            posterLabel.setText(file.getName());     // โชว์แค่ชื่อไฟล์ "Dune.JPG"
            posterLabel.setForeground(AdminTheme.TEXT_DARK);
            pack();                                  // ชื่อไฟล์ยาว กล่องขยายให้พอดี
        }
    }

    /**
     * ⑤ กด Add Movie: แปลงความยาวเป็นตัวเลข → ส่งให้ AdminService เพิ่มหนัง → บอกผล
     */
    private void handleAdd() {
        // ความยาวเป็นข้อความในช่อง ต้องแปลงเป็นตัวเลขก่อน "166" → 166 / "abc" หรือว่าง → แปลงไม่ได้
        int minutes;
        try {
            minutes = Integer.parseInt(minutesField.getText().trim());
        } catch (NumberFormatException e) {
            warn("Duration must be a whole number of minutes (e.g. 166).");
            return;   // อยู่กล่องเดิม ให้แก้ช่องความยาว
        }
        try {
            // addMovie(admin, ชื่อที่พิมพ์, นาที, รูปที่เลือกหรือ null) → หนังเรื่องใหม่ เช่น M16
            //   เช็กกฎทั้งหมด บันทึก movies.csv และ copy รูปให้ (AdminService.java)
            Movie movie = adminService.addMovie(admin, titleField.getText(), minutes, posterFile);
            JOptionPane.showMessageDialog(this,
                    "Movie added: " + movie.id() + " " + movie.title() + " (" + movie.durationMinutes() + " min)",
                    "Added", JOptionPane.INFORMATION_MESSAGE);   // ไอคอน i สีฟ้า
            dispose();        // ปิดกล่องเพิ่มหนัง
            onDone.run();     // ทำคำสั่งที่ AdminFrame ส่งมา เช่น showLineup() ให้ช่อง Movie 1–5 มีเรื่องใหม่
        } catch (IllegalArgumentException e) {
            // ผิดกฎ เช่น ชื่อซ้ำ / ความยาวเกิน 180 / รูปไม่ใช่ jpg png (ข้อความจาก AdminService อ่านได้เลย)
            warn(e.getMessage());
        } catch (IOException e) {
            // อ่าน/เขียนไฟล์ไม่ได้ เช่น movies.csv ถูกเปิดค้างใน Excel
            JOptionPane.showMessageDialog(this, "Cannot add movie: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * กล่องเตือนกรอกผิด (ไอคอน ! สีเหลือง) หัวกล่องเดียวกันทุกกรณี แบบเดียวกับ "Cannot save" ใน LineupPanel
     *   ผลที่ได้: เด้งกล่องเตือน "Cannot add movie" อย่างเดียว
     *
     * @param message ข้อความเตือน เช่น "This movie already exists: M2"
     */
    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot add movie", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * ใส่ 1 แถว: ป้ายตัวหนาคอลัมน์ 0 + ของคอลัมน์ 1 ที่แถว row เช่น addRow(panel, at, 2, "Title:", ช่องชื่อ)
     *   ผลที่ได้: ใส่ป้ายคอลัมน์ 0 + ของคอลัมน์ 1 ลง panel
     *
     * @param panel กล่องที่จะใส่
     * @param at ตัวบอกตำแหน่ง (GridBagConstraints)
     * @param row แถวที่เท่าไร เช่น 2
     * @param text ป้ายซ้าย เช่น "Title:"
     * @param field ของที่วางขวา เช่น ช่องชื่อหนัง
     */
    private static void addRow(JPanel panel, GridBagConstraints at, int row, String text, Component field) {
        at.gridy = row;
        at.gridx = 0;
        panel.add(label(text, AdminTheme.FONT_LABEL, AdminTheme.TEXT_DARK), at);
        at.gridx = 1;
        panel.add(field, at);
    }

    /**
     * ของ 2 ชิ้นเรียงซ้ายไปขวาในช่องเดียว เช่น [ช่อง 166] + "min (1-180)" / ห่างกัน 8 px
     *   FlowLayout(LEFT, 0, 0) = ชิดซ้าย ไม่เว้นขอบ (ถ้าใส่ช่องห่าง 8 ใน FlowLayout จะเว้นขอบซ้ายด้วย
     *   ช่องความยาวจะเยื้องไปขวาไม่ตรงกับช่องชื่อ) เลยใส่ก้อนว่าง 8 px ไว้ตรงกลางแทน
     *
     * @param first ชิ้นซ้าย เช่น ช่อง 166
     * @param second ชิ้นขวา เช่น ป้าย "min (1-180)"
     * @return JPanel กล่องที่มี 2 ชิ้นเรียงซ้ายไปขวา ห่างกัน 8 px
     */
    private static JPanel inline(Component first, Component second) {
        JPanel box = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        box.setOpaque(false);
        box.add(first);
        box.add(Box.createHorizontalStrut(8));   // ก้อนว่างกว้าง 8 px ระหว่าง 2 ชิ้น
        box.add(second);
        return box;
    }

    /**
     * ป้ายข้อความ
     *
     * @param text ข้อความ เช่น "Title:"
     * @param font ฟอนต์
     * @param color สีตัวหนังสือ
     * @return JLabel ป้ายที่ตั้งฟอนต์และสีแล้ว
     */
    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * ปุ่มพื้นขาว กรอบเทา (ปุ่ม Choose...)
     *
     * @param text คำบนปุ่ม เช่น "Choose..."
     * @return JButton ปุ่มพื้นขาว กรอบเทา (ยังไม่ได้ผูกว่ากดแล้วทำอะไร คนเรียกผูกเอง)
     */
    private static JButton whiteButton(String text) {
        JButton button = new JButton(text);
        button.setFont(AdminTheme.FONT_SUBTITLE);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new CompoundBorder(
                new LineBorder(AdminTheme.BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        return button;
    }
}
