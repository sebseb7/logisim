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

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.util.StringUtil;
import com.cburch.draw.util.EditableLabel;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.draw.shapes.SvgCreator;

public class RegisterShape extends DynamicElement {
	static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 10);

	private EditableLabel label;

	public RegisterShape(int x, int y, DynamicElement.Path p) {
		super(p, null);
		label = new EditableLabel(x, y, "", DEFAULT_FONT);
		label.setColor(Color.BLACK);
		label.setHorizontalAlignment(EditableLabel.LEFT);
		label.setVerticalAlignment(EditableLabel.TOP);
		calculateBounds();
	}

	void calculateBounds() {
		BitWidth widthVal = path.leaf().getAttributeSet().getValue(StdAttr.WIDTH);
		int width = (widthVal == null ? 8 : widthVal.getWidth());
		String zeros = StringUtil.toHexString(width, 0);
		label.setText(zeros);
		bounds = label.getBounds();
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return UnmodifiableList.create(new Attribute<?>[] { Text.ATTR_FONT });
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == Text.ATTR_FONT) {
			return (V) label.getFont();
		} else {
			return null;
		}
	}

	@Override
	public void updateValue(Attribute<?> attr, Object value) {
		if (attr == Text.ATTR_FONT) {
			label.setFont((Font)value);
			calculateBounds();
		}
	}

	@Override
	public boolean matches(CanvasObject other) {
		if (other.getClass().equals(RegisterShape.class)) {
			RegisterShape that = (RegisterShape) other;
			return this.bounds.equals(that.bounds);
		} else {
			return false;
		}
	}

	@Override
	public void paintDynamic(Graphics g, CircuitState state) {
		calculateBounds();
		int x = bounds.getX();
		int y = bounds.getY();
		int w = bounds.getWidth();
		int h = bounds.getHeight();
		GraphicsUtil.switchToWidth(g, 1);
		g.setColor(Color.lightGray);
		g.fillRect(x, y, w, h);
		g.setColor(Color.BLACK);
		g.drawRect(x, y, w, h);
		if (state != null) {
			BitWidth widthVal = path.leaf().getAttributeSet().getValue(StdAttr.WIDTH);
			int width = (widthVal == null ? 8 : widthVal.getWidth());
			RegisterData data = (RegisterData)getData(state);
			int val = data == null ? 0 : data.value;
			label.setText(StringUtil.toHexString(width, val));
		}
		label.paint(g);
	}

	@Override
	public Element toSvgElement(Document doc) {
		Element ret = doc.createElement("visible-register");
		ret.setAttribute("x", "" + bounds.getX());
		ret.setAttribute("y", "" + bounds.getY());
		ret.setAttribute("width", "" + bounds.getWidth());
		ret.setAttribute("height", "" + bounds.getHeight());
		Font font = label.getFont();
		if (!font.equals(DEFAULT_FONT))
			SvgCreator.setFontAttribute(ret, font);
		ret.setAttribute("path", path.toSvgString());
		return ret;
	}

	public static RegisterShape fromSvgElement(Element elt, Circuit circuit) {
		try {
			String pathstr = elt.getAttribute("path");
			DynamicElement.Path path = DynamicElement.Path.fromSvgString(pathstr, circuit);
			if (path == null)
				return null;
			double x = Double.parseDouble(elt.getAttribute("x"));
			double y = Double.parseDouble(elt.getAttribute("y"));
			RegisterShape shape = new RegisterShape((int)x, (int)y, path);
			if (elt.hasAttribute("font-family"))
				shape.setValue(Text.ATTR_FONT, SvgReader.getFontAttribute(elt));
			return shape;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getDisplayName() {
		return Strings.get("registerComponent");
	}

	@Override
	public String toString() {
		return "Register:" + getBounds();
	}
}
