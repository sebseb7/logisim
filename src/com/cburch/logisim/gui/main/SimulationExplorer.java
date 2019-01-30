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

package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

class SimulationExplorer extends JPanel
  implements ProjectListener, MouseListener {
  private static final long serialVersionUID = 1L;
  private Project project;
  private SimulationTreeModel model;
  private JTree tree;

  SimulationExplorer(Project proj, MenuListener menu) {
    super(new BorderLayout());
    this.project = proj;

    SimulationToolbarModel toolbarModel = new SimulationToolbarModel(proj, menu);
    Toolbar toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    model = new SimulationTreeModel(proj.getRootCircuitStates());
    model.setCurrentView(project.getCircuitState());
    tree = new JTree(model);
    tree.setCellRenderer(new SimulationTreeRenderer());
    tree.addMouseListener(this);
    tree.setToggleClickCount(3);
    add(new JScrollPane(tree), BorderLayout.CENTER);
    proj.addProjectListener(this);

    ToolTipManager.sharedInstance().registerComponent(tree);

    Object root = model.getRoot();
    for (int i = 0; i < model.getChildCount(root); i++)
      expand((SimulationTreeNode)model.getChild(root, i));
  }

  private void expand(SimulationTreeNode node) {
    TreePath path = model.getPath(node);
    if (path == null)
      return;
    int i = tree.getRowForPath(path);
    if (i < 0)
      return;
    tree.expandRow(i);
  }

  public CircuitState getCircuitStateForLocation(int x, int y) {
    TreePath path = tree.getPathForLocation(x, y);
    Object last = path == null ? null : path.getLastPathComponent();
    if (last instanceof SimulationTreeCircuitNode)
      return ((SimulationTreeCircuitNode)last).getCircuitState();
    return null;
  }


  private void checkForPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
      SwingUtilities.convertMouseEvent(this, e, tree);
      CircuitState cs = getCircuitStateForLocation(e.getX(), e.getY());
      if (cs != null) {
        JPopupMenu menu = Popups.forCircuitState(project, cs);
        if (menu != null)
          menu.show(tree, e.getX(), e.getY());
      }
    }
  }

  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() == 2) {
      CircuitState cs = getCircuitStateForLocation(e.getX(), e.getY());
      if (cs != null)
        project.setCircuitState(cs);
    }
  }

  public void mouseEntered(MouseEvent e) { }

  public void mouseExited(MouseEvent e) { }

  public void mousePressed(MouseEvent e) {
    requestFocus();
    checkForPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    checkForPopup(e);
  }

  public void projectChanged(ProjectEvent event) {
    int action = event.getAction();
    if (action == ProjectEvent.ACTION_SET_STATE) {
      model.setCurrentView(project.getCircuitState());
      TreePath path = model.mapToPath(project.getCircuitState());
      if (path != null)
        tree.scrollPathToVisible(path);
    } else if (action == ProjectEvent.ACTION_CLEAR_STATES) {
      model.clear();
    } else if (action == ProjectEvent.ACTION_ADD_STATE) {
      SimulationTreeNode node = model.addState((CircuitState)event.getData());
      expand(node);
    } else if (action == ProjectEvent.ACTION_DELETE_STATE) {
      model.removeState((CircuitState)event.getData());
    }
  }
}
