/*
 __  __ _____ ____    _    ____   ___   ____ _____ ___  ____  
|  \/  | ____/ ___|  / \  |  _ \ / _ \ / ___|_   _/ _ \|  _ \ 
| |\/| |  _|| |  _  / _ \ | | | | | | | |     | || | | | |_) |
| |  | | |__| |_| |/ ___ \| |_| | |_| | |___  | || |_| |  _ < 
|_|  |_|_____\____/_/   \_\____/ \___/ \____| |_| \___/|_| \_\
                                                              
by tonikelope

 */
package com.tonikelope.megadoctor;

import static com.tonikelope.megadoctor.Main.MEGA_CMD_WINDOWS_PATH;
import static com.tonikelope.megadoctor.Main.TRANSFERENCES_LOCK;
import java.awt.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author tonikelope
 */
public final class Transference extends javax.swing.JPanel {

    public static final int WAIT_TIMEOUT = 15;
    public static final int FOLDER_SIZE_WAIT = 1000;
    public static final int SECURE_PAUSE_WAIT_FOLDER = 5000;
    public static final int SECURE_PAUSE_WAIT_FILE = 2000;
    private volatile int _tag = -1;
    private volatile int _action;
    private volatile int _prog = 0;
    private volatile int _prog_init = 0;
    private volatile long _size = -1;
    private volatile boolean _directory = false;
    private volatile String _lpath, _rpath, _email;
    private volatile boolean _running = false;
    private volatile boolean _finished = false;
    private volatile boolean _canceled = false;
    private volatile boolean _paused = false;
    private volatile boolean _starting = false;
    private volatile boolean _finishing = false;
    private volatile boolean _error = false;
    private volatile long _prog_timestamp = 0;
    private final AtomicBoolean _terminate_walk_tree = new AtomicBoolean();
    private volatile String _public_link = null;

    public boolean isError() {
        return _error;
    }

    public JPanel getMain_panel() {
        return main_panel;
    }

    public String getPublic_link() {
        return _public_link;
    }

    public boolean isStarting() {
        return _starting;
    }

    public boolean isFinishing() {
        return _finishing;
    }

    public boolean isDirectory() {
        return _directory;
    }

    public long getFileSize() {
        return _size;
    }

    public String getEmail() {
        return _email;
    }

    public boolean isCanceled() {
        return _canceled;
    }

    public int getTag() {
        return _tag;
    }

    public void setTag(int tag) {
        this._tag = tag;
    }

    public int getAction() {
        return _action;
    }

    public void setAction(int _action) {
        this._action = _action;
    }

    public String getLpath() {
        return _lpath;
    }

    public void setLpath(String _lpath) {
        this._lpath = _lpath;
    }

    public String getRpath() {
        return _rpath;
    }

    public void setRpath(String _rpath) {
        this._rpath = _rpath;
    }

    public int getProg() {
        return _prog;
    }

    public boolean isRunning() {
        return _running;
    }

    public boolean isFinished() {
        return _finished;
    }

