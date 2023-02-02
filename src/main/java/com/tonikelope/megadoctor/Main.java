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
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

/**
 *
 * @author tonikelope
 */
public class Main extends javax.swing.JFrame {

    public final static String VERSION = "0.82";
    public final static ThreadPoolExecutor THREAD_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public final static String MEGA_CMD_URL = "https://mega.io/cmd";
    public final static String MEGA_CMD_WINDOWS_PATH = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\MEGAcmd";
    public final static String SESSIONS_FILE = System.getProperty("user.home") + File.separator + ".megadoctor_sessions";
    public final static String ACCOUNTS_FILE = System.getProperty("user.home") + File.separator + ".megadoctor_accounts";
    public final static String TRANSFERS_FILE = System.getProperty("user.home") + File.separator + ".megadoctor_transfers";
    public volatile static String MEGA_CMD_VERSION = null;
    public volatile static Main MAIN_WINDOW;
    public static final Object TRANSFERENCES_LOCK = new Object();
    public static LinkedHashMap<String, String> MEGA_ACCOUNTS = new LinkedHashMap<>();
    public volatile static HashMap<String, String> MEGA_SESSIONS = new HashMap<>();
    public final static HashMap<String, Object[]> MEGA_NODES = new HashMap<>();
    private final static ArrayList<String[]> MEGA_ACCOUNTS_SPACE = new ArrayList<>();
    private final static ArrayList<String> MEGA_ACCOUNTS_LOGIN_ERROR = new ArrayList<>();
    private volatile boolean _running_main_action = false;
    private volatile boolean _running_global_check = false;
    private volatile boolean _aborting_global_check = false;
    private volatile boolean _closing = false;
    private volatile boolean _firstAccountsTextareaClick = false;
    private volatile MoveNodeToAnotherAccountDialog _email_dialog = null;
    private volatile MoveNodeDialog _move_dialog = null;
    private volatile boolean _transferences_running = false;
    private volatile Transference _current_transference = null;
    private volatile String _last_email_force_refresh = null;

