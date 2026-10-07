//CsvMovieRepository <- อ่านหนัง ตารางฉาย และหนังที่ฉายแต่ละวัน (lineup) จากไฟล์ CSV และบันทึก lineup
//ตัวอย่างไฟล์ดูได้ที่ Booking_tickets/data/movies.csv, schedule.csv และ lineup.csv
/*
    ทำไมต้องมี 3 ไฟล์:
      admin ต้องเปลี่ยนหนังได้ทุกวัน แต่เวลารอบกับโรงไม่เปลี่ยน
      เลยแยก "รอบ" (schedule.csv) ออกจาก "วันนี้ฉายเรื่องอะไร" (lineup.csv)
      admin แก้แค่ lineup.csv ไฟล์เดียว รอบทั้ง 15 รอบเปลี่ยนหนังตามให้เอง

      movies.csv    หนังทุกเรื่องที่มีในระบบ        M9,Inception,148,posters/m9.jpg
      schedule.csv  แต่ละรอบฉายหนัง "เรื่องที่เท่าไหร่" 1,08:45,1   = โรง 1 เวลา 08:45 ฉายเรื่องที่ 1
      lineup.csv    วันนั้นเรื่องที่ 1–5 คืออะไร       2026-10-09,M6;M9;M10;M11;M15

    ตัวอย่าง: วันที่ 9/10 รอบ "1,08:45,1"
      → lineup ของ 9/10 คือ [M6, M9, M10, M11, M15]
      → เรื่องที่ 1 = M6 (Hello World)
      → ได้รอบ: Hello World โรง 1 วันที่ 9/10 เวลา 08:45

    ถ้าวันไหน admin ไม่ได้ตั้ง ใช้ชุดล่าสุดที่ตั้งไว้ก่อนหน้า (หน้าจอจะได้มีรอบทุกวัน)
      lineup.csv มี 1/10 กับ 9/10 → วันที่ 5/10 ใช้ชุด 1/10 / วันที่ 12/10 ใช้ชุด 9/10
 */

package repository;

import java.io.IOException;                      // error ตอนอ่าน/เขียนไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import java.nio.charset.StandardCharsets;        // อ่าน/เขียนไฟล์แบบ UTF-8 ชื่อหนังภาษาไทยไม่เพี้ยน
import java.nio.file.Files;                      // คำสั่งเช็กว่ามีไฟล์ไหม อ่านไฟล์ทุกบรรทัด และเขียนไฟล์
import java.nio.file.Path;                       // เก็บตำแหน่งไฟล์ เช่น data/movies.csv
import java.time.LocalDate;                      // วันที่ เช่น 2026-10-09 ทั้งที่ส่งเข้ามาและในช่องแรกของ lineup.csv
import java.time.LocalTime;                      // แปลงข้อความ "08:45" ในไฟล์ให้เป็นเวลา
import java.time.format.DateTimeParseException;  // error ตอนวันที่/เวลาในไฟล์ผิดรูปแบบ เช่น "8:45"
import java.util.ArrayList;                      // สร้างรายการใหม่ไว้ใส่หนัง / รอบฉาย / บรรทัด
import java.util.HashSet;                        // เก็บวันที่ที่เจอแล้วใน lineup.csv ไว้เช็กวันซ้ำ
import java.util.List;                           // ชนิดของรายการที่คืนออกไป
import java.util.Set;                            // ชนิดของกลุ่มวันที่ที่ไม่มีตัวซ้ำ
import model.Movie;                              // class หนัง อยู่คนละ package เลยต้อง import
import model.Showtime;                           // class รอบฉาย อยู่คนละ package เลยต้อง import

