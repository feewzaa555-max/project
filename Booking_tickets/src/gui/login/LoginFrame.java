//LoginFrame <- หน้าต่างแรกของโปรแกรม (มี main) โชว์การ์ด Sign In / Sign Up แล้วพาไปหน้าถัดไปตามคนที่ login
/*
    หน้าตา (เลขตรงกับ ①–⑦ ในโค้ดข้างล่าง):
        ┌──────────── KU CINEMA - Sign In ────────────┐  ① หน้าต่าง 440 x 560 ล็อกขนาด
        │  พื้นเทา                                     │  ② พื้นที่กลาง วางของไว้ตรงกลาง
        │                 KU CINEMA                    │  ③ คำสีฟ้าเหนือการ์ด
        │        ┌──────────────────────────┐          │
        │        │  AuthCard (Sign In)      │          │  ④ การ์ด + ช่องกรอก (showSignIn / showSignUp)
        │        │  Username / Password     │          │
        │        │  [ Sign In ]             │          │  ⑤ กดปุ่ม → login / register
        │        │  Don't have an account? Sign Up     │  ④ กดคำ Sign Up → สลับการ์ด
        │        └──────────────────────────┘          │
        └──────────────────────────────────────────────┘
        login ผ่าน → ⑥ admin ไป AdminFrame / ลูกค้าไป MainFrame
        เปิดโปรแกรม → ⑦ main

    ทำไมต้องมี:
        เป็นตัวประกอบชิ้นของหน้า login (LoginTheme, FormField, AuthCard) ให้ขึ้นจอและใช้งานได้จริง
        และเป็นคนตัดสินว่า login แล้วไปหน้าไหน ของเดิม (gui/LoginFrame) เปิด MainFrame ให้ทุกคน admin จึงเข้าหน้า admin ไม่ได้

    เมธอดในไฟล์นี้:
        LoginFrame(app)     ตั้งค่าหน้าต่าง แล้วโชว์การ์ด Sign In
        showSignIn()        สร้างการ์ด Sign In ใบใหม่ (ช่อง Username, Password) วางแทนใบเดิม
        showSignUp()        สร้างการ์ด Sign Up ใบใหม่ วางแทนใบเดิม
        showCard(...)       วางการ์ดลงกลางหน้าต่าง (มีคำ KU CINEMA ข้างบน) และเปลี่ยนชื่อหน้าต่าง
        login(...)          กดปุ่ม Sign In: ถาม AuthService → ผ่าน = เปิดหน้าถัดไป / ผิด = ข้อความแดงใต้ช่อง
        register(...)       กดปุ่ม Create Account: ถาม AuthService → ผ่าน = กล่องแจ้ง แล้วกลับการ์ด Sign In
        showFormErrors(...) เอา error จาก AuthFormException ไปขึ้นใต้ช่องที่ผิด
        showFileError(...)  อ่าน/เขียนไฟล์ users.csv ไม่ได้ → กล่องแจ้งเตือน
        openNextPage(user)  admin → AdminFrame, ลูกค้า → MainFrame แล้วปิดหน้านี้
        main(args)          จุดเริ่มโปรแกรม

    ทำไมสลับหน้าแล้วสร้างการ์ดใบใหม่ (ไม่เก็บใบเดิมไว้):
        ใบใหม่ช่องว่างเสมอ ไม่มีข้อความแดงค้างจากรอบก่อน ไม่ต้องมีเมธอดล้างช่อง (FormField ไม่มี setText)

    ยืดหยุ่นยังไง:
        - เพิ่มช่องใหม่ (เช่น Confirm Password) → เพิ่ม new FormField + addField ใน showSignUp
        - เพิ่มหน้าใหม่ (เช่น ลืมรหัสผ่าน) → เขียนเมธอด showXxx() แบบเดียวกับ showSignUp แล้ว onLink ไปหา
        - ทั้งสองแบบไม่ต้องแก้ AuthCard / FormField / LoginTheme

    เปิดหน้านี้ยังไง:
        new LoginFrame(app).setVisible(true);   // app = AppServices ตัวเดิมของทั้งโปรแกรม (ห้าม create ใหม่)
        ใครเปิด: main ข้างล่าง (ตอนเปิดโปรแกรม), AdminFrame กด Logout (หลังแก้ AdminFrame บรรทัด 30)
        MainFrame กด Logout ยังเปิด gui/LoginFrame ตัวเก่าอยู่ จนกว่าจะแยกไฟล์ MainFrame
 */

