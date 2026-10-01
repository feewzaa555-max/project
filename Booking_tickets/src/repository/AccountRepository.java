//AccountRepository <- เป็น interface ที่บอกแค่ว่า "หาบัญชี / บันทึกบัญชี ได้ด้วยเมธอดอะไร" ยังไม่บอกว่าเก็บยังไง
/*
    มี 2 เมธอด:
    findByUsername(username) → หาบัญชีของคนนี้ ไม่เจอคืน null
    save(account)            → บันทึกบัญชี ถ้ามีชื่อนี้อยู่แล้วเขียนทับ ถ้ายังไม่มีเพิ่มใหม่

    ตัวอย่างการใช้ (ใน AccountService ที่จะทำทีหลัง):
        AccountRepository accounts = new CsvAccountRepository(...);   // ตัวจริงเขียนลงไฟล์ accounts.csv

        Account acc = accounts.findByUsername("somchai");  // สมัครแล้วแต่ยังไม่เคยเติมเงิน → ไม่มีแถวใน accounts.csv → null
        if (acc == null) {
            acc = Account.newFor("somchai");               // ถือว่าเงิน 0 ไม่เป็นสมาชิก
        }
        acc = acc.topUp(300);                              // เงิน 300
        accounts.save(acc);                                // ไฟล์ยังไม่มี somchai → เพิ่มแถวใหม่  "somchai,300,"

        acc = acc.topUp(200);                              // เงิน 500
        accounts.save(acc);                                // มี somchai แล้ว → เขียนทับแถวเดิม "somchai,500,"
        accounts.findByUsername("somchai");                // Account[somchai, 500, null]

    ต่างจาก UserRepository.save ตรงที่ของ User เพิ่มแถวต่อท้ายอย่างเดียว (บันทึกครั้งเดียวตอนสมัคร)
    แต่บัญชีเปลี่ยนทุกครั้งที่เติมเงิน / จ่าย / สมัครสมาชิก จึงต้องเขียนทับแถวเดิมได้
    ไม่อย่างนั้นคนเดียวจะมีหลายแถว แล้วไม่รู้ว่าเงินแถวไหนถูก
 */

package repository;

import java.io.IOException; // throws IOException ท้ายทั้ง 2 เมธอด ตัวที่เขียน CSV จริงอาจอ่าน/เขียนไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import model.Account;       // บัญชีที่จะหา / บันทึก

/**
 * ที่เก็บบัญชีเงินและสถานะสมาชิกของผู้ใช้
 * AccountService เรียกผ่าน interface นี้ จึงไม่ต้องรู้ว่าข้างล่างเก็บในไฟล์หรือที่อื่น
 * และตอนทดสอบใส่ตัวปลอมที่เก็บในหน่วยความจำแทนได้โดยไม่ต้องสร้างไฟล์ (SC5 DIP)
 * interface ไม่มี field จึงไม่มี AF / RI / checkRep ตัวที่ implement ต้องมีเอง
 */
public interface AccountRepository {

    /**
     * หาบัญชีของผู้ใช้จากชื่อ
     * ชื่อต้องตรงตัวพิมพ์ทุกตัว (ต่างจาก UserRepository ที่ไม่สนตัวเล็ก/ใหญ่)
     * เพราะชื่อที่ส่งมาเอามาจาก User ที่ login สำเร็จแล้ว ซึ่งเป็นชื่อตามที่บันทึกไว้อยู่แล้ว
     *
     * ตัวอย่าง (accounts.csv มีแถว "somchai,500,"):
     *   findByUsername("somchai") → Account[somchai, 500, null]
     *   findByUsername("nok")     → null  (nok สมัครแล้วแต่ยังไม่เคยเติมเงินหรือสมัครสมาชิก เลยยังไม่มีแถว
     *                                      คนเรียกถือว่าเป็น Account.newFor("nok") คือเงิน 0 ไม่เป็นสมาชิก)
     *
     * @param username ชื่อผู้ใช้ ห้าม null
     * @return บัญชีของคนนี้ หรือ null ถ้าคนนี้ยังไม่เคยเติมเงินหรือสมัครสมาชิก (ยังไม่มีแถวของคนนี้)
     * @throws IOException ถ้าอ่านข้อมูลไม่ได้ หรือข้อมูลในที่เก็บผิดรูปแบบ
     * @throws IllegalArgumentException ถ้า username เป็น null
     */
    Account findByUsername(String username) throws IOException;

    /**
     * บันทึกบัญชี
     * ถ้ามีบัญชีชื่อเดียวกันอยู่แล้ว → เขียนทับของเดิม
     * ถ้ายังไม่มี → เพิ่มใหม่
     * ผลคือ 1 ชื่อมีบัญชีเดียวเสมอ
     *
     * ตัวอย่าง:
     *   ที่เก็บว่าง,               save(Account[somchai, 300, null]) → มี somchai 300
     *   ที่เก็บมี somchai 300,     save(Account[somchai, 500, null]) → somchai กลายเป็น 500 (ยังมีแค่ 1 บัญชี)
     *   ที่เก็บมี somchai 500,     save(Account[thanawat, 0, null])  → มี somchai 500 และ thanawat 0
     *
     * @param account บัญชีที่จะบันทึก ห้าม null
     * @throws IOException ถ้าอ่านหรือเขียนข้อมูลไม่ได้
     * @throws IllegalArgumentException ถ้า account เป็น null
     */
    void save(Account account) throws IOException;
}