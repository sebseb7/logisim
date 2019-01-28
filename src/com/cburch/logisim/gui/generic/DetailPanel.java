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

package com.cburch.logisim.gui.generic;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.KeyboardFocusManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DetailPanel extends JPanel {
  protected JButton button;
  protected JComponent detail;
  protected Icon iconOpen, iconClosed;

  public DetailPanel(String title, JComponent detail) {
    this.detail = detail;

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    button = new JButton(title);
    button.setContentAreaFilled(false);
    button.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    iconClosed = UIManager.getIcon("Tree.collapsedIcon");
    if (iconClosed == null) UIManager.getIcon("Tree.closedIcon");
    iconOpen = UIManager.getIcon("Tree.expandedIcon");
    if (iconOpen == null) UIManager.getIcon("Tree.openIcon");
    button.setIcon(iconClosed);

    Box buttonBox = Box.createHorizontalBox();
    buttonBox.add(button);
    buttonBox.add(Box.createHorizontalGlue());
    add(buttonBox);
    add(detail);
    detail.setVisible(false);

    button.addActionListener(e -> {
      toggle();
      KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
    });
  }

  public void expand() {
    if (!detail.isShowing())
      toggle();
  }

  public void collapse() {
    if (detail.isShowing())
      toggle();
  }

  public void toggle() {
    Window topWindow = SwingUtilities.getWindowAncestor(this);
    Dimension oldSize = topWindow.getSize();
    if (detail.isShowing()) {
      button.setIcon(iconClosed);
      detail.setVisible(false);
    } else {
      button.setIcon(iconOpen);
      detail.setVisible(true);
    }
    // invalidate();
    revalidate();
    // topWindow.pack();
    Dimension newSize = topWindow.getSize();
    Dimension s = new Dimension(Math.max(oldSize.width, newSize.width),
        Math.max(oldSize.height, newSize.height));
    if (!newSize.equals(s))
      topWindow.setSize(s);
    // repaint();
  }

  public void setTitle(String title) {
    button.setText(title);
  }
}
