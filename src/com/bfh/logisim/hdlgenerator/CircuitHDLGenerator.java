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

import com.bfh.logisim.netlist.ConnectionEnd;
import com.bfh.logisim.netlist.ConnectionPoint;
import com.bfh.logisim.netlist.CorrectLabel;
import com.bfh.logisim.netlist.Net;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.library.DynamicClock;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class CircuitHDLGenerator extends HDLGenerator {

	private Circuit circ;
  private Netlist _circNets; // netlist for this circ; _nets is for our parent

	public CircuitHDLGenerator(String lang, FPGAReport err, Netlist nets, char vendor, Circuit circ) {
    super(new HDLCTX(lang, err, nets, AttributeSets.EMPTY, vendor), "circuit", "Circuit_${LABEL}", "i_Circ");
		this.circ = circ;
		this._circNets = circ.getNetList();

    // Clock trees
    for (int i = 0; i < _circNets.NumberOfClockTrees(); i++)
			inPort.put(ClockHDLGenerator.CLK_TREE_NET_ + i, ClockHDLGenerator.CLK_TREE_WIDTH);

    // Dynamic clock
    NetlistComponent dynClock = _circNets.GetDynamicClock();
    if (dynClock != null)
      outPorts.add("LOGISIM_DYNAMIC_CLOCK_OUT",
          dynClock.GetComponent().getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth(), -1, null);

    // Hidden ports
    addVectorPort(inPorts, "LOGISIM_HIDDEN_FPGA_INPUT", _circNets.NumberOfInputBubbles());
    addVectorPort(inOutPorts, "LOGISIM_HIDDEN_FPGA_INOUT", _circNets.NumberOfInOutBubbles());
    addVectorPort(outPorts, "LOGISIM_HIDDEN_FPGA_OUTPUT", _circNets.NumberOfOutputBubbles());

    // Normal ports
		for (int i = 0; i < _circNets.NumberOfInputPorts(); i++) {
			AttributeSet attrs = _circNets.GetInputPin(i).GetComponent().getAttributeSet();
      inPorts.add(CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL)), attrs.getValue(StdAttr.WIDTH).getWidth(), -1, null);
    }
		for (int i = 0; i < _circNets.NumberOfOutputPorts(); i++) {
			AttributeSet attrs = _circNets.GetInputPin(i).GetComponent().getAttributeSet();
      outPorts.add(CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL)), attrs.getValue(StdAttr.WIDTH).getWidth(), -1, null);
		}

    // Nets
    for (Net net: _circNets.GetAllNets()) {
      if (net.isBus())
				wires.add("s_LOGISIM_BUS_" + _circNets.GetNetId(net), 1);
      else
				wires.add("s_LOGISIM_NET_" + _circNets.GetNetId(net), net.BitWidth());
    }
	}

  private void addVectorPort(Arraylist<PortInfo> ports, String busname, int w) {
    // Note: the parens around "(1)" are a hack to force vector creation.
    // See: HDLGenerator and Hdl.typeForWidth().
    if (w > 0)
      ports.add(busname, w == 1 ? "(1)" : ""+w, -1, null);
  }

  // FIXME: this is convoluted, can't this be done earlier and more directly?
	public int GetEndIndex(NetlistComponent comp, String label, boolean IsOutputPort) {
		SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();
		for (int end = 0; end < comp.NrOfEnds(); end++) {
			if (comp.getEnd(end).IsOutputEnd() == IsOutputPort) {
				if (comp.getEnd(end).GetConnection((byte) 0).getChildsPortIndex()
            == sub.getSubcircuit().getNetList().GetPortInfo(label)) {
					return end;
				}
			}
		}
		return -1;
	}

  // FIXME: convoluted
  private void getMapForCircuitPort(Hdl.Map map, NetlistComponent pinComp, NetlistComponent comp, boolean isOutput) {
    AttributeSet attrs = pinComp.GetComponent().getAttributeSet();
    String name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    int endid = GetEndIndex(comp, name, isOutput);
    if (endid < 0) {
      _err.AddFatalError("INTERNAL ERROR: Missing sub-circuit port '%s'.", name);
      return;
    }
    PortInfo p = new PortInfo(name, attrs.getValue(StdAttr.WIDTH).getWidth(), endid, null);
    super.getPortMappings(map, comp, p);
  }

	@Override
  protected Hdl.Map getPortMappings(NetlistComponent comp) {
    Hdl.Map map = new Hdl.Map(_lang, _err);
		if (comp != null) {
      // Mappings for a subcircuit within the circuit design under tests
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent().getFactory();

      // Clock trees
      for (int i = 0; i < _circNets.NumberOfClockTrees(); i++) {
        String clkTree = ClockHDLGenerator.CLK_TREE_NET_ + i;
        map.add(clkTree, clkTree);
			}

      // Dynamic clock - should not be present in subcircuit

      // Hidden ports
			if (_circNets.NumberOfInputBubbles() > 0)
        map.add("LOGISIM_HIDDEN_FPGA_INPUT", "LOGISIM_HIDDEN_FPGA_INPUT",
					comp.GetLocalBubbleInputEndId(), comp.GetLocalBubbleInputStartId());
			if (_circNets.NumberOfInOutBubbles() > 0)
        map.add("LOGISIM_HIDDEN_FPGA_INOUT", "LOGISIM_HIDDEN_FPGA_INOUT",
					comp.GetLocalBubbleInOutEndId(), comp.GetLocalBubbleInOutStartId());
			if (_circNets.NumberOfOutputBubbles() > 0)
        map.add("LOGISIM_HIDDEN_FPGA_OUTPUT", "LOGISIM_HIDDEN_FPGA_OUTPUT",
					comp.GetLocalBubbleOutputEndId(), comp.GetLocalBubbleOutputStartId());

      // Normal ports
      for (int i = 0; i < _circNets.NumberOfInputPorts(); i++)
        getMapForCircuitPort(map, _cirNets.GetInputPin(i), comp, false);
      for (int i = 0; i < _circNets.NumberOfOutputPorts(); i++)
        getMapForCircuitPort(map, _cirNets.GetOutputPin(i), comp, true);

		} else {
      // Mappings for the circuit design under test, within the TopLevelHDLGenerator circuit
      
      // Clock trees
      for (int i = 0; i < _circNets.NumberOfClockTrees(); i++) {
        String clkTree = ClockHDLGenerator.CLK_TREE_NET_ + i;
        map.add(clkTree, "s_" + clkTree); // fixme: why different from above?
			}

      // Dynamic clock
      NetlistComponent dynClock = _circNets.GetDynamicClock();
      if (dynClock != null)
        map.add("LOGISIM_DYNAMIC_CLOCK_OUT", "s_LOGISIM_DYNAMIC_CLOCK");

      // Hidden ports
			if (_circNets.NumberOfInputBubbles() > 0)
				map.add("LOGISIM_HIDDEN_FPGA_INPUT", "s_LOGISIM_HIDDEN_FPGA_INPUT");
			if (_circNets.NumberOfInputBubbles() > 0)
				map.add("LOGISIM_HIDDEN_FPGA_INOUT", "LOGISIM_HIDDEN_FPGA_INOUT"); // note: no Toplevel inversions for InOut ports
			if (_circNets.NumberOfOutputBubbles() > 0)
				map.add("LOGISIM_HIDDEN_FPGA_OUTPUT", "s_LOGISIM_HIDDEN_FPGA_OUTPUT");
      
      // Normal ports
      for (int i = 0; i < _circNets.NumberOfInputPorts(); i++) {
        NetlistComponent pinComp = _circNets.GetInputPin(i);
        AttributeSet attrs = pinComp.GetComponent().getAttributeSet();
        String name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
        map.put(name, "s_" + name);
      }
      for (int i = 0; i < _circNets.NumberOfOutputPorts(); i++) {
        NetlistComponent pinComp = _circNets.GetOutputPin(i);
        AttributeSet attrs = pinComp.GetComponent().getAttributeSet();
        String name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
        map.put(name, "s_" + name);
      }
		}
    return map;
	}

  // Top-level entry point: recursively write all HDL files for the project.
  public boolean writeAllHDLFiles(String rootDir) {
		if (!rootDir.endsWith(File.separator))
			rootDir += File.separator;
    return writeAllHDLFiles(rootDir, new HashSet<String>(), new ArrayList<String>());
  }

	private boolean writeAllHDLFiles(String rootDir, HashSet<String> writtenComponents, ArrayList<String> path) {
		_circNets.SetCurrentHierarchyLevel(path);

		// Generate this circuit first
		String name = CorrectLabel.getCorrectLabel(circ.getName());
		if (writtenComponents.contains(name))
      return;
    if (!writeHDLFiles(rootDir))
      return false;
    writtenComponents.add(name);

		// Generate this circuit's normal components next
		for (NetlistComponent comp : _circNets.GetNormalComponents()) {
      HDLSupport g = comp.hdlSupport; // getHDLSupport(comp);
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
		for (NetlistComponent subcirc : _circNets.GetSubCircuits()) {
      CircuitHDLGenerator g = (CircuitHDLGenerator)subcirc.hdlSupport; // getHDLSupport(subcirc);
			if (g == null)
        return;
      path.add(CorrectLabel.getCorrectLabel(subcirc
            .GetComponent().getAttributeSet().getValue(StdAttr.LABEL)));
			if (!g.writeAllHDLFiles(rootDir, writtenComponents, path)) {
				return false;
			}
			path.remove(path.size() - 1);
		}

		return true;
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

  // private HashMap<NetlistComp, HDLSupport> generators = new HashMap<>();
  // private HDLSupport getHDLSupport(NetlistComponent comp) {
  //   HDLSupport g = generators.get(comp);
  //   if (generators.containsKey(comp))
  //     return generators.get(comp);;
  //   ComponentFactory factory = comp.GetComponent().getFactory();
  //   AttributeSet compAttrs = comp.GetComponent().getAttributeSet();
  //   HDLSupport g = factory.getHDLSupport(_lang, _err, _circNets, compAttrs, _vendor);
  //   if (g == null)
  //     _err.AddFatalError("INTERNAL ERROR: Missing HDL support for component of type '%s'.",
  //         factory.getName());
  //   generators.put(comp, g);
  //   return g;
  // }

	@Override
	public void generateVhdlComponentDeclarations(Hdl out) {
    generateDeclarations(out, _circNets.GetNormalComponents());
    generateDeclarations(out, _circNets.GetSubCircuits());
  }

  private generateDeclarations(ArrayList<NetlistComponent> components) {
		HashSet<String> done = new HashSet<>();
		for (NetlistComponent comp: components) {
      HDLSupport g = comp.hdlSupport; // getHDLSupport(comp);
      if (g == null)
        continue;
			String name = g.getComponentName();
			if (done.contains(name) || g.inlined)
        continue;
      g.generateVhdlComponentDeclarations(out);
      done.add(name);
		}
	}

	@Override
  protected void generateBehavior(Hdl out) {

    // Handle splitters and tunnels.
    generateWiring(out);

		// Connect each HDL port to the net connected to the corresponding circuit pin.
    // Note: This is almost exactly the same as getPortMappings(), but
    // creating signal assignments instead of port mappings, and also with
    // different logic for handling unconnected pins.
		for (int i = 0; i < _circNets.NumberOfInputPorts(); i++)
      generatePortAssignments(out, _circNets.GetInputPin(i), false /* not output */);
		for (int i = 0; i < _circNets.NumberOfOutputPorts(); i++)
      generatePortAssignments(out, _circNets.GetOutputPin(i), true /* yes output */);

    // Dynamic clock - DynamicClockHDLGenerator will connected directly to output port

    // Handle normal components.
		HashMap<String, Long> ids = new HashMap<>();
		for (NetlistComponent comp: _circNets.GetNormalComponents()) {
      HDLSupport g = comp.hdlSupport; // getHDLSupport(comp);
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

    // Handle subcircuits.
		for (NetlistComponent comp : _circNets.GetSubCircuits()) {
      HDLSupport g = comp.hdlSupport; // getHDLSupport(comp);
      if (g == null)
        continue;
      String prefix = g.getInstanceNamePrefix();
      long id = ids.getOrDefault(prefix, 0) + 1;
      ids.put(prefix, id);
      g.generateComponentInstance(out, id, comp);
    }
	}

	private void generateWiring(Hdl out) {

		// Process nets with ForcedRoot annotation
		for (Net net : _circNets.GetAllNets()) {
			if (!net.IsForcedRootNet())
        continue;

      // Process each bit
      for (int bit = 0; bit < net.BitWidth(); bit++) {

        // First perform source connections
        for (ConnectionPoint srcPt : net.GetSourceNets(bit)) {
          String srcNet = "s_LOGISIM_BUS_"+_circNets.GetNetId(srcPt.GetParrentNet());
          int srcBit = srcPt.GetParrentNetBitIndex();

          int id = _circNets.GetNetId(net);
          if (net.isBus())
            s.assn("s_LOGISIM_BUS_"+id, bit, srcNet, srcBit);
          else
            s.assn("s_LOGISIM_NET_"+id, srcNet, srcBit);
        }

        // Next perform sink connections
        for (ConnectionPoint sinkPt : net.GetSinkNets(bit)) {
          String sinkNet = "s_LOGISIM_BUS_"+_circNets.GetNetId(sinkPt.GetParrentNet());
          int sinkBit = sinkPt.GetParrentNetBitIndex();

          int id = _circNets.GetNetId(net);
          if (net.isBus())
            s.assn(sinkNet, sinkBit, "s_LOGISIM_BUS_"+id);
          else
            s.assn(sinkNet, sinkBit, "s_LOGISIM_NET_"+id);
        }
      }
		}
	}

  private void generatePortAssignments(Hdl out, NetlistComponent pinComp, boolean isOutput) {
    AttributeSet attrs = pinComp.GetComponent().getAttributeSet();
    String name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    PortInfo p = new PortInfo(name, attrs.getValue(StdAttr.WIDTH).getWidth(), 0, null);
    ConnectionEnd end = pinComp.getEnd(0);
    int w = end.NrOfBits();
    if (w == 1) {
      if (isOutput)
        out.assign(name, _circNets.signalForEndBit(end, 0, out));
      else
        out.assign(_circNets.signalForEndBit(end, 0, out), name);
    } else {
      int status = _circNets.busConnectionStatus(end);
      if (status == Netlist.BUS_UNCONNECTED) {
        if (isOutput) {
          _err.AddWarning("Output pin '%s' of circuit '%s' has no driving signal.",
              name, circ.getName());
        } else {
          // Input pin driving nothing is fine.
        }
      } else if (status == Netlist.BUS_SIMPLYCONNECTED ) {
        if (isOutput)
          map.assign(name, _circNets.signalForEndBus(end, w-1, 0, _hdl));
        else
          map.assign(_circNets.signalForEndBus(end, w-1, 0, _hdl), name);
      } else { // Netlist.BUS_MULTICONNECTED || Netlist.BUS_MIXCONNECTED
        // Verilog input example:       Verilog output example:
        // bar[7:5] = name[10:8]        name[10:8] = bar[7:5]
        // unused name[7:6]             name[7:6] not driven (warn)
        // foo[10:9] = name[5:4]        name[5:4] = foo[10:9]
        // foo[23:20] = name[3:0]       name[3:0] = foo[23:20]
        Net prevnet = end.getConnection((byte)(w-1)).GetParrentNet();
        byte prevbit = prevnet == null ? -1 : end.GetConnection((byte)(w-1)).GetParrentNetBitIndex();
        int n = 1;
        for (int i = w-2; i >= -1; i--) {
          Net net = i < 0 ? null : end.getConnection((byte)i).GetParrentNet();
          byte bit = i < 0 ? 0 : end.GetConnection((byte)i).GetParrentNetBitIndex();
          if (i >= 0 && ((prevnet == null && net == null)
                || (prevnet != null && net == prevnet && bit == prevbit-n))) {
            n++;
            continue;
          }
          // emit the n-bit slice, or accumulate it
          if (prevnet == null) {
            if (isOutput && n == 1)
              _err.AddWarning("Output pin '%s' bit %d of circuit '%s', has no driving signal.",
                  name, i+1, circ.getName());
            else if (isOutput)
              _err.AddWarning("Output pin '%s' bits %d downto %d of circuit '%s', have no driving signal.",
                  name, i+n, i+1, circ.getName());
            continue;
          }
          String signal;
          if (n == 1 && prevnet.BitWidth() == 1)
            signal = String.format("s_LOGISIM_NET_%d", _circNets.GetNetId(prevnet));
          else if (n == 1)
            signal = String.format("s_LOGISIM_BUS_%d"+_hdl.idx, _circNets.GetNetId(prevnet), prevbit);
          else
            signal = String.format("s_LOGISIM_BUS_%d"+_hdl.range, _circNets.GetNetId(prevnet), prevbit, prevbit-n+1);
          if (isOutput)
            out.assign(name, i+n, i+1, signal);
          else
            out.assign(signal, name, i+n, i+1);
        }
      }
    }
  }

}
