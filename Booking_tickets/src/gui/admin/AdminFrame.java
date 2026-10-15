//AdminFrame <- หน้าต่างของ admin: เอาแถบบน (AdminTopBar) การ์ดเลือกหนัง (LineupPanel) การ์ดตั้งเวลา (ClockPanel) และปุ่มเพิ่ม/ลบหนัง มาประกอบเป็นหน้าเดียว
/*
    หน้าตา (เลขตรงกับ ①–⑦ ในโค้ดข้างล่าง):
        ┌──────────────────────────────────────────────┐  ① หน้าต่าง 920 x 730 ชื่อ "KU CINEMA - Admin"
        │ AdminTopBar  (KU CINEMA / Admin / Logout)     │  ③ โซนบน (NORTH)
        ├──────────────────────────────────────────────┤
        │  พื้นเทา                                      │  ② พื้นที่กลาง (CENTER) วางคอลัมน์ไว้ตรงกลาง
        │          ┌──── LineupPanel ────┐             │     ในคอลัมน์: lineupBox (ใส่ LineupPanel)
        │          └─────────────────────┘             │                ช่องว่าง 16 px
        │          ┌──── ClockPanel ─────┐             │                ClockPanel
        │          └─────────────────────┘             │                ช่องว่าง 12 px
        │   [ + Add New Movie ]  [ - Delete Movie ]    │                ปุ่มเพิ่มหนัง / ลบหนัง (แถวเดียวกัน)
        └──────────────────────────────────────────────┘
        กด Logout → ⑤ ถามยืนยัน → ปิดหน้านี้ → เปิดหน้า Login
        กด Set / Reset ใน ClockPanel → ClockPanel บอกว่าเวลาเปลี่ยนแล้ว → ⑥ showLineup() ใส่ LineupPanel ตัวใหม่
                                      → Showing on เป็นวันนี้ (ตามเวลาใหม่) + 1
        กด + Add New Movie → ⑦ เปิดกล่อง AddMovieDialog → เพิ่มหนังสำเร็จ → ⑥ showLineup()
                                      → ช่อง Movie 1–5 มีหนังเรื่องใหม่ให้เลือก
        กด - Delete Movie → ⑧ เลือกหนังที่จะลบ → ถามยืนยัน → AdminService.deleteMovie
                                      → ลบสำเร็จ ⑥ showLineup() (ช่องเลือกไม่มีเรื่องนั้นแล้ว)
                                      → ผิดกฎ (เหลือ 10 เรื่อง / อยู่ใน lineup) เด้งข้อความบอกเหตุผล ไม่ลบอะไร

    ไฟล์นี้ทำแค่ 5 อย่าง:
      1. ประกอบหน้าต่าง (ขนาด, พื้นหลัง, วางแถบบน การ์ดเลือกหนัง การ์ดตั้งเวลา)
      2. ปุ่ม Logout: ถามยืนยัน → ปิดหน้านี้ → กลับหน้า Login
      3. เวลาเปลี่ยน → ใส่ LineupPanel ตัวใหม่ ข้อความ Showing on จะได้ตรงกับวันที่ที่ Save จะบันทึก
      4. ปุ่ม + Add New Movie → เปิดกล่องเพิ่มหนัง → เพิ่มเสร็จใส่ LineupPanel ตัวใหม่ (ช่องเลือกมีเรื่องใหม่)
      5. ปุ่ม - Delete Movie → เลือกหนัง + ยืนยัน → ลบเสร็จใส่ LineupPanel ตัวใหม่ (กฎการลบอยู่ใน AdminService)
    ส่วนหน้าตาของแถบบนอยู่ใน AdminTopBar / การเลือกหนังอยู่ใน LineupPanel / การตั้งเวลาอยู่ใน ClockPanel
    / กล่องกรอกหนังใหม่อยู่ใน AddMovieDialog

    เปิดหน้านี้ยังไง (LoginFrame หลัง login แล้วเป็น admin):
        new AdminFrame(user, services).setVisible(true);

    คำที่ใช้บ่อยในไฟล์นี้:
        JFrame          = หน้าต่าง 1 บาน (มีแถบชื่อ ปุ่มปิด) ข้างในใส่ JPanel
        BorderLayout    = แบ่งพื้นที่เป็นโซน NORTH (บน) / CENTER (กลาง กินที่ที่เหลือทั้งหมด)
        GridBagLayout   = ตัวจัดวางที่ถ้าใส่ของชิ้นเดียว จะวางไว้ "ตรงกลาง" ตามขนาดของมัน ไม่ยืด
        BoxLayout(..., Y_AXIS) = เรียงของจากบนลงล่างทีละชิ้น ตามลำดับที่ add
        setContentPane  = เอา JPanel มาเป็นเนื้อหาทั้งหมดของหน้าต่าง
 */

