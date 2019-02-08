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
package com.cburch.logisim.std.gates;

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class GateVhdlGenerator extends AbstractHDLGeneratorFactory {

  protected final static int GENERIC_PARAM_BUSWIDTH = -1;
  protected final static int GENERIC_PARAM_INPUTNEGATIONS = -2;

  protected final String op; // unused for unary ops
  protected final boolean identity;
  protected final boolean invertOutput;

  protected GateVhdlGenerator(String name, HDLCTX ctx, boolean invertOutput) { // unary ops
    super(ctx, deriveHDLName(name, ctx.attrs), "GATE");
    this.op = null;
    this.identity = false;
    this.invertOutput = invertOutput;
  }

  protected GateVhdlGenerator(String name, HDLCTX ctx, boolean identity, String op, boolean invertOutput) {
    super(ctx, deriveHDLName(name, ctx.attrs), "GATE");
    this.identity = identity;
    this.op = op;
    this.invertOutput = invertOutput;
  }

  private static String deriveHDLName(String name, AttributeSet attrs) {
    String hdlName = "${BUS}" + name;
    int nports = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    if (nports > 2)
      hdlName += "_" + nports + "_Way"; // ... multi-input versions
    if (attrs.getValue(GateAttributes.ATTR_XOR) == GateAttributes.XOR_ONE)
      hdlName += "_OneHot"; // ... one-hot versions (for xor/xnor gates)
    return hdlName;
  }

  static GateVhdlGenerator forBuffer(HDLCTX ctx) { return new GateVhdlGenerator("NOP_GATE", ctx, false); }
  static GateVhdlGenerator forNot(HDLCTX ctx) { return new GateVhdlGenerator("NOT_GATE", ctx, true); }
  static GateVhdlGenerator forAnd(HDLCTX ctx) { return new GateVhdlGenerator("AND_GATE", ctx, true, "AND", false); }
  static GateVhdlGenerator forNand(HDLCTX ctx) { return new GateVhdlGenerator("NAND_GATE", ctx, true, "AND", true); }
  static GateVhdlGenerator forOr(HDLCTX ctx) { return new GateVhdlGenerator("OR_GATE", ctx, false, "OR", false); }
  static GateVhdlGenerator forNor(HDLCTX ctx) { return new GateVhdlGenerator("NOR_GATE", ctx, false, "OR", true); }
  static GateVhdlGenerator forXor(HDLCTX ctx) { return new GateVhdlGenerator("XOR_GATE", ctx, false, "XOR", false); }
  static GateVhdlGenerator forXnor(HDLCTX ctx) { return new GateVhdlGenerator("XNOR_GATE", ctx, false, "XOR", true); }

  protected void unaryOp(Hdl out) {
    if (!invertOutput)
      out.stmt("Result <= Input_1;");
    else
      out.stmt("Result <= NOT Input_1;");
  }

  protected void binaryOp(Hdl out) {
    if (!invertOutput)
      out.stmt("Result <= s_in_1 %s s_in_2;", op);
    else
      out.stmt("Result <= NOT (s_in_1 %s s_in_2);", op);
  }

  protected void naryOp(Hdl out, int n) {
    if (!invertOutput) {
      out.stmt("Result <= \ts_in_1 %s", op);
      for (int i = 2; i < n; i++)
        out.cont("\ts_in_%d %s", i, op);
      out.cont("\ts_in_%d;", n);
    } else {
      out.stmt("Result <= NOT (\ts_in_1 %s", op);
      for (int i = 2; i < n; i++)
        out.cont("\ts_in_%d %s", i, op);
      out.cont("\ts_in_%d);", n);
    }
  }

  protected void oneHot(Hdl out, int n) {
    if (n == 3 && !invertOutput) {
      out.stmt("Result <= \t(s_in_1 AND NOT(s_in_2) AND NOT(s_in_3)) OR");
      out.cont("\t(NOT(s_in_1) AND s_in_2 AND NOT(s_in_3)) OR");
      out.cont("\t(NOT(s_in_1) AND NOT(s_in_2) AND s_in_3);");
    } else if (n == 3 && invertOutput) {
      out.stmt("Result <= NOT (\t(s_in_1 AND NOT(s_in_2) AND NOT(s_in_3)) OR");
      out.cont("\t(NOT(s_in_1) AND s_in_2 AND NOT(s_in_3)) OR");
      out.cont("\t(NOT(s_in_1) AND NOT(s_in_2) AND s_in_3));");
    } else {
      if (!invertOutput)
        out.stmt("Result <= \t(\ts_in_1 AND");
      else
        out.stmt("Result <= NOT \t(\ts_in_1 AND");
      for (int i = 2; i < n; i++)
        out.cont("\t\tNOT(s_in_%d) AND", i);
      out.cont("\t\tNOT(s_in_%d)) OR", n);
      for (int j = 2; j < n; j++) {
        out.cont("\t(\tNOT(s_in_1) AND");
        for (int i = 2; i < n; i++) {
          if (i == j)
            out.cont("\t\ts_in_%d AND", i);
          else
            out.cont("\t\tNOT(s_in_%d) AND", i);
        }
        out.cont("\t\tNOT(s_in_%d)) OR", n);
      }
      out.cont("\t(\tNOT(s_in_1) AND");
      for (int i = 2; i < n; i++)
        out.cont("\t\tNOT(s_in_%d) AND", i);
      if (!invertOutput)
        out.cont("\t\ts_in_%d);", n);
      else
        out.cont("\t\ts_in_%d));", n);
    }
  }

  protected void doInputInversions(Hdl out, int n) {
    out.stmt("s_mask <= std_logic_vector(to_unsigned(InputNegations, %d));", n);
    for (int i = 1; i <= n; i++)
      out.stmt("s_in_%d <= NOT(Input_%d) WHEN s_mask(%d) = '1' ELSE Input_%d;", i, i, i, i);
  }

  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    out.indent();

    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);

    if (n > 1)
      doInputInversions(out, n);

    if (n == 1)
      unaryOp(out);
    else if (n == 2)
      binaryOp(out);
    else if (attrs.getValue(GateAttributes.ATTR_XOR) == GateAttributes.XOR_ODD)
      oneHot(out, n);
    else 
      naryOp(out, n);
  }

  // Subdirectory where files will be saved.
  @Override
  public String GetSubDir() { return "gates"; }

  // {SignalName, SignalWidth} for each input port, where negative SignalWidth
  // means use width is defined by corresponding generic parameter.
  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    for (int i = 1; i <= n; i++)
      list.put("Input_"+i, w);
  }

  // {SignalName, SignalWidth} for each output port, where negative SignalWidth
  // means use width is defined by corresponding generic parameter.
  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    int w = width(attrs) > 1 ? GENERIC_PARAM_BUSWIDTH : 1;
    list.put("Result", w);
  }

  // {ID, ParameterName} for each generic parameter, where ID is negative.
  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    if (width(attrs) > 1)
      list.put(GENERIC_PARAM_BUSWIDTH, "BusWidth");
    if (n > 1)
      list.put(GENERIC_PARAM_INPUTNEGATIONS, "InputNegations");
  }

  // {ParameterName, ParameterValue} for each generic parameter, where
  // ParameterValue is a constant.
  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();

    if (width(attrs) > 1)
      list.put("BusWidth", width(attrs));

    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    if (n > 1) {
      int mask = 0;
      for (int i = 1; i <= n; i++)
        if (invertedInput(attrs, i))
          mask |= 1 << (i-1);
      list.put("InputNegations", mask);
    }
  }

  // {PortName, PortValue} for each input/output port, where PortValue can be a
  // net name, "OPEN", zeros, concatenated signals to form a bus, etc.
  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    for (int i = 1; i <= n; i++) {
      boolean inputWhenFloating = invertedInput(attrs, i) ^ identity;
      list.putAll(GetNetMap("Input_"+i, inputWhenFloating, info, i, err, lang, nets));
    }
    list.putAll(GetNetMap("Result", true, info, 0, err, lang, nets));
  }

  // {SignalName, SignalWidth} for signals internally by this entity, where
  // negative SignalWidth means use width is defined by corresponding generic parameter.
  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    int n = attrs.getValueOrElse(GateAttributes.ATTR_INPUTS, 1);
    if (n > 1) {
      boolean is_bus = width(attrs) > 1;
      for (int i = 1; i <= n; i++)
        list.put("s_in_"+i, is_bus ? GENERIC_PARAM_BUSWIDTH : 1);
      list.put("s_mask", n);
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

  protected boolean invertedInput(AttributeSet attrs, int i) {
    return attrs.getValueOrElse(new NegateAttribute(i-1, null), false);
  }
}