    public boolean busy() {
        return isRunning_global_check() || isRunning_main_action() || isTransferences_running();
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

    public boolean isTransferences_running() {
        return _transferences_running;
    }

    public String getLast_email_force_refresh() {
        return _last_email_force_refresh;
    }

    public JTextArea getCuentas_textarea() {
        return cuentas_textarea;
    }

    public JTextArea getOutput_textarea() {
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

    public Transference getCurrent_transference() {
        return _current_transference;
    }

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        Helpers.JTextFieldRegularPopupMenu.addTo(cuentas_textarea);
        Helpers.JTextFieldRegularPopupMenu.addTo(output_textarea);
        progressbar.setMinimum(0);
        upload_button.setEnabled(false);
        transf_scroll.getVerticalScrollBar().setUnitIncrement(20);
        setTitle("MegaDoctor " + VERSION);
        status_label.setText("Checking if MEGACMD is present...");
        pack();
        setEnabled(false);

    }

    private void runMEGACMDCHecker() {

        Helpers.threadRun(() -> {

            MEGA_CMD_VERSION = Helpers.runProcess(new String[]{"mega-version"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (MEGA_CMD_VERSION == null || "".equals(MEGA_CMD_VERSION)) {
                Helpers.mostrarMensajeError(this, "MEGA CMD IS REQUIRED");
                Helpers.openBrowserURLAndWait(MEGA_CMD_URL);
                System.exit(1);
            }

            Helpers.GUIRun(() -> {
                setEnabled(true);
                status_label.setText("");
            });

        });

    }

    private void runTransferenceWatchdog() {
        Helpers.threadRun(() -> {

            while (!_closing) {

                synchronized (TRANSFERENCES_LOCK) {
                    try {
                        TRANSFERENCES_LOCK.wait(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                synchronized (TRANSFERENCES_LOCK) {
                    Helpers.GUIRunAndWait(() -> {

                        if (transferences.getComponentCount() > 0) {

                            transferences_control_panel.setVisible(true);

                            _transferences_running = false;

                            for (Component t : transferences.getComponents()) {

                                Transference trans = (Transference) t;

                                if (trans.isRunning()) {
                                    _transferences_running = true;
                                    _current_transference = trans;
                                    break;
                                }
                            }

                            if (!_transferences_running) {
                                for (Component t : transferences.getComponents()) {

                                    Transference trans = (Transference) t;

                                    if (!trans.isRunning() && !trans.isFinished() && !trans.isCanceled()) {
                                        _transferences_running = true;
                                        _current_transference = trans;
                                        trans.start();
                                        break;
                                    }
                                }
                            }

                            cancel_trans_button.setEnabled(_transferences_running);

                            vamos_button.setEnabled(!busy() || (isRunning_global_check() && !isAborting_global_check()));

                            cuentas_textarea.setEnabled(!busy());

                        } else {
                            _transferences_running = false;

                            _current_transference = null;

                            transferences_control_panel.setVisible(false);

                            vamos_button.setEnabled(!busy() || (isRunning_global_check() && !isAborting_global_check()));

                            cuentas_textarea.setEnabled(!busy());
                        }

                    });
                }
            }

        });
    }

    public void init() {
        runMEGACMDCHecker();
        runTransferenceWatchdog();
        loadAccounts();
    }

    public boolean login(String email) {

        if (Helpers.megaWhoami().equals(email.toLowerCase())) {
            return true;
        }

        logout(true);

        String session = MEGA_SESSIONS.get(email);

        String password = MEGA_ACCOUNTS.get(email);

        if (session != null) {

            Helpers.GUIRunAndWait(() -> {
                status_label.setForeground(new Color(0, 153, 0));
            });

            String login_session_output = Helpers.runProcess(new String[]{"mega-login", MEGA_SESSIONS.get(email)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (login_session_output.startsWith("[API:err:")) {

                Helpers.GUIRunAndWait(() -> {
                    status_label.setForeground(Color.DARK_GRAY);
                });

                String login = Helpers.runProcess(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(password)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                if (login.startsWith("[API:err:")) {
                    Helpers.GUIRunAndWait(() -> {
                        status_label.setForeground(Color.BLACK);
                    });
                    return false;
                }
            }

        } else {

            String login = Helpers.runProcess(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(password)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (login.startsWith("[API:err:")) {
                Helpers.GUIRunAndWait(() -> {
                    status_label.setForeground(Color.BLACK);
                });
                return false;
            }
        }

        Helpers.runProcess(new String[]{"mega-killsession", "-a"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        MEGA_SESSIONS.remove(email);

        MEGA_SESSIONS.put(email, getCurrentSessionID());

        if (_transferences_running && _current_transference.getEmail().equals(email) && _current_transference.isPaused()) {
            _current_transference.pause();
        }

        Helpers.GUIRunAndWait(() -> {
            status_label.setForeground(Color.BLACK);
        });

        return true;
    }

    public String DUWithHandles() {

        String ls = Helpers.runProcess(new String[]{"mega-ls", "/", "--show-handles"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        int max_path_width = 0;

        for (String s : ls.split("\n")) {
            if (s.length() > max_path_width) {
                max_path_width = s.length();
            }
        }

        String du = Helpers.runProcess(new String[]{"mega-du", "-h", "--use-pcre", "/.*", "--path-display-size=" + String.valueOf(max_path_width + 1)}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        if (!du.trim().isEmpty() && !ls.trim().isEmpty()) {

            final String regex = "(.+) <H:[^>]+>";

            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

            final Matcher matcher = pattern.matcher(ls);

            while (matcher.find()) {
                du = du.replace(matcher.group(1), matcher.group(0));
            }

            String[] du_lines = du.split("\n");

            du_lines[du_lines.length - 2] += "-------------";

            du_lines[0] = du_lines[0].replaceAll("( +)", "$1             ");

            du_lines[du_lines.length - 1] = du_lines[du_lines.length - 1].replaceAll("(used:)( +)(\\d)", "$1             $2$3");

            return String.join("\n", du_lines);
        } else {
            return "";
        }
    }

    public void logout(boolean keep_session) {
        if (keep_session) {
            Helpers.runProcess(new String[]{"mega-logout", "--keep-session"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
        } else {
            Helpers.runProcess(new String[]{"mega-logout"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
        }
    }

    private String getCurrentSessionID() {

        String session_output = Helpers.runProcess(new String[]{"mega-session"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return session_output.replaceAll("^.+: +(.+)$", "$1").trim();
    }

    private void enableButtons(boolean enable) {
        Helpers.GUIRun(() -> {
            MAIN_WINDOW.getCuentas_textarea().setEnabled(enable);
            clear_log_button.setEnabled(enable);
            MAIN_WINDOW.getVamos_button().setEnabled(enable || isRunning_global_check());
            upload_button.setEnabled(enable && !MEGA_ACCOUNTS.isEmpty());
            MAIN_WINDOW.getSave_button().setEnabled(enable);
        });
    }

    public void copyNodesToAnotherAccount(String text, final boolean move) {

        _running_main_action = true;

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText((move ? "MOVING" : "COPYING") + " SELECTED FOLDERS/FILES. PLEASE WAIT...");

            if (_transferences_running) {
                upload_button.setText("PAUSING CURRENT TRANSFER...");
            }

        });

        HashMap<String, ArrayList<String>> nodesToCopy = Helpers.extractNodeMapFromText(text);

        if (!nodesToCopy.isEmpty() && MEGA_ACCOUNTS.size() > nodesToCopy.keySet().size()) {

            if (_transferences_running) {
                _current_transference.pause();
            }

            Helpers.GUIRunAndWait(() -> {
                _email_dialog = new MoveNodeToAnotherAccountDialog(MAIN_WINDOW, true, nodesToCopy.keySet(), move);

                _email_dialog.setLocationRelativeTo(MAIN_WINDOW);

                _email_dialog.setVisible(true);
            });

            if (_transferences_running) {
                Helpers.threadRun(() -> {
                    _current_transference.resume();
                });
            }

            String email = _email_dialog.getSelected_email();

            if (_email_dialog.isOk() && email != null && !email.isBlank()) {

                login(email);

                String[] account_space = Helpers.getAccountSpaceData(email);

                if (Helpers.getNodeMapTotalSize(nodesToCopy) <= (Long.parseLong(account_space[2]) - Long.parseLong(account_space[1]))) {

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

                    String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String du = DUWithHandles();

                    String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    parseAccountNodes(email);

                    logout(true);

                    Helpers.GUIRun(() -> {

                        output_textarea.append("\n[" + email + "] (Refreshed after insertion)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                    });

                    if (move) {

                        for (String email_rm : nodesToCopy.keySet()) {

                            ArrayList<String> node_list = nodesToCopy.get(email_rm);

                            ArrayList<String> delete_command = new ArrayList<>();

                            delete_command.add("mega-rm");

                            delete_command.add("-rf");

                            delete_command.addAll(node_list);

                            login(email_rm);

                            Helpers.runProcess(delete_command.toArray(String[]::new), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            String ls_rm = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            String du_rm = DUWithHandles();

                            String df_rm = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            parseAccountNodes(email_rm);

                            Helpers.GUIRun(() -> {

                                output_textarea.append("\n[" + email_rm + "] (Refreshed after deletion)\n\n" + df_rm + "\n" + du_rm + "\n" + ls_rm + "\n\n");

                            });
                        }

                        logout(true);

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES MOVED");
                    } else {
                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES COPIED");
                    }
                } else {
                    Helpers.mostrarMensajeError(MAIN_WINDOW, "THERE IS NO ENOUGH FREE SPACE IN DESTINATION ACCOUNT");
                }

            }

        } else if (nodesToCopy.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:XXXXXXXX MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);
            upload_button.setText("NEW UPLOAD");
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void bye() {

        _closing = true;

        Helpers.threadRun(() -> {

            if (_transferences_running) {
                Helpers.GUIRun(() -> {
                    setEnabled(false);
                });
                _current_transference.pause();
            } else {
                Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            if (session_menu.isSelected() || Helpers.mostrarMensajeInformativoSINO(this, "Do you want to save your MEGA accounts/sessions/transfers to disk to speed up next time?\n\n(If you are using a public computer it is NOT recommended to do so for security reasons).") == 0) {
                saveAccounts();
                saveTransfers();
                logout(true);
            } else {
                removeSessionFILES();
                logout(false);
            }

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

    public void saveTransfers() {

        ArrayList<Object[]> trans = new ArrayList<>();

        Helpers.GUIRunAndWait(() -> {
            if (transferences.getComponentCount() > 0) {

                for (Component c : transferences.getComponents()) {

                    Transference t = (Transference) c;

                    if (!t.isFinished() && !t.isCanceled()) {

                        String email = t.getEmail();

                        String lpath = t.getLpath();

                        String rpath = t.getRpath();

                        int action = t.getAction();

                        trans.add(new Object[]{email, lpath, rpath, action});
                    }
                }

                try ( FileOutputStream fos = new FileOutputStream(TRANSFERS_FILE);  ObjectOutputStream oos = new ObjectOutputStream(fos)) {

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

    public void loadTransfers() {
        if (Files.exists(Paths.get(TRANSFERS_FILE))) {
            try ( FileInputStream fis = new FileInputStream(TRANSFERS_FILE);  ObjectInputStream ois = new ObjectInputStream(fis)) {

                ArrayList<Object[]> trans = (ArrayList<Object[]>) ois.readObject();

                ArrayList<Transference> valid_trans = new ArrayList<>();

                if (!trans.isEmpty()) {
                    synchronized (TRANSFERENCES_LOCK) {

                        Helpers.GUIRunAndWait(() -> {
                            try {

                                for (Object[] o : trans) {
                                    if (MEGA_SESSIONS.containsKey((String) o[0])) {
                                        Transference t = new Transference((String) o[0], (String) o[1], (String) o[2], (int) o[3]);
                                        transferences.add(t);
                                        valid_trans.add(t);
                                    }
                                }
                                transferences.revalidate();
                                transferences.repaint();
                                tabbed_panel.setSelectedIndex(1);
                                vamos_button.setEnabled(false);
                                cuentas_textarea.setEnabled(false);

                            } catch (Exception ex) {
                                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });

                        if (!valid_trans.isEmpty()) {
                            Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                        }

                        TRANSFERENCES_LOCK.notifyAll();
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void copyNodesInsideAccount(String text) {

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("COPYING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToCopy = Helpers.extractNodeMapFromText(text);

        boolean cancel = false;

        if (!nodesToCopy.isEmpty()) {

            for (String email : nodesToCopy.keySet()) {

                login(email);
                ArrayList<String> node_list = nodesToCopy.get(email);

                int conta = 0;

                for (String node : node_list) {

                    String old_full_path = Helpers.getNodePathFromFindCommandOutput(node, Helpers.runProcess(new String[]{"mega-find", "--show-handles", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                    Helpers.GUIRunAndWait(() -> {
                        _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, old_full_path, 1, "[" + email + "]\n" + Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                        _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                        _move_dialog.setVisible(true);
                    });

                    String new_full_path = _move_dialog.getNew_name().getText().trim();

                    if (_move_dialog.isOk() && !old_full_path.equals(new_full_path) && !new_full_path.isBlank()) {

                        String folder = new_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                        Helpers.runProcess(new String[]{"mega-mkdir", "-p", folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        Helpers.runProcess(new String[]{"mega-cp", node, new_full_path}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        conta++;

                    } else if (!_move_dialog.isOk()) {
                        cancel = true;
                        break;
                    }
                }

                if (conta > 0) {

                    String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String du = DUWithHandles();

                    String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    parseAccountNodes(email);

                    Helpers.GUIRun(() -> {

                        output_textarea.append("\n[" + email + "] (Refreshed after copying)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                    });

                }

                if (cancel) {
                    Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT MOVED)");
                    break;
                }

            }

            logout(true);

            if (_move_dialog.isOk()) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES COPIED");
            }

        } else if (nodesToCopy.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void moveNodesInsideAccount(String text) {

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("MOVING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToMove = Helpers.extractNodeMapFromText(text);

        if (!nodesToMove.isEmpty()) {

            for (String email : nodesToMove.keySet()) {

                login(email);
                ArrayList<String> node_list = nodesToMove.get(email);

                int conta = 0;

                for (String node : node_list) {

                    String old_full_path = Helpers.getNodePathFromFindCommandOutput(node, Helpers.runProcess(new String[]{"mega-find", "--show-handles", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                    Helpers.GUIRunAndWait(() -> {
                        _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, old_full_path, 2, "[" + email + "]\n" + Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                        _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                        _move_dialog.setVisible(true);
                    });

                    String new_full_path = _move_dialog.getNew_name().getText().trim();

                    if (_move_dialog.isOk() && !old_full_path.equals(new_full_path) && !new_full_path.isBlank()) {

                        String folder = new_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                        Helpers.runProcess(new String[]{"mega-mkdir", "-p", folder}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        Helpers.runProcess(new String[]{"mega-mv", node, new_full_path}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        conta++;

                    } else if (!_move_dialog.isOk()) {
                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT MOVED)");
                        break;
                    }
                }

                if (conta > 0) {

                    String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String du = DUWithHandles();

                    String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    parseAccountNodes(email);

                    Helpers.GUIRun(() -> {

                        output_textarea.append("\n[" + email + "] (Refreshed after moving)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                    });

                }

            }

            logout(true);

            if (_move_dialog.isOk()) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES RENAMED");
            }

        } else if (nodesToMove.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void renameNodes(String text) {

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("RENAMING SELECTED FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToRename = Helpers.extractNodeMapFromText(text);

        if (!nodesToRename.isEmpty()) {

            for (String email : nodesToRename.keySet()) {

                login(email);
                ArrayList<String> node_list = nodesToRename.get(email);

                int conta = 0;

                for (String node : node_list) {

                    String old_full_path = Helpers.getNodePathFromFindCommandOutput(node, Helpers.runProcess(new String[]{"mega-find", "--show-handles", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                    String old_name = old_full_path.replaceAll("^.*/([^/]*)$", "$1");

                    String old_path = old_full_path.replaceAll("^(.*/)[^/]*$", "$1");

                    Helpers.GUIRunAndWait(() -> {
                        _move_dialog = new MoveNodeDialog(MAIN_WINDOW, true, old_full_path, 0, "[" + email + "]\n" + Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]);

                        _move_dialog.setLocationRelativeTo(MAIN_WINDOW);

                        _move_dialog.setVisible(true);
                    });

                    String new_name = _move_dialog.getNew_name().getText().trim();

                    if (_move_dialog.isOk() && !old_name.equals(new_name) && !new_name.isBlank()) {

                        Helpers.runProcess(new String[]{"mega-mv", node, old_path + new_name}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        conta++;

                    } else if (!_move_dialog.isOk()) {
                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "CANCELED (SOME FOLDERS/FILES WERE NOT RENAMED)");
                        break;
                    }
                }

                if (conta > 0) {

                    String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String du = DUWithHandles();

                    String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    parseAccountNodes(email);

                    Helpers.GUIRun(() -> {

                        output_textarea.append("\n[" + email + "] (Refreshed after rename)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                    });

                }

            }

            logout(true);

            if (_move_dialog.isOk()) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL FOLDERS/FILES RENAMED");
            }

        } else if (nodesToRename.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void exportNodes(String text, boolean enable) {

        _running_main_action = true;

        Helpers.GUIRun(() -> {

            enableButtons(false);
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

                String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                String du = DUWithHandles();

                String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                Helpers.GUIRun(() -> {

                    output_textarea.append("\n[" + email + "] (Refreshed after public links generated/removed)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                });
            }

            logout(true);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES " + (enable ? "PUBLIC LINKS GENERATED" : "PUBLIC LINKS REMOVED"));
        } else if (nodesToExport.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void truncateAccount(String email) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("TRUNCATING " + email + " PLEASE WAIT...");

        });

        if (MEGA_ACCOUNTS.containsKey(email) && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "CAUTION!! ALL CONTENT INSIDE <b>" + email + "</b> WILL BE <b>PERMANENTLY</b> DELETED.<br><br>ARE YOU SURE?") == 0 && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "Sorry for asking you again... ARE YOU SURE?") == 0) {

            login(email);

            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'//in/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            Helpers.runProcess(new String[]{"mega-rm", "-rf", "'//bin/*'"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            forceRefreshAccount(email, "Refreshed after account truncate", false, false);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, email + " TRUNCATED");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);

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

        Helpers.GUIRun(() -> {

            enableButtons(false);
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

                forceRefreshAccount(email, "Refreshed after deletion", false, false);
            }

            logout(true);

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES DELETED");
        } else if (nodesToRemove.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    public void forceRefreshAccount(String email, String reason, boolean notification, boolean login) {

        _running_main_action = true;

        String old_status = MAIN_WINDOW.getStatus_label().getText();

        Helpers.GUIRun(() -> {

            enableButtons(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("REFRESHING " + email + " PLEASE WAIT...");

        });

        if (MEGA_ACCOUNTS.containsKey(email)) {

            if (login) {
                login(email);
            }

            Helpers.runProcess(new String[]{"mega-reload"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            String du = DUWithHandles();

            String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            parseAccountNodes(email);

            Helpers.GUIRun(() -> {

                output_textarea.append("\n[" + email + "] (" + reason + ")\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

            });

            if (login) {
                logout(true);
            }

            _last_email_force_refresh = email;

            Helpers.JTextFieldRegularPopupMenu.refreshLastAccount.setText("REFRESH LAST (" + email + ")");

            Helpers.JTextFieldRegularPopupMenu.refreshLastAccount.setEnabled(true);

            if (notification) {
                Helpers.mostrarMensajeInformativo(MAIN_WINDOW, email + " REFRESHED");
            }

        } else {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "YOU MUST SELECT AN ALREADY CHECKED ACCOUNT");
        }

        Helpers.GUIRun(() -> {

            enableButtons(true);

            if (old_status.isBlank()) {
                MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            }

            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running_main_action = false;
    }

    private void saveAccounts() {

        try ( FileOutputStream fos = new FileOutputStream(ACCOUNTS_FILE);  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(MEGA_ACCOUNTS);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try ( FileOutputStream fos = new FileOutputStream(SESSIONS_FILE);  ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(MEGA_SESSIONS);

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadAccounts() {

        Helpers.threadRun(() -> {

            if (Files.exists(Paths.get(ACCOUNTS_FILE))) {
                try ( FileInputStream fis = new FileInputStream(ACCOUNTS_FILE);  ObjectInputStream ois = new ObjectInputStream(fis)) {

                    MEGA_ACCOUNTS = (LinkedHashMap<String, String>) ois.readObject();

                    if (!MEGA_ACCOUNTS.isEmpty()) {

                        ArrayList<String> accounts = new ArrayList<>();

                        for (String k : MEGA_ACCOUNTS.keySet()) {
                            accounts.add(k + "#" + MEGA_ACCOUNTS.get(k));
                        }

                        Collections.sort(accounts);

                        Helpers.GUIRunAndWait(() -> {

                            if (!_firstAccountsTextareaClick) {
                                _firstAccountsTextareaClick = true;
                                cuentas_textarea.setText("");
                                cuentas_textarea.setForeground(null);
                            }

                            for (String account : accounts) {
                                cuentas_textarea.append(account + "\n");
                            }

                            upload_button.setEnabled(true);

                            session_menu.setSelected(true);

                        });
                    } else {
                        Helpers.GUIRun(() -> {
                            session_menu.setSelected(false);
                        });
                    }

                } catch (Exception ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (Files.exists(Paths.get(SESSIONS_FILE))) {
                    try ( FileInputStream fis = new FileInputStream(SESSIONS_FILE);  ObjectInputStream ois = new ObjectInputStream(fis)) {
                        MEGA_SESSIONS = (HashMap<String, String>) ois.readObject();

                        if (!MEGA_SESSIONS.isEmpty()) {
                            loadTransfers();
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else {
                Helpers.GUIRun(() -> {
                    session_menu.setSelected(false);
                });
            }

        });
    }

    public void parseAccountNodes(String email) {

        String ls = Helpers.runProcess(new String[]{"mega-ls", "-lr", "--show-handles"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        final String regex = "([0-9]+|-) +[^ ]+ +[^ ]+ +(H:[^ ]+) (.+)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(ls);

        while (matcher.find()) {
            MEGA_NODES.put(matcher.group(2), new Object[]{Long.parseLong(matcher.group(1).equals("-") ? "0" : matcher.group(1)), email, matcher.group(3)});
        }
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
        cuentas_scrollpanel = new javax.swing.JScrollPane();
        cuentas_textarea = new javax.swing.JTextArea();
        vamos_button = new javax.swing.JButton();
        status_label = new javax.swing.JLabel();
        progressbar = new javax.swing.JProgressBar();
        save_button = new javax.swing.JButton();
        tabbed_panel = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        output_textarea = new javax.swing.JTextArea();
        transf_scroll = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        transferences_control_panel = new javax.swing.JPanel();
        cancel_trans_button = new javax.swing.JButton();
        clear_trans_button = new javax.swing.JButton();
        transferences = new javax.swing.JPanel();
        upload_button = new javax.swing.JButton();
        clear_log_button = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu2 = new javax.swing.JMenu();
        session_menu = new javax.swing.JCheckBoxMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("MegaDoctor");
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/megadoctor_51.png")).getImage() );
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        logo_label.setBackground(new java.awt.Color(255, 255, 255));
        logo_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/megadoctor_frame.png"))); // NOI18N
        logo_label.setDoubleBuffered(true);

        cuentas_textarea.setColumns(20);
        cuentas_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        cuentas_textarea.setForeground(new java.awt.Color(204, 204, 204));
        cuentas_textarea.setRows(5);
        cuentas_textarea.setText("xxxxxxxxxxxxxxxxxxxx1@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx2@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx3@lalalalala.com#password\nxxxxxxxxxxxxxxxxxxxx4@lalalalala.com#password");
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

        tabbed_panel.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        tabbed_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tabbed_panelMouseClicked(evt);
            }
        });

        output_textarea.setEditable(false);
        output_textarea.setBackground(new java.awt.Color(102, 102, 102));
        output_textarea.setColumns(20);
        output_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        output_textarea.setForeground(new java.awt.Color(255, 255, 255));
        output_textarea.setRows(5);
        jScrollPane1.setViewportView(output_textarea);

        tabbed_panel.addTab("Log", new javax.swing.ImageIcon(getClass().getResource("/images/log.png")), jScrollPane1); // NOI18N

        transf_scroll.setBorder(null);
        transf_scroll.setDoubleBuffered(true);

        cancel_trans_button.setBackground(new java.awt.Color(255, 51, 0));
        cancel_trans_button.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        cancel_trans_button.setForeground(new java.awt.Color(255, 255, 255));
        cancel_trans_button.setText("CANCEL ALL");
        cancel_trans_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancel_trans_button.setDoubleBuffered(true);
        cancel_trans_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_trans_buttonActionPerformed(evt);
            }
        });

        clear_trans_button.setBackground(new java.awt.Color(0, 153, 0));
        clear_trans_button.setFont(new java.awt.Font("Noto Sans", 1, 14)); // NOI18N
        clear_trans_button.setForeground(new java.awt.Color(255, 255, 255));
        clear_trans_button.setText("Clear all finished");
        clear_trans_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        clear_trans_button.setDoubleBuffered(true);
        clear_trans_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_trans_buttonActionPerformed(evt);
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
                .addComponent(cancel_trans_button)
                .addContainerGap(835, Short.MAX_VALUE))
        );
        transferences_control_panelLayout.setVerticalGroup(
            transferences_control_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transferences_control_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transferences_control_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancel_trans_button)
                    .addComponent(clear_trans_button))
                .addContainerGap())
        );

        transferences.setLayout(new javax.swing.BoxLayout(transferences, javax.swing.BoxLayout.PAGE_AXIS));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transferences_control_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(transferences, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(transferences_control_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(transferences, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        transf_scroll.setViewportView(jPanel1);

        tabbed_panel.addTab("Transferences", new javax.swing.ImageIcon(getClass().getResource("/images/transfers.png")), transf_scroll); // NOI18N

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

        jMenuBar1.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

        jMenu2.setText("Options");
        jMenu2.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

        session_menu.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        session_menu.setSelected(true);
        session_menu.setText("Keep session on disk");
        jMenu2.add(session_menu);

        jMenuBar1.add(jMenu2);

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

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(logo_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(vamos_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(status_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(upload_button)
                                .addGap(18, 18, 18)
                                .addComponent(save_button, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(clear_log_button))))
                    .addComponent(cuentas_scrollpanel)
                    .addComponent(progressbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabbed_panel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logo_label)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(vamos_button)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(save_button)
                                .addComponent(upload_button))
                            .addComponent(clear_log_button, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(status_label)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cuentas_scrollpanel, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbed_panel, javax.swing.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void vamos_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vamos_buttonActionPerformed
        // TODO add your handling code here:

        if (MEGA_CMD_VERSION != null) {

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

                enableButtons(false);

                Helpers.threadRun(() -> {
                    final String regex = "(.*?)#(.+)";
                    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                    final Matcher matcher = pattern.matcher(cuentas_textarea.getText());

                    LinkedHashMap<String, String> mega_accounts = new LinkedHashMap<>();

                    MEGA_ACCOUNTS_SPACE.clear();

                    MEGA_ACCOUNTS_LOGIN_ERROR.clear();

                    while (matcher.find()) {
                        MEGA_ACCOUNTS.put(matcher.group(1), matcher.group(2));
                        mega_accounts.put(matcher.group(1), matcher.group(2));
                    }

                    if (!mega_accounts.isEmpty()) {
                        Helpers.GUIRun(() -> {
                            progressbar.setMaximum(mega_accounts.size());
                            output_textarea.append(" __  __                  ____             _             \n"
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
                                MEGA_ACCOUNTS_LOGIN_ERROR.add(email + "#" + mega_accounts.get(email));
                                Helpers.GUIRun(() -> {
                                    output_textarea.append("\n[" + email + "] LOGIN ERROR\n\n");
                                });

                            } else {

                                Helpers.GUIRun(() -> {
                                    status_label.setText("Reading " + email + " info...");
                                });

                                String ls = Helpers.runProcess(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                String du = DUWithHandles();

                                String df = Helpers.runProcess(new String[]{"mega-df", "-h"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                Helpers.GUIRun(() -> {

                                    output_textarea.append("\n[" + email + "]\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                                });

                                MEGA_ACCOUNTS_SPACE.add(Helpers.getAccountSpaceData(email));

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

                        Collections.sort(MEGA_ACCOUNTS_SPACE, new Comparator<String[]>() {
                            @Override
                            public int compare(String[] o1, String[] o2) {

                                return Long.compare(Long.parseLong(o2[2]) - Long.parseLong(o2[1]), Long.parseLong(o1[2]) - Long.parseLong(o1[1]));
                            }
                        });

                        Helpers.GUIRun(() -> {
                            output_textarea.append("--------------------------------------\n");
                            output_textarea.append("ACCOUNTS ORDERED BY FREE SPACE (DESC):\n");
                            output_textarea.append("--------------------------------------\n\n");
                            long total_space = 0;
                            long total_space_used = 0;
                            for (String[] account : MEGA_ACCOUNTS_SPACE) {
                                total_space_used += Long.parseLong(account[1]);
                                total_space += Long.parseLong(account[2]);
                                output_textarea.append(account[0] + " [" + Helpers.formatBytes(Long.parseLong(account[2]) - Long.parseLong(account[1])) + " FREE] (of " + Helpers.formatBytes(Long.parseLong(account[2])) + ")\n\n");
                            }

                            output_textarea.append("TOTAL FREE SPACE: " + Helpers.formatBytes(total_space - total_space_used) + " (of " + Helpers.formatBytes(total_space) + ")\n\n");

                            if (!MEGA_ACCOUNTS_LOGIN_ERROR.isEmpty()) {
                                output_textarea.append("(WARNING) LOGIN ERRORS: " + String.valueOf(MEGA_ACCOUNTS_LOGIN_ERROR.size()) + "\n");
                                for (String errors : MEGA_ACCOUNTS_LOGIN_ERROR) {
                                    output_textarea.append("    ERROR: " + errors + "\n");
                                }
                            }

                            output_textarea.append("\nCHECKING END -> " + Helpers.getFechaHoraActual() + "\n");

                            upload_button.setEnabled(true);

                        });

                        Helpers.mostrarMensajeInformativo(this, isAborting_global_check() ? "CANCELED!" : "DONE");

                    } else {
                        Helpers.mostrarMensajeInformativo(this, "ALL ACCOUNTS REFRESHED");
                    }

                    Helpers.GUIRun(() -> {
                        progressbar.setValue(0);
                        vamos_button.setText("CHECK ACCOUNTS");
                        vamos_button.setBackground(new Color(0, 153, 0));
                        cuentas_textarea.setEnabled(true);
                        status_label.setText("");
                        enableButtons(true);
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

        Helpers.setContainerFont(fileChooser, save_button.getFont().deriveFont(14f).deriveFont(Font.PLAIN));

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

                    if (_transferences_running) {

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
        }
    }//GEN-LAST:event_formWindowClosing

    private void cuentas_textareaFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_cuentas_textareaFocusGained
        // TODO add your handling code here:
        if (!_firstAccountsTextareaClick) {
            _firstAccountsTextareaClick = true;
            cuentas_textarea.setText("");
            cuentas_textarea.setForeground(null);
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

        if (!Main.MEGA_ACCOUNTS.isEmpty()) {

            upload_button.setEnabled(false);

            if (_transferences_running) {
                upload_button.setText("PAUSING CURRENT TRANSFER...");
            }

            Helpers.threadRun(() -> {

                if (_transferences_running) {
                    _current_transference.pause();
                }

                Helpers.GUIRunAndWait(() -> {
                    upload_button.setText("NEW UPLOAD");

                    UploadFileDialog dialog = new UploadFileDialog(this, true);

                    dialog.setLocationRelativeTo(this);

                    dialog.setVisible(true);

                    upload_button.setText("PREPARING UPLOAD...");

                    if (_transferences_running) {
                        Helpers.threadRun(() -> {
                            _current_transference.resume();
                        });
                    }

                    if (dialog.isOk()) {

                        vamos_button.setEnabled(false);

                        cuentas_textarea.setEnabled(false);

                        Helpers.threadRun(() -> {

                            synchronized (TRANSFERENCES_LOCK) {

                                Helpers.GUIRunAndWait(() -> {
                                    status_label.setText("");
                                    progressbar.setIndeterminate(false);
                                    Transference trans = new Transference(dialog.getSelected_email(), dialog.getLocal_path(), dialog.getRemote_path(), 1);
                                    transferences.add(trans);
                                    transferences.revalidate();
                                    transferences.repaint();
                                    tabbed_panel.setSelectedIndex(1);
                                    upload_button.setEnabled(true);
                                    upload_button.setText("NEW UPLOAD");
                                });

                                TRANSFERENCES_LOCK.notifyAll();
                            }
                        });
                    } else {
                        upload_button.setEnabled(true);
                        upload_button.setText("NEW UPLOAD");
                    }
                });

            });

        } else {
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

                        for (Component t : transferences.getComponents()) {

                            Transference trans = (Transference) t;

                            if (trans.isFinished()) {
                                transferences.remove(t);
                            }
                        }

                        transferences.revalidate();

                        transferences.repaint();
                    }

                    clear_trans_button.setEnabled(true);
                });
            }
        });

    }//GEN-LAST:event_clear_trans_buttonActionPerformed

    private void cancel_trans_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_trans_buttonActionPerformed
        // TODO add your handling code here:
        Helpers.threadRun(() -> {
            synchronized (TRANSFERENCES_LOCK) {
                Helpers.GUIRunAndWait(() -> {
                    if (transferences.getComponentCount() > 0) {

                        if (Helpers.mostrarMensajeInformativoSINO(this, "All transactions in progress or on hold will be lost. ARE YOU SURE?") == 0) {

                            cancel_trans_button.setEnabled(false);

                            upload_button.setEnabled(false);

                            Helpers.threadRun(() -> {

                                synchronized (TRANSFERENCES_LOCK) {

                                    Helpers.GUIRun(() -> {
                                        progressbar.setIndeterminate(true);
                                    });

                                    Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                                    if (_current_transference != null) {
                                        _current_transference.setCanceled(true);
                                    }

                                    _transferences_running = false;

                                    _current_transference = null;

                                    Helpers.GUIRun(() -> {
                                        transferences.removeAll();
                                        transferences.revalidate();
                                        transferences.repaint();
                                        upload_button.setEnabled(!MEGA_ACCOUNTS.isEmpty());
                                        vamos_button.setEnabled(true);
                                        cuentas_textarea.setEnabled(true);
                                        cancel_trans_button.setEnabled(true);
                                        progressbar.setIndeterminate(false);
                                    });

                                    TRANSFERENCES_LOCK.notifyAll();
                                }

                            });

                        }
                    }
                });

            }
        });

    }//GEN-LAST:event_cancel_trans_buttonActionPerformed

    private void tabbed_panelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tabbed_panelMouseClicked
        // TODO add your handling code here:
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            cuentas_scrollpanel.setVisible(!cuentas_scrollpanel.isVisible());
            revalidate();
            repaint();
        }
    }//GEN-LAST:event_tabbed_panelMouseClicked

    private void clear_log_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_log_buttonActionPerformed
        // TODO add your handling code here:
        if (!busy() && Helpers.mostrarMensajeInformativoSINO(this, "SURE?") == 0) {
            output_textarea.setText("");
        }
    }//GEN-LAST:event_clear_log_buttonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                MAIN_WINDOW = new Main();
                MAIN_WINDOW.init();
                MAIN_WINDOW.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel_trans_button;
    private javax.swing.JButton clear_log_button;
    private javax.swing.JButton clear_trans_button;
    private javax.swing.JScrollPane cuentas_scrollpanel;
    private javax.swing.JTextArea cuentas_textarea;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel logo_label;
    private javax.swing.JTextArea output_textarea;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBoxMenuItem session_menu;
    private javax.swing.JLabel status_label;
    private javax.swing.JTabbedPane tabbed_panel;
    private javax.swing.JScrollPane transf_scroll;
    private javax.swing.JPanel transferences;
    private javax.swing.JPanel transferences_control_panel;
    private javax.swing.JButton upload_button;
    private javax.swing.JButton vamos_button;
    // End of variables declaration//GEN-END:variables
}
