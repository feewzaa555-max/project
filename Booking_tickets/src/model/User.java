package model;

// ไม่มี import: Role อยู่ package model เดียวกัน ใช้ได้เลย

/**
 * 
 * ผู้ใช้ระบบ (ลูกค้า หรือ admin) — ชื่อผู้ใช้ไม่ซ้ำกันในระบบ จึงใช้เป็นตัวระบุคนแทน id
 *
 * เป็น record จึง immutable: field เป็น private final, ไม่มี setter,   <- กำหนดให้เมื่อสร้างรหัสแล้วไม่สามารถเปลี่ยนชื่อหรอเปลี่ยนรหัสได้
 * และได้ equals() / hashCode() ที่ถูกต้องจาก compiler (SC5)
 *
 * กฎรูปแบบชื่อ/รหัส (ยาวอย่างน้อย 5 ตัว, ขึ้นต้นภาษาอังกฤษ ฯลฯ) ไม่ได้เช็กที่นี่
 * แต่เช็กใน AuthService ตอน Sign up — คลาสนี้แค่เก็บข้อมูลที่ผ่านกฎแล้ว (SC5 SRP)
 *
 * ตัวอย่างการใช้:
 *   User nok   = new User("nok12", "abc12");                  // ไม่บอก role → ได้ USER
 *   User admin = new User("admin", "admin1", Role.ADMIN);     // บอก role ตรง ๆ
 *   nok.isAdmin();     // false → login แล้วเปิดหน้าเลือกหนัง
 *   admin.isAdmin();   // true  → login แล้วเปิดหน้า admin
 *   admin.role();      // Role.ADMIN → ใช้ตอนเขียนลง users.csv
 *
 * @param username ชื่อผู้ใช้ ห้าม null ห้ามว่าง
 * @param password รหัสผ่าน ห้าม null
 * @param role     บทบาท USER หรือ ADMIN ห้าม null
 */
public record User(String username, String password, Role role) {

    // AF: ผู้ใช้ 1 คนที่ชื่อ username รหัส password และมีบทบาท role
    // RI: username ไม่เป็น null และไม่ว่าง, password ไม่เป็น null, role ไม่เป็น null
    // Safety from rep exposure: ข้างนอกแก้ชื่อ รหัส และบทบาทไม่ได้ เพราะ record เป็น private final
    //                           String แก้ไขไม่ได้ และ Role เป็น enum ค่าตายตัว


    /** ตรวจค่าตอนสร้าง: ข้อมูลเสียจาก CSV จะถูกจับได้ทันที ไม่หลุดไปพังที่อื่น (SC2 fail-fast) */
    //ถ้ากฎของ Singup เช่นusername >= 5 อยู่ใน User แล้ววันหนึ่งเป็น >=6 ไอ่คนที่เขียน username ไว้ 5 ก็ซวย เปิด csv อ่าน ตอนเปิดโปรแกรม ระบบอ่าน users.csv เจอคนที่สมัครไว้ด้วยชื่อ 5 ตัว สร้าง User ไม่ผ่าน โปรแกรมพังตั้ง
    public User {
    if (username == null) throw new IllegalArgumentException("username must not be null");
    if (password == null) throw new IllegalArgumentException("password must not be null");
    if (username.isBlank()) throw new IllegalArgumentException("username must not be blank"); // เช็กว่า username ว่างไหม
    if (role == null) throw new IllegalArgumentException("role must not be null"); // ไม่รู้ว่าเป็นใคร = ข้อมูลเสีย
    // 4 บรรทัด if ข้างบนนี้คือการตรวจ RI ครบทุกข้อแล้ว จึงไม่มี checkRep() แยก (แบบเดียวกับ Movie, Showtime)
    // ใน record ค่าจะถูกใส่ลง field หลังจบวงเล็บนี้ ถ้าเรียก checkRep() ที่อ่าน field ตรงนี้ จะเห็นเป็น null ทั้งหมด
    }

    /**
     * สร้างผู้ใช้ทั่วไป (USER) แบบไม่ต้องบอก role
     * ทำงาน: ส่งต่อให้ constructor หลักโดยเติม Role.USER ให้
     * เช่น new User("nok12", "abc12") → User[nok12, abc12, USER]
     * มีไว้ให้โค้ดเดิมที่เรียก new User(ชื่อ, รหัส) ยังใช้ได้ (AuthService, CsvUserRepository, หน้าจอ)
     * และคนที่สมัครเองต้องได้ USER อยู่แล้ว
     *
     * @param username ชื่อผู้ใช้ ห้าม null ห้ามว่าง
     * @param password รหัสผ่าน ห้าม null
     */
    public User(String username, String password) {
        // this(username, password, Role.USER)
        //   username = ชื่อที่ส่งมา เช่น "nok12"
        //   password = รหัสที่ส่งมา เช่น "abc12"
        //   Role.USER = เติมบทบาทลูกค้าให้
        this(username, password, Role.USER);
    }

    /**
     * คนนี้เป็น admin ไหม หน้า Login ใช้ตัดสินว่าจะเปิดหน้า admin หรือหน้าเลือกหนัง
     * เช่น new User("admin", "admin1", Role.ADMIN).isAdmin() → true
     *      new User("nok12", "abc12").isAdmin()               → false
     *
     * @return true ถ้า role เป็น ADMIN
     */
    public boolean isAdmin() {
        // enum เทียบด้วย == ได้ เพราะแต่ละค่ามีตัวเดียวทั้งโปรแกรม
        return role == Role.ADMIN;
    }

    /** ไม่แสดงรหัสผ่าน กันรหัสหลุดเวลา print หรือดู log  <- ตรงนี้ถ้าไม่ทำแล้วปริ้น User ออกมาตรงๆมันจะได้ User[username=ค่าที่กรอก, password=ค่าที่กรอก] ซึ้งถ้าปริ้นออกมามันจะเห็นรหัส */
    @Override
    public String toString() {
        return "User[username=" + username + "]";
    }
}