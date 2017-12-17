package cs555.chord.wireformats;

import java.io.IOException;

public interface Event {

	// Methods to Marshall and Unmarshall messages
	public void getType(byte[] marshalledBytes)
		throws IOException;
	public byte[] getBytes()
		throws IOException;
	
	//Method to set and get Message Type
	public void setMessageType(int messageType);
	public int getMessageType();
	public int getNodeId();
	public String getIPAddress();
	public int getLocalPortNumber();
	public int getListenPortNumber();
	public byte getStatusCode();
	public String getNodeNickName();
	public String getInfo();
	public int getForwardFlag();
	public int getFingerTableEntry();
	
}
