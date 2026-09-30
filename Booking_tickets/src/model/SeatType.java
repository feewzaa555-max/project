//SeatType <- ประเภทที่นั่ง บอกว่ามีกี่ประเภท แต่ละประเภทชื่ออะไร และแพงกว่าราคาปกติเท่าไหร่
/*
    มี 2 ประเภท:
    STANDARD → ชื่อบนจอ "ธรรมดา" บวกเพิ่ม 0 บาท   (แถว A–E)
    VIP      → ชื่อบนจอ "VIP"    บวกเพิ่ม 40 บาท  (แถว F)

    ตัวอย่างการใช้:
        SeatType type = SeatType.VIP;
        type.displayName();          // "VIP"   → หน้าจอใช้ทำป้าย เช่น "VIP (+40 ฿)"
        type.extraPrice();           // 40      → การคิดราคาใช้ ราคาปกติ 160 + 40 = 200
        SeatType.STANDARD.extraPrice();  // 0   → 160 + 0 = 160

        for (SeatType t : SeatType.values()) { ... }  // วนได้ทุกประเภท เช่น ทำป้ายอธิบายสีใต้ผังที่นั่ง
 */

package model;

// ไม่มี import: ใช้แค่ String กับ int ซึ่ง Java มีให้อยู่แล้ว

/**
 * ประเภทที่นั่งในโรงหนัง
 * เป็น enum (ชนิดที่มีค่าให้เลือกตายตัว) พิมพ์ผิดเช่น SeatType.VIPP คอมไพล์ไม่ผ่านทันที (SC5 หน้า 34)
 * ส่วนเพิ่มราคาอยู่ที่นี่ที่เดียว อยากเปลี่ยน VIP เป็น +50 แก้บรรทัด VIP บรรทัดเดียว (SC2 magic number)
 */
public enum SeatType {

    // ชื่อค่าคงที่ (ชื่อบนจอ, บวกเพิ่มกี่บาท)
    // ตัวเลข 0 กับ 40 อยู่ตรงนี้ได้ เพราะนี่คือที่ตั้งชื่อให้มันแล้ว (STANDARD / VIP) ไม่ใช่ตัวเลขลอย
    /** ที่นั่งธรรมดา (แถว A–E) ไม่บวกเพิ่ม */
    STANDARD("ธรรมดา", 0),

    /** ที่นั่ง VIP (แถว F) บวกเพิ่ม 40 บาท */
    VIP("VIP", 40);

    // AF: ประเภทที่นั่ง ชื่อที่โชว์บนจอคือ displayName แพงกว่าราคาปกติ extraPrice บาท
    // RI: displayName ไม่เป็น null และไม่ว่าง, extraPrice >= 0
    // Safety from rep exposure: field เป็น private final, String / int แก้ไขไม่ได้
    // Thread safety: enum สร้างครั้งเดียวตอนเริ่มโปรแกรม ค่าไม่เปลี่ยนอีก ใช้ข้าม thread ได้ (SC7)

    private final String displayName; // ชื่อที่โชว์บนจอ เช่น "ธรรมดา"
    private final int extraPrice;     // บวกเพิ่มจากราคาปกติกี่บาท เช่น 40

    /**
     * enum เรียก constructor นี้เองตอนสร้าง STANDARD กับ VIP (คนอื่นเรียกไม่ได้)
     * เช่น VIP("VIP", 40) → displayName = "VIP", extraPrice = 40
     *
     * @param displayName ชื่อที่โชว์บนจอ ห้ามว่าง
     * @param extraPrice  บวกเพิ่มจากราคาปกติกี่บาท ต้อง >= 0
     */
    SeatType(String displayName, int extraPrice) {
        this.displayName = displayName;
        this.extraPrice = extraPrice;
        checkRep();
    }

    /**
     * ชื่อที่โชว์บนจอ ใช้แทน name() ที่ได้ภาษาอังกฤษตัวใหญ่
     * เช่น SeatType.STANDARD.displayName() → "ธรรมดา" (ส่วน SeatType.STANDARD.name() ได้ "STANDARD")
     *
     * @return ชื่อภาษาที่คนอ่านเข้าใจ
     */
    public String displayName() {
        return displayName;
    }

    /**
     * แพงกว่าราคาปกติกี่บาท
     * เช่น SeatType.VIP.extraPrice() → 40 / SeatType.STANDARD.extraPrice() → 0
     *      ราคาที่นั่ง = ราคาปกติ + extraPrice() เช่น 160 + 40 = 200
     *
     * @return ส่วนเพิ่มเป็นบาท (0 ขึ้นไป)
     */
    public int extraPrice() {
        return extraPrice;
    }

    // ตรวจ RI
    private void checkRep() {
        assert displayName != null && !displayName.isBlank();
        assert extraPrice >= 0;
    }
}