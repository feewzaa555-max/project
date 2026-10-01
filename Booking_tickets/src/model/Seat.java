//Seat <- ที่นั่ง 1 ตัวในโรง บอกว่าอยู่แถวไหน เลขที่เท่าไหร่ เป็นประเภทอะไร
/*
    มี 3 ค่า:
    row    → แถว เป็นตัวอักษรภาษาอังกฤษตัวใหญ่ 1 ตัว เช่น 'F'
    number → เลขที่นั่งในแถว เริ่มที่ 1 เช่น 7
    type   → ประเภทที่นั่ง (SeatType) เช่น SeatType.VIP

    มี 1 เมธอดเพิ่ม:
    code() → รหัสที่นั่ง = แถว + เลข เช่น "F7"

    ตัวอย่างการใช้:
        Seat seat = new Seat('F', 7, SeatType.VIP);
        seat.row();                      // 'F'
        seat.number();                   // 7      → หน้าจอเอาไปเขียนบนปุ่มที่นั่ง
        seat.type();                     // VIP    → หน้าจอใช้เลือกสีปุ่ม (VIP สีทอง)
        seat.type().extraPrice();        // 40     → คิดราคา 160 + 40 = 200 บาท
        seat.code();                     // "F7"   → บันทึกลง bookings.csv ว่าจองที่นั่งไหน

        new Seat('a', 7, SeatType.VIP);  // พัง IllegalArgumentException: แถวต้องเป็นตัวใหญ่
        new Seat('F', 0, SeatType.VIP);  // พัง IllegalArgumentException: เลขที่นั่งต้องเริ่มที่ 1

    Seat รู้แค่ตัวเอง ไม่รู้ว่าโรงมีกี่แถว แถวไหนเป็น VIP
    เรื่องผังโรง (6 แถว × 10 ที่, แถว F เป็น VIP) เป็นหน้าที่ของ HallLayout ที่จะทำถัดไป
 */

package model;

// ไม่มี import: ใช้แค่ char, int, String ที่ Java มีให้อยู่แล้ว และ SeatType อยู่ package model เดียวกัน

/**
 * ที่นั่ง 1 ตัวในโรงหนัง
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5 หน้า 9) และได้ equals/hashCode ให้เอง
 * เช่น new Seat('F', 7, VIP).equals(new Seat('F', 7, VIP)) → true
 *
 * @param row    แถว ตัวอักษรภาษาอังกฤษตัวใหญ่ 'A' ถึง 'Z'
 * @param number เลขที่นั่งในแถว ต้องมากกว่า 0
 * @param type   ประเภทที่นั่ง ห้าม null
 */
public record Seat(char row, int number, SeatType type) {

    // AF: ที่นั่งแถว row เลขที่ number ประเภท type เรียกสั้น ๆ ว่า row ต่อด้วย number เช่น F7
    // RI: row อยู่ระหว่าง 'A' ถึง 'Z', number > 0, type ไม่เป็น null
    // Safety from rep exposure: record เป็น private final, char / int แก้ไขไม่ได้, SeatType เป็น enum แก้ไขไม่ได้
    // Thread safety: immutable ค่าไม่เปลี่ยนหลังสร้าง ใช้ข้าม thread ได้ (SC7)

    private static final char FIRST_ROW = 'A'; // แถวแรกที่ยอมรับ
    private static final char LAST_ROW = 'Z';  // แถวสุดท้ายที่ยอมรับ (โรงจริงใช้แค่ A–F แต่ตรงนี้รับได้ถึง Z)
    private static final int FIRST_NUMBER = 1; // เลขที่นั่งเริ่มที่ 1 (ไม่มีที่นั่งเลข 0)

    /**
     * สร้างที่นั่ง 1 ตัว และตรวจค่าก่อนเก็บ
     *
     * วิธีทำงาน:
     *   1. ตรวจ row ว่าอยู่ระหว่าง 'A' ถึง 'Z' ไหม ไม่ใช่ → throw
     *   2. ตรวจ number ว่า >= 1 ไหม ไม่ใช่ → throw
     *   3. ตรวจ type ว่าเป็น null ไหม เป็น → throw
     *   4. ผ่านหมด → เก็บค่าลง field แล้วเรียก checkRep()
     *
     * ใช้ throw ไม่ใช่ assert เพราะค่ามาจากข้างนอก (HallLayout / ไฟล์ CSV) ต้องตรวจเสมอ (SC2 หน้า 10)
     * เขียนแบบเต็ม (มี this.row = row) แทนแบบย่อ เพราะแบบย่อยังไม่ได้เก็บค่าลง field
     * ถ้าเรียก checkRep() ในแบบย่อ มันจะตรวจค่าเริ่มต้น (0 / null) แทนค่าจริงที่ส่งเข้ามา
     *
     * ตัวอย่าง:
     *   new Seat('F', 7, SeatType.VIP)       → ได้ที่นั่ง F7 ประเภท VIP
     *   new Seat('A', 1, SeatType.STANDARD)  → ได้ที่นั่ง A1 ประเภทธรรมดา
     *   new Seat('f', 7, SeatType.VIP)       → throw "row must be A-Z: f"   (ตัวเล็ก)
     *   new Seat('1', 7, SeatType.VIP)       → throw "row must be A-Z: 1"   (ไม่ใช่ตัวอักษร)
     *   new Seat('F', 0, SeatType.VIP)       → throw "number must be >= 1: 0"
     *   new Seat('F', 7, null)               → throw "type must not be null"
     *
     * @param row    แถว 'A' ถึง 'Z'
     * @param number เลขที่นั่ง ตั้งแต่ 1 ขึ้นไป
     * @param type   ประเภทที่นั่ง ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนผิดเงื่อนไขข้างบน
     */
    public Seat(char row, int number, SeatType type) {
        if (row < FIRST_ROW || row > LAST_ROW) {
            throw new IllegalArgumentException("row must be A-Z: " + row);
        }
        if (number < FIRST_NUMBER) {
            throw new IllegalArgumentException("number must be >= 1: " + number);
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.row = row;
        this.number = number;
        this.type = type;
        checkRep();
    }

    /**
     * รหัสที่นั่ง = แถว ต่อด้วย เลข
     *
     * วิธีทำงาน:
     *   1. แปลง row เป็นข้อความก่อนด้วย String.valueOf(row)  เช่น 'F' → "F"
     *   2. เอาข้อความไปต่อกับ number                          เช่น "F" + 7 → "F7"
     *
     * ทำไมต้องแปลงก่อน: ถ้าเขียน row + number ตรง ๆ Java จะเอา char ไปบวกเป็นตัวเลข
     *   'F' มีค่าเป็นเลข 70 → 70 + 7 = 77 ได้ตัวเลข 77 ไม่ใช่ "F7"
     *
     * ตัวอย่าง:
     *   new Seat('F', 7, SeatType.VIP).code()        → "F7"
     *   new Seat('A', 10, SeatType.STANDARD).code()  → "A10"
     *
     * @return รหัสที่นั่ง เช่น "F7"
     */
    public String code() {
        return String.valueOf(row) + number;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     * ถ้า assert ไหนไม่จริง แปลว่าโค้ดใน constructor ผิดเอง ไม่ใช่ผู้ใช้ส่งค่าผิด
     */
    private void checkRep() {
        assert row >= FIRST_ROW && row <= LAST_ROW;
        assert number >= FIRST_NUMBER;
        assert type != null;
    }
}