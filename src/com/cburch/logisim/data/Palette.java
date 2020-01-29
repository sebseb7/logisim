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

package com.cburch.logisim.data;
import static com.cburch.logisim.data.Strings.S;

import java.awt.Color;
import java.util.HashMap;

import com.cburch.logisim.prefs.AppPreferences;

public final class Palette {
  
  // Palettes are defined by just a handful of base colors.
  // Other auxiliary colors are derived from the base colors
  public static final String[] keys = new String[] {
    "nil", "unknown", "true", "false",
      "error", "bus", "incompatible", "canvas" };

  public static final String[] labels = new String[] {
    "-", "X", "1", "0",
      "E", "bus", "2\u22601", "bg" };

  public static int index(String key) {
    for (int i = 0; i < keys.length; i++)
      if (keys[i].equals(key))
        return i;
    return -1;
  }

  // Pre-defined palettes.
  public static HashMap<String, String> options;
  static {
    options = new HashMap<>();
    options.put(AppPreferences.PALETTE_STANDARD,
        "nil=#808080;unknown=#2828ff;true=#00d200;false=#006400;error=#c00000;bus=#000000;incompatible=#ff7b00;canvas=#ffffff");
    options.put(AppPreferences.PALETTE_CONTRAST,
        "nil=#808080;unknown=#008bff;true=#00c400;false=#300001;error=#d40000;bus=#000000;incompatible=#c96100;canvas=#ffffff");
    options.put(AppPreferences.PALETTE_GREEN,
        "nil=#a8a8a8;unknown=#f7e2b2;true=#fcff12;false=#08fff4;error=#ff7975;bus=#ffffff;incompatible=#ffa0f3;canvas=#094d1c");
    options.put(AppPreferences.PALETTE_BLUE,
        "nil=#a8a8a8;unknown=#f7e2b2;true=#fcff12;false=#08fff4;error=#ff7975;bus=#ffffff;incompatible=#ffa0f3;canvas=#0f214d");
    options.put(AppPreferences.PALETTE_DARK,
        "nil=#a8a8a8;unknown=#f7e2b2;true=#fcff12;false=#08fff4;error=#ff7975;bus=#ffffff;incompatible=#ffa0f3;canvas=#000000");
  }

  public static final Palette MONOCHROME = new Palette(new Color[] {
    Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
      Color.BLACK, Color.BLACK, Color.BLACK, Color.WHITE,
  });

  // The standard palette, used in toolbars, project explorer,
  // and the default for layout and printing as well.
  public static final Palette STANDARD =
    MONOCHROME.derive(AppPreferences.PALETTE_STANDARD);

  // The current palette, as selected in app preferences.
  public static Palette current() { return cur; }
  private static Palette cur = STANDARD; // later changed to user's current choice
  public static Palette ghostVery() { return ghostVery; }
  private static Palette ghostVery = STANDARD.translucent(50);
  public static Palette ghostSome() { return ghostSome; }
  private static Palette ghostSome = STANDARD.translucent(65);
  public static Palette selected() { return selected; }
  private static Palette selected = STANDARD.makeSelected();

  public Palette(Color[] base) {
    this(base, null);
  }

  public Palette(Color[] base, Color lineColor) {
    // base colors
    NIL = base[0];
    UNKNOWN = base[1];
    TRUE = base[2];
    FALSE = base[3];
    ERROR = base[4];
    MULTI = base[5];
    WIDTH_ERROR = base[6];
    CANVAS = base[7];

    // auxiliary colors
    OSCILLATING = ERROR;
    CRASHED = ERROR;
    LINE = lineColor == null ? contrast(CANVAS) : lineColor;
    REVERSE = contrast(LINE);
    GRID_DOT = contrast(CANVAS, 53f);
    GRID_DOT_ZOOMED = contrast(CANVAS, 20f);
    GRID_ZOOMED_OUT = contrast(CANVAS, 18f);
    SOLID = contrast(CANVAS, 25f); // contrast(CANVAS, 5f);
    if (isLight(CANVAS)) {
      HALO = rotate(CANVAS, 50f, 100f, 300f);
      PENDING = rotate(CANVAS, 50f, 100f, 240f);
    } else {
      HALO = rotate(CANVAS, 50f, 100f, 300f);
      PENDING = rotate(CANVAS, 50f, 100f, 130f);
    }
    TICK_RATE = new Color(0x92000000 | (contrast(CANVAS, 80f).getRGB() & 0xffffff));

    // todo: derive these auxiliary colors, or eliminate them
    WIDTH_ERROR_CAPTION = new Color(85, 0, 0, CANVAS.getAlpha());
    WIDTH_ERROR_HIGHLIGHT = new Color(255, 255, 0, CANVAS.getAlpha());
    WIDTH_ERROR_CAPTION_BG = new Color(255, 230, 210, CANVAS.getAlpha());
    ICON_WIDTH = new Color(153, 49, 0, CANVAS.getAlpha());
  }

