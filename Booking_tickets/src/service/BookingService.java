//BookingService <- ตัวที่หน้าเลือกที่นั่งกับหน้าประวัติคุยด้วย: ดูที่นั่งที่ถูกจอง, คิดยอดเงิน, จอง (ตัดเงิน + บันทึก), ดูประวัติ
/*
    หน้าจอใช้ 5 เมธอดนี้:
    bookedSeats(รอบ)                   → รหัสที่นั่งที่ถูกจองแล้วของรอบนั้น เช่น {A1, F7} → ปุ่มเป็นสีเทา กดไม่ได้
    totalFor(user, ที่นั่งที่เลือก)        → ยอดที่ต้องจ่าย (สมาชิกลด 10%) → โชว์ในกล่องสรุปด้านขวา
    book(user, รอบ, ที่นั่งที่เลือก)        → กด "ยืนยันการจอง": ตรวจ → บันทึกการจอง → ตัดเงิน → คืน Booking
    historyOf(user)                    → การจองทั้งหมดของคนนี้ ใหม่สุดอยู่บน → หน้าประวัติ
    showtimeOf(booking)                → รอบจริงของการจองนั้น (ชื่อหนัง วัน เวลา โรง) → หน้าประวัติ

    กฎการจอง:
    - จองได้เฉพาะรอบที่ยังไม่เริ่ม
    - เลือกได้กี่ที่ก็ได้ (อย่างน้อย 1) ห้ามเลือกที่นั่งซ้ำ ห้ามเลือกที่นั่งที่ถูกจองแล้ว
    - จ่ายจากเงินในแอปทันที เงินไม่พอจองไม่ได้
    - จองแล้วยกเลิกไม่ได้

    ตัวอย่างการใช้ (ตอนนี้ 1/10/2026 09:15, somchai มีเงิน 500 เป็นสมาชิก):
        BookingService bookingService = new BookingService(bookingRepo, movieRepo, accountService,
                                                           new PriceCalculator(), new HallLayout(), clock);
        Showtime round = ... // รอบ Spider-Man 1/10 10:00 โรง 1 (ได้จาก MovieService.showtimesOf)
        List<Seat> chosen = List.of(layout.findSeat("A1"), layout.findSeat("F7"));

        bookingService.bookedSeats(round);              // {} ยังไม่มีใครจอง
        bookingService.totalFor(somchai, chosen);       // 324 (144 + 180)
        bookingService.book(somchai, round, chosen);    // Booking[B1, somchai, 2026-10-01_H1_1000, [A1, F7], 324, 2026-10-01T09:15]
                                                        // เงิน somchai เหลือ 176
        bookingService.bookedSeats(round);              // {A1, F7}
        bookingService.book(nok, round, List.of(F7));   // พัง IllegalArgumentException: "ที่นั่ง F7 ถูกจองไปแล้ว"
        bookingService.historyOf(somchai);              // [B1]
        bookingService.showtimeOf(B1);                  // รอบ Spider-Man 1/10 10:00 โรง 1

    ข้อความ error ที่ผู้ใช้ทำผิดได้ (ที่นั่งถูกจอง, เงินไม่พอ, รอบเริ่มแล้ว) เป็นภาษาไทย
    หน้าจอ catch IllegalArgumentException แล้วเอา getMessage() ไปโชว์ในกล่องแจ้งเตือนได้เลย
 */

package service;

