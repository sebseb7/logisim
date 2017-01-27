/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class TtyHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

	@Override
	public String getComponentStringIdentifier() {
		return "TTY";
	}

	@Override
	public String GetSubDir() {
		return "io";
	}

	@Override
	public boolean HDLTargetSupported(String HDLType, AttributeSet attrs, char Vendor) {
		return HDLType.equals(Settings.VHDL);
	}

	@Override
	public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
		SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
		Inputs.put("Data", 7);
		Inputs.put("Enable", 1);
		Inputs.put("GlobalClock", 1);
		Inputs.put("ClockEnable", 1);
		Inputs.put("Clear", 1);
		return Inputs;
	}

	@Override
	public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
		SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
		return Outputs;
	}

	@Override
	public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
		SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
		InOuts.put("lcd_rs_rw_en_db_bl", 12);
		return InOuts;
	}

	@Override
	public SortedMap<String, String> GetPortMap(Netlist Nets,
			NetlistComponent ComponentInfo, FPGAReport Reporter, String HDLType) {
		SortedMap<String, String> PortMap = new TreeMap<String, String>();
		String ZeroBit = (HDLType.equals(Settings.VHDL)) ? "'0'" : "1'b0";
		String SetBit = (HDLType.equals(Settings.VHDL)) ? "'1'" : "1'b1";
		String BracketOpen = (HDLType.equals(Settings.VHDL)) ? "(" : "[";
		String BracketClose = (HDLType.equals(Settings.VHDL)) ? ")" : "]";
		AttributeSet attrs = ComponentInfo.GetComponent().getAttributeSet();
		if (!ComponentInfo.EndIsConnected(Tty.CK)) {
			Reporter.AddSevereWarning("Component \"TTY\" in circuit \""
					+ Nets.getCircuitName() + "\" has no clock connection");
			PortMap.put("GlobalClock", ZeroBit);
			PortMap.put("ClockEnable", ZeroBit);
		} else {
			String ClockNetName = GetClockNetName(ComponentInfo, Tty.CK, Nets);
			if (ClockNetName.isEmpty()) {
				Reporter.AddSevereWarning("Component \"TTY\" in circuit \""
						+ Nets.getCircuitName()
						+ "\" has a none-clock-component forced clock!\n"
						+ "        Functional differences between Logisim simulation and hardware can be expected!");
				PortMap.putAll(GetNetMap("GlobalClock", true, ComponentInfo,
							Tty.CK, Reporter, HDLType, Nets));
				PortMap.put("ClockEnable", SetBit);
			} else {
				int ClockBusIndex = ClockHDLGeneratorFactory.DerivedClockIndex;
				if (Nets.RequiresGlobalClockConnection()) {
					ClockBusIndex = ClockHDLGeneratorFactory.GlobalClockIndex;
				} else {
					if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_RISING)
						ClockBusIndex = ClockHDLGeneratorFactory.PositiveEdgeTickIndex;
					else if (attrs.getValue(StdAttr.EDGE_TRIGGER) == StdAttr.TRIG_FALLING)
						ClockBusIndex = ClockHDLGeneratorFactory.InvertedDerivedClockIndex;
				}
				PortMap.put("GlobalClock",
						ClockNetName
						+ BracketOpen
						+ Integer
						.toString(ClockHDLGeneratorFactory.GlobalClockIndex)
						+ BracketClose);
				PortMap.put("ClockEnable",
						ClockNetName + BracketOpen
						+ Integer.toString(ClockBusIndex)
						+ BracketClose);
			}
		}
		PortMap.putAll(GetNetMap("Data", true, ComponentInfo, Tty.IN,
					Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Clear", true, ComponentInfo, Tty.CLR,
					Reporter, HDLType, Nets));
		PortMap.putAll(GetNetMap("Enable", true, ComponentInfo, Tty.WE,
					Reporter, HDLType, Nets));

		String pin = LocalInOutBubbleBusname;
		int offset = ComponentInfo.GetLocalBubbleInOutStartId();
		int start = offset;
		int end = offset + 12 - 1;
		PortMap.put("lcd_rs_rw_en_db_bl", pin + "(" + end + " DOWNTO " + start + ")");
		return PortMap;
	}

	@Override
	public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
		SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
		return Regs;
	}

	@Override
	public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
		SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
		// Wires.put("s_real_enable", 1);
		return Wires;
	}

	// #2
	@Override
	public ArrayList<String> GetEntity(Netlist TheNetlist, AttributeSet attrs,
			String ComponentName, FPGAReport Reporter, String HDLType) {

		ArrayList<String> Contents = new ArrayList<String>();
		Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
					Settings.VHDL, TheNetlist.projName()));
		Contents.addAll(FileWriter.getExtendedLibrary());
		Contents.add("ENTITY " + ComponentName + " IS");
		Contents.add("   PORT ( ");
		Contents.add("      Data        : IN std_logic_vector (6 downto 0);");
		Contents.add("      Clear       : IN std_logic;");
		Contents.add("      Enable      : IN std_logic;");
		Contents.add("      GlobalClock : IN std_logic;");
		Contents.add("      ClockEnable : IN std_logic;");
		Contents.add("      lcd_rs_rw_en_db_bl : INOUT std_logic_vector (11 downto 0)");
		Contents.add("   );");
		Contents.add("END " + ComponentName + ";");
		Contents.add("");
		return Contents;
	}

	@Override
	public ArrayList<String> GetArchitecture(Netlist TheNetlist,
			AttributeSet attrs, Map<String, File> MemInitFiles, String ComponentName,
			FPGAReport Reporter, String HDLType) {

		ArrayList<String> Contents = new ArrayList<String>();
		if (HDLType.equals(Settings.VHDL)) {
			Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
						HDLType, TheNetlist.projName()));
			Contents.add("");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("-- Title      : TTY-like emulator and 16x2 LCD controller");
			Contents.add("-- Project    : ");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("-- Author     : <stachelsau@T420> <kwalsh@holycross.edu>");
			Contents.add("-- Created    : 2012-07-28");
			Contents.add("-- Last update: 2016-02-05");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("-- Description: The controller initializes the display when rst goes to '0'.");
			Contents.add("--              After that it maintains a 16x2 buffer of characters and writes");
			Contents.add("--              these continously to the display.");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("-- Copyright (c) 2012 ");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("-- Revisions  :");
			Contents.add("-- Date        Version  Author      Description");
			Contents.add("-- 2012-07-28  1.0      stachelsau  Created");
			Contents.add("-- 2016-02-05  1.1      kwalsh      Adapted for Logisim Evolution");
			Contents.add("-- 2016-08-19  1.2      kwalsh      Added TTY style interface");
			Contents.add("-------------------------------------------------------------------------------");
			Contents.add("");
			Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName.toString() + " IS ");
			Contents.add("");
			Contents.add("  constant CLK_PERIOD_NS : positive := 20; -- 50MHz"); // fixem: TERASIC_DE0 is 50MHz, but not others
			Contents.add("  constant DELAY_15_MS   : positive := 15 * 10**6 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_1640_US : positive := 1640 * 10**3 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_4100_US : positive := 4100 * 10**3 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_100_US  : positive := 100 * 10**3 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_40_US   : positive := 40 * 10**3 / CLK_PERIOD_NS + 1;");
			Contents.add("");
			Contents.add("  constant DELAY_NIBBLE     : positive := 10**3 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_LCD_E      : positive := 230 / CLK_PERIOD_NS + 1;");
			Contents.add("  constant DELAY_SETUP_HOLD : positive := 40 / CLK_PERIOD_NS + 1;");
			Contents.add("");
			Contents.add("  constant MAX_DELAY : positive := DELAY_15_MS;");
			Contents.add("");
			Contents.add("  -- this record describes one write operation");
			Contents.add("  type op_t is record");
			Contents.add("    rs      : std_logic;");
			Contents.add("    data    : std_logic_vector(7 downto 0);");
			Contents.add("    delay_h : integer range 0 to MAX_DELAY;");
			Contents.add("    delay_l : integer range 0 to MAX_DELAY;");
			Contents.add("  end record op_t;");
			Contents.add("  constant default_op      : op_t := (rs => '1', data => X\"00\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
			Contents.add("  constant op_select_line1 : op_t := (rs => '0', data => X\"80\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
			Contents.add("  constant op_select_line2 : op_t := (rs => '0', data => X\"C0\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
			Contents.add("");
			Contents.add("  -- init + config operations:");
			Contents.add("  -- write 3 x 0x3 followed by 0x2");
			Contents.add("  -- function set command");
			Contents.add("  -- entry mode set command");
			Contents.add("  -- display on/off command");
			Contents.add("  -- clear display");
			Contents.add("  type config_ops_t is array(0 to 5) of op_t;");
			Contents.add("  constant config_ops : config_ops_t");
			Contents.add("  := (5 => (rs => '0', data => X\"33\", delay_h => DELAY_4100_US, delay_l => DELAY_100_US),");
			Contents.add("  4 => (rs => '0', data => X\"32\", delay_h => DELAY_40_US, delay_l => DELAY_40_US),");
			Contents.add("  3 => (rs => '0', data => X\"28\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
			Contents.add("  2 => (rs => '0', data => X\"06\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
			Contents.add("  1 => (rs => '0', data => X\"0C\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
			Contents.add("  0 => (rs => '0', data => X\"01\", delay_h => DELAY_NIBBLE, delay_l => DELAY_1640_US));");
			Contents.add("");
			Contents.add("  signal this_op : op_t;");
			Contents.add("");
			Contents.add("  type op_state_t is (IDLE,");
			Contents.add("  WAIT_SETUP_H,");
			Contents.add("  ENABLE_H,");
			Contents.add("  WAIT_HOLD_H,");
			Contents.add("  WAIT_DELAY_H,");
			Contents.add("  WAIT_SETUP_L,");
			Contents.add("  ENABLE_L,");
			Contents.add("  WAIT_HOLD_L,");
			Contents.add("  WAIT_DELAY_L,");
			Contents.add("  DONE);");
			Contents.add("");
			Contents.add("  signal op_state      : op_state_t := DONE;");
			Contents.add("  signal next_op_state : op_state_t;");
			Contents.add("  signal cnt           : natural range 0 to MAX_DELAY;");
			Contents.add("  signal next_cnt      : natural range 0 to MAX_DELAY;");
			Contents.add("");
			Contents.add("  type state_t is (RESET,");
			Contents.add("  CONFIG,");
			Contents.add("  SELECT_LINE1,");
			Contents.add("  WRITE_LINE1,");
			Contents.add("  SELECT_LINE2,");
			Contents.add("  WRITE_LINE2);");
			Contents.add("");
			Contents.add("  signal state      : state_t               := RESET;");
			Contents.add("  signal next_state : state_t;");
			Contents.add("  signal ptr        : natural range 0 to 15 := 0;");
			Contents.add("  signal next_ptr   : natural range 0 to 15;");
			Contents.add("");
			Contents.add("  signal ascii : std_logic_vector(7 downto 0);");
			Contents.add("  signal push : std_logic;");
			Contents.add("  signal init : std_logic;");
			Contents.add("");
			Contents.add("  type ROW is array (0 to 15) of std_logic_vector(7 downto 0);");
			Contents.add("  signal line1, line2 : ROW;");
			Contents.add("  signal wptr : natural range 0 to 16;");
			Contents.add("");
			Contents.add("  signal clk50 : std_logic;");
			Contents.add("  signal rst : std_logic;");
			Contents.add("  signal lcd_rs : std_logic;");
			Contents.add("  signal lcd_rw : std_logic;");
			Contents.add("  signal lcd_en : std_logic;");
			Contents.add("  signal lcd_db : std_logic_vector(7 downto 0);");
			Contents.add("  signal lcd_bl : std_logic;");
			Contents.add("");
			Contents.add("begin");
			Contents.add("");
			Contents.add("  clk50 <= GlobalClock;");
			Contents.add("  rst <= Clear;");
			Contents.add("  lcd_bl <= '1';");
			Contents.add("  lcd_rs_rw_en_db_bl <= lcd_rs & lcd_rw & lcd_en & lcd_db & lcd_bl;");
			Contents.add("  push <= ClockEnable and Enable;");
			Contents.add("  ascii <= '0' & Data;"); // fixme, allow 8-bit values
			Contents.add("");
			Contents.add("  -- tty emulation");
			Contents.add("  process (clk50) is");
			Contents.add("  begin");
			Contents.add("  if rising_edge(clk50) then");
			Contents.add("    if rst = '1' then");
			Contents.add("      init <= '0';");
			Contents.add("    else");
			Contents.add("      init <= '1';");
			Contents.add("    end if;");
			Contents.add("    if init = '0' then");
			Contents.add("      -- this init sequence doesn't seem to happen");
			Contents.add("      wptr <= 0;");
			Contents.add("      for i in 0 to 15 loop");
			Contents.add("        line1(i) <= X\"20\";");
			Contents.add("        line2(i) <= X\"20\";");
			Contents.add("      end loop;");
			Contents.add("    elsif push = '1' then");
			Contents.add("      case ascii is");
			Contents.add("        when X\"09\" => -- \"\\t\"");
			Contents.add("          if wptr < 8 then");
			Contents.add("            for i in 0 to 7 loop");
			Contents.add("              if i >= wptr then");
			Contents.add("                line2(i) <= X\"20\";");
			Contents.add("              end if;");
			Contents.add("            end loop;");
			Contents.add("            wptr <= 8;");
			Contents.add("          elsif wptr = 16 then");
			Contents.add("            line1 <= line2;");
			Contents.add("            for i in 0 to 15 loop");
			Contents.add("              line2(i) <= X\"20\";");
			Contents.add("            end loop;");
			Contents.add("            wptr <= 8;");
			Contents.add("          else");
			Contents.add("            for i in 8 to 15 loop");
			Contents.add("              if i >= wptr then");
			Contents.add("                line2(i) <= X\"20\";");
			Contents.add("              end if;");
			Contents.add("            end loop;");
			Contents.add("            wptr <= 16;");
			Contents.add("          end if;");
			Contents.add("        when X\"0a\" => -- \"\\n\"");
			Contents.add("          line1 <= line2;");
			Contents.add("          for i in 0 to 15 loop");
			Contents.add("            line2(i) <= X\"20\";");
			Contents.add("          end loop;");
			Contents.add("          wptr <= 0;");
			Contents.add("        when X\"0d\" => -- \"\\r\"");
			Contents.add("          wptr <= 0;");
			Contents.add("        when others =>");
			Contents.add("          if wptr = 16 then");
			Contents.add("            line1 <= line2;");
			Contents.add("            -- if ascii(6) = '0' and ascii(5) = '0' then");
			Contents.add("            --   line2(0) <= X\"EF\";  -- sad face for garbage in 000xxxxx and 100xxxxx ranges");
			Contents.add("            -- else");
			Contents.add("              line2(0) <= ascii;");
			Contents.add("            -- end if;");
			Contents.add("            for i in 1 to 15 loop");
			Contents.add("              line2(i) <= X\"20\";");
			Contents.add("            end loop;");
			Contents.add("            wptr <= 1;");
			Contents.add("          else");
			Contents.add("            -- if ascii(6) = '0' and ascii(5) = '0' then");
			Contents.add("            --   line2(wptr) <= X\"EF\";  -- sad face for garbage in 000xxxxx and 100xxxxx ranges");
			Contents.add("            -- else");
			Contents.add("              line2(wptr) <= ascii;");
			Contents.add("            -- end if;");
			Contents.add("            wptr <= wptr + 1;");
			Contents.add("          end if;");
			Contents.add("      end case;");
			Contents.add("    end if;");
			Contents.add("  end if;");
			Contents.add("  end process;");
			Contents.add("");
			Contents.add("  proc_state : process(state, op_state, ptr, line1, line2) is");
			Contents.add("  begin");
			Contents.add("    case state is");
			Contents.add("      when RESET =>");
			Contents.add("        this_op    <= default_op;");
			Contents.add("        next_state <= CONFIG;");
			Contents.add("        next_ptr   <= config_ops_t'high;");
			Contents.add("");
			Contents.add("      when CONFIG =>");
			Contents.add("        this_op    <= config_ops(ptr);");
			Contents.add("        next_ptr   <= ptr;");
			Contents.add("        next_state <= CONFIG;");
			Contents.add("        if op_state = DONE then");
			Contents.add("          next_ptr <= ptr - 1;");
			Contents.add("          if ptr = 0 then");
			Contents.add("            next_state <= SELECT_LINE1;");
			Contents.add("          end if;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when SELECT_LINE1 =>");
			Contents.add("        this_op  <= op_select_line1;");
			Contents.add("        next_ptr <= 15;");
			Contents.add("        if op_state = DONE then");
			Contents.add("          next_state <= WRITE_LINE1;");
			Contents.add("        else");
			Contents.add("          next_state <= SELECT_LINE1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WRITE_LINE1 =>");
			Contents.add("        this_op      <= default_op;");
			Contents.add("        if line1(15 - ptr) = X\"00\" then");
			Contents.add("          this_op.data <= X\"20\";");
			Contents.add("        else");
			Contents.add("          this_op.data <= line1(15 - ptr);");
			Contents.add("        end if;");
			Contents.add("        next_ptr     <= ptr;");
			Contents.add("        next_state   <= WRITE_LINE1;");
			Contents.add("        if op_state = DONE then");
			Contents.add("          next_ptr <= ptr - 1;");
			Contents.add("          if ptr = 0 then");
			Contents.add("            next_state <= SELECT_LINE2;");
			Contents.add("          end if;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when SELECT_LINE2 =>");
			Contents.add("        this_op  <= op_select_line2;");
			Contents.add("        next_ptr <= 15;");
			Contents.add("        if op_state = DONE then");
			Contents.add("          next_state <= WRITE_LINE2;");
			Contents.add("        else");
			Contents.add("          next_state <= SELECT_LINE2;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WRITE_LINE2 =>");
			Contents.add("        this_op      <= default_op;");
			Contents.add("        if line2(15 - ptr) = X\"00\" then");
			Contents.add("          this_op.data <= X\"20\";");
			Contents.add("        else");
			Contents.add("          this_op.data <= line2(15 - ptr);");
			Contents.add("        end if;");
			Contents.add("        next_ptr     <= ptr;");
			Contents.add("        next_state   <= WRITE_LINE2;");
			Contents.add("        if op_state = DONE then");
			Contents.add("          next_ptr <= ptr - 1;");
			Contents.add("          if ptr = 0 then");
			Contents.add("            next_state <= SELECT_LINE1;");
			Contents.add("          end if;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("    end case;");
			Contents.add("  end process proc_state;");
			Contents.add("");
			Contents.add("  reg_state : process(clk50)");
			Contents.add("  begin");
			Contents.add("    if rising_edge(clk50) then");
			Contents.add("      if rst = '1' then");
			Contents.add("        state <= RESET;");
			Contents.add("        ptr   <= 0;");
			Contents.add("      else");
			Contents.add("        state <= next_state;");
			Contents.add("        ptr   <= next_ptr;");
			Contents.add("      end if;");
			Contents.add("    end if;");
			Contents.add("  end process reg_state;");
			Contents.add("");
			Contents.add("  -- we never read from the lcd");
			Contents.add("  lcd_rw <= '0';");
			Contents.add("");
			Contents.add("  proc_op_state : process(op_state, cnt, this_op) is");
			Contents.add("  begin");
			Contents.add("    case op_state is");
			Contents.add("      when IDLE =>");
			Contents.add("        lcd_db        <= (others => '0');");
			Contents.add("        lcd_rs        <= '0';");
			Contents.add("        lcd_en        <= '0';");
			Contents.add("        next_op_state <= WAIT_SETUP_H;");
			Contents.add("        next_cnt      <= DELAY_SETUP_HOLD;");
			Contents.add("");
			Contents.add("      when WAIT_SETUP_H =>");
			Contents.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= ENABLE_H;");
			Contents.add("          next_cnt      <= DELAY_LCD_E;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_SETUP_H;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when ENABLE_H =>");
			Contents.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '1';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= WAIT_HOLD_H;");
			Contents.add("          next_cnt      <= DELAY_SETUP_HOLD;");
			Contents.add("        else");
			Contents.add("          next_op_state <= ENABLE_H;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WAIT_HOLD_H =>");
			Contents.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= WAIT_DELAY_H;");
			Contents.add("          next_cnt      <= this_op.delay_h;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_HOLD_H;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WAIT_DELAY_H =>");
			Contents.add("        lcd_db <= (others => '0');");
			Contents.add("        lcd_rs <= '0';");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= WAIT_SETUP_L;");
			Contents.add("          next_cnt      <= DELAY_SETUP_HOLD;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_DELAY_H;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WAIT_SETUP_L =>");
			Contents.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= ENABLE_L;");
			Contents.add("          next_cnt      <= DELAY_LCD_E;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_SETUP_L;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when ENABLE_L =>");
			Contents.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '1';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= WAIT_HOLD_L;");
			Contents.add("          next_cnt      <= DELAY_SETUP_HOLD;");
			Contents.add("        else");
			Contents.add("          next_op_state <= ENABLE_L;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WAIT_HOLD_L =>");
			Contents.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
			Contents.add("        lcd_rs <= this_op.rs;");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= WAIT_DELAY_L;");
			Contents.add("          next_cnt      <= this_op.delay_l;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_HOLD_L;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when WAIT_DELAY_L =>");
			Contents.add("        lcd_db <= (others => '0');");
			Contents.add("        lcd_rs <= '0';");
			Contents.add("        lcd_en <= '0';");
			Contents.add("        if cnt = 0 then");
			Contents.add("          next_op_state <= DONE;");
			Contents.add("          next_cnt      <= 0;");
			Contents.add("        else");
			Contents.add("          next_op_state <= WAIT_DELAY_L;");
			Contents.add("          next_cnt      <= cnt - 1;");
			Contents.add("        end if;");
			Contents.add("");
			Contents.add("      when DONE =>");
			Contents.add("        lcd_db        <= (others => '0');");
			Contents.add("        lcd_rs        <= '0';");
			Contents.add("        lcd_en        <= '0';");
			Contents.add("        next_op_state <= IDLE;");
			Contents.add("        next_cnt      <= 0;");
			Contents.add("");
			Contents.add("    end case;");
			Contents.add("  end process proc_op_state;");
			Contents.add("");
			Contents.add("  reg_op_state : process (clk50) is");
			Contents.add("  begin");
			Contents.add("    if rising_edge(clk50) then");
			Contents.add("      if state = RESET then");
			Contents.add("        op_state <= IDLE;");
			Contents.add("      else");
			Contents.add("        op_state <= next_op_state;");
			Contents.add("        cnt      <= next_cnt;");
			Contents.add("      end if;");
			Contents.add("    end if;");
			Contents.add("  end process reg_op_state;");
			Contents.add("");
			Contents.add("END PlatformIndependent;");
		}
		return Contents;
	}
}