/**
 * อ่านหนัง ตารางฉาย และ lineup จากไฟล์ CSV 3 ไฟล์ (ไม่มีบรรทัดหัวตาราง, บรรทัดว่างข้ามได้)
 * movies.csv   บรรทัดละ 1 เรื่อง: รหัส,ชื่อ,ความยาวนาที,รูปโปสเตอร์  เช่น M1,Spider-Man: Brand New Day,145,posters/m1.jpg
 * schedule.csv บรรทัดละ 1 รอบ:   โรง,เวลาเริ่ม,เรื่องที่              เช่น 1,08:45,1
 * lineup.csv   บรรทัดละ 1 วัน:   วันที่,รหัสหนังเรื่องที่1;2;3;4;5    เช่น 2026-10-01,M1;M2;M3;M4;M5
 * ข้อมูลในไฟล์ผิดจะโยน IOException ที่บอกชื่อไฟล์และเลขบรรทัด ไม่ข้ามเงียบ ๆ
 */
public class CsvMovieRepository implements MovieRepository {

    // AF: หนังทั้งหมดคือบรรทัดใน moviesFile, รอบประจำวัน (บอกแค่ว่าเป็นเรื่องที่เท่าไหร่) คือบรรทัดใน scheduleFile,
    //     หนังที่ฉายแต่ละวันคือบรรทัดใน lineupFile
    // RI: moviesFile, scheduleFile และ lineupFile ไม่เป็น null
    // Safety from rep exposure: ทั้งสาม field เป็น private final และ Path แก้ไขไม่ได้,
    //                           List ที่คืนออกไปสร้างใหม่ทุกครั้ง แก้แล้วไม่กระทบไฟล์

    // ลำดับช่องใน movies.csv (นับจาก 0)  M1 , Spider-Man , 145 , posters/m1.jpg
    //                                  0        1         2          3
    private static final int MOVIE_ID = 0;
    private static final int MOVIE_TITLE = 1;
    private static final int MOVIE_DURATION = 2;
    private static final int MOVIE_POSTER = 3;
    private static final int MOVIE_COLUMNS = 4;

    // ลำดับช่องใน schedule.csv (นับจาก 0)  1 , 08:45 , 1
    //                                    0     1     2 (เรื่องที่เท่าไหร่ใน lineup)
    private static final int SCHEDULE_HALL = 0;
    private static final int SCHEDULE_START = 1;
    private static final int SCHEDULE_SLOT = 2;
    private static final int SCHEDULE_COLUMNS = 3;

    // ลำดับช่องใน lineup.csv (นับจาก 0)  2026-10-01 , M1;M2;M3;M4;M5
    //                                  0               1
    private static final int LINEUP_DATE = 0;
    private static final int LINEUP_MOVIES = 1;
    private static final int LINEUP_COLUMNS = 2;
    private static final String LINEUP_MOVIE_SEPARATOR = ";"; // คั่นรหัสหนังในช่องที่ 2 (ใช้ ; เพราะ , ใช้คั่นช่องไปแล้ว)

    private final Path moviesFile;   // เช่น data/movies.csv
    private final Path scheduleFile; // เช่น data/schedule.csv
    private final Path lineupFile;   // เช่น data/lineup.csv

    /**
     * จำที่อยู่ไฟล์ทั้ง 3 ไว้ เพื่อให้เมธอดอื่นเปิดอ่าน/เขียนได้ ยังไม่เปิดไฟล์ตอนนี้
     * (เปิดตอนใช้จริง ข้อมูลที่ได้จะเป็นของล่าสุดเสมอ แม้มีคนแก้ไฟล์ระหว่างโปรแกรมเปิดอยู่)
     *
     * @param moviesFile   ไฟล์รายชื่อหนัง เช่น data/movies.csv
     * @param scheduleFile ไฟล์ตารางฉาย เช่น data/schedule.csv
     * @param lineupFile   ไฟล์หนังที่ฉายแต่ละวัน เช่น data/lineup.csv
     * @throws IllegalArgumentException ถ้าไฟล์ไหนเป็น null
     */
    public CsvMovieRepository(Path moviesFile, Path scheduleFile, Path lineupFile) {
        // ไม่รู้ที่อยู่ไฟล์ = ทำงานต่อไม่ได้ เลยหยุดตั้งแต่ตอนสร้าง
        if (moviesFile == null) {
            throw new IllegalArgumentException("moviesFile must not be null");
        }
        if (scheduleFile == null) {
            throw new IllegalArgumentException("scheduleFile must not be null");
        }
        if (lineupFile == null) {
            throw new IllegalArgumentException("lineupFile must not be null");
        }
        this.moviesFile = moviesFile;
        this.scheduleFile = scheduleFile;
        this.lineupFile = lineupFile;
        checkRep();
    }

