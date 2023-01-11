package com.tonikelope.megadoctor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

/**
 *
 * @author tonikelope
 */
public class Main extends javax.swing.JFrame {

    public final static String VERSION = "0.29";
    public final static ThreadPoolExecutor THREAD_POOL = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public final static String MEGA_CMD_URL = "https://mega.io/cmd";
    public final static String MEGA_CMD_WINDOWS_PATH = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\MEGAcmd";
    public volatile static String MEGA_CMD_VERSION = null;
    public static Main MAIN_WINDOW;
    public final static LinkedHashMap<String, String> MEGA_ACCOUNTS = new LinkedHashMap<>();
    public final static HashMap<String, Object[]> MEGA_NODES = new HashMap<>();
    private final static ArrayList<String[]> MEGA_ACCOUNTS_SPACE = new ArrayList<>();
    private final static ArrayList<String> MEGA_ACCOUNTS_LOGIN_ERROR = new ArrayList<>();
    private volatile boolean _running = false;
    private volatile boolean _exit = false;
    private volatile boolean _firstAccountsTextareaClick = false;
    private volatile SelectEmailDialog _email_dialog = null;

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

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        Helpers.JTextFieldRegularPopupMenu.addTo(cuentas_textarea);
        Helpers.JTextFieldRegularPopupMenu.addTo(output_textarea);
        progressbar.setMinimum(0);
        this.setTitle("MegaDoctor " + VERSION);
        pack();

