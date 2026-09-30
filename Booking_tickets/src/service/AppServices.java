//AppServices <- สร้างของหลังบ้านทุกตัวครั้งเดียวตอนเปิดโปรแกรม ใช้ AppClock ตัวเดียวกันทั้งโปรแกรม แล้วให้หน้าจอหยิบไปใช้
/*
    ทำไมต้องมี:
    ถ้าแต่ละหน้าจอ new AppClock / new MovieService เอง จะได้นาฬิกาหลายตัว
    กดตั้งเวลาในหน้าหนึ่ง อีกหน้าไม่เปลี่ยนตาม และต้องเขียนโค้ดหาไฟล์ data ซ้ำทุกหน้า
    ไฟล์นี้สร้างทุกอย่างครั้งเดียว แล้วส่ง app ตัวเดียวต่อกันไปทุกหน้าจอ

    มี 6 เมธอดให้หยิบของ:
    auth()      → AuthService     หน้า Login / สมัคร
    movies()    → MovieService    หน้าเลือกหนัง (วันที่, รอบ, รอบไหนจองได้)
    accounts()  → AccountService  แถบบน (เงิน, ป้ายสมาชิก), เติมเงิน, สมัครสมาชิก
    bookings()  → BookingService  หน้าเลือกที่นั่ง (ที่นั่งถูกจอง, ยอดเงิน, จอง), หน้าประวัติ
    layout()    → HallLayout      วาดผังที่นั่ง (แถว A–F, ทางเดินหลังเลข 5)
    clock()     → AppClock        ปุ่มตั้งเวลา / รีเซ็ตเวลา (ทุกหน้าเห็นเวลาเดียวกัน)

    วิธีใช้ในหน้าจอ (ตัวอย่างให้ก๊อปไปแก้ LoginFrame / MainFrame):

        // LoginFrame.main
        public static void main(String[] args) {
            AppServices app = AppServices.create();          // สร้างครั้งเดียวตรงนี้
            SwingUtilities.invokeLater(() -> new LoginFrame(app).setVisible(true));
        }

        // LoginFrame ตอนกดเข้าสู่ระบบ
        User user = app.auth().login(username, password);
        new MainFrame(user, app).setVisible(true);           // ส่ง app ตัวเดิมต่อไป ไม่ new ใหม่

        // MainFrame
        app.movies().bookableDates();                        // ปุ่มวันที่
        app.accounts().accountOf(user).balance();            // "เงิน 300 ฿"
        app.bookings().bookedSeats(showtime);                // ที่นั่งสีเทา
        app.bookings().book(user, showtime, chosenSeats);    // ปุ่มยืนยันการจอง

    หาโฟลเดอร์ data เองแบบเดียวกับ resolveDataPath ใน LoginFrame:
        รันจาก D:\6821651329\project                   → ใช้ Booking_tickets\data
        รันจาก D:\6821651329\project\Booking_tickets   → ใช้ data
 */

package service;

import java.nio.file.Files;              // เช็กว่ามีโฟลเดอร์ Booking_tickets/data ไหม
import java.nio.file.Path;               // ตำแหน่งโฟลเดอร์ data และไฟล์ CSV แต่ละไฟล์
import model.HallLayout;                 // ผังโรง
import repository.CsvAccountRepository;  // ที่เก็บบัญชีในไฟล์ accounts.csv
import repository.CsvBookingRepository;  // ที่เก็บการจองในไฟล์ bookings.csv
import repository.CsvMovieRepository;    // อ่านหนังและรอบฉายจาก movies.csv / schedule.csv
import repository.CsvUserRepository;     // ที่เก็บผู้ใช้ในไฟล์ users.csv
import repository.MovieRepository;       // ชนิด interface ของที่เก็บหนัง (ใช้ร่วมกัน 2 service)

/**
 * ตัวรวมของหลังบ้านทั้งหมด สร้างครั้งเดียวตอนเปิดโปรแกรม
 * ทุก service ใช้ AppClock ตัวเดียวกัน และอ่าน/เขียนไฟล์ในโฟลเดอร์ data เดียวกัน
 * เป็นที่เดียวที่ new ของจริง (CsvXxxRepository) แล้วส่งเข้า service ทาง constructor (SC5 DIP)
 */
