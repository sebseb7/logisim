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

package com.cburch.logisim.std.hdl;

import java.util.Arrays;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitEvent;

public abstract class HdlContent implements HdlModel, Cloneable {

	protected static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	protected EventSourceWeakSupport<HdlModelListener> listeners;
	//protected EventSourceWeakSupport<CircuitListener> circlisteners;

	protected HdlContent() {
		this.listeners = null;
	}

	@Override
	public void addHdlModelListener(HdlModelListener l) {
                //System.out.println("Adding " + l.getClass() + " for " + toString());
		if (listeners == null) {
			listeners = new EventSourceWeakSupport<HdlModelListener>();
		}
		listeners.add(l);
	}

        /*
        public void addCircuitListener(CircuitListener l) {
		if (circlisteners == null) {
			circlisteners = new EventSourceWeakSupport<CircuitListener>();
		}
		circlisteners.add(l);
        }
        public void removeCircuitListener(CircuitListener l) {
		if (circlisteners == null) {
			return;
		}
		circlisteners.remove(l);
        }
        */

	@Override
	public HdlContent clone() throws CloneNotSupportedException {
		HdlContent ret = (HdlContent) super.clone();
                //System.out.println("cloning");
		ret.listeners = null;
                int n = 0;
                if (listeners != null) {
                    for (HdlModelListener l : listeners) {
                        //System.out.println("dropping " + l.getClass());
                        n++;
                    }
                }
                //System.out.println("cloned from " + n + " to " + 0);
		return ret;
	}

        public String toString() {
            String s = super.toString() + " " + listeners;
                if (listeners != null) {
            s += " [";
                    for (HdlModelListener l : listeners) {
                        s += "\n" + l.getClass() + " " + l.toString();
                    }
                s += "\n]";
                }
                return s;
        }

	@Override
	public abstract boolean compare(HdlModel model);

	@Override
	public abstract boolean compare(String value);

	protected void fireContentSet() {
		if (listeners == null) {
                        //System.out.println("no listeners on " + listeners);
			return;
		}

		boolean found = false;
		for (HdlModelListener l : listeners) {
			found = true;
                        //System.out.println(this.toString() + " notifying " + l.getClass() + " " + l);
			l.contentSet(this);
		}

		if (!found) {
                        //System.out.println("clearning listeners " + listeners);
			listeners = null;
		}
                // fireNameChange();
	}

        /*
        protected void fireNameChange() {
		if (circlisteners == null) {
			return;
		}

		boolean found = false;
		for (CircuitListener l : circlisteners) {
			found = true;
			l.circuitChanged(CircuitEvent.ACTION_SET_NAME);
		}

		if (!found) {
			circlisteners = null;
		}
	}
        */

	@Override
	public abstract String getContent();

	@Override
	public abstract String getName();

	@Override
	public void removeHdlModelListener(HdlModelListener l) {
                //System.out.println("Removing " + l.getClass() + " for " + toString());
		if (listeners == null) {
			return;
		}
		listeners.remove(l);
		if (listeners.isEmpty()) {
			listeners = null;
		}
	}

	@Override
	public abstract boolean setContent(String content); // does not necessarily validate

}
