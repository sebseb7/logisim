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
package com.cburch.logisim.std.io;

import java.util.ArrayList;

import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.hdlgenerator.HDLInliner;
import com.bfh.logisim.hdlgenerator.HiddenPort;
import com.cburch.logisim.hdl.Hdl;

public class LightsHDLGenerator extends HDLInliner {

  public LightsHDLGenerator(HDLCTX ctx, String name) {
    super(ctx, name);
  }

  public static LightsHDLGenerator forLed(HDLCTX ctx) {
    this(ctx, "LED_${LABEL}");
    hiddenPort = HiddenPort.makeOutport(1, HiddenPort.LED, HiddenPort.Pin);
  }

  public static LightsHDLGenerator forRGBLed(HDLCTX ctx) {
    this(ctx, "RGBLED_${LABEL}");
    ArrayList<String> labels = new ArrayList<>();
    for (int i = 0; i < 3; i++)
      LabelNames.add("");
    labels.set(RGBLed.RED, "RED");
    labels.set(RGBLed.GREEN, "GREEN");
    labels.set(RGBLed.BLUE, "BLUE");
    hiddenPort = HiddenPort.makeOutport(labels, HiddenPort.RGBLED, HiddenPort.LED, HiddenPort.Pin);
  }

  public static LightsHDLGenerator forSevenSegment(HDLCTX ctx) {
    this(ctx, "SevenSegment_${LABEL}");
    hiddenPort = HiddenPort.makeOutport(HexDigitHDLGenerator.labels(),
        HiddenPort.SevenSegment, HiddenPort.LED, HiddenPort.Pin);
  }

  @Override
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) {
    int b = comp.GetLocalBubbleOutputStartId();
    for (int i = 0; i < comp.ports.size(); i++) {
      if (comp.ports.get(i).isConnected()) {
        // fixme: does not handle mixed or partial connections
        String name = _nets.signalEndForEnd1(comp, i, false, out);
        out.assign("LOGISIM_HIDDEN_FPGA_OUTPUT", b + i, name);
      }
    }
  }

}
