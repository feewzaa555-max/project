package service;

import exception.AuthError;
import exception.AuthFormException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import model.User;
import repository.UserRepository;

/**
 * ตรวจการ Login / Sign up ตามกฎของระบบ
 * ไม่อ่านเขียนไฟล์เอง (ให้ UserRepository ทำ) และไม่ยุ่งกับหน้าจอ (SC5 SRP)
 */
public class AuthService {

    private static final int MIN_USERNAME_LENGTH = 5;
    private static final int MIN_PASSWORD_LENGTH = 5;

    // AF: ตัวตรวจ Login / Sign up ที่ใช้ users เป็นที่เก็บผู้ใช้
    // RI: users ไม่เป็น null
    // Safety from rep exposure: users เป็น private final และไม่มี method ไหนคืน users ออกไป
    private final UserRepository users;

    /**
     * @param users ที่เก็บผู้ใช้
     * @throws IllegalArgumentException ถ้า users เป็น null
     */
    public AuthService(UserRepository users) {
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        this.users = users;
        checkRep();
    }

    /**
     * ตรวจการ Login
     *
     * @param username ชื่อที่ผู้ใช้กรอก ห้าม null (ช่องว่างหน้า-หลังจะถูกตัดออก)
     * @param password รหัสที่ผู้ใช้กรอก ห้าม null
     * @return User ที่ Login สำเร็จ (ชื่อตามที่บันทึกไว้)
     * @throws AuthFormException ถ้าช่องว่าง ไม่พบชื่อ หรือรหัสไม่ถูกต้อง
     * @throws IOException ถ้าอ่านข้อมูลผู้ใช้ไม่ได้
     */
    public User login(String username, String password) throws AuthFormException, IOException {
        String name = username.trim(); 

        // 1) ช่องว่างไหม ตรวจทั้ง 2 ช่องพร้อมกัน
        List<AuthError> errors = new ArrayList<>(); //ไว้เก็บค่า errror ว่างจาก username และ password
        if (name.isEmpty()) {
            errors.add(AuthError.USERNAME_EMPTY);
        }
        if (password.isEmpty()) {
            errors.add(AuthError.PASSWORD_EMPTY);
        }
        if (!errors.isEmpty()) {
            throw new AuthFormException(errors); // ใส่ error ลง AuthFormException แล้วโยนไปให้หน้าจอ ถ้าไม่มี error ก็เช็กกรณีอื่นต่อ
        }

        // 2) เช็กว่ามี User นี้ในระบบไหม
        User user = users.findByUsername(name);
        if (user == null) {
            throw new AuthFormException(List.of(AuthError.USERNAME_NOT_FOUND)); // ไม่มีบอกว่าไม่พบ User นี้ตอน Login
        }

        // 3) รหัสตรงไหม
        if (!user.password().equals(password)) { // รหัสไม่ตรงก็ส่งไปว่ารหัสไม่ตรง
            throw new AuthFormException(List.of(AuthError.PASSWORD_WRONG));
        }
        return user; // ถ้าถูก throw จะไม่ return user
                    // ถ้า return user แปลว่า login สำเร็จ
    }

