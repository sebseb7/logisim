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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JLabel;

public class BoardIcon implements Icon {
  private Image image;
  private int icon_width = 240;
  private int icon_height = 130;
  public final JLabel label = new JLabel();

  public BoardIcon() {
  }

  public int getIconHeight() { return icon_height; }
  public int getIconWidth() { return icon_width; }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (image != null) {
      g.drawImage(image, x, y, null);
    } else {
      g.setColor(Color.gray);
      g.fillRect(0, 0, getIconWidth(), getIconHeight());
    }
  }

  public void setImage(BufferedImage img) {
    if (img != null)
      image = img.getScaledInstance(getIconWidth(),
          getIconHeight(), BufferedImage.SCALE_SMOOTH);
    else
      image = null;
		label.setIcon(this);
    repaint();
    label.repaint();
  }

}
