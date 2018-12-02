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

package com.cburch.logisim.gui.log;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.DropMode;;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.instance.StdAttr;

// This is more like a JTree, but wedged into a JTable because it looks more
// reasonable sitting next to the SelectionList JTable .
class ComponentSelector extends JTable {

  static final Comparator<Component> compareComponents = new Comparator<Component>() {
    @Override
    public int compare(Component a, Component b) {
      String aName = a.getFactory().getDisplayName();
      String bName = b.getFactory().getDisplayName();
      int ret = aName.compareToIgnoreCase(bName);
      if (ret != 0)
        return ret;
      return a.getLocation().toString().compareTo(
          b.getLocation().toString());
    }
  };

  static final Comparator<Object> compareNames = new Comparator<Object>() {
    @Override
    public int compare(Object a, Object b) {
      return a.toString().compareToIgnoreCase(b.toString());
    }
  };

  static class TableTreeModel extends AbstractTableModel {
    TreeNode<CircuitNode> root;
    ArrayList<TreeNode<?>> rows = new ArrayList<>();

    TableTreeModel() { }

    @Override
    public int getRowCount() { return rows.size(); }
    @Override
    public Object getValueAt(int row, int column) { return rows.get(row); }
    @Override
    public int getColumnCount() { return 1; }
    @Override
    public boolean isCellEditable(int row, int column) { return false; }
    @Override
    public Class<?> getColumnClass(int columnIndex) { return TreeNode.class; }
    @Override
    public String getColumnName(int column) { return ""; }

    void toggleExpand(int row) {
      if (row < 0 || row >= rows.size())
        return;
      TreeNode<?> o = rows.get(row);
      int n = o.children.size();
      if (n == 0)
        return;
      if (o.expanded) {
        for (int i = 0; i < n; i++)
          removeAll(row+1);
        o.expanded = false;
      } else {
        for (int i = n-1; i >= 0; i--)
          insertAll(row+1, o.children.get(i));
        o.expanded = true;
      }
      super.fireTableDataChanged(); // overkill, but works
    }

    void removeAll(int row) {
      TreeNode<?> item = rows.remove(row);
      if (item.expanded) {
        int n = item.children.size();
        for (int i = 0; i < n; i++)
          removeAll(row);
      }
    }

    void insertAll(int row, TreeNode<?> item) {
      rows.add(row, item);
      if (item.expanded) {
        int n = item.children.size();
        for (int i = n-1; i >= 0; i--)
          insertAll(row+1, item.children.get(i));
      }
    }

    public void fireTableDataChanged() {
      setRoot(root);
    }

    void setRoot(TreeNode<CircuitNode> r) {
      root = r;
      rows.clear();
      int n = root == null ? 0 : root.children.size();
      for (int i = n-1; i >= 0; i--)
        insertAll(0, root.children.get(i));
      super.fireTableDataChanged();
    }

  }

  // TreeNode
  //   ComponentNode (e.g. a Pin or Button, or an expandable Ram placeholder)
  //   CircuitNode
  //   OptionNode (e.g. one location in a Ram component)
  
  private static class TreeNode<P extends TreeNode<?>> {
    P parent;
    int depth;
    boolean expanded;
    ArrayList<TreeNode<?>> children = new ArrayList<>();

    TreeNode(P p) {
      parent = p;
      depth = (parent == null ? 0 : parent.depth + 1);
    }

    void addChild(TreeNode<?> child) {
      children.add(child);
    }

  }

  private static class ComponentNode extends TreeNode<CircuitNode> {

    Component comp;

    public ComponentNode(CircuitNode p, Component c) {
      super(p);
      comp = c;

      Loggable log = (Loggable)comp.getFeature(Loggable.class);
      if (log == null)
        return;
      Object[] opts = log.getLogOptions(parent.circuitState);
      if (opts == null)
        return;
      for (Object opt : opts)
        addChild(new OptionNode(this, opt));
    }

    @Override
    public String toString() {
      Loggable log = (Loggable)comp.getFeature(Loggable.class);
      if (log != null) {
        String ret = log.getLogName(null);
        if (ret != null && !ret.equals(""))
          return ret;
      }
      return comp.getFactory().getDisplayName() + " " + comp.getLocation();
    }
  }

