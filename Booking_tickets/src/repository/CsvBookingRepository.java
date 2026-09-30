//CsvBookingRepository <- อ่าน/เขียนการจองในไฟล์ CSV (ไม่ได้ตรวจกฎการจอง การตรวจอยู่ใน Booking และ BookingService)
/*
    รูปแบบไฟล์ bookings.csv (ไม่มีบรรทัดหัวตาราง, บรรทัดละ 1 การจอง, 7 ช่อง):
        รหัส,ชื่อผู้จอง,รหัสรอบ,ที่นั่ง,ราคารวม,สถานะ,เวลาที่จอง
        B1,somchai,2026-10-01_H1_1000,A1;F7,324,PAID,2026-10-01T09:15
        B2,nok,2026-10-01_H3_1345,C5,160,CANCELLED,2026-10-01T10:02

    ช่องที่นั่งคั่นกันด้วย ; เพราะ , ใช้คั่นช่องไปแล้ว   A1;F7 = 2 ที่ (A1 กับ F7)

    มี 2 เมธอดตาม BookingRepository:
    findAll()      → อ่านทุกแถว แปลงเป็น Booking ยังไม่มีไฟล์คืน []
    save(booking)  → อ่านทุกแถว เจอรหัสเดียวกันแทนที่ ไม่เจอต่อท้าย แล้วเขียนกลับทั้งไฟล์

    ตัวอย่างการใช้:
        BookingRepository bookings = new CsvBookingRepository(Path.of("data", "bookings.csv"));
        bookings.findAll();          // ยังไม่มีไฟล์ → []
        bookings.save(b1);           // สร้างไฟล์ → B1,somchai,2026-10-01_H1_1000,A1;F7,324,PAID,2026-10-01T09:15
        bookings.save(b1.cancel());  // แทนที่แถว B1 → ...,CANCELLED,...
        bookings.findAll();          // [B1 CANCELLED]

    ยังไม่มีไฟล์ก็ได้ (ไฟล์นี้อยู่ใน .gitignore โปรแกรมเขียนเองตอนมีคนจองครั้งแรก) แบบเดียวกับ CsvAccountRepository
    ข้อมูลในไฟล์ผิดจะโยน IOException ที่บอกชื่อไฟล์และเลขบรรทัด แบบเดียวกับ CsvMovieRepository
 */

package repository;

import java.io.IOException;                      // error ตอนอ่าน/เขียนไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import java.nio.charset.StandardCharsets;        // อ่าน/เขียนไฟล์แบบ UTF-8
import java.nio.file.Files;                      // คำสั่งเช็กว่ามีไฟล์ไหม, อ่านทุกบรรทัด, สร้างโฟลเดอร์, เขียนไฟล์
import java.nio.file.Path;                       // เก็บตำแหน่งไฟล์ เช่น data/bookings.csv
import java.time.LocalDateTime;                  // แปลงข้อความ "2026-10-01T09:15" ในไฟล์ให้เป็นวันที่+เวลา
import java.time.format.DateTimeParseException;  // error ตอนวันที่ในไฟล์ผิดรูปแบบ
import java.util.ArrayList;                      // สร้างรายการใหม่ไว้ใส่การจอง / ที่นั่ง / บรรทัดที่จะเขียน
import java.util.List;                           // ชนิดของรายการ
import model.Booking;                            // class การจอง อยู่คนละ package เลยต้อง import
import model.BookingStatus;                      // สถานะการจอง ใช้แปลงข้อความ "PAID" กลับเป็น BookingStatus.PAID

/**
 * เก็บการจองในไฟล์ CSV บรรทัดละ 1 การจอง 7 ช่อง (ไม่มีบรรทัดหัวตาราง, บรรทัดว่างข้ามได้)
 * รหัส,ชื่อผู้จอง,รหัสรอบ,ที่นั่ง(คั่นด้วย ;),ราคารวม,สถานะ,เวลาที่จอง
 */
public class CsvBookingRepository implements BookingRepository {

    // AF: file คือไฟล์ CSV ที่แต่ละบรรทัดคือการจอง 1 ครั้ง เรียงตามลำดับที่จอง
    //     ยังไม่มีไฟล์ = ยังไม่มีใครจองเลย
    // RI: file ไม่เป็น null
    // Safety from rep exposure: file เป็น private final และ Path แก้ไขไม่ได้
    //                           List ที่ findAll คืนออกไปสร้างใหม่ทุกครั้ง แก้แล้วไม่กระทบไฟล์
    //                           Booking ที่คืนออกไปเป็น record แก้ไม่ได้
    // Thread safety: ไม่ได้ใส่ synchronized เพราะเรียกจากหน้าจอ Swing ทีละคำสั่งบน thread เดียว
    //                (แบบเดียวกับ CsvAccountRepository)

