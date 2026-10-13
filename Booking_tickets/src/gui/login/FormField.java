//FormField <- ช่องกรอก 1 ชุดของหน้า login: ป้าย + ช่องพิมพ์ + ข้อความ error สีแดงใต้ช่อง
/*
    หน้าตา (เลขตรงกับ ①–③ ในโค้ดข้างล่าง):
                 Username                 ① ป้าย (สี LABEL)
        ┌──────────────────────────────┐
        │ admin                        │  ② ช่องพิมพ์ (ขอบสี FIELD_BORDER) หรือช่องรหัสที่โชว์เป็นจุด
        └──────────────────────────────┘
        • Please enter username            ③ ข้อความ error (สี ERROR) ซ่อนไว้จนกว่าจะมี error

    ทำไมต้องมี:
        LoginFrame เดิมเขียนชุดนี้ซ้ำ 2 ก้อน (Username บรรทัด 104–128, Password บรรทัด 132–158) ต่างกันแค่ชื่อป้าย
        ตอนนี้มีการ์ด 2 ใบ (Sign In, Sign Up) ใบละ 2 ช่อง = 4 ชุด ถ้าไม่แยกต้องเขียนซ้ำ 4 รอบ
        เขียนที่นี่ครั้งเดียว แล้ว new FormField(...) 4 ครั้ง (SC2 DRY)

    ใช้ยังไง (ใน LoginFrame แล้วส่งให้ AuthCard.addField วางลงการ์ด):
        FormField username = new FormField("Username", false);   // false = ช่องธรรมดา
        FormField password = new FormField("Password", true);    // true  = ช่องรหัส โชว์เป็นจุด
        username.text()                          → "admin"
        username.showErrors(List.of("Please enter username"))   → ข้อความแดงโผล่ใต้ช่อง
        username.clearError()                    → ข้อความแดงหาย
        password.onEnter(submitButton::doClick)  → กด Enter ในช่องนี้ = กดปุ่มหลัก (AuthCard.addField เรียกให้เอง)

    ไม่มี public: ใช้แค่ในโฟลเดอร์ login (AuthCard, LoginFrame)
 */

package gui.login;

import java.awt.*;                 // Dimension = ขนาด (กว้าง, สูง)
import java.util.List;             // รายการข้อความ error (ช่องเดียวอาจผิดหลายข้อ)
import javax.swing.*;              // JPanel, JLabel, JTextField, JPasswordField, BoxLayout, Box
import javax.swing.border.*;       // CompoundBorder, LineBorder, EmptyBorder = เส้นขอบ + ระยะในช่องพิมพ์

final class FormField extends JPanel {   // extends JPanel = ชุดนี้เป็นแผ่น 1 แผ่น เอาไปวางในการ์ดได้ทั้งชุด

    private final JTextField input;      // ② ช่องพิมพ์ (JPasswordField ก็เป็น JTextField ชนิดหนึ่ง เลยเก็บในตัวแปรเดียวกันได้)
    private final JLabel errorLabel;     // ③ ข้อความ error ใต้ช่อง

    /**
     * สร้างช่องกรอก 1 ชุด
     *
     * @param labelText ชื่อป้ายเหนือช่อง เช่น "Username" ห้าม null
     * @param password  true = ช่องรหัส (โชว์เป็นจุด), false = ช่องธรรมดา
     * @throws IllegalArgumentException ถ้า labelText เป็น null
     */
    FormField(String labelText, boolean password) {
        if (labelText == null) {
            throw new IllegalArgumentException("labelText must not be null");
        }
        // เรียงของในแผ่นนี้จากบนลงล่าง: ป้าย → ช่องพิมพ์ → ข้อความ error
        // ใส่ใน super(...) ไม่ได้ เพราะ BoxLayout ต้องรู้จัก this ซึ่งยังไม่เกิดตอนเรียก super
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);                       // ไม่ทาสีพื้นเอง ให้เห็นพื้นขาวของการ์ดข้างหลัง
        setAlignmentX(CENTER_ALIGNMENT);        // ทั้งชุดวางกลางการ์ด เหมือนชิ้นอื่นในการ์ด

        // ① ป้าย
        JLabel label = new JLabel(labelText);
        label.setFont(LoginTheme.FONT_LABEL);
        label.setForeground(LoginTheme.LABEL);
        label.setAlignmentX(CENTER_ALIGNMENT);  // ป้ายอยู่กลาง ตรงกับหัวข้อการ์ด

