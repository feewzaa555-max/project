//AdminService <- ตัวที่หน้า admin คุยด้วย ให้ admin เลือกหนัง 5 เรื่องที่จะฉายพรุ่งนี้ (lineup.csv) เพิ่มหนังเรื่องใหม่ และลบหนัง (movies.csv)
/*
    หน้า admin ใช้ 5 เมธอดนี้:
    tomorrowLineup()                   → หนัง 5 เรื่องที่จะฉายพรุ่งนี้ ใช้โชว์ว่าตอนนี้ตั้งไว้เป็นเรื่องอะไร
    setTomorrowLineup(admin, หนัง 5 เรื่อง) → เช็กกฎ แล้วบันทึกเป็นชุดหนังของพรุ่งนี้
    tomorrow()                         → วันที่ของพรุ่งนี้ ใช้เขียน "Showing on 09/10/2026"
    addMovie(admin, ชื่อ, นาที, ไฟล์รูป)    → เช็กกฎ ออกรหัสใหม่ (M16) บันทึกลง movies.csv และ copy รูปโปสเตอร์
    deleteMovie(admin, รหัสหนัง)          → เช็กกฎ (เกิน 10 เรื่อง, ไม่อยู่ใน lineup) ลบออกจาก movies.csv และลบรูปโปสเตอร์

    ทำไมแยกเป็น class ใหม่ ไม่ใส่ใน MovieService:
        MovieService เป็นของหน้าลูกค้า (ดูหนัง ดูรอบ) ส่วนนี้เป็นงานของ admin (ตั้งหนังที่จะฉาย)
        แยกกันจะได้แก้กฎของ admin โดยไม่กระทบหน้าลูกค้า (SC5 SRP: 1 class มีเหตุผลให้แก้เรื่องเดียว)

    ตัวอย่างการใช้ (ในโปรแกรมจริง AppServices เป็นคนสร้างให้, สมมุติวันนี้ 8/10):
        AdminService adminService = new AdminService(movieRepository, clock, Path.of("Booking_tickets/data"));

        User admin = authService.login("admin", "admin1");          // role ADMIN
        List<Movie> all = movieService.movies();                    // หนังทั้งหมด 15 เรื่อง ให้ admin เลือก
        adminService.setTomorrowLineup(admin, List.of(all.get(5), all.get(6), all.get(7), all.get(8), all.get(9)));
                                                                    // lineup.csv ได้บรรทัด 2026-10-09,M6;M7;M8;M9;M10
        adminService.tomorrowLineup();                              // [Hello World, Your Name, Weathering with You, Inception, Interstellar]

        adminService.setTomorrowLineup(customer, ...)               // ลูกค้าธรรมดา → throw "Only admin can set the lineup."

        adminService.addMovie(admin, "Dune: Part Two", 166, Path.of("C:/Users/me/Pictures/dune.jpg"));
                                                                    // movies.csv ได้บรรทัด M16,Dune: Part Two,166,posters/m16.jpg
                                                                    // รูป dune.jpg ถูก copy ไปเป็น data/posters/m16.jpg

        adminService.deleteMovie(admin, "M16");                     // บรรทัด M16 หายจาก movies.csv, ลบ data/posters/m16.jpg
        adminService.deleteMovie(admin, "M3");                      // M3 อยู่ใน lineup 2026-10-01 → throw ลบไม่ได้
 */

package service;

