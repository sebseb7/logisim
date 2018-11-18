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
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

// Left panel containing signal names
public class LeftPanel extends JPanel {

    private class SignalTableModel extends AbstractTableModel {
      @Override
      public int getRowCount() {
        return data.getSignalCount();
      }

      @Override
      public String getColumnName(int col) {
        if (col == 0) return "";
        if (col == 1) return S.get("SignalName");
        if (col == 2) return S.get("SignalValue");
        return null;
      }

      @Override
      public int getColumnCount() {
        return 3;
      }

      @Override
      public Class getColumnClass(int column) {
        return (column == 0) ? ImageIcon.class : String.class;
      }

      @Override
      public Object getValueAt(int row, int col) {
        ChronoData.Signal s = data.getSignal(row);
        if (col == 0)
          return s.getIcon();
        if (col == 1)
          return s.getName();
        if (col == 2)
          return s.getFormattedValue(chronoPanel.getRightPanel().getCurrentTick());
        return null;
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

    table.setColumnSelectionAllowed(false);
    table.setRowSelectionAllowed(true);

		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "tick");
		table.getActionMap().put("tick", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// todo
			}
		});
		table.addKeyListener(chronoPanel);
    // highlight on left click
		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				if (row >= 0 && e.getComponent() instanceof JTable) {
					table.clearSelection();
					table.setRowSelectionInterval(row, row);
          ChronoData.Signal s = chronoPanel.getChronoData().getSignal(row);
					chronoPanel.mouseEntered(s);
				}
			}
		});
    // popup on right click
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int row = table.getSelectedRow();
				if (row >= 0 && SwingUtilities.isRightMouseButton(e)
						&& e.getComponent() instanceof JTable) {
          ChronoData.Signal s = chronoPanel.getChronoData().getSignal(row);
					PopupMenu m = new PopupMenu(chronoPanel, s);
					m.doPop(e);
				}
			}
		});

    table.setDragEnabled(true);
    table.setDropMode(DropMode.INSERT_ROWS);
    table.setTransferHandler(new SignalTransferHandler());

    // calculate default sizes
    int nameWidth = 0, valueWidth = 0;
    TableCellRenderer render = table.getDefaultRenderer(String.class);
    int n = data.getSignalCount();
    for (int i = -1; i < n; i++) {
      String name, val;
      if (i < 0) {
        name = tableModel.getColumnName(1);
        val = tableModel.getColumnName(2);
      } else {
        ChronoData.Signal s = data.getSignal(i);
        name = s.getName();
        val = s.getFormattedMaxValue();
      }
      Component c;
      c = render.getTableCellRendererComponent(table, name, false, false, i, 1);
      nameWidth = Math.max(nameWidth, c.getPreferredSize().width);
      c = render.getTableCellRendererComponent(table, val, false, false, i, 2);
      valueWidth = Math.max(valueWidth, c.getPreferredSize().width);
    }

		table.setRowHeight(ChronoPanel.SIGNAL_HEIGHT);
    // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumn col;

		col = table.getColumnModel().getColumn(0);
    col.setMaxWidth(10);
		col.setPreferredWidth(10);
		col.setMaxWidth(10);
		col.setResizable(false);

    col = table.getColumnModel().getColumn(1);
		col.setMinWidth(20);
		col.setPreferredWidth(nameWidth + 10);

    col = table.getColumnModel().getColumn(2);
		col.setMinWidth(20);
		col.setPreferredWidth(valueWidth + 10);

		JTableHeader header = table.getTableHeader();
		Dimension d = header.getPreferredSize();
		d.height = ChronoPanel.HEADER_HEIGHT;
		header.setPreferredSize(d);

		add(header, BorderLayout.NORTH);
		add(table, BorderLayout.CENTER);
	}

	public void highlight(ChronoData.Signal s) {
		table.setRowSelectionInterval(s.idx, s.idx);
	}

	public void updateSignals() {
    tableModel.fireTableDataChanged();
	}

	public void updateSignalValues() {
    for (int row = 0; row < data.getSignalCount(); row++)
      tableModel.fireTableCellUpdated(row, 2);
	}

  // public class TableRowTransferHandler extends TransferHandler {
  //   private final DataFlavor localObjectFlavor = new ActivationDataFlavor(Integer.class, "application/x-java-Integer;class=java.lang.Integer", "Integer Row Index");
  //   private JTable           table             = null;

  //   public TableRowTransferHandler(JTable table) {
  //     this.table = table;
  //   }

  //   @Override
  //   protected Transferable createTransferable(JComponent c) {
  //     assert (c == table);
  //     return new DataHandler(new Integer(table.getSelectedRow()), localObjectFlavor.getMimeType());
  //   }

  //   @Override
  //   public boolean canImport(TransferHandler.TransferSupport info) {
  //     boolean b = info.getComponent() == table && info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
  //     table.setCursor(b ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
  //     return b;
  //   }

  //   @Override
  //   public int getSourceActions(JComponent c) {
  //     return TransferHandler.COPY_OR_MOVE;
  //   }

  //   @Override
  //   public boolean importData(TransferHandler.TransferSupport info) {
  //     JTable target = (JTable) info.getComponent();
  //     JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
  //     int index = dl.getRow();
  //     int max = table.getModel().getRowCount();
  //     if (index < 0 || index > max)
  //       index = max;
  //     target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  //     try {
  //       Integer rowFrom = (Integer) info.getTransferable().getTransferData(localObjectFlavor);
  //       if (rowFrom != -1 && rowFrom != index) {
  //         ((Reorderable)table.getModel()).reorder(rowFrom, index);
  //         if (index > rowFrom)
  //           index--;
  //         target.getSelectionModel().addSelectionInterval(index, index);
  //         return true;
  //       }
  //     } catch (Exception e) {
  //       e.printStackTrace();
  //     }
  //     return false;
  //   }

  //   @Override
  //   protected void exportDone(JComponent c, Transferable t, int act) {
  //     if ((act == TransferHandler.MOVE) || (act == TransferHandler.NONE)) {
  //       table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  //     }
  //   }

  // }

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
