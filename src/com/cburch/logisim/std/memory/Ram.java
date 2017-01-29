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
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JLabel;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

public class Ram extends Mem {

	// kwalsh: Notes on RAM behavior.
	//
	// Logisim simulation never used registered/clocked address and
	// enable lines for writing -- when the clock rises, the addressed memory
	// location changes immediately (if WE is high). For reading, Logisim did
	// use a registered/clocked address, enable, and data output, however, sort
	// of. When the clock rises, the value of the addressed memory location
	// would be read and latched, and, if OE is high, also output immediately.
	// But if OE was low, it would not be output immediately. But if OE
	// subsequently went high, even absent a rising clock edge, then the output
	// would change to be the latched value, then stay that way. This is,
	// needless to say, unusual.
	//
	// FPGA synthesis behaved differently. The address, data in, and both
	// enables are latched on rising clock edges. Writes happen soon after that:
	// when running in fast "raw" clock mode, writes happen at the next rising
	// clock edge; in slow "fake" clock mode, writes happen at the next rising
	// edge of the internal fast clock, so they will appear to happen just
	// following the slow clock edge. Reads would happen at the same time as
	// writes, but the data output was latched into a register (only if OE is
	// high) that gets updated on the next rising edge of the (fast) clock.
	// So data output for reads would appear during the next cycle for slow fake
	// clock mode and 2 full cycles later for fast raw clock mode.
	// Fake slow clock writing exampe:
	//   slow clock rises, start of cycle 0:
	//       OE is 1
	//       Address is aaa
	//   slow clock rises, start of cycle 1
	//       internal latched OEReg is now 1, AddressReg is now aaa
	//       (soon after)
	//       data is written to memory (if WE was 1)
	//       data is read from memory (pre-update old data, if WE was 1)
	//       (soon after)
	//       data is latched into output register (since OE was 1)
	// Raw fast clock writing exampe:
	//   fast clock rises, start of cycle 0:
	//       OE is 1
	//       Address is aaa
	//   fast clock rises, start of cycle 1
	//       internal latched OEReg is now 1, AddressReg is now aaa
	//   fast clock rises, start of cycle 2
	//       data is written to memory (if WE was 1)
	//       data is read from memory (pre-update old data, if WE was 1)
	//    fast clock rises, start of cycle 3:
	//       data is latched into output register (since OE was 1)
	// 
	// New behavior:
	//
	// For Logisim simulation, no internal registers. If you want registers, add
	// them yourself outside the RAM component. If one bidirectional data bus is
	// used, then output enable controls only the tristate for the data bus.
	// Otherwise, there is no output or read enable or any sort. Data output
	// always reflects the current addressed location. When the address changes,
	// the addressed location and the output data changes immediately (or soon
	// after, asynchronously). If you want a registered output, or a tristate
	// output on a two-bus RAM, add one yourself outside the RAM component. If
	// you want registered addresses, add a register yourself outside the RAM
	// component. The clock is used only for writing: when the clock rises, if
	// WE is high, then the addressed location (and data output) is updated
	// immediately or soon after. This behavior is consistent with how registers
	// behave.
	//
	// For FPGA synthesis, it isn't clear that the Cyclone III FPGA, or most
	// other FPGAs, support fully unclocked, asynchronous reads. But we'll try
	// it anyway.

	static final int DIN = MEM_INPUTS + 0; // only if separate
	static final int OE = MEM_INPUTS + 0;  // only if not separate
	static final int WE = MEM_INPUTS + 1;  // always
	static final int CLK = MEM_INPUTS + 2; // always
	static final int BE = MEM_INPUTS + 3;  // as many as needed

	static class ContentsAttribute extends Attribute<MemContents> {

