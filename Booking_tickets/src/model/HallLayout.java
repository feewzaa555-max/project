//HallLayout <- ผังที่นั่งในโรง สร้างที่นั่งครบทุกตัว และเป็นที่เดียวที่รู้ว่าโรงมีกี่แถว แถวละกี่ที่ แถวไหนเป็น VIP
/*
    ผังที่ตกลงกันไว้ (ทั้ง 3 โรงเหมือนกัน):
        A  1  2  3  4  5  |  6  7  8  9 10     ← ธรรมดา
        B  1  2  3  4  5  |  6  7  8  9 10     ← ธรรมดา
        C  1  2  3  4  5  |  6  7  8  9 10     ← ธรรมดา
        D  1  2  3  4  5  |  6  7  8  9 10     ← ธรรมดา
        E  1  2  3  4  5  |  6  7  8  9 10     ← ธรรมดา
        F  1  2  3  4  5  |  6  7  8  9 10     ← VIP
                          ↑ ทางเดิน (หลังที่นั่งเลข 5)
        รวม 6 แถว × 10 ที่ = 60 ที่นั่ง

    มี 4 เมธอด:
    rows()            → รายชื่อแถว ['A','B','C','D','E','F']
    seatsInRow(row)   → ที่นั่งทั้งหมดในแถวนั้น เรียงเลข 1 ถึง 10
    aisleAfter()      → ทางเดินอยู่หลังที่นั่งเลขไหน (5)
    findSeat(code)    → แปลงรหัสเช่น "F7" กลับเป็น Seat ถ้าไม่มีที่นั่งนี้จริง throw

    ตัวอย่างการใช้:
        HallLayout layout = new HallLayout();

        // หน้าจอวาดผังที่นั่ง
        for (char row : layout.rows()) {                    // วนแถว A ถึง F
            for (Seat seat : layout.seatsInRow(row)) {      // วนที่นั่งเลข 1 ถึง 10 ในแถวนั้น
                // วาดปุ่ม 1 ปุ่ม เขียน seat.number() บนปุ่ม ถ้า seat.type() เป็น VIP ใช้สีทอง
                if (seat.number() == layout.aisleAfter()) {
                    // เว้นช่องว่างเป็นทางเดิน
                }
            }
        }

        // หลังบ้านตอนจอง: ผู้ใช้กดที่นั่ง "F7"
        Seat seat = layout.findSeat("F7");              // Seat F7 ประเภท VIP
        int price = 160 + seat.type().extraPrice();     // 160 + 40 = 200

        layout.findSeat("G3");                          // พัง IllegalArgumentException: ไม่มีแถว G
        layout.findSeat("A11");                         // พัง IllegalArgumentException: แถวละ 10 ที่เท่านั้น
 */

package model;

import java.util.ArrayList; // ใช้ ArrayList เป็นรายการชั่วคราวตอนสร้างที่นั่ง (ยังเพิ่มของได้) ก่อนแปลงเป็นแบบแก้ไม่ได้
import java.util.HashMap;   // ใช้ HashMap เป็นตารางชั่วคราวตอนสร้าง จับคู่ รหัส → ที่นั่ง และ แถว → ที่นั่งในแถว
import java.util.List;      // ชนิดของรายการ ใช้กับ rows และที่นั่งในแต่ละแถว และใช้ List.copyOf ทำสำเนาแบบแก้ไม่ได้
import java.util.Map;       // ชนิดของตารางจับคู่ ใช้เก็บ seatsByRow / seatsByCode และใช้ Map.copyOf ทำสำเนาแบบแก้ไม่ได้

/**
 * ผังที่นั่งในโรงหนัง (ทุกโรงใช้ผังเดียวกัน)
 * 6 แถว (A ถึง F) แถวละ 10 ที่ แถว F เป็น VIP ทางเดินอยู่หลังที่นั่งเลข 5
 * ตัวเลขผังทั้งหมดอยู่ที่ไฟล์นี้ที่เดียว อยากเปลี่ยนผังแก้ค่าคงที่ข้างล่างจบ (SC2 magic number)
 * เป็น final class และแก้ไขไม่ได้หลังสร้าง (SC5 หน้า 9)
 */
public final class HallLayout {

