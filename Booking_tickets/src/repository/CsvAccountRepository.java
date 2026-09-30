//CsvAccountRepository <- อ่าน/เขียนบัญชีเงินและสมาชิกในไฟล์ CSV (ไม่ได้ตรวจกฎเงิน การตรวจอยู่ใน Account)
/*
    รูปแบบไฟล์ accounts.csv (ไม่มีบรรทัดหัวตาราง, บรรทัดละ 1 คน):
        ชื่อ,เงิน,เวลาหมดสมาชิก
        somchai,500,                     ← ช่องสุดท้ายว่าง = ไม่เคยสมัครสมาชิก
        thanawat,201,2027-10-29T06:00    ← สมาชิกหมด 29/10/2027 06:00 (T คือตัวคั่นวันที่กับเวลา)

    มี 2 เมธอดตาม AccountRepository:
    findByUsername(username) → อ่านทุกแถว หาแถวที่ชื่อตรง แปลงเป็น Account ไม่เจอคืน null
    save(account)            → อ่านทุกแถว เจอชื่อเดียวกันแทนที่ ไม่เจอต่อท้าย แล้วเขียนกลับทั้งไฟล์

    ตัวอย่างการใช้:
        AccountRepository accounts = new CsvAccountRepository(Path.of("data", "accounts.csv"));
        accounts.findByUsername("somchai");                          // ยังไม่มีไฟล์ → null
        accounts.save(new Account("somchai", 300, null));            // สร้างไฟล์ใหม่  → somchai,300,
        accounts.save(new Account("somchai", 500, null));            // แทนที่แถวเดิม → somchai,500,
        accounts.findByUsername("somchai");                          // Account[somchai, 500, null]

    ยังไม่มีไฟล์ก็ได้ (ตอน clone มาใหม่ ไฟล์นี้ไม่มีเพราะอยู่ใน .gitignore)
    findByUsername จะคืน null และ save ครั้งแรกจะสร้างไฟล์ให้เอง แบบเดียวกับ CsvUserRepository
    ข้อมูลในไฟล์ผิดจะโยน IOException ที่บอกชื่อไฟล์และเลขบรรทัด แบบเดียวกับ CsvMovieRepository
 */

package repository;

import java.io.IOException;                      // error ตอนอ่าน/เขียนไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import java.nio.charset.StandardCharsets;        // อ่าน/เขียนไฟล์แบบ UTF-8
import java.nio.file.Files;                      // คำสั่งเช็กว่ามีไฟล์ไหม, อ่านทุกบรรทัด, สร้างโฟลเดอร์, เขียนไฟล์
import java.nio.file.Path;                       // เก็บตำแหน่งไฟล์ เช่น data/accounts.csv
import java.time.LocalDateTime;                  // แปลงข้อความ "2027-10-29T06:00" ในไฟล์ให้เป็นวันที่+เวลา
import java.time.format.DateTimeParseException;  // error ตอนวันที่ในไฟล์ผิดรูปแบบ เช่น "29/10/2027"
import java.util.ArrayList;                      // สร้างรายการใหม่ไว้ใส่บัญชี / บรรทัดที่จะเขียน
import java.util.List;                           // ชนิดของรายการ
import model.Account;                            // class บัญชี อยู่คนละ package เลยต้อง import

/**
 * เก็บบัญชีในไฟล์ CSV บรรทัดละ 1 คน รูปแบบ ชื่อ,เงิน,เวลาหมดสมาชิก (ไม่มีบรรทัดหัวตาราง, บรรทัดว่างข้ามได้)
 * ช่องเวลาหมดสมาชิกว่าง = ไม่เคยสมัคร
 */
public class CsvAccountRepository implements AccountRepository {

    // AF: file คือไฟล์ CSV ที่แต่ละบรรทัดแทนบัญชี 1 คน ถ้ายังไม่มีไฟล์ แปลว่ายังไม่มีบัญชีเลย
    // RI: file ไม่เป็น null
    // Safety from rep exposure: file เป็น private final และ Path แก้ไขไม่ได้
    //                           Account ที่คืนออกไปเป็น record แก้ไม่ได้
    // Thread safety: ไม่ได้ใส่ synchronized เพราะเรียกจากหน้าจอ Swing ซึ่งทำงานทีละคำสั่งบน thread เดียว
    //                (แบบเดียวกับ CsvUserRepository)

    // ลำดับช่องใน accounts.csv (นับจาก 0)  thanawat , 201 , 2027-10-29T06:00
    //                                    0        1          2
    /** ช่องที่ 0 = ชื่อ */
    private static final int USERNAME = 0;
    /** ช่องที่ 1 = เงิน */
    private static final int BALANCE = 1;
    /** ช่องที่ 2 = เวลาหมดสมาชิก */
    private static final int MEMBER_UNTIL = 2;
    /** 1 บรรทัดต้องมีกี่ช่อง */
    private static final int COLUMNS = 3;

