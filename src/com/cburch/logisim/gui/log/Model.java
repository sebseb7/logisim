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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class Model implements CircuitListener {

  public static class Event {

  }

  public interface Listener {
    public void resetEntries(Event event, Value[] values);
    public void entryAdded(Event event, Value[] values);
    public void filePropertyChanged(Event event);
    public void selectionChanged(Event event);
  }

  private EventSourceWeakSupport<Listener> listeners;
  private Selection selection;
  private HashMap<SelectionItem, ValueLog> log;
  private boolean fileEnabled = false;
  private File file = null;
  private boolean fileHeader = true;
  private boolean selected = false;
  private LogThread logger = null;

  public Model(CircuitState circuitState) {
    listeners = new EventSourceWeakSupport<>();
    selection = new Selection(circuitState, this);
    log = new HashMap<>();

    // Add top-level pins, clocks, etc.
    Circuit circ = circuitState.getCircuit();
    for (Component comp : circ.getNonWires())
      addIfDefaultComponent(circ, comp);
    selection.sort();

    // Listen for new pins, clocks, etc.
    circuitState.getCircuit().addCircuitListener(this);
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    int action = event.getAction();
    // todo: gracefully handle pin width changes, other circuit changes
//     if (action == CircuitEvent.ACTION_ADD) {
//       System.out.println("add " + event);
//       Circuit circ = event.getCircuit();
//       Component comp = (Component)event.getData();
//       addIfDefaultComponent(circ, comp);
//     } else {
//       System.out.println("non-add " + event);
//       if (action == CircuitEvent.TRANSACTION_DONE) {
//         Circuit circuit = event.getCircuit();
//         ReplacementMap repl = event.getResult().getReplacementMap(circuit);
//         System.out.println(repl);
//       }
//    }
    // if (action == CircuitEvent.ACTION_INVALIDATE); // todo width change handle in SelItem
    if (action == CircuitEvent.TRANSACTION_DONE) {
      CircuitState circuitState = getCircuitState();
      // Circuit circuit = event.getCircuit();
      Circuit circuit = circuitState.getCircuit();
      ReplacementMap repl = event.getResult().getReplacementMap(circuit);
      if (repl == null) {
        System.out.println("no changes for top level circuit\n");
        return;
      }
      System.out.println(repl);
    }

      // we are looking for new pins, etc., that are not replacing some old
      // pin
      //for (Component comp : repl.getReplacedComponents()) {

      //    Object compState = componentData.remove(comp);
      //    if (compState == null)
      //      continue;
      //    Class<?> compFactory = comp.getFactory().getClass();
      //    boolean found = false;
      //    for (Component repl : map.get(comp)) {
      //      if (repl.getFactory().getClass() == compFactory) {
      //        found = true;
      //        setData(repl, compState);
      //        break;
      //      }
      //    }


      //HashSet<Instance> adds = new HashSet<>();
      //HashSet<Instance> removes = new HashSet<>();
      //HashMap<Instance, Instance> replaces = new HashMap<>();

      //for (Component comp : repl.getAdditions()) {
      //  addIfDefaultComponent(circ, comp);
      //  if (comp.getFactory() instanceof Pin) {
      //    Instance in = Instance.getInstanceFor(comp);
      //    boolean added = pins.add(in);
      //    if (added) {
      //      comp.addComponentListener(myComponentListener);
      //      in.getAttributeSet().addAttributeListener(
      //          myComponentListener);
      //      adds.add(in);
      //    }
      //  }
      //}
    // for (Component comp : repl.getRemovals()) {
    //   if (comp.getFactory() instanceof Pin) {
    //     Instance in = Instance.getInstanceFor(comp);
    //     boolean removed = pins.remove(in);
    //     if (removed) {
    //       comp.removeComponentListener(myComponentListener);
    //       in.getAttributeSet().removeAttributeListener(
    //           myComponentListener);
    //       Collection<Component> rs = repl
    //           .getComponentsReplacing(comp);
    //       if (rs.isEmpty()) {
    //         removes.add(in);
    //       } else {
    //         Component r = rs.iterator().next();
    //         Instance rin = Instance.getInstanceFor(r);
    //         adds.remove(rin);
    //         replaces.put(in, rin);
    //       }
    //     }
    //   }
    // }

    // appearanceManager.updatePorts(adds, removes, replaces, getPins());
    //}
  }

  // Add top-level pins, clocks, and any other loggable component that doesn't
  // have options (so we exclude Ram/Rom) and isn't a subcircuit.
  private void addIfDefaultComponent(Circuit circ, Component comp) {
    CircuitState circuitState = getCircuitState();
    if (circ != circuitState.getCircuit())
      return;
    if (comp.getFactory() instanceof SubcircuitFactory)
      return;
    Loggable log = (Loggable)comp.getFeature(Loggable.class);
    if (log == null)
      return;
    Object[] opts = log.getLogOptions(circuitState);
    if (opts != null && opts.length > 0) // exclude Ram, Rom, etc.
      return;
    Component[] path = new Component[] { };
    SelectionItem item = new SelectionItem(this, path, comp, null);
    if (!selection.contains(item)) {
      System.out.println("selection is");
      for (int i = 0; i < selection.size(); i++)
        System.out.println(i + " "  + selection.get(i));
      System.out.println("adding: " + item);
      selection.add(item);
      // set an initial value
      Value v = item.fetchValue(circuitState);
      getValueLog(item).append(v);
      System.out.println("selection is now ");
      for (int i = 0; i < selection.size(); i++)
        System.out.println(i + " "  + selection.get(i));
    }
  }

  public void addModelListener(Listener l) {
    listeners.add(l);
  }

  private void fireEntryAdded(Event e, Value[] values) {
    for (Listener l : listeners) {
      l.entryAdded(e, values);
    }
  }

  private void fireReset(Event e, Value[] values) {
    for (Listener l : listeners) {
      l.resetEntries(e, values);
    }
  }

  private void fireFilePropertyChanged(Event e) {
    for (Listener l : listeners) {
      l.filePropertyChanged(e);
    }
  }

  void fireSelectionChanged(Event e) {
    for (Iterator<SelectionItem> it = log.keySet().iterator(); it.hasNext();) {
      SelectionItem i = it.next();
      if (selection.indexOf(i) < 0) {
        it.remove();
      }
    }

    for (Listener l : listeners) {
      l.selectionChanged(e);
    }
  }

  public CircuitState getCircuitState() {
    return selection.getCircuitState();
  }

  public File getFile() {
    return file;
  }

  public boolean getFileHeader() {
    return fileHeader;
  }

  public Selection getSelection() {
    return selection;
  }

  public ValueLog getValueLog(SelectionItem item) {
    ValueLog ret = log.get(item);
    if (ret == null && selection.indexOf(item) >= 0) {
      ret = new ValueLog();
      log.put(item, ret);
    }
    return ret;
  }

  public boolean isFileEnabled() {
    return fileEnabled;
  }

  public boolean isSelected() {
    return selected;
  }

  public void propagationCompleted() {
    CircuitState circuitState = getCircuitState();
    Value[] vals = new Value[selection.size()];
    boolean changed = false;
    for (int i = selection.size() - 1; i >= 0; i--) {
      SelectionItem item = selection.get(i);
      vals[i] = item.fetchValue(circuitState);
      if (!changed) {
        Value v = getValueLog(item).getLast();
        changed = v == null ? vals[i] != null : !v.equals(vals[i]);
      }
    }
    if (changed) {
      for (int i = selection.size() - 1; i >= 0; i--) {
        SelectionItem item = selection.get(i);
        getValueLog(item).append(vals[i]);
      }
      fireEntryAdded(new Event(), vals);
    }
  }

  public void simulatorReset() {
    CircuitState circuitState = getCircuitState();
    Value[] vals = new Value[selection.size()];
    for (int i = selection.size() - 1; i >= 0; i--) {
      SelectionItem item = selection.get(i);
      vals[i] = item.fetchValue(circuitState);
      getValueLog(item).reset(vals[i]);
    }
    fireReset(new Event(), vals);
  }

  public void removeModelListener(Listener l) {
    listeners.remove(l);
  }

  public void setFile(File value) {
    if (file == null ? value == null : file.equals(value))
      return;
    file = value;
    fileEnabled = file != null;
    fireFilePropertyChanged(new Event());
  }

  public void setFileEnabled(boolean value) {
    if (fileEnabled == value)
      return;
    fileEnabled = value;
    fireFilePropertyChanged(new Event());
  }

  public void setFileHeader(boolean value) {
    if (fileHeader == value)
      return;
    fileHeader = value;
    fireFilePropertyChanged(new Event());
  }

  public void setSelected(JFrame frame, boolean value) {
    if (selected == value)
      return;
    selected = value;
    if (selected) {
      logger = new LogThread(this);
      logger.start();
    } else {
      if (logger != null)
        logger.cancel();
      logger = null;
      fileEnabled = false;
    }
    fireFilePropertyChanged(new Event());
  }
}
