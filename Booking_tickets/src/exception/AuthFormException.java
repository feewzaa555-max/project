package exception;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * error ที่ส่งกลับไปให้หน้าจอ ตอน Login / Sign up ไม่ผ่าน
 * 1 ช่องมี error ได้หลายตัว แต่ error ตัวเดียวกันห้ามซ้ำ
 * สร้างแล้วแก้ไม่ได้ (immutable)
 */
public final class AuthFormException extends Exception {

    // AF: errors แทนผลการตรวจฟอร์ม ช่อง k ผิดด้วยเหตุผลทุกตัวใน errors.get(k) ตามลำดับ
    //     ช่องที่ไม่มีใน errors คือช่องที่กรอกถูก
    // RI: errors ไม่ว่าง, ทุก List ใน errors ไม่ว่าง,
    //     error ทุกตัวใน errors.get(k) ต้องมี field() == k และห้ามซ้ำกันใน List เดียวกัน
    // Safety from rep exposure: errors และ List ข้างในเป็นของคลาสนี้คนเดียว
    //     constructor คัดลอกค่าจาก list มาใส่เอง ไม่ได้เก็บ list ไว้
    //     และ messagesFor คืน List ใหม่ทุกครั้ง ไม่ได้คืน List ข้างใน
    private final Map<AuthField, List<AuthError>> errors = new EnumMap<>(AuthField.class);

    /**
     * @param list error ที่เจอ อย่างน้อย 1 ตัว และ error ตัวเดียวกันห้ามซ้ำ
     * @throws IllegalArgumentException ถ้า list ว่าง หรือมี error ตัวเดียวกันซ้ำ
     */
    public AuthFormException(List<AuthError> list) {

        //[USERNAME_TOO_SHORT, USERNAME_INVALID_CHARS]
        if (list.isEmpty()) {
            throw new IllegalArgumentException("ต้องมี error อย่างน้อย 1 ตัว");//ผ่าน
        }
        for (AuthError error : list) {
            if (!errors.containsKey(error.field())) { //ใน field มี error หรือยัง
                errors.put(error.field(), new ArrayList<>());   // ยังไม่มี → สร้าง ArrayList ว่างขึ้นมาซึ่ง ArrayList แต่ละอันก็จะเก็บ Error ของ field ตัวเอง
            }
            //หยิบถัง list จาก field ตัวเองมาเช็กว่าซ้ำกับ error ไหม
            List<AuthError> fieldErrors = errors.get(error.field()); // error.field() ได้key errors.get(key) ได้ value ซึ่งก็คือ new ArrayList<>()
            //หยิบ list จาก field ตัวเองมาเช็กว่าซ้ำกับ error ไหม
            if (fieldErrors.contains(error)) { //เช็กว่า list ของ field นั้นมี error ซ้ำไหม
                throw new IllegalArgumentException(error + " ซ้ำ"); //ถ้าซ้ำถือว่าไม่ปกติ
            }
            fieldErrors.add(error); //นำ error ยัดใส่ใน list 
        }
        checkRep();
    }

    /**
     * @param field ช่องที่ต้องการถาม
     * @return ข้อความทุกข้อของช่องนี้ ตามลำดับการตรวจ หรือ List ว่างถ้าช่องนี้ไม่ผิด
     */
    public List<String> messagesFor(AuthField field) {
        List<String> messages = new ArrayList<>();
        List<AuthError> fieldErrors = errors.get(field);
        if (fieldErrors != null) {
            for (AuthError error : fieldErrors) {
                messages.add(error.message());
            }
        }
        return messages;
    }

    // ตรวจ RI
    private void checkRep() {
        assert !errors.isEmpty();
        for (AuthField field : errors.keySet()) {
            List<AuthError> fieldErrors = errors.get(field);
            assert !fieldErrors.isEmpty();
            for (AuthError error : fieldErrors) {
                assert error.field() == field;
            }
        }
    }
}