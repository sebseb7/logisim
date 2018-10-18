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
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.SwingUtilities;

import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.Tool;

class ProjectExplorerModel extends DefaultTreeModel implements ProjectListener {

  static abstract class Node<T> extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;
    ProjectExplorerModel model;
    int oldIndex;
    int newIndex;

    Node(ProjectExplorerModel model, T userObject) {
      super(userObject);
      this.model = model;
    }

    abstract Node<T> create(T userObject);

    abstract void decommission();

    public void fireNodeChanged() {
      Node<?> parent = (Node<?>) this.getParent();

      if (parent == null) {
        try {
          model.fireTreeNodesChanged(this, null, null, null);
        } catch (IllegalArgumentException e) {
          // On mac, using null to specify root, as per the
          // official docs, causes a crash. Seen on:
          //   java version "1.7.0_79"
          //   Java(TM) SE Runtime Environment (build 1.7.0_79-b15)
          //   Java HotSpot(TM) 64-Bit Server VM (build 24.79-b02, mixed mode)
          // Let's fire it for each child instead, I suppose.
          int n = getChildCount();
          int[] indices = new int[n];
          Node<?>[] children = new Node<?>[n];
          for (int i = 0; i < n; i++) {
            indices[i] = i;
            children[i] = (Node<?>)getChildAt(i);
          }
          model.fireTreeNodesChanged(model, this.getPath(), indices, children);
        }
      } else {
        int[] indices = new int[] { parent.getIndex(this) };
        Object[] items = new Object[] { this.getUserObject() };
        model.fireTreeNodesChanged(this, parent.getPath(), indices,
            items);
      }
    }

    void fireNodesChanged(int[] indices, Node<?>[] children) {
      model.fireTreeNodesChanged(model, this.getPath(), indices, children);
    }

    void fireNodesInserted(int[] indices, Node<?>[] children) {
      model.fireTreeNodesInserted(model, this.getPath(), indices,
          children);
    }

    void fireNodesRemoved(int[] indices, Node<?>[] children) {
      model.fireTreeNodesRemoved(model, this.getPath(), indices, children);
    }

    void fireStructureChanged() {
      model.fireStructureChanged();
    }

    ProjectExplorerModel getModel() {
      return model;
    }

    public T getValue() {
      @SuppressWarnings("unchecked")
      T val = (T) getUserObject();
      return val;
    }

  }

  private static final long serialVersionUID = 1L;

  private Project proj;

  ProjectExplorerModel(Project proj) {
    super(null);
    this.proj = proj;
    setRoot(new ProjectExplorerLibraryNode(this, proj.getLogisimFile()));
    proj.addProjectListener(this);
  }

  Node<Tool> findTool(Tool tool) {
    final Node<?> root = (Node<?>) getRoot();
    if (root == null || tool == null)
      return null;
    Enumeration<DefaultMutableTreeNode> en = root.depthFirstEnumeration();
    while (en.hasMoreElements()) {
      Node<?> node = (Node<?>)en.nextElement();
      if (node.getValue() == tool)
        return (Node<Tool>)node;
    }
    return null;
  }

  void fireStructureChanged() {
    final ProjectExplorerModel model = this;
    final Node<?> root = (Node<?>) getRoot();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (root != null) {
          model.fireTreeNodesChanged(model, root.getUserObjectPath(), null, null);
          model.fireTreeStructureChanged(model, root.getUserObjectPath(), null, null);
        } else {
          model.fireTreeNodesChanged(model, null, null, null);
          model.fireTreeStructureChanged(model, null, null, null);
        }
      }
    });
  }

  // ProjectListener methods
  public void projectChanged(ProjectEvent event) {
    int act = event.getAction();
    if (act == ProjectEvent.ACTION_SET_FILE) {
      setLogisimFile(proj.getLogisimFile());
      fireStructureChanged();
    }
  }

  public void updateStructure() {
    fireStructureChanged();
  }

  private void setLogisimFile(LogisimFile file) {
    Node<?> oldRoot = (Node<?>) getRoot();
    oldRoot.decommission();

    if (file == null) {
      setRoot(null);
    } else {
      setRoot(new ProjectExplorerLibraryNode(this, file));
    }

    fireStructureChanged();
  }

  public void setProject(Project value) {
    Project old = proj;

    if (old != null) {
      old.removeProjectListener(this);
    }

    setLogisimFile(null);
    proj = value;

    if (value != null) {
      value.addProjectListener(this);
      setLogisimFile(value.getLogisimFile());
    }
    fireStructureChanged();
  }

}
