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

package com.cburch.logisim.std.gates;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.Icons;

class NotGate extends InstanceFactory {

  static void configureLabel(Instance instance, boolean isRectangular,
      Location control) {
    Object facing = instance.getAttributeValue(StdAttr.FACING);
    Bounds bds = instance.getBounds();
    int x;
    int y;
    int halign;
    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
      x = bds.getX() + bds.getWidth() / 2 + 2;
      y = bds.getY() - 2;
      halign = TextField.H_LEFT;
    } else { // west or east
      y = isRectangular ? bds.getY() - 2 : bds.getY();
      if (control != null && control.getY() == bds.getY()) {
        // the control line will get in the way
        x = control.getX() + 2;
        halign = TextField.H_LEFT;
      } else {
        x = bds.getX() + bds.getWidth() / 2;
        halign = TextField.H_CENTER;
      }
    }
    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign,
        TextField.V_BASELINE);
  }

  public static final AttributeOption SIZE_NARROW = new AttributeOption(
      Integer.valueOf(20), S.getter("gateSizeNarrowOpt"));
  public static final AttributeOption SIZE_WIDE = new AttributeOption(
      Integer.valueOf(30), S.getter("gateSizeWideOpt"));

  public static final Attribute<AttributeOption> ATTR_SIZE = Attributes
      .forOption("size", S.getter("gateSizeAttr"),
          new AttributeOption[] { SIZE_NARROW, SIZE_WIDE });
  private static final String RECT_LABEL = "1";
  private static final Icon toolIcon = Icons.getIcon("notGate.gif");
  private static final Icon toolIconRect = Icons.getIcon("notGateRect.gif");

  private static final Icon toolIconDin = Icons.getIcon("dinNotGate.gif");

  public static InstanceFactory FACTORY = new NotGate();

  private NotGate() {
    super("NOT Gate", S.getter("notGateComponent"));
    setAttributes(new Attribute[] { StdAttr.FACING, StdAttr.WIDTH,
      ATTR_SIZE, GateAttributes.ATTR_OUTPUT, StdAttr.LABEL,
      StdAttr.LABEL_FONT, }, new Object[] { Direction.EAST,
        BitWidth.ONE, SIZE_WIDE, GateAttributes.OUTPUT_01, "",
        StdAttr.DEFAULT_LABEL_FONT, });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
    String gateShape = AppPreferences.GATE_SHAPE.get();
    configureLabel(instance,
        gateShape.equals(AppPreferences.SHAPE_RECTANGULAR), null);
  }

  private void configurePorts(Instance instance) {
    Object size = instance.getAttributeValue(ATTR_SIZE);
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    int dx = size == SIZE_NARROW ? -20 : -30;

    Port[] ports = new Port[2];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    Location out = Location.create(0, 0).translate(facing, dx);
    ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, StdAttr.WIDTH);
    instance.setPorts(ports);
  }

  @Override
  protected Object getInstanceFeature(final Instance instance, Object key) {
    if (key == ExpressionComputer.class) {
      return new ExpressionComputer() {
        public void computeExpression(
            ExpressionComputer.Map expressionMap) {
          int width = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
          for (int b = 0; b < width; b++) {
            Expression e = expressionMap.get(instance
                .getPortLocation(1), b);
            if (e != null) {
              expressionMap.put(instance.getPortLocation(0), b,
                  Expressions.not(e));
            }
          }
        }
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Object value = attrs.getValue(ATTR_SIZE);
    if (value == SIZE_NARROW) {
      Direction facing = attrs.getValue(StdAttr.FACING);
      if (facing == Direction.SOUTH)
        return Bounds.create(-9, -20, 18, 20);
      if (facing == Direction.NORTH)
        return Bounds.create(-9, 0, 18, 20);
      if (facing == Direction.WEST)
        return Bounds.create(0, -9, 20, 18);
      return Bounds.create(-20, -9, 20, 18);
    } else {
      Direction facing = attrs.getValue(StdAttr.FACING);
      if (facing == Direction.SOUTH)
        return Bounds.create(-9, -30, 18, 30);
      if (facing == Direction.NORTH)
        return Bounds.create(-9, 0, 18, 30);
      if (facing == Direction.WEST)
        return Bounds.create(0, -9, 30, 18);
      return Bounds.create(-30, -9, 30, 18);
    }
  }

  @Override
  public boolean HasThreeStateDrivers(AttributeSet attrs) {
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      return !(attrs.getValue(GateAttributes.ATTR_OUTPUT) == GateAttributes.OUTPUT_01);
    else
      return false;
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    if (ctx.lang.equals("VHDL"))
      return GateVhdlGenerator.forNot(ctx);
    else
      return GateVerilogGenerator.forNot(ctx);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_SIZE || attr == StdAttr.FACING) {
      instance.recomputeBounds();
      configurePorts(instance);
      String gateShape = AppPreferences.GATE_SHAPE.get();
      configureLabel(instance,
          gateShape.equals(AppPreferences.SHAPE_RECTANGULAR), null);
    }
  }

  //
  // painting methods
  //
  @Override
  public void paintIcon(InstancePainter painter) {
    Graphics2D g = painter.getGraphics();
    g.setColor(Color.BLACK);
    if (painter.getGateShape() == AppPreferences.SHAPE_RECTANGULAR) {
      if (toolIconRect != null) {
        toolIconRect.paintIcon(painter.getDestination(), g, 2, 2);
      } else {
        g.drawRect(0, 2, 16, 16);
        painter.drawCenteredText(RECT_LABEL, 8, 8);
        g.drawOval(16, 8, 4, 4);
      }
    } else if (painter.getGateShape() == AppPreferences.SHAPE_DIN40700) {
      if (toolIconDin != null) {
        toolIconDin.paintIcon(painter.getDestination(), g, 2, 2);
      } else {
        g.drawRect(0, 2, 16, 16);
        painter.drawCenteredText(RECT_LABEL, 8, 8);
        g.drawOval(16, 8, 4, 4);
      }
    } else {
      if (toolIcon != null) {
        toolIcon.paintIcon(painter.getDestination(), g, 2, 2);
      } else {
        int[] xp = new int[] { 15, 1,  1, 15 };
        int[] yp = new int[] { 10, 3, 17, 10 };
        g.drawPolyline(xp, yp, 4);
        g.drawOval(15, 8, 4, 4);
      }
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.paintWithLocRotPortLabel(p -> {
      Object shape = painter.getGateShape();
      if (shape == AppPreferences.SHAPE_RECTANGULAR) {
        paintRectangularBase(g, painter);
      } else if (shape == AppPreferences.SHAPE_DIN40700) {
        int width = painter.getAttributeValue(ATTR_SIZE) == SIZE_NARROW ? 20 : 30;
        PainterDin.paintAnd(painter, width, 18, true);
      } else {
        PainterShaped.paintNot(painter);
      }
    });
  }

  private void paintRectangularBase(Graphics2D g, InstancePainter painter) {
    painteer.switchToWidth(2);
    if (painter.getAttributeValue(ATTR_SIZE) == SIZE_NARROW) {
      g.drawRect(-20, -9, 14, 18);
      painter.drawCenteredText(RECT_LABEL, -13, 0);
      g.drawOval(-6, -3, 6, 6);
    } else {
      g.drawRect(-30, -9, 20, 18);
      painter.drawCenteredText(RECT_LABEL, -20, 0);
      g.drawOval(-10, -5, 9, 9);
    }
    painter.switchToWidth(1);
  }

  @Override
  public void propagate(InstanceState state) {
    Value in = state.getPortValue(1);
    Value out = in.not();
    out = Buffer.repair(state, out);
    state.setPort(0, out, GateAttributes.DELAY);
  }
}