		public ContentsAttribute() {
			super("contents", Strings.getter("romContentsAttr"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, MemContents value) {
			ContentsCell ret = new ContentsCell(source, value);
			ret.mouseClicked(null);
			return ret;
		}

		public MemContents parse(String value) {
			int lineBreak = value.indexOf('\n');
			String first = lineBreak < 0 ? value : value
					.substring(0, lineBreak);
			String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
			StringTokenizer toks = new StringTokenizer(first);
			try {
				String header = toks.nextToken();
				if (!header.equals("addr/data:")) {
					return null;
				}
				int addr = Integer.parseInt(toks.nextToken());
				int data = Integer.parseInt(toks.nextToken());
				MemContents ret = MemContents.create(addr, data);
				HexFile.open(ret, new StringReader(rest));
				return ret;
			} catch (IOException e) {
				return null;
			} catch (NumberFormatException e) {
				return null;
			} catch (NoSuchElementException e) {
				return null;
			}
		}

		@Override
		public String toDisplayString(MemContents value) {
			return Strings.get("romContentsValue");
		}

		@Override
		public String toStandardString(MemContents state) {
			int addr = state.getLogLength();
			int data = state.getWidth();
			StringWriter ret = new StringWriter();
			ret.write("addr/data: " + addr + " " + data + "\n");
			try {
				HexFile.save(ret, state);
			} catch (IOException e) {
			}
			return ret.toString();
		}
	}

	@SuppressWarnings("serial")
	private static class ContentsCell extends JLabel implements MouseListener {

		Window source;
		MemContents contents;

		ContentsCell(Window source, MemContents contents) {
			super(Strings.get("romContentsValue"));
			this.source = source;
			this.contents = contents;
			addMouseListener(this);
		}

		public void mouseClicked(MouseEvent e) {
			if (contents == null) {
				return;
			}
			Project proj = source instanceof Frame ? ((Frame) source)
					.getProject() : null;
			HexFrame frame = RamAttributes.getHexFrame(contents, proj);
			frame.setVisible(true);
			frame.toFront();
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	public static class Logger extends InstanceLogger {

		@Override
		public String getLogName(InstanceState state, Object option) {
			String Label = state.getAttributeValue(StdAttr.LABEL);
			if (Label.equals("")) {
				Label = null;
			}
			if (option instanceof Integer) {
				String disp = Strings.get("ramComponent");
				Location loc = state.getInstance().getLocation();
				return (Label == null) ? disp + loc + "[" + option + "]"
						: Label + "[" + option + "]";
			} else {
				return Label;
			}
		}

		@Override
		public Object[] getLogOptions(InstanceState state) {
			int addrBits = state.getAttributeValue(ADDR_ATTR).getWidth();
			if (addrBits >= logOptions.length) {
				addrBits = logOptions.length - 1;
			}
			synchronized (logOptions) {
				Object[] ret = logOptions[addrBits];
				if (ret == null) {
					ret = new Object[1 << addrBits];
					logOptions[addrBits] = ret;
					for (int i = 0; i < ret.length; i++) {
						ret[i] = Integer.valueOf(i);
					}
				}
				return ret;
			}
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			if (option instanceof Integer) {
				MemState s = (MemState) state.getData();
				int addr = ((Integer) option).intValue();
				return Value.createKnown(BitWidth.create(s.getDataBits()), s
						.getContents().get(addr));
			} else {
				return Value.NIL;
			}
		}
	}

	public static int GetNrOfByteEnables(AttributeSet Attrs) {
		Object be = Attrs.getValue(RamAttributes.ATTR_ByteEnables);
		if (be == null || !be.equals(RamAttributes.BUS_WITH_BYTEENABLES))
			return 0;
		int NrOfBits = Attrs.getValue(Mem.DATA_ATTR).getWidth();
		return (NrOfBits + 7) / 8;
	}

	public static Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

	static final int AByEnBiDir = MEM_INPUTS + 2;

	static final int AByEnSep = MEM_INPUTS + 3;

	static final int SByEnBiDir = MEM_INPUTS + 3;

	static final int SByEnSep = MEM_INPUTS + 4;

	private static Object[][] logOptions = new Object[9][];

	public Ram() {
		super("RAM", Strings.getter("ramComponent"), 3);
		setIconName("ram.gif");
		setInstanceLogger(Logger.class);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		super.configureNewInstance(instance);
		instance.addAttributeListener();
	}

	@Override
	void configurePorts(Instance instance) {
		boolean separate = isSeparate(instance.getAttributeSet());
		int NrOfByteEnables = GetNrOfByteEnables(instance.getAttributeSet());
		int portCount = MEM_INPUTS + 3 + NrOfByteEnables;
		Port[] ps = new Port[portCount];
		ps[ADDR] = new Port(0, 10, Port.INPUT, ADDR_ATTR);
		ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
		ps[CLK] = new Port(0, 70 + NrOfByteEnables * 10, Port.INPUT, 1);
		ps[CLK].setToolTip(Strings.getter("ramClkTip"));
		ps[WE] = new Port(0, 50, Port.INPUT, 1);
		ps[WE].setToolTip(Strings.getter("ramWETip"));
		int ypos = getControlHeight(instance.getAttributeSet());
		if (instance.getAttributeValue(Mem.DATA_ATTR).getWidth() == 1)
			ypos += 10;
		if (separate) {
			ps[DIN] = new Port(0, ypos, Port.INPUT, DATA_ATTR);
			ps[DIN].setToolTip(Strings.getter("ramInTip"));
			ps[DATA] = new Port(SymbolWidth + 40, ypos, Port.OUTPUT, DATA_ATTR);
			ps[DATA].setToolTip(Strings.getter("memDataTip"));
		} else {
			ps[OE] = new Port(0, 60, Port.INPUT, 1);
			ps[OE].setToolTip(Strings.getter("ramOETip"));
			ps[DATA] = new Port(SymbolWidth + 50, ypos, Port.INOUT, DATA_ATTR);
			ps[DATA].setToolTip(Strings.getter("ramBusTip"));
		}
		for (int i = 0; i < NrOfByteEnables; i++) {
			ps[BE + i] = new Port(0, 70 + i * 10, Port.INPUT, 1);
			String Label = "ramByteEnableTip" + Integer.toString(NrOfByteEnables - i - 1);
			ps[BE + i].setToolTip(Strings.getter(Label));
		}
		instance.setPorts(ps);
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new RamAttributes();
	}

	private void DrawConnections(Graphics g, int xpos, int ypos,
			boolean singleBit, boolean separate, 
			boolean ByteEnabled, int bit) {
		Font font = g.getFont();
		GraphicsUtil.switchToWidth(g, 2);
		if (separate) {
			if (singleBit) {
				g.drawLine(xpos, ypos + 10, xpos + 20, ypos + 10);
				g.drawLine(xpos + 20 + SymbolWidth, ypos + 10, xpos + 40
						+ SymbolWidth, ypos + 10);
			} else {
				g.drawLine(xpos + 5, ypos + 5, xpos + 10, ypos + 10);
				g.drawLine(xpos + 10, ypos + 10, xpos + 20, ypos + 10);
				g.drawLine(xpos + 20 + SymbolWidth, ypos + 10, xpos + 30
						+ SymbolWidth, ypos + 10);
				g.drawLine(xpos + 30 + SymbolWidth, ypos + 10, xpos + 35
						+ SymbolWidth, ypos + 5);
				g.setFont(font.deriveFont(7.0f));
				GraphicsUtil
						.drawText(g, Integer.toString(bit), xpos + 17,
								ypos + 7, GraphicsUtil.H_RIGHT,
								GraphicsUtil.V_BASELINE);
				GraphicsUtil.drawText(g, Integer.toString(bit), xpos + 23
						+ SymbolWidth, ypos + 7, GraphicsUtil.H_LEFT,
						GraphicsUtil.V_BASELINE);
				g.setFont(font);
			}
			String ByteIndex = "";
			if (ByteEnabled) {
				int Index = bit / 8;
				ByteIndex = "," + Integer.toString(Index + 4);
			}
			String DLabel = "A,1,3" + ByteIndex + "D";
			String QLabel = "A";
			g.setFont(font.deriveFont(9.0f));
			GraphicsUtil.drawText(g, DLabel, xpos + 23, ypos + 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, QLabel, xpos + 17 + SymbolWidth,
					ypos + 10, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			g.setFont(font);
		} else {
			g.drawLine(xpos + 24 + SymbolWidth, ypos + 2, xpos + 28
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 24 + SymbolWidth, ypos + 8, xpos + 28
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 20 + SymbolWidth, ypos + 5, xpos + 30
					+ SymbolWidth, ypos + 5);
			g.drawLine(xpos + 22 + SymbolWidth, ypos + 15, xpos + 26
					+ SymbolWidth, ypos + 12);
			g.drawLine(xpos + 22 + SymbolWidth, ypos + 15, xpos + 26
					+ SymbolWidth, ypos + 18);
			g.drawLine(xpos + 20 + SymbolWidth, ypos + 15, xpos + 30
					+ SymbolWidth, ypos + 15);
			g.drawLine(xpos + 30 + SymbolWidth, ypos + 5, xpos + 30
					+ SymbolWidth, ypos + 15);
			g.drawLine(xpos + 30 + SymbolWidth, ypos + 10, xpos + 40
					+ SymbolWidth, ypos + 10);
			if (singleBit) {
				g.drawLine(xpos + 40 + SymbolWidth, ypos + 10, xpos + 50
						+ SymbolWidth, ypos + 10);
			} else {
				g.drawLine(xpos + 40 + SymbolWidth, ypos + 10, xpos + 45
						+ SymbolWidth, ypos + 5);
			}
			g.setFont(font.deriveFont(7.0f));
			GraphicsUtil.drawText(g, Integer.toString(bit), xpos + 33
					+ SymbolWidth, ypos + 7, GraphicsUtil.H_LEFT,
					GraphicsUtil.V_BASELINE);
			String ByteIndex = "";
			if (ByteEnabled) {
				int Index = bit / 8;
				ByteIndex = "," + Integer.toString(Index + 4);
			}
			String DLabel = "A,1,3" + ByteIndex + "D";
			String QLabel = "A,2" + "  ";
			g.setFont(font.deriveFont(9.0f));
			GraphicsUtil.drawText(g, DLabel, xpos + 17 + SymbolWidth,
					ypos + 13, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			GraphicsUtil.drawText(g, QLabel, xpos + 17 + SymbolWidth, ypos + 5,
					GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			g.setFont(font);
			GraphicsUtil.switchToWidth(g, 1);
			g.drawLine(xpos + 11 + SymbolWidth, ypos + 4, xpos + 19
					+ SymbolWidth, ypos + 4);
			g.drawLine(xpos + 11 + SymbolWidth, ypos + 4, xpos + 15
					+ SymbolWidth, ypos + 8);
			g.drawLine(xpos + 15 + SymbolWidth, ypos + 8, xpos + 19
					+ SymbolWidth, ypos + 4);
		}
		GraphicsUtil.switchToWidth(g, 1);
	}

	private void DrawControlBlock(InstancePainter painter, int xpos, int ypos) {
		boolean separate = isSeparate(painter.getAttributeSet());
		Object trigger = painter.getAttributeValue(StdAttr.TRIGGER);
		boolean asynch = trigger.equals(StdAttr.TRIG_HIGH)
				|| trigger.equals(StdAttr.TRIG_LOW);
		boolean inverted = trigger.equals(StdAttr.TRIG_FALLING)
				|| trigger.equals(StdAttr.TRIG_LOW);
		Object be = painter.getAttributeValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be
				.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		int NrOfByteEnables = GetNrOfByteEnables(painter.getAttributeSet());
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		AttributeSet attrs = painter.getAttributeSet();
		g.drawLine(xpos + 20, ypos, xpos + 20 + SymbolWidth, ypos);
		g.drawLine(xpos + 20, ypos, xpos + 20, ypos + getControlHeight(attrs)
				- 10);
		g.drawLine(xpos + 20 + SymbolWidth, ypos, xpos + 20 + SymbolWidth, ypos
				+ getControlHeight(attrs) - 10);
		g.drawLine(xpos + 20, ypos + getControlHeight(attrs) - 10, xpos + 30,
				ypos + getControlHeight(attrs) - 10);
		g.drawLine(xpos + 20 + SymbolWidth - 10, ypos + getControlHeight(attrs)
				- 10, xpos + 20 + SymbolWidth, ypos + getControlHeight(attrs)
				- 10);
		g.drawLine(xpos + 30, ypos + getControlHeight(attrs) - 10, xpos + 30,
				ypos + getControlHeight(attrs));
		g.drawLine(xpos + 20 + SymbolWidth - 10, ypos + getControlHeight(attrs)
				- 10, xpos + 20 + SymbolWidth - 10, ypos
				+ getControlHeight(attrs));
		GraphicsUtil.drawCenteredText(
				g,
				"RAM "
						+ GetSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR)
								.getWidth())
						+ " x "
						+ Integer.toString(painter.getAttributeValue(
								Mem.DATA_ATTR).getWidth()), xpos
						+ (SymbolWidth / 2) + 20, ypos + 5);
		g.drawLine(xpos, ypos + 50, xpos + 20, ypos + 50);
		GraphicsUtil.drawText(g, "M1 [Write Enable]", xpos + 33, ypos + 50,
				GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		painter.drawPort(WE);
		g.drawLine(xpos, ypos + 60, xpos + 20, ypos + 60);
		if (separate) {
			GraphicsUtil.drawText(g, "M2 [Output Enable]", xpos + 33, ypos + 60,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			painter.drawPort(OE);
		}
		int yoffset = 70 + NrOfByteEnables * 10;
		if (inverted) {
			g.drawLine(xpos, ypos + yoffset, xpos + 12, ypos + yoffset);
			g.drawOval(xpos + 12, ypos + yoffset - 4, 8, 8);
		} else {
			g.drawLine(xpos, ypos + yoffset, xpos + 20, ypos + yoffset);
		}
		if (asynch) {
			GraphicsUtil.drawText(g, "E3", xpos + 33, ypos + yoffset,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		} else {
			GraphicsUtil.drawText(g, "C3", xpos + 33, ypos + yoffset,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			painter.drawClockSymbol(xpos + 20, ypos + yoffset);
		}
		painter.drawPort(CLK);

		GraphicsUtil.switchToWidth(g, 2);
		for (int i = 0; i < NrOfByteEnables; i++) {
			g.drawLine(xpos, ypos + 70 + i * 10, xpos + 20, ypos + 70 + i
					* 10);
			painter.drawPort(BE + i);
			String Label = "M"
				+ Integer.toString((NrOfByteEnables - i) + 3)
				+ " [ByteEnable "
				+ Integer.toString((NrOfByteEnables - i) - 1) + "]";
			GraphicsUtil.drawText(g, Label, xpos + 33, ypos + 70 + i * 10,
					GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
		}

		GraphicsUtil.switchToWidth(g, 1);
		DrawAddress(painter, xpos, ypos + 10,
				painter.getAttributeValue(Mem.ADDR_ATTR).getWidth());
	}

	private void DrawDataBlock(InstancePainter painter, int xpos, int ypos, int bit, int NrOfBits) {
		int realypos = ypos + getControlHeight(painter.getAttributeSet()) + bit * 20;
		int realxpos = xpos + 20;
		boolean FirstBlock = bit == 0;
		boolean LastBlock = bit == (NrOfBits - 1);
		Graphics g = painter.getGraphics();
		boolean separate = isSeparate(painter.getAttributeSet());
		Object be = painter.getAttributeValue(RamAttributes.ATTR_ByteEnables);
		boolean byteEnables = be == null ? false : be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(realxpos, realypos, SymbolWidth, 20);
		DrawConnections(g, xpos, realypos, FirstBlock & LastBlock, separate, byteEnables, bit);
		if (FirstBlock) {
			painter.drawPort(DATA);
			if (separate)
				painter.drawPort(DIN);
			if (!LastBlock) {
				GraphicsUtil.switchToWidth(g, 5);
				if (separate) {
					g.drawLine(xpos, realypos, xpos + 5, realypos + 5);
					g.drawLine(xpos + 5, realypos + 5, xpos + 5, realypos + 20);
					g.drawLine(xpos + 40 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 5);
					g.drawLine(xpos + 35 + SymbolWidth, realypos + 5, xpos + 35
							+ SymbolWidth, realypos + 20);
				} else {
					g.drawLine(xpos + 50 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 5);
					g.drawLine(xpos + 45 + SymbolWidth, realypos + 5, xpos + 45
							+ SymbolWidth, realypos + 20);
				}
			}
		} else {
			GraphicsUtil.switchToWidth(g, 5);
			if (LastBlock) {
				if (separate) {
					g.drawLine(xpos + 5, realypos, xpos + 5, realypos + 5);
					g.drawLine(xpos + 35 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 5);
				} else {
					g.drawLine(xpos + 45 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 5);
				}
			} else {
				if (separate) {
					g.drawLine(xpos + 5, realypos, xpos + 5, realypos + 20);
					g.drawLine(xpos + 35 + SymbolWidth, realypos, xpos + 35
							+ SymbolWidth, realypos + 20);
				} else {
					g.drawLine(xpos + 45 + SymbolWidth, realypos, xpos + 45
							+ SymbolWidth, realypos + 20);
				}
			}
		}
		GraphicsUtil.switchToWidth(g, 1);
	}

	public int getControlHeight(AttributeSet attrs) {
		int NrByteEnables = GetNrOfByteEnables(attrs);
		return 90 + NrByteEnables * 10;
	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		String Name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
		if (Name.length() == 0)
			return "RAM";
		else
			return "RAMCONTENTS_" + Name;
	}

	@Override
	HexFrame getHexFrame(Project proj, Instance instance, CircuitState circState) {
		return RamAttributes.getHexFrame(
				instance.getAttributeValue(CONTENTS_ATTR), proj);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		boolean separate = isSeparate(attrs);
		int xoffset = (separate) ? 40 : 50;
		if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
			return Bounds.create(0, 0, SymbolWidth + xoffset, 140);
		} else {
			int len = attrs.getValue(Mem.DATA_ATTR).getWidth();
			return Bounds.create(0, 0, SymbolWidth + xoffset,
					getControlHeight(attrs) + 20 * len);
		}
	}

	@Override
	MemState getState(Instance instance, CircuitState state) {
		RamState ret = (RamState) instance.getData(state);
		if (ret == null) {
			MemContents contents = instance
					.getAttributeValue(Ram.CONTENTS_ATTR);
			ret = new RamState(instance, contents, new MemListener(instance));
			instance.setData(state, ret);
		} else {
			ret.setRam(instance);
		}
		return ret;
	}

	@Override
	MemState getState(InstanceState state) {
		RamState ret = (RamState) state.getData();
		if (ret == null) {
			MemContents contents = state.getInstance().getAttributeValue(
					Ram.CONTENTS_ATTR);
			Instance instance = state.getInstance();
			ret = new RamState(instance, contents, new MemListener(instance));
			state.setData(ret);
		} else {
			ret.setRam(state.getInstance());
		}
		return ret;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs, char Vendor) {
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new RamHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs, Vendor);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		super.instanceAttributeChanged(instance, attr);
		Attribute<AttributeOption> be = RamAttributes.ATTR_ByteEnables;
		if ((attr == Mem.DATA_ATTR)) {
			boolean narrow = instance.getAttributeValue(Mem.DATA_ATTR).getWidth() < 9;
			if (narrow) {
				if (!instance.getAttributeValue(be).equals(RamAttributes.BUS_WITHOUT_BYTEENABLES)) {
					instance.getAttributeSet().setValue(be, RamAttributes.BUS_WITHOUT_BYTEENABLES);
					super.instanceAttributeChanged(instance, be);
				}
				instance.setAttributeReadOnly(be, true);
				super.instanceAttributeChanged(instance, be);
				instance.setAttributeReadOnly(be, true);
				super.instanceAttributeChanged(instance, be);
			} else {
				if (instance.getAttributeSet().isReadOnly(be)) {
					instance.setAttributeReadOnly(be, false);
					super.instanceAttributeChanged(instance,be);
				}
			}
			instance.recomputeBounds();
			configurePorts(instance);
		} else if ((attr == RamAttributes.ATTR_DBUS) || (attr == RamAttributes.ATTR_ByteEnables)) {
			instance.recomputeBounds();
			configurePorts(instance);
		} else if ((attr == StdAttr.APPEARANCE)) {
			instance.recomputeBounds();
			configurePorts(instance);
		}
	}

	public void DrawRamClassic(InstancePainter painter) {
		DrawMemClassic(painter);
		painter.drawPort(WE, Strings.get("ramWELabel"), Direction.EAST);
		painter.drawClock(CLK, Direction.EAST);

		boolean separate = isSeparate(painter.getAttributeSet());
		if (separate)
			painter.drawPort(DIN, Strings.get("ramDataLabel"), Direction.EAST);
		else
			painter.drawPort(OE, Strings.get("ramOELabel"), Direction.EAST);

		int NrOfByteEnables = GetNrOfByteEnables(painter.getAttributeSet());
		for (int i = 0; i < NrOfByteEnables; i++) {
			painter.drawPort(BE + i, Strings.get("ramWELabel")+(NrOfByteEnables-i-1), Direction.EAST);
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
			DrawRamClassic(painter);
		} else {
			Graphics g = painter.getGraphics();
			Bounds bds = painter.getBounds();
			int NrOfBits = painter.getAttributeValue(Mem.DATA_ATTR).getWidth();

			painter.drawLabel();

			int xpos = bds.getX();
			int ypos = bds.getY();

			DrawControlBlock(painter, xpos, ypos);
			for (int i = 0; i < NrOfBits; i++) {
				DrawDataBlock(painter, xpos, ypos, i, NrOfBits);
			}
			/* Draw contents */
			if (painter.getShowState()) {
				RamState state = (RamState) getState(painter);
				state.paint(painter.getGraphics(), bds.getX(), bds.getY(),
						60, getControlHeight(painter.getAttributeSet()) + 5,
						Mem.SymbolWidth - 75, 20 * NrOfBits - 10, false);
			}
		}
	}

	@Override
	public void propagate(InstanceState state) {
		AttributeSet attrs = state.getAttributeSet();
		RamState myState = (RamState) getState(state);
		boolean separate = isSeparate(attrs);

		// get address
		Value addrValue = state.getPortValue(ADDR);
		int addr = addrValue.toIntValue();
		boolean goodAddr = (addrValue.isFullyDefined() && addr >= 0);
		if (goodAddr && addr != myState.getCurrent()) {
			myState.setCurrent(addr);
			myState.scrollToShow(addr);
		}

		// perform writes
		Object trigger = state.getAttributeValue(StdAttr.TRIGGER);
		boolean triggered = myState.setClock(state.getPortValue(CLK), trigger);
		boolean writeEnabled = triggered && (state.getPortValue(WE) == Value.TRUE);
		if (writeEnabled && goodAddr) {
			int NrOfByteEnables = GetNrOfByteEnables(attrs);

			int dataValue = state.getPortValue(separate ? DIN : DATA).toIntValue();
			int memValue = myState.getContents().get(addr);
			if (NrOfByteEnables > 0) {
				int mask = 0xFF << (NrOfByteEnables - 1) * 8;
				for (int i = 0; i < NrOfByteEnables; i++) {
					Value bitvalue = state.getPortValue(BE + i);
					boolean disabled = bitvalue == null ? false : bitvalue.equals(Value.FALSE);
					if (disabled) {
						dataValue &= ~mask;
						dataValue |= (memValue & mask);
					}
					mask >>= 8;
				}
			}
			myState.getContents().set(addr, dataValue);
		}

		// perform reads
		BitWidth width = state.getAttributeValue(DATA_ATTR);
		boolean outputEnabled = separate || (state.getPortValue(OE) != Value.FALSE);
		if (outputEnabled && goodAddr) {
			int val = myState.getContents().get(addr);
			state.setPort(DATA, Value.createKnown(width, val), DELAY);
		} else {
			state.setPort(DATA, Value.createUnknown(width), DELAY);
		}
	}

	private boolean isSeparate(AttributeSet attrs) {
		Object bus = attrs.getValue(RamAttributes.ATTR_DBUS);
		return bus == null || bus.equals(RamAttributes.BUS_SEP);
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}
}
