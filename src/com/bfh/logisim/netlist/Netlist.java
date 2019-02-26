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

package com.bfh.logisim.netlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bfh.logisim.library.DynamicClock;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.std.hdl.VhdlEntity;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.Tty;
import com.cburch.logisim.std.io.Keyboard;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;

// Netlist holds info about the connectivity within a circuit based on the
// layout of wires, tunnels, splitters, and any component ports that touch each
// other. Netlist also holds other info about the circuit, for convenience:
// - inputs and outputs (based on Pin components within the circuit)
// - "normal" components (muxes, adders, vhdl entities, etc.)
// - subcircuit components
// - clock components within the circuit
// - clock networks (?) FIXME
// - dynamic clock control (if that component is present)
// - hidden nets (needed for Button, Tty, Keyboard, subcircuit, and other I/O components)
//
public class Netlist {

  // Circuit for which we store connectivity info.
	private final Circuit circ;

  // Signal nets, each defining a collection of connected points.
	private final ArrayList<Net> nets = new ArrayList<>();
  private final HashMap<Location, Net> netAt = new HashMap<>();

  // Shadow data for components within this circuit, arranged by factory type.
  private final ArrayList<NetlistComponent> components = new ArrayList<>(); // any factory
	private final ArrayList<NetlistComponent> subcircuits = new ArrayList<>(); // SubCircuitFactory
	private final ArrayList<NetlistComponent> clocks = new ArrayList<>(); // Clock
	private final ArrayList<NetlistComponent> inpins = new ArrayList<>(); // Pin (input)
	private final ArrayList<NetlistComponent> outpins = new ArrayList<>(); // Pin (output)
  private NetlistComponent dynClock; // DynamicClock

  // Something about clock trees ? FIXME
	private final ClockTreeFactory clocktree = new ClockTreeFactory();

  // Info about hidden networks, needed by certain I/O-related components.
	private int numHiddenInbits;
	private int numHiddenInOutbits;
	private int numHiddenOutbits;
  
  // Path to one of the instantiations of this circuit. For a circuit that gets
  // instantiated several times, there is one Netlist for the circuit, and the
  // currentPath will rotate between the different instances.
	private Path currentPath;

  // Info about design rule checks.
	private int status;
	public static final int DRC_REQUIRED = -1;
	public static final int DRC_PASSED = 0;
	public static final int DRC_ERROR = 1;

	public Netlist(Circuit c) {
		circ = c;
		clear();
	}

	public void clear() {
		nets.clear();
		netAt.clear();
		components.clear();
		subcircuits.clear();
		clocks.clear();
		inpins.clear();
		outpins.clear();
    dynClock = null;
    clocktree.clean();
		numHiddenInbits = 0;
		numHiddenOutbits = 0;
		numHiddenInOutbits = 0;
    currentPath = null;
		status = DRC_REQUIRED;
	}

	private void recursiveClear() {
    clear();
		for (Component comp : circ.getNonWires()) {
			if (comp.getFactory() instanceof SubcircuitFactory) {
				SubcircuitFactory fac = (SubcircuitFactory) comp.getFactory();
				fac.getSubcircuit().getNetList().ClearNetlist();
			}
		}
	}

  // Primary entry point for performing design-rule-check (DRC) validation.
  public boolean validate(FPGAReport err, String lang, char vendor) {
    recursiveClear();
    return recursiveValidate(err, lang, vendor, new ArrayList<>(), true) == DRC_PASSED:
  }

  private boolean recursiveValidate(FPGAReport err, String lang, char vendor,
      ArrayList<String> sheets, boolean isTop) {

    // Avoid re-validating when there are multiple instances of this circuit.
    if (status == DRC_PASSED)
      return true;
    else if (status == DRC_ERROR)
      return false;

    // Sanity check for identically named circuits or infinite recursion.
    String name = circ.getName();
    if (sheets.contains(name)) {
			err.AddFatalError("Multiple circuits in your design are named '%s'."
          + " Each circuit must have a unique name.", name);
      status = DRC_ERROR;
      return false;
    }
    sheets.add(name);

    // Perform the actual DRC and cache the result.
    status = drc(err, lang, vendor, sheets, isTop);
    if (status != DRC_PASSED) {
			this.clear();
      status = DRC_ERROR;
      return false;
    }
    return true;
  }