  public Palette stateless() {
    Color[] base = toArray();
    // Use LINE in place of UNKNOWN, TRUE, FALSE, and ERROR
    // Or maybe use MULTI_COLOR instead?
    base[1] = base[2] = base[3] = base[4] = LINE;
    return new Palette(base);
  }

  public Color[] toArray() {
    return new Color[] { NIL, UNKNOWN, TRUE, FALSE,
      ERROR, MULTI, WIDTH_ERROR, CANVAS };
  }

  public final Color NIL;
  public final Color FALSE;
  public final Color TRUE;
  public final Color UNKNOWN;
  public final Color ERROR;

  public final Color ICON_WIDTH;
  public final Color WIDTH_ERROR;
  public final Color WIDTH_ERROR_CAPTION;
  public final Color WIDTH_ERROR_HIGHLIGHT;
  public final Color WIDTH_ERROR_CAPTION_BG;

  public final Color MULTI; // bus color

  public final Color CANVAS;

  public final Color HALO; // complements CANVAS, but magenta
  public final Color PENDING; // complements CANVAS, but blue
  public final Color OSCILLATING; // same as ERROR
  public final Color CRASHED; // same as ERROR
  public final Color TICK_RATE; // contrasts CANVAS, but translucent

  public final Color LINE; // contrast(CANVAS)
  public final Color REVERSE; // contrast(LINE))
  
  public final Color GRID_DOT; // contrasts CANVAS
  public final Color GRID_DOT_ZOOMED; // contrasts CANVAS
  public final Color GRID_ZOOMED_OUT; // contrasts CANVAS

  public final Color SOLID; // contrasts CANVAS


  public static void setPalette(String specs) {
    setPalette(cur.derive(specs));
  }

  public static void setPalette(Palette p) {
    cur = p;
    ghostVery = cur.translucent(50);
    ghostSome = cur.translucent(65);
    selected = cur.makeSelected(); // fixme: also replace wire colors
  }

  public Palette derive(String specs) {
    if (specs == null || specs.equals(""))
      return this;
    if (options.containsKey(specs))
      specs = options.get(specs);
    Color[] base = toArray();
    for (String spec : specs.split(";")) {
      if (spec.equals(""))
        continue;
      String[] kv = spec.split("=");
      if (kv.length != 2) {
        System.out.println("Error parsing circuit palette: bad key/value pair: " + spec);
        continue;
      }
      String k = kv[0];
      String v = kv[1];
      int idx = index(k);
      if (idx < 0) {
        System.out.println("Error parsing circuit palette: bad key: " + k);
        continue;
      }
      Color c;
      try {
        c = Color.decode(v);
      } catch (NumberFormatException e) {
        System.out.println("Error parsing circuit palette: bad color " + v);
        continue;
      }
      base[idx] = c;
    }
    return new Palette(base);
  }

  public Palette translucent(float percent) {
    Color[] base = toArray();
    for (int i = 0; i < base.length; i++)
      base[i] = new Color(base[i].getRed(),
          base[i].getBlue(),
          base[i].getGreen(),
          Math.max(0, Math.min(255, (int)(255 * percent/100))));
    return new Palette(base);
  }

  private static final Color COLOR_RECT_SELECT = new Color(0, 64, 128, 255);
  public Palette makeSelected() {
    Color[] base = toArray();
    base[1] = base[2] = base[3] = base[4] = COLOR_RECT_SELECT;
    return new Palette(base, COLOR_RECT_SELECT);
  }

  public String toPrefString() {
    return toPrefString(toArray());
  }

  public static String toPrefString(Color[] base) {
    String s = "";
    for (int i = 0; i < base.length; i++) {
      int rgb = base[i].getRGB() & 0xffffff;
      s += (i == 0 ? "" : ";") + String.format("%s=#%06x", keys[i], rgb); 
    }
    return s;
  }
 
  public static boolean isLight(Color c) {
    return (299 * c.getRed() + 587 * c.getGreen() + 114 * c.getBlue()) > 128000;
  }

