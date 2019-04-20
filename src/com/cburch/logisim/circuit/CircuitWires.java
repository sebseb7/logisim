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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.PullResistor;
import com.cburch.logisim.std.wiring.Tunnel;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IteratorUtil;

class CircuitWires {

  static class BundleMap {
    HashMap<Location, WireBundle> pointBundles = new HashMap<Location, WireBundle>();
    HashSet<WireBundle> bundles = new HashSet<WireBundle>();
    ArrayList<Location> allLocations = new ArrayList<>();
    HashMap<Location, ArrayList> componentsAtLocations = new HashMap<>();
    boolean isValid = true;
    // NOTE: It would make things more efficient if we also had
    // a set of just the first bundle in each tree.
    HashSet<WidthIncompatibilityData> incompatibilityData = null;

    void addWidthIncompatibilityData(WidthIncompatibilityData e) {
      if (incompatibilityData == null) {
        incompatibilityData = new HashSet<WidthIncompatibilityData>();
      }
      incompatibilityData.add(e);
    }

    WireBundle createBundleAt(Location p) {
      WireBundle ret = pointBundles.get(p);
      if (ret == null) {
        ret = new WireBundle(p);
        pointBundles.put(p, ret);
        bundles.add(ret);
      }
      return ret;
    }

    WireBundle getBundleAt(Location p) {
      return pointBundles.get(p);
    }

    Set<Location> getBundlePoints() {
      return pointBundles.keySet();
    }

    Set<WireBundle> getBundles() {
      return bundles;
    }

    HashSet<WidthIncompatibilityData> getWidthIncompatibilityData() {
      return incompatibilityData;
    }

    void invalidate() {
      isValid = false;
    }

    boolean isValid() {
      return isValid;
    }

    void setBundleAt(Location p, WireBundle b) {
      pointBundles.put(p, b);
    }
  }

  static class SplitterData {
    WireBundle[] end_bundle; // PointData associated with each end

    SplitterData(int fan_out) {
      end_bundle = new WireBundle[fan_out + 1];
    }
  }

  static class ValuedThread {
    int steps;
    ValuedBus[] bus;
    int[] position;
    boolean pullUp, pullDown;
    Value val = null;
    ValuedThread(WireThread t, HashMap<WireBundle, ValuedBus> allBuses) {
      steps = t.steps;
      position = t.position;
      bus = new ValuedBus[steps];
      for (int i = 0; i < steps; i++) {
        WireBundle b = t.bundle[i];
        bus[i] = allBuses.get(b);
        Value pullHere = b.getPullValue();
        pullUp |= (pullHere == Value.TRUE);
        pullDown |= (pullHere == Value.FALSE);
      }
      if (pullUp && pullDown)
        pullUp = pullDown = false;
    }
    // ValuedThread(ValuedThread t, HashMap<ValuedBus, ValuedBus> xBus) { // for cloning
    //   steps = t.steps;
    //   bus = new ValuedBus[steps];
    //   for (int i = 0; i < steps; i++)
    //     bus[i] = xBus.get(t.bus[i]);
    //   position = t.position;
    //   pullUp = t.pullUp;
    //   pullDown = t.pullDown;
    //   val = t.val;
    // }
    Value recalculate() {
      Value ret = Value.UNKNOWN;
      for (int i = 0; i < steps; i++) {
        ValuedBus vb = bus[i];
        int pos = position[i];
        Value val = vb.valAtPointSum;
        if (val != Value.NIL)
          ret = ret.combine(val.get(pos));
      }
      if (ret != Value.UNKNOWN)
        return ret;
      else if (pullUp)
        return Value.TRUE;
      else if (pullDown)
        return Value.FALSE;
      else
        return Value.UNKNOWN;
    }
  }

