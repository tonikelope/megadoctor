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
public class Transference extends javax.swing.JPanel {

    private volatile int _tag;
    private volatile int _action;
    private volatile int _prog = 0;
    private volatile long _size;
    private volatile String _lpath, _rpath, _email;
    private volatile boolean _running = false;
    private volatile boolean _finished = false;
    private volatile boolean _canceled = false;
    private volatile long _prog_timestamp = 0;

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

    private void setTransferTag() {

        if (_size > 0) {
            String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--col-separator=#_#"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (!transfer_data.trim().isEmpty()) {
                String[] transfer_data_lines = transfer_data.split("\n");

                if (transfer_data_lines.length >= 2) {
                    String[] transfer_tokens = transfer_data_lines[1].trim().split("#_#");

                    _tag = Integer.parseInt(transfer_tokens[1].trim());
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

    public void start() {
        Helpers.threadRun(() -> {

            Helpers.GUIRun(() -> {
                Main.MAIN_WINDOW.getVamos_button().setEnabled(false);
                Main.MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
            });

            _running = true;

            Main.MAIN_WINDOW.login(_email);

            if (_action == 0) {
                Helpers.runProcess(new String[]{"mega-get", "-mq", "--ignore-quota-warn", _rpath, _lpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            } else {
                Helpers.runProcess(new String[]{"mega-put", "-cq", "--ignore-quota-warn", _lpath, _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
            }

            long start_timestamp = System.currentTimeMillis();

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(true);

            });

            if (_size == 0) {
                while (!remoteFileExists(_rpath)) {
                    try {
                        Thread.sleep(1000);
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

                setTransferTag();

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

                while (!remoteFileExists(_rpath)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                long finish_timestamp = System.currentTimeMillis();

                Helpers.runProcess(new String[]{"mega-export", "-af", _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                long folder_size = 0;

                if (_size == 0) {
                    long pre_folder_size = remoteFolderSize(_rpath);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    while ((folder_size = remoteFolderSize(_rpath)) != pre_folder_size) {
                        pre_folder_size = folder_size;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }

                long fsize = folder_size;

                Helpers.GUIRun(() -> {
                    progress.setIndeterminate(false);
                    progress.setStringPainted(true);
                    progress.setValue(progress.getMaximum());
                    ok.setVisible(true);

                    local_path.setText("[" + Helpers.formatBytes(_size > 0 ? _size : fsize) + "] " + _lpath);

                    if (_size > 0) {
                        long speed = calculateSpeed(_size, 0, 10000, start_timestamp, finish_timestamp);
                        action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");
                    } else {
                        long speed = calculateSpeed(fsize, 0, 10000, start_timestamp, finish_timestamp);
                        action.setText("(Avg: " + Helpers.formatBytes(speed) + "/s)");
                    }

                });

                Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload [" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _rpath, false, false);

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

        return 0;
    }

    private boolean remoteFileExists(String rpath) {

        String find = Helpers.runProcess(new String[]{"mega-find", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return !find.trim().startsWith("[API:err:");
    }

    private boolean updateProgress() {

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

            _prog = (int) (Float.parseFloat(matcher.group((_action == 0 ? 4 : 8))) * 100);

            _prog_timestamp = System.currentTimeMillis();

            Helpers.GUIRun(() -> {

                if (_size > 0) {

                    if (old_timestamp != 0) {
                        long speed = calculateSpeed(_size, old_prog, _prog, old_timestamp, _prog_timestamp);
                        action.setText(Helpers.formatBytes(speed) + "/s");
                    } else {
                        action.setText("");
                    }

                    local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _lpath);

                    progress.setValue(_prog);

                } else {

                    action.setText(Integer.parseInt(matcher.group((_action == 0 ? 1 : 5))) + " files pending (" + matcher.group((_action == 0 ? 3 : 7)).replaceAll("  *", " ") + ")");

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
        ok.setVisible(false);
        _email = email.trim();
        _lpath = lpath;

        rpath = rpath.isBlank() ? "/" : rpath.trim();

        _action = act;

        File local = new File(lpath);

        if (local.isDirectory()) {
            _size = 0;
        } else {
            _size = new File(lpath).length();
        }

        _rpath = rpath.endsWith("/") ? rpath + new File(_lpath).getName() : rpath;

        local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _lpath);
        remote_path.setText("(" + _email + ") " + _rpath);
        action.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/" + (act == 0 ? "left" : "right") + "-arrow.png")));

        progress.setMinimum(0);
        progress.setMaximum(10000);
        progress.setStringPainted(_size > 0);
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
        ok = new javax.swing.JLabel();

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

        ok.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ok.png"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ok)
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
                    .addComponent(ok, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        // TODO add your handling code here:

        if (SwingUtilities.isRightMouseButton(evt)) {
            if (!_canceled && !_finished && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, _lpath + "<br><br>CANCEL this transference?") == 0) {

                stop();

            } else if (!_canceled && _finished && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, _lpath + "<br><br>Clear this finished transference?") == 0) {
                clearFinished();
            }
        }
    }//GEN-LAST:event_formMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel action;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel local_path;
    private javax.swing.JLabel ok;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel remote_path;
    // End of variables declaration//GEN-END:variables
}