  private int drc(FPGAReport err, String lang, char vendor,
      ArrayList<String> sheets, boolean isTop) {

    String circName = circ.getName();

    // DRC Step 1: Check for presence of tri-state devices or bidirectional
    // ports, neither of which are typicallysupported for HDL synthesis.
		err.AddInfo("Checking for tri-state drivers or bidirectional ports in circuit '%s'.", circName);
		for (Component comp : circ.getNonWires()) {
			if (comp.getFactory().HasThreeStateDrivers(comp.getAttributeSet()))
        return drcFail(err, comp, "component has tri-state output drivers or is configured "
            "to allow floating outputs, features typically not supported for FPGA synthesis.");
      for (EndData end : comp.getEnds())
        if (end.isInput() && end.isOutput())
          return drcFail(err, comp, "component has a bidirectional port, a feature not yet supported.");
    }

    // DRC Step 2: Check connectivity and attempt to build nets.
		err.AddInfo("Checking wire, tunnel, and splitter connectivity in circuit '%s'.", circName);

		if (!buildNets(err, lang, vendor))
			return DRC_ERROR;

		if (hasShortCircuits()) {
			err.AddFatalError("Circuit '%s' has short-circuit faults.", circName);
			return DRC_ERROR;
		}

    printNetlistStats(err);

    // DRC Step 3: For top-level, build clock tree.
		if (isTop) {
      recursiveSetClockSource(new ClockSourceContainer());

      ArrayList<String> allNames = new ArrayList<>();
      ArrayList<Netlist> topAndParentNetlists = new ArrayList<>();
      topAndParentNetlists.add(this);
      if (!recursiveMarkClockSources(allNames, topAndParentNetlists, err))
        return DRC_ERROR;

      // here
			ConstructHierarchyTree(null, new ArrayList<String>(),
					new Integer(0), new Integer(0), new Integer(0));
			int ports = NumberOfInputPorts() + NumberOfOutputPorts()
					+ numHiddenInbits + numHiddenOutbits
					+ numHiddenInOutbits;
			if (ports == 0) {
				err.AddFatalError("Toplevel \"" + circ.getName()
						+ "\" has no input(s) and/or no output(s)!");
				status = DRC_ERROR;
				return status;
			}
		}



    // Step 1: Check for illegal dynamic clocks in non-top-level circuits, or
    // multiple dynamic clocks in a top-level circuit.
    if (isTop && countDynamicClocks() > 1) {
      err.AddFatalError("Found multiple dynamic clock controls in " +
          " circuit '%s'. At most one is allowed.", circName);
      return DRC_ERROR;
    } else if (!isTop && countDynamicClocks() > 0) {
      err.AddFatalError("Found a dynamic clock control in " +
          " sub-circuit '%s'. These can only be placed in " +
          " the top-level main circuit.", circName);
      return DRC_ERROR;
    }

    // Step 2: Ensure all components support this HDL configuration, and perform
    // some simple name validation.
    HashMap<Component, HDLGeneratorFactory> generators = new HashMap<>();
		for (Component comp : circ.getNonWires()) {
      if (comp.getFactory().HDLIgnore())
        continue; // Text, Probe, and other similarly irrelevant components
      if (comp.getFactory() instanceof Pin)
        continue; // supported, but handled later
			if (comp.getFactory() instanceof SubcircuitFactory
          || comp.getFactory() instanceof VhdlEntity) {
        String compName = comp.getFactory().getName();
				if (!CorrectLabel.IsCorrectLabel(compName,
						lang, "Bad name for component '"+compName+"' in circuit '"+circName+"'", err))
					return DRC_ERROR;
			}



      // if (comp.getFactory().HDLSpecialHandling())
      //  continue; // ? maybe ..?
      HDLGeneratorFactory g = comp.getFactory().getHDLGenerator(lang, err,
            null, /* fixme - no nets yet... */
					comp.getAttributeSet(), vendor);
      if (g == null) {
				err.AddFatalError("Found unsupported component: \""
						+ comp.getFactory().getName() + "\" for "
						+ lang.toString()
						+ " generation in circuit : \"" + circ.getName()
						+ "\"");
				status = DRC_ERROR;
				return status;
			}
			/* Now we add the name to the set if it is not already in */
      String ComponentName = g.getHDLNameWithinCircuit(circ.getName());
			if (!CompName.contains(ComponentName)) {
				CompName.add(ComponentName);
				AnnotationNames.add(new HashSet<String>());
        generators.put(comp, g);
			}
		}
    
    
    ArrayList<String> uniqueLabels = new ArrayList<>();
		ArrayList<String> CompName = new ArrayList<>();
		ArrayList<Set<String>> AnnotationNames = new ArrayList<>();
		for (Component comp : circ.getNonWires()) {
      if (comp.getFactory() instanceof DynamicClock) {
        if (!isTop) {
          err.AddFatalError("Found dynamic clock control in " +
              " sub-circuit \"" + circ.getName() + "\"." +
              " This component must only be placed in the top-level main circuit.");
          status = DRC_ERROR;
          return status;
        }
        if (dynClock != null) {
          err.AddFatalError("Found multiple dynamic clock controls in " +
              " circuit \"" + circ.getName() + "\"." +
              " Only a single instance of this component is allowed.");
          status = DRC_ERROR;
          return status;
        }
        // dynClock = new NetlistComponent(comp);
        continue;
      }
			/*
			 * Here we check if the components are supported for the HDL
			 * generation
			 */
      if (comp.getFactory().HDLIgnore())
        continue;
      if (comp.getFactory() instanceof Pin)
        continue;
			if (comp.getFactory() instanceof SubcircuitFactory
          || comp.getFactory() instanceof VhdlEntity) {
				/* Special care has to be taken for sub-circuits and vhdl entities*/
        // discard
				if (!CorrectLabel.IsCorrectLabel(comp.getFactory().getName(),
						lang, "Bad name for component \""
								+ comp.getFactory().getName() + "\" in circuit \"" + circ.getName(), err)) {
					status = DRC_ERROR;
					return status;
				}
			}
      // if (comp.getFactory().HDLSpecialHandling())
      //  continue; // ? maybe ..?
      HDLGeneratorFactory g = comp.getFactory().getHDLGenerator(lang, err,
            null, /* fixme - no nets yet... */
					comp.getAttributeSet(), vendor);
      if (g == null) {
				err.AddFatalError("Found unsupported component: \""
						+ comp.getFactory().getName() + "\" for "
						+ lang.toString()
						+ " generation in circuit : \"" + circ.getName()
						+ "\"");
				status = DRC_ERROR;
				return status;
			}
			/* Now we add the name to the set if it is not already in */
      String ComponentName = g.getHDLNameWithinCircuit(circ.getName());
			if (!CompName.contains(ComponentName)) {
				CompName.add(ComponentName);
				AnnotationNames.add(new HashSet<String>());
        generators.put(comp, g);
			}
		}

		for (Component comp : circ.getNonWires()) {
			/*
       * we check that all components that require a non zero label (annotation)
       * have a label set. These are components like LED, Pin, DipSwitch, etc.,
       * which maybe need the label b/c they might show up in the mapping gui
       * and/or b/c the vhdl they generate is unique to each instance so we need
       * a unique name for each.
       * In any case, they should all have a generator, because none of them
       * have HDLIgnore set (which would cause them to be skipped above).
			 */
      HDLGeneratorFactory g = generators.get(comp);
			if (g.requiresUniqueLabel()) {
				String Label = CorrectLabel.getCorrectLabel(comp.getAttributeSet().getValue(StdAttr.LABEL).toString()).toUpperCase();
				if (Label.isEmpty()) {
					err.AddError("Component \""
							+ comp.getFactory().getName()
							+ "\" in sheet "
							+ circ.getName()
							+ " does not have a label! Run annotate, or manually add labels.");
					status = DRC_ERROR;
					return status;
				}
				if (CompName.contains(Label)) {
					err.AddSevereError("Sheet \""
							+ circ.getName()
							+ "\" has one or more components with the name \""
							+ Label
							+ "\" and also components with a label of the same name. This is not supported!");
					status = DRC_ERROR;
					return status;
				}
				if (!CorrectLabel
						.IsCorrectLabel(Label, lang, "Component \""
								+ comp.getFactory().getName() + "\" in sheet "
								+ circ.getName() + " with label \""
								+ Label.toString(), err)) {
					status = DRC_ERROR;
					return status;
				}
				String ComponentName = g.getHDLNameWithinCircuit(circ.getName());
				if (AnnotationNames.get(CompName.indexOf(ComponentName)).contains(Label)) {
					err.AddSevereError("Duplicated label \""
							+ comp.getAttributeSet().getValue(StdAttr.LABEL)
									.toString() + "\" found for component "
							+ comp.getFactory().getName() + " in sheet "
							+ circ.getName());
					status = DRC_ERROR;
					return status;
				} else {
					AnnotationNames.get(CompName.indexOf(ComponentName)).add(Label);
				}
				if (comp.getFactory() instanceof SubcircuitFactory) {
					/* Special care has to be taken for sub-circuits */
					if (Label.equals(ComponentName.toUpperCase())) {
						err.AddError("Found that the component \""
								+ comp.getFactory().getName() + "\" in sheet "
								+ circ.getName() + " has a label that"
								+ " corresponds to the component name!");
						err.AddError("Labels must be unique and may not correspond to the component name!");
						status = DRC_ERROR;
						return status;
					}
					if (CompName.contains(Label)) {
						err.AddError("Subcircuit name "
								+ comp.getFactory().getName() + " in sheet "
								+ circ.getName()
								+ " is a reserved name; please rename!");
						status = DRC_ERROR;
						return status;
					}
					SubcircuitFactory sub = (SubcircuitFactory) comp
							.getFactory();
					/* Here we recurse into the sub-circuits */
					status = sub
							.getSubcircuit()
							.getNetList()
							.recursiveValidate(err, lang, vendor, sheets, false);
					if (status != DRC_PASSED) {
						return status;
					}
					numHiddenInbits = numHiddenInbits
							+ sub.getSubcircuit().getNetList()
									.NumberOfInputBubbles();
					numHiddenOutbits = numHiddenOutbits
							+ sub.getSubcircuit().getNetList()
									.NumberOfOutputBubbles();
					numHiddenInOutbits = numHiddenInOutbits
							+ sub.getSubcircuit().getNetList()
									.NumberOfInOutBubbles();
				}
			}
		}

		err.AddInfo("Circuit \"" + circ.getName()
				+ "\" passed DRC check");
		status = DRC_PASSED;
		return status;
	}

  private int countDynamicClocks() {
    int n = 0;
    for (Component comp : circ.getNonWires())
      if (comp.getFactory() instanceof DynamicClock)
        n++;
    return n;
  }


  // here good functions

	private void recursiveSetClockSource(ClockSourceContainer clkSource) {
		clocktree.setSource(clkSource);
		for (Component comp : subcircuits) {
      SubcircuitFactory fac = (SubcircuitFactory) comp.getComponent().getFactory();
      fac.getSubcircuit().getNetList().setClockSource(clkSource);
    }
  }
 
  private void printNetlistStats(FPGAReport err) {
    int n = 0, b = 0;
		for (Net net : nets) {
			if (net.IsRootNet() && net.isBus())
        b++;
			if (net.IsRootNet() && !net.isBus())
				n++;
    }
    err.AddInfo("Circuit '%s' contains %d signal nets and %d bus nets.", circ.getName(), n, b);
  }

  private boolean hasShortCircuits() {
		for (Net net : nets)
			if (net.IsRootNet() && net.hasShortCircuit())
        return true;
		return false;
	}
 
  private int drcFail(FPGAReport err, Component comp, String msg, String ...args) {
    String label = comp.getAttributeSet().getValue(StdAttr.LABEL).toString();
    String prefix;
    if (label.isEmpty())
      prefix = String.format("Component '%s' at %s in circuit '%s': ",
          comp.getFactory().getName(), comp.getLocation().toString(), label, circ.getName());
    else
      prefix = String.format("Component '%s' with label '%s' in circuit '%s': ",
          comp.getFactory().getName(), label, circ.getName());
    err.AddSevereError(prefix + String.format(msg, args));
    return DRC_ERROR;
  }


