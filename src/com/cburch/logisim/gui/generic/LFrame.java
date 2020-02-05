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

package com.cburch.logisim.gui.generic;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import com.cburch.logisim.Main;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.WindowClosable;

public class LFrame extends JFrame implements WindowClosable {
  public static void attachIcon(Window frame) {
    attachIcon(frame, PATH);
  }
  public static void attachIcon(Window frame, String pathfmt) {
    ArrayList<Image> icons;
    Image defaultIcon = null;
    synchronized(ICONS) {
      icons = ICONS.get(pathfmt);
      defaultIcon = DEFAULT_ICON.get(pathfmt);
    }
    if (icons == null) {
      icons = new ArrayList<Image>();
      ClassLoader loader = LFrame.class.getClassLoader();
      for (int size : SIZES) {
        URL url = loader.getResource(pathfmt.replaceAll("%d", ""+size));
        if (url != null) {
          ImageIcon icon = new ImageIcon(url);
          icons.add(icon.getImage());
          if (size == DEFAULT_SIZE)
            defaultIcon = icon.getImage();
        }
      }
      synchronized(ICONS) {
        ICONS.put(pathfmt, icons);
        DEFAULT_ICON.put(pathfmt, defaultIcon);
      }
    }

    boolean success = false;
    try {
      if (icons != null && !icons.isEmpty()) {
        // Method set = frame.getClass().getMethod("setIconImages", List.class);
        // set.invoke(frame, ICONS);
        frame.setIconImages(icons);
        success = true;
      }
    } catch (Exception e) {
    }

    if (!success && frame instanceof JFrame && defaultIcon != null)
      ((JFrame) frame).setIconImage(defaultIcon);
  }

  private static final long serialVersionUID = 1L;
  private static final String PATH = "resources/logisim/img/logisim-icon-%d.png";
  private static final int[] SIZES = { 16, 20, 24, 48, 64, 128 };
  private static final int DEFAULT_SIZE = 48;

  private static final HashMap<String, ArrayList<Image>> ICONS = new HashMap<>();
  private static final HashMap<String, Image> DEFAULT_ICON = new HashMap<>();


  // A main window holds a circuit, always has menubar with Close, Save, etc.
  public static final int MAIN_WINDOW = 1;
  // A sub-window is either standalone or is associated with a project, always
  // has a menubar but without Close, Save, etc. If associated with a project,
  // the window will close when the project closes.
  public static final int SUB_WINDOW = 2;
  // A dialog is either standalone or is associated with a project, and doesn't
  // have a menubar except on MacOS where it is mostly empty. If associated with
  // a project, the window will close when the project closes.
  public static final int DIALOG = 3;

  protected final LogisimMenuBar menubar;
  protected final Project project;
  protected final int type; 

  public static class MainWindow extends LFrame {
    public MainWindow(Project p) {
      super(MAIN_WINDOW, p, true);
      if (p == null)
        throw new IllegalArgumentException("project is null");
    }
  }
  public static class SubWindow extends LFrame {
    public SubWindow(Project p) { // may be null
      super(SUB_WINDOW, p, false);
    }
  }
  public static class SubWindowWithSimulation extends LFrame {
    public SubWindowWithSimulation(Project p) { // may be null
      super(SUB_WINDOW, p, true);
    }
  }
  public static class Dialog extends LFrame {
    public Dialog(Project p) { // may be null
      super(DIALOG, p, false);
    }
  }

