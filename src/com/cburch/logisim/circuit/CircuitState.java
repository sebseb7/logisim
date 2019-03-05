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

package com.cburch.logisim.circuit;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.cburch.logisim.circuit.Propagator.SetData;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentState;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamState;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Pin;

public class CircuitState implements InstanceData {

  private class MyCircuitListener implements CircuitListener {
    public void circuitChanged(CircuitEvent event) {
      int action = event.getAction();

      /* Component was added */
      if (action == CircuitEvent.ACTION_ADD) {
        Component comp = (Component) event.getData();
        if (comp instanceof Wire) {
          Wire w = (Wire) comp;
          markPointAsDirty(w.getEnd0());
          markPointAsDirty(w.getEnd1());
        } else {
          markComponentAsDirty(comp);
        }
      }

      /* Component was removed */
      else if (action == CircuitEvent.ACTION_REMOVE) {
        Component comp = (Component) event.getData();
        if (comp == temporaryClock)
          temporaryClock = null;
        if (comp.getFactory() instanceof Clock) {
          knownClocks = false; // just in case, will be recomputed by simulator
        }

        if (comp.getFactory() instanceof SubcircuitFactory) {
          knownClocks = false; // just in case, will be recomputed by simulator
          // disconnect from tree
          CircuitState substate = (CircuitState) getData(comp);
          if (substate != null && substate.parentComp == comp) {
            substates.remove(substate);
            substate.parentState = null;
            substate.parentComp = null;
          }
        }

        if (comp instanceof Wire) {
          Wire w = (Wire) comp;
          markPointAsDirty(w.getEnd0());
          markPointAsDirty(w.getEnd1());
        } else {
          base.checkComponentEnds(CircuitState.this, comp);
          dirtyComponents.remove(comp);
        }
      }

      /* Whole circuit was cleared */
      else if (action == CircuitEvent.ACTION_CLEAR) {
        temporaryClock = null;
        knownClocks = false;
        substates.clear();
        wireData = null;
        componentData.clear();
        values.clear(); // slow path
        clearFastpathGrid(); // fast path
        dirtyComponents.clear();
        dirtyPoints.clear();
        causes.clear();
      }

      /* Component changed */
//      else if (action == CircuitEvent.ACTION_CHANGE) {
//        Object data = event.getData();
//        if (data instanceof Collection) {
//          @SuppressWarnings("unchecked")
//          Collection<Component> comps = (Collection<Component>) data;
//          markComponentsDirty(comps);
//          for (Component comp : comps)
//            base.checkComponentEnds(CircuitState.this, comp);
//        } else {
//          Component comp = (Component) event.getData();
//          markComponentAsDirty(comp);
//          base.checkComponentEnds(CircuitState.this, comp);
//        }
//      }
      else if (action == CircuitEvent.ACTION_INVALIDATE) {
        Component comp = (Component) event.getData();
        markComponentAsDirty(comp);
        // If simulator is in single step mode, we want to hilight the
        // invalidated components (which are likely Pins, Buttons, or other
        // inputs), so pass this component to the simulator for display.
        proj.getSimulator().addPendingInput(CircuitState.this, comp);

        // TODO detemine if this should really be missing
        // base.checkComponentEnds(CircuitState.this, comp);
      } else if (action == CircuitEvent.TRANSACTION_DONE) {
        ReplacementMap map = event.getResult().getReplacementMap(circuit);
        if (map == null)
          return;
        for (Component comp : map.getRemovals()) {
          Object compState = componentData.remove(comp);
          if (compState == null)
            continue;
          Class<?> compFactory = comp.getFactory().getClass();
          boolean found = false;
          for (Component repl : map.getReplacementsFor(comp)) {
            if (repl.getFactory().getClass() == compFactory) {
              found = true;
              setData(repl, compState);
              break;
            }
          }
          if (!found && compState instanceof RamState) {
            Ram.closeHexFrame((RamState)compState);
          }
          if (!found && compState instanceof CircuitState) {
            CircuitState sub = (CircuitState) compState;
            sub.parentState = null;
            substates.remove(sub);
          }
        }
      }
    }
  }

  private MyCircuitListener myCircuitListener = new MyCircuitListener();
  private Propagator base; // inherited from base of tree of CircuitStates
  private Project proj; // project containing this circuit
  private Circuit circuit; // circuit being simulated

  private CircuitState parentState; // parent in tree of CircuitStates
  private Component parentComp; // subcircuit component containing this state
  private HashSet<CircuitState> substates = new HashSet<>();

