//AuthCard <- การ์ด 1 ใบของหน้า login (ใช้ทั้งใบ Sign In และใบ Sign Up) หัวข้อ + ช่องกรอก + ปุ่ม + ลิงก์สลับหน้า
/*
    หน้าตา (เลขตรงกับ ①–⑤ ในโค้ดข้างล่าง):
        ┌──────────────────────────────────┐   การ์ดพื้นขาว เส้นขอบเทา
        │             Sign In              │   ① หัวข้อ
        │   Welcome to KU CINEMA           │   ② คำต้อนรับ
        │   Username                       │
        │   [ admin                     ]  │   ③ ช่องกรอก (FormField ที่ส่งมาทาง addField เรียงตามลำดับที่สั่ง)
        │   Password                       │
        │   [ •••••                     ]  │
        │   [         Sign In           ]  │   ④ ปุ่มหลัก (สีส่งมาตอน new)
        │  Don't have an account? Sign Up  │   ⑤ คำถาม + ปุ่มลิงก์
        └──────────────────────────────────┘

    ทำไมต้องมี:
        การ์ด Sign In กับ Sign Up หน้าตาเหมือนกันทั้งใบ ต่างกันแค่ข้อความ สีปุ่ม และกดแล้วทำอะไร
        เขียนการ์ดที่นี่ครั้งเดียว แล้ว new AuthCard(...) 2 ครั้ง ไม่ต้องเขียนซ้ำ 2 ไฟล์ (SC2 DRY)

    ยืดหยุ่นยังไง:
        - อยากได้ช่องเพิ่ม (เช่น Confirm Password) → addField อีกบรรทัดเดียว ไม่ต้องแก้ไฟล์นี้
        - อยากได้หน้าใหม่ (เช่น ลืมรหัสผ่าน) → new AuthCard(...) อีกใบด้วยข้อความของหน้านั้น
        - การ์ดไม่รู้จัก login / register เลย แค่เรียกคำสั่งที่ฝากไว้ตอนกดปุ่ม / ลิงก์
          (แบบเดียวกับ onLogout ของ AdminTopBar) คนใช้ (LoginFrame) เป็นคนบอกว่ากดแล้วทำอะไร

    ใช้ยังไง (ใน LoginFrame):
        FormField username = new FormField("Username", false);
        FormField password = new FormField("Password", true);
        AuthCard signIn = new AuthCard("Sign In", "Welcome to KU CINEMA", "Sign In",
                                       LoginTheme.PRIMARY, "Don't have an account?", "Sign Up");
        signIn.addField(username);          // วางช่อง Username ลงการ์ด
        signIn.addField(password);          // วางช่อง Password ต่อข้างล่าง
        signIn.onSubmit(this::login);       // กดปุ่ม Sign In (หรือกด Enter ในช่องไหนก็ได้) → เรียก login()
        signIn.onLink(this::showSignUp);    // กดคำว่า Sign Up → เรียก showSignUp()

    ไม่มี public: ใช้แค่ในโฟลเดอร์ login (LoginFrame)
 */

package gui.login;

import java.awt.*;              // Color = สีปุ่ม, Dimension = ขนาด, Cursor = รูปมือตอนชี้ปุ่ม, FlowLayout = เรียงคำถามกับลิงก์ในแถวเดียว
import javax.swing.*;           // JPanel, JLabel, JButton, BoxLayout, Box
import javax.swing.border.*;    // CompoundBorder, LineBorder, EmptyBorder = เส้นขอบการ์ด + ระยะข้างใน

final class AuthCard extends JPanel {   // extends JPanel = การ์ดเป็นแผ่น 1 แผ่น LoginFrame เอาไปวางกลางหน้าต่างได้ทั้งใบ

    private final JPanel fields;          // ③ แผ่นที่วางช่องกรอก addField จะวางช่องเรียงลงในแผ่นนี้
    private final JButton submitButton;   // ④ ปุ่มหลัก (Sign In / Create Account)
    private final JButton linkButton;     // ⑤ ปุ่มลิงก์ (Sign Up / Sign In) ใช้สลับไปอีกการ์ด