    // AF: ผังโรงที่มีแถวตามลำดับใน rows
    //     แต่ละแถว r มีที่นั่งตามลำดับใน seatsByRow.get(r)
    //     seatsByCode คือตารางค้นที่นั่งจากรหัส เช่น "F7" → ที่นั่ง F7
    // RI: rows มี ROW_COUNT แถว เรียงจาก FIRST_ROW ทีละตัวอักษร (A, B, C, ...)
    //     ทุกแถวมีที่นั่ง SEATS_PER_ROW ตัว เลข 1 ถึง SEATS_PER_ROW เรียงกัน และ row ของที่นั่งตรงกับแถว
    //     ที่นั่งแถว VIP_ROW เป็น VIP ที่เหลือเป็น STANDARD
    //     seatsByCode มีที่นั่งครบ ROW_COUNT × SEATS_PER_ROW ตัว และรหัส (key) ตรงกับ seat.code()
    //     AISLE_AFTER อยู่ระหว่าง 1 ถึง SEATS_PER_ROW - 1 (ทางเดินต้องอยู่ระหว่างที่นั่ง ไม่ใช่ขอบ)
    // Safety from rep exposure: field เป็น private final
    //     rows / seatsByRow / seatsByCode และ list ในแต่ละแถว ทำด้วย List.copyOf / Map.copyOf จึงแก้ไม่ได้
    //     คืนออกไปตรง ๆ ได้ ใครเรียก .add() / .clear() จะโดน UnsupportedOperationException
    //     Seat เป็น record ที่แก้ไม่ได้ Character แก้ไม่ได้
    // Thread safety: สร้างเสร็จแล้วไม่มีอะไรเปลี่ยน ใช้ข้าม thread ได้ (SC7)

    /** แถวแรก */
    private static final char FIRST_ROW = 'A';
    /** จำนวนแถว → A B C D E F */
    private static final int ROW_COUNT = 6;
    /** เลขที่นั่งแรกในแถว */
    private static final int FIRST_SEAT_NUMBER = 1;
    /** แถวละกี่ที่นั่ง */
    private static final int SEATS_PER_ROW = 10;
    /** แถวที่เป็น VIP */
    private static final char VIP_ROW = 'F';
    /** ทางเดินอยู่หลังที่นั่งเลขนี้ */
    private static final int AISLE_AFTER = 5;

    /** แถวเรียงตามลำดับ เช่น ['A','B','C','D','E','F'] */
    private final List<Character> rows;
    /** แถว → ที่นั่งในแถว เช่น 'F' → [F1, F2, ..., F10] */
    private final Map<Character, List<Seat>> seatsByRow;
    /** รหัส → ที่นั่ง เช่น "F7" → Seat F7 VIP */
    private final Map<String, Seat> seatsByCode;

