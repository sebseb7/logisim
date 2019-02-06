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
package com.cburch.logisim.std.arith;

import java.util.SortedMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class ShifterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  protected final static int GENERIC_PARAM_MODE = -1;

  @Override
  public boolean HDLTargetSupported(String lang, AttributeSet attrs, char Vendor) { return true; }

  @Override
  public String getComponentStringIdentifier() { return "SHIFTER"; }

  @Override
  public String GetSubDir() { return "arithmetic"; }

  @Override
  public void inputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("DataA", width(attrs));
    list.put("Shift", stages(attrs));
  }

  @Override
  public void outputs(SortedMap<String, Integer> list, Netlist nets, AttributeSet attrs) {
    list.put("Result", width(attrs));
  }

  @Override
	public void params(SortedMap<Integer, String> list, AttributeSet attrs) {
    list.put(GENERIC_PARAM_MODE, "Mode");
  }

  @Override
  public void paramValues(SortedMap<String, Integer> list, Netlist nets, NetlistComponent info, FPGAReport err) {
    AttributeSet attrs = info.GetComponent().getAttributeSet();
    Object mode = attrs.getValue(Shifter.ATTR_SHIFT);
    if (mode == Shifter.SHIFT_LOGICAL_LEFT)
      list.put("Mode", 0);
    else if (mode == Shifter.SHIFT_ROLL_LEFT)
      list.put("Mode", 1);
    else if (mode == Shifter.SHIFT_LOGICAL_RIGHT)
      list.put("Mode", 2);
    else if (mode == Shifter.SHIFT_ARITHMETIC_RIGHT)
      list.put("Mode", 3);
    else if (mode == Shifter.SHIFT_ROLL_RIGHT)
      list.put("Mode", 4);
  }

  @Override
  public void portValues(SortedMap<String, String> list, Netlist nets, NetlistComponent info, FPGAReport err, String lang) {
    list.putAll(GetNetMap("DataA", true, info, Shifter.IN0, err, lang, nets));
    list.putAll(GetNetMap("Shift", true, info, Shifter.IN1, err, lang, nets));
    list.putAll(GetNetMap("Result", true, info, Shifter.OUT, err, lang, nets));
  }

  @Override
  public void wires(SortedMap<String, Integer> list, AttributeSet attrs, Netlist nets) {
    System.out.println("BUG?! width and num stages must be a param?!");
    int w = width(attrs);
    if (w > 1) {
      int n = stages(attrs);
      for (int i = 0; i < n; i++) {
        list.put(String.format("s_%d_out", i), w);
        list.put(String.format("s_%d_in", i), 1<<i);
      }
    }
  }

  public void vhdlStageBehavior(Hdl out, int stage, int w) {
    int amt = (1 << stage);
    if (stage == 0) {
      out.stmt("s_0_in <= DataA(%d) WHEN Mode = 1 OR Mode = 3 ELSE", w-1);
      out.stmt("          DataA(0) WHEN Mode = 4 ELSE");
      out.stmt("          '0';");
      out.stmt("s_0_out <= DataA WHEN Shift%s = '0' ELSE", w == 2 ? "" : "(0)");
      out.stmt("           DataA(%d DOWNTO 0) & s_0_in WHEN Mode = 0 OR Mode = 1 ELSE", w-2);
      out.stmt("           s_0_in & DataA(%d DOWNTO 1);", w-1);
    } else {
      out.stmt("s_%d_in <= s_%d_out(%d DOWNTO %d) WHEN Mode = 1 ELSE", stage, stage-1, w-1, w-amt);
      out.stmt("          (others => s_%d_out(%d)) WHEN Mode = 3 ELSE", stage-1, w-1);
      out.stmt("          s_%d_out(%d DOWNTO 0) WHEN Mode = 4 ELSE", stage-1, amt-1);
      out.stmt("          (others => '0');");
      out.stmt("s_%d_out <= s_%d_out WHEN SHift(%d) = '0' ELSE", stage, stage-1, stage);
      out.stmt("           s_%d_out(%d DOWNTO 0) & s_%d_in WHEN Mode = 0 OR Mode = 1 ELSE", stage-1, w-amt-1, stage);
      out.stmt("           s_%d_in & s_%d_out(%d DOWNTO %d);", stage, stage-1, w-1, amt);
    }
  }

  public void verilogStageBehavior(Hdl out, int stage, int w) {
    int amt = (1 << stage);
    if (stage == 0) {
      out.stmt("assign s_0_in = ((Mode == 1) || (Mode == 3)) ? DataA[%d] :", w-1);
      out.stmt("                 (Mode == 4)                 ? DataA[0] :");
      out.stmt("                                               0;");
      out.stmt("assign s_0_out = (Shift == 0)                 ? DataA :");
      out.stmt("                 ((Mode == 0) || (Mode == 1)) ? { DataA[%d:0], s_0_in } :", w-2);
      out.stmt("                                                { s_0_in, DataA[%d:1] };", w-1);
    } else {
      out.stmt("assign s_%d_in = (Mode == 1) ? s_%d_out[%d:%d] : ", stage, stage-1, w-1, w-amt);
      out.stmt("                (Mode == 3) ? { %d{s_%d_out[%d]} } :", amt, stage-1, w-1);
      out.stmt("                (Mode == 4) ? s_%d_out[%d:0] :", stage-1, amt-1);
      out.stmt("                              0;");
      out.stmt("assign s_%d_out = (Shift[%d] == 0)             ? s_%d_out : ", stage, stage, stage-1);
      out.stmt("                  ((Mode == 0) || (Mode == 1)) ? { s_%d_out[%d:0], s_%d_in } :", stage-1, w-amt-1, stage);
      out.stmt("                                                 { s_%d_in, s_%d_out[%d:%d] };", stage, stage-1, w-1, amt);
    }
  }
  
  @Override
  public void behavior(Hdl out, Netlist TheNetlist, AttributeSet attrs) {
    System.out.println("BUG? need bus width param?");
    out.indent();
    int w = width(attrs);
    int n = stages(attrs);
    if (out.isVhdl) {
      out.stmt("--- Accepted Mode Values:");
      out.stmt("--- 0 : Logical Shift Left");
      out.stmt("--- 1 : Rotate Left");
      out.stmt("--- 2 : Logical Shift Right");
      out.stmt("--- 3 : Arithmetic Shift Right");
      out.stmt("--- 4 : Rotate Right");
      out.stmt("");
      if (n == 1) {
        out.stmt("Result <= DataA WHEN Mode = 1 OR Mode = 3 OR Mode = 4 ELSE");
        out.stmt("          DataA AND NOT(Shift);");
      } else {
        for (int i = 0; i < n; i++)
          vhdlStageBehavior(out, i, w);
        out.stmt("");
        out.stmt("Result <= s_%d_out;", n-1);
      }
    } else {
      out.stmt("/* Accepted Mode Values");
      out.stmt(" * 0 : Logical Shift Left");
      out.stmt(" * 1 : Rotate Left");
      out.stmt(" * 2 : Logical Shift Right");
      out.stmt(" * 3 : Arithmetic Shift Right");
      out.stmt(" * 4 : Rotate Right");
      out.stmt(" */");
      out.stmt("");
      if (n == 1) {
        out.stmt("assign Result = ((Mode == 1) || (Mode == 3) || (Mode == 4)");
        out.stmt("                ? DataA");
        out.stmt("                : DataA & ~Shift;");
      } else {
        int stage;
        for (int i = 0; i < n; i++)
          verilogStageBehavior(out, i, w);
        out.stmt("");
        out.stmt("assign Result = s_%d_out;", n-1);
      }
    }
  }

  protected int width(AttributeSet attrs) {
    return attrs.getValue(StdAttr.WIDTH).getWidth();
  }

  private int stages(AttributeSet attrs) {
    int w = width(attrs);
    int stages = 1;
    while ((1 << stages) < w)
      stages++;
    return stages;
  }

}
