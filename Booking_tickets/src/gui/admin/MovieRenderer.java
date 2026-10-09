//MovieRenderer <- "คนวาด" ข้อความในช่องเลือกหนัง ทำให้โชว์ "Inception  (148 min)" แทนข้อความยาว ๆ ของ record
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

    ใช้ยังไง (ใน LineupPanel):
        box.setRenderer(new MovieRenderer());   // บอกช่องเลือกว่า "เปลี่ยนคนวาดเป็นคนนี้"
 */

package gui.admin;

import java.awt.*;      // Component = ชนิดของสิ่งที่คืนออกไป (สิ่งที่วาดเสร็จแล้ว)
import javax.swing.*;   // DefaultListCellRenderer = คนวาดที่ Swing ให้มา, JList = รายการที่เด้งลงมาตอนกดช่องเลือก
import model.Movie;     // หนัง 1 เรื่อง เอาชื่อกับความยาวมาเขียน

// extends DefaultListCellRenderer = ได้ความสามารถของคนวาดเดิมมาทั้งหมด (โดยเฉพาะเรื่องลงสี) แล้วแก้แค่ส่วนที่อยากเปลี่ยน
final class MovieRenderer extends DefaultListCellRenderer {

    /**
     * วาดหนัง 1 บรรทัดในช่องเลือก
     * เราไม่ได้เรียกเมธอดนี้เอง Swing เป็นคนเรียก ทุกครั้งที่จะโชว์หนัง 1 บรรทัด
     * (ตอนเปิดหน้า, ตอนกดช่องเลือกให้รายการเด้งลงมา, ตอนเลื่อนเมาส์ไปทับแต่ละบรรทัด)
     *
     * Swing ส่งมาให้ 5 ค่า ชื่อและลำดับต้องตรงกับของตัวแม่ (ไม่งั้น @Override ไม่ได้)
     * เราใช้เองแค่ value ตัวเดียว อีก 4 ตัวส่งต่อให้ตัวแม่ใช้ตัดสินว่าจะลงสียังไง
     *
     * @param list         รายการที่เด้งลงมา ตัวแม่หยิบสีไฮไลต์ / สีปกติ / ฟอนต์ ของรายการนี้มาใช้
     * @param value        หนังที่จะวาดบรรทัดนี้ เช่น Movie M9 (Inception) — ตัวเดียวที่เราใช้เอง
     *                     อาจเป็น null ได้ (ตอนช่องเลือกยังไม่มีอะไรถูกเลือก)
     * @param index        บรรทัดที่เท่าไหร่ในรายการ (นับจาก 0) ตัวแม่ใช้ เราไม่ได้ใช้
     * @param isSelected   true = บรรทัดนี้ถูกเลือก / ถูกเมาส์ชี้อยู่ → ตัวแม่ลงพื้นสีไฮไลต์
     *                     false = บรรทัดธรรมดา → ตัวแม่ลงพื้นสีปกติ
     * @param cellHasFocus ไม่ได้ใช้เป็น false ตลอด
     * @return สิ่งที่วาดเสร็จแล้ว (ตัวมันเอง ซึ่งเป็น JLabel) Swing เอาไปแปะเป็นบรรทัดนั้น
     */
    @Override   // บอก Java ว่ากำลังเขียนทับเมธอดของตัวแม่ ถ้าพิมพ์ชื่อหรือพารามิเตอร์ผิด Java จะเตือนทันที
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        // 1. ให้ตัวแม่ทำงานของเขาก่อน: ลงสีพื้น/สีตัวหนังสือตาม isSelected, ใช้ฟอนต์ของ list, วาดกรอบตาม cellHasFocus (ไม่ใช้)
        //    และเขียนข้อความแบบยาวจาก toString() (เดี๋ยวข้อ 3 เขียนทับ)
        //    ถ้าไม่มีบรรทัดนี้ บรรทัดที่ถูกเลือกจะไม่มีแถบไฮไลต์ ดูไม่ออกว่าเลือกเรื่องไหนอยู่
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // 2. เช็กว่า value เป็นหนังจริงไหม ถ้าใช่ให้ตั้งชื่อตัวแปรว่า movie แล้วใช้ต่อได้เลย
        //    ต้องเช็ก เพราะ value อาจเป็น null ถ้าเรียก movie.title() ตอนเป็น null โปรแกรมจะพัง
        if (value instanceof Movie movie) {
            // 3. เขียนทับข้อความแบบยาว ให้เหลือ "ชื่อหนัง  (นาที)" เช่น "Inception  (148 min)"
            //    สีที่ตัวแม่ลงไว้ในข้อ 1 ยังอยู่เหมือนเดิม เปลี่ยนแค่ข้อความ
            setText(movie.title() + "  (" + movie.durationMinutes() + " min)");
        }

        // 4. ส่งตัวเองที่วาดเสร็จแล้ว (สีจากตัวแม่ + ข้อความจากเรา) กลับไปให้ Swing แสดงเป็นบรรทัดนั้น
        return this;
    }
}