  private class CircuitNode extends TreeNode<CircuitNode>
    implements CircuitListener {

    CircuitState circuitState;
    Component comp;

    public CircuitNode(CircuitNode p, CircuitState cs, Component c) {
      super(p);
      circuitState = cs;
      comp = c;
      cs.getCircuit().addCircuitListener(this);
      computeChildren();
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();
      if (action == CircuitEvent.ACTION_SET_NAME)
        tableModel.fireTableDataChanged(); // overkill, but works
      else if (computeChildren())
        tableModel.fireTableDataChanged(); // overkill, but works
      else if (action == CircuitEvent.ACTION_INVALIDATE)
        tableModel.fireTableDataChanged(); // overkill, but works
    }

    private ComponentNode findChildFor(Component c) {
      for (TreeNode<?> o : children) {
        if (o instanceof ComponentNode) {
          ComponentNode child = (ComponentNode) o;
          if (child.comp == c) 
            return child;
        }
      }
      return null;
    }

    private CircuitNode findChildFor(CircuitState cs) {
        for (TreeNode<?> o : children) {
          if (o instanceof CircuitNode) {
            CircuitNode child = (CircuitNode)o;
            if (child.circuitState == cs)
              return child;
          }
        }
        return null;
    }

    private boolean computeChildren() { // returns true if changed
      ArrayList<TreeNode<?>> newChildren = new ArrayList<>();
      ArrayList<Component> subcircs = new ArrayList<>();
      boolean changed = false;
      for (Component c : circuitState.getCircuit().getNonWires()) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          subcircs.add(c);
          continue;
        }
        if (c.getFeature(Loggable.class) == null)
          continue;
        ComponentNode toAdd = findChildFor(c);
        if (toAdd == null) {
          toAdd = new ComponentNode(this, c);
          changed = true;
        }
        newChildren.add(toAdd);
      }
      Collections.sort(newChildren, compareNames);
      Collections.sort(subcircs, compareComponents);
      for (Component c : subcircs) {
        SubcircuitFactory factory = (SubcircuitFactory) c.getFactory();
        CircuitState cs = factory.getSubstate(circuitState, c);
        CircuitNode toAdd = findChildFor(cs);
        if (toAdd == null) {
          changed = true;
          toAdd = new CircuitNode(this, cs, c);
        }
        newChildren.add(toAdd);
      }