  public static Color contrast(Color c) {
    int a = c.getAlpha();
    if (a == 255)
      return isLight(c) ? Color.BLACK : Color.WHITE;
    else
      return isLight(c) ? new Color(0,0,0,a) : new Color(255,255,255,a);
  }

  public static Color contrast(Color c, float percent) {
    return isLight(c) ? darken(c, percent) : lighten(c, percent);
  }

  // Quick and dirty HSL color space implementation
  // h in 0 to 360
  // s in 0 to 100
  // l in 0 to 100
  // a in 0 to 1

	public static Color darken(Color c, float percent)
	{
    float[] hsl = toHSL(c);
		float multiplier = (100.0f - percent) / 100.0f;
		hsl[2] = Math.max(0.0f, hsl[2] * multiplier);
		return fromHSL(hsl, c.getAlpha() / 255.0f);
	}

	public static Color lighten(Color c, float percent)
	{
    float[] hsl = toHSL(c);
		float multiplier = (100.0f - percent) / 100.0f;
		hsl[2] = Math.min(100.0f, 100.0f - (100.0f-hsl[2]) * multiplier);
		return fromHSL(hsl, c.getAlpha() / 255.0f);
	}

  public static Color rotate(Color c, float bold, float vivid, float rotation) {
    float[] hsl = toHSL(c);
    hsl[0] += rotation;
    hsl[1] = vivid;
    hsl[2] = bold;
    return fromHSL(hsl, c.getAlpha() / 255.0f);
  }

	public static Color tint(Color c, float degrees)
	{
    float[] hsl = toHSL(c);
    // System.out.printf("tinting %s (%s %s %s) by %s\n",
      //  c, hsl[0], hsl[1], hsl[2], degrees);
		hsl[0] += degrees;
		return fromHSL(hsl, c.getAlpha() / 255.0f);
	}

	public static Color saturate(Color c, float amount)
	{
    float[] hsl = toHSL(c);
    // System.out.printf("saturating %s (%s %s %s) by %s\n",
      //  c, hsl[0], hsl[1], hsl[2], degrees);
		hsl[1] = amount;
		return fromHSL(hsl, c.getAlpha() / 255.0f);
	}

	public static float[] toHSL(Color color)
	{
		float[] rgb = color.getRGBColorComponents( null );
		float r = rgb[0];
		float g = rgb[1];
		float b = rgb[2];

		float min = Math.min(r, Math.min(g, b));
		float max = Math.max(r, Math.max(g, b));

		float h = 0;
		if (max == min)
			h = 0;
		else if (max == r)
			h = ((60 * (g - b) / (max - min)) + 360) % 360;
		else if (max == g)
			h = (60 * (b - r) / (max - min)) + 120;
		else if (max == b)
			h = (60 * (r - g) / (max - min)) + 240;

		float l = (max + min) / 2;

		float s = 0;
		if (max == min)
			s = 0;
		else if (l <= .5f)
			s = (max - min) / (max + min);
		else
			s = (max - min) / (2 - max - min);

		return new float[] {h, s * 100, l * 100};
	}

	public static Color fromHSL(float[] hsl, float alpha)
	{
    float h = hsl[0];
    float s = hsl[1];
    float l = hsl[2];
		if (s <0.0f || s > 100.0f)
			throw new IllegalArgumentException("Bad color saturation: " + s);

		if (l <0.0f || l > 100.0f)
			throw new IllegalArgumentException("Bad color luminance: " + l);

		if (alpha <0.0f || alpha > 1.0f)
			throw new IllegalArgumentException("Bad color transparency: " + alpha);

		h = h % 360.0f;
    if (h < 0)
      h += 360.0f;

		h /= 360f;
		s /= 100f;
		l /= 100f;

		float q = 0;
		if (l < 0.5)
			q = l * (1 + s);
		else
			q = (l + s) - (s * l);
		float p = 2 * l - q;
		float r = Math.min(1.0f, Math.max(0, spectrum(p, q, h + (1.0f / 3.0f))));
		float g = Math.min(1.0f, Math.max(0, spectrum(p, q, h)));
		float b = Math.min(1.0f, Math.max(0, spectrum(p, q, h - (1.0f / 3.0f))));
		return new Color(r, g, b, alpha);
	}

	private static float spectrum(float p, float q, float h) {
		if (h < 0) h += 1;
		if (h > 1 ) h -= 1;
		if (6 * h < 1)
			return p + (q - p) * 6 * h;
    else if (2 * h < 1 )
			return q;
    else if (3 * h < 2)
			return p + (q - p) * 6 * ((2.0f / 3.0f) - h);
    else
   		return p;
	}

}
