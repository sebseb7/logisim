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

package com.cburch.logisim.analyze.gui;
import static com.cburch.logisim.analyze.model.Strings.S;

import java.awt.Color;
import java.awt.Component;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.text.AttributedString;
import java.awt.font.TextAttribute;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.ExpressionVisitor;

class ExpressionRenderer extends JPanel {

  @Override
  public void validate() { }
  @Override
  public void invalidate() { }
  @Override
  public void revalidate() { }
  @Override
  public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) { }
  @Override
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) { }
  @Override
  public void repaint(long tm, int x, int y, int width, int height) { }
  @Override
  public void repaint(Rectangle r) { }
  @Override
  public void repaint() { }
  @Override
  public boolean isOpaque() {
    Color back = getBackground();
    Component p = getParent();
    if (p != null)
      p = p.getParent();
    boolean colorMatch = (back != null) && (p != null) &&
        back.equals(p.getBackground()) && p.isOpaque();
    return !colorMatch && super.isOpaque();
  }

  public static final Font OP_FONT = new Font("Serif", Font.PLAIN, 14);
  public static final Font TXT_FONT = new Font("Serif", Font.BOLD, 14);
  public static final Font VAR_FONT = new Font("Serif", Font.BOLD | Font.ITALIC, 14);
  public static final Font SUB_FONT = new Font("Serif", Font.ITALIC, 10);
  public static final FontMetrics OP_FONT_METRICS;
  public static final FontMetrics TXT_FONT_METRICS;
  public static final FontMetrics VAR_FONT_METRICS;
  public static final FontMetrics SUB_FONT_METRICS;
  public static final float OP_ASCENT, OP_DESCENT;
  public static final float TXT_ASCENT, TXT_DESCENT;
  public static final float VAR_ASCENT, VAR_DESCENT;
  public static final float SUB_ASCENT, SUB_DESCENT;
  public static final float TYP_ASCENT, TYP_DESCENT, TYP_HEIGHT;
  static {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D)img.getGraphics().create();
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setFont(OP_FONT);
    OP_FONT_METRICS = g.getFontMetrics();
    g.setFont(TXT_FONT);
    TXT_FONT_METRICS = g.getFontMetrics();
    g.setFont(VAR_FONT);
    VAR_FONT_METRICS = g.getFontMetrics();
    g.setFont(SUB_FONT);
    SUB_FONT_METRICS = g.getFontMetrics();

    OP_ASCENT = OP_FONT_METRICS.getAscent();
    OP_DESCENT = OP_FONT_METRICS.getDescent();
    TXT_ASCENT = TXT_FONT_METRICS.getAscent();
    TXT_DESCENT = TXT_FONT_METRICS.getDescent();
    VAR_ASCENT = VAR_FONT_METRICS.getAscent();
    VAR_DESCENT = VAR_FONT_METRICS.getDescent();
    SUB_ASCENT = SUB_FONT_METRICS.getAscent();
    SUB_DESCENT = SUB_FONT_METRICS.getDescent();
    TYP_ASCENT = VAR_ASCENT;
    TYP_DESCENT = VAR_DESCENT + SUB_DESCENT;
    TYP_HEIGHT = TYP_ASCENT + TYP_DESCENT;

    g.dispose();
  }

  abstract class Box {
    //           ___________________
    //     ^    |                   |   )
    //     |    |                   |   ) ascent 
    //  height  |                   |   )
    //     |   -o-  -  -  -  -  -  -|-
    //     v    |___________________|   ) descent
    //           <----- width -----> 
    
    float w, h, a, d; // width, height, ascent, descent
    float xx, yy;
    int depth;

    Box(int depth) { this.depth = depth; }
    abstract void paint(Graphics2D g);
    abstract void layout(float x, float y, LineBreaker lines);
    void move(float dx, float dy) { xx += dx; yy += dy; }
  }

  class NotBox extends Box {
    Box inner;
    LineBreaker innerLines = new LineBreaker();

    NotBox(Box inner, int depth) {
      super(depth);
      this.inner = inner;
      w = inner.w + 2f;
      a = inner.a + 3f;
      d = inner.d;
      h = a + d;
    }
    void layout(float x, float y, LineBreaker lines) {
      lines.boxes.add(this);
      xx = x;
      yy = y;
      innerLines.layout(inner, x+1, y);
    }
    void move(float dx, float dy) {
      xx += dx; yy += dy; 
      for (Box b: innerLines.boxes)
        b.move(dx, dy);
    }
    void paint(Graphics2D g) {
      g.draw(new Line2D.Float(xx+1, yy-a+1.5f, xx+w-1, yy-a+1.5f));
      innerLines.paint(g);
    }
  }

  class StringBox extends Box {
    String s;
    int precedence;
    StringBox(String s, int depth) {
      this(s, 0, depth);
    }
    StringBox(String s, int precedence, int depth) {
      super(depth);
      this.s = s;
      this.precedence = precedence;
      w = OP_FONT_METRICS.stringWidth(s);
      a = OP_ASCENT;
      d = OP_DESCENT;
      h = a+d;
    }
    void layout(float x, float y, LineBreaker lines) {
      lines.boxes.add(this);
      xx = x;
      yy = y;
    }
    void paint(Graphics2D g) {
      g.setFont(OP_FONT);
      g.drawString(s, xx, yy);
    }
  }

  static final float PAREN_GROWTH = 1.18f; // each paren nesting grows 18% larger
  static final float PAREN_SHIFT = 0.10f; // shift down 20% to make it look more balanced
  class ParenBox extends StringBox {
    float scale = PAREN_GROWTH;
    ParenBox(String s, int depth) {
      super(s, depth);
      rescale(0);
    }
    void rescale(float innerHeight) {
      scale = PAREN_GROWTH * Math.max(1.0f, innerHeight / TYP_HEIGHT);
      w = scale*OP_FONT_METRICS.stringWidth(s);
      a = scale*OP_ASCENT * PAREN_GROWTH;
      d = scale*OP_DESCENT * PAREN_GROWTH;
      h = a+d;
      float shift = h * PAREN_SHIFT;
      a -= shift;
      d += shift;
    }
    void paint(Graphics2D g) {
      g.setFont(OP_FONT);
      AffineTransform xform = g.getTransform();
      g.scale(scale, scale);
      g.drawString(s, xx/scale, (yy+h*PAREN_SHIFT)/scale);
      g.setTransform(xform);
    }
  }

  class ConstBox extends Box {
    String s;
    ConstBox(String s, int depth) {
      super(depth);
      this.s = s;
      w = TXT_FONT_METRICS.stringWidth(s);
      a = TXT_ASCENT;
      d = TXT_DESCENT;
      h = a+d;
    }
    void layout(float x, float y, LineBreaker lines) {
      lines.boxes.add(this);
      xx = x;
      yy = y;
    }
    void paint(Graphics2D g) {
      g.setFont(TXT_FONT);
      g.drawString(s, xx, yy);
    }
  }

  class VarBox extends Box {
    String name, sub;
    float sx, sy;
    VarBox(String desc, int depth) {
      super(depth);
      int i = desc.indexOf(':');
      if (i >= 0) {
        sub = desc.substring(i+1);
        name = desc.substring(0, i);
      } else {
        name = desc;
      }
      w = VAR_FONT_METRICS.stringWidth(name);
      a = VAR_ASCENT;
      d = VAR_DESCENT;
      if (sub != null) {
        float sw = SUB_FONT_METRICS.stringWidth(sub);
        float sd = SUB_DESCENT;
        sx = w;
        sy = d;
        w += sw;
        d += sd;
      }
      h = a+d;
    }
    void layout(float x, float y, LineBreaker lines) {
      lines.boxes.add(this);
      xx = x;
      yy = y;
    }
    void paint(Graphics2D g) {
      g.setFont(VAR_FONT);
      g.drawString(name, xx, yy);
      if (sub != null) {
        g.setFont(SUB_FONT);
        g.drawString(sub, xx+sx, yy+sy);
      }
    }
  }

  static final Color[] BBOX_SHADE = {
      new Color(200, 100, 100),
      new Color(100, 200, 100),
      new Color(100, 100, 200),
      new Color(200, 100, 200),
  };

  class BoundingBox extends Box {
    int depth; // debug
    Box[] insides;
    BoundingBox(int depth, Box ...insides) {
      super(depth);
      this.insides = insides;
      for (Box b : insides) {
        a = Math.max(a, b.a);
        d = Math.max(d, b.d);
        w += b.w;
        if (b instanceof BoundingBox) { // debug
          int id = ((BoundingBox)b).depth;
          if (id >= depth)
            depth = id+1;
        }
      }
      h = a + d;
    }
    void layout(float x, float y, LineBreaker lines) {
      lines.bboxes.add(this);
      xx = x;
      yy = y;
      for (Box b : insides) {
        b.layout(x, y, lines);
        x += b.w;
      }
    }
    void paint(Graphics2D g) {
      debugShade(g, this, BBOX_SHADE, depth); // debug
    }
  }

  class BoxMaker implements ExpressionVisitor<Box> {
    int depth = 1;
    Box binary(Expression a, Expression b, int level, String op) {
      Box bb_a;
      if (a.getPrecedence() < level) {
        ParenBox lparen = new ParenBox("(", depth);
        ParenBox rparen = new ParenBox(")", depth);
        depth++;
        Box bb = a.visit(this);
        depth--;
        lparen.rescale(bb.h);
        rparen.rescale(bb.h);
        bb_a = new BoundingBox(depth, lparen, bb, rparen);
      } else {
        bb_a = a.visit(this);
      }

      StringBox mid = new StringBox(op, level, depth);

      if (b == null)
        return new BoundingBox(depth, mid, bb_a);


      Box bb_b;
      if (b.getPrecedence() < level) {
        ParenBox lparen = new ParenBox("(", depth);
        ParenBox rparen = new ParenBox(")", depth);
        depth++;
        Box bb = b.visit(this);
        depth--;
        lparen.rescale(bb.h);
        rparen.rescale(bb.h);
        bb_b = new BoundingBox(depth, lparen, bb, rparen);
      } else {
        bb_b = b.visit(this);
      }

      return new BoundingBox(depth, bb_a, mid, bb_b);
    }

    public Box visitAnd(Expression a, Expression b) {
      return binary(a, b, Expression.AND_LEVEL, OPS[notation][0]);
    }
    public Box visitOr(Expression a, Expression b) {
      return binary(a, b, Expression.OR_LEVEL, OPS[notation][1]);
    }
    public Box visitXor(Expression a, Expression b) {
      return binary(a, b, Expression.XOR_LEVEL, OPS[notation][2]);
    }
    public Box visitEq(Expression a, Expression b) {
      return binary(a, b, Expression.EQ_LEVEL, OPS[notation][4]);
    }
    public Box visitConstant(int value) {
      return new ConstBox(Integer.toString(value, 16), depth);
    }
    public Box visitVariable(String desc) {
      return new VarBox(desc, depth);
    }
    public Box visitNot(Expression a) {
      if (notation == ENGINEERING) {
        depth++;
        Box inner = a.visit(this);
        depth--;
        return new NotBox(inner, depth);
      } else {
        return binary(a, null, Expression.NOT_LEVEL, OPS[notation][3]);
      }
    }
  }

  int notation = ENGINEERING;
  
  // Notation choices:
  //   "engineering": downvee, vee, oplus, overbar, =
  //   "mathematics": dot, +, oplus, not, =
  //   "programming": &, |, ^, ~, ==
  static final int ENGINEERING = 0;
  static final int MATHEMATICS = 1;
  static final int PROGRAMMING = 2;
  static final String[][] OPS = {
    { " \u2227 ", " \u2228 ", " \u2295 ", null, " = " }, // { " \u22C0 ", " \u22C1 ", " \u2295 ", null, " = " },
    { " \u22C5 ", " + ", "\u2295", "\u00AC", " = " },
    { " && ", " || ", " ^ ", "!", " == " },
  };

  static final Color[] BOX_SHADE = {
      new Color(200, 150, 100),
      new Color(100, 200, 120),
      new Color(150, 150, 200),
  };

  static class LineBreaker {
    final ArrayList<Box> boxes = new ArrayList<>();
    final ArrayList<BoundingBox> bboxes = new ArrayList<>();

    void paint(Graphics2D g) {
      for (Box b : boxes)
        b.paint(g);
    }

    void debugPaint(Graphics2D g) {
      for (Box b : bboxes)
        b.paint(g);
      int i = 0;
      for (Box b : boxes) {
        debugShade(g, b, BOX_SHADE, i++);
        b.paint(g);
        g.setFont(SUB_FONT);
        g.drawString(""+b.depth, b.xx, b.yy+b.d+5);
      }
    }

    void layout(Box top, float x, float y) {
      boxes.clear();
      bboxes.clear();
      top.layout(x, y, this);
    }

    void fitToWidth(float width) {
      // why = null;
      int n = boxes.size();
      int end = n-1;
      if (n < 1) //  || totalWidth(0, end) <= width)
        return;
      // p[s][end] is total penalty for s..end as one line
      // p[s][e] for e<end is penalty for a line s..e followed by other lines e+1.. ..end
      double[][] p = new double[n][n];
      // best[i] is either end (best is one line) or e<end (best is a linebreak after e)
      int[] best = new int[n];

     // System.out.printf("There are %d boxes\n", n);
      for (int start = end; start >= 0; start--) {
       // System.out.printf("Penalties for laying out %d..%d\n", start, end);
        float w = start == 0 ? width : (width - LEFT_INDENT);
        p[start][end] = penalty(start, end, w);
        // System.out.printf("  as one line %d..%d --> %f\n", start, end, p[start][end]);
        best[start] = end;
        for (int endl = start; endl < end; endl++) {
          p[start][endl] = penalty(start, endl, w) + p[endl+1][best[endl+1]];
          // System.out.printf("  as multiline %d..%d, %d.. ..%s --> %f\n",
          //   start, endl, endl+1, end, p[start][endl]);
          if (p[start][endl] < p[start][best[start]])
            best[start] = endl;
        }
       // System.out.printf("  best is %d..%d --> %f\n", start, best[start], p[start][best[start]]);
      }

      int startl = 0;
      int endl = best[startl];

      // why = "";
      // System.out.printf("  first line is %d..%d --> %f = %f + ...", startl, endl,
      //     p[startl][endl], penalty(startl, endl, width));
      // System.out.println(why);

      // first line stays at left margin, maybe moves up a little
      float xoff = 0;
      float yoff = -(maxAscent(0, end) - maxAscent(0, endl));
      for (int i = startl; i <= endl; i++)
        boxes.get(i).move(xoff, yoff);

      // each next line moves down, move way left, and indents a bit
      xoff = LEFT_INDENT;
      while (endl < end) {
        int next = best[endl+1];
        // why = "";
        // System.out.printf("  next line is %d..%d --> %f = %f + ... ",
        //     endl+1, next, p[endl+1][next], penalty(endl+1, next, width-LEFT_INDENT));
        // System.out.println(why);
        xoff -= totalWidth(startl, endl);
        yoff += maxDescent(startl, endl) + LEADING + maxAscent(endl+1, next);
        for (int i = endl+1; i <= next; i++) {
          boxes.get(i).move(xoff, yoff);
        }
        startl = endl+1;
        endl = next;
      }
      // System.out.println("Layout fit into width " + width + " + and height " + getHeight());
    }

    float maxAscent(int s, int e) {
      float a = 0;
      for (int i = s; i <= e; i++)
        a = Math.max(a, boxes.get(i).a);
      return a;
    }

    float maxDescent(int s, int e) {
      float d = 0;
      for (int i = s; i <= e; i++)
        d = Math.max(d, boxes.get(i).d);
      return d;
    }

    float totalWidth(int s, int e) {
      float w = 0;
      for (int i = s; i <= e; i++)
        w += boxes.get(i).w;
      return w;
    }

    float getHeight() {
      if (boxes.size() == 0)
        return DEFAULT_HEIGHT;
      Box last = boxes.get(boxes.size()-1);
      return last.yy + last.d;
    }

    // String why;
    double penalty(int s, int e, float width) {
      double p = 0;
      float w = totalWidth(s, e);
      // Overly-long lines get penalized harshly.
      // Short lines get penalized using square, so that two moderate lines is
      // preferred to one shorter and one longer line.
      if (w > width) {
        p += 10000 + Math.pow((w-width)/width, 2);
        // if (why != null) why += String.format(" %f b/c overlong", p);
      } else {
        p += 10 * Math.pow((width-w)/width, 2);
        // if (why != null) why += String.format(" %f b/c too short", p);
      }
      if (e == boxes.size()-1)
        return p; // no other penalties if this is the last line
      // Breaking after an operator is worse than just before it, and
      // penalties get somewhat worse at higher depths or with higher-precedence
      // operators.
      Box left = boxes.get(e);
      Box right = boxes.get(e+1);
      if (left instanceof ParenBox || right instanceof ParenBox) { // break near paren
        p += (150 * left.depth);
        // if (why != null) why += String.format(" %f b/c near paren", p);
      } else if (left instanceof StringBox) { // break just after an operator
        p += (100 + ((StringBox)left).precedence) * left.depth;
        // if (why != null) why += String.format(" %f b/c after op", p);
      } else if (right instanceof StringBox) { // break just before an operator
        p += (50 + ((StringBox)right).precedence) * left.depth;
        // if (why != null) why += String.format(" %f b/c before op", p);
      }
      return p;
    }

  }

  static void debugShade(Graphics2D g, Box b, Color[] colors, int idx) {
    Color c = g.getColor();
    g.setColor(colors[idx % colors.length]);
    g.fill(new Rectangle2D.Float(b.xx, b.yy-b.a, b.w, b.h));
    g.setColor(g.getColor().darker());
    g.draw(new Rectangle2D.Float(b.xx, b.yy-b.a, b.w, b.h));
    g.setColor(c);
  }

  static final int DEFAULT_HEIGHT = 35;
  static final int LEADING = 6; // gap between lines
  static final int LEFT_MARGIN = 15; // margin at left of every line
  static final int LEFT_INDENT = 25; // extra margin for continuation lines

  int width = 100;
  LineBreaker lines = new LineBreaker();

  public ExpressionRenderer() { }

  public void setWidth(int w) {
    width = w - LEFT_MARGIN;
  }

  public void setExpression(String name, Expression expr) {
    expr = Expressions.eq(Expressions.variable(name), expr);
    Box top = expr.visit(new BoxMaker());
    // for now, just lay things out on one line, don't break any boxes
    lines.layout(top, 0, top.a);
    lines.fitToWidth(width);
  }

  public void setError(String name, String msg) {
    ArrayList<Box> boxes = new ArrayList<>();
    boxes.add(new VarBox(name, 0));
    boxes.add(new StringBox(" = ", 0));
    boxes.add(new StringBox("{", 0));
    for (String word : msg.split("\\s+"))
      boxes.add(new StringBox(" " + word, 1));
    boxes.add(new StringBox(" }", 0));
    Box[] a = boxes.toArray(new Box[boxes.size()]);
    Box top = new BoundingBox(0, a);
    lines.layout(top, 0, top.a);
    lines.fitToWidth(width);
  }

  public int getExpressionHeight() {
    return (int)Math.ceil(lines.getHeight());
  }

  @Override
  public void paintComponent(Graphics g) {
    /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    super.paintComponent(g);

    AffineTransform xform = g2.getTransform();
    g2.translate(LEFT_MARGIN, (getHeight() - lines.getHeight())/2);
    lines.paint(g2);
    g2.setTransform(xform);
  }

}
