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

  private static class ExpressionData {
    String text;
    final ArrayList<Range> nots = new ArrayList<Range>();
    final ArrayList<Range> subscripts = new ArrayList<Range>();
    int[] badness;

    ExpressionData(Expression expr) {
      if (expr == null) {
        text = "";
        badness = new int[0];
      } else {
        computeText(expr);
        computeBadnesses();
      }
    }

    private void computeBadnesses() {
      badness = new int[text.length() + 1];
      badness[text.length()] = 0;
      if (text.length() == 0)
        return;

      badness[0] = Integer.MAX_VALUE;
      Range curNot = nots.isEmpty() ? null : (Range) nots.get(0);
      int curNotIndex = 0;
      char prev = text.charAt(0);
      for (int i = 1; i < text.length(); i++) {
        // invariant: curNot.stopIndex >= i (and is first such),
        // or curNot == null if none such exists
        char cur = text.charAt(i);
        if (cur == ' ') {
          badness[i] = BADNESS_BEFORE_SPACE;
        } else if (Character.isJavaIdentifierPart(cur)) {
          if (Character.isJavaIdentifierPart(prev)) {
            badness[i] = BADNESS_IDENT_BREAK;
          } else {
            badness[i] = BADNESS_BEFORE_AND;
          }
        } else if (cur == '+') {
          badness[i] = BADNESS_BEFORE_OR;
        } else if (cur == '^') {
          badness[i] = BADNESS_BEFORE_XOR;
        } else if (cur == ')') {
          badness[i] = BADNESS_BEFORE_SPACE;
        } else { // cur == '('
          badness[i] = BADNESS_BEFORE_AND;
        }

        while (curNot != null && curNot.stopIndex <= i) {
          ++curNotIndex;
          curNot = (curNotIndex >= nots.size() ? null
              : (Range) nots.get(curNotIndex));
        }

        if (curNot != null && badness[i] < BADNESS_IDENT_BREAK) {
          int depth = 0;
          Range nd = curNot;
          int ndi = curNotIndex;
          while (nd != null && nd.startIndex < i) {
            if (nd.stopIndex > i)
              ++depth;
            ++ndi;
            nd = ndi < nots.size() ? (Range) nots.get(ndi) : null;
          }
          if (depth > 0) {
            badness[i] += BADNESS_NOT_BREAK + (depth - 1)
                * BADNESS_PER_NOT_BREAK;
          }
        }

        prev = cur;
      }
    }

    private void computeText(Expression expr) {
      final StringBuilder text = new StringBuilder();
      expr.visit(new ExpressionVisitor<Object>() {
        private Object binary(Expression a, Expression b, int level,
            String op) {
          if (a.getPrecedence() < level) {
            text.append("(");
            a.visit(this);
            text.append(")");
          } else {
            a.visit(this);
          }
          text.append(op);
          if (b.getPrecedence() < level) {
            text.append("(");
            b.visit(this);
            text.append(")");
          } else {
            b.visit(this);
          }
          return null;
        }

        public Object visitAnd(Expression a, Expression b) {
          return binary(a, b, Expression.AND_LEVEL, " ");
        }

        public Object visitConstant(int value) {
          text.append("" + Integer.toString(value, 16));
          return null;
        }

        public Object visitNot(Expression a) {
          Range notData = new Range();
          notData.startIndex = text.length();
          nots.add(notData);
          a.visit(this);
          notData.stopIndex = text.length();
          return null;
        }

        public Object visitOr(Expression a, Expression b) {
          return binary(a, b, Expression.OR_LEVEL, " + ");
        }

        public Object visitVariable(String name) {
          int i = name.indexOf(':');
          if (i >= 0) {
            String sub = name.substring(i+1);
            name = name.substring(0, i);
            text.append(name);
            Range subscript = new Range();
            subscript.startIndex = text.length();
            text.append(sub);
            subscript.stopIndex = text.length();
            subscripts.add(subscript);
          } else {
            text.append(name);
          }
          return null;
        }

        public Object visitXor(Expression a, Expression b) {
          return binary(a, b, Expression.XOR_LEVEL, " \u2295 "); // oplus
        }

        public Object visitEq(Expression a, Expression b) {
          return binary(a, b, Expression.EQ_LEVEL, " = ");
        }
      });
      this.text = text.toString();
    }
  }

  private static class Range {
    int startIndex;
    int stopIndex;
    int depth;
  }

  private static class RenderData {
    ExpressionData exprData;
    int prefWidth;
    int width;
    int height;
    String[] lineText;
    ArrayList<ArrayList<Range>> lineNots;
    ArrayList<ArrayList<Range>> lineSubscripts;
    int[] lineY;

    AttributedString[] lineStyled;
    int[][] notStarts;
    int[][] notStops;

    RenderData(ExpressionData exprData, int width, FontMetrics fm) {
      this.exprData = exprData;
      this.width = width;
      height = MINIMUM_HEIGHT;

      if (fm == null) {
        lineStyled = null;
        lineText = new String[] { exprData.text };
        lineSubscripts = new ArrayList<ArrayList<Range>>();
        lineSubscripts.add(exprData.subscripts);
        lineNots = new ArrayList<ArrayList<Range>>();
        lineNots.add(exprData.nots);
        computeNotDepths();
        lineY = new int[] { MINIMUM_HEIGHT };
      } else {
        if (exprData.text.length() == 0) {
          lineStyled = null;
          lineText = new String[] { S.get("expressionEmpty") };
          lineSubscripts = new ArrayList<ArrayList<Range>>();
          lineSubscripts.add(new ArrayList<Range>());
          lineNots = new ArrayList<ArrayList<Range>>();
          lineNots.add(new ArrayList<Range>());
        } else {
          computeLineText(fm);
          lineSubscripts = computeLineAttribs(exprData.subscripts);
          lineNots = computeLineAttribs(exprData.nots);
          computeNotDepths();
        }
        computeLineY(fm);
        prefWidth = lineText.length > 1 ? width : fm.stringWidth(lineText[0]);
      }
    }

    private ArrayList<ArrayList<Range>> computeLineAttribs(ArrayList<Range> attribs) {
      ArrayList<ArrayList<Range>> attrs = new ArrayList<ArrayList<Range>>();
      for (int i = 0; i < lineText.length; i++) {
        attrs.add(new ArrayList<Range>());
      }
      for (Range nd : attribs) {
        int pos = 0;
        for (int j = 0; j < attrs.size() && pos < nd.stopIndex; j++) {
          String line = lineText[j];
          int nextPos = pos + line.length();
          if (nextPos > nd.startIndex) {
            Range toAdd = new Range();
            toAdd.startIndex = Math.max(pos, nd.startIndex) - pos;
            toAdd.stopIndex = Math.min(nextPos, nd.stopIndex) - pos;
            attrs.get(j).add(toAdd);
          }
          pos = nextPos;
        }
      }
      return attrs;
    }

    private void computeLineText(FontMetrics fm) {
      String text = exprData.text;
      int[] badness = exprData.badness;

      if (fm.stringWidth(text) <= width) {
        lineStyled = null;
        lineText = new String[] { text };
        return;
      }

      int startPos = 0;
      ArrayList<String> lines = new ArrayList<String>();
      while (startPos < text.length()) {
        int stopPos = startPos + 1;
        String bestLine = text.substring(startPos, stopPos);
        if (stopPos >= text.length()) {
          lines.add(bestLine);
          break;
        }
        int bestStopPos = stopPos;
        int lineWidth = fm.stringWidth(bestLine);
        int bestBadness = badness[stopPos] + (width - lineWidth) * BADNESS_PER_PIXEL;
        while (stopPos < text.length()) {
          ++stopPos;
          String line = text.substring(startPos, stopPos);
          lineWidth = fm.stringWidth(line);
          if (lineWidth > width)
            break;

          int lineBadness = badness[stopPos] + (width - lineWidth) * BADNESS_PER_PIXEL;
          if (lineBadness < bestBadness) {
            bestBadness = lineBadness;
            bestStopPos = stopPos;
            bestLine = line;
          }
        }
        lines.add(bestLine);
        startPos = bestStopPos;
      }
      lineStyled = null;
      lineText = lines.toArray(new String[lines.size()]);
    }

    private void computeLineY(FontMetrics fm) {
      lineY = new int[lineNots.size()];
      int curY = 0;
      for (int i = 0; i < lineY.length; i++) {
        int maxDepth = -1;
        ArrayList<Range> nots = lineNots.get(i);
        for (Range nd : nots) {
          if (nd.depth > maxDepth)
            maxDepth = nd.depth;
        }
        lineY[i] = curY + maxDepth * NOT_SEP;
        curY = lineY[i] + fm.getHeight() + EXTRA_LEADING;
      }
      height = Math.max(MINIMUM_HEIGHT, curY - fm.getLeading() - EXTRA_LEADING);
    }

    private void computeNotDepths() {
      for (ArrayList<Range> nots : lineNots) {
        int n = nots.size();
        int[] stack = new int[n];
        for (int i = 0; i < nots.size(); i++) {
          Range nd = nots.get(i);
          int depth = 0;
          int top = 0;
          stack[0] = nd.stopIndex;
          for (int j = i + 1; j < nots.size(); j++) {
            Range nd2 = nots.get(j);
            if (nd2.startIndex >= nd.stopIndex)
              break;
            while (nd2.startIndex >= stack[top])
              top--;
            ++top;
            stack[top] = nd2.stopIndex;
            if (top > depth)
              depth = top;
          }
          nd.depth = depth;
        }
      }
    }

    public Dimension getPreferredSize() {
      return new Dimension(10, height);
    }

    private AttributedString style(String s, int end, ArrayList<Range> subs) {
      AttributedString as = new AttributedString(s.substring(0, end));
      as.addAttribute(TextAttribute.FAMILY, EXPR_FONT_FAMILY);
      as.addAttribute(TextAttribute.SIZE, EXPR_FONT_SIZE);
      as.addAttribute(TextAttribute.POSTURE, EXPR_FONT_POSTURE);
      for (Range r : subs) {
        if (r.stopIndex <= end)
          as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, r.startIndex, r.stopIndex);
      }
      return as;
    }

    private int getWidth(FontRenderContext ctx, String s, int end, ArrayList<Range> subs) {
      // TextLayout seems to exclude trailing whitespace, so we need to
      // account for it here.
      if (end == 0)
        return 0;
      AttributedString as = new AttributedString(s.substring(0, end) + ".");
      for (Range r : subs) {
        if (r.stopIndex <= end)
          as.addAttribute(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, r.startIndex, r.stopIndex);
      }
      LineBreakMeasurer m = new LineBreakMeasurer(as.getIterator(), ctx);
      TextLayout layout = m.nextLayout(Integer.MAX_VALUE);
      int w = (int)layout.getBounds().getWidth();
      as = new AttributedString(".");
      m = new LineBreakMeasurer(as.getIterator(), ctx);
      layout = m.nextLayout(Integer.MAX_VALUE);
      int periodWidth = (int)layout.getBounds().getWidth();
      return w - periodWidth;
    }

    public static final String EXPR_FONT_FAMILY = "Serif";
    public static final int EXPR_FONT_SIZE = 12;
    public static final int EXPR_FONT_STYLE = Font.ITALIC;
    public static final Float EXPR_FONT_POSTURE = TextAttribute.POSTURE_OBLIQUE;

    public static final Font EXPR_FONT = new Font(
        EXPR_FONT_FAMILY, EXPR_FONT_STYLE, EXPR_FONT_SIZE);

    public void paint(Graphics g, int x, int y) {
      g.setFont(EXPR_FONT);
      FontMetrics fm = g.getFontMetrics();
      if (lineStyled == null) {
        FontRenderContext ctx = ((Graphics2D)g).getFontRenderContext();
        lineStyled = new AttributedString[lineText.length];
        notStarts = new int[lineText.length][];
        notStops = new int[lineText.length][];
        for (int i = 0; i < lineText.length; i++) {
          String line = lineText[i];
          ArrayList<Range> nots = lineNots.get(i);
          ArrayList<Range> subs = lineSubscripts.get(i);
          notStarts[i] = new int[nots.size()];
          notStops[i] = new int[nots.size()];
          for (int j = 0; j < nots.size(); j++) {
            Range not = nots.get(j);
            notStarts[i][j] = getWidth(ctx, line, not.startIndex, subs);
            notStops[i][j] = getWidth(ctx, line, not.stopIndex, subs);
          }
          lineStyled[i] = style(line, line.length(), subs);
        }
      }
      for (int i = 0; i < lineStyled.length; i++) {
        AttributedString as = lineStyled[i];
        g.drawString(as.getIterator(), x, y + lineY[i] + fm.getAscent());

        ArrayList<Range> nots = lineNots.get(i);
        for (int j = 0; j < nots.size(); j++) {
          Range nd = nots.get(j);
          int notY = y + lineY[i] - nd.depth * NOT_SEP;
          int startX = x + notStarts[i][j];
          int stopX = x + notStops[i][j];
          g.drawLine(startX, notY, stopX, notY);
        }
      }
    }
  }

  private static final long serialVersionUID = 1L;
  private static final int BADNESS_IDENT_BREAK = 10000;
  private static final int BADNESS_BEFORE_SPACE = 500;
  private static final int BADNESS_BEFORE_AND = 50;
  private static final int BADNESS_BEFORE_XOR = 30;

  private static final int BADNESS_BEFORE_OR = 0;
  private static final int BADNESS_NOT_BREAK = 100;
  private static final int BADNESS_PER_NOT_BREAK = 30;

  private static final int BADNESS_PER_PIXEL = 1;

  private static final int NOT_SEP = 3;
  private static final int EXTRA_LEADING = 4;

  private static final int MINIMUM_HEIGHT = 25;
  private static final int LEFT_MARGIN = 10;

  Expression expr = null;

  public ExpressionRenderer() { }

  public void setExpression(String name, Expression expr) {
    this.expr = Expressions.eq(Expressions.variable(name), expr);
  }

  public int getExpressionHeight(int w) {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    Graphics gr = img.getGraphics().create();
    if (!(gr instanceof Graphics2D))
      return 50; // ??
    Graphics2D g = (Graphics2D)gr;
    g.setFont(RenderData.EXPR_FONT);
    FontMetrics fm = g.getFontMetrics();
    ExpressionData exprData = new ExpressionData(expr);
    RenderData renderData = new RenderData(exprData, w - LEFT_MARGIN, fm);
    return renderData.height;
  }

  @Override
  public void paintComponent(Graphics g) {
    g.setFont(RenderData.EXPR_FONT);
    FontMetrics fm = g.getFontMetrics();

    ExpressionData exprData = new ExpressionData(expr);
    RenderData renderData = new RenderData(exprData, getWidth(), fm);
    // System.out.println("width = " + getWidth() + " height = " + getHeight());
    // System.out.println(expr + " --> " + renderData.getPreferredSize());

    /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    super.paintComponent(g);

    if (renderData != null) {
      int x = LEFT_MARGIN; // Math.max(0, (getWidth() - renderData.prefWidth) / 2);
      int y = Math.max(0, (getHeight() - renderData.height) / 2);
      renderData.paint(g, x, y);
    }
  }

}
