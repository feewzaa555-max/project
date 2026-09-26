//CsvMovieRepository <- อ่านหนังกับตารางฉายจากไฟล์ CSV (อ่านอย่างเดียว ไม่เขียนไฟล์)
//ตัวอย่างไฟล์ดูได้ที่ Booking_tickets/data/movies.csv และ Booking_tickets/data/schedule.csv

package repository;

import java.io.IOException;                      // error ตอนอ่านไฟล์ไม่ได้ หรือข้อมูลในไฟล์ผิด
import java.nio.charset.StandardCharsets;        // อ่านไฟล์แบบ UTF-8 ชื่อหนังภาษาไทยไม่เพี้ยน
import java.nio.file.Files;                      // คำสั่งเช็กว่ามีไฟล์ไหม และอ่านไฟล์ทุกบรรทัด
import java.nio.file.Path;                       // เก็บตำแหน่งไฟล์ เช่น data/movies.csv
import java.time.LocalDate;                      // วันที่ที่ส่งเข้ามาใน findShowtimesOn เช่น 2026-09-26
import java.time.LocalTime;                      // แปลงข้อความ "10:00" ในไฟล์ให้เป็นเวลา
import java.time.format.DateTimeParseException;  // error ตอนเวลาในไฟล์ผิดรูปแบบ เช่น "9:00"
import java.util.ArrayList;                      // สร้างรายการใหม่ไว้ใส่หนัง / รอบฉาย
import java.util.List;                           // ชนิดของรายการที่คืนออกไป
import model.Movie;                              // class หนัง อยู่คนละ package เลยต้อง import
import model.Showtime;                           // class รอบฉาย อยู่คนละ package เลยต้อง import

/**
 * อ่านหนังและตารางฉายจากไฟล์ CSV 2 ไฟล์ (ไม่มีบรรทัดหัวตาราง, บรรทัดว่างข้ามได้)
 * movies.csv   บรรทัดละ 1 เรื่อง: รหัส,ชื่อ,ความยาวนาที,รูปโปสเตอร์  เช่น M1,Spider-Man: Brand New Day,145,posters/m1.png
 * schedule.csv บรรทัดละ 1 รอบ:   โรง,เวลาเริ่ม,รหัสหนัง            เช่น 1,10:00,M1
 * ตารางฉายใช้ซ้ำทุกวัน findShowtimesOn(date) เอาวันที่มาประกอบเป็นรอบของวันนั้น
 * ข้อมูลในไฟล์ผิดจะโยน IOException ที่บอกชื่อไฟล์และเลขบรรทัด ไม่ข้ามเงียบ ๆ
 */
public class CsvMovieRepository implements MovieRepository {

    // AF: หนังทั้งหมดคือบรรทัดใน moviesFile, ตารางฉายประจำวันคือบรรทัดใน scheduleFile
    // RI: moviesFile และ scheduleFile ไม่เป็น null
    // Safety from rep exposure: ทั้งสอง field เป็น private final และ Path แก้ไขไม่ได้,
    //                           List ที่คืนออกไปสร้างใหม่ทุกครั้ง แก้แล้วไม่กระทบไฟล์

    // ลำดับช่องใน movies.csv (นับจาก 0)  M1 , Spider-Man , 145 , posters/m1.png
    //                                  0        1         2          3
    private static final int MOVIE_ID = 0;
    private static final int MOVIE_TITLE = 1;
    private static final int MOVIE_DURATION = 2;
    private static final int MOVIE_POSTER = 3;
    private static final int MOVIE_COLUMNS = 4;

    // ลำดับช่องใน schedule.csv (นับจาก 0)  1 , 10:00 , M1
    //                                    0     1     2
    private static final int SCHEDULE_HALL = 0;
    private static final int SCHEDULE_START = 1;
    private static final int SCHEDULE_MOVIE_ID = 2;
    private static final int SCHEDULE_COLUMNS = 3;

    private final Path moviesFile;   // เช่น data/movies.csv
    private final Path scheduleFile; // เช่น data/schedule.csv

