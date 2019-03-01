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

package com.bfh.logisim.fpgaboardeditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.RGBLed;
import com.cburch.logisim.std.io.SevenSegment;
import com.cburch.logisim.util.Errors;

// Each BoardIO represents one physical I/O resource, like an LED or button.
public class BoardIO {

  public static final EnumSet<Type> KnownTypes = EnumSet.range(Type.Button, Type.LED);
  public static final EnumSet<Type> InputTypes = EnumSet.range(Type.Button, Type.Pin);
  public static final EnumSet<Type> OutputTypes = EnumSet.range(Type.PortIO, Type.LED);
  public static final EnumSet<Type> InOutTypes = EnumSet.of(Type.PortIO, Type.Pin);

	public static String Type {
    // Note: The order here matters, because of EnumSet ranges above.
    Button,        // known in
    DIPSwitch,     // known in
    PortIO,        // known in out inout
    Pin,           // known in out inout
    SevenSegment,  // known    out
    RGBLED,        // known    out
		LED,           // known    out
    // Note: Bus does not appear in Board descriptions as a type of physical
    // resource, and no BoardIO will have this type. But logisim components can
    // request mappings of type Bus to indicate they need n-bits worth of Pin or
    // something compatable.
    Bus,
    Unknown;

		private static Type get(String str) {
			for (Type t : KnownTypes)
				if (t.name().equalsIgnoreCase(str))
					return t;
			return Type.Unknown;
		}

    public defaultWidth() {
      switch (this) {
      case LED:
      case Button:
      case Pin:
        return 1;
      case DIPSwitch:
        return (DipSwitch.MIN_SWITCH + DipSwitch.MAX_SWITCH)/2;
      case PortIO:
        return (PortIO.MIN_IO + PortIO.MAX_IO)/2;
      case SevenSegment:
        return 8;
      case RGBLED:
        return 3;
      default:
        return 0;
      }
    }

    public ArrayList<String> pinLabels(int width) {
      switch (this) {
      case SevenSegment:
        return SevenSegment.pinLabels();
      case RGBLED:
        return RGBLed.pinLabels();
      case DIPSwitch:
        return DipSwitch.pinLabels(width);
      case PortIO:
        return PortIO.pinLabels(width);
      default:
        return genericPinLabels(width);
      }
    }

    private ArrayList<String> genericPinLabels(int width) {
      ArrayList<String> a = new ArrayList<>();
      if (width == 1)
        a.add("Pin");
      else
        for (int i = 0; i < width; i++)
          a.add("Pin " + i);
      return a;
    }

	}

	public final Type type;
	public final int width;
  public final String label;
	public final BoardRectangle rect;
	public final IoStandard standard;
	public final PullBehavior pull;
	public final PinActivity activity;
	public final DriveStrength strength;

	private final String[] pins;

  private BoardIO(Type t, int w, String l, BoardRectangle r,
      IoStandard s, PullBehavior p, PinActivity a, DriveStrength g, String[] x) {
    type = t;
    width = w;
    label = l;
    rect = r;
    standard = s;
    pull = p;
    activity = a;
    strength = g;
    pins = x;
  }

  static BoardIO parseXml(Node node) throws Exception {
    Type t = Type.get(node.getNodeName());
    if (t == Type.UNKNOWN)
      throw new Exception("unrecognized I/O resource type: " + node.getNodeName());

    HashMap<String, String> xml = new HashMap<>();
    NamedNodeMap attrs = node.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      Node attr = attrs.item(i);
      String tag = attr.getNodeName();
      String val = attr.getNodeValue();
      params.put(tag, val);
    }

    String label = params.get("Label");
    int x = Integer.parseInt(params.getOrDefault("LocationX", "-1"));
    int y = Integer.parseInt(params.getOrDefault("LocationY", "-1"));
    int w = Integer.parseInt(params.getOrDefault("Width", "-1"));
    int h = Integer.parseInt(params.getOrDefault("Height", "-1"));
		if (x < 0 || y < 0 || w < 1 || h < 1)
      throw new Exception("invalid coordinates or size for I/O resource");
		BoardRectangle r = new BoardRectangle(x, y, w, h, label);

