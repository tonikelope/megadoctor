package com.tonikelope.megadoctor;

import static com.tonikelope.megadoctor.Main.THREAD_POOL;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

/**
 *
 * @author tonikelope
 */
public class Helpers {

    public static String formatBytes(Long bytes) {

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        Double bytes_double = (double) bytes / (1L << (10 * pow));

        DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bytes_double) + ' ' + units[pow];
    }

    public static String[] getAccountSpaceData(String email, String df) {
        final String regex = "USED STORAGE: *([0-9]+).*?of *([0-9]+)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(df);

        if (matcher.find()) {
            return new String[]{email, matcher.group(1), matcher.group(2)};
        }

        return null;
    }

    public static String getFechaHoraActual() {

        String format = "dd-MM-yyyy HH:mm:ss";

        return getFechaHoraActual(format);
    }

    public static String getFechaHoraActual(String format) {

        Date currentDate = new Date(System.currentTimeMillis());

        DateFormat df = new SimpleDateFormat(format);

        return df.format(currentDate);
    }

    public static Sequencer midiLoopPlay(String midi) {
        try {
            Sequencer sequencer = MidiSystem.getSequencer(); // Get the default Sequencer
            if (sequencer == null) {
                System.err.println("Sequencer device not supported");
                return null;
            }
            sequencer.open(); // Open device
            // Create sequence, the File must contain MIDI file data.
            Sequence sequence = MidiSystem.getSequence(Helpers.class.getResource(midi));
            sequencer.setSequence(sequence); // load it into sequencer
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();  // start the playback
            return sequencer;
        } catch (MidiUnavailableException | InvalidMidiDataException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void GUIRun(Runnable r) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(r);
        } else {
            r.run();
        }

    }

    public static void GUIRunAndWait(Runnable r) {

        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(r);
            } else {
                r.run();
            }
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static Future threadRun(Runnable r) {

        return THREAD_POOL.submit(r);

    }

    public static long getNodeMapTotalSize(HashMap<String, ArrayList<String>> map) {

        long total = 0L;

        for (String email : map.keySet()) {

            ArrayList<String> node_list = map.get(email);

            for (String node : node_list) {
                total += (long) ((Object[]) Main.MEGA_NODES.get(node))[0];
            }
        }

        return total;
    }

    public static void mostrarMensajeInformativo(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>");

        } else {
            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>");
                }
            });
        }
    }

    // 0=yes, 1=no, 2=cancel
    public static int mostrarMensajeInformativoSINO(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            return JOptionPane.showConfirmDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);

        } else {

            final int[] res = new int[1];

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {

                    res[0] = JOptionPane.showConfirmDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);
                }
            });

            return res[0];

        }

    }

    public static void mostrarMensajeError(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);

        } else {

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center' style='font-size:1.2em'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            });
        }

    }

    public static void openBrowserURLAndWait(final String url) {

        try {
            Desktop.getDesktop().browse(new URI(url));

        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(Helpers.class
                    .getName()).log(Level.SEVERE, null, ex.getMessage());
        }

    }

    public static boolean isWindows() {

        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static String[] buildCommand(String[] command) {

        String[] c = Helpers.isWindows() ? new String[]{"cmd.exe", "/c"} : new String[]{};

        String[] result = Arrays.copyOf(c, c.length + command.length);

        System.arraycopy(command, 0, result, c.length, command.length);

        return result;
    }

    public static String escapeMEGAPassword(String password) {

        return password.contains(" ") ? password : "\"" + password + "\"";
    }

    public static String[] runProcess(String[] command, String path) {
        try {

            ProcessBuilder processbuilder = new ProcessBuilder(command);

            if (path != null && !"".equals(path)) {

                processbuilder.environment().put("PATH", path + File.pathSeparator + System.getenv("PATH"));

            }

            Process process = processbuilder.start();

            long pid = process.pid();

            StringBuilder sb = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception ex) {
                Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, null, ex);
            }

            process.waitFor();

            return new String[]{String.valueOf(pid), sb.toString()};

        } catch (Exception ex) {
        }

        return null;
    }

    public static class JTextFieldRegularPopupMenu {

        public static void addTo(JTextArea txtArea) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
                    } else {
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.selectAll();
                }
            };

            Action removeMEGANodesAction = new AbstractAction("DELETE SELECTED MEGA FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (Main.MAIN_WINDOW.getCuentas_textarea().isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.removeNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action copyMEGANodesAction = new AbstractAction("COPY SELECTED MEGA FOLDERS/FILES TO ANOTHER ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (Main.MAIN_WINDOW.getCuentas_textarea().isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtArea.isEditable()) {
                JMenuItem undo = new JMenuItem(undoAction);
                undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
                popup.add(undo);

                popup.addSeparator();
            }

            JMenuItem cut = new JMenuItem(cutAction);
            cut.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cut.png")));
            popup.add(cut);

            JMenuItem copy = new JMenuItem(copyAction);
            copy.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));
            popup.add(copy);

            JMenuItem paste = new JMenuItem(pasteAction);
            paste.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/paste.png")));
            popup.add(paste);

            popup.addSeparator();

            JMenuItem selectAll = new JMenuItem(selectAllAction);
            selectAll.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/select_all.png")));
            popup.add(selectAll);

            popup.addSeparator();

            JMenuItem removeNodes = new JMenuItem(removeMEGANodesAction);
            removeNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(removeNodes);

            popup.addSeparator();

            JMenuItem copyNodes = new JMenuItem(copyMEGANodesAction);
            copyNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));

            popup.add(copyNodes);

            txtArea.setComponentPopupMenu(popup);
        }

        private JTextFieldRegularPopupMenu() {
        }
    }

}
