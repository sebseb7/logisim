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

package com.cburch.logisim.std.base;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.tools.CustomHandles;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

public class Text extends InstanceFactory implements CustomHandles {

  private static class MultilineAttribute extends Attribute<String> {
    MultilineAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public String parse(String escaped) {
      StringBuilder s = new StringBuilder();
      boolean escape = false;
      for (int i = 0; i < escaped.length(); i++) {
        char c = (char)escaped.charAt(i);
        if (c == '\\')
          escape = true;
        else if (escape) {
          escape = false;
          switch (c) {
          case 't': s.append('\t'); break;
          case 'n': s.append('\n'); break;
          default: s.append(c); break;
          }
        } else {
          s.append(c);
        }
      }
      return s.toString();
    }

    public String toDisplayString(String s) {
      StringBuilder escaped = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        char c = (char)s.charAt(i);
        switch (c) {
        case '\t': escaped.append("\\t"); break;
        case '\n': escaped.append("\\n"); break;
        case '\\': escaped.append("\\\\"); break;
        default: escaped.append(c); break;
        }
      }
      return escaped.toString();
    }
  }



  public static Attribute<String> ATTR_TEXT = new MultilineAttribute("text",
      S.getter("textTextAttr"));
  public static Attribute<Font> ATTR_FONT = Attributes.forFont("font",
      S.getter("textFontAttr"));
  public static Attribute<AttributeOption> ATTR_HALIGN = Attributes
      .forOption(
          "halign",
          S.getter("textHorzAlignAttr"),
          new AttributeOption[] {
            new AttributeOption(Integer.valueOf(TextField.H_LEFT),
                "left", S.getter("textHorzAlignLeftOpt")),
            new AttributeOption(Integer.valueOf(TextField.H_RIGHT),
                "right", S.getter("textHorzAlignRightOpt")),
            new AttributeOption(Integer.valueOf(TextField.H_CENTER),
                "center", S.getter("textHorzAlignCenterOpt")),
          });
  public static Attribute<AttributeOption> ATTR_VALIGN = Attributes
      .forOption(
          "valign",
          S.getter("textVertAlignAttr"),
          new AttributeOption[] {
            new AttributeOption(Integer.valueOf(TextField.V_TOP),
                "top", S.getter("textVertAlignTopOpt")),
            new AttributeOption(Integer.valueOf(TextField.V_BASELINE),
                "base", S.getter("textVertAlignBaseOpt")),
            new AttributeOption(Integer.valueOf(TextField.V_BOTTOM),
                "bottom", S.getter("textVertAlignBottomOpt")),
            new AttributeOption(Integer.valueOf(TextField.H_CENTER),
                "center", S.getter("textVertAlignCenterOpt")),
          });

  public static final Text FACTORY = new Text();

  private Text() {
    super("Text", S.getter("textComponent"));
    setIconName("text.gif");
    setShouldSnap(false);
  }

  private void configureLabel(Instance instance) {
    TextAttributes attrs = (TextAttributes) instance.getAttributeSet();
    Location loc = instance.getLocation();
    instance.setTextField(ATTR_TEXT, ATTR_FONT, loc.getX(), loc.getY(),
        attrs.getHorizontalAlign(), attrs.getVerticalAlign(), true);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configureLabel(instance);
    instance.addAttributeListener();
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new TextAttributes();
  }

  @Override
  public Component createComponent(Location loc, AttributeSet attrs) {
    InstanceComponent ret = new InstanceComponent(this, loc, attrs) {
      @Override
      public Bounds getBounds() {
        // Can't properly compute without a graphics context.
        // But this still gets called in some cases, e.g. while opening a file
        // to check for overlap, and during some copy-paste operations.
        return getBounds(null);
        // return Bounds.EMPTY_BOUNDS.translate(p.getX(), p.getY());
      }
      @Override
      public Bounds getBounds(Graphics g) {
        Location p = getLocation();
        return getFactory().getOffsetBounds(getAttributeSet(), g).translate(p.getX(), p.getY());
      }
      @Override
      public void expose(ComponentDrawContext context) {
        Bounds b = getBounds(context.getGraphics());
        context.getDestination().repaint(b.getX(), b.getY(), b.getWidth(), b.getHeight());
      }
    };
    configureNewInstance(ret.getInstance());
    return ret;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase) {
    // Can't properly compute without a graphics context.
    // But this still gets called in some cases, e.g. while opening a file
    // to check for overlap, and during some copy-paste operations.
    // return Bounds.EMPTY_BOUNDS;
    return getOffsetBounds(attrsBase, null);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase, Graphics g) {
    TextAttributes attrs = (TextAttributes) attrsBase;
    String text = attrs.getText();
    if (text == null || text.equals("")) {
      return Bounds.EMPTY_BOUNDS;
    } else {
      Bounds bds = attrs.getOffsetBounds();
      if (bds == null) {
        int halign = attrs.getHorizontalAlign();
        int valign = attrs.getVerticalAlign();
        Font font = attrs.getFont();
        if (g == null) {
          // This should not happen in most (any) cases...
          bds = StringUtil.estimateBounds(text, font, halign, valign);
        } else {
          String lines[] = text.split("\n");
          Rectangle r = GraphicsUtil.getTextBounds(g, font, lines, 0, 0, halign, valign);
          bds = Bounds.create(r).expand(4);
          attrs.setOffsetBounds(bds);
        }
      }
      return bds;
    }
  }

  @Override
  public boolean HDLIgnore() { return true; }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_HALIGN || attr == ATTR_VALIGN) {
      configureLabel(instance);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paint(painter, true);
  }

  public void paint(InstancePainter painter, boolean border) {
    TextAttributes attrs = (TextAttributes) painter.getAttributeSet();
    String text = attrs.getText();
    if (text == null || text.equals(""))
      return;
    int halign = attrs.getHorizontalAlign();
    int valign = attrs.getVerticalAlign();
    Font font = attrs.getFont();

    Graphics g = painter.getGraphics();
    Location loc = painter.getLocation();

    String lines[] = text.split("\n");
    Rectangle r = GraphicsUtil.getTextBounds(g, font, lines, 0, 0, halign, valign);
    Bounds b = Bounds.create(r).expand(4);
    if (attrs.setOffsetBounds(b)) {
      Instance instance = painter.getInstance();
      if (instance != null)
        instance.recomputeBounds();
    }

    if (border) {
      Bounds bds = painter.getBoundsWithText();
      g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
    GraphicsUtil.drawText(g, font, lines, loc.getX(), loc.getY(), halign, valign);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.getGraphics().setColor(Color.BLACK);
    paint(painter, false);
  }

  @Override
  public void propagate(InstanceState state) {
  }

  @Override
  public Object getInstanceFeature(Instance instance, Object key) {
    if (key == CustomHandles.class)
      return this;
    return null;
  }

  @Override
  public void drawHandles(ComponentDrawContext context) {
    Graphics g = context.getGraphics();
    g.setColor(Color.GRAY);
    InstancePainter painter = context.getInstancePainter();
    Bounds bds = painter.getBoundsWithText();
    TextAttributes attrs = (TextAttributes) painter.getAttributeSet();
    String text = attrs.getText();
    g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    painter.drawHandles();
  }
}