  static class ValuedBus {
    int idx = -1;
    ValuedThread[] threads;
    Location[] componentPoints; // subset of wire bundle xpoints that have components at them
    Component[][] componentsAffected; // components at each of those points
    Value[] valAtPoint;
    Value valAtPointSum; // cached sum of valAtPoint, or null if dirty
    Value val; // cached final value for bus
    int width;
    ValuedBus[] dependentBuses; // buses affected if this one changes value.
    boolean dirty;
    ValuedBus(WireBundle wb, BundleMap bm) {
      idx = -1; // filled in by caller
      filterComponents(bm, wb.xpoints);
      valAtPoint = new Value[componentPoints.length];
      width = wb.threads == null ? -1 : wb.getWidth().getWidth();
      dirty = true;
    }
    void filterComponents(BundleMap bm, Location[] locs) {
      ArrayList<Location> found = new ArrayList<>();
      ArrayList<ArrayList<Component>> affected = new ArrayList<>();
      for (Location p : locs) {
        ArrayList<Component> a = bm.componentsAtLocations.get(p);
        if (a == null)
          continue;
        found.add(p);
        affected.add(a);
      }
      int n = found.size();
      componentPoints = n == locs.length ? locs : found.toArray(new Location[n]);
      componentsAffected = new Component[n][];
      for (int i = 0; i < n; i++) {
        ArrayList<Component> a = affected.get(i);
        componentsAffected[i] = a.toArray(new Component[a.size()]);
      }
    }
    // ValuedBus(ValuedBus vb) { // for cloning
    //   idx = vb.idx;
    //   componentPoints = vb.componentPoints;
    //   valAtPoint = vb.valAtPoint.clone();
    //   valAtPointSum = vb.valAtPointSum;
    //   width = vb.width;
    //   dirty = vb.dirty;
    // }
    void makeThreads(WireThread[] wbthreads, HashMap<WireBundle, ValuedBus> allBuses,
        HashMap<WireThread, ValuedThread> allThreads) {
      if (width <= 0)
        return;
      threads = new ValuedThread[width];
      for (int i = 0; i < width; i++) {
        WireThread t = wbthreads[i];
        threads[i] = allThreads.get(t);
        if (threads[i] == null) {
          threads[i] = new ValuedThread(t, allBuses);
          allThreads.put(t, threads[i]);
        }
      }
    }
    // void makeThreads(ValuedThread[] oldThreads, HashMap<ValuedBus, ValuedBus> xBus,
    //     HashMap<ValuedThread, ValuedThread> xThread) { // for cloning
    //   if (width <= 0)
    //     return;
    //   threads = new ValuedThread[width];
    //   for (int i = 0; i < width; i++) {
    //     ValuedThread tOld = oldThreads[i];
    //     ValuedThread tNew = xThread.get(tOld);
    //     if (tNew == null) {
    //       tNew = new ValuedThread(tOld, xBus);
    //       xThread.put(tOld, tNew);
    //     }
    //     threads[i] = tNew;
    //   }
    // }
    Value recalculate() {
      if (width == 1) {
        Value tv = threads[0].val;
        if (tv == null)
          tv = threads[0].val = threads[0].recalculate();
        return tv;
      }
      int error = 0, unknown = 0, value = 0;
      for (int i = 0; i < width; i++) {
        int mask = 1 << i;
        Value tv = threads[i].val;
        if (tv == null)
          tv = threads[i].val = threads[i].recalculate();
        if (tv == Value.TRUE)
          value |= mask;
        else if (tv == Value.FALSE)
          ;
        else if (tv == Value.UNKNOWN)
          unknown |= mask;
        else
          error |= mask;
      }
      return Value.create_unsafe(width, error, unknown, value);
    }
  }

  State newState(CircuitState circState) {
    return new State(circState, getBundleMap());
  }

  static class State {
    private BundleMap bundleMap; // original source of connectivity info
    ValuedBus[] buses;
    int numDirty;
    HashMap<Location, ValuedBus> busAt = new HashMap<>();

    State(CircuitState circState, BundleMap bundleMap) {
      this.bundleMap = bundleMap;
      HashMap<WireBundle, ValuedBus> allBuses = new HashMap<>();
      HashMap<ValuedBus, WireBundle> srcBuses = new HashMap<>();
      buses = new ValuedBus[bundleMap.bundles.size()];
      int i = 0;
      for (WireBundle wb : bundleMap.bundles) {
        ValuedBus vb = new ValuedBus(wb, bundleMap);
        vb.idx = i++;
        buses[vb.idx] = vb;
        for (Location loc : wb.xpoints) {
          ValuedBus old = busAt.put(loc, vb);
          if (old != null) {
            throw new IllegalStateException("oops, two wires occupy same location");
          }
        }
        allBuses.put(wb, vb);
        srcBuses.put(vb, wb);
      }
      HashMap<WireThread, ValuedThread> allThreads = new HashMap<>();
      for (ValuedBus vb : buses) {
        vb.makeThreads(srcBuses.get(vb).threads, allBuses, allThreads);
        if (circState != null) {
          for (int j = 0; j < vb.componentPoints.length; j++) {
            Value val = circState.getComponentOutputAt(vb.componentPoints[j]);
            vb.valAtPoint[j] = val;
          }
        }
      }
      for (ValuedBus vb : buses) {
        if (vb.threads == null)
          continue;
        HashSet<ValuedBus> deps = new HashSet<>();
        for (ValuedThread t : vb.threads)
          for (ValuedBus dep : t.bus)
            if (dep != vb)
              deps.add(dep);
        int n = deps.size();
        if (n == 0)
          continue;
        vb.dependentBuses = deps.toArray(new ValuedBus[n]);
      }
      numDirty = buses.length;
    }

