//AdminService <- ตัวที่หน้า admin คุยด้วย ให้ admin เลือกหนัง 5 เรื่องที่จะฉายพรุ่งนี้ แล้วบันทึกลง lineup.csv
/*
    หน้า admin ใช้ 2 เมธอดนี้:
    tomorrowLineup()                   → หนัง 5 เรื่องที่จะฉายพรุ่งนี้ ใช้โชว์ว่าตอนนี้ตั้งไว้เป็นเรื่องอะไร
    setTomorrowLineup(admin, หนัง 5 เรื่อง) → เช็กกฎ แล้วบันทึกเป็นชุดหนังของพรุ่งนี้

    ทำไมแยกเป็น class ใหม่ ไม่ใส่ใน MovieService:
        MovieService เป็นของหน้าลูกค้า (ดูหนัง ดูรอบ) ส่วนนี้เป็นงานของ admin (ตั้งหนังที่จะฉาย)
        แยกกันจะได้แก้กฎของ admin โดยไม่กระทบหน้าลูกค้า (SC5 SRP: 1 class มีเหตุผลให้แก้เรื่องเดียว)

    ตัวอย่างการใช้ (ในโปรแกรมจริง AppServices เป็นคนสร้างให้, สมมุติวันนี้ 8/10):
        AdminService adminService = new AdminService(movieRepository, clock);

        User admin = authService.login("admin", "admin1");          // role ADMIN
        List<Movie> all = movieService.movies();                    // หนังทั้งหมด 15 เรื่อง ให้ admin เลือก
        adminService.setTomorrowLineup(admin, List.of(all.get(5), all.get(6), all.get(7), all.get(8), all.get(9)));
                                                                    // lineup.csv ได้บรรทัด 2026-10-09,M6;M7;M8;M9;M10
        adminService.tomorrowLineup();                              // [Hello World, Your Name, Weathering with You, Inception, Interstellar]

        adminService.setTomorrowLineup(customer, ...)               // ลูกค้าธรรมดา → throw "Only admin can set the lineup."
 */

package service;

import java.io.IOException;              // error ตอนอ่าน/เขียน movies.csv / lineup.csv ไม่ได้ (ส่งต่อจาก MovieRepository ให้หน้าจอ)
import java.time.LocalDate;              // วันที่อย่างเดียว ใช้หาวันพรุ่งนี้ เช่น 2026-10-09
import java.util.HashSet;                // เก็บรหัสหนังแบบไม่ซ้ำ ใช้เช็กเลือกเรื่องซ้ำ และเช็กว่าหนังมีจริง
import java.util.List;                   // ชนิดของรายการหนังที่รับเข้ามา / คืนออกไป
import java.util.Set;                    // ชนิดของกลุ่มที่ไม่มีตัวซ้ำ
import model.Movie;                      // class หนัง อยู่คนละ package เลยต้อง import
import model.User;                       // ผู้ใช้ที่ login แล้ว ใช้เช็กว่าเป็น admin ไหม
import repository.MovieRepository;       // ที่เก็บหนัง/ชุดหนังที่ฉาย (interface) รับเข้ามาทาง constructor

/**
 * บริการของ admin: ตั้งหนังที่จะฉายพรุ่งนี้
 * ไม่อ่าน/เขียนไฟล์เอง ใช้ MovieRepository และถามวันนี้จาก AppClock
 * กฎการตั้งหนัง (ต้องเป็น admin, ครบ 5 เรื่อง, ไม่ซ้ำ, มีอยู่จริง) อยู่ที่ class นี้ที่เดียว
 */
public class AdminService {

    // AF: บริการของ admin ที่อ่าน/บันทึกชุดหนังผ่าน movieRepository และรู้วันนี้จาก clock
    // RI: movieRepository และ clock ไม่เป็น null
    // Safety from rep exposure: field เป็น private final, List ที่คืนออกไปสร้างใหม่ทุกครั้ง (มาจาก repository)
    // Thread safety: ไม่มี field ที่แก้ค่าได้ (final ทั้งหมด), AppClock ล็อกของตัวเองอยู่แล้ว (SC7)

    /**
     * จำนวนหนังที่ต้องเลือกต่อวัน
     * ต้องเท่ากับจำนวนเรื่องใน schedule.csv (ช่อง slot มี 1–5) ถ้าเลือกไม่ครบ รอบของเรื่องที่ขาดจะหาหนังไม่เจอ
     */
    public static final int LINEUP_SIZE = 5;

    private final MovieRepository movieRepository; // ที่เก็บหนังและชุดหนังที่ฉาย เช่น CsvMovieRepository
    private final AppClock clock;                  // นาฬิกาตัวเดียวของโปรแกรม ใช้รู้ว่าพรุ่งนี้คือวันไหน