package gui.admin;

import gui.login.LoginFrame;  // หน้า Login ตัวใหม่ (โฟลเดอร์ gui/login) กด Logout แล้วกลับไปหน้านี้ อยู่คนละ package เลยต้อง import
import java.awt.*;            // BorderLayout = แบ่งบน/กลาง, GridBagLayout = วางการ์ดไว้กลางจอ, FlowLayout = วางปุ่มเพิ่ม/ลบหนังเรียงกันตรงกลาง, Dimension = ขนาดช่องว่าง, Color / Cursor / Component = สี เมาส์รูปมือ และการจัดกลาง
import java.io.IOException;   // error ตอนอ่าน/เขียน movies.csv ไม่ได้ ตอนลบหนัง (เอาไปโชว์ในกล่อง Error)
import java.util.List;        // รายชื่อหนังทั้งหมดที่ใส่ในช่องเลือกตอนลบ (เขียนชัด ๆ เพราะ java.awt.* ก็มี List คนละตัว)
import javax.swing.*;         // JFrame = หน้าต่าง, JPanel = กล่อง, JOptionPane = กล่องถามยืนยัน / แจ้งผล, Box/BoxLayout = เรียงการ์ดบนลงล่าง, JButton / BorderFactory = ปุ่มและกรอบ, JComboBox / JLabel = ช่องเลือกหนังที่จะลบ
import model.Movie;           // หนัง 1 เรื่อง ในช่องเลือกตอนลบ และหนังที่ถูกลบ (เอาชื่อไปบอก admin)
import model.User;            // admin ที่ login อยู่
import service.AdminService;  // ใช้ค่าคงที่ MIN_MOVIES (10) เขียนกฎใต้ช่องเลือกตอนลบ ให้ตรงกับกฎจริง
import service.AppServices;   // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม

public final class AdminFrame extends JFrame {

    private final AppServices services;   // เก็บไว้ส่งกลับให้ LoginFrame ตอน Logout (นาฬิกาและไฟล์ชุดเดิม) และส่งให้ LineupPanel ตัวใหม่
    private final User admin;             // คนที่ login อยู่ ใช้ตอนสร้าง LineupPanel ตัวใหม่ใน ⑥ showLineup()
    private final JPanel lineupBox;       // กล่องที่ใส่ LineupPanel ไว้ 1 ตัว เวลาเปลี่ยน → เอาตัวเก่าออก ใส่ตัวใหม่ในกล่องนี้

    /**
     * เปิดหน้า admin
     * public เพราะ LoginFrame (อยู่ package gui.login ไม่ใช่ gui.admin) ต้อง new หน้านี้
     *
     * @param admin    คนที่ login แล้ว ห้าม null และต้องเป็น ADMIN
     * @param services ของหลังบ้านชุดเดียวกับทั้งโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า admin หรือ services เป็น null หรือ admin ไม่ใช่ ADMIN
     */
    public AdminFrame(User admin, AppServices services) {
        // ตรวจของที่ส่งเข้ามาก่อน ผิดตั้งแต่ต้น → หยุดเลย ไม่เปิดหน้าครึ่ง ๆ (SC2 fail-fast)
        if (admin == null) {
            throw new IllegalArgumentException("admin must not be null");
        }
        if (!admin.isAdmin()) {   // ลูกค้าธรรมดาเข้าหน้านี้ไม่ได้ (ถ้าเกิด แปลว่าโค้ดที่เรียกเปิดผิดหน้า)
            throw new IllegalArgumentException("user is not admin: " + admin.username());
        }
        if (services == null) {
            throw new IllegalArgumentException("services must not be null");
        }
        this.services = services;
        this.admin = admin;

        // ① ตั้งค่าหน้าต่าง
        setTitle("KU CINEMA - Admin");                     // ข้อความบนแถบชื่อหน้าต่าง
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);    // กดกากบาทปิดหน้าต่าง = ปิดโปรแกรม
        setSize(920, 730);                                 // กว้าง 920 สูง 730 px เท่ากับ MainFrame
        setLocationRelativeTo(null);                       // null = เปิดกลางจอ
        setResizable(false);                               // ห้ามลากยืดหน้าต่าง หน้าตาจะได้ไม่เพี้ยน