    /**
     * ตรวจการ Sign up แล้วบันทึกผู้ใช้ใหม่
     *
     * @param username ชื่อที่ผู้ใช้กรอก ห้าม null (ช่องว่างหน้า-หลังจะถูกตัดออก)
     * @param password รหัสที่ผู้ใช้กรอก ห้าม null
     * @return User ที่สมัครสำเร็จ
     * @throws AuthFormException ถ้าชื่อหรือรหัสผิดกฎ หรือชื่อนี้ถูกใช้แล้ว
     * @throws IOException ถ้าอ่านหรือบันทึกข้อมูลผู้ใช้ไม่ได้
     */
    public User register(String username, String password) throws AuthFormException, IOException {
        String name = username.trim(); //ตัดช่องว่างหน้าหลังออก

        // 1) รูปแบบถูกไหม ตรวจทั้ง 2 ช่องพร้อมกัน ช่องชื่อมีได้หลาย error
        List<AuthError> errors = new ArrayList<>(usernameFormatErrors(name)); //เอา name ไปเช็กใน usernameFormatErrors ว่ามี error อะไรต้องแจ้งไหม
        errors.addAll(passwordFormatErrors(password)); // เอา error ของรหัสมาต่อท้าย (ถ้าไม่มี error ก็ไม่ได้เพิ่มอะไร)
        if (!errors.isEmpty()) {
            throw new AuthFormException(errors); // ถ้าใน list errors มีข้อมูลที่ผิด -> ใส่ลง AuthFormException แล้วโยนไปให้หน้าจอ
        }

        // 2) ชื่อซ้ำไหม (ไม่สนตัวพิมพ์เล็ก/ใหญ่)
        if (users.findByUsername(name) != null) { //ถ้าชื่อที่ใช้ Sign Up เคยมีคนใช้ Sign Up ไปแล้วจะไม่สามารถใช้ชื่อนั้น Sign Up ได้อีก
            throw new AuthFormException(List.of(AuthError.USERNAME_TAKEN));
        }

        // 3) ผ่านหมด บันทึก
        User user = new User(name, password);
        users.save(user); //เขียนชื่อและรหัส ที่Sign Up แล้ว ลง csv
        return user; // ส่งไปให้ตัวที่เรียก 
    }

    // คืน error ทุกข้อที่เจอของช่องชื่อ ตามลำดับการตรวจ หรือ List ว่างถ้าผ่าน
    private List<AuthError> usernameFormatErrors(String name) {
        List<AuthError> errors = new ArrayList<>();
        if (name.isEmpty()) {
            errors.add(AuthError.USERNAME_EMPTY);
            return errors;   // ชื่อว่าง ข้ออื่นไม่มีความหมาย
        }
        if (name.length() < MIN_USERNAME_LENGTH) {
            errors.add(AuthError.USERNAME_TOO_SHORT);
        }
        if (!isEnglishLetter(name.charAt(0))) {
            errors.add(AuthError.USERNAME_FIRST_NOT_LETTER);
        }
        // เริ่มเช็กตั้งแต่ตัวที่ 2 เพราะตัวแรกเช็กไปแล้วข้างบน
        for (char c : name.substring(1).toCharArray()) { //ตัดตัวแรก ตัวแรกของ substring คือ 1 = index 0 แล้วเอาที่เหลือมาพิจารณา
            if (!isEnglishLetter(c) && !isDigit(c)) {
                errors.add(AuthError.USERNAME_INVALID_CHARS);
                break;   // เจอตัวแรกพอ ไม่งั้นจะใส่ error เดิมซ้ำ
            }
        }
        return errors;
    }

       // คืน error ทุกข้อที่เจอของช่องรหัส ตามลำดับการตรวจ หรือ List ว่างถ้าผ่าน
    private List<AuthError> passwordFormatErrors(String password) {
        List<AuthError> errors = new ArrayList<>();
        if (password.isEmpty()) {
            errors.add(AuthError.PASSWORD_EMPTY);
            return errors;   // รหัสว่าง ข้ออื่นไม่มีความหมาย
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add(AuthError.PASSWORD_TOO_SHORT);
        }
        for (char c : password.toCharArray()) {
            if (!isEnglishLetter(c) && !isDigit(c)) {
                errors.add(AuthError.PASSWORD_INVALID_CHARS);
                break;   // เจอตัวแรกพอ ไม่งั้นจะใส่ error เดิมซ้ำ
            }
        }
        return errors;
    }

    // เช็กช่วงตรงๆ เพราะ Character.isLetter ยอมรับภาษาไทยด้วย
    private static boolean isEnglishLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // เช็กช่วงตรงๆ เพราะ Character.isDigit ยอมรับเลขไทยด้วย
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // ตรวจ RI
    private void checkRep() {
        assert users != null;
    }
}