//MovieRenderer <- "คนวาด" ข้อความและรูปโปสเตอร์ในช่องเลือกหนัง
/*
    ทำไมต้องมี:
        ช่องเลือก (JComboBox) ในหน้า admin เก็บ Movie ตัวจริงไว้ (ตอนกด Save จะได้ส่ง Movie ให้ AdminService ได้เลย)
        เวลาจะโชว์หนังแต่ละบรรทัด ช่องเลือกจะส่งหนังไปให้ "คนวาด" แล้วถามว่า "บรรทัดนี้วาดยังไง"

        คนวาดที่ Swing ให้มา (DefaultListCellRenderer) ทำ 2 อย่าง
            1. ลงสี: บรรทัดที่ถูกเลือกเป็นพื้นสีไฮไลต์ บรรทัดอื่นพื้นปกติ
            2. เขียนข้อความ: ใช้ toString() ของ record ได้
               Movie[id=M9, title=Inception, durationMinutes=148, posterPath=posters/m9.jpg]   ← อ่านยาก

        ไฟล์นี้ = คนวาดคนใหม่ ต่อยอดจากคนเดิม (extends)
            ข้อ 1 (ลงสี) ให้คนเดิมทำเหมือนเดิม
            ข้อ 2 (ข้อความ) เราเขียนทับเป็น  Inception  (148 min)
            ข้อ 3 (รูปภาพ) ใส่รูปไอคอนโปสเตอร์ขนาดเล็กย่อส่วนหน้ารายชื่อ

    ใช้ยังไง (ใน LineupPanel):
        box.setRenderer(new MovieRenderer());   // บอกช่องเลือกว่า "เปลี่ยนคนวาดเป็นคนนี้"
 */

package gui.admin;

import gui.MainFrame;
import model.Movie;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ตัวแสดงผล (Renderer) สำหรับรายการภาพยนตร์ใน Dropdown
 * แสดงรูปโปสเตอร์ขนาดเล็ก พร้อมชื่อและระยะเวลาของภาพยนตร์
 */
final class MovieRenderer extends DefaultListCellRenderer {

    // แคชไอคอนรูปโปสเตอร์ เพื่อไม่ต้องโหลดไฟล์ซ้ำ ๆ เวลาเลื่อนเมาส์
    private static final Map<String, Icon> THUMBNAIL_CACHE = new HashMap<>();

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        // 1. ให้ตัวแม่ลงสีพื้นและสีตัวหนังสือตามปกติ
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // 2. เช็กว่า value เป็น Movie จริงไหม
        if (value instanceof Movie movie) {
            // 3. แสดงชื่อหนังและความยาว
            setText(movie.title() + "  (" + movie.durationMinutes() + " min)");

            // 4. แสดงรูปโปสเตอร์ขนาดเล็ก
            Icon icon = getThumbnail(movie);
            setIcon(icon);
            setIconTextGap(8);
        } else {
            setIcon(null);
        }

        return this;
    }

    /**
     * ดึงหรือโหลดรูปโปสเตอร์ขนาดย่อ (Thumbnail) ของภาพยนตร์
     */
    private static Icon getThumbnail(Movie movie) {
        if (movie.posterPath() == null || movie.posterPath().isBlank()) {
            return null;
        }
        return THUMBNAIL_CACHE.computeIfAbsent(movie.id(), id -> {
            try {
                Path p = MainFrame.resolveDataPath(movie.posterPath());
                if (Files.exists(p)) {
                    BufferedImage original = ImageIO.read(p.toFile());
                    if (original != null) {
                        Image scaled = original.getScaledInstance(22, 30, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }
}