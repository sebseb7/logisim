/* This file is adopted from the MIPS.jar library by
 * Martin Dybdal <dybber@dybber.dk> and
 * Anders Boesen Lindbo Larsen <abll@diku.dk>.
 * It was developed for the computer architecture class at the Department of
 * Computer Science, University of Copenhagen.
 */

package com.cburch.logisim.std.gates;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Font;
import java.awt.Window;

import javax.swing.JLabel;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.gui.main.Frame;

class PLA extends InstanceFactory {
	private static final int IN_PORT = 0;
	private static final int OUT_PORT = 1;

	private static final Attribute<BitWidth> ATTR_IN_WIDTH
		= Attributes.forBitWidth("in_width", Strings.getter("Bit Width In"));
	private static final Attribute<BitWidth> ATTR_OUT_WIDTH
		= Attributes.forBitWidth("out_width", Strings.getter("Bit Width Out"));
	private static Attribute<PLATable> ATTR_TABLE = new TruthTableAttribute();

	public static InstanceFactory FACTORY = new PLA();

	private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);
	
	private static final List<Attribute<?>> ATTRIBUTES = Arrays.asList(new Attribute<?>[] {
				ATTR_IN_WIDTH, ATTR_OUT_WIDTH, ATTR_TABLE,
				StdAttr.LABEL, StdAttr.LABEL_FONT
		});

	private static class TruthTableAttribute extends Attribute<PLATable> {
		public TruthTableAttribute() { 
			super("table", Strings.getter("Program"));
		}
		
		@Override
		public java.awt.Component getCellEditor(Window source, PLATable tt) {
			return ContentsCell.getEditor((Frame)source, tt);
		}

		@Override
		public String toDisplayString(PLATable value) { 
			return Strings.get("(click to edit)");
		}

		@Override 
		public String toStandardString(PLATable tt) {
			return tt.toStandardString();
		}
		
		@Override
		public PLATable parse(String str) {
			return PLATable.parse(str);
		}
	}

	private static class ContentsCell extends JLabel implements MouseListener {
		PLATable tt;
		Frame parent;

		private ContentsCell(Frame parent, PLATable tt) {
			super(Strings.get("(click to edit)"));
			this.tt = tt;
			this.parent = parent;
			addMouseListener(this);
			//mouseClicked(null);
		}

		static ContentsCell getEditor(Frame parent, PLATable tt) {
			ContentsCell editor = new ContentsCell(parent, tt);
			editor.mouseClicked(null); // this cannot be called in constructor
			return editor;
		}

		public void mouseClicked(MouseEvent e) {
			if (tt == null) return;
			PLATable.EditorDialog dialog = new PLATable.EditorDialog(this.parent);
			dialog.showAndResize(tt);
			dialog.toFront();
		}

		public void mousePressed(MouseEvent e) { }
		public void mouseReleased(MouseEvent e) { }
		public void mouseEntered(MouseEvent e) { }
		public void mouseExited(MouseEvent e) { }
	}
	

	private class PLAAttributes extends AbstractAttributeSet {
		private String label = "PLA";
		private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
		private BitWidth widthIn = BitWidth.create(2);
		private BitWidth widthOut = BitWidth.create(2);
		private PLATable tt = new PLATable(2, 2);

		@Override
		protected void copyInto(AbstractAttributeSet destObj) {
			PLAAttributes dest = (PLAAttributes) destObj;
			dest.label = this.label;
			dest.labelFont = this.labelFont;
			dest.widthIn = this.widthIn;
			dest.widthOut = this.widthOut;
			dest.tt = this.tt;
		}

		@Override
		public List<Attribute<?>> getAttributes() { return ATTRIBUTES; }

		@Override
		@SuppressWarnings("unchecked")
		public <V> V getValue(Attribute<V> attr) {
			if (attr == ATTR_IN_WIDTH)  return (V) widthIn;
			if (attr == ATTR_OUT_WIDTH) return (V) widthOut;
			if (attr == ATTR_TABLE) return (V) tt;
			if (attr == StdAttr.LABEL) return (V) label;
			if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
			return null;
		}

		@Override
		public <V> void setValue(Attribute<V> attr, V value) {
			if (attr == ATTR_IN_WIDTH) {
				widthIn = (BitWidth) value;
				tt.pendingInputSize(widthIn.getWidth());
			} else if (attr == ATTR_OUT_WIDTH) {
				widthOut = (BitWidth) value;
				tt.pendingOutputSize(widthOut.getWidth());
			} else if (attr == ATTR_TABLE) {
				tt = (PLATable) value;
			} else if (attr == StdAttr.LABEL) {
				label = (String) value;
			} else if (attr == StdAttr.LABEL_FONT) {
				labelFont = (Font) value;
			} else {
				throw new IllegalArgumentException("unknown attribute " + attr);
			}
			fireAttributeValueChanged(attr, value);
		}
	}
	
	private static class PLAExpression implements ExpressionComputer {
		private Instance instance;
		
		public PLAExpression(Instance instance) {
			this.instance = instance;
		}
		
		public void computeExpression(Map expressionMap) {
			// fixme: compute expressions
			AttributeSet attrs = instance.getAttributeSet();
			int intValue = 5;

			expressionMap.put(instance.getLocation(), 0,
					Expressions.constant(intValue));
		}
	}
	
	public PLA() {
		super("PLA", Strings.getter("PLA"));
		setIconName("pla.gif");
	}

	@Override
	public AttributeSet createAttributeSet() {
		return new PLAAttributes();
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		super.configureNewInstance(instance);
		PLAAttributes attributes = (PLAAttributes)instance.getAttributeSet();
		attributes.tt = new PLATable(instance.getAttributeValue(ATTR_TABLE));
		
		instance.addAttributeListener();
		updatePorts(instance);
	}
	
	private void updatePorts(Instance instance) {
		Port[] ps = { new Port(0, 0, Port.INPUT, ATTR_IN_WIDTH),
		              new Port(50, 0, Port.OUTPUT, ATTR_OUT_WIDTH) };
		instance.setPorts(ps);
	}

	@Override
	protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		if (attr == ATTR_OUT_WIDTH) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == ATTR_IN_WIDTH) {
			instance.recomputeBounds();
			updatePorts(instance);
		} else if (attr == ATTR_TABLE) {
			instance.fireInvalidated();
		}
	}
	
	@Override
	protected Object getInstanceFeature(Instance instance, Object key) {
		if (key == ExpressionComputer.class)
			return new PLAExpression(instance);
		return super.getInstanceFeature(instance, key);
	}

	@Override
	public void propagate(InstanceState state) {
		BitWidth outWidth = state.getAttributeValue(ATTR_OUT_WIDTH);
		PLATable tt = state.getAttributeValue(ATTR_TABLE);
		Value input = state.getPortValue(IN_PORT);
		int val = tt.valueFor(input.toIntValue());
		state.setPort(1, Value.createKnown(outWidth, val), 1);
	}

	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		Bounds ret = Bounds.create(0, -25, 50, 50);
		return ret;
	}

	@Override
	public void paintGhost(InstancePainter painter) {
		paintInstance(painter, true);
	}
	
	@Override
	public void paintInstance(InstancePainter painter) {
		paintInstance(painter, false);
	}

	void paintInstance(InstancePainter painter, boolean ghost) {
		Graphics g = painter.getGraphics();
		Bounds bds = painter.getBounds();
		int x = bds.getX();
		int y = bds.getY();
		int w = bds.getWidth();
		int h = bds.getHeight();

		if (!ghost && painter.shouldDrawColor()) {
			g.setColor(BACKGROUND_COLOR);
			g.fillRect(x, y, w, h);
		}

		if (!ghost)
			g.setColor(Color.BLACK);
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRect(x, y, bds.getWidth(), bds.getHeight());

		g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
		String label = painter.getAttributeValue(StdAttr.LABEL);
		GraphicsUtil.drawCenteredText(g, label, x+w/2, y+h/3);
		if (!ghost) {
			if (painter.getShowState()) {
				PLATable tt = painter.getAttributeValue(ATTR_TABLE);
				Value input = painter.getPortValue(IN_PORT);
				String comment = tt.commentFor(input.toIntValue());
				GraphicsUtil.drawCenteredText(g, comment, x+w/2, y+2*h/3);
			}
			painter.drawPorts();
		}
	}
}
