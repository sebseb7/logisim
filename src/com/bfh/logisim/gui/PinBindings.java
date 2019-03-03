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

package com.bfh.logisim.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.fpga.Board;
import com.bfh.logisim.fpga.BoardRectangle;
import com.bfh.logisim.fpga.BoardIO;
import com.bfh.logisim.fpga.PinActivity;
import com.bfh.logisim.hdlgenerator.HiddenPort;
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
// FPGACommanderGui holds the instance of this, but mostly doesn't use it.
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

  // User-selected type for each path, either the primary or an alternate.
  // private final HashMap<Path, BoardIO.Type> curType = new HashMap<>();

  public static class Source {
    public final Path path;
    public final NetlistComponent comp;
    public final int bit; // -1 means entire NetlistComponent is mapped
    public final BoardIO.Type type;
    public final Int3 width;
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
  }

  public static class Dest {
    public final BoardIO io;
    public final int bit; // -1 means something maps to this entire BoardIO
    public Dest(BoardIO i, int b) { io = i; bit = b; }
  }

  // Mappings defined so far.
  public final HashMap<Source, Dest> mappings = new HashMap<>();

  // A list of displayable names, in nicely presentable order, for all
  // I/O-related components (or bits derived from them), and a boolean
  // indicating whether component is mapped.
  // Names to display on the left side and right side...
  // ///// ?

  public PinBindings(FPGAReport err, Board board, HashMap<Path, NetlistComponent> components) {
    this.err = err;
		this.components = components;
		this.board = board;
    // intitially, each component maps to no I/O resource
    // for (Path path : components.keySet()) {
    //   mappings.put(sourceFor(path), null);
    // }
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
    // sanity check - sizes match
    int srcWidth = src.bit >= 0 ? 1 : src.width.size();
    int ioWidth = bit >= 0 ? 1 : io.width;
    if (srcWidth != ioWidth) {
      err.AddError("INTERNAL ERROR: Component is %d bits, but I/O resource is %d bits.",
          srcWidth, ioWidth);
      return;
    }
    // // annul existing mappings to same I/O resource or from same component
    // for (Source s : mappings.keySet()) {
    //   if (s.path.equals(src.path) && (src.bit < 0 || s.bit < 0 || src.bit == s.bit))
    //     mappings.put(s, null);
    //   else if (mappings.get(s).io == io)
    //     mappings.put(s, null);
    // }
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

  // here ------------------------------------------------------

	public void BuildIOMappingInformation() {
		if (fpgaInputsList == null) {
			fpgaInputsList = new HashMap<String, Integer>();
		} else {
			fpgaInputsList.clear();
		}
		if (fpgaInOutsList == null) {
			fpgaInOutsList = new HashMap<String, Integer>();
		} else {
			fpgaInOutsList.clear();
		}
		if (fpgaOutputsList == null) {
			fpgaOutputsList = new HashMap<String, Integer>();
		} else {
			fpgaOutputsList.clear();
		}
		nrOfFPGAInputPins = 0;
		nrOfFPGAInOutPins = 0;
		nrOfFPGAOutputPins = 0;
		for (Path key : components.keySet()) {
			NetlistComponent comp = components.get(key);
			for (String Map : GetMapNamesList(key, comp)) {
        BoardRectangle r = comp.getMap(Map);
        if (!r.isDeviceSignal())
          continue;
				BoardIO BoardComp = board.GetComponent(r);
				if (BoardComp.GetType().equals(BoardIO.Type.Pin)) {
					if (comp.ports.get(0).isOutput) {
						fpgaInputsList.put(Map, nrOfFPGAInputPins);
						nrOfFPGAInputPins++;
					} else {
						fpgaOutputsList.put(Map, nrOfFPGAOutputPins);
						nrOfFPGAOutputPins++;
					}
				} else {
					int NrOfPins = BoardIO.Type.GetFPGAInputRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaInputsList.put(Map, nrOfFPGAInputPins);
						if (BoardComp.GetType().equals(
								BoardIO.Type.DIPSwitch)) {
							nrOfFPGAInputPins += BoardComp.getNrOfPins();
						} else if (BoardComp.GetType().equals(BoardIO.Type.Ribbon)) {
							nrOfFPGAInputPins += BoardComp.getNrOfPins();
						} else {
							nrOfFPGAInputPins += NrOfPins;
						}
					}
					NrOfPins = BoardIO.Type.GetFPGAOutputRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaOutputsList.put(Map, nrOfFPGAOutputPins);
						nrOfFPGAOutputPins += NrOfPins;
					}
					NrOfPins = BoardIO.Type.GetFPGAInOutRequirement(BoardComp.GetType());
					if (NrOfPins != 0) {
						fpgaInOutsList.put(Map, nrOfFPGAInOutPins);
						if (BoardComp.GetType().equals(BoardIO.Type.Ribbon)) {
							nrOfFPGAInOutPins += BoardComp.getNrOfPins();
						} else {
							nrOfFPGAInOutPins += NrOfPins;
						}
					}
				}
			}
		}
	}

	private String DisplayNametoMapName(String item) {
		String[] parts = item.split(" ");
		if (parts.length != 2) {
			System.err.println("Internal error");
			return "";
		}
		return board.name + ":" + parts[1];
	}

	private int getBestComponent(ArrayList<Integer> list, int requiredPin) {
		int delta = 999;
		int bestMatch = -1;
		for (Integer comp : list) {
			if (comp.equals(requiredPin)) {
				return list.indexOf(comp);
			}
			if (requiredPin < comp && ((comp - requiredPin) < delta)) {
				bestMatch = comp;
			}
		}
		return list.indexOf(bestMatch);
	}

	public NetlistComponent GetComponent(Path hiername) {
		if (components.containsKey(hiername)) {
			return components.get(hiername);
		} else {
			return null;
		}
	}

	public Set<Path> GetComponents() {
		return components.keySet();
	}

	public String GetDisplayName(BoardRectangle rect) {
		for (String Map : mappedList.keySet()) {
			if (mappedList.get(Map).equals(rect)) {
				return MapNametoDisplayName(Map);
			}
		}
		return "";
	}

	public int GetFPGAInOutPinId(String MapName) {
		if (fpgaInOutsList.containsKey(MapName)) {
			return fpgaInOutsList.get(MapName);
		}
		return -1;
	}

	public int GetFPGAInputPinId(String MapName) {
		if (fpgaInputsList.containsKey(MapName)) {
			return fpgaInputsList.get(MapName);
		}
		return -1;
	}

	public int GetFPGAOutputPinId(String MapName) {
		if (fpgaOutputsList.containsKey(MapName)) {
			return fpgaOutputsList.get(MapName);
		}
		return -1;
	}

  // This is used by download scripts for final HDL synthesis tool configuration.
	public ArrayList<String> GetFPGAPinLocs(int FPGAVendor) {
		ArrayList<String> Contents = new ArrayList<>();
		for (String Map : fpgaInputsList.keySet()) {
			int InputId = fpgaInputsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				System.err.printf("No mapping found for %s\n", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
      if (rect.isDeviceSignal()) {
        BoardIO Comp = board.GetComponent(rect);
        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "in", InputId));
      } else {
        return null;
      }
		}
		for (String Map : fpgaInOutsList.keySet()) {
			int InOutId = fpgaInOutsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				System.err.printf("No mapping found for %s\n", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
      if (rect.isDeviceSignal()) {
        BoardIO Comp = board.GetComponent(rect);
        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "inout", InOutId));
      } else {
        return null;
      }
		}
		for (String Map : fpgaOutputsList.keySet()) {
			int OutputId = fpgaOutputsList.get(Map);
			if (!mappedList.containsKey(Map)) {
				System.err.printf("No mapping found for %s\n", Map);
				return Contents;
			}
			BoardRectangle rect = mappedList.get(Map);
      if (rect.isDeviceSignal()) {
        BoardIO Comp = board.GetComponent(rect);
        Contents.addAll(Comp.getPinAssignments(FPGAVendor, "out", OutputId));
      } else {
        return null;
      }
		}
		return Contents;
	}

	private Path GetHierarchyKey(String str) {
		Path result = new ArrayList<String>();
		String[] subtype = str.split("#");
		String[] iotype = subtype[0].split(" ");
		String[] parts = iotype[iotype.length - 1].split("/");
		result.add(board.name);
		for (int i = 1; i < parts.length; i++) {
			result.add(parts[i]);
		}
		return result;
	}

	public BoardRectangle GetMap(String id) {
		Path key = GetHierarchyKey(id);
		NetlistComponent MapComp = components.get(key);
		if (MapComp == null) {
			System.err.println("Internal error!");
			return null;
		}
		return MapComp.getMap(DisplayNametoMapName(id));
	}

	public ArrayList<String> GetMapNamesList(Path HierName) {
		if (components.containsKey(HierName)) {
			NetlistComponent comp = components.get(HierName);
			return GetMapNamesList(HierName, comp);
		}
		return null;
	}

	private ArrayList<String> GetMapNamesList(Path hiername, NetlistComponent comp) {
		ArrayList<String> result = new ArrayList<String>();
		/* we strip off the board path and add the component type */
		String path = board.name + ":";
		for (int i = 1; i < hiername.size(); i++)
			path += "/" + hiername.get(i);
		if (comp.AlternateMappingEnabled(hiername)) {
			for (String label: comp.hiddenPort.inports)
				result.add(path + "#" + label);
			for (String label: comp.hiddenPort.inoutports)
				result.add(path + "#" + label);
			for (String label: comp.hiddenPort.outports)
				result.add(path + "#" + label);
		} else {
			result.add(path);
		}
		return result;
	}

	public Collection<BoardRectangle> GetMappedRectangles() {
		return mappedList.values();
	}

  public boolean IsDeviceSignal(String MapName) {
		// if (mappedList.containsKey(MapName))
    return mappedList.get(MapName).isDeviceSignal();
    // return false;
  }

  public int GetSyntheticInputValue(String MapName) {
		// if (mappedList.containsKey(MapName))
    return mappedList.get(MapName).getSyntheticInputValue();
    // return 0;
  }

  public int GetNrOfSyntheticBits(String MapName) {
		// if (mappedList.containsKey(MapName))
    return mappedList.get(MapName).GetNrOfSyntheticBits();
    // return 0;
  }

  public boolean IsDisconnectedOutput(String MapName) {
		// if (mappedList.containsKey(MapName))
    return mappedList.get(MapName).isDisconnectedOutput();
  }

	public int GetNrOfPins(String MapName) {
		if (mappedList.containsKey(MapName)) {
      BoardRectangle rect = mappedList.get(MapName);
      if (!rect.isDeviceSignal())
        return -1;
      BoardIO BoardComp = board.GetComponent(rect);
			if (BoardComp.GetType().equals(BoardIO.Type.DIPSwitch)) {
				return BoardComp.getNrOfPins();
			} else if (BoardComp.GetType().equals(BoardIO.Type.Ribbon)) {
				return BoardComp.getNrOfPins();
			} else {
				return BoardIO.Type.GetNrOfFPGAPins(BoardComp.GetType());
			}
		}
		return 0;
	}

	public int GetNrOfToplevelInOutPins() {
		return nrOfFPGAInOutPins;
	}

	public int GetNrOfToplevelInputPins() {
		return nrOfFPGAInputPins;
	}

	public int GetNrOfToplevelOutputPins() {
		return nrOfFPGAOutputPins;
	}

  public HiddenPort getTypeFor(String DisplayName) {
		Path key = GetHierarchyKey(DisplayName);
		NetlistComponent comp = components.get(key);
    return comp.hiddenPort;
  }

	public ArrayList<BoardRectangle> GetSelectableItemsList(String DisplayName,
			Board BoardInfo) {
		ArrayList<BoardRectangle> rects;
		Path key = GetHierarchyKey(DisplayName);
		NetlistComponent comp = components.get(key);
		int pinNeeded = comp.hiddenPort.numPorts();
		// First try main map types
		if (!comp.AlternateMappingEnabled(key)) {
			rects = BoardInfo.GetIoComponentsOfType(comp.hiddenPort.mainType, pinNeeded);
			if (!rects.isEmpty())
				return RemoveUsedItems(rects);
		}
    // If no matching resources, try all alternate types
		rects = new ArrayList<BoardRectangle>();
    for (BoardIO.Type mapType: comp.hiddenPort.altTypes)
			rects.addAll(BoardInfo.GetIoComponentsOfType(mapType, 0));
		return RemoveUsedItems(rects);
	}

	private ArrayList<BoardRectangle> RemoveUsedItems(ArrayList<BoardRectangle> rects) {
		Iterator<BoardRectangle> ListIterator = rects.iterator();
		while (ListIterator.hasNext()) {
			BoardRectangle current = ListIterator.next();
			if (mappedList.containsValue(current))
				ListIterator.remove();
		}
		return rects;
	}


	public boolean hasMappedComponents() {
		return !mappedList.isEmpty();
	}

	public void Map(String comp, BoardRectangle item /*, String Maptype*/) {
		Path key = GetHierarchyKey(comp);
		NetlistComponent MapComp = components.get(key);
		if (MapComp == null) {
			System.err.printf("Internal error! comp: %s, key: %s\n", comp, key);
			return;
		}
    if (!item.isDeviceSignal()) {
      item.SetNrOfSyntheticBits(MapComp.hiddenPort.numPorts());
    }
		MapComp.addMap(DisplayNametoMapName(comp), item /*, Maptype */);
		rebuildMappedLists();
	}

	private String MapNametoDisplayName(String item) {
		String[] parts = item.split(":");
		if (parts.length != 2) {
			System.err.println("Internal error!");
			return "";
		}
		Path key = GetHierarchyKey(parts[1]);
		if (key != null) {
			return components.get(key).hiddenPort.mainType.toString().toUpperCase() + ": " + parts[1];
		}
		return "";
	}

	public Set<String> MappedList() {
		SortedSet<String> result = new TreeSet<String>(new NaturalOrderComparator());
		for (String MapName : mappedList.keySet()) {
			result.add(MapNametoDisplayName(MapName));
		}
		return result;
	}

	public boolean RequiresToplevelInversion(
			Path ComponentIdentifier, String MapName) {
		if (!mappedList.containsKey(MapName)) {
			return false;
		}
		if (!components.containsKey(ComponentIdentifier)) {
			return false;
		}
    BoardRectangle r = mappedList.get(MapName);
    boolean BoardActiveHigh;
    if (r.isDeviceSignal()) {
      BoardIO BoardComp = board.GetComponent(r);
      BoardActiveHigh = (BoardComp.GetActivityLevel() == PinActivity.ACTIVE_HIGH);
    } else {
      BoardActiveHigh = true;
    }
		NetlistComponent Comp = components.get(ComponentIdentifier);
		boolean CompActiveHigh = Comp.GetComponent().getFactory().ActiveOnHigh(Comp.GetComponent().getAttributeSet());
		boolean Invert = BoardActiveHigh ^ CompActiveHigh;
		return Invert;
	}

	public void ToggleAlternateMapping(String item) {
		Path key = GetHierarchyKey(item);
		NetlistComponent comp = components.get(key);
		if (comp != null) {
			if (comp.AlternateMappingEnabled(key)) {
				for (String MapName : GetMapNamesList(key, comp)) {
					if (mappedList.containsKey(MapName)) {
						return;
					}
				}
			}
			comp.ToggleAlternateMapping(key);
		}
	}

	public void TryMap(String DisplayName, BoardRectangle rect /*, String Maptype*/) {
		Path key = GetHierarchyKey(DisplayName);
		if (!components.containsKey(key)) {
			return;
		}
		if (UnmappedList().contains(DisplayName)) {
			Map(DisplayName, rect /*, Maptype*/);
			return;
		}
		components.get(key).ToggleAlternateMapping(key);
		if (UnmappedList().contains(DisplayName)) {
			Map(DisplayName, rect /*, Maptype*/);
			return;
		}
		components.get(key).ToggleAlternateMapping(key);
	}

	public void UnMap(String comp) {
		Path key = GetHierarchyKey(comp);
		NetlistComponent MapComp = components.get(key);
		if (MapComp == null) {
			System.err.println("Internal error!");
			return;
		}
		MapComp.removeMap(DisplayNametoMapName(comp));
		rebuildMappedLists();
	}

	public void UnmapAll() {
		for (Path key : components.keySet()) {
			if (key.get(0).equals(board.name)) {
				NetlistComponent comp = components.get(key);
				for (String MapName : GetMapNamesList(key, comp)) {
					comp.removeMap(MapName);
				}
			}
		}
	}

	public Set<String> UnmappedList() {
		SortedSet<String> result = new TreeSet<String>(new NaturalOrderComparator());

		for (Path key : components.keySet()) {
			for (String MapName : GetMapNamesList(key,
					components.get(key))) {
				if (!mappedList.containsKey(MapName)) {
					result.add(MapNametoDisplayName(MapName));
				}
			}
		}
		return result;
	}

}

	// private Map<String, BoardRectangle> mappedList;
	// private Map<String, Integer> fpgaInputsList;
	// private Map<String, Integer> fpgaInOutsList;
	// private Map<String, Integer> fpgaOutputsList;
	// private Integer nrOfFPGAInputPins = 0;
	// private Integer nrOfFPGAInOutPins = 0;
	// private Integer nrOfFPGAOutputPins = 0;
	// There are two different notations for each component:
  // (1) The display name. Example: "LED: /Some/Circ/LED1" 
  //     This name can be augmented with alternates. For example,
  //     a 7-segment display could be shown with variations:
  //       "SEVENSEGMENT: /Some/Circ/DS1"
  //       "SEVENSEGMENT: /Some/Circ/DS1#Segment_A"
  //       "SEVENSEGMENT: /Some/Circ/DS1#Segment_B"
	// (2) The map name. Examples:
  //     "FPGA4U:/Some/Circ/LED1"
  //     "FPGA4U:/Some/Circ/DS1#Segment_A", etc.
	// The mappedList keeps track of the display names.
	public void rebuildMappedLists() {
		mappedList.clear();
		for (Path key : components.keySet()) {
			if (key.get(0).equals(board.name)) {
				NetlistComponent comp = components.get(key);
				/*
				 * we can have two different situations:
         *   1) A multipin component is mapped to a multipin resource.
         *   2) A multipin component is mapped to multiple singlepin resources.
				 */
				/* first we handle the single pin version */
				boolean hasmap = false;
				for (String MapName : GetMapNamesList(key, comp)) {
					if (comp.getMap(MapName) != null) {
						hasmap = true;
						mappedList.put(MapName, comp.getMap(MapName));
					}
				}
				if (!hasmap) {
					comp.ToggleAlternateMapping(key);
					for (String MapName : GetMapNamesList(key, comp)) {
						if (comp.getMap(MapName) != null) {
							hasmap = true;
							mappedList.put(MapName, comp.getMap(MapName));
						}
					}
					if (!hasmap) {
						comp.ToggleAlternateMapping(key);
					}
				}
			}
		}
	}
