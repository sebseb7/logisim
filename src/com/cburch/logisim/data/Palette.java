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

  public static Color NIL_COLOR;
  public static Color FALSE_COLOR;
  public static Color TRUE_COLOR;
  public static Color UNKNOWN_COLOR;
  public static Color ERROR_COLOR;

  public static Color WIDTH_ERROR_COLOR;
  public static Color WIDTH_ERROR_CAPTION_COLOR = new Color(85, 0, 0);
  public static Color WIDTH_ERROR_HIGHLIGHT_COLOR = new Color(255, 255, 0);
  public static Color WIDTH_ERROR_CAPTION_BGCOLOR = new Color(255, 230, 210);
  public static Color ICON_WIDTH_COLOR; // WIDTH_ERROR_COLOR.darker();

  public static Color MULTI_COLOR; // bus color

  public static Color CANVAS_COLOR;

  public static Color HALO_COLOR; // complements CANVAS_COLOR, but magenta
  public static Color PENDING_COLOR; // complements CANVAS_COLOR, but cyan
  public static Color OSCILLATING_COLOR; // complements CANVAS_COLOR, but red
  public static Color CRASHED_COLOR; // complements CANVAS_COLOR, but blue
  public static Color TICK_RATE_COLOR; // contrasts CANVAS_COLOR

  public static Color LINE_COLOR; // contrast(CANVAS_COLOR)
  public static Color REVERSE_COLOR; // contrast(LINE_COLOR))
  
  public static Color GRID_DOT_COLOR; // contrasts CANVAS_COLOR
  public static Color GRID_DOT_ZOOMED_COLOR; // contrasts CANVAS_COLOR
  public static Color GRID_ZOOMED_OUT_COLOR; // contrasts CANVAS_COLOR

  public static Color SOLID_COLOR; // contrasts CANVAS_COLOR

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
    setPalette(AppPreferences.PALETTE_STANDARD); // later changed to user's current choice
  }

  public static void setPalette(String specs) {
    if (specs == null || specs.equals(""))
      specs = "standard";
    if (options.containsKey(specs))
        specs = options.get(specs);
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
      Color c;
      try {
        c = Color.decode(v);
      } catch (NumberFormatException e) {
        System.out.println("Error parsing circuit palette: bad color " + v);
        continue;
      }
      if (k.equals("nil"))
        NIL_COLOR = c;
      else if (k.equals("unknown"))
        UNKNOWN_COLOR = c;
      else if (k.equals("true"))
        TRUE_COLOR = c;
      else if (k.equals("false"))
        FALSE_COLOR = c;
      else if (k.equals("error")) {
        ERROR_COLOR = c;
        OSCILLATING_COLOR = c;
        CRASHED_COLOR = c;
      } else if (k.equals("bus"))
        MULTI_COLOR = c;
      else if (k.equals("incompatible")) {
        WIDTH_ERROR_COLOR = c;
      } else if (k.equals("canvas")) {
        CANVAS_COLOR = c;
        LINE_COLOR = contrast(c);
        REVERSE_COLOR = contrast(LINE_COLOR);
        GRID_DOT_COLOR = contrast(c, 53f);
        GRID_DOT_ZOOMED_COLOR = contrast(c, 20f);
        GRID_ZOOMED_OUT_COLOR = contrast(c, 18f);

        // SOLID_COLOR = contrast(c, 5f);
        SOLID_COLOR = contrast(c, 25f);

        if (isLight(c)) {
          HALO_COLOR = rotate(c, 50f, 100f, 300f);
          PENDING_COLOR = rotate(c, 50f, 100f, 240f);
        } else {
          HALO_COLOR = rotate(c, 50f, 100f, 300f);
          PENDING_COLOR = rotate(c, 50f, 100f, 130f);
        }
        Color c2 = new Color(0x92000000 | (contrast(c, 80f).getRGB() & 0xffffff));
        TICK_RATE_COLOR = c2;
      }
      else
        System.out.println("Error parsing circuit palette: bad key " + k);
    }
  }

  public static final String[] keys = new String[] {
    "nil", "unknown", "true", "false",
      "error", "bus", "incompatible", "canvas" };
  public static final String[] labels = new String[] {
    "-", "X", "1", "0",
      "E", "bus", "2\u22601", "bg" };
  public static Color[] toArray() {
      return new Color[] {
            NIL_COLOR,
            UNKNOWN_COLOR,
            TRUE_COLOR,
            FALSE_COLOR,
            ERROR_COLOR,
            MULTI_COLOR,
            WIDTH_ERROR_COLOR,
            CANVAS_COLOR
      };
  }

  public static String toPrefString() {
    return toPrefString(toArray());
  }

  public static String toPrefString(Color[] palette) {
    String s = "";
    for (int i = 0; i < palette.length; i++) {
      int rgb = palette[i].getRGB() & 0xffffff;
      s += (i == 0 ? "" : ";") + String.format("%s=#%06x", keys[i], rgb); 
    }
    return s;
  }
 
  public static boolean isLight(Color c) {
    return (299 * c.getRed() + 587 * c.getGreen() + 114 * c.getBlue()) > 128000;
  }

  public static Color contrast(Color c) {
    return isLight(c) ? Color.BLACK : Color.WHITE;
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
