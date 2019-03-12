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
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

// import javax.xml.parsers.DocumentBuilder;
// import javax.xml.parsers.DocumentBuilderFactory;
// import javax.xml.transform.OutputKeys;
// import javax.xml.transform.Result;
// import javax.xml.transform.Source;
// import javax.xml.transform.Transformer;
// import javax.xml.transform.TransformerFactory;
// import javax.xml.transform.dom.DOMSource;
// import javax.xml.transform.stream.StreamResult;
// import org.w3c.dom.Attr;
// import org.w3c.dom.Document;
// import org.w3c.dom.Element;
// import org.w3c.dom.NamedNodeMap;
// import org.w3c.dom.Node;
// import org.w3c.dom.NodeList;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

// import com.bfh.logisim.data.Bounds;
// import com.bfh.logisim.netlist.CorrectLabel;
import com.bfh.logisim.fpga.Board;
import com.bfh.logisim.fpga.BoardIO;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.netlist.NetlistComponent;
import com.bfh.logisim.netlist.Path;
import static com.bfh.logisim.fpga.PinBindings.Dest;
import static com.bfh.logisim.fpga.PinBindings.Source;

public class BindingsDialog extends JDialog {

  private Board board;
  private PinBindings pinBindings;

  private Synthetic zeros, ones, constants, bitbucket;
  private Synthetic[] synthetics;

  private JButton unmap = new JButton("Unset");
  private JButton reset = new JButton("Reset All");
  // private JButton save = new JButton("Load Map from XML");
  // private JButton load = new JButton("Save Map to XML");
  private JButton done = new JButton("Close/Done");
  private JLabel status = new JLabel();

  private HashMap<BoardIO, Rect> rects = new HashMap<>();
  private SelectionPanel picture = new SelectionPanel();
  private SourceList sources;
  JLayeredPane overlay;

  private String xmlDir;

  // Caution: these *must* be Integer. See javadoc for JLayeredPane.
  private static final Integer LAYER_BOTTOM = 0;
  private static final Integer LAYER_TOP = 1;

  public BindingsDialog(Board board, PinBindings pinBindings, JFrame parentFrame, String projectPath) {
    super(parentFrame, ModalityType.APPLICATION_MODAL);
    this.board = board;
    this.pinBindings = pinBindings;
    sources = new SourceList();

    overlay = new JLayeredPane();

    for (BoardIO io : board) {
      Rect r = new Rect(io);
      rects.put(io, r);
      overlay.add(r, LAYER_TOP);
      r.setBounds(io.rect.x, io.rect.y, io.rect.width, io.rect.height);
      r.setVisible(false);
    }

    xmlDir = new File(projectPath).getParent();
    if (xmlDir == null)
      xmlDir = "";
    else if (xmlDir.length() > 0 && !xmlDir.endsWith(File.separator))
      xmlDir += File.separator;

    setTitle("Configure FPGA Pin Bindings");
    setResizable(false);
    setAlwaysOnTop(false);
    setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    // Synthetic BoardIO buttons
    zeros = new Synthetic(BoardIO.Type.AllZeros, 0, "Zeros", "zeros.gif",
        "Use constant 0 (or all zeros) for selected input signal.");
    ones = new Synthetic(BoardIO.Type.AllOnes, -1, "Ones", "ones.gif",
        "Use constant 1 (or all ones) for selected input signal.");
    constants = new Synthetic(BoardIO.Type.Constant, 0, "Constant", "constants.gif",
        "Define a constant to use for selected input signal.");
    bitbucket = new Synthetic(BoardIO.Type.Unconnected, 0, "Disconnected", "disconnected.gif",
        "Leave the selected output or inout signal disconnected.");
    synthetics = new Synthetic[] { ones, zeros, constants, bitbucket };
    JPanel buttonpanel = new JPanel(); // default FlowLayout
    buttonpanel.setBackground(Color.BLACK);
    for (Synthetic b : synthetics)
      buttonpanel.add(b);

    // Component selection list
    JScrollPane scroll = new JScrollPane(sources);
    scroll.setToolTipText("<html>Select a component and connect it to a board I/O resource.<br>"
        + "Use drop-down menu to expand or change component type.</html>");
   
    // Action buttons along right side
    unmap.addActionListener(e -> sources.unmapCurrent());
    reset.addActionListener(e -> sources.resetAll());
    done.addActionListener(e -> { setVisible(false); dispose(); });

    // Scroll panel and status along left side
    status.setBorder(
        BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
          BorderFactory.createEmptyBorder(2, 2, 2, 2)));

