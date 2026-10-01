//Account <- บัญชีของผู้ใช้ 1 คน เก็บว่ามีเงินในแอปเท่าไหร่ และเป็นสมาชิกถึงวันไหนกี่โมง
/*
    มี 3 ค่า:
    username    → ชื่อผู้ใช้ ตรงกับ User ที่ login เช่น "somchai"
    balance     → เงินคงเหลือในแอป (บาท จำนวนเต็ม) เช่น 300
    memberUntil → เวลาที่สมาชิกหมดอายุ เช่น 29/10/2027 06:00 (null = ไม่เคยสมัครสมาชิก)

    กฎที่อยู่ในไฟล์นี้:
    - สมัครใหม่ทุกคนเริ่มที่ 0 บาท ไม่เป็นสมาชิก                        → Account.newFor(...)
    - เติมเงินได้ครั้งละ 1 ถึง 5,000 บาท (จำกัดต่อครั้ง ไม่จำกัดยอดรวม)   → topUp(...)
    - จ่ายเงินได้ไม่เกินเงินที่มี                                        → pay(...)
    - สมาชิก 30 วันเต็มนับจากเวลาที่สมัคร ต่อก่อนหมดจะต่อจากเวลาหมดเดิม  → extendMembership(...)

    Account แก้ค่าไม่ได้ ทุกเมธอดที่ "เปลี่ยน" อะไร จะคืน Account ตัวใหม่ ตัวเดิมเหมือนเดิม
    จึงต้องเอาค่าที่คืนมาไปใช้ต่อเสมอ

    ตัวอย่างการใช้ (now = 29/09/2027 06:00 ได้จาก clock.now()):
        Account acc = Account.newFor("somchai");     // เงิน 0, ไม่เป็นสมาชิก
        acc = acc.topUp(300);                        // เงิน 300
        acc = acc.pay(99);                           // เงิน 201   (ค่าสมาชิก ตัดโดย AccountService)
        acc = acc.extendMembership(now);             // สมาชิกหมด 29/10/2027 06:00
        acc.isMemberAt(now);                         // true  → หน้าจอโชว์ป้าย "สมาชิก ถึง 29/10 06:00" / ได้ลด 10%
        acc.isMemberAt(29/10/2027 05:59);            // true
        acc.isMemberAt(29/10/2027 06:00);            // false → หมดตรงเวลาพอดี
        acc.pay(500);                                // พัง IllegalArgumentException: เงินไม่พอ (มี 201)
        acc.topUp(6000);                             // พัง IllegalArgumentException: เกิน 5,000 ต่อครั้ง

    ไฟล์นี้ไม่ตัดค่าสมาชิก 99 บาทเอง และไม่เขียนไฟล์ CSV
    (ตัดเงินเป็นหน้าที่ของ AccountService, เขียนไฟล์เป็นหน้าที่ของ CsvAccountRepository)
 */

package model;

import java.time.LocalDateTime;     // ใช้เก็บเวลาหมดสมาชิก (วันที่ + เวลา) และคำนวณด้วย plusDays / isBefore
import java.time.temporal.ChronoUnit; // ใช้ ChronoUnit.MINUTES ตัดวินาทีทิ้ง ให้เวลาหมดเป็นนาทีเต็มตรงกับป้ายบนจอ

/**
 * บัญชีเงินและสถานะสมาชิกของผู้ใช้ 1 คน
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5 หน้า 9)
 * เมธอดที่เปลี่ยนค่าเป็น producer คืนตัวใหม่ (SC4 หน้า 16)
 *
 * @param username    ชื่อผู้ใช้ ห้าม null ห้ามว่าง
 * @param balance     เงินคงเหลือ (บาท) ต้อง >= 0
 * @param memberUntil เวลาที่สมาชิกหมดอายุ (ถึงเวลานี้แล้วไม่เป็นสมาชิก) หรือ null ถ้าไม่เคยสมัคร
 */
public record Account(String username, int balance, LocalDateTime memberUntil) {

