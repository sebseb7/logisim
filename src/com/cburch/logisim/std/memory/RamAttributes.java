/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

public class RamAttributes extends AbstractAttributeSet {

  static HexFrame getHexFrame(MemContents value, Project proj) {
    synchronized (windowRegistry) {
      HexFrame ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  /* here the rest is defined */
  static final AttributeOption VOLATILE = new AttributeOption("volatile",
      Strings.getter("ramTypeVolatile"));
  static final AttributeOption NONVOLATILE = new AttributeOption("nonvolatile",
      Strings.getter("ramTypeNonVolatile"));
  static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption(
      "type", Strings.getter("ramTypeAttr"), new AttributeOption[] {
        VOLATILE, NONVOLATILE });
  static final AttributeOption BUS_BIDIR = new AttributeOption("bidir",
      Strings.getter("ramBidirDataBus"));
  static final AttributeOption BUS_SEP = new AttributeOption("bibus",
      Strings.getter("ramSeparateDataBus"));
  static final Attribute<AttributeOption> ATTR_DBUS = Attributes.forOption(
      "databus", Strings.getter("ramDataAttr"), new AttributeOption[] {
        BUS_BIDIR, BUS_SEP });

  private static List<Attribute<?>> ATTRIBUTES = Arrays
      .asList(new Attribute<?>[] { Mem.ADDR_ATTR, Mem.DATA_ATTR, Mem.LINE_ATTR,
        StdAttr.TRIGGER, ATTR_TYPE, ATTR_DBUS, 
        Ram.CONTENTS_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT,
        StdAttr.APPEARANCE});

  private static WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<MemContents, HexFrame>();
  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private MemContents contents;
  private AttributeOption lineSize = Mem.SINGLE;
  private String Label = "";
  private AttributeOption Trigger = StdAttr.TRIG_RISING;
  private AttributeOption Type = VOLATILE; // NONVOLATILE;
  private AttributeOption BusStyle = BUS_SEP; // BUS_BIDIR;
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private AttributeOption Appearance = StdAttr.APPEAR_CLASSIC;

  RamAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RamAttributes d = (RamAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.Trigger = Trigger;
    d.BusStyle = BusStyle;
    d.LabelFont = LabelFont;
    d.Appearance = Appearance;
    d.contents = contents.clone();
    d.lineSize = lineSize;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Mem.ADDR_ATTR) {
      return (V) addrBits;
    }
    if (attr == Mem.DATA_ATTR) {
      return (V) dataBits;
    }
    if (attr == Ram.CONTENTS_ATTR) {
      return (V) contents;
    }
    if (attr == Mem.LINE_ATTR) {
      return (V) lineSize;
    }
    if (attr == StdAttr.LABEL) {
      return (V) Label;
    }
    if (attr == StdAttr.TRIGGER) {
      return (V) Trigger;
    }
    if (attr == ATTR_TYPE) {
      return (V) Type;
    }
    if (attr == ATTR_DBUS) {
      return (V) BusStyle;
    }
    if (attr == StdAttr.LABEL_FONT) {
      return (V) LabelFont;
    }
    if (attr == StdAttr.APPEARANCE) {
      return (V) Appearance;
    }
    return null;
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return !(Type.equals(VOLATILE) && attr.equals(Ram.CONTENTS_ATTR));
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      BitWidth newAddr = (BitWidth) value;
      if (addrBits == newAddr) {
        return;
      }
      addrBits = newAddr;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value);
    } else if (attr == Mem.DATA_ATTR) {
      BitWidth newData = (BitWidth) value;
      if (dataBits == newData) {
        return;
      }
      dataBits = newData;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
      fireAttributeValueChanged(attr, value);
    } else if (attr == Mem.LINE_ATTR) {
      AttributeOption newLine = (AttributeOption) value;
      if (newLine == lineSize)
        return;
      lineSize = newLine;
      fireAttributeValueChanged(attr, value);
    } else if (attr == Ram.CONTENTS_ATTR) {
      MemContents newContents = (MemContents) value;
      if (contents.equals(newContents)) {
        return;
      }
      if ((newContents.getLogLength() != addrBits.getWidth())
          || (newContents.getValueWidth() != dataBits.getWidth())) {
        newContents.setDimensions(addrBits.getWidth(),
            dataBits.getWidth());
      }
      contents = newContents;
      fireAttributeValueChanged(attr, value);
    } else if (attr == StdAttr.LABEL) {
      String NewLabel = (String) value;
      if (Label.equals(NewLabel)) {
        return;
      }
      Label = NewLabel;
      fireAttributeValueChanged(attr, value);
    } else if (attr == StdAttr.TRIGGER) {
      AttributeOption newTrigger = (AttributeOption) value;
      if (Trigger.equals(newTrigger)) {
        return;
      }
      Trigger = newTrigger;
      fireAttributeValueChanged(attr, value);
    } else if (attr == ATTR_TYPE) {
      AttributeOption NewType = (AttributeOption) value;
      if (Type.equals(NewType)) {
        return;
      }
      Type = NewType;
      fireAttributeValueChanged(attr, value);
    } else if (attr == ATTR_DBUS) {
      AttributeOption NewStyle = (AttributeOption) value;
      if (BusStyle.equals(NewStyle)) {
        return;
      }
      BusStyle = NewStyle;
      fireAttributeValueChanged(attr, value);
    } else if (attr == StdAttr.LABEL_FONT) {
      Font NewFont = (Font) value;
      if (LabelFont.equals(NewFont)) {
        return;
      }
      LabelFont = NewFont;
      fireAttributeValueChanged(attr, value);
    } else if (attr == StdAttr.APPEARANCE) {
      AttributeOption NewAppearance = (AttributeOption) value;
      if (Appearance.equals(NewAppearance))
        return;
      Appearance = NewAppearance;
      fireAttributeValueChanged(attr, value);
    }
  }
}