    // ลำดับช่องใน bookings.csv (นับจาก 0)
    //   B1 , somchai , 2026-10-01_H1_1000 , A1;F7 , 324 , PAID , 2026-10-01T09:15
    //   0      1              2              3      4      5           6
    /** ช่องที่ 0 = รหัสการจอง */
    private static final int ID = 0;
    /** ช่องที่ 1 = ชื่อผู้จอง */
    private static final int USERNAME = 1;
    /** ช่องที่ 2 = รหัสรอบ */
    private static final int SHOWTIME_ID = 2;
    /** ช่องที่ 3 = ที่นั่ง (คั่นด้วย ;) */
    private static final int SEATS = 3;
    /** ช่องที่ 4 = ราคารวม */
    private static final int TOTAL_PRICE = 4;
    /** ช่องที่ 5 = สถานะ */
    private static final int STATUS = 5;
    /** ช่องที่ 6 = เวลาที่จอง */
    private static final int BOOKED_AT = 6;
    /** 1 บรรทัดต้องมีกี่ช่อง */
    private static final int COLUMNS = 7;

    /** ตัวคั่นช่องในไฟล์ */
    private static final String SEPARATOR = ",";
    /** ตัวคั่นที่นั่งหลายที่ในช่องเดียว เช่น A1;F7 */
    private static final String SEAT_SEPARATOR = ";";

    /** ไฟล์ที่อ่าน/เขียน เช่น data/bookings.csv */
    private final Path file;

