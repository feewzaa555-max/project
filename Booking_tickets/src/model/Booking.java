//Booking <- การจอง 1 ครั้ง (1 แถวใน bookings.csv) ใครจอง รอบไหน ที่นั่งไหน จ่ายเท่าไหร่ สถานะอะไร จองตอนไหน
/*
    มี 7 ค่า:
    id          → รหัสการจอง เช่น "B1" (BookingService ออกเลขเรียงให้)
    username    → ใครจอง เช่น "somchai"
    showtimeId  → รอบไหน ใช้รหัสจาก Showtime.id() เช่น "2026-10-01_H1_1000" (วันที่_โรง_เวลา)
    seatCodes   → ที่นั่งไหน เช่น ["A1", "F7"]
    totalPrice  → จ่ายไปกี่บาท เช่น 324
    status      → PAID (ชำระแล้ว) หรือ CANCELLED (ยกเลิกแล้ว)
    bookedAt    → จองตอนไหน เช่น 2026-10-01T09:15

    มี 2 เมธอดเพิ่ม:
    isActive() → true ถ้ายัง PAID แปลว่าที่นั่งยังถูกจองอยู่
    cancel()   → คืน Booking ตัวใหม่ที่เป็น CANCELLED (ตัวเดิมไม่เปลี่ยน)

    ตัวอย่างการใช้:
        Booking b = new Booking("B1", "somchai", "2026-10-01_H1_1000",
                                List.of("A1", "F7"), 324, BookingStatus.PAID,
                                LocalDateTime.of(2026, 10, 1, 9, 15));
        b.seatCodes();          // [A1, F7]  → หน้าประวัติโชว์ "ที่นั่ง A1, F7"
        b.isActive();           // true      → ที่นั่ง A1, F7 ของรอบนี้ไม่ว่าง
        b = b.cancel();         // Booking ตัวใหม่ status = CANCELLED
        b.isActive();           // false     → ที่นั่ง A1, F7 กลับมาว่าง
        b.cancel();             // พัง IllegalStateException: ยกเลิกไปแล้ว

    ไม่เก็บ Showtime ทั้งก้อน เก็บแค่รหัสรอบ เพราะต้องเขียนลง CSV ได้
    หน้าประวัติที่ต้องโชว์ชื่อหนัง ให้ BookingService เอารหัสรอบไปหารอบจริงให้
 */

package model;

import java.time.LocalDateTime;  // เวลาที่จอง (วันที่ + เวลา)
import java.util.HashSet;        // ใช้ตรวจที่นั่งซ้ำ (Set ใส่ของซ้ำไม่ได้)
import java.util.List;           // ชนิดของรายการที่นั่ง และใช้ List.copyOf ทำสำเนาแบบแก้ไม่ได้

/**
 * การจอง 1 ครั้ง
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5 หน้า 9)
 * ยกเลิกด้วย cancel() ได้ตัวใหม่กลับมา (producer SC4 หน้า 16)
 *
 * @param id         รหัสการจอง ห้ามว่าง
 * @param username   ชื่อผู้จอง ห้ามว่าง
 * @param showtimeId รหัสรอบ (จาก Showtime.id()) ห้ามว่าง
 * @param seatCodes  รหัสที่นั่ง อย่างน้อย 1 ที่ ห้ามซ้ำ ห้ามว่าง
 * @param totalPrice ราคารวมที่จ่าย ต้องมากกว่า 0
 * @param status     สถานะ ห้าม null
 * @param bookedAt   เวลาที่จอง ห้าม null
 */
