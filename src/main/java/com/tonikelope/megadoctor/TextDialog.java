/*
 __  __ _____ ____    _    ____   ___   ____ _____ ___  ____  
|  \/  | ____/ ___|  / \  |  _ \ / _ \ / ___|_   _/ _ \|  _ \ 
| |\/| |  _|| |  _  / _ \ | | | | | | | |     | || | | | |_) |
| |  | | |__| |_| |/ ___ \| |_| | |_| | |___  | || |_| |  _ < 
|_|  |_|_____\____/_/   \_\____/ \___/ \____| |_| \___/|_| \_\
                                                              
by tonikelope

 */
package com.tonikelope.megadoctor;

/**
 *
 * @author tonikelope
 */
public class TextDialog extends javax.swing.JDialog {

    private volatile String link = null;

    public String getLink() {
        return link;
    }

    /**
     * Creates new form MegaLinkImport
     */
    public TextDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        Helpers.JTextFieldRegularPopupMenu.addTo(texto);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        texto = new javax.swing.JTextField();
        cancel_button = new javax.swing.JButton();
        ok_button = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        texto.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        texto.setDoubleBuffered(true);

        cancel_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        ok_button.setBackground(new java.awt.Color(0, 153, 0));
        ok_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        ok_button.setForeground(new java.awt.Color(255, 255, 255));
        ok_button.setText("OK");
        ok_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ok_button.setDoubleBuffered(true);
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(texto, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ok_button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancel_button)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(texto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ok_button)
                    .addComponent(cancel_button))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed
        // TODO add your handling code here:
        dispose();
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed
        // TODO add your handling code here:

        if (Helpers.checkMEGALInk(texto.getText().trim())) {

            link = texto.getText().trim();

        }

        dispose();
    }//GEN-LAST:event_ok_buttonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton ok_button;
    private javax.swing.JTextField texto;
    // End of variables declaration//GEN-END:variables
}