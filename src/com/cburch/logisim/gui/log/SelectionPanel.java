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
package com.cburch.logisim.gui.log;
import static com.cburch.logisim.gui.log.Strings.S;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// import javax.swing.event.TableSelectionEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.cburch.logisim.util.Icons;
import com.hepia.logisim.chronodata.TimelineParam;

class SelectionPanel extends LogPanel {

  // private class Listener extends MouseAdapter {

    // public void actionPerformed(ActionEvent event) {
    //   Object src = event.getSource();
    //   if (src == addTool) {
    //     doAdd(selector.getSelectedItems());
    //   } else if (src == changeBase) {
    //     SelectionItem sel = (SelectionItem) list.getSelectedValue();
    //     if (sel != null) {
    //       int radix = sel.getRadix();
    //       switch (radix) {
    //       case 2:
    //         sel.setRadix(10);
    //         break;
    //       case 10:
    //         sel.setRadix(16);
    //         break;
    //       default:
    //         sel.setRadix(2);
    //       }
    //     }
    //   } else if (src == moveUp) {
    //     doMove(-1);
    //   } else if (src == moveDown) {
    //     doMove(1);
    //   } else if (src == remove) {
    //     Selection sel = getSelection();
    //     Object[] toRemove = list.getSelectedValuesList().toArray();
    //     boolean changed = false;
    //     for (int i = 0; i < toRemove.length; i++) {
    //       int index = sel.indexOf((SelectionItem) toRemove[i]);
    //       if (index >= 0) {
    //         sel.remove(index);
    //         changed = true;
    //       }
    //     }
    //     if (changed) {
    //       list.clearSelection();
    //     }
    //   }
    // }

    // private void doAdd(List<SelectionItem> selectedItems) {
    //   if (selectedItems != null && selectedItems.size() > 0) {
    //     SelectionItem last = null;
    //     for (SelectionItem item : selectedItems) {
    //       if (!getSelection().contains(item)) {
    //         getSelection().add(item);
    //         last = item;
    //       }
    //     }
    //     list.setSelectedValue(last, true);
    //   }
    // }

    // private void doMove(int delta) {
    //   Selection sel = getSelection();
    //   int oldIndex = list.getSelectedIndex();
    //   int newIndex = oldIndex + delta;
    //   if (oldIndex >= 0 && newIndex >= 0 && newIndex < sel.size()) {
    //     sel.move(oldIndex, newIndex);
    //     list.setSelectedIndex(newIndex);
    //   }
    // }

    // @Override
    // public void mouseClicked(MouseEvent e) {
    //   if (e.getClickCount() == 2) {
    //     TreePath path = selector.getPathForLocation(e.getX(), e.getY());
    //     if (path != null && listener != null) {
    //       doAdd(selector.getSelectedItems());
    //     }
    //   }
    // }

    // public void valueChanged(ListSelectionEvent event) { }

    // public void valueChanged(TreeSelectionEvent event) { }
  // }

  // private Listener listener = new Listener();
  private ComponentSelector selector;
  private SelectionList list;
  private JLabel selectDesc, exploreLabel, listLabel;

  public SelectionPanel(LogFrame window) {
    super(window);
    selector = new ComponentSelector(getModel());
    list = new SelectionList();
    list.setSelection(getSelection());

    // selector.addMouseListener(listener);
    // selector.addTreeSelectionListener(listener);
    // list.addTableSelectionListener(listener);

    JScrollPane explorerPane = new JScrollPane(selector,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JScrollPane listPane = new JScrollPane(list,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    // listPane.setTransferHandler(list.getTransferHandler());

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    setLayout(gridbag);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = gbc.weighty = 0.0;
    gbc.insets = new Insets(15, 10, 0, 10);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 3;
    selectDesc = new JLabel();
    gridbag.setConstraints(selectDesc, gbc);
    add(selectDesc);
    gbc.gridwidth = 1;

    gbc.gridx = 0;
    gbc.gridy = 1;
    exploreLabel = new JLabel();
    gridbag.setConstraints(exploreLabel, gbc);
    add(exploreLabel);

    gbc.gridx = 2;
    gbc.gridy = 1;
    listLabel = new JLabel();
    gridbag.setConstraints(listLabel, gbc);
    add(listLabel);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.gridx = 0;
    gbc.gridy = 2;
    gridbag.setConstraints(explorerPane, gbc);
    add(explorerPane);
    explorerPane.setPreferredSize(new Dimension(120, 200));

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = gbc.weighty = 0.0;
    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.gridx = 1;
    gbc.gridy = 2;
    JLabel arrow = new JLabel(Icons.getIcon("rightarrow.png"));
    gridbag.setConstraints(arrow, gbc);
    add(arrow);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.insets = new Insets(10, 10, 10, 10);
    gbc.gridx = 2;
    gbc.gridy = 2;
    gridbag.setConstraints(listPane, gbc);
    add(listPane);
    listPane.setPreferredSize(new Dimension(180, 200));
  }

  @Override
  public String getHelpText() {
    return S.get("selectionHelp");
  }

  public TimelineParam getTimelineParam() {
    return null; // fixme
  }

  @Override
  public String getTitle() {
    return S.get("selectionTab");
  }

  @Override
  public void localeChanged() {
    selectDesc.setText(S.get("selectionDesc"));
    exploreLabel.setText(S.get("exploreLabel"));
    listLabel.setText(S.get("listLabel"));
    selector.localeChanged();
    list.localeChanged();
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    if (getModel() == null) {
      selector.setLogModel(newModel);
      list.setSelection(null);
    } else {
      selector.setLogModel(newModel);
      list.setSelection(getSelection());
    }
  }
}
