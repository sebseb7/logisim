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

package com.bfh.logisim.fpga;

import java.util.ArrayList;
import java.util.HashMap;

import com.bfh.logisim.gui.FPGAReport;
import com.bfh.logisim.netlist.NetlistComponent;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.std.wiring.Pin;
import static com.bfh.logisim.netlist.Netlist.Int3;

// PinBindings tracks the bindings ("mappings") between, on the one side,
// I/O-related components present in the logisim design and, on the other side,
// the physical (or synthetic) I/O resources defined by the FPGA baord.
//
// For the I/O-related components in the design, each one defines:
//   - a "hidden net" (one or more bits)
//   - a list of acceptable BoardIO types (a primary and some alternates)
//   - a path from the top level down to that specific instance
//
// For a multi-bit I/O-related component, an acceptable BoardIO types might only
// support a 1-bit version. In that case, the component can be treated like
// separate 1-bit signals. For example, a 3-bit output with primary type RGBLed
// would be treated as 3 individual signals when used with alternate type LED.
//
// For physical I/O resources, each BoardIO in the Board defines:
//   - its actual physicial type (LED, DipSwtich, Button, raw pin, etc.)
//   - a direction, defined by its type
//   - its bit width
//   - the pin location/number on the FPGA chip for each bit
//   - its location on the FPGA demo board (a rectangle on a picture)
//
// For a multi-bit physical BoardIO resource, like a 4-bit physical DipSwitch,
// the single BoardIO could idealy be treated as three separate one-bit output
// signals with the same direction. [This wasn't previously supported... the xml files
// don't describe locations for the individual pins within a multi-bit I/O
// resource, for example, so there isn't a great way to let the user know which pin was
// which. But I supposed we can have a popup instead showing bit index and/or
// actual pin names.]
//
// There are also "synthetic" I/O resources, like "all ones input", "all zeros
// input", "disconnected output", "constant value input", etc., that can be used
// in place of physical I/O resources.
//
// In the simplest case, each whole I/O component is paired with one whole I/O
// resource. In that case, the type, width and direction of the component and
// resource must match. In the more tricky cases, the separate signals for an
// I/O-related component might be individually mapped to different physical I/O
// resources [which ideally we will also allow to have been broken up and
// treated as separate signals, but again that wasn't previously supported].
//
// Commander holds the instance of this, but mostly doesn't use it.
//
// BindingsDialog is the UI for configuring the bindings.
//
// The Xilinx and Altera FPGA download scripts use this to get the list of pin
// locations that need to be given to the HDL synthesis tools.
//   see: GetFPGAPinLocs(int FPGAVendor)
//
// TopLevelHDLGenerator uses this:
//  - to connect hidden nets (with inversions, if needed) to the
//    user-selected BoardIO FPGA pins and/or synthetic values.
public class PinBindings {

  FPGAReport err;

  // List of I/O-related components in the design, organized by path.
	private final HashMap<Path, NetlistComponent> components;

  // The FPGA board describing available physical I/O resources.
	private final Board board;

  // Mappings defined so far.
  public final HashMap<Source, Dest> mappings = new HashMap<>();

