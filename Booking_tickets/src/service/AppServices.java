//AppServices <- สร้างของหลังบ้านทุกตัวครั้งเดียวตอนเปิดโปรแกรม แล้วให้ทุกหน้าจอหยิบไปใช้ตัวเดียวกัน
/*
    ไฟล์นี้มีไว้ทำไม:
      ถ้าแต่ละหน้าจอ (LoginFrame, MainFrame, SeatFrame) สร้าง AppClock / MovieService / AccountService เอง
      จะเกิด 2 ปัญหา
        1. ได้นาฬิกาหลายตัว ตอน demo ตั้งเวลาในหน้าหนึ่ง อีกหน้าไม่เปลี่ยนตาม เวลาไม่ตรงกัน
        2. ทุกหน้าต้องเขียนโค้ดหาโฟลเดอร์ data และสร้างตัวอ่านไฟล์ CSV ซ้ำกัน
      ไฟล์นี้จึงสร้างทุกอย่าง "ครั้งเดียว" ตอนเปิดโปรแกรม แล้วส่งตัว app นี้ต่อกันไปทุกหน้าจอ
      ทุกหน้าจอเลยใช้นาฬิกาตัวเดียวกัน และอ่าน/เขียนไฟล์ชุดเดียวกัน

    หน้าจอหยิบของได้ 6 อย่าง:
      auth()      → AuthService     ใช้ในหน้า Login / สมัคร
      movies()    → MovieService    ใช้ในหน้าเลือกหนัง (หนัง, รอบ, รอบไหนยังจองได้)
      accounts()  → AccountService  ใช้แสดงเงินคงเหลือ, เติมเงิน, สมัครสมาชิก
      bookings()  → BookingService  ใช้ในหน้าเลือกที่นั่ง (ที่นั่งถูกจอง, ยอดเงิน, กดจอง) และหน้าประวัติ
      layout()    → HallLayout      ใช้วาดผังที่นั่ง (แถว A–F, ทางเดินหลังที่นั่งเลข 5)
      clock()     → AppClock        ใช้ปุ่มตั้งเวลา / รีเซ็ตเวลาตอน demo (ทุกหน้าเห็นเวลาเดียวกัน)

    ตัวอย่างการใช้ในหน้าจอ:

        // LoginFrame.main : สร้าง app ครั้งเดียวตอนเปิดโปรแกรม
        public static void main(String[] args) {
            AppServices app = AppServices.create();
            SwingUtilities.invokeLater(() -> new LoginFrame(app).setVisible(true));
        }

        // LoginFrame ตอนกดเข้าสู่ระบบ : ส่ง app ตัวเดิมต่อไปหน้าถัดไป ห้าม create() ใหม่
        User user = app.auth().login(username, password);
        new MainFrame(user, app).setVisible(true);

        // MainFrame : หยิบ service จาก app มาใช้
        app.accounts().accountOf(user).balance();            // แสดง "Balance: 300 THB"
        app.bookings().bookedSeats(showtime);                // ที่นั่งที่ถูกจองแล้ว (ปุ่มสีเทา)
        app.bookings().book(user, showtime, chosenSeats);    // ปุ่มยืนยันการจอง
 */

package service;

import java.nio.file.Files;              // ใช้เช็กว่ามีโฟลเดอร์ Booking_tickets/data อยู่จริงไหม (ใน create())
import java.nio.file.Path;               // ใช้เก็บทางไปหาโฟลเดอร์ data และไฟล์ CSV แต่ละไฟล์
import model.HallLayout;                 // ผังที่นั่งในโรง สร้างที่นี่ตัวเดียว ให้ BookingService กับหน้าจอใช้ร่วมกัน
import repository.CsvAccountRepository;  // ตัวอ่าน/เขียนบัญชีเงินในไฟล์ accounts.csv
import repository.CsvBookingRepository;  // ตัวอ่าน/เขียนการจองในไฟล์ bookings.csv
import repository.CsvMovieRepository;    // ตัวอ่านหนัง รอบฉาย และหนังที่ฉายแต่ละวัน จาก movies.csv / schedule.csv / lineup.csv
import repository.CsvUserRepository;     // ตัวอ่าน/เขียนผู้ใช้ในไฟล์ users.csv
import repository.MovieRepository;       // ชนิด interface ของที่เก็บหนัง ใช้ประกาศตัวแปรที่ MovieService กับ BookingService ใช้ร่วมกัน

