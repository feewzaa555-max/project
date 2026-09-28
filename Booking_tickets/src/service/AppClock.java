//เป็นนาฬิกาตัวเดียวของทั้งโปรแกรม ทุกส่วนที่อยากรู้ว่า "ตอนนี้กี่โมง / วันนี้วันไหน" ต้องถามจากตัวนี้
/*
    ทำไมต้องมี (จำเป็นจริง):
    ปุ่มวันที่: ปุ่ม วันนี้ 26/9, 27/9, 28/9 ต้องรู้ว่าวันนี้วันไหน
    ปุ่มรอบที่เลยเวลาเป็นสีเทา: ต้องรู้ว่าตอนนี้กี่โมง
    ปรับเวลาได้ตอนสาธิต: เช่น ตั้งเป็น 15:00 เพื่อโชว์ว่ารอบเช้ากลายเป็นสีเทา ไม่ต้องรอถึงเวลาจริง แล้วกด Reset เวลากลับเป็นเวลาจริง 
    ถ้าไม่มี: แต่ละที่จะถามเวลาจากเครื่องเอง (LocalDateTime.now()) ปรับเวลาตอนสาธิตไม่ได้ และผลทดสอบเปลี่ยนไปตามเวลาจริงที่รัน
 */

/*
function
    now() เวลาตอนนี้
    setTo(เวลา) ตั้งเวลาเอง ไว้สาธิต
    reset() กลับเป็นเวลาจริง
 */

package service;

import java.time.Clock;          // นาฬิกา: ของเครื่อง (โปรแกรมจริง) หรือแบบเข็มค้าง (ไฟล์ทดสอบ)
import java.time.Duration;       // ระยะเวลาที่เลื่อนไปจากเวลาจริง เช่น ถอยหลัง 3 ชั่วโมง
import java.time.LocalDateTime;  // วันที่ + เวลา เช่น 2026-09-27 15:00
import java.time.ZoneId;         // โซนเวลาของเครื่อง (ไทย) ใช้ตอนสร้างนาฬิกาสมมุติ

/**
 * นาฬิกาตัวเดียวของทั้งโปรแกรม
 * ปกติเดินตามเวลาจริงของเครื่อง ตั้งเวลาเองได้ไว้สาธิต (setTo) และกลับเป็นเวลาจริงได้ (reset)
 * ตั้งเวลาแล้วเวลายังเดินต่อ เช่น ตั้ง 15:00 ผ่านไปจริง 5 นาที now() ได้ 15:05
 */
public class AppClock {

    // AF: เวลาตอนนี้ของโปรแกรม = เวลาจริงจาก realClock + offset
    //     offset เป็นศูนย์ = ตรงกับเวลาจริง / ติดลบ = ถอยหลัง / เป็นบวก = เดินหน้า
    // RI: realClock และ offset ไม่เป็น null
    // Safety from rep exposure: field เป็น private, Clock / Duration / LocalDateTime แก้ไขไม่ได้
    // Thread safety: ทุกเมธอดที่อ่านหรือแก้ offset เป็น synchronized (SC7)
    //                เพราะนาฬิกาตัวนี้ทุกส่วนของโปรแกรมใช้ร่วมกัน และ setTo / reset แก้ค่าได้

        //ตอนนี้โปรแกรมยังไม่มี thread เบื้องหลัง ทุกอย่างรันบนหน้าจอ thread 
        // เดียว ถ้าไม่ใส่ก็ยังไม่พัง แต่ AppClock เป็นตัวที่ทุกส่วนของโปรแกรมใช้ร่วมกันและแก้ค่าได้ ตรงกับสูตรอันตรายของ 
        // SC7 หน้า 9 (ใช้ร่วมกัน + แก้ค่าได้ + ใช้พร้อมกัน) ใส่ไว้ตอนนี้ไม่เสียอะไร ถ้าวันหลังมี thread เบื้องหลังจะไม่ต้องย้อนมาแก้ 
    

    private final Clock realClock;           // เวลาจริง
    private Duration offset = Duration.ZERO; // เลื่อนไปจากเวลาจริงเท่าไหร่ เริ่มต้นไม่เลื่อน

    /**
     * ใช้เวลาจริงของเครื่อง ตอนรันโปรแกรมจริงใช้ตัวนี้
     * เช่น new AppClock() แล้วเรียก now() ตอน 18:30 ได้ 18:30
     */
    public AppClock() {
        // นาฬิกาจริงของเครื่องคอม ใช้โซนเวลาที่เครื่องตั้งไว้ (ไทย) ไม่ต้องใส่พารามิเตอร์
        this.realClock = Clock.systemDefaultZone();
        checkRep();
    }