	private void ConstructHierarchyTree(Set<String> ProcessedCircuits,
			ArrayList<String> HierarchyName, Integer GlobalInputID,
			Integer GlobalOutputID, Integer GlobalInOutID) {
		if (ProcessedCircuits == null) {
			ProcessedCircuits = new HashSet<String>();
		}
		/*
		 * The first step is to go down to the leaves and visit all involved
		 * sub-circuits to construct the local bubble information and form the
		 * Mappable components tree
		 */
		numHiddenInbits = 0;
		numHiddenOutbits = 0;
		numHiddenInOutbits = 0;
		for (NetlistComponent comp : subcircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(HierarchyName);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			boolean FirstTime = !ProcessedCircuits.contains(sub.getName()
					.toString());
			if (FirstTime) {
				ProcessedCircuits.add(sub.getName());
				sub.getSubcircuit()
						.getNetList()
						.ConstructHierarchyTree(ProcessedCircuits,
								MyHierarchyName, GlobalInputID, GlobalOutputID,
								GlobalInOutID);
			}
			int subInputBubbles = sub.getSubcircuit().getNetList().NumberOfInputBubbles();
			int subInOutBubbles = sub.getSubcircuit().getNetList().NumberOfInOutBubbles();
			int subOutputBubbles = sub.getSubcircuit().getNetList().NumberOfOutputBubbles();
			comp.setLocalHiddenPortIndices(
          numHiddenInbits, subInputBubbles,
					numHiddenInOutbits, subInOutBubbles,
					numHiddenOutbits, subOutputBubbles);
			numHiddenInbits += subInputBubbles;
			numHiddenInOutbits += subInOutBubbles;
			numHiddenOutbits += subOutputBubbles;
			comp.setGlobalHiddenPortIndices(MyHierarchyName,
          GlobalInputID, subInputBubbles,
					GlobalInOutID, subInOutBubbles
          GlobalOutputID, subOutputBubbles);
			if (!FirstTime) {
				sub.getSubcircuit()
						.getNetList()
						.EnumerateGlobalBubbleTree(MyHierarchyName,
								GlobalInputID, GlobalOutputID, GlobalInOutID);
			}
			GlobalInputID += subInputBubbles;
			GlobalInOutID += subInOutBubbles;
			GlobalOutputID += subOutputBubbles;
		}
		/*
		 * Here we processed all sub-circuits of the local hierarchy level, now
		 * we have to process the IO components
		 */
		for (NetlistComponent comp : components) {
			if (comp.hiddenPort != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(HierarchyName);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				int subInputBubbles = comp.hiddenPort.inports.size();
				int subInOutBubbles = comp.hiddenPort.inoutports.size();
				int subOutputBubbles = comp.hiddenPort.outports.size();
				comp.setLocalHiddenPortIndices(
            numHiddenInbits, subInputBubbles,
						numHiddenInOutbits, subInOutBubbles,
						numHiddenOutbits, subOutputBubbles);
				numHiddenInbits += subInputBubbles;
				numHiddenInOutbits += subInOutBubbles;
				numHiddenOutbits += subOutputBubbles;
				comp.setGlobalHiddenPortIndices(MyHierarchyName,
            GlobalInputID, subInputBubbles,
						GlobalInOutID, subInOutBubbles,
            GlobalOutputID, subOutputBubbles);
				GlobalInputID += subInputBubbles;
				GlobalInOutID += subInOutBubbles;
				GlobalOutputID += subOutputBubbles;
			}
		}
	}

	private void EnumerateGlobalBubbleTree(ArrayList<String> HierarchyName,
			int StartInputID, int StartOutputID, int StartInOutID) {
		for (NetlistComponent comp : subcircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(HierarchyName);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			sub.getSubcircuit()
					.getNetList()
					.EnumerateGlobalBubbleTree(MyHierarchyName,
							StartInputID + comp.GetLocalBubbleInputStartId(),
							StartOutputID + comp.GetLocalBubbleOutputStartId(),
							StartInOutID + comp.GetLocalBubbleInOutStartId());
		}
		for (NetlistComponent comp : components) {
			if (comp.hiddenPort != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(HierarchyName);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				int subInputBubbles = comp.hiddenPort.inports.size();
				int subInOutBubbles = comp.hiddenPort.inoutports.size();
				int subOutputBubbles = comp.hiddenPort.outports.size();
				comp.setGlobalHiddenPortIndices(MyHierarchyName,
						StartInputID + comp.GetLocalBubbleInputStartId(), subInputBubbles,
            StartInOutID, subInOutBubbles,
						StartOutputID + comp.GetLocalBubbleOutputStartId(), subOutputBubbles);
			}
		}
	}

	private Net FindConnectedNet(Location loc) {
		for (Net Current : nets) {
			if (Current.contains(loc)) {
				return Current;
			}
		}
		return null;
	}

  private boolean touching(Location pt, HashSet<Wire> wires) {
    for (Wire w : wires)
      if (w.endsAt(pt))
        return true;
    return false;
  }

  private static class CopperTrace extends HashSet<Location> {
    CopperTrace(Location pt) { add(pt); }
    CopperTrace(Wire w) { add(w.getEndLocation(0)); add(w.getEndLocation(1)); }
  }

