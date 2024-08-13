/*
 __  __ _____ ____    _    ____   ___   ____ _____ ___  ____
|  \/  | ____/ ___|  / \  |  _ \ / _ \ / ___|_   _/ _ \|  _ \
| |\/| |  _|| |  _  / _ \ | | | | | | | |     | || | | | |_) |
| |  | | |__| |_| |/ ___ \| |_| | |_| | |___  | || |_| |  _ <
|_|  |_|_____\____/_/   \_\____/ \___/ \____| |_| \___/|_| \_\

by tonikelope

 */
package com.tonikelope.megadoctor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.BadLocationException;

/**
 *
 * @author tonikelope
 */
public class Main extends javax.swing.JFrame {

    public final static String VERSION = "3.10";
    public final static int MESSAGE_DIALOG_FONT_SIZE = 20;
    public final static int MEGADOCTOR_ONE_INSTANCE_PORT = 32856;
    public final static ThreadPoolExecutor THREAD_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public final static String MEGA_CMD_URL = "https://mega.io/cmd";
    public final static String MEGA_CMD_WINDOWS_PATH = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\MEGAcmd";
    public final static String MEGADOCTOR_DIR = System.getProperty("user.home") + File.separator + ".megadoctor";
    public final static String MEGADOCTOR_MISC_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_misc";
    public final static String SESSIONS_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_sessions";
    public final static String ACCOUNTS_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_accounts";
    public final static String FILE_SPLITTER_TASKS_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_file_splitter";
    public final static String EXCLUDED_ACCOUNTS_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_excluded_accounts";
    public final static String TRANSFERS_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_transfers";
    public final static String NODES_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_nodes";
    public final static String FREE_SPACE_CACHE_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_free_space_cache";
    public final static String LOG_FILE = MEGADOCTOR_DIR + File.separator + "megadoctor_log";
    public final static Object TRANSFERENCES_LOCK = new Object();
    public volatile static ServerSocket ONE_INSTANCE_SOCKET = null;
    public volatile static boolean EXIT = false;

    public final static ConcurrentHashMap<Component, Transference> TRANSFERENCES_MAP = new ConcurrentHashMap<>();
    public final static Object FILE_SPLITTER_LOCK = new Object();

    public volatile static Main MAIN_WINDOW;
    public volatile static String MEGA_CMD_VERSION = null;
    public volatile static ConcurrentHashMap<String, Object> MEGADOCTOR_MISC = new ConcurrentHashMap<>();
    public volatile static LinkedHashMap<String, String> MEGA_ACCOUNTS = new LinkedHashMap<>();
    public volatile static ConcurrentLinkedQueue<String> MEGA_EXCLUDED_ACCOUNTS = new ConcurrentLinkedQueue<>();
    public volatile static HashMap<String, String> MEGA_SESSIONS = new HashMap<>();
    public volatile static ConcurrentHashMap<String, Object[]> MEGA_NODES = new ConcurrentHashMap<>();
    public volatile static ConcurrentHashMap<String, Long> FREE_SPACE_CACHE = new ConcurrentHashMap<>();
    public volatile static ConcurrentLinkedQueue<Object[]> FILE_SPLITTER_TASKS = new ConcurrentLinkedQueue<>();

    private final DragMouseAdapter _transfer_drag_drop_adapter = new DragMouseAdapter(TRANSFERENCES_LOCK);
    private volatile boolean _running_main_action = false;
    private volatile boolean _running_global_check = false;
    private volatile boolean _aborting_global_check = false;
    private volatile boolean _closing = false;
    private volatile boolean _firstAccountsTextareaClick = false;
    private volatile MoveNodeToAnotherAccountDialog _email_dialog = null;
    private volatile MoveNodeDialog _move_dialog = null;
    private volatile Transference _current_transference = null;
    private volatile String _last_email_force_refresh = null;
    private volatile JPanel transferences = null;
    private volatile boolean _check_only_new = true;
    private volatile boolean _pausing_transference = false;
    private volatile boolean _transferences_paused = false;
    private volatile boolean _provisioning_upload = false;