import java.io.IOException;              // error ตอนอ่าน/เขียน movies.csv / lineup.csv / copy รูปไม่ได้ (ส่งต่อให้หน้าจอ)
import java.nio.file.Files;              // คำสั่ง copy ไฟล์รูป / สร้างโฟลเดอร์ posters / เช็กว่ามีไฟล์ไหม / ลบไฟล์รูปตอนลบหนัง
import java.nio.file.Path;               // ตำแหน่งไฟล์ เช่น โฟลเดอร์ data, ไฟล์รูปที่ admin เลือก
import java.nio.file.StandardCopyOption; // REPLACE_EXISTING = copy ทับได้ถ้ามีไฟล์ชื่อเดียวกันค้างอยู่
import java.time.LocalDate;              // วันที่อย่างเดียว ใช้หาวันพรุ่งนี้ เช่น 2026-10-09
import java.util.Collections;            // Collections.max = หาวันที่ล่าสุดที่หนังอยู่ใน lineup เอาไปบอกในข้อความ error ตอนลบไม่ได้
import java.util.HashSet;                // เก็บรหัสหนังแบบไม่ซ้ำ ใช้เช็กเลือกเรื่องซ้ำ และเช็กว่าหนังมีจริง
import java.util.List;                   // ชนิดของรายการหนังที่รับเข้ามา / คืนออกไป
import java.util.Locale;                 // Locale.ROOT = แปลงตัวพิมพ์เล็ก/ใหญ่แบบเดียวกันทุกเครื่อง (เทียบชื่อหนัง, นามสกุลไฟล์)
import java.util.Set;                    // ชนิดของกลุ่มที่ไม่มีตัวซ้ำ
import model.Movie;                      // class หนัง อยู่คนละ package เลยต้อง import
import model.User;                       // ผู้ใช้ที่ login แล้ว ใช้เช็กว่าเป็น admin ไหม
import repository.MovieRepository;       // ที่เก็บหนัง/ชุดหนังที่ฉาย (interface) รับเข้ามาทาง constructor

/**
 * บริการของ admin: ตั้งหนังที่จะฉายพรุ่งนี้ เพิ่มหนังเรื่องใหม่ และลบหนัง
 * ข้อมูลหนังอ่าน/เขียนผ่าน MovieRepository และถามวันนี้จาก AppClock (ไฟล์ที่ทำเองมีแค่ copy / ลบรูปโปสเตอร์)
 * กฎการตั้งหนัง (ต้องเป็น admin, ครบ 5 เรื่อง, ไม่ซ้ำ, มีอยู่จริง)
 * กฎการเพิ่มหนัง (ต้องเป็น admin, ชื่อไม่ว่าง/ไม่ซ้ำ, ความยาว, ชนิดรูป)
 * และกฎการลบหนัง (ต้องเป็น admin, ต้องเหลืออย่างน้อย 10 เรื่อง, ไม่อยู่ใน lineup) อยู่ที่ class นี้ที่เดียว
 */
public class AdminService {

    // AF: บริการของ admin ที่อ่าน/บันทึกหนังและชุดหนังผ่าน movieRepository, รู้วันนี้จาก clock
    //     และเก็บรูปโปสเตอร์ของหนังเรื่องใหม่ไว้ในโฟลเดอร์ dataFolder/posters
    // RI: movieRepository, clock และ dataFolder ไม่เป็น null
    // Safety from rep exposure: field เป็น private final, List ที่คืนออกไปสร้างใหม่ทุกครั้ง (มาจาก repository)
    // Thread safety: ไม่มี field ที่แก้ค่าได้ (final ทั้งหมด), AppClock ล็อกของตัวเองอยู่แล้ว (SC7)

    /**
     * จำนวนหนังที่ต้องเลือกต่อวัน
     * ต้องเท่ากับจำนวนเรื่องใน schedule.csv (ช่อง slot มี 1–5) ถ้าเลือกไม่ครบ รอบของเรื่องที่ขาดจะหาหนังไม่เจอ
     */
    public static final int LINEUP_SIZE = 5;

    /**
     * จำนวนหนังน้อยสุดที่ต้องเหลือในระบบ มีเกินจำนวนนี้ถึงจะลบได้ (มี 11 → ลบได้ เหลือ 10 / มี 10 → ลบไม่ได้)
     * ต้องไม่น้อยกว่า LINEUP_SIZE ไม่งั้น admin อาจเลือกหนังไม่ครบ 5 เรื่อง
     * ตั้ง 10 = เลือก lineup ได้อย่างน้อย 2 ชุดไม่ซ้ำกัน
     */
    public static final int MIN_MOVIES = 10;

