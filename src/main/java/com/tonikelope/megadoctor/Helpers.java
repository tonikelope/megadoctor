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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

/**
 *
 * @author tonikelope
 */
public class Helpers {

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

    public static void mostrarMensajeInformativo(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, msg);

        } else {
            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, msg);
                }
            });
        }
    }

    // 0=yes, 1=no, 2=cancel
    public static int mostrarMensajeInformativoSINO(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            return JOptionPane.showConfirmDialog(frame, msg, "Info", JOptionPane.YES_NO_OPTION);

        } else {

            final int[] res = new int[1];

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {

                    res[0] = JOptionPane.showConfirmDialog(frame, msg, "Info", JOptionPane.YES_NO_OPTION);
                }
            });

            return res[0];

        }

    }

    public static void mostrarMensajeError(JFrame frame, String msg) {

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, msg, "ERROR", JOptionPane.ERROR_MESSAGE);

        } else {

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
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

        public static void addTo(JTextField txtField) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtField.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Deshacer") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtField.isEditable()) {
                        undoManager.undo();
                    } else {
                    }
                }
            };
            Action copyAction = new AbstractAction("Copiar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.copy();
                }
            };
            Action cutAction = new AbstractAction("Cortar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.cut();
                }
            };
            Action pasteAction = new AbstractAction("Pegar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Seleccionar todo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.selectAll();
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            JMenuItem undo = new JMenuItem(undoAction);
            undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
            popup.add(undo);

            popup.addSeparator();

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

            txtField.setComponentPopupMenu(popup);
        }

        public static void addTo(JTextArea txtArea) {
            JPopupMenu popup = new JPopupMenu();
            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Deshacer") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
                    } else {
                    }
                }
            };
            Action copyAction = new AbstractAction("Copiar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.copy();
                }
            };
            Action cutAction = new AbstractAction("Cortar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.cut();
                }
            };
            Action pasteAction = new AbstractAction("Pegar") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Seleccionar todo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtArea.selectAll();
                }
            };
            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));
            JMenuItem undo = new JMenuItem(undoAction);
            undo.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/undo.png")));
            popup.add(undo);

            popup.addSeparator();

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

            txtArea.setComponentPopupMenu(popup);
        }

        private JTextFieldRegularPopupMenu() {
        }
    }

}
