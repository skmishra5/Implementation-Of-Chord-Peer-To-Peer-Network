package cs555.chord.node;

import cs555.chord.wireformats.Event;

public interface Node {
	
	public void onEvent(Event e);
	
}
