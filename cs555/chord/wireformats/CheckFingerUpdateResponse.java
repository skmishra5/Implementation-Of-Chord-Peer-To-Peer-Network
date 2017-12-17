package cs555.chord.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

public class CheckFingerUpdateResponse implements Event{
	private int type;
	private long timestamp;
	private String m_nodeInfo;
	private int m_entry;

	public void setMessageType(int messageType){ type = messageType; }
	public int getMessageType(){ return type; }
	public int getNodeId(){ return -1;}
	public String getIPAddress(){ return null; }
	public int getLocalPortNumber(){ return -1;}
	public int getListenPortNumber(){ return -1;}
	public String getNodeNickName(){ return null; }
	public byte getStatusCode(){ return 0; }
	public String getInfo(){ return m_nodeInfo; }
	public int getForwardFlag(){ return -1; }
	public int getFingerTableEntry(){ return m_entry; }
	
	public byte[] checkFingerUpdateResponseMessage(String nodeInfo, int entry) throws IOException
	{
		type = Protocol.CHECK_FINGER_UPDATE_RESPONSE;
		Date dte=new Date();
	    timestamp = dte.getTime();
	    m_nodeInfo = nodeInfo;
	    m_entry = entry;
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
		m_nodeInfo = new String(identifierBytes);
		
		m_entry = din.readInt();
		
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
		
		byte[] identifierBytes = m_nodeInfo.getBytes();
		int elementLength = identifierBytes.length;
		dout.writeInt(elementLength);
		dout.write(identifierBytes);
		
		dout.writeInt(m_entry);
		
		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();
		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}
}
