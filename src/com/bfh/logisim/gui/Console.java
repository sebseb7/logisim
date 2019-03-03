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
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleContext;
import javax.swing.text.StyleConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;

public class Console extends JPanel {
  String title;
  JTextPane area;
  int count;

  public static final int FONT_SIZE = 12;
  public static final String FONT_FAMILY = "monospaced";

  static final StyleContext styles = StyleContext.getDefaultStyleContext();
  static final AttributeSet INFO, WARNING, SEVERE, ERROR;
  static {
    AttributeSet aset = SimpleAttributeSet.EMPTY;
    aset = styles.addAttribute(aset, StyleConstants.FontFamily, FONT_FAMILY);
    aset = styles.addAttribute(aset, StyleConstants.FontSize, FONT_SIZE);
    aset = styles.addAttribute(aset, StyleConstants.Background, Color.BLACK);
    INFO = styles.addAttribute(aset, StyleConstants.Foreground, Color.GRAY);
    WARNING = styles.addAttribute(aset, StyleConstants.Foreground, Color.YELLOW);
    SEVERE = styles.addAttribute(aset, StyleConstants.Foreground, Color.ORANGE);
    ERROR = styles.addAttribute(aset, StyleConstants.Foreground, Color.RED);
  }

  public Console(String title) {
    this.title = title;
    setLayout(new GridLayout(1, 1));
    setName(title);

    area = new JTextPane();
    area.setBackground(Color.BLACK);
    area.setEditable(false);
    ((DefaultCaret)area.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

    JScrollPane scrollpane = new JScrollPane(area);
    scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(scrollpane);
  }

  AttributeSet styleFor(String msg) {
    msg = msg.trim();
    if (msg.startsWith("Warning"))
      return WARNING;
    if (msg.startsWith("Error"))
      return ERROR;
    else if (msg.startsWith("**SEVERE**"))
      return SEVERE;
    else if (msg.startsWith("**FATAL**"))
      return ERROR;
    else
      return INFO;
  }

  String format(String msg) {
    if (msg.endsWith("\n"))
      msg = msg.substring(0, msg.length() - 1);
    msg.replaceAll("\n", "\n       ");
    return String.format("%5d> %s\n", count, msg);
  }

  public void append(String msg) {
    append(msg, styleFor(msg));
  }

  public void append(String msg, AttributeSet style) {
    count++;
    try {
      Document doc = area.getDocument();
      doc.insertString(doc.getLength(), format(msg), style);
    }
    catch (Exception e) { }
    // Rectangle rect = tabbedPane.getBounds();
    // rect.x = 0;
    // rect.y = 0;
    // if (EventQueue.isDispatchThread())
    //     tabbedPane.paintImmediately(rect);
    // else
    //     tabbedPane.repaint(rect);
  }

  public void clear() {
    count = 0;
    area.setText(null);
  }
}