    // AF: บัญชีของผู้ใช้ username มีเงิน balance บาท
    //     ถ้า memberUntil เป็น null = ไม่เคยเป็นสมาชิก
    //     ไม่เป็น null = เป็นสมาชิกทุกเวลาที่ก่อน memberUntil พอถึง memberUntil ก็หมดทันที
    // RI: username ไม่เป็น null และไม่ว่าง, balance >= 0
    //     (memberUntil เป็น null ได้ ไม่มีเงื่อนไขอื่น)
    // Safety from rep exposure: record เป็น private final, String / int / LocalDateTime แก้ไขไม่ได้
    // Thread safety: immutable ค่าไม่เปลี่ยนหลังสร้าง ใช้ข้าม thread ได้ (SC7)

    /** เงินเริ่มต้นของผู้ใช้ที่สมัครใหม่ */
    private static final int STARTING_BALANCE = 0;
    /** เติมเงินได้ต่ำสุดครั้งละกี่บาท */
    private static final int MIN_TOP_UP = 1;
    /** เติมเงินได้สูงสุดครั้งละกี่บาท */
    private static final int MAX_TOP_UP = 5000;
    /** จ่ายเงินต่ำสุดครั้งละกี่บาท (จ่าย 0 หรือติดลบไม่ได้) */
    private static final int MIN_PAYMENT = 1;
    /** สมัครสมาชิก 1 ครั้งได้กี่วัน (นับเต็ม 24 ชั่วโมงต่อวัน จากเวลาที่สมัคร) */
    private static final int MEMBERSHIP_DAYS = 30;