public final class AppServices {

    // AF: ชุดของหลังบ้านของโปรแกรม 1 ชุด ที่อ่าน/เขียนไฟล์ในโฟลเดอร์ data เดียวกัน และใช้นาฬิกา clock ตัวเดียวกัน
    // RI: ทุก field ไม่เป็น null
    //     movies, accounts, bookings ใช้ clock ตัวเดียวกันกับ field clock
    // Safety from rep exposure: field เป็น private final ไม่มีเมธอดเปลี่ยน field
    //     เมธอดคืน service ตัวจริงออกไปโดยตั้งใจ เพื่อให้ทุกหน้าจอใช้ตัวเดียวกัน
    //     (ถ้าคืนตัวสำเนา นาฬิกาแต่ละหน้าจะไม่ตรงกัน ซึ่งเป็นปัญหาที่ไฟล์นี้มีไว้แก้)
    // Thread safety: field ไม่เปลี่ยนหลังสร้าง ส่วนความปลอดภัยของแต่ละ service ดูที่ไฟล์นั้น ๆ

    /** ชื่อโฟลเดอร์ข้อมูล */
    private static final String DATA_FOLDER = "data";
    /** ชื่อโฟลเดอร์โปรเจกต์ (ใช้ตอนรันจากโฟลเดอร์ที่อยู่เหนือขึ้นไป) */
    private static final String PROJECT_FOLDER = "Booking_tickets";
    /** ไฟล์ผู้ใช้ */
    private static final String USERS_FILE = "users.csv";
    /** ไฟล์หนัง */
    private static final String MOVIES_FILE = "movies.csv";
    /** ไฟล์ตารางฉาย */
    private static final String SCHEDULE_FILE = "schedule.csv";
    /** ไฟล์บัญชีเงิน */
    private static final String ACCOUNTS_FILE = "accounts.csv";
    /** ไฟล์การจอง */
    private static final String BOOKINGS_FILE = "bookings.csv";

    /** นาฬิกาตัวเดียวของโปรแกรม */
    private final AppClock clock;
    /** ผังโรง */
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
     * เรียกผ่าน create() หรือ create(dataFolder) เท่านั้น (constructor เป็น private)
     *
     * วิธีทำงาน:
     *   1. สร้าง AppClock 1 ตัว และ HallLayout 1 ตัว
     *   2. สร้างที่เก็บข้อมูลแต่ละไฟล์ในโฟลเดอร์ dataFolder
     *   3. สร้าง service ทุกตัว ส่ง clock ตัวเดียวกันเข้าไป
     *   4. เรียก checkRep()
     *
     * ตัวอย่าง: dataFolder = Booking_tickets/data
     *   → users.csv    = Booking_tickets/data/users.csv
     *   → movies.csv   = Booking_tickets/data/movies.csv  ... (ครบ 5 ไฟล์)
     *
     * @param dataFolder โฟลเดอร์ที่มีไฟล์ CSV
     */
    private AppServices(Path dataFolder) {
        this.clock = new AppClock();
        this.layout = new HallLayout();

        // dataFolder.resolve(USERS_FILE)
        //   dataFolder = โฟลเดอร์ data เช่น Booking_tickets/data
        //   USERS_FILE = "users.csv" → ได้ Booking_tickets/data/users.csv
        this.auth = new AuthService(new CsvUserRepository(dataFolder.resolve(USERS_FILE)));

        // ที่เก็บหนังตัวเดียว ใช้ร่วมกันทั้ง MovieService และ BookingService (BookingService ใช้หารอบในหน้าประวัติ)
        MovieRepository movieRepository =
                new CsvMovieRepository(dataFolder.resolve(MOVIES_FILE), dataFolder.resolve(SCHEDULE_FILE));
        this.movies = new MovieService(movieRepository, clock);

        this.accounts = new AccountService(new CsvAccountRepository(dataFolder.resolve(ACCOUNTS_FILE)), clock);

        // new BookingService(ที่เก็บการจอง, ที่เก็บหนัง, บริการเงิน, ตัวคิดราคา, ผังโรง, นาฬิกา)
        //   ทุกตัวเป็นตัวเดียวกับที่ใช้ในไฟล์นี้ เช่น accounts ตัวเดียวกับ accounts() ที่หน้าจอใช้
        this.bookings = new BookingService(new CsvBookingRepository(dataFolder.resolve(BOOKINGS_FILE)),
                movieRepository, accounts, new PriceCalculator(), layout, clock);
        checkRep();
    }

