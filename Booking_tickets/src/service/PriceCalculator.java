//PriceCalculator <- คิดราคาตั๋ว เป็นที่เดียวที่รู้ว่าราคาปกติ 160 บาท และสมาชิกลด 10%
/*
    สูตร:
        ราคาที่นั่ง 1 ที่ = 160 + ส่วนเพิ่มของประเภทที่นั่ง (ธรรมดา +0 / VIP +40)
        ถ้าเป็นสมาชิก    = ราคานั้น × 90 / 100   (ลด 10%)

        ธรรมดา ไม่เป็นสมาชิก  = 160
        VIP    ไม่เป็นสมาชิก  = 200
        ธรรมดา เป็นสมาชิก     = 144   (160 × 90 / 100)
        VIP    เป็นสมาชิก     = 180   (200 × 90 / 100)

    มี 2 เมธอด:
    priceOf(seat, isMember)   → ราคาที่นั่ง 1 ที่
    totalOf(seats, isMember)  → ราคารวมทุกที่นั่งที่เลือก

    ตัวอย่างการใช้ (หน้าเลือกที่นั่ง, somchai เป็นสมาชิก):
        PriceCalculator prices = new PriceCalculator();
        HallLayout layout = new HallLayout();
        List<Seat> chosen = List.of(layout.findSeat("A1"), layout.findSeat("F7"));

        prices.priceOf(layout.findSeat("F7"), false);    // 200  → เขียนบนปุ่ม/ป้าย "VIP 200 ฿"
        prices.totalOf(chosen, false);                   // 360  → "ราคาปกติ 360 ฿"
        prices.totalOf(chosen, true);                    // 324  → "ยอดชำระ 324 ฿"
        prices.totalOf(chosen, false) - prices.totalOf(chosen, true);   // 36 → "ส่วนลดสมาชิก −36 ฿"
        prices.totalOf(List.of(), true);                 // 0    → ยังไม่ได้เลือกที่นั่ง

    isMember ได้จาก accountService.isMember(user)
    BookingService ก็ใช้ totalOf ตัวเดียวกันตอนตัดเงินจริง ราคาบนจอกับเงินที่ตัดจึงตรงกันเสมอ
 */

package service;

import java.util.List;   // ชนิดของรายการที่นั่งที่รับเข้ามาใน totalOf
import model.Seat;      // ที่นั่ง 1 ตัว (มี type บอกว่า ธรรมดา / VIP) อยู่คนละ package เลยต้อง import

/**
 * ตัวคิดราคาตั๋ว ตัวเลขราคาทั้งหมดอยู่ที่ไฟล์นี้ที่เดียว (SC2 magic number)
 * อยากเปลี่ยนราคาปกติหรือส่วนลด แก้ค่าคงที่ข้างล่างจบ ส่วนเพิ่ม VIP อยู่ใน SeatType
 */
public class PriceCalculator {

    // AF: ตัวคิดราคาที่ราคาปกติ BASE_PRICE บาท บวกส่วนเพิ่มตามประเภทที่นั่ง
    //     และลด MEMBER_DISCOUNT_PERCENT เปอร์เซ็นต์ให้สมาชิก
    // RI: BASE_PRICE > 0, MEMBER_DISCOUNT_PERCENT อยู่ระหว่าง 0 ถึง 100
    //     (ไม่มี field ของ object มีแต่ค่าคงที่ checkRep ตรวจว่าค่าคงที่ตั้งถูก)
    // Safety from rep exposure: ไม่มี field ที่แก้ได้ ค่าคงที่เป็น private static final
    // Thread safety: ไม่มีอะไรเปลี่ยนค่า ใช้ข้าม thread ได้ (SC7)

    /** ราคาปกติของที่นั่ง 1 ที่ (บาท) ก่อนบวกส่วนเพิ่ม VIP */
    private static final int BASE_PRICE = 160;
    /** สมาชิกลดกี่เปอร์เซ็นต์ */
    private static final int MEMBER_DISCOUNT_PERCENT = 10;
    /** เต็มร้อย ใช้คิดเปอร์เซ็นต์ */
    private static final int FULL_PERCENT = 100;

    /**
     * สร้างตัวคิดราคา ไม่ต้องส่งอะไรเข้ามา เพราะตัวเลขราคาอยู่ในค่าคงที่แล้ว
     *
     * ตัวอย่าง: new PriceCalculator()
     */
    public PriceCalculator() {
        checkRep();
    }