/**
 * ตัวรวมของหลังบ้านทั้งหมด สร้างครั้งเดียวตอนเปิดโปรแกรม
 * ทุก service ใช้ AppClock ตัวเดียวกัน และอ่าน/เขียนไฟล์ในโฟลเดอร์ data เดียวกัน
 * เป็นที่เดียวในโปรแกรมที่ new ตัวอ่านไฟล์จริง (CsvXxxRepository) แล้วส่งเข้า service ทาง constructor
 * service จึงรู้จักแค่ interface ไม่ต้องรู้ว่าข้อมูลเก็บในไฟล์ CSV (SC5 DIP)
 */
public final class AppServices {

    // AF: ชุดของหลังบ้านของโปรแกรม 1 ชุด ที่อ่าน/เขียนไฟล์ในโฟลเดอร์ data เดียวกัน และใช้นาฬิกา clock ตัวเดียวกัน
    // RI: ทุก field ไม่เป็น null
    //     movies, accounts, bookings ใช้ clock ตัวเดียวกันกับ field clock
    // Safety from rep exposure: field เป็น private final ไม่มีเมธอดเปลี่ยน field
    //     เมธอดคืน service ตัวจริงออกไปโดยตั้งใจ เพื่อให้ทุกหน้าจอใช้ตัวเดียวกัน
    //     (ถ้าคืนตัวสำเนา นาฬิกาแต่ละหน้าจะไม่ตรงกัน ซึ่งเป็นปัญหาที่ไฟล์นี้มีไว้แก้)
    // Thread safety: field ไม่เปลี่ยนหลังสร้าง ส่วนความปลอดภัยของแต่ละ service ดูที่ไฟล์นั้น ๆ

    // ชื่อโฟลเดอร์และชื่อไฟล์ ตั้งเป็นค่าคงที่ไว้ที่เดียว ถ้าวันหลังเปลี่ยนชื่อไฟล์ แก้บรรทัดเดียวพอ
    /** ชื่อโฟลเดอร์ที่เก็บไฟล์ CSV ทั้งหมด */
    private static final String DATA_FOLDER = "data";
    /** ชื่อโฟลเดอร์โปรเจกต์ ใช้ตอนรันจาก VS Code ที่เปิดโฟลเดอร์ project (อยู่เหนือ Booking_tickets ขึ้นไป 1 ชั้น) */
    private static final String PROJECT_FOLDER = "Booking_tickets";
    /** ไฟล์ผู้ใช้: ชื่อ, รหัส, บทบาท */
    private static final String USERS_FILE = "users.csv";
    /** ไฟล์หนังทุกเรื่อง: รหัส, ชื่อ, ความยาว, รูปโปสเตอร์ */
    private static final String MOVIES_FILE = "movies.csv";
    /** ไฟล์ตารางฉาย: โรง, เวลา, เรื่องที่เท่าไหร่ (1–5) */
    private static final String SCHEDULE_FILE = "schedule.csv";
    /** ไฟล์หนังที่ฉายแต่ละวัน: วันที่, รหัสหนัง 5 เรื่อง (admin เป็นคนตั้ง) */
    private static final String LINEUP_FILE = "lineup.csv";
    /** ไฟล์บัญชีเงิน: ชื่อ, เงินคงเหลือ, วันหมดสมาชิก */
    private static final String ACCOUNTS_FILE = "accounts.csv";
    /** ไฟล์การจอง: รหัสตั๋ว, ชื่อ, รอบ, ที่นั่ง, ราคา, เวลาที่จอง */
    private static final String BOOKINGS_FILE = "bookings.csv";

