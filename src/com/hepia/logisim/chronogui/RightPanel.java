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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.hepia.logisim.chronodata.SignalData;

/**
 * Chronogram's right side Panel Composed of one TimeLine on top and multiple
 * SignalDraw
 */
public class RightPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private ChronoPanel mChronoPanel;
	private DrawAreaEventManager mDrawAreaEventManager;
	private TimelineDraw mTimeLine;
	private CommonPanelParam mCommonPanelParam;

	private ArrayList<SignalDraw> allSignalDraw = new ArrayList<SignalDraw>();
	private Box rightBox;
	private JLayeredPane layeredPane;
	private Cursor mCursor;

	private int mousePosXClicked = 0;
	private static final int minTickWidth = 1;
	private int tickWidth = 20;
	private int displayOffsetX = 0;

	private int globalHeight;

	/**
	 * Standard constructor
	 */
	public RightPanel(ChronoPanel chronoPanel,
			DrawAreaEventManager drawAreaEventManager) {
		this.mChronoPanel = chronoPanel;
		this.mDrawAreaEventManager = drawAreaEventManager;
		this.mCommonPanelParam = chronoPanel.getCommonPanelParam();
		this.globalHeight = mCommonPanelParam.getSignalHeight()
				* chronoPanel.getChronoData().size();
		this.setLayout(new BorderLayout());
		this.setBackground(Color.white);
		createPanel();
	}

	/**
	 * Clone constructor
	 */
	public RightPanel(RightPanel oldPanel) {
		this.mChronoPanel = oldPanel.mChronoPanel;
		this.mDrawAreaEventManager = oldPanel.mDrawAreaEventManager;
		this.mCommonPanelParam = mChronoPanel.getCommonPanelParam();
		this.globalHeight = mCommonPanelParam.getSignalHeight()
				* mChronoPanel.getChronoData().size();
		this.tickWidth = oldPanel.tickWidth;
		this.mousePosXClicked = oldPanel.mousePosXClicked;
		this.displayOffsetX = oldPanel.displayOffsetX;
		this.setLayout(new BorderLayout());
		this.setBackground(Color.white);
		createPanel();
	}

	public void adjustmentValueChanged(int value) {
		float posPercent = (float) value / (float) getSignalWidth();
		int i = Math.round(/* mChronoPanel.getNbrOfTick()*/ 2 * posPercent);
		i = i > 5 ? i - 5 : 0;
		displayOffsetX = i * tickWidth;
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setBufferObsolete();
			sDraw.repaint();
		}
	}

	/**
	 * Creates and add all component: -timeline -all signalDraw -cursor
	 */
	private void createPanel() {
		rightBox = Box.createVerticalBox();
		rightBox.setOpaque(true);

		// Add the time line
		mTimeLine = new TimelineDraw(mChronoPanel,
				mCommonPanelParam.getHeaderHeight(), tickWidth);

		// creates the SignalDraw
		for (String signalName : mChronoPanel.getChronoData().getSignalOrder()) {
			if (!signalName.equals("sysclk"))
				allSignalDraw.add(new SignalDraw(this, mDrawAreaEventManager,
						mChronoPanel.getChronoData().get(signalName),
						mCommonPanelParam.getSignalHeight()));
		}

		// add the signals to the box
		for (SignalDraw sDraw : allSignalDraw) {
			rightBox.add(sDraw);
		}

		// add the cursor
		mCursor = new Cursor();

		// creates a JLayeredPane, to put the Cursor in front of the SignalDraw
		// and the timeline
		layeredPane = new JLayeredPane();

		defineSizes();

		layeredPane.add(mCursor, new Integer(1));
		layeredPane.add(mTimeLine, new Integer(0));
		layeredPane.add(rightBox, new Integer(0));

		this.add(layeredPane, BorderLayout.WEST);
	}

	private void defineSizes() {
		int totalWidth = tickWidth * 2; // mChronoPanel.getNbrOfTick();
		layeredPane.setPreferredSize(new Dimension(totalWidth, globalHeight));
		rightBox.setBounds(0, mCommonPanelParam.getHeaderHeight(), totalWidth,
				globalHeight);
		mTimeLine.setBounds(0, 0, totalWidth,
				mCommonPanelParam.getHeaderHeight());
		mCursor.setBounds(0, 0, totalWidth, globalHeight);
	}

	/**
	 * Set the cursor position
	 */
	public void drawVerticalMouseClicked() {
		drawVerticalMouseClicked(mousePosXClicked);
	}

	/**
	 * Set the cursor position
	 */
	public void drawVerticalMouseClicked(int posX) {
		mCursor.setPosition(posX);
		mCursor.repaint();
		mousePosXClicked = posX;
	}

	public int getDisplayOffsetX() {
		return displayOffsetX;
	}

	public int getMousePosXClicked() {
		return mousePosXClicked;
	}

	public int getSignalWidth() {
		return /* mChronoPanel.getNbrOfTick()*/ 2 * tickWidth;
	}

	public int getTickWidth() {
		return tickWidth;
	}

	public int getVisibleWidth() {
		return mChronoPanel.getVisibleSignalsWidth();
	}

    public int getTotalWidth() {
        return (/*mChronoPanel.getNbrOfTick()*/ 2  * tickWidth);
    }

    public int getTotalHeight() {
        return globalHeight;
    }
	/**
	 * Highlight a signal in bold
	 */
	public void highlight(SignalData signalToHighlight) {
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.highlight(sDraw.getSignalData() == signalToHighlight);
		}
	}

    public ArrayList<SignalDraw> getAllSdraws() {
        return allSignalDraw;
    }

	/**
	 * Repaint the cursor and all signalDraw
	 */
	public void repaintAll() {
		mCursor.repaint();
		int width;
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setBufferObsolete();
			sDraw.repaint();
			if (mChronoPanel.isRealTimeMode()) {
				width = getSignalWidth();
				sDraw.setSignalDrawSize(width,
						mCommonPanelParam.getSignalHeight());
				mTimeLine.setTimeLineSize(width);
				defineSizes();
			}
		}
	}

	public void zoom(int sens, int posX) {
		int nbrOfTick = mousePosXClicked / tickWidth;

		tickWidth += sens;
		if (tickWidth <= minTickWidth)
			tickWidth = minTickWidth;

		// make the mousePosXClicked follow the zoom
		int newPosX = nbrOfTick * tickWidth;
		mousePosXClicked = newPosX;
		// set the cusor position
		mCursor.setPosition(newPosX);

		// Scrollbar follow the zoom
		int scrollBarCursorPos = mCursor.getPosition()
				- (mChronoPanel.getVisibleSignalsWidth() / 2);

		// zoom on every signals
		for (SignalDraw sDraw : allSignalDraw) {
			sDraw.setTickWidth(tickWidth);
		}

		// zoom on the timeline
		mTimeLine.setTickWidth(tickWidth, 2 /* mChronoPanel.getNbrOfTick() */);

		defineSizes();

		// force redraw everything
		SwingUtilities.updateComponentTreeUI(mChronoPanel);

		// scrollbar position
		mChronoPanel.setScrollbarPosition(scrollBarCursorPos);
	}
}
