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

package com.cburch.logisim.gui.log;
import static com.cburch.logisim.gui.log.Strings.S;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import com.hepia.logisim.chronodata.TimelineParam;
import com.hepia.logisim.chronogui.ChronoPanel;

public class LogFrame extends LFrame {
  private class MyListener implements ProjectListener,
          LibraryListener, SimulatorListener, LocaleListener {

    public void libraryChanged(LibraryEvent event) {
      int action = event.getAction();
      if (action == LibraryEvent.SET_NAME) {
        setTitle(computeTitle(curModel, project));
      }
    }

    public void localeChanged() {
      setTitle(computeTitle(curModel, project));
      for (int i = 0; i < panels.length; i++) {
        tabbedPane.setTitleAt(i, panels[i].getTitle());
        tabbedPane.setToolTipTextAt(i, panels[i].getToolTipText());
        panels[i].localeChanged();
      }
      windowManager.localeChanged();
    }

    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_STATE) {
        setSimulator(event.getProject().getSimulator(),
            event.getProject().getCircuitState());
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        setTitle(computeTitle(curModel, project));
      }
    }

    public void propagationCompleted(SimulatorEvent e) {
      curModel.propagationCompleted();
    }

    public void simulatorStateChanged(SimulatorEvent e) {
    }

    public void tickCompleted(SimulatorEvent e) {
    }
  }

  // TODO should automatically repaint icons when component attr change
  // TODO ? moving a component using Select tool removes it from selection
  private class WindowMenuManager extends WindowMenuItemManager
    implements LocaleListener, ProjectListener, LibraryListener {
    WindowMenuManager() {
      super(S.get("logFrameMenuItem"), false);
      project.addProjectListener(this);
      project.addLibraryListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return LogFrame.this;
    }

    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.SET_NAME) {
        localeChanged();
      }
    }

    public void localeChanged() {
      String title = project.getLogisimFile().getDisplayName();
      setText(S.fmt("logFrameMenuItem", title));
    }

    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
        localeChanged();
      }
    }
  }

  private static String computeTitle(Model data, Project proj) {
    String name = data == null ? "???" : data.getCircuitState()
        .getCircuit().getName();
    return S.fmt("logFrameTitle", name, proj
        .getLogisimFile().getDisplayName());
  }

  private static final long serialVersionUID = 1L;
  private Simulator curSimulator = null;
  private Model curModel;
  private Map<CircuitState, Model> modelMap = new HashMap<CircuitState, Model>();
  private MyListener myListener = new MyListener();

  private WindowMenuManager windowManager;
  private LogPanel[] panels;
  // private SelectionPanel selPanel;
  private JTabbedPane tabbedPane;

  static class SelectionDialog extends JDialogOk {
    SelectionPanel selPanel;
    SelectionDialog(LogFrame logFrame) {
      super("Signal Selection", false);
      selPanel = new SelectionPanel(logFrame);
      selPanel.localeChanged();
      getContentPane().add(selPanel);
      setMinimumSize(new Dimension(350, 300));
      setSize(400, 400);
      pack();
      setVisible(true);
    }
    public void cancelClicked() { okClicked(); }
    public void okClicked() { }
  }

  static class TempButtonPanel extends LogPanel {
    TempButtonPanel(LogFrame frame) {
      super(frame);
      JButton button = new JButton("press m");
      add(button);
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
          SelectionDialog d = new SelectionDialog(getLogFrame());
          d.setVisible(true);
        }
      });
    }
    public String getHelpText() { return "temp"; }
    public String getTitle() { return "button"; }
    public void localeChanged() { }
    public void modelChanged(Model oldModel, Model newModel) { }
  }

  public LogFrame(Project project) {
    super(false, project);
    this.windowManager = new WindowMenuManager();
    project.addProjectListener(myListener);
    project.addLibraryListener(myListener);
    setSimulator(project.getSimulator(), project.getCircuitState());

    // selPanel = new SelectionPanel(this);
    panels = new LogPanel[] {
      // selPanel,
      new TempButtonPanel(this),
      new ScrollPanel(this),
      new FilePanel(this),
      new ChronoPanel(this),
    };
    tabbedPane = new JTabbedPane();
    // tabbedPane.setFont(new Font("Dialog", Font.BOLD, 9));
    for (int index = 0; index < panels.length; index++) {
      LogPanel panel = panels[index];
      tabbedPane.addTab(panel.getTitle(), null, panel,
          panel.getToolTipText());
    }

    Container contents = getContentPane();
    tabbedPane.setPreferredSize(new Dimension(550, 300));
    contents.add(tabbedPane, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
    setLocationRelativeTo(project.getFrame());
  }

  public Model getModel() {
    return curModel;
  }

  LogPanel[] getPrefPanels() {
    return panels;
  }

  public TimelineParam getTimelineParam() {
    return new TimelineParam("Hz", "clk", 10);
    // return selPanel.getTimelineParam();
  }

  private void setSimulator(Simulator value, CircuitState state) {
    if ((value == null) == (curModel == null)) {
      if (value == null || value.getCircuitState() == curModel.getCircuitState())
        return;
    }

    if (curSimulator != null)
      curSimulator.removeSimulatorListener(myListener);
    if (curModel != null)
      curModel.setSelected(this, false);

    Model oldModel = curModel;
    Model data = null;
    if (value != null) {
      data = modelMap.get(value.getCircuitState());
      if (data == null) {
        data = new Model(value.getCircuitState());
        modelMap.put(data.getCircuitState(), data);
      }
    }
    curSimulator = value;
    curModel = data;

    if (curSimulator != null)
      curSimulator.addSimulatorListener(myListener);
    if (curModel != null)
      curModel.setSelected(this, true);
    setTitle(computeTitle(curModel, project));
    if (panels != null) {
      for (int i = 0; i < panels.length; i++) {
        panels[i].modelChanged(oldModel, curModel);
      }
    }
  }

  @Override
  public void setVisible(boolean value) {
    if (value) {
      windowManager.frameOpened(this);
    }
    super.setVisible(value);
  }
}
