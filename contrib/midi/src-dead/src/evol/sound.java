package kahdeg.sound;

import javax.sound.midi.*;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.DurationAttribute;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class sound extends InstanceFactory{
	
	private static final BitWidth BIT_WIDTH4 = BitWidth.create(4);
	private static final BitWidth BIT_WIDTH7 = BitWidth.create(7);
	private static final BitWidth BIT_WIDTH8 = BitWidth.create(8);
	
	static AttributeOption instlist[];
	static Attribute<AttributeOption> ATTR_INSTR;
	static Attribute<Integer> ATTR_CHAN;

	public sound(){
		super("sound emitter");
		init();
		setAttributes(new Attribute[] { ATTR_INSTR, ATTR_CHAN },
                new Object[] { instlist[0], 0 });
		setOffsetBounds(Bounds.create(-30, -30, 30, 60));
		Port[] ps=new Port[5];
        ps[0]=new Port(-30, 0, Port.INPUT, BIT_WIDTH8);
		ps[0].setToolTip(new SimpleStringGetter("Note"));
        ps[1]=new Port(-30, 10, Port.INPUT, BIT_WIDTH7);
		ps[1].setToolTip(new SimpleStringGetter("Velocity"));
		ps[2]=new Port(-30, -10, Port.INPUT, 1);
		ps[2].setToolTip(new SimpleStringGetter("On / Off"));
        ps[3]=new Port(-30, -20, Port.INPUT, BIT_WIDTH4);
		ps[3].setToolTip(new SimpleStringGetter("Channel"));
        ps[4]=new Port(-30, 20, Port.INPUT, BIT_WIDTH8);
		ps[4].setToolTip(new SimpleStringGetter("Instrument"));
		setPorts(ps);
		setIconName("speaker.gif");
		setInstancePoker(Poker.class);
	}
	
	@Override
	public void propagate(InstanceState state) {
		int note = 0; 		//7 bit in
		int velocity = 0; 	//7 bit in
		int chan=0;
		int inst=0;
		int onoff=0;
		Value valnote = state.getPortValue(0);
		Value valvelocity = state.getPortValue(1);
		Value valonoff = state.getPortValue(2);
		Value valchan = state.getPortValue(3);
		Value valinst = state.getPortValue(4);
		note = valnote.toIntValue();
		velocity = valvelocity.toIntValue();
		if ((velocity<0)&&(note>=0)) { velocity=127; }
		onoff = valonoff.toIntValue();
		if (onoff<0) { onoff=1; }
		chan = valchan.toIntValue();
		if (chan<0) { chan=state.getAttributeValue(ATTR_CHAN); }
		inst = valinst.toIntValue();
		if (inst<0)
			{
			String instv = state.getAttributeValue(ATTR_INSTR).toString();
			int i;
			for (i=1;i<3;i++) { if (instv.charAt(i)==':') break; }
			inst=Integer.parseInt(instv.substring(0,i));
			}
		
		if (note==128) { play(0,0,onoff,chan,inst); }
		else if (note>128) { play(note-128,velocity,onoff,chan,inst); }
		else { play(note,velocity,onoff,chan,inst); }
	}
	
	public void paintInstance(InstancePainter painter) {
        painter.drawBounds();
        painter.drawPort(0); // draw a triangle on port 0
        painter.drawPort(1); // draw port 1 as just a dot
		painter.drawPort(2);
		painter.drawPort(3);
		painter.drawPort(4);
        
        // Display the current counter value centered within the rectangle.
        // However, if the context says not to show state (as when generating
        // printer output), then skip this.
        if(painter.getShowState()) {
            Bounds bds = painter.getBounds();
        }
    }

	Synthesizer midiSynth;
	Instrument[] instr;
	MidiChannel[] mChannels;

	public void init()
	{
      try{
		midiSynth = MidiSystem.getSynthesizer(); 
        midiSynth.open();
        //get and load default instrument and channel lists
        instr = midiSynth.getDefaultSoundbank().getInstruments();
		/*
		for (int i=0;i<instr.length;i++) { System.out.print(i+": "+instr[i].getName()+"\n"); }
		*/
		instlist=new AttributeOption[instr.length];
		for (int i=0;i<instr.length;i++)
			{
			String name=i+": "+instr[i].getName();
			instlist[i]=new AttributeOption(name,name,new SimpleStringGetter(name));
			}
		ATTR_INSTR=Attributes.forOption("instrument",
			new SimpleStringGetter("Instrument"),instlist);
        mChannels = midiSynth.getChannels();
		ATTR_CHAN=Attributes.forIntegerRange("channel", new SimpleStringGetter("Channel"),0,mChannels.length-1);
		mChannels[0].allNotesOff();
      } catch (MidiUnavailableException e) {}
	}

    public void play(int note,int velocity,int flag,int chan,int inst) { 
		//int note = 90;
		//int velocity = 100;
		int volume = 127;
		int durat=200;
		
		if (note<0) { note=0; }
		if (velocity<0) { velocity=0; }
		if (flag<0) { flag=1; }
		if (chan<0) { chan=0; }
		if (chan>=mChannels.length) { chan=mChannels.length-1; }
		if (inst<0) { inst=0; }
		if (inst>=instr.length) { inst=instr.length-1; }
		//System.out.print(note+" "+velocity+" "+flag+" "+chan+" "+inst+"\n");

		if (flag==1)
		{
			mChannels[chan].allNotesOff();
		}

        midiSynth.loadInstrument(instr[inst]);//load an instrument
		mChannels[chan].programChange(instr[inst].getPatch().getProgram());

        mChannels[chan].noteOn(note,velocity);//On channel 0, play note number 60 with velocity 100
		mChannels[chan].controlChange(7,volume);

   }

	public static class Poker extends InstancePoker {
		/* @Override
		public void mousePressed(InstanceState state, MouseEvent e) {
		}
		
		@Override
		public void mouseReleased(InstanceState state, MouseEvent e) {
		}
		
		private void setValue(InstanceState state, Value val) {
			InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
			if (data == null) {
				state.setData(new InstanceDataSingleton(val));
			} else {
				data.setValue(val);
			}
			state.getInstance().fireInvalidated();
		} */
	}

}    
