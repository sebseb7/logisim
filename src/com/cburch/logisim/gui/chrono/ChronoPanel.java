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
package com.cburch.logisim.gui.chrono;
import static com.cburch.logisim.gui.chrono.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.log.LogPanel;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.log.Selection;
import com.cburch.logisim.gui.log.SelectionItem;
import com.cburch.logisim.gui.main.SimulationToolbarModel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.cburch.logisim.gui.menu.EditHandler;

public class ChronoPanel extends LogPanel implements KeyListener, Model.Listener {

  private class MyListener implements ActionListener, AdjustmentListener {

    @Override
    public void actionPerformed(ActionEvent e) {
//       // load a chronogram from a file
//       if ("load".equals(e.getActionCommand())) {
//         final JFileChooser fc = new JFileChooser();
//         int returnVal = fc.showOpenDialog(ChronoPanel.this);
//         if (returnVal == JFileChooser.APPROVE_OPTION) {
//           loadFile(fc.getSelectedFile().getAbsolutePath());
//         }
// 
//         // export a chronogram to a file
//       } else if ("export".equals(e.getActionCommand())) {
//         final JFileChooser fc = new JFileChooser();
//         int returnVal = fc.showSaveDialog(ChronoPanel.this);
//         if (returnVal == JFileChooser.APPROVE_OPTION) {
//           exportFile(fc.getSelectedFile().getAbsolutePath());
//         }
// 
//       }
//       else if ("exportImg".equals(e.getActionCommand())) {
//         final JFileChooser fc = new JFileChooser();
//         int returnVal = fc.showSaveDialog(ChronoPanel.this);
//         if (returnVal == JFileChooser.APPROVE_OPTION) {
//           File file = fc.getSelectedFile();
// 
//           //add .png to the filename if the user forgot
//           if (!fc.getSelectedFile().getAbsolutePath().endsWith(".png")) {
//             file = new File(fc.getSelectedFile() + ".png");
//           }
//           exportImage(file);
//         }
// 
//       } else if ("play".equals(e.getActionCommand())) {
//         if (simulator.isRunning()) {
//           ((JButton) e.getSource()).setIcon(Icons
//           .getIcon("simplay.png"));
//         } else {
//           ((JButton) e.getSource()).setIcon(Icons
//           .getIcon("simstop.png"));
//         }
//         simulator.setIsRunning(!simulator.isRunning());
//       } else if ("step".equals(e.getActionCommand())) {
//         simulator.step();
//       } else if ("tplay".equals(e.getActionCommand())) {
//         if (simulator.isTicking()) {
//           ((JButton) e.getSource()).setIcon(Icons
//           .getIcon("simtplay.png"));
//         } else {
//           ((JButton) e.getSource()).setIcon(Icons
//           .getIcon("simtstop.png"));
//         }
//         simulator.setIsTicking(!simulator.isTicking());
//       } else if ("thalf".equals(e.getActionCommand())) {
//         simulator.tick(1);
//       } else if ("tfull".equals(e.getActionCommand())) {
//         simulator.tick(2);
//       }
    }


    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
      if (rightPanel != null)
        rightPanel.adjustmentValueChanged(e.getValue());
    }
  }

  public static final int HEADER_HEIGHT = 20;
  public static final int SIGNAL_HEIGHT = 30;
  public static final int GAP = 2; // gap above and below each signal
  public static final int INITIAL_SPLIT = 150;

  // state
  private Simulator simulator;
  private ChronoData data = new ChronoData();
  private Model model;

  // button bar
  private JPanel buttonBar = new JPanel();
  private JButton chooseFileButton = new JButton();
  private JButton exportDataInFile = new JButton();
  private JButton exportDataToImage = new JButton();

  // panels
  private RightPanel rightPanel;
  private LeftPanel leftPanel;
  private JScrollPane leftScroll, rightScroll;
  private JSplitPane splitPane;

  // listeners
  private MyListener myListener = new MyListener();

  public ChronoPanel(LogFrame logFrame) {
    super(logFrame);

    SELECT1 = UIManager.getDefaults().getColor("List.selectionBackground");
    SELECT2 = darker(SELECT1);
    SELECT = new Color[] { SELECT1, SELECT2 };

    simulator = getProject().getSimulator();

    setModel(logFrame.getModel());

    configure();
    resplit();

    editHandler.computeEnabled();
    // simulationHandler.computeEnabled();
  }

  private void configure() {
    setLayout(new BorderLayout());
    setFocusable(true);
    requestFocusInWindow();
    addKeyListener(this);

    // button bar
    Dimension buttonSize = new Dimension(150, 25);
    buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));

    chooseFileButton.setActionCommand("load");
    chooseFileButton.addActionListener(myListener);
    chooseFileButton.setPreferredSize(buttonSize);
    chooseFileButton.setFocusable(false);

    exportDataInFile.setActionCommand("export");
    exportDataInFile.addActionListener(myListener);
    exportDataInFile.setPreferredSize(buttonSize);
    exportDataInFile.setFocusable(false);

    exportDataToImage.setActionCommand("exportImg");
    exportDataToImage.addActionListener(myListener);
    exportDataToImage.setPreferredSize(buttonSize);
    exportDataToImage.setFocusable(false);

    LogFrame logFrame = getLogFrame();
    SimulationToolbarModel simTools;
    simTools = new SimulationToolbarModel(getProject(), logFrame.getMenuListener());
    Toolbar toolbar = new Toolbar(simTools);

    JPanel toolpanel = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gc = new GridBagConstraints();
    toolpanel.setLayout(gb);
    gc.fill = GridBagConstraints.NONE;
    gc.weightx = gc.weighty = 0.0;
    gc.gridx = gc.gridy = 0;
    gb.setConstraints(toolbar, gc);
    toolpanel.add(toolbar);

    JButton b = logFrame.makeSelectionButton();
    gc.gridx = 1;
    gb.setConstraints(b, gc);
    toolpanel.add(b);

    Component filler = Box.createHorizontalGlue();
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.weightx = 1.0;
    gc.gridx = 2;
    gb.setConstraints(filler, gc);
    toolpanel.add(filler);
    add(toolpanel, BorderLayout.NORTH);

    // statusLabel = new JLabel();
    buttonBar.add(chooseFileButton);
    buttonBar.add(exportDataInFile);
    buttonBar.add(exportDataToImage);
    add(BorderLayout.SOUTH, buttonBar);

    // panels
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setDividerSize(5);
    splitPane.setResizeWeight(0.0);
    add(BorderLayout.CENTER, splitPane);
  }