    /**
     * ราคาที่นั่ง 1 ที่
     *
     * วิธีทำงาน:
     *   1. seat เป็น null → throw
     *   2. ราคา = BASE_PRICE + ส่วนเพิ่มของประเภทที่นั่ง   เช่น F7 (VIP) → 160 + 40 = 200
     *   3. ไม่เป็นสมาชิก → คืนราคานั้นเลย
     *      เป็นสมาชิก    → คืน ราคา × (100 − 10) / 100    เช่น 200 × 90 / 100 = 180
     *
     * คูณก่อนหารเสมอ เพราะ int หารแล้วทิ้งเศษ ถ้าหารก่อน 90 / 100 = 0 ราคาจะกลายเป็น 0
     *
     * ตัวอย่าง:
     *   priceOf(A1 ธรรมดา, false) → 160
     *   priceOf(F7 VIP,    false) → 200
     *   priceOf(A1 ธรรมดา, true)  → 144
     *   priceOf(F7 VIP,    true)  → 180
     *   priceOf(null, true)       → throw "seat must not be null"
     *
     * @param seat     ที่นั่ง ห้าม null
     * @param isMember true ถ้าผู้จองเป็นสมาชิกตอนนี้
     * @return ราคาที่นั่งนี้เป็นบาท
     * @throws IllegalArgumentException ถ้า seat เป็น null
     */
    public int priceOf(Seat seat, boolean isMember) {
        if (seat == null) {
            throw new IllegalArgumentException("seat must not be null");
        }
        // seat.type().extraPrice()
        //   seat.type() = ประเภทที่นั่ง เช่น SeatType.VIP
        //   .extraPrice() = ส่วนเพิ่มของประเภทนั้น เช่น 40
        int price = BASE_PRICE + seat.type().extraPrice();
        if (!isMember) {
            return price;
        }
        // price * (FULL_PERCENT - MEMBER_DISCOUNT_PERCENT) / FULL_PERCENT
        //   FULL_PERCENT - MEMBER_DISCOUNT_PERCENT = 100 - 10 = 90 (จ่าย 90%)
        //   เช่น price = 200 → 200 * 90 = 18000 → 18000 / 100 = 180
        return price * (FULL_PERCENT - MEMBER_DISCOUNT_PERCENT) / FULL_PERCENT;
    }

    /**
     * ราคารวมทุกที่นั่งที่เลือก
     *
     * วิธีทำงาน:
     *   1. seats เป็น null → throw
     *   2. เริ่มยอดรวมที่ 0
     *   3. วนทีละที่นั่ง บวกราคาจาก priceOf() (ที่นั่งที่เป็น null → priceOf throw)
     *   4. คืนยอดรวม (ไม่ได้เลือกเลย ได้ 0)
     *
     * ตัวอย่าง:
     *   totalOf([A1, F7], false) → 160 + 200 = 360
     *   totalOf([A1, F7], true)  → 144 + 180 = 324
     *   totalOf([], true)        → 0
     *   totalOf(null, true)      → throw "seats must not be null"
     *
     * @param seats    ที่นั่งที่เลือก ห้าม null และห้ามมี null อยู่ข้างใน (ว่างได้)
     * @param isMember true ถ้าผู้จองเป็นสมาชิกตอนนี้
     * @return ราคารวมเป็นบาท
     * @throws IllegalArgumentException ถ้า seats เป็น null หรือมีที่นั่งที่เป็น null
     */
    public int totalOf(List<Seat> seats, boolean isMember) {
        if (seats == null) {
            throw new IllegalArgumentException("seats must not be null");
        }
        int total = 0;
        for (Seat seat : seats) {
            // priceOf(seat, isMember)
            //   seat     = ที่นั่งรอบนี้ เช่น F7
            //   isMember = ส่งต่อค่าที่รับมา เช่น true → ได้ 180
            total += priceOf(seat, isMember);
        }
        return total;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     * ถ้า assert ไม่จริง แปลว่าตั้งค่าคงที่ราคาผิด เช่น ส่วนลด 150%
     */
    private void checkRep() {
        assert BASE_PRICE > 0;
        assert MEMBER_DISCOUNT_PERCENT >= 0 && MEMBER_DISCOUNT_PERCENT <= FULL_PERCENT;
    }
}