  public static class Source {
    public final Path path;
    public final NetlistComponent comp;
    public final int bit; // -1 means entire NetlistComponent is mapped
    public final BoardIO.Type type;
    public final Int3 width;
    Int3 seqno;
    public Source(Path p, NetlistComponent c, int b, BOardIO.Type t, Int3 w) {
      path = p;
      comp = c;
      bit = b;
      type = t;
      width = w;
    }
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Source))
        return false;
      Source other = (Source)o;
      return path.equals(other.path) && bit == other.bit;
    }
    public int hashCode() {
      return path.toString().hashCode() + 31*bit;
    }
    public Int3 seqno() {
      return seqno.copy();
    }
  }

  public static class Dest {
    public final BoardIO io; // may be synthetic
    public final int bit; // -1 means something maps to this entire BoardIO
    public Dest(BoardIO i, int b) { io = i; bit = b; }
  }

  public PinBindings(FPGAReport err, Board board, HashMap<Path, NetlistComponent> components) {
    this.err = err;
		this.components = components;
		this.board = board;
	}

  public setComponents(HashMap<Path, NetlistComponent> newComponents) {
    // todo: preserve existing mappings wherever possible.
    // For now, we just clear all mappings and reset components.
    mappings.clear();
    components.clear();
    components.addAll(newComponents);
  }
  
  // ToplevelHDLGenerator has a port for each bit of each physical I/O pin that
  // it uses (for hidden nets and/or top-level circuit pins). Commander calls
  // this to lock in the mappings, giving each physical BoardIO input, inout,
  // and output bit a sequence number, and counting how many bits there are in
  // total.
  private Int3 finalizedCounts;
  public void finalizeMappings() {
    Int3 counts = new Int3();
    mappings.forEach((s, d) -> {
      if (!BoardIO.PhysicalTypes.contains(d.type))
        continue;
      s.seqno = counts.copy();
      counts.increment(s.width);
    }
    finalizedCounts = counts;
  }

  // Counts of all I/O-related physical FPGA pins used in the design.
  public Int3 countFPGAPhysicalIOPins() {
    return finalizedCounts.copy();
  }

  public ArrayList<String> pinLabels(Path path) {
    NetlistComponent comp = components.get(path);
    if (comp.hiddenPort == null) {
      // Top-level input or output port.
      int w = comp.original.getEnd(0).getWidth().getWidth();
      return BoardIO.Ribbon.pinLabels(w);
    } else {
      // Button, LED, PortIO, and other I/O-related types.
      ArrayList<String> labels = new ArrayList<>(); 
      labels.addAll(comp.hiddenPort.inports);
      labels.addAll(comp.hiddenPort.inoutports);
      labels.addAll(comp.hiddenPort.outports);
      return labels;
    }
  }

  public Source sourceFor(Path path) {
    NetlistComponent comp = components.get(path);
    Int3 compWidth;
    BoardIO.Type type;
    if (comp.hiddenPort == null) {
      // Top-level input or output port.
      int w = comp.original.getEnd(0).getWidth().getWidth();
      boolean i = comp.original.getEnd(0).isOutput(); // output to circuit, input from board
      boolean o = comp.original.getEnd(0).isInput(); // input to circuit, output from board
      compWidth = new Int3();
      if (i && o)
        compWidth.inout = w;
      else if (i)
        compWidth.in = w;
      else
        compWidth.out = w;
      type = w > 1 ? BoardIO.Ribbon : BoardIO.Pin;
    } else {
      // Button, LED, PortIO, and other I/O-related types.
      compWidth = comp.hiddenPort.size();
      type = selectDefaultType(comp.hiddenPort.types, compWidth.size());
    }
    return new Source(path, -1, type, compWidth);
  }

  public ArrayList<Source> bitSourcesFor(Path path) {
    NetlistComponent comp = components.get(path);
    ArrayList<Source> ret = new ArrayList<>();
    ArrayList<String> pinLabels = pinLabels(path);
    BoardIO.Type bitType;
    Int3 compWidth = new Int3();
    if (comp.hiddenPort == null) {
      bitType = BoardIO.Type.PIN;
      boolean i = comp.original.getEnd(0).isOutput(); // output to circuit, input from board
      boolean o = comp.original.getEnd(0).isInput(); // input to circuit, output from board
      if (i && o)
        compWidth.inout = 1;
      else if (i)
        compWidth.in = 1;
      else
        compWidth.out = 1;
    } else {
      bitType = selectDefaultType(comp.hiddenPort.types, 1);
      Int3 w = comp.hiddenPort.size();
      compWidth.in = (w.in > 0 ? 1 : 0);
      compWidth.inout = (w.inout > 0 ? 1 : 0);
      compWidth.out = (w.out > 0 ? 1 : 0);
    }
    for (int i = 0; i < pinLabels.size(); i++)
      ret.add(new Source(path, i, bitType, compWidth.copy()));
    return ret;
  }

  public boolean isMapped(Path path) {
    if (mappings.containsKey(sourceFor(path)))
      return true;
    for (Source bitSrc : bitSourcesFor(path))
      if (!mappings.containsKey(bitSrc))
        return false;
    return true;
  }

  private BoardIO.Type selectDefaultType(List<BoardIO.Type> types, int width) {
    if (width == 1) {
      // Pick first type meant for single-bit inputs.
      for (BoardIO.Type t : types)
        if (BoardIO.OneBitTypes.contains(t))
          return t;
      return BoardIO.Type.Pin; // default if nothing appropriate
    } else {
      // Pick first type meant for multi-bit inputs.
      for (BoardIO.Type t : types)
        if (!BoardIO.OneBitTypes.contains(t))
          return t;
      return BoardIO.Type.Ribbon; // default if nothing appropriate
    }
  }

  public ArrayList<BoardIO> compatibleResources(Source src) {
    // NetlistComponent comp = components.get(src.path);
    if (src.bit < 0)
      return compatibleResources(src.width, src.type);
    else
      return compatibleResources(1, src.type);
  }

  private ArrayList<BoardIO> compatibleResources(Int3 compWidth, BoardIO.Type compType) {
    ArrayList<Dest> res = new ArrayList<>();
    if (compWidth.isMised()) {
      err.AddError("INTERNAL ERROR: Detected I/O component with mixed direction bits.");
      return res;
    }
    for (BoardIO io : board)
      if (io.isCompatible(compWidth, compType))
          res.add(io);
    return res;
  }

  public HashSet<Source> addMapping(Source src, BoardIO io, int bit) {
    // sanity check: sizes must match
    int srcWidth = src.bit >= 0 ? 1 : src.width.size();
    int ioWidth = bit >= 0 ? 1 : io.width;
    if (srcWidth != ioWidth) {
      err.AddError("INTERNAL ERROR: Component is %d bits, but I/O resource is %d bits.",
          srcWidth, ioWidth);
      return;
    }
    // remove existing mappings to same I/O resource or from same component
    HashSet<Source> modified = new HashSet<>();
    mappings.entrySet().removeIf(e -> {
      Source s = e.getKey();
      Dest d = e.getValue();
      boolean conflict = (mappings.get(s).io == io)
          || (s.path.equals(src.path) && (src.bit < 0 || s.bit < 0 || src.bit == s.bit));
      if (confict)
        modified.add(s);
      return conflict;
    });
    modified.add(src);
    mappings.put(src, new Dest(io, bit));
    return modified;
  }

  public String getStatus() { // result begins with "All" if and only if everything mapped
    int remaining = 0, count = 0;
    for (Path path : pinBindings.components.keySet()) {
      count++;
      if (!pinBindings.isMapped(path))
        remaining++;
    }
    finished = (remaining == 0);
    status.setForeground(finished ? Color.GREEN.darker() : Color.BLUE);
    if (remaining == 0)
      return String.format("All %d components are mapped to I/O resources", count);
    else
      return String.format("%d of %d components remaining to be mapped to I/O resources",
          remaining, count);
  }

  public boolean allPinsAssigned() {
    for (Path path : pinBindings.components.keySet())
      if (!isMapped(path))
        return false;
    return true;
  }

// 	public int GetFPGAInOutPinId(String MapName) {
// 		if (fpgaInOutsList.containsKey(MapName)) {
// 			return fpgaInOutsList.get(MapName);
// 		}
// 		return -1;
// 	}
// 
// 	public int GetFPGAInputPinId(String MapName) {
// 		if (fpgaInputsList.containsKey(MapName)) {
// 			return fpgaInputsList.get(MapName);
// 		}
// 		return -1;
// 	}
// 
// 	public int GetFPGAOutputPinId(String MapName) {
// 		if (fpgaOutputsList.containsKey(MapName)) {
// 			return fpgaOutputsList.get(MapName);
// 		}
// 		return -1;
// 	}

//  // This is used by download scripts for final HDL synthesis tool configuration.
//	public ArrayList<String> GetFPGAPinLocs(int FPGAVendor) {
//		ArrayList<String> Contents = new ArrayList<>();
//		for (String Map : fpgaInputsList.keySet()) {
//			int InputId = fpgaInputsList.get(Map);
//			if (!mappedList.containsKey(Map)) {
//				System.err.printf("No mapping found for %s\n", Map);
//				return Contents;
//			}
//			Bounds rect = mappedList.get(Map);
//      if (rect.isDeviceSignal()) {
//        BoardIO Comp = board.GetComponent(rect);
//        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "in", InputId));
//      } else {
//        return null;
//      }
//		}
//		for (String Map : fpgaInOutsList.keySet()) {
//			int InOutId = fpgaInOutsList.get(Map);
//			if (!mappedList.containsKey(Map)) {
//				System.err.printf("No mapping found for %s\n", Map);
//				return Contents;
//			}
//			Bounds rect = mappedList.get(Map);
//      if (rect.isDeviceSignal()) {
//        BoardIO Comp = board.GetComponent(rect);
//        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "inout", InOutId));
//      } else {
//        return null;
//      }
//		}
//		for (String Map : fpgaOutputsList.keySet()) {
//			int OutputId = fpgaOutputsList.get(Map);
//			if (!mappedList.containsKey(Map)) {
//				System.err.printf("No mapping found for %s\n", Map);
//				return Contents;
//			}
//			Bounds rect = mappedList.get(Map);
//      if (rect.isDeviceSignal()) {
//        BoardIO Comp = board.GetComponent(rect);
//        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "out", OutputId));
//      } else {
//        return null;
//      }
//		}
//		return Contents;
//	}

}
