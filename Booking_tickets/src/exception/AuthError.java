package exception;

/**
 * ข้อผิดพลาดทุกแบบของหน้า Login / Sign up
 * แต่ละตัวรู้ว่าต้องขึ้นใต้ช่องไหน (field) และขึ้นข้อความอะไร (message)
 */
public enum AuthError {
    // ----- ช่องชื่อ -----
    USERNAME_EMPTY(AuthField.USERNAME, "Please enter username"),
    USERNAME_TOO_SHORT(AuthField.USERNAME, "Username must be at least 5 characters"),
    USERNAME_FIRST_NOT_LETTER(AuthField.USERNAME, "First character must be an English letter"),
    USERNAME_INVALID_CHARS(AuthField.USERNAME, "Username can only contain English letters and numbers"),
    USERNAME_TAKEN(AuthField.USERNAME, "This username is already taken"),
    USERNAME_NOT_FOUND(AuthField.USERNAME, "Username not found, please sign up"),

    // ----- ช่องรหัส -----
    PASSWORD_EMPTY(AuthField.PASSWORD, "Please enter password"),
    PASSWORD_TOO_SHORT(AuthField.PASSWORD, "Password must be at least 5 characters"),
    PASSWORD_INVALID_CHARS(AuthField.PASSWORD, "Password can only contain English letters and numbers"),
    PASSWORD_WRONG(AuthField.PASSWORD, "Incorrect password");

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