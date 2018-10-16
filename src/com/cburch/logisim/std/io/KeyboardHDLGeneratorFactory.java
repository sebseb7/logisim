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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.io.File;

import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.designrulecheck.NetlistComponent;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.hdlgenerator.AbstractHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;

public class KeyboardHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "KBD";
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
    Inputs.put("GlobalClock", 1);
    Inputs.put("ClockEnable", 1);
    Inputs.put("Read", 1);
    // Inputs.put("Clear", 1);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    Outputs.put("Available", 1);
    int asciiWidth = Keyboard.getWidth(attrs.getValue(Keyboard.ATTR_WIDTH));
    Outputs.put("Data", asciiWidth);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
    InOuts.put("ps2kbd", 4);
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
    if (!ComponentInfo.EndIsConnected(Keyboard.CK)) {
      Reporter.AddSevereWarning("Component \"KBD\" in circuit \""
          + Nets.getCircuitName() + "\" has no clock connection");
      PortMap.put("GlobalClock", ZeroBit);
      PortMap.put("ClockEnable", ZeroBit);
    } else {
      String ClockNetName = GetClockNetName(ComponentInfo, Keyboard.CK, Nets);
      if (ClockNetName.isEmpty()) {
        Reporter.AddSevereWarning("Component \"KBD\" in circuit \""
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
    PortMap.putAll(GetNetMap("Read", true, ComponentInfo, Keyboard.RE,
          Reporter, HDLType, Nets));
    // PortMap.putAll(GetNetMap("Clear", true, ComponentInfo, Keyboard.CLR,
    //       Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Available", true, ComponentInfo, Keyboard.AVL,
          Reporter, HDLType, Nets));
    PortMap.putAll(GetNetMap("Data", true, ComponentInfo, Keyboard.OUT,
          Reporter, HDLType, Nets));

    String pin = LocalInOutBubbleBusname;
    int offset = ComponentInfo.GetLocalBubbleInOutStartId();
    int start = offset;
    int end = offset + 4 - 1;
    PortMap.put("ps2kbd", pin + "(" + end + " DOWNTO " + start + ")");
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
    int asciiWidth = Keyboard.getWidth(attrs.getValue(Keyboard.ATTR_WIDTH));
    ArrayList<String> Contents = new ArrayList<String>();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName,
          Settings.VHDL, TheNetlist.projName()));
    Contents.addAll(FileWriter.getExtendedLibrary2());
    Contents.add("ENTITY " + ComponentName + " IS");
    Contents.add("   PORT ( ");
    Contents.add("      Read        : IN std_logic;");
    // Contents.add("      Clear       : IN std_logic;");
    Contents.add("      Available   : OUT std_logic;");
    Contents.add("      Data        : OUT std_logic_vector ("+(asciiWidth-1)+" downto 0);");
    Contents.add("      GlobalClock : IN std_logic;");
    Contents.add("      ClockEnable : IN std_logic;");
    Contents.add("      ps2kbd      : INOUT std_logic_vector (3 downto 0)");
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
      Contents.add("-- Title      : PS2 scancode protocol, ascii conversion, and character buffer");
      Contents.add("-------------------------------------------------------------------------------");
      Contents.add("-- File       : PS2_ASCII_BUFFER.vhd");
      Contents.add("-- Author     : <kwalsh@holycross.edu>");
      Contents.add("-- Created    : 2016-08-19");
      Contents.add("-------------------------------------------------------------------------------");
      Contents.add("-- Description: Accepts raw PS2 keyboard inputs and presents a character buffer");
      Contents.add("--    interface. It needs two separate clock sources: a 50MHz \"fast\" clock,");
      Contents.add("--    which is used for the scancode handling and low-level timing; and a");
      Contents.add("--    second \"slow\" clock used to control the rate at which characters are");
      Contents.add("--    removed from the character buffer. The slow clock can be the same as the");
      Contents.add("--    fast clock or any fixed multiple slower. Actually, the slow clock does not");
      Contents.add("--    really need to be a fixed multiple at all, but it must be synchronized");
      Contents.add("--    with the fast clock (i.e. all rising edges of the slow clock must");
      Contents.add("--    coincide with rising edges of the fast clock). This module is a");
      Contents.add("--    combination of what was previously four separate modules:");
      Contents.add("--          1. Debouncing PS2 inputs.");
      Contents.add("--          2. Checking PS2 parity and detecting PS2 scancodes.");
      Contents.add("--          3. Converting PS2 scancodes (set 2) to ascii.");
      Contents.add("--          4. Buffering ascii characters in a FIFO queue.");
      Contents.add("--     ");
      Contents.add("-------------------------------------------------------------------------------");
      Contents.add("-- Copyright (c) 2016 ");
      Contents.add("-------------------------------------------------------------------------------");
      Contents.add("-- Date        Version  Author  Description");
      Contents.add("-- 201?-??-??  1.0      kwalsh  Created");
      Contents.add("-- 2016-02-05  1.1      kwalsh  Adapted for Logisim Evolution");
      Contents.add("-- 2016-08-19  1.2      kwalsh  Merged with ps2_keyboard and STD_FIFO");
      Contents.add("-------------------------------------------------------------------------------");
      Contents.add("--");
      Contents.add("-- Some code adapated from ps2_keyboard.vhd, with following disclaimer:");
      Contents.add("--------------------------------------------------------------------------------");
      Contents.add("--");
      Contents.add("--   FileName:         ps2_keyboard.vhd");
      Contents.add("--   Dependencies:     debounce.vhd");
      Contents.add("--   Design Software:  Quartus II 32-bit Version 12.1 Build 177 SJ Full Version");
      Contents.add("--");
      Contents.add("--   HDL CODE IS PROVIDED \"AS IS.\"  DIGI-KEY EXPRESSLY DISCLAIMS ANY");
      Contents.add("--   WARRANTY OF ANY KIND, WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT");
      Contents.add("--   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A");
      Contents.add("--   PARTICULAR PURPOSE, OR NON-INFRINGEMENT. IN NO EVENT SHALL DIGI-KEY");
      Contents.add("--   BE LIABLE FOR ANY INCIDENTAL, SPECIAL, INDIRECT OR CONSEQUENTIAL");
      Contents.add("--   DAMAGES, LOST PROFITS OR LOST DATA, HARM TO YOUR EQUIPMENT, COST OF");
      Contents.add("--   PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY OR SERVICES, ANY CLAIMS");
      Contents.add("--   BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF),");
      Contents.add("--   ANY CLAIMS FOR INDEMNITY OR CONTRIBUTION, OR OTHER SIMILAR COSTS.");
      Contents.add("--");
      Contents.add("--   Version History");
      Contents.add("--   Version 1.0 11/25/2013 Scott Larson");
      Contents.add("--     Initial Public Release");
      Contents.add("--    ");
      Contents.add("--------------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName.toString() + " IS ");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- ps2_keyboard scancode protocol");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  constant clk50_freq   : integer := 50_000_000;         -- Frequency of clk50 (50 MHz)"); // fixme: TERASIC_DE0 is 50MHz, but not others
      Contents.add("  constant counter_size : integer := 8;                  -- Width (minus 1) for ps2 debounce counters");
      Contents.add("");
      Contents.add("  signal sync_ffs       : std_logic_vector(3 downto 0);  -- synchronizer flip-flops for PS/2 signals");
      Contents.add("");
      Contents.add("  signal d_ps2clk_ffs   : std_logic_vector(1 downto 0);  -- ps2clk debounce input flip flops");
      Contents.add("  signal d_ps2clk_set   : std_logic;                     -- ps2clk debounce sync reset to zero");
      Contents.add("  signal d_ps2clk_out   : std_logic_vector(counter_size downto 0) := (others => '0'); -- ps2clk debounce counter output");
      Contents.add("  signal ps2clk         : std_logic;                     -- debounced clock signal from PS/2 keyboard");
      Contents.add("");
      Contents.add("  signal d_ps2data_ffs  : std_logic_vector(1 downto 0);  -- ps2data debounce input flip flops");
      Contents.add("  signal d_ps2data_set  : std_logic;                     -- ps2data debounce sync reset to zero");
      Contents.add("  signal d_ps2data_out  : std_logic_vector(counter_size downto 0) := (others => '0'); -- ps2data debounce counter output");
      Contents.add("  signal ps2data        : std_logic;                     -- debounced data signal from PS/2 keyboard");
      Contents.add("  ");
      Contents.add("  signal ps2_prev_new   : std_logic;                     -- PS/2 keyboard clock edge detection");
      Contents.add("  signal ps2_curr_new   : std_logic;                     -- PS/2 keyboard clock edge detection");
      Contents.add("");
      Contents.add("  signal ps2_word       : std_logic_vector(10 downto 0); -- stores the ps2 data word");
      Contents.add("  signal ps2_error      : std_logic;                     -- validate parity, start, and stop bits");
      Contents.add("  signal count_idle     : integer range 0 to clk50_freq/18_000;  -- counter to determine whether PS/2 is idle");
      Contents.add("");
      Contents.add("  signal ps2_recv       : std_logic;                     -- flag that new scancode is available in ps2_code");
      Contents.add("  signal ps2_code       : std_logic_vector(7 downto 0);  -- scancode received from PS/2");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- ps2_ascii scancode to ascii conversion");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  signal scc : integer range 0 to 7;         -- count of buffered scancodes");
      Contents.add("  signal sc0 : std_logic_vector(7 downto 0); -- max 8 buffered scancodes");
      Contents.add("  signal sc1 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc2 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc3 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc4 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc5 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc6 : std_logic_vector(7 downto 0);");
      Contents.add("  -- signal sc7 : std_logic_vector(7 downto 0);");
      Contents.add("");
      Contents.add("  signal caps : std_logic;");
      Contents.add("  signal shift : std_logic;");
      Contents.add("  signal ascii_w : std_logic_vector(7 downto 0);");
      Contents.add("  signal ready_w : std_logic;");
      Contents.add("  signal ctrl_w : std_logic;");
      Contents.add("  signal caps_w : std_logic;");
      Contents.add("  signal shift_w : std_logic;");
      Contents.add("  signal ps2_recv_prev : std_logic;");
      Contents.add("");
      Contents.add("  signal char_ascii : std_logic_vector(7 downto 0);");
      Contents.add("  signal char_ready : std_logic;");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- std_fifo character buffer");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  constant DATA_WIDTH : positive := 8;");
      int fifoDepth = Keyboard.getBufferLength(attrs.getValue(Keyboard.ATTR_BUFFER));
      Contents.add("  constant FIFO_DEPTH  : positive := " + fifoDepth + ";");
      Contents.add("  signal fifo_pop : std_logic;");
      Contents.add("");
      Contents.add("  signal clk50 : std_logic;");
      Contents.add("  signal ready : std_logic;");
      Contents.add("  signal ascii : std_logic_vector(7 downto 0);");
      Contents.add("");
      Contents.add("begin");
      Contents.add("");
      Contents.add("  clk50 <= GlobalClock;");
      int asciiWidth = Keyboard.getWidth(attrs.getValue(Keyboard.ATTR_WIDTH));
      Contents.add("  Data <= ascii("+(asciiWidth-1)+" downto 0);");
      Contents.add("  Available <= ready;");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- ps2_keyboard scancode protocol");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  -- synchronize ps2 inputs");
      Contents.add("  process (clk50)");
      Contents.add("  begin");
      Contents.add("    if (rising_edge(clk50)) then");
      Contents.add("      sync_ffs <= ps2kbd;  -- synchronize PS/2 clock signal");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  -- debounce ps2clk signal");
      Contents.add("  d_ps2clk_set <= d_ps2clk_ffs(0) xor d_ps2clk_ffs(1);  -- determine when to start/reset counter");
      Contents.add("  process (clk50)");
      Contents.add("  begin");
      Contents.add("    if (rising_edge(clk50)) then");
      Contents.add("      d_ps2clk_ffs(0) <= sync_ffs(0);");
      Contents.add("      d_ps2clk_ffs(1) <= d_ps2clk_ffs(0);");
      Contents.add("      if (d_ps2clk_set = '1') then  -- reset counter because input is changing");
      Contents.add("        d_ps2clk_out <= (others => '0');");
      Contents.add("      elsif (d_ps2clk_out(counter_size) = '0') then  -- stable input time is not yet met");
      Contents.add("        d_ps2clk_out <= d_ps2clk_out + 1;");
      Contents.add("      else  -- stable input time is met");
      Contents.add("        ps2clk <= d_ps2clk_ffs(1);");
      Contents.add("      end if;    ");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  -- debounce ps2data signal");
      Contents.add("  d_ps2data_set <= d_ps2data_ffs(0) xor d_ps2data_ffs(1);  -- determine when to start/reset counter");
      Contents.add("  process (clk50)");
      Contents.add("  begin");
      Contents.add("    if (rising_edge(clk50)) then");
      Contents.add("      d_ps2data_ffs(0) <= sync_ffs(1);");
      Contents.add("      d_ps2data_ffs(1) <= d_ps2data_ffs(0);");
      Contents.add("      if (d_ps2data_set = '1') then  -- reset counter because input is changing");
      Contents.add("        d_ps2data_out <= (others => '0');");
      Contents.add("      elsif (d_ps2data_out(counter_size) = '0') then  -- stable input time is not yet met");
      Contents.add("        d_ps2data_out <= d_ps2data_out + 1;");
      Contents.add("      else  -- stable input time is met");
      Contents.add("        ps2data <= d_ps2data_ffs(1);");
      Contents.add("      end if;    ");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  -- move PS2 data into shift register");
      Contents.add("  process (ps2clk)");
      Contents.add("  begin");
      Contents.add("    if (falling_edge(ps2clk)) then  -- falling edge of PS2 clock");
      Contents.add("      ps2_word <= ps2data & ps2_word(10 downto 1);  -- shift in PS2 data bit");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("    ");
      Contents.add("  -- verify that parity, start, and stop bits are all correct");
      Contents.add("  ps2_error <= not (not ps2_word(0) and ps2_word(10) and (ps2_word(9) xor ps2_word(8) xor");
      Contents.add("        ps2_word(7) xor ps2_word(6) xor ps2_word(5) xor ps2_word(4) xor ps2_word(3) xor ");
      Contents.add("        ps2_word(2) xor ps2_word(1)));  ");
      Contents.add("");
      Contents.add("  -- determine if PS2 port is idle (i.e. last transaction is finished) and output result");
      Contents.add("  process (clk50)");
      Contents.add("  begin");
      Contents.add("    if (rising_edge(clk50)) then");
      Contents.add("      if (ps2clk = '0') then                        -- low PS2 clock, PS/2 is active");
      Contents.add("        count_idle <= 0;                            -- reset idle counter");
      Contents.add("      elsif (count_idle /= clk50_freq/18_000) then  -- PS2 clock has been high less than a half clock period (<55us)");
      Contents.add("          count_idle <= count_idle + 1;             -- continue counting");
      Contents.add("      end if;");
      Contents.add("      ");
      Contents.add("      ps2_prev_new <= ps2_curr_new;");
      Contents.add("      if (count_idle = clk50_freq/18_000 and ps2_error = '0') then  -- idle threshold reached and no errors detected");
      Contents.add("        ps2_curr_new <= '1';                                        -- set flag that new PS/2 code is available");
      Contents.add("        ps2_code <= ps2_word(8 downto 1);                           -- output new PS/2 code");
      Contents.add("      else                                                          -- PS/2 port active or error detected");
      Contents.add("        ps2_curr_new <= '0';                                        -- set flag that PS/2 transaction is in progress");
      Contents.add("      end if;");
      Contents.add("      ps2_recv <= (not ps2_prev_new) and ps2_curr_new;");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- ps2_ascii scancode to ascii conversion");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  process (clk50) is");
      Contents.add("  begin");
      Contents.add("    if rising_edge(clk50) then");
      Contents.add("      char_ascii <= ascii_w;");
      Contents.add("      char_ready <= ready_w;");
      Contents.add("      caps <= caps_w;");
      Contents.add("      shift <= shift_w;");
      Contents.add("");
      Contents.add("      ps2_recv_prev <= ps2_recv;");
      Contents.add("      if ps2_recv = '1' and ps2_recv_prev = '0' then");
      Contents.add("        -- sc7 <= sc6;");
      Contents.add("        -- sc6 <= sc5;");
      Contents.add("        -- sc5 <= sc4;");
      Contents.add("        -- sc4 <= sc3;");
      Contents.add("        -- sc3 <= sc2;");
      Contents.add("        -- sc2 <= sc1;");
      Contents.add("        sc1 <= sc0;");
      Contents.add("        sc0 <= ps2_code;");
      Contents.add("        if (ready_w = '1') or (ctrl_w = '1') then");
      Contents.add("          scc <= 1;");
      Contents.add("        else");
      Contents.add("          scc <= scc + 1;");
      Contents.add("        end if;");
      Contents.add("      elsif (ready_w = '1') or (ctrl_w = '1') then");
      Contents.add("        scc <= 0;");
      Contents.add("      end if;");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  process (caps, shift, scc, sc0, sc1) is");
      Contents.add("  begin");
      Contents.add("    caps_w <= caps;");
      Contents.add("    shift_w <= shift;");
      Contents.add("    if (scc = 1) and (caps = shift) then");
      Contents.add("            -- unshifted");
      Contents.add("      case sc0 is");
      Contents.add("        when X\"1C\" => ascii_w <= X\"61\"; ready_w <= '1'; ctrl_w <= '0';  -- 1C | F0,1C (ascii \"a\")");
      Contents.add("        when X\"32\" => ascii_w <= X\"62\"; ready_w <= '1'; ctrl_w <= '0';  -- 32 | F0,32 (ascii \"b\")");
      Contents.add("        when X\"21\" => ascii_w <= X\"63\"; ready_w <= '1'; ctrl_w <= '0';  -- 21 | F0,21 (ascii \"c\")");
      Contents.add("        when X\"23\" => ascii_w <= X\"64\"; ready_w <= '1'; ctrl_w <= '0';  -- 23 | F0,23 (ascii \"d\")");
      Contents.add("        when X\"24\" => ascii_w <= X\"65\"; ready_w <= '1'; ctrl_w <= '0';  -- 24 | F0,24 (ascii \"e\")");
      Contents.add("        when X\"2B\" => ascii_w <= X\"66\"; ready_w <= '1'; ctrl_w <= '0';  -- 2B | F0,2B (ascii \"f\")");
      Contents.add("        when X\"34\" => ascii_w <= X\"67\"; ready_w <= '1'; ctrl_w <= '0';  -- 34 | F0,34 (ascii \"g\")");
      Contents.add("        when X\"33\" => ascii_w <= X\"68\"; ready_w <= '1'; ctrl_w <= '0';  -- 33 | F0,33 (ascii \"h\")");
      Contents.add("        when X\"43\" => ascii_w <= X\"69\"; ready_w <= '1'; ctrl_w <= '0';  -- 43 | F0,43 (ascii \"i\")");
      Contents.add("        when X\"3B\" => ascii_w <= X\"6a\"; ready_w <= '1'; ctrl_w <= '0';  -- 3B | F0,3B (ascii \"j\")");
      Contents.add("        when X\"42\" => ascii_w <= X\"6b\"; ready_w <= '1'; ctrl_w <= '0';  -- 42 | F0,42 (ascii \"k\")");
      Contents.add("        when X\"4B\" => ascii_w <= X\"6c\"; ready_w <= '1'; ctrl_w <= '0';  -- 4B | F0,4B (ascii \"l\")");
      Contents.add("        when X\"3A\" => ascii_w <= X\"6d\"; ready_w <= '1'; ctrl_w <= '0';  -- 3A | F0,3A (ascii \"m\")");
      Contents.add("        when X\"31\" => ascii_w <= X\"6e\"; ready_w <= '1'; ctrl_w <= '0';  -- 31 | F0,31 (ascii \"n\")");
      Contents.add("        when X\"44\" => ascii_w <= X\"6f\"; ready_w <= '1'; ctrl_w <= '0';  -- 44 | F0,44 (ascii \"o\")");
      Contents.add("        when X\"4D\" => ascii_w <= X\"70\"; ready_w <= '1'; ctrl_w <= '0';  -- 4D | F0,4D (ascii \"p\")");
      Contents.add("        when X\"15\" => ascii_w <= X\"71\"; ready_w <= '1'; ctrl_w <= '0';  -- 15 | F0,15 (ascii \"q\")");
      Contents.add("        when X\"2D\" => ascii_w <= X\"72\"; ready_w <= '1'; ctrl_w <= '0';  -- 2D | F0,2D (ascii \"r\")");
      Contents.add("        when X\"1B\" => ascii_w <= X\"73\"; ready_w <= '1'; ctrl_w <= '0';  -- 1B | F0,1B (ascii \"s\")");
      Contents.add("        when X\"2C\" => ascii_w <= X\"74\"; ready_w <= '1'; ctrl_w <= '0';  -- 2C | F0,2C (ascii \"t\")");
      Contents.add("        when X\"3C\" => ascii_w <= X\"75\"; ready_w <= '1'; ctrl_w <= '0';  -- 3C | F0,3C (ascii \"u\")");
      Contents.add("        when X\"2A\" => ascii_w <= X\"76\"; ready_w <= '1'; ctrl_w <= '0';  -- 2A | F0,2A (ascii \"v\")");
      Contents.add("        when X\"1D\" => ascii_w <= X\"77\"; ready_w <= '1'; ctrl_w <= '0';  -- 1D | F0,1D (ascii \"w\")");
      Contents.add("        when X\"22\" => ascii_w <= X\"78\"; ready_w <= '1'; ctrl_w <= '0';  -- 22 | F0,22 (ascii \"x\")");
      Contents.add("        when X\"35\" => ascii_w <= X\"79\"; ready_w <= '1'; ctrl_w <= '0';  -- 35 | F0,35 (ascii \"y\")");
      Contents.add("        when X\"1A\" => ascii_w <= X\"7a\"; ready_w <= '1'; ctrl_w <= '0';  -- 1A | F0,1A (ascii \"z\")");
      Contents.add("");
      Contents.add("        when X\"45\" => ascii_w <= X\"30\"; ready_w <= '1'; ctrl_w <= '0';  -- 45 | F0,45 (ascii \"0\")");
      Contents.add("        when X\"16\" => ascii_w <= X\"31\"; ready_w <= '1'; ctrl_w <= '0';  -- 16 | F0,16 (ascii \"1\")");
      Contents.add("        when X\"1E\" => ascii_w <= X\"32\"; ready_w <= '1'; ctrl_w <= '0';  -- 1E | F0,1E (ascii \"2\")");
      Contents.add("        when X\"26\" => ascii_w <= X\"33\"; ready_w <= '1'; ctrl_w <= '0';  -- 26 | F0,26 (ascii \"3\")");
      Contents.add("        when X\"25\" => ascii_w <= X\"34\"; ready_w <= '1'; ctrl_w <= '0';  -- 25 | F0,25 (ascii \"4\")");
      Contents.add("        when X\"2E\" => ascii_w <= X\"35\"; ready_w <= '1'; ctrl_w <= '0';  -- 2E | F0,2E (ascii \"5\")");
      Contents.add("        when X\"36\" => ascii_w <= X\"36\"; ready_w <= '1'; ctrl_w <= '0';  -- 36 | F0,36 (ascii \"6\")");
      Contents.add("        when X\"3D\" => ascii_w <= X\"37\"; ready_w <= '1'; ctrl_w <= '0';  -- 3D | F0,3D (ascii \"7\")");
      Contents.add("        when X\"3E\" => ascii_w <= X\"38\"; ready_w <= '1'; ctrl_w <= '0';  -- 3E | F0,3E (ascii \"8\")");
      Contents.add("        when X\"46\" => ascii_w <= X\"39\"; ready_w <= '1'; ctrl_w <= '0';  -- 46 | F0,46 (ascii \"9\")");
      Contents.add("");
      Contents.add("        when X\"54\" => ascii_w <= X\"5b\"; ready_w <= '1'; ctrl_w <= '0';  -- 54 | F0,54 (ascii \"[\")");
      Contents.add("        when X\"0E\" => ascii_w <= X\"60\"; ready_w <= '1'; ctrl_w <= '0';  -- 0E | F0,0E (ascii \"`\")");
      Contents.add("        when X\"4E\" => ascii_w <= X\"2d\"; ready_w <= '1'; ctrl_w <= '0';  -- 4E | F0,4E (ascii \"-\")");
      Contents.add("        when X\"55\" => ascii_w <= X\"3d\"; ready_w <= '1'; ctrl_w <= '0';  -- 55 | F0,55 (ascii \"=\")");
      Contents.add("        when X\"5D\" => ascii_w <= X\"5c\"; ready_w <= '1'; ctrl_w <= '0';  -- 5D | F0,5D (ascii \"\\\\\") -- should be backslash ");
      Contents.add("        when X\"5B\" => ascii_w <= X\"5d\"; ready_w <= '1'; ctrl_w <= '0';  -- 5B | F0,5B (ascii \"]\")");
      Contents.add("        when X\"4C\" => ascii_w <= X\"3b\"; ready_w <= '1'; ctrl_w <= '0';  -- 4C | F0,4C (ascii \";\")");
      Contents.add("        when X\"52\" => ascii_w <= X\"27\"; ready_w <= '1'; ctrl_w <= '0';  -- 52 | F0,52 (ascii \"'\")");
      Contents.add("        when X\"41\" => ascii_w <= X\"2c\"; ready_w <= '1'; ctrl_w <= '0';  -- 41 | F0,41 (ascii \",\")");
      Contents.add("        when X\"49\" => ascii_w <= X\"2e\"; ready_w <= '1'; ctrl_w <= '0';  -- 49 | F0,49 (ascii \".\")");
      Contents.add("        when X\"4A\" => ascii_w <= X\"2f\"; ready_w <= '1'; ctrl_w <= '0';  -- 4A | F0,4A (ascii \"/\")");
      Contents.add("");
      Contents.add("        when X\"7C\" => ascii_w <= X\"2a\"; ready_w <= '1'; ctrl_w <= '0';  -- 7C | F0,7C (KP) (ascii \"*\")");
      Contents.add("        when X\"7B\" => ascii_w <= X\"2d\"; ready_w <= '1'; ctrl_w <= '0';  -- 7B | F0,7B (KP) (ascii \"-\")");
      Contents.add("        when X\"79\" => ascii_w <= X\"2b\"; ready_w <= '1'; ctrl_w <= '0';  -- 79 | F0,79 (KP) (ascii \"+\")");
      Contents.add("        when X\"71\" => ascii_w <= X\"2e\"; ready_w <= '1'; ctrl_w <= '0';  -- 71 | F0,71 (KP) (ascii \".\")");
      Contents.add("        when X\"70\" => ascii_w <= X\"30\"; ready_w <= '1'; ctrl_w <= '0';  -- 70 | F0,70 (KP) (ascii \"0\")");
      Contents.add("        when X\"69\" => ascii_w <= X\"31\"; ready_w <= '1'; ctrl_w <= '0';  -- 69 | F0,69 (KP) (ascii \"1\")");
      Contents.add("        when X\"72\" => ascii_w <= X\"32\"; ready_w <= '1'; ctrl_w <= '0';  -- 72 | F0,72 (KP) (ascii \"2\")");
      Contents.add("        when X\"7A\" => ascii_w <= X\"33\"; ready_w <= '1'; ctrl_w <= '0';  -- 7A | F0,7A (KP) (ascii \"3\")");
      Contents.add("        when X\"6B\" => ascii_w <= X\"34\"; ready_w <= '1'; ctrl_w <= '0';  -- 6B | F0,6B (KP) (ascii \"4\")");
      Contents.add("        when X\"73\" => ascii_w <= X\"35\"; ready_w <= '1'; ctrl_w <= '0';  -- 73 | F0,73 (KP) (ascii \"5\")");
      Contents.add("        when X\"74\" => ascii_w <= X\"36\"; ready_w <= '1'; ctrl_w <= '0';  -- 74 | F0,74 (KP) (ascii \"6\")");
      Contents.add("        when X\"6C\" => ascii_w <= X\"37\"; ready_w <= '1'; ctrl_w <= '0';  -- 6C | F0,6C (KP) (ascii \"7\")");
      Contents.add("        when X\"75\" => ascii_w <= X\"38\"; ready_w <= '1'; ctrl_w <= '0';  -- 75 | F0,75 (KP) (ascii \"8\")");
      Contents.add("        when X\"7D\" => ascii_w <= X\"39\"; ready_w <= '1'; ctrl_w <= '0';  -- 7D | F0,7D (KP) (ascii \"9\")");
      Contents.add("");
      Contents.add("        when X\"29\" => ascii_w <= X\"20\"; ready_w <= '1'; ctrl_w <= '0';  -- 29 | F0,29 (ascii \" \")");
      Contents.add("        when X\"0D\" => ascii_w <= X\"09\"; ready_w <= '1'; ctrl_w <= '0';  -- 0D | F0,0D (ascii \"\\t\")");
      Contents.add("        when X\"5A\" => ascii_w <= X\"0a\"; ready_w <= '1'; ctrl_w <= '0';  -- 5A | F0,5A (ascii \"\\n\")");
      Contents.add("");
      Contents.add("        when X\"66\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 66 | F0,66 (BKSP)");
      Contents.add("        when X\"58\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; caps_w <= not caps;  -- 58 | F0,58 (CAPS)");
      Contents.add("        when X\"77\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 77 | F0,77 (NUM)");
      Contents.add("        when X\"12\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; shift_w <= '1';  -- 12 | F0,12 (L SHFT)");
      Contents.add("        when X\"14\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 14 | F0,14 (L CTRL)");
      Contents.add("        when X\"11\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 11 | F0,11 (L ALT)");
      Contents.add("        when X\"59\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; shift_w <= '1';  -- 59 | F0,59 (R SHFT)");
      Contents.add("        when X\"76\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 76 | F0,76 (ESC)");
      Contents.add("        when X\"05\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 05 | F0,05 (F1)");
      Contents.add("        when X\"06\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 06 | F0,06 (F2)");
      Contents.add("        when X\"04\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 04 | F0,04 (F3)");
      Contents.add("        when X\"0C\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0C | F0,0C (F4)");
      Contents.add("        when X\"03\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 03 | F0,03 (F5)");
      Contents.add("        when X\"0B\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0B | F0,0B (F6)");
      Contents.add("        when X\"83\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 83 | F0,83 (F7)");
      Contents.add("        when X\"0A\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0A | F0,0A (F8)");
      Contents.add("        when X\"01\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 01 | F0,01 (F9)");
      Contents.add("        when X\"09\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 09 | F0,09 (F10)");
      Contents.add("        when X\"78\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 78 | F0,78 (F11)");
      Contents.add("        when X\"07\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 07 | F0,07 (F12)");
      Contents.add("        when X\"7E\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 7E | F0,7E (SCROLL)");
      Contents.add("");
      Contents.add("        when X\"F0\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of break code");
      Contents.add("        when X\"E0\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of multi-byte make or break code");
      Contents.add("        when X\"E1\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of multi-byte make or break code");
      Contents.add("");
      Contents.add("        when others => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; -- unrecognized, discard");
      Contents.add("");
      Contents.add("      end case;");
      Contents.add("    elsif scc = 1 then");
      Contents.add("            -- shifted");
      Contents.add("      case sc0 is");
      Contents.add("        when X\"1C\" => ascii_w <= X\"41\"; ready_w <= '1'; ctrl_w <= '0';  -- 1C | F0,1C (ascii \"A\")");
      Contents.add("        when X\"32\" => ascii_w <= X\"42\"; ready_w <= '1'; ctrl_w <= '0';  -- 32 | F0,32 (ascii \"B\")");
      Contents.add("        when X\"21\" => ascii_w <= X\"43\"; ready_w <= '1'; ctrl_w <= '0';  -- 21 | F0,21 (ascii \"C\")");
      Contents.add("        when X\"23\" => ascii_w <= X\"44\"; ready_w <= '1'; ctrl_w <= '0';  -- 23 | F0,23 (ascii \"D\")");
      Contents.add("        when X\"24\" => ascii_w <= X\"45\"; ready_w <= '1'; ctrl_w <= '0';  -- 24 | F0,24 (ascii \"E\")");
      Contents.add("        when X\"2B\" => ascii_w <= X\"46\"; ready_w <= '1'; ctrl_w <= '0';  -- 2B | F0,2B (ascii \"F\")");
      Contents.add("        when X\"34\" => ascii_w <= X\"47\"; ready_w <= '1'; ctrl_w <= '0';  -- 34 | F0,34 (ascii \"G\")");
      Contents.add("        when X\"33\" => ascii_w <= X\"48\"; ready_w <= '1'; ctrl_w <= '0';  -- 33 | F0,33 (ascii \"H\")");
      Contents.add("        when X\"43\" => ascii_w <= X\"49\"; ready_w <= '1'; ctrl_w <= '0';  -- 43 | F0,43 (ascii \"I\")");
      Contents.add("        when X\"3B\" => ascii_w <= X\"4a\"; ready_w <= '1'; ctrl_w <= '0';  -- 3B | F0,3B (ascii \"J\")");
      Contents.add("        when X\"42\" => ascii_w <= X\"4b\"; ready_w <= '1'; ctrl_w <= '0';  -- 42 | F0,42 (ascii \"K\")");
      Contents.add("        when X\"4B\" => ascii_w <= X\"4c\"; ready_w <= '1'; ctrl_w <= '0';  -- 4B | F0,4B (ascii \"L\")");
      Contents.add("        when X\"3A\" => ascii_w <= X\"4d\"; ready_w <= '1'; ctrl_w <= '0';  -- 3A | F0,3A (ascii \"M\")");
      Contents.add("        when X\"31\" => ascii_w <= X\"4e\"; ready_w <= '1'; ctrl_w <= '0';  -- 31 | F0,31 (ascii \"N\")");
      Contents.add("        when X\"44\" => ascii_w <= X\"4f\"; ready_w <= '1'; ctrl_w <= '0';  -- 44 | F0,44 (ascii \"O\")");
      Contents.add("        when X\"4D\" => ascii_w <= X\"50\"; ready_w <= '1'; ctrl_w <= '0';  -- 4D | F0,4D (ascii \"P\")");
      Contents.add("        when X\"15\" => ascii_w <= X\"51\"; ready_w <= '1'; ctrl_w <= '0';  -- 15 | F0,15 (ascii \"Q\")");
      Contents.add("        when X\"2D\" => ascii_w <= X\"52\"; ready_w <= '1'; ctrl_w <= '0';  -- 2D | F0,2D (ascii \"R\")");
      Contents.add("        when X\"1B\" => ascii_w <= X\"53\"; ready_w <= '1'; ctrl_w <= '0';  -- 1B | F0,1B (ascii \"S\")");
      Contents.add("        when X\"2C\" => ascii_w <= X\"54\"; ready_w <= '1'; ctrl_w <= '0';  -- 2C | F0,2C (ascii \"T\")");
      Contents.add("        when X\"3C\" => ascii_w <= X\"55\"; ready_w <= '1'; ctrl_w <= '0';  -- 3C | F0,3C (ascii \"U\")");
      Contents.add("        when X\"2A\" => ascii_w <= X\"56\"; ready_w <= '1'; ctrl_w <= '0';  -- 2A | F0,2A (ascii \"V\")");
      Contents.add("        when X\"1D\" => ascii_w <= X\"57\"; ready_w <= '1'; ctrl_w <= '0';  -- 1D | F0,1D (ascii \"W\")");
      Contents.add("        when X\"22\" => ascii_w <= X\"58\"; ready_w <= '1'; ctrl_w <= '0';  -- 22 | F0,22 (ascii \"X\")");
      Contents.add("        when X\"35\" => ascii_w <= X\"59\"; ready_w <= '1'; ctrl_w <= '0';  -- 35 | F0,35 (ascii \"Y\")");
      Contents.add("        when X\"1A\" => ascii_w <= X\"5a\"; ready_w <= '1'; ctrl_w <= '0';  -- 1A | F0,1A (ascii \"Z\")");
      Contents.add("");
      Contents.add("        when X\"45\" => ascii_w <= X\"29\"; ready_w <= '1'; ctrl_w <= '0';  -- 45 | F0,45 (ascii \"0\", shifted to \")\")");
      Contents.add("        when X\"16\" => ascii_w <= X\"21\"; ready_w <= '1'; ctrl_w <= '0';  -- 16 | F0,16 (ascii \"1\", shifted to \"!\")");
      Contents.add("        when X\"1E\" => ascii_w <= X\"40\"; ready_w <= '1'; ctrl_w <= '0';  -- 1E | F0,1E (ascii \"2\", shifted to \"@\")");
      Contents.add("        when X\"26\" => ascii_w <= X\"23\"; ready_w <= '1'; ctrl_w <= '0';  -- 26 | F0,26 (ascii \"3\", shifted to \"#\")");
      Contents.add("        when X\"25\" => ascii_w <= X\"24\"; ready_w <= '1'; ctrl_w <= '0';  -- 25 | F0,25 (ascii \"4\", shifted to \"$\")");
      Contents.add("        when X\"2E\" => ascii_w <= X\"25\"; ready_w <= '1'; ctrl_w <= '0';  -- 2E | F0,2E (ascii \"5\", shifted to \"%\")");
      Contents.add("        when X\"36\" => ascii_w <= X\"5e\"; ready_w <= '1'; ctrl_w <= '0';  -- 36 | F0,36 (ascii \"6\", shifted to \"^\")");
      Contents.add("        when X\"3D\" => ascii_w <= X\"26\"; ready_w <= '1'; ctrl_w <= '0';  -- 3D | F0,3D (ascii \"7\", shifted to \"&\")");
      Contents.add("        when X\"3E\" => ascii_w <= X\"2a\"; ready_w <= '1'; ctrl_w <= '0';  -- 3E | F0,3E (ascii \"8\", shifted to \"*\")");
      Contents.add("        when X\"46\" => ascii_w <= X\"28\"; ready_w <= '1'; ctrl_w <= '0';  -- 46 | F0,46 (ascii \"9\", shifted to \"(\")");
      Contents.add("");
      Contents.add("        when X\"54\" => ascii_w <= X\"7b\"; ready_w <= '1'; ctrl_w <= '0';  -- 54 | F0,54 (ascii \"[\", shifted to \"{\")");
      Contents.add("        when X\"0E\" => ascii_w <= X\"7e\"; ready_w <= '1'; ctrl_w <= '0';  -- 0E | F0,0E (ascii \"`\", shifted to \"~\")");
      Contents.add("        when X\"4E\" => ascii_w <= X\"5f\"; ready_w <= '1'; ctrl_w <= '0';  -- 4E | F0,4E (ascii \"-\", shifted to \"_\")");
      Contents.add("        when X\"55\" => ascii_w <= X\"2b\"; ready_w <= '1'; ctrl_w <= '0';  -- 55 | F0,55 (ascii \"=\", shifted to \"+\")");
      Contents.add("        when X\"5D\" => ascii_w <= X\"7c\"; ready_w <= '1'; ctrl_w <= '0';  -- 5D | F0,5D (ascii \"?\", shifted to \"|\") -- should be backslash ");
      Contents.add("        when X\"5B\" => ascii_w <= X\"7d\"; ready_w <= '1'; ctrl_w <= '0';  -- 5B | F0,5B (ascii \"]\", shifted to \"}\")");
      Contents.add("        when X\"4C\" => ascii_w <= X\"3a\"; ready_w <= '1'; ctrl_w <= '0';  -- 4C | F0,4C (ascii \";\", shifted to \":\")");
      Contents.add("        when X\"52\" => ascii_w <= X\"22\"; ready_w <= '1'; ctrl_w <= '0';  -- 52 | F0,52 (ascii \"'\", shifted to \"\\\"\")");
      Contents.add("        when X\"41\" => ascii_w <= X\"3c\"; ready_w <= '1'; ctrl_w <= '0';  -- 41 | F0,41 (ascii \",\", shifted to \"<\")");
      Contents.add("        when X\"49\" => ascii_w <= X\"3e\"; ready_w <= '1'; ctrl_w <= '0';  -- 49 | F0,49 (ascii \".\", shifted to \">\")");
      Contents.add("        when X\"4A\" => ascii_w <= X\"3f\"; ready_w <= '1'; ctrl_w <= '0';  -- 4A | F0,4A (ascii \"/\", shifted to \"?\")");
      Contents.add("");
      Contents.add("        when X\"7C\" => ascii_w <= X\"2a\"; ready_w <= '1'; ctrl_w <= '0';  -- 7C | F0,7C (KP) (ascii \"*\")");
      Contents.add("        when X\"7B\" => ascii_w <= X\"2d\"; ready_w <= '1'; ctrl_w <= '0';  -- 7B | F0,7B (KP) (ascii \"-\")");
      Contents.add("        when X\"79\" => ascii_w <= X\"2b\"; ready_w <= '1'; ctrl_w <= '0';  -- 79 | F0,79 (KP) (ascii \"+\")");
      Contents.add("        when X\"71\" => ascii_w <= X\"2e\"; ready_w <= '1'; ctrl_w <= '0';  -- 71 | F0,71 (KP) (ascii \".\")");
      Contents.add("        when X\"70\" => ascii_w <= X\"30\"; ready_w <= '1'; ctrl_w <= '0';  -- 70 | F0,70 (KP) (ascii \"0\")");
      Contents.add("        when X\"69\" => ascii_w <= X\"31\"; ready_w <= '1'; ctrl_w <= '0';  -- 69 | F0,69 (KP) (ascii \"1\")");
      Contents.add("        when X\"72\" => ascii_w <= X\"32\"; ready_w <= '1'; ctrl_w <= '0';  -- 72 | F0,72 (KP) (ascii \"2\")");
      Contents.add("        when X\"7A\" => ascii_w <= X\"33\"; ready_w <= '1'; ctrl_w <= '0';  -- 7A | F0,7A (KP) (ascii \"3\")");
      Contents.add("        when X\"6B\" => ascii_w <= X\"34\"; ready_w <= '1'; ctrl_w <= '0';  -- 6B | F0,6B (KP) (ascii \"4\")");
      Contents.add("        when X\"73\" => ascii_w <= X\"35\"; ready_w <= '1'; ctrl_w <= '0';  -- 73 | F0,73 (KP) (ascii \"5\")");
      Contents.add("        when X\"74\" => ascii_w <= X\"36\"; ready_w <= '1'; ctrl_w <= '0';  -- 74 | F0,74 (KP) (ascii \"6\")");
      Contents.add("        when X\"6C\" => ascii_w <= X\"37\"; ready_w <= '1'; ctrl_w <= '0';  -- 6C | F0,6C (KP) (ascii \"7\")");
      Contents.add("        when X\"75\" => ascii_w <= X\"38\"; ready_w <= '1'; ctrl_w <= '0';  -- 75 | F0,75 (KP) (ascii \"8\")");
      Contents.add("        when X\"7D\" => ascii_w <= X\"39\"; ready_w <= '1'; ctrl_w <= '0';  -- 7D | F0,7D (KP) (ascii \"9\")");
      Contents.add("");
      Contents.add("        when X\"29\" => ascii_w <= X\"20\"; ready_w <= '1'; ctrl_w <= '0';  -- 29 | F0,29 (ascii \" \")");
      Contents.add("        when X\"0D\" => ascii_w <= X\"09\"; ready_w <= '1'; ctrl_w <= '0';  -- 0D | F0,0D (ascii \"\\t\")");
      Contents.add("        when X\"5A\" => ascii_w <= X\"0a\"; ready_w <= '1'; ctrl_w <= '0';  -- 5A | F0,5A (ascii \"\\n\")");
      Contents.add("");
      Contents.add("        when X\"66\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 66 | F0,66 (BKSP)");
      Contents.add("        when X\"58\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; caps_w <= not caps;  -- 58 | F0,58 (CAPS)");
      Contents.add("        when X\"77\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 77 | F0,77 (NUM)");
      Contents.add("        when X\"12\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; shift_w <= '1';  -- 12 | F0,12 (L SHFT)");
      Contents.add("        when X\"14\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 14 | F0,14 (L CTRL)");
      Contents.add("        when X\"11\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 11 | F0,11 (L ALT)");
      Contents.add("        when X\"59\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; shift_w <= '1';  -- 59 | F0,59 (R SHFT)");
      Contents.add("        when X\"76\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 76 | F0,76 (ESC)");
      Contents.add("        when X\"05\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 05 | F0,05 (F1)");
      Contents.add("        when X\"06\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 06 | F0,06 (F2)");
      Contents.add("        when X\"04\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 04 | F0,04 (F3)");
      Contents.add("        when X\"0C\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0C | F0,0C (F4)");
      Contents.add("        when X\"03\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 03 | F0,03 (F5)");
      Contents.add("        when X\"0B\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0B | F0,0B (F6)");
      Contents.add("        when X\"83\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 83 | F0,83 (F7)");
      Contents.add("        when X\"0A\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 0A | F0,0A (F8)");
      Contents.add("        when X\"01\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 01 | F0,01 (F9)");
      Contents.add("        when X\"09\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 09 | F0,09 (F10)");
      Contents.add("        when X\"78\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 78 | F0,78 (F11)");
      Contents.add("        when X\"07\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 07 | F0,07 (F12)");
      Contents.add("        when X\"7E\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1';  -- 7E | F0,7E (SCROLL)");
      Contents.add("");
      Contents.add("        when X\"F0\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of break code");
      Contents.add("        when X\"E0\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of multi-byte make or break code");
      Contents.add("        when X\"E1\" => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0';  -- start of multi-byte make or break code");
      Contents.add("");
      Contents.add("        when others => ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; -- unrecognized, discard");
      Contents.add("");
      Contents.add("      end case;");
      Contents.add("    elsif scc = 2 then");
      Contents.add("      -- KP /      | E0,4A                     | E0,F0,4A");
      Contents.add("      -- KP EN     | E0,5A                     | E0,F0,5A");
      Contents.add("      -- INSERT    | E0,70                     | E0,F0,70");
      Contents.add("      -- HOME      | E0,6C                     | E0,F0,6C");
      Contents.add("      -- PG UP     | E0,7D                     | E0,F0,7D");
      Contents.add("      -- DELETE    | E0,71                     | E0,F0,71");
      Contents.add("      -- end       | E0,69                     | E0,F0,69");
      Contents.add("      -- PG DN     | E0,7A                     | E0,F0,7A");
      Contents.add("      -- U ARROW   | E0,75                     | E0,F0,75");
      Contents.add("      -- L ARROW   | E0,6B                     | E0,F0,6B");
      Contents.add("      -- D ARROW   | E0,72                     | E0,F0,72");
      Contents.add("      -- R ARROW   | E0,74                     | E0,F0,74");
      Contents.add("      -- L GUI     | E0,1F                     | E0,F0,1F");
      Contents.add("      -- R ALT     | E0,11                     | E0,F0,11");
      Contents.add("      -- R CTRL    | E0,14                     | E0,F0,14");
      Contents.add("      -- R GUI     | E0,27                     | E0,F0,27");
      Contents.add("      -- APPS      | E0,2F                     | E0,F0,2F");
      Contents.add("      -- PRNT SCRN | E0,12, E0,7C              | E0,F0,7C, E0,F0,12 ");
      Contents.add("      -- PAUSE     | E1,14,77, E1,F0,14, F0,77 | ");
      Contents.add("");
      Contents.add("      if (sc1 = X\"F0\") and ((sc0 = X\"12\") or (sc0 = X\"59\")) then");
      Contents.add("        ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; shift_w <= '0'; -- break for (L SHFT) or (R SHFT)");
      Contents.add("      elsif sc1 = X\"F0\" then");
      Contents.add("        ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; -- break for other 1 byte make codes");
      Contents.add("      elsif (sc1 = X\"E0\") and (sc0 = X\"4a\") then");
      Contents.add("        ascii_w <= X\"2f\"; ready_w <= '1'; ctrl_w <= '0'; -- E0,4A | E0,F0,4A (KP) (ascii \"/\")");
      Contents.add("      elsif (sc1 = X\"E0\") and (sc0 = X\"5a\") then");
      Contents.add("        ascii_w <= X\"0a\"; ready_w <= '1'; ctrl_w <= '0'; -- E0,5A | E0,F0,5A (KP) (ascii \"\\n\")");
      Contents.add("      elsif (sc1 = X\"E0\") and (sc0 /= X\"F0\") then");
      Contents.add("        ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '1'; -- other 2 byte make codes");
      Contents.add("      else");
      Contents.add("        ascii_w <= X\"00\"; ready_w <= '0'; ctrl_w <= '0'; -- other");
      Contents.add("      end if;");
      Contents.add("    elsif scc = 3 then");
      Contents.add("      ascii_w <= X\"00\";");
      Contents.add("      ready_w <= '0';");
      Contents.add("      ctrl_w <= '1'; -- probably 3 byte make code or break code for 2 byte make code ");
      Contents.add("    else");
      Contents.add("      ascii_w <= X\"00\";");
      Contents.add("      ready_w <= '0';");
      Contents.add("      ctrl_w <= '0';");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("  -- ps2_keyboard scancode protocol");
      Contents.add("  -----------------------------------------------------------------------------");
      Contents.add("");
      Contents.add("  fifo_pop <= ClockEnable AND Read;");
      Contents.add("");
      Contents.add("  -- fifo process");
      Contents.add("  process (clk50)");
      Contents.add("    type FIFO_Memory is array (0 to FIFO_DEPTH - 1) of std_logic_vector (DATA_WIDTH - 1 downto 0);");
      Contents.add("    variable Memory : FIFO_Memory;");
      Contents.add("    variable Head : natural range 0 to FIFO_DEPTH - 1;");
      Contents.add("    variable Tail : natural range 0 to FIFO_DEPTH - 1;");
      Contents.add("    variable Wrapped : boolean;");
      Contents.add("  begin");
      Contents.add("    if (rising_edge(clk50)) then");
      Contents.add("      -- Pop old data");
      Contents.add("      if (fifo_pop = '1') then");
      Contents.add("        if ((Wrapped = true) or (Head /= Tail)) then");
      Contents.add("          -- Update Tail pointer as needed");
      Contents.add("          if (Tail = FIFO_DEPTH - 1) then");
      Contents.add("            Tail := 0;");
      Contents.add("            Wrapped := false;");
      Contents.add("          else");
      Contents.add("            Tail := Tail + 1;");
      Contents.add("          end if;");
      Contents.add("        end if;");
      Contents.add("      end if;");
      Contents.add("");
      Contents.add("      -- Update data output");
      Contents.add("      ascii <= Memory(Tail);");
      Contents.add("");
      Contents.add("      -- Push new data");
      Contents.add("      if (char_ready = '1') then");
      Contents.add("        if ((Wrapped = false) or (Head /= Tail)) then");
      Contents.add("            -- Write Data to Memory");
      Contents.add("          Memory(Head) := char_ascii;");
      Contents.add("");
      Contents.add("            -- Increment Head pointer as needed");
      Contents.add("          if (Head = FIFO_DEPTH - 1) then");
      Contents.add("            Head := 0;");
      Contents.add("");
      Contents.add("            Wrapped := true;");
      Contents.add("          else");
      Contents.add("            Head := Head + 1;");
      Contents.add("          end if;");
      Contents.add("        end if;");
      Contents.add("      end if;");
      Contents.add("");
      Contents.add("      -- Update status flags");
      Contents.add("      if (Head = Tail) then");
      Contents.add("        if Wrapped then");
      Contents.add("          -- full <= '1';");
      Contents.add("          ready <= '1';");
      Contents.add("          -- empty <= '0';");
      Contents.add("        else");
      Contents.add("          -- full <= '0';");
      Contents.add("          ready <= '0';");
      Contents.add("          -- empty <= '1';");
      Contents.add("        end if;");
      Contents.add("      else");
      Contents.add("        -- full  <= '0';");
      Contents.add("        ready <= '1';");
      Contents.add("        -- empty  <= '0';");
      Contents.add("      end if;");
      Contents.add("    end if;");
      Contents.add("  end process;");
      Contents.add("");
      Contents.add("END PlatformIndependent;");
      Contents.add("");
    }
    return Contents;
  }
}