    overlay.add(picture, LAYER_BOTTOM);
    picture.setBounds(0, 0, picture.getWidth(), picture.getHeight());
    overlay.setPreferredSize(picture.getPreferredSize());
    JPanel top = new JPanel();
    top.add(overlay);
    top.setBackground(Color.BLACK);
    top.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.BLACK));

    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = GridBagConstraints.RELATIVE;
    c.gridwidth = 2;
    add(top, c);
    add(buttonpanel, c);

    c.gridwidth = 1;
    c.gridx = 1;
    c.insets.left = c.insets.right = c.insets.bottom = 5;
    c.weightx = 0.0;
    c.fill = GridBagConstraints.BOTH;
    c.weighty = 1.0;
    add(new JPanel(), c); // filler
    c.weighty = 0.0;
    c.fill = GridBagConstraints.HORIZONTAL;
    add(unmap, c);
    add(reset, c);
    add(done, c);

    c.gridx = 0;
    c.gridwidth = 2;
    c.insets.left = c.insets.right = 0;
    c.insets.top = c.insets.bottom = 0;
    add(status, c);

    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = 1;
    c.gridheight = 4;
    c.gridx = 0;
    c.gridy = 2;
    c.weightx = 1.0;
    c.insets.left = 5;
    c.insets.right = 0;
    c.insets.top = c.insets.bottom = 5;
    add(scroll, c);

    if (sources.model.getSize() > 0)
      sources.setSelectedIndex(0);

    updateStatus();

    pack();
    setLocationRelativeTo(parentFrame);
  }

  private class SelectionPanel extends JPanel {
    SelectionPanel() {
      setPreferredSize(new Dimension(Board.IMG_WIDTH, Board.IMG_HEIGHT));
      setBackground(Color.BLACK);
      setOpaque(true);
    }
    @Override
    public int getWidth() { return Board.IMG_WIDTH; }
    @Override
    public int getHeight() { return Board.IMG_HEIGHT; }
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (board.image != null)
        g.drawImage(board.image, 0, 0, null);
    }
  }

  private static final Color MAPPED = new Color(0, 0x60, 0);
  private static final Color MISTY = new Color(1f, 0f, 0f, 0.4f);
  private static final Color MISTYB = new Color(1f, 0f, 0f);
  private static final Color HILIGHT = new Color(0f, 1f, 0f, 0.4f);
  private static final Color HILIGHTB = new Color(0f, 1f, 0f);
  private static final Color HOVER = new Color(1f, 1f, 0f, 0.4f);
  private static final Color HOVERB = new Color(1f, 1f, 0f);

  private class Rect extends JPanel implements MouseListener {
    BoardIO io;
    boolean select, hover;

    Rect(BoardIO io) {
      this.io = io;
      setOpaque(false);
      setVisible(false);
      addMouseListener(this);
      setToolTipText("Map component to " + io);
    }

    @Override
    protected void paintComponent(Graphics g) {
      g.setColor(hover ? HOVERB : select ? HILIGHTB : MISTYB);
      g.drawRect(0, 0, io.rect.width, io.rect.height);
      g.setColor(hover ? HOVER : select ? HILIGHT : MISTY);
      g.fillRect(0, 0, io.rect.width, io.rect.height);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton(e))
        return;
      if (sources.current == null)
        return;
      if (sources.current.width.size() == 1 && io.width > 1)
        doBitSelectPopup(e);
      else {
        Dest dest = pinBindings.mappings.get(sources.current);
        if (dest != null && dest.io == io)
          sources.unmapCurrent();
        else
          sources.mapCurrent(io, -1);
      }
    }

    // todo: hilight/tint on hover
    public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
    public void mouseExited(MouseEvent e) { hover = false; repaint(); }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }

    void doBitSelectPopup(MouseEvent e) {
      JPopupMenu popup = new JPopupMenu("Select Bit");
      String[] pinLabels = io.type.pinLabels(io.width);
      Dest dest = pinBindings.mappings.get(sources.current);
      // System.out.printf("source is %s\n", sources.current);
      // System.out.printf("this is %s\n", io);
      // System.out.printf("dest is %s currently\n", dest);
      for (int i = 0; i < io.width; i++) {
        final int bit = i;
        // System.out.printf("   selected bit %d? %s\n", i, 
        //     dest != null && dest.io == io && dest.bit == bit);
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(pinLabels[i]);
        menu.setSelected(dest != null && dest.io == io && dest.bit == bit);
        if (pinBindings.containsMappingFor(io, bit))
          menu.setForeground(MAPPED);
        menu.addActionListener(ev -> sources.mapCurrent(io, bit));
        popup.add(menu);
      }
      if (dest != null && dest.io == io) {
        JMenuItem menu = new JMenuItem("Remove Mapping");
        menu.addActionListener((ev) -> sources.unmapCurrent());
        popup.add(menu);
      }
      popup.show(this, e.getX(), e.getY());
    }

    void emphasize(boolean b) {
      if (select == b)
        return;
      select = b;
      repaint();
    }
  }

  private class SourcesModel extends AbstractListModel {
    ArrayList<Source> data = new ArrayList<>();
    SourcesModel() {
      sync();
    }
    void sync() {
      int n = data.size();
      if (n > 0) {
        data.clear();
        fireIntervalRemoved(this, 0, n-1);
      }
      for (Path path : pinBindings.components.keySet())
        data.add(pinBindings.sourceFor(path));
      Collections.sort(data, new NaturalOrderComparator<>(src -> src.path.toString()));
      n = data.size();
      fireIntervalAdded(this, 0, n-1);
      for (Source src : pinBindings.mappings.keySet())
        if (src.bit >= 0)
          expand(src.path, src.comp);
    }
    void changed(Source src) {
      int i = data.indexOf(src);
      if (i < 0)
        return;
      fireContentsChanged(this, i, i);
    }
    void expand(Path path, NetlistComponent comp) {
      int i = startIndex(path);
      if (i+1 < data.size() && data.get(i+1).path.equals(path))
        return; // already expanded
      ArrayList<Source> bitSources = pinBindings.bitSourcesFor(path);
      data.addAll(i+1, bitSources);
      fireIntervalAdded(this, i+1, i+1+bitSources.size()-1);
    }
    int startIndex(Path path) {
      for (int i = 0; i < data.size(); i++)
        if (data.get(i).equals(path))
          return i;
      return -1; // should never happen
    }
    @Override
    public Object getElementAt(int index) { return data.get(index); }
    @Override
    public int getSize() { return data.size(); }
  }

  private class SourceRenderer extends DefaultListCellRenderer {
    @Override
    public java.awt.Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      boolean done = false;
      if (value instanceof Source) {
        Source src = (Source)value;
        Dest dst = pinBindings.mappings.get(src);
        done = dst != null;
        int w = src.width.size();
        String dir = src.width.in > 0 ? "in" : src.width.out > 0 ? "out" : "inout";
        if (w == 1)
          value = String.format("%s [type %s, %s, 1 bit]", src.path, src.type, dir);
        else if (src.bit < 0)
          value = String.format("%s [type %s, %s, %d bits]", src.path, src.type, dir, w);
        else
          value = String.format("    %s [type %s, %s, bit %d]", src.path, src.type, dir, src.bit);
        if (dst != null)
          value = value + " mapped to " + dst;
        // todo: display header part of expanded source as grayed out, or
        // different, etc.
      }
      java.awt.Component c
          = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (c instanceof JComponent)
        ((JComponent)c).setForeground(done ? MAPPED : Color.BLACK);
        // ((JComponent)c).setBorder(matteBorder);
      return c;
    }
  }

  private class SourceList extends JList<Source> {
    SourcesModel model = new SourcesModel();
    Source current;
    SourceList() {
      setModel(model);
      setCellRenderer(new SourceRenderer());
      setFont(getFont().deriveFont(Font.PLAIN));
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      current = null;
      addListSelectionListener(e -> selected(getSelectedValue()));
    }
    void selected(Source src) {
      Source old = current;
      current = src;
      selectedChanged(old, current);
    }
    void mapCurrent(BoardIO io, int bit) {
      System.out.printf("Mapping %s of %s to %s\n", 
          (bit < 0 ? "all bits" : "bit "+bit), current, io);
      if (current == null)
          return;
      unmapCurrent();
      HashSet<Source> changed = pinBindings.addMapping(current, io, bit);
      for (Source src : changed)
        model.changed(src);
      selectedChanged(current, current);
    }
    void mapCurrent(BoardIO.Type synthType, int val) {
      if (current == null)
          return;
      if (synthType == BoardIO.Type.Constant) {
        while (true) {
          Object sel = JOptionPane.showInputDialog(this,
              "Enter a constant integer value (signed decimal, hex, or octal):", "Define Constant",
              JOptionPane.QUESTION_MESSAGE, null, null, "0x00000000");
          if (sel == null || sel.equals(""))
            return;
          try {
            val = Integer.decode(""+sel);
            break;
          } catch (NumberFormatException ex) { }
        }
      }
      BoardIO io = BoardIO.makeSynthetic(synthType, current.width.size(), val);
      mapCurrent(io, -1);
    }
    void unmapCurrent() {
      if (current == null)
        return;
      Dest old = pinBindings.mappings.get(current);
      if (old == null)
        return;
      pinBindings.mappings.remove(current);
      model.changed(current);
      selectedChanged(current, current);
    }
    void resetAll() {
      Source old = current;
      pinBindings.mappings.clear();
      model.sync();
      if (!model.data.contains(current))
        current = model.data.size() > 0 ? model.data.get(0) : null;
      selectedChanged(old, current);
    }
  }

  static final Color hilight = new Color(200, 0, 0);
  private class Synthetic extends JToggleButton {
    BoardIO.Type type;
    int val;
    Color oldBg = null;
    Synthetic(BoardIO.Type type, int val, String label, String icon, String tip) {
      super(label, Commander.getIcon(icon));
      this.type = type;
      this.val = val;
      setToolTipText(tip);
      setEnabled(false);
      addItemListener(e -> {
        if (oldBg == null)
          oldBg = getBackground();
        if(e.getStateChange() == ItemEvent.SELECTED)
          setBackground(hilight);
        else if (e.getStateChange() == ItemEvent.DESELECTED)
          setBackground(oldBg);
      });
      addActionListener(e -> {
        if (isSelected())
          sources.mapCurrent(type, val);
        else
          sources.unmapCurrent();
      });
    }
      
    // At least on Linux with openjdk 11.0.2, JToggleButton width is
    // underestimated consistently by about 6 pixels, resulting in a truncated
    // label. Correct the estimate here.
    @Override
    public Dimension getPreferredSize() {
      Dimension d = super.getPreferredSize();
      if (d == null)
        return null;
      d.width += 10;
      return d;
    }
    @Override
    public Dimension getMaximumSize() {
      Dimension d = super.getMaximumSize();
      if (d == null)
        return null;
      d.width += 10;
      return d;
    }
    @Override
    public Dimension getMinimumSize() {
      Dimension d = super.getMinimumSize();
      if (d == null)
        return null;
      d.width += 10;
      return d;
    }

  }

  void selectedChanged(Source oldSource, Source current) {
    updateStatus();
    if (oldSource == null && current == null)
      return;
    if (oldSource != null) {
      // De-emphasize rect for old mapping destination, if there was one.
      // Synthetics will be entirely reset below, so skip them here.
      Dest old = pinBindings.mappings.get(oldSource);
      if (old != null) {
        Rect r = rects.get(old.io);
        if (r != null)
          r.emphasize(false);
      }
    }
    if (current == null) {
      // No current selection, so hide all rects.
      for (Rect r : rects.values())
        r.setVisible(false);
      // No current selection, so disable and deselect all synthetics.
      for (Synthetic b : synthetics) {
        b.setSelected(false);
        b.setEnabled(false);
      }
    } else {
      if (oldSource != current) {
        for (Rect r : rects.values())
          r.setVisible(false);
        // Recalculate visibility of all rects and synthetics
        for (BoardIO io : pinBindings.compatibleResources(current)) {
          Rect r = rects.get(io);
          r.setVisible(true);
        }
        // ones, zeros, and constants are for source with direction "in"
        ones.setEnabled(current.width.in > 0);
        zeros.setEnabled(current.width.in > 0);
        constants.setEnabled(current.width.in > 0);
        // bitbucket is for source with direction "inout" or "out"
        bitbucket.setEnabled(current.width.inout > 0 || current.width.out > 0);
      }
      // Recalculate emphasis
      Dest dest = pinBindings.mappings.get(current);
      for (Synthetic b : synthetics)
        b.setSelected(dest != null && dest.io.type == b.type);
      for (Rect r : rects.values())
        r.emphasize(dest != null && r.io == dest.io);
    }
    overlay.repaint();
  }

  void updateStatus() {
    String txt = pinBindings.getStatus();
    status.setText(txt);
    boolean finished = txt.startsWith("All");
    done.setText(finished ? "Done" : "Close");
  }

  // private String getFileName(String window_name, String suggested_name) {
  //   JFileChooser fc = new JFileChooser(OldDirectory);
  //   fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
  //   fc.setDialogTitle(window_name);
  //   File SelFile = new File(OldDirectory + suggested_name);
  //   fc.setSelectedFile(SelFile);
  //   fc.setFileFilter(Loader.XML_FILTER);
  //   fc.setAcceptAllFileFilterUsed(false);
  //   int retval = fc.showSaveDialog(null);
  //   if (retval == JFileChooser.APPROVE_OPTION) {
  //     File file = fc.getSelectedFile();
  //     if (file.getParent() != null) {
  //       OldDirectory = file.getParent();
  //       if (OldDirectory == null)
  //         OldDirectory = "";
  //       else if (OldDirectory.length() != 0 && !OldDirectory.endsWith(File.separator))
  //         OldDirectory += File.separator;
  //     }
  //     return file.getPath();
  //   } else {
  //     return "";
  //   }
  // }

  // public void LoadDefaultSaved() {
  //   String suggestedName =
  //       CorrectLabel.getCorrectLabel(pinBindings.GetToplevelName())
  //       + "-" + board.getBoardName() + "-MAP.xml";
  // }

  // private String[] MapSectionStrings = { "Key", "LocationX", "LocationY", "Width", "Height", "Kind", "Value" };
  // private void Load() {
  //   JFileChooser fc = new JFileChooser(OldDirectory);
  //   fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
  //   fc.setDialogTitle("Choose XML board description file to use");
  //   fc.setFileFilter(Loader.XML_FILTER);
  //   fc.setAcceptAllFileFilterUsed(false);
  //   setVisible(false);
  //   int retval = fc.showOpenDialog(null);
  //   if (retval == JFileChooser.APPROVE_OPTION) {
  //     File file = fc.getSelectedFile();
  //     String FileName = file.getName();
  //     String AbsoluteFileName = file.getPath();
  //     OldDirectory = AbsoluteFileName.substring(0,
  //         AbsoluteFileName.length() - FileName.length());
  //     try {
  //       // Create instance of DocumentBuilderFactory
  //       DocumentBuilderFactory factory = DocumentBuilderFactory
  //           .newInstance();
  //       // Get the DocumentBuilder
  //       DocumentBuilder parser = factory.newDocumentBuilder();
  //       // Create blank DOM Document
  //       File xml = new File(AbsoluteFileName);
  //       Document MapDoc = parser.parse(xml);
  //       NodeList Elements = MapDoc
  //           .getElementsByTagName("LogisimGoesFPGABoardMapInformation");
  //       Node CircuitInfo = Elements.item(0);
  //       NodeList CircuitInfoDetails = CircuitInfo.getChildNodes();
  //       for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
  //         if (CircuitInfoDetails.item(i).getNodeName().equals("GlobalMapInformation")) {
  //           NamedNodeMap Attrs = CircuitInfoDetails.item(i)
  //               .getAttributes();
  //           for (int j = 0; j < Attrs.getLength(); j++) {
  //             if (Attrs.item(j).getNodeName().equals("BoardName")) {
  //               if (!board.getBoardName().equals(Attrs.item(j).getNodeValue())) {
  //                 status.setForeground(Color.RED);
  //                 status
  //                     .setText("LOAD ERROR: The selected Map file is not for the selected target board!");
  //                 setVisible(true);
  //                 return;
  //               }
  //             } else if (Attrs.item(j).getNodeName().equals("ToplevelCircuitName")) {
  //               if (!pinBindings.GetToplevelName().equals(Attrs.item(j).getNodeValue())) {
  //                 status.setForeground(Color.RED);
  //                 status
  //                     .setText("LOAD ERROR: The selected Map file is not for the selected toplevel circuit!");
  //                 setVisible(true);
  //                 return;
  //               }
  //             }
  //           }
  //           break;
  //         }
  //       }
  //       /* cleanup the current map */
  //       UnMapAll();
  //       for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
  //         if (CircuitInfoDetails.item(i).getNodeName().startsWith("MAPPEDCOMPONENT")) {
  //           int x = -1, y = -1, width = -1, height = -1, constval = -1;
  //           String key = "", kind = "";
  //           NamedNodeMap Attrs = CircuitInfoDetails.item(i).getAttributes();
  //           for (int j = 0; j < Attrs.getLength(); j++) {
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[0]))
  //               key = Attrs.item(j).getNodeValue();
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[1]))
  //               x = Integer.parseInt(Attrs.item(j).getNodeValue());
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[2]))
  //               y = Integer.parseInt(Attrs.item(j).getNodeValue());
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[3]))
  //               width = Integer.parseInt(Attrs.item(j).getNodeValue());
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[4]))
  //               height = Integer.parseInt(Attrs.item(j).getNodeValue());
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[5]))
  //               kind = Attrs.item(j).getNodeValue();
  //             if (Attrs.item(j).getNodeName().equals(MapSectionStrings[6]))
  //               constval = Integer.parseInt(Attrs.item(j).getNodeValue());
  //           }
  //           Bounds rect = null;
  //           if (key.isEmpty()) {
  //             rect = null;
  //           } else if ("constant".equals(kind)) {
  //             rect = Bounds.constant(constval);
  //           } else if ("device".equals(kind) || "".equals(kind)) {
  //             if ((x > 0) && (y > 0) && (width > 0) && (height > 0)) {
  //               for (BoardIO comp : board.GetAllComponents()) {
  //                 if ((comp.GetRectangle().getXpos() == x)
  //                     && (comp.GetRectangle().getYpos() == y)
  //                     && (comp.GetRectangle().getWidth() == width)
  //                     && (comp.GetRectangle().getHeight() == height)) {
  //                   rect = comp.GetRectangle();
  //                   break;
  //                 }
  //               }
  //             }
  //           } else if ("ones".equals(kind)) {
  //             rect = Bounds.ones();
  //           } else if ("zeros".equals(kind)) {
  //             rect = Bounds.zeros();
  //           } else if ("disconnected".equals(kind)) {
  //             rect = Bounds.disconnected();
  //           } 
  //           if (rect != null)
  //             pinBindings.TryMap(key, rect/* , board.GetComponentType(rect) */);
  //         }
  //       }
  //       ClearSelections();
  //       RebuildSelectionLists();
  //       picture.paintImmediately(0, 0, picture.getWidth(),
  //           picture.getHeight());
  //     } catch (Exception e) {
  //       /* TODO: handle exceptions */
  //       System.err.printf(
  //           "Exceptions not handled yet in Load(), but got an exception: %s\n",
  //           e.getMessage());
  //     }
  //   }
  //   setVisible(true);
  // }

  // private void Save() {
  //   setVisible(false);
  //   String suggestedName =
  //       CorrectLabel.getCorrectLabel(pinBindings.GetToplevelName())
  //       + "-" + board.getBoardName() + "-MAP.xml";
  //   String SaveFileName = getFileName("Select filename to save the current map", suggestedName);
  //   if (!SaveFileName.isEmpty()) {
  //     try {
  //       // Create instance of DocumentBuilderFactory
  //       DocumentBuilderFactory factory = DocumentBuilderFactory
  //           .newInstance();
  //       // Get the DocumentBuilder
  //       DocumentBuilder parser = factory.newDocumentBuilder();
  //       // Create blank DOM Document
  //       Document MapInfo = parser.newDocument();
  //       Element root = MapInfo
  //           .createElement("LogisimGoesFPGABoardMapInformation");
  //       MapInfo.appendChild(root);
  //       Element CircuitInfo = MapInfo
  //           .createElement("GlobalMapInformation");
  //       CircuitInfo.setAttribute("BoardName", board.getBoardName());
  //       Attr circ = MapInfo.createAttribute("ToplevelCircuitName");
  //       circ.setNodeValue(pinBindings.GetToplevelName());
  //       CircuitInfo.setAttributeNode(circ);
  //       root.appendChild(CircuitInfo);
  //       int count = 1;
  //       for (String key : pinBindings.sources()) {
  //         Element Map = MapInfo.createElement("MAPPEDCOMPONENT_"
  //             + Integer.toHexString(count++));
  //         Bounds rect = pinBindings.GetMap(key);
  //         Map.setAttribute(MapSectionStrings[0], key);
  //         Attr k = MapInfo.createAttribute(MapSectionStrings[5]);
  //         k.setValue("constant");
  //         Map.setAttributeNode(k);
  //         if (rect.isConstantInput()) {
  //           Attr v = MapInfo.createAttribute(MapSectionStrings[6]);
  //           v.setValue(Integer.toString(rect.getSyntheticInputValue()));
  //           Map.setAttributeNode(v);
  //         } else if (!rect.isDeviceSignal()) {
  //           Attr xpos = MapInfo.createAttribute(MapSectionStrings[1]);
  //           xpos.setValue(Integer.toString(rect.getXpos()));
  //           Map.setAttributeNode(xpos);
  //           Attr ypos = MapInfo.createAttribute(MapSectionStrings[2]);
  //           ypos.setValue(Integer.toString(rect.getYpos()));
  //           Map.setAttributeNode(ypos);
  //           Attr width = MapInfo.createAttribute(MapSectionStrings[3]);
  //           width.setValue(Integer.toString(rect.getWidth()));
  //           Map.setAttributeNode(width);
  //           Attr height = MapInfo.createAttribute(MapSectionStrings[4]);
  //           height.setValue(Integer.toString(rect.getHeight()));
  //           Map.setAttributeNode(height);
  //         }
  //         root.appendChild(Map);
  //       }
  //       TransformerFactory tranFactory = TransformerFactory.newInstance();
  //       tranFactory.setAttribute("indent-number", 3);
  //       Transformer aTransformer = tranFactory.newTransformer();
  //       aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
  //       Source src = new DOMSource(MapInfo);
  //       File file = new File(SaveFileName);
  //       Result dest = new StreamResult(file);
  //       aTransformer.transform(src, dest);
  //     } catch (Exception e) {
  //       /* TODO: handle exceptions */
  //       System.err.printf(
  //           "Exceptions not handled yet in Save(), but got an exception: %s\n",
  //           e.getMessage());
  //     }
  //   }
  //   setVisible(true);
  // }


	/*
	   The code below for NaturalOrderComparator comes from:
       https://github.com/paour/natorder/blob/master/NaturalOrderComparator.java
	   It has been altered for use in Logisim. The original file header is as follows:

	   NaturalOrderComparator.java -- Perform 'natural order' comparisons of strings in Java.
	   Copyright (C) 2003 by Pierre-Luc Paour <natorder@paour.com>

	   Based on the C version by Martin Pool, of which this is more or less a straight conversion.
	   Copyright (C) 2000 by Martin Pool <mbp@humbug.org.au>

	   This software is provided 'as-is', without any express or implied
	   warranty.  In no event will the authors be held liable for any damages
	   arising from the use of this software.

	   Permission is granted to anyone to use this software for any purpose,
	   including commercial applications, and to alter it and redistribute it
	   freely, subject to the following restrictions:

	   1. The origin of this software must not be misrepresented; you must not
	   claim that you wrote the original software. If you use this software
	   in a product, an acknowledgment in the product documentation would be
	   appreciated but is not required.
	   2. Altered source versions must be plainly marked as such, and must not be
	   misrepresented as being the original software.
	   3. This notice may not be removed or altered from any source distribution.
	*/

	private static class NaturalOrderComparator<T> implements Comparator<T> {
    Function<T, String> stringify;
    public NaturalOrderComparator(Function<T, String> f) {
      stringify = f;
    }
    @Override
		public int compare(T objA, T objB) {
      String a = stringify.apply(objA);
      String b = stringify.apply(objB);
			int na = a.length(), nb = b.length();
			int ia = 0, ib = 0;
			for (;; ia++, ib++) {
				char ca = charAt(a, ia, na);
				char cb = charAt(b, ib, nb);

				// skip spaces
				while (Character.isSpaceChar(ca))
					ca = charAt(a, ++ia, na);
				while (Character.isSpaceChar(cb))
					cb = charAt(b, ++ib, nb);

				// copmare numerical sequences
				if (Character.isDigit(ca) && Character.isDigit(cb))
				{
					int bias = 0;
					for (;; ia++, ib++)
					{
						ca = charAt(a, ia, na);
						cb = charAt(b, ib, nb);
						if (!Character.isDigit(ca) && !Character.isDigit(cb))
							break;
						else if (!Character.isDigit(ca))
							return -1; // a is less
						else if (!Character.isDigit(cb))
							return +1; // a is greater
						else if (bias == 0 && ca < cb)
							bias = -1; // a is less, if equal length
						else if (bias == 0 && ca > cb)
							bias = +1; // a is greater, if equal length
					}
					if (bias != 0)
						return bias;
				}

				// compare ascii
				if (ca < cb)
					return -1; // a is less
				else if (ca > cb)
					return +1; // a is greater
				else if (ca == 0 && cb == 0)
					return a.compareTo(b);
			}
		}

		static char charAt(String s, int i, int n) {
			return (i >= n ? 0 : s.charAt(i));
		}

		/*
		public static void main(String[] args) { // test program for NaturalOrderComparator
			String[] strings = new String[] {
				"1-2", "1-02", "1-20", "10-20",
					"fred", "jane",
					"pic2", "pic3", "pic4", "pic 4 else", "pic 5", "pic 5", "pic 5 something", "pic 6", "pic   7",
					"pic01", "pic02", "pic02a", "pic05",
					"pic100", "pic100a", "pic120", "pic121", "pic02000",
					"tom",
					"x2-g8", "x2-y7", "x2-y08", "x8-y8" };
			java.util.List list = java.util.Arrays.asList(strings);

			String orig = list.toString();
			System.out.println("Original: " + orig);

			Collections.shuffle(list);
			// System.out.println("Scrambled: " + list);
			Collections.sort(list, new NaturalOrderComparator());
			System.out.println("Sorted  : " + list);

			System.out.println("Correct? " + (orig.equals(list.toString())));
		}
		*/
	}


}