import java.io.IOException;                      // error ตอนอ่าน/เขียนไฟล์ไม่ได้ (ส่งต่อจาก repository ให้หน้าจอ)
import java.time.LocalDate;                      // วันที่ของรอบ ใช้หารอบจริงใน showtimeOf
import java.time.format.DateTimeParseException;  // error ตอนรหัสรอบผิดรูปแบบ แปลงเป็นวันที่ไม่ได้
import java.time.temporal.ChronoUnit;            // ChronoUnit.MINUTES ตัดวินาทีของเวลาที่จองทิ้ง
import java.util.ArrayList;                      // สร้างรายการใหม่ไว้ใส่รหัสที่นั่ง / ประวัติ
import java.util.Collections;                    // Collections.reverse กลับลำดับประวัติให้ใหม่สุดอยู่บน
import java.util.HashSet;                        // เก็บรหัสที่นั่งแบบไม่ซ้ำ ใช้ตรวจเลือกซ้ำ / รวมที่นั่งที่ถูกจอง
import java.util.List;                           // ชนิดของรายการ
import java.util.Set;                            // ชนิดของกลุ่มที่ไม่มีตัวซ้ำ ที่ bookedSeats คืนออกไป
import model.Booking;                            // การจอง 1 ครั้ง
import model.HallLayout;                         // ผังโรง ใช้ตรวจว่าที่นั่งมีอยู่จริง
import model.Seat;                               // ที่นั่ง 1 ตัว
import model.Showtime;                           // รอบฉาย
import model.User;                               // ผู้ใช้ที่ login แล้ว
import repository.BookingRepository;             // ที่เก็บการจอง (interface)
import repository.MovieRepository;               // ที่เก็บหนัง/ตารางฉาย (interface) ใช้หารอบจริงใน showtimeOf

/**
 * บริการการจองตั๋ว สำหรับหน้าเลือกที่นั่งและหน้าประวัติ
 * ไม่อ่าน/เขียนไฟล์เอง ใช้ BookingRepository / MovieRepository / AccountService
 * ราคาถามจาก PriceCalculator ผังโรงถามจาก HallLayout เวลาถามจาก AppClock
 */
public class BookingService {

    // AF: บริการจองที่ใช้ bookingRepository เก็บการจอง, movieRepository หารอบ,
    //     accountService ตัดเงิน/เช็กสมาชิก, priceCalculator คิดราคา, hallLayout ตรวจที่นั่ง, clock บอกเวลา
    // RI: ทุก field ไม่เป็น null
    // Safety from rep exposure: field เป็น private final
    //     Set / List ที่คืนออกไปสร้างใหม่ทุกครั้ง แก้แล้วไม่กระทบข้อมูลข้างใน
    //     Booking / Showtime ที่คืนออกไปเป็น record แก้ไม่ได้
    // Thread safety: ไม่มี field ที่แก้ค่าได้ (final ทั้งหมด)
    //     เรียกจากหน้าจอ Swing ทีละคำสั่งบน thread เดียว (แบบเดียวกับ AccountService)

    /** ตัวอักษรนำหน้ารหัสการจอง เช่น B1, B2 */
    private static final String ID_PREFIX = "B";
    /** เลขของการจองครั้งแรก → B1 */
    private static final int FIRST_BOOKING_NUMBER = 1;
    /** ตัวคั่นในรหัสรอบ "2026-10-01_H1_1000" ส่วนแรกก่อน _ คือวันที่ */
    private static final String SHOWTIME_ID_SEPARATOR = "_";

    /** ที่เก็บการจอง เช่น CsvBookingRepository */
    private final BookingRepository bookingRepository;
    /** ที่เก็บหนัง/ตารางฉาย เช่น CsvMovieRepository */
    private final MovieRepository movieRepository;
    /** บริการเงินและสมาชิก */
    private final AccountService accountService;
    /** ตัวคิดราคา */
    private final PriceCalculator priceCalculator;
    /** ผังโรง */
    private final HallLayout hallLayout;
    /** นาฬิกาตัวเดียวของโปรแกรม */
    private final AppClock clock;

