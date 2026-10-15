//MovieRow <- แถวโปสเตอร์หนังของวันนี้ในหน้าลูกค้า กดโปสเตอร์ไหน หนังเรื่องนั้นถูกเลือก
/*
    หน้าตา (หนัง 5 เรื่อง):
        [โปสเตอร์1] [โปสเตอร์2] [โปสเตอร์3] [โปสเตอร์4] [โปสเตอร์5]
        Spider-Man   Pacific Rim  Ghost Rider  Jurassic..   Blade Runner
         145 min      131 min      110 min      92 min       164 min
        เรื่องที่เลือกอยู่ = โปสเตอร์มีกรอบฟ้า ชื่อตัวหนาสีฟ้า / ตอนเปิดหน้าเลือกเรื่องแรกไว้ให้

    ขนาดโปสเตอร์ปรับเองตามจำนวนหนัง (posterWidth ข้างล่าง):
        5 เรื่อง → กว้าง 141 px / 8 เรื่อง → 80 px / 10 เรื่อง → 59 px (ทั้งแถวกว้างไม่เกิน 800 px)
        รองรับได้ถึง 10 เรื่อง เกินกว่านั้นโปสเตอร์เล็กจนดูไม่ออก ต้องเปลี่ยนเป็น 2 แถวหรือแถบเลื่อน
        (ตอนนี้ admin ตั้งได้ทีละ 5 เรื่องเสมอ AdminService.LINEUP_SIZE)

    รูปโปสเตอร์: โฟลเดอร์ data (app.dataFolder()) + posterPath ของหนัง เช่น data/posters/m1.jpg
        หาไม่เจอ / อ่านไม่ได้ → การ์ดสีแทน มีคำว่า Poster และตัวอักษร A, B, C ... (สีจาก MainTheme.POSTER_COLORS)

    แถวนี้แค่บอกว่า "เปลี่ยนเรื่องแล้ว" ผ่าน onChange ที่ส่งเข้ามา
    ส่วนการโหลดรอบฉายของเรื่องใหม่เป็นงานของ MainFrame แถวนี้จะได้ไม่ต้องรู้จัก MovieService

    ใช้ยังไง (ใน MainFrame):
        MovieRow movieRow = new MovieRow(app.movies().moviesToday(), app.dataFolder(), () -> refreshShowtimes());
        movieRow.selected()   // หนังที่เลือกอยู่ เช่น Movie[id=M1, title=Spider-Man: Brand New Day, ...] / ไม่มีหนัง → null

    ย้ายมาจาก gui/MainFrame.java เดิม บรรทัด 911–1006 (การ์ดหนัง), 1133–1212 (PosterBox), 1214–1224 (หาไฟล์รูป)
    ต่างจากเดิม:
        ตัดปุ่ม < > และจุดบอกตำแหน่งออก เพราะโชว์ทุกเรื่องในแถวเดียวแล้ว
        เดิมหารูปจากโฟลเดอร์ที่เปิดโปรแกรม → ใหม่หาจาก app.dataFolder() โฟลเดอร์เดียวกับไฟล์ CSV
        เดิม catch (Exception ignored) เงียบ ๆ → ใหม่จับแค่ IOException ตอนอ่านรูปไม่ได้ แล้วใช้การ์ดสีแทน (SC2 หน้า 11)

    คำที่ใช้บ่อยในไฟล์นี้:
        BufferedImage = รูป 1 รูปในหน่วยความจำ วาดลงไปได้ (Graphics2D) และเอาไปทำเป็น ImageIcon ใส่ปุ่มได้
        ImageIcon     = รูปที่ติดบนปุ่ม / ป้ายได้ (button.setIcon)
        GridLayout(1, n, ห่าง, 0) = แบ่งแถวเป็น n ช่องเท่ากัน 1 ช่อง = หนัง 1 เรื่อง
 */

package gui.main;

