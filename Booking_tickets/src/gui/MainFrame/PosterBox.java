package gui.MainFrame;

import model.Movie;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * กล่องแสดงภาพโปสเตอร์ของภาพยนตร์ รองรับทั้งภาพจริงจาก posterPath และภาพจำลองพร้อมตัวอักษร A, B, C...
 */
public class PosterBox extends JPanel {

    private final Movie movie;
    private final int index;
    private final boolean isSelected;
    private BufferedImage image;

    /**
     * คอนสตรัคเตอร์สร้างกล่องแสดงโปสเตอร์หนัง
     * โหลดรูปภาพจาก posterPath หากพบไฟล์ ถ้าไม่พบจะเตรียมแสดงการ์ดสีมินิมอลทดแทน
     *
     * @param movie ข้อมูลภาพยนตร์
     * @param index ลำดับของภาพยนตร์ในรายการ (ใช้สำหรับสุ่มสีและการ์ดตัวอักษร)
     * @param isSelected ภาพยนตร์เรื่องนี้กำลังถูกเลือกอยู่หรือไม่ (หากใช่จะมีกรอบสีน้ำเงิน)
     */
    public PosterBox(Movie movie, int index, boolean isSelected) {
        this.movie = movie;
        this.index = index;
        this.isSelected = isSelected;
        setPreferredSize(new Dimension(150, 200));
        setMaximumSize(new Dimension(150, 200));
        setOpaque(false);

        if (movie != null && movie.posterPath() != null && !movie.posterPath().isBlank()) {
            try {
                Path p = MainFrame.resolveDataPath(movie.posterPath());
                if (Files.exists(p)) {
                    this.image = ImageIO.read(p.toFile());
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * วาดกราฟิกโปสเตอร์หนัง กรอบขอบมน และกรอบเน้นสีน้ำเงินเมื่อถูกเลือก
     * หากไม่มีรูปภาพจะวาดการ์ดสีและตัวอักษรประจำเรื่อง (A, B, C...)
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = 2, y = 2;
        int w = getWidth() - 4;
        int h = getHeight() - 4;
        int arc = 10;

        // กรอบเส้นสีน้ำเงินเมื่อถูกเลือก
        if (isSelected) {
            g2.setColor(MainFrame.COLOR_PRIMARY);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(x - 1, y - 1, w + 2, h + 2, arc + 2, arc + 2);
        }

        // ถ้ามีรูปโปสเตอร์จริง ให้วาดรูปภาพ
        if (image != null) {
            g2.setClip(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
            g2.drawImage(image, x, y, w, h, null);
        } else {
            // ถ้าไม่มีรูปภาพ ให้แสดงการ์ดสีมินิมอล A, B, C...
            Color[] softColors = {
                    new Color(71, 85, 105),  // Slate Blue
                    new Color(109, 40, 217), // Violet
                    new Color(13, 148, 136), // Teal
                    new Color(217, 119, 6),  // Amber
                    new Color(37, 99, 235)   // Blue
            };
            Color color = softColors[index % softColors.length];
            g2.setColor(color);
            g2.fillRoundRect(x, y, w, h, arc, arc);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Tahoma", Font.BOLD, 13));
            FontMetrics fm1 = g2.getFontMetrics();
            String text1 = "Poster";
            g2.drawString(text1, x + (w - fm1.stringWidth(text1)) / 2, y + (h / 2) - 10);

            char letter = (char) ('A' + (index % 26));
            g2.setFont(new Font("Tahoma", Font.BOLD, 36));
            FontMetrics fm2 = g2.getFontMetrics();
            String text2 = String.valueOf(letter);
            g2.drawString(text2, x + (w - fm2.stringWidth(text2)) / 2, y + (h / 2) + 28);
        }

        g2.dispose();
    }

    /**
     * ดึงข้อมูลภาพยนตร์ที่แสดงในกล่องนี้
     */
    public Movie getMovie() {
        return movie;
    }

    /**
     * ดึงลำดับ index ของภาพยนตร์
     */
    public int getIndex() {
        return index;
    }

    /**
     * ตรวจสอบว่าภาพยนตร์ในกล่องนี้ถูกเลือกอยู่หรือไม่
     */
    public boolean isSelected() {
        return isSelected;
    }
}