    /**
     * รับทุกอย่างที่ต้องใช้เข้ามาทาง constructor ไม่ new เอง (SC5 DIP)
     *
     * ตัวอย่าง:
     *   new BookingService(new CsvBookingRepository(Path.of("data", "bookings.csv")),
     *                      movieRepository, accountService,
     *                      new PriceCalculator(), new HallLayout(), clock)
     *   ส่ง null ตัวไหน → throw เช่น "bookingRepository must not be null"
     *
     * @param bookingRepository ที่เก็บการจอง ห้าม null
     * @param movieRepository   ที่เก็บหนัง/ตารางฉาย ห้าม null
     * @param accountService    บริการเงินและสมาชิก ห้าม null
     * @param priceCalculator   ตัวคิดราคา ห้าม null
     * @param hallLayout        ผังโรง ห้าม null
     * @param clock             นาฬิกาของโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้าตัวไหนเป็น null
     */
    public BookingService(BookingRepository bookingRepository, MovieRepository movieRepository,
                          AccountService accountService, PriceCalculator priceCalculator,
                          HallLayout hallLayout, AppClock clock) {
        requireNonNull(bookingRepository, "bookingRepository");
        requireNonNull(movieRepository, "movieRepository");
        requireNonNull(accountService, "accountService");
        requireNonNull(priceCalculator, "priceCalculator");
        requireNonNull(hallLayout, "hallLayout");
        requireNonNull(clock, "clock");
        this.bookingRepository = bookingRepository;
        this.movieRepository = movieRepository;
        this.accountService = accountService;
        this.priceCalculator = priceCalculator;
        this.hallLayout = hallLayout;
        this.clock = clock;
        checkRep();
    }

    /**
     * รหัสที่นั่งที่ถูกจองแล้วของรอบนี้ หน้าเลือกที่นั่งใช้ทำปุ่มเป็นสีเทา
     *
     * วิธีทำงาน:
     *   1. showtime เป็น null → throw
     *   2. ไล่การจองทุกรายการ อันไหนรหัสรอบตรงกับ showtime.id() → เอาที่นั่งทั้งหมดของมันใส่กลุ่ม
     *   3. คืนกลุ่มรหัสที่นั่ง (ไม่มีตัวซ้ำ)
     *
     * ตัวอย่าง (bookings.csv มี B1 รอบ 2026-10-01_H1_1000 ที่นั่ง A1;F7 และ B2 รอบอื่น ที่นั่ง C5):
     *   bookedSeats(รอบ 1/10 10:00 โรง 1)  → {A1, F7}
     *   bookedSeats(รอบที่ไม่มีใครจอง)       → {}
     *
     * @param showtime รอบที่อยากรู้ ห้าม null
     * @return รหัสที่นั่งที่ถูกจองแล้ว (แก้กลุ่มที่ได้ไปแล้วไม่กระทบข้อมูลจริง)
     * @throws IOException ถ้าอ่าน bookings.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด
     * @throws IllegalArgumentException ถ้า showtime เป็น null
     */
    public Set<String> bookedSeats(Showtime showtime) throws IOException {
        requireNonNull(showtime, "showtime");
        Set<String> taken = new HashSet<>();
        for (Booking booking : bookingRepository.findAll()) {
            // booking.showtimeId().equals(showtime.id())
            //   showtime.id() = รหัสรอบที่ถาม เช่น "2026-10-01_H1_1000"
            //   ตรงกัน → ที่นั่งของการจองนี้ไม่ว่าง
            if (booking.showtimeId().equals(showtime.id())) {
                // taken.addAll(booking.seatCodes())
                //   booking.seatCodes() = ที่นั่งของการจองนี้ เช่น [A1, F7] → ใส่ทั้งหมดลงกลุ่ม
                taken.addAll(booking.seatCodes());
            }
        }
        return taken;
    }