//  public void exportFile(String file) {
//    ChronoDataWriter.export(file, timelineParam, data);
//  }
//  public void exportImage(File file) {
//    ImageExporter ie = new ImageExporter(this, data, HEADER_HEIGHT);
//    ie.createImage(file);
//  }

  private void resplit() {
    // todo: why replace panels here?
    leftPanel = new LeftPanel(this);
    leftScroll = new JScrollPane(leftPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    int p = rightScroll == null ? 0 : rightScroll.getHorizontalScrollBar().getValue();
    if (rightPanel == null)
      rightPanel = new RightPanel(this, leftPanel.getSelectionModel());
    else
      rightPanel = new RightPanel(rightPanel, leftPanel.getSelectionModel());

    rightScroll = new JScrollPane(rightPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    rightScroll.getHorizontalScrollBar().addAdjustmentListener(myListener);

    // Synchronize the two scrollbars
    leftScroll.getVerticalScrollBar().setModel(
        rightScroll.getVerticalScrollBar().getModel());

    splitPane.setLeftComponent(leftScroll);
    splitPane.setRightComponent(rightScroll);

		setSignalCursor(rightPanel.getSignalCursor()); // sets cursor in both panels

    // put right scrollbar into same position
    rightScroll.getHorizontalScrollBar().setValue(p);
    rightScroll.getHorizontalScrollBar().setValue(p);

    // splitPane.setDividerLocation(INITIAL_SPLIT);
  }

  public ChronoData getChronoData() {
    return data;
  }

  public Selection getSelection() {
    return getLogFrame().getModel().getSelection();
  }

  public LeftPanel getLeftPanel() {
    return leftPanel;
  }

  public RightPanel getRightPanel() {
    return rightPanel;
  }

  public JScrollBar getVerticalScrollBar() {
    return rightScroll == null ? null : rightScroll.getVerticalScrollBar();
  }

  public JScrollBar getHorizontalScrollBar() {
    return rightScroll == null ? null : rightScroll.getHorizontalScrollBar();
  }

  public JViewport getRightViewport() {
    return rightScroll == null ? null : rightScroll.getViewport();
  }

  @Override
  public void keyPressed(KeyEvent ke) {
    // todo: tick, half tick, etc.
    int keyCode = ke.getKeyCode();
    if (keyCode == KeyEvent.VK_F2) {
      System.out.println("F2 tick");
      simulator.tick(2);
    }
  }

  @Override
  public void keyReleased(KeyEvent ke) { }

  @Override
  public void keyTyped(KeyEvent ke) { }

//   /**
//    * Load the chronogram from the log file
//    */
//   public void loadFile(String logFile) {
//     try {
//       ChronoData tmp = new ChronoData(logFile, this);
//       if (tmp != null) {
//         realTimeMode = false;
//         data = tmp;
//         resplit();
//         // statusLabel.setText(S.get("InputFileLoaded") + logFile);
//         System.out.println("imported file");
//       }
//     } catch (NoSysclkException ex) {
//       errorMessage(S.get("InputFileNoSysclk"));
//     } catch (Exception ex) {
//       errorMessage(ex.toString());
//     }
//   }

//  public void repaintAll(boolean force) {
//    rightPanel.repaintAll();
//
//    SwingUtilities.invokeLater(new Runnable() {
//      @Override
//      public void run() {
//        // scroll right to follow most recent data
//        int x = rightPanel.getSignalWidth();
//        rightScroll.getHorizontalScrollBar().setValue(x);
//        // SwingUtilities.updateComponentTreeUI(ChronoPanel.this);
//      }
//    });
//    //if (force)
//    //  SwingUtilities.updateComponentTreeUI(this);
//  }

  // todo
//   public void toggleBusExpand(SignalDataBus choosenBus, boolean expand) {
//     if (expand) {
//       data.expandBus(choosenBus);
//     } else {
//       data.contractBus(choosenBus);
//     }
//     resplit();
//   }

  @Override
  public String getTitle() {
    return S.get("ChronoTitle");
  }

  @Override
  public String getHelpText() {
    return S.get("ChronoTitle");
  }

  @Override
  public void localeChanged() {
    chooseFileButton.setText(S.get("ButtonLoad"));
    exportDataInFile.setText(S.get("ButtonExport"));
    exportDataToImage.setText(S.get("Export as image"));
  }

  // todo merge SelectionItem and Signal to preserve waveforms?
  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    setModel(newModel);
    setSignalCursor(Integer.MAX_VALUE);
    leftPanel.updateSignals();
    rightPanel.updateSignals();
  }

//  class ChronoMenuListener extends MenuListener {
//
//    protected class FileListener implements ActionListener {
//      public void actionPerformed(ActionEvent event) {
//        if (printer != null)
//          printer.actionPerformed(event);
//      }
//      boolean registered;
//      public void register(boolean en) {
//        if (registered == en)
//          return;
//        registered = en;
//        if (en) {
//          menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
//          menubar.addActionListener(LogisimMenuBar.PRINT, this);
//        } else {
//          menubar.removeActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
//          menubar.removeActionListener(LogisimMenuBar.PRINT, this);
//        }
//      }
//    }
//
//    private FileListener fileListener = new FileListener();
//    private PrintHandler printer;
//
//    public ChronoMenuListener(LogisimMenuBar menubar) {
//      super(menubar);
//      fileListener.register(false);
//      editListener.register();
//    }
//
//    public void setPrintHandler(PrintHandler printer) {
//      this.printer = printer;
//      fileListener.register(printer != null);
//    }
//  }

  public void changeSpotlight(ChronoData.Signal s) {
    ChronoData.Signal old = data.setSpotlight(s);
    if (old == s)
      return;
    rightPanel.changeSpotlight(old, s);
    leftPanel.changeSpotlight(old, s);
  }

//	public void mouseEntered(ChronoData.Signal s) {
//    changeSpotlight(s);
//	}
//
//	public void mousePressed(ChronoData.Signal s, int posX) {
//		setSignalCursor(posX);
//	}
//
//	public void mouseDragged(ChronoData.Signal s, int posX) {
//		setSignalCursor(posX);
//	}
//
//	public void mouseExited(ChronoData.Signal s) {
//    changeSpotlight(null);
//	}

  public void setSignalCursor(int posX) {
		rightPanel.setSignalCursor(posX);
    leftPanel.updateSignalValues();
  }

	@Override
	public void entryAdded(Model.Event event, Value[] values) {
    data.addSignalValues(values);
    leftPanel.updateSignalValues();
    rightPanel.updateWaveforms();
  }

	@Override
	public void resetEntries(Model.Event event, Value[] values) {
    data.resetSignalValues(values);
    setSignalCursor(Integer.MAX_VALUE);
    rightPanel.updateWaveforms();
  }

	@Override
	public void filePropertyChanged(Model.Event event) {
    // System.out.println("prop changed "  + event);
	}

	@Override
	public void selectionChanged(Model.Event event) {
    data.setSignals(model.getSelection(), model.getCircuitState());
    leftPanel.updateSignals();
    rightPanel.updateSignals();
	}

	public void toggleBusExpand(ChronoData.Signal s, boolean expand) {
    System.out.println("toggle bus");
    // todo: later
		// mChronoPanel.toggleBusExpand(signalDataSource, expand);
	}

	// public void zoom(int sens, int posX) {
  //   System.out.println("zoom");
  //   rightPanel.zoom(sens, posX);
	// }

	// public void zoom(ChronoData.Signal s, int sens, int val) {
  //   System.out.println("zoom");
	// 	rightPanel.zoom(sens, val);
	// }

  public void setModel(Model newModel) {
    if (model != null)
      model.removeModelListener(this);
    data.clear();
    model = newModel;
    if (model == null)
      return;

    data.setSignals(model.getSelection(), model.getCircuitState());

		model.addModelListener(this);
	}

	private static final Color SPOT1 = new Color(0xaa, 0xff, 0xaa);
	private static final Color SPOT2 = darker(SPOT1);
	private static final Color PLAIN1 = new Color(0xbb, 0xbb, 0xbb);
	private static final Color PLAIN2 = darker(PLAIN1);
  private final Color SELECT1; // set in constructor
  private final Color SELECT2; // set in constructor
  private static final Color[] SPOT = { SPOT1, SPOT2 };
  private static final Color[] PLAIN = { PLAIN1, PLAIN2 };
  private final Color[] SELECT; // set in constructor

  public Color[] rowColors(SelectionItem item, boolean isSelected) {
    if (isSelected)
      return SELECT;
    ChronoData.Signal spotlight = data.getSpotlight();
    if (spotlight != null && spotlight.info == item)
      return SPOT;
    return PLAIN;
  }

  private static Color darker(Color c) {
    float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null); 
    float s = 0.8f;
    if (hsb[1] == 0.0)
      return Color.getHSBColor(hsb[0], hsb[1] + hsb[1], hsb[2]*s);
    else
      return Color.getHSBColor(hsb[0], 1.0f - (1.0f - hsb[1])*s, hsb[2]);
  }

  // @Override
  // SimulationHandler getSimulationHandler() {
  //   return simulationHandler;
  // }

  @Override
  public EditHandler getEditHandler() {
    return editHandler;
  }

  EditHandler editHandler = new EditHandler() {
    @Override
    public void computeEnabled() {
      boolean empty = data.getSignalCount() == 0;
      boolean sel = !empty && !leftPanel.getSelectionModel().isSelectionEmpty();
      setEnabled(LogisimMenuBar.CUT, sel);
      setEnabled(LogisimMenuBar.COPY, sel);
      setEnabled(LogisimMenuBar.PASTE, true);
      setEnabled(LogisimMenuBar.DELETE, sel);
      setEnabled(LogisimMenuBar.DUPLICATE, false);
      setEnabled(LogisimMenuBar.SELECT_ALL, !empty);
      // todo: raise/lower handlers
      setEnabled(LogisimMenuBar.RAISE, false);
      setEnabled(LogisimMenuBar.LOWER, false);
      setEnabled(LogisimMenuBar.RAISE_TOP, false);
      setEnabled(LogisimMenuBar.LOWER_BOTTOM, false);
      setEnabled(LogisimMenuBar.ADD_CONTROL, false);
      setEnabled(LogisimMenuBar.REMOVE_CONTROL, false);
    }

    // todo

    // @Override
    // public void copy() {
    //   requestFocus();
    //   clip.copy();
    // }

    // @Override
    // public void paste() {
    //   requestFocus();
    //   clip.paste();
    // }

    // @Override
    // public void selectAll() {
    //   caret.selectAll();
    // }

    // @Override
    // public void delete() {
    //   requestFocus();
    //   Rectangle s = caret.getSelection();
    //   int inputs = table.getInputColumnCount();
    //   for (int c = s.x; c < s.x + s.width; c++) {
    //     if (c < inputs)
    //       continue; // todo: allow input row delete?
    //     for (int r = s.y; r < s.y + s.height; r++) {
    //       table.setVisibleOutputEntry(r, c - inputs, Entry.DONT_CARE);
    //     }
    //   }
    // }

  };

  // todo