  private CircuitWires.State wireData;
  private HashMap<Component, Object> componentData = new HashMap<>();
  private Map<Location, Value> values = new HashMap<>();
  private CopyOnWriteArraySet<Component> dirtyComponents = new CopyOnWriteArraySet<>();
  private CopyOnWriteArraySet<Location> dirtyPoints = new CopyOnWriteArraySet<>();
  HashMap<Location, SetData> causes = new HashMap<>();

  private static final int FASTPATH_GRID_WIDTH = 200;
  private static final int FASTPATH_GRID_HEIGHT = 200;
  private Value[][] fastpath_grid = new Value[FASTPATH_GRID_HEIGHT][FASTPATH_GRID_WIDTH];

  private static int lastId = 0;
  private int id = lastId++;

  private CircuitState(Project proj, Circuit circuit, Propagator prop) {
    this.proj = proj;
    this.circuit = circuit;
    this.base = prop != null ? prop : new Propagator(this);
    circuit.addCircuitListener(myCircuitListener);
    markAllComponentsDirty();
  }

  @Override
  public CircuitState clone() {
    try { throw new Exception("*** why? ***"); }
    catch (Exception e) { e.printStackTrace(); }
    return cloneAsNewRootState();
  }

  public static CircuitState createRootState(Project proj, Circuit circuit) {
    return new CircuitState(proj, circuit, null /* make new Propagator */);
  }

  public CircuitState cloneAsNewRootState() {
    CircuitState ret = new CircuitState(proj, circuit, null /* make new Propatator */);
    ret.copyFrom(this);
    ret.parentComp = null; // detatch from old parent component and state
    ret.parentState = null;
    return ret;
  }

  private void copyFrom(CircuitState src) {
    // this.base = ... DO NOT COPY propagator, as this circuit has its own already.
    this.parentComp = src.parentComp;
    this.parentState = src.parentState;
    HashMap<CircuitState, CircuitState> substateData = new HashMap<>();
    this.substates = new HashSet<CircuitState>();
    for (CircuitState oldSub : src.substates) {
      CircuitState newSub = new CircuitState(src.proj, oldSub.circuit, this.base);
      newSub.copyFrom(oldSub);
      newSub.parentState = this;
      this.substates.add(newSub);
      substateData.put(oldSub, newSub);
    }
    for (Component key : src.componentData.keySet()) {
      Object oldValue = src.componentData.get(key);
      if (oldValue instanceof CircuitState) {
        Object newValue = substateData.get(oldValue);
        if (newValue != null)
          this.componentData.put(key, newValue);
        else
          this.componentData.remove(key);
      } else {
        Object newValue;
        if (oldValue instanceof ComponentState) {
          newValue = ((ComponentState) oldValue).clone();
        } else {
          newValue = oldValue;
        }
        this.componentData.put(key, newValue);
      }
    }
    for (Location key : src.causes.keySet()) {
      Propagator.SetData oldValue = src.causes.get(key);
      Propagator.SetData newValue = oldValue.cloneFor(this);
      this.causes.put(key, newValue);
    }
    if (src.wireData != null) {
      this.wireData = (CircuitWires.State) src.wireData.clone();
    }
    this.values.clear(); // slow path
    this.values.putAll(src.values);
    for(int y = 0; y < FASTPATH_GRID_HEIGHT; y++) { // fast path
      System.arraycopy(src.fastpath_grid[y], 0,
          this.fastpath_grid[y], 0, FASTPATH_GRID_WIDTH);
    }
    this.dirtyComponents.addAll(src.dirtyComponents);
    this.dirtyPoints.addAll(src.dirtyPoints);
  }

  public void drawOscillatingPoints(ComponentDrawContext context) {
    base.drawOscillatingPoints(context);
  }

  public Circuit getCircuit() {
    return circuit;
  }

  Value getComponentOutputAt(Location p) {
    // for CircuitWires - to get values, ignoring wires' contributions
    Propagator.SetData cause_list = causes.get(p);
    return Propagator.computeValue(cause_list);
  }

  public Object getData(Component comp) {
    return componentData.get(comp);
  }

  public InstanceState getInstanceState(Component comp) {
    Object factory = comp.getFactory();
    if (factory instanceof InstanceFactory) {
      return ((InstanceFactory) factory).createInstanceState(this, comp);
    } else {
      throw new RuntimeException(
          "getInstanceState requires instance component");
    }
  }

  public InstanceState getInstanceState(Instance instance) {
    Object factory = instance.getFactory();
    if (factory instanceof InstanceFactory) {
      return ((InstanceFactory) factory).createInstanceState(this,
          instance);
    } else {
      throw new RuntimeException(
          "getInstanceState requires instance component");
    }
  }