    // State(State s) { // for cloning
    //   this.bundleMap = s.bundleMap;
    //   this.buses = new ValuedBus[s.buses.length];
    //   this.numDirty = s.numDirty;
    //   HashMap<ValuedBus, ValuedBus> xBus = new HashMap<>();
    //   for (int i = 0; i < buses.length; i++) {
    //     ValuedBus vbOld = s.buses[i];
    //     ValuedBus vbNew = new ValuedBus(vbOld);
    //     buses[i] = vbNew;
    //     xBus.put(vbOld, vbNew);
    //   }
    //   HashMap<ValuedThread, ValuedThread> xThread = new HashMap<>();
    //   for (int i = 0; i < buses.length; i++) {
    //     ValuedBus vbOld = s.buses[i];
    //     ValuedBus vbNew = buses[i];
    //     if (vbOld.dependentBuses != null) {
    //       int n = vbOld.dependentBuses.length;
    //       vbNew.dependentBuses = new ValuedBus[n];
    //       for (int j = 0; j < n; j++)
    //         vbNew.dependentBuses[j] = xBus.get(vbOld);
    //     }
    //     vbNew.makeThreads(vbOld.threads, xBus, xThread);
    //   }
    // }

    void markClean(ValuedBus vb) {
      if (!vb.dirty) {
        throw new IllegalStateException("can't clean element that is not dirty");
      }
      if (vb.idx > numDirty-1) {
        throw new IllegalStateException("bad position for dirty element");
      }
      if (vb.idx < numDirty-1) {
        ValuedBus other = buses[numDirty-1];
        other.idx = vb.idx;
        buses[other.idx] = other;
        vb.idx = numDirty - 1;
        buses[vb.idx] = vb;
      }
      vb.dirty = false;
      numDirty--;
    }

    void markDirty(ValuedBus vb) {
      if (vb.dirty) {
        throw new IllegalStateException("can't mark dirty element as dirty");
      }
      if (vb.idx < numDirty) {
        throw new IllegalStateException("bad position for clean element");
      }
      if (vb.idx > numDirty) {
        ValuedBus other = buses[numDirty];
        other.idx = vb.idx;
        buses[other.idx] = other;
        vb.idx = numDirty;
        buses[vb.idx] = vb;
      }
      if (vb.threads != null) {
        for (ValuedThread vt : vb.threads)
          vt.val = null;
      }
      vb.dirty = true;
      numDirty++;
    }

    // @Override
    // public Object clone() {
    //   return new State(this);
    // }
  }

  private class TunnelListener implements AttributeListener {
    public void attributeListChanged(AttributeEvent e) {
    }

    public void attributeValueChanged(AttributeEvent e) {
      Attribute<?> attr = e.getAttribute();
      if (attr == StdAttr.LABEL || attr == PullResistor.ATTR_PULL_TYPE) {
        voidBundleMap();
      }
    }
  }

  private static Value pullValue(Value base, Value pullTo) {
    if (base.isFullyDefined()) {
      return base;
    } else if (base.getWidth() == 1) {
      if (base == Value.UNKNOWN)
        return pullTo;
      else
        return base;
    } else {
      Value[] ret = base.getAll();
      for (int i = 0; i < ret.length; i++) {
        if (ret[i] == Value.UNKNOWN)
          ret[i] = pullTo;
      }
      return Value.create(ret);
    }
  }

  // user-given data
  private HashSet<Wire> wires = new HashSet<Wire>();
  private HashSet<Splitter> splitters = new HashSet<Splitter>();
  private HashSet<Component> tunnels = new HashSet<Component>(); // of Components with Tunnel factory
  private TunnelListener tunnelListener = new TunnelListener();
  private HashSet<Component> pulls = new HashSet<Component>(); // of Components with PullResistor factory

  final CircuitPoints points = new CircuitPoints();
  private Bounds bounds = Bounds.EMPTY_BOUNDS;

  private volatile BundleMap masterBundleMap = null;

  CircuitWires() {
  }

  // NOTE: this could be made much more efficient in most cases to
  // avoid voiding the bundle map.
  /*synchronized*/ boolean add(Component comp) {
    boolean added = true;
    if (comp instanceof Wire) {
      added = addWire((Wire) comp);
    } else if (comp instanceof Splitter) {
      splitters.add((Splitter) comp);
    } else {
      Object factory = comp.getFactory();
      if (factory instanceof Tunnel) {
        tunnels.add(comp);
        comp.getAttributeSet().addAttributeWeakListener(null, tunnelListener);
      } else if (factory instanceof PullResistor) {
        pulls.add(comp);
        comp.getAttributeSet().addAttributeWeakListener(null, tunnelListener);
      }
    }
    if (added) {
      points.add(comp);
      voidBundleMap();
    }
    return added;
  }

