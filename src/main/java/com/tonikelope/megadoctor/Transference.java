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
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private volatile long _prog_timestamp = 0;
    private final AtomicBoolean _terminate_walk_tree = new AtomicBoolean();

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

            if (!transfer_data.trim().isEmpty()) {
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
        if (_tag > 0) {

            int tag = 0;

            int timeout = 0;

            while (tag != _tag && timeout < WAIT_TIMEOUT) {

                try {
                    Thread.sleep(1000);
                    timeout++;
                } catch (InterruptedException ex) {
                    Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                }

                String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--show-completed", "--limit=1", "--output-cols=TAG", _action == 0 ? "--only-downloads" : "--only-uploads"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                if (!transfer_data.trim().isEmpty()) {
                    String[] transfer_data_lines = transfer_data.split("\n");

                    if (transfer_data_lines.length >= 4) {
                        tag = Integer.parseInt(transfer_data_lines[3].trim());
                    }

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
                    Main.MAIN_WINDOW.getTransferences().remove(this);

                    Main.MAIN_WINDOW.getTransferences().revalidate();

                    Main.MAIN_WINDOW.getTransferences().repaint();
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

            _running = false;

            Helpers.GUIRunAndWait(() -> {
                Main.MAIN_WINDOW.getTransferences().remove(this);

                Main.MAIN_WINDOW.getTransferences().revalidate();

                Main.MAIN_WINDOW.getTransferences().repaint();
            });

            Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload CANCEL [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);

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

        while ((current = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1000000", "--show-completed", "--output-cols=TAG,STATE"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1]).contains("COMPLETING") || !current.equals(last) || (current.trim().isEmpty() && readTotalRunningTransferences() > 0)) {

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

        synchronized (TRANSFERENCES_LOCK) {

            Helpers.GUIRun(() -> {
                action.setText("(PAUSING...)");
            });

            securePauseTransfer();

            Helpers.GUIRun(() -> {
                action.setText("(PAUSED)");
            });

            TRANSFERENCES_LOCK.notifyAll();
        }

    }

    public boolean isPaused() {
        return _paused;
    }

    public void resume() {

        if (_paused && _running) {
            _paused = false;

            synchronized (TRANSFERENCES_LOCK) {

                Helpers.GUIRun(() -> {
                    action.setText("(RESUMING...)");
                    Main.MAIN_WINDOW.getPause_button().setEnabled(false);
                    Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(false);
                });

                Main.MAIN_WINDOW.login(_email);

                Helpers.runProcess(new String[]{"mega-transfers", "-ra"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.GUIRun(() -> {
                    Main.MAIN_WINDOW.getPause_button().setEnabled(true);
                    Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(true);
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

        if (!transfer_data.trim().isEmpty()) {

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

    public void start() {
        Helpers.threadRun(() -> {

            _starting = true;

            Helpers.GUIRun(() -> {
                Main.MAIN_WINDOW.getVamos_button().setEnabled(false);
                Main.MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
                Main.MAIN_WINDOW.getPause_button().setEnabled(false);
                Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(false);
                Main.MAIN_WINDOW.getUpload_button().setEnabled(false);
                action.setText("(STARTING...)");
            });

            _running = true;

            Main.MAIN_WINDOW.login(_email);

            Helpers.runProcess(new String[]{"mega-transfers", "-pa"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            if (!transferRunning()) {

                if (_action == 0) {
                    Helpers.runProcess(new String[]{"mega-get", "-mq", "--ignore-quota-warn", _rpath, _lpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
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

            if (isDirectory()) {

                waitRemoteExists();
            }

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(false);
                Main.MAIN_WINDOW.getPause_button().setEnabled(true);
                Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(true);
                Main.MAIN_WINDOW.getUpload_button().setEnabled(true);

            });

            _starting = false;

            long start_timestamp = System.currentTimeMillis();

            while (updateProgress()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            long finish_timestamp = System.currentTimeMillis();

            if (!_canceled) {

                _finishing = true;

                Helpers.GUIRun(() -> {

                    progress.setIndeterminate(true);
                    action.setText("(FINISHING...)");
                    folder_stats_textarea.setText("");
                    folder_stats_scroll.setVisible(false);
                    Main.MAIN_WINDOW.getUpload_button().setEnabled(false);
                    Main.MAIN_WINDOW.getPause_button().setEnabled(false);
                    Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(false);

                });

                Helpers.runProcess(new String[]{"mega-reload"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                int verification_timeout = 0;

                while (!remoteFileExists(_rpath) && verification_timeout < WAIT_TIMEOUT) {
                    try {
                        Thread.sleep(1000);
                        verification_timeout++;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                boolean c_error = false;

                if (verification_timeout == WAIT_TIMEOUT && !isDirectory()) {
                    if (!isDirectory()) {
                        c_error = waitCompletedTAG();
                    } else {
                        c_error = true;
                    }
                }

                final boolean check_error = c_error;

                long folder_size = readRemoteFolderSize();

                final boolean warning_folder_size = (isDirectory() && folder_size != _size);

                Helpers.runProcess(new String[]{"mega-export", "-af", _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                long speed = calculateSpeed(_size, _prog_init < 0 ? 0 : _prog_init, 10000, start_timestamp, finish_timestamp);

                Helpers.GUIRun(() -> {
                    progress.setIndeterminate(false);

                    progress.setStringPainted(true);

                    progress.setValue(progress.getMaximum());

                    status.setVisible(true);

                    local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _lpath);

                    action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");

                    if (check_error) {
                        status.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                        this.setToolTipText("Unable to verify that the transfer was completed correctly");
                    }

                    if (warning_folder_size) {
                        status.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                        this.setToolTipText("REMOTE FOLDER SIZE IS DIFFERENT FROM LOCAL SIZE");
                    }

                });

                Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);
            }

            Helpers.GUIRun(() -> {
                Main.MAIN_WINDOW.getUpload_button().setEnabled(true);
                Main.MAIN_WINDOW.getPause_button().setEnabled(Main.MAIN_WINDOW.getTransferences().getComponentCount() > 0);
                Main.MAIN_WINDOW.getCancel_trans_button().setEnabled(Main.MAIN_WINDOW.getTransferences().getComponentCount() > 0);
            });

            _finishing = false;

            _running = false;

            _finished = true;

            synchronized (TRANSFERENCES_LOCK) {

                TRANSFERENCES_LOCK.notifyAll();

            }
        });
    }

    private boolean transferRunning() {

        if (_canceled) {
            return false;
        }

        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return (!transfer_data.trim().isEmpty() && readTotalRunningTransferences() > 0);
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

            if (!fstats.trim().isEmpty()) {
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

    private boolean updateProgress() {

        while ((_paused || _size < 0) && !_canceled && !_finished) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

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
                _prog = (int) (isDirectory() ? (((float) remoteFolderSize(_rpath) / _size) * 10000) : (Float.parseFloat(matcher.group((_action == 0 ? 4 : 8))) * 100));
            }

            if (_prog_init < 0) {
                _prog_init = _prog;
            }

            _prog_timestamp = System.currentTimeMillis();

            String fstats = readFolderStats();

            long speed = calculateSpeed(_size, old_prog, _prog, old_timestamp, _prog_timestamp);

            Helpers.GUIRun(() -> {

                if (!isDirectory()) {

                    action.setText(speed > 0 ? Helpers.formatBytes(speed) : "----" + "/s");

                } else {

                    action.setText(Integer.parseInt(matcher.group((_action == 0 ? 1 : 5))) + " files remaining (" + matcher.group((_action == 0 ? 3 : 7)).replaceAll("  *", " ") + ")");

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
        status.setVisible(false);

        DefaultCaret caret = (DefaultCaret) folder_stats_textarea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        _terminate_walk_tree.set(false);

        rpath = rpath.isBlank() ? "/" : rpath.trim();

        _email = email.trim();
        _lpath = lpath;

        _action = act;

        Helpers.threadRun(() -> {
            File local = new File(lpath);

            if (local.isDirectory()) {
                _directory = true;

                _size = Helpers.getDirectorySize(local, _terminate_walk_tree);

            } else {
                _size = new File(lpath).length();
            }

            Helpers.GUIRun(() -> {

                if (isDirectory()) {
                    local_path.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/folder.png")));
                }

                if (_size > 0) {
                    local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _lpath);
                }
                progress.setIndeterminate(false);
            });

        });

        String fname = new File(_lpath).getName();

        _rpath = rpath.endsWith("/") ? rpath + fname : rpath;

        remote_path.setText("(" + _email + ") " + _rpath);
        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/" + (act == 0 ? "left" : "right") + "-arrow.png")));
        progress.setMinimum(0);
        progress.setMaximum(10000);
        progress.setIndeterminate(true);

        folder_stats_scroll.setVisible(isDirectory());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        local_path = new javax.swing.JLabel();
        action = new javax.swing.JLabel();
        remote_path = new javax.swing.JLabel();
        progress = new javax.swing.JProgressBar();
        status = new javax.swing.JLabel();
        folder_stats_scroll = new javax.swing.JScrollPane();
        folder_stats_textarea = new javax.swing.JTextArea();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        local_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_path.setText("jLabel1");
        local_path.setToolTipText("Click for details (folders)");
        local_path.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        local_path.setDoubleBuffered(true);
        local_path.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                local_pathMouseClicked(evt);
            }
        });

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(local_path)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(action)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(remote_path)))
                .addGap(0, 0, 0))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(local_path)
                    .addComponent(action)
                    .addComponent(remote_path))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        status.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ok.png"))); // NOI18N

        folder_stats_textarea.setEditable(false);
        folder_stats_textarea.setBackground(new java.awt.Color(153, 153, 153));
        folder_stats_textarea.setColumns(20);
        folder_stats_textarea.setFont(new java.awt.Font("Monospaced", 0, 18)); // NOI18N
        folder_stats_textarea.setForeground(new java.awt.Color(255, 255, 255));
        folder_stats_textarea.setRows(5);
        folder_stats_textarea.setDoubleBuffered(true);
        folder_stats_scroll.setViewportView(folder_stats_textarea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(folder_stats_scroll)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(status)
                        .addGap(0, 0, 0)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0)
                .addComponent(folder_stats_scroll)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        // TODO add your handling code here:

        if (SwingUtilities.isRightMouseButton(evt)) {

            String filename = new File(_lpath).getName();

            if (!_canceled && !_finished) {

                if (Main.MAIN_WINDOW.getCancel_trans_button().isEnabled() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + filename + "</b><br><br><b>CANCEL</b> this transference?") == 0) {
                    Helpers.threadRun(() -> {
                        stop();
                    });
                }

            } else if (!_canceled && _finished && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + filename + "</b><br><br><b>Clear</b> this finished transference?") == 0) {
                clearFinished();
            }
        }
    }//GEN-LAST:event_formMouseClicked

    private void local_pathMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_local_pathMouseClicked
        // TODO add your handling code here:
        if (isDirectory() && !isFinished() && !isFinishing() && !isStarting() && SwingUtilities.isLeftMouseButton(evt)) {
            folder_stats_scroll.setVisible(!folder_stats_scroll.isVisible());
            revalidate();
            repaint();
        }

    }//GEN-LAST:event_local_pathMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel action;
    private javax.swing.JScrollPane folder_stats_scroll;
    private javax.swing.JTextArea folder_stats_textarea;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel local_path;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel remote_path;
    private javax.swing.JLabel status;
    // End of variables declaration//GEN-END:variables
}
