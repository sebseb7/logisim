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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.OutputExpressions;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;

class MinimizedTab extends AnalyzerTab {
  @SuppressWarnings("rawtypes")
  private static class FormatModel extends AbstractListModel
    implements ComboBoxModel {
    static int getFormatIndex(int choice) {
      switch (choice) {
      case AnalyzerModel.FORMAT_PRODUCT_OF_SUMS:
        return 1;
      default:
        return 0;
      }
    }

    private static final long serialVersionUID = 1L;

    private String[] choices;
    private int selected;

    private FormatModel() {
      selected = 0;
      choices = new String[2];
      localeChanged();
    }

    public Object getElementAt(int index) {
      return choices[index];
    }

    int getSelectedFormat() {
      switch (selected) {
      case 1:
        return AnalyzerModel.FORMAT_PRODUCT_OF_SUMS;
      default:
        return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
      }
    }

    public Object getSelectedItem() {
      return choices[selected];
    }

    public int getSize() {
      return choices.length;
    }

    void localeChanged() {
      choices[0] = S.get("minimizedSumOfProducts");
      choices[1] = S.get("minimizedProductOfSums");
      fireContentsChanged(this, 0, choices.length);
    }

    public void setSelectedItem(Object value) {
      for (int i = 0; i < choices.length; i++) {
        if (choices[i].equals(value)) {
          selected = i;
        }
      }
    }
  }

  private class MyListener
    implements OutputExpressionsListener, ActionListener, ItemListener {
    public void actionPerformed(ActionEvent event) {
      String output = getCurrentVariable();
      int format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
      outputExprs.setExpression(output,
          outputExprs.getMinimalExpression(output));
    }

    public void expressionChanged(OutputExpressionsEvent event) {
      String output = getCurrentVariable();
      if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL
          && event.getVariable().equals(output)) {
        minimizedExpr.setExpression(
            output, outputExprs.getMinimalExpression(output));
        MinimizedTab.this.validate();
      }
      setAsExpr.setEnabled(output != null
          && !outputExprs.isExpressionMinimal(output));
      int format = outputExprs.getMinimizedFormat(output);
      formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    }