import java.awt.*;                         // Color, Font, Graphics2D = ปากกาวาดรูป, GridLayout, Dimension, Cursor, RenderingHints = วาดให้เนียน
import java.awt.geom.RoundRectangle2D;     // สี่เหลี่ยมมุมโค้ง ใช้ตัดขอบรูปโปสเตอร์ให้มุมมน
import java.awt.image.BufferedImage;       // รูปในหน่วยความจำ (รูปโปสเตอร์ที่อ่านจากไฟล์ / การ์ดสีที่วาดเอง)
import java.io.IOException;                // error ตอนอ่านไฟล์รูปไม่ได้
import java.nio.file.Files;                // เช็กว่ามีไฟล์รูปอยู่จริงไหม
import java.nio.file.Path;                 // ที่อยู่ไฟล์ เช่น data/posters/m1.jpg
import java.util.ArrayList;                // รายการรูปโปสเตอร์ (มี null ได้ = เรื่องที่ไม่มีรูป)
import java.util.List;                     // รายการหนัง (เขียนเต็ม เพราะ java.awt.* ก็มี List ชื่อซ้ำ)
import javax.imageio.ImageIO;              // อ่านไฟล์ .jpg / .png เป็น BufferedImage
import javax.swing.*;                      // JPanel, JButton = โปสเตอร์ที่กดได้, JLabel = ชื่อ/ความยาวหนัง, ImageIcon, Box, BoxLayout
import javax.swing.border.*;               // LineBorder = กรอบฟ้าของเรื่องที่เลือก, EmptyBorder = ที่ว่างแทนกรอบ
import model.Movie;                        // หนัง 1 เรื่อง: id, title, durationMinutes, posterPath

final class MovieRow extends JPanel {

    private static final int ROW_WIDTH = 800;          // ความกว้างที่แถวนี้ใช้ได้ (การ์ดกลางของหน้า 920 หักขอบแล้ว)
    private static final int GAP = 16;                  // ระยะห่างระหว่างโปสเตอร์ px
    private static final int MAX_POSTER_WIDTH = 150;    // โปสเตอร์กว้างสุด 150 px (เท่าของเดิม) หนัง 1–4 เรื่องก็ไม่ใหญ่เกินนี้
    private static final int FRAME = 3;                 // ความหนากรอบฟ้ารอบโปสเตอร์ที่เลือก px

    // Abstraction Function (SC4 หน้า 10):
    //   AF(movies, selected) = แถวโปสเตอร์ 1 ใบต่อหนัง 1 เรื่องใน movies เรียงตามลำดับ
    //                          selected = เรื่องที่ลูกค้าเลือกอยู่ (null = วันนี้ไม่มีหนัง)
    // Rep Invariant:
    //   movies ไม่เป็น null และไม่มี null, posters / onChange ไม่เป็น null
    //   posters.size() == movies.size() (รูปที่ i คือรูปของหนังเรื่องที่ i, null = ไม่มีรูป)
    //   movies ว่าง ⇔ selected เป็น null / ไม่ว่าง → selected เป็นเรื่องหนึ่งใน movies
    // Safety from rep exposure (SC4 หน้า 14):
    //   movies เก็บสำเนาแบบแก้ไม่ได้ (List.copyOf) / Movie เป็น record แก้ค่าไม่ได้ คืน selected ตัวจริงได้
    //   posters อยู่ข้างในอย่างเดียว ไม่มีเมธอดไหนคืนออกไป
    private final List<Movie> movies;                  // หนังวันนี้ เช่น [M1, M2, M3, M4, M5]
    private final List<BufferedImage> posters;         // รูปโปสเตอร์ของแต่ละเรื่อง อ่านครั้งเดียวตอนสร้าง (null = ไม่มีรูป)
    private final Runnable onChange;                   // สิ่งที่ต้องทำหลังเปลี่ยนเรื่อง (MainFrame ส่งมา) เช่น () -> refreshShowtimes()
    private Movie selected;                            // เรื่องที่เลือกอยู่ เปลี่ยนเมื่อกดโปสเตอร์