	private boolean buildNets(FPGAReport err, String lang, char vendor) {
		LinkedList<CopperTrace> traces = new LinkedList<>();
   
    // Make a CopperTrace for every wire.
    for (Wire w : circ.getWires())
      traces.add(new CopperTrace(w));

    // Make a CopperTrace for every group of tunnels with the same name.
		HashMap<String, CopperTrace> tunnels = new HashMap<>();
    for (Component comp : circ.getNonWires()) {
      if (!(comp.getFactory() instanceof Tunnel))
        continue;
      String name = comp.getAttributeSet().getValue(StdAttr.LABEL);
      Location pt = comp.getEnd(0).getLocation();
      CopperTrace c = tunnels.get(name);
      if (c == null) {
        c = new CopperTrace(pt);
        traces.add(c);
        tunnels.put(name, c);
      } else {
        c.add(pt);
      }
    }

    // Make a CopperTrace for every component port.
    for (Component comp : circ.getNonWires())
      for (EndData end : comp.getEnds())
        traces.add(new CopperTrace(end.getLocation()));

    // Make a Net for each set of touching CopperTraces.
		while (traces.size() != 0) {
      CopperTrace c = traces.pop();
			Net net = new Net(c);
      LinkedList<CopperTrace> workingset = new LinkedList<>();
      workingset.add(c);
      while (!workingset.isEmpty()) {
        c = workingset.pop();
        Iterator<CopperTrace> it = traces.iterator();
        while (it.hasNext()) {
          CopperTrace c2 = it.next();
          if (c2.touches(c)) {
            workingset.add(c2);
            net.add(c2);
            it.remove();
          }
        }
      }
      nets.add(net);
		}

    // Create index mapping Location to Net containing that location.
    for (Net net : nets)
      for (Location pt : net.getPoints())
        netAt.put(pt, net);

    // Annotate each Net with appropriate bit width.
		for (Component comp : circ.getNonWires()) {
      for (EndData end : comp.getEnds()) {
        Location pt = end.getLocation();
        int w = end.getWidth().getWidth();
        Net net = netAt.get(pt);
        int nw = net.bitWidth();
        if (nw == 0) {
          net.setBitWidth(w);
        } else if (nw != w) {
          drcFail(err, comp, "%d-bit port is connected to a %d-bit bus.", w, nw);
          return false;
        }
      }
    }

    // If a Net has zero width, it must be entirely unconnected, so remove it.
    for (Net net : nets)
      if (net.bitWidth() == 0)
        for (Location pt : net.getPoints())
          netAt.remove(pt);
    nets.removeIf(net -> net.bitWidth() == 0);

    // Lastly, process the splitters.
    for (Component comp : circ.getNonWires()) {
			if (!(comp.getFactory() instanceof SplitterFactory))
        continue;

		/*
		 * Finally we have to process the splitters to determine the bus
		 * hierarchy (if any)
		 */
		/*
		 * In this round we only process the evident splitters and remove them
		 * from the list
		 */
		Iterator<Component> MySplitters = SplitterList.iterator();
		while (MySplitters.hasNext()) {
			Component com = MySplitters.next();
			/*
			 * Currently by definition end(0) is the combined end of the
			 * splitter
			 */
			List<EndData> ends = com.getEnds();
			EndData CombinedEnd = ends.get(0);
			int RootNet = -1;
			/* We search for the root net in the list of nets */
			for (int i = 0; i < nets.size() && RootNet < 0; i++) {
				if (nets.get(i).contains(CombinedEnd.getLocation())) {
					RootNet = i;
				}
			}
			if (RootNet < 0) {
				err.AddFatalError("Could not find the rootnet of a Splitter in circuit \""
						+ circ.getName() + "\"!");
				this.clear();
				return false;
			}
			/*
			 * Now we process all the other ends to find the child busses/nets
			 * of this root bus
			 */
			ArrayList<Integer> Connections = new ArrayList<Integer>();
			for (int i = 1; i < ends.size(); i++) {
				EndData ThisEnd = ends.get(i);
				/* Find the connected net */
				int ConnectedNet = -1;
				for (int j = 0; j < nets.size() && ConnectedNet < 1; j++) {
					if (nets.get(j).contains(ThisEnd.getLocation())) {
						ConnectedNet = j;
					}
				}
				Connections.add(ConnectedNet);
			}
			for (int i = 1; i < ends.size(); i++) {
				int ConnectedNet = Connections.get(i - 1);
				if (ConnectedNet >= 0) {
					/* There is a net connected to this splitter's end point */
					if (!nets.get(ConnectedNet)
							.setParent(nets.get(RootNet))) {
						nets.get(ConnectedNet).ForceRootNet();
					}
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) com).GetEndpoints();
					for (byte b = 0; b < BusBitConnection.length; b++) {
						if (BusBitConnection[b] == i) {
							nets.get(ConnectedNet).AddParrentBit(b);
						}
					}
				}
			}
		}
		/*
		 * Now the complete netlist is created, we have to check that each
		 * net/bus entry has only 1 source and 1 or more sinks. If there exist
		 * more than 1 source we have a short circuit! We keep track of the
		 * sources and sinks at the root nets/buses
		 */
		for (Net ThisNet : nets) {
			if (ThisNet.IsRootNet()) {
				ThisNet.InitializeSourceSinks();
			}
		}
		/*
		 * We are going to iterate through all components and their respective
		 * pins to see if they are connected to a net, and if yes if they
		 * present a source or sink. We omit the splitter and tunnel as we
		 * already processed those
		 */
		for (Component comp : allComponents) {
			if (comp.getFactory() instanceof SubcircuitFactory) {
				if (!ProcessSubcircuit(comp, err)) {
					this.clear();
					return false;
				}
      } else if ((comp.getFactory() instanceof Pin)
          || (comp.getFactory() instanceof DynamicClock)
					|| (comp.getFactory().getIOInformation() != null)
					|| (comp.getFactory().getHDLGenerator(lang, err,
              null, /* no nets yet ... fixme ? maybe use "this"?*/
							comp.getAttributeSet(), vendor) != null)) {
				if (!ProcessNormalComponent(comp, err)) {
					this.clear();
					return false;
				}
			}
		}
		/*
		 * Here we are going to process the complex splitters, note that in the
		 * previous handling of the splitters we marked all nets connected to a
		 * complex splitter with a forcerootnet annotation; we are going to
		 * cycle trough all these nets
		 */
		for (Net thisnet : nets) {
			if (thisnet.IsForcedRootNet()) {
				/* Cycle through all the bits of this net */
				for (int bit = 0; bit < thisnet.BitWidth(); bit++) {
					for (Component comp : SplitterList) {
						/*
						 * Currently by definition end(0) is the combined end of
						 * the splitter
						 */
						List<EndData> ends = comp.getEnds();
						EndData CombinedEnd = ends.get(0);
						int ConnectedBus = -1;
						/* We search for the root net in the list of nets */
						for (int i = 0; i < nets.size() && ConnectedBus < 0; i++) {
							if (nets.get(i).contains(
									CombinedEnd.getLocation())) {
								ConnectedBus = i;
							}
						}
						if (ConnectedBus < 0) {
							/*
							 * This should never happen as we already checked in
							 * the first pass
							 */
							err.AddFatalError("Internal error!");
							this.clear();
							return false;
						}
						for (int endid = 1; endid < ends.size(); endid++) {
							// Iterate through bits to see if the current net is connected to this splitter.
							if (thisnet.contains(ends.get(endid).getLocation()))
                continue;
              // first we have to get the bitindices of the rootbus
              // Here we have to process the inherited bits of the parent
              byte[] BusBitConnection = ((Splitter) comp).GetEndpoints();
              ArrayList<Byte> IndexBits = new ArrayList<Byte>();
              for (byte b = 0; b < BusBitConnection.length; b++) {
                if (BusBitConnection[b] == endid) {
                  IndexBits.add(b);
                }
              }
              byte ConnectedBusIndex = IndexBits.get(bit);
              /* Figure out the rootbusid and rootbusindex */
              Net Rootbus = nets.get(ConnectedBus);
              while (!Rootbus.IsRootNet()) {
                ConnectedBusIndex = Rootbus
                    .getBit(ConnectedBusIndex);
                Rootbus = Rootbus.getParent();
              }
              NetlistComponent.ConnectionPoint pt = new NetlistComponent.ConnectionPoint(Rootbus, ConnectedBusIndex);
              Boolean IsSink = true;
              if (!thisnet.hasBitSource(bit)) {
                if (HasHiddenSource(Rootbus,
                      ConnectedBusIndex, SplitterList,
                      comp, new HashSet<String>())) {
                  IsSink = false;
                }
              }
              if (IsSink)
                thisnet.addSinkNet(bit, pt);
              else
                thisnet.addSourceNet(bit, pt);
            }
					}
				}
			}
		}
		/* So now we have all information we need! */
		return true;
	}

	public ArrayList<Component> GetAllClockSources() {
		return clocktree.getSource().getSources();
	}

	public ArrayList<Net> GetAllNets() {
		return nets;
	}

	public Circuit getCircuit() {
		return circ;
	}

	public String getCircuitName() {
		return circ.getName();
	}

	public int GetClockSourceId(ArrayList<String> HierarchyLevel, Net WhichNet, Byte Bitid) {
		return clocktree.GetClockSourceId(HierarchyLevel, WhichNet, Bitid);
	}

	public int GetClockSourceId(Net WhichNet, Byte Bitid) {
		return clocktree.GetClockSourceId(currentPath, WhichNet, Bitid);
	}


	public int GetClockSourceId(Component comp) {
		return clocktree.GetClockSourceId(comp);
	}

	// public ArrayList<NetlistComponent> GetClockSources() {
	// 	return clocks;
	// }

	// public ArrayList<String> GetCurrentHierarchyLevel() {
	// 	return currentPath;
	// }

	private ArrayList<NetlistComponent.ConnectionPoint> GetHiddenSinks(Net thisNet,
			Byte bitIndex, Set<Component> SplitterList,
			Component ActiveSplitter, Set<String> HandledNets,
			Boolean isSourceNet) {
		ArrayList<NetlistComponent.ConnectionPoint> result = new ArrayList<>();

		// Check if already handled to prevent infinite looping.
		String NetId = nets.indexOf(thisNet) + "-" + bitIndex;
		if (HandledNets.contains(NetId))
			return result;
		else
			HandledNets.add(NetId);

		if (thisNet.hasBitSinks(bitIndex) && !isSourceNet)
			result.add(new NetlistComponent.ConnectionPoint(thisNet, bitIndex));

		/* Check if we have a connection to another splitter */
		for (Component currentSplitter : SplitterList) {
			if (ActiveSplitter != null) {
				if (currentSplitter.equals(ActiveSplitter)) {
					continue;
				}
			}
			List<EndData> ends = currentSplitter.getEnds();
			for (byte end = 0; end < ends.size(); end++) {
				if (thisNet.contains(ends.get(end).getLocation())) {
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
					if (end == 0) {
						/* this is a main net, find the connected end */
						Byte SplitterEnd = BusBitConnection[bitIndex];
						/* Find the corresponding Net index */
						Byte Netindex = 0;
						for (int index = 0; index < bitIndex; index++) {
							if (BusBitConnection[index] == SplitterEnd) {
								Netindex++;
							}
						}
						/* Find the connected Net */
						Net SlaveNet = null;
						for (Net thisnet : nets) {
							if (thisnet.contains(ends.get(SplitterEnd)
									.getLocation())) {
								SlaveNet = thisnet;
							}
						}
						if (SlaveNet != null) {
							if (SlaveNet.IsRootNet()) {
								/* Trace down the slavenet */
								result.addAll(GetHiddenSinks(SlaveNet,
										Netindex, SplitterList,
										currentSplitter, HandledNets, false));
							} else {
								result.addAll(GetHiddenSinks(
										SlaveNet.getParent(),
										SlaveNet.getBit(Netindex),
										SplitterList, currentSplitter,
										HandledNets, false));
							}
						}
					} else {
						ArrayList<Byte> Rootindices = new ArrayList<Byte>();
						for (byte b = 0; b < BusBitConnection.length; b++) {
							if (BusBitConnection[b] == end) {
								Rootindices.add(b);
							}
						}
						Net RootNet = null;
						for (Net thisnet : nets) {
							if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
								RootNet = thisnet;
							}
						}
						if (RootNet != null) {
							if (RootNet.IsRootNet()) {
								result.addAll(GetHiddenSinks(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets, false));
							} else {
								result.addAll(GetHiddenSinks(RootNet
										.getParent(), RootNet
										.getBit(Rootindices.get(bitIndex)),
										SplitterList, currentSplitter,
										HandledNets, false));
							}
						}
					}
				}
			}
		}
		return result;
	}
	
  public NetlistComponent GetOutputPin(int index) {
		if ((index < 0) || (index >= outpins.size())) {
			return null;
		}
		return outpins.get(index);
	}

	// public NetlistComponent GetInOutPin(int index)...
