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

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
//import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class PortIO extends InstanceFactory {

	public static final ArrayList<String> GetLabels(int size) {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			LabelNames.add("pin_" + Integer.toString(i + 1));
		}
		return LabelNames;
	}

	public static final int MAX_IO = 128;
	public static final int MIN_IO = 2;
	public static final Attribute<Integer> ATTR_SIZE = Attributes
			.forIntegerRange("number", Strings.getter("pioNumber"), MIN_IO,
					MAX_IO);
	// public static final Attribute<Boolean> ATTR_BUS =
	// Attributes.forBoolean("showBus", Strings.getter("pioShowBus"));
	// public static final String BUSES = Strings.getter("pioBuses").toString();
	// public static final String PINS = Strings.getter("pioPins").toString();
	// public static final String[] OPTIONS = { BUSES, PINS };

	// public static final Attribute<String> ATTR_BUS = Attributes.forOption(
	//		"showBus", Strings.getter("pioShowBus"), OPTIONS);


	public static final String INPUT = "input";
	public static final String OUTPUT = "output";
	public static final String INOUT = "inout";

	public static final String[] DIRECTIONS = { INPUT, OUTPUT, INOUT };
	public static final Attribute<String> ATTR_DIR = Attributes.forOption(
			"direction", Strings.getter("pioDirection"), DIRECTIONS);

	private MappableResourcesContainer mapInfo;

	public PortIO() {
		super("PortIO", Strings.getter("pioComponent"));
		int portSize = 8;
		setAttributes(new Attribute[] { StdAttr.LABEL, Io.ATTR_LABEL_LOC,
				StdAttr.LABEL_FONT, Io.ATTR_LABEL_COLOR, ATTR_SIZE, ATTR_DIR},
				new Object[] { "", Direction.EAST, StdAttr.DEFAULT_LABEL_FONT,
						Color.BLACK, portSize, INOUT });
		setFacingAttribute(StdAttr.FACING);
		setIconName("pio.gif");
		// setInstancePoker(Poker.class);
		MyIOInformation = new IOComponentInformationContainer(0, 0, portSize,
				null, null, GetLabels(portSize),
				FPGAIOInformationContainer.IOComponentTypes.PortIO);
		// MyIOInformation.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Button);
		MyIOInformation
				.AddAlternateMapType(FPGAIOInformationContainer.IOComponentTypes.Pin);
	}

	private void computeTextField(Instance instance) {
		Direction facing = Direction.NORTH;
		Object labelLoc = instance.getAttributeValue(Io.ATTR_LABEL_LOC);

		Bounds bds = instance.getBounds();
		int x = bds.getX() + bds.getWidth() / 2;
		int y = bds.getY() + bds.getHeight() / 2;
		int halign = GraphicsUtil.H_CENTER;
		int valign = GraphicsUtil.V_CENTER;
		if (labelLoc == Direction.NORTH) {
			y = bds.getY() - 2;
			valign = GraphicsUtil.V_BOTTOM;
		} else if (labelLoc == Direction.SOUTH) {
			y = bds.getY() + bds.getHeight() + 2;
			valign = GraphicsUtil.V_TOP;
		} else if (labelLoc == Direction.EAST) {
			x = bds.getX() + bds.getWidth() + 2;
			halign = GraphicsUtil.H_LEFT;
		} else if (labelLoc == Direction.WEST) {
			x = bds.getX() - 2;
			halign = GraphicsUtil.H_RIGHT;
		}
		if (labelLoc == facing) {
			if (labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
				x += 2;
				halign = GraphicsUtil.H_LEFT;
			} else {
				y -= 2;
				valign = GraphicsUtil.V_BOTTOM;
			}
		}

		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign,
				valign);
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		configurePorts(instance);
		computeTextField(instance);
		MyIOInformation.setNrOfInOutports(
				instance.getAttributeValue(ATTR_SIZE),
				GetLabels(instance.getAttributeValue(ATTR_SIZE)));
	}

	private void configurePorts(Instance instance) {
		if (false /* instance.getAttributeValue(ATTR_BUS).equals(PINS) */) {
			// TODO YSY PINS
			// Port[] ps = new Port[instance.getAttributeValue(ATTR_SIZE)];
			// for (int i = 0; i < instance.getAttributeValue(ATTR_SIZE); i++) {
			//
			// ps[i] = new Port((i + 1) * 10, 0, Port.OUTPUT, 1);
			// ps[i].setToolTip(StringUtil.constantGetter(String.valueOf(i+1)));
			// }
			// instance.setPorts(ps);
		} else {
			int nbPorts = instance.getAttributeValue(ATTR_SIZE);
			String dir = instance.getAttributeValue(ATTR_DIR);
                        int k = (dir == INOUT ? 3 : 1);
			Port[] ps = new Port[k*(((nbPorts - 1) / 32) + 1)];
			int i = 0, p = 0;
                        int x = (dir == INOUT ? 0 : 10);
                        while (nbPorts > 0) {
                            int n = (nbPorts > 32 ? 32 : nbPorts);
                            String range = "[" + i + " to " + (i + n - 1) +"]";
                            if (dir == INOUT) {
                                ps[p] = new Port(x, 10, Port.INPUT, n);
                                ps[p].setToolTip(StringUtil.constantGetter("OutEnable"+range));
                                p++;
                                x += 10;
                            }
                            if (dir == OUTPUT || dir == INOUT) {
                                ps[p] = new Port(x, 0, Port.INPUT, n);
                                ps[p].setToolTip(StringUtil.constantGetter("Out"+range));
                                p++;
                                x += 10;
                            }
                            if (dir == INPUT || dir == INOUT) {
                                ps[p] = new Port(x, 0, Port.OUTPUT, n);
                                ps[p].setToolTip(StringUtil.constantGetter("In"+range));
                                p++;
                                x += 10;
                            }
                            i += 32;
                            nbPorts -= n;
                        }
			instance.setPorts(ps);
		}

	}

	@Override
	public String getHDLName(AttributeSet attrs) {
            String label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
            if (label == null || label.length() == 0) {
                return "PORTIO";
            }
            StringBuffer CompleteName = new StringBuffer("PORTIO_");
            CompleteName.append(label);
            return CompleteName.toString();
	}

	public MappableResourcesContainer getMapInfo() {
		return mapInfo;
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		if (false /* attrs.getValue(ATTR_BUS).equals(PINS) */) {
			return Bounds.create(0, 0,
					10 + attrs.getValue(ATTR_SIZE).intValue() * 10, 40).rotate(
					Direction.NORTH, Direction.NORTH, 0, 0);
		} else {
                        int n = attrs.getValue(ATTR_SIZE).intValue();
                        if (n < 8)
                            n = 8;
			return Bounds.create(0, 0, 10 + n/2 * 10 , 60).rotate(
                                        Direction.NORTH, Direction.NORTH, 0, 0);
		}
	}

	/*
	 * private static class State implements InstanceData, Cloneable {
	 * 
	 * private int Value; private int size;
	 * 
	 * public State(int value, int size) { Value = value; this.size = size; }
	 * 
	 * public boolean BitSet(int bitindex) { if (bitindex >= size) { return
	 * false; } int mask = 1 << bitindex; return (Value & mask) != 0; }
	 * 
	 * public void ToggleBit(int bitindex) { if ((bitindex < 0) || (bitindex >=
	 * size)) { return; } int mask = 1 << bitindex; Value ^= mask; }
	 * 
	 * @Override public Object clone() { try { return super.clone(); } catch
	 * (CloneNotSupportedException e) { return null; } } }
	 */

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs, char Vendor) {
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new PortHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs, Vendor);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == Io.ATTR_LABEL_LOC) {
			computeTextField(instance);
		} else if (attr == ATTR_SIZE || attr == ATTR_DIR) {
			instance.recomputeBounds();
			configurePorts(instance);
			computeTextField(instance);
			MyIOInformation.setNrOfInOutports(
					instance.getAttributeValue(ATTR_SIZE),
					GetLabels(instance.getAttributeValue(ATTR_SIZE)));
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		/*
		 * State state = (State) painter.getData(); if (state == null) { state =
		 * new State(0,painter.getAttributeValue(ATTR_SIZE));
		 * painter.setData(state); }
		 */
		Bounds bds = painter.getBounds();
                int x = bds.getX();
                int y = bds.getY();
                int w = bds.getWidth();
                int h = bds.getHeight();

		Graphics g = painter.getGraphics();
		GraphicsUtil.switchToWidth(g, 2);
		g.setColor(Color.darkGray);
		g.fillRect(x+1, y+20, w-2, h-24);
		GraphicsUtil.switchToWidth(g, 1);
		if (false /* painter.getAttributeValue(ATTR_BUS).equals(PINS) */) {
			// TODO YSY PINS
			g.setColor(Color.white);
			g.setFont(StdAttr.DEFAULT_LABEL_FONT
					.deriveFont(StdAttr.DEFAULT_LABEL_FONT.getSize2D() * 0.6f));
			for (int i = 0; i < painter.getAttributeValue(ATTR_SIZE); i++) {
				g.fillRect(bds.getX() + 6 + (i * 10), bds.getY() + 15, 6, 6);
				if (i == 0 || i == painter.getAttributeValue(ATTR_SIZE) - 1) {
					g.drawChars(Integer.toString(i).toCharArray(), 0, Integer
							.toString(i).toCharArray().length, bds.getX() + 6
							+ (i < 10 ? 0 : -2) + i * 10, bds.getY() + 12);
				}
			}
		} else {
			g.setColor(Color.LIGHT_GRAY);
                        int n = painter.getAttributeValue(ATTR_SIZE);
			for (int i = 0; i < n; i++) {
				g.fillRect(x + 7 + ((i/2) * 10), y + 35 + (i%2)*10, 6, 6);
			}
			g.setColor(Color.WHITE);
			g.setFont(StdAttr.DEFAULT_LABEL_FONT);
			String text = "" + n + " PIN";
			g.drawChars(text.toCharArray(), 0, text.toCharArray().length, x + 7, y + 32);
                        g.setColor(Color.BLACK);
                        String dir = painter.getAttributeValue(ATTR_DIR);
                        int px = (dir == INOUT ? x : x + 10);
                        int py = y;
                        GraphicsUtil.switchToWidth(g, 2);
                        while (n > 0) {
                            if (dir == INOUT) {
                                g.drawLine(px, py+10, px+6, py+10);
                                px += 10;
                            }
                            if (dir == OUTPUT || dir == INOUT) {
                                g.drawLine(px, py, px, py+4);
                                int[] xp = {px, px-4, px+4, px};
                                int[] yp = {py+15, py+5, py+5, py+15};
                                g.drawPolyline(xp, yp, 4);
                                g.drawLine(px, py+15, px, py+20);
                                px += 10;
                            }
                            if (dir == INPUT || dir == INOUT) {
                                g.drawLine(px, py, px, py+5);
                                int[] xp = {px, px-4, px+4, px};
                                int[] yp = {py+6, py+16, py+16, py+6};
                                g.drawPolyline(xp, yp, 4);
                                g.drawLine(px, py+16, px, py+20);
                                px += 10;
                            }
                            n -= 32;
                        }
                        GraphicsUtil.switchToWidth(g, 1);
		}
		g.setColor(painter.getAttributeValue(Io.ATTR_LABEL_COLOR));
		painter.drawLabel();
		painter.drawPorts();
	}

	@Override
	public void propagate(InstanceState state) {
		throw new UnsupportedOperationException(
				"PortIO simulation not implemented");
		// State pins = (State) state.getData();
		// if (pins == null) {
		// pins = new State(0, state.getAttributeValue(ATTR_SIZE));
		// state.setData(pins);
		// }
		// for (int i = 0; i < state.getAttributeValue(ATTR_SIZE); i++) {
		// Value pinstate = (pins.BitSet(i)) ? Value.TRUE : Value.FALSE;
		// state.setPort(i, pinstate, 1);
		// }
	}

	// public static class Poker extends InstancePoker {
	//
	// @Override
	// public void mousePressed(InstanceState state, MouseEvent e) {
	// State val = (State) state.getData();
	// Location loc = state.getInstance().getLocation();
	// int cx = e.getX() - loc.getX() - 5;
	// int i = cx / 10;
	// val.ToggleBit(i);
	// state.getInstance().fireInvalidated();
	// }
	// }
	//
	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	public void setMapInfo(MappableResourcesContainer mapInfo) {
		this.mapInfo = mapInfo;
	}
}
