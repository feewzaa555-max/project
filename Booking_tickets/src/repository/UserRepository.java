package repository;

import java.io.IOException;
import model.User;

/**
 * ที่เก็บข้อมูลผู้ใช้
 * AuthService เรียกผ่าน interface นี้ จึงไม่ต้องรู้ว่าข้างล่างเก็บในไฟล์หรือที่อื่น (SC5 DIP)
 */
public interface UserRepository {

    /**
     * หาผู้ใช้จากชื่อ โดยไม่สนตัวพิมพ์เล็ก/ใหญ่
     *
     * @param username ชื่อที่ต้องการหา
     * @return User ที่เจอ (ชื่อตามที่บันทึกไว้) หรือ null ถ้าไม่มีชื่อนี้
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้
     */
    User findByUsername(String username) throws IOException; // <- function นี้อาจเกิด Exception ได้ในจึงอนุญาตให้ตัวที่ implement เขียน throws IOException 

    /**
     * บันทึกผู้ใช้ใหม่ ไม่เช็กชื่อซ้ำ (AuthService ต้องเช็กก่อนเรียก)
     *
     * @param user ผู้ใช้ที่จะบันทึก ห้าม null
     * @throws IOException ถ้าเขียนข้อมูลไม่ได้
     */
    void save(User user) throws IOException;
}