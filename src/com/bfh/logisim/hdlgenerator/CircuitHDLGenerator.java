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

package com.bfh.logisim.hdlgenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.bfh.logisim.library.DynamicClock;
import com.bfh.logisim.netlist.CorrectLabel;
import com.bfh.logisim.netlist.Net;
import com.bfh.logisim.netlist.Netlist;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.netlist.Path;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;

public class CircuitHDLGenerator extends HDLGenerator {

	private Circuit circ;
  private Netlist _circNets; // netlist for this circ; _nets is for our parent

	public CircuitHDLGenerator(HDLCTX ctx, Circuit circ) {
    super(ctx, "circuit", "Circuit_${LABEL}", "i_Circ");
		this.circ = circ;
		this._circNets = circ.getNetList();
    
    // Normal ports
    for (Component pin : circ.getNonWires()) {
      if (!(pin.getFactory() instanceof Pin))
        continue;
      String label = NetlistComponent.labelOf(pin);
      EndData end = pin.getEnd(0);
      if (end.isInput())
        inPorts.add(label, end.getWidth().getWidth(), -1, null);
      else
        outPorts.add(label, end.getWidth().getWidth(), -1, null);
    }

    // Note: Other setup is deferred until later, because it depends on Netlist
    // state that won't be ready until during or after global
    // validation/analysis of the entire design hierarchy and all Netslists.
  }

  private boolean finishedSetup = false;
  private void setup() {
    if (finishedSetup)
      return;
    finishedSetup = true;

    // hidden ports
    Netlist.Int3 hidden = _circNets.numHiddenBits();
    inPorts.addVector("LOGISIM_HIDDEN_FPGA_INPUT", hidden.in);
    inOutPorts.addVector("LOGISIM_HIDDEN_FPGA_INOUT", hidden.inout);
    outPorts.addVector("LOGISIM_HIDDEN_FPGA_OUTPUT", hidden.out);

    // global clock buses
    for (int i = 0; i < _circNets.clockbus.shapes().size(); i++)
			inPort.put(ClockHDLGenerator.CLK_TREE_NET_ + i, ClockHDLGenerator.CLK_TREE_WIDTH, -1, null);

    // dynamic clock
    NetlistComponent dynClock = _circNets.dynamicClock();
    if (dynClock != null)
      outPorts.add("LOGISIM_DYNAMIC_CLOCK_OUT",
          dynClock.original.getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth(), -1, null);

    // Nets
    for (Net net: _circNets.nets)
      wires.add(net.name, net.width);
  }

  // For a given external end corresponding to some internal Pin, return the
  // internal net connected to that Pin.
  public Net getInternalNetFor(NetlistComponent comp, int end) {
    // The NetlistComponent is embedded in some parent circuit, and the end
    // refers to the port ordering as seen from that parent. This matches the
    // ordering used by NetlistComponent, which uses the ordering defined by
    // Component.getEnds(), which in turn comes from CircuitAttributes, which is
    // based ultimately on the appearance and the sorting order of the pins
    // within the appearance (not on the canvas). Here we need to figure out
    // which Pin underlies that end, so that we can figure out which internal
    // net it belongs to. Forunately, SubcircuitFactory tucks the needed
    // correspondence away inside the circuit attributes.
    CircuitAttributes attrs = (CircuitAttributes)comp.original.getAttributeSet();
    Component pin = attrs.getPinInstances()[end].getComponent();
    return getPortMappingForPin(pin);
  }

  // For a given internal Pin, get the internal net conncted to it.
  public Net getPortMappingForPin(Component pin) {
    return _circNets.netAt.get(pin.getEnd(0).getLocation());
  }

  // For a given internal Pin, get the external end number.
	private int getEndIndex(NetlistComponent comp, NetlistComponent pin) {
    CircuitAttributes attrs = (CircuitAttributes)comp.original.getAttributeSet();
    Instance[] ports = attrs.getPinInstances();
    for (int i = 0; i < ports.length; i++)
      if (ports[i].getComponent() == pin)
        return i;
    return -1;
	}

  // For a given internal Pin, get a mapping to an externally connected Net
  private void getMapForCircuitPort(Hdl.Map map, NetlistComponent comp, NetlistComponent pin, boolean isOutput) {
    int endid = getEndIndex(comp, pin);
    if (endid < 0) {
      _err.AddFatalError("INTERNAL ERROR: Missing sub-circuit port '%s'.", pin.label());
      return;
    }
    int w = pin.original.getEnd(0).getWidth().getWidth();
    PortInfo p = new PortInfo(pin.label(), w, endid, isOutput ? null : false);
    super.getPortMappings(map, comp, p);
  }

