//MovieService <- ตัวที่หน้าจอเลือกหนังคุยด้วย เอาข้อมูลจาก MovieRepository กับเวลาจาก AppClock มาจัดให้พร้อมแสดง
/*
    มี 5 เมธอด:
    moviesToday()            → หนัง 5 เรื่องที่ฉายวันนี้ ใส่กล่องโปสเตอร์ ◀ ▶ ให้ลูกค้าเลือก
    movies()                 → หนังทั้งหมด 15 เรื่อง ให้หน้า admin เลือกชุดหนังของพรุ่งนี้
    bookableDates()          → วันที่จองได้ ตอนนี้มีแค่วันนี้ (อาจารย์ให้จองได้แค่ในวัน)
    showtimesOf(หนัง, วันที่)  → รอบของหนังเรื่องนั้นวันนั้น เรียงเช้า → ค่ำ ใส่ปุ่มรอบ
    canBook(รอบ)             → true = กดจองได้ / false = เลยเวลาแล้ว ปุ่มเป็นสีเทา

    ตัวอย่างการใช้ (ในโปรแกรมจริง AppServices เป็นคนสร้างให้):
        AppClock clock = new AppClock();
        MovieRepository repo = new CsvMovieRepository(Path.of("data", "movies.csv"),
                Path.of("data", "schedule.csv"), Path.of("data", "lineup.csv"));
        MovieService movieService = new MovieService(repo, clock);

        // สมมุติวันนี้ 8/10 เวลา 13:00 และ lineup.csv มี 2026-10-01,M1;M2;M3;M4;M5
        List<Movie> today = movieService.moviesToday();                   // [Spider-Man, Pacific Rim, Ghost Rider, Jurassic Park III, Blade Runner 2049]
        List<LocalDate> dates = movieService.bookableDates();             // [8/10]
        List<Showtime> rounds = movieService.showtimesOf(today.get(0), dates.get(0));
                                                                          // Spider-Man วันที่ 8/10: [08:45 โรง 1, 11:45 โรง 3, 17:45 โรง 2]
        boolean ok = movieService.canBook(rounds.get(0));                 // ตอนนี้ 13:00 → รอบ 08:45 ได้ false (ปุ่มสีเทา)
 */

package service;

import java.io.IOException;              // error ตอนอ่านไฟล์หนัง / ตารางฉายไม่ได้ (ส่งต่อจาก MovieRepository ให้หน้าจอ)
import java.time.LocalDate;              // วันที่อย่างเดียว เช่น 2026-10-08 ใช้กับปุ่มวันที่ และถามหนังที่ฉายวันนี้
import java.util.ArrayList;              // สร้างรายการใหม่ไว้ใส่วันที่ / รอบฉาย
import java.util.Comparator;             // ตัวบอกวิธีเรียง ใช้เรียงรอบตามเวลาเริ่ม
import java.util.List;                   // ชนิดของรายการที่คืนออกไป
import model.Movie;                      // class หนัง อยู่คนละ package เลยต้อง import
import model.Showtime;                   // class รอบฉาย อยู่คนละ package เลยต้อง import
import repository.MovieRepository;       // ที่เก็บหนัง/ตารางฉาย (interface) รับเข้ามาทาง constructor

/**
 * บริการเกี่ยวกับหนังและรอบฉาย สำหรับหน้าจอเลือกหนัง
 * ไม่อ่านไฟล์เอง ถามจาก MovieRepository และถามเวลาจาก AppClock
 */
public class MovieService {

    // AF: บริการหนังที่ดึงข้อมูลจาก movieRepository และใช้เวลาปัจจุบันจาก clock
    // RI: movieRepository และ clock ไม่เป็น null
    // Safety from rep exposure: field เป็น private final, List ที่คืนออกไปสร้างใหม่ทุกครั้ง
    //                           คนเรียกแก้ List แล้วไม่กระทบข้อมูลข้างใน
    // Thread safety: ไม่มี field ที่แก้ค่าได้ (final ทั้งหมด), AppClock ล็อกของตัวเองอยู่แล้ว (SC7)

