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
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

/**
 *
 * @author tonikelope
 */
public class UploadFileDialog extends javax.swing.JDialog implements Refresheable {

    public static String LAST_FOLDER = null;
    private volatile boolean _closing = false;

    public JRadioButton getParts_radio() {
        return parts_radio;
    }

    public JSpinner getParts_spinner() {
        return parts_spinner;
    }

    public JTextField getSplit_textbox() {
        return split_textbox;
    }

    public boolean isAuto() {
        return _auto;
    }

    public boolean isSplit() {
        return _split;
    }

    public String getEmail() {
        return _email;
    }

    public boolean isOk() {
        return _ok;
    }

    public String getLocal_path() {
        return _lpath;
    }

    public String getRemote_path() {
        return _rpath;
    }

    public long getLocal_size() {
        return _local_size;
    }

    public JCheckBox getRemove_after() {
        return remove_after;
    }

    private volatile boolean _ok = false;
    private volatile long _local_size = 0;
    private volatile long _free_space = 0;
    private volatile String _email = null;
    private volatile String _link = null;
    private volatile String _lpath = null;
    private volatile String _rpath = null;
    private volatile boolean _split = false;
    private volatile long _split_size = -1;
    private volatile boolean _auto = false;
    private volatile boolean _init = false;
    private final AtomicBoolean _terminate_walk_tree = new AtomicBoolean();
    private static volatile String LAST_EMAIL = null;

    public long getSplit_size() {
        return _split_size;
    }

    public JCheckBox getSplit_delete() {
        return split_delete;
    }

    private void updatePartsSpinnerRange() {
        Helpers.GUIRun(() -> {

            try {
                long chunk_size = Long.parseLong(split_textbox.getText()) * 1024 * 1024;

                if (chunk_size > 0 && _local_size >= chunk_size) {

                    int tot_chunks = (int) Math.ceil((float) _local_size / chunk_size);

                    SpinnerModel sm = new SpinnerNumberModel(1, 1, tot_chunks, 1);

                    parts_spinner.setModel(sm);

                    ((DefaultEditor) parts_spinner.getEditor()).getTextField().setEditable(false);
                }
            } catch (Exception ex) {
            }
        });
    }

    public UploadFileDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        initComponents();

        _init = true;

        split_panel.setVisible(false);

        split_textbox.setEnabled(false);

        split_delete.setEnabled(false);

        split_mb.setEnabled(false);

        split_icon.setEnabled(false);

        parts_spinner.setEnabled(false);

        all_chunks_radio.setSelected(true);

        all_chunks_radio.setEnabled(false);

        parts_radio.setEnabled(false);

        remove_after.setVisible(false);

        ((DefaultEditor) parts_spinner.getEditor()).getTextField().setEditable(false);

