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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
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
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
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
    
    public static String formatBytes(Long bytes) {
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        
        bytes = Math.max(bytes, 0L);
        
        int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);
        
        Double bytes_double = (double) bytes / (1L << (10 * pow));
        
        DecimalFormat df = new DecimalFormat("#.##");
        
        return df.format(bytes_double) + ' ' + units[pow];
    }
    
    public static String megaWhoami() {
        String whoami = Helpers.runProcess(new String[]{"mega-whoami"}, Helpers.isWindows() ? MEGA_CMD_WINDOWS_PATH : null)[1];
        
        if (!whoami.startsWith("[API:err:")) {
            
            return whoami.replaceAll("^.+: +(.+)$", "$1").trim();
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
    
    public static long getAccountFreeSpace(String email) {
        String[] space_used = getAccountSpaceData(email);
        
        return Long.parseLong(space_used[2]) - Long.parseLong(space_used[1]);
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
    
    public static void mostrarMensajeInformativo(JFrame frame, String m) {
        
        final String msg = m.replace("\n", "<br>");
        
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
    public static int mostrarMensajeInformativoSINO(JFrame frame, String m) {
        
        final String msg = m.replace("\n", "<br>");
        
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
    
    public static void mostrarMensajeError(JFrame frame, String m) {
        
        final String msg = m.replace("\n", "<br>");
        
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
            
            return new String[]{String.valueOf(pid), sb.toString()};
            
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
    
    public static void setContainerFont(Container container, Font font) {
        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                setContainerFont((Container) c, font);
            }
            try {
                c.setFont(font);
            } catch (Exception e) {
            }
        }
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
    
    public static String getNodePathFromFindCommandOutput(String node, String find) {
        
        final String regex = "(.+) <" + node + ">";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(find);
        
        if (matcher.find()) {
            return "/" + matcher.group(1).trim();
        }
        
        return null;
    }
    
    public static class JTextFieldRegularPopupMenu {
        
        public static JMenuItem refreshLastAccount = null;
        
        public static void addTo(JTextField txtField) {
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
            
            txtField.setComponentPopupMenu(popup);
        }
        
        public static void addTo(JTextArea txtArea) {
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
                                
                                Main.MAIN_WINDOW.forceRefreshAccount(email, "Force refresh", true, true);
                            }
                        });
                    }
                }
            };
            
            Action forceRefreshLastAccountAction = new AbstractAction("REFRESH LAST ACCOUNT") {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (!Main.MAIN_WINDOW.busy() && txtArea.isEnabled() && Main.MAIN_WINDOW.getLast_email_force_refresh() != null) {
                        Helpers.threadRun(() -> {
                            Main.MAIN_WINDOW.forceRefreshAccount(Main.MAIN_WINDOW.getLast_email_force_refresh(), "Force refresh", true, true);
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
            
            refreshLastAccount = new JMenuItem(forceRefreshLastAccountAction);
            refreshLastAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/refresh.png")));
            popup.add(refreshLastAccount);
            
            refreshLastAccount.setEnabled(false);
            
            popup.addSeparator();
            
            JMenuItem truncateAccount = new JMenuItem(truncateAccountAction);
            
            truncateAccount.setIcon(new javax.swing.ImageIcon(Helpers.class.getResource("/images/menu/remove.png")));
            
            popup.add(truncateAccount);
            
            txtArea.setComponentPopupMenu(popup);
        }
        
        public static void addRefreshableTo(JTextArea txtArea, Refresheable r) {
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
            
            txtArea.setComponentPopupMenu(popup);
        }
        
        private JTextFieldRegularPopupMenu() {
        }
    }
    
}