    PullBehavior p = (t == Types.Pin) ? PullBehavior.ACTIVE_HIGH
        : PullBehavior.get(params.get("FPGAPinPullBehavior"));
    PinActivity a = PinActivity.get(params.get("ActivityLevel"));
    IoStandard s = IoStandard.get(params.get("FPGAPinIOStandard"));
    DriveStrength g = DriveStrength.get(params.get("FPGAPinDriveStrength"));

    String[] pins;
		int width;
    if (params.containsKey("FPGAPinName")) {
      width = 1;
      pins = new String[] { params.get("FPGAPinName"); }
      if (pins[0] == null)
        throw new Exception("missing pin label for " + t + " " + label);
    } else {
      width = Integer.parseInt(params.get("NrOfPins"));
      if (width < 0)
        throw new Exception("invalid pin count for " + t + " " + label);
      pins = new String[width];
      for (int i = 0; width; i++) {
        pins[i] = params.get("FPGAPin_" + i);
        if (pins[i] == null)
          throw new Exception("missing pin label " + i + " for " + t + " " + label);
      }
    }

    return new BoardIO(t, width, label, r, s, p, a, g, pins);
	}

	public Element encodeXml(Document doc) throws Exception {
    Element elt = doc.createElement(type.toString());
    elt.setAttribute("LocationX", rect.x);
    elt.setAttribute("LocationY", rect.y);
    elt.setAttribute("Width", rect.width);
    elt.setAttribute("Height", rect.height);
    if (label != null)
      elt.setAttribute("Label", label);
    if (width == 1) {
      elt.setAttribute("FPGAPinName", pins[0]);
    } else {
      elt.setAttribute("NrOfPins", width);
      for (int i = 0; i < width; i++)
        elt.setAttribute("FPGAPin_"+i, pins[i]);
    }
    if (strength != DriveStrength.UNKNOWN)
      elt.setAttribute("FPGAPinDriveStrength", strength);
    if (activity != PinActivity.UNKNOWN)
      elt.setAttribute("ActivityLevel", activity);
    if (pull != PullBehavior.UNKNOWN && type != Type.PIN) // skip Pin
      elt.setAttribute("FPGAPinPullBehavior", pull);
    if (standard != IoStandard.UNKNOWN)
      elt.setAttribute("FPGAPinIOStandard", standard);
    return elt;
  }

	public BoardIO makeUserDefined(Type t, BoardRectangle r, BoardDialog parent) {
    int w = t.defaultWidth();
    BoardIO template = new BoardIO(t, w, null, r,
        IoStandard.UNKNOWN, PullBehavior.UNKNOWN, PinActivity.UNKNOWN, DriveStrength.UNKOWN,
        null);
    if (t == Type.DIPSwitch || t == Type.PortIO)
      template = doSizeDialog(template, parent);
    if (template == null)
      return null;
    template = doInfoDialog(template, parent);
    return template;
  }

  private static BoardIO doSizeDialog(BoardIO t, BoardDialog parent) {
    int min = t.type == DIPSwitch ? DipSwitch.MIN_SWITCH : PortIO.MIN_IO;
    int max = t.type == DIPSwitch ? DipSwitch.MAX_SWITCH : PortIO.MAX_IO;
    final int[] width = new int[] { t.width };

    final JDialog dlg = new JDialog(parent.panel, t.type + " Size");
    dlg.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    String things = t.type == DIPSwitch ? "switches" : "pins";
    JLabel question = new JLabel("Specify number of " + things + " in " + t.type + ":");

    JComboBox<Integer> size = new JComboBox<>();
    for (int i = min; i <= max; i++)
      size.addItem(i);
    size.setSelectedItem(width[0]);

    JButton next = new JButton("Next");
    next.addActionListener(e -> {
      width[0] = (Integer)size.getSelectedItem();
      dlg.setVisible(false);
    });

    c.gridx = 0;
    c.gridy = 0;
    dlg.add(question, c);

    c.gridx = 1;
    dlg.add(size, c);

    c.gridy++;
    dlg.add(next, c);

    dlg.pack();
    dlg.setLocation(Projects.getCenteredLoc(dlg.getWidth(), dlg.getHeight()));
    dlg.setModal(true);
    dlg.setResizable(false);
    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dlg.setAlwaysOnTop(true);
    dlg.setVisible(true);

    return new BoardIO(t.type, width[0], t.label,
        t.rect, t.standard, t.pull, t.activity, t.strength  t.pins);
  }

  private static DriveStrength defaultStrength = DriveStrength.DEFAULT;
  private static PinActivity defaultActivity = PinActivity.ACTIVE_HIGH;
  private static IoStandard defaultStandard = IoStandard.DEFAULT;
  private static PullBehavior defaultPull = PullBehavior.FLOAT;

  private static void add(JDialog dlg, GridBagConstraints c,
      String caption, JComponent input) {
      dlg.add(new JLabel(caption + " "), c);
      c.gridx++;
      dlg.add(input, c);
      c.gridx--;
      c.gridy++;
  }

  private static BoardIO doInfoDialog(BoardIO t, BoardDialog parent) {
    final JDialog dlg = new JDialog(parent.panel, t.type + " Properties");
    dlg.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    JComboBox<IoStandard> standard = new JComboBox<>(IoStandard.OPTIONS);
    JComboBox<DriveStrength> strength = new JComboBox<>(DriveStrength.OPTIONS);
    JComboBox<PinActivity> activity = new JComboBox<>(PinActivity.OPTIONS);
    JComboBox<PullBehavior> pull = new JComboBox<>(PullBehavior.OPTIONS);

    standard.setSelectedItem(defaultStandard);
    strength.setSelectedItem(defaultStrength);
    activity.setSelectedItem(defaultActivity);
    pull.setSelectedItem(defaultPull);

    if (!OutputTypes.contains(t.type))
      strength = null;
    if (!InputTypes.contains(t.type) || t.type == Type.Pin)
      pull = null;
    if (InOutTypes.contains(t.type))
      activity = null;

    JTextField label = new JTextField(6);
    if (t.label != null && t.label.length() > 0)
      label.setText(t.label);

    ArrayList<String> pinLabels = t.type.pinLabels(t.width);
    ArrayList<JTextField> pinLocs = new ArrayList<JTextField>();
    for (int i = 0; i < t.width; i++)
      pinLocs.add(new JTextField(6));

    c.gridx = 0;
    c.gridy = 0;

    for (int i = 0; i < t.width; i++) {
      add(dlg, c, pinLabels.get(i) + " location:", pinLocs.get(i));
      if (c.gridy == 32) {
        c.gridx += 2;
        c.gridy = 0;
      }
    }

    c.gridx = 0;
    c.gridy = Math.max(t.width, 32);

    add(dlg, c, "Label (optional):", label);
    add(dlg, c, "I/O standard:", standard);
    if (strength != null)
      add(dlg, c, "Drive strength:", strength);
    if (pull != null)
      add(dlg, c, "Pull behavior:", pull);
    if (activity != null)
      add(dlg, c, "Signal activity:", activity);

    final boolean[] good = new boolean{ true };
    JButton ok = new JButton("Save");
    ok.addActionListener(e -> dlg.setVisible(false));
    dlg.add(ok, c);
    c.gridx++;

    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> {
      good[0] = false;
      dlg.setVisible(false);
    });
    dlg.add(cancel, c);

    dlg.pack();
    dlg.setLocation(Projects.getCenteredLoc(dlg.getWidth(), dlg.getHeight()));
    dlg.setModal(true);
    dlg.setResizable(false);
    dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dlg.setAlwaysOnTop(true);

    String[] pins = new String[t.width];

    for (;;) {
      dlg.setVisible(true);
      if (!good[0])
        return null; // user cancelled
      // ensure all locations are specified
      boolean missing = false;
      for (int i = 0; i < t.width && !missing; i++) {
        pins[i] = pinLocs.get(i).getText();
        missing = pins[i] == null || pins[i].isEmpty();
      }
      if (!missing)
        break;
      Errors.title("Error").show("Please specify a location for all pins.");
    }

    String txt = label.getText();
    if (txt != null && txt.length() == 0)
      txt = null;

    defaultStandard = (IoStandard)standard.getSelectedItem();
    if (pull != null)
      defaultPull = (PullBehavior)pull.getSelectedItem();
    if (activity != null)
      defaultActivity = (PinActivity)activity.getSelectedItem();
    if (strength != null)
      defaultStrength = (DriveStrength)strength.getSelectedItem();

    PullBehavior p = pull != null ? defaultPull : PullBehavior.UNKNOWN;
    if (t.type == Type.Pin)
      p = PullBehavior.ACTIVE_HIGH; // special case: Pin is always active high

    return new BoardIO(t.type, t.width, txt, t.rect, defaultStandard, p,
        activity != null ? defaultActivity : PinActivity.UNKNOWN,
        strength != null ? defaultStrength : DriveStrength.UNKNOWN,
        pins);
  }

  public boolean isInput() {
    return InputTypes.contains(type);
  }

  public boolean isInputOutput() {
    return InOutTypes.contains(type);
  }

  public boolean isOutput() {
    return OutputTypes.contains(type);
  }
  
  @Override
  public String toString() {
    if (desc.equals("DIPSwitch") && size.size() > 0)
      return String.format("%s[%d bits]", desc, size);
    else if (desc.equals("PortIO") && size.size() > 0)
      return String.format("%s[%d in, %d inout, %d out]", desc, size.in, size.inout, size.out);
    else
      return desc;
  }

  public Int3 getPinCounts() {
    Int3 num = new Int3();
    switch (comp) {
    case Button:
    case DIPSwitch:
      num.in = width;
      break;
    case Pin:
    case PortIO:
      num.inout = width;
      break;
    case LED:
    case SevenSegment:
    case RGBLED:
      num.out = width;
      break;
    }
  }

  public ArrayList<String> getPinAssignments(char vendor, String direction, int startId) {
    ArrayList<String>() locs = new ArrayList<>();
    if (vendor == Chipset.ALTERA)
      getAlteraPinAssignments(locs, direction, startId);
    if (vendor == Chipset.XILINX)
      getXilinxUCFAssignments(locs, direction, startId);
    return locs;
  }

  private static String net(int i, String direction) {
    if (direction.equals("in"))
      return "FPGA_INPUT_PIN_" + i;
    else if (direction.equals("inout"))
      return "FPGA_INOUT_PIN_" + i;
    else
      return "FPGA_OUTPUT_PIN_" + i;
  }

	private void getAlteraPinAssignments(ArrayList<String> locs, String direction, int startId) {
    // Note: Only works for components that aren't very complex. (FIXME: why?)
		for (int i = 0; i < width; i++) {
			String net = net(startId + i, direction);
			locs.add("    set_location_assignment " + pins[i] + " -to " + net);
			if (pull == PullBehavior.PULL_UP)
				locs.add("    set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to " + net);
		}
	}

  private void getXilinxUCFAssignments(ArrayList<String> locs, String direction, int startId) {
    // Note: Only works for components that aren't very complex. (FIXME: why?)
    ArrayList<String> pinLabels = type.pinLabels(width);
    for (int i = 0; i < width; i++) {
      String net = net(startId + i, direction);
      String spec = "LOC = \"" + pads.get(i) + "\"";
      if (pull == PullBehavior.PULL_UP || pull == PullBehavior.PULL_DOWN)
        spec += " | " + pull;
      if (strength != DriveStrength.UNKNOWN && strength != DriveStrength.DEFAULT)
        spec += " | DRIVE = " + strength.ma;
      if (standard != IoStandard.Unknown && standard != IoStandard.DEFAULT)
        spec += " | IOSTANDARD = " + standard;
      spec += " ;"
      if (pinLabels != null)
        spec += " # " + pinLabels.get(i);
      locs.add("NET \"" + net + "\" " + spec);
    }
  }
}

