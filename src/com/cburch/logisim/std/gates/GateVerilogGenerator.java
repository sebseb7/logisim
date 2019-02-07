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

import com.bfh.logisim.fpgagui.FPGAReport;
import com.cburch.logisim.hdl.Hdl;

public class GateVerilogGenerator extends GateVhdlGenerator {

  protected GateVerilogGenerator(HDLCTX ctx, boolean invertOutput) { // unary ops
    super(ctx, invertOutput);
  }

  protected GateVerilogGenerator(HDLCTX ctx, boolean identity, String op, boolean invertOutput) {
    super(ctx, identity, op, invertOutput);
  }

  static GateVerilogGenerator forBuffer(HDLCTX ctx) { return new GateVerilogGenerator(ctx, false); }
  static GateVerilogGenerator forNot(HDLCTX ctx) { return new GateVerilogGenerator(ctx, true); }
  static GateVerilogGenerator forAnd(HDLCTX ctx) { return new GateVerilogGenerator(ctx, true, "&", false); }
  static GateVerilogGenerator forNand(HDLCTX ctx) { return new GateVerilogGenerator(ctx, true, "&", true); }
  static GateVerilogGenerator forOr(HDLCTX ctx) { return new GateVerilogGenerator(ctx, false, "|", false); }
  static GateVerilogGenerator forNor(HDLCTX ctx) { return new GateVerilogGenerator(ctx, false, "|", true); }
  static GateVerilogGenerator forXor(HDLCTX ctx) { return new GateVerilogGenerator(ctx, false, "^", false); }
  static GateVerilogGenerator forXnor(HDLCTX ctx) { return new GateVerilogGenerator(ctx, false, "^", true); }

  @Override
  protected void unaryOp(Hdl out) {
    if (!invertOutput)
      out.stmt("assign Result = Input_1;");
    else
      out.stmt("assign Result = ~ Input_1;");
  }

  @Override
  protected void binaryOp(Hdl out) {
    if (!invertOutput)
      out.stmt("assign Result = s_in_1 %s s_in_2;", op);
    else
      out.stmt("assign Result = ~ (s_in_1 %s s_in_2);", op);
  }

  @Override
  protected void naryOp(Hdl out, int n) {
    if (!invertOutput) {
      out.stmt("assign Result = \ts_in_1 %s", op);
      for (int i = 2; i < n; i++)
        out.cont("\ts_in_%d %s", i, op);
      out.cont("\ts_in_%d;", n);
    } else {
      out.stmt("assign Result = ~ (\ts_in_1 %s", op);
      for (int i = 2; i < n; i++)
        out.cont("\ts_in_%d %s", i, op);
      out.cont("\ts_in_%d);", n);
    }
  }

  @Override
  public void oneHot(Hdl out, int n) {
    if (n == 3 && !invertOutput) {
      out.stmt("assign Result = \t(s_in_1 & ~s_in_2 & ~s_in_3) |");
      out.cont("\t(~s_in_1 & s_in_2 & ~s_in_3) |");
      out.cont("\t(~s_in_1 & ~s_in_2 & s_in_3);");
    } else if (n == 3 && invertOutput) {
      out.stmt("assign Result = ~ (\t(s_in_1 & ~s_in_2 & ~s_in_3) |");
      out.cont("\t(~s_in_1 & s_in_2 & ~s_in_3) |");
      out.cont("\t(~s_in_1 & ~s_in_2 & s_in_3));");
    } else {
      if (!invertOutput)
        out.stmt("assign Result = \t(\ts_in_1 &");
      else
        out.stmt("assign Result = ~ (\t(\ts_in_1 &");
      for (int i = 2; i < n; i++)
        out.cont("\t\t~s_in_%d &", i);
      out.cont("\t\t~s_in_%d) |", n);
      for (int j = 2; j < n; j++) {
        out.cont("\t(\t~s_in_1 &");
        for (int i = 2; i < n; i++) {
          if (i == j)
            out.cont("\t\ts_in_%d &", i);
          else
            out.cont("\t\t~s_in_%d &", i);
        }
        out.cont("\t\t~s_in_%d) |", n);
      }
      out.cont("\t(\t~s_in_1 &");
      for (int i = 2; i < n; i++)
        out.cont("\t\t~s_in_%d &", i);
      if (!invertOutput)
        out.cont("\t\ts_in_%d);", n);
      else
        out.cont("\t\ts_in_%d));", n);
    }
  }

  @Override
  protected void doInputInversions(Hdl out, int n) {
    out.stmt("assign s_mask = InputNegations;", n);
    for (int i = 1; i <= n; i++)
      out.stmt("assign s_in_%d = s_mask[%d] ? ~Input_%d : Input_%d;", i, i, i, i);
  }

}
