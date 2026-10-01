//BookingRepository <- เป็น interface ที่บอกแค่ว่า "อ่านการจองทั้งหมด / บันทึกการจองใหม่ ได้ด้วยเมธอดอะไร" ยังไม่บอกว่าเก็บยังไง
/*
    มี 2 เมธอด:
    findAll()      → การจองทั้งหมด เรียงตามลำดับที่จอง (ยังไม่มีใครจองเลย ได้รายการว่าง)
    save(booking)  → เพิ่มการจองใหม่ต่อท้าย (รหัสซ้ำกับที่มีอยู่แล้ว throw)

    การจองยกเลิกไม่ได้ จึงไม่มีการแก้หรือเขียนทับการจองเดิม มีแต่เพิ่มใหม่

    ตัวอย่างการใช้ (ใน BookingService ที่จะทำทีหลัง):
        BookingRepository bookings = new CsvBookingRepository(...);   // ตัวจริงเขียนลงไฟล์ bookings.csv

        bookings.findAll();                      // ยังไม่มีใครจอง → []
        bookings.save(booking B1 ของ somchai);   // เพิ่มใหม่ → [B1]
        bookings.save(booking B2 ของ nok);       // เพิ่มใหม่ → [B1, B2]
        bookings.save(booking B1 อีกอัน);         // รหัส B1 มีแล้ว → throw ไม่บันทึก

    ไม่มีเมธอดค้นตามรอบหรือตามคน BookingService กรองเองจาก findAll()
    เช่น ที่นั่งที่ถูกจองของรอบ X = ที่นั่งของทุกการจองที่ showtimeId เป็น X
 */

package repository;

import java.io.IOException; // throws IOException ท้ายทั้ง 2 เมธอด ตัวที่เขียน CSV จริงอาจอ่าน/เขียนไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import java.util.List;      // ชนิดของรายการการจองที่ findAll คืนออกไป
import model.Booking;       // การจองที่จะอ่าน / บันทึก

/**
 * ที่เก็บการจอง
 * BookingService เรียกผ่าน interface นี้ จึงไม่ต้องรู้ว่าข้างล่างเก็บในไฟล์หรือที่อื่น
 * และตอนทดสอบใส่ตัวปลอมที่เก็บในหน่วยความจำแทนได้โดยไม่ต้องสร้างไฟล์ (SC5 DIP)
 * interface ไม่มี field จึงไม่มี AF / RI / checkRep ตัวที่ implement ต้องมีเอง
 */
public interface BookingRepository {

    /**
     * การจองทั้งหมด เรียงตามลำดับที่จอง (B1, B2, B3, ...)
     *
     * ตัวอย่าง:
     *   ยังไม่มีใครจองเลย                → []
     *   bookings.csv มีการจอง B1, B2      → [B1, B2]
     *
     * @return การจองทุกรายการ แก้รายการที่ได้ไปแล้วไม่กระทบที่เก็บ
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     */
    List<Booking> findAll() throws IOException;

    /**
     * เพิ่มการจองใหม่ต่อท้าย
     * รหัสการจองต้องไม่ซ้ำกับที่มีอยู่แล้ว (BookingService ออกรหัสใหม่ให้ทุกครั้ง ปกติจึงไม่ซ้ำ)
     *
     * ตัวอย่าง:
     *   ที่เก็บว่าง,        save(B1) → [B1]
     *   ที่เก็บมี B1,       save(B2) → [B1, B2]
     *   ที่เก็บมี B1, B2,   save(B1) → throw "duplicate booking id: B1" ที่เก็บไม่เปลี่ยน
     *
     * @param booking การจองที่จะบันทึก ห้าม null รหัสห้ามซ้ำกับที่มีอยู่
     * @throws IOException ถ้าอ่านหรือเขียนข้อมูลไม่ได้
     * @throws IllegalArgumentException ถ้า booking เป็น null หรือรหัสซ้ำกับที่มีอยู่แล้ว
     */
    void save(Booking booking) throws IOException;
}