    /** นาฬิกาตัวเดียวของโปรแกรม ทุก service ถามเวลาจากตัวนี้ */
    private final AppClock clock;
    /** ผังที่นั่งในโรง ตัวเดียวที่ทั้ง BookingService และหน้าจอใช้ */
    private final HallLayout layout;
    /** บริการ login / สมัคร */
    private final AuthService auth;
    /** บริการหนังและรอบฉาย */
    private final MovieService movies;
    /** บริการเงินและสมาชิก */
    private final AccountService accounts;
    /** บริการจองตั๋ว */
    private final BookingService bookings;

    /**
     * สร้างของหลังบ้านทุกตัวจากโฟลเดอร์ data ที่ให้มา
     * เป็น private เพื่อบังคับให้คนข้างนอกสร้างผ่าน create() หรือ create(dataFolder) เท่านั้น
     *
     * วิธีทำงาน:
     *   1. สร้างนาฬิกา 1 ตัว และผังโรง 1 ตัว
     *   2. สร้างตัวอ่าน/เขียนไฟล์ CSV แต่ละไฟล์ในโฟลเดอร์ dataFolder
     *   3. สร้าง service ทุกตัว แล้วส่งตัวอ่านไฟล์กับนาฬิกาตัวเดียวกันเข้าไป
     *   4. เรียก checkRep() ตรวจว่าไม่มีอะไรเป็น null
     *
     * ตัวอย่าง: dataFolder = Booking_tickets/data
     *   → users.csv    อยู่ที่ Booking_tickets/data/users.csv
     *   → movies.csv   อยู่ที่ Booking_tickets/data/movies.csv
     *   → ... ครบ 6 ไฟล์ในโฟลเดอร์เดียวกัน
     *
     * @param dataFolder โฟลเดอร์ที่มีไฟล์ CSV
     */
    private AppServices(Path dataFolder) {
        // นาฬิกาตัวเดียวของโปรแกรม เดี๋ยวส่งตัวนี้ให้ทุก service ที่ต้องรู้เวลา
        this.clock = new AppClock();
        // ผังโรงตัวเดียว เดี๋ยวส่งให้ BookingService และให้หน้าจอหยิบผ่าน layout()
        this.layout = new HallLayout();

        // บริการ login / สมัคร
        // new AuthService(ตัวอ่าน/เขียนผู้ใช้)
        //   dataFolder.resolve(USERS_FILE) = ต่อชื่อไฟล์ท้ายโฟลเดอร์
        //     เช่น Booking_tickets/data + "users.csv" → Booking_tickets/data/users.csv
        this.auth = new AuthService(new CsvUserRepository(dataFolder.resolve(USERS_FILE)));

        // ตัวอ่านหนัง สร้างตัวเดียวแล้วใช้ร่วมกัน 2 ที่ ได้แก่ MovieService (ใช้ทำหน้าเลือกหนัง) กับ BookingService (ใช้ทำหน้าประวัติการจอง)
        //   MovieService ใช้ทำหน้าเลือกหนัง / BookingService ใช้หารอบของตั๋วในหน้าประวัติ
        // new CsvMovieRepository(ไฟล์หนัง, ไฟล์ตารางฉาย, ไฟล์หนังที่ฉายแต่ละวัน)
        //   เช่น data/movies.csv, data/schedule.csv, data/lineup.csv
        MovieRepository movieRepository = new CsvMovieRepository(dataFolder.resolve(MOVIES_FILE),
                dataFolder.resolve(SCHEDULE_FILE), dataFolder.resolve(LINEUP_FILE));

        // บริการหนังและรอบฉาย (หน้าเลือกหนัง)
        // new MovieService(ตัวอ่านหนังข้างบน, นาฬิกาตัวเดียวกัน ไว้เช็กว่าวันนี้วันไหนและรอบเริ่มไปหรือยัง)
        this.movies = new MovieService(movieRepository, clock);

        // บริการเงินและสมาชิก (เติมเงิน, สมัครสมาชิก และ BookingService ใช้ตัดเงินตอนจอง)
        // new AccountService(ตัวอ่าน/เขียนบัญชีใน accounts.csv, นาฬิกาตัวเดียวกัน ไว้เช็กวันหมดสมาชิก)
        this.accounts = new AccountService(new CsvAccountRepository(dataFolder.resolve(ACCOUNTS_FILE)), clock);

        // บริการจองตั๋ว ต้องใช้ของหลายอย่าง จึงส่งเข้าไป 6 ตัว
        // new BookingService(ที่เก็บการจอง, ที่เก็บหนัง, บริการเงิน, ตัวคิดราคา, ผังโรง, นาฬิกา)
        //   new CsvBookingRepository(...) = ตัวอ่าน/เขียนตั๋วใน bookings.csv
        //   movieRepository               = ตัวอ่านหนังตัวเดียวกับข้างบน ไว้หารอบของตั๋ว
        //   accounts                      = บริการเงินตัวเดียวกับที่หน้าจอใช้ ตัดเงินแล้วยอดบนจอจะตรงกัน
        //   new PriceCalculator()         = ตัวคิดราคา (160 บาท, VIP +40, สมาชิกลด 10%)
        //   layout                        = ผังโรงตัวเดียวกับที่หน้าจอใช้วาดที่นั่ง
        //   clock                         = นาฬิกาตัวเดียวกัน ไว้เช็กว่ารอบเริ่มไปแล้วหรือยัง
        this.bookings = new BookingService(new CsvBookingRepository(dataFolder.resolve(BOOKINGS_FILE)),
                movieRepository, accounts, new PriceCalculator(), layout, clock);

        checkRep();
    }

