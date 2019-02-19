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

import static com.cburch.logisim.std.io.PortIO.ATTR_SIZE;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;

import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.hdlgenerator.HiddenPort;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.std.wiring.Pin;

// For each real component, we make a shadow NetlistComponent that holds
// HDL-related information, including the HDL generator, info about hidden ports
// (if any), assigned bubble IDs for those hidden ports, which type of mapping
// (main or one of the alternates) is enabled, etc.
public class NetlistComponent {

  // A range [end:start] of bit indices for a port or bus.
  public static class PortIndices {
    public final int end, start;
    public PortIndices(int e, int s) { end = e; start = s; }
    public static final PortIndices EMPTY = new PortIndices(0, 0);
  }

  public static class PortIndices3 {
    public final PortIndices in, inout, out;
    public PortIndices3(PortIndices in, PortIndices inout, PortIndices out) {
      this.in = in;
      this.inout = inout;
      this.out = out;
    }
  }

  // The real, original logisim component we are shadowing
	private Component original;

  // Shadow data for normal ports
	public final List<ConnectionEnd> ports;

  // HDL generation
  public final HDLSupport hdlSupport;
  public final HiddenPort hiddenPort;

  // Bit ranges for hidden ports
	private HashMap<Path, PortIndices3> globalIndices = new HashMap<>();
  private PortIndices3 localIndices = new PortIndices3(
      PortIndices.EMPTY, PortIndices.EMPTY, PortIndices.EMPTY);

  // Board mapping info
	private HashMap<String, BoardRectangle> boardMaps = new HashMap<>();
	private HashMap<Path, Boolean> altEnabled = new HashMap<>();
	private HashMap<Path, Boolean> altLocked = new HashMap<>();

	public NetlistComponent(Component comp, HDLCTX ctx) {
    ComponentFactory factory = Ref.getFactory();
    AttributeSet attrs = Ref.getAttributeSet();

		this.original = comp;

    ArrayList<ConnectionEnd> ports = new ArrayList<>();
    for (EndData end : comp.getEnds())
			ports.add(new ConnectionEnd(end.isOutput(), (byte) end.getWidth().getWidth()));
    this.ports = Collections.unmodifiableList(ports);

    if (factory instanceof Pin) {
      this.hdlSupport = null;
      this.hiddenPort = HiddenPort.forPin(comp);
    } else {
      this.hdlSupport = factory.getHDLSupport(ctx.lang, ctx.err, ctx.nets, attrs, _vendor);
      this.hiddenPort = hdlSupport != null ? hdlSupport.hiddenPort : null;
    }
	}

	public void setGlobalHiddenPortIndices(Path path,
			int inStart, int inCount,
			int inoutStart, int inoutCount
			int outStart, int outCount) {
		if ((inCount == 0) && (inoutCount == 0) && (outCount == 0))
			return;
    globalIndices.put(path,
        new PortIndices3(
          new PortIndices(inStart, inStart + inCount - 1),
          new PortIndices(inoutStart, inoutStart + inoutCount - 1),
          new PortIndices(outStart, outStart + outCount - 1)));
	}

	public BubbleInformationContainer getGlobalHiddenPortIndices(Path path) {
    return globalIndices.get(path);
	}

	public void setLocalHiddenPortIndices(
      int inStart, inCount,
      int inoutStart, inoutCount,
      int outStart, outCount) {
    localIndices = new PortIndices3(
        new PortIndices(inStart, inStart + inCount - 1),
        new PortIndices(inoutStart, inoutStart + inoutCount - 1),
        new PortIndices(outStart, outStart + outCount - 1));
	}

	public PortIndices3 getLocalHiddenPortIndices() {
    return localIndices;
  }

	public void addMap(String MapName, BoardRectangle map) {
		boardMaps.put(MapName, map);
	}

	public boolean AlternateMappingEnabled(Path key) {
		if (!altEnabled.containsKey(key)) {
			altEnabled.put(key, hiddenPort.mainType == HiddenPort.Bus);
			altLocked.put(key, hiddenPort.mainType == HiddenPort.Bus);
		}
		return altEnabled.get(key);
	}

	public boolean AlternateMappingIsLocked(Path key) {
		if (!altLocked.containsKey(key)) {
			altEnabled.put(key, hiddenPort.mainType == HiddenPort.Bus);
			altLocked.put(key, hiddenPort.mainType == HiddenPort.Bus);
    }
		return altLocked.get(key);
	}

	public void ToggleAlternateMapping(Path key) {
		boolean newIsLocked = hiddenPort.mainType == HiddenPort.Bus;
		if (altLocked.containsKey(key)) {
			if (altLocked.get(key))
				return;
		} else {
			altLocked.put(key, newIsLocked);
			if (newIsLocked)
				return;
		}
		if (!altEnabled.containsKey(key))
			altEnabled.put(key, true);
		if (altEnabled.get(key))
			altEnabled.put(key, false);
		else if (!hiddenPort.altTypes.isEmpty())
      altEnabled.put(key, true);
	}

	public void UnlockAlternateMapping(Path key) {
    if (hiddenPort.mainType != HiddenPort.Bus)
      altLocked.put(key, false);
	}

	public boolean endIsConnected(int index) {
		if (index < 0 || index >= ports.size())
			return false;
    return ports.get(index).isConnected();
	}

	public Component GetComponent() {
		return original;
	}

	public byte GetConnectionBitIndex(Net rootNet, byte BitIndex) {
		for (ConnectionEnd p : ports) {
			for (byte bit = 0; bit < p.NrOfBits(); bit++) {
				ConnectionPoint connection = p.GetConnection(bit);
				if (connection.GetParrentNet() == rootNet
						&& connection.GetParrentNetBitIndex() == BitIndex) {
					return bit;
				}
			}
		}
		return -1;
	}

	public ArrayList<ConnectionPoint> GetConnections(Net rootNet, byte BitIndex, boolean IsOutput) {
		ArrayList<ConnectionPoint> Connections = new ArrayList<>();
		for (ConnectionEnd p : Ends) {
			for (byte bit = 0; bit < p.NrOfBits(); bit++) {
				ConnectionPoint connection = p.GetConnection(bit);
				if (connection.GetParrentNet() == rootNet
						&& connection.GetParrentNetBitIndex() == BitIndex
						&& p.IsOutputEnd() == IsOutput) {
					Connections.add(connection);
				}
			}
		}
		return Connections;
	}

	public void LockAlternateMapping(Path key) {
		altLocked.put(key, true);
	}

	public void removeMap(String MapName) {
		boardMaps.remove(MapName);
	}


}
