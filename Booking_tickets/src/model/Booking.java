//Booking <- การจอง 1 ครั้ง (1 แถวใน bookings.csv) ใครจอง รอบไหน ที่นั่งไหน จ่ายเท่าไหร่ จองตอนไหน
/*
    มี 6 ค่า:
    id          → รหัสการจอง เช่น "B1" (BookingService ออกเลขเรียงให้)
    username    → ใครจอง เช่น "somchai"
    showtimeId  → รอบไหน ใช้รหัสจาก Showtime.id() เช่น "2026-10-01_H1_1000" (วันที่_โรง_เวลา)
    seatCodes   → ที่นั่งไหน เช่น ["A1", "F7"]
    totalPrice  → จ่ายไปกี่บาท เช่น 324
    bookedAt    → จองตอนไหน เช่น 2026-10-01T09:15

    ทุกการจองคือจ่ายเงินแล้ว (ตัดเงินในแอปทันทีตอนกดยืนยัน) และยกเลิกไม่ได้
    เหมือนโรงหนังจริงที่ซื้อแล้วไม่คืนเงิน ที่นั่งจะได้ไม่ว่างตอนนาทีสุดท้ายโดยไม่มีใครทันจอง
    จึงไม่มีช่องสถานะ ถ้ามีแถวใน bookings.csv = ที่นั่งเหล่านั้นถูกจองแล้ว

    ตัวอย่างการใช้:
        Booking b = new Booking("B1", "somchai", "2026-10-01_H1_1000",
                                List.of("A1", "F7"), 324,
                                LocalDateTime.of(2026, 10, 1, 9, 15));
        b.seatCodes();          // [A1, F7]  → หน้าประวัติโชว์ "ที่นั่ง A1, F7" / ที่นั่ง A1, F7 ของรอบนี้ไม่ว่าง
        b.totalPrice();         // 324       → หน้าประวัติโชว์ "324 ฿"

    ไม่เก็บ Showtime ทั้งก้อน เก็บแค่รหัสรอบ เพราะต้องเขียนลง CSV ได้
    หน้าประวัติที่ต้องโชว์ชื่อหนัง ให้ BookingService เอารหัสรอบไปหารอบจริงให้
 */

package model;

import java.time.LocalDateTime;  // เวลาที่จอง (วันที่ + เวลา)
import java.util.HashSet;        // ใช้ตรวจที่นั่งซ้ำ (Set ใส่ของซ้ำไม่ได้)
import java.util.List;           // ชนิดของรายการที่นั่ง และใช้ List.copyOf ทำสำเนาแบบแก้ไม่ได้

/**
 * การจอง 1 ครั้ง (จ่ายเงินแล้ว ยกเลิกไม่ได้)
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5 หน้า 9)
 *
 * @param id         รหัสการจอง ห้ามว่าง
 * @param username   ชื่อผู้จอง ห้ามว่าง
 * @param showtimeId รหัสรอบ (จาก Showtime.id()) ห้ามว่าง
 * @param seatCodes  รหัสที่นั่ง อย่างน้อย 1 ที่ ห้ามซ้ำ ห้ามว่าง
 * @param totalPrice ราคารวมที่จ่าย ต้องมากกว่า 0
 * @param bookedAt   เวลาที่จอง ห้าม null
 */
