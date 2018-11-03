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

package com.cburch.logisim.gui.start;
import static com.cburch.logisim.gui.start.Strings.S;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JComponent;

class AboutCredits extends JComponent {
  private static class CreditsLine {
    private int y;
    private int type;
    private String text;
    private Image img;
    private int imgWidth;

    public CreditsLine(int type, String text) {
      this(type, text, null, 0);
    }

    public CreditsLine(int type, String text, Image img, int imgWidth) {
      this.y = 0;
      this.type = type;
      this.text = text;
      this.img = img;
      this.imgWidth = imgWidth;
    }
  }

  private static final long serialVersionUID = 1L;

  /** Time to spend freezing the credits before after after scrolling */
  private static final int MILLIS_FREEZE = 1000;

  /** Speed of how quickly the scrolling occurs */
  private static final int MILLIS_PER_PIXEL = 20;
  /**
   * Path to Hendrix College's logo - if you want your own logo included,
   * please add it separately rather than replacing this.
   */
  private static final String HENDRIX_PATH = "resources/logisim/hendrix.png";
  private static final int HENDRIX_WIDTH = 50;

  private static final String HOLYCROSS_PATH = "resources/logisim/holycross.png";
  private static final int HOLYCROSS_WIDTH = 55;

  private Color[] colorBase;
  private Paint[] paintSteady;
  private Font[] font;

  private int scroll = 0;
  private float fadeStop = About.IMAGE_HEIGHT / 4.0f;
  private ArrayList<CreditsLine> lines;
  private int initialLines = 4; // number of lines to show in initial freeze
  private int initialHeight; // height of first few credit lines
  private int linesHeight; // total height of all credit lines
  private int headHeight; // blank space above first few lines of credits
  private int footStop; // where to freze for showing end of credits
  private int endStop; // where to stop credits and begin anew
  private int totalMillis;

  private static final int CREDITS_WIDTH = About.ABOUT_WIDTH;
  private static final int CREDITS_HEIGHT = About.ABOUT_HEIGHT / 2;
  private static final int BOTTOM_BORDER = About.IMAGE_BORDER;
  private static final int CREDITS_CENTER = CREDITS_WIDTH / 2;

  public AboutCredits() {
    setOpaque(false);
    setPreferredSize(new Dimension(CREDITS_WIDTH, CREDITS_HEIGHT));

    colorBase = new Color[] {
      new Color(143, 0, 0),
      new Color(48, 0, 96),
      new Color(48, 0, 96),
    };
    font = new Font[] {
      new Font("Sans Serif", Font.ITALIC, 20),
      new Font("Sans Serif", Font.BOLD, 24),
      new Font("Sans Serif", Font.BOLD, 18),
    };

    paintSteady = new Paint[colorBase.length];
    for (int i = 0; i < colorBase.length; i++) {
      Color hue = colorBase[i];
      paintSteady[i] = new GradientPaint(0.0f, 0.0f, derive(hue, 0), 0.0f, fadeStop, hue);
    }

    URL url = AboutCredits.class.getClassLoader().getResource(HENDRIX_PATH);
    Image hendrixLogo = null;
    if (url != null) {
      hendrixLogo = getToolkit().createImage(url);
    }

    url = AboutCredits.class.getClassLoader().getResource(HOLYCROSS_PATH);
    Image holycrossLogo = null;
    if (url != null) {
      holycrossLogo = getToolkit().createImage(url);
    }

    // Logisim's policy concerning who is given credit:
    // Past contributors are not acknowledged in the About dialog for the
    // current version, but they do appear in the acknowledgements section of
    // the User's Guide. Current contributors appear in both locations.

    lines = new ArrayList<CreditsLine>();
    
    lines.add(new CreditsLine(0, S.get("creditsRoleCurrent"),
          holycrossLogo, HOLYCROSS_WIDTH));
    lines.add(new CreditsLine(1, "Kevin Walsh"));
    lines.add(new CreditsLine(1, "College of the Holy Cross"));
    lines.add(new CreditsLine(2, "http://mathcs.holycross.edu/~kwalsh/"));

    lines.add(new CreditsLine(0, S.get("creditsRoleFork")));
    lines.add(new CreditsLine(1, "Haute \u00C9cole Sp\u00E9cialis\u00E9e Bernoise"));
    lines.add(new CreditsLine(2, "http://www.bfh.ch"));
    lines.add(new CreditsLine(1, "Haute \u00C9cole du paysage, d'ing\u00E9nierie"));
    lines.add(new CreditsLine(1, "et d'architecture de Gen\u00E8ve"));
    lines.add(new CreditsLine(2, "http://hepia.hesge.ch"));
    lines.add(new CreditsLine(1, "Haute \u00C9cole d'Ing\u00E9nierie"));
    lines.add(new CreditsLine(1, "et de Gestion du Canton de Vaud"));
    lines.add(new CreditsLine(2, "http://www.heig-vd.ch"));

    // If you fork Logisim, feel free to change the above lines, but please
    // do not change these last four lines!
    lines.add(new CreditsLine(0, S.get("creditsRoleOriginal"),
          hendrixLogo, HENDRIX_WIDTH));
    lines.add(new CreditsLine(1, "Carl Burch"));
    lines.add(new CreditsLine(2, "Hendrix College"));
    lines.add(new CreditsLine(1, "www.cburch.com/logisim/"));
  }