        local_path_scroll_panel.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));

        _terminate_walk_tree.set(false);

        Helpers.JTextFieldRegularPopupMenu.addLiteMEGAAccountPopupMenuTo(account_stats_textarea, this);

        Helpers.JTextFieldRegularPopupMenu.addTextActionsPopupMenuTo(remote_path);

        vamos_button.setEnabled(false);

        ArrayList<String> emails = new ArrayList<>();

        for (String email : Main.MEGA_ACCOUNTS.keySet()) {
            emails.add(email);
        }

        Collections.sort(emails);

        for (String email : emails) {
            email_combobox.addItem(email);
        }

        email_combobox.setSelectedItem(LAST_EMAIL != null ? LAST_EMAIL : emails.get(0));

        progress.setIndeterminate(true);

        progress.setVisible(false);

        local_folder_progress.setIndeterminate(true);

        local_folder_progress.setVisible(false);

        _init = false;

        email_comboboxItemStateChanged(null);

        pack();
    }

    private boolean checkFreeSpace() {

        if (_local_size > 0 && _free_space > 0) {

            if (auto_select_account.isSelected()) {
                if (_local_size > 0) {
                    Helpers.GUIRun(() -> {
                        vamos_button.setEnabled(true);
                    });
                    return true;
                }
            } else if (_local_size > _free_space) {
                Helpers.GUIRun(() -> {
                    free_space.setForeground(Color.red);
                    vamos_button.setEnabled(false);
                });
                return false;
            } else {
                Helpers.GUIRun(() -> {
                    free_space.setForeground(new Color(0, 153, 0));
                    vamos_button.setEnabled(true);
                });
                return true;
            }
        } else {

            Helpers.GUIRun(() -> {
                vamos_button.setEnabled(_link != null);
            });

            return _link != null;
        }

        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        vamos_button = new javax.swing.JButton();
        local_file_button = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        remote_path = new javax.swing.JTextField();
        local_folder_button = new javax.swing.JButton();
        progress = new javax.swing.JProgressBar();
        local_size = new javax.swing.JLabel();
        account_stats_scroll = new javax.swing.JScrollPane();
        account_stats_textarea = new javax.swing.JTextArea();
        local_folder_progress = new javax.swing.JProgressBar();
        mega_button = new javax.swing.JButton();
        auto_select_account = new javax.swing.JCheckBox();
        local_path_scroll_panel = new javax.swing.JScrollPane();
        local_path = new javax.swing.JLabel();
        accounts_panel = new javax.swing.JPanel();
        free_space = new javax.swing.JLabel();
        email_combobox = new javax.swing.JComboBox<>();
        remove_after = new javax.swing.JCheckBox();
        split_panel = new javax.swing.JPanel();
        split_textbox = new javax.swing.JTextField();
        split_mb = new javax.swing.JLabel();
        split_checkbox = new javax.swing.JCheckBox();
        split_icon = new javax.swing.JLabel();
        split_delete = new javax.swing.JCheckBox();
        all_chunks_radio = new javax.swing.JRadioButton();
        parts_radio = new javax.swing.JRadioButton();
        parts_spinner = new javax.swing.JSpinner();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("UPLOAD");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        vamos_button.setBackground(new java.awt.Color(0, 153, 0));
        vamos_button.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        vamos_button.setForeground(new java.awt.Color(255, 255, 255));
        vamos_button.setText("LET'S GO");
        vamos_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        vamos_button.setDoubleBuffered(true);
        vamos_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vamos_buttonActionPerformed(evt);
            }
        });

        local_file_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_file_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/file.png"))); // NOI18N
        local_file_button.setText("SELECT FILE");
        local_file_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        local_file_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                local_file_buttonActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        jLabel2.setText("Customized remote full path (optional):");
        jLabel2.setToolTipText("Default remote path is /filename (if some folder in your remote path does not exist it will be created)");
        jLabel2.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        remote_path.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N

        local_folder_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_folder_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/folder.png"))); // NOI18N
        local_folder_button.setText("SELECT FOLDER");
        local_folder_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        local_folder_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                local_folder_buttonActionPerformed(evt);
            }
        });

        local_size.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_size.setDoubleBuffered(true);

        account_stats_textarea.setEditable(false);
        account_stats_textarea.setBackground(new java.awt.Color(102, 102, 102));
        account_stats_textarea.setColumns(20);
        account_stats_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        account_stats_textarea.setForeground(new java.awt.Color(255, 255, 255));
        account_stats_textarea.setRows(5);
        account_stats_textarea.setDoubleBuffered(true);
        account_stats_scroll.setViewportView(account_stats_textarea);

        local_folder_progress.setDoubleBuffered(true);

        mega_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        mega_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mega.png"))); // NOI18N
        mega_button.setText("USE MEGA LINK");
        mega_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        mega_button.setDoubleBuffered(true);
        mega_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mega_buttonActionPerformed(evt);
            }
        });

        auto_select_account.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        auto_select_account.setText("AUTO-ALLOCATION");
        auto_select_account.setToolTipText("Auto search an account with free space");
        auto_select_account.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        auto_select_account.setDoubleBuffered(true);
        auto_select_account.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                auto_select_accountActionPerformed(evt);
            }
        });

        local_path_scroll_panel.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        local_path.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        local_path_scroll_panel.setViewportView(local_path);

        accounts_panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        free_space.setFont(new java.awt.Font("Noto Sans", 1, 24)); // NOI18N
        free_space.setText("---");
        free_space.setToolTipText("Free account space");
        free_space.setDoubleBuffered(true);

        email_combobox.setFont(new java.awt.Font("Noto Sans", 0, 24)); // NOI18N
        email_combobox.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        email_combobox.setDoubleBuffered(true);
        email_combobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                email_comboboxItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout accounts_panelLayout = new javax.swing.GroupLayout(accounts_panel);
        accounts_panel.setLayout(accounts_panelLayout);
        accounts_panelLayout.setHorizontalGroup(
            accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accounts_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(free_space)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(email_combobox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        accounts_panelLayout.setVerticalGroup(
            accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(accounts_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(email_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(free_space))
                .addContainerGap())
        );

        remove_after.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        remove_after.setText("Delete local file/folder after successful upload");
        remove_after.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        remove_after.setDoubleBuffered(true);
        remove_after.setOpaque(true);
        remove_after.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remove_afterActionPerformed(evt);
            }
        });

        split_panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        split_textbox.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        split_textbox.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        split_textbox.setText("20450");
        split_textbox.setDoubleBuffered(true);
        split_textbox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                split_textboxKeyReleased(evt);
            }
        });

        split_mb.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        split_mb.setText("MB");
        split_mb.setDoubleBuffered(true);

        split_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        split_checkbox.setText("SPLIT");
        split_checkbox.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        split_checkbox.setDoubleBuffered(true);
        split_checkbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                split_checkboxActionPerformed(evt);
            }
        });

        split_icon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/menu/icons8-cut-30.png"))); // NOI18N
        split_icon.setDoubleBuffered(true);

        split_delete.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        split_delete.setText("Delete original file");
        split_delete.setToolTipText("Delete original file after splitting");
        split_delete.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        split_delete.setDoubleBuffered(true);
        split_delete.setOpaque(true);
        split_delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                split_deleteActionPerformed(evt);
            }
        });

        all_chunks_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        all_chunks_radio.setText("All parts");
        all_chunks_radio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        all_chunks_radio.setDoubleBuffered(true);
        all_chunks_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                all_chunks_radioActionPerformed(evt);
            }
        });

        parts_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        parts_radio.setText("Part:");
        parts_radio.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        parts_radio.setDoubleBuffered(true);
        parts_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parts_radioActionPerformed(evt);
            }
        });

        parts_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        parts_spinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        parts_spinner.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        parts_spinner.setDoubleBuffered(true);

        javax.swing.GroupLayout split_panelLayout = new javax.swing.GroupLayout(split_panel);
        split_panel.setLayout(split_panelLayout);
        split_panelLayout.setHorizontalGroup(
            split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, split_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(split_panelLayout.createSequentialGroup()
                        .addComponent(all_chunks_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parts_radio)
                        .addGap(0, 0, 0)
                        .addComponent(parts_spinner, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(split_delete))
                    .addGroup(split_panelLayout.createSequentialGroup()
                        .addComponent(split_checkbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(split_icon)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(split_textbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(split_mb)))
                .addContainerGap())
        );
        split_panelLayout.setVerticalGroup(
            split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(split_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(split_icon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(split_checkbox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(split_textbox)
                        .addComponent(split_mb)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(split_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(split_delete)
                    .addComponent(all_chunks_radio)
                    .addComponent(parts_radio)
                    .addComponent(parts_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(local_folder_progress, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(account_stats_scroll)
                    .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(auto_select_account)
                        .addGap(18, 18, 18)
                        .addComponent(accounts_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(vamos_button))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(remote_path))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(local_file_button)
                        .addGap(18, 18, 18)
                        .addComponent(local_folder_button)
                        .addGap(18, 18, 18)
                        .addComponent(mega_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(local_size)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(local_path_scroll_panel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(split_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(remove_after)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(local_file_button)
                        .addComponent(local_folder_button)
                        .addComponent(local_size)
                        .addComponent(mega_button))
                    .addComponent(local_path_scroll_panel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(remove_after)
                    .addComponent(split_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(local_folder_progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(account_stats_scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(remote_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(auto_select_account, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(accounts_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(vamos_button, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void vamos_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vamos_buttonActionPerformed
        // TODO add your handling code here:

        if (!_lpath.isBlank()) {
            auto_select_account.setEnabled(false);
            split_panel.setEnabled(false);
            local_file_button.setEnabled(false);
            local_folder_button.setEnabled(false);
            mega_button.setEnabled(false);
            vamos_button.setEnabled(false);
            progress.setVisible(true);

            File f = new File(_lpath);

            _rpath = remote_path.getText().isBlank() ? "/" : remote_path.getText().trim();

            if (!auto_select_account.isSelected()) {
                _email = (String) email_combobox.getSelectedItem();
            }

            if (_split && _link != null) {

                if (f.isDirectory()) {

                    if (!_rpath.endsWith("/")) {
                        _rpath += "/";
                    }

                    if (_rpath.equals("/")) {
                        _rpath += f.getName() + "/";
                    }
                } else {

                    Long sp_size = Long.parseLong(getSplit_textbox().getText()) * 1024 * 1024;

                    try {
                        if (Files.size(Paths.get(this._lpath)) > sp_size) {

                            _split_size = sp_size;

                            if (!_rpath.endsWith("/")) {
                                _rpath += "/" + f.getName() + "/";
                            }

                            if (_rpath.equals("/")) {
                                _rpath += f.getName() + "/";
                            }
                        } else {
                            _split = false;
                            _ok = true;
                            dispose();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(UploadFileDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                _ok = true;
                dispose();

            } else {

                _ok = true;
                dispose();
            }
        } else {
            dispose();
        }

    }//GEN-LAST:event_vamos_buttonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        if (!progress.isVisible()) {
            _terminate_walk_tree.set(true);
            dispose();
        } else if (!_closing) {
            _closing = true;
        } else if (_closing && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "FORCE MegaDoctor EXIT?") == 0) {
            Main.EXIT = true;
            Helpers.destroyAllExternalProcesses();
            System.exit(1);
        } else {
            _closing = false;
        }
    }//GEN-LAST:event_formWindowClosing

    private void local_file_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_local_file_buttonActionPerformed
        // TODO add your handling code here:

        local_file_button.setEnabled(false);

        local_folder_button.setEnabled(false);

        mega_button.setEnabled(false);

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setPreferredSize(new Dimension(800, 600));

        if (LAST_FOLDER != null) {
            fileChooser.setCurrentDirectory(new File(LAST_FOLDER));
        }

        Helpers.updateComponentFont(fileChooser, fileChooser.getFont(), 1.20f);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {

            File file = fileChooser.getSelectedFile();

            LAST_FOLDER = file.getParentFile().getAbsolutePath();

            _lpath = file.getAbsolutePath();

            local_path.setText(file.getAbsolutePath());

            local_size.setText("");

            _local_size = file.length();

            _link = null;

            local_size.setText(Helpers.formatBytes(_local_size));

            split_panel.setVisible(getLocal_path() != null);

            split_textbox.setVisible(true);

            split_mb.setVisible(true);

            split_delete.setVisible(true);

            all_chunks_radio.setVisible(true);
            parts_radio.setVisible(true);
            parts_spinner.setVisible(true);

            split_checkbox.setText("SPLIT FILE");

            split_checkbox.setToolTipText("Split file in chunks");

            remove_after.setText(_split ? "Delete local file PART after successful upload" : "Delete local file after successful upload");

            remove_after.setVisible(true);

            updatePartsSpinnerRange();

            checkFreeSpace();
        }

        local_folder_button.setEnabled(true);

        local_file_button.setEnabled(true);

        mega_button.setEnabled(true);

        Helpers.smartPack(this);
    }//GEN-LAST:event_local_file_buttonActionPerformed

    private void local_folder_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_local_folder_buttonActionPerformed
        // TODO add your handling code here:

        local_folder_button.setEnabled(false);

        local_file_button.setEnabled(false);

        mega_button.setEnabled(false);

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setPreferredSize(new Dimension(800, 600));

        if (LAST_FOLDER != null) {
            fileChooser.setCurrentDirectory(new File(LAST_FOLDER));
        }

        Helpers.updateComponentFont(fileChooser, fileChooser.getFont(), 1.20f);

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {

            File file = fileChooser.getSelectedFile();

            LAST_FOLDER = file.getAbsolutePath();

            _lpath = file.getAbsolutePath();

            local_path.setText(file.getAbsolutePath());
            local_folder_progress.setVisible(true);
            local_folder_button.setText("Calculating folder size...");
            local_size.setText("");
            Helpers.threadRun(() -> {
                long size = Helpers.getDirectorySize(file, _terminate_walk_tree);
                if (size > 0) {
                    _local_size = size;
                    _link = null;
                    Helpers.GUIRun(() -> {
                        local_folder_button.setEnabled(true);
                        local_file_button.setEnabled(true);
                        mega_button.setEnabled(true);
                        local_folder_button.setText("SELECT FOLDER");
                        local_folder_progress.setVisible(false);
                        local_size.setText(Helpers.formatBytes(_local_size));
                        split_panel.setVisible(auto_select_account.isSelected() && getLocal_path() != null);
                        split_textbox.setVisible(false);
                        split_mb.setVisible(false);
                        split_delete.setVisible(false);
                        all_chunks_radio.setVisible(false);
                        parts_radio.setVisible(false);
                        parts_spinner.setVisible(false);
                        split_checkbox.setText("SPLIT FOLDER");
                        split_checkbox.setToolTipText("Create a transfer for every folder child (first level)");
                        remove_after.setText("Delete local " + (_split ? "file" : "folder") + " after successful upload");
                        remove_after.setVisible(true);
                        checkFreeSpace();
                    });
                }

            });
        } else {
            local_folder_button.setEnabled(true);
            local_file_button.setEnabled(true);
            mega_button.setEnabled(true);
            local_folder_button.setText("SELECT FOLDER");
        }

        Helpers.smartPack(this);
    }//GEN-LAST:event_local_folder_buttonActionPerformed

    private void email_comboboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_email_comboboxItemStateChanged
        // TODO add your handling code here:

        if (!_init) {
            String email = (String) email_combobox.getSelectedItem();

            if (email != null && !email.isBlank() && email_combobox.isEnabled()) {
                LAST_EMAIL = email;
                progress.setVisible(true);
                email_combobox.setEnabled(false);
                local_file_button.setEnabled(false);
                local_folder_button.setEnabled(false);
                mega_button.setEnabled(false);
                vamos_button.setEnabled(false);
                account_stats_textarea.setText("");
                free_space.setText("");
                auto_select_account.setEnabled(false);

                Helpers.threadRun(() -> {

                    String stats = Main.MAIN_WINDOW.getAccountStatistics(email);

                    if (stats == null) {
                        Helpers.mostrarMensajeError(Main.MAIN_WINDOW, "LOGIN ERROR: " + email);

                        _init = true;

                        Helpers.GUIRunAndWait(() -> {

                            progress.setVisible(false);
                            email_combobox.setEnabled(true);
                            local_file_button.setEnabled(true);
                            local_folder_button.setEnabled(true);
                            mega_button.setEnabled(true);
                            vamos_button.setEnabled(true);
                            auto_select_account.setEnabled(true);
                            email_combobox.setSelectedIndex(-1);

                        });

                        _init = false;
                    } else {

                        ConcurrentHashMap<String, Long> reserved = Helpers.getReservedTransfersSpace();

                        _free_space = Helpers.getAccountFreeSpace(email) - (reserved.containsKey(email) ? reserved.get(email) : 0);

                        Main.MAIN_WINDOW.parseAccountNodes(email);

                        Helpers.GUIRun(() -> {

                            free_space.setText(Helpers.formatBytes(_free_space) + " (free)");
                            account_stats_textarea.setText("[" + email + "] \n\n" + stats + "\n\n");
                            account_stats_textarea.setCaretPosition(0);
                            progress.setVisible(false);
                            email_combobox.setEnabled(true);
                            local_file_button.setEnabled(true);
                            local_folder_button.setEnabled(true);
                            mega_button.setEnabled(true);
                            vamos_button.setEnabled(true);
                            auto_select_account.setEnabled(true);
                            Helpers.smartPack(this);

                        });

                        checkFreeSpace();
                    }

                });
            }

        }
    }//GEN-LAST:event_email_comboboxItemStateChanged

    private void mega_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mega_buttonActionPerformed
        // TODO add your handling code here:
        boolean vamos_button_enabled = vamos_button.isEnabled();

        UploadMegaLinkDialog dialog = new UploadMegaLinkDialog(null, true);
        Helpers.setCenterOfParent(this, dialog);
        dialog.setVisible(true);

        if (dialog.getLink() != null) {
            auto_select_account.setSelected(false);
            auto_select_accountActionPerformed(evt);
            _link = dialog.getLink();
            local_path.setText(dialog.getLink());

            _local_size = Helpers.getMEGALinkSize(_link);

            if (_local_size > 0) {
                local_size.setText(Helpers.formatBytes(_local_size));
            } else {
                local_size.setText("");
                _local_size = 0;
            }

            _lpath = dialog.getLink();
            vamos_button.setEnabled(true);
            email_comboboxItemStateChanged(null);
            Helpers.smartPack(this);
        } else {
            vamos_button.setEnabled(vamos_button_enabled);
        }
    }//GEN-LAST:event_mega_buttonActionPerformed

    private void auto_select_accountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_auto_select_accountActionPerformed
        // TODO add your handling code here:

        _auto = auto_select_account.isSelected();
        email_combobox.setEnabled(!auto_select_account.isSelected());
        account_stats_textarea.setVisible(!auto_select_account.isSelected());
        account_stats_scroll.setHorizontalScrollBarPolicy(!auto_select_account.isSelected() ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        account_stats_scroll.setVerticalScrollBarPolicy(!auto_select_account.isSelected() ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        free_space.setVisible(!auto_select_account.isSelected());

        split_panel.setVisible((auto_select_account.isSelected() || (getLocal_path() != null && !Files.isDirectory(Paths.get(getLocal_path())))) && getLocal_path() != null && this._link == null);

        split_textbox.setVisible(getLocal_path() != null && !Files.isDirectory(Paths.get(getLocal_path())));

        split_mb.setVisible(getLocal_path() != null && !Files.isDirectory(Paths.get(getLocal_path())));

        if (split_panel.isVisible()) {
            split_checkbox.setText("SPLIT " + (Files.isDirectory(Paths.get(getLocal_path())) ? "FOLDER" : "FILE"));
            split_checkbox.setToolTipText(Files.isDirectory(Paths.get(getLocal_path())) ? "Create a transfer for every folder child (first level)" : "Split file in chunks");
        }

        checkFreeSpace();
    }//GEN-LAST:event_auto_select_accountActionPerformed

    private void split_checkboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_checkboxActionPerformed
        // TODO add your handling code here:
        _split = split_checkbox.isSelected();

        split_textbox.setEnabled(_split);

        split_mb.setEnabled(_split);

        split_delete.setEnabled(_split);

        split_icon.setEnabled(_split);

        all_chunks_radio.setEnabled(_split);

        parts_radio.setEnabled(_split);

        parts_spinner.setEnabled(_split && parts_radio.isSelected());

        remove_after.setText((_split && !Files.isDirectory(Paths.get(_lpath))) ? "Delete local file PART after successful upload" : "Delete local " + ((Files.isDirectory(Paths.get(_lpath)) && !_split) ? "folder" : "file") + " after successful upload");

    }//GEN-LAST:event_split_checkboxActionPerformed

    private void remove_afterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_afterActionPerformed
        // TODO add your handling code here:
        if (remove_after.isSelected() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "SURE?") != 0) {
            remove_after.setSelected(false);
        }

        remove_after.setBackground(remove_after.isSelected() ? Color.red : null);
        remove_after.setForeground(remove_after.isSelected() ? Color.white : null);

    }//GEN-LAST:event_remove_afterActionPerformed

    private void split_deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_deleteActionPerformed
        // TODO add your handling code here:
        if (split_delete.isSelected() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "SURE?") != 0) {
            split_delete.setSelected(false);
        }

        split_delete.setBackground(split_delete.isSelected() ? Color.red : null);
        split_delete.setForeground(split_delete.isSelected() ? Color.white : null);

    }//GEN-LAST:event_split_deleteActionPerformed

    private void all_chunks_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_all_chunks_radioActionPerformed
        // TODO add your handling code here:

        all_chunks_radio.setSelected(true);
        parts_radio.setSelected(false);
        parts_spinner.setEnabled(false);

    }//GEN-LAST:event_all_chunks_radioActionPerformed

    private void parts_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parts_radioActionPerformed
        // TODO add your handling code here:
        all_chunks_radio.setSelected(false);
        parts_radio.setSelected(true);
        parts_spinner.setEnabled(true);
    }//GEN-LAST:event_parts_radioActionPerformed

    private void split_textboxKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_split_textboxKeyReleased
        // TODO add your handling code here:
        try {

            updatePartsSpinnerRange();

        } catch (Exception ex) {
        }
    }//GEN-LAST:event_split_textboxKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane account_stats_scroll;
    private javax.swing.JTextArea account_stats_textarea;
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JRadioButton all_chunks_radio;
    private javax.swing.JCheckBox auto_select_account;
    private javax.swing.JComboBox<String> email_combobox;
    private javax.swing.JLabel free_space;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton local_file_button;
    private javax.swing.JButton local_folder_button;
    private javax.swing.JProgressBar local_folder_progress;
    private javax.swing.JLabel local_path;
    private javax.swing.JScrollPane local_path_scroll_panel;
    private javax.swing.JLabel local_size;
    private javax.swing.JButton mega_button;
    private javax.swing.JRadioButton parts_radio;
    private javax.swing.JSpinner parts_spinner;
    private javax.swing.JProgressBar progress;
    private javax.swing.JTextField remote_path;
    private javax.swing.JCheckBox remove_after;
    private javax.swing.JCheckBox split_checkbox;
    private javax.swing.JCheckBox split_delete;
    private javax.swing.JLabel split_icon;
    private javax.swing.JLabel split_mb;
    private javax.swing.JPanel split_panel;
    private javax.swing.JTextField split_textbox;
    private javax.swing.JButton vamos_button;
    // End of variables declaration//GEN-END:variables

    @Override
    public void refresh() {
        Helpers.GUIRun(() -> {
            email_comboboxItemStateChanged(null);
        });
    }

    @Override
    public void enableR(boolean enable) {
        Helpers.GUIRun(() -> {

            progress.setVisible(!enable);
            email_combobox.setEnabled(enable);

        });
    }
}
