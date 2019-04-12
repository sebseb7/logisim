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

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.bfh.logisim.hdlgenerator.HiddenPort;
import com.cburch.logisim.hdl.Hdl;

public class TtyHDLGenerator extends HDLGenerator {

  public TtyHDLGenerator(ComponentContext ctx) {
    super(ctx, "io", "Tty", "i_Tty");
    int w = Tty.getWidth(_attrs.getValue(Tty.ATTR_WIDTH));
    parameters.add("AsciiWidth", w);
    long period_ns = 1000000000 / ctx.oscFreq;
    parameters.add("CLK_PERIOD_NS", (int)period_ns);

    // todo: support CLR
    _err.AddWarning("Note: Clear signal, if used, is likely broken for TTY component in HDL");

    // Note: We expect the slow clock to actually be FPGAClock (or its inverse),
    // which it will be whenever the clock input is connected to a proper clock
    // bus. In cases where this is not the case, e.g. when using a gated clock,
    // are not supported: proper HDL would be tricky, since we need data to
    // cross between the raw clock domain and slow clock domain. There is a
    // warning in HDLGenerator about this.

    clockPort = new ClockPortInfo("FPGAClock", "SlowClockEnable", Tty.CK);
    inPorts.add("Enable", 1, Tty.WE, false);
    inPorts.add("Clear", 1, Tty.CLR, false);
    inPorts.add("Data", "AsciiWidth", Tty.IN, false);

    String[] labels = new String[] {
      "lcd_bl",
      "lcd_db0", "lcd_db1", "lcd_db2", "lcd_db3",
      "lcd_db4", "lcd_db5", "lcd_db6", "lcd_db7",
      "lcd_en",
      "lcd_rw",
      "lcd_rs" };
    hiddenPort = HiddenPort.makeInOutport(labels, HiddenPort.Ribbon, HiddenPort.Pin);
  }