        // ② พื้นที่กลาง: พื้นเทา + คอลัมน์การ์ด 2 ใบอยู่ตรงกลาง
        //    lineupBox = กล่องใส่ LineupPanel (BorderLayout ใส่ชิ้นเดียว → ชิ้นนั้นเต็มกล่องพอดี)
        //    setOpaque(false) = โปร่งใส ไม่ทาสีพื้นของกล่อง ให้เห็นพื้นเทาข้างหลัง
        lineupBox = new JPanel(new BorderLayout());
        lineupBox.setOpaque(false);
        showLineup();   // ⑥ ใส่ LineupPanel ตัวแรกลงกล่อง (ใช้เมธอดเดียวกับตอนเวลาเปลี่ยน ไม่เขียนซ้ำ)

        //    column = เรียงบนลงล่าง: lineupBox (560 x 420) → ช่องว่าง 16 px → ClockPanel (560 x 130)
        //    Box.createRigidArea(new Dimension(0, 16)) = ก้อนว่าง กว้าง 0 สูง 16 px
        //    new ClockPanel(นาฬิกา, สิ่งที่ต้องทำหลังเวลาเปลี่ยน)
        //      services.clock()  = นาฬิกาตัวเดียวของทั้งโปรแกรม (AppServices.java บรรทัด 231)
        //      this::showLineup  = ส่งเมธอด showLineup (⑥) ให้ ClockPanel เก็บไว้ กด Set / Reset สำเร็จ → ClockPanel เรียก onChange.run() → showLineup() ทำงาน
        //                          ใช้ :: เพราะ showLineup ไม่รับพารามิเตอร์ (เหมือน this::handleLogout ข้างล่าง)
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.add(lineupBox);
        column.add(Box.createRigidArea(new Dimension(0, 16)));
        column.add(new ClockPanel(services.clock(), this::showLineup));
        column.add(Box.createRigidArea(new Dimension(0, 12)));   // ช่องว่าง 12 px ระหว่างการ์ดตั้งเวลากับปุ่ม
        column.add(makeMovieButtons());                          // ⑦ ⑧ ปุ่ม + Add New Movie / - Delete Movie (แถวเดียว ความสูงหน้าต่างเท่าเดิม)

        //    GridBagLayout ใส่ของชิ้นเดียว (column) → วางไว้กลางพื้นที่ ตามขนาดของมัน (560 x 566) ไม่ยืดเต็ม
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(AdminTheme.BACKGROUND);
        center.add(column);

        // ③ แบ่งหน้าต่างเป็น บน (แถบบน) / กลาง (พื้นที่กลาง ②)
        //    new AdminTopBar(ชื่อ, คำสั่งตอนกด Logout)
        //      admin.username()   = ชื่อที่โชว์ "Admin: admin"
        //      this::handleLogout = ส่งเมธอด handleLogout (⑤) ของหน้านี้ให้แถบบนเก็บไว้ เรียกตอนกดปุ่ม Logout
        JPanel root = new JPanel(new BorderLayout());
        root.add(new AdminTopBar(admin.username(), this::handleLogout), BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        // ④ ใช้ root เป็นเนื้อหาทั้งหมดของหน้าต่าง
        setContentPane(root);
    }

