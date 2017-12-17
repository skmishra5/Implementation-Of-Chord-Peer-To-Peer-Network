package cs555.chord.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

public class StoreDataRandomNodeRequest implements Event{
	private int type;
	private long timestamp;
	private String IPAddress;
	private int listenPortNumber;

	public void setMessageType(int messageType){ type = messageType; }
	public int getMessageType(){ return type; }
	public int getNodeId(){ return -1;}
	public String getIPAddress(){ return IPAddress; }
	public int getLocalPortNumber(){ return -1;}
	public int getListenPortNumber(){ return listenPortNumber;}
	public String getNodeNickName(){ return null; }
	public byte getStatusCode(){ return 0; }
	public String getInfo(){ return null; }
	public int getForwardFlag(){ return -1; }
	public int getFingerTableEntry(){ return -1; }
	
	public byte[] storeDataRandomNodeRequestMessage(String IP, int listenPort) throws IOException
	{
		type = Protocol.STORE_DATA_RANDOM_NODE_REQUEST;
		Date dte=new Date();
	    timestamp = dte.getTime();
		IPAddress = IP;
		listenPortNumber = listenPort;
		byte[] marshalledBytes = getBytes();
		return marshalledBytes;
	}
	
	@Override
	public void getType(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

		type = din.readInt();
		timestamp = din.readLong();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		IPAddress = new String(identifierBytes);

		listenPortNumber = din.readInt();
		
		baInputStream.close();
		din.close();		
	}
	
	@Override
	public byte[] getBytes() throws IOException {		
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

		dout.writeInt(type);
		dout.writeLong(timestamp);

		byte[] identifierBytes = IPAddress.getBytes();
		int elementLength = identifierBytes.length;
		dout.writeInt(elementLength);
		dout.write(identifierBytes);

		dout.writeInt(listenPortNumber);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();
		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}
}
