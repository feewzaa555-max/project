//เป็น interface ที่บอกแค่ว่า "ขอข้อมูลหนังกับรอบฉายได้ด้วยเมธอดอะไร" 
/*
    มี 2 เมธอด:
    findAll() คืนหนังทั้งหมด
    findShowtimesOn(date) คืนรอบฉายทั้งหมดของวันนั้น
 */


package repository;

import java.io.IOException; //throws IOException ท้ายทั้ง 2 เมธอด ตัวที่อ่าน CSV จริงอาจเจอไฟล์หาย หรือข้อมูลผิด
import java.time.LocalDate; //วันที่อย่างเดียว ไม่มีเวลา
import java.util.List;
import model.Movie; // เรียกใช้ file Movive
import model.Showtime; // เรียกใช้ file Showtime

/**
 * ที่เก็บข้อมูลหนังและตารางฉาย (อ่านอย่างเดียว)
 * MovieService เรียกผ่าน interface นี้ จึงไม่ต้องรู้ว่าข้างล่างเก็บในไฟล์หรือที่อื่น
 * และตอนทดสอบใส่ตัวปลอมแทนได้โดยไม่ต้องสร้างไฟล์ (SC5 DIP)
 */
public interface MovieRepository {

    /**
     * หนังทั้งหมดที่เข้าฉาย
     *
     * @return หนังทุกเรื่อง เรียงตามลำดับในที่เก็บ หรือ List ว่างถ้ายังไม่มีหนัง
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     */
    List<Movie> findAll() throws IOException;

    /**
     * รอบฉายทั้งหมดของวันที่ระบุ
     *
     * @param date วันที่ต้องการ ห้าม null
     * @return รอบฉายทุกรอบของวันนั้น เรียงตามลำดับในตารางฉาย หรือ List ว่างถ้าไม่มีรอบ
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     *                     (เช่น ตารางฉายอ้างรหัสหนังที่ไม่มีอยู่จริง)
     * @throws IllegalArgumentException ถ้า date เป็น null
     */
    List<Showtime> findShowtimesOn(LocalDate date) throws IOException;
}