    /**
     * ใช้ในไฟล์ทดสอบ: สมมุติว่าเวลาจริงค้างอยู่ที่ fixedNow (ไม่เดิน)
     * เช่น new AppClock(LocalDateTime.of(2026, 9, 27, 18, 0)) → now() ได้ 18:00 / reset() แล้วก็ได้ 18:00
     *
     * @param fixedNow เวลาที่สมมุติว่าเป็นเวลาจริง ห้าม null
     * @throws IllegalArgumentException ถ้า fixedNow เป็น null
     */
    public AppClock(LocalDateTime fixedNow) {
        if (fixedNow == null) {
            throw new IllegalArgumentException("fixedNow must not be null");
        }
        // โซนเวลาของเครื่อง เครื่องในไทยได้ Asia/Bangkok
        ZoneId zone = ZoneId.systemDefault();

        // สร้างนาฬิกาที่ตอบเวลา fixedNow ตลอด ไม่เดิน เช่น fixedNow = 27/9 18:00 → ถามกี่ครั้งก็ได้ 18:00
        //   fixedNow.atZone(zone)  : ใส่โซนให้เวลา  27/9 18:00 → 27/9 18:00 เวลาไทย
        //   .toInstant()           : แปลงเป็นจุดเวลาแบบที่ Clock.fixed รับ
        //   Clock.fixed(จุดเวลา, zone)
        //       พารามิเตอร์ 1 = จุดเวลาที่ให้เข็มค้าง (27/9 18:00 เวลาไทย)
        //       พารามิเตอร์ 2 = โซนเวลาของนาฬิกา (Asia/Bangkok) เวลาอ่านออกมาจะได้เป็นเวลาไทย


        /*
            fixedNow เป็น LocalDateTime เช่น 2026-09-28T14:00 ยังไม่รู้ว่าเป็นเวลาของประเทศไหน
            .atZone(zone) แปะ timezone ให้ เช่น Asia/Bangkok จะได้รู้ว่าเป็น 14:00 ที่ไทย
        .   toInstant() แปลงเป็นจุดเวลาจริงบนโลก (เวลาสากล) ขั้นนี้ต้องทำเพราะ Clock.fixed รับแค่แบบนี้
            Clock.fixed(..., zone) สร้างนาฬิกาที่ถามกี่ครั้งก็ตอบเวลาเดิมตลอด
         */
        this.realClock = Clock.fixed(fixedNow.atZone(zone).toInstant(), zone);
        checkRep();
    }

    /**
     * เวลาตอนนี้ของโปรแกรม
     * เช่น ยังไม่เคย setTo → ได้เวลาจริง 18:00
     *      เวลาจริง 18:00 เคย setTo(15:00) → offset = -3 ชั่วโมง → ได้ 15:00
     *      ผ่านไปจริงอีก 5 นาที (เวลาจริง 18:05) → ได้ 15:05
     *
     * @return เวลาจริง + offset
     */
    public synchronized LocalDateTime now() {
        // LocalDateTime.now(realClock) : อ่านเวลาจากนาฬิกา realClock ตอนนี้ เช่น 18:05
        //     พารามิเตอร์ = นาฬิกาที่จะอ่าน (ของเครื่อง หรือแบบเข็มค้าง)
        // .plus(offset)                : บวกระยะที่เลื่อนไว้ เช่น 18:05 + (-3 ชั่วโมง) = 15:05
        //     พารามิเตอร์ = ระยะเวลาที่จะบวก (ติดลบได้ = ถอยหลัง)
        return LocalDateTime.now(realClock).plus(offset);
    }

    /**
     * ตั้งเวลาของโปรแกรมเป็น time ไว้สาธิต หลังจากนี้เวลาเดินต่อจาก time
     * ทำงาน: offset = time - เวลาจริงตอนนี้
     * เช่น เวลาจริง 18:00 setTo(วันนี้ 15:00) → offset = -3 ชั่วโมง
     *      เวลาจริง 18:00 setTo(พรุ่งนี้ 10:00) → offset = +16 ชั่วโมง
     *
     * @param time เวลาที่ต้องการให้เป็นตอนนี้ ห้าม null
     * @throws IllegalArgumentException ถ้า time เป็น null
     */
    public synchronized void setTo(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("time must not be null");
        }
        // Duration.between(A, B) : หาว่าจาก A ไป B ห่างกันเท่าไหร่ (B - A)
        //     พารามิเตอร์ 1 (A) = เวลาจริงตอนนี้ เช่น 18:00
        //     พารามิเตอร์ 2 (B) = เวลาที่อยากตั้ง เช่น 15:00
        //     ได้ 15:00 - 18:00 = -3 ชั่วโมง เก็บลง offset
        offset = Duration.between(LocalDateTime.now(realClock), time);
        checkRep();
    }

    /**
     * กลับเป็นเวลาจริง (ปุ่ม Reset ใช้) ทำงาน: offset = 0
     * เช่น เคย setTo(15:00) ไว้ เวลาจริง 18:00 → reset() → now() ได้ 18:00
     */
    public synchronized void reset() {
        offset = Duration.ZERO;
        checkRep();
    }

    // ตรวจ RI
    private void checkRep() {
        assert realClock != null;
        assert offset != null;
    }
}