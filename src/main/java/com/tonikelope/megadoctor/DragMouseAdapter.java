package com.tonikelope.megadoctor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

//T-H-A-N-K-S -> https://stackoverflow.com/a/27247011
class DragMouseAdapter extends MouseAdapter {

    private static final Rectangle R1 = new Rectangle();
    private static final Rectangle R2 = new Rectangle();
    private static volatile Rectangle prevRect;
    private final JWindow window = new JWindow();
    private final int gestureMotionThreshold = DragSource.getDragThreshold();
    private final Object working_notifier;
    private volatile Component draggingComonent;
    private volatile int index = -1;
    private volatile Component gap;
    private volatile Point startPt;
    private volatile Point dragOffset;
    private volatile boolean working = false;

    public boolean isWorking() {
        return working;
    }

    public DragMouseAdapter(Object wnotifier) {
        super();
        window.setBackground(new Color(0, true));
        working_notifier = wnotifier;
    }

    private void setWorking(boolean w) {

        working = w;

        if (!working) {
            Helpers.threadRun(() -> {
                synchronized (working_notifier) {
                    working_notifier.notifyAll();
                }
            });
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setWorking(true);
        JComponent parent = (JComponent) e.getComponent();
        if (parent.getComponentCount() <= 1) {
            startPt = null;
            return;
        }
        startPt = e.getPoint();
    }

    private void startDragging(JComponent parent, Point pt) {
        //get a dragging panel
        Component c = parent.getComponentAt(pt);
        index = parent.getComponentZOrder(c);
        if (Objects.equals(c, parent) || index < 0) {
            return;
        }
        draggingComonent = c;
        Dimension d = draggingComonent.getSize();

        Point dp = draggingComonent.getLocation();
        dragOffset = new Point(pt.x - dp.x, pt.y - dp.y);

        //make a dummy filler
        gap = Box.createRigidArea(d);
        swapComponentLocation(parent, c, gap, index);

        //make a cursor window
        window.add(draggingComonent);
        window.pack();

        updateWindowLocation(pt, parent);
        window.setVisible(true);
    }

    private void updateWindowLocation(Point pt, JComponent parent) {
        Point p = new Point(pt.x - dragOffset.x, pt.y - dragOffset.y);
        SwingUtilities.convertPointToScreen(p, parent);
        window.setLocation(p);
    }

    private static int getTargetIndex(Rectangle r, Point pt, int i) {
        int ht2 = (int) (.5 + r.height * .5);
        R1.setBounds(r.x, r.y, r.width, ht2);
        R2.setBounds(r.x, r.y + ht2, r.width, ht2);
        if (R1.contains(pt)) {
            prevRect = R1;
            return i - 1 > 0 ? i : 0;
        } else if (R2.contains(pt)) {
            prevRect = R2;
            return i;
        }
        return -1;
    }

    private static void swapComponentLocation(
            Container parent, Component remove, Component add, int idx) {
        parent.remove(remove);
        parent.add(add, idx);
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (startPt == null) {
            return;
        }

        Point pt = e.getPoint();
        JComponent parent = (JComponent) e.getComponent();

        //MotionThreshold
        double a = Math.pow(pt.x - startPt.x, 2);
        double b = Math.pow(pt.y - startPt.y, 2);
        if (draggingComonent == null
                && Math.sqrt(a + b) > gestureMotionThreshold) {
            startDragging(parent, pt);
            return;
        }
        if (!window.isVisible() || draggingComonent == null) {
            return;
        }

        //update the cursor window location
        updateWindowLocation(pt, parent);
        if (prevRect != null && prevRect.contains(pt)) {
            return;
        }

        //change the dummy filler location
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponent(i);
            Rectangle r = c.getBounds();
            if (Objects.equals(c, gap) && r.contains(pt)) {
                return;
            }
            int tgt = getTargetIndex(r, pt, i);
            if (tgt >= 0) {
                swapComponentLocation(parent, gap, gap, tgt);
                return;
            }
        }
        parent.remove(gap);
        parent.revalidate();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        startPt = null;
        if (!window.isVisible() || draggingComonent == null) {
            setWorking(false);
            return;
        }
        Point pt = e.getPoint();
        JComponent parent = (JComponent) e.getComponent();

        //close the cursor window
        Component cmp = draggingComonent;
        draggingComonent = null;
        prevRect = null;
        startPt = null;
        dragOffset = null;
        window.setVisible(false);

        //swap the dragging panel and the dummy filler
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponent(i);
            if (Objects.equals(c, gap)) {
                swapComponentLocation(parent, gap, cmp, i);
                setWorking(false);
                return;
            }
            int tgt = getTargetIndex(c.getBounds(), pt, i);
            if (tgt >= 0) {
                swapComponentLocation(parent, gap, cmp, tgt);
                setWorking(false);
                return;
            }
        }
        if (parent.getParent().getBounds().contains(pt)) {
            swapComponentLocation(parent, gap, cmp, parent.getComponentCount());
        } else {
            swapComponentLocation(parent, gap, cmp, index);
        }

        setWorking(false);
    }
}
