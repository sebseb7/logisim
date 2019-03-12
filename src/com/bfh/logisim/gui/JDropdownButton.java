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

package com.bfh.logisim.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

// The code below is borrwed and adapted from public domain code by Joris Van
// den Bogaert posted at: http://esus.com/creating-jtoolbar-drop-menu-one-items/

class JDropdownButton extends AbstractButton {

   JButton actionButton;
   JToggleButton menuButton;
   JPopupMenu popupMenu;
 
   public JDropdownButton(JButton button, JPopupMenu popup, ImageIcon icon) {
      actionButton = button;
      popupMenu = popup;

      Dimension d = actionButton.getPreferredSize();
      actionButton.setMinimumSize(d);
      actionButton.setMaximumSize(d);
      actionButton.setPreferredSize(d);
 
      setLayout(new BorderLayout());
      // actionButton.setBorderPainted(false);
      add(BorderLayout.CENTER, actionButton);
      menuButton = new JToggleButton(icon);
      menuButton.setPreferredSize(new Dimension(15, 10));
      add(BorderLayout.EAST, menuButton);
      // menuButton.setBorderPainted(false);
  
      MouseAdapter ma = new MouseAdapter() {
         public void mouseClicked(MouseEvent me) { }
         public void mousePressed(MouseEvent me) { 
            if (me.getSource() == actionButton) {
               menuButton.setSelected(true);
            }
         }
         public void mouseReleased(MouseEvent me) {
            if (me.getSource() == actionButton) {
               menuButton.setSelected(false);
            }
         }
         public void mouseEntered(MouseEvent me) { 
            setRolloverBorder(true); 
         }
         public void mouseExited(MouseEvent me) { 
            setRolloverBorder(false);
         }
      };
 
      actionButton.addMouseListener(ma);
      menuButton.addMouseListener(ma);
      menuButton.addActionListener(e ->
          popupMenu.show(actionButton, 0, actionButton.getSize().height));
   }  
 
   protected void setRolloverBorder(boolean b) {
      // actionButton.setBorderPainted(b);
      // menuButton.setBorderPainted(b);
   }
}