    /**
     * สร้างการ์ด 1 ใบ (ยังไม่มีช่องกรอก ต้อง addField ต่อ)
     *
     * @param title       ① หัวข้อ เช่น "Sign In"
     * @param subtitle    ② คำต้อนรับใต้หัวข้อ เช่น "Welcome to KU CINEMA"
     * @param buttonText  ④ ข้อความบนปุ่มหลัก เช่น "Sign In"
     * @param buttonColor ④ สีปุ่มหลัก และ ⑤ สีคำลิงก์ เช่น LoginTheme.PRIMARY (น้ำเงิน)
     * @param question    ⑤ คำถามหน้าลิงก์ เช่น "Don't have an account?"
     * @param linkText    ⑤ คำบนปุ่มลิงก์ เช่น "Sign Up"
     * @throws IllegalArgumentException ถ้าค่าไหนเป็น null
     */
    AuthCard(String title, String subtitle, String buttonText, Color buttonColor,
             String question, String linkText) {
        // ลืมส่งค่าไหนมา → หยุดทันทีตอนสร้าง ดีกว่าไปเจอการ์ดขึ้นว่างเปล่าตอนรัน
        if (title == null || subtitle == null || buttonText == null || buttonColor == null
                || question == null || linkText == null) {
            throw new IllegalArgumentException("AuthCard arguments must not be null");
        }

        // การ์ด: เรียงของจากบนลงล่าง พื้นขาว
        // ใส่ใน super(...) ไม่ได้ เพราะ BoxLayout ต้องรู้จัก this ซึ่งยังไม่เกิดตอนเรียก super
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(LoginTheme.CARD);
        // ขอบ 2 ชั้น: ชั้นนอก LineBorder(สี, หนา 1, มุมมน true) = เส้นขอบการ์ด
        //            ชั้นใน EmptyBorder(บน, ซ้าย, ล่าง, ขวา = 25) = ระยะจากเส้นขอบถึงของข้างใน (ค่าเดิม)
        setBorder(new CompoundBorder(
                new LineBorder(LoginTheme.BORDER, 1, true),
                new EmptyBorder(25, 25, 25, 25)));

        // ① หัวข้อ
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(LoginTheme.FONT_HEADER);         // Tahoma หนา 20
        titleLabel.setForeground(LoginTheme.TEXT_DARK);     // ดำเกือบสนิท
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);         // อยู่กลางการ์ด