    /**
     * เก็บแค่ตำแหน่งไฟล์ ยังไม่เปิดอ่านจนกว่าจะเรียก findAll() หรือ save()
     * ตัวไฟล์ยังไม่มีก็ได้
     *
     * ตัวอย่าง:
     *   new CsvBookingRepository(Path.of("data", "bookings.csv")) → ได้ที่เก็บที่อ่าน/เขียน data/bookings.csv
     *   new CsvBookingRepository(null)                            → throw "file must not be null"
     *
     * @param file ไฟล์ CSV ที่ใช้เก็บการจอง เช่น data/bookings.csv
     * @throws IllegalArgumentException ถ้า file เป็น null
     */
    public CsvBookingRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
        checkRep();
    }

    /**
     * การจองทั้งหมดในไฟล์ เรียงตามบรรทัด
     *
     * วิธีทำงาน:
     *   1. ยังไม่มีไฟล์ → คืนรายการว่าง [] (ยังไม่มีใครจองเลย)
     *   2. อ่านทุกบรรทัด นับเลขบรรทัดไปด้วย (นับบรรทัดว่างด้วย ให้ตรงกับเลขบรรทัดในไฟล์)
     *   3. บรรทัดว่างข้าม / บรรทัดอื่นแปลงเป็น Booking ด้วย parseLine()
     *   4. รหัสซ้ำกับบรรทัดก่อนหน้า → error (1 รหัสต้องมีการจองเดียว)
     *
     * ตัวอย่าง:
     *   ไฟล์มี 2 บรรทัด B1..., B2...  → [Booking B1, Booking B2]
     *   ยังไม่มีไฟล์                  → []
     */
    @Override
    public List<Booking> findAll() throws IOException {
        List<Booking> bookings = new ArrayList<>();
        if (!Files.exists(file)) {
            return bookings;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            Booking booking = parseLine(line, lineNumber);
            for (Booking existing : bookings) {
                if (existing.id().equals(booking.id())) {
                    throw error(lineNumber, "รหัสการจอง " + booking.id() + " ซ้ำกับบรรทัดก่อนหน้า");
                }
            }
            bookings.add(booking);
        }
        return bookings;
    }

    /**
     * บันทึกการจอง รหัสเดียวกันเขียนทับ ไม่มีเพิ่มท้าย
     *
     * วิธีทำงาน:
     *   1. booking เป็น null → throw
     *   2. ชื่อมี , → throw (ถ้าเขียนลงไฟล์ ช่องจะเลื่อน อ่านกลับไม่ได้)
     *   3. อ่านการจองทั้งหมดด้วย findAll()
     *   4. ไล่หารหัสเดียวกัน เจอ → แทนที่ตำแหน่งนั้น / ไม่เจอ → ต่อท้าย
     *   5. แปลงทุกการจองเป็นบรรทัดด้วย toLine() แล้วเขียนทับทั้งไฟล์ (ยังไม่มีโฟลเดอร์ก็สร้างให้)
     *
     * ตัวอย่าง:
     *   ไฟล์ก่อน:  B1,...,PAID,...
     *   save(B2 PAID)       → ไฟล์หลัง: B1,...,PAID,...   B2,...,PAID,...        (ต่อท้าย)
     *   save(B1 CANCELLED)  → ไฟล์หลัง: B1,...,CANCELLED,...   B2,...,PAID,...   (แทนที่ ยังมี 2 แถว)
     */
    @Override
    public void save(Booking booking) throws IOException {
        if (booking == null) {
            throw new IllegalArgumentException("booking must not be null");
        }
        if (booking.username().contains(SEPARATOR)) {
            throw new IllegalArgumentException("username must not contain ,");
        }

        List<Booking> bookings = findAll();

        // หาตำแหน่งการจองรหัสเดียวกัน
        // bookings = [B1, B2]  save(B1 ยกเลิก) → i = 0 ตรง → set(0, ...) แทนที่
        //                      save(B3)       → ไม่ตรงเลย → replaced ยังเป็น false → add ต่อท้าย
        boolean replaced = false;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).id().equals(booking.id())) {
                // bookings.set(i, booking)
                //   i       = ตำแหน่งที่เจอรหัสเดียวกัน เช่น 0
                //   booking = การจองตัวใหม่ที่จะใส่แทน เช่น B1 ที่ยกเลิกแล้ว
                bookings.set(i, booking);
                replaced = true;
                break; // เจอแล้วไม่ต้องหาต่อ (findAll ตรวจแล้วว่ารหัสไม่ซ้ำ)
            }
        }
        if (!replaced) {
            bookings.add(booking);
        }

        List<String> lines = new ArrayList<>();
        for (Booking b : bookings) {
            lines.add(toLine(b));
        }

        // file.getParent() = โฟลเดอร์ที่ไฟล์อยู่ เช่น data/bookings.csv → data
        Path folder = file.getParent();
        if (folder != null) {
            Files.createDirectories(folder);
        }
        // Files.write(file, lines, StandardCharsets.UTF_8)
        //   file  = ไฟล์ที่จะเขียน (ไม่มีก็สร้าง มีแล้วเขียนทับทั้งไฟล์)
        //   lines = ทุกบรรทัด เช่น ["B1,somchai,2026-10-01_H1_1000,A1;F7,324,PAID,2026-10-01T09:15"]
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /**
     * แปลง 1 บรรทัดเป็น Booking
     * เช่น "B1,somchai,2026-10-01_H1_1000,A1;F7,324,PAID,2026-10-01T09:15"
     *      → Booking[B1, somchai, 2026-10-01_H1_1000, [A1, F7], 324, PAID, 2026-10-01T09:15]
     *
     * @param line       ข้อความ 1 บรรทัดจากไฟล์
     * @param lineNumber เลขบรรทัดในไฟล์ ใช้บอกใน error เช่น 2
     * @return การจองที่แปลงได้
     * @throws IOException ถ้าบรรทัดนี้ผิดรูปแบบ
     */
    private Booking parseLine(String line, int lineNumber) throws IOException {
        // 1. ตัดตรง , → ["B1", "somchai", "2026-10-01_H1_1000", "A1;F7", "324", "PAID", "2026-10-01T09:15"]
        //    ใช้ split(",") ธรรมดาได้ เพราะไฟล์นี้ไม่มีช่องไหนว่างได้ (ต่างจาก accounts.csv)
        String[] parts = line.split(SEPARATOR);

        // 2. ต้องได้ครบ 7 ช่อง  "B1,somchai,..." ขาดช่องไหนไป → error
        if (parts.length != COLUMNS) {
            throw error(lineNumber, "ต้องมี " + COLUMNS + " ช่อง คือ รหัส,ชื่อ,รหัสรอบ,ที่นั่ง,ราคา,สถานะ,เวลาที่จอง");
        }

        // 3. ที่นั่ง "A1;F7" → ตัดตรง ; → ["A1", "F7"] → ตัดเว้นวรรคหน้าหลังทีละตัว
        List<String> seatCodes = new ArrayList<>();
        for (String code : parts[SEATS].split(SEAT_SEPARATOR)) {
            seatCodes.add(code.trim());
        }

        // 4. ราคา "324" → 324  /  "3O0" (ตัวโอ) แปลงไม่ได้ → error
        int totalPrice; // ประกาศนอก try เพราะต้องใช้ต่อตอนสร้าง Booking
        try {
            totalPrice = Integer.parseInt(parts[TOTAL_PRICE].trim());
        } catch (NumberFormatException e) {
            throw error(lineNumber, "ราคาต้องเป็นตัวเลข แต่เจอ " + parts[TOTAL_PRICE].trim());
        }

        // 5. สถานะ "PAID" → BookingStatus.PAID
        //    BookingStatus.valueOf("PAYED") หาไม่เจอ → IllegalArgumentException → error
        BookingStatus status;
        try {
            status = BookingStatus.valueOf(parts[STATUS].trim());
        } catch (IllegalArgumentException e) {
            throw error(lineNumber, "สถานะต้องเป็น PAID หรือ CANCELLED แต่เจอ " + parts[STATUS].trim());
        }

        // 6. เวลาที่จอง "2026-10-01T09:15" → 1/10/2026 09:15  /  "1/10/2026" → error
        LocalDateTime bookedAt;
        try {
            bookedAt = LocalDateTime.parse(parts[BOOKED_AT].trim());
        } catch (DateTimeParseException e) {
            throw error(lineNumber, "เวลาที่จองต้องเป็นแบบ yyyy-MM-ddTHH:mm เช่น 2026-10-01T09:15 แต่เจอ " + parts[BOOKED_AT].trim());
        }

        // 7. สร้าง Booking กฎว่าค่าไหนผิด (ว่าง, ที่นั่งซ้ำ, ราคา <= 0) อยู่ใน Booking ที่เดียว
        //    ถ้าผิด Booking โยน IllegalArgumentException → เปลี่ยนเป็น error ที่บอกไฟล์/บรรทัด
        //    "B1,somchai,...,A1;A1,..." บรรทัด 2 → "bookings.csv บรรทัด 2: duplicate seat: A1"
        try {
            return new Booking(parts[ID].trim(), parts[USERNAME].trim(), parts[SHOWTIME_ID].trim(),
                    seatCodes, totalPrice, status, bookedAt);
        } catch (IllegalArgumentException e) {
            throw error(lineNumber, e.getMessage());
        }
    }

    /**
     * แปลง Booking เป็น 1 บรรทัดที่จะเขียนลงไฟล์ (กลับด้านกับ parseLine)
     * Booking[B1, somchai, 2026-10-01_H1_1000, [A1, F7], 324, PAID, 2026-10-01T09:15]
     *   → "B1,somchai,2026-10-01_H1_1000,A1;F7,324,PAID,2026-10-01T09:15"
     *
     * @param booking การจองที่จะแปลง
     * @return ข้อความ 1 บรรทัด
     */
    private String toLine(Booking booking) {
        // String.join(SEAT_SEPARATOR, booking.seatCodes())
        //   SEAT_SEPARATOR       = ";" ตัวที่ใส่คั่นระหว่างที่นั่ง
        //   booking.seatCodes()  = ["A1", "F7"] → ได้ "A1;F7"
        String seats = String.join(SEAT_SEPARATOR, booking.seatCodes());
        // status.name() = ชื่อภาษาอังกฤษ "PAID" (ไม่ใช้ displayName ภาษาไทย เพราะ valueOf อ่านกลับได้แค่ name)
        return booking.id() + SEPARATOR
                + booking.username() + SEPARATOR
                + booking.showtimeId() + SEPARATOR
                + seats + SEPARATOR
                + booking.totalPrice() + SEPARATOR
                + booking.status().name() + SEPARATOR
                + booking.bookedAt();
    }

    /**
     * สร้าง error ที่บอกชื่อไฟล์และเลขบรรทัด
     * error(3, "ราคาต้องเป็นตัวเลข แต่เจอ 3O0") → "bookings.csv บรรทัด 3: ราคาต้องเป็นตัวเลข แต่เจอ 3O0"
     *
     * @param lineNumber เลขบรรทัดที่ผิด เช่น 3
     * @param reason     ผิดเพราะอะไร
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