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
import static com.tonikelope.megadoctor.Main.MEGA_NODES;
import static com.tonikelope.megadoctor.Main.THREAD_POOL;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitOption;
import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
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

    public static class ClipStateListener implements LineListener {

        private final Object _notifier = new Object();
        private volatile boolean _completed = false;

        public Object getNotifier() {
            return _notifier;
        }

        public boolean isCompleted() {
            return _completed;
        }

        @Override
        public void update(LineEvent event) {
            if (LineEvent.Type.STOP == event.getType()) {

                _completed = true;

                synchronized (_notifier) {
                    _notifier.notifyAll();
                }
            }
        }
    }

    public static void playWavResource(String sound) {

        try (BufferedInputStream bis = new BufferedInputStream(Helpers.class.getResourceAsStream("/sounds/" + sound)); final AudioInputStream audioStream = AudioSystem.getAudioInputStream(bis); final Clip clip = AudioSystem.getClip()) {

            final ClipStateListener listener = new ClipStateListener();

            clip.addLineListener(listener);

            clip.open(audioStream);

            final var gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float db = 20f * (float) Math.log10(1.0f);

            gainControl.setValue(db >= gainControl.getMinimum() ? db : gainControl.getMinimum());

            clip.start();

            while (!listener.isCompleted()) {
                synchronized (listener._notifier) {
                    listener._notifier.wait(1000);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, ex.getMessage());
            Logger.getLogger(Helpers.class.getName()).log(Level.SEVERE, "ERROR -> {0}", sound);
        }
    }

    public static void setWindowLowRightCorner(Window w) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - w.getWidth();
        int y = (int) rect.getMaxY() - w.getHeight();
        w.setLocation(x, y);
    }

    public static String[] extractAccountLoginDataFromText(String email, String text) {
        final String regex = Pattern.quote(email) + "#(.+)";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            return new String[]{email, matcher.group(1)};

        }

        return null;
    }

    public static ConcurrentHashMap<String, Long> getReservedTransfersSpace() {

        synchronized (Main.TRANSFERENCES_LOCK) {

            ConcurrentHashMap<Component, Transference> transferences_map = Main.TRANSFERENCES_MAP;

            ConcurrentHashMap<String, Long> reserved = new ConcurrentHashMap<>();

            for (Transference t : transferences_map.values()) {
                if (!t.isFinishing() && !t.isFinished() && !t.isCanceled()) {

                    if (reserved.containsKey(t.getEmail())) {
                        long s = reserved.get(t.getEmail());
                        reserved.put(t.getEmail(), s + t.getFileSize());
                    } else {
                        reserved.put(t.getEmail(), t.getFileSize());
                    }
                }
            }

            return reserved;
        }

    }

    public static String findFirstAccountWithSpace(long required, String filename) {
        String bingo = null;

        ConcurrentHashMap<String, Long> reserved = getReservedTransfersSpace();

        ArrayList<String> emails = new ArrayList<>();

        for (String email : Main.MEGA_ACCOUNTS.keySet()) {
            emails.add(email);
        }

        Collections.sort(emails);

        for (String email : emails) {

            Helpers.GUIRunAndWait(() -> {
                Main.MAIN_WINDOW.getStatus_label().setText("[" + Helpers.formatBytes(required) + "] " + filename + " -> " + email);
            });

            Long r = reserved.get(email);

            Long s = Main.FREE_SPACE_CACHE.get(email);

            if (s == null) {

                s = Helpers.getAccountFreeSpace(email);

                Main.FREE_SPACE_CACHE.put(email, s);
            }

            if (s - (r != null ? r : 0) >= required) {
                bingo = email;
                break;
            }
        }

        Helpers.GUIRunAndWait(() -> {
            Main.MAIN_WINDOW.getStatus_label().setText("");
        });

        return bingo;
    }

    public static String exportPathFromCurrentAccount(String rpath) {
        String[] export = Helpers.runProcess(new String[]{"mega-export", "-af", rpath}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(export[2]) == 0) {
            final String regex = "https://.+$";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(export[1]);

            if (matcher.find()) {

                return matcher.group(0);
            }
        }

        return null;

    }

    public static String adjustSpaces(String s, int n) {

        String spaces = s.trim().replaceAll("^[^ ]*( *)[^ ]*$", "$1");

        String new_spaces = " ".repeat(spaces.length() + n);

        return s.replace(spaces, new_spaces);
    }

    public static void smartPack(Window w) {

        Helpers.GUIRun(() -> {
            if (w.getPreferredSize().getHeight() > w.getSize().getHeight() || w.getPreferredSize().getWidth() > w.getSize().getWidth()) {
                w.pack();
                setCenterOfParent((Window) w.getParent(), w);
            }

            w.revalidate();
            w.repaint();

        });

    }

    public static boolean checkMEGALInk(String link) {
        link = link.trim();

        final String regex = "https://mega\\.nz/((folder|file)/([^#]+)#(.+)|#(F?)!([^!]+)!(.+))";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(link);

        return matcher.find();
    }

    public static String formatBytes(Long bytes) {

        String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        Double bytes_double = (double) bytes / (1L << (10 * pow));

        DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bytes_double) + ' ' + units[pow];
    }

    public static String megaWhoami() {
        String[] whoami = Helpers.runProcess(new String[]{"mega-whoami"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(whoami[2]) == 0) {

            return whoami[1].replaceAll("^.+: +(.+)$", "$1").trim().toLowerCase();
        }

        return "";
    }

    public static void setCenterOfParent(Window parent, Window dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);/*from w  ww. j av a2 s. com*/
    }

    public static void setCenterOfParent(JFrame parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);/*from w  ww. j av a2 s. com*/
    }

    public static void setCenterOfParent(JDialog parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x
                + (parentSize.width / 2 - size.width / 2), parentPosition.y
                + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);
    }

    public static String[] getAccountSpaceData(String email) {
        Main.MAIN_WINDOW.login(email);

        String df = Helpers.runProcess(new String[]{"mega-df"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];
        final String regex = "USED STORAGE: *([0-9]+).*?of *([0-9]+)";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(df);

        if (matcher.find()) {
            return new String[]{email, matcher.group(1), matcher.group(2)};
        }

        return null;
    }

    public static long getAccountUsedSpace(String email) {
        String[] space_stats = getAccountSpaceData(email);

        return Long.parseLong(space_stats[1]);
    }

    public static long getAccountFreeSpace(String email) {
        String[] space_stats = getAccountSpaceData(email);

        return Long.parseLong(space_stats[2]) - Long.parseLong(space_stats[1]);
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

    public static long getNodeListTotalSize(ArrayList<String> node_list) {

        long total = 0L;

        for (String node : node_list) {
            total += (long) ((Object[]) Main.MEGA_NODES.get(node))[0];
        }

        return total;
    }

    public static void mostrarMensajeInformativo(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>");

        } else {
            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>");
                }
            });
        }
    }

    // 0=yes, 1=no, 2=cancel
    public static int mostrarMensajeInformativoSINO(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            return JOptionPane.showConfirmDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);

        } else {

            final int[] res = new int[1];

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {

                    res[0] = JOptionPane.showConfirmDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "Info", JOptionPane.YES_NO_OPTION);
                }
            });

            return res[0];

        }

    }

    public static void mostrarMensajeError(JFrame frame, String m) {

        final String msg = m.replace("\n", "<br>");

        if (SwingUtilities.isEventDispatchThread()) {

            JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);

        } else {

            Helpers.GUIRunAndWait(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(frame, "<html><div align='center'>" + msg + "</div></html>", "ERROR", JOptionPane.ERROR_MESSAGE);
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

            ProcessBuilder processbuilder = new ProcessBuilder(Helpers.buildCommand(command));

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

            return new String[]{String.valueOf(pid), sb.toString(), String.valueOf(process.exitValue())};

        } catch (Exception ex) {
        }

        return null;
    }

    public static String extractFirstEmailFromtext(String text) {

        final String regex = "[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.\\-]+@[a-zA-Z0-9.\\-]+";

        final Pattern pattern = Pattern.compile(regex);

        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(0);
        }

        return null;
    }

    //Thanks -> https://stackoverflow.com/a/19877372 https://stackoverflow.com/a/1385498
    public static long getDirectorySize(final File folder, final AtomicBoolean terminate) {

        Path path = folder.toPath();

        final AtomicLong size = new AtomicLong(0);

        EnumSet<FileVisitOption> opts = EnumSet.of(FOLLOW_LINKS);

        try {
            Files.walkFileTree(path, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    if (terminate.get()) {
                        System.out.println("WALKTREEE" + folder.getAbsolutePath() + " TERMINATED!");
                        size.set(-1);
                        return FileVisitResult.TERMINATE;
                    }

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null) {
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    }
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();

    }

    public static HashMap<String, ArrayList<String>> extractNodeMapFromText(String text) {

        final String regex = "H:[^> ]+";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(text);

        HashMap<String, ArrayList<String>> nodesMAP = new HashMap<>();

        while (matcher.find()) {

            String node = matcher.group(0);

            if (MEGA_NODES.containsKey(node)) {

                String email = (String) ((Object[]) MEGA_NODES.get(node))[1];

                if (!nodesMAP.containsKey(email)) {
                    nodesMAP.put(email, new ArrayList<>());
                }

                nodesMAP.get(email).add(node);
            }

        }

        return nodesMAP;
    }

    public static void copyTextToClipboard(String text) {

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);

    }

    public static String getNodeFullPath(String node) {

        String[] f = Helpers.runProcess(new String[]{"mega-find", "--show-handles", node}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null);

        if (Integer.parseInt(f[2]) == 0) {
            final String regex = "(.+) <" + node + ">";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(f[1]);

            if (matcher.find()) {
                return "/" + matcher.group(1).trim();
            }
        }

        return null;
    }

    public static void updateComponentFont(final Component component, final Font font, final Float zoom_factor) {

        if (component != null) {

            if (component instanceof Container) {

                for (Component child : ((Container) component).getComponents()) {
                    updateComponentFont(child, font, zoom_factor);
                }
            }

            Font old_font = component.getFont();

            Font new_font = font.deriveFont(old_font.getStyle(), zoom_factor != null ? Math.round(old_font.getSize() * zoom_factor) : old_font.getSize());

            if (component instanceof JTable) {
                ((JTable) component).getTableHeader().setFont(new_font);
            }

            component.setFont(new_font);

        }
    }

    public static class JTextFieldRegularPopupMenu {

        public static void addTextActionsPopupMenuTo(JTextField txtField) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtField.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtField.isEditable()) {
                        undoManager.undo();
                    }
                }
            };

            Action copyAction = new AbstractAction("Copy") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.copy();
                }
            };
            Action cutAction = new AbstractAction("Cut") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.cut();
                }
            };
            Action pasteAction = new AbstractAction("Paste") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.paste();
                }
            };
            Action selectAllAction = new AbstractAction("Sellect all") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    txtField.selectAll();
                }
            };

            cutAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
            copyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
            pasteAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
            selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));

            if (txtField.isEditable()) {
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

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtField.setComponentPopupMenu(popup);
        }

        public static void addMainMEGAPopupMenuTo(JTextArea txtArea) {
            JPopupMenu popup = new JPopupMenu();

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
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
            Action copyInsideMEGANodesAction = new AbstractAction("COPY SELECTED MEGA FOLDERS/FILES (INSIDE THE ACCOUNT)") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesInsideAccount(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action moveInsideMEGANodesAction = new AbstractAction("MOVE SELECTED MEGA FOLDERS/FILES (INSIDE THE ACCOUNT)") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.moveNodesInsideAccount(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action removeMEGANodesAction = new AbstractAction("DELETE SELECTED MEGA FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.removeNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action copyMEGANodesAction = new AbstractAction("COPY SELECTED MEGA FOLDERS/FILES TO ANOTHER ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesToAnotherAccount(txtArea.getSelectedText(), false);
                        });
                    }
                }
            };
            Action moveMEGANodesAction = new AbstractAction("MOVE SELECTED MEGA FOLDERS/FILES TO ANOTHER ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.copyNodesToAnotherAccount(txtArea.getSelectedText(), true);
                        });
                    }
                }
            };
            Action renameMEGANodesAction = new AbstractAction("RENAME SELECTED MEGA FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.renameNodes(txtArea.getSelectedText());
                        });
                    }
                }
            };
            Action enableExporMEGANodesAction = new AbstractAction("ENABLE PUBLIC LINK ON SELECTED MEGA FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.exportNodes(txtArea.getSelectedText(), true);
                        });
                    }
                }
            };
            Action disableExporMEGANodesAction = new AbstractAction("DISABLE PUBLIC LINK ON SELECTED MEGA FOLDERS/FILES") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.exportNodes(txtArea.getSelectedText(), false);
                        });
                    }
                }
            };
            Action forceRefreshAccountAction = new AbstractAction("REFRESH SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.forceRefreshAccount(email, "Forced FULL REFRESH (SESSION was regenerated)", true, true);
                            }
                        });
                    }
                }
            };

            Action forceRefreshFastAccountAction = new AbstractAction("REFRESH (FAST) SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {

                                Main.MAIN_WINDOW.forceRefreshAccount(email, "Forced FAST REFRESH", true, false);
                            }
                        });
                    }
                }
            };

            Action truncateAccountAction = new AbstractAction("TRUNCATE SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {
                                Main.MAIN_WINDOW.truncateAccount(email);
                            }
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

            JMenuItem renameNodes = new JMenuItem(renameMEGANodesAction);
            renameNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/rename.png")));

            popup.add(renameNodes);

            popup.addSeparator();

            JMenuItem copyInsideNodes = new JMenuItem(copyInsideMEGANodesAction);
            copyInsideNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));

            popup.add(copyInsideNodes);

            popup.addSeparator();

            JMenuItem moveInsideNodes = new JMenuItem(moveInsideMEGANodesAction);
            moveInsideNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/move.png")));

            popup.add(moveInsideNodes);

            popup.addSeparator();

            JMenuItem removeNodes = new JMenuItem(removeMEGANodesAction);
            removeNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(removeNodes);

            popup.addSeparator();

            JMenuItem copyNodes = new JMenuItem(copyMEGANodesAction);
            copyNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/copy.png")));

            popup.add(copyNodes);

            popup.addSeparator();

            JMenuItem moveNodes = new JMenuItem(moveMEGANodesAction);
            moveNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/move.png")));

            popup.add(moveNodes);

            popup.addSeparator();

            JMenuItem publicONNodes = new JMenuItem(enableExporMEGANodesAction);
            publicONNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

            popup.add(publicONNodes);

            popup.addSeparator();

            JMenuItem publicOFFNodes = new JMenuItem(disableExporMEGANodesAction);
            publicOFFNodes.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_off.png")));

            popup.add(publicOFFNodes);

            popup.addSeparator();

            JMenuItem refreshAccount = new JMenuItem(forceRefreshAccountAction);
            refreshAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(refreshAccount);

            JMenuItem refreshFastAccount = new JMenuItem(forceRefreshFastAccountAction);

            refreshFastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

            popup.add(refreshFastAccount);

            if (Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                Action forceRefreshLastAccountAction = new AbstractAction("REFRESH (FAST) LAST ACCOUNT -> " + Main.MAIN_WINDOW.getLast_email_force_refresh()) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                            Helpers.threadRun(() -> {
                                Main.MAIN_WINDOW.forceRefreshAccount(Main.MAIN_WINDOW.getLast_email_force_refresh(), "Forced FAST REFRESH", true, false);
                            });
                        }
                    }
                };
                JMenuItem refreshLastAccount = new JMenuItem(forceRefreshLastAccountAction);

                refreshLastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));

                refreshLastAccount.setEnabled(Main.MAIN_WINDOW != null && Main.MAIN_WINDOW.getLast_email_force_refresh() != null);

                popup.add(refreshLastAccount);

            }

            popup.addSeparator();

            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);

            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(truncateAccount);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtArea.setComponentPopupMenu(popup);
        }

        public static void addLiteMEGAAccountPopupMenuTo(JTextArea txtArea, Refresheable r) {
            JPopupMenu popup = new JPopupMenu();
            Refresheable _refresh = r;

            UndoManager undoManager = new UndoManager();
            txtArea.getDocument().addUndoableEditListener(undoManager);
            Action undoAction = new AbstractAction("Undo") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (undoManager.canUndo() && txtArea.isEditable()) {
                        undoManager.undo();
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
                    if (txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {
                            _refresh.enableR(false);
                            Main.MAIN_WINDOW.removeNodes(txtArea.getSelectedText());
                            _refresh.enableR(true);
                            _refresh.refresh();
                        });
                    }
                }
            };

            Action truncateAccountAction = new AbstractAction("TRUNCATE SELECTED ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (txtArea.isEnabled() && txtArea.getSelectedText() != null && !txtArea.getSelectedText().isEmpty()) {
                        Helpers.threadRun(() -> {

                            String email = Helpers.extractFirstEmailFromtext(txtArea.getSelectedText());

                            if (email != null) {
                                _refresh.enableR(false);
                                Main.MAIN_WINDOW.truncateAccount(email);
                                _refresh.enableR(true);
                                _refresh.refresh();
                            }

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

            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);

            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));

            popup.add(truncateAccount);

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            txtArea.setComponentPopupMenu(popup);
        }

        public static void addTransferencePopupMenuTo(Transference t) {
            JPopupMenu popup = new JPopupMenu();

            Transference _t = t;

            Action copyPublicLinkAction = new AbstractAction("COPY PUBLIC LINK") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (_t.isFinished()) {
                        File f = new File(_t.getLpath());
                        Helpers.threadRun(() -> {
                            Helpers.copyTextToClipboard(_t.getPublic_link() != null ? _t.getPublic_link() : "");
                            Helpers.mostrarMensajeInformativo(Main.MAIN_WINDOW, "<b>" + f.getName() + "</b><br>" + _t.getPublic_link() + "<br><br>COPIED TO CLIPBOARD");
                        });
                    }
                }
            };

            Action cancelTransferenceLinkAction = new AbstractAction("CANCEL") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    File f = new File(_t.getLpath());
                    if (!_t.isFinished() && !_t.isCanceled() && Helpers.mostrarMensajeInformativoSINO(Main.MAIN_WINDOW, "<b>" + f.getName() + "</b><br><br><b>CANCEL</b> this transference?") == 0) {
                        Helpers.threadRun(() -> {
                            _t.stop();
                        });
                    }
                }
            };

            Action clearTransferenceLinkAction = new AbstractAction("CLEAR") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    File f = new File(_t.getLpath());
                    if (_t.isFinished() && !_t.isCanceled()) {
                        Helpers.threadRun(() -> {
                            _t.stop();
                        });
                    }
                }
            };

            if (!_t.isFinishing() && !_t.isFinished()) {

                JMenuItem cancelTransference = new JMenuItem(cancelTransferenceLinkAction);

                cancelTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/cancel.png")));

                popup.add(cancelTransference);

            } else {

                JMenuItem clearTransference = new JMenuItem(clearTransferenceLinkAction);

                clearTransference.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/clear.png")));

                popup.add(clearTransference);

                if (_t.getPublic_link() != null) {

                    popup.addSeparator();

                    JMenuItem copyPublicLink = new JMenuItem(copyPublicLinkAction);

                    copyPublicLink.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/export_on.png")));

                    popup.add(copyPublicLink);

                }
            }

            updateComponentFont(popup, popup.getFont(), 1.20f);

            popup.setCursor(new Cursor(Cursor.HAND_CURSOR));

            _t.getMain_panel().setComponentPopupMenu(popup);
        }

        private JTextFieldRegularPopupMenu() {
        }
    }

}