    /**
     * โฟลเดอร์เก็บรูปโปสเตอร์ (อยู่ในโฟลเดอร์ data) ใช้ 2 ที่ให้ตรงกัน:
     *   ช่องรูปใน movies.csv  "posters/m16.jpg"
     *   ไฟล์จริงที่ copy ไปวาง  data/posters/m16.jpg
     * หน้าลูกค้า (MovieRow) หารูปด้วย dataFolder + ช่องรูป จึงต้องเป็นชื่อเดียวกัน
     */
    private static final String POSTERS_FOLDER = "posters";
    /** รหัสหนังขึ้นต้นด้วยตัวนี้ตามด้วยเลข เช่น M1, M15 → เรื่องใหม่ได้ M16 */
    private static final String MOVIE_ID_PREFIX = "M";
    /** นามสกุลรูปที่รับ (หน้าลูกค้าเปิดได้ทั้งคู่) */
    private static final List<String> POSTER_TYPES = List.of("jpg", "jpeg", "png");

    private final MovieRepository movieRepository; // ที่เก็บหนังและชุดหนังที่ฉาย เช่น CsvMovieRepository
    private final AppClock clock;                  // นาฬิกาตัวเดียวของโปรแกรม ใช้รู้ว่าพรุ่งนี้คือวันไหน
    private final Path dataFolder;                 // โฟลเดอร์ data เช่น Booking_tickets/data ใช้หาโฟลเดอร์ posters