    /**
     * สร้างของหลังบ้านทั้งหมด โดยหาโฟลเดอร์ data เอง ใช้ในโปรแกรมจริง (LoginFrame.main)
     *
     * ทำไมต้องหาเอง:
     *   VS Code เปิดโฟลเดอร์ D:\6821651329\project
     *   NetBeans เปิดโฟลเดอร์ D:\6821651329\project\Booking_tickets
     *   สองโปรแกรมเริ่มรันจากคนละโฟลเดอร์ ทางไปหาโฟลเดอร์ data จึงไม่เหมือนกัน
     *   ถ้าเขียนทางไว้แบบเดียว จะมีโปรแกรมหนึ่งหาไฟล์ CSV ไม่เจอ
     *
     * วิธีทำงาน:
     *   1. ลองหาโฟลเดอร์ชื่อ Booking_tickets/data (นับจากโฟลเดอร์ที่โปรแกรมเริ่มรัน)
     *      ถ้าเจอ แปลว่ารันจาก VS Code (เริ่มที่ project)
     *      → อ่านไฟล์ CSV จาก D:\6821651329\project\Booking_tickets\data
     *   2. ถ้าไม่เจอ แปลว่ารันจาก NetBeans (เริ่มที่ Booking_tickets อยู่แล้ว)
     *      → อ่านไฟล์ CSV จากโฟลเดอร์ data ที่อยู่ข้างในเลย
     *        ซึ่งก็คือ D:\6821651329\project\Booking_tickets\data เหมือนกัน
     *
     * สรุป: ไม่ว่าจะรันจากโปรแกรมไหน ได้โฟลเดอร์ data ตัวเดียวกันเสมอ
     *
     * @return ของหลังบ้านชุดใหม่ 1 ชุด
     */
    public static AppServices create() {
        // ประกอบทางไปหาโฟลเดอร์ (ยังไม่ได้เช็กว่ามีจริง แค่ต่อชื่อ)
        // Path.of("Booking_tickets", "data") → Booking_tickets/data
        Path fromProjectRoot = Path.of(PROJECT_FOLDER, DATA_FOLDER);

        // เช็กว่าโฟลเดอร์ Booking_tickets/data มีอยู่จริงไหม นับจากโฟลเดอร์ที่โปรแกรมเริ่มรัน
        //   รันจาก VS Code   (เริ่มที่ project)          → project\Booking_tickets\data มีจริง → เจอ
        //   รันจาก NetBeans  (เริ่มที่ Booking_tickets)  → Booking_tickets\Booking_tickets\data ไม่มี → ไม่เจอ
        if (Files.isDirectory(fromProjectRoot)) {
            // เจอ → ใช้ Booking_tickets/data
            return new AppServices(fromProjectRoot);
        }
        // ไม่เจอ → แปลว่าอยู่ใน Booking_tickets อยู่แล้ว ใช้ data ที่อยู่ข้างในได้เลย
        return new AppServices(Path.of(DATA_FOLDER));
    }