    /**
     * ยอดที่ต้องจ่ายของที่นั่งที่เลือก (สมาชิกได้ลด 10%) หน้าจอโชว์ในกล่องสรุป
     *
     * วิธีทำงาน: ถามว่าเป็นสมาชิกไหมจาก accountService → ให้ priceCalculator คิดยอดรวม
     *
     * ตัวอย่าง:
     *   somchai เป็นสมาชิก เลือก [A1, F7] → 144 + 180 = 324
     *   nok ไม่เป็นสมาชิก เลือก [A1, F7]   → 160 + 200 = 360
     *   ยังไม่ได้เลือก []                  → 0
     *
     * @param user  ผู้ใช้ที่ login แล้ว ห้าม null
     * @param seats ที่นั่งที่เลือก ห้าม null (ว่างได้)
     * @return ยอดที่ต้องจ่ายเป็นบาท
     * @throws IOException ถ้าอ่าน accounts.csv ไม่ได้
     * @throws IllegalArgumentException ถ้า user หรือ seats เป็น null หรือมีที่นั่งที่เป็น null
     */
    public int totalFor(User user, List<Seat> seats) throws IOException {
        // priceCalculator.totalOf(seats, accountService.isMember(user))
        //   seats                        = ที่นั่งที่เลือก เช่น [A1, F7]
        //   accountService.isMember(user) = เป็นสมาชิกตอนนี้ไหม เช่น true → ได้ส่วนลด
        return priceCalculator.totalOf(seats, accountService.isMember(user));
    }

    /**
     * จองที่นั่ง: ตรวจ → บันทึกการจอง → ตัดเงิน
     *
     * วิธีทำงาน:
     *   1. user / showtime / seats เป็น null → throw
     *   2. รอบเริ่มไปแล้ว → throw "รอบนี้เริ่มฉายไปแล้ว จองไม่ได้"
     *   2.1 รอบไม่ใช่ของวันนี้ (เช่น รอบพรุ่งนี้) → throw "จองได้แค่รอบของวันนี้" (อาจารย์ไม่ให้จองล่วงหน้า)
     *   3. ไม่ได้เลือกที่นั่ง → throw "กรุณาเลือกที่นั่งอย่างน้อย 1 ที่"
     *   4. ไล่ทีละที่นั่ง: ต้องมีอยู่จริงในผังโรง และไม่เลือกซ้ำ (ราคาคิดจากที่นั่งตัวจริงในผังโรง)
     *   5. เทียบกับ bookedSeats() ถ้ามีที่นั่งไหนถูกจองแล้ว → throw "ที่นั่ง F7 ถูกจองไปแล้ว"
     *   6. คิดยอดด้วย totalFor() ถ้าเงินในแอปไม่พอ → throw "เงินไม่พอ มี 100 บาท ต้องจ่าย 324 บาท"
     *   7. ออกรหัสการจองถัดไป (B1, B2, ...) สร้าง Booking แล้วบันทึก
     *   8. ตัดเงินด้วย accountService.pay() แล้วคืน Booking
     *
     * ทำไมบันทึกการจองก่อนตัดเงิน: ข้อ 6 เช็กแล้วว่าเงินพอ ข้อ 8 จึงไม่ติดเรื่องเงินไม่พอ
     * ถ้าตัดเงินก่อนแล้วบันทึกการจองพัง ลูกค้าจะเสียเงินแต่ไม่ได้ตั๋ว ซึ่งแย่กว่า
     *
     * ตัวอย่าง (ตอนนี้ 1/10 09:15, รอบ 1/10 10:00 โรง 1):
     *   somchai สมาชิก มี 500 เลือก [A1, F7]  → Booking[B1, somchai, 2026-10-01_H1_1000, [A1, F7], 324, 2026-10-01T09:15]
     *                                          เงินเหลือ 176, bookings.csv เพิ่มแถว B1
     *   nok เลือก [F7] รอบเดียวกัน             → throw "ที่นั่ง F7 ถูกจองไปแล้ว"
     *   nok มี 100 เลือก [C5] (160 บาท)        → throw "เงินไม่พอ มี 100 บาท ต้องจ่าย 160 บาท"
     *   ตอนนี้ 10:00 จองรอบ 10:00              → throw "รอบนี้เริ่มฉายไปแล้ว จองไม่ได้"
     *   ตอนนี้ 1/10 จองรอบ 2/10 10:00          → throw "จองได้แค่รอบของวันนี้"
     *   เลือก [A1, A1]                         → throw "เลือกที่นั่ง A1 ซ้ำ"
     *
     * @param user     ผู้ใช้ที่ login แล้ว ห้าม null
     * @param showtime รอบที่จะจอง ห้าม null
     * @param seats    ที่นั่งที่เลือก อย่างน้อย 1 ที่ ห้ามซ้ำ ห้ามถูกจองแล้ว
     * @return การจองที่บันทึกแล้ว
     * @throws IOException ถ้าอ่าน/เขียน bookings.csv หรือ accounts.csv ไม่ได้
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null หรือผิดกฎข้อ 2–6 (ข้อความภาษาไทย ให้หน้าจอโชว์ได้เลย)
     */
    public Booking book(User user, Showtime showtime, List<Seat> seats) throws IOException {
        requireNonNull(user, "user");
        requireNonNull(showtime, "showtime");
        requireNonNull(seats, "seats");

        // 2. showtime.hasStarted(clock.now())
        //   clock.now() = เวลาตอนนี้ของโปรแกรม เช่น 1/10 10:00 → รอบ 10:00 เริ่มแล้ว → จองไม่ได้
        if (showtime.hasStarted(clock.now())) {
            throw new IllegalArgumentException("This showtime has already started. Booking unavailable.");
        }
        // 2.1 จองได้แค่รอบของวันนี้ เพราะอาจารย์ไม่ให้จองล่วงหน้า
        //   ข้อ 2 กันได้แค่รอบที่เริ่มไปแล้ว ส่วนรอบของพรุ่งนี้ยังไม่เริ่ม เลยต้องเช็กวันแยกตรงนี้
        //   กฎอยู่ที่ book() เลย จะได้ไม่ต้องพึ่งหน้าจอว่ามีปุ่มแค่วันนี้
        //   showtime.date()           = วันฉายของรอบที่จะจอง เช่น 2026-10-09
        //   clock.now().toLocalDate() = วันนี้ของโปรแกรม เช่น 2026-10-08
        //   สองวันไม่ตรงกัน → รอบนี้ไม่ใช่ของวันนี้ → จองไม่ได้
        if (!showtime.date().equals(clock.now().toLocalDate())) {
            throw new IllegalArgumentException("You can only book showtimes for today.");
        }
        // 3.
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("Please select at least 1 seat.");
        }