  public CircuitState getParentState() {
    return parentState;
  }

  public CircuitState getAncestorState() {
    CircuitState ancestor = this;
    while (ancestor.parentState != null)
      ancestor = ancestor.parentState;
    return ancestor;
  }

  public Project getProject() {
    return proj;
  }

  public Propagator getPropagator() {
    return base;
  }

  Component getSubcircuit() {
    return parentComp;
  }

  public Set<CircuitState> getSubstates() { // returns Set of CircuitStates
    return substates;
  }

  public Value getValue(Location p) {
    Value ret = getValueByWire(p);
    if (ret != null)
      return ret;
    return Value.createUnknown(circuit.getWidth(p));
  }

  Value getValueByWire(Location p) {
    if (p.x % 10 == 0 && p.y % 10 == 0
        && p.x < FASTPATH_GRID_WIDTH*10
        && p.y < FASTPATH_GRID_HEIGHT*10) {
      // fast path
      int x = p.x/10;
      int y = p.y/10;
      return fastpath_grid[y][x];
    } else {
      // slow path
      return values.get(p);
    }
  }

  CircuitWires.State getWireData() {
    return wireData;
  }

  public boolean isSubstate() {
    return parentState != null;
  }

  private void markAllComponentsDirty() {
    dirtyComponents.addAll(circuit.getNonWires());
  }

  public void markComponentAsDirty(Component comp) {
    try {
      dirtyComponents.add(comp);
    } catch (RuntimeException e) {
      CopyOnWriteArraySet<Component> set = new CopyOnWriteArraySet<Component>();
      set.add(comp);
      dirtyComponents = set;
    }
  }

  public void markComponentsDirty(Collection<Component> comps) {
    dirtyComponents.addAll(comps);
  }

  public void markPointAsDirty(Location pt) {
    dirtyPoints.add(pt);
  }

