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
package com.bfh.logisim.library;

import com.bfh.logisim.hdlgenerator.HDLGenerator;

public class bcd2sevensegHDLGenerator extends HDLGenerator{

  public bcd2sevensegHDLGenerator(HDLCTX ctx) {
    super(ctx, "bfh", "BCD_to_7_Segment_decoder", "i_Bcd");
    inPorts.add(new PortInfo("BCDin", 4, bcd2sevenseg.BCDin, false));
    outPorts.add(new PortInfo("Segment_A", 1, bcd2sevenseg.Segment_A, null));
    outPorts.add(new PortInfo("Segment_B", 1, bcd2sevenseg.Segment_B, null));
    outPorts.add(new PortInfo("Segment_C", 1, bcd2sevenseg.Segment_C, null));
    outPorts.add(new PortInfo("Segment_D", 1, bcd2sevenseg.Segment_D, null));
    outPorts.add(new PortInfo("Segment_E", 1, bcd2sevenseg.Segment_E, null));
    outPorts.add(new PortInfo("Segment_F", 1, bcd2sevenseg.Segment_F, null));
    outPorts.add(new PortInfo("Segment_G", 1, bcd2sevenseg.Segment_G, null));
    wires.add(new WireInfo("s_output_value", 7));
  }

	@Override
  public void generateBehavior(Hdl out) {
    // todo: verilog support
		out.stmt("Segment_A <= s_output_value(0);");
		out.stmt("Segment_B <= s_output_value(1);");
		out.stmt("Segment_C <= s_output_value(2);");
		out.stmt("Segment_D <= s_output_value(3);");
		out.stmt("Segment_E <= s_output_value(4);");
		out.stmt("Segment_F <= s_output_value(5);");
		out.stmt("Segment_G <= s_output_value(6);");
		out.stmt("");
		out.stmt("MakeSegs : PROCESS( BCDin )");
		out.stmt("BEGIN");
		out.stmt("   CASE (BCDin) IS");
		out.stmt("      WHEN \"0000\" => s_output_value <= \"0111111\";");
		out.stmt("      WHEN \"0001\" => s_output_value <= \"0000110\";");
		out.stmt("      WHEN \"0010\" => s_output_value <= \"1011011\";");
		out.stmt("      WHEN \"0011\" => s_output_value <= \"1001111\";");
		out.stmt("      WHEN \"0100\" => s_output_value <= \"1100110\";");
		out.stmt("      WHEN \"0101\" => s_output_value <= \"1101101\";");
		out.stmt("      WHEN \"0110\" => s_output_value <= \"1111101\";");
		out.stmt("      WHEN \"0111\" => s_output_value <= \"0000111\";");
		out.stmt("      WHEN \"1000\" => s_output_value <= \"1111111\";");
		out.stmt("      WHEN \"1001\" => s_output_value <= \"1101111\";");
		out.stmt("      WHEN OTHERS => s_output_value <= \"-------\";");
		out.stmt("   END CASE;");
		out.stmt("END PROCESS MakeSegs;");
	}

}