package gui.login;

import exception.AuthField;           // ชื่อช่อง USERNAME / PASSWORD ใช้ถาม AuthFormException ว่าช่องไหนผิดอะไร
import exception.AuthFormException;   // error ของฟอร์ม (ชื่อสั้น / รหัสผิด / ชื่อซ้ำ ...) ที่ AuthService โยนมา
import gui.MainFrame;                 // หน้าลูกค้า (เลือกหนัง) เปิดหลังลูกค้า login ผ่าน
import gui.admin.AdminFrame;          // หน้า admin (เลือกหนัง 5 เรื่องของพรุ่งนี้) เปิดหลัง admin login ผ่าน
import java.awt.*;                    // GridBagLayout = วางของไว้กลางพื้นที่, Dimension = ขนาดที่ว่าง
import java.io.IOException;           // อ่าน/เขียนไฟล์ users.csv ไม่ได้ (AuthService โยนมา)
import javax.swing.*;                 // JFrame, JPanel, JLabel, JOptionPane, BoxLayout, Box, SwingUtilities, UIManager
import model.User;                    // คนที่ login / สมัครสำเร็จ (ชื่อ, รหัส, ADMIN หรือ USER)
import service.AppServices;           // ของหลังบ้านชุดเดียวกับทั้งโปรแกรม (auth, movies, accounts, ...)

public final class LoginFrame extends JFrame {   // public เพราะ AdminFrame (โฟลเดอร์ gui/admin) ต้อง new หน้านี้ตอน Logout

    private final AppServices app;   // ของหลังบ้าน ใช้ app.auth() ตอนกดปุ่ม และส่งต่อให้หน้าถัดไป
    private final JPanel center;     // ② พื้นที่กลางหน้าต่าง showCard วางการ์ดลงที่นี่

    /**
     * ① เปิดหน้า login (เริ่มที่การ์ด Sign In)
     *
     * @param app ของหลังบ้านชุดเดียวกับทั้งโปรแกรม ห้าม null
     * @throws IllegalArgumentException ถ้า app เป็น null
     */
    public LoginFrame(AppServices app) {
        // ไม่มีของหลังบ้าน = กดปุ่มแล้ว login ไม่ได้ → หยุดตั้งแต่ตอนสร้าง (SC2 หน้า 9 Defensive Programming)
        if (app == null) {
            throw new IllegalArgumentException("app must not be null");
        }
        this.app = app;

        // ① ตั้งค่าหน้าต่าง (ทุกเมธอดเป็นของ JFrame ที่ Swing มีให้)
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);   // กดกากบาท = ปิดโปรแกรม
        setSize(440, 560);                                // กว้าง 440 สูง 560 px (ค่าเดิม)
        setLocationRelativeTo(null);                      // null = เปิดกลางจอ
        setResizable(false);                              // ห้ามลากยืด หน้าตาจะได้ไม่เพี้ยน

        // ② พื้นที่กลาง: พื้นเทา
        //    GridBagLayout ใส่ของชิ้นเดียว → วางชิ้นนั้นไว้กลางพื้นที่ ตามขนาดของมัน ไม่ยืดเต็ม (แบบเดียวกับ AdminFrame)
        center = new JPanel(new GridBagLayout());
        center.setBackground(LoginTheme.BACKGROUND);
        setContentPane(center);   // setContentPane = ให้ center เป็นเนื้อหาทั้งหมดของหน้าต่าง

