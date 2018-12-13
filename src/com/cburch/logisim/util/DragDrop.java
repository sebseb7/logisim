/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */

package com.cburch.logisim.util;

// import java.awt.dnd.DragSourceEvent;
// import java.awt.dnd.DragSourceListener;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceMotionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class DragDrop {

  public final DataFlavor dataFlavor;
  public final DataFlavor[] dataFlavors;
  public final Class dataClass;

  // DragDrop and Support are intended to make it easier to add drag and drop
  // support to a class:
  //
  // public class Foo ... implements DragDrop.Support {
  //    ...
  //    public static final DragDrop dnd = new DragDrop(Foo.class);
  //    public DragDrop getDragDrop() { return dnd; }
  // }
  //
  // This is enough to make Foo implement all of the methods of interface
  // Transferable (the sending side), and provide a jvm-local data flavor for
  // the class (used by the receiving side).
  
  public DragDrop(Class cls) {
    DataFlavor f = null;
    try {
      f = new DataFlavor(
          String.format("%s;class=\"%s\"",
            DataFlavor.javaJVMLocalObjectMimeType,
            cls.getName()));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    dataClass = cls;
    dataFlavor = f;
    dataFlavors = f == null ? new DataFlavor[] { } : new DataFlavor[] { f };
  }

  public DragDrop(String ...mimetype) {
    DataFlavor[] f = new DataFlavor[mimetype.length];
    try {
      for (int i = 0; i < mimetype.length; i++)
        f[i] = new DataFlavor(mimetype[i]);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    dataClass = null;
    dataFlavor = f[0];
    dataFlavors = f;
  }

  public interface Support extends Transferable {
    public DragDrop getDragDrop();
    public default Object convertTo(String mimetype) { return null; }

    @Override
    public default Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if(!isDataFlavorSupported(flavor))
        throw new UnsupportedFlavorException(flavor);
      DragDrop dnd = getDragDrop();
      if (dnd.dataClass != null && dnd.dataClass.isInstance(this))
          return this;
      Object obj = convertTo(flavor.getMimeType());
      if (obj == null)
        throw new UnsupportedFlavorException(flavor);
      return obj;
    }

    @Override
    public default DataFlavor[] getTransferDataFlavors() {
      return getDragDrop().dataFlavors;
    }

    @Override
    public default boolean isDataFlavorSupported(DataFlavor flavor) {
      if (flavor == null)
        return false;
      DragDrop dnd = getDragDrop();
      for (DataFlavor supported: dnd.dataFlavors)
        if (flavor.equals(supported))
            return true;
      return false;
    }
  }

  // This code makes it easier to add support for a component acting as a drag
  // source, for components that don't natively provide that support.
  public static final int MOVE = DnDConstants.ACTION_MOVE;
  public static final int COPY = DnDConstants.ACTION_COPY;
  public static final int LINK = DnDConstants.ACTION_LINK;

  public static <T extends JComponent & Transferable> void enable(T t, int actions) {
    t.addHierarchyListener(new HierarchyListener() {
      Handler<T> h;
      DragGestureRecognizer r;
      public void hierarchyChanged(HierarchyEvent e) {
        if (h == null && t.isShowing()) {
          h = new Handler<T>(t);
          r = source.createDefaultDragGestureRecognizer(t, actions, h);
        } else if (h != null && !t.isShowing()) {
          r.removeDragGestureListener(h);
          h = null;
          r = null;
        }
      }
    });
  }

  public static final DragSource source;
 
  public static class Handler<T extends Transferable> extends DragSourceAdapter implements DragGestureListener {
    T t;

    public Handler(T t) { this.t = t; }

    @Override
    public void dragGestureRecognized(DragGestureEvent e) {
      Cursor cursor = null; // move cursor?
      if (t instanceof Ghost)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      e.getDragSource().startDrag(e, cursor, t, this);
    }

    // @Override
    // public void dragDropEnd(DragSourceDropEvent e) { }
  }

  private static final DragImageAnimator animator;

  static {
    animator = new DragImageAnimator();
    source = DragSource.getDefaultDragSource();
    source.addDragSourceListener(animator);
    source.addDragSourceMotionListener(animator);
  }

  // If a Transferable component implements Ghost, it will be painted near the
  // mouse in an overlay as it is being dragged. We need to do our own animation
  // here because DragSource does not support drag Image on Linux (or mac or
  // windows?).
  public interface Ghost {
    // For a JComponent, these defaults work fine, or override them to customize
    // the drag image.
    default public void paintComponent(Graphics g) { }
    default public void paintDragImage(JComponent dest, Graphics g, Dimension dim) {
      paintComponent(g);
    }
    default public Dimension getSize() { return new Dimension(1, 1); }
  }


  private static class DragImageOverlay extends JFrame {
    Ghost ghost;
    private DragImageOverlay() {
      setUndecorated(true);
      setBackground(new Color(1.0f, 1.0f, 1.0f, 0.0f));
      setContentPane(new JComponent() {
        @Override
        public void paintComponent(Graphics g) {
          if (ghost != null)
            ghost.paintDragImage(this, g, getSize());
        }
      });
      setAlwaysOnTop(true);
    }
  }

  private static class DragImageAnimator extends DragSourceAdapter
    implements DragSourceMotionListener {
    DragImageOverlay overlay = new DragImageOverlay();
    @Override
    public void dragEnter(DragSourceDragEvent e) {
      Object t = e.getDragSourceContext().getTransferable();
      if (t instanceof Ghost) {
        Ghost g = (Ghost)t;
        overlay.ghost = g;
        Dimension d = g.getSize();
        overlay.setBounds(0, 0, d.width, d.height);
        overlay.setLocation(e.getX() + 10, e.getY() + 10); 
        overlay.setVisible(true);
      } else {
        overlay.setVisible(false);
      }
    }
    @Override
    public void dragMouseMoved(DragSourceDragEvent e) {
      overlay.setLocation(e.getX() + 10, e.getY() + 10); 
    }
    @Override
    public void dragDropEnd(DragSourceDropEvent e) {
      overlay.setVisible(false);
      overlay.ghost = null;
    }
  }

}
