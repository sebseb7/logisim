/* ProjectExplorerFalseRootNode
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.std.base.Base;

public class ProjectExplorerLibraryNode
  extends ProjectExplorerModel.Node<Library> implements LibraryListener {

  private static final long serialVersionUID = 1L;
  private LogisimFile file;

  ProjectExplorerLibraryNode(ProjectExplorerModel model, Library lib, ProjectExplorerModel.Node<?> parent) {
    super(model, lib, parent);
    if (lib instanceof LogisimFile) {
      file = (LogisimFile) lib;
      file.addLibraryListener(this);
    }
    buildChildren();
  }

  private boolean ancestorContainsLibrary(Library lib) {
    ProjectExplorerModel.Node<?> p = (ProjectExplorerModel.Node<?>)getParent();
    // note: root node contains same library as child; don't supress in that case
    while (p != null && !(p instanceof ProjectExplorerRootNode)) {
      Object v = p.getValue();
      if (v instanceof Library && ((Library)v).getLibraries().contains(lib))
        return true;
      p = (ProjectExplorerModel.Node<?>)p.getParent();
    }
    return false;
  }

  private void buildDescendents() {
    buildChildren();
    for (Enumeration<?> en = children(); en.hasMoreElements();) {
      Object child = en.nextElement();
      if (child instanceof ProjectExplorerLibraryNode) {
        @SuppressWarnings("unchecked")
        ProjectExplorerLibraryNode childNode = (ProjectExplorerLibraryNode)child;
        childNode.buildDescendents();
      }
    }
  }

  private void buildChildren() {
    Library lib = getValue();
    if (lib != null) {
      buildChildren(
          ProjectExplorerToolNode.class,
          tool -> new ProjectExplorerToolNode(getModel(), tool, this),
          lib.getTools(), 0);
      buildChildren(
          ProjectExplorerLibraryNode.class,
          subLib -> new ProjectExplorerLibraryNode(getModel(), subLib, this),
          lib.getLibraries(), lib.getTools().size());
    }
  }

  interface NodeFactory<T> {
    ProjectExplorerModel.Node<T> create(T userObject);
  }

  private <T> void buildChildren(Class t, NodeFactory<T> factory,
      List<? extends T> items, int startIndex) {
    // go through previously built children
    Map<T, ProjectExplorerModel.Node<T>> nodeMap = new HashMap<T, ProjectExplorerModel.Node<T>>();
    List<ProjectExplorerModel.Node<T>> nodeList = new ArrayList<ProjectExplorerModel.Node<T>>();
    int oldPos = startIndex;

    for (Enumeration<?> en = children(); en.hasMoreElements(); ) {
      Object child = en.nextElement();
      if (child.getClass() == t) {
        @SuppressWarnings("unchecked")
        ProjectExplorerModel.Node<T> childNode = (ProjectExplorerModel.Node<T>) child;
        nodeMap.put(childNode.getValue(), childNode);
        nodeList.add(childNode);
        childNode.oldIndex = oldPos;
        childNode.newIndex = -1;
        oldPos++;
      }
    }

    int oldCount = oldPos;

    // go through what should be the children
    int actualPos = startIndex;
    int insertionCount = 0;
    oldPos = startIndex;

    for (T item : items) {
      if (item instanceof Base)
        continue;
      if (item instanceof Library && ancestorContainsLibrary((Library)item))
        continue; // hide libraries already shown in an ancestor
      ProjectExplorerModel.Node<T> node = nodeMap.get(item);

      if (node == null) {
        node = factory.create(item);
        node.oldIndex = -1;
        node.newIndex = actualPos;
        nodeList.add(node);
        insertionCount++;
      } else {
        node.newIndex = actualPos;
        oldPos++;
      }
      actualPos++;
    }

    // identify removals first
    if (oldPos != oldCount) {
      int[] delIndex = new int[oldCount - oldPos];
      ProjectExplorerModel.Node<?>[] delNodes = new ProjectExplorerModel.Node<?>[delIndex.length];
      int delPos = 0;

      for (int i = nodeList.size() - 1; i >= 0; i--) {
        ProjectExplorerModel.Node<T> node = nodeList.get(i);

        if (node.newIndex < 0) {
          node.decommission();
          remove(node.oldIndex);
          nodeList.remove(node.oldIndex - startIndex);

          for (ProjectExplorerModel.Node<T> other : nodeList) {
            if (other.oldIndex > node.oldIndex)
              other.oldIndex--;
          }
          delIndex[delPos] = node.oldIndex;
          delNodes[delPos] = node;
          delPos++;
        }
      }
      this.fireNodesRemoved(delIndex, delNodes);
    }

    // identify moved nodes
    int minChange = Integer.MAX_VALUE >> 3;
    int maxChange = Integer.MIN_VALUE >> 3;

    for (ProjectExplorerModel.Node<T> node : nodeList) {
      if (node.newIndex != node.oldIndex && node.oldIndex >= 0) {
        minChange = Math.min(minChange, node.oldIndex);
        maxChange = Math.max(maxChange, node.oldIndex);
      }
    }
    if (minChange <= maxChange) {
      int[] moveIndex = new int[maxChange - minChange + 1];
      ProjectExplorerModel.Node<?>[] moveNodes = new ProjectExplorerModel.Node<?>[moveIndex.length];

      for (int i = maxChange; i >= minChange; i--) {
        ProjectExplorerModel.Node<T> node = nodeList.get(i);
        moveIndex[node.newIndex - minChange] = node.newIndex;
        moveNodes[node.newIndex - minChange] = node;
        remove(i);
      }

      for (int i = 0; i < moveIndex.length; i++) {
        insert(moveNodes[i], moveIndex[i]);
      }

      this.fireNodesChanged(moveIndex, moveNodes);
    }

    // identify inserted nodes
    if (insertionCount > 0) {
      int[] insIndex = new int[insertionCount];
      ProjectExplorerModel.Node<?>[] insNodes = new ProjectExplorerModel.Node<?>[insertionCount];
      int insertionsPos = 0;

      for (ProjectExplorerModel.Node<T> node : nodeList) {
        if (node.oldIndex < 0) {
          insert(node, node.newIndex);
          insIndex[insertionsPos] = node.newIndex;
          insNodes[insertionsPos] = node;
          insertionsPos++;
        }
      }
      this.fireNodesInserted(insIndex, insNodes);
    }
  }

  @Override
  void decommission() {
    if (file != null) {
      file.removeLibraryListener(this);
    }
    for (Enumeration<?> en = children(); en.hasMoreElements();) {
      Object n = en.nextElement();
      if (n instanceof ProjectExplorerModel.Node<?>) {
        ((ProjectExplorerModel.Node<?>) n).decommission();
      }
    }
  }

  public void libraryChanged(LibraryEvent event) {
    switch (event.getAction()) {
    case LibraryEvent.DIRTY_STATE:
    case LibraryEvent.SET_NAME:
      this.fireNodeChanged();
      break;
    case LibraryEvent.SET_MAIN:
      break;
    case LibraryEvent.ADD_TOOL:
    case LibraryEvent.REMOVE_TOOL:
    case LibraryEvent.MOVE_TOOL:
      buildChildren();
      break;
    case LibraryEvent.ADD_LIBRARY:
    case LibraryEvent.REMOVE_LIBRARY:
      buildDescendents();
      break;
    default:
      fireStructureChanged();
    }
  }

}