    /**
     * รับที่เก็บข้อมูลกับนาฬิกาเข้ามาทาง constructor ไม่ new เอง (SC5 DIP)
     * ใช้ MovieRepository ตัวเดียวกับ MovieService (AppServices ส่งตัวเดียวกันมาให้)
     * admin ตั้งหนังแล้ว หน้าลูกค้าจะเห็นชุดใหม่ทันทีที่ถึงวันนั้น เพราะอ่านจากไฟล์เดียวกัน
     *
     * ตัวอย่าง:
     *   new AdminService(new CsvMovieRepository(moviesPath, schedulePath, lineupPath), new AppClock())
     *   new AdminService(null, clock) → throw "movieRepository must not be null"
     *
     * @param movieRepository ที่เก็บหนังและชุดหนังที่ฉาย ห้าม null
     * @param clock           นาฬิกาของโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า movieRepository หรือ clock เป็น null
     */
    public AdminService(MovieRepository movieRepository, AppClock clock) {
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
     * หนังที่จะฉายพรุ่งนี้ ใช้โชว์ในหน้า admin ว่าตอนนี้ตั้งไว้เป็นเรื่องอะไร
     * ทำงาน: หาวันพรุ่งนี้จาก clock → ถาม movieRepository.findLineupOn(พรุ่งนี้)
     *        ถ้า admin ยังไม่ได้ตั้งของพรุ่งนี้ จะได้ชุดล่าสุดที่ตั้งไว้ก่อนหน้า
     *        เพราะพรุ่งนี้จะฉายชุดนั้นต่อจริง ๆ (กฎเดียวกับหน้าลูกค้า)
     * เช่น วันนี้ 8/10 และ lineup.csv มีแค่ 2026-10-01,M1;M2;M3;M4;M5
     *      → [Spider-Man, Pacific Rim, Ghost Rider, Jurassic Park III, Blade Runner 2049] (ยังไม่ได้ตั้งของ 9/10)
     *      admin ตั้ง 9/10 เป็น M6–M10 แล้ว → [Hello World, Your Name, Weathering with You, Inception, Interstellar]
     *
     * @return หนังที่จะฉายพรุ่งนี้ เรียงตามเรื่องที่ 1–5 หรือรายการว่างถ้ายังไม่เคยตั้งชุดหนังเลย
     * @throws IOException ถ้าอ่านไฟล์หนัง / lineup.csv ไม่ได้ หรือข้อมูลในไฟล์ผิด
     */
    public List<Movie> tomorrowLineup() throws IOException {
        return movieRepository.findLineupOn(tomorrow());
    }

    /**
     * ตั้งหนัง 5 เรื่องที่จะฉายพรุ่งนี้ แล้วบันทึกลง lineup.csv
     * ตั้งได้แค่พรุ่งนี้ เพราะวันนี้มีคนจองรอบของวันนี้ไปแล้ว ถ้าเปลี่ยนหนังของวันนี้ ตั๋วที่จองไว้จะกลายเป็นหนังอีกเรื่อง
     *
     * วิธีทำงาน:
     *   1. admin เป็น null → throw
     *   2. ไม่ใช่ admin (ลูกค้าธรรมดา) → throw "Only admin can set the lineup."
     *   3. movies เป็น null หรือไม่ใช่ 5 เรื่องพอดี → throw "Please select exactly 5 movies (selected 3)."
     *   4. ไล่ทีละเรื่อง: เรื่องเดียวกันเลือกซ้ำ → throw (เรื่องนั้นจะได้ 6 รอบ อีกเรื่องหายไป)
     *                    รหัสหนังไม่มีใน movies.csv → throw (บันทึกไปแล้วหน้าลูกค้าจะอ่าน lineup.csv ไม่ได้)
     *   5. ผ่านหมด → movieRepository.saveLineup(พรุ่งนี้, movies)
     *      ถ้าพรุ่งนี้เคยตั้งไว้แล้ว saveLineup เขียนทับของเดิม (admin เปลี่ยนใจได้จนกว่าจะถึงวันนั้น)
     *
     * ตัวอย่าง (วันนี้ 8/10):
     *   admin เลือก [M6, M7, M8, M9, M10]   → lineup.csv ได้บรรทัด 2026-10-09,M6;M7;M8;M9;M10
     *   admin เลือกใหม่ [M11, M12, M13, M14, M15] → บรรทัด 9/10 ถูกเขียนทับเป็น M11;M12;M13;M14;M15
     *   ลูกค้าธรรมดาเรียก                    → throw "Only admin can set the lineup."
     *   admin เลือก [M6, M7, M8]            → throw "Please select exactly 5 movies (selected 3)."
     *   admin เลือก [M6, M6, M8, M9, M10]   → throw "Movie M6 is selected more than once."
     *   admin เลือกหนังรหัส M99              → throw "Movie M99 not found."
     *
     * @param admin  ผู้ใช้ที่ login แล้ว ต้องเป็น admin ห้าม null
     * @param movies หนัง 5 เรื่องเรียงตามลำดับ (ตัวแรก = เรื่องที่ 1) ห้ามซ้ำ ต้องมีอยู่ใน movies.csv
     * @throws IOException ถ้าอ่าน movies.csv หรือเขียน lineup.csv ไม่ได้
     * @throws IllegalArgumentException ถ้าผิดกฎข้อ 1–4 (ข้อความภาษาอังกฤษ ให้หน้าจอโชว์ได้เลย)
     */
    public void setTomorrowLineup(User admin, List<Movie> movies) throws IOException {
        // 1.
        if (admin == null) {
            throw new IllegalArgumentException("admin must not be null");
        }
        // 2. admin.isAdmin() : role ของคนนี้เป็น ADMIN ไหม (อยู่ใน User)
        //    กันที่นี่ด้วย ไม่พึ่งแค่หน้าจอ ถ้ามีหน้าไหนเผลอเรียกด้วยลูกค้าธรรมดา จะได้ไม่บันทึก
        if (!admin.isAdmin()) {
            throw new IllegalArgumentException("Only admin can set the lineup.");
        }
        // 3. ต้องครบ 5 เรื่องพอดี เพราะ schedule.csv มีเรื่องที่ 1–5
        //    น้อยกว่า 5 → รอบของเรื่องที่ 4, 5 หาหนังไม่เจอ / มากกว่า 5 → เรื่องที่เกินไม่มีรอบ
        if (movies == null || movies.size() != LINEUP_SIZE) {
            int selected = (movies == null) ? 0 : movies.size();
            throw new IllegalArgumentException(
                    "Please select exactly " + LINEUP_SIZE + " movies (selected " + selected + ").");
        }
        // 4. ไล่ทีละเรื่อง เช็กว่าไม่ซ้ำและมีอยู่จริง
        Set<String> existingIds = allMovieIds(); // รหัสหนังทั้งหมดใน movies.csv เช่น {M1, M2, ..., M15}
        Set<String> chosenIds = new HashSet<>(); // รหัสที่เลือกไปแล้วในรอบนี้ ใช้จับเรื่องซ้ำ
        for (Movie movie : movies) {
            if (movie == null) {
                throw new IllegalArgumentException("movie must not be null");
            }
            // chosenIds.add(รหัส) : ยังไม่เคยเลือก → ใส่แล้วได้ true / เคยเลือกแล้ว → ไม่ใส่ซ้ำ ได้ false
            //   ได้ false = เลือกเรื่องนี้ซ้ำ → throw
            if (!chosenIds.add(movie.id())) {
                throw new IllegalArgumentException("Movie " + movie.id() + " is selected more than once.");
            }
            // existingIds.contains(รหัส) : รหัสนี้มีใน movies.csv ไหม
            //   ไม่มี → ถ้าบันทึกไป findLineupOn จะ error ตอนหน้าลูกค้าอ่าน เลยกันไว้ตั้งแต่ตอนบันทึก
            if (!existingIds.contains(movie.id())) {
                throw new IllegalArgumentException("Movie " + movie.id() + " not found.");
            }
        }
        // 5. บันทึกเป็นชุดของพรุ่งนี้ (วันเดิมเคยตั้งไว้ → เขียนทับ)
        movieRepository.saveLineup(tomorrow(), movies);
    }

    /**
     * วันพรุ่งนี้ตามนาฬิกาของโปรแกรม
     * ใช้ทั้งตอนอ่านและตอนบันทึก จะได้ได้วันเดียวกันแน่นอน
     * เช่น ตอนนี้ 2026-10-08 13:00 → 2026-10-09 / ตอนนี้ 2026-10-08 23:59 → 2026-10-09
     *
     * @return วันพรุ่งนี้
     */
    private LocalDate tomorrow() {
        // clock.now().toLocalDate() : วันนี้ เช่น 2026-10-08 / .plusDays(1) : บวก 1 วัน → 2026-10-09
        return clock.now().toLocalDate().plusDays(1);
    }

    /**
     * รหัสหนังทั้งหมดใน movies.csv ใช้เช็กว่าหนังที่ admin เลือกมีอยู่จริง
     * เช่น {M1, M2, ..., M15}
     *
     * @return กลุ่มรหัสหนังทั้งหมด
     * @throws IOException ถ้าอ่าน movies.csv ไม่ได้
     */
    private Set<String> allMovieIds() throws IOException {
        Set<String> ids = new HashSet<>();
        for (Movie movie : movieRepository.findAll()) {
            ids.add(movie.id());
        }
        return ids;
    }

    // ตรวจ RI
    private void checkRep() {
        assert movieRepository != null;
        assert clock != null;
    }
}