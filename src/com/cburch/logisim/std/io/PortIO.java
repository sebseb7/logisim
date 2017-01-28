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
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePoker;
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
	public static final String INOUT_1 = "in/out (single enable)";
	public static final String INOUT_N = "in/out (per-bit enable)";

	public static final String[] DIRECTIONS = { INPUT, OUTPUT, INOUT_1, INOUT_N };
	public static final Attribute<String> ATTR_DIR = Attributes.forOption(
			"direction", Strings.getter("pioDirection"), DIRECTIONS);

	private MappableResourcesContainer mapInfo;

	public PortIO() {
		super("PortIO", Strings.getter("pioComponent"));
		int portSize = 8;
		setAttributes(new Attribute[] { StdAttr.FACING, StdAttr.LABEL, Io.ATTR_LABEL_LOC,
				StdAttr.LABEL_FONT, Io.ATTR_LABEL_COLOR, ATTR_SIZE, ATTR_DIR},
				new Object[] { Direction.EAST, "", Direction.EAST, StdAttr.DEFAULT_LABEL_FONT,
						Color.BLACK, portSize, INOUT_1 });
		setFacingAttribute(StdAttr.FACING);
		setIconName("pio.gif");
		setInstancePoker(PortPoker.class);
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
		updatePorts(instance);
		computeTextField(instance);
		MyIOInformation.setNrOfInOutports(
				instance.getAttributeValue(ATTR_SIZE),
				GetLabels(instance.getAttributeValue(ATTR_SIZE)));
	}

	private void updatePorts(Instance instance) {
		Direction facing = instance.getAttributeValue(StdAttr.FACING);
		String dir = instance.getAttributeValue(ATTR_DIR);
		int size = instance.getAttributeValue(ATTR_SIZE);
		// logisim max bus size is 32, so use multiple buses if needed
		int nBus = (((size - 1) / 32) + 1);
		int nPorts = -1;
		if (dir == INPUT || dir == OUTPUT)
			nPorts = nBus;
		else if (dir == INOUT_N)
			nPorts = 3*nBus;
		else if (dir == INOUT_1)
			nPorts = 2*nBus + 1;
		Port[] ps = new Port[nPorts];
		int p = 0;

		int x = 0, y = 0, dx = 0, dy = 0;
		if (facing == Direction.NORTH)
			dy = -10;
		else if (facing == Direction.SOUTH)
			dy = 10;
		else if (facing == Direction.WEST)
			dx = -10;
		else 
			dx = 10;
		if (dir == INPUT || dir == OUTPUT) {
			x += dx; y += dy;
		}
		if (dir == INOUT_1) {
			ps[p] = new Port(x-dy, y+dx, Port.INPUT, 1);
			ps[p].setToolTip(StringUtil.constantGetter("OutEnable"));
			p++;
			x += dx; y += dy;
		}
		int n = size;
		int i = 0;
		while (n > 0) {
			int e = (n > 32 ? 32 : n);
			String range = "[" + i + " to " + (i + e - 1) +"]";
			if (dir == INOUT_N) {
				ps[p] = new Port(x-dy, y+dx, Port.INPUT, e);
				ps[p].setToolTip(StringUtil.constantGetter("OutEnable"+range));
				p++;
				x += dx; y += dy;
			}
			if (dir == OUTPUT || dir == INOUT_1 || dir == INOUT_N) {
				ps[p] = new Port(x, y, Port.INPUT, e);
				ps[p].setToolTip(StringUtil.constantGetter("Out"+range));
				p++;
				x += dx; y += dy;
			}
			i += 32;
			n -= e;
		}
		n = size;
		i = 0;
		while (n > 0) {
			int e = (n > 32 ? 32 : n);
			String range = "[" + i + " to " + (i + e - 1) +"]";
			if (dir == INPUT || dir == INOUT_1 || dir == INOUT_N) {
				ps[p] = new Port(x, y, Port.OUTPUT, e);
				ps[p].setToolTip(StringUtil.constantGetter("In"+range));
				p++;
				x += dx; y += dy;
			}
			i += 32;
			n -= e;
		}
		instance.setPorts(ps);

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
		Direction facing = attrs.getValue(StdAttr.FACING);
                int n = attrs.getValue(ATTR_SIZE).intValue();
                if (n < 8)
                    n = 8;
                return Bounds.create(0, 0, 10 + n/2 * 10 , 50)
                    .rotate(Direction.EAST, facing, 0, 0);
	}

	
        private static class PortState implements InstanceData, Cloneable {

            Value pin[]; // pindata = usrdata + indata
            Value usr[]; // usrdata
            int size;

            public PortState(int size) {
                this.size = size;
                int nBus = (((size - 1) / 32) + 1);
                pin = new Value[nBus];
                usr = new Value[nBus];
                for (int i = 0; i < nBus; i++) {
                    int n = (size > 32 ? 32 : size);
                    pin[i] = Value.createUnknown(BitWidth.create(n));
                    usr[i] = Value.createUnknown(BitWidth.create(n));
                    size -= n;
                }
            }

            public void resize(int sz) {
                int nBus = (((sz - 1) / 32) + 1);
                if (nBus != (((size - 1) / 32) + 1)) {
                    pin = Arrays.copyOf(pin, nBus);
                    usr = Arrays.copyOf(usr, nBus);
                }
                for (int i = 0; i < nBus; i++) {
                    int n = (sz > 32 ? 32 : sz);
                    if (pin[i] == null)
                        pin[i] = Value.createUnknown(BitWidth.create(n));
                    else
                        pin[i] = pin[i].extendWidth(n, Value.UNKNOWN);
                    if (usr[i] == null)
                        usr[i] = Value.createUnknown(BitWidth.create(n));
                    else
                        usr[i] = usr[i].extendWidth(n, Value.UNKNOWN);
                }
                size = sz;
            }

            public void toggle(int i) {
                int n = i / 32;
                i = i % 32;
                Value v = usr[n].get(i);
                if (v == Value.UNKNOWN)
                    v = Value.FALSE;
                else if (v == Value.FALSE)
                    v = Value.TRUE;
                else 
                    v = Value.UNKNOWN;
                usr[n] = usr[n].set(i, v);
            }

            public Value get(int i) {
                return pin[i/32].get(i%32);
            }

            public Color getColor(int i) {
                Value v = get(i);
                return (v == Value.UNKNOWN ? Color.LIGHT_GRAY : v.getColor());
            }

            @Override
            public Object clone() {
                try {
                    PortState other = (PortState)super.clone();
                    other.pin = Arrays.copyOf(pin, pin.length);
                    other.usr = Arrays.copyOf(usr, usr.length);
                    return other;
                }
                catch (CloneNotSupportedException e) { return null; }
            }
        }
	

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
		} else if (attr == ATTR_SIZE || attr == ATTR_DIR || attr == StdAttr.FACING) {
			instance.recomputeBounds();
			updatePorts(instance);
			computeTextField(instance);
			MyIOInformation.setNrOfInOutports(
					instance.getAttributeValue(ATTR_SIZE),
					GetLabels(instance.getAttributeValue(ATTR_SIZE)));
		}
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Direction facing = painter.getAttributeValue(StdAttr.FACING);

		Bounds bds = painter.getBounds().rotate(Direction.EAST, facing, 0, 0);
                int w = bds.getWidth();
                int h = bds.getHeight();
                int x = painter.getLocation().getX();
                int y = painter.getLocation().getY();
                Graphics g = painter.getGraphics(); 
                g.translate(x, y); 
                double rotate = 0.0; 
                if (facing != Direction.EAST && g instanceof Graphics2D) { 
                    rotate = -facing.toRadians(); 
                    ((Graphics2D) g).rotate(rotate); 
                } 

		GraphicsUtil.switchToWidth(g, 2);
		g.setColor(Color.DARK_GRAY);
                int bx[] = {1, 1, 5, w-6, w-2, w-2, 1};
                int by[] = {20, h-8, h-4, h-4, h-8, 20, 20};
                g.fillPolygon(bx, by, 6);
		g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 1);
                g.drawPolyline(bx, by, 7);
		
                int size = painter.getAttributeValue(ATTR_SIZE);
                int nBus = (((size - 1) / 32) + 1);
                if (!painter.getShowState()) {
                    g.setColor(Color.LIGHT_GRAY);
                    for (int i = 0; i < size; i++)
                        g.fillRect(7 + ((i/2) * 10),  25 + (i%2)*10, 6, 6);
                }  else {
                    PortState data = getState(painter);
                    for (int i = 0; i < size; i++) {
                        g.setColor(data.getColor(i));
                        g.fillRect(7 + ((i/2) * 10),  25 + (i%2)*10, 6, 6);
                    }
                }
                g.setColor(Color.BLACK);
                String dir = painter.getAttributeValue(ATTR_DIR);
                int px = ((dir == INOUT_1 || dir == INOUT_N) ? 0 : 10);
                int py = 0;
                for (int p = 0; p < nBus; p++) {
                    if (dir == INOUT_1) {
                        GraphicsUtil.switchToWidth(g, 3);
                        if (p == 0) {
                            g.drawLine(px, py+10, px+6, py+10);
                            px += 10;
                        } else {
                            g.drawLine(px-6, py+10, px-4, py+10);
                        }
                    }
                    if (dir == INOUT_N) {
                        GraphicsUtil.switchToWidth(g, 3);
                        g.drawLine(px, py+10, px+6, py+10);
                        px += 10;
                    }
                    if (dir == OUTPUT || dir == INOUT_1 || dir == INOUT_N) {
                        GraphicsUtil.switchToWidth(g, 3);
                        g.drawLine(px, py, px, py+4);
                        g.drawLine(px, py+15, px, py+20);
                        GraphicsUtil.switchToWidth(g, 2);
                        int[] xp = {px, px-4, px+4, px};
                        int[] yp = {py+15, py+5, py+5, py+15};
                        g.drawPolyline(xp, yp, 4);
                        px += 10;
                    }
                }

                for (int p = 0; p < nBus; p++) {
                    if (dir == INPUT || dir == INOUT_1 || dir == INOUT_N) {
                        GraphicsUtil.switchToWidth(g, 3);
                        g.drawLine(px, py, px, py+5);
                        g.drawLine(px, py+16, px, py+20);
                        GraphicsUtil.switchToWidth(g, 2);
                        int[] xp = {px, px-4, px+4, px};
                        int[] yp = {py+6, py+16, py+16, py+6};
                        g.drawPolyline(xp, yp, 4);
                        px += 10;
                    }
                }

                GraphicsUtil.switchToWidth(g, 1);
                ((Graphics2D) g).rotate(-rotate); 
                g.translate(-x, -y);

		painter.drawPorts();
		g.setColor(painter.getAttributeValue(Io.ATTR_LABEL_COLOR));
		painter.drawLabel();
	}

	protected static final int DELAY = 1;

        private static PortState getState(InstanceState state) {
            int size = state.getAttributeValue(ATTR_SIZE);
            PortState data = (PortState) state.getData();
            if (data == null) {
                data = new PortState(size);
                state.setData(data);
                return data;
            }
            if (data.size != size)
                data.resize(size);
            return data;
        }

	@Override
	public void propagate(InstanceState state) {
            String dir = state.getAttributeValue(ATTR_DIR);
            int size = state.getAttributeValue(ATTR_SIZE);
            int nBus = (((size - 1) / 32) + 1);

            PortState data = getState(state);

            if (dir == OUTPUT) {
                for (int i = 0; i < nBus; i++) {
                    data.pin[i] = state.getPortValue(i);
                }
            } else if (dir == INPUT) {
                for (int i = 0; i < nBus; i++) {
                    data.pin[i] = data.usr[i];
                    state.setPort(i, data.pin[i], DELAY);
                }
            } else if (dir == INOUT_1) {
                Value en = state.getPortValue(0);
                // pindata = usrdata + en.controls(indata)
                // where "+" resolves like:
                //     Z 0 1 E
                //     -------
                // Z | Z 0 1 E
                // 0 | 0 0 E E
                // 1 | 1 E 1 E
                for (int i = 0; i < nBus; i++) {
                    Value in = state.getPortValue(i+1);
                    data.pin[i] = data.usr[i].combine(en.controls(in));
                    state.setPort(1+nBus+i, data.pin[i], DELAY);
                }
            } else if (dir == INOUT_N) {
                for (int i = 0; i < nBus; i++) {
                    Value en = state.getPortValue(i*2);
                    Value in = state.getPortValue(i*2+1);
                    data.pin[i] = data.usr[i].combine(en.controls(in));
                    state.setPort(2*nBus+i, data.pin[i], DELAY);
                }
            }
	}

        public static class PortPoker extends InstancePoker {
            @Override
            public void mouseReleased(InstanceState state, MouseEvent e) {
                Location loc = state.getInstance().getLocation();
                int cx = e.getX() - loc.getX() - 7 + 2;
                int cy = e.getY() - loc.getY() - 25 + 2;
                if (cx < 0 || cy < 0)
                    return;
                int i = cx / 10;
                int j = cy / 10;
                if (j > 1)
                    return;
                int n = 2*i + j;
                PortState data = getState(state);
                if (n < 0 || n >= data.size)
                    return;
                data.toggle(n);
                state.getInstance().fireInvalidated();
            }
	}
	
	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

	public void setMapInfo(MappableResourcesContainer mapInfo) {
		this.mapInfo = mapInfo;
	}
}
