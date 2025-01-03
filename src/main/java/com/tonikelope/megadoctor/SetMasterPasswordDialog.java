/*
 __  __ _____ ____    _    ____   ___   ____ _____ ___  ____  
|  \/  | ____/ ___|  / \  |  _ \ / _ \ / ___|_   _/ _ \|  _ \ 
| |\/| |  _|| |  _  / _ \ | | | | | | | |     | || | | | |_) |
| |  | | |__| |_| |/ ___ \| |_| | |_| | |___  | || |_| |  _ < 
|_|  |_|_____\____/_/   \_\____/ \___/ \____| |_| \___/|_| \_\
                                                              
by tonikelope

 */
package com.tonikelope.megadoctor;

import java.awt.event.KeyEvent;
import java.util.Arrays;

/**
 *
 * @author tonikelope
 */
public class SetMasterPasswordDialog extends javax.swing.JDialog {

    private boolean _pass_ok;

    private String _new_pass_hash;

    private String _current_pass_hash;

    private byte[] _new_pass;

    private final String _salt;

    public boolean isPass_ok() {
        return _pass_ok;
    }

    public byte[] getNew_pass() {
        return _new_pass;
    }

    public void deleteNewPass() {

        if (_new_pass != null) {

            Arrays.fill(_new_pass, (byte) 0);
        }

        _new_pass = null;
    }

    public String getNew_pass_hash() {
        return _new_pass_hash;
    }