    /**
     * สร้างบัญชี และตรวจค่าก่อนเก็บ
     * ปกติใช้ Account.newFor(...) ตอนสมัครใหม่ ส่วนตัวนี้ใช้ตอนอ่านบัญชีเดิมกลับมาจากไฟล์
     *
     * วิธีทำงาน:
     *   1. ตรวจ username ว่าไม่ null ไม่ว่าง ไม่ใช่ → throw
     *   2. ตรวจ balance ว่า >= 0 ไม่ใช่ → throw
     *   3. memberUntil ไม่ต้องตรวจ (null = ไม่เคยสมัคร)
     *   4. เก็บค่าลง field แล้วเรียก checkRep()
     *
     * ใช้ throw เพราะค่ามาจากข้างนอก เช่น ไฟล์ accounts.csv (SC2 หน้า 10)
     * เขียนแบบเต็ม (มี this.xxx = xxx) เพื่อให้เรียก checkRep() หลังเก็บค่าได้
     *
     * ตัวอย่าง:
     *   new Account("somchai", 300, null)                                   → เงิน 300 ไม่เป็นสมาชิก
     *   new Account("somchai", 201, LocalDateTime.of(2027, 10, 29, 6, 0))    → เงิน 201 สมาชิกหมด 29/10 06:00
     *   new Account("", 300, null)                                          → throw "username must not be blank"
     *   new Account("somchai", -1, null)                                    → throw "balance must be >= 0: -1"
     *
     * @param username    ชื่อผู้ใช้ ห้าม null ห้ามว่าง
     * @param balance     เงินคงเหลือ ต้อง >= 0
     * @param memberUntil เวลาที่สมาชิกหมดอายุ หรือ null
     * @throws IllegalArgumentException ถ้า username ว่าง หรือ balance ติดลบ
     */
    public Account(String username, int balance, LocalDateTime memberUntil) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("balance must be >= 0: " + balance);
        }
        this.username = username;
        this.balance = balance;
        this.memberUntil = memberUntil;
        checkRep();
    }

    /**
     * สร้างบัญชีใหม่ของคนที่เพิ่งสมัคร: เงิน 0 บาท ไม่เป็นสมาชิก
     *
     * วิธีทำงาน: เรียก constructor ด้วย STARTING_BALANCE (0) และ memberUntil = null
     *
     * ตัวอย่าง:
     *   Account.newFor("somchai") → Account[username=somchai, balance=0, memberUntil=null]
     *   Account.newFor("")        → throw "username must not be blank"
     *
     * @param username ชื่อผู้ใช้ที่เพิ่งสมัคร ห้าม null ห้ามว่าง
     * @return บัญชีใหม่ เงิน 0 ไม่เป็นสมาชิก
     * @throws IllegalArgumentException ถ้า username ว่าง
     */
    public static Account newFor(String username) {
        // new Account(username, STARTING_BALANCE, null)
        //   username         = ชื่อคนที่สมัคร เช่น "somchai"
        //   STARTING_BALANCE = เงินเริ่มต้น 0
        //   null             = ยังไม่เคยเป็นสมาชิก
        return new Account(username, STARTING_BALANCE, null);
    }

    /**
     * ณ เวลานี้ยังเป็นสมาชิกอยู่ไหม
     *
     * วิธีทำงาน:
     *   1. now เป็น null → throw
     *   2. memberUntil เป็น null (ไม่เคยสมัคร) → false
     *   3. ถ้า now อยู่ก่อน memberUntil → true, ถึงหรือเลย memberUntil แล้ว → false
     *
     * ตัวอย่าง (memberUntil = 29/10/2027 06:00):
     *   isMemberAt(29/09/2027 06:00) → true
     *   isMemberAt(29/10/2027 05:59) → true   (เหลืออีก 1 นาที)
     *   isMemberAt(29/10/2027 06:00) → false  (หมดตรงเวลาพอดี)
     *   isMemberAt(29/10/2027 18:00) → false
     *   ถ้า memberUntil = null → false ทุกเวลา
     *
     * @param now เวลาที่จะถาม ปกติใช้ clock.now()
     * @return true ถ้าเวลานั้นเป็นสมาชิก
     * @throws IllegalArgumentException ถ้า now เป็น null
     */
    public boolean isMemberAt(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        // now.isBefore(memberUntil)
        //   memberUntil = เวลาหมด เช่น 29/10 06:00
        //   now = 29/10 05:59 → true  (ยังไม่ถึงเวลาหมด)
        //   now = 29/10 06:00 → false (ถึงแล้ว = หมด)
        return memberUntil != null && now.isBefore(memberUntil);
    }

    /**
     * เติมเงิน คืนบัญชีตัวใหม่ที่เงินเพิ่มขึ้น
     *
     * วิธีทำงาน:
     *   1. ถ้า amount น้อยกว่า 1 หรือเกิน 5,000 → throw
     *   2. สร้าง Account ตัวใหม่ เงิน = balance + amount ค่าอื่นเหมือนเดิม
     *
     * หน้าจอตรวจค่าก่อนอยู่แล้ว (ช่องขึ้นแดง) แต่ตรงนี้ตรวจซ้ำ กันโค้ดส่วนอื่นเรียกผิด
     *
     * ตัวอย่าง (balance = 0):
     *   topUp(300)  → Account เงิน 300
     *   topUp(5000) → Account เงิน 5000
     *   topUp(6000) → throw "top-up must be 1-5000: 6000"
     *   topUp(0)    → throw "top-up must be 1-5000: 0"
     *   เติม 5000 สองครั้ง → เงิน 10000 ได้ (จำกัดต่อครั้ง ไม่จำกัดยอดรวม)
     *
     * @param amount จำนวนเงินที่เติม 1 ถึง 5,000
     * @return บัญชีตัวใหม่ที่เงินเพิ่มแล้ว
     * @throws IllegalArgumentException ถ้า amount อยู่นอกช่วง 1 ถึง 5,000
     */
    public Account topUp(int amount) {
        if (amount < MIN_TOP_UP || amount > MAX_TOP_UP) {
            throw new IllegalArgumentException("top-up must be " + MIN_TOP_UP + "-" + MAX_TOP_UP + ": " + amount);
        }
        // new Account(username, balance + amount, memberUntil)
        //   username         = คนเดิม เช่น "somchai"
        //   balance + amount = เงินใหม่ เช่น 0 + 300 = 300
        //   memberUntil      = สถานะสมาชิกเดิม ไม่เปลี่ยน
        return new Account(username, balance + amount, memberUntil);
    }

    /**
     * จ่ายเงิน คืนบัญชีตัวใหม่ที่เงินลดลง
     * ใช้ทั้งจ่ายค่าตั๋วและค่าสมาชิก
     *
     * วิธีทำงาน:
     *   1. ถ้า amount น้อยกว่า 1 → throw
     *   2. ถ้า amount มากกว่าเงินที่มี → throw (เงินไม่พอ)
     *   3. สร้าง Account ตัวใหม่ เงิน = balance - amount ค่าอื่นเหมือนเดิม
     *
     * คนเรียก (AccountService / BookingService) ควรเช็ก balance() ก่อน
     * แล้วแจ้งผู้ใช้ว่าเงินไม่พอ ส่วน throw ตรงนี้เป็นด่านสุดท้ายกันเงินติดลบ
     *
     * ตัวอย่าง (balance = 300):
     *   pay(99)  → Account เงิน 201
     *   pay(300) → Account เงิน 0     (จ่ายหมดพอดีได้)
     *   pay(301) → throw "not enough balance: has 300, needs 301"
     *   pay(0)   → throw "payment must be >= 1: 0"
     *
     * @param amount จำนวนเงินที่จ่าย ตั้งแต่ 1 และไม่เกินเงินที่มี
     * @return บัญชีตัวใหม่ที่เงินลดแล้ว
     * @throws IllegalArgumentException ถ้า amount น้อยกว่า 1 หรือเงินไม่พอ
     */
    public Account pay(int amount) {
        if (amount < MIN_PAYMENT) {
            throw new IllegalArgumentException("payment must be >= " + MIN_PAYMENT + ": " + amount);
        }
        if (amount > balance) {
            throw new IllegalArgumentException("not enough balance: has " + balance + ", needs " + amount);
        }
        // new Account(username, balance - amount, memberUntil)
        //   username         = คนเดิม
        //   balance - amount = เงินใหม่ เช่น 300 - 99 = 201
        //   memberUntil      = สถานะสมาชิกเดิม ไม่เปลี่ยน
        return new Account(username, balance - amount, memberUntil);
    }

        /**
     * สมัคร / ต่ออายุสมาชิก 30 วัน คืนบัญชีตัวใหม่ (ไม่ตัดเงิน ตัดเงินเป็นหน้าที่ของ AccountService)
     *
     * วิธีทำงาน:
     *   1. now เป็น null → throw
     *   2. ถ้ายังเป็นสมาชิกอยู่ (ยังไม่ถึงเวลาหมด) → เวลาหมดใหม่ = เวลาหมดเดิม + 30 วัน
     *      เวลาที่เหลือจึงไม่หาย
     *   3. ถ้าไม่เคยเป็น หรือหมดแล้ว → เวลาหมดใหม่ = เวลาตอนนี้ (ตัดวินาทีทิ้ง) + 30 วัน
     *      ตัดวินาทีเพื่อให้เวลาหมดเป็นนาทีเต็ม ตรงกับป้าย "ถึง 29/10 06:00" บนจอ
     *   4. สร้าง Account ตัวใหม่ด้วยเวลาหมดใหม่ เงินเท่าเดิม
     *
     * ตัวอย่าง (ทุกวันที่เป็นปี 2027):
     *
     *   กรณี 1: ไม่เคยเป็นสมาชิก มาสมัครครั้งแรก
     *     กดสมัครตอน    29/09 06:00:45
     *     ได้เวลาหมด    29/10 06:00       ← ตัดวินาที 06:00:45 เหลือ 06:00 แล้วบวก 30 วัน
     *
     *   กรณี 2: ยังเป็นสมาชิกอยู่ มาต่อล่วงหน้า
     *     เดิมหมด       29/10 06:00
     *     กดต่อตอน      15/10 20:00       ← ยังไม่ถึง 29/10 06:00 แปลว่ายังเป็นสมาชิกอยู่
     *     ได้เวลาหมด    28/11 06:00       ← บวก 30 วันจาก "เวลาหมดเดิม" (29/10 06:00) ไม่ใช่จากวันที่กด (15/10)
     *                                       วันที่เหลืออีก 13 วันกว่าเลยไม่หาย
     *
     *   กรณี 3: มาต่อตอนเหลืออีกแค่ 1 นาที
     *     เดิมหมด       29/10 06:00
     *     กดต่อตอน      29/10 05:59       ← ยังไม่ถึง 06:00 ยังเป็นสมาชิกอยู่
     *     ได้เวลาหมด    28/11 06:00       ← ใช้กฎเดียวกับกรณี 2 คือบวก 30 วันจากเวลาหมดเดิม
     *
     *   กรณี 4: หมดไปแล้ว ค่อยมาสมัครใหม่
     *     เดิมหมด       29/10 06:00
     *     กดสมัครตอน    29/10 18:00       ← เลย 06:00 มาแล้ว ตอนนี้ไม่เป็นสมาชิก
     *     ได้เวลาหมด    28/11 18:00       ← นับ 30 วันใหม่จากตอนที่กด (18:00) เหมือนกรณี 1
     *                                       ช่วง 06:00 ถึง 18:00 ที่หลุดสมาชิกไป ไม่ได้คืน
     *
     * @param now เวลาที่สมัคร / ต่ออายุ ปกติใช้ clock.now()
     * @return Account ตัวใหม่ ที่ memberUntil เป็นเวลาหมดใหม่ (ชื่อกับเงินเหมือนเดิม)
     *         เช่น เดิมหมด 29/10 06:00 ต่อก่อนหมด → ได้ Account ที่หมด 28/11 06:00
     * @throws IllegalArgumentException ถ้า now เป็น null
     */
    public Account extendMembership(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        LocalDateTime newUntil;
        if (isMemberAt(now)) {
            // memberUntil.plusDays(MEMBERSHIP_DAYS)
            //   MEMBERSHIP_DAYS = 30 → 29/10 06:00 + 30 วัน = 28/11 06:00
            newUntil = memberUntil.plusDays(MEMBERSHIP_DAYS);
        } else {
            //   now.truncatedTo(ChronoUnit.MINUTES)
            //   truncatedTo = ตัดหน่วยที่เล็กกว่าที่สั่งทิ้งให้เป็น 0 แล้วคืนเวลาตัวใหม่ (ตัวเดิมไม่เปลี่ยน) ->"หน่วยที่เล็กกว่าที่สั่ง" หมายความว่า ถ้าสั่ง MINUTES อะไรที่เล็กกว่านาทีจะกลายเป็น 0 หมด 
            //   ChronoUnit.MINUTES = สั่งให้เก็บไว้ถึงหลักนาที → วินาทีกับเสี้ยววินาทีโดนตัดทิ้ง
            //   เช่น now = 29/09 06:37:45.123 → ได้ 29/09 06:37:00
            //   ตัดเพื่อให้เวลาหมดตรงกับป้ายบนจอที่โชว์แค่ชั่วโมง:นาที
            //  .plusDays(MEMBERSHIP_DAYS)
            //   MEMBERSHIP_DAYS = 30 → 29/09 06:37 + 30 วัน = 29/10 06:37
            newUntil = now.truncatedTo(ChronoUnit.MINUTES).plusDays(MEMBERSHIP_DAYS);
        }
        // new Account(username, balance, newUntil)
        //   username = คนเดิม
        //   balance  = เงินเท่าเดิม (ไฟล์นี้ไม่ตัดเงิน)
        //   newUntil = เวลาหมดสมาชิกใหม่ เช่น 29/10 06:00
        return new Account(username, balance, newUntil);
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     * ถ้า assert ไหนไม่จริง แปลว่าโค้ดใน constructor ผิดเอง
     */
    private void checkRep() {
        assert username != null && !username.isBlank();
        assert balance >= 0;
    }
}