

//CsvUserRepository <- อ่าน/เขียนข้อมูลผู้ใช้ในไฟล์ CSV (ไม่ได้ตรวจกฎ การตรวจอยู่ใน AuthService)

package repository;

import java.io.IOException; // <- เพื่อ throw exception
import java.nio.charset.StandardCharsets; //<- import มาเพื่อให้เขียนภาษาไทยแล้วไม่ error
import java.nio.file.Files; //<- คำสั่งจัดการไฟล์
import java.nio.file.Path; // <- เก็บตำแหน่งไฟล์
import java.util.ArrayList; 
import java.util.List;
import model.User; // folder.file คือเราจะใช้เจ้าตัว User เลย import มา

/**
 * เก็บผู้ใช้ในไฟล์ CSV บรรทัดละ 1 คน รูปแบบ username,password (ไม่มีบรรทัดหัวตาราง)
 */
public class CsvUserRepository implements UserRepository {

    // AF: file คือไฟล์ CSV ที่แต่ละบรรทัดแทนผู้ใช้ 1 คน ถ้ายังไม่มีไฟล์ แปลว่ายังไม่มีผู้ใช้
    // RI: file ไม่เป็น null
    // Safety from rep exposure: file เป็น private final และ Path แก้ไขไม่ได้ (immutable)
    private final Path file; // <- เก็บตำแหน่งไฟล์ที่จะอ่านเขียน เช่น data/users.csv

    /**
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

    @Override
    public User findByUsername(String username) throws IOException {
        for (String line : readLines()) {
            User user = parseLine(line); //parseline คือทำให้ข้อความประกอบเป็น user เช่น "thanawat,12345" จับ thanawat เป็น username จับ 12345 เป็นรหัส
            if (user != null && user.username().equalsIgnoreCase(username)) { // parseline ที่ส่งมาต้องไม่เป็น null และ user.username() ต้องเท่ากับ username ที่รับเข้ามา
                return user; // ส่ง user ที่ตรงกับ username ออกไป
            }
        }
        return null; // ไม่มี user ที่ตรงกับ username ส่ง null

        // ทั้ง return user และ return null ส่งไปให้ authservice พิจารณา
    }

    /**
     * @throws IllegalArgumentException ถ้าชื่อมี , เพราะจะทำให้ไฟล์ CSV เสีย
     */
    @Override
    public void save(User user) throws IOException {
        if (user.username().contains(",")) {
            throw new IllegalArgumentException("username must not contain ,");
        }
        List<String> lines = readLines();
        lines.add(user.username() + "," + user.password()); //"thanawat,12345"

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
        //<-อ่านทีละบรรทัดใน fileแล้วก็ยัดใส่ ArrayList() ทีละตัว -> ["thanawat,12345", "somchai,abcde"]  , โดยอ่านเป็น UTF_8
    }

    // แปลง 1 บรรทัดที่เป็นข้อความ String ให้เป็น User ถ้าบรรทัดเสีย คืน null
    private User parseLine(String line) {
        String[] parts = line.split(",", 2);   // แยกแค่ , ตัวแรก รหัสที่มี , จึงไม่ขาด
        if (parts.length < 2 || parts[0].isBlank()) {  //<-ชื่อ = ป้ายชื่อของคน ทั้งระบบใช้ชื่อบอกว่าเป็นใคร ทั้งตอนหาใน CSV และตอนจองตั๋ว ถ้าชื่อว่าง ระบบจะไม่รู้ว่าเป็นใคร จึงห้ามว่างเด็ดขาด
                                                       // parts.length < 2 กันไว้เพราะถ้าบรรทัดนั้นไม่มี "," จะได้แค่ 1 ชิ้น แล้วพอเรียก parts[1] จะพัง
            return null; //ที่ไม่มี parts[1].isBlank()   <-รหัส = กุญแจ มีไว้เทียบตอน Login อย่างเดียว ถ้าว่างก็แค่ Login ไม่ผ่าน ระบบไม่พัง ส่วนเรื่องห้ามรหัสว่าง AuthService คอยเช็กให้ตอนสมัครอยู่แล้ว
        }
        return new User(parts[0], parts[1]); // User = part[0]=username , part[1]=password
    }

    // ตรวจ RI
    private void checkRep() {
        assert file != null; // ตัวแปร file ต้องไม่เป็น null
    }
}