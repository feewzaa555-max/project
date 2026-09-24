package model;

/**
 * ผู้ใช้ระบบ (ลูกค้า) — ชื่อผู้ใช้ไม่ซ้ำกันในระบบ จึงใช้เป็นตัวระบุคนแทน id
 *
 * เป็น record จึง immutable: field เป็น private final, ไม่มี setter,   <- กำหนดให้เมื่อสร้างรหัสแล้วไม่สามารถเปลี่ยนชื่อหรอเปลี่ยนรหัสได้
 * และได้ equals() / hashCode() ที่ถูกต้องจาก compiler (SC5)
 *
 * กฎรูปแบบชื่อ/รหัส (ยาวอย่างน้อย 5 ตัว, ขึ้นต้นภาษาอังกฤษ ฯลฯ) ไม่ได้เช็กที่นี่
 * แต่เช็กใน CredentialRules ตอน Sign up — คลาสนี้แค่เก็บข้อมูลที่ผ่านกฎแล้ว (SC5 SRP)
 *
 * @param username ชื่อผู้ใช้ ห้าม null ห้ามว่าง
 * @param password รหัสผ่าน ห้าม null
 */
public record User(String username, String password) {

    /** ตรวจค่าตอนสร้าง: ข้อมูลเสียจาก CSV จะถูกจับได้ทันที ไม่หลุดไปพังที่อื่น (SC2 fail-fast) */
    //ถ้ากฎของ Singup เช่นusername >= 5 อยู่ใน User แล้ววันหนึ่งเป็น >=6 ไอ่คนที่เขียน username ไว้ 5 ก็ซวย เปิด csv อ่าน ตอนเปิดโปรแกรม ระบบอ่าน users.csv เจอคนที่สมัครไว้ด้วยชื่อ 5 ตัว สร้าง User ไม่ผ่าน โปรแกรมพังตั้ง
    public User {
    if (username == null) throw new IllegalArgumentException("username must not be null");
    if (password == null) throw new IllegalArgumentException("password must not be null");
    if (username.isBlank()) throw new IllegalArgumentException("username must not be blank"); // เช็กว่า username ว่างไหม
    }

    /** ไม่แสดงรหัสผ่าน กันรหัสหลุดเวลา print หรือดู log  <- ตรงนี้ถ้าไม่ทำแล้วปริ้น User ออกมาตรงๆมันจะได้ User[username=ค่าที่กรอก, password=ค่าที่กรอก] ซึ้งถ้าปริ้นออกมามันจะเห็นรหัส */
    @Override
    public String toString() {
        return "User[username=" + username + "]";
    }
}