        // 4. ตรวจทีละที่นั่ง แล้วเก็บรหัสไว้ใส่ Booking
        //    ใช้ที่นั่งตัวจริงจากผังโรงไปคิดราคา (realSeats) ไม่ใช้ตัวที่ส่งมาตรง ๆ
        //    กันกรณีมีคนสร้าง new Seat('F', 7, SeatType.STANDARD) ส่งมา แล้วได้ราคาธรรมดาแทน VIP
        List<String> codes = new ArrayList<>();
        List<Seat> realSeats = new ArrayList<>();
        Set<String> chosen = new HashSet<>();
        for (Seat seat : seats) {
            requireNonNull(seat, "seat");
            // hallLayout.findSeat(seat.code())
            //   seat.code() = รหัสที่นั่ง เช่น "F7" → ได้ที่นั่ง F7 VIP ตัวจริงจากผังโรง
            //                 "G3" → ไม่มีในผังโรง HallLayout throw "no such seat: G3"
            Seat real = hallLayout.findSeat(seat.code());
            // chosen.add(...) คืน false ถ้ามีรหัสนี้อยู่แล้ว = เลือกซ้ำ
            // กันการจองที่นั่งเดิม 2 ครั้ง F7 F7 -> throw
            if (!chosen.add(real.code())) {
                throw new IllegalArgumentException("Duplicate seat selected: " + real.code());
            }
            codes.add(real.code());
            realSeats.add(real);
        }

        // 5. ที่นั่งที่เลือกไปชนกับที่ถูกจองแล้วไหม
        //   taken = {A1, F7}, codes = [C5, F7] → F7 ชน → "ที่นั่ง F7 ถูกจองไปแล้ว"
        Set<String> taken = bookedSeats(showtime);
        List<String> clashes = new ArrayList<>();
        for (String code : codes) {
            if (taken.contains(code)) {
                clashes.add(code);
            }
        }
        if (!clashes.isEmpty()) {
            // String.join(", ", clashes) : ต่อรหัสที่ชนด้วย ", " เช่น [A1, F7] → "A1, F7"
            throw new IllegalArgumentException("Seat(s) already booked: " + String.join(", ", clashes));
        }

