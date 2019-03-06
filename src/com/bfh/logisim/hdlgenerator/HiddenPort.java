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
import java.util.Collections;
import java.util.List;

import com.bfh.logisim.fpga.BoardIO;
import com.cburch.logisim.comp.Component;
import static com.bfh.logisim.netlist.Netlist.Int3;

public class HiddenPort {

  // These are here simply for convenience in generator classes.
  public static final BoardIO.Type LED = BoardIO.Type.LED;
  public static final BoardIO.Type Button = BoardIO.Type.Button;
  public static final BoardIO.Type Pin = BoardIO.Type.Pin;
  public static final BoardIO.Type SevenSegment = BoardIO.Type.SevenSegment;
  public static final BoardIO.Type DIPSwitch = BoardIO.Type.DIPSwitch;
  public static final BoardIO.Type RGBLED = BoardIO.Type.RGBLED;
  public static final BoardIO.Type Ribbon = BoardIO.Type.Ribbon;
  
  public final List<String> inports, inoutports, outports;
  public final BoardIO.Type mainType;
  public final List<BoardIO.Type> altTypes;
  public final List<BoardIO.Type> types; // main + alternates

  // So far, all hidden ports are pure input, pure output, or pure inout. There
  // are no mixed-direction hidden ports. So we provide convenience
  // constructors for these cases.

  public static HiddenPort makeInport(List<String> labels, BoardIO.Type mainType, BoardIO.Type ...altTypes)    { return new HiddenPort(labels, null, null, mainType, altTypes); }
  public static HiddenPort makeInOutport(List<String> labels, BoardIO.Type mainType, BoardIO.Type ...altTypes) { return new HiddenPort(null, labels, null, mainType, altTypes); }
  public static HiddenPort makeOutport(List<String> labels, BoardIO.Type mainType, BoardIO.Type ...altTypes)   { return new HiddenPort(null, null, labels, mainType, altTypes); }
  public static HiddenPort makeInport(int width, BoardIO.Type mainType, BoardIO.Type ...altTypes)    { return new HiddenPort(width, 0, 0, mainType, altTypes); }
  public static HiddenPort makeInOutport(int width, BoardIO.Type mainType, BoardIO.Type ...altTypes) { return new HiddenPort(0, width, 0, mainType, altTypes); }
  public static HiddenPort makeOutport(int width, BoardIO.Type mainType, BoardIO.Type ...altTypes)   { return new HiddenPort(0, 0, width, mainType, altTypes); }

  public static HiddenPort forPin(Component pin) {
    int w = pin.getEnd(0).getWidth().getWidth();
    boolean o = pin.getEnd(0).isInput(); // input from circuit means output to fpga
    boolean i = pin.getEnd(0).isOutput(); // output to circuit means input from fpga
    if (i && o && w > 1)
      return makeInOutport(w, Ribbon, Pin);
    else if (i && o)
      return makeInOutport(w, Pin);
    else if (i && w > 1)
      return makeInport(w, Ribbon, Pin);
    else if (i)
      return makeInport(w, Pin);
    else if (o && w > 1)
      return makeOutport(w, Ribbon, Pin);
    else if (o)
      return makeOutport(w, Pin);
    else
      return null;
  }

	private HiddenPort(List<String> inports, List<String> inoutports, List<String> outports, BoardIO.Type mainType, BoardIO.Type ...altTypes) {
    this.inports = Collections.unmodifiableList(inports != null ? inports : new ArrayList<String>());
    this.inoutports = Collections.unmodifiableList(inoutports != null ? inoutports : new ArrayList<String>());
    this.outports = Collections.unmodifiableList(outports != null ? outports : new ArrayList<String>());
		this.mainType = mainType;
    Arraylist<BoardIO.Type> alt = new ArrayList<>();
    alt.addAll(altTypes);
    altTypes = Collections.unmodifiableList(alt);
    Arraylist<BoardIO.Type> all = new ArrayList<>();
    all.add(mainType);
    all.addAll(altTypes);
    types = Collections.unmodifiableList(all);
	}

	private HiddenPort(int inports, int inoutport, int outports, BoardIO.Type mainType, BoardIO.Type ...altTypes) {
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