      changed = changed || !children.equals(newChildren);
      if (changed)
        children = newChildren;
      return changed;
    }

    @Override
    public String toString() {
      if (comp != null) {
        String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
        if (label != null && !label.equals(""))
          return label;
      }
      String ret = circuitState.getCircuit().getName();
      if (comp != null)
        ret += comp.getLocation();
      return ret;
    }

  }

  // OptionNode represents some value in the internal state of a component, e.g.
  // the value inside a shift register stage, or the value at a specific RAM
  // location. 
  // TODO: Those are the only two components that have been outfitted for this,
  // apparently. 
  // FIXME: And for RAM, the current UI is unworkable unless there are only a
  // very few addresses.
  private static class OptionNode extends TreeNode<ComponentNode> {
    private Object option;

    public OptionNode(ComponentNode p, Object o) {
      super(p);
      option = o;
    }

    @Override
    public String toString() {
      return option.toString();
    }
  }

  private static class TreeNodeRenderer extends DefaultTableCellRenderer implements Icon {

    private TreeNode<?> node;

    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof CircuitNode)
        isSelected = false;
      java.awt.Component ret = super.getTableCellRendererComponent(table,
          value, isSelected, hasFocus, row, column);
      if (ret instanceof JLabel && value instanceof TreeNode) {
        node = (TreeNode)value;
        ((JLabel)ret).setIcon(this);
      }
      return ret;
    }

    @Override
    public int getIconHeight() { return 20; }

    @Override
    public int getIconWidth() {
      return 10 * (node.depth-1) + (needsTriangle() ? 40 : 20);
    }

    boolean needsTriangle() {
      return (node instanceof CircuitNode)
          || (node instanceof ComponentNode && node.children.size() > 0);
    }

    @Override
    public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
      g.setColor(Color.GRAY);
      for (int i = 1; i < node.depth; i++) {
        g.drawLine(x+5, 0, x+5, 20);
        x += 10;
      }

      if (node instanceof OptionNode) {
        // todo
        g.setColor(Color.MAGENTA);
        g.fillRect(x+3, x+3, 15, 15);
        return;
      }
      Component comp;
      if (node instanceof ComponentNode)
        comp = ((ComponentNode)node).comp;
      else if (node instanceof CircuitNode)
        comp = ((CircuitNode)node).comp;
      else
        return; // null node?

      Graphics g2 = g.create();
      int xi = needsTriangle() ? x + 10 : x;
      ComponentDrawContext context = new ComponentDrawContext(c, null, null, g, g2);
      comp.getFactory().paintIcon(context, xi, y, comp.getAttributeSet());
      g2.dispose();

      if (!needsTriangle())
        return;

      int[] xp, yp;
      if (node.expanded) {
        xp = new int[] { x + 0, x + 10, x + 5 };
        yp = new int[] { y + 9, y + 9, y + 14 };
      } else {
        xp = new int[] { x + 3, x + 3, x + 8 };
        yp = new int[] { y + 5, y + 15, y + 10 };
      }
      g.setColor(new Color(51, 102, 255));
      g.fillPolygon(xp, yp, 3);
      g.setColor(Color.BLACK);
      g.drawPolygon(xp, yp, 3);
    }
  }

  private Model logModel;
  private TableTreeModel tableModel = new TableTreeModel();

  public ComponentSelector(Model m) {
    logModel = m;
    setLogModel(logModel);
    setModel(tableModel);
    setDefaultRenderer(TreeNode.class, new TreeNodeRenderer());
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    getTableHeader().setUI(null);
    setRowHeight(24);
    // setAutoResizeMode(AUTO_RESIZE_OFF);
    setShowGrid(false);
    setFillsViewportHeight(true);
    setDragEnabled(true);
    setDropMode(DropMode.ON_OR_INSERT); // ?
    setTransferHandler(new ComponentTransferHandler());

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());
        if (row < 0 || col < 0)
          return;
        tableModel.toggleExpand(row);
      }
    });

  }

  public SignalInfo.List getSelectedItems() {
    SignalInfo.List items = new SignalInfo.List();
    int[] sel = getSelectedRows();
    for (int i : sel) {
      TreeNode<?> node = tableModel.rows.get(i);
      ComponentNode n = null;
      Object opt = null;
      if (node instanceof OptionNode) {
        n = ((OptionNode)node).parent;
        opt = ((OptionNode)node).option;
      } else if (node instanceof ComponentNode) {
        n = (ComponentNode)node;
        if (n.children.size() > 0)
          continue;
      } else {
        continue;
      }
      int count = 0;
      for (CircuitNode cur = n.parent; cur != null; cur = cur.parent)
        count++;
      Component[] nPath = new Component[count];
      nPath[nPath.length-1] = n.comp;
      CircuitNode cur = n.parent;
      for (int j = nPath.length - 2; j >= 0; j--) {
        nPath[j] = cur.comp;
        cur = cur.parent;
      }
      items.add(new SignalInfo(logModel, nPath, opt));
    }
    return (items.size() > 0 ? items : null);
  }

  public void localeChanged() {
    repaint();
  }

  public void setLogModel(Model m) {
    logModel = m;

    if (logModel == null) {
      tableModel.setRoot(null);
      return;
    }
    CircuitState cs = logModel.getCircuitState();
    if (cs == null) {
      tableModel.setRoot(null);
      return;
    }
    CircuitNode root = new CircuitNode(null, cs, null);
    tableModel.setRoot(root);
  }

  class ComponentTransferHandler extends TransferHandler {
    boolean sending;

    public ComponentTransferHandler() { }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return !sending && support.isDataFlavorSupported(SignalInfo.List.dataFlavor);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
      sending = true;
      ComponentSelector tree = (ComponentSelector)c;
      SignalInfo.List items = tree.getSelectedItems();
      return items == null || items.isEmpty() ? null : items;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
      sending = false;
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) { 
      sending = false;
      return false;
    }

  }

}
