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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;

import javax.swing.JPanel;

// Time scale at top of right panel
public class Timeline extends JPanel {

	private int tickWidth, numTicks, width;

	public Timeline(int tickWidth, int numTicks, int width) {
    update(tickWidth, numTicks, width);
	}

	public void update(int tickWidth, int numTicks, int width) {
		this.tickWidth = tickWidth;
    this.numTicks = numTicks;
		this.width = width;
		setMaximumSize(new Dimension(width, ChronoPanel.HEADER_HEIGHT));
		setPreferredSize(new Dimension(width, ChronoPanel.HEADER_HEIGHT));
	}

	private String getTimeString(int i) {
    // todo: later
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(i/10.0) + " ms";
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g.drawLine(0, 5, width, 5);
    for (int i = 0; i <= numTicks; i++)
      g.drawLine(i*tickWidth, 2, i*tickWidth, 8);

    // later
    // int minimalWidthToDisp = 60;
    // int nbrTick = 0;
    // int lastDispPos = -minimalWidthToDisp;

    // if (clk != null) {
    //   for (int i = 1; i < clk.getSignalValues().size(); ++i) {
    //     // is it a clk rising edge ?
    //     if (clk.getSignalValues().get(i - 1).equals("0")
    //         && clk.getSignalValues().get(i).equals("1")) {

    //       // is there enough place to display the text?
    //       if ((i - 1) * tickWidth - lastDispPos > minimalWidthToDisp) {
    //         lastDispPos = (i - 1) * tickWidth;
    //         g2.setStroke(new BasicStroke(2));
    //         g2.drawLine(lastDispPos, 6, lastDispPos, 12);
    //         g2.setStroke(new BasicStroke(1));
    //         g.drawString(getTimeString(nbrTick), lastDispPos + 3,
    //             20);
    //       }
    //       nbrTick++;
    //     }
    //   }
    // }
  }
}
