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
package com.bfh.logisim.library;

import java.awt.Color;
import java.awt.Graphics;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.std.arith.Strings;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class DynamicClock extends InstanceFactory{

	private static final int SPEED = 0;

	public static final Attribute<BitWidth> WIDTH_ATTR = Attributes
			.forBitWidth("width", Strings.getter("Tick Resolution"), 1, 32);

	public DynamicClock() {
		super("Dynamic_Clock_Control", Strings.getter("DynamicClockControl"));
		setAttributes(new Attribute[] { DynamicClock.WIDTH_ATTR },
				new Object[] { BitWidth.create(28) });
		setKeyConfigurator(new BitWidthConfigurator(DynamicClock.WIDTH_ATTR,1,32,0));
		setOffsetBounds(Bounds.create(-60, -30, 65, 60));
		setIconName("dynclock.gif");
		Port[] ps = new Port[1];
		ps[SPEED] = new Port(-30, 30, Port.INPUT, WIDTH_ATTR);
                ps[SPEED].setToolTip(Strings.getter("CountReset: Reset value for tick counter (higher numbers make slower clocks)"));
		setPorts(ps);
	}
	
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		g.setColor(Color.BLACK);
		painter.drawBounds();

		Bounds bds = painter.getInstance().getBounds();
                drawTicks(g, bds.getX() + 60, bds.getY() + 12, 1, 4, 5, 12);
                drawTicks(g, bds.getX() + 60, bds.getY() + 30, 2, 8, 5, 6);
                drawTicks(g, bds.getX() + 60, bds.getY() + 48, 3, 12, 5, 4);

		painter.drawPort(SPEED);
	}

        private static void drawTicks(Graphics g, int x, int y, int label, int w, int h, int n) {
            GraphicsUtil.switchToWidth(g, 2);
            g.setColor(h > 0 ? Value.TRUE_COLOR : Value.FALSE_COLOR);
            g.drawLine(x, y, x, y-h);
            for (int i = 0; i < n; i++) {
                g.setColor(h > 0 ? Value.TRUE_COLOR : Value.FALSE_COLOR);
                g.drawLine(x, y-h, x-w, y-h);
                if (i != n-1)
                    g.drawLine(x-w, y-h, x-w, y+h);
                else 
                    g.drawLine(x-w, y-h, x-w, y);
                h *= -1;
                x -= w;
            }
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(9.0f));
            GraphicsUtil.drawCenteredText(g, "" + label , x-6, y);
        }
	
	@Override
	public void propagate(InstanceState state) { }
	
	@Override
	public String getHDLName(AttributeSet attrs) { return null; }
	
	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			                             AttributeSet attrs,
			                             char Vendor) { return true; }
}
