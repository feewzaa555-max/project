package exception;

/**
 * ข้อผิดพลาดทุกแบบของหน้า Login / Sign up
 * แต่ละตัวรู้ว่าต้องขึ้นใต้ช่องไหน (field) และขึ้นข้อความอะไร (message)
 */
public enum AuthError {
    // ----- ช่องชื่อ -----
    USERNAME_EMPTY(AuthField.USERNAME, "กรุณาใส่ชื่อ"),
    USERNAME_TOO_SHORT(AuthField.USERNAME, "ชื่อควรยาวอย่างน้อย 5 ตัวอักษร"),
    USERNAME_FIRST_NOT_LETTER(AuthField.USERNAME, "ตัวอักษรตัวแรกต้องเป็นภาษาอังกฤษ"),
    USERNAME_INVALID_CHARS(AuthField.USERNAME, "ชื่อใช้ได้เฉพาะภาษาอังกฤษและตัวเลข"),
    USERNAME_TAKEN(AuthField.USERNAME, "ชื่อนี้ถูกใช้แล้ว"),
    USERNAME_NOT_FOUND(AuthField.USERNAME, "ไม่พบชื่อผู้ใช้นี้ กรุณา Sign up"),

    // ----- ช่องรหัส -----
    PASSWORD_EMPTY(AuthField.PASSWORD, "กรุณาใส่รหัส"),
    PASSWORD_TOO_SHORT(AuthField.PASSWORD, "รหัสควรยาวอย่างน้อย 5 ตัวอักษร"),
    PASSWORD_INVALID_CHARS(AuthField.PASSWORD, "รหัสใช้ได้เฉพาะภาษาอังกฤษและตัวเลข"),
    PASSWORD_WRONG(AuthField.PASSWORD, "รหัสผ่านไม่ถูกต้อง");

    private final AuthField field;   // เอาไว้เช็กว่าจะขึ้นเตือนข้อความใต้ช่องไหน-> username หรือ password 
    private final String message;    // ข้อความที่แสดงบนหน้าจอ

    AuthError(AuthField field, String message) {
        this.field = field;
        this.message = message;
    }

    public AuthField field() {
        return field;
    }

    public String message() {
        return message;
    }
}