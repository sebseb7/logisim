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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class Model {

  public static class Event {

  }

  public interface Listener {
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
    listeners = new EventSourceWeakSupport<Listener>();
    selection = new Selection(circuitState, this);
    log = new HashMap<SelectionItem, ValueLog>();

    // Add pins, clocks, and any other loggable component that doesn't
    // have options (so we exclude Ram/Rom) and isn't a subcircuit.
    for (Component comp : circuitState.getCircuit().getNonWires()) {
      if (comp.getFactory() instanceof SubcircuitFactory)
        continue;
      Loggable log = (Loggable)comp.getFeature(Loggable.class);
      if (log == null)
        continue;
      Object[] opts = log.getLogOptions(circuitState);
      if (opts != null && opts.length > 0)
        continue;
      Component[] path = new Component[] { };
      selection.add(new SelectionItem(this, path, comp, null));
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