    /**
     * สร้างผังโรงพร้อมที่นั่งครบ 60 ตัว
     *
     * วิธีทำงาน:
     *   1. เตรียมรายการ / ตารางชั่วคราว 3 อัน (ยังเพิ่มของได้)
     *   2. วนแถวทีละแถว i = 0 ถึง 5
     *      2.1 หาตัวอักษรแถว = FIRST_ROW + i     เช่น i = 0 → 'A', i = 5 → 'F'
     *      2.2 ถ้าเป็นแถว VIP_ROW ใช้ประเภท VIP ไม่ใช่ใช้ STANDARD
     *      2.3 วนเลขที่นั่ง 1 ถึง 10 สร้าง Seat ใส่รายการของแถว และใส่ตาราง รหัส → ที่นั่ง
     *      2.4 เก็บแถวนี้ลงรายชื่อแถว และเก็บรายการที่นั่งของแถวนี้ลงตาราง แถว → ที่นั่ง
     *   3. แปลงของชั่วคราวทั้งหมดเป็นสำเนาแบบแก้ไม่ได้ แล้วเก็บลง field
     *   4. เรียก checkRep() ตรวจว่าผังถูกต้อง
     *
     * ตัวอย่างผลลัพธ์:
     *   rows        = ['A','B','C','D','E','F']
     *   seatsByRow  = {'A' → [A1 ... A10 ธรรมดา], ..., 'F' → [F1 ... F10 VIP]}
     *   seatsByCode = {"A1" → A1, "A2" → A2, ..., "F10" → F10}   (60 คู่)
     */
    public HallLayout() {
        List<Character> rowList = new ArrayList<>();
        Map<Character, List<Seat>> byRow = new HashMap<>();
        Map<String, Seat> byCode = new HashMap<>();

        for (int i = 0; i < ROW_COUNT; i++) {
            // char บวก int ได้ผลเป็น int ('A' คือเลข 65 → 65 + 2 = 67) ต้องแปลงกลับเป็น char ด้วย (char)
            // เช่น i = 2 → (char) ('A' + 2) → (char) 67 → 'C'
            char row = (char) (FIRST_ROW + i);

            // ถ้า row เป็น 'F' ได้ VIP นอกนั้นได้ STANDARD
            // เช่น row = 'F' → SeatType.VIP / row = 'C' → SeatType.STANDARD
            SeatType type = (row == VIP_ROW) ? SeatType.VIP : SeatType.STANDARD;

            List<Seat> seatsOfRow = new ArrayList<>();
            for (int number = FIRST_SEAT_NUMBER; number <= SEATS_PER_ROW; number++) {
                // new Seat(row, number, type)
                //   row    = ตัวอักษรแถว เช่น 'F'
                //   number = เลขที่นั่ง เช่น 7
                //   type   = ประเภทของแถวนี้ เช่น SeatType.VIP
                //   → ได้ที่นั่ง F7 ประเภท VIP
                Seat seat = new Seat(row, number, type);
                seatsOfRow.add(seat);

                // byCode.put(seat.code(), seat)
                //   seat.code() = รหัสที่ใช้ค้น เช่น "F7"
                //   seat        = ที่นั่งที่จะได้คืนตอนค้นด้วยรหัสนี้
                byCode.put(seat.code(), seat);
            }

            rowList.add(row);
            // byRow.put(row, List.copyOf(seatsOfRow))
            //   row                    = แถว เช่น 'F'
            //   List.copyOf(seatsOfRow) = สำเนาแบบแก้ไม่ได้ของที่นั่งในแถว เช่น [F1, F2, ..., F10]
            byRow.put(row, List.copyOf(seatsOfRow));
        }

        // ทำสำเนาแบบแก้ไม่ได้ ข้างนอกจะเพิ่ม / ลบ ที่นั่งในผังไม่ได้ (SC4 rep exposure)
        this.rows = List.copyOf(rowList);
        this.seatsByRow = Map.copyOf(byRow);
        this.seatsByCode = Map.copyOf(byCode);
        checkRep();
    }

    /**
     * รายชื่อแถวเรียงจากหน้าจอไปหลังโรง
     *
     * วิธีทำงาน: คืน rows ตรง ๆ ได้เลย เพราะเป็น List ที่แก้ไม่ได้อยู่แล้ว
     *
     * ตัวอย่าง:
     *   layout.rows()          → ['A','B','C','D','E','F']
     *   layout.rows().size()   → 6
     *   layout.rows().add('G') → พัง UnsupportedOperationException (แก้ผังจากข้างนอกไม่ได้)
     *
     * @return รายชื่อแถว แก้ไขไม่ได้
     */
    public List<Character> rows() {
        return rows;
    }

    /**
     * ที่นั่งทั้งหมดในแถวที่ระบุ เรียงจากเลข 1 ถึง 10
     *
     * วิธีทำงาน:
     *   1. ค้นแถวในตาราง seatsByRow
     *   2. ไม่เจอ (ไม่มีแถวนี้ในโรง) → throw
     *   3. เจอ → คืนรายการที่นั่ง (แก้ไม่ได้อยู่แล้ว)
     *
     * ตัวอย่าง:
     *   layout.seatsInRow('A') → [A1, A2, ..., A10]  ประเภทธรรมดาทั้งแถว
     *   layout.seatsInRow('F') → [F1, F2, ..., F10]  ประเภท VIP ทั้งแถว
     *   layout.seatsInRow('G') → throw "no such row: G"
     *   layout.seatsInRow('a') → throw "no such row: a"  (ต้องตัวใหญ่)
     *
     * @param row ตัวอักษรแถว เช่น 'F'
     * @return ที่นั่งในแถวนั้น เรียงตามเลข แก้ไขไม่ได้
     * @throws IllegalArgumentException ถ้าไม่มีแถวนี้ในโรง
     */
    public List<Seat> seatsInRow(char row) {
        // seatsByRow.get(row)
        //   row = แถวที่อยากได้ เช่น 'F' → ได้ [F1 ... F10] / 'G' → ได้ null เพราะไม่มีแถวนี้
        List<Seat> seats = seatsByRow.get(row);
        if (seats == null) {
            throw new IllegalArgumentException("no such row: " + row);
        }
        return seats;
    }