        showSignIn();   // เปิดมาเจอการ์ด Sign In ก่อน
    }

    /**
     * ④ สร้างการ์ด Sign In ใบใหม่ วางแทนใบเดิม
     * เรียกตอน: เปิดหน้าต่าง, กดคำ "Sign In" ใต้การ์ด Sign Up, สมัครสำเร็จ
     */
    private void showSignIn() {
        // new FormField(ชื่อป้าย, เป็นช่องรหัสไหม) = ป้าย + ช่องพิมพ์ + ข้อความแดง 1 ชุด (FormField.java บรรทัด 45)
        //   false = ช่องธรรมดา, true = ช่องรหัส พิมพ์แล้วโชว์เป็นจุด
        FormField username = new FormField("Username", false);
        FormField password = new FormField("Password", true);

        // new AuthCard(หัวข้อ, คำต้อนรับ, คำบนปุ่ม, สีปุ่ม, คำถาม, คำลิงก์) = การ์ดเปล่า 1 ใบ (AuthCard.java บรรทัด 61)
        //   LoginTheme.BRAND = "KU CINEMA" → คำต้อนรับ "Welcome to KU CINEMA"
        //   LoginTheme.PRIMARY = สีฟ้า → ปุ่ม Sign In และคำ "Sign Up" เป็นสีฟ้า
        AuthCard card = new AuthCard("Sign In", "Welcome to " + LoginTheme.BRAND, "Sign In",
                LoginTheme.PRIMARY, "Don't have an account?", "Sign Up");

        // addField(ช่อง) = วางช่องลงการ์ดเรียงตามลำดับ และให้กด Enter ในช่อง = กดปุ่มหลัก (AuthCard.java บรรทัด 150)
        card.addField(username);   // Username อยู่บน
        card.addField(password);   // Password อยู่ล่าง

        // onSubmit(คำสั่ง) = ฝากคำสั่งไว้กับปุ่มหลัก กดปุ่ม (หรือ Enter ในช่อง) แล้วคำสั่งนี้ทำงาน (AuthCard.java บรรทัด 183)
        //   () -> login(username, password) = คำสั่งที่ฝาก เป็น Runnable (lambda, SC8 หน้า 12–13) ยังไม่ทำตอนนี้ ทำตอนกดปุ่ม
        //   Java แปลบรรทัดนี้เป็นแบบยาวได้ว่า (ความหมายเหมือนกันทุกตัวอักษร):
        //       card.onSubmit(new Runnable() {
        //           public void run() {
        //               login(username, password);   // ← สิ่งที่อยู่หลัง -> มาอยู่ในตัว run()
        //           }
        //       });
        //   ข้างใน onSubmit: submitButton.addActionListener(e -> action.run())   (AuthCard.java บรรทัด 190)
        //   กดปุ่ม → action.run() → ทำตัว run() ข้างบน → login(username, password) ทำงาน
        //   ใช้ () -> เพราะ login ต้องรับพารามิเตอร์ 2 ตัว (this::login ใช้ไม่ได้ คอมไพล์ไม่ผ่าน)
        //   username, password = 2 ช่องที่ new ไว้ข้างบน (บรรทัด 99–100) ของการ์ดใบนี้
        card.onSubmit(() -> login(username, password));

        // onLink(คำสั่ง) = ฝากคำสั่งไว้กับคำลิงก์ "Sign Up" (AuthCard.java บรรทัด 200) กดคำนี้ → run() → showSignUp()
        //   this::showSignUp = "เรียก showSignUp() ของหน้านี้" ใช้ :: ได้เพราะ showSignUp ไม่รับพารามิเตอร์
        //   เขียนยาวได้ว่า () -> showSignUp() ผลเหมือนกัน  (:: แบบ Integer::intValue SC8 หน้า 17)
        card.onLink(this::showSignUp);

        showCard(card, "Sign In");
    }

    /**
     * ④ สร้างการ์ด Sign Up ใบใหม่ วางแทนใบเดิม
     * เรียกตอน: กดคำ "Sign Up" ใต้การ์ด Sign In
     */
    private void showSignUp() {
        FormField username = new FormField("Username", false);
        FormField password = new FormField("Password", true);

        // ต่างจาก Sign In แค่ข้อความ กับสีเขียว (LoginTheme.SUCCESS) ที่ปุ่ม Create Account และคำ "Sign In"
        AuthCard card = new AuthCard("Sign Up", "Create an account to start booking tickets", "Create Account",
                LoginTheme.SUCCESS, "Already have an account?", "Sign In");
        card.addField(username);
        card.addField(password);
        card.onSubmit(() -> register(username, password));   // กด Create Account → register
        card.onLink(this::showSignIn);                        // กดคำ "Sign In" → กลับการ์ด Sign In

        showCard(card, "Sign Up");
    }

    /**
     * ③④ วางการ์ดลงกลางหน้าต่าง (เอาใบเดิมออก) มีคำ KU CINEMA สีฟ้าข้างบน และเปลี่ยนชื่อหน้าต่าง
     *
     * @param card การ์ดที่จะโชว์
     * @param mode "Sign In" หรือ "Sign Up" ใช้ต่อท้ายชื่อหน้าต่าง เช่น "KU CINEMA - Sign Up"
     */
    private void showCard(AuthCard card, String mode) {
        // ③ คำ KU CINEMA สีฟ้า (LoginTheme: FONT_BRAND = Tahoma หนา 18, PRIMARY = ฟ้า)
        JLabel brand = new JLabel(LoginTheme.BRAND);
        brand.setFont(LoginTheme.FONT_BRAND);
        brand.setForeground(LoginTheme.PRIMARY);
        // CENTER_ALIGNMENT = ค่าคงที่ 0.5 (= กลาง) ของ Component ที่ JFrame สืบทอดมา เลยเขียนชื่อสั้น ๆ ได้เลย
        brand.setAlignmentX(CENTER_ALIGNMENT);   // JLabel ค่าเริ่มต้นชิดซ้าย ต้องสั่งให้อยู่กลางเหนือการ์ด
        card.setAlignmentX(CENTER_ALIGNMENT);    // การ์ดอยู่กลางเหมือนกัน คำกับการ์ดจะได้ตรงกัน

        // แผ่นใส ๆ ซ้อน KU CINEMA ไว้บนการ์ด (BoxLayout Y = เรียงบนลงล่าง)
        // ต้องมีแผ่นนี้ เพราะ GridBagLayout ของ center วางกลางได้ทีละชิ้น → รวม 2 ชิ้นเป็นชิ้นเดียวก่อน
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);                                // ไม่ทาสีพื้น ให้เห็นพื้นเทาของหน้าต่าง
        column.add(brand);
        column.add(Box.createRigidArea(new Dimension(0, 12)));  // ที่ว่างสูง 12 มองไม่เห็น: KU CINEMA ห่างการ์ด 12
        column.add(card);

        // เอาการ์ดใบเดิมออก แล้ววางใบใหม่ (เมธอดของ JPanel ที่ Swing มีให้)
        center.removeAll();     // เอาของทุกชิ้นใน center ออก (การ์ดใบเดิม)
        center.add(column);     // วางชิ้นใหม่ GridBagLayout จัดไว้กลาง
        center.revalidate();    // บอกให้คำนวณตำแหน่งใหม่ (มีของเปลี่ยน)
        center.repaint();       // วาดใหม่ ไม่งั้นอาจเห็นการ์ดเก่าค้างบนจอ

        setTitle(LoginTheme.BRAND + " - " + mode);   // ชื่อหน้าต่าง เช่น "KU CINEMA - Sign In"
    }

    /**
     * ⑤ กดปุ่ม Sign In: ตรวจชื่อกับรหัส ผ่าน → เปิดหน้าถัดไป / ผิด → ข้อความแดงใต้ช่องที่ผิด
     *
     * @param username ช่อง Username ของการ์ดที่โชว์อยู่
     * @param password ช่อง Password ของการ์ดที่โชว์อยู่
     */
    private void login(FormField username, FormField password) {
        // ล้างข้อความแดงของรอบก่อน (FormField.java บรรทัด 148) ไม่งั้นแก้ถูกแล้ว error เก่ายังค้าง
        username.clearError();
        password.clearError();
        try {
            // text() = ค่าที่พิมพ์ในช่อง (FormField.java บรรทัด 102) เช่น "admin", "admin1"
            // app.auth() = AuthService (AppServices.java บรรทัด 245)
            // login(ชื่อ, รหัส) (AuthService.java บรรทัด 46) ผ่าน → คืน User / ผิด → throw AuthFormException
            //   เช่น login("admin", "admin1") → User(admin, admin1, ADMIN)
            User user = app.auth().login(username.text(), password.text());
            openNextPage(user);
        } catch (AuthFormException e) {
            // ผิด เช่น ชื่อไม่เจอ / รหัสผิด / ช่องว่าง → ขึ้นใต้ช่องที่ผิด
            showFormErrors(e, username, password);
        } catch (IOException e) {
            showFileError(e);
        }
    }

    /**
     * ⑤ กดปุ่ม Create Account: สมัคร ผ่าน → กล่องแจ้ง แล้วกลับการ์ด Sign In / ผิด → ข้อความแดงใต้ช่องที่ผิด
     *
     * @param username ช่อง Username ของการ์ดที่โชว์อยู่
     * @param password ช่อง Password ของการ์ดที่โชว์อยู่
     */
    private void register(FormField username, FormField password) {
        username.clearError();
        password.clearError();
        try {
            // register(ชื่อ, รหัส) (AuthService.java บรรทัด 84) ตรวจกฎ → ผ่าน = เขียนแถวใหม่ลง users.csv แล้วคืน User
            //   เช่น register("tester9", "pass1234") → users.csv มีแถว tester9,pass1234,USER เพิ่ม
            //   ผิด เช่น register("1a", "12") → throw AuthFormException (ชื่อสั้น, ขึ้นต้นไม่ใช่ตัวอักษร, รหัสสั้น)
            User user = app.auth().register(username.text(), password.text());

            // JOptionPane.showMessageDialog(หน้าที่เด้งทับ, ข้อความ, หัวกล่อง, ไอคอน i) = กล่องแจ้ง มีปุ่ม OK
            //   โค้ดบรรทัดถัดไปรอจนกด OK ก่อน (\n = ขึ้นบรรทัดใหม่)
            //   user.username() = ชื่อของ User ที่เพิ่งสมัคร (User เป็น record ชื่อช่องคือชื่อเมธอด) เช่น "tester9"
            JOptionPane.showMessageDialog(this,
                    "Registration successful!\nUsername: " + user.username() + "\nPlease sign in to continue.",
                    "Registration successful", JOptionPane.INFORMATION_MESSAGE);
            showSignIn();   // กลับการ์ด Sign In ใบใหม่ ให้ login ด้วยบัญชีที่เพิ่งสมัคร
        } catch (AuthFormException e) {
            showFormErrors(e, username, password);
        } catch (IOException e) {
            showFileError(e);
        }
    }

    /**
     * เอา error จาก AuthFormException ไปขึ้นใต้ช่องที่ผิด ช่องที่ไม่ผิดไม่มีอะไรขึ้น
     * ใช้ร่วมกันทั้ง login และ register (เขียนครั้งเดียว SC2 หน้า 8 DRY)
     *
     * @param e        error ที่ AuthService โยนมา
     * @param username ช่อง Username
     * @param password ช่อง Password
     */
    private void showFormErrors(AuthFormException e, FormField username, FormField password) {
        // e.messagesFor(ช่อง) = ข้อความ error ของช่องนั้น (AuthFormException.java บรรทัด 53) ไม่ผิด = list ว่าง
        //   เช่น สมัคร "1a" / "12" → USERNAME: ["Username must be at least 5 characters", "First character must be an English letter"]
        //                          → PASSWORD: ["Password must be at least 5 characters"]
        // showErrors(list) = ขึ้นข้อความแดงข้อละบรรทัด list ว่าง = ซ่อน (FormField.java บรรทัด 123)
        username.showErrors(e.messagesFor(AuthField.USERNAME));
        password.showErrors(e.messagesFor(AuthField.PASSWORD));
    }

    /**
     * อ่าน/เขียนไฟล์ users.csv ไม่ได้ (เช่น ไฟล์ถูกเปิดค้างใน Excel) → กล่องแจ้งเตือนสีแดง
     * ไม่ใช่ความผิดของคนกรอก จึงไม่ขึ้นใต้ช่อง
     *
     * @param e error จากการอ่าน/เขียนไฟล์
     */
    private void showFileError(IOException e) {
        // e.getMessage() = ข้อความอธิบายว่าพังเพราะอะไร (Java ใส่มาให้)
        JOptionPane.showMessageDialog(this,
                "Cannot read or write user data: " + e.getMessage(),
                "File error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * ⑥ login ผ่าน → เปิดหน้าถัดไปตามบทบาท แล้วปิดหน้านี้
     *   admin  → AdminFrame (เลือกหนัง 5 เรื่องของพรุ่งนี้)
     *   ลูกค้า → MainFrame (เลือกหนัง จองตั๋ว)
     *
     * @param user คนที่ login ผ่าน
     */
    private void openNextPage(User user) {
        // user.isAdmin() = บทบาทเป็น ADMIN ไหม (User.java บรรทัด 70) เช่น admin → true, tester9 → false
        // x ? a : b = ถ้า x จริงได้ a ถ้าไม่จริงได้ b (ทั้ง 2 หน้าเป็น JFrame เลยเก็บในตัวแปรเดียวกันได้)
        // ส่ง app ตัวเดิมไป นาฬิกาและไฟล์ทั้งโปรแกรมจะได้เป็นชุดเดียวกัน
        //   new AdminFrame(คน, app) (AdminFrame.java บรรทัด 48) / new MainFrame(คน, app) (MainFrame.java บรรทัด 87)
        JFrame next = user.isAdmin() ? new AdminFrame(user, app) : new MainFrame(user, app);
        next.setVisible(true);   // โชว์หน้าใหม่ก่อน
        dispose();               // แล้วค่อยปิดหน้า login (dispose = ปิดหน้าต่างนี้ ไม่ได้ปิดโปรแกรม)
    }

    /**
     * ⑦ จุดเริ่มโปรแกรม: สร้างของหลังบ้านครั้งเดียว แล้วเปิดหน้า login
     *
     * @param args ไม่ได้ใช้
     */
    public static void main(String[] args) {
        // ให้ปุ่ม ช่อง กล่องแจ้ง หน้าตาแบบเครื่องที่รัน (Windows) เหมือนของเดิม ทุกหน้าที่เปิดต่อจากนี้ใช้แบบเดียวกัน
        //   UIManager.getSystemLookAndFeelClassName() = ชื่อหน้าตาของเครื่องนี้
        //   UIManager.setLookAndFeel(ชื่อ) = ใช้หน้าตานั้น
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // | = จับได้ทั้ง 2 แบบในก้อนเดียว (setLookAndFeel ประกาศว่าอาจโยน 2 แบบนี้ เป็น checked ต้องจัดการ)
            //   ReflectiveOperationException = หา/สร้างคลาสหน้าตานั้นไม่ได้
            //   UnsupportedLookAndFeelException = เครื่องนี้ใช้หน้าตานั้นไม่ได้
            // หาหน้าตาของเครื่องไม่เจอ / ใช้ไม่ได้ → ใช้หน้าตาปกติของ Java แทน โปรแกรมยังทำงานต่อได้ (SC2 หน้า 11)
            System.err.println("Cannot use system look and feel: " + e.getMessage());   // แจ้งใน console สีแดง ไม่เด้งกล่อง
        }

        // SwingUtilities.invokeLater(คำสั่ง) = ให้ Swing สร้างหน้าต่างในเธรดของหน้าจอ (กฎของ Swing)
        // AppServices.create() = สร้างของหลังบ้าน 1 ชุด หาโฟลเดอร์ data เอง (AppServices.java บรรทัด 183)
        SwingUtilities.invokeLater(() -> new LoginFrame(AppServices.create()).setVisible(true));
    }
}