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
package com.cburch.logisim.gui.chrono;
import static com.cburch.logisim.gui.chrono.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.gui.log.ComponentIcon;
import com.cburch.logisim.gui.log.SelectionItem;
import com.cburch.logisim.gui.log.SelectionItems;
import com.cburch.logisim.util.Icons;

// Left panel containing signal names
public class LeftPanel extends JPanel {

  private class SignalTableModel extends AbstractTableModel {
    @Override
    public String getColumnName(int col) {
      return (col == 0 ? S.get("SignalName") : S.get("SignalValue"));
    }
    @Override
    public int getColumnCount() { return 2; }
    @Override
    public Class getColumnClass(int col) {
      return col == 0 ? SelectionItem.class : ChronoData.Signal.class; }
    @Override
    public int getRowCount() { return data.getSignalCount(); }
    @Override
    public Object getValueAt(int row, int col) {
      return col  == 0 ? data.getSignal(row).info : data.getSignal(row);
    }
    @Override
    public boolean isCellEditable(int row, int col) {
      return col == 1;
    }
  }

  private static final Border rowInsets =
      BorderFactory.createMatteBorder(Waveform.GAP, 0, Waveform.GAP, 0, Color.WHITE);

  private class SignalRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof SelectionItem))
        return null;
      Component ret = super.getTableCellRendererComponent(table,
          value, false, false, row, col);
      if (ret instanceof JLabel && value instanceof SelectionItem) {
        JLabel label = (JLabel)ret;
        label.setBorder(rowInsets);
        SelectionItem item = (SelectionItem)value;
        label.setBackground(rowColor(item, isSelected));
        label.setIcon(new ComponentIcon(item.getComponent()));
      }
      return ret;
    }
  }

  private class ValueRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int col) {
      if (!(value instanceof ChronoData.Signal))
        return null;
      ChronoData.Signal s = (ChronoData.Signal)value;
      String txt = s.getFormattedValue(chronoPanel.getRightPanel().getCurrentTick());
      Component ret = super.getTableCellRendererComponent(table,
          txt, false, false, row, col);
      if (ret instanceof JLabel) {
        JLabel label = (JLabel) ret;
        label.setBorder(rowInsets);
        label.setIcon(null);
        label.setBackground(rowColor(s.info, isSelected));
        label.setHorizontalAlignment(JLabel.CENTER);
      }
      return ret;
    }
  }

  Color rowColor(SelectionItem item, boolean isSelected) {
    if (isSelected)
      return table.getSelectionBackground();
    ChronoData.Signal spotlight = data.getSpotlight();
    if (spotlight != null && spotlight.info == item)
      return Waveform.LIGHTPINK;
    return table.getBackground();
  }

  class SignalEditor extends AbstractCellEditor implements TableCellEditor {
    JPanel panel = new JPanel();
    JLabel label = new JLabel();
    JButton button = new JButton(Icons.getIcon("dropdown.png"));
    JPopupMenu popup = new JPopupMenu("Options");
    ChronoData.Signal signal;
    List<ChronoData.Signal> signals;

    public SignalEditor() {
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      button.setFont(button.getFont().deriveFont(9.0f));

      for (RadixOption r : RadixOption.OPTIONS) {
        JMenuItem m = new JMenuItem(r.toDisplayString()); 
        popup.add(m);
        m.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {              
            for (ChronoData.Signal s : signals)
              s.info.setRadix(r);
            LeftPanel.this.repaint();
          }
        });
      }

      popup.addSeparator();
      JMenuItem m = new JMenuItem("Delete");
      popup.add(m);
      m.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {              
          cancelCellEditing();
          removeSelected();
        }
      });

      button.setMargin(new Insets(0, 0, 0, 0));
      button.setHorizontalTextPosition(SwingConstants.LEFT);
      button.setText("Options");
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          popup.show(panel, button.getX(), button.getY() + button.getHeight());
        }
      });
      button.setMinimumSize(button.getPreferredSize());

      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setAlignmentX(0.0f);
      label.setAlignmentY(0.5f);
      button.setAlignmentX(1.0f);
      button.setAlignmentY(0.5f);
      panel.add(label);
      panel.add(button);
    }

    @Override
    public Object getCellEditorValue() {
      return signal;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int col) {
      int margin = table.getColumnModel().getColumnMargin();
      label.setBorder(BorderFactory.createEmptyBorder(0, margin, 0, margin));

      int w = table.getColumnModel().getTotalColumnWidth();
      int h = table.getRowHeight() - 2 * Waveform.GAP;
      Dimension d = new Dimension(w, h);
      label.setMinimumSize(new Dimension(10, d.height));
      label.setPreferredSize(new Dimension(d.width - button.getWidth(), d.height));
      label.setMaximumSize(new Dimension(d.width - button.getWidth(), d.height));

      signal = (ChronoData.Signal)value;
      signals = getSelectedValuesList();

      panel.setBackground(rowColor(signal.info, true));

      if (!signals.contains(signal)) {
        signals.clear();
        signals.add(signal);
      }
      label.setIcon(new ComponentIcon(signal.info.getComponent()));
      label.setText(signal.info.toString() + " [" + signal.info.getRadix().toDisplayString() + "]");
      //width.setSelectedItem(item.getRadix());
      return panel;
    }

     @Override
     public boolean stopCellEditing() {
       super.stopCellEditing();
       return true;
     }

     @Override
     public boolean isCellEditable(EventObject e) {
       return true;
     }
  }


	private ChronoPanel chronoPanel;
  private ChronoData data;
	private JTable table;
	private SignalTableModel tableModel;

	public LeftPanel(ChronoPanel chronoPanel) {
		this.chronoPanel = chronoPanel;
    data = chronoPanel.getChronoData();

		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

    tableModel = new SignalTableModel();
		table = new JTable(tableModel);
    table.setShowGrid(false);
    table.setDefaultRenderer(SelectionItem.class, new SignalRenderer());
    table.setDefaultRenderer(ChronoData.Signal.class, new ValueRenderer());
    table.setDefaultEditor(ChronoData.Signal.class, new SignalEditor());
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    table.setColumnSelectionAllowed(false);
    table.setRowSelectionAllowed(true);

		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "tick");
		table.getActionMap().put("tick", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// todo
			}
		});
		table.addKeyListener(chronoPanel);
    // highlight on mouse over
		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				if (row >= 0 && e.getComponent() instanceof JTable) {
					chronoPanel.mouseEntered(data.getSignal(row));
				} else {
					chronoPanel.mouseExited(null);
        }
			}
		});
    // popup on right click
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
        System.out.println("release popup");
        if (!SwingUtilities.isRightMouseButton(e))
          return;
        if (!(e.getComponent() instanceof JTable))
          return;
				int row = table.getSelectedRow();
        if (row < 0) {
          row = table.rowAtPoint(e.getPoint());
          if (row < 0)
            return;
          table.setRowSelectionInterval(row, row);
        }
        List<ChronoData.Signal> signals = getSelectedValuesList();
        PopupMenu m = new PopupMenu(chronoPanel, signals);
        m.doPop(e);
			}
		});

    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    table.setDragEnabled(true);
    table.setDropMode(DropMode.INSERT_ROWS);
    table.setTransferHandler(new SignalTransferHandler());

    InputMap inputMap = table.getInputMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
    ActionMap actionMap = table.getActionMap();
    actionMap.put("Delete", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        removeSelected();
      }
    });

    // calculate default sizes
    int nameWidth = 0, valueWidth = 0;
    TableCellRenderer render = table.getDefaultRenderer(String.class);
    int n = data.getSignalCount();
    for (int i = -1; i < n; i++) {
      String name, val;
      if (i < 0) {
        name = tableModel.getColumnName(0);
        val = tableModel.getColumnName(1);
      } else {
        ChronoData.Signal s = data.getSignal(i);
        name = s.getName();
        val = s.getFormattedMaxValue();
      }
      Component c;
      c = render.getTableCellRendererComponent(table, name, false, false, i, 0);
      nameWidth = Math.max(nameWidth, c.getPreferredSize().width);
      c = render.getTableCellRendererComponent(table, val, false, false, i, 1);
      valueWidth = Math.max(valueWidth, c.getPreferredSize().width);
    }

    table.setFillsViewportHeight(true);
		table.setRowHeight(ChronoPanel.SIGNAL_HEIGHT);
    // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumn col;

    col = table.getColumnModel().getColumn(0);
		col.setMinWidth(20);
		col.setPreferredWidth(nameWidth + 10);

    col = table.getColumnModel().getColumn(1);
		col.setMinWidth(20);
		col.setPreferredWidth(valueWidth + 10);

		JTableHeader header = table.getTableHeader();
		Dimension d = header.getPreferredSize();
		d.height = ChronoPanel.HEADER_HEIGHT;
		header.setPreferredSize(d);

		add(header, BorderLayout.NORTH);
		add(table, BorderLayout.CENTER);
	}

	public void changeSpotlight(ChronoData.Signal oldSignal, ChronoData.Signal newSignal) {
    if (oldSignal != null)
      tableModel.fireTableRowsUpdated(oldSignal.idx, oldSignal.idx);
    if (newSignal != null)
      tableModel.fireTableRowsUpdated(newSignal.idx, newSignal.idx);
    System.out.println("changed spotlight in left pane");
    //tableModel.fireTableDataChanged();
    //repaint();
	}

	public void updateSignals() {
    tableModel.fireTableDataChanged();
	}

	public void updateSignalValues() {
    for (int row = 0; row < data.getSignalCount(); row++)
      tableModel.fireTableCellUpdated(row, 1);
	}

  List<ChronoData.Signal> getSelectedValuesList() {
    ArrayList<ChronoData.Signal> signals = new ArrayList<>();
    int[] sel = table.getSelectedRows();
    for (int i : sel)
      signals.add(data.getSignal(i));
    return signals;
  }

  void removeSelected() {
    int idx = 0;
    List<ChronoData.Signal> signals = getSelectedValuesList();
    SelectionItems items = new SelectionItems();
    for (ChronoData.Signal s : signals) {
      items.add(s.info);
      idx = Math.max(idx, s.idx);
    }
    int count = chronoPanel.getSelection().remove(items);
    if (count > 0 && items.size() > 0) {
      idx = Math.min(idx+1-count, items.size()-1);
      table.setRowSelectionInterval(idx, idx);
    }
    repaint();
  }

  private class SignalTransferHandler extends TransferHandler {
    ChronoData.Signal removing = null;

    @Override
    public int getSourceActions(JComponent comp) {
      return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      removing = null;
      int idx = table.getSelectedRow();
      if (idx < 0 || idx >= data.getSignalCount())
        return null;
      removing = data.getSignal(idx);
      return removing;
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
      if (removing == null)
        return;
      ChronoData.Signal s = removing;
      removing = null;
      chronoPanel.getSelection().remove(s.idx);
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(ChronoData.Signal.dataFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      if (removing == null) {
        System.out.println("paste with no cut... maybe import name and restore waveform?");
        return false;
      }
      ChronoData.Signal s = removing;
      removing = null;
      try {
        ChronoData.Signal s2 =
            (ChronoData.Signal)support.getTransferable().getTransferData(ChronoData.Signal.dataFlavor);
        int newIdx = data.getSignalCount();
        if (support.isDrop()) {
          try {
            JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
            newIdx = Math.min(newIdx, dl.getRow());
          } catch (ClassCastException e) {
          }
        }
        if (s2 != s) {
          System.out.println("paste with wrong cut... maybe remove, then import name and restore waveform?");
          return false;
        }
        if (newIdx > s.idx)
          newIdx--;
        chronoPanel.getSelection().move(s.idx, newIdx);
        return true;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }
 
}
