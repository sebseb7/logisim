/**;
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

import static com.bfh.logisim.netlist.Netlist.Int3;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.bfh.logisim.hdlgenerator.HiddenPort;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;

// For each real Component within a Circuit, a shadow NetlistComponent holds
// HDL-related information about the Component, including the HDL generator,
// info about hidden ports (if any), assigned IDs for those hidden ports, which
// type of mapping (main or one of the alternates) is enabled, etc.
public class NetlistComponent {

  // A set of ranges [end:start] of bit indices for hidden in/inout/out ports.
  public static class Range3 {
    public Int3 end, start;
    public Range3() {
      end = new Int3();
      start = new Int3();
    }
    void update(Int3 start, Int3 count) {
      this.start = start.copy();
      end.in = start.in + count.in - 1;
      end.inout = start.inout + count.inout - 1;
      end.out = start.out + count.out - 1;
    }
    public Range3 copy() {
      Range3 r = new Range3();
      r.end = end.copy();
      r.start = start.copy();
      return r;
    }
    @Override
    public String toString() {
      return String.format("{in:%s, inout:%s, out:%s}",
          end.in >= start.in ? end.in +".."+start.in : "-",
          end.inout >= start.inout ? end.inout +".."+start.inout : "-",
          end.out >= start.out ? end.out +".."+start.out : "-");
    }
  }

  // The real, original logisim component we are shadowing
	public final Component original;

  // The Net connected to each port, if any, in order.
	public final List<Net> portConnections;

  // HDL generation
  public final HDLSupport hdlSupport;
  public final HiddenPort hiddenPort;

  // Bit ranges for hidden ports, relative to the circuit this component is in.
  // All the localIndices for components within a circuit will together be
  // sequentially contiguous starting from 0. For example, if some circuit Foo
  // might have 8 hidden nets because it contains a DipSwitch [7:3] and an
  // RGBLed [2:0]. And a top-level circuit Bar might contains 3 instances of the
  // above circuit Foo, with 24 hidden nets total stemming from Foo2 [23:16],
  // Foo1 [15:8], and Foo0 [7:0].
  private Range3 localIndices = new Range3();

  // Bit ranges for hidden ports, globally unique across all (nested) instances
  // of all components. In the above example, the assignments might be:
  //                        Global   Local
  //   (Bar)                         [23:0]
  //         Foo2                    [23:16]
  //         Foo1                    [15:8]
  //         Foo0                    [7:0]
  //              DipSwitch          [7:3]
  //              RGBLed             [2:0]
  //   (Bar)/Foo2/DipSwitch [23:19]
  //   (Bar)/Foo2/RGBLed    [18:16]
  //   (Bar)/Foo1/DipSwitch [15:10]
  //   (Bar)/Foo1/RGBLed    [9:8]
  //   (Bar)/Foo0/DipSwitch [7:3]
  //   (Bar)/Foo0/RGBLed    [2:0]
	private HashMap<Path, Range3> globalIndices = new HashMap<>();

	public NetlistComponent(Component comp, HDLSupport.HDLCTX ctx) {
    ComponentFactory factory = comp.getFactory();
    AttributeSet attrs = comp.getAttributeSet();

		this.original = comp;

    ArrayList<Net> nets = new ArrayList<>();
    for (EndData end : comp.getEnds())
      nets.add(ctx.nets.netAt.get(end.getLocation()));
    this.portConnections = Collections.unmodifiableList(nets);

    if (factory instanceof Pin) {
      this.hdlSupport = null;
      // // Note: Only top-level circuit Pin components have HiddenPort.
      // this.hiddenPort = isTop ? HiddenPort.forPin(comp) : null;
      this.hiddenPort = null;
    } else {
      this.hdlSupport = factory.getHDLSupport(ctx.lang, ctx.err, ctx.nets, attrs, ctx.vendor);
      this.hiddenPort = hdlSupport != null ? hdlSupport.hiddenPort() : null;
    }
	}

	public void setGlobalHiddenPortIndices(Path path, Int3 start, Int3 count) {
		if (count.size() == 0)
			return;
    Range3 range = new Range3();
    range.update(start, count);
    globalIndices.put(path, range);
	}

	public Range3 getGlobalHiddenPortIndices(Path path) {
    Range3 r = globalIndices.get(path);
    return r == null ? null : r.copy();
	}

	public void setLocalHiddenPortIndices(Int3 start, Int3 count) {
    localIndices.update(start, count);
  }

	public Range3 getLocalHiddenPortIndices() {
    return localIndices.copy();
  }

	public boolean endIsConnected(int index) {
    return index >= 0 && index < portConnections.size() && portConnections.get(index) != null;
	}

	public Net getConnection(int index) {
    return index >= 0 && index < portConnections.size() ? portConnections.get(index) : null;
	}

  public String label() {
    return labelOf(original);
  }

  public static String labelOf(Component comp) {
    return CorrectLabel.getCorrectLabel(comp.getAttributeSet().getValue(StdAttr.LABEL)).toUpperCase();
  }

}