        // 6. เงินพอไหม
        // totalFor(user, realSeats)
        //   realSeats = ที่นั่งตัวจริงจากผังโรง เช่น [A1 ธรรมดา, F7 VIP] → สมาชิกได้ 324
        int total = totalFor(user, realSeats);
        int balance = accountService.accountOf(user).balance();
        if (balance < total) {
            throw new IllegalArgumentException("Insufficient balance: Current " + balance + " THB, Required " + total + " THB");
        }

        // 7. สร้างการจองแล้วบันทึก
        // clock.now().truncatedTo(ChronoUnit.MINUTES)
        //   ตัดวินาทีทิ้ง 09:15:42 → 09:15 ให้เวลาที่จองในไฟล์และหน้าประวัติอ่านง่าย (แบบเดียวกับ Account)
        Booking booking = new Booking(nextId(), user.username(), showtime.id(), codes, total,
                clock.now().truncatedTo(ChronoUnit.MINUTES));
        bookingRepository.save(booking);

        // 8. ตัดเงิน
        // accountService.pay(user, total)
        //   user  = คนที่จอง
        //   total = ยอดที่คิดได้ในข้อ 6 เช่น 324
        accountService.pay(user, total);
        return booking;
    }

    /**
     * การจองทั้งหมดของผู้ใช้คนนี้ ใหม่สุดอยู่บน หน้าประวัติใช้
     *
     * วิธีทำงาน:
     *   1. user เป็น null → throw
     *   2. ไล่การจองทุกรายการ เก็บเฉพาะที่ชื่อผู้จองตรงกัน (ในไฟล์เรียงเก่า → ใหม่)
     *   3. กลับลำดับให้ใหม่สุดอยู่บน
     *
     * ตัวอย่าง (bookings.csv มี B1 somchai, B2 nok, B3 somchai):
     *   historyOf(somchai) → [B3, B1]
     *   historyOf(nok)     → [B2]
     *   คนที่ยังไม่เคยจอง   → []
     *
     * @param user ผู้ใช้ที่ login แล้ว ห้าม null
     * @return การจองของคนนี้ ใหม่สุดอยู่บน
     * @throws IOException ถ้าอ่าน bookings.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด
     * @throws IllegalArgumentException ถ้า user เป็น null
     */
    public List<Booking> historyOf(User user) throws IOException {
        requireNonNull(user, "user");
        List<Booking> mine = new ArrayList<>();
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.username().equals(user.username())) {
                mine.add(booking);
            }
        }
        // Collections.reverse(mine) : กลับลำดับในรายการเดิม [B1, B3] → [B3, B1]
        Collections.reverse(mine);
        return mine;
    }

    /**
     * 
     * ลูปเทียบรหัสรอบในตารางฉายกับรหัสรอบในตั๋ว เพื่อหาหนัง/วัน/เวลา/โรงของการจองใบนั้น และ return null เมื่อไม่เจอ
     * รอบจริงของการจอง ใช้โชว์ชื่อหนัง วัน เวลา โรง ในหน้าประวัติ
     *
     * วิธีทำงาน:
     *   1. booking เป็น null → throw
     *   2. เอาวันที่จากส่วนแรกของรหัสรอบ "2026-10-01_H1_1000" → "2026-10-01" → 1/10/2026
     *   3. ถามรอบทั้งหมดของวันนั้นจาก movieRepository แล้วหารอบที่ id ตรงกัน
     *   4. ไม่เจอ (ตารางฉายเปลี่ยนไปแล้ว หรือรหัสรอบผิดรูปแบบ) → คืน null ให้หน้าจอโชว์ "ไม่พบข้อมูลรอบ"
     *
     * ตัวอย่าง:
     *   showtimeOf(B1 รอบ "2026-10-01_H1_1000") → รอบ Spider-Man: Brand New Day 1/10/2026 10:00 โรง 1
     *   showtimeOf(การจองที่รอบ "2026-10-01_H9_2300") → null (ไม่มีรอบนี้ในตารางฉาย)
     *
     * @param booking การจองที่อยากรู้รอบ ห้าม null
     * @return รอบฉายของการจองนี้ หรือ null ถ้าหาไม่เจอ
     * @throws IOException ถ้าอ่าน movies.csv / schedule.csv ไม่ได้
     * @throws IllegalArgumentException ถ้า booking เป็น null
     */
    public Showtime showtimeOf(Booking booking) throws IOException {
        requireNonNull(booking, "booking");
        LocalDate date;
        try {
            // booking.showtimeId().split(SHOWTIME_ID_SEPARATOR)[0]
            //   "2026-10-01_H1_1000".split("_") → ["2026-10-01", "H1", "1000"] → เอาช่องแรก "2026-10-01"
            // LocalDate.parse("2026-10-01") → วันที่ 1/10/2026
            date = LocalDate.parse(booking.showtimeId().split(SHOWTIME_ID_SEPARATOR)[0]);
        } catch (DateTimeParseException e) {
            return null; // รหัสรอบผิดรูปแบบ หาไม่ได้
        }
        // movieRepository.findShowtimesOn(date) : รอบทั้งหมดของวันนั้น (15 รอบ)
        //   date = วันที่ที่ได้ข้างบน เช่น 2026-10-01
        for (Showtime showtime : movieRepository.findShowtimesOn(date)) {
            if (showtime.id().equals(booking.showtimeId())) {
                return showtime;
            }
        }
        return null;
    }

    /**
     * รหัสการจองถัดไป = เลขมากที่สุดที่มีอยู่ + 1
     *
     * วิธีทำงาน: ไล่การจองทุกรายการ ตัด "B" ข้างหน้าออกแล้วแปลงเป็นเลข จำเลขที่มากที่สุดไว้ แล้ว +1
     *
     * ตัวอย่าง:
     *   ยังไม่มีการจอง       → "B1"
     *   มี B1, B2           → "B3"
     *   มี B1, B5 (ถูกแก้มือ) → "B6"  (ใช้เลขมากสุด ไม่ใช่นับจำนวน จึงไม่ชนรหัสเดิม)
     *
     * @return รหัสใหม่ที่ไม่ซ้ำกับที่มีอยู่
     * @throws IOException ถ้าอ่าน bookings.csv ไม่ได้
     */
    private String nextId() throws IOException {
        int max = FIRST_BOOKING_NUMBER - 1; // ยังไม่มีการจอง → ถัดไปคือ FIRST_BOOKING_NUMBER
        for (Booking booking : bookingRepository.findAll()) {
            String id = booking.id();
            if (id.startsWith(ID_PREFIX)) {
                try {
                    // id.substring(ID_PREFIX.length()) : ตัด "B" ข้างหน้าออก "B12" → "12"
                    max = Math.max(max, Integer.parseInt(id.substring(ID_PREFIX.length())));
                } catch (NumberFormatException e) {
                    // รหัสที่ไม่ใช่ B+เลข (เช่นมีคนแก้ไฟล์มือ) ไม่นับ
                }
            }
        }
        return ID_PREFIX + (max + 1);
    }

    /**
     * ตรวจว่าไม่เป็น null ใช้ซ้ำหลายที่
     * เช่น requireNonNull(null, "user") → throw "user must not be null"
     *
     * @param value ค่าที่จะตรวจ
     * @param name  ชื่อ ใช้ในข้อความ error
     * @throws IllegalArgumentException ถ้า value เป็น null
     */
    private static void requireNonNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    /**
     * ตรวจ RI (SC4 หน้า 12) ทำงานเมื่อรันด้วย -ea
     */
    private void checkRep() {
        assert bookingRepository != null && movieRepository != null && accountService != null;
        assert priceCalculator != null && hallLayout != null && clock != null;
    }
}