    /**
     * สร้างแถวโปสเตอร์ เริ่มต้นเลือกเรื่องแรก
     *
     * @param movies     หนังวันนี้ เช่น app.movies().moviesToday() → [M1, M2, M3, M4, M5] (ว่างได้ = วันนี้ไม่มีหนัง)
     * @param dataFolder โฟลเดอร์ data ที่มีรูปโปสเตอร์ เช่น app.dataFolder() → data
     * @param onChange   คำสั่ง run method นั้น
     *                   สิ่งที่ต้องทำหลังกดเปลี่ยนเรื่อง (MainFrame ส่งมา) เช่น () -> refreshShowtimes()
     *                   Runnable = "คำสั่ง 1 ชิ้น" ที่ส่งมาเก็บไว้ก่อน ยังไม่ทำ จะทำตอนสั่ง onChange.run()
     * @throws IllegalArgumentException ถ้า movies เป็น null / มี null ข้างใน หรือ dataFolder / onChange เป็น null
     */
    MovieRow(List<Movie> movies, Path dataFolder, Runnable onChange) {
        // ① ตรวจของที่ส่งเข้ามา ผิดแล้ว throw เลย (SC2 หน้า 9 Defensive Programming)
        if (movies == null) {
            throw new IllegalArgumentException("movies must not be null");
        }
        for (Movie movie : movies) {     // ไล่ดูทีละเรื่อง มี null ปน → ทำโปสเตอร์ไม่ได้
            if (movie == null) {         // ไม่ใช้ movies.contains(null) เพราะลิสต์จาก List.of(...) ถามหา null แล้วพัง
                throw new IllegalArgumentException("movies must not contain null");
            }
        }
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder must not be null");
        }
        if (onChange == null) {
            throw new IllegalArgumentException("onChange must not be null");
        }

        // ② เก็บของ + อ่านรูปโปสเตอร์ทุกเรื่องครั้งเดียว (กดเปลี่ยนเรื่องทีหลังไม่ต้องอ่านไฟล์ซ้ำ)
        //    List.copyOf(movies) = สำเนาแบบแก้ไม่ได้ ลิสต์ตัวนอกถูกแก้ทีหลังก็ไม่กระทบ (SC4 หน้า 14)
        this.movies = List.copyOf(movies);
        this.onChange = onChange;
        this.posters = new ArrayList<>();
        for (Movie movie : this.movies) {
            posters.add(readPoster(dataFolder, movie));   // ได้รูป หรือ null ถ้าไม่มีรูป
        }
        //    เริ่มต้นเลือกเรื่องแรก / ไม่มีหนัง → null
        this.selected = this.movies.isEmpty() ? null : this.movies.get(0);

        // ③ ตั้งค่าตัวแถว
        setOpaque(false);                            // โปร่งใส ให้เห็นพื้นขาวของการ์ด
        setAlignmentX(Component.CENTER_ALIGNMENT);   // อยู่กลางการ์ด (การ์ดเรียงของแนวตั้งด้วย BoxLayout)

