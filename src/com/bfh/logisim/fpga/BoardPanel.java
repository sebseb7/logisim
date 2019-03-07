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

package com.bfh.logisim.fpga;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import com.cburch.logisim.data.Bounds;

public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {

	private Image image;
  private Image scaledImage;
	private int xs, ys, w, h;
	private boolean editing;
	private BoardEditor editor;

  private BoardPanel() {
		xs = ys = w = h = 0;
		setPreferredSize(new Dimension(getWidth(), getHeight()));
		addMouseListener(this);
		addMouseMotionListener(this);
  }

	public BoardPanel(BoardEditor parent) {
    this();
		editing = true;
		editor = parent;
	}

	public BoardPanel(URL filename) {
    this();
		editing = false;
		try {
			setImage(ImageIO.read(filename));
		} catch (IOException ex) { }
	}

  public void setImage(File file) throws IOException {
    setImage(ImageIO.read(file));
  }
	
  public void setImage(Image pic) {
		image = pic;
    if (image != null)
      scaledImage = image.getScaledInstance(getWidth(), getHeight(), 4);
    else
      scaledImage = null;
		this.repaint();
	}

  public Image getImage() {
    return scaledImage;
  }

	public void clear() {
		image = null;
    scaledImage = null;
	}

	public Boolean isEmpty() {
		return image == null;
	}


  @Override
	public int getWidth() { return Board.IMG_WIDTH; }
  @Override
	public int getHeight() { return Board.IMG_HEIGHT; }

  @Override
	public void mouseClicked(MouseEvent e) {
		if (editing)
      editor.doClickDialog(e.getX(), e.getY());
  }

  @Override
	public void mouseEntered(MouseEvent e) { }

  @Override
	public void mouseExited(MouseEvent e) { }

  @Override
	public void mouseMoved(MouseEvent e) { }

  @Override
	public void mousePressed(MouseEvent e) {
		if (editing && image != null) {
			xs = e.getX();
			ys = e.getY();
			w = h = 0;
		}
	}

  @Override
	public void mouseDragged(MouseEvent e) {
		if (editing && image != null) {
			w = e.getX() - xs;
			h = e.getY() - ys;
			this.repaint();
		}
	}

  @Override
	public void mouseReleased(MouseEvent e) {
		if (editing && image != null) {
      if (h != 0 && w != 0) {
        Bounds rect = Bounds.create(xs, ys, w, h);
        editor.doRectSelectDialog(rect);
      }
      xs = ys = w = h = 0;
      this.repaint();
		}
	}

  @Override
	public void paint(Graphics g) {
		super.paint(g);
		if (image != null) {
			g.drawImage(scaledImage, 0, 0, null);
			g.setColor(Color.RED);
			if (w != 0 || h != 0) {
				int xr, yr, wr, hr;
				xr = (w < 0) ? xs + w : xs;
				yr = (h < 0) ? ys + h : ys;
				wr = (w < 0) ? -w : w;
				hr = (h < 0) ? -h : h;
				g.drawRect(xr, yr, wr, hr);
			}
		} else {
			g.setColor(Color.gray);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (editing) {
				String[] lines = {
          "Click to add picture of FPGA board,",
          "or select a Built-in FPGA board below.",
          "",
          "The board picture must be PNG format and should be",
          " at least" + getWidth() + "x" + getHeight() + " pixels for best display." };

				g.setColor(Color.black);
				g.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 18));

        int ypos = 100, i = 0;
        for (String msg : lines) {
          FontMetrics fm = g.getFontMetrics();
          float ascent = fm.getAscent();
          int xpos = (getWidth() - fm.stringWidth(msg)) / 2;
          ypos += ascent*3/2;
          if (msg.equals(""))
            g.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 16));
          else
            g.drawString(msg, xpos, ypos);
        }
			}
		}
	}

}
