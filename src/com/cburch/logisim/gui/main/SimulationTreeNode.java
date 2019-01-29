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

import java.util.Enumeration;
import java.util.Collections;
import java.util.ArrayList;

import javax.swing.tree.TreeNode;

import com.cburch.logisim.comp.ComponentFactory;

public class SimulationTreeNode implements TreeNode {

  protected SimulationTreeModel model;
  protected SimulationTreeNode parent;
  protected ArrayList<TreeNode> children;

  public SimulationTreeNode(SimulationTreeModel model, SimulationTreeNode parent) {
    this.model = model;
    this.parent = parent;
    this.children = new ArrayList<TreeNode>();
  }

  public Enumeration<TreeNode> children() {
    return Collections.enumeration(children);
  }

  public boolean getAllowsChildren() {
    return true;
  }

  public TreeNode getChildAt(int index) {
    return children.get(index);
  }

  public int getChildCount() {
    return children.size();
  }

  public ComponentFactory getComponentFactory() {
    return null;
  }

  public int getIndex(TreeNode node) {
    return children.indexOf(node);
  }

  public TreeNode getParent() {
    return parent;
  }

  public boolean isCurrentView(SimulationTreeModel model) {
    return false;
  }

  public boolean isLeaf() {
    return false;
  }

  // Subclasses should call this when this node's appearance (icon, label,
  // etc.) has changed. It will fire the appropriate TreeModelEvent.
  public void fireAppearanceChanged() {
    if (parent == null) {
      model.fire(model.getPath(this), new int[0], null,
          (l,e) -> l.treeNodesChanged(e));
    } else {
      int[] indices = new int[] { parent.getIndex(this) };
      SimulationTreeNode[] nodes = new SimulationTreeNode[] { this };
      model.fire(model.getPath(parent), indices, nodes,
          (l,e) -> l.treeNodesChanged(e));
    }
  }

  public void fireStructureChanged() {
    model.fire(model.getPath(this), new int[0], null,
        (l,e) -> l.treeStructureChanged(e));
  }
}