    public void itemStateChanged(ItemEvent event) {
      if (event.getSource() == formatChoice) {
        String output = getCurrentVariable();
        FormatModel model = (FormatModel) formatChoice.getModel();
        outputExprs.setMinimizedFormat(output,
            model.getSelectedFormat());
      } else {
        updateTab();
      }
    }
  }

  private static class ExpressionPanel extends JPanel {
    String name;
    Expression expr;
    ExpressionRenderer prettyView = new ExpressionRenderer();
    Color selColor;
    boolean selected;
    int lastWidth = -1;
    Rectangle exprBounds;
    static final int BW = 1, MARGIN = 9;
    static final int VERTICAL_PAD = 20;

    ExpressionPanel() {
      selColor = UIManager.getDefaults().getColor("List.selectionBackground");

      setFocusable(true);
      prettyView.setBorder(BorderFactory.createLineBorder(Color.BLACK, BW));
      // prettyView.setFocusable(true);
      // setBackground(Color.WHITE);
      // prettyView.setBackground(Color.WHITE);
      prettyView.setCentered(true);
      addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          if (lastWidth != getWidth())
            update();
        }
      });
      FocusListener f = new FocusListener() {
        public void focusGained(FocusEvent e) {
          selected = true;
          repaint();
        }
        public void focusLost(FocusEvent e) {
          selected = false;
          repaint();
        }
      };
      addFocusListener(f);
      prettyView.addFocusListener(f);
      MouseAdapter m = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          if (exprBounds != null && exprBounds.contains(e.getPoint()))
            requestFocusInWindow();
        }
      };
      addMouseListener(m);
    }
    public void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D)g;
      super.paintComponent(g2);
      if (expr != null) {
        AffineTransform xform = g2.getTransform();
        g2.translate(prettyView.getX(), prettyView.getY());
        prettyView.setBackground(selected ? selColor : Color.WHITE);
        prettyView.paintComponent(g2);
        g2.setTransform(xform);
      }
    }
    void update() {
      Dimension d = getSize();
      lastWidth = d.width;

      prettyView.setExpressionWidth(d.width - 2*BW);
      if (expr != null)
        prettyView.setExpression(name, expr);

      // make expr take up only what it needs, plus space for border,
      // limited only by our own size
      prettyView.setSize(d);
      Rectangle r = prettyView.getExpressionBounds();
      r.grow(BW, BW+MARGIN);
      Rectangle area = new Rectangle(0, 0, d.width, d.height);
      exprBounds = r.intersection(area);
      exprBounds.grow(-2, -2);
      prettyView.setBounds(exprBounds);

      // make our preferred size tall enough, but narrower than necessary to
      // allow window to shrink
      d.width = 100;
      d.height = r.height;
      setPreferredSize(d);

      invalidate();
      repaint();
    }
    void setExpression(String name, Expression expr) {
      this.name = name;
      this.expr = expr;
      update();
    }

  }

  private static final long serialVersionUID = 1L;

  private OutputSelector selector;
  private KarnaughMapPanel karnaughMap;
  private JLabel formatLabel = new JLabel();
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private JComboBox formatChoice = new JComboBox<>(new FormatModel());
  private ExpressionPanel minimizedExpr = new ExpressionPanel();
  private ExpressionTab.NotationSelector notation;
  private JButton setAsExpr = new JButton();

  private MyListener myListener = new MyListener();
  private AnalyzerModel model;
  private OutputExpressions outputExprs;

  public MinimizedTab(AnalyzerModel model) {
    this.model = model;
    this.outputExprs = model.getOutputExpressions();
    outputExprs.addOutputExpressionsListener(myListener);

    notation = new ExpressionTab.NotationSelector(
        minimizedExpr.prettyView, BoxLayout.Y_AXIS) {
      @Override
      void updated() {
        minimizedExpr.update();
      }
    };

    selector = new OutputSelector(model);
    selector.addItemListener(myListener);
    karnaughMap = new KarnaughMapPanel(model);
    karnaughMap.addMouseListener(new TruthTableMouseListener());
    setAsExpr.addActionListener(myListener);
    formatChoice.addItemListener(myListener);

    JPanel buttons = new JPanel(new GridLayout(1, 1));
    buttons.add(setAsExpr);

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);

    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.weightx = 1.0;
    gc.weighty = 0.0;

    JPanel outPanel = selector.createPanel();
    JPanel fmtPanel = new JPanel();
    fmtPanel.setLayout(new BoxLayout(fmtPanel, BoxLayout.Y_AXIS));
    formatLabel.setAlignmentX(0.0f);
    formatChoice.setAlignmentX(0.0f);
    fmtPanel.add(formatLabel);
    fmtPanel.add(formatChoice);
    outPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    fmtPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    notation.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    JPanel selectors = new JPanel();
    selectors.setLayout(new BoxLayout(selectors, BoxLayout.X_AXIS));
    selectors.add(outPanel);
    selectors.add(fmtPanel);
    selectors.add(notation);

    gc.fill = GridBagConstraints.NONE;
    gc.anchor = GridBagConstraints.CENTER;
    gb.setConstraints(selectors, gc);
    add(selectors);

    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.anchor = GridBagConstraints.CENTER;
    gb.setConstraints(karnaughMap, gc);
    add(karnaughMap);

    gc.fill = GridBagConstraints.HORIZONTAL;
    Insets oldInsets = gc.insets;
    gc.insets = new Insets(10, 5, 10, 5);
    gb.setConstraints(minimizedExpr, gc);
    add(minimizedExpr);
    gc.insets = oldInsets;

    gc.weightx = 0.0;
    gc.fill = GridBagConstraints.NONE;
    gb.setConstraints(buttons, gc);
    add(buttons);

    String selected = selector.getSelectedOutput();
    setAsExpr.setEnabled(selected != null
        && !outputExprs.isExpressionMinimal(selected));
  }

  private String getCurrentVariable() {
    return selector.getSelectedOutput();
  }

  @Override
  void localeChanged() {
    selector.localeChanged();
    karnaughMap.localeChanged();
    notation.localeChanged();
    setAsExpr.setText(S.get("minimizedSetButton"));
    formatLabel.setText(S.get("minimizedFormat"));
    ((FormatModel) formatChoice.getModel()).localeChanged();
  }

  @Override
  void updateTab() {
    final String output = getCurrentVariable();
    if (model.getTruthTable().getRowCount() > 4096) {
      (new Analyzer.PleaseWait<Void>("Calculating Expression", this) {
        @Override
        public Void doInBackground() throws Exception {
          model.getOutputExpressions().getExpression(output);
          return null;
        }
      }).get();
    }
    karnaughMap.setOutput(output);
    int format = outputExprs.getMinimizedFormat(output);
    formatChoice.setSelectedIndex(FormatModel.getFormatIndex(format));
    minimizedExpr.setExpression(
        output, outputExprs.getMinimalExpression(output));
    setAsExpr.setEnabled(output != null
        && !outputExprs.isExpressionMinimal(output));
  }

  
  public void editCopy() {
  }
  public void editPaste() {
  }
  public void editDelete() {
  }
  public void editSelectAll() {
  }
}
