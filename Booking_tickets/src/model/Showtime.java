//Showtime (รอบฉาย 1 รอบ)
//หน้าที่เก็บข้อมูลว่ารอบนี้ฉายหนังอะไร โรงไหน วันไหน กี่โมง และตอบคำถามง่ายๆ เกี่ยวกับรอบของตัวเองได้ เช่น จบกี่โมง หรือเริ่มไปแล้วหรือยัง


/*
field	ตัวอย่าง	ใช้ทำอะไร
movie	Movie       ของหนัง A	รู้ว่าฉายเรื่องอะไร (ได้ชื่อ ความยาว โปสเตอร์ไปด้วย)
hall	1	        โรงที่ฉาย
date	2026-09-26	วันที่ฉาย
start	10:00	    เวลาเริ่ม
 */

/*
method	                  ได้อะไร	                             ใครใช้
id()	             2026-09-26_H1_1000	                การจองอ้างถึงรอบนี้ด้วย id นี้ใน bookings.csv
end()	             12:10 (เริ่ม + ความยาวหนัง)	            เช็กตารางทับกัน และแสดงเวลาจบ
hasStarted(เวลาตอนนี้)	  true / false	                  หน้าจอใช้ทำปุ่มรอบเป็นสีเทา / ห้ามจอง

 */

package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * รอบฉาย 1 รอบ
 * เก็บว่ารอบนี้ฉายหนังอะไร โรงไหน วันไหน กี่โมง และตอบได้ว่าจบกี่โมง เริ่มไปแล้วหรือยัง
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ รอบที่มีคนจองแล้วจะไม่โดนเปลี่ยนเวลาทีหลัง
 *
 * @param movie หนังที่ฉาย ห้าม null
 * @param hall  เลขโรง ต้องมากกว่า 0
 * @param date  วันที่ฉาย ห้าม null
 * @param start เวลาเริ่ม ห้าม null
 */
public record Showtime(Movie movie, int hall, LocalDate date, LocalTime start) {

    // AF: รอบฉายหนัง movie ที่โรง hall วันที่ date เริ่ม start จบ start + ความยาวหนัง
    // RI: movie, date, start ไม่เป็น null, hall > 0, หนังต้องจบภายในวันเดียวกับที่เริ่ม (ไม่ข้ามเที่ยงคืน)
    // Safety from rep exposure: record เป็น private final และ Movie / LocalDate / LocalTime แก้ไขไม่ได้

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /** ตรวจค่าตอนสร้าง ข้อมูลเสียจาก schedule.csv จะถูกจับได้ทันที (SC2 fail-fast) */
    public Showtime {
        if (movie == null) {
            throw new IllegalArgumentException("movie must not be null");
        }
        if (hall <= 0) {
            throw new IllegalArgumentException("hall must be > 0");
        }
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (start == null) {
            throw new IllegalArgumentException("start must not be null");
        }
        int startMinute = start.getHour() * 60 + start.getMinute();
        if (startMinute + movie.durationMinutes() >= MINUTES_PER_DAY) {
            throw new IllegalArgumentException("showtime must end before midnight");
        }
    }

    /** รหัสรอบ เช่น 2026-09-26_H1_1000 ใช้อ้างถึงรอบนี้ใน bookings.csv */
    public String id() {
        return date + "_H" + hall + "_" + start.format(ID_TIME_FORMAT);
    }

    /** เวลาจบ = เวลาเริ่ม + ความยาวหนัง */
    public LocalTime end() {
        return start.plusMinutes(movie.durationMinutes());
    }

    /** true ถ้าถึงเวลาเริ่มแล้ว (หน้าจอใช้ทำปุ่มรอบเป็นสีเทา / ห้ามจอง) */
    public boolean hasStarted(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        return !now.isBefore(date.atTime(start));
    }
}