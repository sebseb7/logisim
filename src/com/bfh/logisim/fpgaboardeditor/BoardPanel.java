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

package com.bfh.logisim.fpgaboardeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

public class BoardPanel extends JPanel implements MouseListener, MouseMotionListener {

	public static final FileFilter PNG_FILTER = new FileFilter {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".png");
		}
		@Override
		public String getDescription() {
			return Strings.get("PNGFileFilter");
		}
	}

	private BufferedImage image;
  private Image scaledImage;
	private int xs, ys, w, h;
	private boolean editing;
	private BoardDialog edit_parent;

  private BoardPanel() {
		xs = ys = w = h = 0;
		setPreferredSize(new Dimension(getWidth(), getHeight()));
		addMouseListener(this);
		addMouseMotionListener(this);
  }

	public BoardPanel(BoardDialog parent) {
    this();
		editing = true;
		edit_parent = parent;
	}

	public BoardPanel(URL filename) {
    this();
		editing = false;
		try {
			image = ImageIO.read(filename);
      scaledImage = image.getScaledInstance(getWidth(), getHeight(), 4);
		} catch (IOException ex) { }
	}
	
  public void setImage(BufferedImage pic) {
		image = pic;
    scaledImage = image.getScaledInstance(getWidth(), getHeight(), 4);
		this.repaint();
	}

	public void clear() {
		image = null;
	}

	public Boolean isEmpty() {
		return image == null;
	}


  @Override
	public int getWidth() { return 740; }

  @Override
	public int getHeight() { return 400; }

  @Override
	public void mouseClicked(MouseEvent e) {
		if (editing && !this.ImageLoaded()) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogTitle("Choose FPGA board picture to use");
			fc.setFileFilter(PNG_FILTER);
			fc.setAcceptAllFileFilterUsed(false);
			int retval = fc.showOpenDialog(null);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				try {
					image = ImageIO.read(file);
				} catch (IOException ex) {
					image = null;
				}
				this.repaint();
				edit_parent.SetBoardName(file.getName());
			}
		}
	}

  @Override
	public void mouseDragged(MouseEvent e) {
		if (editing && this.ImageLoaded()) {
			w = e.getX() - xs;
			h = e.getY() - ys;
			this.repaint();
		}
	}

  @Override
	public void mouseEntered(MouseEvent e) { }

  @Override
	public void mouseExited(MouseEvent e) { }

  @Override
	public void mouseMoved(MouseEvent e) { }

  @Override
	public void mousePressed(MouseEvent e) {
		if (editing && this.ImageLoaded()) {
			xs = e.getX();
			ys = e.getY();
			w = h = 0;
		}
	}

  @Override
	public void mouseReleased(MouseEvent e) {
		if (editing && this.ImageLoaded()) {
      if (h != 0 && w != 0) {
        BoardRectangle rect = new BoardRectangle(xs, ys, w, h);
        edit_parent.doRectSelectDialog(rect);
      }
      xs = ys = w = h = 0;
      this.repaint();
		}
	}

  @Override
	public void paint(Graphics g) {
		super.paint(g);
		if (image != null) {
			g.drawImage(scaledImage);
			g.setColor(Color.red);
			if (w != 0 || h != 0) {
				int xr, yr, wr, hr;
				xr = (w < 0) ? xs + w : xs;
				yr = (h < 0) ? ys + h : ys;
				wr = (w < 0) ? -w : w;
				hr = (h < 0) ? -h : h;
				g.drawRect(xr, yr, wr, hr);
			}
      LinkedList<BoardRectangle> rects = editing ? edit_parent.defined_components : null;
			if (rects != null) {
				g.setColor(Color.red);
        for (BoardRectangle rect : rects)
					g.fillRect(rect.x, rect.y, rect.width, rect.height);
			}
		} else {
			g.setColor(Color.gray);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (editing) {
				String[] lines = {
          "Click to add picture of FPGA board.",
          "The board picture must be PNG format and should be"
          " at least" + getWidth() + "x" + getHeight() + " pixels for best display." };

				g.setColor(Color.black);
				g.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 20));

				FontMetrics fm = g.getFontMetrics();
        float ascent = fm.getAscent();
        int ypos = 100, i = 0;
        for (String msg : lines) {
          int xpos = (getWidth() - fm.stringWidth(message)) / 2;
          g.drawString(message, xpos, ypos);
          ypos = 200 + (int)((i++) * 1.5 * ascent);
        }
			}
		}
	}

}
