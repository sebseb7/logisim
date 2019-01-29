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

package com.cburch.logisim.gui.main;
import static com.cburch.logisim.gui.main.Strings.S;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.gui.generic.ProjectExplorer;

public class SimulationTreeRenderer extends DefaultTreeCellRenderer {
  private static class RendererIcon implements Icon {
    private ComponentFactory factory;
    private boolean isCurrentView;

    RendererIcon(ComponentFactory factory, boolean isCurrentView) {
      this.factory = factory;
      this.isCurrentView = isCurrentView;
    }

    public int getIconHeight() {
      return 20;
    }

    public int getIconWidth() {
      return 20;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      ComponentDrawContext context = new ComponentDrawContext(c, null,
          null, g, g);
      factory.paintIcon(context, x, y, factory.createAttributeSet());

      // draw magnifying glass if appropriate
      if (isCurrentView) {
        int tx = x + 13;
        int ty = y + 13;
        int[] xp = { tx - 1, x + 18, x + 20, tx + 1 };
        int[] yp = { ty + 1, y + 20, y + 18, ty - 1 };
        g.setColor(ProjectExplorer.MAGNIFYING_INTERIOR);
        g.fillOval(x + 5, y + 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawOval(x + 5, y + 5, 10, 10);
        g.fillPolygon(xp, yp, xp.length);
      }
    }
  }

  private static final long serialVersionUID = 1L;

  Font plainFont, boldFont;

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean selected, boolean expanded, boolean leaf, int row,
      boolean hasFocus) {
    Component ret = super.getTreeCellRendererComponent(tree, value,
        selected, expanded, leaf, row, hasFocus);
    if (plainFont == null) {
      plainFont = ret.getFont();
      boldFont = new Font(plainFont.getFontName(), Font.BOLD, plainFont.getSize());
    }
    ret.setFont(plainFont);
    if (ret instanceof JComponent) {
      JComponent comp = (JComponent) ret;
      comp.setToolTipText(null);
      comp.setOpaque(false);
    }
    if (!(ret instanceof JLabel))
      return ret;
    JLabel label = (JLabel) ret;
    if (value instanceof SimulationTreeNode) {
      SimulationTreeNode node = (SimulationTreeNode) value;
      SimulationTreeModel model = (SimulationTreeModel) tree.getModel();
      ComponentFactory factory = node.getComponentFactory();
      if (factory != null) {
        boolean viewed = node.isCurrentView(model);
        if (viewed) {
          label.setFont(boldFont);
          label.setBackground(ProjectExplorer.VIEWED_TOOL_COLOR);
          label.setOpaque(true);
        }
        label.setText(node.toString()); // resizes when bold
        label.setIcon(new RendererIcon(factory, viewed));
        label.setToolTipText(S.fmt("simulationToolTip", factory.getDisplayName()));
      }
    }
    return ret;
  }
}