    private void readTransferTag() {

        if (!isDirectory()) {
            String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1", "--output-cols=TAG", _action == 0 ? "--only-downloads" : "--only-uploads"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (!transfer_data.isBlank()) {
                String[] transfer_data_lines = transfer_data.split("\n");
                try {
                    _tag = Integer.parseInt(transfer_data_lines[transfer_data_lines.length - 1].trim());
                } catch (NumberFormatException e) {
                    _tag = -1;
                }
            }
        }
    }

    private boolean waitTransferStart() {

        int timeout = 0;

        while (!transferRunning() && timeout < WAIT_TIMEOUT) {

            try {
                Thread.sleep(1000);
                timeout++;
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return (timeout == WAIT_TIMEOUT);
    }

    private boolean waitFreeSpaceChange(long old_free_space) {

        int timeout = 0;

        while (Helpers.getAccountFreeSpace(_email) == old_free_space && timeout < WAIT_TIMEOUT) {

            try {
                Thread.sleep(1000);
                timeout++;
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return (timeout == WAIT_TIMEOUT);

    }

    private boolean waitRemoteExists() {

        int timeout = 0;

        while (!remoteFileExists(_rpath) && timeout < WAIT_TIMEOUT) {

            try {
                Thread.sleep(1000);
                timeout++;
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return (timeout == WAIT_TIMEOUT);
    }

    private boolean waitCompletedTAG() {
        if (!isDirectory() && _tag > 0) {

            int tag = 0;

            int timeout = 0;

            while (tag != _tag && timeout < WAIT_TIMEOUT) {

                try {
                    Thread.sleep(1000);
                    timeout++;
                } catch (InterruptedException ex) {
                    Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                }

                String[] transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--show-completed", "--only-completed", "--limit=1", "--output-cols=TAG", _action == 0 ? "--only-downloads" : "--only-uploads"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                if (Integer.parseInt(transfer_data[2]) == 0 && !transfer_data[1].isBlank()) {

                    String[] transfer_data_lines = transfer_data[1].split("\n");

                    tag = Integer.parseInt(transfer_data_lines[transfer_data_lines.length - 1].trim());
                }
            }

            return (timeout == WAIT_TIMEOUT);
        }

        return false;
    }

    public void clearFinished() {
        Helpers.threadRun(() -> {

            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRunAndWait(() -> {
                    Helpers.GUIRunAndWait(() -> {
                        for (Component c : Main.TRANSFERENCES_MAP.keySet()) {

                            if (Main.TRANSFERENCES_MAP.get(c) == this) {
                                Main.TRANSFERENCES_MAP.remove(c);
                                Main.MAIN_WINDOW.getTransferences().remove(c);
                                break;
                            }
                        }

                        Main.MAIN_WINDOW.getTransferences().revalidate();
                        Main.MAIN_WINDOW.getTransferences().repaint();

                    });
                });

                TRANSFERENCES_LOCK.notifyAll();

            }
        });
    }

    public void stop() {

        _terminate_walk_tree.set(true);

        _canceled = true;

        synchronized (TRANSFERENCES_LOCK) {

            if (_running) {
                Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            Helpers.GUIRunAndWait(() -> {
                for (Component c : Main.TRANSFERENCES_MAP.keySet()) {

                    if (Main.TRANSFERENCES_MAP.get(c) == this) {
                        Main.TRANSFERENCES_MAP.remove(c);
                        Main.MAIN_WINDOW.getTransferences().remove(c);
                        break;
                    }
                }

                Main.MAIN_WINDOW.getTransferences().revalidate();
                Main.MAIN_WINDOW.getTransferences().repaint();

            });

            if (_running) {
                Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload CANCEL [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);
            }

            _running = false;

            TRANSFERENCES_LOCK.notifyAll();

        }

    }

    private void securePauseTransfer() {
        Helpers.runProcess(new String[]{"mega-transfers", "-pa"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        String current, last;

        last = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1000000", "--show-completed", "--output-cols=TAG,STATE"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        try {
            Thread.sleep(isDirectory() ? SECURE_PAUSE_WAIT_FOLDER : SECURE_PAUSE_WAIT_FILE);
        } catch (InterruptedException ex) {
            Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
        }

        while ((current = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1000000", "--show-completed", "--output-cols=TAG,STATE"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]).contains("COMPLETING") || !current.equals(last) || (current.isBlank() && readTotalRunningTransferences() > 0)) {

            last = current;

            try {
                Thread.sleep(isDirectory() ? SECURE_PAUSE_WAIT_FOLDER : SECURE_PAUSE_WAIT_FILE);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void pause() {

        _paused = true;

        Main.MAIN_WINDOW.setTransferences_paused(true);

        synchronized (TRANSFERENCES_LOCK) {

            if (!_finished && !_canceled) {

                Helpers.GUIRun(() -> {
                    action.setText("(PAUSING...)");
                });

                securePauseTransfer();

                Helpers.GUIRun(() -> {
                    action.setText("(PAUSED)");
                    setBackground(Main.MAIN_WINDOW.getPause_button().getBackground());
                });
            }

            TRANSFERENCES_LOCK.notifyAll();
        }

    }

    public boolean isPaused() {
        return _paused;
    }

    public void resume() {

        if (_paused && _running) {
            _paused = false;

            Main.MAIN_WINDOW.setTransferences_paused(false);

            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRun(() -> {
                    action.setText("(RESUMING...)");

                });

                Main.MAIN_WINDOW.login(_email);

                Helpers.runProcess(new String[]{"mega-transfers", "-ra"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                waitTransferStart();

                Helpers.GUIRun(() -> {
                    setBackground(null);
                });

                TRANSFERENCES_LOCK.notifyAll();
            }

        }
    }

    public void setCanceled(boolean _canceled) {
        this._canceled = _canceled;
    }

    private long readRemoteFolderSize() {
        long folder_size = -1;
        if (isDirectory()) {

            try {
                Thread.sleep(FOLDER_SIZE_WAIT);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }

            long pre_folder_size = remoteFolderSize(_rpath);

            if (pre_folder_size > 0) {
                try {
                    Thread.sleep(FOLDER_SIZE_WAIT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                }

                while ((folder_size = remoteFolderSize(_rpath)) != pre_folder_size && folder_size < _size) {
                    pre_folder_size = folder_size;
                    try {
                        Thread.sleep(FOLDER_SIZE_WAIT);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        return folder_size;
    }

    private int readTotalRunningTransferences() {
        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--summary"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        if (!transfer_data.isBlank()) {

            String[] transfer_data_lines = transfer_data.split("\n");

            final String regex = "^ *(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *% +(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *%.*$";

            final Pattern pattern = Pattern.compile(regex);

            final Matcher matcher = pattern.matcher(transfer_data_lines[1].trim());

            if (matcher.find()) {
                return Integer.parseInt(matcher.group((_action == 0 ? 1 : 5)));
            }
        }

        return 0;
    }

    public void retry() {
        Helpers.threadRun(() -> {
            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRunAndWait(() -> {
                    status_icon.setVisible(false);
                    progress.setValue(progress.getMinimum());
                    progress.setIndeterminate(true);
                    folder_stats_scroll.setVisible(false);
                    action.setText("RETRY (QUEUED)");
                });

                _running = false;

                _finished = false;

                _canceled = false;

                _error = false;

                TRANSFERENCES_LOCK.notifyAll();
            }
        });
    }

    public void start() {

        Helpers.GUIRun(() -> {
            status_icon.setVisible(false);
            progress.setValue(progress.getMinimum());
            progress.setIndeterminate(true);
            folder_stats_scroll.setVisible(false);
        });

        Main.MAIN_WINDOW.setCurrent_transference(this);

        Helpers.threadRun(() -> {

            if (Main.MAIN_WINDOW.isTransferences_paused()) {
                this.pause();
            }

            _running = true;

            waitPaused();

            if (!_canceled) {

                synchronized (TRANSFERENCES_LOCK) {

                    _starting = true;

                    Helpers.GUIRun(() -> {
                        status_icon.setVisible(false);
                        Main.MAIN_WINDOW.getVamos_button().setEnabled(false);
                        Main.MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
                        action.setText("(STARTING...)");
                    });

                    Main.MAIN_WINDOW.login(_email);

                    Helpers.runProcess(new String[]{"mega-transfers", "-pa"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                    if (!transferRunning()) {

                        if (_action == 0) {
                            Helpers.runProcess(new String[]{"mega-get", "-q", "--ignore-quota-warn", _rpath, _lpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                        } else {
                            Helpers.runProcess(new String[]{"mega-put", "-cq", "--ignore-quota-warn", _lpath, _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                        }

                    } else {

                        _prog_init = -1;
                    }

                    readTransferTag();

                    Helpers.runProcess(new String[]{"mega-transfers", "-ra"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                    Helpers.GUIRun(() -> {
                        progress.setIndeterminate(true);

                    });

                    waitTransferStart();

                    if (_action == 1 && isDirectory()) {

                        waitRemoteExists();
                    }

                    Helpers.GUIRun(() -> {
                        progress.setIndeterminate(false);
                        folder_stats_scroll.setVisible(isDirectory());
                    });

                    _starting = false;

                }

                long free_space = Helpers.getAccountFreeSpace(_email);

                long start_timestamp = System.currentTimeMillis();

                while (updateProgress()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                long finish_timestamp = System.currentTimeMillis();

                synchronized (TRANSFERENCES_LOCK) {

                    if (_action == 1 && Main.MAIN_WINDOW.isProvisioning_upload()) {
                        Main.FREE_SPACE_CACHE.put(_email, Helpers.getAccountFreeSpace(_email));
                    }

                    if (!_canceled) {

                        _finishing = true;

                        waitPaused();

                        Helpers.GUIRun(() -> {
                            progress.setValue(progress.getMaximum());
                            progress.setIndeterminate(true);
                            action.setText("(FINISHING...)");
                            folder_stats_textarea.setText("");
                            folder_stats_scroll.setVisible(false);
                        });

                        boolean c_error = false;

                        if (_action == 0) {
                            if (!isDirectory()) {
                                c_error = waitCompletedTAG();
                            }
                        } else {
                            if (isDirectory()) {

                                c_error = (waitRemoteExists() && waitFreeSpaceChange(free_space));

                            } else {

                                c_error = (waitRemoteExists() && waitCompletedTAG() && waitFreeSpaceChange(free_space));
                            }
                        }

                        _error = c_error;

                        final boolean warning_folder_size = (_action == 1 && isDirectory() && readRemoteFolderSize() != _size);

                        if (_action == 1) {
                            _public_link = Helpers.exportPathFromCurrentAccount(_rpath);
                        }

                        long speed = calculateSpeed(_size, _prog_init < 0 ? 0 : _prog_init, 10000, start_timestamp, finish_timestamp);

                        Helpers.GUIRunAndWait(() -> {
                            progress.setIndeterminate(false);

                            status_icon.setVisible(true);

                            local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + (_action == 1 ? _lpath : (_lpath + (_rpath.startsWith("/") ? "" : "/") + _rpath)));

                            action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");

                            if (_error) {
                                status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                                status_icon.setToolTipText("Unable to verify that transfer was completed correctly (TIMEOUT)");
                            }

                            if (warning_folder_size) {
                                status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                                status_icon.setToolTipText("REMOTE FOLDER SIZE IS DIFFERENT FROM FOLDER LOCAL SIZE");
                            }

                            boolean running = false;

                            for (Component c : Main.TRANSFERENCES_MAP.keySet()) {

                                Transference t = Main.TRANSFERENCES_MAP.get(c);

                                if (t != this && !t.isFinished() && !t.isCanceled()) {
                                    running = true;
                                    break;
                                }
                            }

                            if (!running) {
                                Notification notification = new Notification(new javax.swing.JFrame(), false, "ALL TRANSFERS FINISHED", (Main.MAIN_WINDOW.getExtendedState() & JFrame.ICONIFIED) == 0 ? 3000 : 0, "finish.wav");
                                Helpers.setWindowLowRightCorner(notification);
                                notification.setVisible(true);
                            }

                            Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);

                        });

                        if (_action == 1) {
                            Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);
                        }

                        _finishing = false;
                    }

                    _running = false;

                    _finished = true;

                    TRANSFERENCES_LOCK.notifyAll();

                }

            }
        });
    }

    private boolean transferRunning() {

        if (_canceled) {
            return false;
        }

        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return (!transfer_data.isBlank() && readTotalRunningTransferences() > 0);
    }

    private long calculateSpeed(long size, int old_prog, int new_prog, long old_timestamp, long new_timestamp) {

        try {
            int delta_prog = new_prog - old_prog;

            long delta_time = new_timestamp - old_timestamp;

            long speed = (long) (((((float) delta_prog / 10000) * size) / delta_time) * 1000);
            return speed;
        } catch (Exception ex) {

        }

        return 0;
    }

    private long remoteFolderSize(String rpath) {

        String du = Helpers.runProcess(new String[]{"mega-du", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        final String regex = Pattern.quote(rpath) + ": *?(\\d+)";

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(du);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return -1;
    }

    private boolean remoteFileExists(String rpath) {

        String[] find = Helpers.runProcess(new String[]{"mega-find", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        return (Integer.parseInt(find[2]) == 0);
    }

    private String readFolderStats() {
        if (isDirectory() && transferRunning()) {

            String fstats = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1000000", "--path-display-size=10000", "--output-cols=SOURCEPATH,PROGRESS,STATE"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (_action == 1 && !fstats.isBlank()) {
                fstats = fstats.replace(_lpath, ".");

                Pattern pattern_header = Pattern.compile("SOURCEPATH +PROGRESS");

                Matcher matcher_header = pattern_header.matcher(fstats);

                String header = "";

                if (matcher_header.find()) {
                    header = matcher_header.group(0);
                }

                fstats = fstats.replace(header, Helpers.adjustSpaces(header, -1 * (_lpath.length() - 1)));
            }

            return fstats;
        }

        return "";
    }

    private void waitPaused() {
        while ((Main.MAIN_WINDOW.isTransferences_paused() || _paused || _size < 0) && !_canceled && !_finished) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean updateProgress() {

        waitPaused();

        if (!transferRunning() || _prog == 10000) {
            return false;
        }

        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--summary"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        String[] transfer_data_lines = transfer_data.split("\n");

        final String regex = "^ *(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *% +(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *%.*$";

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(transfer_data_lines[1].trim());

        if (matcher.find()) {

            int old_prog = _prog;

            long old_timestamp = _prog_timestamp;

            if (_size > 0) {
                _prog = (int) (isDirectory() ? (((float) (_action == 1 ? remoteFolderSize(_rpath) : Helpers.getDirectorySize(new File(_lpath), _terminate_walk_tree)) / _size) * 10000) : (Float.parseFloat(matcher.group((_action == 0 ? 4 : 8))) * 100));
            }

            if (_prog_init < 0) {
                _prog_init = _prog;
            }

            _prog_timestamp = System.currentTimeMillis();

            String fstats = readFolderStats();

            long speed = calculateSpeed(_size, old_prog, _prog, old_timestamp, _prog_timestamp);

            Helpers.GUIRun(() -> {

                if (!isDirectory()) {

                    action.setText(speed > 0 ? Helpers.formatBytes(speed) + "/s" : "----");

                } else {

                    action.setText(Integer.valueOf(matcher.group((_action == 0 ? 1 : 5))) + " files remaining (" + matcher.group((_action == 0 ? 3 : 7)).replaceAll("  *", " ") + ")");

                    folder_stats_textarea.setText(fstats);

                }

                if (_size > 0) {
                    progress.setValue(_prog);
                }

            });
        }

        return true;

    }

    /**
     * Creates new form TransferenceQueueItem
     */
    public Transference(String email, String lpath, String rpath, int act) {
        initComponents();

        status_icon.setVisible(false);

        Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);

        DefaultCaret caret = (DefaultCaret) folder_stats_textarea.getCaret();

        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        _terminate_walk_tree.set(false);

        _email = email.trim();

        _lpath = lpath;

        local_path.setText(_lpath);

        remote_path.setText("...");

        _action = act;

        rpath = rpath.isBlank() ? "/" : rpath.trim();

        String rp = rpath;

        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/" + (_action == 0 ? "left" : "right") + "-arrow.png")));

        progress.setMinimum(0);

        progress.setMaximum(10000);

        progress.setIndeterminate(true);

        folder_stats_scroll.setVisible(false);

        Helpers.threadRun(() -> {

            if (_action == 1) {

                File local_file = new File(_lpath);

                String fname = local_file.getName();

                if (local_file.isDirectory()) {
                    _directory = true;

                    _size = Helpers.getDirectorySize(local_file, _terminate_walk_tree);

                } else {
                    _size = new File(lpath).length();
                }

                _rpath = rp.endsWith("/") ? rp + fname : rp;

            } else {
                _rpath = Helpers.getNodeFullPath(rp, _email);

                Object[] node_info = Main.MEGA_NODES.get(rp);

                _size = (long) node_info[0];

                _directory = (boolean) node_info[3];
            }

            Helpers.GUIRun(() -> {

                remote_path.setText("(" + _email + ") " + _rpath);

                if (isDirectory()) {
                    local_path.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/folder.png")));
                }

                local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + (_action == 1 ? _lpath : (_lpath + (_rpath.startsWith("/") ? "" : "/") + _rpath)));

                progress.setIndeterminate(false);
            });

        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        main_panel = new javax.swing.JPanel();
        status_icon = new javax.swing.JLabel();
        action = new javax.swing.JLabel();
        remote_path = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        local_path = new javax.swing.JLabel();
        folder_stats_scroll = new javax.swing.JScrollPane();
        folder_stats_textarea = new javax.swing.JTextArea();
        drag = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        main_panel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        main_panel.setOpaque(false);
        main_panel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                main_panelMousePressed(evt);
            }
        });

        status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ok.png"))); // NOI18N

        action.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        action.setForeground(new java.awt.Color(0, 153, 255));
        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/right-arrow.png"))); // NOI18N
        action.setText("(QUEUED)");
        action.setDoubleBuffered(true);

        remote_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        remote_path.setText("jLabel3");
        remote_path.setDoubleBuffered(true);

        progress.setFont(new java.awt.Font("Noto Sans", 0, 24)); // NOI18N
        progress.setStringPainted(true);

        local_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_path.setText("jLabel1");
        local_path.setDoubleBuffered(true);

        javax.swing.GroupLayout main_panelLayout = new javax.swing.GroupLayout(main_panel);
        main_panel.setLayout(main_panelLayout);
        main_panelLayout.setHorizontalGroup(
            main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_panelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(status_icon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(main_panelLayout.createSequentialGroup()
                        .addComponent(local_path)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(action)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(remote_path)))
                .addGap(0, 0, 0))
        );
        main_panelLayout.setVerticalGroup(
            main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(status_icon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(main_panelLayout.createSequentialGroup()
                        .addGroup(main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(action)
                            .addComponent(remote_path)
                            .addComponent(local_path))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        folder_stats_textarea.setEditable(false);
        folder_stats_textarea.setBackground(new java.awt.Color(153, 153, 153));
        folder_stats_textarea.setColumns(20);
        folder_stats_textarea.setFont(new java.awt.Font("Monospaced", 0, 18)); // NOI18N
        folder_stats_textarea.setForeground(new java.awt.Color(255, 255, 255));
        folder_stats_textarea.setRows(5);
        folder_stats_textarea.setDoubleBuffered(true);
        folder_stats_scroll.setViewportView(folder_stats_textarea);

        drag.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/drag.png"))); // NOI18N
        drag.setCursor(new java.awt.Cursor(java.awt.Cursor.MOVE_CURSOR));
        drag.setDoubleBuffered(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(drag)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(main_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(folder_stats_scroll))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(main_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(drag))
                .addGap(0, 0, 0)
                .addComponent(folder_stats_scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void main_panelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_main_panelMousePressed
        // TODO add your handling code here:
        if (evt.isPopupTrigger()) {
            getComponentPopupMenu().show(evt.getComponent(), evt.getX(), evt.getY());
        } else if (isDirectory() && !isFinished() && !isFinishing() && !isStarting() && SwingUtilities.isLeftMouseButton(evt)) {
            folder_stats_scroll.setVisible(!folder_stats_scroll.isVisible());
            revalidate();
            repaint();
        }
    }//GEN-LAST:event_main_panelMousePressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel action;
    private javax.swing.JLabel drag;
    private javax.swing.JScrollPane folder_stats_scroll;
    private javax.swing.JTextArea folder_stats_textarea;
    private javax.swing.JLabel local_path;
    private javax.swing.JPanel main_panel;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel remote_path;
    private javax.swing.JLabel status_icon;
    // End of variables declaration//GEN-END:variables
}
