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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

/**
 *
 * @author tonikelope
 */
public final class Transference extends javax.swing.JPanel {

    public static final int WAIT_TIMEOUT = 10;

    private volatile int _tag = -1;
    private volatile int _action;
    private volatile int _prog = 0;
    private volatile int _prog_init = 0;
    private volatile long _size;
    private volatile boolean _directory = false;
    private volatile String _lpath, _rpath, _email;
    private volatile boolean _running = false;
    private volatile boolean _finished = false;
    private volatile boolean _canceled = false;
    private volatile boolean _paused = false;
    private volatile long _prog_timestamp = 0;

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
            String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1", "--output-cols=TAG", _action == 0 ? "--only-downoads" : "--only-uploads"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

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

    private void waitCompletedTAG() {
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

                String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--show-completed", "--limit=1", "--output-cols=TAG", _action == 0 ? "--only-downoads" : "--only-uploads"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                if (!transfer_data.trim().isEmpty()) {
                    String[] transfer_data_lines = transfer_data.split("\n");

                    if (transfer_data_lines.length >= 4) {
                        tag = Integer.parseInt(transfer_data_lines[3].trim());
                    }

                }
            }
        }
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
        _canceled = true;

        Helpers.threadRun(() -> {

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

                TRANSFERENCES_LOCK.notifyAll();
            }

        });
    }

    public void pause() {

        _paused = true;

        synchronized (TRANSFERENCES_LOCK) {

            Helpers.runProcess(new String[]{"mega-transfers", "-pa"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            Helpers.GUIRunAndWait(() -> {
                action.setText("(--- PAUSED ---)");
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

                if (!Helpers.megaWhoami().equals(_email.toLowerCase())) {

                    Main.MAIN_WINDOW.login(_email);

                }

                Helpers.runProcess(new String[]{"mega-transfers", "-ra"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.GUIRunAndWait(() -> {
                    action.setText("");
                });

                TRANSFERENCES_LOCK.notifyAll();
            }

        }
    }

    public void start() {
        Helpers.threadRun(() -> {

            int timeout;

            Helpers.GUIRun(() -> {
                Main.MAIN_WINDOW.getVamos_button().setEnabled(false);
                Main.MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
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

            long start_timestamp = System.currentTimeMillis();

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(true);

            });

            timeout = 0;

            if (isDirectory()) {
                while (!remoteFileExists(_rpath) && timeout < WAIT_TIMEOUT) {
                    try {
                        Thread.sleep(1000);
                        timeout++;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (_size > 0) {
                Helpers.GUIRun(() -> {
                    progress.setIndeterminate(false);

                });
            }

            if (transferRunning()) {

                while (updateProgress()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            if (!_canceled) {

                Helpers.GUIRun(() -> {

                    progress.setIndeterminate(true);
                    action.setText("");

                });

                timeout = 0;

                while (!remoteFileExists(_rpath) && timeout < WAIT_TIMEOUT) {
                    try {
                        Thread.sleep(1000);
                        timeout++;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                final boolean warning = (timeout == WAIT_TIMEOUT);

                if (warning && !isDirectory()) {

                    waitCompletedTAG();
                }

                if (isDirectory()) {
                    long folder_size;
                    long pre_folder_size = remoteFolderSize(_rpath);

                    if (pre_folder_size > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        while ((folder_size = remoteFolderSize(_rpath)) != pre_folder_size && folder_size < _size) {
                            pre_folder_size = folder_size;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }

                long finish_timestamp = System.currentTimeMillis();

                Helpers.runProcess(new String[]{"mega-export", "-af", _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.GUIRun(() -> {
                    progress.setIndeterminate(false);

                    progress.setStringPainted(true);

                    progress.setValue(progress.getMaximum());

                    status.setVisible(true);

                    local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _lpath);

                    long speed = calculateSpeed(_size, _prog_init < 0 ? 0 : _prog_init, 10000, start_timestamp, finish_timestamp);

                    action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");

                    if (warning) {
                        status.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/warning_transference.png")));
                        this.setToolTipText("Unable to verify that the transfer was completed correctly (check manually)");
                    }

                });

                Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload [" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _rpath, false, false);

                _running = false;

                _finished = true;

                synchronized (TRANSFERENCES_LOCK) {

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

        return !transfer_data.trim().isEmpty();
    }

    private long calculateSpeed(long size, int old_prog, int new_prog, long old_timestamp, long new_timestamp) {

        int delta_prog = new_prog - old_prog;

        long delta_time = new_timestamp - old_timestamp;

        long speed = (long) (((((float) delta_prog / 10000) * size) / delta_time) * 1000);

        return speed;
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

        String find = Helpers.runProcess(new String[]{"mega-find", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return !find.trim().startsWith("[API:err:");
    }

    private boolean updateProgress() {

        while (_paused) {

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

            Helpers.GUIRun(() -> {

                if (!isDirectory()) {

                    if (old_timestamp != 0) {
                        long speed = calculateSpeed(_size, old_prog, _prog, old_timestamp, _prog_timestamp);
                        action.setText(Helpers.formatBytes(speed) + "/s");
                    } else {
                        action.setText("");
                    }

                } else {

                    action.setText(Integer.parseInt(matcher.group((_action == 0 ? 1 : 5))) + " files pending (" + matcher.group((_action == 0 ? 3 : 7)).replaceAll("  *", " ") + ")");

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

        rpath = rpath.isBlank() ? "/" : rpath.trim();

        _email = email.trim();
        _lpath = lpath;

        _action = act;

        File local = new File(lpath);

        if (local.isDirectory()) {
            _directory = true;
            _size = Helpers.getDirectorySize(local.toPath());
        } else {
            _size = new File(lpath).length();
        }

        String fname = new File(_lpath).getName();

        _rpath = rpath.endsWith("/") ? rpath + fname : rpath;

        local_path.setText("[" + ((isDirectory() && _size == 0) ? "---" : Helpers.formatBytes(_size)) + "] " + _lpath);
        remote_path.setText("(" + _email + ") " + _rpath);
        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/" + (act == 0 ? "left" : "right") + "-arrow.png")));
        progress.setMinimum(0);
        progress.setMaximum(10000);
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

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });

        local_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_path.setText("jLabel1");
        local_path.setDoubleBuffered(true);

        action.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        action.setForeground(new java.awt.Color(0, 153, 255));
        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/right-arrow.png"))); // NOI18N
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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(status)
                .addGap(0, 0, 0)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        // TODO add your handling code here:

        if (SwingUtilities.isRightMouseButton(evt)) {

            String filename = new File(_lpath).getName();

            if (!_canceled && !_finished && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + filename + "</b><br><br><b>CANCEL</b> this transference?") == 0) {

                stop();

            } else if (!_canceled && _finished && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + filename + "</b><br><br><b>Clear</b> this finished transference?") == 0) {
                clearFinished();
            }
        }
    }//GEN-LAST:event_formMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel action;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel local_path;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel remote_path;
    private javax.swing.JLabel status;
    // End of variables declaration//GEN-END:variables
}