    /**
     * สร้างของหลังบ้านทั้งหมด โดยหาโฟลเดอร์ data เอง ใช้ในโปรแกรมจริง (LoginFrame.main)
     *
     * วิธีทำงาน:
     *   1. ถ้ามีโฟลเดอร์ Booking_tickets/data (รันจาก D:\6821651329\project) → ใช้อันนั้น
     *   2. ไม่มี (รันจาก D:\6821651329\project\Booking_tickets) → ใช้ data
     *
     * ตัวอย่าง:
     *   รันใน VS Code ที่เปิดโฟลเดอร์ project        → ใช้ Booking_tickets/data
     *   รันใน NetBeans ที่เปิดโฟลเดอร์ Booking_tickets → ใช้ data
     *
     * @return ของหลังบ้านชุดใหม่ 1 ชุด
     */
    public static AppServices create() {
        // Path.of(PROJECT_FOLDER, DATA_FOLDER)
        //   PROJECT_FOLDER = "Booking_tickets", DATA_FOLDER = "data" → Booking_tickets/data
        Path fromProjectRoot = Path.of(PROJECT_FOLDER, DATA_FOLDER);
        if (Files.isDirectory(fromProjectRoot)) {
            return new AppServices(fromProjectRoot);
        }
        return new AppServices(Path.of(DATA_FOLDER));
    }

    /**
     * สร้างของหลังบ้านทั้งหมดจากโฟลเดอร์ data ที่ระบุเอง ใช้ตอนทดสอบกับโฟลเดอร์ชั่วคราว
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
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder must not be null");
        }
        return new AppServices(dataFolder);
    }

    /**
     * นาฬิกาตัวเดียวของโปรแกรม ปุ่มตั้งเวลา / รีเซ็ตเวลาใช้ตัวนี้
     * เช่น app.clock().setTo(1/10 09:59) → ทุกหน้าจอเห็นเวลา 09:59 พร้อมกัน
     *
     * @return AppClock ตัวที่ทุก service ใช้อยู่
     */
    public AppClock clock() {
        return clock;
    }

    /**
     * ผังโรง ใช้วาดปุ่มที่นั่ง
     * เช่น app.layout().rows() → [A, B, C, D, E, F]
     *
     * @return HallLayout ตัวที่ BookingService ใช้อยู่
     */
    public HallLayout layout() {
        return layout;
    }

    /**
     * บริการ login / สมัคร
     * เช่น app.auth().login("somchai", "abc123") → User somchai
     *
     * @return AuthService ที่อ่าน/เขียน users.csv
     */
    public AuthService auth() {
        return auth;
    }

    /**
     * บริการหนังและรอบฉาย
     * เช่น app.movies().bookableDates() → [วันนี้, พรุ่งนี้, มะรืน]
     *
     * @return MovieService ที่ใช้นาฬิกาตัวเดียวกับทั้งโปรแกรม
     */
    public MovieService movies() {
        return movies;
    }

    /**
     * บริการเงินและสมาชิก
     * เช่น app.accounts().topUp(user, 300) → เงินเพิ่ม 300 และบันทึก accounts.csv
     *
     * @return AccountService ที่ BookingService ใช้ตัดเงินด้วย (ตัวเดียวกัน)
     */
    public AccountService accounts() {
        return accounts;
    }

    /**
     * บริการจองตั๋ว
     * เช่น app.bookings().book(user, showtime, seats) → Booking B1 และตัดเงิน
     *
     * @return BookingService ที่ใช้นาฬิกา ผังโรง และบริการเงินตัวเดียวกับทั้งโปรแกรม
     */
    public BookingService bookings() {
        return bookings;
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     */
    private void checkRep() {
        assert clock != null && layout != null;
        assert auth != null && movies != null && accounts != null && bookings != null;
    }
}