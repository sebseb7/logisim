Logisim Turing-Tape Component Library
-------------------------------------

This provides an output component that accepts midi data and plays the
corresponding sounds.

Original code by kahdeg.
Modified by Mark Craig.
Modified by K. Walsh, July 2020.

Below is an excerpt from Mark Craig's README:

    I modified kahdeg's midi device by making it faster and more responsive, and
    adding the capability to change the instrument and the MIDI channel.  The
    10th MIDI channel (#9) is a percussion instrument.

    Every input line other than note has defaults and therefore doesn't need to be
    connected (you can override defaults by using the component's parameters at
    the lower left hand side of the Logisim window).

    A note value of 128 plays silence.  I added this feature since note 0 of a
    piano (and some other instruments) does actually have a hearable note (or
    sound).  This is why the note line is now 8 bits.

