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
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
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

// import com.bfh.logisim.data.Bounds;
import com.bfh.logisim.fpga.Board;
import com.bfh.logisim.fpga.BoardIO;
// import com.bfh.logisim.netlist.CorrectLabel;
import static com.bfh.logisim.gui.PinBindings.Source;
import static com.bfh.logisim.gui.PinBindings.Dest;

public class BindingsDialog implements ActionListener {

  private Board board;
  private PinBindings pinBindings;

  private JDialog panel;
  private Synthetic zeros, ones, constants, bitbucket;
  private Synthetic[] synthetics;

  private JButton unmap = new JButton();
  private JButton reset = new JButton();
  // private JButton save = new JButton();
  // private JButton load = new JButton();
  private JButton cancel = new JButton();
  private JButton done = new JButton();
  private JLabel status = new JLabel();

  private HashMap<BoardIO, Rect> rects = new HashMap<>();
  private SelectionPanel picture = new SelectionPanel();
  private SourceList sources = new SourceList();

  private String xmlDir;

  private boolean finished;

  public BindingsDialog(Board board, PinBindings pinBindings, JFrame parentFrame, String projectPath) {
    this.board = board;
    this.pinBindings = pinBindings;

    for (BoardIO io : board)
      rects.put(io, io.rect);

    xmlDir = new File(projectPath).getParent();
    if (xmlDir == null)
      xmlDir = "";
    else if (xmlDir.length() > 0 && !xmlDir.endsWith(File.separator))
      xmldir += File.separator;

    panel = new JDialog(parentFrame, ModalityType.APPLICATION_MODAL);
    panel.setTitle("Configure FPGA Pin Bindings");
    panel.setResizable(false);
    panel.setAlwaysOnTop(false);
    panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    panel.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    // Board picture
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 3;
    panel.add(picture, c);

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
    JPanel buttonpanel = new JPanel();
    for (Synthetic b : synthetics)
      buttonpanel.add(b);
    c.gridy++;
    panel.add(buttonpanel, c);

    // Labels and instructions
    JLabel header = new JLabel();
    header.setText("I/O Components in Design Under Test");
    header.setHorizontalTextPosition(JLabel.CENTER);
    header.setPreferredSize(new Dimension(picture.getWidth(), 25));
    header.setToolTipText("<html>Select a component and connect it to a board I/O resource.<br>
        + "Use drop-down menu to expand or change component type.</html>");
    c.gridy++;
    c.gridx = 0;
    c.gridwidth = 1;
    panel.add(header, c);

    // Component selection list
    JScrollPane scroll = new JScrollPane(sources);
    scroll.setPreferredSize(new Dimension(picture.getWidth(), 150));
    c.gridx = 1;
    c.gridheight = 9;
    panel.add(scroll, c);

    c.gridheight = 1;

    // Unmap button
    unmap.setText("Unset Component");
    unmap.addActionListener(e -> sources.unmapCurrent());
    c.gridy++;
    panel.add(unmap, c);

    // Reset button
    reset.setText("Reset All");
    reset.addActionListener(e -> sources.resetAll());
    c.gridy++;
    panel.add(reset, c);

    // load.setText("Load Map");
    // load.setActionCommand("Load");
    // load.addActionListener(this);
    // load.setEnabled(true);
    // c.gridy++;
    // panel.add(load, c);

    // save.setText("Save Map");
    // save.setActionCommand("Save");
    // save.addActionListener(this);
    // save.setEnabled(false);
    // c.gridy++;
    // panel.add(save, c);

    // Cancel button
    cancel.setText("Cancel");
    cancel.addActionListener(e -> {
      finished = false;
      panel.setVisible(false);
      panel.dispose();
    });
    cancel.setEnabled(true);
    c.gridy++;
    panel.add(cancel, c);

    // Done button
    done.setText("Done");
    done.setActionCommand("Done");
    done.addActionListener(e -> {
      // finished = true;
      panel.setVisible(false);
      panel.dispose();
    });
    c.gridy++;
    panel.add(done, c);

    // Status line
    c.gridx = 0;
    c.gridy++;
    c.gridwidth = 3;
    panel.add(status, c);

    if (sources.model.getSize() > 0)
      sources.setSelectedIndex(0);

    updateStatus();

    panel.pack();
    panel.setLocationRelativeTo(parentFrame);
    panel.setVisible(true);
  }

  private static class SelectionPanel extends JPanel {
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

  private class Rect extends JPanel implements MouseListener {
    BoardIO io;

    Rect(BoardIO io) {
      this.io = io;
      this.setPrefferedSize(new Dimension(io.rect.width, io.rect.height));
      setOpaque(false);
      setBackground(Color.RED); // todo: semi-transparent
      setVisible(false);
      addMouseListener(this)
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (!SwingUtilities.isLeftMouseButton())
        return;
      if (sources.current == null)
        return;
      if (sources.current.width.size() == 1 && io.width > 1)
        doBitSelectPopup();
      else if (pinBindings.mappings.get(sources.current))
        sources.unmapCurrent();
      else
        sources.mapCurrent(io, -1);
    }

    // todo: hilight/tint on hover
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }

    void doBitSelectPopup(MouseEvent e) {
      JPopupMenu popup = new JPopupMenu("Select Bit");
      ArrayList<String> pinLabels = io.type.getPinLabels(io.width);
      Dest dest = pinBindings.mappings.get(sources.current);
      for (int i = 0; i < io.width; i++) {
        JCheckBoxMenuItem menu = new JCheckBoxMenuItem(pinLabels.get(i));
        menu.setSelected(dest != null && dest.io == io && dest.bit == i);
        menu.addActionListener(e -> sources.mapCurrent(io, i));
        popup.add(menu);
      }
      if (dest != null && dest.io == io) {
        JMenuItem menu = new JMenuItem("Remove Mapping");
        menu.addActionListener(e -> sources.unmapCurrent());
        popup.add(menu);
      }
      popup.show(this, e.getX(), e.getY());
    }