    /**
     * เอาหนังทุกเรื่องในระบบ เพื่อให้หน้าจอทำโปสเตอร์ และให้เมธอดอื่นหาว่ารหัสหนังคือเรื่องไหน
     * ทำงาน: อ่าน movies.csv ทีละบรรทัด (ข้ามบรรทัดว่าง) → แปลงเป็น Movie
     *        → รหัสซ้ำกับที่มีแล้วโยน error (ถ้าปล่อยไว้ จะไม่รู้ว่า M1 หมายถึงเรื่องไหน)
     * เช่น ไฟล์มี M1, M2, M3 ได้ [หนัง M1, หนัง M2, หนัง M3]
     *
     * @return หนังทุกเรื่องตามลำดับในไฟล์
     * @throws IOException ถ้าไม่มีไฟล์ หรือข้อมูลในไฟล์ผิด
     */
    @Override
    public List<Movie> findAll() throws IOException {
        List<String> lines = readLines(moviesFile);
        List<Movie> movies = new ArrayList<>(); // เริ่มจากรายการเปล่า []
        int lineNumber = 0;                     // นับทุกบรรทัดรวมบรรทัดว่าง เพื่อให้ error บอกเลขบรรทัดตรงกับในไฟล์
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue; // บรรทัดว่าง ไม่มีข้อมูล ข้ามไป
            }
            Movie movie = parseMovie(line, lineNumber);
            // กันรหัสหนังซ้ำ: ถ้ามี M1 สองบรรทัด ระบบจะไม่รู้ว่า M1 คือเรื่องไหน
            // หาในรายการที่ใส่ไปแล้ว: เจอ = ซ้ำ (error) / ไม่เจอ (null) = ใส่ได้
            if (findMovie(movies, movie.id()) != null) {
                throw error(moviesFile, lineNumber, "Movie ID " + movie.id() + " is duplicate of previous line");
            }
            movies.add(movie);
        }
        return movies;
    }

    /**
     * สร้างรอบฉายทั้งหมดของวันที่ date เพื่อให้หน้าเลือกหนังเอาไปทำปุ่มรอบ และให้ BookingService หารอบของตั๋ว
     * ทำงาน: หาก่อนว่าวันนั้นเรื่องที่ 1–5 คืออะไร (findLineupOn)
     *        → อ่าน schedule.csv ทีละบรรทัด → เอาเลข "เรื่องที่" ไปหยิบหนังจาก lineup → สร้าง Showtime
     * เช่น date = 2026-10-09, lineup = [M6, M9, M10, M11, M15]
     *      บรรทัด "1,08:45,1" → เรื่องที่ 1 = M6  → Hello World โรง 1 เวลา 08:45
     *      บรรทัด "2,08:45,3" → เรื่องที่ 3 = M10 → Interstellar โรง 2 เวลา 08:45
     *
     * @param date วันที่ต้องการ ห้าม null
     * @return รอบทั้งหมดของวันนั้นตามลำดับใน schedule.csv หรือรายการว่างถ้าไม่มี lineup
     * @throws IOException ถ้าอ่านไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
     */
    @Override
    public List<Showtime> findShowtimesOn(LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null"); // ไม่รู้ว่าจะสร้างรอบของวันไหน
        }
        // หาว่าวันนั้นฉายเรื่องอะไรบ้าง เพราะ schedule.csv บอกแค่ "เรื่องที่ 1" ไม่ได้บอกว่าเป็นหนังอะไร
        // findLineupOn(date) : หนังของวันนั้นเรียงตามลำดับ เช่น [M1, M2, M3, M4, M5]
        List<Movie> lineup = findLineupOn(date);
        List<Showtime> showtimes = new ArrayList<>();  // เริ่มจากรายการเปล่า []
        if (lineup.isEmpty()) {
            return showtimes; // วันนั้นยังไม่มีหนังให้ฉาย (ก่อนวันแรกที่ตั้งไว้) → ไม่มีรอบ
        }
        List<String> lines = readLines(scheduleFile);
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            showtimes.add(parseShowtime(line, lineNumber, date, lineup));
        }
        return showtimes;
    }

    /**
     * หาว่าวันที่ date ฉายหนังเรื่องอะไรบ้าง เพื่อเอาไปสร้างรอบฉายของวันนั้น
     * ถ้า admin ไม่ได้ตั้งวันนั้นไว้ ใช้ชุดล่าสุดที่ตั้งไว้ก่อนหน้า (หน้าจอจะได้มีรอบทุกวัน)
     * และไม่เอาชุดของวันข้างหน้ามาใช้ (admin ตั้งของพรุ่งนี้แล้ว รอบของวันนี้ต้องไม่เปลี่ยนตาม)
     *
     * ทำงาน: อ่าน lineup.csv ทีละบรรทัด → เลือกบรรทัดที่วันที่ไม่เกิน date และใหม่ที่สุด
     *        ระหว่างอ่านเช็กด้วยว่าไม่มีวันซ้ำ และรหัสหนังมีจริง (ข้อมูลผิดจะรู้ทันที)
     * เช่น lineup.csv มี 2026-10-01 กับ 2026-10-09
     *      findLineupOn(2026-10-05) → ใช้แถว 1/10 (9/10 ยังไม่ถึง)
     *      findLineupOn(2026-10-09) → ใช้แถว 9/10 (ตรงวันพอดี)
     *      findLineupOn(2026-09-30) → ไม่มีแถวไหนก่อนวันนี้ → [] (ไม่มีรอบ)
     *
     * @param date วันที่ต้องการ ห้าม null
     * @return หนังเรียงตามลำดับ (ตัวแรก = เรื่องที่ 1) หรือรายการว่าง
     * @throws IOException ถ้าไม่มีไฟล์ lineup.csv หรือข้อมูลในไฟล์ผิด
     */
    @Override
    public List<Movie> findLineupOn(LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        // ถ้าไม่มีไฟล์ readLines โยน error "File not found" ทันที
        // เพื่อให้รู้ว่าลืมไฟล์ ถ้าคืนรายการว่างเงียบ ๆ หน้าจอจะขึ้นแค่ "ไม่มีรอบ" แล้วไม่มีใครรู้สาเหตุ (SC2 fail-fast)
        List<Movie> movies = findAll();             // หนังทั้งหมด เพื่อแปลงรหัส M9 เป็นหนังจริง
        List<String> lines = readLines(lineupFile);
        Set<LocalDate> seenDates = new HashSet<>(); // จำวันที่ที่เจอแล้ว เพื่อจับวันซ้ำ
        LocalDate bestDate = null;                  // วันที่ของแถวที่ตรงที่สุดที่เจอตอนนี้ (null = ยังไม่เจอ)
        List<Movie> bestLineup = new ArrayList<>(); // หนังของแถวนั้น (ยังไม่เจอ = ว่าง)
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            // 1. แยกวันที่ออกจากรายการหนัง "2026-10-09,M6;M9" → ["2026-10-09", "M6;M9"]
            String[] parts = line.split(",");
            if (parts.length != LINEUP_COLUMNS) {
                throw error(lineupFile, lineNumber, "Must have " + LINEUP_COLUMNS + " columns: date, movieIds");
            }
            // 2. แปลงข้อความเป็นวันที่ เพื่อเอาไปเทียบก่อน/หลังได้ "2026-10-09" → 9/10/2026
            //    "9/10/2026" แปลงไม่ได้ → error
            LocalDate lineDate;
            try {
                lineDate = LocalDate.parse(parts[LINEUP_DATE].trim());
            } catch (DateTimeParseException e) {
                throw error(lineupFile, lineNumber,
                        "Date must be in yyyy-MM-dd format e.g. 2026-10-09, but found " + parts[LINEUP_DATE].trim());
            }
            // 3. กันวันซ้ำ: ถ้ามี 9/10 สองแถว ระบบไม่รู้จะฉายชุดไหน
            //    seenDates.add(lineDate) คืน false ถ้าเคยเจอวันนี้แล้ว
            if (!seenDates.add(lineDate)) {
                throw error(lineupFile, lineNumber, "Date " + lineDate + " is duplicate of previous line");
            }
            // 4. แปลงรหัสหนังเป็นหนังจริง ทำทุกแถว เพื่อให้รหัสผิดแถวไหนก็รู้ทันที ไม่ต้องรอถึงวันนั้น
            List<Movie> lineup = parseLineupMovies(parts[LINEUP_MOVIES], lineNumber, movies);
            // 5. เอาชุดที่ admin ตั้งไว้ล่าสุด แต่ต้องไม่ใช่ชุดของวันข้างหน้า
            //    !lineDate.isAfter(date)       : แถวนี้ไม่เกินวันที่ถาม เช่น ถาม 8/10 แถว 9/10 → เกิน ข้าม
            //    lineDate.isAfter(bestDate)    : แถวนี้ใหม่กว่าที่เลือกไว้ เช่น แถว 9/10 ใหม่กว่า 1/10 → เปลี่ยนมาใช้แถวนี้
            //    bestDate == null              : ยังไม่ได้เลือกแถวไหนเลย แถวแรกที่ผ่านเงื่อนไขก็เลือกไว้ก่อน
            if (!lineDate.isAfter(date) && (bestDate == null || lineDate.isAfter(bestDate))) {
                bestDate = lineDate;
                bestLineup = lineup;
            }
        }
        return bestLineup;
    }

    /**
     * ให้ admin บันทึกว่าวันที่ date ฉายหนังเรื่องอะไร ถ้าวันเดิมบันทึกซ้ำ ให้เขียนทับของเดิม
     * ทำงาน: อ่านบรรทัดเดิม → เก็บทุกบรรทัดยกเว้นของวันเดียวกัน → ต่อท้ายบรรทัดใหม่ → เขียนกลับทั้งไฟล์
     * เช่น saveLineup(2026-10-09, [M6, M9, M10, M11, M15])
     *      → ไฟล์ได้บรรทัด "2026-10-09,M6;M9;M10;M11;M15" (วันอื่นเหมือนเดิม)
     *      บันทึก 9/10 อีกรอบ → บรรทัด 9/10 เก่าหายไป เหลือบรรทัดใหม่บรรทัดเดียว
     *
     * @param date   วันที่ฉาย ห้าม null
     * @param movies หนังเรียงตามลำดับ (ตัวแรก = เรื่องที่ 1) ห้าม null ห้ามว่าง
     * @throws IOException ถ้าอ่านหรือเขียนไฟล์ไม่ได้
     */
    @Override
    public void saveLineup(LocalDate date, List<Movie> movies) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (movies == null || movies.isEmpty()) {
            throw new IllegalArgumentException("movies must not be empty"); // ไม่มีหนังเลย = วันนั้นไม่มีรอบ ไม่ควรบันทึก
        }
        // เก็บแค่รหัสลงไฟล์ (ชื่อ/ความยาวอยู่ใน movies.csv แล้ว) [M6, M9] → ["M6", "M9"]
        List<String> ids = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie == null) {
                throw new IllegalArgumentException("movie must not be null");
            }
            ids.add(movie.id());
        }
        // ประกอบเป็น 1 บรรทัด  String.join(";", ids) : ต่อรหัสด้วย ; เช่น ["M6", "M9"] → "M6;M9"
        String newLine = date + "," + String.join(LINEUP_MOVIE_SEPARATOR, ids);

        List<String> kept = new ArrayList<>(); // บรรทัดเดิมที่จะเก็บไว้
        if (Files.exists(lineupFile)) {        // ยังไม่มีไฟล์ = ยังไม่มีบรรทัดเดิม ข้ามไปเขียนเลย
            for (String line : readLines(lineupFile)) {
                // บันทึกวันเดิมซ้ำ = เขียนทับ: ทิ้งบรรทัดเก่าของวันนั้น (และบรรทัดว่าง) เก็บวันอื่นไว้
                // ถ้าไม่ทิ้ง ไฟล์จะมีวันเดียวกัน 2 บรรทัด แล้ว findLineupOn จะ error "duplicate"
                //   เช่น บันทึก 2026-10-09 → บรรทัดที่ขึ้นต้นด้วย "2026-10-09," ถูกทิ้ง
                if (!line.isBlank() && !line.trim().startsWith(date + ",")) {
                    kept.add(line);
                }
            }
        }
        kept.add(newLine);

        Path folder = lineupFile.getParent();
        if (folder != null) {
            Files.createDirectories(folder); // ยังไม่มีโฟลเดอร์ data ก็สร้างให้ เพื่อไม่ให้เขียนไฟล์พัง
        }
        Files.write(lineupFile, kept, StandardCharsets.UTF_8);
    }

    // อ่านทุกบรรทัดในไฟล์ ใช้ร่วมกันทุกเมธอดเพื่อไม่ต้องเขียนโค้ดอ่านไฟล์ซ้ำ
    // ถ้าไม่มีไฟล์ โยน IOException บอก path เต็ม เพื่อให้รู้ว่าโปรแกรมไปหาไฟล์ที่ไหน
    private List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            // toAbsolutePath() แปลง path สั้นเป็น path เต็มตั้งแต่ไดรฟ์
            // data\movies.csv → D:\6821651329\project\Booking_tickets\data\movies.csv
            throw new IOException("File not found: " + file.toAbsolutePath());
        }
        // ["M1,Spider-Man: Brand New Day,145,posters/m1.jpg", "M2,Pacific Rim,131,posters/m2.jpg", ...]
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    // แปลง 1 บรรทัดของ movies.csv เป็น Movie เพื่อให้ส่วนอื่นใช้หนังเป็น object ได้ ไม่ต้องตัดข้อความเอง
    // เช่น "M2,Pacific Rim,131,posters/m2.jpg" → Movie(M2, Pacific Rim, 131, posters/m2.jpg)
    private Movie parseMovie(String line, int lineNumber) throws IOException {
        // 1. ตัดตรง , → ["M2", "Pacific Rim", "131", "posters/m2.jpg"]
        String[] parts = line.split(",");

        // 2. ต้องได้ครบ 4 ช่อง ถ้าขาด จะไม่รู้ว่าช่องไหนคืออะไร
        //    "M2,Pacific Rim,131"             → 3 ช่อง error
        //    "M2,Pacific Rim,131,"            → 3 ช่อง error (ลืมใส่โปสเตอร์ split ทิ้งช่องว่างท้ายให้)
        if (parts.length != MOVIE_COLUMNS) {
            throw error(moviesFile, lineNumber,
                    "Must have " + MOVIE_COLUMNS + " columns: id, title, durationMinutes, posterPath");
        }

        // 3. ความยาวในไฟล์เป็นข้อความ "131" ต้องแปลงเป็นตัวเลข 131 ก่อน เพราะ Movie เก็บเป็นตัวเลข
        //    trim() ตัดเว้นวรรคหน้าหลัง " 131 " → "131"
        //    "13O" (ตัวโอ) แปลงไม่ได้ → เปลี่ยนเป็น error ที่บอกไฟล์/บรรทัด
        int duration; // ประกาศนอก try เพราะต้องใช้ต่อหลัง try จบ
        try {
            duration = Integer.parseInt(parts[MOVIE_DURATION].trim());
        } catch (NumberFormatException e) {
            throw error(moviesFile, lineNumber, "Movie duration must be a number, but found " + parts[MOVIE_DURATION].trim());
        }

        // 4. สร้าง Movie กฎว่าค่าไหนผิด (รหัสว่าง, ชื่อว่าง, ความยาว <= 0) อยู่ใน Movie ที่เดียว
        //    ถ้าผิด Movie โยน IllegalArgumentException → เติมชื่อไฟล์กับเลขบรรทัด เพื่อให้รู้ว่าแก้ตรงไหน
        //    "M2,Pacific Rim,0,p" บรรทัด 2 → "movies.csv line 2: durationMinutes must be > 0"
        try {
            return new Movie(parts[MOVIE_ID].trim(), parts[MOVIE_TITLE].trim(), duration, parts[MOVIE_POSTER].trim());
        } catch (IllegalArgumentException e) {
            throw error(moviesFile, lineNumber, e.getMessage());
        }
    }

    // แปลงรายการรหัสหนังใน lineup.csv เป็นหนังจริง เพื่อให้ findShowtimesOn หยิบหนังตามลำดับไปสร้างรอบได้
    // เช่น "M6;M9;M10" → [Hello World, Inception, Interstellar]
    //      "M6;M99"    → ไม่มีหนังรหัส M99 → error (กัน admin ตั้งหนังที่ไม่มีอยู่จริง)
    private List<Movie> parseLineupMovies(String text, int lineNumber, List<Movie> movies) throws IOException {
        List<Movie> lineup = new ArrayList<>();
        // text.split(";") : ตัดตรง ; เช่น "M6;M9;M10" → ["M6", "M9", "M10"]
        for (String rawId : text.split(LINEUP_MOVIE_SEPARATOR)) {
            String id = rawId.trim();
            Movie movie = findMovie(movies, id);
            if (movie == null) {
                throw error(lineupFile, lineNumber, "No movie with ID " + id + " in " + moviesFile.getFileName());
            }
            lineup.add(movie);
        }
        return lineup;
    }

    // แปลง 1 บรรทัดของ schedule.csv เป็นรอบจริงของวันที่ date
    // เพื่อ: schedule.csv บอกแค่ "เรื่องที่ 1" ต้องเอาไปหยิบหนังจาก lineup ของวันนั้นถึงจะรู้ว่าฉายอะไร
    // เช่น "1,08:45,1" + date 2026-10-09 + lineup [M6, M9, ...] → Hello World โรง 1 วันที่ 9 เวลา 08:45
    private Showtime parseShowtime(String line, int lineNumber, LocalDate date, List<Movie> lineup)
            throws IOException {
        // 1. ตัดตรง , → ["1", "08:45", "1"]
        //    ถ้าเผลอมี , เกินท้าย "1,08:45,1," split ทิ้งช่องว่างท้ายให้ ยังได้ 3 ช่องเหมือนเดิม
        String[] parts = line.split(",");

        // 2. ต้องได้ครบ 3 ช่อง (โรง, เวลา, เรื่องที่) ไม่งั้นสร้างรอบไม่ได้  "1,08:45" → 2 ช่อง error
        if (parts.length != SCHEDULE_COLUMNS) {
            throw error(scheduleFile, lineNumber,
                    "Must have " + SCHEDULE_COLUMNS + " columns: hall, startTime, slot");
        }

        // 3. เลขโรงเป็นข้อความ ต้องแปลงเป็นตัวเลข "1" → 1  /  "A" แปลงไม่ได้ → error
        int hall; // ประกาศนอก try เพราะต้องใช้ตอนสร้าง Showtime ข้างล่าง
        try {
            hall = Integer.parseInt(parts[SCHEDULE_HALL].trim());
        } catch (NumberFormatException e) {
            throw error(scheduleFile, lineNumber, "Hall number must be a number, but found " + parts[SCHEDULE_HALL].trim());
        }

        // 4. เวลาเริ่มเป็นข้อความ ต้องแปลงเป็นเวลา เพื่อเทียบว่ารอบเริ่มหรือยัง "08:45" → 08:45
        //    "8:45" (ชั่วโมงหลักเดียว) แปลงไม่ได้ → error ต้องเขียน "08:45"
        LocalTime start; // ประกาศนอก try เหตุผลเดียวกับ hall
        try {
            start = LocalTime.parse(parts[SCHEDULE_START].trim());
        } catch (DateTimeParseException e) {
            throw error(scheduleFile, lineNumber,
                    "Start time must be in HH:mm format e.g. 09:30, but found " + parts[SCHEDULE_START].trim());
        }

        // 5. เอาเลข "เรื่องที่" ไปหยิบหนังจาก lineup ของวันนั้น นี่คือจุดที่ทำให้หนังเปลี่ยนตามวัน
        //    lineup มี 5 เรื่อง: เรื่องที่ 3 → lineup.get(2) (List นับจาก 0 จึงลบ 1)
        //    เรื่องที่ 6 หรือ 0 → ไม่มีในรายการ → error
        int slot;
        try {
            slot = Integer.parseInt(parts[SCHEDULE_SLOT].trim());
        } catch (NumberFormatException e) {
            throw error(scheduleFile, lineNumber, "Slot must be a number, but found " + parts[SCHEDULE_SLOT].trim());
        }
        if (slot < 1 || slot > lineup.size()) {
            throw error(scheduleFile, lineNumber,
                    "Slot must be 1-" + lineup.size() + " (number of movies in lineup), but found " + slot);
        }
        Movie movie = lineup.get(slot - 1);

        // 6. สร้าง Showtime กฎว่ารอบไหนผิด (โรง <= 0, ข้ามเที่ยงคืน) อยู่ใน Showtime ที่เดียว
        //    ถ้าผิด เติมชื่อไฟล์กับเลขบรรทัด เพื่อให้รู้ว่าแก้บรรทัดไหน
        //    "0,08:45,1"  → "hall must be > 0"
        //    "1,23:00,1"  → หนัง 145 นาทีจบเลยเที่ยงคืน → "showtime must end before midnight"
        try {
            return new Showtime(movie, hall, date, start);
        } catch (IllegalArgumentException e) {
            throw error(scheduleFile, lineNumber, e.getMessage());
        }
    }

    // หาหนังจากรหัส เพื่อแปลงรหัสในไฟล์ (เช่น "M2") เป็นหนังจริง
    // ไล่ทีละเรื่อง เจอคืนหนังเรื่องนั้นทันที วนหมดแล้วไม่เจอคืน null
    // movies = [M1, M2, M3]  findMovie(movies, "M2")  → คืนหนัง M2
    //                        findMovie(movies, "M99") → ไม่มี → คืน null
    // ใช้ 2 ที่: findAll() เช็กรหัสซ้ำ / parseLineupMovies() แปลงรหัสใน lineup
    private Movie findMovie(List<Movie> movies, String id) {
        for (Movie movie : movies) {
            if (movie.id().equals(id)) {
                return movie;
            }
        }
        return null;
    }

    // สร้าง error ที่บอกชื่อไฟล์และเลขบรรทัด เพื่อให้คนเปิดไฟล์ไปแก้ถูกบรรทัดทันที
    // error(moviesFile, 3, "Movie duration must be a number, but found 13O")
    //   → "movies.csv line 3: Movie duration must be a number, but found 13O"
    // หน้า GUI catch IOException แล้วเอา getMessage() ไปโชว์ในกล่องแจ้งเตือน
    private IOException error(Path file, int lineNumber, String reason) {
        return new IOException(file.getFileName() + " line " + lineNumber + ": " + reason);
    }

    // ตรวจ RI
    private void checkRep() {
        assert moviesFile != null;
        assert scheduleFile != null;
        assert lineupFile != null;
    }
}