    /**
     * Creates new form MegaPassDialog
     *
     * @param parent
     * @param modal
     * @param salt
     */
    public SetMasterPasswordDialog(java.awt.Frame parent, boolean modal, String salt, String current_pass_hash) {

        super(parent, modal);

        initComponents();

        _pass_ok = false;

        _new_pass = null;

        _new_pass_hash = null;

        _current_pass_hash = current_pass_hash;

        _salt = salt;

        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancel_button = new javax.swing.JButton();
        ok_button = new javax.swing.JButton();
        warning_label = new javax.swing.JLabel();
        status_label = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        confirm_pass_label = new javax.swing.JLabel();
        new_pass_textfield = new javax.swing.JPasswordField();
        current_pass_textfield = new javax.swing.JPasswordField();
        confirm_pass_textfield = new javax.swing.JPasswordField();
        current_pass = new javax.swing.JLabel();
        new_pass_label = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Master password setup");
        setResizable(false);

        cancel_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        ok_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ok_button.setText("OK");
        ok_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ok_button.setDoubleBuffered(true);
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        warning_label.setBackground(new java.awt.Color(255, 0, 0));
        warning_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        warning_label.setForeground(new java.awt.Color(255, 255, 255));
        warning_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        warning_label.setText("WARNING: if you forget this password, you will have to insert all your accounts again.");
        warning_label.setDoubleBuffered(true);
        warning_label.setOpaque(true);

        status_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        status_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        status_label.setDoubleBuffered(true);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/lock_medium.png"))); // NOI18N

        confirm_pass_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        confirm_pass_label.setText("Confirm new password:");
        confirm_pass_label.setDoubleBuffered(true);

        new_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        new_pass_textfield.setDoubleBuffered(true);
        new_pass_textfield.setMargin(new java.awt.Insets(2, 2, 2, 2));

        current_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        current_pass_textfield.setDoubleBuffered(true);
        current_pass_textfield.setMargin(new java.awt.Insets(2, 2, 2, 2));

        confirm_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        confirm_pass_textfield.setDoubleBuffered(true);
        confirm_pass_textfield.setMargin(new java.awt.Insets(2, 2, 2, 2));
        confirm_pass_textfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                confirm_pass_textfieldKeyPressed(evt);
            }
        });

        current_pass.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        current_pass.setText("Current password:");
        current_pass.setDoubleBuffered(true);

        new_pass_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        new_pass_label.setText("New password:");
        new_pass_label.setDoubleBuffered(true);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(confirm_pass_label)
                    .addComponent(current_pass)
                    .addComponent(new_pass_label))
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(confirm_pass_textfield)
                    .addComponent(current_pass_textfield)
                    .addComponent(new_pass_textfield))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(current_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(current_pass))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(new_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(new_pass_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(confirm_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confirm_pass_label))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(warning_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(status_label, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(ok_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cancel_button))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(24, 24, 24)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(warning_label)
                .addGap(18, 18, 18)
                .addComponent(status_label)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ok_button)
                    .addComponent(cancel_button))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        this.setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed

        status_label.setText("Verifying your password, please wait...");

        pack();

        Helpers.threadRun(() -> {
            try {

                String current_pass_hash = "";

                if (!"".equals(_current_pass_hash)) {
                    byte[] current_pass = Helpers.PBKDF2HMACSHA256(new String(current_pass_textfield.getPassword()), Helpers.BASE642Bin(_salt), Helpers.MASTER_PASSWORD_PBKDF2_ITERATIONS, Helpers.MASTER_PASSWORD_PBKDF2_OUTPUT_BIT_LENGTH);
                    current_pass_hash = Helpers.Bin2BASE64(Helpers.HashBin("SHA-1", current_pass));
                }

                if ((!"".equals(_current_pass_hash) && !_current_pass_hash.equals(current_pass_hash)) || ("".equals(_current_pass_hash) && !"".equals(new String(current_pass_textfield.getPassword())))) {

                    Helpers.GUIRun(() -> {
                        Helpers.mostrarMensajeError(Main.MAIN_WINDOW, "CURRENT PASSWORD DOES NOT MATCH!");

                        status_label.setText("");

                        current_pass_textfield.setText("");

                        new_pass_textfield.setText("");

                        confirm_pass_textfield.setText("");

                        new_pass_textfield.grabFocus();

                        pack();
                    });

                } else {

                    if (Arrays.equals(new_pass_textfield.getPassword(), confirm_pass_textfield.getPassword())) {

                        Helpers.GUIRun(() -> {
                            status_label.setText("Processing your password, please wait...");
                        });

                        if (new_pass_textfield.getPassword().length > 0) {

                            _new_pass = Helpers.PBKDF2HMACSHA256(new String(new_pass_textfield.getPassword()), Helpers.BASE642Bin(_salt), Helpers.MASTER_PASSWORD_PBKDF2_ITERATIONS, Helpers.MASTER_PASSWORD_PBKDF2_OUTPUT_BIT_LENGTH);

                            _new_pass_hash = Helpers.Bin2BASE64(Helpers.HashBin("SHA-1", _new_pass));
                        } else {
                            _new_pass_hash = "";
                        }

                        _pass_ok = true;

                        Helpers.GUIRun(() -> {
                            this.setVisible(false);
                        });

                    } else {
                        Helpers.GUIRun(() -> {
                            Helpers.mostrarMensajeError(Main.MAIN_WINDOW, "PASSWORDS DO NOT MATCH");

                            status_label.setText("");

                            new_pass_textfield.setText("");

                            confirm_pass_textfield.setText("");

                            new_pass_textfield.grabFocus();

                            pack();
                        });
                    }
                }

            } catch (Exception ex) {
            }
        });
    }//GEN-LAST:event_ok_buttonActionPerformed

    private void confirm_pass_textfieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_confirm_pass_textfieldKeyPressed

        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {

            ok_buttonActionPerformed(null);
        }
    }//GEN-LAST:event_confirm_pass_textfieldKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel_button;
    private javax.swing.JLabel confirm_pass_label;
    private javax.swing.JPasswordField confirm_pass_textfield;
    private javax.swing.JLabel current_pass;
    private javax.swing.JPasswordField current_pass_textfield;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel new_pass_label;
    private javax.swing.JPasswordField new_pass_textfield;
    private javax.swing.JButton ok_button;
    private javax.swing.JLabel status_label;
    private javax.swing.JLabel warning_label;
    // End of variables declaration//GEN-END:variables

}