    public void output_textarea_append(String msg) {
        try {
            output_textarea.getStyledDocument().insertString(output_textarea.getStyledDocument().getLength(), msg, null);
        } catch (BadLocationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public boolean isProvisioning_upload() {
        return _provisioning_upload;
    }

    public boolean isPausing_transference() {
        return _pausing_transference;
    }

    public boolean isTransferences_paused() {
        return _transferences_paused;
    }

    public void setTransferences_paused(boolean _transferences_paused) {
        this._transferences_paused = _transferences_paused;
    }

    public JButton getUpload_button() {
        return upload_button;
    }

    public JButton getPause_button() {
        return pause_button;
    }

    public JButton getCancel_all_button() {
        return cancel_all_button;
    }

    public boolean busy() {
        return isRunning_global_check() || isRunning_main_action() || isSomeTransference_running();
    }

    public boolean isRunning_global_check() {
        return _running_global_check;
    }

    public boolean isAborting_global_check() {
        return _aborting_global_check;
    }

    public boolean isRunning_main_action() {
        return _running_main_action;
    }

    public boolean isSomeTransference_running() {

        return (_current_transference != null && !_current_transference.isFinished() && !_current_transference.isCanceled() && !_current_transference.isRetry());

    }

    public String getLast_email_force_refresh() {
        return _last_email_force_refresh;
    }

    public JTextArea getCuentas_textarea() {
        return cuentas_textarea;
    }

    public JTextPane getOutput_textarea() {
        return output_textarea;
    }

    public JProgressBar getProgressbar() {
        return progressbar;
    }

    public JButton getSave_button() {
        return save_button;
    }

    public JLabel getStatus_label() {
        return status_label;
    }

    public JButton getVamos_button() {
        return vamos_button;
    }

    public JPanel getTransferences() {
        return transferences;
    }

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        Helpers.JTextFieldRegularPopupMenu.addAccountsMEGAPopupMenuTo(cuentas_textarea);
        Helpers.JTextFieldRegularPopupMenu.addMainMEGAPopupMenuTo(output_textarea);
        transferences = new JPanel();
        transferences.setLayout(new BoxLayout(transferences, BoxLayout.Y_AXIS));
        transferences.addMouseListener((MouseListener) _transfer_drag_drop_adapter);
        transferences.addMouseMotionListener((MouseMotionListener) _transfer_drag_drop_adapter);
        transferences_panel.add(transferences);
        transferences_control_panel.setVisible(false);
        progressbar.setMinimum(0);
        getUpload_button().setEnabled(false);
        new_account_button.setEnabled(false);
        transf_scroll.getVerticalScrollBar().setUnitIncrement(20);
        transf_scroll.getHorizontalScrollBar().setUnitIncrement(20);
        setTitle("MegaDoctor " + VERSION + " - MEGAcmd's best friend");
        progressbar.setIndeterminate(true);
        status_label.setText("Loading data (be patient)...");
        Color bgColor = new Color(102, 102, 102);
        Color fgColor = Color.WHITE;
        UIDefaults defaults = new UIDefaults();
        defaults.put("TextPane.background", new ColorUIResource(bgColor));
        defaults.put("TextPane[Enabled].backgroundPainter", bgColor);
        defaults.put("TextPane.foreground", new ColorUIResource(fgColor));
        output_textarea.putClientProperty("Nimbus.Overrides", defaults);
        output_textarea.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        output_textarea.setBackground(bgColor);
        pack();
        setEnabled(false);
    }

    private void runMEGACMDCHecker() {

        Helpers.GUIRun(() -> {
            status_label.setText("Checking MEGA-CMD...");
        });

        MEGA_CMD_VERSION = Helpers.runProcess(new String[]{"mega-version"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, false, Pattern.compile("MEGAcmd version:"))[1];

        if (MEGA_CMD_VERSION == null || "".equals(MEGA_CMD_VERSION)) {
            Helpers.mostrarMensajeError(this, "MEGA CMD IS REQUIRED");
            Helpers.openBrowserURLAndWait(MEGA_CMD_URL);
            if (ONE_INSTANCE_SOCKET != null) {
                try {
                    ONE_INSTANCE_SOCKET.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            Main.EXIT = true;

            Helpers.destroyAllExternalProcesses();

            System.exit(1);
        }

        if (!MEGADOCTOR_MISC.containsKey("mega_cmd_installed")) {

            Helpers.mostrarMensajeInformativo(this, "MEGAcmd IS CORRECTLY INSTALLED 😃\n\nRemember that to avoid possible failures, <b>YOU MUST NOT USE MEGAcmd WHILE USING MegaDoctor.</b>");

            MEGADOCTOR_MISC.put("mega_cmd_installed", true);

            saveMISC();
        }

        Helpers.GUIRunAndWait(() -> {
            setEnabled(true);
            progressbar.setIndeterminate(false);
            status_label.setText("");
            enableTOPControls(true);
        });

    }

    private Transference findSplitTransference(String path) {

        synchronized (TRANSFERENCES_LOCK) {

            for (Map.Entry<Component, Transference> entry : TRANSFERENCES_MAP.entrySet()) {

                Transference t = entry.getValue();

                if (t.getLpath().replaceAll("\\.part[0-9]+-[0-9]+$", "").equals(path) && !t.isFinished()) {
                    return t;
                }
            }
        }

        return null;
    }

    private void runFileSplitter() {

        loadFileSplitterTasks();

        Helpers.threadRun(() -> {

            while (!_closing) {

                if (!FILE_SPLITTER_TASKS.isEmpty()) {

                    saveFileSplitterTasks();

                    try {
                        Object[] task = (Object[]) FILE_SPLITTER_TASKS.peek();

                        Transference t = findSplitTransference((String) task[0]);

                        if (t != null) {

                            boolean delete_after_split = (boolean) task[2];

                            String file_path = (String) task[0];

                            Long file_size = Files.size(Paths.get(file_path));

                            Long chunk_size = (Long) task[1];

                            final int tot_chunks = (int) Math.ceil((float) file_size / chunk_size);

                            Integer part = (Integer) task[3];

                            if (part != null) {

                                int i = part - 1;

                                long current_chunk_size = Math.min(chunk_size, file_size - chunk_size * i);

                                Path fileName = Paths.get(file_path + ".part" + String.valueOf(i + 1) + "-" + String.valueOf(tot_chunks));

                                try (RandomAccessFile sourceFile = new RandomAccessFile(file_path, "r"); FileChannel sourceChannel = sourceFile.getChannel()) {

                                    if (!(Files.exists(fileName) && Files.size(fileName) == current_chunk_size)) {

                                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter PART " + String.valueOf(i + 1) + " " + Helpers.formatBytes(current_chunk_size) + " " + task[0]);

                                        long source_offset = chunk_size * i;

                                        long dest_bytes_copied = Files.exists(fileName) ? Files.size(fileName) : 0;

                                        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw"); FileChannel toChannel = toFile.getChannel();) {

                                            while (dest_bytes_copied < current_chunk_size) {

                                                sourceChannel.position(source_offset + dest_bytes_copied);

                                                dest_bytes_copied += toChannel.transferFrom(sourceChannel, dest_bytes_copied, current_chunk_size - dest_bytes_copied);
                                            }

                                            toChannel.force(true);
                                        }

                                    } else {
                                        Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter PART " + String.valueOf(i + 1) + " EXISTS (SKIPPING)" + task[0]);
                                    }

                                }

                            } else {

                                Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter splitting file" + task[0]);

                                for (int i = 0; i < tot_chunks && !t.isFinished(); i++) {

                                    long current_chunk_size = Math.min(chunk_size, file_size - chunk_size * i);

                                    try (RandomAccessFile sourceFile = new RandomAccessFile(file_path, "r"); FileChannel sourceChannel = sourceFile.getChannel()) {

                                        Path fileName = Paths.get(file_path + ".part" + String.valueOf(i + 1) + "-" + String.valueOf(tot_chunks));

                                        if (!(Files.exists(fileName) && Files.size(fileName) == current_chunk_size)) {

                                            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter PART " + String.valueOf(i + 1) + " " + Helpers.formatBytes(current_chunk_size) + " " + task[0]);

                                            long source_offset = chunk_size * i;

                                            long dest_bytes_copied = Files.exists(fileName) ? Files.size(fileName) : 0;

                                            try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw"); FileChannel toChannel = toFile.getChannel();) {

                                                while (dest_bytes_copied < current_chunk_size) {

                                                    sourceChannel.position(source_offset + dest_bytes_copied);

                                                    dest_bytes_copied += toChannel.transferFrom(sourceChannel, dest_bytes_copied, current_chunk_size - dest_bytes_copied);
                                                }

                                                toChannel.force(true);
                                            }

                                        } else {
                                            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter PART " + String.valueOf(i + 1) + " EXISTS (SKIPPING)" + task[0]);
                                        }

                                    }

                                }

                            }

                            if (delete_after_split) {
                                Files.deleteIfExists(Paths.get(file_path));
                            }

                        } else {
                            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "FileSplitter transference not found: " + task[0]);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    FILE_SPLITTER_TASKS.poll();

                    saveFileSplitterTasks();

                } else {

                    synchronized (FILE_SPLITTER_LOCK) {
                        try {
                            FILE_SPLITTER_LOCK.wait(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

            }

        });
    }

    private void runTransferenceWatchdog() {
        Helpers.threadRun(() -> {

            while (!_closing) {

                synchronized (TRANSFERENCES_LOCK) {

                    if (!isSomeTransference_running()) {
                        _current_transference = null;
                    }

                    Helpers.GUIRunAndWait(() -> {

                        if (!_transfer_drag_drop_adapter.isWorking()) {

                            if (transferences.getComponentCount() > 0) {

                                if (!isSomeTransference_running()) {

                                    transferences_control_panel.setVisible(true);

                                    for (Component tr : transferences.getComponents()) {

                                        Transference t = TRANSFERENCES_MAP.get(tr);

                                        if (isSomeTransference_running()) {
                                            break;
                                        }

                                        if (!t.isRunning() && !t.isFinished() && !t.isFinishing() && !t.isCanceled()) {

                                            if (t.getSplit_file() != null) {

                                                if (!t.isSplitting() && !t.isSplit_finished()) {

                                                    t.start();

                                                } else if (_current_transference != t) {
                                                    _current_transference = t;

                                                    t.setSplitting(false);

                                                    synchronized (t.getSplit_lock()) {
                                                        t.getSplit_lock().notify();
                                                    }

                                                    break;
                                                }

                                            } else if (_current_transference != t) {

                                                _current_transference = t;

                                                t.start();

                                                break;
                                            }
                                        }

                                    }

                                }

                                vamos_button.setEnabled(!busy() || (isRunning_global_check() && !isAborting_global_check()));

                                cuentas_textarea.setEnabled(!busy());

                                new_account_button.setEnabled(getUpload_button().isEnabled());

                                purge_cache_menu.setEnabled(!busy());

                                getPause_button().setVisible(isSomeTransference_running());

                                getCancel_all_button().setVisible(isSomeTransference_running());

                            } else {

                                _pausing_transference = false;

                                _transferences_paused = false;

                                _current_transference = null;

                                getPause_button().setText("PAUSE");

                                transferences_control_panel.setVisible(false);

                                vamos_button.setEnabled(!busy() || (isRunning_global_check() && !isAborting_global_check()));

                                cuentas_textarea.setEnabled(!busy());

                                new_account_button.setEnabled(getUpload_button().isEnabled());

                                purge_cache_menu.setEnabled(!busy());
                            }

                        }

                    });

                    try {
                        TRANSFERENCES_LOCK.wait(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

        });
    }

    public void init() {
        Helpers.threadRun(() -> {
            loadMISC();
            loadLog();
            runMEGACMDCHecker();
            loadAccountsAndTransfers();
            runTransferenceWatchdog();
        });
    }

    public boolean login(String email) {

        if (Helpers.megaWhoami().equals(email.toLowerCase())) {
            return true;
        }

        logout(true);

        String session = MEGA_SESSIONS.get(email);

        String password = MEGA_ACCOUNTS.get(email);

        if (session != null) {

            Helpers.GUIRun(() -> {
                status_label.setForeground(new Color(0, 153, 0));
            });

            String[] login_session_output = Helpers.runProcess(new String[]{"mega-login", MEGA_SESSIONS.get(email)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null);

            if (login_session_output[1].contains("security needs upgrading")) {

                Helpers.GUIRun(() -> {
                    status_label.setForeground(Color.MAGENTA);
                });

                Helpers.runProcess(new String[]{"mega-confirm", "--security"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            if (Integer.parseInt(login_session_output[2]) != 0) {

                Helpers.GUIRun(() -> {
                    status_label.setForeground(Color.WHITE);
                });

                String[] login = Helpers.runProcess(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(password)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null);

                if (login[1].contains("security needs upgrading")) {

                    Helpers.GUIRun(() -> {
                        status_label.setForeground(Color.MAGENTA);
                    });

                    Helpers.runProcess(new String[]{"mega-confirm", "--security"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                }

                if (Integer.parseInt(login[2]) != 0) {

                    Helpers.GUIRun(() -> {
                        status_label.setForeground(Color.BLACK);
                    });
                    return false;
                }
            }

        } else {

            Helpers.GUIRun(() -> {
                status_label.setForeground(Color.BLUE);
            });

            String[] login = Helpers.runProcess(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(password)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, true, null);

            if (login[1].contains("security needs upgrading")) {

                Helpers.GUIRun(() -> {
                    status_label.setForeground(Color.MAGENTA);
                });

                Helpers.runProcess(new String[]{"mega-confirm", "--security"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            if (Integer.parseInt(login[2]) != 0) {

                Helpers.GUIRun(() -> {
                    status_label.setForeground(Color.BLACK);
                });
                return false;
            }
        }

        Helpers.runProcess(new String[]{"mega-killsession", "-a"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        MEGA_SESSIONS.remove(email);

        MEGA_SESSIONS.put(email, getCurrentSessionID());

        if (isSomeTransference_running() && _current_transference.getEmail().equals(email) && _current_transference.isPaused()) {
            _current_transference.pause();
        }

        Helpers.GUIRun(() -> {
            status_label.setForeground(Color.BLACK);
        });

        return true;
    }

    public String getAccountStatistics(String email) {

        if (login(email)) {

            String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            String du = DUWithHandles();

            String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            String[] shared = Helpers.runProcess(new String[]{"mega-share", "/"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            return df + "\n" + du + "\n" + ls.replace("shared as exported permanent file link: ", "").replace("shared as exported permanent folder link: ", "") + (Integer.parseInt(shared[2]) == 0 ? "\n" + shared[1] : "");
        } else {
            return null;
        }
    }

    public String DUWithHandles() {

        String ls = Helpers.runProcess(new String[]{"mega-ls", "/", "--show-handles"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        int max_path_width = 0;

        for (String s : ls.split("\n")) {
            if (s.length() > max_path_width) {
                max_path_width = s.length();
            }
        }

        String du = Helpers.runProcess(new String[]{"mega-du", "-h", "--use-pcre", "/.*", "--path-display-size=" + String.valueOf(Math.max(50, max_path_width + 1))}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        if (!du.isBlank() && !ls.isBlank()) {

            final String regex = "(.+) <H:[^>]+>";

            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

            final Matcher matcher = pattern.matcher(ls);

            while (matcher.find()) {

                final Pattern pattern2 = Pattern.compile("(^/)" + Pattern.quote(matcher.group(1)) + "(:[^:/]+)$", Pattern.MULTILINE);

                final Matcher matcher2 = pattern2.matcher(du);

                du = matcher2.replaceAll("$1" + matcher.group(0) + "$2");
            }

            String[] du_lines = du.split("\n");

            du_lines[du_lines.length - 2] += "-".repeat(13);

            du_lines[0] = du_lines[0].replaceAll("( +)", "$1" + " ".repeat(13));

            du_lines[du_lines.length - 1] = du_lines[du_lines.length - 1].replaceAll("(used:)( +)(\\d)", "$1" + " ".repeat(13) + "$2$3");

            return String.join("\n", du_lines);
        } else {
            return "";
        }
    }

    public void logout(boolean keep_session) {
        logout(keep_session, null);
    }

    public void logout(boolean keep_session, String email) {
        if (keep_session) {
            Helpers.runProcess(new String[]{"mega-logout", "--keep-session"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
        } else {
            Helpers.runProcess(new String[]{"mega-logout"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            if (email != null) {
                MEGA_SESSIONS.remove(email);
            }
        }
    }

    private String getCurrentSessionID() {

        String session_output = Helpers.runProcess(new String[]{"mega-session"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return session_output.replaceAll("^.+: +(.+)$", "$1").trim();
    }

    private void enableTOPControls(boolean enable) {
        Helpers.GUIRun(() -> {
            MAIN_WINDOW.getCuentas_textarea().setEnabled(enable);
            clear_log_button.setEnabled(enable);
            check_only_new_checkbox.setEnabled(enable);
            getVamos_button().setEnabled(enable || isRunning_global_check());
            getUpload_button().setEnabled(enable && !isPausing_transference() && !isProvisioning_upload());
            getSave_button().setEnabled(enable);
            new_account_button.setEnabled(enable);
            options_menu.setEnabled(enable);
        });
    }

    public void copyNodesToAnotherAccount(String text, final boolean move) {

        _running_main_action = true;

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText((move ? "MOVING" : "COPYING") + " SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToCopy = Helpers.extractNodeMapFromText(text);

        if (!nodesToCopy.isEmpty() && MEGA_ACCOUNTS.size() > nodesToCopy.keySet().size()) {

            if (isSomeTransference_running()) {
                _current_transference.pause();
            }

            Helpers.GUIRunAndWait(() -> {
                _email_dialog = new MoveNodeToAnotherAccountDialog(MAIN_WINDOW, true, nodesToCopy.keySet(), move, Helpers.getNodeMapTotalSize(nodesToCopy));

                _email_dialog.setLocationRelativeTo(MAIN_WINDOW);

                _email_dialog.setVisible(true);
            });

            if (isSomeTransference_running()) {
                Helpers.threadRun(() -> {
                    _current_transference.resume();
                });
            }

            String email = _email_dialog.getSelected_email();

            if (_email_dialog.isOk() && email != null && !email.isBlank()) {

                if (Helpers.getNodeMapTotalSize(nodesToCopy) <= Helpers.getAccountFreeSpace(email)) {

                    ArrayList<String[]> exported_links = new ArrayList<>();

                    for (String e : nodesToCopy.keySet()) {

                        ArrayList<String> node_list = nodesToCopy.get(e);

                        ArrayList<String> export_command = new ArrayList<>();

                        export_command.add("mega-export");

                        export_command.add("-fa");

                        export_command.addAll(node_list);

                        login(e);

                        String exported_links_output = Helpers.runProcess(export_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                        final String regex2 = "Exported +(.*?): +(https://.+)";

                        final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);

                        final Matcher matcher2 = pattern2.matcher(exported_links_output);

                        while (matcher2.find()) {

                            exported_links.add(new String[]{matcher2.group(1), matcher2.group(2)});

                        }

                    }

                    login(email);

                    for (String[] s : exported_links) {

                        String folder = s[0].replaceAll("^(.*/)[^/]*$", "$1");

                        Helpers.runProcess(new String[]{"mega-mkdir", "-p", folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        Helpers.runProcess(new String[]{"mega-import", s[1], folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                    }

                    refreshAccount(email, "Refreshed after insertion", false, false);

                    if (move) {

                        for (String email_rm : nodesToCopy.keySet()) {

                            ArrayList<String> node_list = nodesToCopy.get(email_rm);

                            ArrayList<String> delete_command = new ArrayList<>();

                            delete_command.add("mega-rm");

                            delete_command.add("-rf");

                            delete_command.addAll(node_list);

                            login(email_rm);

                            Helpers.runProcess(delete_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            refreshAccount(email_rm, "Refreshed after deletion", false, false);
                        }

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES MOVED");

                    } else {

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES COPIED");
                    }
                } else {
                    Helpers.mostrarMensajeError(MAIN_WINDOW, "THERE IS NO ENOUGH FREE SPACE IN\n<b>" + email + "</b>");
                }
            }

            logout(true);

            _email_dialog = null;

        } else if (nodesToCopy.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:XXXXXXXX MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void bye() {

        _closing = true;

        Helpers.threadRun(() -> {

            Helpers.GUIRun(() -> {

                setEnabled(false);
            });

            if (isSomeTransference_running()) {
                _current_transference.pause();
            } else {
                Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            String cuentas_text = cuentas_textarea.getText();

            final String regex = "(.*?)#(.+)";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(cuentas_text);

            LinkedHashMap<String, String> new_accounts = new LinkedHashMap<>();

            while (matcher.find()) {
                if (!MEGA_ACCOUNTS.containsKey(matcher.group(1))) {
                    new_accounts.put(matcher.group(1), matcher.group(2));
                }
            }

            ArrayList<String> remove = new ArrayList<>();

            for (String email : MEGA_ACCOUNTS.keySet()) {

                if (!cuentas_text.contains(email)) {
                    remove.add(email);
                }
            }

            if (_firstAccountsTextareaClick && (!new_accounts.isEmpty() || !remove.isEmpty()) && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "Changes detected in accounts list, do you want to save?") == 0) {

                for (String email : new_accounts.keySet()) {
                    MEGA_ACCOUNTS.put(email, new_accounts.get(email));
                }

                for (String email : remove) {
                    MEGA_ACCOUNTS.remove(email);
                    MEGA_SESSIONS.remove(email);
                }
            }

            if (_firstAccountsTextareaClick && !MEGA_ACCOUNTS.isEmpty() && (session_menu.isSelected() || Helpers.mostrarMensajeInformativoSINO(this, "Do you want to save your MEGA accounts/sessions/transfers to disk to speed up next time?\n\n(If you are using a public computer it is NOT recommended to do so for security reasons).") == 0)) {
                synchronized (TRANSFERENCES_LOCK) {
                    Helpers.GUIRunAndWait(() -> {
                        progressbar.setIndeterminate(true);

                        if (transferences.getComponentCount() > 0) {
                            ArrayList<String> links = new ArrayList<>();

                            for (Component c : transferences.getComponents()) {

                                Transference t = TRANSFERENCES_MAP.get(c);
                                if (t.isFinished()) {

                                    String filename = new File(t.getLpath()).getName();
                                    if (!t.isCanceled() && !t.isError()) {

                                        links.add(t.getLpath() + " -> " + t.getRpath() + " " + (t.getRemote_handle() != null ? " <" + t.getRemote_handle() + ">" : "") + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   " + (t.getPublic_link() != null ? t.getPublic_link() : ""));

                                    } else {

                                        links.add("[ERROR/CANCELED] " + t.getLpath() + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   ");

                                    }
                                }
                            }

                            if (!links.isEmpty()) {
                                Collections.sort(links);
                                output_textarea_append("\n\nTRANSFERENCES CLEARED (on exit):\n\n" + String.join("\n", links) + "\n\n");
                            }
                        }

                    });
                }

                saveMISC();
                saveAccounts();
                saveTransfers();
                saveLog();
                logout(true);
            } else {
                removeSessionFILES();
                logout(false);
            }

            if (ONE_INSTANCE_SOCKET != null) {
                try {
                    ONE_INSTANCE_SOCKET.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            Main.EXIT = true;

            Helpers.destroyAllExternalProcesses();

            System.exit(0);

        });

    }

    public void removeSessionFILES() {
        try {
            Files.deleteIfExists(Paths.get(ACCOUNTS_FILE));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Files.deleteIfExists(Paths.get(SESSIONS_FILE));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Files.deleteIfExists(Paths.get(TRANSFERS_FILE));
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveFileSplitterTasks() {

        synchronized (FILE_SPLITTER_LOCK) {
            try (FileOutputStream fos = new FileOutputStream(FILE_SPLITTER_TASKS_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                oos.writeObject(FILE_SPLITTER_TASKS);

            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void loadFileSplitterTasks() {
        if (Files.exists(Paths.get(FILE_SPLITTER_TASKS_FILE))) {
            try (FileInputStream fis = new FileInputStream(FILE_SPLITTER_TASKS_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                FILE_SPLITTER_TASKS = (ConcurrentLinkedQueue<Object[]>) ois.readObject();

            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                try {
                    Files.deleteIfExists(Paths.get(FILE_SPLITTER_TASKS_FILE));
                } catch (IOException ex1) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }

    public void saveTransfers() {

        synchronized (TRANSFERENCES_LOCK) {
            Helpers.GUIRunAndWait(() -> {
                if (session_menu.isSelected() && transferences.getComponentCount() > 0) {

                    final ArrayList<Object[]> trans = new ArrayList<>();

                    for (Component c : transferences.getComponents()) {

                        Transference t = TRANSFERENCES_MAP.get(c);

                        if (!t.isFinished() && !t.isCanceled()) {

                            String email = t.getEmail();

                            String lpath = t.getLpath();

                            String rpath = t.getRpath();

                            int action = t.getAction();

                            boolean remove_after = t.isRemove_after();

                            Long split_file = t.getSplit_file();

                            trans.add(new Object[]{email, lpath, rpath, action, remove_after, split_file});
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(TRANSFERS_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                        oos.writeObject(trans);

                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

                    }

                } else {

                    try {
                        Files.deleteIfExists(Paths.get(TRANSFERS_FILE));
                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            });
        }

        saveFileSplitterTasks();
    }

    public void loadTransfers() {
        if (Files.exists(Paths.get(TRANSFERS_FILE))) {
            try (FileInputStream fis = new FileInputStream(TRANSFERS_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {

                ArrayList<Object[]> trans = (ArrayList<Object[]>) ois.readObject();

                ArrayList<Transference> valid_trans = new ArrayList<>();
                synchronized (TRANSFERENCES_LOCK) {

                    if (!trans.isEmpty()) {

                        Helpers.GUIRunAndWait(() -> {
                            try {

                                for (Object[] o : trans) {
                                    if (MEGA_SESSIONS.containsKey((String) o[0])) {
                                        Transference t = new Transference((String) o[0], (String) o[1], (String) o[2], (int) o[3], (boolean) o[4], (Long) o[5]);
                                        TRANSFERENCES_MAP.put(transferences.add(t), t);
                                        valid_trans.add(t);
                                    }
                                }

                                tabbed_panel.setSelectedIndex(1);
                                vamos_button.setEnabled(false);
                                cuentas_textarea.setEnabled(false);
                                transferences_control_panel.setVisible(!TRANSFERENCES_MAP.isEmpty());

                                tabbed_panel.revalidate();
                                tabbed_panel.repaint();

                            } catch (Exception ex) {
                                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });

                        if (!valid_trans.isEmpty()) {
                            Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                        }
                    }

                    TRANSFERENCES_LOCK.notifyAll();

                }

            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void copyNodesInsideAccount(String text) {

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("COPYING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToCopy = Helpers.extractNodeMapFromText(text);

        boolean cancel = false;

        if (!nodesToCopy.isEmpty()) {

            outer:
            for (String email : nodesToCopy.keySet()) {

                login(email);

                ArrayList<String> node_list = nodesToCopy.get(email);

                if (Helpers.getNodeListTotalSize(node_list) <= Helpers.getAccountFreeSpace(email)) {

                    Helpers.GUIRunAndWait(() -> {
                        _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, null, 1, email);

                        _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                        _move_dialog.setVisible(true);
                    });

                    if (_move_dialog.isOk()) {

                        int conta = 0;

                        for (String node : node_list) {

                            String old_full_path = Helpers.getNodeFullPath(node, email);

                            String old_n = old_full_path.replaceAll("^.*/([^/]*)$", "$1");

                            String new_full_path = _move_dialog.getNew_name().getText().trim() + old_n;

                            if (_move_dialog.isOk() && !old_full_path.equals(new_full_path) && !new_full_path.isBlank()) {

                                String folder = new_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                                Helpers.runProcess(new String[]{"mega-mkdir", "-p", folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                                Helpers.runProcess(new String[]{"mega-cp", node, new_full_path}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                                conta++;

                            } else if (!_move_dialog.isOk()) {
                                cancel = true;
                                break outer;
                            }
                        }

                        if (conta > 0) {

                            refreshAccount(email, "Refreshed after copying", false, false);
                        }
                    } else {
                        cancel = true;
                        break outer;
                    }

                } else {
                    Helpers.mostrarMensajeError(MAIN_WINDOW, "THERE IS NO ENOUGH FREE SPACE IN\n<b>" + email + "</b>");
                    cancel = true;
                    break outer;
                }
            }

            logout(true);

            if (!cancel) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES COPIED");
            } else {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT COPIED)");
            }

            _move_dialog = null;

        } else if (nodesToCopy.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void moveNodesInsideAccount(String text) {

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("MOVING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToMove = Helpers.extractNodeMapFromText(text);

        boolean cancel = false;

        if (!nodesToMove.isEmpty()) {

            outer:
            for (String email : nodesToMove.keySet()) {

                login(email);
                ArrayList<String> node_list = nodesToMove.get(email);

                Helpers.GUIRunAndWait(() -> {
                    _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, null, 2, email);

                    _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                    _move_dialog.setVisible(true);
                });

                int conta = 0;

                for (String node : node_list) {

                    String old_full_path = Helpers.getNodeFullPath(node, email);

                    String old_n = old_full_path.replaceAll("^.*/([^/]*)$", "$1");

                    String new_full_path = _move_dialog.getNew_name().getText().trim() + old_n;

                    if (_move_dialog.isOk() && !old_full_path.equals(new_full_path) && !new_full_path.isBlank()) {

                        String folder = new_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                        Helpers.runProcess(new String[]{"mega-mkdir", "-p", folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        Helpers.runProcess(new String[]{"mega-mv", node, new_full_path}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        conta++;

                    } else if (!_move_dialog.isOk()) {
                        cancel = true;
                        break outer;
                    }
                }

                if (conta > 0) {

                    String stats = getAccountStatistics(email);

                    parseAccountNodes(email);

                    Helpers.GUIRun(() -> {

                        output_textarea_append("\n[" + email + "] (Refreshed after moving)\n\n" + stats + "\n\n");

                    });

                }

            }

            logout(true);

            if (!cancel) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES MOVED");
            } else {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT MOVED)");
            }

            _move_dialog = null;

        } else if (nodesToMove.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void renameNodes(String text) {

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("RENAMING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToRename = Helpers.extractNodeMapFromText(text);

        if (!nodesToRename.isEmpty()) {

            boolean cancel = false;

            outer:
            for (String email : nodesToRename.keySet()) {

                login(email);
                ArrayList<String> node_list = nodesToRename.get(email);

                int conta = 0;

                for (String node : node_list) {

                    String old_full_path = Helpers.getNodeFullPath(node, email);

                    String old_name = old_full_path.replaceAll("^.*/([^/]*)$", "$1");

                    String old_path = old_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                    Helpers.GUIRunAndWait(() -> {
                        _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, old_full_path, 0, email);

                        _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                        _move_dialog.setVisible(true);
                    });

                    String new_name = _move_dialog.getNew_name().getText().trim();

                    if (_move_dialog.isOk() && !old_name.equals(new_name) && !new_name.isBlank()) {

                        Helpers.runProcess(new String[]{"mega-mv", node, old_path + new_name}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        conta++;

                    } else if (!_move_dialog.isOk()) {
                        cancel = true;
                        break outer;
                    }
                }

                if (conta > 0) {

                    refreshAccount(email, "Refreshed after copying", false, false);

                }

            }

            logout(true);

            if (!cancel) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES RENAMED");
            } else {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT RENAMED)");
            }

            _move_dialog = null;

        } else if (nodesToRename.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void exportNodes(String text, boolean enable) {

        _running_main_action = true;

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText((enable ? "ENABLING" : "DISABLING") + " PUBLIC LINK ON SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToExport = Helpers.extractNodeMapFromText(text);

        if (!nodesToExport.isEmpty()) {

            for (String email : nodesToExport.keySet()) {

                ArrayList<String> node_list = nodesToExport.get(email);

                ArrayList<String> export_command = new ArrayList<>();

                export_command.add("mega-export");

                export_command.add(enable ? "-af" : "-d");

                export_command.addAll(node_list);

                login(email);

                Helpers.runProcess(export_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                refreshAccount(email, "Refreshed after public links generated/removed", false, false);

            }

            logout(true);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES " + (enable ? "PUBLIC LINKS GENERATED" : "PUBLIC LINKS REMOVED"));
        } else if (nodesToExport.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void importLink(String email, String link, String rpath) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("IMPORTING " + link + " -> " + email + " PLEASE WAIT...");

        });

        if (MEGA_ACCOUNTS.containsKey(email)) {

            login(email);

            if (!rpath.equals("/")) {
                Helpers.runProcess(new String[]{"mega-mkdir", "-p", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            String[] import_result = Helpers.runProcess(new String[]{"mega-import", link, rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            if (Integer.parseInt(import_result[2]) == 0) {
                refreshAccount(email, "Refreshed after insertion", false, false);
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "<b>" + link + "</b>\nIMPORTED");
            } else {
                Helpers.mostrarMensajeError(MAIN_WINDOW, link + " " + rpath + " IMPORTATION ERROR (" + import_result[2] + ")");
            }
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText(old_status);

        });

        _running_main_action = false;

    }

    public void truncateAccount(String email) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("TRUNCATING " + email + " PLEASE WAIT...");

        });

        if (MEGA_ACCOUNTS.containsKey(email) && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "<font color='red'><b>CAUTION!!</b></font> ALL CONTENT (<b>" + Helpers.formatBytes(Helpers.getAccountUsedSpace(email)) + "</b>) INSIDE <b>" + email + "</b> WILL BE <font color='red'><b>PERMANENTLY</b> DELETED.</b></font><br><br>ARE YOU SURE?") == 0 && (Helpers.getAccountUsedSpace(email) == 0 || Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "Sorry for asking you again... ARE YOU SURE?") == 0)) {

            login(email);

            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'//in/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'//bin/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            refreshAccount(email, "Refreshed after account truncate", false, false);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "<b>" + email + "</b>\nTRUNCATED");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText(old_status);

        });

        _running_main_action = false;

    }

    public void removeNodes(String text) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("DELETING FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToRemove = Helpers.extractNodeMapFromText(text);

        if (!nodesToRemove.isEmpty() && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "CAUTION!! SELECTED FILES/FOLDERS WILL BE <b>PERMANENTLY</b> DELETED.<br><br>ARE YOU SURE?") == 0) {

            for (String email : nodesToRemove.keySet()) {

                ArrayList<String> node_list = nodesToRemove.get(email);

                ArrayList<String> delete_command = new ArrayList<>();

                delete_command.add("mega-rm");

                delete_command.add("-rf");

                delete_command.addAll(node_list);

                login(email);

                Helpers.runProcess(delete_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                for (String n : node_list) {
                    MEGA_NODES.remove(n);
                }

                refreshAccount(email, "Refreshed after deletion", false, false);
            }

            logout(true);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES DELETED");
        } else if (nodesToRemove.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void downloadNodes(String text) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("DOWNLOADING FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToDownload = Helpers.extractNodeMapFromText(text);

        if (!nodesToDownload.isEmpty()) {

            JFileChooser fileChooser = new JFileChooser("DOWNLOAD DIRECTORY");

            fileChooser.setPreferredSize(new Dimension(800, 600));

            Helpers.updateComponentFont(fileChooser, fileChooser.getFont(), 1.20f);

            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int option = fileChooser.showOpenDialog(this);

            if (option == JFileChooser.APPROVE_OPTION) {

                File download_directory = fileChooser.getSelectedFile();

                synchronized (TRANSFERENCES_LOCK) {

                    for (String email : nodesToDownload.keySet()) {

                        ArrayList<String> node_list = nodesToDownload.get(email);

                        for (String n : node_list) {
                            Helpers.GUIRunAndWait(() -> {

                                Transference trans = new Transference(email, download_directory.getAbsolutePath(), n, 0, false, null);
                                TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                transferences.revalidate();
                                transferences.repaint();

                            });

                        }
                    }

                    saveTransfers();

                    TRANSFERENCES_LOCK.notifyAll();

                }
                Helpers.GUIRunAndWait(() -> {
                    tabbed_panel.setSelectedIndex(1);
                });
            }
        } else if (nodesToDownload.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO VALID FOLDERS/FILES SELECTED\n(you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void refreshAccount(String email, String reason, boolean notification, boolean refresh_session) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("REFRESHING " + email + " PLEASE WAIT...");

        });

        Main.FREE_SPACE_CACHE.remove(email);

        if (!MEGA_ACCOUNTS.containsKey(email) || refresh_session) {

            String[] account_data = Helpers.extractAccountLoginDataFromText(email, cuentas_textarea.getText());

            if (!MEGA_ACCOUNTS.containsKey(email) || !account_data[1].equals(MEGA_ACCOUNTS.get(email))) {
                MEGA_ACCOUNTS.put(email, account_data[1]);
            }

            MEGA_SESSIONS.remove(email);

            logout(false);
        }

        if (login(email)) {

            String stats = getAccountStatistics(email);

            parseAccountNodes(email);

            _last_email_force_refresh = email;

            Helpers.GUIRun(() -> {

                output_textarea_append("\n[" + email + "] (" + reason + ")\n\n" + stats + "\n\n");
                Helpers.JTextFieldRegularPopupMenu.addMainMEGAPopupMenuTo(output_textarea);
                Helpers.JTextFieldRegularPopupMenu.addAccountsMEGAPopupMenuTo(cuentas_textarea);
            });

            if (notification) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "<b>" + email + "</b>\nREFRESHED");
            }
        } else {
            output_textarea_append("\n[" + email + "] LOGIN ERROR\n\n");
            Helpers.mostrarMensajeError(MAIN_WINDOW, "LOGIN ERROR WITH <b>" + email + "</b>");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    private void saveAccounts() {

        try (FileOutputStream fos = new FileOutputStream(ACCOUNTS_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(MEGA_ACCOUNTS);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (FileOutputStream fos = new FileOutputStream(EXCLUDED_ACCOUNTS_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(MEGA_EXCLUDED_ACCOUNTS);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (FileOutputStream fos = new FileOutputStream(SESSIONS_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(MEGA_SESSIONS);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (FileOutputStream fos = new FileOutputStream(NODES_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(MEGA_NODES);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try (FileOutputStream fos = new FileOutputStream(FREE_SPACE_CACHE_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(FREE_SPACE_CACHE);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveLog() {

        try {
            Files.writeString(Paths.get(LOG_FILE), output_textarea.getText());
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveMISC() {

        try (FileOutputStream fos = new FileOutputStream(Main.MEGADOCTOR_MISC_FILE); ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(Main.MEGADOCTOR_MISC);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadMISC() {
        if (Files.exists(Paths.get(Main.MEGADOCTOR_MISC_FILE))) {
            try (FileInputStream fis = new FileInputStream(Main.MEGADOCTOR_MISC_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                Main.MEGADOCTOR_MISC = (ConcurrentHashMap<String, Object>) ois.readObject();
            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Helpers.GUIRunAndWait(() -> {
            menu_https.setSelected(Main.MEGADOCTOR_MISC.containsKey("megacmd_https") && (boolean) Main.MEGADOCTOR_MISC.get("megacmd_https"));
            cuentas_scrollpanel.setVisible(!(Main.MEGADOCTOR_MISC.containsKey("hide_accounts") && (boolean) Main.MEGADOCTOR_MISC.get("hide_accounts")));
            show_accounts.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/vertical_" + (cuentas_scrollpanel.isVisible() ? "less" : "more") + ".png")));
            revalidate();
            repaint();
        });
    }

    private void loadLog() {

        if (Files.exists(Paths.get(LOG_FILE))) {

            try {

                String log_string = Files.readString(Paths.get(LOG_FILE));

                Helpers.GUIRun(() -> {

                    try {
                        output_textarea.getStyledDocument().insertString(0, log_string, null);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                );

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void loadAccountsAndTransfers() {

        if (Files.exists(Paths.get(ACCOUNTS_FILE))) {
            try (FileInputStream fis = new FileInputStream(ACCOUNTS_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {

                MEGA_ACCOUNTS = (LinkedHashMap<String, String>) ois.readObject();

                if (!MEGA_ACCOUNTS.isEmpty()) {

                    ArrayList<String> accounts = new ArrayList<>();

                    for (String k : MEGA_ACCOUNTS.keySet()) {
                        accounts.add(k + "#" + MEGA_ACCOUNTS.get(k));
                    }

                    Collections.sort(accounts, String.CASE_INSENSITIVE_ORDER);

                    Helpers.GUIRun(() -> {

                        if (!_firstAccountsTextareaClick) {
                            _firstAccountsTextareaClick = true;
                            cuentas_textarea.setText("");
                            cuentas_textarea.setForeground(null);
                        }

                        cuentas_textarea.append(String.join("\n", accounts) + "\n");

                        cuentas_textarea.setCaretPosition(0);

                        session_menu.setSelected(true);

                        getUpload_button().setEnabled(true);

                    });
                } else {
                    Helpers.GUIRun(() -> {
                        session_menu.setSelected(false);
                    });
                }

            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (Files.exists(Paths.get(EXCLUDED_ACCOUNTS_FILE))) {
                try (FileInputStream fis = new FileInputStream(EXCLUDED_ACCOUNTS_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                    MEGA_EXCLUDED_ACCOUNTS = (ConcurrentLinkedQueue<String>) ois.readObject();
                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (Files.exists(Paths.get(NODES_FILE))) {
                try (FileInputStream fis = new FileInputStream(NODES_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                    MEGA_NODES = (ConcurrentHashMap<String, Object[]>) ois.readObject();

                    if (MEGA_NODES.values().toArray().length > 0) {
                        Object[] test = (Object[]) MEGA_NODES.values().toArray()[0];

                        if (test.length != 4) {
                            MEGA_NODES.clear();
                            Files.deleteIfExists(Paths.get(NODES_FILE));
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "WRONG NODES FILE (DELETING...)");
                        }
                    }

                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (Files.exists(Paths.get(SESSIONS_FILE))) {
                try (FileInputStream fis = new FileInputStream(SESSIONS_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                    MEGA_SESSIONS = (HashMap<String, String>) ois.readObject();

                    if (!MEGA_SESSIONS.isEmpty()) {
                        loadTransfers();
                    }

                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    try {
                        Files.deleteIfExists(Paths.get(SESSIONS_FILE));
                    } catch (IOException ex1) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }

            if (Files.exists(Paths.get(FREE_SPACE_CACHE_FILE))) {
                try (FileInputStream fis = new FileInputStream(FREE_SPACE_CACHE_FILE); ObjectInputStream ois = new ObjectInputStream(fis)) {
                    FREE_SPACE_CACHE = (ConcurrentHashMap<String, Long>) ois.readObject();

                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    try {
                        Files.deleteIfExists(Paths.get(FREE_SPACE_CACHE_FILE));
                    } catch (IOException ex1) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }

        } else {
            Helpers.GUIRun(() -> {
                session_menu.setSelected(false);
            });
        }

        runFileSplitter();

    }

    public void parseAccountNodes(String email) {

        synchronized (MEGA_NODES) {

            clearAccountNodes(email);

            login(email);

            String ls = Helpers.runProcess(new String[]{"mega-ls", "-lr", "--show-handles"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            final String regex = "^(.).* (H:[^ ]+) (.+)";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(ls);

            while (matcher.find()) {

                MEGA_NODES.put(matcher.group(2), new Object[]{getCurrentAccountNodeSize(matcher.group(2)), email, matcher.group(3), (matcher.group(1).toLowerCase().equals("d"))});

            }
        }
    }

    public void purgeMEGAcmdCache() {

        _running_main_action = true;

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("PURGING MEGAcmd cache PLEASE WAIT...");

        });

        Helpers.runProcess(new String[]{"mega-quit"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        String cache_dir = (Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : System.getProperty("user.home")) + File.separator + ".megaCmd";

        try {
            Helpers.deleteDirectoryRecursion(Paths.get(cache_dir));

            Helpers.runProcess(new String[]{"mega-version"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, false, Pattern.compile("MEGAcmd version:"));

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "MEGAcmd cache PURGED");

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

            Helpers.runProcess(new String[]{"mega-version"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null, false, Pattern.compile("MEGAcmd version:"));

            Helpers.mostrarMensajeError(MAIN_WINDOW, "ERROR deleting MEGAcmd cache directory");
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");
            purge_cache_menu.setEnabled(true);

        });

        _running_main_action = false;

    }

    public void exportAllNodesInAccount(String email, boolean enable, boolean notify) {

        _running_main_action = true;

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText((enable ? "ENABLING" : "DISABLING") + " ALL PUBLIC LINKS FROM " + email + " PLEASE WAIT...");

        });

        parseAccountNodes(email);

        ArrayList<String> node_list = new ArrayList<>();

        synchronized (MEGA_NODES) {

            var it = MEGA_NODES.entrySet().iterator();

            while (it.hasNext()) {

                Map.Entry<String, Object[]> node = (Map.Entry<String, Object[]>) it.next();

                Object[] o = node.getValue();

                if (o[1].equals(email)) {
                    node_list.add(node.getKey());
                }
            }
        }

        ArrayList<String> export_command = new ArrayList<>();

        export_command.add("mega-export");

        export_command.add(enable ? "-af" : "-d");

        export_command.addAll(node_list);

        login(email);

        Helpers.runProcess(export_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        refreshAccount(email, "Refreshed after public links generated/removed", false, false);

        logout(true);

        if (notify) {
            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL <b>" + email + "</b> " + (enable ? "PUBLIC LINKS GENERATED" : "PUBLIC LINKS REMOVED"));
        }

        Helpers.GUIRunAndWait(() -> {

            enableTOPControls(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;

    }

    public void clearAccountNodes(String email) {
        synchronized (MEGA_NODES) {

            var it = MEGA_NODES.entrySet().iterator();

            while (it.hasNext()) {

                Map.Entry<String, Object[]> node = (Map.Entry<String, Object[]>) it.next();

                Object[] o = node.getValue();

                if (o[1].equals(email)) {
                    it.remove();
                }
            }
        }
    }

    private long getCurrentAccountNodeSize(String node) {

        String[] du = Helpers.runProcess(new String[]{"mega-du", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(du[2]) == 0) {

            String[] lines = du[1].split("\n");

            String[] size = lines[lines.length - 1].split(":");

            return Long.parseLong(size[1].trim());
        }

        return -1;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logo_label = new javax.swing.JLabel();
        vamos_button = new javax.swing.JButton();
        status_label = new javax.swing.JLabel();
        progressbar = new javax.swing.JProgressBar();
        check_only_new_checkbox = new javax.swing.JCheckBox();
        show_accounts = new javax.swing.JLabel();
        mainSplitPanel = new javax.swing.JSplitPane();
        cuentas_scrollpanel = new javax.swing.JScrollPane();
        cuentas_textarea = new javax.swing.JTextArea();
        tabbed_panel = new javax.swing.JTabbedPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        output_textarea = new javax.swing.JTextPane();
        jPanel3 = new javax.swing.JPanel();
        transferences_control_panel = new javax.swing.JPanel();
        cancel_all_button = new javax.swing.JButton();
        clear_trans_button = new javax.swing.JButton();
        pause_button = new javax.swing.JButton();
        copy_all_button = new javax.swing.JButton();
        transf_scroll = new javax.swing.JScrollPane();
        transferences_panel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        clear_log_button = new javax.swing.JButton();
        new_account_button = new javax.swing.JButton();
        upload_button = new javax.swing.JButton();
        save_button = new javax.swing.JButton();
        barra_menu = new javax.swing.JMenuBar();
        options_menu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        session_menu = new javax.swing.JCheckBoxMenuItem();
        menu_https = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        purge_cache_menu = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("MegaDoctor");
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/megadoctor_51.png")).getImage() );
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowIconified(java.awt.event.WindowEvent evt) {
                formWindowIconified(evt);
            }
        });

        logo_label.setBackground(new java.awt.Color(255, 255, 255));
        logo_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/megadoctor_frame.png"))); // NOI18N
        logo_label.setDoubleBuffered(true);

        vamos_button.setBackground(new java.awt.Color(0, 153, 0));
        vamos_button.setFont(new java.awt.Font("Noto Sans", 1, 48)); // NOI18N
        vamos_button.setForeground(new java.awt.Color(255, 255, 255));
        vamos_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/check_accounts.png"))); // NOI18N
        vamos_button.setText("CHECK ACCOUNTS");
        vamos_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        vamos_button.setDoubleBuffered(true);
        vamos_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vamos_buttonActionPerformed(evt);
            }
        });

        status_label.setFont(new java.awt.Font("Noto Sans", 3, 18)); // NOI18N
        status_label.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        status_label.setDoubleBuffered(true);

        check_only_new_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        check_only_new_checkbox.setSelected(true);
        check_only_new_checkbox.setText("Check only new accounts");
        check_only_new_checkbox.setToolTipText("Check only new accounts");
        check_only_new_checkbox.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        check_only_new_checkbox.setDoubleBuffered(true);
        check_only_new_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                check_only_new_checkboxActionPerformed(evt);
            }
        });

        show_accounts.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/vertical_less.png"))); // NOI18N
        show_accounts.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        show_accounts.setDoubleBuffered(true);
        show_accounts.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                show_accountsMouseClicked(evt);
            }
        });

        mainSplitPanel.setDividerSize(4);
        mainSplitPanel.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSplitPanel.setResizeWeight(0.2);
        mainSplitPanel.setToolTipText("");
        mainSplitPanel.setDoubleBuffered(true);

        cuentas_scrollpanel.setDoubleBuffered(true);

        cuentas_textarea.setColumns(20);
        cuentas_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        cuentas_textarea.setForeground(new java.awt.Color(204, 204, 204));
        cuentas_textarea.setRows(5);
        cuentas_textarea.setText("xxxxxxxxxxxxxxxxxxxx1@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx2@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx3@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx4@lalalalala.com#password");
        cuentas_textarea.setDoubleBuffered(true);
        cuentas_textarea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                cuentas_textareaFocusGained(evt);
            }
        });
        cuentas_textarea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                cuentas_textareaMouseReleased(evt);
            }
        });
        cuentas_scrollpanel.setViewportView(cuentas_textarea);

        mainSplitPanel.setTopComponent(cuentas_scrollpanel);

        tabbed_panel.setToolTipText("Double click to show/hide accounts textbox");
        tabbed_panel.setDoubleBuffered(true);
        tabbed_panel.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        tabbed_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabbed_panelMouseClicked(evt);
            }
        });

        output_textarea.setEditable(false);
        output_textarea.setBackground(new java.awt.Color(102, 102, 102));
        output_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        output_textarea.setForeground(new java.awt.Color(255, 255, 255));
        output_textarea.setDoubleBuffered(true);
        jScrollPane2.setViewportView(output_textarea);

        tabbed_panel.addTab("Log", new javax.swing.ImageIcon(getClass().getResource("/images/log.png")), jScrollPane2); // NOI18N

        cancel_all_button.setBackground(new java.awt.Color(255, 51, 0));
        cancel_all_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        cancel_all_button.setForeground(new java.awt.Color(255, 255, 255));
        cancel_all_button.setText("CANCEL AND REMOVE ALL");
        cancel_all_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancel_all_button.setDoubleBuffered(true);
        cancel_all_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_all_buttonActionPerformed(evt);
            }
        });

        clear_trans_button.setBackground(new java.awt.Color(0, 153, 0));
        clear_trans_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        clear_trans_button.setForeground(new java.awt.Color(255, 255, 255));
        clear_trans_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/menu/clear.png"))); // NOI18N
        clear_trans_button.setText("CLEAR ALL OK FINISHED");
        clear_trans_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        clear_trans_button.setDoubleBuffered(true);
        clear_trans_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_trans_buttonActionPerformed(evt);
            }
        });

        pause_button.setBackground(new java.awt.Color(255, 204, 0));
        pause_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        pause_button.setText("PAUSE");
        pause_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        pause_button.setDoubleBuffered(true);
        pause_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pause_buttonActionPerformed(evt);
            }
        });

        copy_all_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        copy_all_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/menu/export_on.png"))); // NOI18N
        copy_all_button.setText("COPY ALL PUBLIC LINKS");
        copy_all_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        copy_all_button.setDoubleBuffered(true);
        copy_all_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_all_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout transferences_control_panelLayout = new javax.swing.GroupLayout(transferences_control_panel);
        transferences_control_panel.setLayout(transferences_control_panelLayout);
        transferences_control_panelLayout.setHorizontalGroup(
            transferences_control_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transferences_control_panelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(clear_trans_button)
                .addGap(18, 18, 18)
                .addComponent(copy_all_button)
                .addGap(18, 18, 18)
                .addComponent(pause_button)
                .addGap(18, 18, 18)
                .addComponent(cancel_all_button)
                .addContainerGap(334, Short.MAX_VALUE))
        );
        transferences_control_panelLayout.setVerticalGroup(
            transferences_control_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transferences_control_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transferences_control_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancel_all_button)
                    .addComponent(clear_trans_button)
                    .addComponent(pause_button)
                    .addComponent(copy_all_button))
                .addContainerGap())
        );

        transf_scroll.setBorder(null);

        transferences_panel.setLayout(new java.awt.BorderLayout());
        transf_scroll.setViewportView(transferences_panel);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transferences_control_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(transf_scroll)
                .addGap(0, 0, 0))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(transferences_control_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(transf_scroll)
                .addGap(0, 0, 0))
        );

        tabbed_panel.addTab("Transferences", new javax.swing.ImageIcon(getClass().getResource("/images/transfers.png")), jPanel3); // NOI18N

        mainSplitPanel.setBottomComponent(tabbed_panel);

        clear_log_button.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        clear_log_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/trash.png"))); // NOI18N
        clear_log_button.setText("CLEAR LOG");
        clear_log_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        clear_log_button.setDoubleBuffered(true);
        clear_log_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_log_buttonActionPerformed(evt);
            }
        });

        new_account_button.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        new_account_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mega.png"))); // NOI18N
        new_account_button.setText("NEW ACCOUNT");
        new_account_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        new_account_button.setDoubleBuffered(true);
        new_account_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_account_buttonActionPerformed(evt);
            }
        });

        upload_button.setBackground(new java.awt.Color(0, 0, 0));
        upload_button.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        upload_button.setForeground(new java.awt.Color(255, 255, 255));
        upload_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/new_upload.png"))); // NOI18N
        upload_button.setText("NEW UPLOAD");
        upload_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        upload_button.setDoubleBuffered(true);
        upload_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upload_buttonActionPerformed(evt);
            }
        });

        save_button.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        save_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/guardar.png"))); // NOI18N
        save_button.setText("SAVE LOG TO FILE");
        save_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        save_button.setDoubleBuffered(true);
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(upload_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(new_account_button)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(save_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clear_log_button)
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(upload_button)
                        .addComponent(save_button)
                        .addComponent(clear_log_button))
                    .addComponent(new_account_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        barra_menu.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

        options_menu.setText("Options");
        options_menu.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

        jMenuItem2.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        jMenuItem2.setText("Configure auto allocation excluded accounts");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        options_menu.add(jMenuItem2);

        session_menu.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        session_menu.setSelected(true);
        session_menu.setText("Keep session on disk");
        options_menu.add(session_menu);

        menu_https.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        menu_https.setSelected(true);
        menu_https.setText("Use HTTPS TRANSFERS");
        menu_https.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_httpsActionPerformed(evt);
            }
        });
        options_menu.add(menu_https);
        options_menu.add(jSeparator1);

        purge_cache_menu.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        purge_cache_menu.setForeground(new java.awt.Color(255, 0, 0));
        purge_cache_menu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/menu/clear.png"))); // NOI18N
        purge_cache_menu.setText("Purge MEGAcmd CACHE");
        purge_cache_menu.setDoubleBuffered(true);
        purge_cache_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                purge_cache_menuActionPerformed(evt);
            }
        });
        options_menu.add(purge_cache_menu);

        barra_menu.add(options_menu);

        jMenu1.setText("Help");
        jMenu1.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

        jMenuItem1.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        jMenuItem1.setText("About");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        barra_menu.add(jMenu1);

        setJMenuBar(barra_menu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(show_accounts)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progressbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(logo_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(vamos_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(mainSplitPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(check_only_new_checkbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(status_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(logo_label)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(vamos_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(check_only_new_checkbox)
                    .addComponent(status_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(progressbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(show_accounts))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainSplitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void vamos_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vamos_buttonActionPerformed
        // TODO add your handling code here:

        if (MEGA_CMD_VERSION != null && (_running_global_check || Helpers.mostrarMensajeInformativoSINO(this, "This will run through all your accounts to generate a report on their status.\n<b>It may take a long time</b> and while the report is being generated\nyou will not be able to make transfers in MegaDoctor.\n\n<b>CONTINUE?</b>") == 0)) {

            if (!isRunning_global_check()) {

                if (!_firstAccountsTextareaClick) {
                    _firstAccountsTextareaClick = true;
                    cuentas_textarea.setText("");
                    cuentas_textarea.setForeground(null);
                }

                _running_global_check = true;
                _aborting_global_check = false;
                cuentas_textarea.setEnabled(false);
                vamos_button.setText("STOP");
                vamos_button.setBackground(Color.red);

                enableTOPControls(false);

                Helpers.threadRun(() -> {
                    final String regex = "(.*?)#(.+)";
                    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                    final Matcher matcher = pattern.matcher(cuentas_textarea.getText());

                    LinkedHashMap<String, String> mega_accounts = new LinkedHashMap<>();

                    ArrayList<String[]> accounts_space_info = new ArrayList<>();

                    ArrayList<String> login_error_accounts = new ArrayList<>();

                    while (matcher.find()) {

                        if (_check_only_new) {
                            if (!MEGA_ACCOUNTS.containsKey(matcher.group(1))) {
                                MEGA_ACCOUNTS.put(matcher.group(1), matcher.group(2));
                                mega_accounts.put(matcher.group(1), matcher.group(2));
                            }
                        } else {
                            MEGA_ACCOUNTS.put(matcher.group(1), matcher.group(2));
                            mega_accounts.put(matcher.group(1), matcher.group(2));
                        }

                    }

                    if (!mega_accounts.isEmpty()) {
                        Helpers.GUIRun(() -> {
                            progressbar.setMaximum(mega_accounts.size());
                            output_textarea_append(" __  __                  ____             _             \n"
                                    + "|  \\/  | ___  __ _  __ _|  _ \\  ___   ___| |_ ___  _ __ \n"
                                    + "| |\\/| |/ _ \\/ _` |/ _` | | | |/ _ \\ / __| __/ _ \\| '__|\n"
                                    + "| |  | |  __/ (_| | (_| | |_| | (_) | (__| || (_) | |   \n"
                                    + "|_|  |_|\\___|\\__, |\\__,_|____/ \\___/ \\___|\\__\\___/|_|   \n"
                                    + "             |___/                                      \n\nCHECKING START -> " + Helpers.getFechaHoraActual() + "\n");
                        });
                        int i = 0;

                        for (String email : mega_accounts.keySet()) {

                            Helpers.GUIRun(() -> {
                                status_label.setText("Login " + email + " ...");
                            });

                            boolean login_ok = login(email);

                            if (!login_ok) {
                                login_error_accounts.add(email + "#" + mega_accounts.get(email));
                                Helpers.GUIRun(() -> {
                                    output_textarea_append("\n[" + email + "] LOGIN ERROR\n\n");
                                });
                            } else {

                                Helpers.GUIRun(() -> {
                                    status_label.setText("Reading " + email + " info...");
                                });

                                String stats = getAccountStatistics(email);

                                Helpers.GUIRun(() -> {

                                    output_textarea_append("\n[" + email + "]\n\n" + stats + "\n\n");

                                });

                                String[] space_stats = Helpers.getAccountSpaceData(email);

                                accounts_space_info.add(space_stats);

                                Main.FREE_SPACE_CACHE.put(email, Long.parseLong(space_stats[2]) - Long.parseLong(space_stats[1]));

                                parseAccountNodes(email);
                            }

                            i++;

                            int j = i;

                            Helpers.GUIRun(() -> {
                                progressbar.setValue(j);

                            });

                            if (isAborting_global_check()) {
                                break;
                            }

                        }

                        logout(true);

                        Collections.sort(accounts_space_info, new Comparator<String[]>() {
                            @Override
                            public int compare(String[] o1, String[] o2) {

                                return Long.compare(Long.parseLong(o2[2]) - Long.parseLong(o2[1]), Long.parseLong(o1[2]) - Long.parseLong(o1[1]));
                            }
                        });

                        Helpers.GUIRun(() -> {
                            output_textarea_append("--------------------------------------\n");
                            output_textarea_append("ACCOUNTS ORDERED BY FREE SPACE (DESC):\n");
                            output_textarea_append("--------------------------------------\n\n");
                            long total_space = 0;
                            long total_space_used = 0;
                            for (String[] account : accounts_space_info) {
                                total_space_used += Long.parseLong(account[1]);
                                total_space += Long.parseLong(account[2]);
                                output_textarea_append(account[0] + " [" + Helpers.formatBytes(Long.parseLong(account[2]) - Long.parseLong(account[1])) + " FREE] (of " + Helpers.formatBytes(Long.parseLong(account[2])) + ")\n\n");
                            }

                            output_textarea_append("TOTAL FREE SPACE: " + Helpers.formatBytes(total_space - total_space_used) + " (of " + Helpers.formatBytes(total_space) + ")\n\n");

                            String cuentas_text = cuentas_textarea.getText();

                            if (!login_error_accounts.isEmpty()) {

                                output_textarea_append("(WARNING) LOGIN ERRORS: " + String.valueOf(login_error_accounts.size()) + "\n");

                                for (String error_account : login_error_accounts) {
                                    output_textarea_append("    ERROR: " + error_account + "\n");
                                    cuentas_text.replace(error_account, "");
                                }

                                cuentas_textarea.setText(cuentas_text);
                            }

                            output_textarea_append("\nCHECKING END -> " + Helpers.getFechaHoraActual() + "\n");

                            Notification notification = new Notification(new javax.swing.JFrame(), false, "ALL ACCOUNTS CHECKED", (Main.MAIN_WINDOW.getExtendedState() & JFrame.ICONIFIED) == 0 ? 3000 : 0, "finish.wav");
                            Helpers.setWindowLowRightCorner(notification);
                            notification.setVisible(true);

                        });

                        Helpers.mostrarMensajeInformativo(this, isAborting_global_check() ? "CANCELED!" : "ALL ACCOUNTS CHECKED");

                    } else {
                        Helpers.mostrarMensajeInformativo(this, "ALL ACCOUNTS CHECKED");
                    }

                    Helpers.GUIRunAndWait(() -> {
                        progressbar.setValue(0);
                        vamos_button.setText("CHECK ACCOUNTS");
                        vamos_button.setBackground(new Color(0, 153, 0));
                        cuentas_textarea.setEnabled(true);
                        status_label.setText("");
                        enableTOPControls(true);
                    });

                    _running_global_check = false;
                    _aborting_global_check = false;
                });

            } else if (isRunning_global_check()) {
                if (Helpers.mostrarMensajeInformativoSINO(this, "SURE?") == 0) {
                    _aborting_global_check = true;
                    Helpers.GUIRun(() -> {
                        vamos_button.setText("CANCELING...");
                        vamos_button.setEnabled(false);
                    });
                }
            }

        }

    }//GEN-LAST:event_vamos_buttonActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        About about = new About(this, true);
        about.setLocationRelativeTo(this);
        about.setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void save_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed
        // TODO add your handling code here:

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setPreferredSize(new Dimension(800, 600));

        Helpers.updateComponentFont(fileChooser, fileChooser.getFont(), 1.20f);

        int retval = fileChooser.showSaveDialog(this);

        if (retval == JFileChooser.APPROVE_OPTION) {

            File file = fileChooser.getSelectedFile();

            if (file == null) {
                return;
            }

            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getParentFile(), file.getName() + ".txt");
            }

            try {
                output_textarea.write(new OutputStreamWriter(new FileOutputStream(file), "utf-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }//GEN-LAST:event_save_buttonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        if (!_closing) {
            if (busy() || !"".equals(output_textarea.getText().trim())) {

                if (Helpers.mostrarMensajeInformativoSINO(this, "EXIT NOW?") == 0) {

                    if (isSomeTransference_running()) {

                        if (!session_menu.isSelected()) {
                            if (Helpers.mostrarMensajeInformativoSINO(this, "All transactions in progress or on hold will be lost. ARE YOU SURE?") == 0) {

                                bye();
                            }
                        } else {
                            bye();
                        }
                    } else {

                        bye();
                    }
                }

            } else {

                bye();
            }
        } else {

            if (ONE_INSTANCE_SOCKET != null) {
                try {
                    ONE_INSTANCE_SOCKET.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            Main.EXIT = true;

            Helpers.destroyAllExternalProcesses();

            System.exit(1);
        }
    }//GEN-LAST:event_formWindowClosing

    private void cuentas_textareaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_cuentas_textareaFocusGained
        // TODO add your handling code here:
        if (!_firstAccountsTextareaClick) {
            _firstAccountsTextareaClick = true;
            cuentas_textarea.setText("");
            cuentas_textarea.setForeground(null);
            MEGA_ACCOUNTS.clear();
        }
    }//GEN-LAST:event_cuentas_textareaFocusGained

    private void cuentas_textareaMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_cuentas_textareaMouseReleased
        // TODO add your handling code here:
        if (!_firstAccountsTextareaClick) {
            _firstAccountsTextareaClick = true;
            cuentas_textarea.setText("");
            cuentas_textarea.setForeground(null);
        }
    }//GEN-LAST:event_cuentas_textareaMouseReleased

    private void upload_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upload_buttonActionPerformed
        // TODO add your handling code here:

        if (Main.MEGA_ACCOUNTS.isEmpty()) {
            Helpers.mostrarMensajeError(Main.MAIN_WINDOW, "YOU MUST ADD AND CHECK SOME MEGA ACCOUNT BEFORE UPLOAD");
        } else if (_firstAccountsTextareaClick) {

            _provisioning_upload = true;

            getUpload_button().setEnabled(false);

            getPause_button().setEnabled(false);

            new_account_button.setEnabled(false);

            Helpers.threadRun(() -> {

                while (isSomeTransference_running() && isPausing_transference()) {
                    synchronized (TRANSFERENCES_LOCK) {
                        try {
                            TRANSFERENCES_LOCK.wait(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                if (isSomeTransference_running() && !isTransferences_paused()) {
                    _current_transference.pause();
                }

                Helpers.GUIRunAndWait(() -> {

                    if (isSomeTransference_running()) {
                        getPause_button().setText("RESUME");
                    }

                    UploadFileDialog dialog = new UploadFileDialog(this, true);

                    dialog.setLocationRelativeTo(this);

                    dialog.setVisible(true);

                    getUpload_button().setText("PREPARING UPLOAD/s...");

                    if (dialog.isOk()) {

                        boolean remove_after = dialog.getRemove_after().isSelected();

                        tabbed_panel.setSelectedIndex(1);

                        vamos_button.setEnabled(false);

                        cuentas_textarea.setEnabled(false);

                        progressbar.setIndeterminate(true);

                        if (Helpers.checkMEGALInk(dialog.getLocal_path())) {

                            Helpers.threadRun(() -> {
                                String filename = new File(dialog.getLocal_path()).getName();

                                String email = dialog.getEmail() != null ? dialog.getEmail() : Helpers.findFirstAccountWithSpace(dialog.getLocal_size(), filename);

                                importLink(email, dialog.getLocal_path(), dialog.getRemote_path());

                                Helpers.GUIRunAndWait(() -> {
                                    progressbar.setIndeterminate(false);
                                    getUpload_button().setEnabled(true);
                                    new_account_button.setEnabled(true);
                                    getPause_button().setEnabled(true);
                                    getUpload_button().setText("NEW UPLOAD");
                                });

                                _provisioning_upload = false;

                            });

                        } else {

                            Helpers.threadRun(() -> {

                                File f = new File(dialog.getLocal_path());

                                synchronized (TRANSFERENCES_LOCK) {

                                    if (dialog.getEmail() != null && !dialog.isSplit()) {
                                        Helpers.GUIRunAndWait(() -> {

                                            Transference trans = new Transference(dialog.getEmail(), dialog.getLocal_path(), dialog.getRemote_path(), 1, remove_after, null);
                                            TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                            transferences.revalidate();
                                            transferences.repaint();

                                        });
                                        saveTransfers();
                                    } else {

                                        if (f.isDirectory() && dialog.isSplit()) {

                                            File[] directoryListing = f.listFiles();

                                            if (directoryListing != null) {

                                                ArrayList<Object[]> hijos = new ArrayList<>();

                                                for (File child : directoryListing) {

                                                    AtomicBoolean terminate_walk_tree = new AtomicBoolean();

                                                    long size = child.isDirectory() ? Helpers.getDirectorySize(child, terminate_walk_tree) : child.length();

                                                    hijos.add(new Object[]{child.getAbsolutePath(), size});
                                                }

                                                Collections.sort(hijos, new Comparator<Object[]>() {
                                                    @Override
                                                    public int compare(Object[] o1, Object[] o2) {
                                                        return Long.compare((long) o2[1], (long) o1[1]);
                                                    }
                                                });

                                                for (Object[] h : hijos) {

                                                    String filename = new File((String) h[0]).getName();

                                                    long size = (long) h[1];

                                                    String email = Helpers.findFirstAccountWithSpace(size, filename);

                                                    if (email != null) {

                                                        Helpers.GUIRunAndWait(() -> {

                                                            Transference trans = new Transference(email, (String) h[0], dialog.getRemote_path(), 1, remove_after, null);
                                                            TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                                            transferences.revalidate();
                                                            transferences.repaint();

                                                        });

                                                    } else {
                                                        Helpers.mostrarMensajeError(null, "THERE IS NO ACCOUNT WITH ENOUGH FREE SPACE FOR:\n" + (String) h[0]);
                                                    }

                                                }

                                                saveTransfers();

                                            } else {
                                                Helpers.mostrarMensajeError(null, "EMPTY FOLDER");
                                            }

                                        } else if (!dialog.isSplit() || dialog.getSplit_size() >= dialog.getLocal_size()) {

                                            String email = Helpers.findFirstAccountWithSpace(dialog.getLocal_size(), f.getName());

                                            if (email != null) {
                                                Helpers.GUIRunAndWait(() -> {

                                                    Transference trans = new Transference(email, f.getAbsolutePath(), dialog.getRemote_path(), 1, remove_after, null);
                                                    TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                                    transferences.revalidate();
                                                    transferences.repaint();

                                                });

                                                saveTransfers();

                                            } else {
                                                Helpers.mostrarMensajeError(null, "THERE IS NO ACCOUNT WITH ENOUGH FREE SPACE FOR:\n" + f.getName());
                                            }

                                        } else {

                                            long chunk_size = dialog.getSplit_size();

                                            final int tot_chunks = (int) Math.ceil((float) dialog.getLocal_size() / chunk_size);

                                            if (dialog.getParts_radio().isSelected()) {

                                                Integer part = (Integer) dialog.getParts_spinner().getModel().getValue();

                                                if (part > tot_chunks) {
                                                    part = 1;
                                                }

                                                long csize = Math.min(chunk_size, dialog.getLocal_size() - chunk_size * (part - 1));

                                                String email = dialog.getEmail() != null ? dialog.getEmail() : Helpers.findFirstAccountWithSpace(csize, f.getName());

                                                final int ii = part;

                                                if (email != null) {

                                                    Helpers.GUIRunAndWait(() -> {

                                                        Transference trans = new Transference(email, f.getAbsolutePath() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks), dialog.getRemote_path() + f.getName() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks), 1, remove_after, csize);
                                                        TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                                        transferences.revalidate();
                                                        transferences.repaint();

                                                    });

                                                    Object[] file_splitter_task = new Object[]{f.getAbsolutePath(), chunk_size, dialog.getSplit_delete().isSelected(), part};

                                                    FILE_SPLITTER_TASKS.add(file_splitter_task);

                                                } else {
                                                    Helpers.mostrarMensajeError(null, "THERE IS NO ACCOUNT WITH ENOUGH FREE SPACE FOR:\n" + f.getName() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks));
                                                }

                                            } else {
                                                for (int i = 1; i <= tot_chunks; i++) {

                                                    long csize = Math.min(chunk_size, dialog.getLocal_size() - chunk_size * (i - 1));

                                                    String email = dialog.getEmail() != null ? dialog.getEmail() : Helpers.findFirstAccountWithSpace(csize, f.getName());

                                                    final int ii = i;

                                                    if (email != null) {

                                                        Helpers.GUIRunAndWait(() -> {

                                                            Transference trans = new Transference(email, f.getAbsolutePath() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks), dialog.getRemote_path() + f.getName() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks), 1, remove_after, csize);
                                                            TRANSFERENCES_MAP.put(transferences.add(trans), trans);
                                                            transferences.revalidate();
                                                            transferences.repaint();

                                                        });

                                                    } else {
                                                        Helpers.mostrarMensajeError(null, "THERE IS NO ACCOUNT WITH ENOUGH FREE SPACE FOR:\n" + f.getName() + ".part" + String.valueOf(ii) + "-" + String.valueOf(tot_chunks));
                                                    }
                                                }

                                                Object[] file_splitter_task = new Object[]{f.getAbsolutePath(), chunk_size, dialog.getSplit_delete().isSelected(), null};

                                                FILE_SPLITTER_TASKS.add(file_splitter_task);
                                            }

                                            saveTransfers();

                                            synchronized (FILE_SPLITTER_LOCK) {

                                                FILE_SPLITTER_LOCK.notifyAll();
                                            }
                                        }
                                    }

                                    Helpers.GUIRunAndWait(() -> {

                                        progressbar.setIndeterminate(false);
                                        getUpload_button().setEnabled(true);
                                        new_account_button.setEnabled(true);
                                        getPause_button().setEnabled(true);
                                        getUpload_button().setText("NEW UPLOAD");
                                    });

                                    _provisioning_upload = false;

                                    TRANSFERENCES_LOCK.notifyAll();
                                }
                            }
                            );
                        }
                    } else {
                        getUpload_button().setText("NEW UPLOAD");
                        getUpload_button().setEnabled(true);
                        new_account_button.setEnabled(true);
                        getPause_button().setEnabled(true);

                        _provisioning_upload = false;

                    }
                }
                );

            });
        } else if (MEGA_ACCOUNTS.isEmpty()) {
            Helpers.mostrarMensajeError(this, "YOU HAVE TO FIRST ADD SOME ACCOUNTS AND CHECK THEM");
        }
    }//GEN-LAST:event_upload_buttonActionPerformed

    private void clear_trans_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_trans_buttonActionPerformed
        // TODO add your handling code here:

        clear_trans_button.setEnabled(false);

        Helpers.threadRun(() -> {

            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRunAndWait(() -> {
                    if (transferences.getComponentCount() > 0) {

                        ArrayList<String> links = new ArrayList<>();

                        for (Component c : transferences.getComponents()) {

                            Transference t = TRANSFERENCES_MAP.get(c);

                            if (t.isFinished()) {

                                if (!t.isCanceled() && !t.isError()) {

                                    links.add(t.getLpath() + " -> " + t.getRpath() + " " + (t.getRemote_handle() != null ? " <" + t.getRemote_handle() + ">" : "") + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   " + (t.getPublic_link() != null ? t.getPublic_link() : ""));

                                } else {
                                    links.add("[ERROR/CANCELED] " + t.getLpath() + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   ");

                                }

                                TRANSFERENCES_MAP.remove(c);
                                transferences.remove(c);
                            }
                        }

                        if (!links.isEmpty()) {

                            Collections.sort(links);
                            output_textarea_append("\n\nTRANSFERENCES CLEARED:\n\n" + String.join("\n", links) + "\n\n");
                        }

                        transferences.revalidate();
                        transferences.repaint();
                    }

                    clear_trans_button.setEnabled(true);

                });

                TRANSFERENCES_LOCK.notifyAll();
            }
        });

    }//GEN-LAST:event_clear_trans_buttonActionPerformed

    private void cancel_all_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_all_buttonActionPerformed
        // TODO add your handling code here:

        getCancel_all_button().setEnabled(false);

        Helpers.threadRun(() -> {
            synchronized (TRANSFERENCES_LOCK) {
                Helpers.GUIRunAndWait(() -> {
                    if (transferences.getComponentCount() > 0) {

                        if (Helpers.mostrarMensajeInformativoSINO(this, "All transactions in progress or on hold will be lost. ARE YOU SURE?") == 0) {

                            Helpers.threadRun(() -> {

                                synchronized (TRANSFERENCES_LOCK) {

                                    Helpers.GUIRunAndWait(() -> {
                                        transferences_control_panel.setVisible(false);
                                        progressbar.setIndeterminate(true);
                                        getUpload_button().setEnabled(false);
                                        TRANSFERENCES_MAP.clear();
                                        transferences.removeAll();
                                        transferences.revalidate();
                                        transferences.repaint();
                                    });

                                    if (_current_transference != null) {
                                        _current_transference.stop();
                                    }
                                    Helpers.GUIRunAndWait(() -> {
                                        getCancel_all_button().setEnabled(true);
                                    });

                                    TRANSFERENCES_LOCK.notifyAll();
                                }

                            });

                        } else {
                            getCancel_all_button().setEnabled(true);
                        }
                    }
                });

            }
        });

    }//GEN-LAST:event_cancel_all_buttonActionPerformed

    private void tabbed_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabbed_panelMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            show_accountsMouseClicked(evt);
        }
    }//GEN-LAST:event_tabbed_panelMouseClicked

    private void clear_log_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_log_buttonActionPerformed
        // TODO add your handling code here:
        if (!busy() && Helpers.mostrarMensajeInformativoSINO(this, "SURE?") == 0) {
            output_textarea.setText("");
        }
    }//GEN-LAST:event_clear_log_buttonActionPerformed

    private void pause_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_buttonActionPerformed
        // TODO add your handling code here:

        if (!isPausing_transference()) {

            getPause_button().setEnabled(false);

            _pausing_transference = true;

            _transferences_paused = pause_button.getText().equals("PAUSE");

            getUpload_button().setEnabled(false);

            Helpers.threadRun(() -> {
                synchronized (TRANSFERENCES_LOCK) {

                    if (isSomeTransference_running()) {

                        if (this.isTransferences_paused()) {

                            _current_transference.pause();

                            Helpers.GUIRunAndWait(() -> {
                                getPause_button().setText("RESUME");
                                getPause_button().setEnabled(true);
                                getUpload_button().setEnabled(true);

                            });

                        } else {

                            _current_transference.resume();

                            Helpers.GUIRunAndWait(() -> {
                                getPause_button().setText("PAUSE");
                                getPause_button().setEnabled(true);
                                getUpload_button().setEnabled(true);

                            });

                        }

                    } else {
                        Helpers.GUIRunAndWait(() -> {
                            getPause_button().setEnabled(true);
                            getUpload_button().setEnabled(true);

                        });
                    }

                    _pausing_transference = false;

                    TRANSFERENCES_LOCK.notifyAll();
                }
            });
        }
    }//GEN-LAST:event_pause_buttonActionPerformed

    private void check_only_new_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_check_only_new_checkboxActionPerformed
        // TODO add your handling code here:
        this._check_only_new = check_only_new_checkbox.isSelected();
    }//GEN-LAST:event_check_only_new_checkboxActionPerformed

    private void show_accountsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_show_accountsMouseClicked
        // TODO add your handling code here:

        cuentas_scrollpanel.setVisible(!cuentas_scrollpanel.isVisible());
        Main.MEGADOCTOR_MISC.put("hide_accounts", !cuentas_scrollpanel.isVisible());
        saveMISC();
        show_accounts.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/vertical_" + (cuentas_scrollpanel.isVisible() ? "less" : "more") + ".png")));
        mainSplitPanel.resetToPreferredSizes();
        revalidate();
        repaint();
    }//GEN-LAST:event_show_accountsMouseClicked

    private void copy_all_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_all_buttonActionPerformed
        // TODO add your handling code here:
        Helpers.threadRun(() -> {

            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRunAndWait(() -> {
                    ArrayList<String> links = new ArrayList<>();

                    for (Component c : transferences.getComponents()) {
                        Transference t = Main.TRANSFERENCES_MAP.get(c);

                        if (t.isFinished() && t.getPublic_link() != null) {
                            String filename = new File(t.getLpath()).getName();
                            links.add(filename + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   " + t.getPublic_link());
                        }
                    }

                    Collections.sort(links);

                    if (!links.isEmpty()) {

                        Helpers.copyTextToClipboard(String.join("\n", links));
                        Helpers.mostrarMensajeInformativo(Main.MAIN_WINDOW, "ALL LINKS COPIED TO CLIPBOARD");
                    }

                });

            }

        });
    }//GEN-LAST:event_copy_all_buttonActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        AutoAllocationExcludedDialog dialog = new AutoAllocationExcludedDialog(this, true);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        String cuentas = dialog.getExcluded_accounts().getText().trim();

        MEGA_EXCLUDED_ACCOUNTS.clear();

        if (!cuentas.isEmpty()) {

            final String regex = "([^#\r\n]+)(?:#.+)?";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(dialog.getExcluded_accounts().getText().trim());

            while (matcher.find()) {
                if (!MEGA_EXCLUDED_ACCOUNTS.contains(matcher.group(1))) {
                    MEGA_EXCLUDED_ACCOUNTS.add(matcher.group(1));
                }
            }
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void formWindowIconified(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowIconified
        // TODO add your handling code here:

        //De momento lo desactivamos por problemas en Wayland
        /*if (SystemTray.isSupported()) {
            setVisible(false);
        }*/
    }//GEN-LAST:event_formWindowIconified

    private void purge_cache_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_purge_cache_menuActionPerformed
        // TODO add your handling code here:
        if (Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "This will cancel all MEGAcmd transfers and delete MEGAcmd cache folder.\nAll scheduled backups of MEGAcmd will also be removed.\n\n<span color='red'><b>CONTINUE?</b></span>") == 0) {

            purge_cache_menu.setEnabled(false);

            Helpers.threadRun(() -> {
                purgeMEGAcmdCache();
            });
        }
    }//GEN-LAST:event_purge_cache_menuActionPerformed

    private void menu_httpsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_httpsActionPerformed
        // TODO add your handling code here:
        Main.MEGADOCTOR_MISC.put("megacmd_https", menu_https.isSelected());
        saveMISC();
    }//GEN-LAST:event_menu_httpsActionPerformed

    private void new_account_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_account_buttonActionPerformed
        // TODO add your handling code here:
        _running_main_action = true;

        enableTOPControls(false);
        MAIN_WINDOW.getProgressbar().setIndeterminate(true);
        MAIN_WINDOW.getStatus_label().setText("Creating new account, please wait...");

        Helpers.threadRun(() -> {

            synchronized (TRANSFERENCES_LOCK) {

                logout(true);

                if (Helpers.mostrarMensajeInformativoSINO(this,
                        "This feature helps in the creation of a MEGA account through its official API"
                        + "\n(in case you do not want or are unable to use a web browser)."
                        + "\n<b>YOU MUST AGREE TO ITS TERMS OF USE</b>"
                        + "\n\n<b>CONTINUE?</b>") == 0) {

                    String[] account = Helpers.registerNewMEGAaccount();

                    if (account != null) {

                        Helpers.mostrarMensajeInformativo(this, "<b>Account successfully created (copied to clipboard)</b>\n" + String.join("#", account));

                        output_textarea_append("\nAccount successfully created (copied to clipboard):\n" + String.join("#", account) + "\n\n");

                        Helpers.GUIRun(() -> {
                            this.cuentas_textarea.append("\n" + String.join("#", account));
                        });

                        Helpers.copyTextToClipboard(String.join("#", account));

                    } else {
                        Helpers.mostrarMensajeInformativo(this, "SOMETHING FAILED (try again later)");
                    }
                }

                Helpers.GUIRunAndWait(() -> {
                    enableTOPControls(true);
                    MAIN_WINDOW.getProgressbar().setIndeterminate(false);
                    MAIN_WINDOW.getStatus_label().setText("");
                });

                _running_main_action = false;

                TRANSFERENCES_LOCK.notifyAll();
            }

        });
    }//GEN-LAST:event_new_account_buttonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        UIManager.put("OptionPane.messageFont", new Font("Noto Sans", Font.PLAIN, MESSAGE_DIALOG_FONT_SIZE));
        UIManager.put("OptionPane.buttonFont", new Font("Noto Sans", Font.PLAIN, MESSAGE_DIALOG_FONT_SIZE));

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        try {
            ONE_INSTANCE_SOCKET = new ServerSocket(MEGADOCTOR_ONE_INSTANCE_PORT);

            //Helpers.createTrayIcon();
            Helpers.createMegaDoctorDir();

            /* Create and display the form */
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {

                    MAIN_WINDOW = new Main();
                    MAIN_WINDOW.init();
                    MAIN_WINDOW.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    MAIN_WINDOW.setVisible(true);
                }
            });

        } catch (Exception ex) {
            if (Helpers.mostrarMensajeInformativoSINO(null, "<b>THERE IS ANOTHER MEGADOCTOR INSTANCE ALREADY RUNNING</b>\n\nIT IS STRONGLY DISCOURAGED TO RUN SEVERAL INSTANCES OF MEGADOCTOR\nAT THE SAME TIME BECAUSE THEY COULD CORRUPT YOUR FILES\n\n<b>Continue anyway ignoring this warning?</b>") == 0) {

                if (ONE_INSTANCE_SOCKET != null) {
                    try {
                        ONE_INSTANCE_SOCKET.close();
                    } catch (IOException ex2) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                Helpers.createMegaDoctorDir();

                /* Create and display the form */
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {

                        MAIN_WINDOW = new Main();
                        MAIN_WINDOW.init();
                        MAIN_WINDOW.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        MAIN_WINDOW.setVisible(true);
                    }
                });

            } else {

                if (ONE_INSTANCE_SOCKET != null) {
                    try {
                        ONE_INSTANCE_SOCKET.close();
                    } catch (IOException ex2) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                Main.EXIT = true;

                Helpers.destroyAllExternalProcesses();

                System.exit(1);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar barra_menu;
    private javax.swing.JButton cancel_all_button;
    private javax.swing.JCheckBox check_only_new_checkbox;
    private javax.swing.JButton clear_log_button;
    private javax.swing.JButton clear_trans_button;
    private javax.swing.JButton copy_all_button;
    private javax.swing.JScrollPane cuentas_scrollpanel;
    private javax.swing.JTextArea cuentas_textarea;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JLabel logo_label;
    private javax.swing.JSplitPane mainSplitPanel;
    private javax.swing.JCheckBoxMenuItem menu_https;
    private javax.swing.JButton new_account_button;
    private javax.swing.JMenu options_menu;
    private javax.swing.JTextPane output_textarea;
    private javax.swing.JButton pause_button;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JMenuItem purge_cache_menu;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBoxMenuItem session_menu;
    private javax.swing.JLabel show_accounts;
    private javax.swing.JLabel status_label;
    private javax.swing.JTabbedPane tabbed_panel;
    private javax.swing.JScrollPane transf_scroll;
    private javax.swing.JPanel transferences_control_panel;
    private javax.swing.JPanel transferences_panel;
    private javax.swing.JButton upload_button;
    private javax.swing.JButton vamos_button;
    // End of variables declaration//GEN-END:variables
}