    /**
     * เก็บแค่ตำแหน่งไฟล์ ยังไม่เปิดอ่านจนกว่าจะเรียก findAll() หรือ findShowtimesOn()
     *
     * @param moviesFile   ไฟล์รายชื่อหนัง เช่น data/movies.csv
     * @param scheduleFile ไฟล์ตารางฉาย เช่น data/schedule.csv
     * @throws IllegalArgumentException ถ้า moviesFile หรือ scheduleFile เป็น null
     */
    public CsvMovieRepository(Path moviesFile, Path scheduleFile) {
        if (moviesFile == null) {
            throw new IllegalArgumentException("moviesFile must not be null");
        }
        if (scheduleFile == null) {
            throw new IllegalArgumentException("scheduleFile must not be null");
        }
        this.moviesFile = moviesFile;
        this.scheduleFile = scheduleFile;
        checkRep();
    }

    /**
     * อ่านหนังทุกเรื่องจาก movies.csv
     * ทำงาน: สร้างรายการเปล่า → อ่านทีละบรรทัด (ข้ามบรรทัดว่าง) → แปลงเป็น Movie
     *        → รหัสซ้ำกับที่มีแล้วโยน error ไม่ซ้ำก็ add เข้ารายการ
     * เช่น ไฟล์มี M1, M2, M3 ได้ [หนัง M1, หนัง M2, หนัง M3]
     */
    @Override
    public List<Movie> findAll() throws IOException {
        List<String> lines = readLines(moviesFile);
        List<Movie> movies = new ArrayList<>(); // เริ่มจากรายการเปล่า []
        int lineNumber = 0;                     // นับทุกบรรทัดรวมบรรทัดว่าง ให้ตรงกับเลขบรรทัดในไฟล์
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue; // บรรทัดว่าง ข้ามไปบรรทัดถัดไป
            }
            Movie movie = parseMovie(line, lineNumber);
            // หาในรายการที่ใส่ไปแล้ว: เจอ = รหัสซ้ำ (error) / ไม่เจอ (null) = ยังไม่มี ใส่ได้
            // เช่น movies = [M1, M2] แล้วบรรทัดนี้เป็น M1 อีก → เจอ → error
            if (findMovie(movies, movie.id()) != null) {
                throw error(moviesFile, lineNumber, "รหัสหนัง " + movie.id() + " ซ้ำกับบรรทัดก่อนหน้า");
            }
            movies.add(movie);
        }
        return movies;
    }

    /**
     * อ่านรอบฉายทั้งหมดของวันที่ date จาก schedule.csv
     * ทำงาน: อ่านหนังทั้งหมดไว้ก่อน (ตารางฉายมีแค่รหัสหนัง ต้องใช้หาว่ารหัสนั้นคือเรื่องไหน)
     *        → อ่าน schedule.csv ทีละบรรทัด (ข้ามบรรทัดว่าง) → แปลงเป็น Showtime ของวันนั้น
     * เช่น date = 2026-09-26 บรรทัด 1,10:00,M1 ได้รอบโรง 1 เวลา 10:00 หนัง M1 วันที่ 26
     */
    @Override
    public List<Showtime> findShowtimesOn(LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null"); // ไม่รู้ว่าจะสร้างรอบของวันไหน
        }
        List<Movie> movies = findAll();                // หนังทั้งหมด ไว้หาว่า M1 คือเรื่องไหน
        List<String> lines = readLines(scheduleFile);
        List<Showtime> showtimes = new ArrayList<>();  // เริ่มจากรายการเปล่า []
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            showtimes.add(parseShowtime(line, lineNumber, date, movies));
        }
        return showtimes;
    }

    // อ่านทุกบรรทัดในไฟล์ ถ้าไม่มีไฟล์ โยน IOException บอกว่าไฟล์ไหนหาย
    // ไม่ต้องห่อ new ArrayList<>(...) แบบ CsvUserRepository เพราะที่นี่แค่วนอ่าน ไม่ได้ add เพิ่ม
    private List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            // toAbsolutePath() แปลง path สั้นเป็น path เต็มตั้งแต่ไดรฟ์
            // data\movies.csv → D:\6821651329\project\Booking_tickets\data\movies.csv
            // error จะบอกว่าโปรแกรมไปหาไฟล์ที่ไหน ถ้าหาผิดที่จะเห็นทันที
            throw new IOException("ไม่พบไฟล์ " + file.toAbsolutePath());
        }
        // ["M1,Spider-Man: Brand New Day,145,posters/m1.png", "M2,Pacific Rim,131,posters/m2.png", ...]
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    // แปลง 1 บรรทัดของ movies.csv เป็น Movie
    // เช่น "M2,Pacific Rim,131,posters/m2.png" → Movie(M2, Pacific Rim, 131, posters/m2.png)
    private Movie parseMovie(String line, int lineNumber) throws IOException {
        // 1. ตัดตรง , → ["M2", "Pacific Rim", "131", "posters/m2.png"]
        String[] parts = line.split(",");

        // 2. ต้องได้ครบ 4 ช่อง
        //    "M2,Pacific Rim,131"             → 3 ช่อง error
        //    "M2,Pacific Rim,131,"            → 3 ช่อง error (ลืมใส่โปสเตอร์ split ทิ้งช่องว่างท้ายให้)
        if (parts.length != MOVIE_COLUMNS) {
            throw error(moviesFile, lineNumber,
                    "ต้องมี " + MOVIE_COLUMNS + " ช่อง คือ รหัส,ชื่อ,ความยาวนาที,รูปโปสเตอร์");
        }

        // 3. ความยาวในไฟล์เป็นข้อความ "131" ต้องแปลงเป็นตัวเลข 131 ก่อนส่งให้ Movie
        //    trim() ตัดเว้นวรรคหน้าหลัง " 131 " → "131"
        //    "13O" (ตัวโอ) แปลงไม่ได้ → parseInt โยน NumberFormatException → เปลี่ยนเป็น error ที่บอกไฟล์/บรรทัด
        int duration; // ประกาศนอก try เพราะต้องใช้ต่อหลัง try จบ
        try {
            duration = Integer.parseInt(parts[MOVIE_DURATION].trim());
        } catch (NumberFormatException e) {
            throw error(moviesFile, lineNumber, "ความยาวหนังต้องเป็นตัวเลข แต่เจอ " + parts[MOVIE_DURATION].trim());
        }

        // 4. สร้าง Movie กฎว่าค่าไหนผิด (รหัสว่าง, ชื่อว่าง, ความยาว <= 0) อยู่ใน Movie ที่เดียว
        //    ถ้าผิด Movie โยน IllegalArgumentException พร้อมข้อความ เช่น "durationMinutes must be > 0"
        //    e.getMessage() ดึงข้อความนั้นออกมา แล้วเติมชื่อไฟล์กับเลขบรรทัด
        //    "M2,Pacific Rim,0,p" บรรทัด 2 → "movies.csv บรรทัด 2: durationMinutes must be > 0"
        try {
            return new Movie(parts[MOVIE_ID].trim(), parts[MOVIE_TITLE].trim(), duration, parts[MOVIE_POSTER].trim());
        } catch (IllegalArgumentException e) {
            throw error(moviesFile, lineNumber, e.getMessage());
        }
    }

    // แปลง 1 บรรทัดของ schedule.csv เป็น Showtime ของวันที่ date
    // เช่น "1,10:00,M1" + date 2026-09-26 → รอบโรง 1 วันที่ 26 เวลา 10:00 หนัง M1
    private Showtime parseShowtime(String line, int lineNumber, LocalDate date, List<Movie> movies)
            throws IOException {
        // 1. ตัดตรง , → ["1", "10:00", "M1"]
        //    ถ้าเผลอมี , เกินท้าย "1,10:00,M1," split ทิ้งช่องว่างท้ายให้ ยังได้ 3 ช่องเหมือนเดิม
        String[] parts = line.split(",");

        // 2. ต้องได้ครบ 3 ช่อง  "1,10:00" → 2 ช่อง error
        if (parts.length != SCHEDULE_COLUMNS) {
            throw error(scheduleFile, lineNumber,
                    "ต้องมี " + SCHEDULE_COLUMNS + " ช่อง คือ โรง,เวลาเริ่ม,รหัสหนัง");
        }

        // 3. เลขโรง "1" → 1   /  "A" แปลงไม่ได้ → error
        int hall; // ประกาศนอก try เพราะต้องใช้ตอนสร้าง Showtime ข้างล่าง
        try {
            hall = Integer.parseInt(parts[SCHEDULE_HALL].trim());
        } catch (NumberFormatException e) {
            throw error(scheduleFile, lineNumber, "เลขโรงต้องเป็นตัวเลข แต่เจอ " + parts[SCHEDULE_HALL].trim());
        }

        // 4. เวลาเริ่ม "10:00" → เวลา 10:00  /  "9:00" (ชั่วโมงหลักเดียว) แปลงไม่ได้ → error ต้องเขียน "09:00"
        LocalTime start; // ประกาศนอก try เหตุผลเดียวกับ hall
        try {
            start = LocalTime.parse(parts[SCHEDULE_START].trim());
        } catch (DateTimeParseException e) {
            throw error(scheduleFile, lineNumber,
                    "เวลาเริ่มต้องเป็นแบบ HH:mm เช่น 09:30 แต่เจอ " + parts[SCHEDULE_START].trim());
        }

        // 5. หาว่ารหัสหนังคือเรื่องไหน "M1" → Spider-Man: Brand New Day
        //    ตรงนี้ เจอ = ดี เอาไปสร้างรอบ / ไม่เจอ (null) = ไม่มีหนังรหัสนี้ เช่น "M9" → error
        String movieId = parts[SCHEDULE_MOVIE_ID].trim();
        Movie movie = findMovie(movies, movieId);
        if (movie == null) {
            throw error(scheduleFile, lineNumber, "ไม่มีหนังรหัส " + movieId + " ใน " + moviesFile.getFileName());
        }

        // 6. สร้าง Showtime กฎว่ารอบไหนผิด (โรง <= 0, ข้ามเที่ยงคืน) อยู่ใน Showtime ที่เดียว
        //    "0,10:00,M1"  → "hall must be > 0"
        //    "1,23:00,M1"  → หนัง 145 นาทีจบเลยเที่ยงคืน → "showtime must end before midnight"
        try {
            return new Showtime(movie, hall, date, start);
        } catch (IllegalArgumentException e) {
            throw error(scheduleFile, lineNumber, e.getMessage());
        }
    }

    // หาหนังจากรหัส ไล่ทีละเรื่อง เจอคืนหนังเรื่องนั้นทันที วนหมดแล้วไม่เจอคืน null
    // movies = [M1, M2, M3]  findMovie(movies, "M2") → รอบ 1 M1 ไม่ตรง, รอบ 2 M2 ตรง → คืนหนัง M2
    //                        findMovie(movies, "M9") → ไม่ตรงทั้ง 3 รอบ → คืน null
    // ใช้ 2 ที่: findAll() เช็กรหัสซ้ำ / parseShowtime() หาว่ารหัสในตารางฉายคือเรื่องไหน
    private Movie findMovie(List<Movie> movies, String id) {
        for (Movie movie : movies) {
            if (movie.id().equals(id)) {
                return movie;
            }
        }
        return null;
    }

    // สร้าง error ที่บอกชื่อไฟล์และเลขบรรทัด
    // error(moviesFile, 3, "ความยาวหนังต้องเป็นตัวเลข แต่เจอ 13O")
    //   → "movies.csv บรรทัด 3: ความยาวหนังต้องเป็นตัวเลข แต่เจอ 13O"
    // หน้า GUI catch IOException แล้วเอา getMessage() ไปโชว์ในกล่องแจ้งเตือน (แบบ LoginFrame)
    private IOException error(Path file, int lineNumber, String reason) {
        return new IOException(file.getFileName() + " บรรทัด " + lineNumber + ": " + reason);
    }

    // ตรวจ RI
    private void checkRep() {
        assert moviesFile != null;
        assert scheduleFile != null;
    }
}