  private Color derive(Color base, int alpha) {
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
  }

  @Override
  protected void paintComponent(Graphics g) {

    FontMetrics[] fms = new FontMetrics[font.length];
    for (int i = 0; i < fms.length; i++)
      fms[i] = g.getFontMetrics(font[i]);

    if (linesHeight == 0) {
      int y = 0;
      for (int i = 0; i < lines.size(); i++) {
        CreditsLine line = lines.get(i);
        if (i+1 == initialLines)
          initialHeight = y;
        if (line.type == 0)
          y += 10;
        FontMetrics fm = fms[line.type];
        line.y = y + fm.getAscent();
        y += fm.getHeight();
      }
      linesHeight = y;
      headHeight = Math.max(0, CREDITS_HEIGHT - BOTTOM_BORDER - initialHeight);
      footStop = headHeight + linesHeight - CREDITS_HEIGHT - BOTTOM_BORDER;
      endStop = headHeight + linesHeight + CREDITS_HEIGHT;
      totalMillis = 2 * MILLIS_FREEZE + endStop * MILLIS_PER_PIXEL;
    }

    int offs = scroll % totalMillis;
    int yPos;
    if (offs >= 0 && offs < MILLIS_FREEZE) {
      // frozen before starting the credits scroll
      yPos = -headHeight;
    } else if (offs < MILLIS_FREEZE + footStop * MILLIS_PER_PIXEL) {
      // scrolling through credits
      yPos = -headHeight + (offs - MILLIS_FREEZE) / MILLIS_PER_PIXEL;
    } else if (offs < 2 * MILLIS_FREEZE + footStop * MILLIS_PER_PIXEL) {
      // freezing at bottom of scroll
      yPos = -headHeight + footStop;
    } else if (offs < 2 * MILLIS_FREEZE + (linesHeight + headHeight) * MILLIS_PER_PIXEL) {
      // scrolling bottom off screen
      yPos = -headHeight + (offs - 2 * MILLIS_FREEZE) / MILLIS_PER_PIXEL;
    } else {
      // scrolling next credits onto screen
      int millis = offs - 2 * MILLIS_FREEZE - (linesHeight + headHeight) * MILLIS_PER_PIXEL;
      yPos = -headHeight - CREDITS_HEIGHT + millis / MILLIS_PER_PIXEL;
    }

    for (CreditsLine line : lines) {
      int y = line.y - yPos;
      if (y < -100 || y > CREDITS_HEIGHT + 50)
        continue;

      int type = line.type;
      ((Graphics2D) g).setPaint(paintSteady[type]);
      g.setFont(font[type]);
      int textWidth = fms[type].stringWidth(line.text);
      g.drawString(line.text, CREDITS_CENTER - textWidth / 2, line.y - yPos);

      Image img = line.img;
      if (img != null) {
        int x = CREDITS_WIDTH - line.imgWidth - BOTTOM_BORDER;
        int top = y - fms[type].getAscent();
        g.drawImage(img, x, top, this); // todo: fade image
      }
    }
  }

  public void setScroll(int value) {
    scroll = value;
    repaint();
  }
}
