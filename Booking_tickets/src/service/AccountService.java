//AccountService <- ตัวที่หน้าจอคุยด้วยเรื่องเงินกับสมาชิก เอาบัญชีจาก AccountRepository กับเวลาจาก AppClock มาจัดการให้
/*
    หน้าจอใช้ 4 เมธอดนี้ (ทุกตัวรับ User ที่ login แล้ว):
    accountOf(user)      → บัญชีของคนนี้ ใช้โชว์ "เงิน 300 ฿" และป้าย "สมาชิก ถึง 29/10 06:00" ที่แถบบน
    isMember(user)       → true = เป็นสมาชิกตอนนี้ (โชว์ป้าย) / false = ไม่เป็น (โชว์ปุ่ม "★ สมัครสมาชิก")
    topUp(user, จำนวน)   → เติมเงิน แล้วบันทึกลงไฟล์ คืนบัญชีใหม่ให้หน้าจออัปเดตแถบบน
    subscribe(user)      → ตัดเงิน 99 บาท ต่อสมาชิก 30 วัน แล้วบันทึกลงไฟล์ คืนบัญชีใหม่

    ค่าคงที่ที่หน้าจอใช้ได้:
    AccountService.MEMBERSHIP_PRICE = 99   → ใช้เขียนในกล่องสมัคร "99 ฿ / 30 วัน" และคำนวณ "เหลือหลังสมัคร"

    ตัวอย่างการใช้ (ในโปรแกรมจริง, ตอนนี้ 29/09/2027 06:00):
        AppClock clock = new AppClock();
        AccountRepository repo = new CsvAccountRepository(Path.of("data", "accounts.csv"));
        AccountService accountService = new AccountService(repo, clock);

        User user = authService.login("somchai", "abc123");    // ได้ User มาจากการ login เท่านั้น
        accountService.accountOf(user);                         // ยังไม่เคยเติม → Account[somchai, 0, null]
        accountService.isMember(user);                          // false → โชว์ปุ่ม "★ สมัครสมาชิก"
        accountService.topUp(user, 300);                        // Account[somchai, 300, null]  accounts.csv: somchai,300,
        accountService.subscribe(user);                         // Account[somchai, 201, 2027-10-29T06:00]
        accountService.isMember(user);                          // true → โชว์ป้าย "สมาชิก ถึง 29/10 06:00"
        accountService.topUp(user, 6000);                       // พัง IllegalArgumentException: เกิน 5,000 ต่อครั้ง
 */

package service;

import java.io.IOException;              // error ตอนอ่าน/เขียน accounts.csv ไม่ได้ (ส่งต่อจาก AccountRepository ให้หน้าจอ)
import model.Account;                    // class บัญชี อยู่คนละ package เลยต้อง import
import model.User;                       // ผู้ใช้ที่ login แล้ว รับเข้ามาทุกเมธอด
import repository.AccountRepository;     // ที่เก็บบัญชี (interface) รับเข้ามาทาง constructor

/**
 * บริการเรื่องเงินในแอปและสมาชิก สำหรับหน้าจอ
 * ไม่อ่าน/เขียนไฟล์เอง ถามจาก AccountRepository และถามเวลาจาก AppClock
 * กฎเงิน (เติมครั้งละ 1–5,000, ห้ามติดลบ, สมาชิก 30 วัน) อยู่ใน Account ไฟล์นี้แค่เรียกใช้แล้วบันทึก
 */
public class AccountService {

    // AF: บริการบัญชีที่อ่าน/บันทึกบัญชีผ่าน accountRepository และใช้เวลาปัจจุบันจาก clock
    // RI: accountRepository และ clock ไม่เป็น null
    // Safety from rep exposure: field เป็น private final, Account ที่คืนออกไปเป็น record แก้ไม่ได้
    // Thread safety: ไม่มี field ที่แก้ค่าได้ (final ทั้งหมด), AppClock ล็อกของตัวเองอยู่แล้ว
    //                เรียกจากหน้าจอ Swing ทีละคำสั่งบน thread เดียว (แบบเดียวกับ CsvAccountRepository)

    /** ค่าสมัครสมาชิก 1 ครั้ง (30 วัน) เป็นบาท หน้าจอใช้โชว์ราคาและคำนวณเงินหลังสมัคร */
    public static final int MEMBERSHIP_PRICE = 99;

    /** ที่เก็บบัญชี เช่น CsvAccountRepository */
    private final AccountRepository accountRepository;
    /** นาฬิกาตัวเดียวของโปรแกรม */
    private final AppClock clock;

