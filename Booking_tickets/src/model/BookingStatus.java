//BookingStatus <- สถานะของการจอง 1 ครั้ง ว่าจ่ายแล้วหรือยกเลิกแล้ว
/*
    มี 2 สถานะ:
    PAID      → ชื่อบนจอ "ชำระแล้ว"    เกิดตอนกดยืนยันจอง แล้วตัดเงินสำเร็จ
    CANCELLED → ชื่อบนจอ "ยกเลิกแล้ว"  เกิดตอนกดยกเลิก แล้วได้เงินคืน

    ไม่มีสถานะ "รอจ่าย" เพราะตกลงกันว่ากดยืนยันแล้วตัดเงินในแอปทันที

    ตัวอย่างการใช้:
        BookingStatus status = BookingStatus.PAID;
        status.displayName();                      // "ชำระแล้ว" → หน้าประวัติโชว์ป้ายข้างรายการจอง
        status == BookingStatus.CANCELLED;         // false → ที่นั่งของการจองนี้ยังไม่ว่าง
        BookingStatus.valueOf("CANCELLED");        // CANCELLED → ใช้แปลงข้อความใน bookings.csv กลับเป็นสถานะ
 */

package model;

// ไม่มี import: ใช้แค่ String ที่ Java มีให้อยู่แล้ว

/**
 * สถานะของการจอง
 * เป็น enum (ชนิดที่มีค่าให้เลือกตายตัว) พิมพ์ผิดเช่น BookingStatus.PAYED คอมไพล์ไม่ผ่านทันที (SC5 หน้า 34)
 */
public enum BookingStatus {

    // ชื่อค่าคงที่ (ชื่อบนจอ)
    /** จ่ายเงินแล้ว ที่นั่งถูกจองอยู่ */
    PAID("ชำระแล้ว"),

    /** ยกเลิกแล้ว คืนเงินแล้ว ที่นั่งกลับมาว่าง */
    CANCELLED("ยกเลิกแล้ว");

    // AF: สถานะการจอง ชื่อที่โชว์บนจอคือ displayName
    // RI: displayName ไม่เป็น null และไม่ว่าง
    // Safety from rep exposure: field เป็น private final, String แก้ไขไม่ได้
    // Thread safety: enum สร้างครั้งเดียวตอนเริ่มโปรแกรม ค่าไม่เปลี่ยนอีก ใช้ข้าม thread ได้ (SC7)

    /** ชื่อที่โชว์บนจอ เช่น "ชำระแล้ว" */
    private final String displayName;

    /**
     * enum เรียก constructor นี้เองตอนสร้าง PAID กับ CANCELLED (คนอื่นเรียกไม่ได้)
     * เช่น PAID("ชำระแล้ว") → displayName = "ชำระแล้ว"
     *
     * @param displayName ชื่อที่โชว์บนจอ ห้ามว่าง
     */
    BookingStatus(String displayName) {
        this.displayName = displayName;
        checkRep();
    }

    /**
     * ชื่อที่โชว์บนจอ ใช้แทน name() ที่ได้ภาษาอังกฤษตัวใหญ่
     * เช่น BookingStatus.PAID.displayName() → "ชำระแล้ว" (ส่วน BookingStatus.PAID.name() ได้ "PAID")
     *
     * @return ชื่อภาษาที่คนอ่านเข้าใจ
     */
    public String displayName() {
        return displayName;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     */
    private void checkRep() {
        assert displayName != null && !displayName.isBlank();
    }
}