  void processDirtyComponents() {
    if (!dirtyComponents.isEmpty()) {
      // This seeming wasted copy is to avoid ConcurrentModifications
      // if we used an iterator instead.
      Object[] toProcess;
      RuntimeException firstException = null;
      for (int tries = 4; true; tries--) {
        try {
          toProcess = dirtyComponents.toArray();
          break;
        } catch (RuntimeException e) {
          if (firstException == null)
            firstException = e;
          if (tries == 0) {
            toProcess = new Object[0];
            dirtyComponents = new CopyOnWriteArraySet<Component>();
            throw firstException;
          }
        }
      }
      dirtyComponents.clear();
      for (Object compObj : toProcess) {
        if (compObj instanceof Component) {
          Component comp = (Component) compObj;
          comp.propagate(this);
          if (comp.getFactory() instanceof Pin && parentState != null) {
            // should be propagated in superstate
            parentComp.propagate(parentState);
          }
        }
      }
    }

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs))
      substate.processDirtyComponents();
  }

  void processDirtyPoints() {
    HashSet<Location> dirty = new HashSet<>(dirtyPoints);
    dirtyPoints.clear();
    if (circuit.wires.isMapVoided()) {
      // Note: this is a stopgap hack until we figure out the cause of the
      // concurrent modification exception.
      for (int i = 3; i >= 0; i--) {
        try {
          dirty.addAll(circuit.wires.points.getSplitLocations());
          break;
        } catch (ConcurrentModificationException e) {
          // try again...
          try {
            Thread.sleep(1);
          } catch (InterruptedException e2) {
            // Yes, swallow the interrupt -- if simulator thread is interrupted
            // while it is in here, we want to keep going. The simulator thread
            // uses interrupts only for cancelling its own sleep/wait calls, not
            // this sleep call.
          }
          if (i == 0)
            e.printStackTrace();
        }
      }
    }
    if (!dirty.isEmpty()) {
      circuit.wires.propagate(this, dirty);
    }

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs))
      substate.processDirtyPoints();
  }

  public void reset() {
    temporaryClock = null;
    wireData = null;
    for (Iterator<Component> it = componentData.keySet().iterator(); it.hasNext();) {
      Component comp = it.next();
      if (comp.getFactory() instanceof Ram) {
        Ram ram = (Ram)comp.getFactory();
        boolean remove = ram.reset(this, Instance.getInstanceFor(comp));
        if (remove)
          it.remove();
      } else if (!(comp.getFactory() instanceof SubcircuitFactory)) {
        it.remove();
      }
    }
    values.clear(); // slow path
    clearFastpathGrid(); // fast path
    dirtyComponents.clear();
    dirtyPoints.clear();
    causes.clear();
    markAllComponentsDirty();

    for (CircuitState sub : substates) {
      sub.reset();
    }
  }

  public CircuitState createCircuitSubstateFor(Component comp, Circuit circ) {
      CircuitState oldState = (CircuitState)componentData.get(comp);
      if (oldState != null && oldState.parentComp == comp) {
        // fixme: Does this ever happen?
        System.out.println("fixme: removed stale circuitstate... should never happen");
        substates.remove(oldState);
        oldState.parentState = null;
        oldState.parentComp = null;
      }
      CircuitState newState = new CircuitState(proj, circ, base);
      substates.add(newState);
      newState.parentState = this;
      newState.parentComp = comp;
      componentData.put(comp, newState);
      return newState;
  }

  public void setData(Component comp, Object data) {
    if (data instanceof CircuitState) {
      // fixme: should never happen?
      System.out.println("fixme: setData with circuitstate... should never happen");
    }
    componentData.put(comp, data);
  }

  public void setValue(Location pt, Value val, Component cause, int delay) {
    base.setValue(this, pt, val, cause, delay);
  }

  private void clearFastpathGrid() {
    for (int y = 0; y < FASTPATH_GRID_HEIGHT; y++)
      for (int x = 0; x < FASTPATH_GRID_WIDTH; x++)
        fastpath_grid[y][x] = null;
  }


  // for CircuitWires - to set value at point
  void setValueByWire(Location p, Value v) {
    boolean changed;
    if (p.x % 10 == 0 && p.y % 10 == 0
        && p.x < FASTPATH_GRID_WIDTH*10
        && p.y < FASTPATH_GRID_HEIGHT*10) {
      // fast path
      int x = p.x/10;
      int y = p.y/10;
      if (v == Value.NIL) {
        if (fastpath_grid[y][x] != null) {
          changed = true;
          fastpath_grid[y][x] = null;
        } else {
          changed = false;
        }
      } else {
        if (!v.equals(fastpath_grid[y][x])) {
          changed = true;
          fastpath_grid[y][x] = v;
        } else {
          changed = false;
        }
      }
    } else {
      // slow path
      if (v == Value.NIL) {
        Object old = values.remove(p);
        changed = (old != null && old != Value.NIL);
      } else {
        Object old = values.put(p, v);
        changed = !v.equals(old);
      }
    }
    if (changed) {
      boolean found = false;
      for (Component comp : circuit.getComponents(p)) {
        if (!(comp instanceof Wire) && !(comp instanceof Splitter)) {
          found = true;
          markComponentAsDirty(comp);
        }
      }
      // NOTE: this will cause a double-propagation on components
      // whose outputs have just changed.

      if (found)
        base.locationTouched(this, p);
    }
  }

  void setWireData(CircuitWires.State data) {
    wireData = data;
  }

  boolean toggleClocks(int ticks) {
    boolean ret = false;

    if (temporaryClock != null)
      ret |= temporaryClockValidateOrTick(ticks);

    for (Component clock : circuit.getClocks())
      ret |= Clock.tick(this, ticks, clock);

    CircuitState[] subs = new CircuitState[substates.size()];
    for (CircuitState substate : substates.toArray(subs))
      ret |= substate.toggleClocks(ticks);

    return ret;
  }

  private boolean temporaryClockValidateOrTick(int ticks) {
    // temporaryClock.getFactory() will be Pin, normally a 1 bit input
    Pin pin;
    try {
      pin = (Pin)temporaryClock.getFactory();
    } catch (ClassCastException e) {
      temporaryClock = null;
      return false;
    }
    Instance i = Instance.getInstanceFor(temporaryClock);
    if (i == null || !pin.isInputPin(i) || pin.getWidth(i).getWidth() != 1) {
      temporaryClock = null;
      return false;
    }
    if (ticks >= 0) {
      InstanceState state = getInstanceState(i);
      // Value v = pin.getValue(state);
      pin.setValue(state, ticks%2==0 ? Value.FALSE : Value.TRUE);
      state.fireInvalidated();
    }
    return true;
  }

  private boolean knownClocks;
  private Component temporaryClock;

  public boolean hasKnownClocks() {
    return knownClocks || temporaryClock != null;
  }

  public void markKnownClocks() {
    knownClocks = true;
  }

  public boolean setTemporaryClock(Component clk) {
    temporaryClock = clk;
    return clk == null ? true : temporaryClockValidateOrTick(-1);
  }
  
  public Component getTemporaryClock() {
    return temporaryClock;
  }

  @Override
  public String toString() {
    return "State" + id + "[" + circuit.getName() + "]";
  }

  public int getId() {
    return id;
  }
}