  /*synchronized*/ void add(Component comp, EndData end) {
    points.add(comp, end);
    voidBundleMap();
  }

  private boolean addWire(Wire w) {
    boolean added = wires.add(w);
    if (!added)
      return false;

    if (bounds != Bounds.EMPTY_BOUNDS) { // update bounds
      bounds = bounds.add(w.e0).add(w.e1);
    }
    return true;
  }

  // To be called by getBundleMap only
  private void computeBundleMap(BundleMap ret) {
    // create bundles corresponding to wires and tunnels
    connectWires(ret);
    connectTunnels(ret);
    connectPullResistors(ret);

    // merge any WireBundle objects united by previous steps
    for (Iterator<WireBundle> it = ret.getBundles().iterator(); it.hasNext();) {
      WireBundle b = it.next();
      WireBundle bpar = b.find();
      if (bpar != b) { // b isn't group's representative
        for (Location pt : b.tempPoints)
          ret.setBundleAt(pt, bpar);
        bpar.tempPoints.addAll(b.tempPoints);
        bpar.addPullValue(b.getPullValue());
        it.remove();
      }
    }

    // make a WireBundle object for each end of a splitter
    for (Splitter spl : splitters) {
      List<EndData> ends = new ArrayList<EndData>(spl.getEnds());
      for (EndData end : ends) {
        Location p = end.getLocation();
        WireBundle pb = ret.createBundleAt(p);
        pb.setWidth(end.getWidth(), p);
      }
    }

    // set the width for each bundle whose size is known
    // based on components
    for (Location p : ret.getBundlePoints()) {
      WireBundle pb = ret.getBundleAt(p);
      BitWidth width = points.getWidth(p);
      if (width != BitWidth.UNKNOWN) {
        pb.setWidth(width, p);
      }
    }

    // determine the bundles at the end of each splitter
    for (Splitter spl : splitters) {
      List<EndData> ends = new ArrayList<EndData>(spl.getEnds());
      int index = -1;
      for (EndData end : ends) {
        index++;
        Location p = end.getLocation();
        WireBundle pb = ret.getBundleAt(p);
        if (pb != null) {
          pb.setWidth(end.getWidth(), p);
          spl.wire_data.end_bundle[index] = pb;
        }
      }
    }
    
    // finish constructing the bundles, start constructing the threads
    for (WireBundle b : ret.getBundles()) {
      b.xpoints = b.tempPoints.toArray(new Location[b.tempPoints.size()]);
      b.tempPoints = null;
      BitWidth width = b.getWidth();
      if (width != BitWidth.UNKNOWN) {
        int n = width.getWidth();
        b.threads = new WireThread[n];
        for (int i = 0; i < n; i++)
          b.threads[i] = new WireThread();
      }
    }

    // unite threads going through splitters
    for (Splitter spl : splitters) {
      synchronized (spl) {
        SplitterAttributes spl_attrs = (SplitterAttributes) spl.getAttributeSet();
        byte[] bit_end = spl_attrs.bit_end;
        SplitterData spl_data = spl.wire_data;
        WireBundle from_bundle = spl_data.end_bundle[0];
        if (from_bundle == null || !from_bundle.isValid())
          continue;

        for (int i = 0; i < bit_end.length; i++) {
          int j = bit_end[i];
          if (j > 0) {
            int thr = spl.bit_thread[i];
            WireBundle to_bundle = spl_data.end_bundle[j];
            WireThread[] to_threads = to_bundle.threads;
            if (to_threads != null && to_bundle.isValid()) {
              WireThread[] from_threads = from_bundle.threads;
              if (i >= from_threads.length) {
                throw new ArrayIndexOutOfBoundsException(
                    "from " + i + " of "
                    + from_threads.length);
              }
              if (thr >= to_threads.length) {
                throw new ArrayIndexOutOfBoundsException("to "
                    + thr + " of " + to_threads.length);
              }
              from_threads[i].unite(to_threads[thr]);
            }
          }
        }
      }
    }

    // merge any threads united by previous step
    for (WireBundle b : ret.getBundles()) {
      if (b.threads != null) {
        for (int i = 0; i < b.threads.length; i++) {
          WireThread thr = b.threads[i].getRepresentative();
          b.threads[i] = thr;
          thr.addBundlePosition(i, b);
        }
      }
    }

    // finish constructing the threads
    for (WireBundle b : ret.getBundles()) {
      if (b.threads != null) {
        for (WireThread t: b.threads)
          t.finishConstructing();
      }
    }

    // All bundles are made, all threads are now sewn together.

    // Record all component locations so they can be marked as dirty when this
    // wire bundle map is used to initialize a new State.
    ret.allLocations.addAll(points.getAllLocations());

    // Record all interesting component (non-wire, non-splitter) locations so
    // they can be used to filter out uninteresting points when this wire bundle
    // map is used to initialize a new State. We also need to know which
    // interesting components are at those locations.
    for (Location p : ret.allLocations) {
      ArrayList<Component> a = null;
      for (Component comp : points.getComponents(p)) {
        if ((comp instanceof Wire) || (comp instanceof Splitter))
          continue;
        if (a == null)
          a = new ArrayList<Component>();
        a.add(comp);
      }
      if (a != null)
        ret.componentsAtLocations.put(p, a);
    }
    
    // Compute the exception set before leaving.
    Collection<WidthIncompatibilityData> exceptions = points
        .getWidthIncompatibilityData();
    if (exceptions != null && exceptions.size() > 0) {
      for (WidthIncompatibilityData wid : exceptions) {
        ret.addWidthIncompatibilityData(wid);
      }
    }
    for (WireBundle b : ret.getBundles()) {
      WidthIncompatibilityData e = b.getWidthIncompatibilityData();
      if (e != null)
        ret.addWidthIncompatibilityData(e);
    }
  }