        // ② ช่องพิมพ์: password เป็น true → ช่องรหัส (พิมพ์แล้วโชว์เป็นจุด), false → ช่องธรรมดา
        if (password) {
            input = new JPasswordField();
        } else {
            input = new JTextField();
        }
        input.setFont(LoginTheme.FONT_INPUT);
        input.setPreferredSize(new Dimension(320, 36));          // กว้าง 320 สูง 36 (ค่าเดิม)
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); // ยืดกว้างเต็มการ์ดได้ แต่สูง 36 เสมอ
        input.setAlignmentX(CENTER_ALIGNMENT);   // ช่องพิมพ์อยู่กลาง (JTextField อยู่กลางเป็นค่าเริ่มต้นอยู่แล้ว บรรทัดนี้ลบได้ ภาพไม่เปลี่ยน)
        // ขอบ 2 ชั้น: CompoundBorder(ชั้นนอก, ชั้นใน)
        //   ชั้นนอก LineBorder(สี, หนา 1, มุมมน true) = เส้นขอบช่อง
        //   ชั้นใน EmptyBorder(บน 6, ซ้าย 10, ล่าง 6, ขวา 10) = ระยะระหว่างขอบกับตัวหนังสือ
        input.setBorder(new CompoundBorder(
                new LineBorder(LoginTheme.FIELD_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));

        // ③ ข้อความ error: เริ่มต้นว่างและซ่อนไว้ ไม่กินที่
        errorLabel = new JLabel("");
        errorLabel.setFont(LoginTheme.FONT_ERROR);
        errorLabel.setForeground(LoginTheme.ERROR);
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);   // JLabel ค่าเริ่มต้นชิดซ้าย ต้องสั่งให้วางกลางเหมือนชิ้นอื่นในแผ่น
        errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // กว้างเท่าช่องพิมพ์ สูงได้ถึง 60 (error หลายบรรทัด)
        errorLabel.setHorizontalAlignment(SwingConstants.LEFT);           // ตัวหนังสือชิดซ้าย ตรงกับขอบช่องพิมพ์
        errorLabel.setVisible(false);   // setVisible(false) = ซ่อน (เมธอดของ Swing ทุกชิ้นมี) ซ่อนแล้วไม่กินที่

        // วางลงแผ่นจากบนลงล่าง Box.createRigidArea(Dimension(0, สูง)) = ที่ว่างแนวตั้ง
        add(label);
        add(Box.createRigidArea(new Dimension(0, 6)));   // ป้าย ห่าง ช่องพิมพ์ 6 (ค่าเดิม)
        add(input);
        add(Box.createRigidArea(new Dimension(0, 3)));   // ช่องพิมพ์ ห่าง ข้อความ error 3 (ค่าเดิม)
        add(errorLabel);
    }

    /**
     * ค่าที่พิมพ์ในช่อง
     * ช่องรหัสต้องอ่านด้วย getPassword() (ได้เป็น char[]) แล้วแปลงเป็น String
     * เพราะ getText() ของช่องรหัส Java ไม่แนะนำให้ใช้ (deprecated)
     *
     * @return ข้อความในช่องตามที่พิมพ์ ไม่ตัดช่องว่าง (AuthService เป็นคนตัดเอง)
     */
    String text() {
        // input instanceof JPasswordField = เช็กว่าช่องนี้เป็นช่องรหัสไหม (สร้างเป็น JPasswordField ตอน password = true)
        // passwordInput = ตัวแปรใหม่ชนิด JPasswordField ที่ Java ประกาศให้ตรงนี้เลย ชี้ไปที่ช่องเดียวกับ input
        //   ต้องมีเพราะ input ประกาศเป็น JTextField ซึ่งไม่มีเมธอด getPassword()
        if (input instanceof JPasswordField passwordInput) {
            // getPassword() = เมธอดของ JPasswordField คืนตัวอักษรที่พิมพ์เป็น char[] เช่น ['a','b','c','1','2']
            // new String(...) = แปลง char[] เป็น String → "abc12"
            return new String(passwordInput.getPassword());
        }
        return input.getText();   // getText() = เมธอดของ JTextField คืนข้อความที่พิมพ์ เช่น "admin"
    }

    /**
     * โชว์ข้อความ error ใต้ช่อง ข้อละ 1 บรรทัด ขึ้นต้นด้วย •
     * เช่น ["Username must be at least 5 characters", "First character must be an English letter"]
     *   → • Username must be at least 5 characters
     *     • First character must be an English letter
     * JLabel ขึ้นบรรทัดใหม่ไม่ได้ เลยใช้ HTML: <html> ... <br> ... </html> (<br> = ขึ้นบรรทัดใหม่)
     *
     * @param messages ข้อความจาก AuthFormException.messagesFor(...) ว่างหรือ null = ไม่มี error → ซ่อน
     */
    void showErrors(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            clearError();
            return;
        }
        // ใช้ทำอะไร: เอา error หลายข้อมาต่อเป็นข้อความเดียว ให้ขึ้นใต้ช่องข้อละบรรทัด
        // ตัวอย่าง สมัครด้วยชื่อ 1ab → messages = ["Username must be at least 5 characters",
        //                                          "First character must be an English letter"]
        // ขั้น 1  String.join("<br>• ", messages) = เอาทุกข้อมาต่อกัน ใส่ "<br>• " คั่นระหว่างข้อ
        //         ได้ Username must be at least 5 characters<br>• First character must be an English letter
        // ขั้น 2  เติม "<html>• " ข้างหน้า กับ "</html>" ข้างหลัง
        //         ได้ <html>• Username must be at least 5 characters<br>• First character must be an English letter</html>
        //         (• ข้อแรกมาจาก "<html>• " ข้างหน้า, • ข้อสองมาจากตัวคั่น, <br> = ขึ้นบรรทัดใหม่
        //          ต้องมี <html> ครอบ เพราะ JLabel จะแปล <br> ก็ต่อเมื่อข้อความขึ้นต้นด้วย <html>)
        // ขั้น 3  setText ใส่ข้อความนี้ลงป้ายแดง → บนจอเห็น
        //           • Username must be at least 5 characters
        //           • First character must be an English letter
        // error ข้อเดียว เช่น ["Incorrect password"] → ไม่มีตัวคั่น ได้ <html>• Incorrect password</html> บรรทัดเดียว
        errorLabel.setText("<html>• " + String.join("<br>• ", messages) + "</html>");
        errorLabel.setVisible(true);   // ป้ายแดงซ่อนอยู่ตั้งแต่สร้าง (บรรทัด 85) → สั่งให้โผล่
    }

    /**
     * ซ่อนข้อความ error (เรียกก่อนกดปุ่มทุกครั้ง และตอนสลับการ์ด)
     */
    void clearError() {
        errorLabel.setText("");          // setText("") = ล้างข้อความในป้ายแดงให้ว่าง (เมธอดของ JLabel)
        errorLabel.setVisible(false);    // ซ่อนป้ายแดง ไม่กินที่
    }

    /**
     * ตั้งว่ากด Enter ในช่องนี้แล้วให้ทำอะไร
     * เช่น AuthCard.addField เรียก field.onEnter(submitButton::doClick) → กด Enter ในช่อง = กดปุ่มหลัก
     *
     * @param action คำสั่งที่จะทำตอนกด Enter ห้าม null
     * @throws IllegalArgumentException ถ้า action เป็น null
     */
    void onEnter(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        // input.addActionListener(...) = เมธอดของ JTextField (Swing มีให้)
        //   กฎของ Swing: "action" ของช่องพิมพ์ คือ การกด Enter ในช่องนั้น (ของปุ่ม JButton คือการคลิก)
        //   → ฝากโค้ดไว้ตรงนี้ = ผู้ใช้กด Enter ในช่องนี้เมื่อไหร่ โค้ดนี้ทำงาน (กดปุ่มอื่น เช่น a b c หรือ Tab ไม่ทำงาน)
        //   เทสกดคีย์จริงแล้ว: พิมพ์ abc → 0 ครั้ง, กด Tab → 0 ครั้ง, กด Enter → 1 ครั้ง, กด Enter อีก → 2 ครั้ง
        // e -> action.run() = โค้ดที่ฝากไว้ (lambda)
        //   e = ข้อมูลการกด Enter ที่ Java ส่งมาให้ (ไม่ได้ใช้)
        //   action.run() = ทำคำสั่งที่ส่งเข้ามาใน onEnter เช่น submitButton::doClick → กดปุ่มหลักแทนผู้ใช้
        input.addActionListener(e -> action.run());
    }
}