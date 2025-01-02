/*
 __  __ _____ ____    _    ____   ___   ____ _____ ___  ____  
|  \/  | ____/ ___|  / \  |  _ \ / _ \ / ___|_   _/ _ \|  _ \ 
| |\/| |  _|| |  _  / _ \ | | | | | | | |     | || | | | |_) |
| |  | | |__| |_| |/ ___ \| |_| | |_| | |___  | || |_| |  _ < 
|_|  |_|_____\____/_/   \_\____/ \___/ \____| |_| \___/|_| \_\
                                                              
by tonikelope

 */
package com.tonikelope.megadoctor;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import static com.tonikelope.megadoctor.Main.MAIN_WINDOW;
import static com.tonikelope.megadoctor.Main.MEGA_ACCOUNTS;
import static com.tonikelope.megadoctor.Main.MEGA_CMD_WINDOWS_PATH;
import static com.tonikelope.megadoctor.Main.MEGA_NODES;
import static com.tonikelope.megadoctor.Main.THREAD_POOL;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;
import me.shivzee.JMailTM;
import me.shivzee.callbacks.MessageFetchedCallback;
import me.shivzee.util.JMailBuilder;
import me.shivzee.util.Message;
import me.shivzee.util.Response;

/**
 *
 * @author tonikelope
 */
public class Helpers {

    public static final int NEW_ACCOUNT_CONFIRM_TIMEOUT = 60;

    public static final int DOM_SELECTOR_TIMEOUT = 20000;

    public static final float FILE_DIALOG_FONT_ZOOM = 1.3f;

    public static final float FILE_DIALOG_SIZE_ZOOM = 0.7f;

    public static final int MASTER_PASSWORD_PBKDF2_SALT_BYTE_LENGTH = 16;

    public static final int MASTER_PASSWORD_PBKDF2_OUTPUT_BIT_LENGTH = 256;

    public static final int MASTER_PASSWORD_PBKDF2_ITERATIONS = 65536;

    public static final ConcurrentLinkedQueue<Process> PROCESSES_QUEUE = new ConcurrentLinkedQueue<>();