    /**
     * ⑤ ปุ่ม Logout: ถามยืนยันก่อน แล้วกลับหน้า Login (แบบเดียวกับ MainFrame)
     * ส่ง services ตัวเดิมกลับไป นาฬิกาและไฟล์จะได้เป็นชุดเดียวกัน ไม่สร้างใหม่
     */
    private void handleLogout() {
        // showConfirmDialog(หน้าที่เด้งทับ, ข้อความ, หัวกล่อง, ปุ่ม Yes/No, ไอคอน ?) → คืนค่าปุ่มที่กด
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Confirm Logout",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {          // กด Yes (กด No หรือปิดกล่อง = อยู่หน้าเดิม)
            dispose();                                     // ปิดหน้า admin
            new LoginFrame(services).setVisible(true);     // เปิดหน้า Login ด้วย services ตัวเดิม
        }
    }

    /**
     * ⑦ ⑧ แถวปุ่ม "+ Add New Movie" (ฟ้า) กับ "- Delete Movie" (แดง) อยู่กลางคอลัมน์
     * วางแถวเดียวกัน หน้าต่างจะได้สูงเท่าเดิม (ถ้าซ้อนเป็น 2 แถว ของจะล้นหน้าต่าง 730 px)
     *
     * @return JPanel แถวที่มีปุ่มเพิ่มหนังและปุ่มลบหนัง
     */
    private JPanel makeMovieButtons() {
        // FlowLayout(CENTER, 12, 0) = เรียงซ้ายไปขวา จัดกลาง ห่างกัน 12 px ไม่มีช่องว่างบนล่าง
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);                                   // โปร่งใส เห็นพื้นเทาข้างหลัง
        row.add(makeOutlineButton("+ Add New Movie", AdminTheme.PRIMARY, this::openAddMovie));
        row.add(makeOutlineButton("- Delete Movie", AdminTheme.LOGOUT_TEXT, this::openDeleteMovie)); // แดงเดียวกับ Logout = ปุ่มที่ต้องระวัง
        // setAlignmentX(CENTER) = อยู่กลางคอลัมน์ (ใน BoxLayout ทุกชิ้นต้องตั้งเท่ากัน)
        // setMaximumSize(ขนาดพอดี) = กัน BoxLayout ยืดแถวนี้ให้สูงเกินปุ่ม
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(row.getPreferredSize());
        return row;
    }

    /**
     * ปุ่มพื้นขาว ตัวหนังสือ/กรอบสีที่กำหนด (ใช้ทั้งปุ่มเพิ่มหนังและปุ่มลบหนัง หน้าตาจะได้เหมือนกัน)
     *
     * @param text   ข้อความบนปุ่ม เช่น "+ Add New Movie"
     * @param color  สีตัวหนังสือและกรอบ เช่น AdminTheme.PRIMARY
     * @param action สิ่งที่ทำตอนกดปุ่ม เช่น this::openAddMovie
     * @return JButton ปุ่มที่ผูกกับ action แล้ว
     */
    private JButton makeOutlineButton(String text, Color color, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(AdminTheme.FONT_BUTTON);
        button.setForeground(color);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);                          // กดแล้วไม่วาดกรอบประรอบคำ
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));       // ชี้แล้วเป็นรูปมือ
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),                   // กรอบสีมุมโค้ง
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));                  // ที่ว่างระหว่างกรอบกับคำ
        button.addActionListener(e -> action.run());            // กดปุ่ม → ทำ action
        return button;
    }

    /**
     * ⑦ เปิดกล่องเพิ่มหนังใหม่ (AddMovieDialog) เพิ่มสำเร็จแล้วกล่องจะเรียก showLineup (⑥)
     * ช่อง Movie 1–5 จะได้มีหนังเรื่องใหม่ให้เลือกทันที
     */
    private void openAddMovie() {
        // new AddMovieDialog(หน้าต่างแม่, admin ที่ login อยู่, ตัวเพิ่มหนัง, สิ่งที่ทำหลังเพิ่มสำเร็จ)
        //   this              = หน้า admin นี้ กล่องจะเด้งกลางหน้านี้
        //   admin             = คนที่ login อยู่ ให้ AdminService เช็กว่าเป็น admin
        //   services.admin()  = AdminService ตัวเดียวของโปรแกรม (AppServices.java) มีเมธอด addMovie
        //   this::showLineup  = เพิ่มสำเร็จ → ใส่ LineupPanel ตัวใหม่ (อ่านหนังใหม่จาก movies.csv)
        new AddMovieDialog(this, admin, services.admin(), this::showLineup).setVisible(true);
    }

    /**
     * ⑧ ปุ่ม - Delete Movie: ให้เลือกหนัง → ถามยืนยัน → ลบ
     * กฎว่าลบได้ไหม (เป็น admin, ต้องเหลืออย่างน้อย 10 เรื่อง, ไม่อยู่ใน lineup) อยู่ใน AdminService.deleteMovie ที่เดียว
     * หน้านี้แค่เอาข้อความที่ AdminService throw มาโชว์ จะได้ไม่เขียนกฎซ้ำ 2 ที่
     * เช่น เลือก M15 → ยืนยัน → "Movie deleted: M15 คู่แรด" → ช่องเลือก Movie 1–5 ไม่มี M15 แล้ว
     *      เลือก M3  → ยืนยัน → "Cannot delete M3: it is in the lineup on 2026-10-01." ไม่ลบอะไร
     */
    private void openDeleteMovie() {
        Movie chosen = chooseMovieToDelete();
        if (chosen == null) {
            return;   // กด Cancel / ปิดกล่อง / อ่านรายชื่อหนังไม่ได้ → ไม่ทำอะไร
        }
        // ถามยืนยันอีกครั้ง เพราะลบแล้วย้อนกลับไม่ได้
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + chosen.id() + " " + chosen.title() + "?\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            Movie deleted = services.admin().deleteMovie(admin, chosen.id());
            JOptionPane.showMessageDialog(this, "Movie deleted: " + deleted.id() + " " + deleted.title(),
                    "Deleted", JOptionPane.INFORMATION_MESSAGE);   // หัวกล่องแบบเดียวกับ "Added" ตอนเพิ่มหนัง
            showLineup();   // ⑥ ช่องเลือกหนังอ่าน movies.csv ใหม่ เรื่องที่ลบหายไป
        } catch (IllegalArgumentException e) {
            // ผิดกฎ → โชว์เหตุผลจาก AdminService (movies.csv ไม่ถูกแตะ)
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot delete movie", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot delete movie: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * กล่องให้ admin เลือกหนังที่จะลบจากหนังทั้งหมด (ช่องเลือกหน้าตาเดียวกับ LineupPanel ใช้ MovieRenderer)
     * ใต้ช่องเลือกบอกกฎไว้ admin จะได้รู้ก่อนกดว่าเรื่องไหนลบไม่ได้
     *
     * @return Movie เรื่องที่เลือกแล้วกด OK / กด Cancel ปิดกล่อง หรืออ่านรายชื่อหนังไม่ได้ → null
     */
    private Movie chooseMovieToDelete() {
        List<Movie> movies;
        try {
            movies = services.movies().movies();   // หนังทั้งหมดใน movies.csv
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot load movies: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        // movies.toArray(new Movie[0]) = แปลงเป็น array เพราะ JComboBox รับ array
        JComboBox<Movie> box = new JComboBox<>(movies.toArray(new Movie[0]));
        box.setFont(AdminTheme.FONT_TEXT);
        box.setRenderer(new MovieRenderer());     // โชว์ "Inception  (148 min)" แบบเดียวกับช่องเลือกใน LineupPanel

        JLabel rule = new JLabel("<html>At least " + AdminService.MIN_MOVIES + " movies must remain (now "
                + movies.size() + ").<br>Movies used in any lineup cannot be deleted.</html>");
        rule.setFont(AdminTheme.FONT_SMALL);
        rule.setForeground(AdminTheme.TEXT_MUTED);

        // panel = เรียงบนลงล่าง: ป้าย → ช่องเลือก → กฎ (BorderLayout ห่างแนวตั้ง 8 px)
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Choose a movie to delete:"), BorderLayout.NORTH);
        panel.add(box, BorderLayout.CENTER);
        panel.add(rule, BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(this, panel, "Delete Movie",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }
        return (Movie) box.getSelectedItem();     // ช่องเลือกเก็บ Movie ตัวจริง (ไม่มีหนังเลย → null)
    }

    /**
     * ⑥ ใส่ LineupPanel ตัวใหม่ลง lineupBox
     * ใช้ 4 ที่: ตอนเปิดหน้า (②), ตอน ClockPanel บอกว่าเวลาเปลี่ยนแล้ว, ตอนเพิ่มหนังใหม่สำเร็จ (⑦) และตอนลบหนังสำเร็จ (⑧) (ส่งไปเป็น this::showLineup)
     * ทำไมต้องสร้างใหม่: LineupPanel เขียน "Showing on 15/10/2026" ครั้งเดียวตอนสร้าง
     *   ไม่สร้างใหม่ → ตั้งเวลาเป็น 20/10 แล้วจอยังบอก 15/10 แต่กด Save จะบันทึกของ 21/10 (จอหลอก admin)
     *   สร้างใหม่    → ตัวใหม่ถาม tomorrow() ใหม่ → Showing on 21/10/2026 ตรงกับที่ Save จะบันทึก
     *   ถ้า admin ไม่จัดหนังของพรุ่งนี้ ก็ไม่ต้องกด Save หนังจะฉายชุดเดิมต่อไป
     */
    private void showLineup() {
        lineupBox.removeAll();                               // เอา LineupPanel ตัวเก่าออก ไม่งั้นมี 2 ตัวซ้อนกัน
        lineupBox.add(new LineupPanel(admin, services));     // ใส่ตัวใหม่ (ตอนสร้างจะอ่าน tomorrow() ตามเวลาตอนนี้)
        lineupBox.revalidate();                              // บอก Swing ว่าของข้างในเปลี่ยน ให้จัดตำแหน่งใหม่
        lineupBox.repaint();                                 // วาดใหม่บนจอ
    }
}