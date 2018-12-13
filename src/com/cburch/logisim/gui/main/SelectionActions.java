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
import static com.cburch.logisim.gui.main.Strings.S;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.HashSet;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.CircuitTransactionResult;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Dependencies;
import com.cburch.logisim.proj.JoinedAction;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.tools.Library;

public class SelectionActions {
  /**
   * Code taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Anchor extends Action {

    private Selection sel;
    private int numAnchor;
    private SelectionSave before;
    private CircuitTransaction xnReverse;

    Anchor(Selection sel, int numAnchor) {
      this.sel = sel;
      this.before = SelectionSave.create(sel);
      this.numAnchor = numAnchor;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.dropAll(xn);
      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numAnchor == 1 ? S.get("dropComponentAction")
          : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction)
        last = ((JoinedAction) other).getLastAction();
      else
        last = other;

      SelectionSave otherAfter = null;
      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }

  }

  private static class Delete extends Action {
    private Selection sel;
    private CircuitTransaction xnReverse;

    Delete(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.deleteAllHelper(xn);
      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("deleteSelectionAction");
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  /**
   * Code taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Drop extends Action {

    private Selection sel;
    private Component[] drops;
    private int numDrops;
    private SelectionSave before;
    private CircuitTransaction xnReverse;

    Drop(Selection sel, Collection<Component> toDrop, int numDrops) {
      this.sel = sel;
      this.drops = new Component[toDrop.size()];
      toDrop.toArray(this.drops);
      this.numDrops = numDrops;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);

      for (Component comp : drops) {
        sel.remove(xn, comp);
      }

      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numDrops == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction)
        last = ((JoinedAction) other).getLastAction();
      else
        last = other;

      SelectionSave otherAfter = null;

      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }

      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }

  }

  private static class Duplicate extends Action {
    private Selection sel;
    private CircuitTransaction xnReverse;
    private SelectionSave after;

    Duplicate(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.duplicateHelper(xn);

      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
      after = SelectionSave.create(sel);
    }

    @Override
    public String getName() {
      return S.get("duplicateSelectionAction");
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  private static class Paste extends Action {
    private Selection sel;
    private LayoutClipboard.Data clip;
    private CircuitTransaction xnReverse, cxnReverse;
    private SelectionSave after;

    Paste(Selection sel, LayoutClipboard.Data clip) {
      this.sel = sel;
      this.clip = clip;
    }

    private HashSet<Circuit> getDependencies(Circuit circ, Collection<Circuit> newCircs) {
      LinkedList<Circuit> todo = new LinkedList<>();
      HashSet<Circuit> downstream = new HashSet<>();
      todo.add(circ);
      while (!todo.isEmpty()) {
        Circuit c = todo.remove();
        downstream.add(c);
        if (!newCircs.contains(c))
          continue; // reached the boundary between new and old circuits
        for (Component comp : circ.getNonWires()) {
          if (comp.getFactory() instanceof SubcircuitFactory) {
            SubcircuitFactory factory = (SubcircuitFactory) comp.getFactory();
            Circuit subCirc = factory.getSubcircuit();
            if (subCirc == circ)
              return null; // circular
            if (todo.contains(subCirc) || downstream.contains(subCirc))
              continue; // already visited
            todo.add(subCirc);
          }
        }
      }
      return downstream;
    }

    @Override
    public void doIt(Project proj) {
      LayoutClipboard.Data clip = LayoutClipboard.SINGLETON.get(proj);
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);

      Canvas canvas = proj.getFrame().getCanvas();
      Circuit circ = canvas.getCircuit();
      
      // check if adding these components would cause a circular dependency
      Dependencies dependencies = canvas.getProject().getDependencies();
      for (Component c : clip.components) {
        ComponentFactory factory = c.getFactory();
        if (!(factory instanceof SubcircuitFactory))
          continue;
        Circuit subCirc = ((SubcircuitFactory)factory).getSubcircuit();
        if (clip.circuits.contains(subCirc))
          continue; // no dependency info for this subcircuit
        if (!dependencies.canAdd(circ, subCirc)) {
          canvas.setErrorMessage(
              com.cburch.logisim.tools.Strings.S.getter("circularError"));
          return;
        }
      }

      // new circuits are not represented in the existing dependencies, so we
      // need to (a) make sure there are no circularities within the new
      // circuits and (b) check whether we can add an edge from the current
      // circuit to every dependency of the new circuits.
      for (Circuit c : clip.circuits) {
        HashSet<Circuit> downstream = getDependencies(c, clip.circuits);
        if (downstream == null) { // failure inicates circularity
          canvas.setErrorMessage(
              com.cburch.logisim.tools.Strings.S.getter("circularError"));
          return;
        }
        for (Circuit subCirc : downstream) {
          if (clip.circuits.contains(subCirc))
            continue; // not on the boundary between new and old circuits
          if (!dependencies.canAdd(circ, subCirc)) {
            canvas.setErrorMessage(
                com.cburch.logisim.tools.Strings.S.getter("circularError"));
            return;
          }
        }
      }

      LogisimFile file = proj.getLogisimFile();
      for (Library lib : clip.libraries)
        file.addLibrary(lib);
      for (VhdlContent vhdl : clip.vhdl)
        file.addVhdlContent(vhdl);
      if (clip.circuitTransaction != null) {
        for (Circuit c : clip.circuits)
          file.addCircuit(c);
        CircuitTransactionResult result = clip.circuitTransaction.execute();
        cxnReverse = result.getReverseTransaction();
      } else {
        cxnReverse = null;
      }

      sel.pasteHelper(xn, clip.components);
      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
      after = SelectionSave.create(sel);
    }

    @Override
    public String getName() {
      return S.get("pasteClipboardAction");
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
      if (cxnReverse != null)
        cxnReverse.execute();
      LogisimFile file = proj.getLogisimFile();
      for (Circuit circ : clip.circuits)
        file.removeCircuit(circ);
      for (VhdlContent vhdl : clip.vhdl)
        file.removeVhdl(vhdl);
      for (Library lib : clip.libraries)
        file.removeLibrary(lib);
    }
  }

  private static class Translate extends Action {
    private Selection sel;
    private int dx;
    private int dy;
    private ReplacementMap replacements;
    private SelectionSave before;
    private CircuitTransaction xnReverse;

    Translate(Selection sel, int dx, int dy, ReplacementMap replacements) {
      this.sel = sel;
      this.dx = dx;
      this.dy = dy;
      this.replacements = replacements;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);

      sel.translateHelper(xn, dx, dy);
      if (replacements != null) {
        xn.replace(replacements);
      }

      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("moveSelectionAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction)
        last = ((JoinedAction) other).getLastAction();
      else
        last = other;

      SelectionSave otherAfter = null;
      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  /**
   * Code taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  // anchors all floating elements, keeping elements in selection
  public static Action anchorAll(Selection sel) {
    int numAnchor = sel.getFloatingComponents().size();
    if (numAnchor == 0) {
      return null;
    } else {
      return new Anchor(sel, numAnchor);
    }
  }

  public static Action clear(Selection sel) {
    return new Delete(sel);
  }

  public static void copy(Selection sel) { // Note: copy is not an Action
    LayoutClipboard.SINGLETON.set(sel);
  }

  public static Action cut(Selection sel) {
    LayoutClipboard.SINGLETON.set(sel);
    return new Delete(sel);
  }

  // clears the selection, anchoring all floating elements in selection
  public static Action drop(Selection sel, Collection<Component> comps) {
    HashSet<Component> floating = new HashSet<Component>(
        sel.getFloatingComponents());
    HashSet<Component> anchored = new HashSet<Component>(
        sel.getAnchoredComponents());
    ArrayList<Component> toDrop = new ArrayList<Component>();
    ArrayList<Component> toIgnore = new ArrayList<Component>();
    for (Component comp : comps) {
      if (floating.contains(comp)) {
        toDrop.add(comp);
      } else if (anchored.contains(comp)) {
        toDrop.add(comp);
        toIgnore.add(comp);
      }
    }
    if (toDrop.size() == toIgnore.size()) {
      for (Component comp : toIgnore) {
        sel.remove(null, comp);
      }
      return null;
    } else {
      int numDrop = toDrop.size() - toIgnore.size();
      return new Drop(sel, toDrop, numDrop);
    }
  }

  public static Action dropAll(Selection sel) {
    return drop(sel, sel.getComponents());
  }

  public static Action duplicate(Selection sel) {
    return new Duplicate(sel);
  }

  public static Action pasteMaybe(Project proj, Selection sel) {
    LayoutClipboard.Data clip = LayoutClipboard.SINGLETON.get(proj);
    if (clip == null)
      return null;
    return new Paste(sel, clip);
  }

  public static Action translate(Selection sel, int dx, int dy, ReplacementMap repl) {
    return new Translate(sel, dx, dy, repl);
  }

  private SelectionActions() {
  }
}