        Helpers.threadRun(() -> {

            Helpers.GUIRun(() -> {
                status_label.setText("Checking if MEGACMD is present...");
            });

            MEGA_CMD_VERSION = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-version"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

            if (MEGA_CMD_VERSION == null || "".equals(MEGA_CMD_VERSION)) {
                Helpers.mostrarMensajeError(this, "MEGA CMD IS REQUIRED");
                Helpers.openBrowserURLAndWait(MEGA_CMD_URL);
                System.exit(1);
            }

            Helpers.GUIRun(() -> {
                status_label.setText("");
            });

        });
    }

    public void copyNodes(String text, final boolean move) {

        _running = true;

        Helpers.GUIRun(() -> {

            MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
            MAIN_WINDOW.getVamos_button().setEnabled(false);
            MAIN_WINDOW.getSave_button().setEnabled(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText((move ? "MOVING" : "COPYING") + " FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToCopy = Helpers.extractNodeMapFromText(text);

        if (!nodesToCopy.isEmpty() && MEGA_ACCOUNTS.size() > nodesToCopy.keySet().size()) {

            ArrayList<String[]> exported_links = new ArrayList<>();

            for (String email : nodesToCopy.keySet()) {

                ArrayList<String> node_list = nodesToCopy.get(email);

                ArrayList<String> export_command = new ArrayList<>();

                export_command.add("mega-export");

                export_command.add("-fa");

                export_command.addAll(node_list);

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(MEGA_ACCOUNTS.get(email))}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                String exported_links_output = Helpers.runProcess(Helpers.buildCommand(export_command.toArray(String[]::new)), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                final String regex2 = "Exported +(.*?): +(https://.+)";

                final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);

                final Matcher matcher2 = pattern2.matcher(exported_links_output);

                while (matcher2.find()) {

                    exported_links.add(new String[]{matcher2.group(1), matcher2.group(2)});

                }

            }

            Helpers.GUIRunAndWait(() -> {
                _email_dialog = new SelectEmailDialog(MAIN_WINDOW, true, nodesToCopy.keySet());

                _email_dialog.setLocationRelativeTo(MAIN_WINDOW);

                _email_dialog.setVisible(true);
            });

            String email = (String) _email_dialog.getEmail_combobox().getSelectedItem();

            if (_email_dialog.isOk() && email != null && !email.isBlank()) {

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(MEGA_ACCOUNTS.get(email))}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                String df2 = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                String[] account_space = Helpers.getAccountSpaceData(email, df2);

                if (Helpers.getNodeMapTotalSize(nodesToCopy) <= (Long.parseLong(account_space[2]) - Long.parseLong(account_space[1]))) {

                    for (String[] s : exported_links) {

                        String folder = s[0].replaceAll("^(.*/)[^/]*$", "$1");

                        Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-mkdir", "-p", folder}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                        Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-import", s[1], folder}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);
                    }

                    String ls = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String ls2 = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-lr", "--show-handles"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String du = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-du", "-h", "--use-pcre", "/.*"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    String df = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df", "-h"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                    Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                    parseAccountNodes(email, ls2);

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

                            Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-login", email_rm, Helpers.escapeMEGAPassword(MEGA_ACCOUNTS.get(email_rm))}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            Helpers.runProcess(Helpers.buildCommand(delete_command.toArray(String[]::new)), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            String ls_rm = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            String ls2_rm = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-lr", "--show-handles"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            String du_rm = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-du", "-h", "--use-pcre", "/.*"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            String df_rm = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df", "-h"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            parseAccountNodes(email_rm, ls2_rm);

                            Helpers.GUIRun(() -> {

                                output_textarea.append("\n[" + email_rm + "] (Refreshed after deletion)\n\n" + df_rm + "\n" + du_rm + "\n" + ls_rm + "\n\n");

                            });
                        }

                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES MOVED");
                    } else {
                        Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES COPIED");
                    }
                } else {
                    Helpers.mostrarMensajeError(MAIN_WINDOW, "THERE IS NO ENOUGH FREE SPACE IN DESTINATION ACCOUNT");
                }

            }

        }else if (nodesToCopy.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:XXXXXXXX MEGA NODE)");
        }

        Helpers.GUIRun(() -> {

            MAIN_WINDOW.getCuentas_textarea().setEnabled(true);
            MAIN_WINDOW.getVamos_button().setEnabled(true);
            MAIN_WINDOW.getSave_button().setEnabled(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running = false;
    }

    public void removeNodes(String text) {

        _running = true;

        Helpers.GUIRun(() -> {

            MAIN_WINDOW.getCuentas_textarea().setEnabled(false);
            MAIN_WINDOW.getVamos_button().setEnabled(false);
            MAIN_WINDOW.getSave_button().setEnabled(false);
            MAIN_WINDOW.getProgressbar().setIndeterminate(true);
            MAIN_WINDOW.getStatus_label().setText("DELETING FOLDERS/FILES. PLEASE WAIT...");

        });

        HashMap<String, ArrayList<String>> nodesToRemove = Helpers.extractNodeMapFromText(text);

        if (!nodesToRemove.isEmpty() && Helpers.mostrarMensajeInformativoSINO(MAIN_WINDOW, "CAUTION!! THE FILES WILL BE PERMANENTLY DELETED. ARE YOU SURE?") == 0) {

            for (String email : nodesToRemove.keySet()) {

                ArrayList<String> node_list = nodesToRemove.get(email);

                ArrayList<String> delete_command = new ArrayList<>();

                delete_command.add("mega-rm");

                delete_command.add("-rf");

                delete_command.addAll(node_list);

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(MEGA_ACCOUNTS.get(email))}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                Helpers.runProcess(Helpers.buildCommand(delete_command.toArray(String[]::new)), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                String ls = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                String du = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-du", "-h", "--use-pcre", "/.*"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                String df = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df", "-h"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                Helpers.GUIRun(() -> {

                    output_textarea.append("\n[" + email + "] (Refreshed after deletion)\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                });
            }

            Helpers.mostrarMensajeInformativo(MAIN_WINDOW, "ALL SELECTED FOLDERS/FILES DELETED");
        } else if (nodesToRemove.isEmpty()) {
            Helpers.mostrarMensajeError(MAIN_WINDOW, "NO FOLDERS/FILES SELECTED (you must select with your mouse text that contains some H:xxxxxxxx MEGA NODE)");
        }

        Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        Helpers.GUIRun(() -> {

            MAIN_WINDOW.getCuentas_textarea().setEnabled(true);
            MAIN_WINDOW.getVamos_button().setEnabled(true);
            MAIN_WINDOW.getSave_button().setEnabled(true);
            MAIN_WINDOW.getProgressbar().setIndeterminate(false);
            MAIN_WINDOW.getStatus_label().setText("");

        });

        _running = false;
    }

    private void parseAccountNodes(String email, String ls) {

        final String regex = "([0-9]+|-) +[^ ]+ +[^ ]+ +(H:[^ ]+)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(ls);

        while (matcher.find()) {
            MEGA_NODES.put(matcher.group(2), new Object[]{Long.parseLong(matcher.group(1).equals("-") ? "0" : matcher.group(1)), email});
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
        jScrollPane1 = new javax.swing.JScrollPane();
        output_textarea = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        cuentas_textarea = new javax.swing.JTextArea();
        vamos_button = new javax.swing.JButton();
        status_label = new javax.swing.JLabel();
        progressbar = new javax.swing.JProgressBar();
        save_button = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
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

        output_textarea.setEditable(false);
        output_textarea.setBackground(new java.awt.Color(102, 102, 102));
        output_textarea.setColumns(20);
        output_textarea.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        output_textarea.setForeground(new java.awt.Color(255, 255, 255));
        output_textarea.setRows(5);
        jScrollPane1.setViewportView(output_textarea);

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
        jScrollPane2.setViewportView(cuentas_textarea);

        vamos_button.setBackground(new java.awt.Color(0, 153, 0));
        vamos_button.setFont(new java.awt.Font("Noto Sans", 1, 48)); // NOI18N
        vamos_button.setForeground(new java.awt.Color(255, 255, 255));
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

        save_button.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        save_button.setText("SAVE RESULTS TO FILE");
        save_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        save_button.setDoubleBuffered(true);
        save_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_buttonActionPerformed(evt);
            }
        });

        jMenuBar1.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N

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
                            .addComponent(vamos_button, javax.swing.GroupLayout.DEFAULT_SIZE, 912, Short.MAX_VALUE)
                            .addComponent(status_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(save_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jScrollPane1)
                    .addComponent(jScrollPane2)
                    .addComponent(progressbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                        .addComponent(save_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(status_label)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressbar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void vamos_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vamos_buttonActionPerformed
        // TODO add your handling code here:

        if (MEGA_CMD_VERSION != null) {

            if (!_running) {

                if (!_firstAccountsTextareaClick) {
                    _firstAccountsTextareaClick = true;
                    cuentas_textarea.setText("");
                    cuentas_textarea.setForeground(null);
                }

                _running = true;
                cuentas_textarea.setEnabled(false);
                save_button.setEnabled(false);
                vamos_button.setText("STOP");
                vamos_button.setBackground(Color.red);

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

                            Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

                            String login = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-login", email, Helpers.escapeMEGAPassword(mega_accounts.get(email))}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                            if (login.contains("Login failed")) {
                                MEGA_ACCOUNTS_LOGIN_ERROR.add(email + "#" + mega_accounts.get(email));
                                Helpers.GUIRun(() -> {
                                    output_textarea.append("\n[" + email + "] LOGIN ERROR\n\n");
                                });

                            } else {

                                Helpers.GUIRun(() -> {
                                    status_label.setText("Reading " + email + " info...");
                                });

                                String ls = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-aahr", "--show-handles", "--tree"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                String ls2 = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-ls", "-lr", "--show-handles"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                String du = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-du", "-h", "--use-pcre", "/.*"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                String df = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df", "-h"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                String df2 = Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-df"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];

                                Helpers.GUIRun(() -> {

                                    output_textarea.append("\n[" + email + "]\n\n" + df + "\n" + du + "\n" + ls + "\n\n");

                                });

                                MEGA_ACCOUNTS_SPACE.add(Helpers.getAccountSpaceData(email, df2));

                                parseAccountNodes(email, ls2);
                            }

                            i++;

                            int j = i;

                            Helpers.GUIRun(() -> {
                                progressbar.setValue(j);

                            });

                            if (_exit) {
                                break;
                            }

                        }

                        Helpers.runProcess(Helpers.buildCommand(new String[]{"mega-logout"}), Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

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

                        });

                        Helpers.mostrarMensajeInformativo(this, _exit ? "CANCELED!" : "DONE");

                    } else {
                        Helpers.mostrarMensajeInformativo(this, "DONE");
                    }

                    Helpers.GUIRun(() -> {
                        progressbar.setValue(0);
                        vamos_button.setText("CHECK ACCOUNTS");
                        vamos_button.setBackground(new Color(0, 153, 0));
                        vamos_button.setEnabled(true);
                        cuentas_textarea.setEnabled(true);
                        status_label.setText("");
                        save_button.setEnabled(true);
                    });

                    _running = false;
                    _exit = false;

                });

            } else if (!_exit) {
                if (Helpers.mostrarMensajeInformativoSINO(this, "SURE?") == 0) {
                    _exit = true;
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

        if (_running || !"".equals(output_textarea.getText().trim())) {

            if (Helpers.mostrarMensajeInformativoSINO(this, "EXIT?") == 0) {
                System.exit(0);
            }

        } else {
            System.exit(0);
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
                MAIN_WINDOW.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea cuentas_textarea;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel logo_label;
    private javax.swing.JTextArea output_textarea;
    private javax.swing.JProgressBar progressbar;
    private javax.swing.JButton save_button;
    private javax.swing.JLabel status_label;
    private javax.swing.JButton vamos_button;
    // End of variables declaration//GEN-END:variables
}