    // จองได้กี่วัน นับวันนี้ด้วย
    // ตั้งเป็น 1 เพราะอาจารย์ให้จองได้แค่ในวัน (ไม่ให้จองล่วงหน้า) → ได้แค่วันนี้วันเดียว
    // เดิมเป็น 3 (วันนี้ + พรุ่งนี้ + มะรืน)
    private static final int BOOKABLE_DAYS = 1;

    private final MovieRepository movieRepository; // ที่เก็บหนัง/ตารางฉาย เช่น CsvMovieRepository
    private final AppClock clock;                  // นาฬิกาตัวเดียวของโปรแกรม

    /**
     * รับที่เก็บข้อมูลกับนาฬิกาเข้ามาทาง constructor ไม่ new เอง (SC5 DIP)
     * โปรแกรมจริงส่ง CsvMovieRepository ตอนทดสอบส่งตัวปลอมแทนได้
     * เช่น new MovieService(new CsvMovieRepository(moviesPath, schedulePath, lineupPath), new AppClock())
     *
     * @param movieRepository ที่เก็บหนังและตารางฉาย ห้าม null
     * @param clock           นาฬิกาของโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า movieRepository หรือ clock เป็น null
     */
    public MovieService(MovieRepository movieRepository, AppClock clock) {
        if (movieRepository == null) {
            throw new IllegalArgumentException("movieRepository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.movieRepository = movieRepository;
        this.clock = clock;
        checkRep();
    }

    /**
     * หนังทั้งหมดที่โรงมี (15 เรื่อง) ใช้ในหน้า admin ให้เลือก 5 เรื่องเป็นชุดหนังของพรุ่งนี้
     * (ตอนนี้ MainFrame ยังเรียกเมธอดนี้อยู่ จะเปลี่ยนไปใช้ moviesToday() ในขั้นแก้ MainFrame)
     * ทำงาน: ถาม movieRepository.findAll() แล้วส่งต่อ
     * เช่น ได้ [Spider-Man: Brand New Day, Pacific Rim, ..., The Dark Knight, คู่แรด] รวม 15 เรื่อง
     *
     * @return หนังทุกเรื่อง เรียงตามลำดับในไฟล์ movies.csv
     * @throws IOException ถ้าอ่านไฟล์หนังไม่ได้ หรือข้อมูลในไฟล์ผิด (หน้าจอเอา getMessage() ไปโชว์)
     */
    public List<Movie> movies() throws IOException {
        return movieRepository.findAll();
    }

    /**
     * หนังที่ฉายวันนี้ ใช้ใส่กล่องโปสเตอร์ในหน้าเลือกหนัง
     * เพื่อให้ลูกค้าเห็นแค่เรื่องที่มีรอบวันนี้ ไม่ต้องเห็นทั้ง 15 เรื่องแล้วกดเจอเรื่องที่ไม่มีรอบ
     * ทำงาน: เอาวันนี้จาก clock → ถาม movieRepository.findLineupOn(วันนี้)
     *        findLineupOn คืนชุดหนังที่ admin ตั้งไว้ล่าสุด โดยวันเริ่มฉายของชุดนั้นต้องไม่เลยวันนี้
     *        (ชุดที่ admin ตั้งไว้ให้พรุ่งนี้ จะยังไม่โผล่วันนี้)
     * เช่น lineup.csv มี 2026-10-01,M1;M2;M3;M4;M5 และวันนี้ 8/10
     *      → [Spider-Man, Pacific Rim, Ghost Rider, Jurassic Park III, Blade Runner 2049]
     *      ถ้า admin ตั้ง 2026-10-09,M6;M7;M8;M9;M10 ไว้ วันนี้ (8/10) ยังได้ชุดเดิม พรุ่งนี้ถึงได้ชุดใหม่
     *
     * @return หนังที่ฉายวันนี้ เรียงตามเรื่องที่ 1–5 หรือรายการว่างถ้ายังไม่เคยตั้งชุดหนังไว้ก่อนวันนี้
     * @throws IOException ถ้าอ่านไฟล์หนัง / lineup.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด
     */
    public List<Movie> moviesToday() throws IOException {
        // clock.now()        : เวลาตอนนี้ของโปรแกรม เช่น 2026-10-08 13:00
        // .toLocalDate()     : ตัดเวลาทิ้ง เหลือแค่วันที่ 2026-10-08
        LocalDate today = clock.now().toLocalDate();
        return movieRepository.findLineupOn(today);
    }

    /**
     * วันที่ที่จองได้ ใช้ทำปุ่มวันที่
     * ทำงาน: เอาวันนี้จาก clock → ใส่วันที่ลงรายการ BOOKABLE_DAYS วัน เริ่มจากวันนี้
     *        ตอนนี้ BOOKABLE_DAYS = 1 เลยได้แค่วันนี้วันเดียว
     * เช่น ตอนนี้ 8/10 13:00 → [8/10]
     *      ตั้งเวลาเป็น 9/10 00:05 → [9/10] (เปลี่ยนตามนาฬิกาของโปรแกรม)
     *
     * @return วันที่ BOOKABLE_DAYS วัน เริ่มจากวันนี้
     */
    public List<LocalDate> bookableDates() {
        // clock.now()        : เวลาตอนนี้ของโปรแกรม เช่น 2026-10-08 13:00
        // .toLocalDate()     : ตัดเวลาทิ้ง เหลือแค่วันที่ 2026-10-08
        LocalDate today = clock.now().toLocalDate();
        List<LocalDate> dates = new ArrayList<>();
        // วน BOOKABLE_DAYS รอบ ตอนนี้ = 1 รอบ (daysFromToday = 0 → วันนี้)
        for (int daysFromToday = 0; daysFromToday < BOOKABLE_DAYS; daysFromToday++) {
            // today.plusDays(n) : วันนี้บวกไป n วัน
            //     พารามิเตอร์ = จำนวนวันที่บวก เช่น 0 → 8/10
            dates.add(today.plusDays(daysFromToday));
        }
        return dates;
    }

    /**
     * รอบฉายของหนังเรื่องเดียวในวันที่เลือก เรียงจากเช้าไปค่ำ ใช้ทำปุ่มรอบ
     * ทำงาน: ถามรอบทั้งวัน (15 รอบ) → เก็บเฉพาะรอบที่หนังรหัสตรงกัน → เรียงตามเวลาเริ่ม
     * เช่น showtimesOf(Spider-Man, 8/10) โดยวันนั้น Spider-Man เป็นเรื่องที่ 1
     *      รอบทั้งวันเรียงตามโรงในไฟล์: ... โรง 1 08:45 M1 ... โรง 2 17:45 M1 ... โรง 3 11:45 M1 ...
     *      เก็บเฉพาะ M1 ได้ [08:45 โรง 1, 17:45 โรง 2, 11:45 โรง 3]
     *      เรียงเวลาแล้วได้ [08:45 โรง 1, 11:45 โรง 3, 17:45 โรง 2]
     *
     * @param movie หนังที่เลือก ห้าม null
     * @param date  วันที่ที่เลือก ห้าม null
     * @return รอบของหนังเรื่องนั้นในวันนั้น เรียงตามเวลาเริ่ม หรือ List ว่างถ้าไม่มีรอบ
     * @throws IOException ถ้าอ่านไฟล์หนัง / ตารางฉายไม่ได้ หรือข้อมูลในไฟล์ผิด
     * @throws IllegalArgumentException ถ้า movie หรือ date เป็น null
     */
    public List<Showtime> showtimesOf(Movie movie, LocalDate date) throws IOException {
        if (movie == null) {
            throw new IllegalArgumentException("movie must not be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        List<Showtime> result = new ArrayList<>();
        // movieRepository.findShowtimesOn(date) : รอบฉายทั้งหมดของวันนั้น (15 รอบ)
        //     พารามิเตอร์ = วันที่ เช่น 2026-10-08
        for (Showtime showtime : movieRepository.findShowtimesOn(date)) {
            // เทียบด้วยรหัสหนัง เช่น "M1".equals("M1") → true เก็บไว้ / "M2".equals("M1") → false ข้าม
            if (showtime.movie().id().equals(movie.id())) {
                result.add(showtime);
            }
        }
        // result.sort(วิธีเรียง) : เรียงรายการตามวิธีที่ส่งเข้าไป
        //     พารามิเตอร์ = Comparator.comparing(showtime -> showtime.start())
        //         แปลว่า "เอา start() ของแต่ละรอบมาเทียบกัน เวลาน้อยอยู่ก่อน"
        //         showtime -> showtime.start() คือ lambda (ฟังก์ชันไม่มีชื่อ SC8 หน้า 12)
        //         รับรอบ 1 รอบ คืนเวลาเริ่มของรอบนั้น เช่น รอบโรง 3 → 11:45
        //     ก่อนเรียง [08:45, 17:45, 11:45] → หลังเรียง [08:45, 11:45, 17:45]
        
        /*
                result.sort(...) สั่งให้ list เรียงตัวเอง แก้ list เดิมเลย ไม่ได้สร้างใหม่
                showtime -> showtime.start() บอกว่าแต่ละรอบ ให้หยิบเวลาเริ่มออกมาใช้เทียบ
                Comparator.comparing(...) สร้างตัวเปรียบเทียบจากค่าที่หยิบมา คือเอา start() ของสองรอบมาเทียบกันว่าอันไหนมาก่อน
         */
        result.sort(Comparator.comparing(showtime -> showtime.start()));
        return result;
    }

    /**
     * รอบนี้ยังจองได้ไหม ใช้ตัดสินว่าปุ่มรอบเป็นสีเทาหรือไม่
     * ทำงาน: ถามเวลาตอนนี้จาก clock → ถ้ารอบเริ่มไปแล้ว (ถึงหรือเลยเวลาเริ่ม) = จองไม่ได้
     * เช่น ตอนนี้ 27/9 13:00
     *      รอบ 27/9 10:00 → เริ่มไปแล้ว → false (ปุ่มสีเทา)
     *      รอบ 27/9 13:45 → ยังไม่เริ่ม → true
     *      รอบ 28/9 10:00 → ยังไม่เริ่ม (พรุ่งนี้) → true
     *      ตอนนี้ 13:45 พอดี รอบ 13:45 → เริ่มแล้ว → false
     *
     * @param showtime รอบที่จะเช็ก ห้าม null
     * @return true ถ้ายังไม่ถึงเวลาเริ่ม, false ถ้าถึงหรือเลยเวลาเริ่มแล้ว
     * @throws IllegalArgumentException ถ้า showtime เป็น null
     */
    public boolean canBook(Showtime showtime) {
        if (showtime == null) {
            throw new IllegalArgumentException("showtime must not be null");
        }
        // showtime.hasStarted(เวลาตอนนี้) : รอบนี้เริ่มไปแล้วหรือยัง (อยู่ใน Showtime)
        //     พารามิเตอร์ = clock.now() เวลาตอนนี้ของโปรแกรม เช่น 2026-09-27 13:00
        // ! กลับค่า: เริ่มแล้ว (true) → จองไม่ได้ (false) / ยังไม่เริ่ม (false) → จองได้ (true)
        return !showtime.hasStarted(clock.now());
    }

    // ตรวจ RI
    private void checkRep() {
        assert movieRepository != null;
        assert clock != null;
    }
}