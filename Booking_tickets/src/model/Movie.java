package model;

/**
 * หนัง 1 เรื่อง
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5) equals/hashCode ให้เอง
 *
 * @param id              รหัสหนัง เช่น M1 ห้าม null ห้ามว่าง
 * @param title           ชื่อหนัง ห้าม null ห้ามว่าง
 * @param durationMinutes ความยาวหนังเป็นนาที ต้องอยู่ระหว่าง 1 ถึง MAX_DURATION_MINUTES (180)
 * @param posterPath      ที่อยู่รูปโปสเตอร์ เช่น posters/m1.png ห้าม null
 */
public record Movie(String id, String title, int durationMinutes, String posterPath) {

    /**
     * ความยาวหนังที่ยาวที่สุดที่ยอมให้มีได้ (นาที)
     * ทำไม 180: รอบใน schedule.csv ของโรงเดียวกันห่างกัน 3 ชั่วโมงพอดี (08:45, 11:45, 14:45, 17:45, 20:45)
     * ถ้าหนังยาวเกิน 180 นาที จะฉายทับรอบถัดไปในโรงเดียวกัน
     * เช่น 190 นาที รอบ 08:45 จบ 11:55 แต่รอบถัดไปเริ่ม 11:45
     * ถ้าวันหลังเปลี่ยนช่วงห่างของรอบใน schedule.csv ต้องแก้ตัวเลขนี้ตาม
     */
    public static final int MAX_DURATION_MINUTES = 180;

    // AF: หนัง 1 เรื่อง รหัส id ชื่อ title ยาว durationMinutes นาที รูปโปสเตอร์อยู่ที่ posterPath
    // RI: id และ title ไม่เป็น null และไม่ว่าง, 0 < durationMinutes <= MAX_DURATION_MINUTES, posterPath ไม่เป็น null
    // Safety from rep exposure: ข้างนอกแก้ค่าไม่ได้ เพราะ record เป็น private final และ String / int แก้ไขไม่ได้

    /** ตรวจค่าตอนสร้าง ข้อมูลเสียจาก movies.csv จะถูกจับได้ทันที (SC2 fail-fast) */
    public Movie {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        // ความยาวต้องมากกว่า 0 และไม่เกิน MAX_DURATION_MINUTES
        // เช่น 0 → error, 148 → ผ่าน, 180 → ผ่าน (จบตรงกับเวลาเริ่มรอบถัดไปพอดี), 181 → error (ทับรอบถัดไป)
        if (durationMinutes <= 0 || durationMinutes > MAX_DURATION_MINUTES) {
            throw new IllegalArgumentException(
                    "durationMinutes must be 1-" + MAX_DURATION_MINUTES + ", but was " + durationMinutes);
        }
        if (posterPath == null) {
            throw new IllegalArgumentException("posterPath must not be null");
        }
    }
}