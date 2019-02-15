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

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.hdl.Hdl;
import com.cburch.logisim.instance.StdAttr;

public class RamHDLGenerator extends HDLGenerator {

  static boolean supports(String lang, AttributeSet attrs, char vendor) {
    Object dbus = attrs.getValue(RamAttributes.ATTR_DBUS);
    boolean separate = dbus == RamAttributes.BUS_SEP;
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    boolean synch = trigger == StdAttr.TRIG_RISING || trigger == StdAttr.TRIG_FALLING;
    return lang.equals("VHDL") && separate && synch;
  }

  public RamHDLGenerator(HDLCTX ctx) {
    super(ctx, "memory", deriveHDLName(ctx.attrs), "i_RAM");
    // Address, DataOut0, then... DataOutX, DataInX, CLK, WE, LEX
    inPorts.add(new PortInfo("Address", addrWidth(), Mem.ADDR, false));
    outPorts.add(new PortInfo("DataOut0", dataWidth(), Mem.DATA, null));
    int n = Mem.lineSize(attrs);
    int portnr = Mem.MEM_INPUTS;
    for (int i = 1; i < n; i++)
      outPorts.add(new PortInfo("DataOut"+i, dataWidth(), portnr++ null));
    for (int i = 0; i < n; i++)
      inPorts.add(new PortInfo("DataIn"+i, dataWidth(), portnr++, false));
    clockPort = new ClockPortInfo("GlobalClock", "ClockEnable", portnr++);
    inPorts.add(new PortInfo("WE", 1, portnr++, false));
    for (int i = 0; i < n && n > 1; i++)
      inPorts.add(new PortInfo("LE"+i, 1, portnr++, true));
  }

  private static String deriveHDLName(AttributeSet attrs) {
    if (attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.VOLATILE)
      return "NVRAM_${CIRCUIT}_${LABEL}";
    int wd = dataWidth(attrs);
    int wa = addrWidth(attrs);
    int n = Mem.lineSize(attrs);
    return String.format("RAM_%dx%dx%d", wd, n, 1<<wa); // could probably be generic
  }

  // Generate and write all "memory init" files for this non-volatile Ram component.
  protected File[] writeMemInitFiles(String rootDir) {
    int n = Mem.lineSize(_attrs);
    File[] files = new File[n];
    for (int i = 0; i < n; i++) {
      Hdl data = getMemInitData(i);
      File f = openFile(rootDir, hdlComponentName+"_"+i+"_contents", true, false);
      if (f == null)
        continue;
      if (!FileWriter.WriteContents(f, data, _err))
        continue;
      files[i] = f;
    }
    return files;
  }

  private Hdl getMemInitData(int offset) {
    int skip = Mem.lineSize(_attrs);
    int width = dataWidth(_attrs);
    MemContents c = null; // _attrs.getValue(Ram.CONTENTS_ATTR); FIXME, if possible ?!?!
    Hdl out = new Hdl(_lang, _err);
    out.add("-- Memory initialization data line " + offset);
    int depth = (int)((c.getLastOffset() - c.getFirstOffset() + 1) / skip);
    out.add("DEPTH = " + depth + ";");
    out.add("WIDTH = " + width + ";");
    out.add("ADDRESS_RADIX = HEX;");
    out.add("DATA_RADIX = HEX;");
    out.add("CONTENT");
    out.add("BEGIN");
    for (int a = 0; a < depth; a++) {
      int d = c.get(a*skip+offset);
      if (width != 32)
        d &= ((1 << width) - 1);
      out.stmt("%8x : %8x;", a, d);
    }
    out.add("END;");
    return out;
  }

  File[] nvFiles = null;
  protected boolean writeArchitecture(String rootDir) {
    nvFiles = null;
    if (_attrs.getValue(RamAttributes.ATTR_TYPE) == RamAttributes.NONVOLATILE)
      nvFiles = writeMemInitFiles(rootDir);
    super.writeArchitecture(rootDir);
  }

  @Override
	protected Hdl getArchitecture(HashMap<String) {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

    SignalList inPorts = getInPorts();
    SignalList outPorts = getOutPorts();

    int wd = dataWidth();
    int rows = (1 << addrWidth());
    int n = Mem.lineSize(attrs);

		if (out.isVhdl) {

			out.stmt("architecture logisim_generated of " + hdlComponentName + " is ");
      out.indent();
      out.stmt("type MEMORY_ARRAY is array (%d downto 0) of std_logic_vector(%d downto 0);", rows-1, wd-1);

      out.comment("memory definitions");
      for (int i = 0; i < n; i++)
        out.stmt("signal s_mem_%d__contents : MEMORY_ARRAY;", i);
      out.stmt();

      if (nvFiles != null) {
        out.stmt("attribute nvram_init_file : string;");
        for (int i = 0; i < n; i++)
          out.stmt("attribute nvram_init_file of s_mem_%d_contents : signal is \"%s\";",
              i, nvFiles[i].getPath());
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

  protected static int addrWidth() {
    return attrs.getValue(Mem.ADDR_ATTR).getWidth();
  }

  protected static int dataWidth() {
    return attrs.getValue(Mem.DATA_ATTR).getWidth();
  }
}
