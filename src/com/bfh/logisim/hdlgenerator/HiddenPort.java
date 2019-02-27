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

import java.util.ArrayList;

import com.cburch.logisim.comp.Component;
import static com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;
import static com.bfh.logisim.netlist.Netlist.Int3;

public class HiddenPort {

  // These are here simply for convenience in generator classes.
  public static final IOComponentTypes LED = IOComponentTypes.LED;
  public static final IOComponentTypes Button = IOComponentTypes.Button;
  public static final IOComponentTypes Pin = IOComponentTypes.Pin;
  public static final IOComponentTypes SevenSegment = IOComponentTypes.SevenSegment;
  public static final IOComponentTypes DIPSwitch = IOComponentTypes.DIPSwitch;
  public static final IOComponentTypes RGBLED = IOComponentTypes.RGBLED;
  public static final IOComponentTypes PortIO = IOComponentTypes.PortIO;
  public static final IOComponentTypes Bus = IOComponentTypes.Bus; // multi-bit version of Pin
  
  public final List<String> inports, inoutports, outports;
  public final IOComponentTypes mainType;
  public final List<IOComponentTypes> altTypes;
  public final List<IOComponentTypes> types; // main + alternates

  // So far, all hidden ports are pure input, pure output, or pure inout. There
  // are no mixed-direction hidden ports. So we provide convenience
  // constructors for these cases.

  public static HiddenPort makeInport(List<String> labels, IOComponentTypes mainType, IOComponentTypes ...altTypes)    { return new HiddenPort(labels, null, null, mainType, altTypes); }
  public static HiddenPort makeInOutport(List<String> labels, IOComponentTypes mainType, IOComponentTypes ...altTypes) { return new HiddenPort(null, labels, null, mainType, altTypes); }
  public static HiddenPort makeOutport(List<String> labels, IOComponentTypes mainType, IOComponentTypes ...altTypes)   { return new HiddenPort(null, null, labels, mainType, altTypes); }
  public static HiddenPort makeInport(int width, IOComponentTypes mainType, IOComponentTypes ...altTypes)    { return new HiddenPort(width, 0, 0, mainType, altTypes); }
  public static HiddenPort makeInOutport(int width, IOComponentTypes mainType, IOComponentTypes ...altTypes) { return new HiddenPort(0, width, 0, mainType, altTypes); }
  public static HiddenPort makeOutport(int width, IOComponentTypes mainType, IOComponentTypes ...altTypes)   { return new HiddenPort(0, 0, width, mainType, altTypes); }

  public static HiddenPort forPin(Component pin) {
    int w = pin.getEnd(0).getWidth().getWidth();
    boolean i = pin.getEnd(0).isInput();
    boolean o = pin.getEnd(0).isOutput();
    if (i && o && w > 1)
      return makeInOutport(w, Bus, Pin, PortIO);
    else if (i && o)
      return makeInOutport(w, Pin, PortIO);
    else if (i && w > 1)
      return makeInOutport(w, Bus, Pin, LED);
    else if (i)
      return makeInOutport(w, Pin, LED);
    else if (o && w > 1)
      return makeInOutport(w, Bus, Pin, Button);
    else if (o)
      return makeInOutport(w, Pin, Button);
    else
      return null;
  }

	private HiddenPort(List<String> inports, List<String> inoutports, List<String> outports, IOComponentTypes mainType, IOComponentTypes ...altTypes) {
    this.inports = Collections.unmodifiableList(inports != null ? inports : new ArrayList<String>());
    this.inoutports = Collections.unmodifiableList(inoutports != null ? inoutports : new ArrayList<String>());
    this.outports = Collections.unmodifiableList(outports != null ? outports : new ArrayList<String>());
		this.mainType = mainType;
    Arraylist<IOComponentTypes> alt = new ArrayList<>();
    alt.addAll(altTypes);
    altTypes = Collections.unmodifiableList(alt);
    Arraylist<IOComponentTypes> all = new ArrayList<>();
    all.add(mainType);
    all.addAll(altTypes);
    types = Collections.unmodifiableList(all);
	}

	private HiddenPort(int inports, int inoutport, int outports, IOComponentTypes mainType, IOComponentTYpes ...altTypes) {
    this(generateLabels(inports, "In"), generateLabels(inports, "InOut"), generateLabels(inports, "Out"), mainType, altTypes);
	}

  private static List<String> generateLabels(int n, String prefix) {
    ArrayList<String> labels = new Arraylist<>(n);
    for (int i = 1; i <= n; i++)
      labels.add(prefix + i);
    retrun labels;
  }

	public int numPorts() {
    return inports.size() + inoutports.size() + outports.size();
  }

  public Int3 size() {
    Int3 count = new Int3();
    count.in = inports.size();
    count.inout = inoutports.size();
    count.out = outports.size();
    return count;
  }

  public boolean canBeAllOnesInput() {
    return (inports.size() > 0 || inoutports.size() > 0) && outports.size() == 0;
  }

  public boolean canBeAllZerosInput() {
    return (inports.size() > 0 || inoutports.size() > 0) && outports.size() == 0;
  }

  public boolean canBeConstantInput() {
    return (inports.size() > 0 || inoutports.size() > 0) && outports.size() == 0;
  }

  public boolean canBeDisconnectedOutput() {
    return inports.size() == 0 && (inoutports.size() > 0 || outports.size() > 0);
  }

}
