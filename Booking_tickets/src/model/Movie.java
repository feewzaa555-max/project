package model;

/**
 * หนัง 1 เรื่อง
 * เป็น record จึง immutable สร้างแล้วแก้ไม่ได้ (SC5) equals/hashCode ให้เอง
 *
 * @param id              รหัสหนัง เช่น M1 ห้าม null ห้ามว่าง
 * @param title           ชื่อหนัง ห้าม null ห้ามว่าง
 * @param durationMinutes ความยาวหนังเป็นนาที ต้องมากกว่า 0
 * @param posterPath      ที่อยู่รูปโปสเตอร์ เช่น posters/m1.png ห้าม null
 */
public record Movie(String id, String title, int durationMinutes, String posterPath) {

    // AF: หนัง 1 เรื่อง รหัส id ชื่อ title ยาว durationMinutes นาที รูปโปสเตอร์อยู่ที่ posterPath
    // RI: id และ title ไม่เป็น null และไม่ว่าง, durationMinutes > 0, posterPath ไม่เป็น null
    // Safety from rep exposure: ข้างนอกแก้ค่าไม่ได้ เพราะ record เป็น private final และ String / int แก้ไขไม่ได้

    /** ตรวจค่าตอนสร้าง ข้อมูลเสียจาก movies.csv จะถูกจับได้ทันที (SC2 fail-fast) */
    public Movie {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be > 0");
        }
        if (posterPath == null) {
            throw new IllegalArgumentException("posterPath must not be null");
        }
    }
}