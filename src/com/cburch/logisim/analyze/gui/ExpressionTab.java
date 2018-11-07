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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.AbstractCellEditor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.JTextField;
import org.jdesktop.xswingx.BuddySupport;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.analyze.model.VariableListEvent;
import com.cburch.logisim.analyze.model.VariableListListener;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.util.StringGetter;

class ExpressionTab extends AnalyzerTab {

  private AnalyzerModel model;
  private StringGetter errorMessage;
  private ExpressionTableModel tableModel;

  private JTable table = new JTable(1, 1);
  private JLabel error = new JLabel();

  private static class NamedExpression {
    String name;
    Expression expr; // can be null
    String exprString;
    String err;
    NamedExpression(String n) {
      name = n;
    }
    NamedExpression(String n, Expression e, String s) {
      name = n;
      expr = e;
      exprString = s;
    }
  }

  public class ExpressionTableModel extends AbstractTableModel
    implements VariableListListener, OutputExpressionsListener {
    NamedExpression[] listCopy;

    public ExpressionTableModel() {
      updateCopy();
      model.getOutputs().addVariableListListener(this);
      model.getOutputExpressions().addOutputExpressionsListener(this);
    }

    @Override
    public void setValueAt(Object o, int row, int column) {
      if (o == null || !(o instanceof NamedExpression))
        return;
      NamedExpression e = (NamedExpression)o;
      if (e.expr != null)
        model.getOutputExpressions().setExpression(e.name, e.expr, e.exprString);
    }

    @Override
    public Object getValueAt(int row, int col) {
      return listCopy[row];
    }
    
    @Override
    public boolean isCellEditable(int row, int column) { return true; }
    @Override
    public int getColumnCount() { return 1; };
    @Override
    public String getColumnName(int column) { return ""; }
    @Override
    public Class<?> getColumnClass(int columnIndex) { return NamedExpression.class; }
    @Override
    public int getRowCount() { return listCopy.length; }
   
    @Override
    public void expressionChanged(OutputExpressionsEvent event) {
      if (event.getType() == OutputExpressionsEvent.OUTPUT_EXPRESSION) {
        String name = event.getVariable();
        int idx = -1;
        for (NamedExpression e : listCopy) {
          idx++;
          if (e.name.equals(name)) {
            try {
              e.expr = model.getOutputExpressions().getExpression(name);
              e.err = null;
            } catch (Exception ex) {
              e.expr = null;
              e.err = ex.getMessage();
            }
            fireTableRowsUpdated(idx, idx);
            break;
          }
        }
      }
    }

    @Override
    public void listChanged(VariableListEvent event) {
      int oldSize = listCopy.length;
      updateCopy();
      Integer idx = event.getIndex();
      switch (event.getType()) {
      case VariableListEvent.ALL_REPLACED:
        fireTableDataChanged();
        return;
      case VariableListEvent.ADD:
        fireTableRowsInserted(idx, idx);
        return;
      case VariableListEvent.REMOVE:
        fireTableRowsDeleted(idx, idx);
        return;
      case VariableListEvent.MOVE:
        fireTableDataChanged();
        return;
      case VariableListEvent.REPLACE:
        fireTableRowsUpdated(idx, idx);
        return;
      }
    }

    void update() {
      updateCopy();
      fireTableDataChanged();
    }

    private ExpressionRenderer prettyView = new ExpressionRenderer();
    void updateCopy() {
      VariableList outputs = model.getOutputs();
      int n = outputs.bits.size();
      listCopy = new NamedExpression[n];
      int i = -1;
      for (String name : outputs.bits) {
        i++;
        listCopy[i] = new NamedExpression(name);
        try {
          listCopy[i].expr = model.getOutputExpressions().getExpression(name);
        } catch (Exception e) {
          listCopy[i].err = e.getMessage();
        }
      }
      updateRowHeights();
    }
    void updateRowHeights() {
      for (int i = 0; i < listCopy.length; i++) {
        NamedExpression e = listCopy[i];
        int h = 40;
        int w = table.getColumnModel().getColumn(0).getWidth();
        if (e.expr != null) {
          prettyView.setExpression(e.name, e.expr);
          h = prettyView.getExpressionHeight(w) + 15;
        }
        if (table.getRowHeight(i) != h)
          table.setRowHeight(i, h);
      }
    }
  }

  public class ExpressionTableCellRenderer extends DefaultTableCellRenderer {

