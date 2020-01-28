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

package com.cburch.logisim.gui.prefs;
import static com.cburch.logisim.gui.prefs.Strings.S;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.bric.swing.ColorPickerDialog;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Palette;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.TableLayout;

class WindowOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private PrefBoolean[] checks;
  private PrefOptionList toolbarPlacement;
  private PrefOptionList circuitPalette;

  public WindowOptions(PreferencesFrame window) {
    super(window);

    checks = new PrefBoolean[] {
      new PrefBoolean(AppPreferences.SHOW_TICK_RATE, S.getter("windowTickRate")),
      new PrefBoolean(AppPreferences.SHOW_COORDS, S.getter("windowCoordinates")),
    };

    toolbarPlacement = new PrefOptionList(AppPreferences.TOOLBAR_PLACEMENT,
        S.getter("windowToolbarLocation"), new PrefOption[] {
          new PrefOption(Direction.NORTH.toString(),
              Direction.NORTH.getDisplayGetter()),
          new PrefOption(Direction.SOUTH.toString(),
              Direction.SOUTH.getDisplayGetter()),
          new PrefOption(Direction.EAST.toString(),
              Direction.EAST.getDisplayGetter()),
          new PrefOption(Direction.WEST.toString(),
              Direction.WEST.getDisplayGetter()),
          new PrefOption(AppPreferences.TOOLBAR_DOWN_MIDDLE,
              S.getter("windowToolbarDownMiddle")),
          new PrefOption(AppPreferences.TOOLBAR_HIDDEN,
              S.getter("windowToolbarHidden")) });

    circuitPalette = new PrefOptionList(AppPreferences.CIRCUIT_PALETTE,
        S.getter("circuitPalette"), new PrefOption[] {
          new PrefOption(AppPreferences.PALETTE_CUSTOM, S.getter("circuitPaletteCustom")),
          new PrefOption(AppPreferences.PALETTE_STANDARD, S.getter("circuitPaletteStandard")),
          new PrefOption(AppPreferences.PALETTE_CONTRAST, S.getter("circuitPaletteContrast")),
          new PrefOption(AppPreferences.PALETTE_GREEN, S.getter("circuitPaletteGreen")),
          new PrefOption(AppPreferences.PALETTE_BLUE, S.getter("circuitPaletteBlue")),
          new PrefOption(AppPreferences.PALETTE_DARK, S.getter("circuitPaletteDark")) }) {
      @Override
      public void actionPerformed(ActionEvent e) {
        PrefOption x = (PrefOption) combo.getSelectedItem();
        if (x.getValue() == AppPreferences.PALETTE_CUSTOM)
          return;
        pref.set((String) x.getValue()); // super.actionPerformed(e);
      }
    };

    JPanel panel = new JPanel(new TableLayout(2));
    panel.add(toolbarPlacement.getJLabel());
    panel.add(toolbarPlacement.getJComboBox());
    panel.add(circuitPalette.getJLabel());
    panel.add(circuitPalette.getJComboBox());

    JPanel colors = new PalettePanel();
    JPanel space = new JPanel();
    space.setSize(10, 10);

    setLayout(new TableLayout(1));
    for (int i = 0; i < checks.length; i++)
      add(checks[i]);
    add(panel);
    add(space);
    add(colors);
  }

  @Override
  public String getHelpText() {
    return S.get("windowHelp");
  }

  @Override
  public String getTitle() {
    return S.get("windowTitle");
  }

  @Override
  public void localeChanged() {
    for (int i = 0; i < checks.length; i++) {
      checks[i].localeChanged();
    }
    toolbarPlacement.localeChanged();
    circuitPalette.localeChanged();
  }

  static class HitBox {
    Rectangle rect;
    String key;
    HitBox(int x0, int y0, int x1, int y1, int e, String k) {
      rect = new Rectangle(x0-e, y0-e, (x1-x0)+2*(e+1), (y1-y0)+2*(e+1));
      key = k;
    }
  }

  class PalettePanel extends JPanel implements PropertyChangeListener {
    static final int W = 340;
    static final int H = 130;
    static final int BH = 25;
    int BW = 35;
    final HitBox[] hitbox = new HitBox[] {
      new HitBox(20, 30, 40, 50, 0, "true"),
      new HitBox(20, 70, 40, 90, 0, "false"),
      new HitBox(20, 20, 110, 20, 5, "nil"),
      new HitBox(120, 20, 210, 20, 5, "unknown"),
      new HitBox(40, 40, 210, 40, 5, "true"),
      new HitBox(100, 40, 100, 70, 5, "true"),
      new HitBox(100, 70, 130, 70, 5, "true"),
      new HitBox(40, 80, 130, 80, 5, "false"),
      new HitBox(240, 30, 320, 30, 5, "error"),
      new HitBox(140, 60, 210, 60, 5, "bus"),
      new HitBox(170, 80, 210, 80, 5, "bus"),
      new HitBox(240, 70, 290, 70, 5, "incompatible"),
      new HitBox(240, 70, 240, 70, 8, "incompatible"),
      new HitBox(290, 70, 290, 70, 8, "incompatible"),
      new HitBox(210, 15, 240, 45, 0, null),
      new HitBox(210, 55, 240, 85, 0, null),
    };

    public PalettePanel() {
      setPreferredSize(new Dimension(W, H));
      addMouseListener(new Listener());
      addMouseMotionListener(new Listener());
      AppPreferences.LAYOUT_SHOW_GRID.addPropertyChangeListener(this);
      AppPreferences.CIRCUIT_PALETTE.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent event) {
      repaint();
    }

    String hilight = null;
    public void hilight(String k) {
      hilight = k;
      repaint();
    }

    void drawButton(Graphics2D g, int i, Color fill, String s) {
      int x = 5+(BW+5)*i;
      int y = H-BH-5;
      if (Palette.keys[i].equals(hilight)) {
        g.setColor(Palette.contrast(Palette.LINE_COLOR, 20));
        g.drawRect(x+2, y+2, BW-5, BH-5);
      }
      g.setColor(fill);
      g.fillRect(x+3, y+3, BW-6, BH-6);
      g.setColor(Palette.LINE_COLOR);
      g.drawRect(x, y, BW-1, BH-1);
      if (s != null) {
        g.setColor(Palette.contrast(fill));
        g.setFont(Pin.DEFAULT_FONT);
        GraphicsUtil.drawCenteredText(g, s, x+BW/2, y+BH/2);
      }
    }

    public void paintComponent(Graphics gr)
    {
      Graphics2D g = (Graphics2D)gr;
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);

      g.setColor(Palette.CANVAS_COLOR);
      g.fillRect(0, 0, W, H);
      g.setColor(Color.BLACK);
      g.drawRect(0, 0, W-1, H-1);

      if (AppPreferences.LAYOUT_SHOW_GRID.get())
        GridPainter.paintGridOld(g, 10, 1.0, new Rectangle(0, 0, W, H));

      GraphicsUtil.switchToWidth(g, 2);
      g.setColor(Palette.SOLID_COLOR);
      g.fillRect(20+1, 30+1, 20-2, 20-2);
      g.fillRect(20+1, 70+1, 20-2, 20-2);
      g.setColor(Palette.LINE_COLOR);
      g.drawRect(20+1, 30+1, 20-2, 20-2);
      g.drawRect(20+1, 70+1, 20-2, 20-2);

      GraphicsUtil.switchToWidth(g, Wire.WIDTH+2);
      g.drawPolyline(new int[] { 132, 132, 140 }, new int[] { 80, 68, 60}, 3);

      GraphicsUtil.switchToWidth(g, 2);
      {
        int[] xp = new int[] { 290+20, 290+1,  290+1, 290+20 };
        int[] yp = new int[] {  60+10,  60+3,  60+17,  60+10 };
        g.setColor(Palette.SOLID_COLOR);
        g.fillPolygon(xp, yp, 4);
        g.fillOval(290+21, 70-4, 8, 8);
        g.setColor(Palette.LINE_COLOR);
        g.drawPolyline(xp, yp, 4);
        g.drawOval(290+21, 70-4, 8, 8);
      }

      int[] xo = new int[] { 210, 210 };
      int[] yo = new int[] {  15,  55 };
      for (int i = 0; i < 2; i++) {
        int[] xp = new int[] { xo[i]+15, xo[i]+1, xo[i]+ 1, xo[i]+15 };
        int[] yp = new int[] { yo[i]+ 0, yo[i]+0, yo[i]+30, yo[i]+30 };
        g.setColor(Palette.SOLID_COLOR);
        g.fillPolygon(xp, yp, 4);
        GraphicsUtil.drawSolidCenteredArc(g, xo[i]+15, yo[i]+15, 15, -90, 180);
        g.setColor(Palette.LINE_COLOR);
        g.drawPolyline(xp, yp, 4);
      }

      Color[] palette = Palette.toArray();
      BW = (W-5)/palette.length - 5;
      for (int i = 0; i < palette.length; i++)
        drawButton(g, i, palette[i], Palette.labels[i]);
      
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.setColor(Palette.NIL_COLOR);
      g.drawLine(20, 20, 110, 20);

      g.setColor(Palette.UNKNOWN_COLOR);
      g.drawLine(120, 20, 210, 20);
      
      g.setColor(Palette.TRUE_COLOR);
      g.drawLine(40, 40, 210, 40);
      g.drawLine(100, 40, 100, 70);
      g.drawLine(100, 70, 130, 70);
      g.fillOval(100-Wire.WIDTH, 40-Wire.WIDTH, 2*Wire.WIDTH, 2*Wire.WIDTH);
      g.fillOval(20 + 4, 40 - 6, 13, 13);
      g.setColor(Palette.REVERSE_COLOR);
      g.setFont(Pin.DEFAULT_FONT);
      GraphicsUtil.drawCenteredText(g, "1", 20 + 10, 40 - 1);

      g.setColor(Palette.FALSE_COLOR);
      g.drawLine(40, 80, 130, 80);
      g.fillOval(20 + 4, 80 - 6, 13, 13);
      g.setColor(Palette.REVERSE_COLOR);
      g.setFont(Pin.DEFAULT_FONT);
      GraphicsUtil.drawCenteredText(g, "0", 20 + 10, 80 - 1);

      g.setColor(Palette.ERROR_COLOR);
      g.drawLine(240, 30, 320, 30);

      g.setColor(Palette.MULTI_COLOR);
      GraphicsUtil.switchToWidth(g, Wire.WIDTH_BUS);
      g.drawLine(140, 60, 210, 60);
      g.drawLine(170, 80, 210, 80);

      g.setColor(Palette.WIDTH_ERROR_COLOR);
      GraphicsUtil.switchToWidth(g, Wire.WIDTH);
      g.drawLine(240, 70, 290, 70);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawOval(240-4, 70-4, 8, 8);
      g.drawOval(290-4, 70-4, 8, 8);
    }

    void doColorChange(int i) {
      String old = AppPreferences.CIRCUIT_PALETTE.get();
      Color[] palette = Palette.toArray();
      Color oldColor = palette[i];
      JFrame parent = getPreferencesFrame();
      ColorPickerDialog p = new ColorPickerDialog(parent, oldColor, false);
      p.setTitle(S.get(Palette.keys[i] + "ColorTitle"));
      p.getPicker().getPreviewSwatch().setSize(150, 75);
      p.pack();
      p.setLocationRelativeTo(parent);
      // try to place to right or left of prefs window
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      Rectangle r = parent.getBounds();
      int w = p.getWidth();
      int h = p.getHeight();
      int x = r.x + r.width;
      int y = r.y;
      if (x + w > d.width) { // too small to right of prefs
        if (r.x >= w) // plenty of room to left of prefs
          x = r.x - w;
        else if (r.x > d.width - w) // prefs is near right of screen
          x = 0;
        else // prefs is near left of screen
          x = d.width - w;
      }
      p.setLocation(x, y);
      p.getPicker().addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
          Color newColor = (Color)event.getNewValue();
          if (newColor == null)
            return;
          palette[i] = newColor;
          String custom = Palette.toPrefString(palette);
          AppPreferences.CIRCUIT_PALETTE.set(custom);
        }
      });
      p.setVisible(true);
      Color newColor = p.getColor();
      if (newColor == null || newColor.equals(oldColor)) {
        AppPreferences.CIRCUIT_PALETTE.set(old);
        return;
      }
      palette[i] = newColor;
      String custom = Palette.toPrefString(palette);
      AppPreferences.CIRCUIT_PALETTE.set(custom);
    }

    class Listener extends MouseAdapter {
      @Override
      public void mouseExited(MouseEvent e)
      {
        hilight(null);
      }
      @Override
      public void mouseMoved(MouseEvent e)
      {
        int x = e.getX();
        int y = e.getY();
        if (y >= H-BH-5-5) {
          for (int i = 0; i <= 7; i++) {
            int xb = 5+(BW+5)*i;
            int yb = H-BH-5;
            if (x >= xb && x < xb+BW && y >= yb && y < yb+BH) {
              hilight(Palette.keys[i]);
              return;
            }
          }
          hilight(null);
        } else {
          for (int i = 0; i < hitbox.length; i++) {
            if (hitbox[i].rect.contains(x, y)) {
              hilight(hitbox[i].key);
              return;
            }
          }
          hilight("canvas");
        }
      }
      @Override
      public void mouseClicked(MouseEvent e)
      {
        int x = e.getX();
        int y = e.getY();
        for (int i = 0; i <= 7; i++) {
          int xb = 5+(BW+5)*i;
          int yb = H-BH-5;
          if (x >= xb && x < xb+BW && y >= yb && y < yb+BH) {
            doColorChange(i);
            return;
          }
          if (hilight != null && Palette.keys[i].equals(hilight)) {
            doColorChange(i);
            return;
          }
        }
      }
    }
  }

}
