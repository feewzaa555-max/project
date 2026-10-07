//CsvUserRepository <- อ่าน/เขียนข้อมูลผู้ใช้ในไฟล์ CSV (ไม่ได้ตรวจกฎ การตรวจอยู่ใน AuthService)
/*
    รูปแบบไฟล์ users.csv บรรทัดละ 1 คน ไม่มีบรรทัดหัวตาราง:
        ชื่อ,รหัส,บทบาท
        admin,admin1,ADMIN      ← admin ใส่ไว้ล่วงหน้า (สมัครผ่านหน้าจอไม่ได้)
        nok12,abc12,USER        ← คนที่สมัครผ่านหน้าจอ
        somchai,abcde           ← แถวแบบเก่า (ก่อนมี role) ยังอ่านได้ ถือเป็น USER

    ตัวอย่างการใช้:
        UserRepository users = new CsvUserRepository(Path.of("data", "users.csv"));
        users.findByUsername("admin");   // User[admin, admin1, ADMIN]
        users.findByUsername("ADMIN");   // ได้คนเดียวกัน (ไม่สนตัวพิมพ์เล็ก/ใหญ่)
        users.findByUsername("nobody");  // null → AuthService บอกว่าไม่พบชื่อนี้
        users.save(new User("nok12", "abc12"));   // ต่อท้ายไฟล์ "nok12,abc12,USER"
 */

package repository;

import java.io.IOException; // <- เพื่อ throw exception
import java.nio.charset.StandardCharsets; //<- import มาเพื่อให้เขียนภาษาไทยแล้วไม่ error
import java.nio.file.Files; //<- คำสั่งจัดการไฟล์
import java.nio.file.Path; // <- เก็บตำแหน่งไฟล์
import java.util.ArrayList; // <- สร้างรายการบรรทัดที่แก้ได้ (เพิ่มบรรทัดใหม่ตอน save)
import java.util.List; // <- ชนิดของรายการบรรทัด
import model.Role; // <- บทบาท USER / ADMIN แปลงจากคำในคอลัมน์ที่ 3
import model.User; // folder.file คือเราจะใช้เจ้าตัว User เลย import มา

/**
 * เก็บผู้ใช้ในไฟล์ CSV บรรทัดละ 1 คน รูปแบบ username,password,role (ไม่มีบรรทัดหัวตาราง)
 * แถวเก่าที่มีแค่ username,password ถือเป็น USER
 */
public class CsvUserRepository implements UserRepository {

    private static final int NAME = 0;       // ช่องที่ 1 = ชื่อ
    private static final int PASSWORD = 1;   // ช่องที่ 2 = รหัส
    private static final int ROLE = 2;       // ช่องที่ 3 = บทบาท
    private static final int MAX_COLUMNS = 3; // แยกได้มากสุด 3 ช่อง

    // AF: file คือไฟล์ CSV ที่แต่ละบรรทัดแทนผู้ใช้ 1 คน ถ้ายังไม่มีไฟล์ แปลว่ายังไม่มีผู้ใช้
    // RI: file ไม่เป็น null
    // Safety from rep exposure: file เป็น private final และ Path แก้ไขไม่ได้ (immutable)
    private final Path file; // <- เก็บตำแหน่งไฟล์ที่จะอ่านเขียน เช่น data/users.csv