    public static String getMasterKey() {
        try {
            // Iniciar la consola interactiva de MEGAcmd
            ProcessBuilder processBuilder = new ProcessBuilder("mega-cmd");

            // Configurar PATH según tu implementación actual
            String path = Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null;
            processBuilder.environment().put("PATH", path + File.pathSeparator + System.getenv("PATH"));

            processBuilder.redirectErrorStream(true); // Redirige la salida de error a la salida estándar

            Process process = processBuilder.start();

            // Crear lector y escritor para interactuar con el proceso
            try (
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())); BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;

                String masterKey = null;

                writer.write("masterkey\n");

                writer.flush();

                // Leer hasta que aparezca el prompt inicial
                while ((line = reader.readLine()) != null) {

                    if (line.trim().endsWith(":/$")) {
                        break;
                    }
                }

                while ((line = reader.readLine()) != null) {

                    if (line.trim().length() > 0 && !line.trim().endsWith(":/$")) {
                        masterKey = line.trim();
                        break;
                    }
                }

                return masterKey; // Retornar la clave maestra
            } finally {
                process.destroy(); // Asegurar destrucción del proceso
            }
        } catch (IOException ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static byte[] PBKDF2HMACSHA256(String password, byte[] salt, int iterations, int output_length) throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec ks = new PBEKeySpec(password.toCharArray(), salt, iterations, output_length);

        return f.generateSecret(ks).getEncoded();
    }

    public static byte[] BASE642Bin(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static String Bin2BASE64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] HashBin(String algo, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);

        return md.digest(data);
    }

    public static void fetchTMmailMessages(String email, String password) {

        MAIN_WINDOW.enableTOPControls(false);

        Helpers.GUIRun(() -> {
            MAIN_WINDOW.getStatus_label().setText("Reading emails from " + email + "...");
            MAIN_WINDOW.output_textarea_append("\n\nReading emails from " + email + "...");
        });

        try {
            JMailTM mailer = JMailBuilder.login(email, password);

            mailer.init();

            mailer.fetchMessages(new MessageFetchedCallback() {
                @Override
                public void onMessagesFetched(List<Message> list) {

                    if (list.isEmpty()) {

                        Helpers.GUIRun(() -> {
                            MAIN_WINDOW.output_textarea_append("\nTHERE ARE NO MESSAGES\n\n");
                        });

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "NO MESSAGES");

                    } else {

                        int i = 1;
                        for (Message message : list) {
                            Helpers.GUIRun(() -> {
                                MAIN_WINDOW.output_textarea_append("\n\n(" + String.valueOf(i) + "/" + String.valueOf(list.size()) + ") " + message.getSenderName() + " (" + message.getSenderAddress() + ") [" + message.getCreatedAt() + "]\n" + message.getContent() + "\n\n");
                            });
                        }

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, String.valueOf(list.size()) + " MESSAGES");

                    }

                    Helpers.GUIRun(() -> {
                        MAIN_WINDOW.getStatus_label().setText("");
                    });

                    MAIN_WINDOW.enableTOPControls(true);

                }

                @Override
                public void onError(Response response) {

                    Helpers.GUIRun(() -> {
                        MAIN_WINDOW.getStatus_label().setText("");
                    });

                    MAIN_WINDOW.enableTOPControls(true);

                }
            });

        } catch (javax.security.auth.login.LoginException ex) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "ERROR READING EMAILS FROM " + email + "\n\nIs this a TM mail?");

            Helpers.GUIRun(() -> {
                MAIN_WINDOW.getStatus_label().setText("");
                MAIN_WINDOW.output_textarea_append("\nERROR -> Is this a TM mail?\n\n");
            });

            MAIN_WINDOW.enableTOPControls(true);

        } catch (Exception ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static boolean MEGAWebLogin(String email, String password, boolean headless) {

        final String status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRun(() -> {
            MAIN_WINDOW.getStatus_label().setForeground(Color.MAGENTA);
            MAIN_WINDOW.getStatus_label().setText("WEB-LOGIN in " + email + "...");
        });

        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {

                BrowserContext context = browser.newContext(new Browser.NewContextOptions().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.5938.132 Safari/537.36"));

                Page page = context.newPage();

                // Navegar a la página de login de MEGA
                page.navigate("https://mega.nz/login");
                page.waitForLoadState(LoadState.NETWORKIDLE);
                // Esperar los selectores de los campos de login
                page.locator("input#login-name2").waitFor(new Locator.WaitForOptions().setTimeout(DOM_SELECTOR_TIMEOUT));
                page.locator("input#login-password2").waitFor(new Locator.WaitForOptions().setTimeout(DOM_SELECTOR_TIMEOUT));
                // Rellenar email y password
                page.fill("input#login-name2", email);
                page.fill("input#login-password2", password);
                // Hacer clic en el botón de login
                page.click("button.login-button");
                // Esperar hasta que se cargue el selector de la carpeta principal
                page.locator(".fm-new-folder").waitFor(new Locator.WaitForOptions().setTimeout(DOM_SELECTOR_TIMEOUT));
                // Cerrar el navegador
            }

            Helpers.GUIRun(() -> {
                MAIN_WINDOW.getStatus_label().setForeground(Color.BLACK);
                MAIN_WINDOW.getStatus_label().setText(status);
            });

            // Login exitoso
            return true;

        } catch (Exception e) {
            Helpers.GUIRun(() -> {
                MAIN_WINDOW.getStatus_label().setForeground(Color.BLACK);
                MAIN_WINDOW.getStatus_label().setText(status);
            });
            return false;
        }
    }

    public static void createMegaDoctorDir() {

        if (!Files.exists(Paths.get(Main.MEGADOCTOR_DIR))) {
            try {
                Files.createDirectory(Paths.get(Main.MEGADOCTOR_DIR));

                File dir = new File(System.getProperty("user.home"));

                for (File file : dir.listFiles()) {
                    if (!file.isDirectory() && file.getName().startsWith(".megadoctor")) {
                        Files.move(file.toPath(), Paths.get(Main.MEGADOCTOR_DIR + File.separator + file.getName().substring(1)));
                    }
                }

            } catch (IOException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void destroyAllExternalProcesses() {

        while (!PROCESSES_QUEUE.isEmpty()) {

            Process p = PROCESSES_QUEUE.poll();

            p.destroyForcibly();

        }
    }

    public static void restoreMegaDoctorMainWindow() {
        Helpers.GUIRun(() -> {
            if (MAIN_WINDOW != null && !MAIN_WINDOW.isVisible()) {
                MAIN_WINDOW.setExtendedState((MAIN_WINDOW.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0 ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
                MAIN_WINDOW.setVisible(true);
                MAIN_WINDOW.revalidate();
                MAIN_WINDOW.repaint();
            }
        });
    }

    public static void createTrayIcon() throws IOException, AWTException {

        //Check for SystemTray support
        if (!SystemTray.isSupported()) {
            System.err.println("System tray feature is not supported");
            return;
        }

        //Get system tray object
        SystemTray tray = SystemTray.getSystemTray();

        TrayIcon trayIcon = new TrayIcon(new ImageIcon(Helpers.class.getResource("/images/megadoctor_51.png")).getImage(), "MegaDoctor", null);
        trayIcon.setImageAutoSize(true);

        JPopupMenu jpopup = new JPopupMenu();
        JMenuItem restore = new JMenuItem("Restore");
        restore.setIcon(new ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));
        jpopup.add(restore);

        restore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {

                restoreMegaDoctorMainWindow();

            }
        });

        trayIcon.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (MAIN_WINDOW != null && !MAIN_WINDOW.isVisible()) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        jpopup.setLocation(e.getX(), e.getY());
                        jpopup.setInvoker(jpopup);
                        jpopup.setVisible(true);
                    } else {
                        restoreMegaDoctorMainWindow();
                    }
                }

            }
        });

        //Attach TrayIcon to SystemTray
        tray.add(trayIcon);
    }

    public static class ClipStateListener implements LineListener {

        private final Object _notifier = new Object();
        private volatile boolean _completed = false;

        public Object getNotifier() {
            return _notifier;
        }

        public boolean isCompleted() {
            return _completed;
        }

        @Override
        public void update(LineEvent event) {
            if (LineEvent.Type.STOP == event.getType()) {

                _completed = true;

                synchronized (_notifier) {
                    _notifier.notifyAll();
                }
            }
        }
    }

    public static void playWavResource(String sound) {

        try (BufferedInputStream bis = new BufferedInputStream(Helpers.class.getResourceAsStream("/sounds/" + sound)); final AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis); final Clip clip = AudioSystem.getClip()) {

            final ClipStateListener listener = new ClipStateListener();

            clip.addLineListener(listener);

            clip.open(audioStream);

            final var gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float db = 20f * (float) Math.log10(1.0f);

            gainControl.setValue(db >= gainControl.getMinimum() ? db : gainControl.getMinimum());

            clip.start();

            while (!listener.isCompleted()) {
                synchronized (listener._notifier) {
                    listener._notifier.wait(1000);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, ex.getMessage());
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "ERROR -> {0}", sound);
        }
    }

    public static void setWindowLowRightCorner(Window w) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - w.getWidth();
        int y = (int) rect.getMaxY() - w.getHeight();
        w.setLocation(x, y);
    }

    public static String[] extractAccountLoginDataFromText(String email, String text) {
        final String regex = Pattern.quote(email) + "#(.+)";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            return new String[]{email, matcher.group(1)};

        }

        return null;
    }

    public static ConcurrentHashMap<String, Long> getReservedTransfersSpace() {

        synchronized (Main.TRANSFERENCES_LOCK) {

            ConcurrentHashMap<Component, Transference> transferences_map = Main.TRANSFERENCES_MAP;

            ConcurrentHashMap<String, Long> reserved = new ConcurrentHashMap<>();

            for (Transference t : transferences_map.values()) {
                if (!t.isFinishing() && !t.isFinished() && !t.isCanceled()) {

                    if (reserved.containsKey(t.getEmail())) {
                        long s = reserved.get(t.getEmail());
                        reserved.put(t.getEmail(), s + t.getFileSize());
                    } else {
                        reserved.put(t.getEmail(), t.getFileSize());
                    }
                }
            }

            return reserved;
        }

    }

    public static String findFirstAccountWithSpace(long required, String filename) {
        return findFirstAccountWithSpace(required, filename, false);
    }

    public static String findFirstAccountWithSpace(long required, String filename, boolean clear_cache) {

        if (clear_cache) {
            Main.FREE_SPACE_CACHE.clear();
        }

        String bingo = null;

        ConcurrentHashMap<String, Long> reserved = getReservedTransfersSpace();

        ArrayList<String> emails = new ArrayList<>();

        for (String email : Main.MEGA_ACCOUNTS.keySet()) {
            if (!Main.MEGA_EXCLUDED_ACCOUNTS.contains(email)) {
                emails.add(email);
            }
        }

        Collections.sort(emails, String.CASE_INSENSITIVE_ORDER);

        final String space_required = Helpers.formatBytes(required);

        for (String email : emails) {
            Helpers.GUIRunAndWait(() -> {
                Main.MAIN_WINDOW.getStatus_label().setText("AUTO-ALLOCATION: [" + space_required + "] " + filename + " -> " + email);
            });
            Long r = reserved.get(email);
            Long s = Main.FREE_SPACE_CACHE.get(email);
            if (s == null) {
                s = Helpers.getAccountFreeSpace(email);

                Main.FREE_SPACE_CACHE.put(email, s);
            } else if (s - (r != null ? r : 0) >= required) {
                //Doble comprobación en caso de que el valor cacheado sea aparentemente válido para minimizar error al empezar transferencia
                s = Helpers.getAccountFreeSpace(email);

                Main.FREE_SPACE_CACHE.put(email, s);
            }
            if (s - (r != null ? r : 0) >= required) {
                bingo = email;
                break;
            }
        }

        Helpers.GUIRunAndWait(() -> {
            Main.MAIN_WINDOW.getStatus_label().setText("");
        });

        return (bingo != null || clear_cache) ? bingo : findFirstAccountWithSpace(required, filename, true);
    }

    public static String exportPathFromCurrentAccount(String rpath) {
        String[] export = Helpers.runProcess(new String[]{"mega-export", "-af", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(export[2]) == 0) {
            final String regex = "https://.+$";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(export[1]);

            if (matcher.find()) {

                return matcher.group(0);
            }
        }

        return null;

    }

    public static String adjustSpaces(String s, int n) {

        String spaces = s.trim().replaceAll("^[^ ]*( *)[^ ]*$", "$1");

        String new_spaces = " ".repeat(spaces.length() + n);

        return s.replace(spaces, new_spaces);
    }

    public static void smartPack(Window w) {

        Helpers.GUIRun(() -> {
            if (w.getPreferredSize().getHeight() > w.getSize().getHeight() || w.getPreferredSize().getWidth() > w.getSize().getWidth()) {

                w.pack();

                int width = w.getWidth(), height = w.getHeight();

                if (w.getWidth() > ((Window) w.getParent()).getWidth()) {
                    width = ((Window) w.getParent()).getWidth();
                }

                if (w.getHeight() > ((Window) w.getParent()).getHeight()) {
                    height = ((Window) w.getParent()).getHeight();
                }

                if (width != w.getWidth() || height != w.getHeight()) {

                    w.setPreferredSize(new Dimension(width, height));

                    w.setSize(w.getPreferredSize());
                }

                setCenterOfParent((Window) w.getParent(), w);
            }

            w.revalidate();
            w.repaint();

        });

    }

    public static String genRandomString(int l) {
        if (l < 1) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        StringBuilder cadena = new StringBuilder(l);
        for (int i = 0; i < l; i++) {
            var tipo = new Random().nextInt(62);
            char caracter;
            if (tipo < 10) {
                // Generar un dígito (0-9)
                caracter = (char) ('0' + tipo);
            } else if (tipo < 36) {
                // Generar una letra mayúscula (A-Z)
                caracter = (char) ('A' + tipo - 10);
            } else {
                // Generar una letra minúscula (a-z)
                caracter = (char) ('a' + tipo - 36);
            }
            cadena.append(caracter);
        }
        return cadena.toString();
    }

    public static void copyCompletedUploads() {

        String log = Main.MAIN_WINDOW.getOutput_textarea().getText();

        final String regex = "^.+->.+?([^" + Pattern.quote(File.separator) + "]+<H:.*?>.+)$";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        final Matcher matcher = pattern.matcher(log);

        ArrayList<String> transfers = new ArrayList<>();

        while (matcher.find()) {
            transfers.add(matcher.group(1));
        }

        Collections.sort(transfers);

        Helpers.copyTextToClipboard(String.join("\n\n", transfers.toArray(new String[0])));

        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL COMPLETED UPLOADS COPIED TO CLIPBOARD");

    }

    public static String[] registerNewMEGAaccount() {

        final CyclicBarrier barrera = new CyclicBarrier(2);

        JMailTM mailer = null;

        final String password = Helpers.genRandomString(32);

        final String[] mega_register_status = new String[1];

        mega_register_status[0] = "*";

        Helpers.threadRun(() -> {

            int t = Helpers.NEW_ACCOUNT_CONFIRM_TIMEOUT;

            while ("*".equals(mega_register_status[0]) && t > 0) {
                final int ft = t--;

                Helpers.GUIRun(() -> {
                    MAIN_WINDOW.getStatus_label().setText("Creating new MEGA account, please wait (" + String.valueOf(ft) + ")...");
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (t == 0) {

                mega_register_status[0] = "#";
            }

            Helpers.GUIRun(() -> {
                MAIN_WINDOW.getStatus_label().setText("");
            });
        });

        boolean antiflood;

        do {

            antiflood = false;

            try {
                mailer = JMailBuilder.createDefault(password);

                mailer.init();

                final String email = mailer.getSelf().getEmail();

                mailer.openEventListener(new me.shivzee.callbacks.EventListener() {
                    @Override
                    public void onMessageReceived(Message message) {

                        try {

                            String link = Helpers.findFirstRegex("https.*?#confirm[a-zA-Z0-9_-]+", message.getContent(), 0);

                            if (link != null) {

                                String[] confirm = Helpers.runProcess(new String[]{"mega-confirm", link, email, password}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null, NEW_ACCOUNT_CONFIRM_TIMEOUT);

                                mega_register_status[0] = confirm[2];

                                barrera.await();
                            }

                        } catch (Exception ex) {
                            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
                            mega_register_status[0] = "-";
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "Some Error Occurred {0}", error);
                    }
                });

                String[] register = Helpers.runProcess(new String[]{"mega-signup", mailer.getSelf().getEmail(), password}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null, NEW_ACCOUNT_CONFIRM_TIMEOUT);

                Logger.getLogger(Helpers.class.getName()).log(Level.INFO, register[1]);

                if (Integer.parseInt(register[2]) == 0) {

                    try {
                        barrera.await(NEW_ACCOUNT_CONFIRM_TIMEOUT, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "MEGA-CONFIRM EMAIL TIMEOUT :(");
                    }

                    mailer.closeMessageListener();

                    return (!barrera.isBroken() && Integer.parseInt(mega_register_status[0]) == 0) ? new String[]{mailer.getSelf().getEmail(), password} : null;
                }

            } catch (javax.security.auth.login.LoginException ex1) {
                antiflood = true;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
                }

            } catch (Exception ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            }

        } while (antiflood && !"#".equals(mega_register_status[0]));

        if (mailer != null) {
            mailer.closeMessageListener();
        }

        mega_register_status[0] = "-";

        return null;

    }

    public static boolean checkMEGALInk(String link) {
        link = link.trim();

        final String regex = "https://mega\\.nz/((folder|file)/([^#]+)#(.+)|#(F?)!([^!]+)!(.+))";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(link);

        return matcher.find();
    }

    public static String formatBytes(Long bytes) {

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        Double bytes_double = (double) bytes / (1L << (10 * pow));

        DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bytes_double) + ' ' + units[pow];
    }

    public static String megaWhoami() {
        String[] whoami = Helpers.runProcess(new String[]{"mega-whoami"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null);
        if (whoami[1].contains("security needs upgrading")) {
            Helpers.GUIRun(() -> {
                Main.MAIN_WINDOW.getStatus_label().setForeground(Color.MAGENTA);
            });

            Helpers.runProcess(new String[]{"mega-confirm", "--security"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
        }
        if (Integer.parseInt(whoami[2]) == 0) {

            return whoami[1].replaceAll("^.+: +(.+)$", "$1").trim().toLowerCase();
        }
        return "";
    }

    public static void setCenterOfParent(Window parent, Window dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);/*from w  ww. j av a2 s. com*/
    }

    public static void setCenterOfParent(JFrame parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);/*from w  ww. j av a2 s. com*/
    }

    public static void setCenterOfParent(JDialog parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);
    }

    public static String findFirstRegex(String regex, String data, int group) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        Matcher matcher = pattern.matcher(data);

        return matcher.find() ? matcher.group(group) : null;
    }

    private static long fallbackCalculateAnonMEGAFolderSize(String link) {

        Helpers.runProcess(new String[]{"mega-login", link}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        String[] ls = Helpers.runProcess(new String[]{"mega-ls", "-lr"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        Main.MAIN_WINDOW.logout(false);

        long total = 0L;

        if (Integer.parseInt(ls[2]) == 0) {

            Pattern pattern = Pattern.compile("^\\-[^ ]+ *?\\d+ *?(\\d+)");

            for (String line : ls[1].split("\n")) {

                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    total += Long.parseLong(matcher.group(1));
                }

            }

        }

        return total;
    }

    public static long getMEGALinkSize(String link) {

        if (link.contains("#F!") || link.contains("/folder/")) {

            Main.MAIN_WINDOW.logout(true);

            Helpers.runProcess(new String[]{"mega-login", link}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            String[] du = Helpers.runProcess(new String[]{"mega-du"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            Main.MAIN_WINDOW.logout(false);

            if (Integer.parseInt(du[2]) == 0) {

                String[] lines = du[1].split("\n");

                String[] size = lines[lines.length - 1].split(":");

                long total;

                try {
                    total = Long.parseLong(size[1].trim());
                } catch (Exception ex) {
                    total = fallbackCalculateAnonMEGAFolderSize(link);
                }

                return total;
            }
        } else {

            String file_id = findFirstRegex("(<?=#!)[^!]+|(?<=/file/)[^#]+", link, 0);

            try {

                URL api_url = new URL("https://g.api.mega.co.nz/cs?id=");

                String request = "[{\"a\":\"g\", \"p\":\"" + file_id + "\"}]";

                HttpURLConnection con = (HttpURLConnection) api_url.openConnection();

                con.setRequestMethod("POST");

                con.setRequestProperty("Content-Type", "application/json");

                con.setRequestProperty("Accept", "application/json");

                con.setDoOutput(true);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = request.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                StringBuilder response = new StringBuilder();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {

                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                String size = findFirstRegex("\" *s *\" *: *(\\d+)", response.toString(), 1);

                if (size != null) {
                    return Long.parseLong(size);
                }

            } catch (MalformedURLException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ProtocolException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return -1;
    }

    public static String[] getAccountSpaceData(String email) {
        Main.MAIN_WINDOW.login(email);

        String df = Helpers.runProcess(new String[]{"mega-df"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        long cloud = 0, inbox = 0, rubbish = 0, total;

        String regex_cloud = "drive: *([0-9]+)";

        Pattern pattern = Pattern.compile(regex_cloud, Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(df);

        if (matcher.find()) {
            cloud = Long.parseLong(matcher.group(1));
        }

        String regex_inbox = "box: *([0-9]+)";

        pattern = Pattern.compile(regex_inbox, Pattern.MULTILINE);

        matcher = pattern.matcher(df);

        if (matcher.find()) {
            inbox = Long.parseLong(matcher.group(1));
        }

        String regex_rubbish = "bin: *([0-9]+)";

        pattern = Pattern.compile(regex_rubbish, Pattern.MULTILINE);

        matcher = pattern.matcher(df);

        if (matcher.find()) {
            rubbish = Long.parseLong(matcher.group(1));
        }

        total = cloud + inbox + rubbish;

        String regex_used = "STORAGE: *([0-9]+).*?of *([0-9]+)";

        pattern = Pattern.compile(regex_used, Pattern.MULTILINE);

        matcher = pattern.matcher(df);

        if (matcher.find()) {
            return new String[]{email, String.valueOf(Math.max(Long.parseLong(matcher.group(1)), total)), matcher.group(2)};
        }

        return null;
    }

    public static void deleteDirectoryRecursion(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

    public static long getAccountCloudDriveUsedSpace(String email) {
        Main.MAIN_WINDOW.login(email);

        String df = Helpers.runProcess(new String[]{"mega-df"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        String regex_cloud = "drive: *([0-9]+)";

        Pattern pattern = Pattern.compile(regex_cloud, Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(df);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return 0;
    }

    public static long getAccountUsedSpace(String email) {
        String[] space_stats = getAccountSpaceData(email);

        return Long.parseLong(space_stats[1]);
    }

    public static long getAccountFreeSpace(String email) {
        String[] space_stats = getAccountSpaceData(email);

        return Long.parseLong(space_stats[2]) - Long.parseLong(space_stats[1]);
    }

    public static String getFechaHoraActual() {

        String format = "dd-MM-yyyy HH:mm:ss";

        return getFechaHoraActual(format);
    }

    public static String getFechaHoraActual(String format) {

        Date currentDate = new Date(System.currentTimeMillis());

        DateFormat df = new SimpleDateFormat(format);

        return df.format(currentDate);
    }

    public static Sequencer midiLoopPlay(String midi, int volume) {

        try {
            Sequencer sequencer = MidiSystem.getSequencer();

            if (sequencer == null) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "MIDI Sequencer device not supported");
                return null;
            }

            Synthesizer synthesizer = MidiSystem.getSynthesizer();

            Sequence sequence = MidiSystem.getSequence(Helpers.class.getResource(midi));

            //VOLUME CHANGE THANKS TO -> https://stackoverflow.com/a/18999468
            for (Track t : sequence.getTracks()) {

                for (int k = 0; k < synthesizer.getChannels().length; k++) {
                    t.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, k, 7, volume), t.ticks()));
                }
            }

            sequencer.open();

            sequencer.setSequence(sequence);

            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);

            sequencer.start();

            return sequencer;

        } catch (MidiUnavailableException | InvalidMidiDataException | IOException ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static void GUIRun(Runnable r) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(r);
        } else {
            r.run();
        }

    }

    public static void GUIRunAndWait(Runnable r) {

        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(r);
            } else {
                r.run();
            }
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static Future threadRun(Runnable r) {

        return THREAD_POOL.submit(r);

    }

    public static long getNodeMapTotalSize(HashMap<String, ArrayList<String>> map) {

        long total = 0L;

        for (String email : map.keySet()) {

            ArrayList<String> node_list = map.get(email);

            for (String node : node_list) {
                total += (long) ((Object[]) Main.MEGA_NODES.get(node))[0];
            }
        }

        return total;
    }

    public static long getNodeListTotalSize(ArrayList<String> node_list) {

        long total = 0L;

        for (String node : node_list) {
            total += (long) ((Object[]) Main.MEGA_NODES.get(node))[0];
        }

        return total;
    }

    public static void mostrarMensajeInformativo(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>");

        } else {
            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>");
                }
            });
        }
    }

    public static void mostrarMensajeAviso(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "WARNING", JOptionPane.WARNING_MESSAGE);

        } else {
            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "WARNING", JOptionPane.WARNING_MESSAGE);
                }
            });
        }
    }

    // 0=yes, 1=no, 2=cancel
    public static int mostrarMensajeInformativoSINO(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            return JOptionPane.showConfirmDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);

        } else {

            final int[] res = new int[1];

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {

                    res[0] = JOptionPane.showConfirmDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);
                }
            });

            return res[0];

        }

    }

    public static void mostrarMensajeError(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);

        } else {

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            });
        }

    }

    public static void openBrowserURLAndWait(final String url) {

        try {
            Desktop.getDesktop().browse(new URI(url));

        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(Helpers.class
                    .getName()).log(Level.SEVERE, null, ex.getMessage());
        }

    }

    public static boolean isWindows() {

        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static String[] buildCommand(String[] command) {

        String[] c = Helpers.isWindows() ? new String[]{"cmd.exe", "/c"} : new String[]{};

        String[] result = Arrays.copyOf(c, c.length + command.length);

        System.arraycopy(command, 0, result, c.length, command.length);

        return result;
    }

    public static String escapeMEGAPassword(String password) {

        return password.contains(" ") ? password : "\"" + password + "\"";
    }

    public static String[] runProcess(String[] command, String path) {
        return runProcess(command, path, false, null);
    }

    public static String[] runProcess(String[] command, String path, boolean redirectstream, Pattern stop_regex) {

        return runProcess(command, path, redirectstream, stop_regex, 0);
    }

    public static String[] runProcess(String[] command, String path, boolean redirectstream, Pattern stop_regex, long timeout) {
        if (Main.EXIT) {
            return null;
        }
        Process process = null;
        try {
            ProcessBuilder processbuilder = new ProcessBuilder(Helpers.buildCommand(command));
            if (path != null && !"".equals(path)) {
                processbuilder.environment().put("PATH", path + File.pathSeparator + System.getenv("PATH"));
            }
            processbuilder.redirectErrorStream(redirectstream);
            process = processbuilder.start();
            PROCESSES_QUEUE.add(process);
            long pid = process.pid();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (stop_regex != null) {
                        Matcher matcher = stop_regex.matcher(Pattern.quote(line));
                        if (matcher.find()) {
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (timeout > 0) {
                process.waitFor(timeout, TimeUnit.SECONDS);
            } else {
                process.waitFor();
            }
            process.destroy();
            PROCESSES_QUEUE.remove(process);
            return new String[]{String.valueOf(pid), new String(sb.toString().getBytes(), StandardCharsets.UTF_8), String.valueOf(process.exitValue())};
        } catch (Exception ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (process != null) {
            process.destroyForcibly();
        }
        PROCESSES_QUEUE.remove(process);
        return null;
    }

    public static String extractFirstEmailFromtext(String text) {

        final String regex = "[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.\\-]+@[a-zA-Z0-9.\\-]+";

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(0);
        }

        return null;
    }

    //Thanks -> https://stackoverflow.com/a/19877372 https://stackoverflow.com/a/1385498
    public static long getDirectorySize(final File folder, final AtomicBoolean terminate) {

        Path path = folder.toPath();

        final AtomicLong size = new AtomicLong(0);

        EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);

        try {
            Files.walkFileTree(path, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    if (terminate.get()) {
                        System.out.println("WALKTREEE" + folder.getAbsolutePath() + " TERMINATED!");
                        size.set(-1);
                        return FileVisitResult.TERMINATE;
                    }

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null) {
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    }
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();

    }

    public static HashMap<String, ArrayList<String>> extractNodeMapFromText(String text) {

        final String regex = "H:[^> ]+";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(text);

        HashMap<String, ArrayList<String>> nodesMAP = new HashMap<>();

        while (matcher.find()) {

            String node = matcher.group(0);

            if (MEGA_NODES.containsKey(node)) {

                String email = (String) ((Object[]) MEGA_NODES.get(node))[1];

                if (!nodesMAP.containsKey(email)) {
                    nodesMAP.put(email, new ArrayList<>());
                }

                nodesMAP.get(email).add(node);
            }

        }

        return nodesMAP;
    }

    public static void copyTextToClipboard(String text) {

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);

    }

    public static String getNodeFullPath(String node, String email) {

        Main.MAIN_WINDOW.login(email);

        String[] f = Helpers.runProcess(new String[]{"mega-find", "--show-handles", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(f[2]) == 0) {
            final String regex = "(.+) <" + node + ">";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(f[1]);

            if (matcher.find()) {
                return "/" + matcher.group(1).trim();
            }
        }

        return null;
    }

    public static void updateComponentFont(final Component component, final Font font, final Float zoom_factor) {

        if (component != null) {

            if (component instanceof Container) {

                for (Component child : ((Container) component).getComponents()) {
                    updateComponentFont(child, font, zoom_factor);
                }
            }

            Font old_font = component.getFont();

            Font new_font = font.deriveFont(old_font.getStyle(), zoom_factor != null ? Math.round(old_font.getSize() * zoom_factor) : old_font.getSize());

            if (component instanceof JTable) {
                ((JTable) component).getTableHeader().setFont(new_font);
            }

            component.setFont(new_font);

        }
    }

    public static class JTextFieldRegularPopupMenu {

        public static void addTextActionsPopupMenuTo(JTextField txtField) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtField.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtField.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.selectAll();
                }
            };

            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtField.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();

                JMenuItem cut = new JMenuItem(cutAction);
                cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
                popup.add(cut);

                JMenuItem paste = new JMenuItem(pasteAction);
                paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
                popup.add(paste);
            }

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtField.setComponentPopupMenu(popup);
        }

        public static void addTextActionsPopupMenuTo(JTextArea txtField) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtField.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtField.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.selectAll();
                }
            };

            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtField.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();

                JMenuItem cut = new JMenuItem(cutAction);
                cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
                popup.add(cut);

                JMenuItem paste = new JMenuItem(pasteAction);
                paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
                popup.add(paste);
            }

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtField.setComponentPopupMenu(popup);
        }

        public static void addAccountsMEGAPopupMenuTo(JTextArea txtArea) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.selectAll();
                }
            };

            Action checkTMmailsAction = new AbstractAction("CHECK EMAILS FROM SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Helpers.fetchTMmailMessages(email, MEGA_ACCOUNTS.get(email));
                            }
                        });
                    }
                }
            };

            Action forceRefreshAccountAction = new AbstractAction("REFRESH (FULL) SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.refreshAccount(email, "Forced FULL REFRESH (SESSION was regenerated)", true, true);
                            }
                        });
                    }
                }
            };

            Action forceRefreshFastAccountAction = new AbstractAction("REFRESH (FAST) SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.refreshAccount(email, "Forced FAST REFRESH", true, false);
                            }
                        });
                    }
                }
            };

            Action truncateAccountAction = new AbstractAction("TRUNCATE SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {
                                Main.MAIN_WINDOW.truncateAccount(email);
                            }
                        });
                    }
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtArea.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();

                JMenuItem cut = new JMenuItem(cutAction);
                cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
                popup.add(cut);

                JMenuItem paste = new JMenuItem(pasteAction);
                paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
                popup.add(paste);
            }

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            popup.addSeparator();

            JMenuItem readEmails = new JMenuItem(checkTMmailsAction);

            readEmails.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(readEmails);

            popup.addSeparator();

            JMenuItem refreshAccount = new JMenuItem(forceRefreshAccountAction);
            refreshAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(refreshAccount);

            JMenuItem refreshFastAccount = new JMenuItem(forceRefreshFastAccountAction);

            refreshFastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            refreshFastAccount.setFont(refreshFastAccount.getFont().deriveFont(Font.BOLD));

            popup.add(refreshFastAccount);

            if (Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                Action forceRefreshLastAccountAction = new AbstractAction("REFRESH (FAST) LAST ACCOUNT -> " + Main.MAIN_WINDOW.getLast_email_force_refresh()) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                            Helpers.threadRun(() -> {
                                Main.MAIN_WINDOW.refreshAccount(Main.MAIN_WINDOW.getLast_email_force_refresh(), "Forced FAST REFRESH", true, false);
                            });
                        }
                    }
                };
                JMenuItem refreshLastAccount = new JMenuItem(forceRefreshLastAccountAction);

                refreshLastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

                refreshLastAccount.setEnabled(Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null);

                popup.add(refreshLastAccount);

            }

            popup.addSeparator();

            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);

            truncateAccount.setForeground(Color.red);

            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(truncateAccount);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtArea.setComponentPopupMenu(popup);
        }

        public static void addMainMEGAPopupMenuTo(JTextPane txtArea) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.selectAll();
                }
            };

            Action copyTransfersAction = new AbstractAction("COPY ALL COMPLETED UPLOADS LINKS") {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    Helpers.threadRun(() -> {
                        copyCompletedUploads();
                    });

                }
            };

            Action downloadMEGANodesAction = new AbstractAction("DOWNLOAD SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.downloadNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };

            Action copyInsideMEGANodesAction = new AbstractAction("COPY SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesInsideAccount(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action moveInsideMEGANodesAction = new AbstractAction("MOVE SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.moveNodesInsideAccount(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action removeMEGANodesAction = new AbstractAction("DELETE SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.removeNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action copyMEGANodesAction = new AbstractAction("COPY SELECTED FOLDERS/FILES TO ANOTHER ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesToAnotherAccount(txtArea.getSelectedText(), false);
                        });
                    }
                }
            };
            Action moveMEGANodesAction = new AbstractAction("MOVE SELECTED FOLDERS/FILES TO ANOTHER ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesToAnotherAccount(txtArea.getSelectedText(), true);
                        });
                    }
                }
            };
            Action renameMEGANodesAction = new AbstractAction("RENAME SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.renameNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action enableExportMEGANodesAction = new AbstractAction("ENABLE PUBLIC LINK ON SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.exportNodes(txtArea.getSelectedText(), true);
                        });
                    }
                }
            };
            Action disableExportMEGANodesAction = new AbstractAction("DISABLE PUBLIC LINK ON SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.exportNodes(txtArea.getSelectedText(), false);
                        });
                    }
                }
            };
            Action enableAllExportMEGANodesAction = new AbstractAction("ENABLE ALL PUBLIC LINKS ON SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "This could take quite some time.\n\n<b>CONTINUE?</b>") == 0) {
                                Main.MAIN_WINDOW.exportAllNodesInAccount(email, true, true);
                            }
                        });
                    }
                }
            };
            Action disableAllExportMEGANodesAction = new AbstractAction("DISABLE ALL PUBLIC LINKS ON SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "This could take quite some time.\n\n<b>CONTINUE?</b>") == 0) {
                                Main.MAIN_WINDOW.exportAllNodesInAccount(email, false, true);
                            }
                        });
                    }
                }
            };

            Action checkTMmailsAction = new AbstractAction("CHECK EMAILS FROM SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Helpers.fetchTMmailMessages(email, MEGA_ACCOUNTS.get(email));
                            }
                        });
                    }
                }
            };

            Action forceRefreshAccountAction = new AbstractAction("REFRESH (FULL) SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.refreshAccount(email, "Forced FULL REFRESH (SESSION was regenerated)", true, true);
                            }
                        });
                    }
                }
            };

            Action forceRefreshFastAccountAction = new AbstractAction("REFRESH (FAST) SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.refreshAccount(email, "Forced FAST REFRESH", true, false);
                            }
                        });
                    }
                }
            };

            Action truncateAccountAction = new AbstractAction("TRUNCATE SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {
                                Main.MAIN_WINDOW.truncateAccount(email);
                            }
                        });
                    }
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtArea.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();

                JMenuItem cut = new JMenuItem(cutAction);
                cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
                popup.add(cut);

                JMenuItem paste = new JMenuItem(pasteAction);
                paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
                popup.add(paste);
            }

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            popup.addSeparator();

            JMenuItem copyTransfers = new JMenuItem(copyTransfersAction);
            copyTransfers.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copyTransfers);

            popup.addSeparator();

            JMenuItem downloadNodes = new JMenuItem(downloadMEGANodesAction);

            downloadNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/download.png")));

            popup.add(downloadNodes);

            popup.addSeparator();

            JMenuItem renameNodes = new JMenuItem(renameMEGANodesAction);

            renameNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/rename.png")));

            popup.add(renameNodes);

            popup.addSeparator();

            JMenuItem copyInsideNodes = new JMenuItem(copyInsideMEGANodesAction);
            copyInsideNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));

            popup.add(copyInsideNodes);

            popup.addSeparator();

            JMenuItem moveInsideNodes = new JMenuItem(moveInsideMEGANodesAction);
            moveInsideNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/move.png")));

            popup.add(moveInsideNodes);

            popup.addSeparator();

            JMenuItem removeNodes = new JMenuItem(removeMEGANodesAction);
            removeNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(removeNodes);

            popup.addSeparator();

            JMenuItem copyNodes = new JMenuItem(copyMEGANodesAction);
            copyNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));

            popup.add(copyNodes);

            popup.addSeparator();

            JMenuItem moveNodes = new JMenuItem(moveMEGANodesAction);
            moveNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/move.png")));

            popup.add(moveNodes);

            popup.addSeparator();

            JMenuItem publicONNodes = new JMenuItem(enableExportMEGANodesAction);
            publicONNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

            popup.add(publicONNodes);

            JMenuItem publicOFFNodes = new JMenuItem(disableExportMEGANodesAction);

            publicOFFNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_off.png")));

            popup.add(publicOFFNodes);

            JMenuItem publicONNodesAll = new JMenuItem(enableAllExportMEGANodesAction);

            publicONNodesAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

            popup.add(publicONNodesAll);

            JMenuItem publicOFFNodesAll = new JMenuItem(disableAllExportMEGANodesAction);

            publicOFFNodesAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_off.png")));

            popup.add(publicOFFNodesAll);

            popup.addSeparator();

            JMenuItem publicONNodesAllEvery = new JMenuItem();

            publicONNodesAllEvery.setAction(new AbstractAction("ENABLE ALL PUBLIC LINKS ON EVERY ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    publicONNodesAllEvery.setEnabled(false);

                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled()) {

                        Helpers.threadRun(() -> {

                            if (Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "<b>WARNING: ENABLING ALL PUBLIC LINKS ON EVERY ACCOUNT COULD TAKE A VERY VERY VERY LONG TIME</b>.\n\n<span color='red'><b>CONTINUE?</b></span>") == 0) {

                                for (String email : MEGA_ACCOUNTS.keySet()) {
                                    Main.MAIN_WINDOW.exportAllNodesInAccount(email, true, false);
                                }

                                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL PUBLIC LINKS ON EVERY ACCOUNT ENABLED");
                            }

                            Helpers.GUIRun(() -> {
                                publicONNodesAllEvery.setEnabled(true);
                            });
                        });
                    }
                }
            });

            publicONNodesAllEvery.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

            popup.add(publicONNodesAllEvery);

            popup.addSeparator();

            JMenuItem readEmails = new JMenuItem(checkTMmailsAction);

            readEmails.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(readEmails);

            popup.addSeparator();

            JMenuItem refreshAccount = new JMenuItem(forceRefreshAccountAction);

            refreshAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(refreshAccount);

            JMenuItem refreshFastAccount = new JMenuItem(forceRefreshFastAccountAction);

            refreshFastAccount.setFont(refreshFastAccount.getFont().deriveFont(Font.BOLD));

            refreshFastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(refreshFastAccount);

            if (Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                Action forceRefreshLastAccountAction = new AbstractAction("REFRESH (FAST) LAST ACCOUNT -> " + Main.MAIN_WINDOW.getLast_email_force_refresh()) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                            Helpers.threadRun(() -> {
                                Main.MAIN_WINDOW.refreshAccount(Main.MAIN_WINDOW.getLast_email_force_refresh(), "Forced FAST REFRESH", true, false);
                            });
                        }
                    }
                };
                JMenuItem refreshLastAccount = new JMenuItem(forceRefreshLastAccountAction);

                refreshLastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

                refreshLastAccount.setEnabled(Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null);

                popup.add(refreshLastAccount);

            }

            popup.addSeparator();

            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);

            truncateAccount.setForeground(Color.red);

            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(truncateAccount);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtArea.setComponentPopupMenu(popup);
        }

        public static void addLiteMEGAAccountPopupMenuTo(JTextArea txtArea, Refresheable r) {
            JPopupMenu popup = new JPopupMenu();
            Refresheable _refresh = r;

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.selectAll();
                }
            };

            Action removeMEGANodesAction = new AbstractAction("DELETE SELECTED FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            _refresh.enableR(false);
                            Main.MAIN_WINDOW.removeNodes(txtArea.getSelectedText());
                            _refresh.enableR(true);
                            _refresh.refresh();
                        });
                    }
                }
            };

            Action truncateAccountAction = new AbstractAction("TRUNCATE SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {
                                _refresh.enableR(false);
                                Main.MAIN_WINDOW.truncateAccount(email);
                                _refresh.enableR(true);
                                _refresh.refresh();
                            }

                        });
                    }
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtArea.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();

                JMenuItem cut = new JMenuItem(cutAction);
                cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
                popup.add(cut);

                JMenuItem paste = new JMenuItem(pasteAction);
                paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
                popup.add(paste);
            }

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            popup.addSeparator();

            JMenuItem removeNodes = new JMenuItem(removeMEGANodesAction);

            removeNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(removeNodes);

            popup.addSeparator();

            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);

            truncateAccount.setForeground(Color.red);

            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(truncateAccount);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtArea.setComponentPopupMenu(popup);
        }

        public static void addTransferencePopupMenuTo(Transference transference) {
            JPopupMenu popup = new JPopupMenu();

            Action copyPublicLinkAction = new AbstractAction("COPY PUBLIC LINK") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (transference.isFinished()) {
                        File f = new File(transference.getLpath());
                        Helpers.threadRun(() -> {
                            Helpers.copyTextToClipboard(transference.getPublic_link() != null ? f.getName() + "   [" + transference.getEmail() + "]   " + transference.getPublic_link() : "");
                            Helpers.mostrarMensajeInformativo(Main.MAIN_WINDOW, "<b>" + f.getName() + "</b><br>" + transference.getPublic_link() + "<br><br>COPIED TO CLIPBOARD");
                        });
                    }
                }
            };

            Action cancelTransferenceLinkAction = new AbstractAction("CANCEL TRANSFERENCE") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    File f = new File(transference.getLpath());
                    if (!transference.isFinished() && !transference.isCanceled() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + f.getName() + "</b><br><br><b>CANCEL</b> this transference?") == 0) {
                        Helpers.threadRun(() -> {
                            transference.stop();
                        });
                    }
                }
            };

            Action cancelAndRetryTransferenceLinkAction = new AbstractAction("RESET TRANSFERENCE") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    File f = new File(transference.getLpath());
                    if (!transference.isFinished() && !transference.isCanceled() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + f.getName() + "</b><br><br><b>RESET</b> this transference?") == 0) {
                        Helpers.threadRun(() -> {
                            transference.stopAndRetry();
                        });
                    }
                }
            };

            Action clearTransferenceLinkAction = new AbstractAction("CLEAR TRANSFERENCE") {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    transference.clearTransference();
                }
            };

            Action retryTransferenceLinkAction = new AbstractAction("RETRY TRANSFERENCE") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    Helpers.threadRun(() -> {
                        transference.retry();
                    });

                }
            };

            if (!transference.isFinishing() && !transference.isFinished() && !transference.isCanceled()) {

                JMenuItem cancelTransference = new JMenuItem(cancelTransferenceLinkAction);

                cancelTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cancel.png")));

                popup.add(cancelTransference);

                JMenuItem cancelAndRetryTransference = new JMenuItem(cancelAndRetryTransferenceLinkAction);

                cancelAndRetryTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

                popup.add(cancelAndRetryTransference);

            } else {

                JMenuItem clearTransference = new JMenuItem(clearTransferenceLinkAction);

                clearTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/clear.png")));

                popup.add(clearTransference);

                if (transference.getPublic_link() != null) {

                    popup.addSeparator();

                    JMenuItem copyPublicLink = new JMenuItem(copyPublicLinkAction);

                    copyPublicLink.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

                    popup.add(copyPublicLink);

                }

                if (transference.isError() || transference.isCanceled()) {

                    popup.addSeparator();

                    JMenuItem retryTransference = new JMenuItem(retryTransferenceLinkAction);

                    retryTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

                    popup.add(retryTransference);
                }
            }

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            transference.getMain_panel().setComponentPopupMenu(popup);

        }

        private JTextFieldRegularPopupMenu() {
        }
    }

}
