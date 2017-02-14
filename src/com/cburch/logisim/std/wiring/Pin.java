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

package com.cburch.logisim.std.wiring;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

public class Pin extends InstanceFactory {

	@SuppressWarnings("serial")
	private static class EditDecimal extends JDialog implements KeyListener {

		private JFormattedTextField text;
		private int bitWidth;
		PinState pinState;
		InstanceState state;
		RadixOption radix;
		boolean tristate;
		private static final Color VALID_COLOR = new Color(0xff, 0xf0, 0x99);
		private static final Color INVALID_COLOR = new Color(0xff, 0x66, 0x66);

		public EditDecimal(InstanceState state) {
			super();
			this.state = state;
			radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
			pinState = getState(state);
			Value value = pinState.intendedValue;
			bitWidth = value.getWidth();
			PinAttributes attrs = (PinAttributes) state.getAttributeSet();
			tristate = (attrs.threeState && attrs.pull == PULL_NONE);

			setTitle("Enter Decimal");
			GridBagConstraints gbc = new GridBagConstraints();
			final JButton ok = new JButton("OK");
			final JButton cancel = new JButton("Cancel");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					accept();
				}
			});
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditDecimal.this.setVisible(false);
				}
			});
			addWindowFocusListener(new WindowFocusListener() {            
				public void windowLostFocus(WindowEvent e) {
					EditDecimal.this.setVisible(false);
				}
				public void windowGainedFocus(WindowEvent e) { }
			});
			setLayout(new GridBagLayout());

			text = new JFormattedTextField();
			text.setFont(DEFAULT_FONT);
			text.setColumns(11);
			text.setText(value.toDecimalString(radix == RadixOption.RADIX_10_SIGNED));
			text.selectAll();

			text.getDocument().addDocumentListener(new DocumentListener() {
				public void insertUpdate(DocumentEvent e) {
					String s = text.getText();
					if (isEditValid(s)) {
						text.setBackground(VALID_COLOR);
						ok.setEnabled(true);
					} else {
						text.setBackground(INVALID_COLOR);
						ok.setEnabled(false);
					}

				}
				public void removeUpdate(DocumentEvent e) {
					insertUpdate(e);
				}
				public void changedUpdate(DocumentEvent e) { }
			});

			gbc.gridx = 0;
			gbc.gridy = 1;
			add(cancel, gbc);
			gbc.gridx = 1;
			gbc.gridy = 1;
			add(ok, gbc);
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.anchor = GridBagConstraints.BASELINE;
			gbc.insets = new Insets(8,4,8,4);
			text.addKeyListener(this);
			text.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			text.setBackground(VALID_COLOR);
			add(text, gbc);

			pack();
		}

		public void accept() {
			String s = text.getText();
			if (isEditValid(s)) {
				Value newVal;
				if (s.equals("x") || s.equals("???")) {
					newVal = Value.createUnknown(BitWidth.create(bitWidth));
				} else {
					try {
						int n = (int) Long.parseLong(s);
						newVal = Value.createKnown(BitWidth.create(bitWidth), n);
					} catch (NumberFormatException exception) {
						return;
					}
				}
				setVisible(false);
				pinState.intendedValue = newVal;
				state.fireInvalidated();
			}
		}

		boolean isEditValid(String s) {
			if (s == null)
				return false;
			s = s.trim();
			if (s.equals(""))
				return false;
			if (tristate && (s.equals("x") || s.equals("???")))
				return true;
			try {
				long n = Long.parseLong(s);
				if (radix == RadixOption.RADIX_10_SIGNED)
					return (n >= -(1L << (bitWidth-1)) && n < (1L << bitWidth-1));
				else
					return (n >= 0 && n < (1L << bitWidth));
			} catch (NumberFormatException e) {
				return false;
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				accept();
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				setVisible(false);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) { }

		@Override
		public void keyTyped(KeyEvent e) { }
	}

	public static class PinLogger extends InstanceLogger {

		@Override
		public String getLogName(InstanceState state, Object option) {
			PinAttributes attrs = (PinAttributes) state.getAttributeSet();
			String ret = attrs.label;
			if (ret == null || ret.equals("")) {
				String type = attrs.type == EndData.INPUT_ONLY ? Strings
						.get("pinInputName") : Strings.get("pinOutputName");
				return type + state.getInstance().getLocation();
			} else {
				return ret;
			}
		}

		@Override
		public Value getLogValue(InstanceState state, Object option) {
			PinState s = getState(state);
			return s.intendedValue;
		}
	}

	public static class PinPoker extends InstancePoker {

		int bitPressed = -1;
		int bitCaret = -1;

		private int getBit(InstanceState state, MouseEvent e) {
			RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
			BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
			int r;
			if (radix == RadixOption.RADIX_16) {
				r = 4;
			} else if (radix == RadixOption.RADIX_8) {
				r = 3;
			} else if (radix == RadixOption.RADIX_2) {
				r = 1;
			} else {
				return -1;
			}
			if (width.getWidth() <= r) {
				return 0;
			} else {
				Bounds bds = state.getInstance().getBounds(); // intentionally with no graphics object - we don't want label included
				int i = (bds.getX() + bds.getWidth() - e.getX() - 4) / (r == 1 ? 10 : 7);
				int j = (bds.getY() + bds.getHeight() - e.getY() - 2) / 14;
				int bit;
				if (r == 1) {
					bit = 8 * j + i;
				} else {
					bit = i * r;
				}
				if (bit < 0 || bit >= width.getWidth()) {
					return -1;
				} else {
					return bit;
				}
			}
		}

		private boolean handleBitPress(InstanceState state, int bit, RadixOption radix, java.awt.Component src, char ch) {
			PinAttributes attrs = (PinAttributes) state.getAttributeSet();
			if (!attrs.isInput()) {
				return false;
			}

			if (src instanceof Canvas && !state.isCircuitRoot()) {
				Canvas canvas = (Canvas)src;
				CircuitState circState = canvas.getCircuitState();
				java.awt.Component frame = SwingUtilities.getRoot(canvas);
				int choice = JOptionPane.showConfirmDialog(frame,
						Strings.get("pinFrozenQuestion"),
						Strings.get("pinFrozenTitle"),
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
				if (choice == JOptionPane.OK_OPTION) {
					circState = circState.cloneState();
					canvas.getProject().setCircuitState(circState);
					state = circState.getInstanceState(state.getInstance());
				} else {
					return false;
				}
			}

			BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
			PinState pinState = getState(state);
			int r = (radix == RadixOption.RADIX_16 ? 4 :
					(radix == RadixOption.RADIX_8 ? 3 : 1));
			if (bit+r > width.getWidth())
				r = width.getWidth() - bit;
			Value val[] = pinState.intendedValue.getAll();
			boolean tristate = (attrs.threeState && attrs.pull == PULL_NONE);
			if (ch == 0) {
				boolean zeros = true, ones = true, defined = true;
				for (int b = bit; b < bit + r; b++) {
					if (val[b] == Value.FALSE)
						ones = false;
					else if (val[b] == Value.TRUE)
						zeros = false;
					else
						defined = false;
				}
				if (!defined || (ones && !tristate)) {
					for (int b = bit; b < bit + r; b++)
						val[b] = Value.FALSE;
				} else if (ones && tristate) {
					for (int b = bit; b < bit + r; b++)
						val[b] = Value.UNKNOWN;
				} else {
					int carry = 1;
					Value v[] = new Value[]{ Value.FALSE, Value.TRUE };
					for (int b = bit; b < bit + r; b++) {
						int s = (val[b] == Value.TRUE ? 1 : 0) + carry;
						val[b] = v[(s % 2)];
						carry = s / 2;
					}
				}
			} else if (tristate && ch == 'x') {
				for (int b = bit; b < bit + r; b++)
					val[b] = Value.UNKNOWN;
			} else {
				int d;
				if ('0' <= ch && ch <= '9')
					d = ch - '0';
				else if ('a' <= ch && ch <= 'f')
					d = 0xa + (ch - 'a');
				else if ('A' <= ch && ch <= 'F')
					d = 0xA + (ch - 'A');
				else
					return false;
				if (d >= 1 << r)
					return false;
				Value v[] = new Value[]{ Value.FALSE, Value.TRUE };
				for (int i = 0; i < r; i++)
					val[bit+i] = (((d&(1<<i)) != 0) ? Value.TRUE : Value.FALSE);
			}
			for (int b = bit; b < bit + r; b++)
				pinState.intendedValue = pinState.intendedValue.set(b, val[b]);
			state.fireInvalidated();
			return true;
		}

		@Override
		public void mousePressed(InstanceState state, MouseEvent e) {
			bitPressed = getBit(state, e);
		}

		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
			RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
			if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED) {
				EditDecimal dialog = new EditDecimal(state);
				dialog.setLocation(e.getXOnScreen()-60, e.getYOnScreen()-40);
				dialog.setVisible(true);
			} else {
				int bit = getBit(state, e);
				if (bit == bitPressed && bit >= 0) {
					bitCaret = bit;
					handleBitPress(state, bit, radix, e.getComponent(), (char)0);
				}
				if (bitCaret < 0) {
					BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
					int r = (radix == RadixOption.RADIX_16 ? 4 :
							(radix == RadixOption.RADIX_8 ? 3 : 1));
					bitCaret = ((width.getWidth()-1)/r) * r;
				}
				bitPressed = -1;
			}
		}

		@Override
		public void keyTyped(InstanceState state, KeyEvent e) {
			char ch = e.getKeyChar();
			RadixOption radix = state.getAttributeValue(RadixOption.ATTRIBUTE);
			if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED)
				return;
			int r = (radix == RadixOption.RADIX_16 ? 4 :
					(radix == RadixOption.RADIX_8 ? 3 : 1));
			BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
			if (bitCaret < 0)
				bitCaret = ((width.getWidth()-1)/r) * r;
			if (handleBitPress(state, bitCaret, radix, e.getComponent(), ch)) {
				bitCaret -= r;
				if (bitCaret < 0)
					bitCaret = ((width.getWidth()-1)/r) * r;
			}
		}

		@Override
		public void paint(InstancePainter painter) {
			if (bitCaret < 0)
				return;
			BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
			RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
			if (radix == RadixOption.RADIX_10_SIGNED || radix == RadixOption.RADIX_10_UNSIGNED)
				return;
			int r = (radix == RadixOption.RADIX_16 ? 4 :
					(radix == RadixOption.RADIX_8 ? 3 : 1));
			if (width.getWidth() <= r)
				return;
			Bounds bds = painter.getBounds();
			Graphics g = painter.getGraphics();
			GraphicsUtil.switchToWidth(g, 2);
			g.setColor(Color.RED);
			int y = bds.getY() + bds.getHeight();
			int x = bds.getX() + bds.getWidth();
			if (radix == RadixOption.RADIX_2) {
				x -= 2 + 10 * (bitCaret % 8);
				y -= 2 + 14 * (bitCaret / 8);
				g.drawLine(x - 6, y, x, y);
			} else {
				x -= 4 + 7 * (bitCaret / r);
				y -= 4;
				g.drawLine(x - 6, y, x, y);
			}
			g.setColor(Color.BLACK);
		}
	}

	private static class PinState implements InstanceData, Cloneable {

		Value intendedValue;
		Value foundValue;

		public PinState(Value sending, Value receiving) {
			this.intendedValue = sending;
			this.foundValue = receiving;
		}

		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	private static PinState getState(InstanceState state) {
		PinAttributes attrs = (PinAttributes) state.getAttributeSet();
		BitWidth width = attrs.width;
		PinState ret = (PinState) state.getData();
		if (ret == null) {
			Value val = attrs.threeState ? Value.UNKNOWN : Value.FALSE;
			if (width.getWidth() > 1) {
				Value[] arr = new Value[width.getWidth()];
				java.util.Arrays.fill(arr, val);
				val = Value.create(arr);
			}
			ret = new PinState(val, val);
			state.setData(ret);
		}
		if (ret.intendedValue.getWidth() != width.getWidth()) {
			ret.intendedValue = ret.intendedValue.extendWidth(width.getWidth(),
					attrs.threeState ? Value.UNKNOWN : Value.FALSE);
		}
		if (ret.foundValue.getWidth() != width.getWidth()) {
			ret.foundValue = ret.foundValue.extendWidth(width.getWidth(),
					Value.UNKNOWN);
		}
		return ret;
	}

	private static Value pull2(Value mod, BitWidth expectedWidth, Value pullTo) {
		if (mod.getWidth() == expectedWidth.getWidth()) {
			Value[] vs = mod.getAll();
			for (int i = 0; i < vs.length; i++) {
				if (vs[i] == Value.UNKNOWN) {
					vs[i] = pullTo;
				}
			}
			return Value.create(vs);
		} else {
			return Value.createKnown(expectedWidth, 0);
		}
	}

	public static final Attribute<Boolean> ATTR_TRISTATE = Attributes
			.forBoolean("tristate", Strings.getter("pinThreeStateAttr"));
	public static final Attribute<Boolean> ATTR_TYPE = Attributes.forBoolean(
			"output", Strings.getter("pinOutputAttr"));
	public static final AttributeOption PULL_NONE = new AttributeOption("none",
			Strings.getter("pinPullNoneOption"));
	public static final AttributeOption PULL_UP = new AttributeOption("up",
			Strings.getter("pinPullUpOption"));
	public static final AttributeOption PULL_DOWN = new AttributeOption("down",
			Strings.getter("pinPullDownOption"));

	public static final Attribute<AttributeOption> ATTR_PULL = Attributes
			.forOption("pull", Strings.getter("pinPullAttr"),
					new AttributeOption[] { PULL_NONE, PULL_UP, PULL_DOWN });

	public static final Pin FACTORY = new Pin();

	private static final Icon ICON_IN = Icons.getIcon("pinInput.gif");

	private static final Icon ICON_OUT = Icons.getIcon("pinOutput.gif");

	private static final Font ICON_WIDTH_FONT = new Font("SansSerif", Font.BOLD, 9);

	private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

	private static final Color ICON_WIDTH_COLOR = Value.WIDTH_ERROR_COLOR
			.darker();

	public Pin() {
		super("Pin", Strings.getter("pinComponent"));
		setFacingAttribute(StdAttr.FACING);
		setKeyConfigurator(JoinedConfigurator.create(
					new BitWidthConfigurator(StdAttr.WIDTH),
					new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
		setInstanceLogger(PinLogger.class);
		setInstancePoker(PinPoker.class);
	}

	//
	// methods for instances
	//
	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		configurePorts(instance);
		instance.computeLabelTextField(Instance.AVOID_LEFT);
	}

	private void configurePorts(Instance instance) {
		PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
		String endType = attrs.isOutput() ? Port.INPUT : Port.OUTPUT;
		Port port = new Port(0, 0, endType, StdAttr.WIDTH);
		if (attrs.isOutput()) {
			port.setToolTip(Strings.getter("pinOutputToolTip"));
		} else {
			port.setToolTip(Strings.getter("pinInputToolTip"));
		}
		instance.setPorts(new Port[] { port });
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new PinAttributes();
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Direction facing = attrs.getValue(StdAttr.FACING);
		BitWidth width = attrs.getValue(StdAttr.WIDTH);
		return Probe.getOffsetBounds(facing, width,
				attrs.getValue(RadixOption.ATTRIBUTE) /* RadixOption.RADIX_2 */);
	}

	public int getType(Instance instance) {
		PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
		return attrs.type;
	}

	//
	// state information methods
	//
	public Value getValue(InstanceState state) {
		return getState(state).intendedValue;
	}

	//
	// basic information methods
	//
	public BitWidth getWidth(Instance instance) {
		PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
		return attrs.width;
	}

	@Override
	public boolean HasThreeStateDrivers(AttributeSet attrs) {
		/*
		 * We ignore for the moment the three-state property of the pin, as it
		 * is not an active component, just wiring
		 */
		// PinAttributes myattrs = (PinAttributes) attrs;
		// return myattrs.getValue(Pin.ATTR_TRISTATE);
		return false;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs, char Vendor) {
		return true;
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_TYPE) {
			configurePorts(instance);
		} else if (attr == StdAttr.WIDTH || attr == StdAttr.FACING
				|| attr == StdAttr.LABEL_LOC || attr == RadixOption.ATTRIBUTE) {
			instance.recomputeBounds();
			instance.computeLabelTextField(Instance.AVOID_LEFT);
		} else if (attr == Pin.ATTR_TRISTATE || attr == Pin.ATTR_PULL) {
			instance.fireInvalidated();
		}
	}

	public boolean isInputPin(Instance instance) {
		PinAttributes attrs = (PinAttributes) instance.getAttributeSet();
		return attrs.type != EndData.OUTPUT_ONLY;
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
		Location loc = painter.getLocation();
		Bounds bds = painter.getOffsetBounds();
		int x = loc.getX();
		int y = loc.getY();
		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		boolean output = attrs.isOutput();
		if (output) {
			BitWidth width = attrs.getValue(StdAttr.WIDTH);
			if (width == BitWidth.ONE) {
				g.drawOval(x + bds.getX() + 1, y + bds.getY() + 1,
						bds.getWidth() - 1, bds.getHeight() - 1);
			} else {
				g.drawRoundRect(x + bds.getX() + 1, y + bds.getY() + 1,
						bds.getWidth() - 1, bds.getHeight() - 1, 6, 6);
			}
		} else {
			g.drawRect(x + bds.getX() + 1, y + bds.getY() + 1,
					bds.getWidth() - 1, bds.getHeight() - 1);
		}
	}

	//
	// graphics methods
	//
	@Override
	public void paintIcon(InstancePainter painter) {
		paintIconBase(painter);
		BitWidth w = painter.getAttributeValue(StdAttr.WIDTH);
		if (!w.equals(BitWidth.ONE)) {
			Graphics g = painter.getGraphics();
			g.setColor(ICON_WIDTH_COLOR);
			g.setFont(ICON_WIDTH_FONT);
			GraphicsUtil.drawCenteredText(g, "" + w.getWidth(), 10, 9);
			g.setColor(Color.BLACK);
		}
	}

	private void paintIconBase(InstancePainter painter) {
		PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
		Direction dir = attrs.facing;
		boolean output = attrs.isOutput();
		Graphics g = painter.getGraphics();
		if (output) {
			if (ICON_OUT != null) {
				Icons.paintRotated(g, 2, 2, dir, ICON_OUT,
						painter.getDestination());
				return;
			}
		} else {
			if (ICON_IN != null) {
				Icons.paintRotated(g, 2, 2, dir, ICON_IN,
						painter.getDestination());
				return;
			}
		}
		int pinx = 16;
		int piny = 9;
		if (dir == Direction.EAST) { // keep defaults
		} else if (dir == Direction.WEST) {
			pinx = 4;
		} else if (dir == Direction.NORTH) {
			pinx = 9;
			piny = 4;
		} else if (dir == Direction.SOUTH) {
			pinx = 9;
			piny = 16;
		}

		g.setColor(Color.black);
		if (output) {
			g.drawOval(4, 4, 13, 13);
		} else {
			g.drawRect(4, 4, 13, 13);
		}
		g.setColor(Value.TRUE.getColor());
		g.fillOval(7, 7, 8, 8);
		g.fillOval(pinx, piny, 3, 3);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		PinAttributes attrs = (PinAttributes) painter.getAttributeSet();
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getInstance().getBounds(); // intentionally with no
		// graphics object - we
		// don't want label
		// included
		int x = bds.getX();
		int y = bds.getY();
		GraphicsUtil.switchToWidth(g, 2);
		g.setColor(Color.black);
		if (attrs.type == EndData.OUTPUT_ONLY) {
			if (attrs.width.getWidth() == 1) {
				g.drawOval(x + 1, y + 1, bds.getWidth() - 1,
						bds.getHeight() - 1);
			} else {
				g.drawRoundRect(x + 1, y + 1, bds.getWidth() - 1,
						bds.getHeight() - 1, 12, 12);
			}
		} else {
			g.drawRect(x + 1, y + 1, bds.getWidth() - 1, bds.getHeight() - 1);
		}

		painter.drawLabel();

		if (!painter.getShowState()) {
			g.setColor(Color.BLACK);
			GraphicsUtil.drawCenteredText(g, "x" + attrs.width.getWidth(),
					bds.getX() + bds.getWidth() / 2,
					bds.getY() + bds.getHeight() / 2);
		} else {
			PinState state = getState(painter);
			if (attrs.width.getWidth() <= 1) {
				Value found = state.foundValue;
				g.setColor(found.getColor());
				g.fillOval(x + 4, y + 4, 13, 13);

				if (attrs.width.getWidth() == 1) {
					g.setColor(Color.WHITE);
					g.setFont(DEFAULT_FONT);
					GraphicsUtil.drawCenteredText(g,
							state.intendedValue.toDisplayString(), x + 10, y + 9);
				}
			} else {
				Probe.paintValue(painter, state.intendedValue);
			}
		}

		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		PinAttributes attrs = (PinAttributes) state.getAttributeSet();

		PinState q = getState(state);
		if (attrs.type == EndData.OUTPUT_ONLY) {
			Value found = state.getPortValue(0);
			q.intendedValue = found;
			q.foundValue = found;
			state.setPort(0, Value.createUnknown(attrs.width), 1);
		} else {
			Value found = state.getPortValue(0);
			Value toSend = q.intendedValue;

			Object pull = attrs.pull;
			Value pullTo = null;
			if (pull == PULL_DOWN) {
				pullTo = Value.FALSE;
			} else if (pull == PULL_UP) {
				pullTo = Value.TRUE;
			} else if (!attrs.threeState && !state.isCircuitRoot()) {
				pullTo = Value.FALSE;
			}
			if (pullTo != null) {
				toSend = pull2(toSend, attrs.width, pullTo);
				if (state.isCircuitRoot()) {
					q.intendedValue = toSend;
				}
			}

			q.foundValue = found;
			if (!toSend.equals(found)) { // ignore if no change
				state.setPort(0, toSend, 1);
			}
		}
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	public void setValue(InstanceState state, Value value) {
		PinAttributes attrs = (PinAttributes) state.getAttributeSet();
		Object pull = attrs.pull;

		PinState myState = getState(state);
		if (value == Value.NIL) {
			myState.intendedValue = Value.createUnknown(attrs.width);
		} else {
			Value sendValue;
			if (pull == PULL_NONE || pull == null || value.isFullyDefined()) {
				sendValue = value;
			} else {
				Value[] bits = value.getAll();
				if (pull == PULL_UP) {
					for (int i = 0; i < bits.length; i++) {
						if (bits[i] != Value.FALSE)
							bits[i] = Value.TRUE;
					}
				} else if (pull == PULL_DOWN) {
					for (int i = 0; i < bits.length; i++) {
						if (bits[i] != Value.TRUE)
							bits[i] = Value.FALSE;
					}
				}
				sendValue = Value.create(bits);
			}
			myState.intendedValue = sendValue;
		}
	}

}
