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

/**
 *
 * @author tonikelope
 */
public class Transference extends javax.swing.JPanel {

    public static final int WARMING_TRANSFER_WAIT = 5000;
    private volatile int _tag;
    private volatile int _action;
    private volatile int _prog;
    private volatile long _size;
    private volatile String _lpath, _rpath, _email;
    private volatile boolean _running;
    private volatile boolean _finished;

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

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(true);

            });

            try {
                Thread.sleep(WARMING_TRANSFER_WAIT); //SECURITY WAIT
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
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

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(true);

            });

            while (transferRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Transference.class.getName()).log(Level.SEVERE, null, ex);
            }

            Helpers.runProcess(new String[]{"mega-export", "-af", _rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

            Helpers.GUIRun(() -> {
                progress.setIndeterminate(false);
                progress.setStringPainted(true);
                progress.setValue(progress.getMaximum());
                ok.setVisible(true);
                local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _lpath);
            });

            Main.MAIN_WINDOW.forceRefreshAccount(_email, "Refreshed after upload [" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _rpath, false, false);

            _running = false;

            _finished = true;

            synchronized (TRANSFERENCES_LOCK) {

                TRANSFERENCES_LOCK.notifyAll();

            }
        });
    }

    private boolean transferRunning() {

        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--limit=1"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        return !transfer_data.trim().isEmpty();
    }

    private boolean updateProgress() {

        if (!transferRunning()) {
            return false;
        }

        String transfer_data = Helpers.runProcess(new String[]{"mega-transfers", "--summary"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

        String[] transfer_data_lines = transfer_data.split("\n");

        final String regex = "^ *(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *% +(\\d+) +([\\d.]+ +[^ ]+) +([\\d.]+ +[^ ]+) +([\\d.]+) *%.*$";
        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(transfer_data_lines[1].trim());

        if (matcher.find()) {

            _prog = (int) (Float.parseFloat(matcher.group((_action == 0 ? 4 : 8))) * 100);

            Helpers.GUIRun(() -> {
                local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] (" + Integer.parseInt(matcher.group((_action == 0 ? 1 : 5))) + " files pending) " + _lpath);

                if (_size > 0) {

                    local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] " + _lpath);

                    progress.setValue(_prog);
                } else {
                    local_path.setText("[" + (_size > 0 ? Helpers.formatBytes(_size) : "---") + "] (" + Integer.parseInt(matcher.group((_action == 0 ? 1 : 5))) + " files pending (" + matcher.group((_action == 0 ? 3 : 7)).replaceAll("  *", " ") + ")) " + _lpath);

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

        local_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_path.setText("jLabel1");
        local_path.setDoubleBuffered(true);

        action.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel action;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel local_path;
    private javax.swing.JLabel ok;
    private javax.swing.JProgressBar progress;
    private javax.swing.JLabel remote_path;
    // End of variables declaration//GEN-END:variables
}