        // ④ วาดโปสเตอร์ครั้งแรก
        drawRow();
        checkRep();
    }

    /**
     * หนังที่ลูกค้าเลือกอยู่ตอนนี้
     *
     * @return เช่น Movie[id=M1, title=Spider-Man: Brand New Day, ...] / วันนี้ไม่มีหนัง → null
     */
    Movie selected() {
        return selected;
    }

    /**
     * อ่านรูปโปสเตอร์ของหนัง 1 เรื่อง
     *   คืนรูป หรือ null ถ้าไม่มีไฟล์ / อ่านไม่ได้ (ไปวาดการ์ดสีแทน)
     *
     * @param dataFolder โฟลเดอร์ data เช่น Booking_tickets/data
     * @param movie หนัง 1 เรื่อง (ใช้ movie.posterPath() เช่น posters/m1.jpg)
     * @return BufferedImage รูปโปสเตอร์ที่อ่านได้ / ไม่มีไฟล์หรืออ่านไม่ได้ → null
     */
    private static BufferedImage readPoster(Path dataFolder, Movie movie) {
        if (movie.posterPath().isBlank()) {                    // ไม่ได้ใส่ที่อยู่รูปไว้
            return null;
        }
        Path file = dataFolder.resolve(movie.posterPath());    // resolve = ต่อที่อยู่ เช่น data + posters/m1.jpg → data/posters/m1.jpg
        if (!Files.isRegularFile(file)) {                      // ไม่มีไฟล์นี้ (เช่นหนังเรื่องที่ 6–15 ยังไม่มีรูป)
            return null;
        }
        try {
            return ImageIO.read(file.toFile());                // อ่านรูป / ไฟล์ไม่ใช่รูป → ได้ null
        } catch (IOException e) {
            // จับแค่ error ตอนอ่านไฟล์ (SC2 หน้า 11) รูปเสีย 1 รูปไม่ควรทำให้ทั้งหน้าเปิดไม่ได้ → ใช้การ์ดสีแทน
            return null;
        }
    }

    /**
     * ความกว้างโปสเตอร์ตามจำนวนหนัง n เรื่อง
     *   แบ่ง ROW_WIDTH ให้ n ช่อง หักระยะห่างระหว่างช่อง (n - 1 ช่องว่าง) และกรอบซ้ายขวา (2 × FRAME) แล้วไม่ให้เกิน 150
     *   เช่น 5 เรื่อง: (800 - 4×16) / 5 - 6 = 141 / 10 เรื่อง: (800 - 9×16) / 10 - 6 = 59
     *   ทั้งแถวจึงกว้างไม่เกิน 800 px เสมอ
     *   ไม่รับพารามิเตอร์ ใช้จำนวนหนังใน movies ของแถวนี้
     *
     * @return int ความกว้างโปสเตอร์เป็น px เช่น 5 เรื่อง → 141 / 10 เรื่อง → 59 (ไม่เกิน MAX_POSTER_WIDTH 150)
     */
    private int posterWidth() {
        int n = movies.size();
        return Math.min(MAX_POSTER_WIDTH, (ROW_WIDTH - (n - 1) * GAP) / n - 2 * FRAME);
    }

    /**
     * วาดแถวใหม่ทั้งแถว ใช้ตอนเริ่มและทุกครั้งที่เปลี่ยนเรื่อง เพื่อให้กรอบฟ้าอยู่ที่เรื่องที่เลือก
     */
    private void drawRow() {
        removeAll();                                       // ลบของเก่าทิ้งก่อน ไม่งั้นซ้อนเพิ่มทุกครั้งที่กด
        if (movies.isEmpty()) {                            // วันนี้ไม่มีหนัง → ข้อความแทน
            setLayout(new FlowLayout(FlowLayout.CENTER));
            JLabel empty = new JLabel("No movies today");
            empty.setFont(MainTheme.FONT_TEXT);
            empty.setForeground(MainTheme.TEXT_MUTED);
            add(empty);
        } else {
            // GridLayout(1, n, GAP, 0) = 1 แถว n ช่องเท่ากัน ห่างกันแนวนอน GAP px
            setLayout(new GridLayout(1, movies.size(), GAP, 0));
            for (int i = 0; i < movies.size(); i++) {      // i = ลำดับเรื่อง 0, 1, 2, ...
                add(makeCard(i));
            }
        }
        revalidate();                                      // ของข้างในเปลี่ยน → ให้ Swing จัดขนาดและตำแหน่งใหม่
        repaint();                                         // วาดใหม่บนจอ
    }

    /**
     * การ์ดของหนัง 1 เรื่อง = โปสเตอร์ (กดได้) + ชื่อ + ความยาว เรียงบนลงล่าง
     *
     * @param i ลำดับหนังใน movies เริ่ม 0 เช่น 0 = เรื่องแรก (Spider-Man)
     * @return JPanel การ์ด 1 ใบ = ปุ่มโปสเตอร์ + ชื่อหนัง + "145 min"
     */
    private JPanel makeCard(int i) {
        Movie movie = movies.get(i);
        boolean isSelected = movie.equals(selected);       // เรื่องนี้คือเรื่องที่เลือกอยู่ไหม
        int width = posterWidth();
        int height = width * 4 / 3;                        // สัดส่วนโปสเตอร์ 3:4 เช่น 141 → 188 (ของเดิม 150 → 200)

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));   // เรียงบนลงล่าง: โปสเตอร์ → ชื่อ → ความยาว
        card.setOpaque(false);

        // โปสเตอร์ = ปุ่มที่มีแต่รูป
        JButton poster = new JButton(new ImageIcon(posterImage(i, width, height)));
        poster.setContentAreaFilled(false);                // ไม่ทาพื้นปุ่ม เห็นแค่รูป
        poster.setFocusPainted(false);                     // กดแล้วไม่วาดกรอบประ
        poster.setCursor(new Cursor(Cursor.HAND_CURSOR));  // ชี้เมาส์แล้วเป็นรูปมือ บอกว่ากดได้
        poster.setAlignmentX(Component.CENTER_ALIGNMENT);
        //    เลือกอยู่ → กรอบฟ้าหนา FRAME px มุมโค้ง / ไม่ได้เลือก → ที่ว่างหนาเท่ากัน (โปสเตอร์ทุกใบขนาดเท่ากัน ไม่กระโดด)
        poster.setBorder(isSelected
                ? new LineBorder(MainTheme.PRIMARY, FRAME, true)
                : new EmptyBorder(FRAME, FRAME, FRAME, FRAME));
        //    กดโปสเตอร์ → pick(movie) / แต่ละปุ่มจำหนังของตัวเองไว้
        poster.addActionListener(e -> pick(movie));
        card.add(poster);
        card.add(Box.createRigidArea(new Dimension(0, 6)));   // ที่ว่าง 6 px ใต้โปสเตอร์

        // ชื่อหนัง: <html> ทำให้ชื่อยาวขึ้นบรรทัดใหม่ได้ จัดกลาง
        //   <table width=กว้างโปสเตอร์> = บังคับให้ตัดบรรทัดที่ความกว้างโปสเตอร์พอดี
        //   (ลองแบบ <div style='width:..px'> แล้ว ชื่อไม่ตัดบรรทัด ถูกตัดหาย เช่น "Spider-Man: Brand New")
        JLabel title = new JLabel("<html><table width=" + width + " cellpadding=0 cellspacing=0><tr><td align=center>"
                + movie.title() + "</td></tr></table></html>", SwingConstants.CENTER);
        title.setFont(isSelected ? MainTheme.FONT_SMALL_BOLD : MainTheme.FONT_SMALL);    // เลือกอยู่ = ตัวหนา
        title.setForeground(isSelected ? MainTheme.PRIMARY : MainTheme.TEXT_TITLE);      // เลือกอยู่ = สีฟ้า
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        //    ล็อกขนาดป้ายชื่อ: กว้างเท่าโปสเตอร์ (รวมกรอบ) สูงพอ 3 บรรทัด
        //    ไม่ล็อก → ชื่อยาวอย่าง "Spider-Man: Brand New Day" ทำให้การ์ดกว้างกว่าโปสเตอร์ ทั้งแถวล้นเกิน 800 px (เทสต์เจอ)
        Dimension titleSize = new Dimension(width + 2 * FRAME, 42);
        title.setPreferredSize(titleSize);
        title.setMaximumSize(titleSize);
        card.add(title);

        // ความยาวหนัง เช่น "145 min" ตัวเล็กสีเทา
        JLabel duration = new JLabel(movie.durationMinutes() + " min", SwingConstants.CENTER);
        duration.setFont(MainTheme.FONT_SMALL);
        duration.setForeground(MainTheme.TEXT_MUTED);
        duration.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(duration);
        return card;
    }

    /**
     * วาดรูปโปสเตอร์ขนาด width x height มุมโค้ง: มีรูป → ย่อ/ขยายรูปจริง / ไม่มีรูป → การ์ดสี + Poster + ตัวอักษร
     *
     * @param i ลำดับหนังเริ่ม 0
     * @param width ความกว้างรูป px เช่น 141
     * @param height ความสูงรูป px เช่น 188
     * @return BufferedImage รูปขนาด width x height มุมโค้ง (มีรูปจริง → ย่อรูป / ไม่มี → การ์ดสี + ตัวอักษร)
     */
    private BufferedImage posterImage(int i, int width, int height) {
        // TYPE_INT_ARGB = รูปที่มีพื้นโปร่งใสได้ (มุมที่ตัดโค้งออกจะใส ไม่เป็นสี่เหลี่ยมดำ)
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();                // ปากกาสำหรับวาดลงรูปนี้
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);          // ขอบโค้งเนียน
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // ตัวหนังสือเนียน
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // ย่อรูปแล้วไม่แตก
        //    setClip = วาดได้แค่ในสี่เหลี่ยมมุมโค้งนี้ (โค้ง 10 px) ส่วนเกินที่มุมถูกตัดทิ้ง
        g.setClip(new RoundRectangle2D.Float(0, 0, width, height, 10, 10));

        BufferedImage poster = posters.get(i);
        if (poster != null) {
            g.drawImage(poster, 0, 0, width, height, null);   // ย่อ/ขยายรูปจริงให้เต็มกรอบ
        } else {
            // การ์ดสีสำรอง: เรื่องที่ 1 สีแรก เรื่องที่ 6 วนกลับสีแรก (% = เศษจากการหาร)
            g.setColor(MainTheme.POSTER_COLORS.get(i % MainTheme.POSTER_COLORS.size()));
            g.fillRect(0, 0, width, height);
            g.setColor(MainTheme.BUTTON_TEXT);                // ตัวหนังสือดำ แบบเดียวกับของเดิม
            drawCentered(g, "Poster", MainTheme.FONT_POSTER_LABEL, width, height / 2 - 10);
            //    ตัวอักษร A, B, C ... ตามลำดับเรื่อง ('A' + 1 = 'B') / % 26 = เกิน Z วนกลับ A
            drawCentered(g, String.valueOf((char) ('A' + i % 26)), MainTheme.FONT_POSTER_LETTER, width, height / 2 + 28);
        }
        g.dispose();                                          // ปิดปากกา คืนหน่วยความจำ
        return image;
    }

    /**
     * เขียนข้อความกึ่งกลางแนวนอน
     *
     * @param g        ปากกาที่ใช้วาดบนรูปโปสเตอร์สำรอง
     * @param text     ข้อความ เช่น "Poster" หรือ "A"
     * @param font     ฟอนต์ เช่น MainTheme.FONT_POSTER_LETTER
     * @param width    ความกว้างรูป px ใช้หาจุดกึ่งกลาง เช่น 141
     * @param baseline ความสูงของเส้นบรรทัด px (ตัวหนังสือยืนบนเส้นนี้)
     */
    private static void drawCentered(Graphics2D g, String text, Font font, int width, int baseline) {
        g.setFont(font);
        int textWidth = g.getFontMetrics().stringWidth(text);   // ข้อความนี้กว้างกี่ px ในฟอนต์นี้
        g.drawString(text, (width - textWidth) / 2, baseline);  // เริ่มเขียนที่ (กว้างรูป - กว้างข้อความ) / 2 → อยู่กลาง
    }

    /**
     * เปลี่ยนเรื่องที่เลือก แล้วบอก MainFrame
     *
     * @param movie หนังของโปสเตอร์ที่ถูกกด
     */
    private void pick(Movie movie) {
        selected = movie;    // จำเรื่องใหม่
        drawRow();           // วาดแถวใหม่ กรอบฟ้าย้ายไปเรื่องที่กด
        checkRep();          // ตรวจ RI (SC4 หน้า 12 เรียกท้ายทุกเมธอดที่แก้ rep)
        onChange.run();      // ทำคำสั่งที่ MainFrame ส่งมา เช่น refreshShowtimes() โหลดรอบฉายของเรื่องใหม่
    }

    /**
     * ตรวจ Rep Invariant ทุกข้อ ทำงานเฉพาะตอนรันด้วย java -ea (SC4 หน้า 12)
     */
    private void checkRep() {
        assert movies != null && posters != null && onChange != null : "field ต้องไม่เป็น null";
        assert posters.size() == movies.size() : "รูปโปสเตอร์ต้องมีเท่าจำนวนหนัง";
        assert movies.isEmpty() == (selected == null) : "ไม่มีหนัง ⇔ ไม่มีเรื่องที่เลือก";
        assert selected == null || movies.contains(selected) : "เรื่องที่เลือกต้องอยู่ในแถว";
    }
}