        // ② คำต้อนรับ
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(LoginTheme.FONT_SUBTITLE);    // Tahoma ธรรมดา 12
        subtitleLabel.setForeground(LoginTheme.TEXT_MUTED); // เทา
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        // ③ แผ่นวางช่องกรอก: ตอนนี้ยังว่าง addField จะวางช่องลงมาทีหลัง
        fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));   // ช่องเรียงจากบนลงล่าง
        fields.setOpaque(false);                                     // ไม่ทาสีพื้นเอง ให้เห็นพื้นขาวของการ์ด
        fields.setAlignmentX(CENTER_ALIGNMENT);

        // ④ ปุ่มหลัก
        submitButton = new JButton(buttonText);
        submitButton.setFont(LoginTheme.FONT_BUTTON);           // Tahoma หนา 14
        submitButton.setBackground(buttonColor);                // สีที่ส่งมา เช่น น้ำเงิน (Sign In) / เขียว (Sign Up)
        submitButton.setForeground(LoginTheme.BUTTON_TEXT);     // ตัวหนังสือขาว
        submitButton.setFocusPainted(false);                    // กดแล้วไม่วาดกรอบประรอบคำ
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // ชี้แล้วเป็นรูปมือ บอกว่ากดได้
        submitButton.setPreferredSize(new Dimension(320, 40));  // ขนาดที่ขอ: กว้าง 320 สูง 40 (ค่าเดิม)
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // ยืดกว้างเต็มการ์ด แต่สูง 40 เสมอ
        submitButton.setAlignmentX(CENTER_ALIGNMENT);
        submitButton.setBorder(new EmptyBorder(8, 16, 8, 16));  // ระยะจากขอบปุ่มถึงตัวหนังสือ (ค่าเดิม)

        // ⑤ คำถาม + ปุ่มลิงก์ อยู่แถวเดียวกัน
        JLabel questionLabel = new JLabel(question);
        questionLabel.setFont(LoginTheme.FONT_SUBTITLE);   // Tahoma ธรรมดา 12
        questionLabel.setForeground(LoginTheme.LINK_TEXT); // เทาเข้ม

        linkButton = new JButton(linkText);
        linkButton.setFont(LoginTheme.FONT_LINK);              // Tahoma หนา 12
        linkButton.setForeground(buttonColor);                 // สีเดียวกับปุ่มหลัก (ค่าเดิม: Sign Up น้ำเงิน / Sign In เขียว)
        linkButton.setContentAreaFilled(false);                // ไม่ทาสีพื้นปุ่ม → ดูเป็นคำธรรมดา
        linkButton.setBorderPainted(false);                    // ไม่วาดเส้นขอบปุ่ม
        linkButton.setFocusPainted(false);
        linkButton.setBorder(new EmptyBorder(0, 0, 0, 0));     // ไม่มีระยะรอบคำ → คำลิงก์ชิดคำถาม
        linkButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // FlowLayout(กลาง, ห่างกันแนวนอน 4, ห่างแนวตั้ง 0) = วางคำถามกับลิงก์ต่อกันในแถวเดียว แล้วจัดไว้กลาง
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        linkRow.setAlignmentX(CENTER_ALIGNMENT);
        linkRow.add(questionLabel);
        linkRow.add(linkButton);

        // วางทุกชิ้นลงการ์ดจากบนลงล่าง ระยะห่างเป็นค่าเดิมจาก LoginFrame (บรรทัด 99, 101, 159, 174)
        // Box.createRigidArea(new Dimension(0, N)) = ของว่าง ๆ กว้าง 0 สูง N มองไม่เห็น ใช้เว้นระยะระหว่างชิ้น
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 4)));    // หัวข้อ ↔ คำต้อนรับ ห่าง 4
        add(subtitleLabel);
        add(Box.createRigidArea(new Dimension(0, 24)));   // คำต้อนรับ ↔ ช่องแรก ห่าง 24
        add(fields);
        add(Box.createRigidArea(new Dimension(0, 20)));   // ช่องสุดท้าย ↔ ปุ่ม ห่าง 20
        add(submitButton);
        add(Box.createRigidArea(new Dimension(0, 16)));   // ปุ่ม ↔ ลิงก์ ห่าง 16
        add(linkRow);
    }

    /**
     * วางช่องกรอกลงการ์ด ต่อจากช่องที่วางไว้แล้ว และให้กด Enter ในช่องนี้ = กดปุ่มหลัก
     * เช่น addField(username) แล้ว addField(password) → Username อยู่บน Password อยู่ล่าง
     *
     * @param field ช่องกรอกที่จะวาง (FormField)
     * @throws IllegalArgumentException ถ้า field เป็น null
     */
    void addField(FormField field) {
        if (field == null) {
            throw new IllegalArgumentException("field must not be null");
        }
        // fields.getComponentCount() = เมธอดของ JPanel (Swing มีให้) นับว่าตอนนี้ในแผ่น fields มีของกี่ชิ้น
        //   addField ครั้งที่ 1: ยังว่าง → 0 → 0 > 0 เป็นเท็จ → ไม่เว้น
        //   addField ครั้งที่ 2: มี Username แล้ว → 1 → เป็นจริง → เว้นก่อน แล้วค่อยวาง Password
        if (fields.getComponentCount() > 0) {
            // Box.createRigidArea(new Dimension(0, 14)) = ของว่าง ๆ กว้าง 0 สูง 14 มองไม่เห็น ใช้ดันให้ห่างกัน 14
            fields.add(Box.createRigidArea(new Dimension(0, 14)));  // เว้นจากช่องก่อนหน้า 14 (ค่าเดิม LoginFrame บรรทัด 129)
        }
        fields.add(field);   // วางช่องต่อท้ายในแผ่น fields → ช่องขึ้นบนจอ ตามลำดับที่เรียก addField

        // ทำให้กด Enter ในช่องนี้ = กดปุ่มหลัก
        //   field.onEnter(...) = เมธอดของ FormField (FormField.java บรรทัด 160–172)
        //       รับ "คำสั่ง" 1 อัน (Runnable) แล้วสั่ง Java ว่า ผู้ใช้กด Enter ในช่องนี้เมื่อไหร่ → ทำคำสั่งนั้น
        //   submitButton::doClick = คำสั่งที่ส่งให้ onEnter แปลว่า "เรียก doClick() ของปุ่มหลัก"
        //       เขียนแบบยาวได้ว่า () -> submitButton.doClick()   (:: = อ้างถึงเมธอด ยังไม่เรียกตอนนี้)
        //       doClick() = เมธอดของ JButton (Swing มีให้) สั่งกดปุ่มแทนผู้ใช้ ปุ่มยุบลงแป๊บหนึ่งแล้วทำงานเหมือนโดนคลิก
        //   ผลตอนรัน: ผู้ใช้พิมพ์รหัสแล้วกด Enter → onEnter เรียก doClick → ปุ่มถูกกด
        //             → ปุ่มทำคำสั่งที่ฝากไว้ใน onSubmit (เช่น login ของ LoginFrame)
        //   ฝาก doClick แทนฝาก login ตรง ๆ เพราะ addField ไม่รู้ว่าปุ่มจะทำอะไร
        //   → จะเรียก onSubmit ก่อนหรือหลัง addField ก็ได้ (เทสแล้วทั้ง 2 แบบ)
        field.onEnter(submitButton::doClick);
    }

    /**
     * ฝากคำสั่งที่ต้องทำตอนกดปุ่มหลัก (หรือกด Enter ในช่องกรอก)
     * เช่น signIn.onSubmit(this::login) → กดปุ่ม Sign In แล้วเรียก login() ของ LoginFrame
     *
     * @param action คำสั่งที่จะทำ
     * @throws IllegalArgumentException ถ้า action เป็น null
     */
    void onSubmit(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        // addActionListener(...) = เมธอดของ JButton (Swing มีให้) ฝากโค้ดไว้กับปุ่ม กดปุ่มเมื่อไหร่ → โค้ดนั้นทำงาน
        // e -> action.run() = lambda: e คือข้อมูลเหตุการณ์กดปุ่มที่ Java ส่งมา (ไม่ได้ใช้) แล้วเรียกคำสั่งที่ฝากมา
        //   เช่น onSubmit(this::login) → action คือ login ของ LoginFrame → กดปุ่มแล้ว login() ทำงาน
        submitButton.addActionListener(e -> action.run());
    }

    /**
     * ฝากคำสั่งที่ต้องทำตอนกดปุ่มลิงก์ ⑤
     * เช่น signIn.onLink(this::showSignUp) → กดคำว่า Sign Up แล้วสลับไปการ์ด Sign Up
     *
     * @param action คำสั่งที่จะทำ
     * @throws IllegalArgumentException ถ้า action เป็น null
     */
    void onLink(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        // เหมือน onSubmit แต่ฝากไว้กับปุ่มลิงก์ ⑤ เช่น onLink(this::showSignUp) → กดคำว่า Sign Up แล้ว showSignUp() ทำงาน
        linkButton.addActionListener(e -> action.run());
    }
}