//  @Override
//  PrintHandler getPrintHandler() {
//    return printHandler;
//  }
//
//  PrintHandler printHandler = new PrintHandler() {
//    @Override
//    public Dimension getExportImageSize() {
//      int width = tableWidth;
//      int height = headerHeight + bodyHeight;
//      return new Dimension(width, height);
//    }
//
//    @Override
//    public void paintExportImage(BufferedImage img, Graphics2D g) {
//      int width = img.getWidth();
//      int height = img.getHeight();
//      g.setClip(0, 0, width, height);
//      header.paintComponent(g, true, width, headerHeight);
//      g.translate(0, headerHeight);
//      body.paintComponent(g, true, width, bodyHeight);
//    }
//
//    @Override
//    public int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h) {
//      FontMetrics fm = g.getFontMetrics();
//
//      // shrink horizontally to fit
//      double scale = 1.0;
//      if (tableWidth > w)
//        scale = w / tableWidth;
//
//      // figure out how many pages we will need
//      int n = getRowCount();
//      double headHeight = (fm.getHeight() * 1.5 + headerHeight * scale);
//      int rowsPerPage = (int)((h - headHeight) / (cellHeight * scale));
//      int numPages = (n + rowsPerPage - 1) / rowsPerPage;
//      if (pageNum >= numPages)
//        return Printable.NO_SUCH_PAGE;
//
//      // g.drawRect(0, 0, (int)w-1, (int)h-1); // bage border
//      GraphicsUtil.drawText(g,
//          String.format("Combinational Analysis (page %d of %d)", pageNum+1, numPages),
//          (int)(w/2), 0, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
//
//      g.translate(0, fm.getHeight() * 1.5);
//      g.scale(scale, scale);
//      header.paintComponent(g, true, (int)(w/scale), headerHeight);
//      g.translate(0, headerHeight);
//
//      int yHeight = cellHeight * rowsPerPage;
//      int yTop = pageNum * yHeight;
//      g.translate(0, -yTop);
//      g.setClip(0, yTop, (int)(w/scale), yHeight);
//      body.paintComponent(g, true, (int)(w/scale), bodyHeight);
//
//      return Printable.PAGE_EXISTS;
//    }
//  };
}
