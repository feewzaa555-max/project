//BookingRepository <- เป็น interface ที่บอกแค่ว่า "อ่านการจองทั้งหมด / บันทึกการจอง ได้ด้วยเมธอดอะไร" ยังไม่บอกว่าเก็บยังไง
/*
    มี 2 เมธอด:
    findAll()      → การจองทั้งหมด เรียงตามลำดับที่จอง (ยังไม่มีใครจองเลย ได้รายการว่าง)
    save(booking)  → บันทึกการจอง ถ้ารหัสนี้มีอยู่แล้วเขียนทับ ถ้ายังไม่มีเพิ่มใหม่

    ตัวอย่างการใช้ (ใน BookingService ที่จะทำทีหลัง):
        BookingRepository bookings = new CsvBookingRepository(...);   // ตัวจริงเขียนลงไฟล์ bookings.csv

        bookings.findAll();                    // ยังไม่มีใครจอง → []
        bookings.save(booking B1 ของ somchai PAID);      // เพิ่มใหม่  → [B1]
        bookings.save(booking B2 ของ nok PAID);          // เพิ่มใหม่  → [B1, B2]
        bookings.save(booking B1 ของ somchai CANCELLED); // รหัส B1 มีแล้ว → เขียนทับ → [B1 (ยกเลิก), B2]
        bookings.findAll();                    // [B1 CANCELLED, B2 PAID]

    ไม่มีเมธอดค้นตามรอบหรือตามคน BookingService กรองเองจาก findAll()
    เช่น ที่นั่งที่ถูกจองของรอบ X = การจองที่ showtimeId เป็น X และยัง isActive()
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
     * การจองทั้งหมด ทั้งที่ชำระแล้วและยกเลิกแล้ว เรียงตามลำดับที่จอง (B1, B2, B3, ...)
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
     * บันทึกการจอง
     * ถ้ามีการจองรหัสเดียวกันอยู่แล้ว → เขียนทับของเดิม (ใช้ตอนยกเลิก)
     * ถ้ายังไม่มี → เพิ่มต่อท้าย (ใช้ตอนจองใหม่)
     * ผลคือ 1 รหัสมีการจองเดียวเสมอ
     *
     * ตัวอย่าง:
     *   ที่เก็บว่าง,           save(B1 PAID)      → [B1 PAID]
     *   ที่เก็บมี B1 PAID,     save(B2 PAID)      → [B1 PAID, B2 PAID]
     *   ที่เก็บมี B1, B2,      save(B1 CANCELLED) → [B1 CANCELLED, B2 PAID]  (ยังมี 2 รายการ)
     *
     * @param booking การจองที่จะบันทึก ห้าม null
     * @throws IOException ถ้าอ่านหรือเขียนข้อมูลไม่ได้
     * @throws IllegalArgumentException ถ้า booking เป็น null
     */
    void save(Booking booking) throws IOException;
}