// 	public NetlistComponent GetInOutPort(int Index) {
// 		if ((Index < 0) || (Index >= MyInOutPorts.size())) {
// 			return null;
// 		}
// 		return MyInOutPorts.get(Index);
// 	}

// 	public NetlistComponent GetInputPin(int index)...
// 	public NetlistComponent GetInputPort(int Index) {
// 		if ((Index < 0) || (Index >= inpins.size())) {
// 			return null;
// 		}
// 		return inpins.get(Index);
// 	}

	public ArrayList<NetlistComponent> GetInputPorts() { return inpins; }
	public ArrayList<NetlistComponent> GetInOutPorts() { return MyInOutPorts; }
	public ArrayList<NetlistComponent> GetOutputPorts() { return outpins; }

  // main entry point...
	public Map<ArrayList<String>, NetlistComponent> GetMappableResources(
			ArrayList<String> Hierarchy, boolean toplevel) {
		Map<ArrayList<String>, NetlistComponent> Components = new HashMap<ArrayList<String>, NetlistComponent>();
		/* First we search through my sub-circuits and add those IO components */
		for (NetlistComponent comp : subcircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) comp.GetComponent()
					.getFactory();
			ArrayList<String> MyHierarchyName = new ArrayList<String>();
			MyHierarchyName.addAll(Hierarchy);
			MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
					.GetComponent().getAttributeSet().getValue(StdAttr.LABEL)
					.toString()));
			Components.putAll(sub.getSubcircuit().getNetList()
					.GetMappableResources(MyHierarchyName, false));
		}
		/* Now we search for all local IO components */
		for (NetlistComponent comp : components) {
			if (comp.hiddenPort != null) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
		}
		/* On the toplevel we have to add the pins */
		if (toplevel) {
			for (NetlistComponent comp : inpins) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
			for (NetlistComponent comp : MyInOutPorts) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
			for (NetlistComponent comp : outpins) {
				ArrayList<String> MyHierarchyName = new ArrayList<String>();
				MyHierarchyName.addAll(Hierarchy);
				MyHierarchyName.add(CorrectLabel.getCorrectLabel(comp
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL).toString()));
				Components.put(MyHierarchyName, comp);
			}
		}
		return Components;
	}

	public Integer GetNetId(Net selectedNet) {
		return nets.indexOf(selectedNet);
	}

	private NetlistComponent.ConnectionPoint GetNetlistConnectionForSubCircuit(String label,
			int portIndex, byte bitindex) {
    for (NetlistComponent comp : subcircuits) {
      // Check the label
      String s = comp.GetComponent().getAttributeSet().getValue(StdAttr.LABEL);
      s = CorrectLabel.getCorrectLabel(s);
      if (!s.equals(label))
        continue;
      // Found the component, now search the ports
      for (NetlistComponent.PortConnection end: comp.ports) {
        if (end.isOutput && bitindex < end.width
            && end.connections.get(bitindex).subcircPortIndex == portIndex)
          return end.connections.get(bitindex);
      }
    }
		return null;
	}

	public ArrayList<NetlistComponent> GetNormalComponents() {
		return components;
	}

	public int GetPortInfo(String Label) {
		String Source = CorrectLabel.getCorrectLabel(Label);
		for (NetlistComponent Inport : inpins) {
			String Comp = CorrectLabel.getCorrectLabel(Inport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = inpins.indexOf(Inport);
				return index;
			}
		}
		for (NetlistComponent InOutport : MyInOutPorts) {
			String Comp = CorrectLabel.getCorrectLabel(InOutport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = MyInOutPorts.indexOf(InOutport);
				return index;
			}
		}
		for (NetlistComponent Outport : outpins) {
			String Comp = CorrectLabel.getCorrectLabel(Outport.GetComponent()
					.getAttributeSet().getValue(StdAttr.LABEL));
			if (Comp.equals(Source)) {
				int index = outpins.indexOf(Outport);
				return index;
			}
		}
		return -1;
	}

	private Net GetRootNet(Net Child) {
		if (Child == null) {
			return null;
		}
		if (Child.IsRootNet()) {
			return Child;
		}
		Net RootNet = Child.getParent();
		while (!RootNet.IsRootNet()) {
			RootNet = RootNet.getParent();
		}
		return RootNet;
	}

	private byte GetRootNetIndex(Net Child, byte BitIndex) {
		if (Child == null) {
			return -1;
		}
		if ((BitIndex < 0) || (BitIndex > Child.BitWidth())) {
			return -1;
		}
		if (Child.IsRootNet()) {
			return BitIndex;
		}
		Net RootNet = Child.getParent();
		Byte RootIndex = Child.getBit(BitIndex);
		while (!RootNet.IsRootNet()) {
			RootIndex = RootNet.getBit(RootIndex);
			RootNet = RootNet.getParent();
		}
		return RootIndex;
	}

	// public Set<Splitter> getSplitters() {
	// 	Set<Splitter> SplitterList = new HashSet<Splitter>();
	// 	for (Component comp : circ.getNonWires()) {
	// 		if (comp.getFactory() instanceof SplitterFactory) {
	// 			SplitterList.add((Splitter) comp);
	// 		}
	// 	}
	// 	return SplitterList;
	// }

	public ArrayList<NetlistComponent> GetSubCircuits() {
		return subcircuits;
	}

	private boolean HasHiddenSource(Net thisNet, Byte bitIndex,
			Set<Component> SplitterList, Component ActiveSplitter,
			Set<String> HandledNets) {
		/*
		 * to prevent deadlock situations we check if we already looked at this
		 * net
		 */
		String NetId = Integer.toString(nets.indexOf(thisNet)) + "-"
				+ Byte.toString(bitIndex);
		if (HandledNets.contains(NetId)) {
			return false;
		} else {
			HandledNets.add(NetId);
		}
		if (thisNet.hasBitSource(bitIndex)) {
			return true;
		}
		/* Check if we have a connection to another splitter */
		for (Component currentSplitter : SplitterList) {
			if (currentSplitter.equals(ActiveSplitter)) {
				continue;
			}
			List<EndData> ends = currentSplitter.getEnds();
			for (byte end = 0; end < ends.size(); end++) {
				if (thisNet.contains(ends.get(end).getLocation())) {
					/* Here we have to process the inherited bits of the parent */
					byte[] BusBitConnection = ((Splitter) currentSplitter).GetEndpoints();
					if (end == 0) {
						/* this is a main net, find the connected end */
						Byte SplitterEnd = BusBitConnection[bitIndex];
						/* Find the corresponding Net index */
						Byte Netindex = 0;
						for (int index = 0; index < bitIndex; index++) {
							if (BusBitConnection[index] == SplitterEnd) {
								Netindex++;
							}
						}
						/* Find the connected Net */
						Net SlaveNet = null;
						for (Net thisnet : nets) {
							if (thisnet.contains(ends.get(SplitterEnd)
									.getLocation())) {
								SlaveNet = thisnet;
							}
						}
						if (SlaveNet != null) {
							if (SlaveNet.IsRootNet()) {
								/* Trace down the slavenet */
								if (HasHiddenSource(SlaveNet, Netindex,
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							} else {
								if (HasHiddenSource(SlaveNet.getParent(),
										SlaveNet.getBit(Netindex),
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							}
						}
					} else {
						ArrayList<Byte> Rootindices = new ArrayList<Byte>();
						for (byte b = 0; b < BusBitConnection.length; b++) {
							if (BusBitConnection[b] == end) {
								Rootindices.add(b);
							}
						}
						Net RootNet = null;
						for (Net thisnet : nets) {
							if (thisnet.contains(currentSplitter.getEnd(0).getLocation())) {
								RootNet = thisnet;
							}
						}
						if (RootNet != null) {
							if (RootNet.IsRootNet()) {
								if (HasHiddenSource(RootNet,
										Rootindices.get(bitIndex),
										SplitterList, currentSplitter,
										HandledNets)) {
									return true;
								}
							} else {
								if (HasHiddenSource(RootNet.getParent(),
										RootNet.getBit(Rootindices
												.get(bitIndex)), SplitterList,
										currentSplitter, HandledNets)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

  static final int BUS_UNCONNECTED = 0;     // all bits unconnected
  static final int BUS_SIMPLYCONNECTED = 1; // connected to contiguous slice of a single named bus
  static final int BUS_MULTICONNECTED = 2;  // all bits connected, but non-contiguous or multi-bus
  static final int BUS_MIXCONNECTED = -1;   // mix of connected and unconnected bits
  public int busConnectionStatus(NetlistComponent.PortConnection end) {
		int n = end.width;
		Net net0 = end.connections.get(0).net;
		byte idx = net0 == null ? -1 : end.connections.get(0).bit;
		for (int i = 1; i < n; i++) {
      Net neti = end.connections.get(i).net;
      if ((net0 == null) != (neti == null))
        return BUS_MIXCONNECTED: // This bit is connected when other wasn't, or vice versa
			if (net0 != neti)
				return BUS_MULTICONNECTED; // This bit is connected to different bus
			if (net0 != null && (idx + i) != end.connections.get(i).bit)
        return BUS_MULTICONNECTED; // This bit is connected to same bus, but indexes are not sequential
		}
    return net0 == null ? BUS_UNCONNECTED : BUS_SIMPLYCONNECTED;
	}
  
	// public boolean IsValid() {
	// 	return status == DRC_PASSED;
	// }

	private boolean recursiveMarkClockSources(ArrayList<String> allNames, ArrayList<Netlist> topAndParentNetlists, FPGAReport err) {
    // Depth-first recursion, to start work from leaf circuits and move upwards.
		for (NetlistComponent sub : subcircuits) {
      SubcircuitFactory fac = (SubcircuitFactory) sub.getComponent().getFactory();

			ArrayList<String> moreNames = new ArrayList<>(allNames);
			moreNames.add(sub.label());

			ArrayList<Netlist> moreNetlists = new ArrayList<>(topAndParentNetlists);
			moreNetlists.add(this);

			if (!fac.getSubcircuit().getNetList().recursiveMarkClockSources(
            moreNames, moreNetlists, err))
				return false;
		}

    // Enumerate splitters.
		HashSet<Component> splitters = new HashSet<>();
		for (Component comp : circ.getNonWires())
			if (comp.getFactory() instanceof SplitterFactory)
				splitters.add(comp);

		// Examine clock sources.
		for (NetlistComponent clk : clocks) {
			NetlistComponent.ConnectionPoint pt = clk.ports.get(0).connections.get(0);
      // Ignore clocks with disconnected outputs.
      if (pt.net == null)
        continue;
      // Track this clock source.
      int clkId = clocktree.getSource().getClockId(clk.GetComponent());
      clocktree.AddClockSource(allNames, clockid, pt);
      // Trace the clock source output net.
      if (!TraceClockNet(pt.net, pt.bit, clockid, allNames, topAndParentNetlists, err))
        return false;
      // To account for splitters, trace through our own netlist to find connections.
      for (NetlistComponent.ConnectionPoint pt2 :
          GetHiddenSinks(pt.net, pt.bit, splitters, null, new HashSet<String>(), true)) {
        clocktree.AddClockNet(allNames, clkid, pt2);
        if (!TraceClockNet(pt2.net,
              pt2.bit, clockid,
              allNames, topAndParentNetlists, err))
          return false;
			}
		}
    // Now that clock sources are marked, we can remove all non-root nets.
	  nets.removeIf(net -> !net.isRootNet());	
    // We can also clean up any remaining root nets now.
    nets.forEach(net -> net.FinalCleanup());
		return true;
	}

	public int NumberOfClockTrees() {
		return clocktree.getSource().getNrofSources();
	}

	public int NumberOfInOutBubbles() {
		return numHiddenInOutbits;
	}

	// public int NumberOfInOutPortBits() {
	// 	int count = 0;
	// 	for (NetlistComponent inp : MyInOutPorts) {
	// 		count += inp.ports.get(0).width;
	// 	}
	// 	return count;
	// }

	// public int NumberOfInOutPorts() {
	// 	return MyInOutPorts.size();
	// }

	public int NumberOfInputBubbles() {
		return numHiddenInbits;
	}

	// public int NumberOfInputPortBits() {
	// 	int count = 0;
	// 	for (NetlistComponent inp : inpins) {
	// 		count += inp.ports.get(0).width;
	// 	}
	// 	return count;
	// }

	public int NumberOfInputPorts() {
		return inpins.size();
	}
		
	public int NumberOfOutputBubbles() {
		return numHiddenOutbits;
	}

	// public int NumberOfOutputPortBits() {
	// 	int count = 0;
	// 	for (NetlistComponent outp : outpins) {
	// 		count += outp.ports.get(0).width;
	// 	}
	// 	return count;
	// }

	public int NumberOfOutputPorts() {
		return outpins.size();
	}

	private boolean ProcessNormalComponent(Component comp, FPGAReport err) {
		NetlistComponent NormalComponent = new NetlistComponent(comp);
		for (EndData ThisPin : comp.getEnds()) {
			if (ThisPin.isInput()
					&& ThisPin.isOutput()
					&& !(comp.getFactory() instanceof PortIO)) {
				err.AddFatalError("Found IO pin on component \""
						+ comp.getFactory().getName() + "\" in circuit \""
						+ circ.getName() + "\"!");
				return false;
			}
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			if (Connection == null)
        continue;
      int PinId = comp.getEnds().indexOf(ThisPin);
      boolean PinIsSink = ThisPin.isInput();
      NetlistComponent.PortConnection ThisEnd = NormalComponent.ports.get(PinId);
      Net RootNet = GetRootNet(Connection);
      if (RootNet == null) {
        err.AddFatalError("INTERNAL ERROR: Unable to find a root net!");
        return false;
      }
      for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
        Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
        if (RootNetBitIndex < 0) {
          err.AddFatalError("INTERNAL ERROR: Unable to find a root-net bit-index!");
          return false;
        }
        NetlistComponent.ConnectionPoint ThisSolderPoint = ThisEnd.connections.get(bitid);
        ThisSolderPoint.set(RootNet, RootNetBitIndex);
        if (PinIsSink)
          RootNet.addSink(RootNetBitIndex, ThisSolderPoint);
        else
          RootNet.addSource(RootNetBitIndex, ThisSolderPoint);
      }
		}
		if (comp.getFactory() instanceof Clock) {
			clocks.add(NormalComponent);
		} else if (comp.getFactory() instanceof Pin) {
			if (comp.getEnd(0).isInput()) {
				outpins.add(NormalComponent);
			} else {
				inpins.add(NormalComponent);
			}
    } else if (comp.getFactory() instanceof DynamicClock) {
      dynClock = NormalComponent;
		// } else if (comp.getFactory() instanceof PortIO) {
		// 	MyInOutPorts.add(NormalComponent);
		// 	components.add(NormalComponent);
		// } else if (comp.getFactory() instanceof Tty) {
		// 	MyInOutPorts.add(NormalComponent);
		// 	components.add(NormalComponent);
		// } else if (comp.getFactory() instanceof Keyboard) {
		// 	MyInOutPorts.add(NormalComponent);
		// 	components.add(NormalComponent);
		} else {
			components.add(NormalComponent);
		}
		return true;
	}
fixme: MyInOutPorts no longer has PortIO, Tty, Keyboard

	private boolean ProcessSubcircuit(Component comp, FPGAReport err) {
		NetlistComponent Subcircuit = new NetlistComponent(comp);
		SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
		Instance[] subPins = ((CircuitAttributes) comp.getAttributeSet())
				.getPinInstances();
		Netlist subNetlist = sub.getSubcircuit().getNetList();
		for (EndData ThisPin : comp.getEnds()) {
			if (ThisPin.isInput() && ThisPin.isOutput()) {
				err.AddFatalError("Found IO pin on component \""
						+ comp.getFactory().getName() + "\" in circuit \""
						+ circ.getName() + "\"! (subCirc)");
				return false;
			}
			Net Connection = FindConnectedNet(ThisPin.getLocation());
			int PinId = comp.getEnds().indexOf(ThisPin);
			int SubPortIndex = subNetlist.GetPortInfo(subPins[PinId].getAttributeValue(StdAttr.LABEL));
			if (SubPortIndex < 0) {
				err.AddFatalError("INTERNAL ERROR: Unable to find pin in sub-circuit!");
				return false;
			}
      // Special handling for sub-circuits; we have to find out the connection to the corresponding
      // net in the underlying net-list; At this point the underlying net-lists have already been generated.
      for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++)
        Subcircuit.ports.get(PinId).connections.get(bitid).subcircPortIndex = SubPortIndex;
			if (Connection != null) {
				boolean PinIsSink = ThisPin.isInput();
				Net RootNet = GetRootNet(Connection);
				if (RootNet == null) {
					err.AddFatalError("INTERNAL ERROR: Unable to find a root net!");
					return false;
				}
				for (byte bitid = 0; bitid < ThisPin.getWidth().getWidth(); bitid++) {
					Byte RootNetBitIndex = GetRootNetIndex(Connection, bitid);
					if (RootNetBitIndex < 0) {
						err.AddFatalError("INTERNAL ERROR: Unable to find a root-net bit-index!");
						return false;
					}
					Subcircuit.ports.get(PinId).connections.get(bitid).set(RootNet, RootNetBitIndex);
					if (PinIsSink) {
						RootNet.addSink(RootNetBitIndex,
								Subcircuit.ports.get(PinId).connections.get(bitid));
					} else {
						RootNet.addSource(RootNetBitIndex,
								Subcircuit.ports.get(PinId).connections.get(bitid));
					}
				}
			}
		}
		subcircuits.add(Subcircuit);
		return true;
	}

	public String projName() {
		return circ.getProjName();
	}

	public boolean RawFPGAClock() {
		return clocktree.getSource().RawFPGAClock();
	}

	public void SetRawFPGAClock(boolean en) {
		clocktree.getSource().SetRawFPGAClock(en);
	}

	public void SetRawFPGAClockFreq(long f) {
		clocktree.getSource().SetRawFPGAClockFreq(f);
	}

  public NetlistComponent GetDynamicClock() {
    return dynClock;
  }

	public void SetCurrentHierarchyLevel(Path path) {
		currentPath = path;
	}

	private boolean TraceClockNet(Net ClockNet, byte ClockNetBitIndex,
			int ClockSourceId, ArrayList<String> allNames,
			ArrayList<Netlist> topAndParentNetlists, FPGAReport err) {
		/* first pass, we check if the clock net goes down the hierarchy */
		for (NetlistComponent search : subcircuits) {
			SubcircuitFactory sub = (SubcircuitFactory) search.GetComponent().getFactory();
			for (NetlistComponent.ConnectionPoint SolderPoint : search.GetConnections(ClockNet, ClockNetBitIndex, false)) {
				if (SolderPoint.subcircPortIndex < 0) {
					err.AddFatalError("INTERNAL ERROR: Subcircuit port is not annotated!");
					return false;
				}
				NetlistComponent InputPort = sub.getSubcircuit().getNetList()
						.GetInputPort(SolderPoint.subcircPortIndex);
				if (InputPort == null) {
					err.AddFatalError("INTERNAL ERROR: Unable to find Subcircuit input port!");
					return false;
				}
				byte BitIndex = search.GetConnectionBitIndex(ClockNet,
						ClockNetBitIndex);
				if (BitIndex < 0) {
					err.AddFatalError("INTERNAL ERROR: Unable to find the bit index of a Subcircuit input port!");
					return false;
				}
				NetlistComponent.ConnectionPoint SubClockNet = InputPort.ports.get(0).connections.get(BitIndex);
				if (SubClockNet.net == null) {
					/* we do not have a connected pin */
					continue;
				}
				ArrayList<String> moreNames = new ArrayList<String>();
				ArrayList<Netlist> moreNetlists = new ArrayList<Netlist>();
				moreNames.addAll(allNames);
				moreNames.add(CorrectLabel.getCorrectLabel(search
						.GetComponent().getAttributeSet()
						.getValue(StdAttr.LABEL)));
				moreNetlists.addAll(topAndParentNetlists);
				moreNetlists.add(this);
				sub.getSubcircuit()
						.getNetList()
            .clocktree.AddClockNet(moreNames, ClockSourceId, SubClockNet);
				if (!sub.getSubcircuit()
						.getNetList()
						.TraceClockNet(SubClockNet.net,
								SubClockNet.bit,
								ClockSourceId, moreNames,
								moreNetlists, err)) {
					return false;
				}
			}
		}
		/* second pass, we check if the clock net goes up the hierarchy */
		if (!allNames.isEmpty()) {
			for (NetlistComponent search : outpins) {
				if (!search.GetConnections(ClockNet, ClockNetBitIndex, false)
						.isEmpty()) {
					byte bitindex = search.GetConnectionBitIndex(ClockNet,
							ClockNetBitIndex);
					NetlistComponent.ConnectionPoint SubClockNet = topAndParentNetlists
							.get(topAndParentNetlists.size() - 2)
							.GetNetlistConnectionForSubCircuit(
									allNames
											.get(allNames.size() - 1),
									outpins.indexOf(search), bitindex);
					if (SubClockNet == null) {
						err.AddFatalError("INTERNAL ERROR! Could not find a sub-circuit connection in overlying hierarchy level!");
						return false;
					}
					if (SubClockNet.net == null) {
					} else {
						ArrayList<String> moreNames = new ArrayList<String>();
						ArrayList<Netlist> moreNetlists = new ArrayList<Netlist>();
						moreNames.addAll(allNames);
						moreNames.remove(moreNames.size() - 1);
						moreNetlists.addAll(topAndParentNetlists);
						moreNetlists
								.remove(moreNetlists.size() - 1);
						topAndParentNetlists.get(topAndParentNetlists.size() - 2)
								.clocktree.AddClockNet(moreNames, ClockSourceId,
										SubClockNet);
						if (!topAndParentNetlists
								.get(topAndParentNetlists.size() - 2)
								.TraceClockNet(SubClockNet.net,
										SubClockNet.bit,
										ClockSourceId, moreNames,
										moreNetlists, err)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

  // TODO here
  // Determine the single-bit HDL signal name that is connected to the given
  // "end" (aka component port). If the end is not connected to a signal, then
  // null is returned.
  // xxx either and hdl "open" is returned (for outputs) or the given default value
  // xxx is returned instead (for inputs). But, if the default is null and the end
  // xxx is an input, null is returned instead.
  private String signalForEndBit(NetlistComponent.PortConnection end, int bit, /*Boolean defaultValue,*/ Hdl hdl) {
    NetlistComponent.ConnectionPoint solderPoint = end.connections.get(bit);
    Net net = solderPoint.net;
    if (net == null) { // unconnected net
      // if (end.isOutput) {
      //   return hdl.unconnected;
      // } else if (defaultValue == null) {
      //   hdl.err.AddFatalError("INTERNAL ERROR: Invalid component end/port.");
      //   return "???";
      // } else {
      //   return hdl.bit(defaultValue);
      // }
      return null;
    } else if (net.BitWidth() == 1) { // connected to a single-bit net
      return "s_LOGISIM_NET_" + this.GetNetId(net);
    } else { // connected to one bit of a multi-bit bus
      return String.format("S_LOGISIM_BUS_%d"+hdl.idx,
          this.GetNetId(net),
          solderPoint.bit);
    }
  }

  // TODO here
  // Determine the single-bit HDL signal name that is connected to one of the
  // given component's ports (aka "ends"). If the end is not connected to a
  // signal, then either an HDL "open" is returned (for outputs) or the given
  // default value is returned instead (for inputs). But, if the default is null
  // and the end is an input, a fatal error is posted.
	public String signalForEnd1(NetlistComponent comp, int endIdx, Boolean defaultValue, Hdl hdl) {
    return signalForEndBit(comp, endIdx, 0, defaultValue, hdl);
  }

	public String signalForEndBit(NetlistComponent comp, int endIdx, int bitIdx, Boolean defaultValue, Hdl hdl) {
    if (endIdx < 0 || endIdx >= comp.ports.size()) {
      if (defaultValue == null) {
        hdl.err.AddFatalError("INTERNAL ERROR: Invalid end/port '%d' for component '%s'", endIdx, comp);
        return "???";
      }
      // In some HDLGenerator subclasses, missing pins are sometimes expected
      // (e.g. an enable pin that is configured to be invisible). Returning the
      // default here allows those cases to work.
      return hdl.bit(defaultValue);
    }

    NetlistComponent.PortConnection end = comp.ports.get(endIdx);
    int n = end.width;
    if (n != 1) {
      hdl.err.AddFatalError("INTERNAL ERROR: Unexpected %d-bit end/port '%d' for component '%s'", n, endIdx, comp);
      return "???";
    }

    String s = signalForEndBit(end, bitIdx, hdl);
    if (s != null) {
      return s;
    } else if (end.isOutput) {
      return hdl.unconnected;
    } else if (defaultValue == null) {
      hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected end/port '%d' for component '%s'", endIdx, comp);
      return "???";
    } else {
      return hdl.bit(defaultValue);
    }

	}

  // Assuming isConnectedToSingleNamedBus(end)), returns the name (or slice) of
  // the bus signal this end is connected to, for example:
  //    VHDL:    s_LOGISIM_BUS_27(31 downto 0)
  //    Verilog: s_LOGISIM_BUS_27[31:0]
  public String signalForEndBus(NetlistComponent.PortConnection end, int bitHi, int bitLo, Hdl hdl) {
    Net net = end.getConnection((byte) bitLo).net;
    // if (net == null) {
    //   if (isOutput) {
    //     return hdl.unconnected;
    //   } else if (defaultValue == null) {
    //     hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected net to end/port '%d' for component '%s'", endIdx, comp);
    //     return "???";
    //   } else {
    //     int n = bitHi - bitLo + 1;
    //     return defaultValue ? hdl.ones(n) : hdl.zeros(n);
    //   }
    // }
    byte hi = end.connections.get(bitHi).bit;
    byte lo = end.connections.get(bitLo).bit;
    return String.format("s_LOGISIM_BUS_%d"+hdl.range, this.GetNetId(net), hi, lo);
  } 

  // TODO here - i don't like this interface...
  // Determine the HDL signal names (or name) that are connected to one of the
  // given component's ports (aka "ends"). If the end is not connected to a
  // signal, then given default value is returned instead. Or, if the default is
  // null, a fatal error is posted instead. For a 1-bit signal, a single mapping
  // from name to signal is returned. For a multi-bit signal, one or more
  // mappings are returned, as need.
	// public Map<String, String> signalForEnd(String portName,
  //     NetlistComponent comp, int endIdx, Boolean defaultValue, Hdl hdl) {

	// 	Map<String, String> result = new HashMap<String, String>();
  //   if (endIdx < 0 || endIdx >= comp.ports.size()) {
  //     hdl.err.AddFatalError("INTERNAL ERROR: Invalid end/port '%d' for component '%s'", endIdx, comp);
  //     return result;
  //   }

  //   NetlistComponent.PortConnection end = comp.ports.get(endIdx);
  //   int n = end.width;

  //   if (n == 1) {
  //     // example: somePort => s_LOGISIM_NET_5;
  //     result.put(portName, signalForEnd1(comp, endIdx, defaultValue, hdl));
  //     return result;
  //   }

	// 	boolean isOutput = end.isOutput;

  //   // boolean foundConnection = false;
  //   // for (int i = 0; i < n && !foundConnection; i++)
  //   //   foundConnection |= end.connections.get(i).net != null;

  //   // if (!foundConnection) {
  //   //   if (isOutput) {
  //   //     return hdl.unconnected;
  //   //   } else if (defaultValue == null) {
  //   //     hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected net to end/port '%d' for component '%s'", endIdx, comp);
  //   //     return "???";
  //   //   } else {
  //   //     return defaultValue ? hdl.ones(n) : hdl.zeros(n);
  //   //   }
  //   // }

  //   if (isConnectedToSingleNamedBus(end)) {
  //     // example: somePort => s_LOGISIM_BUS_27(4 downto 0)
  //     Net net = end.getConnection((byte) 0).net;
  //     if (net == null) {
  //       if (isOutput) {
  //         return hdl.unconnected;
  //       } else if (defaultValue == null) {
  //         hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected net to end/port '%d' for component '%s'", endIdx, comp);
  //         return "???";
  //       } else {
  //         return defaultValue ? hdl.ones(n) : hdl.zeros(n);
  //       }
  //     }
  //     byte hi = end.connections.get((n-1)).bit;
  //     byte lo = end.connections.get(0).bit;
  //     results.put(portName,
  //         String.format("s_LOGISIM_BUS_%d"+hdl.range, this.GetNetId(net), hi, lo));
  //     return results;
  //   } 

  //   if (isOutput)
  //     defaultValue = null; // neither VHDL nor Verilog allow only-partially-unconnected outputs
  //     
  //   ArrayList<String> signals = new ArrayList<>();
  //   for (int i = 0; i < n; i++) {
  //     String s = signalForEndBit(end, i, hdl);
  //     if (s != null) {
  //       signals.add(s);
  //     } else if (isOutput) {
  //       hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected bit %d of %d in end/port '%d' for component '%s'", i, n, endIdx, comp);
  //       signals.add(hdl.isVhdl ? "open" : "1'bz"); // not actually legal in VHDL/Verilog
  //     } else if (defaultValue == null) {
  //       hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected bit %d of %d in end/port '%d' for component '%s'", i, n, endIdx, comp);
  //       signals.add("???");
  //     } else {
  //       return signals.add(hdl.bit(defaultValue));
  //     }
  //   }

  //   if (hdl.isVhdl) {
  //     // VHDL example:
  //     //    somePort(2) => s_LOGISIM_BUS_27(3)
  //     //    somePort(1) => s_LOGISIM_BUS_27(2)
  //     //    somePort(0) => s_LOGISIM_BUS_45(8)
  //     // todo: contiguous ranges can be encoded more compactly as slices
  //     for (int i = 0; i < n; i++)
  //       results.put(String.format(portName+hdl.idx, i), signals.get(i));
  //   } else {
  //     // Verilog example:
  //     //   .somePort({s_LOGISIM_BUS_27(3),s_LOGISIM_BUS_27(2),s_LOGISIM_BUS_45(8)})
  //     results.put(portName, "{"+String.join(",", signals)+"}");
  //   }
  //   return results;
  // }

  // TODO here - i don't like this interface...
  // Determine the single-bit HDL signal name that is connected to a given bit
  // of the given component's ports (aka "ends"). If the end is not connected to
  // a signal, then either an HDL "open" is returned (for outputs) or the given
  // default value is returned instead (for inputs). But, if the default is null
  // and the end is an input, a fatal error is posted.
  public String signalForEndBit(NetlistComponent.PortConnection end, int bit, Boolean defaultValue, Hdl hdl) {
    String s = signalForEndBit(end, bit, hdl);
    if (s != null) {
      return s;
    } else if (isOutput) {
      return hdl.unconnected;
    } else if (defaultValue == null) {
      hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected bit %d of %d in end/port '%d' for component '%s'", i, n, endIdx, comp);
      return "???";
    } else {
      return hdl.bit(defaultValue);
    }
  }

//   // TODO here - rethink this interface ...
//   // fixme: handle rising/falling/level sensitive here, and non-clock signal cases too
// 	public String clockForEnd(NetlistComponent comp, int endIdx, Hdl hdl) {
//     if (endIdx < 0 || endIdx >= comp.ports.size()) {
//       hdl.err.AddFatalError("INTERNAL ERROR: Invalid end/port '%d' for component '%s'", endIdx, comp);
//       return "???";
//     }
// 
//     NetlistComponent.PortConnection end = comp.ports.get(endIdx);
//     if (end.width != 1) {
//       hdl.err.AddFatalError("INTERNAL ERROR: Bus clock end/port '%d' for component '%s'", endIdx, comp);
//       return "???";
//     }
// 
//     Net net = end.connections.get(0).net;
//     if (net == null) {
//       hdl.err.AddFatalError("INTERNAL ERROR: Unexpected unconnected clock for end/port '%d' for component '%s'", endIdx, comp);
//       return "???";
//     }
// 
//     byte idx = end.connections.get(0).bit;
//     int clkId = clocktree.GetClockSourceId(currentPath, net, idx);
//     if (clkId < 0) {
//       hdl.err.AddSevereWarning("WARNING: Non-clock signal connected to clock for end/port '%d' for component '%s'", endIdx, comp);
//       return "???"; // fixme: return alternate here?
//     }
// 
//     return "LOGISIM_CLOCK_TREE_" + clkId;
// 	}

}