    /** ตัวคั่นช่องในไฟล์ */
    private static final String SEPARATOR = ",";

    /**
     * ส่งให้ split เป็นตัวที่ 2 เพื่อ "ไม่ทิ้งช่องว่างท้ายบรรทัด"
     * ต่างจาก CsvMovieRepository ที่ใช้ split(",") เฉย ๆ เพราะไฟล์นี้ช่องสุดท้ายว่างได้จริง (ไม่เคยสมัครสมาชิก)
     *   "somchai,500,".split(",")     → ["somchai", "500"]        2 ช่อง ← split ทิ้งช่องว่างท้าย นับผิด
     *   "somchai,500,".split(",", -1) → ["somchai", "500", ""]    3 ช่อง ← ถูก ช่องที่ 3 เป็นข้อความว่าง
     */
    private static final int KEEP_EMPTY_FIELDS = -1;

    /** ไฟล์ที่อ่าน/เขียน เช่น data/accounts.csv */
    private final Path file;

    /**
     * เก็บแค่ตำแหน่งไฟล์ ยังไม่เปิดอ่านจนกว่าจะเรียก findByUsername() หรือ save()
     * ตัวไฟล์ยังไม่มีก็ได้
     *
     * ตัวอย่าง:
     *   new CsvAccountRepository(Path.of("data", "accounts.csv")) → ได้ที่เก็บที่อ่าน/เขียน data/accounts.csv
     *   new CsvAccountRepository(null)                            → throw "file must not be null"
     *
     * @param file ไฟล์ CSV ที่ใช้เก็บบัญชี เช่น data/accounts.csv
     * @throws IllegalArgumentException ถ้า file เป็น null
     */
    public CsvAccountRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
        checkRep();
    }

    /**
     * หาบัญชีจากชื่อ (ตัวพิมพ์ต้องตรง)
     *
     * วิธีทำงาน:
     *   1. username เป็น null → throw
     *   2. อ่านบัญชีทั้งหมดในไฟล์ด้วย readAll()
     *   3. ไล่ทีละบัญชี ชื่อตรงคืนบัญชีนั้นทันที
     *   4. ไล่หมดไม่เจอ → คืน null
     *
     * ตัวอย่าง (ไฟล์มี somchai,500,  และ  thanawat,201,2027-10-29T06:00):
     *   findByUsername("thanawat") → Account[thanawat, 201, 2027-10-29T06:00]
     *   findByUsername("somchai")  → Account[somchai, 500, null]
     *   findByUsername("Somchai")  → null (ตัวพิมพ์ไม่ตรง)
     *   findByUsername("nobody")   → null
     *   ยังไม่มีไฟล์              → null ทุกชื่อ
     */
    @Override
    public Account findByUsername(String username) throws IOException {
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        for (Account account : readAll()) {
            if (account.username().equals(username)) {
                return account;
            }
        }
        return null;
    }

    /**
     * บันทึกบัญชี ชื่อเดียวกันเขียนทับ ไม่มีเพิ่มท้าย
     *
     * วิธีทำงาน:
     *   1. account เป็น null → throw
     *   2. ชื่อมี , → throw (ถ้าเขียนลงไฟล์ ช่องจะเลื่อน ไฟล์พัง)
     *   3. อ่านบัญชีทั้งหมดในไฟล์ด้วย readAll()
     *   4. ไล่หาบัญชีชื่อเดียวกัน เจอ → แทนที่ตำแหน่งนั้นด้วย account ใหม่
     *                             ไม่เจอ → ต่อท้ายรายการ
     *   5. แปลงทุกบัญชีเป็นบรรทัดข้อความด้วย toLine()
     *   6. ยังไม่มีโฟลเดอร์ data ก็สร้างให้ แล้วเขียนทุกบรรทัดทับไฟล์เดิม
     *
     * ตัวอย่าง:
     *   ไฟล์ก่อน:  somchai,300,
     *   save(Account[somchai, 500, null])
     *   ไฟล์หลัง:  somchai,500,                          ← แทนที่ (ยังมี 1 แถว)
     *
     *   ไฟล์ก่อน:  somchai,500,
     *   save(Account[thanawat, 201, 2027-10-29T06:00])
     *   ไฟล์หลัง:  somchai,500,
     *              thanawat,201,2027-10-29T06:00          ← ต่อท้าย
     */
    @Override
    public void save(Account account) throws IOException {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        if (account.username().contains(SEPARATOR)) {
            throw new IllegalArgumentException("username must not contain ,");
        }

        List<Account> accounts = readAll();

        // หาตำแหน่งบัญชีชื่อเดียวกัน
        // accounts = [somchai, thanawat]  save(somchai...) → i = 0 ตรง → set(0, ...) แทนที่
        //                                 save(nok...)     → ไม่ตรงเลย → replaced ยังเป็น false → add ต่อท้าย
        boolean replaced = false;
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).username().equals(account.username())) {
                // accounts.set(i, account)
                //   i       = ตำแหน่งที่เจอชื่อเดียวกัน เช่น 0
                //   account = บัญชีใหม่ที่จะใส่แทนของเดิม เช่น Account[somchai, 500, null]
                accounts.set(i, account);
                replaced = true;
                break; // เจอแล้วไม่ต้องหาต่อ (ในไฟล์มีชื่อละแถวเดียว readAll ตรวจไว้แล้ว)
            }
        }
        if (!replaced) {
            accounts.add(account);
        }

        // แปลงบัญชีเป็นบรรทัด [Account[somchai,500,null], ...] → ["somchai,500,", ...]
        List<String> lines = new ArrayList<>();
        for (Account a : accounts) {
            lines.add(toLine(a));
        }

        // file.getParent() = โฟลเดอร์ที่ไฟล์อยู่ เช่น data/accounts.csv → data
        // ถ้ายังไม่มีโฟลเดอร์ createDirectories สร้างให้ (มีแล้วก็ไม่ทำอะไร)
        Path folder = file.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        // Files.write(file, lines, StandardCharsets.UTF_8)
        //   file   = ไฟล์ที่จะเขียน เช่น data/accounts.csv (ไม่มีก็สร้าง มีแล้วเขียนทับทั้งไฟล์)
        //   lines  = ทุกบรรทัดที่จะเขียน เช่น ["somchai,500,", "thanawat,201,2027-10-29T06:00"]
        //   UTF_8  = รูปแบบตัวอักษร
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * อ่านบัญชีทั้งหมดในไฟล์
     * วิธีทำงาน:
     *   1. ยังไม่มีไฟล์ → คืนรายการว่าง [] (ยังไม่มีใครมีบัญชี)
     *   2. อ่านทุกบรรทัด นับเลขบรรทัดไปด้วย (นับบรรทัดว่างด้วย ให้ตรงกับเลขบรรทัดในไฟล์)
     *   3. บรรทัดว่างข้าม / บรรทัดอื่นแปลงเป็น Account ด้วย parseLine()
     *   4. ชื่อซ้ำกับบรรทัดก่อนหน้า → error (1 ชื่อต้องมีบัญชีเดียว ไม่งั้นไม่รู้ว่าเงินแถวไหนถูก)
     * เช่น ไฟล์มี 2 บรรทัด somchai,500, / thanawat,201,2027-10-29T06:00
     *      → [Account[somchai, 500, null], Account[thanawat, 201, 2027-10-29T06:00]]
     *
     * @return บัญชีทุกคนในไฟล์ เรียงตามบรรทัด หรือรายการว่างถ้ายังไม่มีไฟล์
     * @throws IOException ถ้าอ่านไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด (บอกเลขบรรทัด)
     */
    private List<Account> readAll() throws IOException {
        List<Account> accounts = new ArrayList<>();
        if (!Files.exists(file)) { //ไม่มีไฟล์ return accounts
            return accounts;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            Account account = parseLine(line, lineNumber);
            for (Account existing : accounts) {
                if (existing.username().equals(account.username())) {
                    throw error(lineNumber, "ชื่อ " + account.username() + " ซ้ำกับบรรทัดก่อนหน้า");
                }
            }
            accounts.add(account);
        }
        return accounts;
    }

    /**
     * แปลง 1 บรรทัดเป็น Account
     * เช่น "thanawat,201,2027-10-29T06:00" → Account[thanawat, 201, 2027-10-29T06:00]
     *      "somchai,500,"                  → Account[somchai, 500, null]
     *
     * @param line       ข้อความ 1 บรรทัดจากไฟล์ เช่น "somchai,500,"
     * @param lineNumber เลขบรรทัดในไฟล์ ใช้บอกใน error เช่น 2
     * @return บัญชีที่แปลงได้
     * @throws IOException ถ้าบรรทัดนี้ผิดรูปแบบ
     */
    private Account parseLine(String line, int lineNumber) throws IOException {
        // 1. ตัดตรง , โดยไม่ทิ้งช่องว่างท้าย (ดูคำอธิบาย KEEP_EMPTY_FIELDS ข้างบน)
        //    line.split(SEPARATOR, KEEP_EMPTY_FIELDS)
        //      SEPARATOR         = "," ตัดตรงนี้
        //      KEEP_EMPTY_FIELDS = -1 เก็บช่องว่างท้ายไว้
        //    "somchai,500," → ["somchai", "500", ""]
        String[] parts = line.split(SEPARATOR, KEEP_EMPTY_FIELDS);

        // 2. ต้องได้ครบ 3 ช่อง
        //    "somchai,500"      → 2 ช่อง error (ลืม , ตัวสุดท้าย)
        //    "somchai,500,,x"   → 4 ช่อง error
        if (parts.length != COLUMNS) {
            throw error(lineNumber, "ต้องมี " + COLUMNS + " ช่อง คือ ชื่อ,เงิน,เวลาหมดสมาชิก (ไม่เป็นสมาชิกให้เว้นว่าง)");
        }

        // 3. เงิน "500" → 500  /  "5OO" (ตัวโอ) แปลงไม่ได้ → error
        int balance; // ประกาศนอก try เพราะต้องใช้ต่อตอนสร้าง Account
        try {
            balance = Integer.parseInt(parts[BALANCE].trim());
        } catch (NumberFormatException e) {
            throw error(lineNumber, "เงินต้องเป็นตัวเลข แต่เจอ " + parts[BALANCE].trim());
        }

        // 4. เวลาหมดสมาชิก
        //    ""                  → null (ไม่เคยสมัคร)
        //    "2027-10-29T06:00"  → 29/10/2027 06:00
        //    "29/10/2027"        → แปลงไม่ได้ → error
        String untilText = parts[MEMBER_UNTIL].trim();
        LocalDateTime memberUntil = null; // เริ่มที่ null = ไม่เคยสมัคร
        if (!untilText.isEmpty()) {
            try {
                memberUntil = LocalDateTime.parse(untilText);
            } catch (DateTimeParseException e) {
                throw error(lineNumber, "เวลาหมดสมาชิกต้องเป็นแบบ yyyy-MM-ddTHH:mm เช่น 2027-10-29T06:00 แต่เจอ " + untilText);
            }
        }

        // 5. สร้าง Account กฎว่าค่าไหนผิด (ชื่อว่าง, เงินติดลบ) อยู่ใน Account ที่เดียว
        //    ถ้าผิด Account โยน IllegalArgumentException → เปลี่ยนเป็น error ที่บอกไฟล์/บรรทัด
        //    "somchai,-5," บรรทัด 2 → "accounts.csv บรรทัด 2: balance must be >= 0: -5"
        try {
            return new Account(parts[USERNAME].trim(), balance, memberUntil);
        } catch (IllegalArgumentException e) {
            throw error(lineNumber, e.getMessage());
        }
    }

    /**
     * แปลง Account เป็น 1 บรรทัดที่จะเขียนลงไฟล์ (กลับด้านกับ parseLine)
     * Account[somchai, 500, null]                  → "somchai,500,"
     * Account[thanawat, 201, 2027-10-29T06:00]     → "thanawat,201,2027-10-29T06:00"
     * memberUntil.toString() ของ LocalDateTime ได้ข้อความแบบ 2027-10-29T06:00 ซึ่ง LocalDateTime.parse อ่านกลับได้พอดี
     *
     * @param account บัญชีที่จะแปลง เช่น Account[somchai, 500, null]
     * @return ข้อความ 1 บรรทัด เช่น "somchai,500,"
     */
    private String toLine(Account account) {
        // (เงื่อนไข) ? ค่าถ้าจริง : ค่าถ้าไม่จริง  คือ if-else แบบสั้นในบรรทัดเดียว
        //   memberUntil เป็น null     → ได้ ""                  (ช่องสุดท้ายว่าง)
        //   memberUntil = 29/10 06:00 → ได้ "2027-10-29T06:00"
        String untilText = (account.memberUntil() == null) ? "" : account.memberUntil().toString();
        return account.username() + SEPARATOR + account.balance() + SEPARATOR + untilText;
    }

    /**
     * สร้าง error ที่บอกชื่อไฟล์และเลขบรรทัด
     * error(3, "เงินต้องเป็นตัวเลข แต่เจอ 5OO") → "accounts.csv บรรทัด 3: เงินต้องเป็นตัวเลข แต่เจอ 5OO"
     *
     * @param lineNumber เลขบรรทัดที่ผิด เช่น 3
     * @param reason     ผิดเพราะอะไร เช่น "เงินต้องเป็นตัวเลข แต่เจอ 5OO"
     * @return IOException ที่ข้อความบอกชื่อไฟล์ เลขบรรทัด และเหตุผล
     */
    private IOException error(int lineNumber, String reason) {
        return new IOException(file.getFileName() + " บรรทัด " + lineNumber + ": " + reason);
    }

    /**
     * ตรวจ RI
     */
    private void checkRep() {
        assert file != null;
    }
}