public record Booking(String id, String username, String showtimeId, List<String> seatCodes,
                      int totalPrice, LocalDateTime bookedAt) {

    // AF: การจองรหัส id ของผู้ใช้ username รอบ showtimeId ที่นั่ง seatCodes
    //     จ่ายไปแล้ว totalPrice บาท จองเมื่อ bookedAt
    // RI: id, username, showtimeId ไม่เป็น null และไม่ว่าง
    //     seatCodes ไม่เป็น null, มีอย่างน้อย 1 ตัว, ไม่มีตัวที่ null หรือว่าง, ไม่มีตัวซ้ำ
    //     totalPrice > 0, bookedAt ไม่เป็น null
    // Safety from rep exposure: record เป็น private final
    //     seatCodes ทำสำเนาด้วย List.copyOf ตอนรับเข้า จึงแก้ไม่ได้
    //     คนส่ง List มาแล้วแก้ List เดิมทีหลัง ไม่กระทบ Booking / คนที่ได้ seatCodes() ไปก็แก้ไม่ได้
    //     String / int / LocalDateTime แก้ไขไม่ได้
    // Thread safety: immutable ค่าไม่เปลี่ยนหลังสร้าง ใช้ข้าม thread ได้ (SC7)

    /** ต้องมีที่นั่งอย่างน้อยกี่ที่ */
    private static final int MIN_SEATS = 1;
    /** ราคาต่ำสุดที่ยอมรับ (ต้องจ่ายมากกว่า 0 บาท) */
    private static final int MIN_PRICE = 1;

    /**
     * สร้างการจอง และตรวจค่าก่อนเก็บ
     *
     * วิธีทำงาน:
     *   1. ตรวจ id, username, showtimeId ว่าไม่ว่าง
     *   2. ตรวจ seatCodes: ไม่ null, มีอย่างน้อย 1 ที่, แต่ละตัวไม่ว่าง, ไม่ซ้ำกัน
     *   3. ตรวจ totalPrice > 0 และ bookedAt ไม่ null
     *   4. ผ่านหมด → เก็บค่า (seatCodes เก็บสำเนาแบบแก้ไม่ได้) แล้วเรียก checkRep()
     *
     * ใช้ throw เพราะค่ามาจากข้างนอก เช่น BookingService หรือไฟล์ bookings.csv (SC2 หน้า 10)
     *
     * ตัวอย่าง:
     *   new Booking("B1", "somchai", "2026-10-01_H1_1000", List.of("A1","F7"), 324, เวลา) → ได้
     *   seatCodes = []               → throw "seatCodes must have at least 1 seat"
     *   seatCodes = ["A1","A1"]      → throw "duplicate seat: A1"
     *   totalPrice = 0               → throw "totalPrice must be >= 1: 0"
     *   username = ""                → throw "username must not be blank"
     *
     * @param id         รหัสการจอง ห้ามว่าง
     * @param username   ชื่อผู้จอง ห้ามว่าง
     * @param showtimeId รหัสรอบ ห้ามว่าง
     * @param seatCodes  รหัสที่นั่ง อย่างน้อย 1 ที่ ห้ามซ้ำ
     * @param totalPrice ราคารวม ต้องมากกว่า 0
     * @param bookedAt   เวลาที่จอง ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนผิดเงื่อนไขข้างบน
     */
    public Booking(String id, String username, String showtimeId, List<String> seatCodes,
                   int totalPrice, LocalDateTime bookedAt) {
        requireText(id, "id");
        requireText(username, "username");
        requireText(showtimeId, "showtimeId");
        if (seatCodes == null) {
            throw new IllegalArgumentException("seatCodes must not be null");
        }
        if (seatCodes.size() < MIN_SEATS) {
            throw new IllegalArgumentException("seatCodes must have at least " + MIN_SEATS + " seat");
        }
        // ตรวจที่นั่งซ้ำด้วย HashSet: add คืน false ถ้ามีตัวนั้นอยู่แล้ว
        //   ["A1", "F7", "A1"] → add A1 ได้ true, add F7 ได้ true, add A1 ได้ false → ซ้ำ → throw
        HashSet<String> seen = new HashSet<>();
        for (String code : seatCodes) {
            requireText(code, "seat code");
            if (!seen.add(code)) {
                throw new IllegalArgumentException("duplicate seat: " + code);
            }
        }
        if (totalPrice < MIN_PRICE) {
            throw new IllegalArgumentException("totalPrice must be >= " + MIN_PRICE + ": " + totalPrice);
        }
        if (bookedAt == null) {
            throw new IllegalArgumentException("bookedAt must not be null");
        }
        this.id = id;
        this.username = username;
        this.showtimeId = showtimeId;
        // List.copyOf(seatCodes)
        //   seatCodes = รายการที่คนเรียกส่งมา เช่น ArrayList ["A1", "F7"]
        //   ได้สำเนาใหม่ที่แก้ไม่ได้ คนเรียกไปแก้ ArrayList เดิมทีหลัง Booking ไม่เปลี่ยนตาม
        this.seatCodes = List.copyOf(seatCodes);
        this.totalPrice = totalPrice;
        this.bookedAt = bookedAt;
        checkRep();
    }

    /**
     * ตรวจข้อความว่าไม่เป็น null และไม่ว่าง ใช้ซ้ำหลายช่องใน constructor
     * เช่น requireText("", "username") → throw "username must not be blank"
     *
     * @param value ข้อความที่จะตรวจ
     * @param name  ชื่อช่อง ใช้ในข้อความ error
     * @throws IllegalArgumentException ถ้า value เป็น null หรือว่าง
     */
    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     * ถ้า assert ไหนไม่จริง แปลว่าโค้ดใน constructor ผิดเอง
     */
    private void checkRep() {
        assert id != null && !id.isBlank();
        assert username != null && !username.isBlank();
        assert showtimeId != null && !showtimeId.isBlank();
        assert seatCodes != null && seatCodes.size() >= MIN_SEATS;
        assert new HashSet<>(seatCodes).size() == seatCodes.size();
        assert totalPrice >= MIN_PRICE;
        assert bookedAt != null;
    }
}