  private LFrame(int t, Project p, boolean enableSim) {
    project = p;
    type = t;
    attachIcon(this);
    if (type == MAIN_WINDOW) {
      menubar = new LogisimMenuBar(this, p, p, p);
      setJMenuBar(menubar);
    } else if (type == SUB_WINDOW || Main.MacOS) {
      // use null project so there will be no Close, Save, etc.
      menubar = new LogisimMenuBar(this, null, p, enableSim ? p : null);
      setJMenuBar(menubar);
    } else {
      menubar = null;
    }
    if (type != MAIN_WINDOW && project != null) {
      project.getFrame().addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          LFrame.this.dispose();
        }
      });
    }
    addComponentListener(new ComponentListener() {
      @Override
      public void componentMoved(ComponentEvent evt) {
        updateGeometryUponChangingDevice();
      }
      @Override
      public void componentShown(ComponentEvent evt) { }
      @Override
      public void componentResized(ComponentEvent evt) { }
      @Override
      public void componentHidden(ComponentEvent evt) { }
    });
  }

  @Override
  public void requestClose() {
    WindowEvent closing = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
    processWindowEvent(closing);
  }

  public Project getProject() {
    return project;
  }

  // public boolean isMainWindow() {
  //   return type == MAIN_WINDOW;
  // }

  public LogisimMenuBar getLogisimMenuBar() {
    return menubar;
  }

  private static String str(Rectangle r) {
    return String.format("at (%d, %d) size %d x %d",
        r.x, r.y, r.width, r.height);
  }

  // Attempt to address issue #45, "Black screen when program is on Primary
  // Monitor".
  // See also: 
  // https://stackoverflow.com/questions/42367058/application-turns-black-when-dragged-from-a-4k-monitor-to-a-fullhd-monitor
  // https://bugs.openjdk.java.net/browse/JDK-8175527
  private int curDeviceIdx = -1;
  private GraphicsDevice curDevice = null;
  private GraphicsConfiguration curConfiguration = null;
  private void updateGeometryUponChangingDevice() {
    String pref = AppPreferences.DUALSCREEN.get();
    if (pref.equals(AppPreferences.DUALSCREEN_NONE))
      return;

    Rectangle wb = getBounds();
    int width = getWidth();
    int height = getHeight();
    GraphicsConfiguration newConf = getGraphicsConfiguration();
    GraphicsDevice newDevice = newConf.getDevice();
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

    int newDeviceIdx = -1;
    GraphicsDevice[] allDevices = env.getScreenDevices();
    for (int i = 0; i < allDevices.length; i++) {
      if (allDevices[i].equals(newDevice)) {
        if (newDeviceIdx == -1)
          newDeviceIdx = i;
        else
          System.out.printf("DUAL SCREEN ERROR: %d and %d are both the same device: %s\n", newDeviceIdx, i, newDevice);
      }
    }

    if (newDeviceIdx == -1)
      System.out.printf("DUAL SCREEN ERROR: device is not listed: %s\n", newDevice);

    if (newDeviceIdx == curDeviceIdx && newDevice.equals(curDevice) && newConf.equals(curConfiguration))
      return; // no change in device or configuration

    Rectangle sb = newConf.getBounds();
    // It is important to set the bounds somewhere on the new display before
    // setting the extended state to maximized
            
    System.out.printf("Dual Screen Fix [%s]:\n"
        + "  moved from device: (%d) %s\n"
        + "                     %s\n"
        + "      to new device: (%d) %s\n"
        + "                     %s\n"
        + "  new screen bounds: %s\n"
        + "   current geometry: %s\n",
        pref, curDeviceIdx, curDevice, curConfiguration,
        newDeviceIdx, newDevice, newConf,
        str(sb), str(wb));
      
    int x = wb.x, y = wb.y;
    if (pref.equals(AppPreferences.DUALSCREEN_FIX)) {
      // fix #1 - if current geom is fully within the new display, then save
      // geom, hide, refresh geom, maximize, unhide, and remember display
      if (!sb.contains(wb)) {
        System.out.printf("  --> not refreshing, because window splits across screens\n");
        return;
      }
    } else if (pref.equals(AppPreferences.DUALSCREEN_MORE)) {
      // fix #2 - save current geom, hide, adjust geom to force it to be
      // fully within new display, maximize, unhide, and remember display
      width = Math.min(sb.width, width);
      height = Math.min(sb.height, height);
      if (x < sb.x) x = sb.x;
      else if (x > sb.x + sb.width - width) x = sb.x + sb.width - width;
      if (y < sb.y) y = sb.y;
      else if (y > sb.y + sb.height - height) y = sb.y + sb.height - height;
    } else if (pref.equals(AppPreferences.DUALSCREEN_MOST)) {
      // fix #3 - ignore current position, hide, pick a new position fully
      // within new display, maximize, unhide, and remember display
      width = Math.min(sb.width, Math.max(100, width));
      height = Math.min(sb.height, Math.max(100, height));
      x = sb.x + (sb.width - width) / 2;
      y = sb.y + (sb.height - height) / 2;
    }

    System.out.printf("  adjusted geometry: at (%d, %d) size %d x %d\n", x, y, width, height);
    System.out.printf("  --> refreshing geometry due to device change\n");

    setVisible(false);
    setBounds(x, y, width, height);
    setExtendedState(Frame.MAXIMIZED_BOTH);
    setVisible(true);

    curDeviceIdx = newDeviceIdx;
    curDevice = newDevice;
    curConfiguration = newConf;
  }
}