  private void connectPullResistors(BundleMap ret) {
    for (Component comp : pulls) {
      Location loc = comp.getEnd(0).getLocation();
      WireBundle b = ret.getBundleAt(loc);
      if (b == null) {
        b = ret.createBundleAt(loc);
        b.tempPoints.add(loc);
        ret.setBundleAt(loc, b);
      }
      Instance instance = Instance.getInstanceFor(comp);
      b.addPullValue(PullResistor.getPullValue(instance));
    }
  }

  private void connectTunnels(BundleMap ret) {
    // determine the sets of tunnels
    HashMap<String, ArrayList<Location>> tunnelSets = new HashMap<String, ArrayList<Location>>();
    for (Component comp : tunnels) {
      String label = comp.getAttributeSet().getValue(StdAttr.LABEL);
      label = label.trim();
      if (!label.equals("")) {
        ArrayList<Location> tunnelSet = tunnelSets.get(label);
        if (tunnelSet == null) {
          tunnelSet = new ArrayList<Location>(3);
          tunnelSets.put(label, tunnelSet);
        }
        tunnelSet.add(comp.getLocation());
      }
    }

    // now connect the bundles that are tunnelled together
    for (ArrayList<Location> tunnelSet : tunnelSets.values()) {
      WireBundle foundBundle = null;
      Location foundLocation = null;
      for (Location loc : tunnelSet) {
        WireBundle b = ret.getBundleAt(loc);
        if (b != null) {
          foundBundle = b;
          foundLocation = loc;
          break;
        }
      }
      if (foundBundle == null) {
        foundLocation = tunnelSet.get(0);
        foundBundle = ret.createBundleAt(foundLocation);
      }
      for (Location loc : tunnelSet) {
        if (loc != foundLocation) {
          WireBundle b = ret.getBundleAt(loc);
          if (b == null) {
            foundBundle.tempPoints.add(loc);
            ret.setBundleAt(loc, foundBundle);
          } else {
            b.unite(foundBundle);
          }
        }
      }
    }
  }

  private void connectWires(BundleMap ret) {
    // make a WireBundle object for each tree of connected wires
    for (Wire w : wires) {
      WireBundle b0 = ret.getBundleAt(w.e0);
      if (b0 == null) {
        WireBundle b1 = ret.createBundleAt(w.e1);
        b1.tempPoints.add(w.e0);
        ret.setBundleAt(w.e0, b1);
      } else {
        WireBundle b1 = ret.getBundleAt(w.e1);
        if (b1 == null) { // t1 doesn't exist
          b0.tempPoints.add(w.e1);
          ret.setBundleAt(w.e1, b0);
        } else {
          b1.unite(b0); // unite b0 and b1
        }
      }
    }
  }