    void emphasize(boolean b) {
      setBackground(b ? Color.YELLOW : Color.RED);
    }
  }

  private static class SourcesModel extends AbstractListModel {
    ArrayList<Source> data = new ArrayList<>();
    SourcesModel() {
      sync();
    }
    void sync() {
      int n = data.size();
      if (n > 0) {
        data.clear();
        fireIntervalRemoved(0, n-1);
      }
      for (Path path : pinBindings.components)
        data.add(new PinBindings.sourceFor(path));
      Collections.sort(data, new NaturalOrderComparator<>(src -> src.path.toString()));
      n = data.size();
      fireIntervalAdded(0, n-1);
      for (Source src : pinBindings.mappings)
        if (src.bit >= 0)
          expand(src.path, src.comp);
    }
    void changed(Source src) {
      int i = data.indexOf(src);
      if (i < 0)
        return;
      fireIntervalChanged(i, i);
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
        if (data.get(i).equals(path)
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
        done = pinBindings.mappings.containsKey(src);
        int w = in.width.size();
        String dir = in.width.in > 0 ? "in" : in.width.out > 0 ? "out" : "inout";
        if (w == 1)
          value = String.format("%s [type %s, %s, 1 bit]", src.path, src.type, dir);
        else if (src.bit < 0)
          value = String.format("%s [type %s, %s, %d bits]", src.path, src.type, dir, w);
        else
          value = String.format("    %s [type %s, %s, bit %d]", src.path, src.type, dir, src.bit);
        // todo: display header part of expanded source as grayed out, or
        // different, etc.
      }
      java.awt.Component c
          = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (c instanceof JComponent)
        ((JComponent)c).setForeground(done ? Color.BLUE : Color.BLACK);
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
      // setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      current = null;
      addListSelectionListener(e -> selected(getSelectedValuue()));
    }
    void selected(Source src) {
      Source old = current;
      current = src;
      selectedChanged(old, current);
    }
    void mapCurrent(BoardIO io, int bit) {
      if (current == null)
          return;
      unmapCurrent();
      HashSet<Source> changed = pinBindings.addMapping(current, io, -1);
      for (Source src : changed)
        model.changed(src);
      selectedChanged(current, current);
    }
    void mapCurrent(BoardIO synthType, int val) {
      if (current == null)
          return;
      if (synthType == BoardIO.Type.Constant) {
        while (true) {
          Object sel = JOptionPane.showInputDialog(panel,
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
      BoardIO io = BoardIO.makeSynthetic(synthType, current.width.size(). val);
      mapCurrent(io, -1);
    }
    void unmapCurrent() {
      if (current == null)
        return;
      Dest old = pinBindings.mapping.get(current);
      if (old == null)
        return;
      mappings.remove(current);
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

  private static class Synthetic extends JToggleButton {
    BoardIO.Type type;
    int val;
    Color oldBg = null;
    static final Color hilight = new Color(200, 0, 0);
    Synthetic(BoardIO.Type type, int val, String label, String icon, String tip) {
      super(label, getIcon(icon));
      this.type = type;
      this.val = val;
      setToolTipText(tip);
      setEnabled(false);
      addItemListener(e -> {
        if (oldBg == null)
          oldBg = getBackground();
        if(ev.getStateChange() == ItemEvent.SELECTED)
          setBackground(hilight);
        else if (ev.getStateChange() == ItemEvent.DESELECTED)
          setBackground(oldBg);
      });
      addActionListener(e -> {
        if (isSelected())
          sources.mapCurrent(type, val);
        else
          sources.unmapCurrent();
      });
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
        ones.setSelected(dest != null && dest.io.type == b.type);
      Rect r = rects.get(dest.io);
      if (r != null)
        r.emphasize(true);
    }
  }

  static ImageIcon getIcon(String name) {
    String path ="resources/logisim/icons/" + name;
    java.net.URL url = BindingsDialog.class.getClassLoader().getResource(path);
    return url == null ? null : new ImageIcon(url);
  }

  public boolean isDoneAssignment() {
    return finished;
  }

  void updateStatus() {
    int remaining = 0, count = 0;
    for (Path path : pinBindings.components.keySet()) {
      count++;
      if (!pinBindings.isMapped(path))
        remaining++;
    }
    finished = (remaining == 0);
    status.setForeground(finished ? Color.GREEN.darker() : Color.BLUE);
    status.setText(String.format("%d of %d components remaining to be mapped", remaining, count));
    done.setEnabled(finished);
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
  //   panel.setVisible(false);
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
  //                 panel.setVisible(true);
  //                 return;
  //               }
  //             } else if (Attrs.item(j).getNodeName().equals("ToplevelCircuitName")) {
  //               if (!pinBindings.GetToplevelName().equals(Attrs.item(j).getNodeValue())) {
  //                 status.setForeground(Color.RED);
  //                 status
  //                     .setText("LOAD ERROR: The selected Map file is not for the selected toplevel circuit!");
  //                 panel.setVisible(true);
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
  //   panel.setVisible(true);
  // }

  // private void Save() {
  //   panel.setVisible(false);
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
  //   panel.setVisible(true);
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
    public Natural(Function<T, String> f) {
      stringify = f;
    }
    @Override
		public int compare(T objA, T objB) {
      String a = stringify(objA);
      String b = stringify(objB);
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
