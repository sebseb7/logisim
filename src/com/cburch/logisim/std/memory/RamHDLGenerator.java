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
package com.cburch.logisim.std.memory;

import java.io.File;

import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.netlist.Path;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class RamHDLGenerator extends HDLGenerator {

  static boolean supports(String lang, AttributeSet attrs, char vendor) {
    Object dbus = attrs.getValue(RamAttributes.ATTR_DBUS);
    boolean separate = dbus == RamAttributes.BUS_SEP;
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean synch = trigger == StdAttr.TRIG_RISING || trigger == StdAttr.TRIG_FALLING;
    boolean nvram = attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE;
    return lang.equals("VHDL") && separate && synch && (!nvram || vendor == 'A');
  }

  public RamHDLGenerator(ComponentContext ctx) {
    super(ctx, "memory", deriveHDLName(ctx.attrs), "i_RAM");
    // Address, DataOut0, then... DataOutX, DataInX, CLK, WE, LEX
    inPorts.add("Address", addrWidth(), Mem.ADDR, false);
    outPorts.add("DataOut0", dataWidth(), Mem.DATA, null);
    int n = Mem.lineSize(_attrs);
    int portnr = Mem.MEM_INPUTS;
    for (int i = 1; i < n; i++)
      outPorts.add("DataOut"+i, dataWidth(), portnr++, null);
    for (int i = 0; i < n; i++)
      inPorts.add("DataIn"+i, dataWidth(), portnr++, false);
    clockPort = new ClockPortInfo("GlobalClock", "ClockEnable", portnr++);
    inPorts.add("WE", 1, portnr++, false);
    for (int i = 0; i < n && n > 1; i++)
      inPorts.add("LE"+i, 1, portnr++, true);

    // For NVRAM, the values of the generic parameters define the mem init data,
    // which depend on the specific component instance (the full path to the
    // instance within the overall design, not just a unique name within a
    // single circuit or subcircuit. We don't yet have a way to pass parameters down
    // through multiple nested levels of of HDL. So instead for now, we required
    // NVRAM only appears at the top-level circuit (this is checked during DRC),
    // then we just use a UID for this component.

    if (_attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE) {
      for (int i = 0; i < n; i++) {
        parameters.add(new ParameterInfo("nvram_contents_"+i,
              "string",
              "\"" + memInitFilename(i) + "\"", null));
      }
    }
  }

  private static String deriveHDLName(AttributeSet attrs) {
    if (attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE)
      return "NVRAM_${UID}";
    int wd = attrs.getValue(Mem.DATA_ATTR).getWidth();
    int wa = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    int n = Mem.lineSize(attrs);
    return String.format("RAM_%dx%dx%d", wd, n, 1<<wa); // could probably be generic
  }

  @Override
	protected Hdl getArchitecture() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

    int wd = dataWidth();
    int rows = (1 << addrWidth());
    int n = Mem.lineSize(_attrs);

		if (out.isVhdl) {

			out.stmt("architecture logisim_generated of " + hdlModuleName + " is ");
      out.indent();
      out.stmt("type MEMORY_ARRAY is array (%d downto 0) of std_logic_vector(%d downto 0);", rows-1, wd-1);

      out.comment("memory definitions");
      for (int i = 0; i < n; i++)
        out.stmt("signal s_mem%d_contents : MEMORY_ARRAY;", i);
      out.stmt();

      if (_attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE) {
        out.stmt("attribute ram_init_file : string;");
        for (int i = 0; i < n; i++)
          out.stmt("attribute ram_init_file of s_mem%d_contents : signal is nvram_contents_%d;", i, i);
			}
      out.stmt();
      out.dedent();

			out.stmt("begin");
      out.indent();
			out.stmt();
      if (n == 1) {
        out.stmt("Mem0 : PROCESS( GlobalClock, DataIn0, Address, WE, ClockEnable )");
        out.stmt("BEGIN");
        out.stmt("   IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
        out.stmt("      IF (WE = '1' and ClockEnable = '1') THEN");
        out.stmt("         s_mem0_contents(to_integer(unsigned(Address))) <= DataIn0;");
        out.stmt("      ELSE");
        out.stmt("          DataOut0 <= s_mem0_contents(to_integer(unsigned(Address)));");
        out.stmt("      END IF;");
        out.stmt("   END IF;");
        out.stmt("END PROCESS Mem0;");
      } else {
        int sa = (n == 4 ? 2 : n == 2 ? 1 : 0);
        for (int i = 0; i < n; i++) {
          out.stmt("Mem%d : PROCESS( GlobalClock, DataIn%d, Address, WE, LE%d, ClockEnable )", i, i, i);
          out.stmt("BEGIN");
          out.stmt("   IF (GlobalClock'event AND (GlobalClock = '1')) THEN");
          out.stmt("      IF (WE = '1' and LE%d = '1' and ClockEnable = '1') THEN", i);
          out.stmt("         s_mem%d_contents(to_integer(shift_right(unsigned(Address),%d))) <= DataIn%d;", i, sa, i);
          out.stmt("      ELSE");
          out.stmt("          DataOut%d <= s_mem%d_contents(to_integer(shift_right(unsigned(Address),%d)));", i, i, sa);
          out.stmt("      END IF;");
          out.stmt("   END IF;");
          out.stmt("END PROCESS Mem%d;", i);
        }
      }
			out.stmt();
      out.dedent();
			out.stmt("end logisim_generated;");
    } else {
      // todo: Verilog support
    }
		return out;
	}

  protected int addrWidth() {
    return _attrs.getValue(Mem.ADDR_ATTR).getWidth();
  }

  protected int dataWidth() {
    return _attrs.getValue(Mem.DATA_ATTR).getWidth();
  }

  @Override
  public boolean hdlDependsOnCircuitState() { // for NVRAM
    return _attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE;
  }
      
  @Override
  public boolean writeAllHDLThatDependsOn(CircuitState cs, NetlistComponent comp,
      Path path, String rootDir) { // for NVRAM
    if (!hdlDependsOnCircuitState())
      return true;
    RamState state = cs == null ? null : (RamState)cs.getData(comp.original);
    if (state == null)
      _err.AddWarning("Non-volatile RAM %s initializion data not found in current "
          + "simulator state. The FPGA NVRAM will be initialized to zero instead.",
          path);
    return writeMemInitFiles(state, path, rootDir);
  }

  private String memInitFilename(int i) {
      return String.format("../vhdl/memory/%s_%d.mif", hdlModuleName, i);
  }

  // Generate and write a "memory init file" for this non-volatile Ram component.
  private boolean writeMemInitFiles(RamState state, Path path, String rootDir) {
    int n = Mem.lineSize(_attrs);
    for (int i = 0; i < n; i++) {
      Hdl data = getMemInitData(state, i);
      File f = openFile(rootDir, true, false, i);
      if (f == null || !FileWriter.WriteContents(f, data, _err))
        return false;
    }
    return true;
  }

  private Hdl getMemInitData(MemState state, int offset) {
    int skip = Mem.lineSize(_attrs);
    int width = dataWidth();
    int depth = (1 << addrWidth()) / skip;
    Hdl out = new Hdl(_lang, _err);
    out.add("-- Memory initialization data for alignment offset " + offset);
    // int depth = (int)((c.getLastOffset() - c.getFirstOffset() + 1) / skip);
    out.add("DEPTH = " + depth + ";");
    out.add("WIDTH = " + width + ";");
    out.add("ADDRESS_RADIX = HEX;");
    out.add("DATA_RADIX = HEX;");
    out.add("CONTENT");
    out.add("BEGIN");
    if (state != null) {
      // TODO: we could compress this a bit using ranges
      MemContents c = state.getContents();
      for (int a = 0; a < depth; a++) {
        int d = c.get(a*skip+offset);
        if (width != 32)
          d &= ((1 << width) - 1);
        out.stmt("%8x : %8x;", a, d);
      }
    } else {

      out.stmt("[0..%x] : %8x; % default init values due to missing simulator state",
          depth-1, 0);
    }
    out.add("END;");
    return out;
  }

}