  @Override
	protected Hdl getArchitecture() {
    Hdl out = new Hdl(_lang, _err);
    generateFileHeader(out);

    if (out.isVhdl) {
      out.add("");
      out.add("-------------------------------------------------------------------------------");
      out.add("-- Title      : TTY-like emulator and 16x2 LCD controller");
      out.add("-- Project    : ");
      out.add("-------------------------------------------------------------------------------");
      out.add("-- Author     : <stachelsau@T420> <kwalsh@holycross.edu>");
      out.add("-- Created    : 2012-07-28");
      out.add("-- Last update: 2016-02-05");
      out.add("-------------------------------------------------------------------------------");
      out.add("-- Description: The controller initializes the display when rst goes to '0'.");
      out.add("--              After that it maintains a 16x2 buffer of characters and writes");
      out.add("--              these continously to the display.");
      out.add("-------------------------------------------------------------------------------");
      out.add("-- Copyright (c) 2012 ");
      out.add("-------------------------------------------------------------------------------");
      out.add("-- Revisions  :");
      out.add("-- Date        Version  Author      Description");
      out.add("-- 2012-07-28  1.0      stachelsau  Created");
      out.add("-- 2016-02-05  1.1      kwalsh      Adapted for Logisim Evolution");
      out.add("-- 2016-08-19  1.2      kwalsh      Added TTY style interface");
      out.add("-------------------------------------------------------------------------------");
      out.add("");
      out.add("architecture logisim_generated of Tty is ");
      out.add("");
      out.add("  -- constant CLK_PERIOD_NS : positive := 20; -- 50MHz");
      out.add("  constant DELAY_15_MS   : positive := 15 * 10**6 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_1640_US : positive := 1640 * 10**3 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_4100_US : positive := 4100 * 10**3 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_100_US  : positive := 100 * 10**3 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_40_US   : positive := 40 * 10**3 / CLK_PERIOD_NS + 1;");
      out.add("");
      out.add("  constant DELAY_NIBBLE     : positive := 10**3 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_LCD_E      : positive := 230 / CLK_PERIOD_NS + 1;");
      out.add("  constant DELAY_SETUP_HOLD : positive := 40 / CLK_PERIOD_NS + 1;");
      out.add("");
      out.add("  constant MAX_DELAY : positive := DELAY_15_MS;");
      out.add("");
      out.add("  -- this record describes one write operation");
      out.add("  type op_t is record");
      out.add("    rs      : std_logic;");
      out.add("    data    : std_logic_vector(7 downto 0);");
      out.add("    delay_h : integer range 0 to MAX_DELAY;");
      out.add("    delay_l : integer range 0 to MAX_DELAY;");
      out.add("  end record op_t;");
      out.add("  constant default_op      : op_t := (rs => '1', data => X\"00\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
      out.add("  constant op_select_line1 : op_t := (rs => '0', data => X\"80\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
      out.add("  constant op_select_line2 : op_t := (rs => '0', data => X\"C0\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US);");
      out.add("");
      out.add("  -- init + config operations:");
      out.add("  -- write 3 x 0x3 followed by 0x2");
      out.add("  -- function set command");
      out.add("  -- entry mode set command");
      out.add("  -- display on/off command");
      out.add("  -- clear display");
      out.add("  type config_ops_t is array(0 to 5) of op_t;");
      out.add("  constant config_ops : config_ops_t");
      out.add("  := (5 => (rs => '0', data => X\"33\", delay_h => DELAY_4100_US, delay_l => DELAY_100_US),");
      out.add("  4 => (rs => '0', data => X\"32\", delay_h => DELAY_40_US, delay_l => DELAY_40_US),");
      out.add("  3 => (rs => '0', data => X\"28\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
      out.add("  2 => (rs => '0', data => X\"06\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
      out.add("  1 => (rs => '0', data => X\"0C\", delay_h => DELAY_NIBBLE, delay_l => DELAY_40_US),");
      out.add("  0 => (rs => '0', data => X\"01\", delay_h => DELAY_NIBBLE, delay_l => DELAY_1640_US));");
      out.add("");
      out.add("  signal this_op : op_t;");
      out.add("");
      out.add("  type op_state_t is (IDLE,");
      out.add("  WAIT_SETUP_H,");
      out.add("  ENABLE_H,");
      out.add("  WAIT_HOLD_H,");
      out.add("  WAIT_DELAY_H,");
      out.add("  WAIT_SETUP_L,");
      out.add("  ENABLE_L,");
      out.add("  WAIT_HOLD_L,");
      out.add("  WAIT_DELAY_L,");
      out.add("  DONE);");
      out.add("");
      out.add("  signal op_state      : op_state_t := DONE;");
      out.add("  signal next_op_state : op_state_t;");
      out.add("  signal cnt           : natural range 0 to MAX_DELAY;");
      out.add("  signal next_cnt      : natural range 0 to MAX_DELAY;");
      out.add("");
      out.add("  type state_t is (RESET,");
      out.add("  CONFIG,");
      out.add("  SELECT_LINE1,");
      out.add("  WRITE_LINE1,");
      out.add("  SELECT_LINE2,");
      out.add("  WRITE_LINE2);");
      out.add("");
      out.add("  signal state      : state_t               := RESET;");
      out.add("  signal next_state : state_t;");
      out.add("  signal ptr        : natural range 0 to 15 := 0;");
      out.add("  signal next_ptr   : natural range 0 to 15;");
      out.add("");
      out.add("  signal ascii : std_logic_vector(7 downto 0);");
      out.add("  signal push : std_logic;");
      out.add("  signal init : std_logic;");
      out.add("");
      out.add("  type ROW is array (0 to 15) of std_logic_vector(7 downto 0);");
      out.add("  signal line1, line2 : ROW;");
      out.add("  signal wptr : natural range 0 to 16;");
      out.add("");
      out.add("  signal clk : std_logic;");
      out.add("  signal rst : std_logic;");
      // out.add("  signal lcd_rs : std_logic;");
      // out.add("  signal lcd_rw : std_logic;");
      // out.add("  signal lcd_en : std_logic;");
      out.add("  signal lcd_db : std_logic_vector(7 downto 0);");
      // out.add("  signal lcd_bl : std_logic;");
      out.add("");
      out.add("begin");
      out.add("");
      out.add("  clk <= FPGAClock;");
      out.add("  rst <= Clear;");
      out.add("  lcd_bl <= '1';");
      // out.add("  lcd_rs_rw_en_db_bl <= lcd_rs & lcd_rw & lcd_en & lcd_db & lcd_bl;");
      out.add("  lcd_db0 <= lcd_db(0);");
      out.add("  lcd_db1 <= lcd_db(1);");
      out.add("  lcd_db2 <= lcd_db(2);");
      out.add("  lcd_db3 <= lcd_db(3);");
      out.add("  lcd_db4 <= lcd_db(4);");
      out.add("  lcd_db5 <= lcd_db(5);");
      out.add("  lcd_db6 <= lcd_db(6);");
      out.add("  lcd_db7 <= lcd_db(7);");
      out.add("  push <= SlowClockEnable and Enable;");
      out.add("  ascii <= std_logic_vector(resize(unsigned(Data), 8));");
      out.add("");
      out.add("  -- tty emulation");
      out.add("  process (clk) is");
      out.add("  begin");
      out.add("  if rising_edge(clk) then");
      out.add("    if rst = '1' then");
      out.add("      init <= '0';");
      out.add("    else");
      out.add("      init <= '1';");
      out.add("    end if;");
      out.add("    if init = '0' then");
      out.add("      -- this init sequence doesn't seem to happen");
      out.add("      wptr <= 0;");
      out.add("      for i in 0 to 15 loop");
      out.add("        line1(i) <= X\"20\";");
      out.add("        line2(i) <= X\"20\";");
      out.add("      end loop;");
      out.add("    elsif push = '1' then");
      out.add("      case ascii is");
      out.add("        when X\"09\" => -- \"\\t\"");
      out.add("          if wptr < 8 then");
      out.add("            for i in 0 to 7 loop");
      out.add("              if i >= wptr then");
      out.add("                line2(i) <= X\"20\";");
      out.add("              end if;");
      out.add("            end loop;");
      out.add("            wptr <= 8;");
      out.add("          elsif wptr = 16 then");
      out.add("            line1 <= line2;");
      out.add("            for i in 0 to 15 loop");
      out.add("              line2(i) <= X\"20\";");
      out.add("            end loop;");
      out.add("            wptr <= 8;");
      out.add("          else");
      out.add("            for i in 8 to 15 loop");
      out.add("              if i >= wptr then");
      out.add("                line2(i) <= X\"20\";");
      out.add("              end if;");
      out.add("            end loop;");
      out.add("            wptr <= 16;");
      out.add("          end if;");
      out.add("        when X\"0a\" => -- \"\\n\"");
      out.add("          line1 <= line2;");
      out.add("          for i in 0 to 15 loop");
      out.add("            line2(i) <= X\"20\";");
      out.add("          end loop;");
      out.add("          wptr <= 0;");
      out.add("        when X\"0d\" => -- \"\\r\"");
      out.add("          wptr <= 0;");
      out.add("        when others =>");
      out.add("          if wptr = 16 then");
      out.add("            line1 <= line2;");
      out.add("            -- if ascii(6) = '0' and ascii(5) = '0' then");
      out.add("            --   line2(0) <= X\"EF\";  -- sad face for garbage in 000xxxxx and 100xxxxx ranges");
      out.add("            -- else");
      out.add("              line2(0) <= ascii;");
      out.add("            -- end if;");
      out.add("            for i in 1 to 15 loop");
      out.add("              line2(i) <= X\"20\";");
      out.add("            end loop;");
      out.add("            wptr <= 1;");
      out.add("          else");
      out.add("            -- if ascii(6) = '0' and ascii(5) = '0' then");
      out.add("            --   line2(wptr) <= X\"EF\";  -- sad face for garbage in 000xxxxx and 100xxxxx ranges");
      out.add("            -- else");
      out.add("              line2(wptr) <= ascii;");
      out.add("            -- end if;");
      out.add("            wptr <= wptr + 1;");
      out.add("          end if;");
      out.add("      end case;");
      out.add("    end if;");
      out.add("  end if;");
      out.add("  end process;");
      out.add("");
      out.add("  proc_state : process(state, op_state, ptr, line1, line2) is");
      out.add("  begin");
      out.add("    case state is");
      out.add("      when RESET =>");
      out.add("        this_op    <= default_op;");
      out.add("        next_state <= CONFIG;");
      out.add("        next_ptr   <= config_ops_t'high;");
      out.add("");
      out.add("      when CONFIG =>");
      out.add("        this_op    <= config_ops(ptr);");
      out.add("        next_ptr   <= ptr;");
      out.add("        next_state <= CONFIG;");
      out.add("        if op_state = DONE then");
      out.add("          next_ptr <= ptr - 1;");
      out.add("          if ptr = 0 then");
      out.add("            next_state <= SELECT_LINE1;");
      out.add("          end if;");
      out.add("        end if;");
      out.add("");
      out.add("      when SELECT_LINE1 =>");
      out.add("        this_op  <= op_select_line1;");
      out.add("        next_ptr <= 15;");
      out.add("        if op_state = DONE then");
      out.add("          next_state <= WRITE_LINE1;");
      out.add("        else");
      out.add("          next_state <= SELECT_LINE1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WRITE_LINE1 =>");
      out.add("        this_op      <= default_op;");
      out.add("        if line1(15 - ptr) = X\"00\" then");
      out.add("          this_op.data <= X\"20\";");
      out.add("        else");
      out.add("          this_op.data <= line1(15 - ptr);");
      out.add("        end if;");
      out.add("        next_ptr     <= ptr;");
      out.add("        next_state   <= WRITE_LINE1;");
      out.add("        if op_state = DONE then");
      out.add("          next_ptr <= ptr - 1;");
      out.add("          if ptr = 0 then");
      out.add("            next_state <= SELECT_LINE2;");
      out.add("          end if;");
      out.add("        end if;");
      out.add("");
      out.add("      when SELECT_LINE2 =>");
      out.add("        this_op  <= op_select_line2;");
      out.add("        next_ptr <= 15;");
      out.add("        if op_state = DONE then");
      out.add("          next_state <= WRITE_LINE2;");
      out.add("        else");
      out.add("          next_state <= SELECT_LINE2;");
      out.add("        end if;");
      out.add("");
      out.add("      when WRITE_LINE2 =>");
      out.add("        this_op      <= default_op;");
      out.add("        if line2(15 - ptr) = X\"00\" then");
      out.add("          this_op.data <= X\"20\";");
      out.add("        else");
      out.add("          this_op.data <= line2(15 - ptr);");
      out.add("        end if;");
      out.add("        next_ptr     <= ptr;");
      out.add("        next_state   <= WRITE_LINE2;");
      out.add("        if op_state = DONE then");
      out.add("          next_ptr <= ptr - 1;");
      out.add("          if ptr = 0 then");
      out.add("            next_state <= SELECT_LINE1;");
      out.add("          end if;");
      out.add("        end if;");
      out.add("");
      out.add("    end case;");
      out.add("  end process proc_state;");
      out.add("");
      out.add("  reg_state : process(clk)");
      out.add("  begin");
      out.add("    if rising_edge(clk) then");
      out.add("      if rst = '1' then");
      out.add("        state <= RESET;");
      out.add("        ptr   <= 0;");
      out.add("      else");
      out.add("        state <= next_state;");
      out.add("        ptr   <= next_ptr;");
      out.add("      end if;");
      out.add("    end if;");
      out.add("  end process reg_state;");
      out.add("");
      out.add("  -- we never read from the lcd");
      out.add("  lcd_rw <= '0';");
      out.add("");
      out.add("  proc_op_state : process(op_state, cnt, this_op) is");
      out.add("  begin");
      out.add("    case op_state is");
      out.add("      when IDLE =>");
      out.add("        lcd_db        <= (others => '0');");
      out.add("        lcd_rs        <= '0';");
      out.add("        lcd_en        <= '0';");
      out.add("        next_op_state <= WAIT_SETUP_H;");
      out.add("        next_cnt      <= DELAY_SETUP_HOLD;");
      out.add("");
      out.add("      when WAIT_SETUP_H =>");
      out.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= ENABLE_H;");
      out.add("          next_cnt      <= DELAY_LCD_E;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_SETUP_H;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when ENABLE_H =>");
      out.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '1';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= WAIT_HOLD_H;");
      out.add("          next_cnt      <= DELAY_SETUP_HOLD;");
      out.add("        else");
      out.add("          next_op_state <= ENABLE_H;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WAIT_HOLD_H =>");
      out.add("        lcd_db <= this_op.data(7 downto 4) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= WAIT_DELAY_H;");
      out.add("          next_cnt      <= this_op.delay_h;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_HOLD_H;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WAIT_DELAY_H =>");
      out.add("        lcd_db <= (others => '0');");
      out.add("        lcd_rs <= '0';");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= WAIT_SETUP_L;");
      out.add("          next_cnt      <= DELAY_SETUP_HOLD;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_DELAY_H;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WAIT_SETUP_L =>");
      out.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= ENABLE_L;");
      out.add("          next_cnt      <= DELAY_LCD_E;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_SETUP_L;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when ENABLE_L =>");
      out.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '1';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= WAIT_HOLD_L;");
      out.add("          next_cnt      <= DELAY_SETUP_HOLD;");
      out.add("        else");
      out.add("          next_op_state <= ENABLE_L;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WAIT_HOLD_L =>");
      out.add("        lcd_db <= this_op.data(3 downto 0) & X\"0\";");
      out.add("        lcd_rs <= this_op.rs;");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= WAIT_DELAY_L;");
      out.add("          next_cnt      <= this_op.delay_l;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_HOLD_L;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when WAIT_DELAY_L =>");
      out.add("        lcd_db <= (others => '0');");
      out.add("        lcd_rs <= '0';");
      out.add("        lcd_en <= '0';");
      out.add("        if cnt = 0 then");
      out.add("          next_op_state <= DONE;");
      out.add("          next_cnt      <= 0;");
      out.add("        else");
      out.add("          next_op_state <= WAIT_DELAY_L;");
      out.add("          next_cnt      <= cnt - 1;");
      out.add("        end if;");
      out.add("");
      out.add("      when DONE =>");
      out.add("        lcd_db        <= (others => '0');");
      out.add("        lcd_rs        <= '0';");
      out.add("        lcd_en        <= '0';");
      out.add("        next_op_state <= IDLE;");
      out.add("        next_cnt      <= 0;");
      out.add("");
      out.add("    end case;");
      out.add("  end process proc_op_state;");
      out.add("");
      out.add("  reg_op_state : process (clk) is");
      out.add("  begin");
      out.add("    if rising_edge(clk) then");
      out.add("      if state = RESET then");
      out.add("        op_state <= IDLE;");
      out.add("      else");
      out.add("        op_state <= next_op_state;");
      out.add("        cnt      <= next_cnt;");
      out.add("      end if;");
      out.add("    end if;");
      out.add("  end process reg_op_state;");
      out.add("");
      out.add("end logisim_generated;");
      out.add("");
    }
    return out;
  }
}