  static Value getBusValue(CircuitState state, Location loc) {
    State s = state.getWireData();
    if (s == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    ValuedBus vb = s.busAt.get(loc);
    if (vb == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    Value v = vb.val;
    if (v == null)
      return Value.NIL; // return state.getValue(loc); // fallback, probably wrong, who cares
    return v;
  }

  void draw(ComponentDrawContext context, Collection<Component> hidden) {
    boolean showState = context.getShowState();
    CircuitState state = context.getCircuitState();
    Graphics2D g = (Graphics2D)context.getGraphics();
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    WireSet highlighted = context.getHighlightedWires();

    BundleMap bmap = getBundleMap();
    boolean isValid = bmap.isValid();
    if (hidden == null || hidden.size() == 0) {
      for (Wire w : wires) {
        Location s = w.e0;
        Location t = w.e1;
        WireBundle wb = bmap.getBundleAt(s);
        if (!wb.isValid()) {
          g.setColor(Value.WIDTH_ERROR_COLOR);
        } else if (showState) {
          if (!isValid)
            g.setColor(Value.NIL_COLOR);
          else
            g.setColor(getBusValue(state, s).getColor());
        } else {
          g.setColor(Color.BLACK);
        }
        if (highlighted.containsWire(w)) {
          int width;
          if (wb.isBus())
            width = Wire.HIGHLIGHTED_WIDTH_BUS;
          else
            width = Wire.HIGHLIGHTED_WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());

          Stroke oldStroke = g.getStroke();
          g.setStroke(Wire.HIGHLIGHTED_STROKE);
          g.setColor(Color.MAGENTA);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          g.setStroke(oldStroke);
        } else {
          int width;
          if (wb.isBus())
            width = Wire.WIDTH_BUS;
          else
            width = Wire.WIDTH;
          GraphicsUtil.switchToWidth(g, width);
          g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
        }
      }

      for (Location loc : points.getAllLocations()) {
        if (points.getComponentCount(loc) > 2) {
          WireBundle wb = bmap.getBundleAt(loc);
          if (wb != null) {
            if (!wb.isValid()) {
              g.setColor(Value.WIDTH_ERROR_COLOR);
            } else if (showState) {
              if (!isValid)
                g.setColor(Value.NIL_COLOR);
              else
                g.setColor(state.getValue(loc).getColor());
            } else {
              g.setColor(Color.BLACK);
            }
            int radius;
            if (highlighted.containsLocation(loc)) {
              radius = wb.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH;
            } else {
              radius = wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH;
            }
            radius = (int)(radius * Wire.DOT_MULTIPLY_FACTOR);
            g.fillOval(loc.getX() - radius, loc.getY() - radius, radius*2, radius*2);
          }
        }
      }
    } else {
      for (Wire w : wires) {
        if (!hidden.contains(w)) {
          Location s = w.e0;
          Location t = w.e1;
          WireBundle wb = bmap.getBundleAt(s);
          if (!wb.isValid()) {
            g.setColor(Value.WIDTH_ERROR_COLOR);
          } else if (showState) {
            if (!isValid)
              g.setColor(Value.NIL_COLOR);
            else
              g.setColor(getBusValue(state, s).getColor());
          } else {
            g.setColor(Color.BLACK);
          }
          if (highlighted.containsWire(w)) {
            GraphicsUtil.switchToWidth(g, Wire.WIDTH + 2);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
            GraphicsUtil.switchToWidth(g, Wire.WIDTH);
          } else {
            if (wb.isBus())
              GraphicsUtil.switchToWidth(g, Wire.WIDTH_BUS);
            else
              GraphicsUtil.switchToWidth(g, Wire.WIDTH);
            g.drawLine(s.getX(), s.getY(), t.getX(), t.getY());
          }
        }
      }

      // this is just an approximation, but it's good enough since
      // the problem is minor, and hidden only exists for a short
      // while at a time anway.
      for (Location loc : points.getAllLocations()) {
        if (points.getComponentCount(loc) > 2) {
          int icount = 0;
          for (Component comp : points.getComponents(loc)) {
            if (!hidden.contains(comp))
              ++icount;
          }
          if (icount > 2) {
            WireBundle wb = bmap.getBundleAt(loc);
            if (wb != null) {
              if (!wb.isValid()) {
                g.setColor(Value.WIDTH_ERROR_COLOR);
              } else if (showState) {
                if (!isValid)
                  g.setColor(Value.NIL_COLOR);
                else
                  g.setColor(getBusValue(state, loc).getColor());
              } else {
                g.setColor(Color.BLACK);
              }
              int radius;
              if (highlighted.containsLocation(loc)) {
                radius = wb.isBus() ? Wire.HIGHLIGHTED_WIDTH_BUS : Wire.HIGHLIGHTED_WIDTH;
              } else {
                radius = wb.isBus() ? Wire.WIDTH_BUS : Wire.WIDTH;
              }
              radius = (int)(radius * Wire.DOT_MULTIPLY_FACTOR);
              g.fillOval(loc.getX() - radius, loc.getY() - radius, radius*2, radius*2);
            }
          }
        }
      }
    }
  }

  // There are only two threads that need to use the bundle map, I think:
  // the AWT event thread, and the simulation worker thread.
  // AWT does modifications to the components and wires, then voids the
  // masterBundleMap, and eventually recomputes a new map (if needed) during
  // painting. AWT sometimes locks a splitter, then changes components and
  // wires.
  // Computing a new bundle map requires both locking splitters and touching
  // the components and wires, so to avoid deadlock, only the AWT should create
  // the new bundle map. The bundle map is (essentially, if not entirely)
  // read-only once it is fully constructed.
  // The simulation thread never creates a new bundle map. On the other hand,
  // the simulation thread creates the State objects for each simulated instance
  // of the circuit, and each State duplicates data from the bundle map.

  private class BundleMapGetter implements Runnable {
    BundleMap result;
    public void run() {
      result = getBundleMap();
    }
  }

  /*synchronized*/ private BundleMap getBundleMap() {
    BundleMap ret = masterBundleMap; // volatile read by AWT or simulation thread
    if (ret != null)
      return ret;
    if (SwingUtilities.isEventDispatchThread()) {
      // AWT event thread.
      ret = new BundleMap();
      try {
        computeBundleMap(ret);
        masterBundleMap = ret; // volatile write by AWT thread
      } catch (Exception t) {
        ret.invalidate();
        System.err.println(t.getLocalizedMessage());
      }
      return ret;
    } else {
      // Simulation thread.
      try {
        BundleMapGetter awtThread = new BundleMapGetter();
        SwingUtilities.invokeAndWait(awtThread);
        return awtThread.result;
      } catch (Exception t) {
        System.err.println(t.getLocalizedMessage());
        ret = new BundleMap();
        ret.invalidate();
        return ret;
      }
    }
  }

  Iterator<? extends Component> getComponents() {
    return IteratorUtil.createJoinedIterator(splitters.iterator(),
        wires.iterator());
  }

  BitWidth getWidth(Location q) {
    BitWidth det = points.getWidth(q);
    if (det != BitWidth.UNKNOWN)
      return det;

    BundleMap bmap = getBundleMap();
    if (!bmap.isValid())
      return BitWidth.UNKNOWN;
    WireBundle qb = bmap.getBundleAt(q);
    if (qb != null && qb.isValid())
      return qb.getWidth();

    return BitWidth.UNKNOWN;
  }

  Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
    return getBundleMap().getWidthIncompatibilityData();
  }