    /**
     * ทางเดินอยู่หลังที่นั่งเลขไหน หน้าจอใช้เว้นช่องว่างในผัง
     *
     * วิธีทำงาน: คืนค่าคงที่ AISLE_AFTER
     *
     * ตัวอย่าง:
     *   layout.aisleAfter() → 5
     *   หน้าจอวาด 1 2 3 4 5 แล้วเว้นช่อง แล้ววาด 6 7 8 9 10
     *
     * @return เลขที่นั่งที่ทางเดินอยู่ถัดจากมัน
     */
    public int aisleAfter() {
        return AISLE_AFTER;
    }

    /**
     * แปลงรหัสที่นั่ง เช่น "F7" กลับเป็น Seat
     * ใช้ตอนผู้ใช้กดที่นั่ง หรืออ่านรหัสที่นั่งจาก bookings.csv เพื่อรู้ประเภทและราคา
     *
     * วิธีทำงาน:
     *   1. code เป็น null → throw
     *   2. ค้น code ในตาราง seatsByCode
     *   3. ไม่เจอ (ไม่มีที่นั่งนี้ในโรง) → throw
     *   4. เจอ → คืน Seat
     *
     * ตัวอย่าง:
     *   layout.findSeat("F7")  → Seat F7 ประเภท VIP      → ราคา 160 + 40 = 200
     *   layout.findSeat("A1")  → Seat A1 ประเภทธรรมดา   → ราคา 160 + 0  = 160
     *   layout.findSeat("G3")  → throw "no such seat: G3"   (ไม่มีแถว G)
     *   layout.findSeat("A11") → throw "no such seat: A11"  (แถวละ 10 ที่)
     *   layout.findSeat("f7")  → throw "no such seat: f7"   (ต้องตัวใหญ่)
     *   layout.findSeat(null)  → throw "code must not be null"
     *
     * @param code รหัสที่นั่ง เช่น "F7"
     * @return ที่นั่งที่ตรงกับรหัส
     * @throws IllegalArgumentException ถ้า code เป็น null หรือไม่มีที่นั่งนี้ในโรง
     */
    public Seat findSeat(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code must not be null");
        }
        // seatsByCode.get(code)
        //   code = รหัสที่อยากได้ เช่น "F7" → ได้ Seat F7 / "G3" → ได้ null เพราะไม่มีที่นั่งนี้
        Seat seat = seatsByCode.get(code);
        if (seat == null) {
            throw new IllegalArgumentException("no such seat: " + code);
        }
        return seat;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     * ถ้า assert ไหนไม่จริง แปลว่าค่าคงที่ผังตั้งผิด หรือ constructor สร้างผังผิดเอง
     */
    private void checkRep() {
        assert rows.size() == ROW_COUNT;
        assert seatsByRow.size() == ROW_COUNT;
        assert seatsByCode.size() == ROW_COUNT * SEATS_PER_ROW;
        assert AISLE_AFTER >= FIRST_SEAT_NUMBER && AISLE_AFTER < SEATS_PER_ROW;
        for (int i = 0; i < ROW_COUNT; i++) {
            char row = rows.get(i);
            assert row == FIRST_ROW + i;
            List<Seat> seats = seatsByRow.get(row);
            assert seats != null && seats.size() == SEATS_PER_ROW;
            for (int j = 0; j < SEATS_PER_ROW; j++) {
                Seat seat = seats.get(j);
                assert seat.row() == row;
                assert seat.number() == FIRST_SEAT_NUMBER + j;
                assert seat.type() == (row == VIP_ROW ? SeatType.VIP : SeatType.STANDARD);
                assert seatsByCode.get(seat.code()) == seat;
            }
        }
    }
}