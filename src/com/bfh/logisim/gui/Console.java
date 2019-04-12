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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

  final Commander cmdr;
  final String title;
  final int idx;

  final JTextPane area;
  int count;
  long start;
  final ArrayList<String> contents = new ArrayList<>();

  public static final int FONT_SIZE = 12;
  public static final String FONT_FAMILY = "monospaced";

  static final SimpleDateFormat initialDate = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]\n");

  public static final AttributeSet INFO, WARNING, SEVERE, ERROR;
  static {
    StyleContext styles = StyleContext.getDefaultStyleContext();
    AttributeSet aset = SimpleAttributeSet.EMPTY;
    aset = styles.addAttribute(aset, StyleConstants.FontFamily, FONT_FAMILY);
    aset = styles.addAttribute(aset, StyleConstants.FontSize, FONT_SIZE);
    aset = styles.addAttribute(aset, StyleConstants.Background, Color.BLACK);
    INFO = styles.addAttribute(aset, StyleConstants.Foreground, Color.GRAY);
    WARNING = styles.addAttribute(aset, StyleConstants.Foreground, Color.YELLOW);
    SEVERE = styles.addAttribute(aset, StyleConstants.Foreground, Color.ORANGE);
    ERROR = styles.addAttribute(aset, StyleConstants.Foreground, Color.RED);
  }

  public Console(Commander cmdr, String title, int idx) {
    this.cmdr = cmdr;
    this.title = title;
    this.idx = idx;
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
    clear();
  }

  public AttributeSet styleFor(AttributeSet defaultStyle, String msg) {
    msg = msg.trim();
    if (msg.toUpperCase().startsWith("WARNING"))
      return WARNING;
    if (msg.toUpperCase().startsWith("ERROR"))
      return ERROR;
    else if (msg.toUpperCase().startsWith("**SEVERE**"))
      return SEVERE;
    else if (msg.toUpperCase().startsWith("**FATAL**"))
      return ERROR;
    else
      return defaultStyle;
  }

  private String format(String msg) {
    if (msg.endsWith("\n"))
      msg = msg.substring(0, msg.length() - 1);
    msg.replaceAll("\n", "\n    ");
    long elapsed = System.currentTimeMillis() - start;
    return String.format("[%d.%03ds] %3d> %s\n", elapsed/1000, elapsed%1000, count, msg);
  }

	public void printf(String msg, Object ...args) {
    printf(INFO, msg, args);
  }

  private Object lock = new Object();
  public void printf(AttributeSet defaultStyle, String msg, Object ...args) {
    if (args.length > 0)
      msg = String.format(msg, args);
    AttributeSet style = styleFor(defaultStyle, msg);
    synchronized(lock) {
      count++;
      try {
        Document doc = area.getDocument();
        doc.insertString(doc.getLength(), format(msg), style);
        contents.add(msg);
      } catch (Exception e) { }
    }
    try {
      cmdr.repaintConsole(idx);
    } catch (Exception e) { }
  }

  public void clear() {
    synchronized (lock) {
      count = 0;
      try {
        area.setText(null);
        Document doc = area.getDocument();
        doc.insertString(0, initialDate.format(new Date()), INFO);
        contents.clear();
      } catch (Exception e) { }
      start = System.currentTimeMillis();
    }
    try {
      cmdr.repaintConsole(idx);
    } catch (Exception e) { }
  }

  public ArrayList<String> getText() {
    // String lines[] = area.getText().split("[\\r\\n]+");
    ArrayList<String> copy = new ArrayList<>();
    synchronized(lock) {
      copy.addAll(contents);
    }
    return copy;
  }

  public Thread copyFrom(AttributeSet defaultStyle, InputStream is) {
    InputStreamReader isr = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(isr);
    Thread t = new Thread(() -> {
      try {
        String line;
        while ((line = br.readLine()) != null)
          printf(defaultStyle, "%s", line);
      } catch (IOException e) {
        printf(ERROR, "%s (from console)\n", e.getMessage());
      }
    });
    t.start();
    return t;
  }
}