    /**
     * รับที่เก็บข้อมูลกับนาฬิกาเข้ามาทาง constructor ไม่ new เอง (SC5 DIP)
     * ใช้ MovieRepository ตัวเดียวกับ MovieService (AppServices ส่งตัวเดียวกันมาให้)
     * admin ตั้งหนังแล้ว หน้าลูกค้าจะเห็นชุดใหม่ทันทีที่ถึงวันนั้น เพราะอ่านจากไฟล์เดียวกัน
     *
     * dataFolder ใช้แค่ตอนเพิ่มหนัง: copy รูปโปสเตอร์ไปไว้ที่ dataFolder/posters
     * (โฟลเดอร์เดียวกับที่หน้าลูกค้าใช้หารูป ผ่าน app.dataFolder())
     *
     * ตัวอย่าง:
     *   new AdminService(new CsvMovieRepository(moviesPath, schedulePath, lineupPath), new AppClock(), Path.of("Booking_tickets/data"))
     *   new AdminService(null, clock, folder) → throw "movieRepository must not be null"
     *
     * @param movieRepository ที่เก็บหนังและชุดหนังที่ฉาย ห้าม null
     * @param clock           นาฬิกาของโปรแกรม ห้าม null
     * @param dataFolder      โฟลเดอร์ data (มีโฟลเดอร์ posters อยู่ข้างใน หรือจะสร้างให้ตอนเพิ่มหนัง) ห้าม null
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null
     */
    public AdminService(MovieRepository movieRepository, AppClock clock, Path dataFolder) {
        if (movieRepository == null) {
            throw new IllegalArgumentException("movieRepository must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        if (dataFolder == null) {
            throw new IllegalArgumentException("dataFolder must not be null");
        }
        this.movieRepository = movieRepository;
        this.clock = clock;
        this.dataFolder = dataFolder;
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
     * เป็น public ให้หน้า admin (AdminFrame) เอาไปเขียน "Showing on 09/10/2026"
     * หน้าจอจะได้ไม่ต้องคำนวณวันพรุ่งนี้ซ้ำเอง (ถ้าคำนวณเอง อาจไม่ตรงกับวันที่ Save บันทึกจริง)
     * เช่น ตอนนี้ 2026-10-08 13:00 → 2026-10-09 / ตอนนี้ 2026-10-08 23:59 → 2026-10-09
     *
     * @return วันพรุ่งนี้
     */
    public LocalDate tomorrow() {
        // clock.now().toLocalDate() : วันนี้ เช่น 2026-10-08 / .plusDays(1) : บวก 1 วัน → 2026-10-09
        return clock.now().toLocalDate().plusDays(1);
    }

    /**
     * เพิ่มหนังเรื่องใหม่ลง movies.csv (หน้า admin กด Add Movie)
     * เพิ่มแล้วหนังเรื่องนี้จะอยู่ในรายการให้ admin เลือกเข้า lineup ได้ทันที (ยังไม่ฉายจนกว่าจะเลือก)
     *
     * วิธีทำงาน:
     *   1. admin เป็น null / ไม่ใช่ admin → throw "Only admin can add movies."
     *   2. ชื่อว่าง → throw "Please enter a movie title."
     *      ตัดเว้นวรรคหน้าหลังก่อน "  Dune  " → "Dune"
     *   3. ความยาวไม่อยู่ใน 1–180 → throw "Duration must be 1-180 minutes."
     *      (180 มาจาก Movie.MAX_DURATION_MINUTES ที่เดียว หนังยาวกว่านี้รอบจะทับกัน)
     *   4. มีไฟล์รูป แต่ไม่มีไฟล์นั้นจริง หรือไม่ใช่ .jpg/.jpeg/.png → throw
     *   5. อ่านหนังเดิมทั้งหมด: ชื่อซ้ำ (ไม่สนตัวพิมพ์เล็กใหญ่) → throw "This movie already exists: M1"
     *      (ถ้าปล่อย admin จะเห็น 2 เรื่องชื่อเดียวกันในช่องเลือก ไม่รู้ว่าอันไหนอันไหน)
     *   6. ออกรหัสใหม่ = เลขมากสุดที่มี + 1 เช่น มีถึง M15 → M16
     *   7. ช่องรูปใน movies.csv = "posters/m16" + นามสกุลรูป (ไม่ส่งรูป → "posters/m16.jpg"
     *      หน้าลูกค้าหาไฟล์ไม่เจอจะโชว์การ์ดสีแทน และวางรูปชื่อนี้ทีหลังได้)
     *   8. movieRepository.addMovie(...) บันทึก (ชื่อมี , จะโดนกันตรงนี้ เพราะเป็นกฎของไฟล์ CSV)
     *   9. บันทึกสำเร็จแล้วค่อย copy รูป (ถ้าบันทึกไม่ผ่าน จะไม่มีรูปค้างในโฟลเดอร์)
     *
     * ตัวอย่าง (มีหนัง M1–M15):
     *   addMovie(admin, "Dune: Part Two", 166, dune.jpg) → Movie(M16, Dune: Part Two, 166, posters/m16.jpg) + copy รูป
     *   addMovie(admin, "Your Name 2", 110, null)         → Movie(M17, Your Name 2, 110, posters/m17.jpg) ไม่มีรูป
     *   addMovie(customer, ...)                           → throw "Only admin can add movies."
     *   addMovie(admin, "   ", 100, null)                 → throw "Please enter a movie title."
     *   addMovie(admin, "Dune", 0, null)                  → throw "Duration must be 1-180 minutes."
     *   addMovie(admin, "pacific rim", 131, null)         → throw "This movie already exists: M2"
     *   addMovie(admin, "Dune", 166, notes.txt)           → throw "Poster must be a .jpg, .jpeg or .png file."
     *
     * @param admin      ผู้ใช้ที่ login แล้ว ต้องเป็น admin ห้าม null
     * @param title      ชื่อหนัง ห้ามว่าง ห้ามซ้ำ ห้ามมี ,
     * @param minutes    ความยาวหนังเป็นนาที 1–180
     * @param posterFile ไฟล์รูปที่ admin เลือก (.jpg / .jpeg / .png) หรือ null ถ้าไม่ใส่รูป
     * @return หนังเรื่องใหม่ที่บันทึกแล้ว
     * @throws IOException ถ้าอ่าน/เขียน movies.csv ไม่ได้ หรือ copy รูปไม่ได้
     * @throws IllegalArgumentException ถ้าผิดกฎข้อ 1–5 หรือชื่อมี , (ข้อความภาษาอังกฤษ ให้หน้าจอโชว์ได้เลย)
     */
    public Movie addMovie(User admin, String title, int minutes, Path posterFile) throws IOException {
        // 1. ต้องเป็น admin (กันที่นี่ด้วย ไม่พึ่งแค่หน้าจอ แบบเดียวกับ setTomorrowLineup)
        if (admin == null || !admin.isAdmin()) {
            throw new IllegalArgumentException("Only admin can add movies.");
        }
        // 2. ชื่อต้องไม่ว่าง
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Please enter a movie title.");
        }
        String cleanTitle = title.trim();   // "  Dune  " → "Dune"
        // 3. ความยาว 1–180 นาที (Movie ก็เช็ก แต่ข้อความของ Movie เป็นข้อความสำหรับโปรแกรมเมอร์ เลยเช็กก่อนให้ข้อความอ่านง่าย)
        if (minutes < 1 || minutes > Movie.MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException("Duration must be 1-" + Movie.MAX_DURATION_MINUTES + " minutes.");
        }
        // 4. ไฟล์รูป (ถ้ามี) ต้องมีจริง และเป็นรูปที่หน้าลูกค้าเปิดได้
        //    posterType(ไฟล์) = นามสกุลตัวเล็ก เช่น "C:/.../Dune.JPG" → "jpg"
        String posterType = "jpg";          // ไม่ส่งรูป → ตั้งชื่อเผื่อไว้เป็น .jpg
        if (posterFile != null) {
            if (!Files.isRegularFile(posterFile)) {
                throw new IllegalArgumentException("Poster file not found: " + posterFile);
            }
            posterType = posterType(posterFile);
            if (!POSTER_TYPES.contains(posterType)) {
                throw new IllegalArgumentException("Poster must be a .jpg, .jpeg or .png file.");
            }
        }
        // 5 + 6. อ่านหนังเดิม: เช็กชื่อซ้ำ และหาเลขรหัสมากสุด
        List<Movie> movies = movieRepository.findAll();
        int maxNumber = 0;
        for (Movie movie : movies) {
            // equalsIgnoreCase = เทียบไม่สนตัวพิมพ์เล็กใหญ่ "pacific rim" กับ "Pacific Rim" → ซ้ำ
            if (movie.title().trim().equalsIgnoreCase(cleanTitle)) {
                throw new IllegalArgumentException("This movie already exists: " + movie.id());
            }
            maxNumber = Math.max(maxNumber, idNumber(movie.id()));   // M15 → 15
        }
        String id = MOVIE_ID_PREFIX + (maxNumber + 1);               // 15 + 1 → "M16"

        // 7. ช่องรูปใน movies.csv เช่น "posters/m16.jpg" (ใช้ / เพราะเป็นข้อความในไฟล์ ใช้ได้ทั้ง Windows และเครื่องอื่น)
        String posterPath = POSTERS_FOLDER + "/" + id.toLowerCase(Locale.ROOT) + "." + posterType;
        Movie movie = new Movie(id, cleanTitle, minutes, posterPath);

        // 8. บันทึกลง movies.csv (ชื่อมี , → repository throw IllegalArgumentException ไม่บันทึก)
        movieRepository.addMovie(movie);

        // 9. copy รูปไปไว้ที่ data/posters/m16.jpg ให้ตรงกับช่องรูปที่เพิ่งบันทึก
        //    dataFolder.resolve("posters/m16.jpg") = data/posters/m16.jpg
        //    createDirectories = ยังไม่มีโฟลเดอร์ posters ก็สร้างให้
        //    REPLACE_EXISTING  = มีไฟล์ชื่อนี้ค้างอยู่ (เช่น เคยวางไว้เอง) → ทับด้วยรูปที่เพิ่งเลือก
        if (posterFile != null) {
            Path target = dataFolder.resolve(posterPath);
            Files.createDirectories(target.getParent());
            Files.copy(posterFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return movie;
    }

    /**
     * ลบหนัง 1 เรื่องออกจาก movies.csv (หน้า admin กด Delete Movie)
     *
     * วิธีทำงาน:
     *   1. admin เป็น null / ไม่ใช่ admin → throw "Only admin can delete movies."
     *   2. ไม่ได้เลือกหนัง (null / ว่าง) → throw "Please choose a movie to delete."
     *   3. อ่านหนังทั้งหมด หาเรื่องที่จะลบ ไม่เจอ → throw "Movie M99 not found."
     *   4. มีหนังไม่เกิน MIN_MOVIES (10) → throw "Cannot delete: at least 10 movies must remain (now 10)."
     *   5. หนังเรื่องนี้อยู่ใน lineup วันไหน (รวมวันที่ผ่านไปแล้วและพรุ่งนี้) → throw
     *      "Cannot delete M3: it is in the lineup on 2026-10-01."
     *      เหตุผล: lineup.csv ถูกตรวจรหัสทุกบรรทัด ถ้าลบหนังที่อยู่ในบรรทัดไหน หน้าลูกค้าจะเปิดรอบฉายไม่ได้ทุกวัน
     *      และหนังที่มีคนเคยจอง ต้องเคยอยู่ใน lineup มาก่อน ข้อนี้จึงกันประวัติการจองหาชื่อหนังไม่เจอไปด้วย
     *   6. ผ่านหมด → movieRepository.deleteMovie(รหัส)
     *   7. ลบไฟล์รูปโปสเตอร์ด้วย (deletePoster) เพราะถ้าเพิ่มหนังใหม่แล้วได้รหัสนี้อีก จะได้ไม่เห็นรูปของเรื่องเก่า
     *
     * ตัวอย่าง (มีหนัง M1–M15, lineup.csv มีแค่ 2026-10-01,M1;M2;M3;M4;M5):
     *   deleteMovie(admin, "M15")    → ลบได้ คืน Movie(M15, คู่แรด, ...) + ลบ data/posters/m15.jpg
     *   deleteMovie(admin, "M3")     → throw "Cannot delete M3: it is in the lineup on 2026-10-01."
     *   deleteMovie(customer, "M15") → throw "Only admin can delete movies."
     *   ลบจนเหลือ 10 เรื่องแล้วลบอีก  → throw "Cannot delete: at least 10 movies must remain (now 10)."
     *
     * @param admin   ผู้ใช้ที่ login แล้ว ต้องเป็น admin ห้าม null
     * @param movieId รหัสหนังที่จะลบ เช่น "M15"
     * @return Movie หนังเรื่องที่ถูกลบ (หน้าจอเอาชื่อไปบอก admin)
     * @throws IOException ถ้าอ่าน/เขียน movies.csv / lineup.csv ไม่ได้ หรือลบไฟล์รูปไม่ได้
     * @throws IllegalArgumentException ถ้าผิดกฎข้อ 1–5 (ข้อความภาษาอังกฤษ ให้หน้าจอโชว์ได้เลย)
     */
    public Movie deleteMovie(User admin, String movieId) throws IOException {
        // 1. ต้องเป็น admin (กันที่นี่ด้วย ไม่พึ่งแค่หน้าจอ แบบเดียวกับ addMovie)
        if (admin == null || !admin.isAdmin()) {
            throw new IllegalArgumentException("Only admin can delete movies.");
        }
        // 2. ต้องเลือกหนัง
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException("Please choose a movie to delete.");
        }
        String id = movieId.trim();
        // 3. หาเรื่องที่จะลบ (ใช้ตอนลบรูป และคืนให้หน้าจอบอกชื่อ)
        List<Movie> movies = movieRepository.findAll();
        Movie target = null;
        for (Movie movie : movies) {
            if (movie.id().equals(id)) {
                target = movie;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Movie " + id + " not found.");
        }
        // 4. ต้องเหลืออย่างน้อย MIN_MOVIES เรื่องหลังลบ  มี 11 → ลบได้ / มี 10 → ลบไม่ได้
        if (movies.size() <= MIN_MOVIES) {
            throw new IllegalArgumentException("Cannot delete: at least " + MIN_MOVIES
                    + " movies must remain (now " + movies.size() + ").");
        }
        // 5. ห้ามอยู่ใน lineup วันไหนเลย
        //    findLineupDatesOf("M3") → [2026-10-01] / ไม่เคยอยู่ → []
        //    Collections.max(วันที่ทั้งหมด) = วันล่าสุด เอาไปบอกในข้อความ (เช่น พรุ่งนี้ ที่ admin เพิ่งตั้ง)
        List<LocalDate> lineupDates = movieRepository.findLineupDatesOf(id);
        if (!lineupDates.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete " + id + ": it is in the lineup on "
                    + Collections.max(lineupDates) + ".");
        }
        // 6. ลบออกจาก movies.csv
        movieRepository.deleteMovie(id);
        // 7. ลบไฟล์รูป (ถ้ามี) หลังลบในไฟล์สำเร็จแล้วเท่านั้น
        deletePoster(target, movies);
        return target;
    }

    /**
     * ลบไฟล์รูปโปสเตอร์ของหนังที่เพิ่งลบ เช่น "posters/m15.jpg" → ลบ data/posters/m15.jpg
     * ไม่ลบ ถ้า:
     *   - ช่องรูปว่าง (หนังเรื่องนี้ไม่มีรูป)
     *   - ไฟล์อยู่นอกโฟลเดอร์ data/posters (เช่นช่องรูปเขียน "../users.csv") กันลบไฟล์อื่นของโปรแกรม
     *   - หนังเรื่องอื่นใช้รูปไฟล์เดียวกัน (ลบไปแล้วเรื่องนั้นจะไม่มีรูป)
     * ไม่มีไฟล์อยู่แล้ว → ไม่ทำอะไร (Files.deleteIfExists ไม่ error)
     *
     * @param deleted หนังที่เพิ่งลบ (ใช้ช่อง posterPath)
     * @param movies  หนังทั้งหมดก่อนลบ (ใช้เช็กว่ามีเรื่องอื่นใช้รูปเดียวกันไหม)
     * @throws IOException ถ้าลบไฟล์ไม่ได้ เช่น ไฟล์ถูกเปิดค้างอยู่
     */
    private void deletePoster(Movie deleted, List<Movie> movies) throws IOException {
        if (deleted.posterPath().isBlank()) {
            return;
        }
        // normalize() = ตัด "." / ".." ออกก่อนเทียบ  data/posters/../users.csv → data/users.csv
        Path postersFolder = dataFolder.resolve(POSTERS_FOLDER).normalize();
        Path file = dataFolder.resolve(deleted.posterPath()).normalize();
        if (!file.startsWith(postersFolder)) {
            return;   // ไม่ได้อยู่ในโฟลเดอร์ posters → ไม่แตะ
        }
        for (Movie movie : movies) {
            if (!movie.id().equals(deleted.id()) && movie.posterPath().equals(deleted.posterPath())) {
                return;   // มีเรื่องอื่นใช้รูปนี้อยู่ → เก็บไว้
            }
        }
        Files.deleteIfExists(file);
    }

    /**
     * นามสกุลไฟล์ตัวเล็ก เพื่อเช็กชนิดรูปและตั้งชื่อไฟล์ใหม่
     * "dune.JPG" → "jpg" / "poster.final.png" → "png" (เอาหลังจุดตัวสุดท้าย) / "dune" (ไม่มีจุด) → ""
     *
     * @param file ไฟล์รูปที่ admin เลือก เช่น C:/Users/me/Pictures/Dune.JPG
     * @return String นามสกุลตัวเล็กไม่มีจุด เช่น "jpg" / ไม่มีนามสกุล → ""
     */
    private static String posterType(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');     // ตำแหน่งจุดตัวสุดท้าย ไม่มีจุด → -1
        return (dot < 0) ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * เลขในรหัสหนัง เพื่อหาเลขมากสุดแล้วออกรหัสใหม่
     * "M15" → 15 / "M2" → 2 / รหัสที่ไม่ใช่ M ตามด้วยเลข เช่น "X1", "M1a" → 0 (ไม่นับ)
     * ใช้เลข ไม่ใช้เทียบข้อความ เพราะเทียบข้อความ "M9" จะมากกว่า "M15"
     *
     * @param id รหัสหนัง เช่น "M15"
     * @return int เลขในรหัส เช่น 15 / ไม่ใช่ M ตามด้วยเลข → 0
     */
    private static int idNumber(String id) {
        if (!id.startsWith(MOVIE_ID_PREFIX)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.substring(MOVIE_ID_PREFIX.length()));   // "M15" → "15" → 15
        } catch (NumberFormatException e) {
            return 0;
        }
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

    /**
     * ตรวจ RI
     */
    private void checkRep() {
        assert movieRepository != null;
        assert clock != null;
        assert dataFolder != null;
    }
}