
//AuthFormException เก็บว่าช่องไหนผิดอะไรจากนั้นนำข้อความไปให้ Frame แสดง 

package exception;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * error ที่ส่งกลับไปให้หน้าจอ ตอน Login / Sign up ไม่ผ่าน
 * เก็บ error ได้ช่องละไม่เกิน 1 ตัว (ช่องชื่อ 1, ช่องรหัส 1)
 * สร้างแล้วแก้ไม่ได้ (immutable)
 */
public final class AuthFormException extends Exception {

    // AF: errors แทนผลการตรวจฟอร์ม ช่อง k ผิดด้วยเหตุผล errors.get(k)
    //     ช่องที่ไม่มีใน errors คือช่องที่กรอกถูก
    // RI: errors ไม่ว่าง และทุก key k ต้องตรงกับ errors.get(k).field()
    // Safety from rep exposure: errors เป็น private final ไม่มี method ไหนคืน errors ออกไป
    //     และ constructor คัดลอกค่าจาก list มาใส่ errors เอง ไม่ได้เก็บ list ไว้
    private final Map<AuthField, AuthError> errors = new EnumMap<>(AuthField.class);
    //(AuthField.class) <-errors อย่างรู้แค่ว่า key (AuthField) มีค่าอะไรบ้างและมีกี่ค่า เพื่อสร้างช่องให้พอดี ส่วน value

    /**
     * @param list error ที่เจอ อย่างน้อย 1 ตัว และห้ามมี 2 ตัวในช่องเดียวกัน
     * @throws IllegalArgumentException ถ้า list ว่าง หรือมี error 2 ตัวในช่องเดียวกัน
     */
    public AuthFormException(List<AuthError> list) {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("ต้องมี error อย่างน้อย 1 ตัว");
        }
        for (AuthError error : list) {
            if (errors.containsKey(error.field())) { //<- ช่อง Username และ Password มี error ได้แค่อย่างละ 1 ตัว
                throw new IllegalArgumentException("ช่อง " + error.field() + " มี error ซ้ำ"); // <- ถ้าก่อนหน้านี้มี error username แล้วแล้วมี error username มาอีกจะถือว่า่เป็น exception
            }
            errors.put(error.field(), error);
        }
        checkRep();
    }

//messageFor ถูกเรียกใน LoginFrame / SignUpFrame ตอนผู้ใช้กดปุ่มแล้วกรอกผิด ภายในบล็อก catch 

    /**
     * @param field ช่องที่ต้องการถาม
     * @return ข้อความที่จะขึ้นใต้ช่องนี้ หรือ "" ถ้าช่องนี้ไม่ผิด
     */
    public String messageFor(AuthField field) {
        AuthError error = errors.get(field); // errors.get(key) โดย Key คือ Authfield และ value = AuthError .get(key) เลยได้ value
        if (error == null) { //<- ไม่มี error ไม่แจ้งเตือนอะไร
            return "";
        } else { //<-มีผิดพิมพ์ข้อความออกไป
            return error.message();
        }
    }

    // ตรวจ RI
    private void checkRep() {
        assert !errors.isEmpty();
        for (AuthField field : errors.keySet()) {
            assert errors.get(field).field() == field;  //errors.get(field) = ได้ value แล้วนำ value มาหา field ตัวเอง (value.field()
        }
    }
}