    ExpressionRenderer prettyView = new ExpressionRenderer();
    JLabel err = new JLabel();

    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      NamedExpression e = (NamedExpression)value;
      Color fg, bg;
      if (isSelected) {
        fg = table.getSelectionForeground();
        bg = table.getSelectionBackground();
      } else {
        fg = table.getForeground();
        bg = table.getBackground();
      }
      if (e.expr != null) {
        prettyView.setExpression(e.name, e.expr);
        prettyView.setForeground(fg);
        prettyView.setBackground(bg);
        return prettyView;
      } else {
        err.setText(e.name+"... "+(e.err != null ? e.err : ""));
        err.setForeground(fg);
        err.setBackground(bg);
        return err;
      }
    }

  }

  public class ExpressionEditor extends AbstractCellEditor implements TableCellEditor {

    JTextField field = new JTextField();
    JLabel label = new JLabel();
    NamedExpression oldExpr, newExpr;

    public ExpressionEditor() {
      field.setBorder(BorderFactory.createCompoundBorder(
            field.getBorder(),
            BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      BuddySupport.addLeft(label, field);
    }

    @Override
    public Object getCellEditorValue() {
      return newExpr;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
      newExpr = null;
      oldExpr = (NamedExpression)value;
      label.setText(" " + oldExpr.name + " = ");
      if (oldExpr.expr != null)
        field.setText(oldExpr.expr.toString());
      else
        field.setText("");
      return field;
    }

    @Override
    public boolean stopCellEditing() {
      if (ok()) {
        super.stopCellEditing();
        return true;
      } else {
        return false;
      }
    }

    boolean ok() {
      NamedExpression old = oldExpr;
      oldExpr = null;
      String exprString = field.getText();
      try {
        Expression expr = Parser.parse(field.getText(), model);
        setError(null);
        newExpr = new NamedExpression(old.name, expr, exprString);
        return true;
      } catch (ParserException ex) {
        setError(ex.getMessageGetter());
        field.setCaretPosition(ex.getOffset());
        field.moveCaretPosition(ex.getEndOffset());
        newExpr = null;
        return false;
      }
    }

    @Override
    public boolean isCellEditable(EventObject e) {
      if (e instanceof MouseEvent) {
        MouseEvent me = (MouseEvent) e;
        return me.getClickCount() >= 2;
      }
      if (e instanceof KeyEvent) {
        KeyEvent ke = (KeyEvent) e;
        return (ke.getKeyCode() == KeyEvent.VK_F2
            || ke.getKeyCode() == KeyEvent.VK_ENTER);
      }
      return false;
    }

  }

  public ExpressionTab(AnalyzerModel model) {
    this.model = model;

    tableModel = new ExpressionTableModel();
    table.setModel(tableModel);
    table.setShowGrid(false);
    table.setDefaultRenderer(NamedExpression.class, new ExpressionTableCellRenderer());
    table.setDefaultEditor(NamedExpression.class, new ExpressionEditor());
    table.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        tableModel.updateRowHeights();
      }
    });

    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    setLayout(gb);
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    gc.weightx = gc.weighty = 1.0;
    gc.gridx = 0;
    gc.gridy = GridBagConstraints.RELATIVE;
    gc.fill = GridBagConstraints.BOTH;
    
    JScrollPane scroll = new JScrollPane(table,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setPreferredSize(new Dimension(60, 100));
    gc.fill = GridBagConstraints.BOTH;
    gb.setConstraints(scroll, gc);
    add(scroll);
    
    gc.weighty = 0.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gb.setConstraints(error, gc);
    add(error);

    setError(null);
  }

  @Override
  void localeChanged() {
    if (errorMessage != null) {
      error.setText(errorMessage.toString());
    }
  }

  private void setError(StringGetter msg) {
    if (msg == null) {
      errorMessage = null;
      error.setText(" ");
    } else {
      errorMessage = msg;
      error.setText(msg.toString());
    }
  }

  @Override
  void updateTab() {
    tableModel.update();
  }

  @Override
  EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    public void computeEnabled() {
      boolean canEdit = table.getRowCount() > 0;
      boolean editing = table.isEditing();
      setEnabled(LogisimMenuBar.CUT, editing);
      setEnabled(LogisimMenuBar.COPY, canEdit);
      setEnabled(LogisimMenuBar.PASTE, canEdit);
      setEnabled(LogisimMenuBar.DELETE, editing);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, editing);
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }

    // todo, along with drag/drop transfer handling
    public void cut() { }
    public void copy() { }
    public void paste() { }
    public void delete() { }
    public void duplicate() { }
    public void selectAll() { }

    public void raise() { }
    public void lower() { }
    public void raiseTop() { }
    public void lowerBottom() { }

    public void addControlPoint() { }
    public void removeControlPoint() { }
  };

}
