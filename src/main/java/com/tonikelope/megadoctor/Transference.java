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
import static com.tonikelope.megadoctor.Main.TRANSFERENCES_MAP;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private volatile String _error_msg = "";
    private volatile long _prog_timestamp = 0;
    private final AtomicBoolean _terminate_walk_tree = new AtomicBoolean();
    private volatile String _public_link = null;
    private volatile boolean _remove_after;
    private volatile Long _split_file = null;
    private final Object _split_lock = new Object();
    private volatile boolean _splitting = false;
    private volatile Long _thread_id = null;
    private volatile boolean _split_finished;
    private volatile boolean _retry;
    private volatile String _remote_handle = null;
    private volatile String _mediainfo = null;

    public String getMediainfo() {
        return _mediainfo;
    }

    public String getRemote_handle() {
        return _remote_handle;
    }

    public Long getThread_id() {
        return _thread_id;
    }

    public boolean isRetry() {
        return _retry;
    }

    public boolean isSplit_finished() {
        return _split_finished;
    }

    public void setSplitting(boolean _splitting) {
        this._splitting = _splitting;
    }

    public boolean isSplitting() {
        return _splitting;
    }

    public Object getSplit_lock() {
        return _split_lock;
    }

    public boolean isError() {
        return _error;
    }

    public boolean isRemove_after() {
        return _remove_after;
    }

    public Long getSplit_file() {
        return _split_file;
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

    private boolean waitUsedSpaceChange(long old_used_space) {

        int timeout = 0;

        while (Helpers.getAccountCloudDriveUsedSpace(_email) == old_used_space && timeout < WAIT_TIMEOUT) {

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

    public void clearTransference() {
        if (isFinished()) {
            Helpers.threadRun(() -> {

                synchronized (TRANSFERENCES_LOCK) {

                    Helpers.GUIRunAndWait(() -> {
                        String cleared_file = null;

                        for (Component c : Main.TRANSFERENCES_MAP.keySet()) {

                            Transference t = TRANSFERENCES_MAP.get(c);

                            if (t == this && t.isFinished()) {

                                if (!t.isCanceled() && !t.isError()) {

                                    cleared_file = t.getLpath() + " -> " + t.getRpath() + " " + (t.getRemote_handle() != null ? " <" + t.getRemote_handle() + ">" : "") + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   " + (t.getPublic_link() != null ? t.getPublic_link() : "");

                                } else {
                                    cleared_file = "[ERROR/CANCELED] " + t.getLpath() + " (" + Helpers.formatBytes(t.getFileSize()) + ")" + "   [" + t.getEmail() + "]   ";

                                }

                                Main.TRANSFERENCES_MAP.remove(c);
                                Main.MAIN_WINDOW.getTransferences().remove(c);
                                break;

                            }
                        }

                        if (cleared_file != null) {

                            Main.MAIN_WINDOW.output_textarea_append("\n" + cleared_file + "\n");
                        }

                        Main.MAIN_WINDOW.getTransferences().revalidate();
                        Main.MAIN_WINDOW.getTransferences().repaint();

                    });

                    TRANSFERENCES_LOCK.notifyAll();

                }
            });
        }
    }

    public void stopAndRetry() {

        _terminate_walk_tree.set(true);

        synchronized (TRANSFERENCES_LOCK) {

            if (_running) {
                Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            Main.FREE_SPACE_CACHE.remove(_email);

            _thread_id = null;

            _tag = -1;

            _prog = 0;

            _prog_init = 0;

            _starting = false;

            _splitting = false;

            _split_finished = false;

            _finishing = false;

            _running = false;

            _finished = false;

            _canceled = false;

            _error = false;

            _error_msg = "";

            _retry = true;

            Helpers.GUIRunAndWait(() -> {
                Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);
                status_icon.setVisible(false);
                status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ok.png")));
                progress.setValue(progress.getMinimum());
                progress.setIndeterminate(true);
                folder_stats_scroll.setVisible(false);
                action.setText("RESET (QUEUED)");
                if (getBackground().equals(Main.MAIN_WINDOW.getPause_button().getBackground())) {
                    setBackground(null);
                }
                Main.MAIN_WINDOW.getTransferences().revalidate();
                Main.MAIN_WINDOW.getTransferences().repaint();
            });

            TRANSFERENCES_LOCK.notifyAll();

        }
    }

    public void stop() {

        _terminate_walk_tree.set(true);

        _canceled = true;

        synchronized (TRANSFERENCES_LOCK) {

            if (_running) {
                Helpers.runProcess(new String[]{"mega-transfers", "-ca"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            Helpers.GUIRunAndWait(() -> {

                Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);

                status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));

                status_icon.setToolTipText("CANCELED TRANSFERENCE");

                status_icon.setVisible(true);

                action.setText("CANCELED");

                if (getBackground().equals(Main.MAIN_WINDOW.getPause_button().getBackground())) {
                    setBackground(null);
                }

                Main.MAIN_WINDOW.getTransferences().revalidate();

                Main.MAIN_WINDOW.getTransferences().repaint();

            });

            Main.FREE_SPACE_CACHE.remove(_email);

            if (_running) {
                Main.MAIN_WINDOW.refreshAccount(_email, "Refreshed after upload CANCEL [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);
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

                TRANSFERENCES_LOCK.notifyAll();

            }

            waitTransferStart();

            Helpers.GUIRun(() -> {
                setBackground(null);
            });

        }
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

        synchronized (TRANSFERENCES_LOCK) {

            _thread_id = null;

            _tag = -1;

            _prog = 0;

            _prog_init = 0;

            _starting = false;

            _splitting = false;

            _split_finished = false;

            _finishing = false;

            _running = false;

            _finished = false;

            _canceled = false;

            _error = false;

            _error_msg = "";

            _retry = true;

            Helpers.GUIRunAndWait(() -> {
                Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);
                status_icon.setVisible(false);
                status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ok.png")));
                progress.setValue(progress.getMinimum());
                progress.setIndeterminate(true);
                folder_stats_scroll.setVisible(false);
                action.setText("RETRY (QUEUED)");
                Main.MAIN_WINDOW.getTransferences().revalidate();
                Main.MAIN_WINDOW.getTransferences().repaint();
            });

            TRANSFERENCES_LOCK.notifyAll();
        }

    }

    private boolean isTransferenceThreadCanceled() {

        return (_thread_id != Thread.currentThread().getId());
    }

    public void start() {

        _retry = false;

        Logger.getLogger(Main.class.getName()).log(Level.INFO, "STARTING {0}{1}", new Object[]{_action == 0 ? "DOWNLOAD " : "UPLOAD ", _lpath});

        if (_split_file != null) {
            _splitting = true;

            Helpers.GUIRun(() -> {
                action.setForeground(Color.magenta);
            });
        } else {
            _running = true;
        }

        Helpers.threadRun(() -> {

            _thread_id = Thread.currentThread().getId();

            try {
                while (!isTransferenceThreadCanceled() && !_canceled && _split_file != null && !(Files.exists(Paths.get(_lpath)) && Files.size(Paths.get(_lpath)) == _split_file)) {

                    int progreso = Files.exists(Paths.get(_lpath)) ? Math.round(((float) Files.size(Paths.get(_lpath)) / _split_file) * 100) : 0;

                    Helpers.GUIRun(() -> {
                        action.setText("SPLITTING FILE " + String.valueOf(progreso) + "%");
                    });

                    synchronized (_split_lock) {
                        _split_lock.wait(1000);
                    }
                }

            } catch (Exception ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!isTransferenceThreadCanceled() && !this._canceled) {

                if (_splitting) {

                    _split_finished = true;

                    Helpers.GUIRun(() -> {
                        action.setText("FILE ALREADY SPLIT (QUEUED)");
                    });

                    while (!isTransferenceThreadCanceled() && _splitting) {

                        synchronized (TRANSFERENCES_LOCK) {
                            TRANSFERENCES_LOCK.notifyAll();
                        }

                        synchronized (_split_lock) {
                            try {
                                Logger.getLogger(Main.class.getName()).log(Level.INFO, "FILE ALREADY SPLIT (WAITING QUEUED){0}", _lpath);
                                _split_lock.wait(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }

                if (!isTransferenceThreadCanceled() && !this._canceled) {

                    _running = true;

                    if (Main.MAIN_WINDOW.isTransferences_paused()) {
                        this.pause();
                    }

                    waitPaused();

                    if (!isTransferenceThreadCanceled() && !_canceled) {

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

                            Helpers.runProcess(new String[]{"mega-https", (Main.MEGADOCTOR_MISC.containsKey("megacmd_https") && (boolean) Main.MEGADOCTOR_MISC.get("megacmd_https")) ? "on" : "off"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

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

                        long used_space = Helpers.getAccountCloudDriveUsedSpace(_email);

                        long start_timestamp = System.currentTimeMillis();

                        while (updateProgress()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        long finish_timestamp = System.currentTimeMillis();

                        if (!isTransferenceThreadCanceled()) {
                            synchronized (TRANSFERENCES_LOCK) {

                                if (_action == 1 && Main.MAIN_WINDOW.isProvisioning_upload()) {
                                    Main.FREE_SPACE_CACHE.put(_email, Helpers.getAccountFreeSpace(_email));
                                }

                                if (!isTransferenceThreadCanceled() && !_canceled) {

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

                                            c_error = (waitRemoteExists() && waitUsedSpaceChange(used_space));

                                        } else {

                                            c_error = (waitRemoteExists() && waitCompletedTAG() && waitUsedSpaceChange(used_space));
                                        }
                                    }

                                    if (c_error) {
                                        _error_msg = _error_msg + "#Unable to verify that transfer was completed correctly (TIMEOUT)";
                                    }

                                    _error = c_error;

                                    if (_action == 1 && isDirectory() && readRemoteFolderSize() != _size) {
                                        _error = true;
                                        _error_msg = _error_msg + "#REMOTE FOLDER SIZE IS DIFFERENT FROM FOLDER LOCAL SIZE";
                                    }

                                    if (_action == 1) {
                                        _public_link = Helpers.exportPathFromCurrentAccount(_rpath);

                                        if (_public_link == null) {
                                            _error = true;
                                            _error_msg = _error_msg + "#PUBLIC LINK GENERATION FAILED";
                                        }

                                        readUploadRemoteHandle();
                                    }

                                    long speed = calculateSpeed(_size, _prog_init < 0 ? 0 : _prog_init, 10000, start_timestamp, finish_timestamp);

                                    Helpers.GUIRunAndWait(() -> {
                                        progress.setIndeterminate(false);

                                        status_icon.setVisible(true);

                                        local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + (_action == 1 ? _lpath : (_lpath + (_rpath.startsWith("/") ? "" : "/") + _rpath)));

                                        action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");

                                        if (_error) {
                                            status_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                                            status_icon.setToolTipText(_error_msg);
                                        }

                                        boolean running = false;

                                        for (Component c : Main.TRANSFERENCES_MAP.keySet()) {

                                            Transference t = Main.TRANSFERENCES_MAP.get(c);

                                            if (t != this && !t.isFinished() && !t.isCanceled()) {
                                                running = true;
                                                break;
                                            }
                                        }

                                        if (!_error && _remove_after) {
                                            try {
                                                if (isDirectory()) {
                                                    Helpers.deleteDirectoryRecursion(Paths.get(_lpath));
                                                } else {
                                                    Files.deleteIfExists(Paths.get(_lpath));
                                                }
                                                local_path.setBackground(Color.YELLOW);
                                            } catch (IOException ex) {
                                                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
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
                                        Main.MAIN_WINDOW.refreshAccount(_email, "Refreshed after upload [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);
                                    }

                                    _finishing = false;
                                }

                                _running = false;

                                _finished = true;

                                Main.MAIN_WINDOW.saveTransfers();

                                Main.MAIN_WINDOW.saveLog();

                                TRANSFERENCES_LOCK.notifyAll();

                            }

                        }

                    }

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

    private void readUploadRemoteHandle() {

        if (_public_link != null) {

            String[] ls = Helpers.runProcess(new String[]{"mega-ls", "-aar", "--show-handles"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            if (Integer.parseInt(ls[2]) == 0) {

                for (String line : ls[1].split("\n")) {

                    String match = Helpers.findFirstRegex("(H:[^>]+).*?" + Pattern.quote(_public_link), line, 1);

                    if (match != null) {

                        _remote_handle = match;

                        break;
                    }
                }
            }
        }

        if (_remote_handle == null) {

            //Fallback (puede haber colisión de remote path aunque es raro)
            String[] find = Helpers.runProcess(new String[]{"mega-find", "--print-only-handles", isDirectory() ? "--type=d" : "--type=f", _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            _remote_handle = (Integer.parseInt(find[2]) == 0 ? find[1].split("\n")[0] : null);
        }

    }

    private String getRemoteFolderHandle(String rpath) {
        String[] find = Helpers.runProcess(new String[]{"mega-find", "--print-only-handles", "--type=d", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        return (Integer.parseInt(find[2]) == 0 ? find[1].split("\n")[0] : null);
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
        while ((Main.MAIN_WINDOW.isTransferences_paused() || Main.MAIN_WINDOW.isProvisioning_upload() || _paused || _size < 0) && !isTransferenceThreadCanceled() && !_canceled && !_finished) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean updateProgress() {

        waitPaused();

        if (!transferRunning() || isTransferenceThreadCanceled()) {
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
    public Transference(String email, String lpath, String rpath, int act, boolean remove_after, Long split_file) {
        initComponents();

        status_icon.setVisible(false);

        _mediainfo = !Files.isDirectory(Paths.get(lpath)) ? Helpers.getMediaInfo(lpath) : null;

        DefaultCaret caret = (DefaultCaret) folder_stats_textarea.getCaret();

        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        _remove_after = remove_after;

        _split_file = split_file;

        if (_remove_after) {
            local_path.setBackground(Color.YELLOW);
        }

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
                    _size = _split_file != null ? _split_file : new File(lpath).length();
                }

                _rpath = rp.endsWith("/") ? rp + fname : rp;

                _rpath = URLDecoder.decode(_rpath);

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

                Helpers.JTextFieldRegularPopupMenu.addTransferencePopupMenuTo(this);
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
        local_path.setOpaque(true);
        local_path.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                local_pathMouseClicked(evt);
            }
        });

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

    private void local_pathMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_local_pathMouseClicked
        // TODO add your handling code here:
        if (_remove_after && Files.exists(Paths.get(_lpath)) && !isFinished() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "DISABLE REMOVE AFTER UPLOAD?") == 0) {
            _remove_after = false;
            local_path.setBackground(null);
        }
    }//GEN-LAST:event_local_pathMouseClicked

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
