//Role <- บทบาทของผู้ใช้ บอกว่าคนที่ login เข้ามาเป็นลูกค้าทั่วไป หรือเป็น admin ที่จัดตารางหนัง
/*
    มี 2 ค่า:
    USER  → ลูกค้าทั่วไป  สมัครเองได้  เลือกหนัง จองตั๋ว เติมเงิน สมัครสมาชิก
    ADMIN → ผู้ดูแล       สมัครเองไม่ได้ (ใส่ไว้ใน users.csv ล่วงหน้า)  เลือกหนังที่จะฉายวันพรุ่งนี้

    ตัวอย่างการใช้:
        Role role = Role.ADMIN;
        role == Role.ADMIN;          // true  → เปิดหน้า admin
        Role.valueOf("ADMIN");       // Role.ADMIN → ใช้ตอนอ่านคำว่า ADMIN จาก users.csv
        Role.valueOf("ADMN");        // throw IllegalArgumentException → พิมพ์ผิดในไฟล์ จับได้ทันที
        role.name();                 // "ADMIN" → ใช้ตอนเขียนกลับลง users.csv
 */

package model;

// ไม่มี import: enum ไม่ต้องใช้ class อื่น

/**
 * บทบาทของผู้ใช้ในระบบ
 * เป็น enum (ชนิดที่มีค่าให้เลือกตายตัว) แทนการใช้ String "admin" หรือ boolean
 * พิมพ์ผิดเช่น Role.ADMN คอมไพล์ไม่ผ่านทันที (SC5 หน้า 34 / หน้า 36: ใช้ enum แทนค่าคงที่ลอย ๆ)
 */
public enum Role {

    /** ลูกค้าทั่วไป ทุกคนที่สมัครผ่านหน้า Sign up ได้ค่านี้ */
    USER,

    /** ผู้ดูแล จัดได้ว่าวันพรุ่งนี้ฉายหนังเรื่องไหน */
    ADMIN;

    // AF: บทบาทของผู้ใช้ 1 คน USER = ลูกค้า, ADMIN = ผู้ดูแลตารางหนัง
    // RI: ไม่มี field ให้ตรวจ ค่าที่เป็นไปได้มีแค่ USER กับ ADMIN และ Java คุมให้เองตอนคอมไพล์
    //     จึงไม่มี checkRep() (แบบเดียวกับ AuthField) ถ้าวันหลังเพิ่ม field ค่อยเพิ่ม checkRep()
    // Safety from rep exposure: ไม่มี field ที่แก้ได้
    // Thread safety: enum สร้างครั้งเดียวตอนเริ่มโปรแกรม ค่าไม่เปลี่ยนอีก ใช้ข้าม thread ได้ (SC7)
}