    /**
     * รับที่เก็บบัญชีกับนาฬิกาเข้ามาทาง constructor ไม่ new เอง (SC5 DIP)
     * โปรแกรมจริงส่ง CsvAccountRepository ตอนทดสอบส่งตัวปลอมแทนได้
     *
     * ตัวอย่าง:
     *   new AccountService(new CsvAccountRepository(Path.of("data", "accounts.csv")), new AppClock())
     *   new AccountService(null, clock) → throw "accountRepository must not be null"
     *
     * @param accountRepository ที่เก็บบัญชี ห้าม null
     * @param clock             นาฬิกาของโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า accountRepository หรือ clock เป็น null
     */
    public AccountService(AccountRepository accountRepository, AppClock clock) {
        if (accountRepository == null) {
            throw new IllegalArgumentException("accountRepository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.accountRepository = accountRepository;
        this.clock = clock;
        checkRep();
    }

    /**
     * บัญชีของผู้ใช้คนนี้
     *
     * วิธีทำงาน:
     *   1. user เป็น null → throw
     *   2. หาใน accountRepository ด้วยชื่อของ user
     *   3. เจอ → คืนบัญชีนั้น
     *   4. ไม่เจอ (สมัครแล้วแต่ยังไม่เคยเติมเงินหรือสมัครสมาชิก) → คืน Account.newFor(ชื่อ) คือเงิน 0 ไม่เป็นสมาชิก
     *      ยังไม่บันทึกลงไฟล์ จะบันทึกตอนเติมเงินหรือสมัครสมาชิกครั้งแรก
     *
     * ตัวอย่าง:
     *   accounts.csv มี somchai,300,  → accountOf(somchai) → Account[somchai, 300, null]
     *   accounts.csv ไม่มี nok       → accountOf(nok)     → Account[nok, 0, null]  (ไฟล์ไม่เปลี่ยน)
     *
     * @param user ผู้ใช้ที่ login แล้ว ห้าม null
     * @return บัญชีของผู้ใช้คนนี้ (ไม่เคยคืน null)
     * @throws IOException ถ้าอ่าน accounts.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด (หน้าจอเอา getMessage() ไปโชว์)
     * @throws IllegalArgumentException ถ้า user เป็น null
     */
    public Account accountOf(User user) throws IOException {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        // accountRepository.findByUsername(user.username())
        //   user.username() = ชื่อของคนที่ login เช่น "somchai"
        //   ได้ Account ถ้ามีแถวใน accounts.csv / ได้ null ถ้ายังไม่มีแถว
        Account account = accountRepository.findByUsername(user.username());
        if (account == null) {
            // Account.newFor(user.username())
            //   user.username() = ชื่อ เช่น "nok" → ได้ Account[nok, 0, null]
            return Account.newFor(user.username());
        }
        return account;
    }

    /**
     * ตอนนี้ผู้ใช้คนนี้เป็นสมาชิกไหม
     *
     * วิธีทำงาน: หาบัญชีด้วย accountOf() → ถามบัญชีว่าเป็นสมาชิก ณ เวลาตอนนี้ของ clock ไหม
     *
     * ตัวอย่าง (somchai สมาชิกหมด 29/10/2027 06:00):
     *   ตอนนี้ 15/10 12:00 → true   → หน้าจอโชว์ป้าย "สมาชิก ถึง 29/10 06:00"
     *   ตอนนี้ 29/10 06:00 → false  → หน้าจอโชว์ปุ่ม "★ สมัครสมาชิก"
     *   nok ไม่เคยสมัคร   → false
     *
     * @param user ผู้ใช้ที่ login แล้ว ห้าม null
     * @return true ถ้าตอนนี้เป็นสมาชิก
     * @throws IOException ถ้าอ่าน accounts.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด
     * @throws IllegalArgumentException ถ้า user เป็น null
     */
    public boolean isMember(User user) throws IOException {
        // accountOf(user).isMemberAt(clock.now())
        //   accountOf(user) = บัญชีของคนนี้ เช่น Account[somchai, 201, 2027-10-29T06:00]
        //   clock.now()     = เวลาตอนนี้ของโปรแกรม เช่น 2027-10-15T12:00
        return accountOf(user).isMemberAt(clock.now());
    }

    /**
     * เติมเงิน แล้วบันทึกลงไฟล์
     *
     * วิธีทำงาน:
     *   1. หาบัญชีด้วย accountOf() (ยังไม่เคยเติม ได้บัญชีเงิน 0)
     *   2. เติมเงินด้วย account.topUp(amount) ได้บัญชีตัวใหม่ (ผิดกฎ เช่น เกิน 5,000 → Account throw ตรงนี้ ไม่บันทึก)
     *   3. บันทึกบัญชีตัวใหม่ลงไฟล์
     *   4. คืนบัญชีตัวใหม่ ให้หน้าจอเอาไปอัปเดตยอดเงินที่แถบบน
     *
     * ตัวอย่าง:
     *   nok ยังไม่เคยเติม,   topUp(nok, 300)   → Account[nok, 300, null]    accounts.csv เพิ่ม nok,300,
     *   somchai มี 300,     topUp(somchai, 200) → Account[somchai, 500, null] แถว somchai เปลี่ยนเป็น 500
     *   somchai มี 500,     topUp(somchai, 6000) → throw "top-up must be 1-5000: 6000" ไฟล์ไม่เปลี่ยน
     *
     * @param user   ผู้ใช้ที่ login แล้ว ห้าม null
     * @param amount จำนวนเงินที่เติม 1 ถึง 5,000
     * @return บัญชีหลังเติมเงิน
     * @throws IOException ถ้าอ่าน/เขียน accounts.csv ไม่ได้
     * @throws IllegalArgumentException ถ้า user เป็น null หรือ amount อยู่นอกช่วง 1 ถึง 5,000
     */
    public Account topUp(User user, int amount) throws IOException {
        // accountOf(user).topUp(amount)
        //   accountOf(user) = บัญชีตอนนี้ เช่น Account[somchai, 300, null]
        //   amount          = จำนวนที่เติม เช่น 200 → ได้ Account[somchai, 500, null]
        Account updated = accountOf(user).topUp(amount);
        // accountRepository.save(updated)
        //   updated = บัญชีหลังเติม → เขียนทับแถวเดิม (หรือเพิ่มแถวใหม่ถ้ายังไม่มี)
        accountRepository.save(updated);
        return updated;
    }

    /**
     * สมัคร / ต่ออายุสมาชิก: ตัดเงิน 99 บาท ต่ออายุ 30 วัน แล้วบันทึกลงไฟล์
     *
     * วิธีทำงาน:
     *   1. หาบัญชีด้วย accountOf()
     *   2. ตัดเงิน 99 ด้วย account.pay(MEMBERSHIP_PRICE) (เงินไม่ถึง 99 → Account throw ตรงนี้ ไม่บันทึก)
     *   3. ต่ออายุด้วย extendMembership(clock.now())
     *      ยังเป็นสมาชิกอยู่ → ต่อจากเวลาหมดเดิม / ไม่เคยเป็นหรือหมดแล้ว → นับจากตอนนี้
     *   4. บันทึกบัญชีตัวใหม่ลงไฟล์ แล้วคืนให้หน้าจอเปลี่ยนปุ่มเป็นป้าย "สมาชิก ถึง ..."
     *
     * หน้าจอควรเช็กก่อนว่า balance() >= MEMBERSHIP_PRICE ถ้าไม่ถึงให้ปุ่มยืนยันกดไม่ได้
     * throw ตรงนี้เป็นด่านสุดท้าย กันเงินติดลบ
     *
     * ตัวอย่าง (ตอนนี้ 29/09/2027 06:00):
     *   somchai มี 300 ไม่เคยสมัคร        → Account[somchai, 201, 2027-10-29T06:00]
     *   somchai มี 201 หมด 29/10 06:00,
     *     สมัครอีกตอน 15/10 20:00        → Account[somchai, 102, 2027-11-28T06:00]  (ต่อจากเวลาหมดเดิม)
     *   nok มี 50                        → throw "not enough balance: has 50, needs 99" ไฟล์ไม่เปลี่ยน
     *
     * @param user ผู้ใช้ที่ login แล้ว ห้าม null
     * @return บัญชีหลังสมัคร
     * @throws IOException ถ้าอ่าน/เขียน accounts.csv ไม่ได้
     * @throws IllegalArgumentException ถ้า user เป็น null หรือเงินไม่ถึง 99 บาท
     */
    public Account subscribe(User user) throws IOException {
        // accountOf(user).pay(MEMBERSHIP_PRICE).extendMembership(clock.now())
        //   accountOf(user)          = บัญชีตอนนี้ เช่น Account[somchai, 300, null]
        //   .pay(MEMBERSHIP_PRICE)   = ตัด 99 → Account[somchai, 201, null]
        //   .extendMembership(now)   = now = clock.now() เช่น 29/09 06:00 → Account[somchai, 201, 2027-10-29T06:00]
        Account updated = accountOf(user).pay(MEMBERSHIP_PRICE).extendMembership(clock.now());
        accountRepository.save(updated);
        return updated;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     */
    private void checkRep() {
        assert accountRepository != null;
        assert clock != null;
    }
}