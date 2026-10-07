//เป็น interface ที่บอกแค่ว่า "ขอข้อมูลหนังกับรอบฉายได้ด้วยเมธอดอะไร" 
/*
    มี 4 เมธอด:
    findAll()                    คืนหนังทั้งหมด
    findShowtimesOn(date)        คืนรอบฉายทั้งหมดของวันนั้น
    findLineupOn(date)           คืนหนังที่ฉายวันนั้น เรียงตามช่อง 1, 2, 3, ...
    saveLineup(date, movies)     บันทึกว่าวันนั้นฉายหนังเรื่องไหนบ้าง (admin ใช้)
 */


package repository;

import java.io.IOException; //throws IOException ท้ายทุกเมธอด ตัวที่อ่าน/เขียน CSV จริงอาจเจอไฟล์หาย หรือข้อมูลผิด
import java.time.LocalDate; //วันที่อย่างเดียว ไม่มีเวลา
import java.util.List; // ชนิดของรายการหนัง / รอบฉาย
import model.Movie; // เรียกใช้ file Movive
import model.Showtime; // เรียกใช้ file Showtime

/**
 * ที่เก็บข้อมูลหนัง ตารางฉาย และหนังที่ฉายในแต่ละวัน (lineup)
 * MovieService / BookingService / AdminService เรียกผ่าน interface นี้ จึงไม่ต้องรู้ว่าข้างล่างเก็บในไฟล์หรือที่อื่น
 * และตอนทดสอบใส่ตัวปลอมแทนได้โดยไม่ต้องสร้างไฟล์ (SC5 DIP)
 */
public interface MovieRepository {

    /**
     * หนังทั้งหมดที่มีในระบบ (ทั้งที่ฉายและยังไม่ได้ฉาย)
     *
     * @return หนังทุกเรื่อง เรียงตามลำดับในที่เก็บ หรือ List ว่างถ้ายังไม่มีหนัง
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     */
    List<Movie> findAll() throws IOException;

    /**
     * รอบฉายทั้งหมดของวันที่ระบุ หนังในแต่ละรอบมาจาก lineup ของวันนั้น
     *
     * @param date วันที่ต้องการ ห้าม null
     * @return รอบฉายทุกรอบของวันนั้น เรียงตามลำดับในตารางฉาย หรือ List ว่างถ้าวันนั้นยังไม่มี lineup
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     *                     (เช่น lineup อ้างรหัสหนังที่ไม่มีอยู่จริง)
     * @throws IllegalArgumentException ถ้า date เป็น null
     */
    List<Showtime> findShowtimesOn(LocalDate date) throws IOException;

    /**
     * หนังที่ฉายในวันที่ระบุ เรียงตามช่อง (ตัวแรก = ช่อง 1)
     * ถ้าวันนั้นไม่ได้ตั้งไว้ ใช้ชุดล่าสุดที่ตั้งไว้ก่อนวันนั้น
     *
     * @param date วันที่ต้องการ ห้าม null
     * @return หนังของวันนั้นเรียงตามช่อง หรือ List ว่างถ้าไม่เคยตั้งไว้เลยตั้งแต่ก่อนวันนั้น
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     * @throws IllegalArgumentException ถ้า date เป็น null
     */
    List<Movie> findLineupOn(LocalDate date) throws IOException;

    /**
     * บันทึกหนังที่จะฉายในวันที่ระบุ ถ้าวันนั้นเคยตั้งไว้แล้วจะเขียนทับของเดิม
     * ไม่ได้เช็กกฎ (เช่น ต้องกี่เรื่อง, แก้ได้แค่วันพรุ่งนี้) กฎอยู่ที่ AdminService
     *
     * @param date   วันที่ฉาย ห้าม null
     * @param movies หนังเรียงตามช่อง ห้าม null ห้ามว่าง
     * @throws IOException ถ้าอ่านหรือเขียนข้อมูลไม่ได้
     * @throws IllegalArgumentException ถ้า date หรือ movies เป็น null หรือ movies ว่าง
     */
    void saveLineup(LocalDate date, List<Movie> movies) throws IOException;
}