public record Booking(String id, String username, String showtimeId, List<String> seatCodes,
                      int totalPrice, BookingStatus status, LocalDateTime bookedAt) {

    // AF: การจองรหัส id ของผู้ใช้ username รอบ showtimeId ที่นั่ง seatCodes
    //     จ่าย totalPrice บาท สถานะ status จองเมื่อ bookedAt
    // RI: id, username, showtimeId ไม่เป็น null และไม่ว่าง
    //     seatCodes ไม่เป็น null, มีอย่างน้อย 1 ตัว, ไม่มีตัวที่ null หรือว่าง, ไม่มีตัวซ้ำ
    //     totalPrice > 0, status และ bookedAt ไม่เป็น null
    // Safety from rep exposure: record เป็น private final
    //     seatCodes ทำสำเนาด้วย List.copyOf ตอนรับเข้า จึงแก้ไม่ได้
    //     คนส่ง List มาแล้วแก้ List เดิมทีหลัง ไม่กระทบ Booking / คนที่ได้ seatCodes() ไปก็แก้ไม่ได้
    //     String / int / BookingStatus / LocalDateTime แก้ไขไม่ได้
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
     *   3. ตรวจ totalPrice > 0, status และ bookedAt ไม่ null
     *   4. ผ่านหมด → เก็บค่า (seatCodes เก็บสำเนาแบบแก้ไม่ได้) แล้วเรียก checkRep()
     *
     * ใช้ throw เพราะค่ามาจากข้างนอก เช่น BookingService หรือไฟล์ bookings.csv (SC2 หน้า 10)
     *
     * ตัวอย่าง:
     *   new Booking("B1", "somchai", "2026-10-01_H1_1000", List.of("A1","F7"), 324, PAID, เวลา) → ได้
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
     * @param status     สถานะ ห้าม null
     * @param bookedAt   เวลาที่จอง ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนผิดเงื่อนไขข้างบน
     */
    public Booking(String id, String username, String showtimeId, List<String> seatCodes,
                   int totalPrice, BookingStatus status, LocalDateTime bookedAt) {
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
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
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
        this.status = status;
        this.bookedAt = bookedAt;
        checkRep();
    }

    /**
     * การจองนี้ยังจองที่นั่งอยู่ไหม
     *
     * วิธีทำงาน: status เป็น PAID → true / CANCELLED → false
     *
     * ตัวอย่าง:
     *   Booking สถานะ PAID      → true  (ที่นั่ง A1, F7 ของรอบนี้ไม่ว่าง)
     *   Booking สถานะ CANCELLED → false (ที่นั่งกลับมาว่าง)
     *
     * @return true ถ้าสถานะเป็น PAID
     */
    public boolean isActive() {
        return status == BookingStatus.PAID;
    }

    /**
     * ยกเลิกการจอง
     * ได้ Booking ตัวใหม่ที่ status เป็น CANCELLED กลับมา ตัวเดิมไม่เปลี่ยน ค่าอื่นเหมือนเดิมทุกช่อง
     * การคืนเงินและเช็กว่ายังยกเลิกได้ไหม (รอบเริ่มหรือยัง) เป็นหน้าที่ของ BookingService
     *
     * วิธีทำงาน:
     *   1. ถ้ายกเลิกไปแล้ว → throw (กันคืนเงินซ้ำ 2 รอบ)
     *   2. สร้าง Booking ตัวใหม่ ทุกช่องเหมือนเดิม ยกเว้น status = CANCELLED
     *
     * ตัวอย่าง:
     *   Booking[B1, ..., PAID, ...].cancel()      → Booking[B1, ..., CANCELLED, ...]
     *   Booking[B1, ..., CANCELLED, ...].cancel() → throw "booking B1 is already cancelled"
     *
     * @return Booking ตัวใหม่ที่ status = CANCELLED
     * @throws IllegalStateException ถ้าการจองนี้ถูกยกเลิกไปแล้ว
     */
    public Booking cancel() {
        if (!isActive()) {
            throw new IllegalStateException("booking " + id + " is already cancelled");
        }
        // new Booking(... BookingStatus.CANCELLED ...)
        //   ทุกช่องส่งค่าเดิม ยกเว้น status ส่ง BookingStatus.CANCELLED
        return new Booking(id, username, showtimeId, seatCodes, totalPrice, BookingStatus.CANCELLED, bookedAt);
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
        assert status != null && bookedAt != null;
    }
}