	@Override
  protected Hdl.Map getPortMappings(NetlistComponent comp) {
    Hdl.Map map = new Hdl.Map(_lang, _err);
		if (comp != null) {
      // Mappings for a subcircuit within the circuit design under tests
			SubcircuitFactory sub = (SubcircuitFactory) comp.original.getFactory();

      // Clock trees
      for (int i = 0; i < _circNets.clockbus.shapes().size(); i++) {
        String clkTree = ClockHDLGenerator.CLK_TREE_NET_ + i;
        map.add(clkTree, clkTree);
			}

      // Dynamic clock - should not be present in subcircuit

      // Hidden ports
      NetlistComponent.Range3 r = comp.getLocalHiddenPortIndices();
			if (r.in.end >= r.in.start)
        map.add("LOGISIM_HIDDEN_FPGA_INPUT", "LOGISIM_HIDDEN_FPGA_INPUT", r.in.end, r.in.start);
			if (r.inout.end >= r.inout.start)
        map.add("LOGISIM_HIDDEN_FPGA_INOUT", "LOGISIM_HIDDEN_FPGA_INOUT", r.inout.end, r.inout.start);
			if (r.out.end >= r.out.start)
        map.add("LOGISIM_HIDDEN_FPGA_OUTPUT", "LOGISIM_HIDDEN_FPGA_OUTPUT", r.out.end, r.out.start);

      // Normal ports
      for (NetlistComponent pin : _circNets.inpins)
        getMapForCircuitPort(map, comp, pin, false);
      for (NetlistComponent pin : _circNets.outpins)
        getMapForCircuitPort(map, comp, pin, true);

		} else {
      // Mappings for the circuit design under test, within the TopLevelHDLGenerator circuit
      
      // Clock trees
      for (int i = 0; i < _circNets.clockbus.shapes().size(); i++) {
        String clkTree = ClockHDLGenerator.CLK_TREE_NET_ + i;
        map.add(clkTree, "s_" + clkTree); // fixme: why different from above?
			}

      // Dynamic clock
      NetlistComponent dynClock = _circNets.dynamicClock();
      if (dynClock != null)
        map.add("LOGISIM_DYNAMIC_CLOCK_OUT", "s_LOGISIM_DYNAMIC_CLOCK");

      // Hidden ports
      // note: Toplevel has direct connection and inversions for InOut ports
      NetlistComponent.Range3 r = comp.getLocalHiddenPortIndices();
			if (r.in.end >= r.in.start)
        map.add("LOGISIM_HIDDEN_FPGA_INPUT", "s_LOGISIM_HIDDEN_FPGA_INPUT", r.in.end, r.in.start);
			if (r.inout.end >= r.inout.start)
        map.add("LOGISIM_HIDDEN_FPGA_INOUT", "LOGISIM_HIDDEN_FPGA_INOUT", r.inout.end, r.inout.start);
			if (r.out.end >= r.out.start)
        map.add("LOGISIM_HIDDEN_FPGA_OUTPUT", "s_LOGISIM_HIDDEN_FPGA_OUTPUT", r.out.end, r.out.start);
      
      // Normal ports
      for (NetlistComponent pin : _circNets.inpins)
        map.put(pin.label(), "s_" + pin.label());
      for (NetlistComponent pin : _circNets.outpins)
        map.put(pin.label(), "s_" + pin.label());
		}
    return map;
	}

  // Second-level entry point: recursively write all HDL files for the project.
  public boolean writeAllHDLFiles(String rootDir) {
		if (!rootDir.endsWith(File.separator))
			rootDir += File.separator;
    return writeAllHDLFiles(rootDir, new HashSet<String>(), new Path(circ));
  }

	private boolean writeAllHDLFiles(String rootDir, HashSet<String> writtenComponents, Path path) {
		_circNets.currentPath = path;
    try {
      // Generate this circuit first
      String name = CorrectLabel.getCorrectLabel(circ.getName());
      if (writtenComponents.contains(name))
        return;
      if (!writeHDLFiles(rootDir))
        return false;
      writtenComponents.add(name);

      // Generate this circuit's normal components next
      for (NetlistComponent comp : _circNets.components) {
        if (comp.original.getFactory() instanceof SubcircuitFactory)
          continue;
        HDLSupport g = comp.hdlSupport;
        if (g == null)
          return false;
        String name = g.getComponentName();
        if (!writtenComponents.contains(name) && !g.inlined) {
          if (!g.writeHDLFiles(rootDir))
            return false;
          writtenComponents.add(name);
        }
      }

      // Recurse for subcircuits last
      for (NetlistComponent subcirc : _circNets.subcircuits) {
        CircuitHDLGenerator g = (CircuitHDLGenerator)subcirc.hdlSupport;
        if (g == null)
          return;
        Path subpath = path.extend(subcirc);
        if (!g.writeAllHDLFiles(rootDir, writtenComponents, subpath))
          return false;
      }

      return true;
    } finally {
      _circNets.currentPath = null;
    }
	}