    /**
     * สร้างตัวอ่าน/เขียนผู้ใช้ที่ผูกกับไฟล์นี้ ยังไม่อ่านไฟล์ตอนสร้าง
     *
     * @param file ไฟล์ CSV ที่ใช้เก็บผู้ใช้ ยังไม่มีไฟล์ก็ได้
     * @throws IllegalArgumentException ถ้า file เป็น null
     */
    public CsvUserRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null"); //<- ห้ามส่ง null มา ส่วนตัวไฟล์ยังไม่มีก็ได้ save ครั้งแรกจะสร้างให้เอง
        }
        this.file = file;
        checkRep();
    }

    /**
     * หาผู้ใช้จากชื่อ ไม่สนตัวพิมพ์เล็ก/ใหญ่
     * ทำงาน: อ่านทุกบรรทัด → แปลงทีละบรรทัดเป็น User → เจอชื่อตรงคืนเลย / ไม่เจอคืน null
     * เช่น ไฟล์มี "admin,admin1,ADMIN" กับ "nok12,abc12,USER"
     *      findByUsername("nok12") → User[nok12, abc12, USER]
     *      findByUsername("NOK12") → คนเดียวกัน
     *      findByUsername("bob")   → null
     *
     * @param username ชื่อที่จะหา
     * @return User ที่ชื่อตรง หรือ null ถ้าไม่มี
     * @throws IOException ถ้าอ่านไฟล์ไม่ได้ หรือมีบรรทัดที่บทบาทไม่ใช่ USER / ADMIN
     */
    @Override
    public User findByUsername(String username) throws IOException {
        for (String line : readLines()) {
            User user = parseLine(line); //parseline คือทำให้ข้อความประกอบเป็น user เช่น "thanawat,12345,USER" จับ thanawat เป็น username จับ 12345 เป็นรหัส USER เป็นบทบาท
            if (user != null && user.username().equalsIgnoreCase(username)) { // parseline ที่ส่งมาต้องไม่เป็น null และ user.username() ต้องเท่ากับ username ที่รับเข้ามา
                return user; // ส่ง user ที่ตรงกับ username ออกไป
            }
        }
        return null; // ไม่มี user ที่ตรงกับ username ส่ง null

        // ทั้ง return user และ return null ส่งไปให้ authservice พิจารณา
    }

    /**
     * ต่อท้ายผู้ใช้ใหม่ลงไฟล์ (ไม่ได้เช็กชื่อซ้ำ AuthService เช็กก่อนเรียกแล้ว)
     * ทำงาน: เช็กว่าชื่อ/รหัสไม่มี , → อ่านบรรทัดเดิมทั้งหมด → ต่อท้าย "ชื่อ,รหัส,บทบาท" → เขียนกลับ
     * เช่น save(new User("nok12", "abc12")) → ต่อท้ายไฟล์ "nok12,abc12,USER"
     *
     * @param user ผู้ใช้ที่จะบันทึก
     * @throws IllegalArgumentException ถ้าชื่อหรือรหัสมี , เพราะจะทำให้ไฟล์ CSV เสีย
     * @throws IOException ถ้าเขียนไฟล์ไม่ได้
     */
    @Override
    public void save(User user) throws IOException {
        if (user.username().contains(",")) {
            throw new IllegalArgumentException("username must not contain ,");
        }
        // ตอนนี้มี 3 ช่อง ถ้ารหัสมี , จะแยกผิดช่องตอนอ่าน (AuthService ไม่ยอมให้มี , อยู่แล้ว เช็กซ้ำกันไฟล์เสีย)
        if (user.password().contains(",")) {
            throw new IllegalArgumentException("password must not contain ,");
        }
        List<String> lines = readLines();
        // user.role().name() : ชื่อของบทบาท เช่น Role.USER → "USER"
        lines.add(user.username() + "," + user.password() + "," + user.role().name()); //"thanawat,12345,USER"

        Path folder = file.getParent(); //file.getparent ได้ folder ที่ file อยู่
        if (folder != null) {
            Files.createDirectories(folder);   // ยังไม่มีโฟลเดอร์ data ก็สร้างให้
        }
        Files.write(file, lines, StandardCharsets.UTF_8); //เขียนลง csv
    }

    // อ่านทุกบรรทัดในไฟล์ ถ้ายังไม่มีไฟล์ คืน List ว่าง
    private List<String> readLines() throws IOException { //<- ถ้าเกิด IOException function นี้จะไม่จัดการแต่จะโยนให้ตัวที่มันเรียกใช้ function นี้
        if (!Files.exists(file)) {
            return new ArrayList<>();//มีไฟล์ไหม
        }
        return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8)); 
        //<-อ่านทีละบรรทัดใน fileแล้วก็ยัดใส่ ArrayList() ทีละตัว -> ["admin,admin1,ADMIN", "somchai,abcde,USER"]  , โดยอ่านเป็น UTF_8
    }

    /*
        แปลง 1 บรรทัดที่เป็นข้อความ String ให้เป็น User
        ทำงาน:
          1. แยกด้วย , ได้มากสุด 3 ช่อง
          2. ขาดชื่อหรือรหัส (ไม่ถึง 2 ช่อง) หรือชื่อว่าง → บรรทัดเสีย คืน null (ข้ามไป แบบเดิม)
          3. มีแค่ 2 ช่อง → แถวแบบเก่า ได้ USER
          4. มี 3 ช่อง → แปลงคำในช่องที่ 3 เป็น Role ถ้าไม่ใช่ USER / ADMIN → throw
        เช่น "admin,admin1,ADMIN" → ["admin","admin1","ADMIN"] → User[admin, admin1, ADMIN]
             "somchai,abcde"      → ["somchai","abcde"]        → User[somchai, abcde, USER]
             "bob,x1234,ADMN"     → Role.valueOf("ADMN") ไม่มี  → throw IOException
             "nopassword"         → ["nopassword"]             → null
     */
    private User parseLine(String line) throws IOException {
        // line.split(",", MAX_COLUMNS) : แยกด้วย , ได้มากสุด 3 ช่อง
        //   เช่น "admin,admin1,ADMIN" → ["admin", "admin1", "ADMIN"]
        String[] parts = line.split(",", MAX_COLUMNS);
        if (parts.length <= PASSWORD || parts[NAME].isBlank()) {  //<-ชื่อ = ป้ายชื่อของคน ทั้งระบบใช้ชื่อบอกว่าเป็นใคร ทั้งตอนหาใน CSV และตอนจองตั๋ว ถ้าชื่อว่าง ระบบจะไม่รู้ว่าเป็นใคร จึงห้ามว่างเด็ดขาด
                                                                  // parts.length <= PASSWORD คือมีไม่ถึง 2 ช่อง กันไว้เพราะถ้าบรรทัดนั้นไม่มี "," จะได้แค่ 1 ชิ้น แล้วพอเรียก parts[1] จะพัง
            return null; //ที่ไม่มี parts[1].isBlank()   <-รหัส = กุญแจ มีไว้เทียบตอน Login อย่างเดียว ถ้าว่างก็แค่ Login ไม่ผ่าน ระบบไม่พัง ส่วนเรื่องห้ามรหัสว่าง AuthService คอยเช็กให้ตอนสมัครอยู่แล้ว
        }
        if (parts.length <= ROLE) {
            // แถวแบบเก่ามีแค่ชื่อกับรหัส → new User(ชื่อ, รหัส) ได้ USER
            return new User(parts[NAME], parts[PASSWORD]);
        }
        Role role;
        try {
            // Role.valueOf(คำในช่องที่ 3 ตัดช่องว่างหน้าหลัง)
            //   เช่น "ADMIN" → Role.ADMIN / "USER " → Role.USER / "ADMN" → throw
            role = Role.valueOf(parts[ROLE].trim());
        } catch (IllegalArgumentException e) {
            // ไม่ข้ามเงียบ ๆ เหมือนบรรทัดเสียแบบอื่น เพราะถ้าข้าม admin ที่พิมพ์บทบาทผิดจะ login ไม่ได้โดยไม่รู้สาเหตุ
            throw new IOException(file.getFileName() + ": role must be USER or ADMIN but found \""
                    + parts[ROLE].trim() + "\" in line \"" + line + "\"");
        }
        // new User(ชื่อ, รหัส, บทบาท) เช่น new User("admin", "admin1", Role.ADMIN)
        return new User(parts[NAME], parts[PASSWORD], role);
    }

    // ตรวจ RI
    private void checkRep() {
        assert file != null; // ตัวแปร file ต้องไม่เป็น null
    }
}