    /**
     * สร้างของหลังบ้านทั้งหมดจากโฟลเดอร์ data ที่ระบุเอง
     * มีไว้ใช้ตอนทดสอบ จะได้ใช้โฟลเดอร์ชั่วคราว ไม่ไปแก้ไฟล์ CSV จริงของโปรแกรม
     *
     * ตัวอย่าง:
     *   AppServices.create(Path.of("D:/test/data")) → อ่าน/เขียนไฟล์ในโฟลเดอร์ D:/test/data
     *   AppServices.create(null)                    → throw "dataFolder must not be null"
     *
     * @param dataFolder โฟลเดอร์ที่มีไฟล์ CSV ห้าม null
     * @return ของหลังบ้านชุดใหม่ 1 ชุด
     * @throws IllegalArgumentException ถ้า dataFolder เป็น null
     */
    public static AppServices create(Path dataFolder) {
        // ไม่รู้ว่าไฟล์อยู่ไหน = ทำงานต่อไม่ได้ หยุดตั้งแต่ตรงนี้
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder must not be null");
        }
        return new AppServices(dataFolder);
    }

    /**
     * นาฬิกาตัวเดียวของโปรแกรม ให้ปุ่มตั้งเวลา / รีเซ็ตเวลาตอน demo ใช้
     * เช่น app.clock().setTo(8/10 09:59) → ทุกหน้าจอและทุก service เห็นเวลา 09:59 พร้อมกัน
     *
     * @return AppClock ตัวที่ทุก service ใช้อยู่
     */
    public AppClock clock() {
        return clock;
    }

    /**
     * ผังที่นั่งในโรง ให้หน้าเลือกที่นั่งใช้วาดปุ่มที่นั่ง
     * เช่น app.layout().rows() → [A, B, C, D, E, F]
     *
     * @return HallLayout ตัวเดียวกับที่ BookingService ใช้เช็กว่าที่นั่งมีจริง
     */
    public HallLayout layout() {
        return layout;
    }

    /**
     * บริการ login / สมัคร ให้หน้า LoginFrame ใช้
     * เช่น app.auth().login("admin", "admin1") → User admin (role ADMIN)
     *
     * @return AuthService ที่อ่าน/เขียน users.csv
     */
    public AuthService auth() {
        return auth;
    }

    /**
     * บริการหนังและรอบฉาย ให้หน้าเลือกหนังใช้
     * เช่น app.movies().movies() → หนังทั้ง 15 เรื่อง
     *
     * @return MovieService ที่ใช้นาฬิกาตัวเดียวกับทั้งโปรแกรม
     */
    public MovieService movies() {
        return movies;
    }

    /**
     * บริการเงินและสมาชิก ให้หน้าจอแสดงเงิน เติมเงิน และสมัครสมาชิก
     * เช่น app.accounts().topUp(user, 300) → เงินเพิ่ม 300 และบันทึกลง accounts.csv
     *
     * @return AccountService ตัวเดียวกับที่ BookingService ใช้ตัดเงิน (ยอดบนจอจึงตรงกันเสมอ)
     */
    public AccountService accounts() {
        return accounts;
    }

    /**
     * บริการจองตั๋ว ให้หน้าเลือกที่นั่งและหน้าประวัติใช้
     * เช่น app.bookings().book(user, showtime, seats) → ได้ตั๋ว B1 และตัดเงิน
     *
     * @return BookingService ที่ใช้นาฬิกา ผังโรง และบริการเงินตัวเดียวกับทั้งโปรแกรม
     */
    public BookingService bookings() {
        return bookings;
    }

    /**
     * ตรวจ RI ว่าของทุกตัวถูกสร้างครบ ไม่มีตัวไหนเป็น null (SC4) ทำงานเมื่อรันด้วย -ea
     */
    private void checkRep() {
        assert clock != null && layout != null;
        assert auth != null && movies != null && accounts != null && bookings != null;
    }
}