  @Override
  public boolean writeArchitecture(String rootDir) {
    // Instead of using the circuit as defined within logisim, we can substitute
    // an external user-defined VHDL implementation.
    if (circ.getStaticAttributes().getValue(CircuitAttributes.CIRCUIT_IS_VHDL_BOX)) {
      String name = CorrectLabel.getCorrectLabel(circ.getName());
      if (!FileWriter.CopyArchitecture(
						circ.getStaticAttributes().getValue(CircuitAttributes.CIRCUIT_VHDL_PATH),
            rootDir + subdir, name, _err, _lang)) {
					return false;
      }
    } else {
      super.writeArchitecture(rootDir);
    }
  }

	@Override
	public void generateVhdlComponentDeclarations(Hdl out) {
    generateDeclarations(out, _circNets.components);
  }

  private void generateDeclarations(Hdl out, ArrayList<NetlistComponent> components) {
		HashSet<String> done = new HashSet<>();
		for (NetlistComponent comp: components) {
      HDLSupport g = comp.hdlSupport;
      if (g == null)
        continue;
			String name = g.getComponentName();
			if (done.contains(name) || g.inlined)
        continue;
      g.generateVhdlComponentDeclarations(out);
      done.add(name);
		}
	}
    
  private void generatePortAssignments(Hdl out, NetlistComponent pin, boolean isOutput) {
    Net net = getPortMappingForPin(pin);
    if (net == null && isOutput)
      _err.AddSevereWarning("INTERNAL ERROR: Output pin '%s' of circuit '%s' has "
          + " no driving signal.", pin.label(), circ.getName());
    else if (net == null)
      return; // input pin not driving anything, no error
    else if (isOutput)
      out.assign(net.name, pin.label());
    else
      out.assign(pin.label(), net.name);
  }

	@Override
  protected void generateBehavior(Hdl out) {

    generateWiring(out);

		// Connect each HDL port to the net connected to the corresponding circuit pin.
    // Note: This is almost exactly the same as getPortMappings(), but
    // creating signal assignments instead of port mappings, and also with
    // different logic for handling unconnected pins.
    for (NetlistComponent pin : _circNets.inpins)
      generatePortAssignments(out, pin, false);
    for (NetlistComponent pin : _circNets.outpins)
      generatePortAssignments(out, pin, true);

    // Dynamic clock - DynamicClockHDLGenerator will connected directly to output port

    // Handle normal components and subcircuits.
		HashMap<String, Long> ids = new HashMap<>();
		for (NetlistComponent comp: _circNets.components) {
      HDLSupport g = comp.hdlSupport;
      if (g == null)
        continue;
      if (g.inlined) {
        g.generateInlinedCode(out, comp);
      } else {
        String prefix = g.getInstanceNamePrefix();
        long id = ids.getOrDefault(prefix, 0) + 1;
        ids.put(prefix, id);
        g.generateComponentInstance(out, id, comp);
      }
    }
	}

  private boolean sequential(Net.IndirectSource prev, Net.IndirectSource cur) {
    return prev != null && cur != null && prev.net == cur.net && prev.bit+1 == cur.bit;
  }

  private void emit(Hdl out, Net dest, int b, Net.IndirectSource last, int n) {
    if (last != null || n > 0)
      out.assign(dest.slice(b, b-n+1), last.net.slice(last.net.bit, last.net.bit-n+1));
  }

	private void generateWiring(Hdl out) {
		for (Net net : _circNets.nets) {
      Net.IndirectSource prev = null;
      int w = net.indirectSource.length;
      int n = 0;
      for (int b = 0; b < w; b++) {
        Net.IndirectSource src = net.indirectSource[b];
        if (!sequential(prev, src)) {
          emit(out, net, b-1, prev, n);
          n = 0;
        }
        prev = src;
        n++;
      }
      emit(out, net, w-1, prev, n);
    }
  }

  @Override
  protected boolean hdlDependsOnCircuitState() { // for NVRAM
    for (NetlistComponent comp : _circNets.components) {
      HDLSupport g = comp.hdlSupport;
      if (g != null && g.hdlDependsOnCircuitState())
        return true;
    }
    return false;
  }

  @Override
  public boolean writeAllHDLThatDependsOn(CircuitState cs, NetlistComponent shadow,
      String rootDir) { // for NVRAM
    Path path;
    if (shadow == null) {
      path = new Path(circ);
    } else {
      path = shadow.currentPath;
      cs = cs.getData(shadow.original);
    }
    for (NetlistComponent comp : _circNets.components) {
      HDLSupport g = comp.hdlSupport;
      if (g == null)
        continue;
      try {
        comp.currentPath = path.extend(comp);
        CircuitState substate = cs.getData(comp.original);
        if (!g.writeAllHDLThatDependsOn(cs, comp, rootDir))
          return true;
      } finally {
        comp.currentPath = null;
      }
    }
  }
}
