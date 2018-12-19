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

package com.cburch.logisim.gui.generic;

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.hdl.VhdlEntity;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class ProjectExplorer extends JTree implements LocaleListener {

  private class DeleteAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public void actionPerformed(ActionEvent event) {
      TreePath path = getSelectionPath();
      if (listener != null && path != null && path.getPathCount() == 2) {
        listener.deleteRequested(new Event(path));
      }

      ProjectExplorer.this.requestFocus();
    }
  }

  private class MyCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    Font plainFont, boldFont;

    @Override
    public java.awt.Component getTreeCellRendererComponent(JTree tree,
        Object value, boolean selected, boolean expanded, boolean leaf,
        int row, boolean hasFocus) {
      java.awt.Component ret;
      ret = super.getTreeCellRendererComponent(tree, value, selected,
          expanded, leaf, row, hasFocus);
      if (plainFont == null) {
        plainFont = ret.getFont();
        boldFont = new Font(plainFont.getFontName(), Font.BOLD, plainFont.getSize());
      }
      ret.setFont(plainFont);
      if (ret instanceof JComponent) {
        JComponent comp = (JComponent) ret;
        comp.setToolTipText(null);

      }
      if (value instanceof ProjectExplorerToolNode) {
        ProjectExplorerToolNode toolNode = (ProjectExplorerToolNode) value;
        Tool tool = toolNode.getValue();
        if (ret instanceof JLabel) {
          JLabel label = (JLabel)ret;
          boolean viewed = false;
          if (tool instanceof AddTool && proj != null && proj.getFrame() != null) {
            Circuit circ = null;
            VhdlContent vhdl = null;
            ComponentFactory fact = ((AddTool) tool).getFactory(false);
            if (fact instanceof SubcircuitFactory) {
              circ = ((SubcircuitFactory) fact).getSubcircuit();
            } else if (fact instanceof VhdlEntity) {
              vhdl = ((VhdlEntity) fact).getContent();
            }
            if (proj.getFrame().getHdlEditorView() == null)
              viewed = (circ != null && circ == proj.getCurrentCircuit());
            else
              viewed = (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
          }
          label.setFont(viewed ? boldFont : plainFont);
          label.setText(tool.getDisplayName());
          label.setIcon(new ToolIcon(tool));
          label.setToolTipText(tool.getDescription());
        }
      } else if (value instanceof ProjectExplorerLibraryNode) {
        ProjectExplorerLibraryNode libNode = (ProjectExplorerLibraryNode) value;
        Library lib = libNode.getValue();

        if (ret instanceof JLabel) {
          String text = lib.getDisplayName();
          if (lib.isDirty())
            text += DIRTY_MARKER;

          ((JLabel) ret).setText(text);
        }
      }
      return ret;
    }
  }
  private class MyListener implements MouseListener, TreeSelectionListener,
          ProjectListener, PropertyChangeListener {
    private void checkForPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null && listener != null) {
          JPopupMenu menu = listener.menuRequested(new Event(path));
          if (menu != null) {
            menu.show(ProjectExplorer.this, e.getX(), e.getY());
          }
        }
      }
    }

    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() == 2) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null && listener != null) {
          listener.doubleClicked(new Event(path));
        }
      } else {
        // TreePath path = getPathForLocation(e.getX(), e.getY());
        // if (listener != null) {
        //         listener.selectionChanged(new Event(path));
        // }
      }
    }

    //
    // MouseListener methods
    //
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
      ProjectExplorer.this.requestFocus();
      checkForPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      checkForPopup(e);
    }

    void changedNode(Object o) {
      ProjectExplorerModel model = (ProjectExplorerModel) getModel();
      if (model != null && o instanceof Tool) {
        ProjectExplorerModel.Node<Tool> node = model.findTool((Tool)o);
        if (node != null)
          node.fireNodeChanged();
      }
    }

    //
    // project/library file/circuit listener methods
    //
    public void projectChanged(ProjectEvent event) {
      int act = event.getAction();
      if (act == ProjectEvent.ACTION_SET_CURRENT || act == ProjectEvent.ACTION_SET_TOOL) {
        changedNode(event.getOldData());
        changedNode(event.getData());
      }
    }

    //
    // PropertyChangeListener methods
    //
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)) {
        ProjectExplorer.this.repaint();
      }
    }

    //
    // TreeSelectionListener methods
    //
    public void valueChanged(TreeSelectionEvent e) {
      TreePath path = e.getNewLeadSelectionPath();
      if (listener != null) {
        listener.selectionChanged(new Event(path));
      }
    }
  }

  private class ToolIcon implements Icon {

    Tool tool;
    Circuit circ = null;
    VhdlContent vhdl = null;

    ToolIcon(Tool tool) {
      this.tool = tool;
      if (tool instanceof AddTool) {
        ComponentFactory fact = ((AddTool) tool).getFactory(false);
        if (fact instanceof SubcircuitFactory) {
          circ = ((SubcircuitFactory) fact).getSubcircuit();
        } else if (fact instanceof VhdlEntity) {
          vhdl = ((VhdlEntity) fact).getContent();
        }
      }
    }

    public int getIconHeight() { return 20; }
    public int getIconWidth() { return 20; }

    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
      boolean viewed;
      if (proj.getFrame().getHdlEditorView() == null)
        viewed = (circ != null && circ == proj.getCurrentCircuit());
      else
        viewed = (vhdl != null && vhdl == proj.getFrame().getHdlEditorView());
      boolean haloed = !viewed
          && (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean());

      // draw halo if appropriate
      if (haloed) {
        Shape s = g.getClip();
        g.clipRect(x, y, 20, 20);
        g.setColor(Canvas.HALO_COLOR);
        g.fillOval(x-2, y-2, 23, 23);
        g.setColor(Color.BLACK);
        g.setClip(s);
      }

      // draw tool icon
      Graphics gIcon = g.create();
      ComponentDrawContext context = new ComponentDrawContext(
          ProjectExplorer.this, null, null, g, gIcon);
      tool.paintIcon(context, x, y);
      gIcon.dispose();

      // draw magnifying glass if appropriate
      if (viewed) {
        int tx = x + 13;
        int ty = y + 13;
        int[] xp = { tx - 1, x + 18, x + 20, tx + 1 };
        int[] yp = { ty + 1, y + 20, y + 18, ty - 1 };
        g.setColor(MAGNIFYING_INTERIOR);
        g.fillOval(x + 5, y + 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawOval(x + 5, y + 5, 10, 10);
        g.fillPolygon(xp, yp, xp.length);
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private static final String DIRTY_MARKER = "*";

  public static final Color MAGNIFYING_INTERIOR = new Color(200, 200, 255, 64);

  private Project proj;
  private MyListener myListener = new MyListener();
  private MyCellRenderer renderer = new MyCellRenderer();
  private DeleteAction deleteAction = new DeleteAction();
  private Listener listener = null;
  private Tool haloedTool = null;

  public ProjectExplorer(Project proj) {
    this(proj, false);
  }

  public ProjectExplorer(Project proj, boolean showAll) {
    super();
    this.proj = proj;

    setModel(new ProjectExplorerModel(proj, showAll));
    setRootVisible(!showAll); // hide the fake root when showing all
    setShowsRootHandles(!showAll); // but show handles instead
    if (showAll) {
      for (Enumeration<?> en =
          ((ProjectExplorerModel.Node<?>)getModel().getRoot()).children(); en.hasMoreElements();) {
        Object n = en.nextElement();
        if (n instanceof ProjectExplorerModel.Node<?>)
          expandPath(new TreePath(((ProjectExplorerModel.Node<?>)n).getPath()));
      }
    }

    addMouseListener(myListener);
    ToolTipManager.sharedInstance().registerComponent(this);

    getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    setCellRenderer(renderer);
    addTreeSelectionListener(myListener);
    setDragEnabled(true);
    setDropMode(DropMode.INSERT);
    TransferHandler ccp = new ProjectTransferHandler();
    setTransferHandler(ccp);

    InputMap imap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), deleteAction);
    ActionMap amap = getActionMap();
    amap.put(deleteAction, deleteAction);

    proj.addProjectListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    LocaleManager.addLocaleListener(this);
  }

  public Tool getSelectedTool() {
    TreePath path = getSelectionPath();
    if (path == null)
      return null;
    Object last = path.getLastPathComponent();

    if (last instanceof ProjectExplorerToolNode) {
      return ((ProjectExplorerToolNode) last).getValue();
    } else {
      return null;
    }
  }

  public void updateStructure() {
    ProjectExplorerModel model = (ProjectExplorerModel) getModel();
    model.updateStructure();
  }

  public void localeChanged() {
    // repaint() would work, except that names that get longer will be
    // abbreviated with an ellipsis, even when they fit into the window.
    final ProjectExplorerModel model = (ProjectExplorerModel) getModel();
    model.fireStructureChanged();
  }

  public void setHaloedTool(Tool t) {
    haloedTool = t;
  }

  public void setListener(Listener value) {
    listener = value;
  }

  private class ProjectTransferHandler extends TransferHandler {
    Tool removing = null;

    @Override
    public int getSourceActions(JComponent comp) {
      return COPY | MOVE | LINK;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      // Only for top level tools, path len = 0 or 1?
      // Or only for subcircuit and vhdl circuit tools?
      // Or also other tools, so we can send to toolbar?
      removing = null;
      Tool tool = getSelectedTool();
      // temporary hack
      if (tool instanceof AddTool) {
        System.out.println("xfer explorer AddTool item: " + tool);
        removing = tool;
        return (AddTool)removing;
      } else if (tool instanceof Transferable) {
        System.out.println("not add tool, but still ok to send: " + tool);
        return (Transferable)tool;
      } else {
        System.out.println("nvm");
        return null;
      }
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
//       if (removing == null)
//         return;
//       ArrayList<SignalInfo> items = new ArrayList<>();
//       for (Signal s : removing)
//         items.add(s.info);
//       removing = null;
//       model.remove(items);
    }

    // todo: check for isBuiltin, cross-project importing
    // see: gui/main/LayoutToolbarModel
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      if (!support.isDataFlavorSupported(AddTool.dnd.dataFlavor)) {
        // System.out.println("cant import non-AddTool");
        return false;
      }
      try {
        if (support.isDrop()) {
          JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
          TreePath newPath = dl.getPath();
          if (newPath.getPathCount() != 1)
            return false;
        }
        // System.out.println("can import addtool?");
        AddTool incoming = (AddTool)support.getTransferable().getTransferData(AddTool.dnd.dataFlavor);
        if (incoming.isBuiltin())
          return false; // can't import a builtin tool
        // System.out.println("incoming = " + incoming + " removing = " + removing);
        if (incoming == removing)
          return true;
        ComponentFactory cf = incoming.getFactory(false);
        if (cf == null)
          return false;
        // System.out.println("cf = " + cf + " : " + cf.getClass());
        if (!(cf instanceof SubcircuitFactory))
          return false;
        // System.out.println("yup");
        return true;
      } catch (Throwable e) {
        e.printStackTrace();
        return false;
      }
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      System.out.println("importing...");
      Tool outgoing = removing;
      removing = null;
      try {
        AddTool incoming = (AddTool)support.getTransferable().getTransferData(AddTool.dnd.dataFlavor);

        ProjectExplorerLibraryNode libNode = (ProjectExplorerLibraryNode)getModel().getRoot();
        Library lib = libNode.getValue();
        int newIdx = lib.getTools().size();

        if (support.isDrop()) {
          try {
            JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
            TreePath newPath = dl.getPath();
            if (newPath.getPathCount() != 1)
              return false;
            Object o = newPath.getPathComponent(0);
            System.out.println("o = " + o);
            if (o != getModel().getRoot())
              return false;
            newIdx = dl.getChildIndex();
            System.out.println("drop at top position " + newIdx);
            // newIdx = Math.min(newIdx, dl.getRow());
          } catch (ClassCastException e) {
            return false;
          }
        }
        System.out.println("import " + incoming + " at " + newIdx + " vs " + outgoing);
        int oldIdx = lib.getTools().indexOf(incoming);
        if (oldIdx >= 0 && incoming == outgoing) {
          System.out.printf("moving from %d to %d\n", oldIdx, newIdx);
          if (listener != null) {
            TreePath path = new TreePath(
                new Object[] { libNode, libNode.getChildAt(oldIdx) });
            listener.moveRequested(new Event(path), incoming, newIdx);
            return true;
          }
        } else {
          System.out.printf("? moving from %d to %d\n", oldIdx, newIdx);
        }
        return false;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }

  public static interface Listener {
    public void deleteRequested(Event event);
    public void doubleClicked(Event event);
    public JPopupMenu menuRequested(Event event);
    public void moveRequested(Event event, AddTool dragged, int newIdx);
    public void selectionChanged(Event event);
  }

  public static class Event {
    private TreePath path;
    public Event(TreePath p) { path = p; }
    public TreePath getTreePath() { return path; }
    public Object getTarget() {
      return path == null ? null : path.getLastPathComponent();
    }
  }

}
