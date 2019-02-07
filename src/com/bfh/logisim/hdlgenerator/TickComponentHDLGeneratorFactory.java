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
import java.util.SortedMap;
import java.util.TreeMap;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.library.DynamicClock;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;

public class TickComponentHDLGeneratorFactory extends
		AbstractHDLGeneratorFactory {

	private long FpgaClockFrequency;
	private int TickPeriod;
	// private boolean useFPGAClock;
	private static final String NrOfCounterBitsStr = "NrOfBits";
	private static final Integer NrOfCounterBitsId = -1;
	private static final String ReloadValueStr = "ReloadValue";
	private static final Integer ReloadValueId = -2;

	public static final String FPGAClock = "FPGA_GlobalClock";
	public static final String FPGATick = "s_FPGA_Tick";

	public TickComponentHDLGeneratorFactory(
      String lang, FPGAReport err,
      long fpga_clock_frequency, int tick_period) {
    super(lang, err);
		FpgaClockFrequency = fpga_clock_frequency;
		TickPeriod = tick_period;
		// this.useFPGAClock = useFPGAClock;
	}

	@Override
	public String getComponentStringIdentifier() {
		return "LogisimTickGenerator";
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("FPGAClock", 1);
                if (TickPeriod < 0) {
                    // dynamic divided clock
                    Inputs.put("ReloadValueLessOne", NrOfCounterBitsId);
                }
		return Inputs;
	}

	@Override
	public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist,
			AttributeSet attrs, FPGAReport Reporter, String HDLType) {
		ArrayList<String> Contents = new ArrayList<String>();
		String Preamble = (HDLType.equals(Settings.VHDL)) ? "" : "assign ";
		String AssignOperator = (HDLType.equals(Settings.VHDL)) ? "<=" : "=";
		Contents.add("");
    {
    Hdl out = new Hdl(HDLType, Reporter);
    out.comment("definitions for clock tick generator outputs");
    Contents.addAll(out);
    }
		if (TheNetlist.RequiresGlobalClockConnection() || TickPeriod == 0) {
			Contents.add("   " + Preamble + "FPGATick " + AssignOperator
					+ " '1';");
                        return Contents;
		}
                String ReloadValue;
                if (TickPeriod < 0) {
                    ReloadValue = "ReloadValueLessOne";
                } else {
                    if (HDLType.equals(Settings.VHDL)) {
                        ReloadValue = "std_logic_vector(to_unsigned("
                            +"("+ ReloadValueStr + "-1)," + NrOfCounterBitsStr + "))";
                    } else {
                        ReloadValue = ReloadValueStr + "-1";
                    }
                }
                Contents.add("   " + Preamble + "FPGATick " + AssignOperator + " s_tick_reg;");
		Contents.add("");
    {
    Hdl out = new Hdl(HDLType, Reporter);
    out.comment("definitions for clock tick generator update logic");
    Contents.addAll(out);
    }
		if (HDLType.equals(Settings.VHDL)) {
			Contents.add("   s_tick_next   <= '1' WHEN s_count_reg = std_logic_vector(to_unsigned(0,"
					+ NrOfCounterBitsStr + ")) ELSE '0';");
			Contents.add("   s_count_next  <= (OTHERS => '0') WHEN s_tick_reg /= '0' AND s_tick_reg /= '1' ELSE -- For simulation only!");
			Contents.add("                    " + ReloadValue + " WHEN s_tick_next = '1' ELSE");
			Contents.add("                    std_logic_vector(unsigned(s_count_reg)-1);");
			Contents.add("");
		} else {
			Contents.add("   assign s_tick_next  = (s_count_reg == 0) ? 1'b1 : 1'b0;");
			Contents.add("   assign s_count_next = (s_count_reg == 0) ? ReloadValue-1 : s_count_reg-1;");
			Contents.add("");
      Hdl out = new Hdl(HDLType, Reporter);
      out.comment("definitions for initial state (hdl simulation only)");
      Contents.addAll(out);
			Contents.add("   initial");
			Contents.add("   begin");
			Contents.add("      s_count_reg = 0;");
			Contents.add("      s_tick_reg  = 1'b0;");
			Contents.add("   end");
			Contents.add("");
		}
    {
    Hdl out = new Hdl(HDLType, Reporter);
    out.comment("definitions for clock tick generator flip-flops");
    Contents.addAll(out);
    }
		if (HDLType.equals(Settings.VHDL)) {
			Contents.add("   make_tick : PROCESS( FPGAClock , s_tick_next )");
			Contents.add("   BEGIN");
			Contents.add("      IF (FPGAClock'event AND (FPGAClock = '1')) THEN");
			Contents.add("         s_tick_reg <= s_tick_next;");
			Contents.add("      END IF;");
			Contents.add("   END PROCESS make_tick;");
			Contents.add("");
			Contents.add("   make_counter : PROCESS( FPGAClock , s_count_next )");
			Contents.add("   BEGIN");
			Contents.add("      IF (FPGAClock'event AND (FPGAClock = '1')) THEN");
			Contents.add("         s_count_reg <= s_count_next;");
			Contents.add("      END IF;");
			Contents.add("   END PROCESS make_counter;");
		} else {
			Contents.add("   always @(posedge FPGAClock)");
			Contents.add("   begin");
			Contents.add("       s_count_reg <= s_count_next;");
			Contents.add("       s_tick_reg  <= s_tick_next;");
			Contents.add("   end");
		}
		return Contents;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist,
			AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		Outputs.put("FPGATick", 1);
		return Outputs;
	}

	@Override
	public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
		SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
                if (TickPeriod == 0) {
                    // raw fpga clock
                } else if (TickPeriod < 0) {
                    // dynamic divided clock
                    Parameters.put(NrOfCounterBitsId, NrOfCounterBitsStr);
                } else {
                    // static divided clock
                    Parameters.put(NrOfCounterBitsId, NrOfCounterBitsStr);
                    Parameters.put(ReloadValueId, ReloadValueStr);
                }
		return Parameters;
	}

	@Override
	public SortedMap<String, Integer> GetParameterMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter) {
		SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
                if (TickPeriod == 0) {
                    // raw fpga clock
                } else if (TickPeriod < 0) {
                    // dynamic divided clock
                    NetlistComponent dynClock = Nets.GetDynamicClock();
                    if (dynClock == null) {
                        Reporter.AddFatalError("No dynamic clock control component found. "
                                + "To use the dynamic clock speed feature, you must place a "
                                + "dynamic clock control component in your main top-level circuit.");
                        return ParameterMap;
                    }
                    ParameterMap.put(NrOfCounterBitsStr, dynClock.GetComponent().getAttributeSet().getValue(DynamicClock.WIDTH_ATTR).getWidth());
                } else {
                    int nr_of_bits = 0;
                    int p = TickPeriod;
                    while (p != 0) {
                        nr_of_bits++;
                        p /= 2;
                    }
                    ParameterMap.put(ReloadValueStr, TickPeriod);
                    ParameterMap.put(NrOfCounterBitsStr, nr_of_bits);
                }
		return ParameterMap;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		PortMap.put("FPGAClock", TickComponentHDLGeneratorFactory.FPGAClock);
		PortMap.put("FPGATick", TickComponentHDLGeneratorFactory.FPGATick);
                if (TickPeriod < 0) {
                    // dynamic divided clock
                    NetlistComponent dynClock = Nets.GetDynamicClock();
                    if (dynClock == null) {
                        Reporter.AddFatalError("No dynamic clock control component found. "
                                + "To use the dynamic clock speed feature, you must place a "
                                + "dynamic clock control component in your main top-level circuit.");
                        return PortMap;
                    }
                    PortMap.put("ReloadValueLessOne", "s_LOGISIM_DYNAMIC_CLOCK");
                }
		return PortMap;
	}

	@Override
	public SortedMap<String, Integer> GetRegList(AttributeSet attrs,
			String HDLType) {
		SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
                if (TickPeriod != 0) {
                    // dynamic or static divided clock
                    Regs.put("s_tick_reg", 1);
                    Regs.put("s_count_reg", NrOfCounterBitsId);
                }
		return Regs;
	}

	@Override
	public String GetSubDir() {
		return "base";
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs,
			Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
                if (TickPeriod != 0) {
                    // dynamic or static divided clock
                    Wires.put("s_tick_next", 1);
                    Wires.put("s_count_next", NrOfCounterBitsId);
                }
		return Wires;
	}

}
