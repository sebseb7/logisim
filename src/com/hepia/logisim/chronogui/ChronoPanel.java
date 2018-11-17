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
package com.hepia.logisim.chronogui;
import static com.hepia.logisim.chronogui.Strings.S;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.gui.log.LogPanel;
import com.cburch.logisim.gui.log.Model;
import com.cburch.logisim.gui.main.SimulationToolbarModel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.PrintHandler;
import com.hepia.logisim.chronodata.ChronoData;
import com.hepia.logisim.chronodata.ChronoModelEventHandler;

public class ChronoPanel extends LogPanel implements KeyListener {

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
  public static final int SIGNAL_HEIGHT = 38;
  public static final int INITIAL_SPLIT = 353;

  // data
  private Simulator simulator;
  private ChronoData chronoData = new ChronoData();

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
  private ChronoModelEventHandler modelListener;
  private MyListener myListener = new MyListener();

  public ChronoPanel(LogFrame logFrame) {
    super(logFrame);

    simulator = getProject().getSimulator();
    modelListener = new ChronoModelEventHandler(this, logFrame.getModel());

    configure();
    resplit();

    if (chronoData.getSignalCount() == 0)
      System.out.println("no signals"); // todo: show msg or button in left pane?

    // todo: allow drag and delete in left pane
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

    // menu and simulation toolbar
    MenuListener menu = new ChronoMenuListener(getLogisimMenuBar());
    SimulationToolbarModel toolbarModel =
        new SimulationToolbarModel(getProject(), menu);
    Toolbar toolbar = new Toolbar(toolbarModel);
    add(toolbar, BorderLayout.NORTH);

    // statusLabel = new JLabel();
    buttonBar.add(chooseFileButton);
    buttonBar.add(exportDataInFile);
    buttonBar.add(exportDataToImage);
    add(BorderLayout.SOUTH, buttonBar);

    // panels
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setDividerSize(5);
    add(BorderLayout.CENTER, splitPane);
  }

//  public void exportFile(String file) {
//    ChronoDataWriter.export(file, timelineParam, chronoData);
//  }
//  public void exportImage(File file) {
//    ImageExporter ie = new ImageExporter(this, chronoData, HEADER_HEIGHT);
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
      rightPanel = new RightPanel(this);
    else
      rightPanel = new RightPanel(rightPanel);

    rightScroll = new JScrollPane(rightPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    rightScroll.getHorizontalScrollBar().addAdjustmentListener(myListener);

    // Synchronize the two scrollbars
    leftScroll.getVerticalScrollBar().setModel(
        rightScroll.getVerticalScrollBar().getModel());

    splitPane.setLeftComponent(leftScroll);
    splitPane.setRightComponent(rightScroll);

		setSignalCursor(rightPanel.getSignalCursor());

    // put right scrollbar into same position
    rightScroll.getHorizontalScrollBar().setValue(p);
    rightScroll.getHorizontalScrollBar().setValue(p);

    splitPane.setDividerLocation(INITIAL_SPLIT);
  }

  public ChronoData getChronoData() {
    return chronoData;
  }

  public LeftPanel getLeftPanel() {
    return leftPanel;
  }

  public RightPanel getRightPanel() {
    return rightPanel;
  }

  public int getVisibleSignalsWidth() {
    return splitPane.getRightComponent().getWidth();
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
//         chronoData = tmp;
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

  public void repaintAll(boolean force) {
    rightPanel.repaintAll();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // scroll right to follow most recent data
        scrollTo(rightPanel.getSignalWidth());
        SwingUtilities.updateComponentTreeUI(ChronoPanel.this);
      }
    });
    if (force)
      SwingUtilities.updateComponentTreeUI(this);
  }

  public void scrollTo(int pos) {
    rightScroll.getHorizontalScrollBar().setValue(pos);
  }

  // todo
//   public void toggleBusExpand(SignalDataBus choosenBus, boolean expand) {
//     if (expand) {
//       chronoData.expandBus(choosenBus);
//     } else {
//       chronoData.contractBus(choosenBus);
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

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    modelListener.setModel(newModel);
  }

  class ChronoMenuListener extends MenuListener {

    protected class FileListener implements ActionListener {
      public void actionPerformed(ActionEvent event) {
        if (printer != null)
          printer.actionPerformed(event);
      }
      boolean registered;
      public void register(boolean en) {
        if (registered == en)
          return;
        registered = en;
        if (en) {
          menubar.addActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
          menubar.addActionListener(LogisimMenuBar.PRINT, this);
        } else {
          menubar.removeActionListener(LogisimMenuBar.EXPORT_IMAGE, this);
          menubar.removeActionListener(LogisimMenuBar.PRINT, this);
        }
      }
    }

    private FileListener fileListener = new FileListener();
    private PrintHandler printer;

    public ChronoMenuListener(LogisimMenuBar menubar) {
      super(menubar);
      fileListener.register(false);
      editListener.register();
    }

    public void setPrintHandler(PrintHandler printer) {
      this.printer = printer;
      fileListener.register(printer != null);
    }
  }

  public void highlight(ChronoData.Signal s) {
    rightPanel.highlight(s);
    leftPanel.highlight(s);
  }

	public void mouseEntered(ChronoData.Signal s) {
		highlight(s);
	}

	public void mousePressed(ChronoData.Signal s, int posX) {
		setSignalCursor(posX);
	}

	public void mouseDragged(ChronoData.Signal s, int posX) {
		setSignalCursor(posX);
	}

	public void mouseExited(ChronoData.Signal s) {
	}

	public void setCodingFormat(ChronoData.Signal s, String format) {
    // todo later
		// s.setFormat(format);
		// repaintAll(true);
	}

  public void setSignalCursor(int posX) {
		rightPanel.setSignalCursor(posX);
    leftPanel.setSignalsValues(posX);
  }

	public void toggleBusExpand(ChronoData.Signal s, boolean expand) {
    // todo: later
		// mChronoPanel.toggleBusExpand(signalDataSource, expand);
	}

	public void zoom(int sens, int posX) {
    rightPanel.zoom(sens, posX);
	}

	public void zoom(ChronoData.Signal s, int sens, int val) {
		rightPanel.zoom(sens, val);
	}

}