  Bounds getWireBounds() {
    Bounds bds = bounds;
    if (bds == Bounds.EMPTY_BOUNDS) {
      bds = recomputeBounds();
    }
    return bds;
  }

  WireBundle getWireBundle(Location query) {
    BundleMap bmap = getBundleMap();
    return bmap.getBundleAt(query);
  }

  Set<Wire> getWires() {
    return wires;
  }

  List<Wire> getWiresTouching(Location loc) {
    ArrayList<Wire> list = null;
    for (Wire w : wires) {
      if (!w.contains(loc))
        continue;
      if (list == null)
        list = new ArrayList<Wire>();
      list.add(w);
    }
    return list == null ? Collections.emptyList() : list;
  }

  WireSet getWireSet(Wire start) {
    WireBundle bundle = getWireBundle(start.e0);
    if (bundle == null)
      return WireSet.EMPTY;
    HashSet<Wire> wires = new HashSet<Wire>();
    for (Location loc : bundle.xpoints) {
      wires.addAll(points.getWires(loc));
    }
    return new WireSet(wires);
  }

  // boolean isMapVoided() {
  //   return masterBundleMap == null; // volatile read by simulation thread
  // }

  void propagate(CircuitState circState, ArrayList<Location> dirtyPoints, ArrayList<Value> newVals) {
    BundleMap map = getBundleMap();
    ArrayList<WireThread> dirtyThreads = new ArrayList<>();

    // get state, or create a new one if current state is outdated
    State s = circState.getWireData();
    if (s == null || s.bundleMap != map) {
      // if it is outdated, we need to compute for all threads
      s = new State(circState, map);
      circState.setWireData(s);
      // note: all buses are already marked as dirty.
      // But we need to mark all points as dirty as well
      dirtyPoints.addAll(map.allLocations);
      for (Location p : map.allLocations)
        newVals.add(circState.getComponentOutputAt(p));
    }

    int npoints = dirtyPoints.size();
    for (int k = 0; k < npoints; k++) { // for each point of interest
      Location p = dirtyPoints.get(k);
      Value val = newVals.get(k);
      ValuedBus vb = s.busAt.get(p);
      if (vb == null) {
        // point is not wired: just set that point's value and be done
        circState.setValueByWire(p, val);
      } else if (vb.threads == null) {
        // point is wired to a threadless (e.g. invalid-width) bundle:
        // propagate NIL across entire bundle
        if (vb.dirty)
          s.markClean(vb);
        // for (Location buspt : vb.componentPoints)
        //   circState.setValueByWire(buspt, Value.NIL);
        int n = vb.componentPoints.length;
        for (int i = 0; i < n; i++) {
          Location buspt = vb.componentPoints[i];
          Component[] affected = vb.componentsAffected[i];
          circState.setValueByWire(buspt, Value.NIL, affected);
        }
      } else {
        // common case... it is wired to a normal bus: update the stored value
        // of this point on the bus, mark the bus as dirty, and mark as dirty
        // any related buses.
        for (int i = 0; i < vb.componentPoints.length; i++) {
          if (vb.componentPoints[i].equals(p)) {
            Value old = vb.valAtPoint[i];
            if ((val == null || val == Value.NIL) &&
                (old == null || old == Value.NIL))
              break; // ignore, both old and new are NIL
            if (val != null && old != null && val.equals(old))
              break; // ignore, both old and new are same non-NIL value
            vb.valAtPoint[i] = val;
            vb.valAtPointSum = null;
            if (!vb.dirty) {
              s.markDirty(vb);
              if (vb.dependentBuses != null) {
                for (ValuedBus dep : vb.dependentBuses)
                  if (!dep.dirty)
                    s.markDirty(dep);
              }
            }
            break;
          }
        }
      }
    }

    if (s.numDirty <= 0)
      return;

    // recompute valAtPointSum for each dirty bus
    for (int i = 0; i < s.numDirty; i++) {
      ValuedBus vb = s.buses[i];
      vb.valAtPointSum = Value.combineLikeWidths(vb.valAtPoint);
    }

    // recompute thread values for all threads passing through dirty buses,
    // recompute aggregate bus values for all dirty buses,
    // and post those notifications to all bus points
    for (int i = 0; i < s.numDirty; i++) {
      ValuedBus vb = s.buses[i];
      Value val = vb.val = vb.recalculate();
      vb.dirty = false;
      int n = vb.componentPoints.length;
      for (int j = 0; j < n; j++) {
        Location p = vb.componentPoints[j];
        Component[] affected = vb.componentsAffected[j];
        circState.setValueByWire(p, val, affected);
      }
    }
    s.numDirty = 0;
  }

  private Bounds recomputeBounds() {
    Iterator<Wire> it = wires.iterator();
    if (!it.hasNext()) {
      bounds = Bounds.EMPTY_BOUNDS;
      return Bounds.EMPTY_BOUNDS;
    }

    Wire w = it.next();
    int xmin = w.e0.getX();
    int ymin = w.e0.getY();
    int xmax = w.e1.getX();
    int ymax = w.e1.getY();
    while (it.hasNext()) {
      w = it.next();
      int x0 = w.e0.getX();
      if (x0 < xmin)
        xmin = x0;
      int x1 = w.e1.getX();
      if (x1 > xmax)
        xmax = x1;
      int y0 = w.e0.getY();
      if (y0 < ymin)
        ymin = y0;
      int y1 = w.e1.getY();
      if (y1 > ymax)
        ymax = y1;
    }
    Bounds bds = Bounds.create(xmin, ymin, xmax - xmin + 1, ymax - ymin + 1);
    bounds = bds;
    return bds;
  }

  /*synchronized*/ void remove(Component comp) {
    if (comp instanceof Wire) {
      removeWire((Wire) comp);
    } else if (comp instanceof Splitter) {
      splitters.remove(comp);
    } else {
      Object factory = comp.getFactory();
      if (factory instanceof Tunnel) {
        tunnels.remove(comp);
        comp.getAttributeSet().removeAttributeWeakListener(null, tunnelListener);
      } else if (factory instanceof PullResistor) {
        pulls.remove(comp);
        comp.getAttributeSet().removeAttributeWeakListener(null, tunnelListener);
      }
    }
    points.remove(comp);
    voidBundleMap();
  }

  /*synchronized*/ void remove(Component comp, EndData end) {
    points.remove(comp, end);
    voidBundleMap();
  }

  private void removeWire(Wire w) {
    boolean removed = wires.remove(w);
    if (!removed)
      return;

    if (bounds != Bounds.EMPTY_BOUNDS) {
      // bounds is valid - invalidate if endpoint on border
      Bounds smaller = bounds.expand(-2);
      if (!smaller.contains(w.e0) || !smaller.contains(w.e1)) {
        bounds = Bounds.EMPTY_BOUNDS;
      }
    }
  }

  /*synchronized*/ void replace(Component comp, EndData oldEnd, EndData newEnd) {
    points.remove(comp, oldEnd);
    points.add(comp, newEnd);
    voidBundleMap();
  }

  private void voidBundleMap() {
    // This should really only be called by AWT thread, but main() also
    // calls it during startup. It should not be called by the simulation
    // thread.
    masterBundleMap = null; // volatile write by AWT thread (and sometimes main/startup)
  }
}
