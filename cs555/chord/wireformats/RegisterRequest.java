package cs555.chord.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;


public class RegisterRequest implements Event{

	private int type;
	private long timestamp;
	private int m_nodeID;
	private String IPAddress;
	private int portNumber;
	private int listenPortNumber;
	private String m_nickName;

	public void setMessageType(int messageType){ type = messageType; }
	public int getMessageType(){ return type; }
	public int getNodeId(){ return m_nodeID;}
	public String getIPAddress(){ return IPAddress; }
	public int getLocalPortNumber(){ return portNumber;}
	public int getListenPortNumber(){ return listenPortNumber;}
	public String getNodeNickName(){ return m_nickName; }
	public byte getStatusCode(){ return 0; }
	public String getInfo(){ return null; }
	public int getForwardFlag(){ return -1; }
	public int getFingerTableEntry(){ return -1; }
	
	public byte[] registerRequestMessage(int nodeID, String IP, int port, int listenPort, String nickName) throws IOException
	{
		type = Protocol.REGISTER_REQUEST;
		Date dte=new Date();
	    timestamp = dte.getTime();
	    m_nodeID = nodeID;
		IPAddress = IP;
		portNumber = port;
		listenPortNumber = listenPort;
		m_nickName = nickName;
		byte[] marshalledBytes = getBytes();
		return marshalledBytes;
	}
	
	@Override
	public void getType(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

		type = din.readInt();
		timestamp = din.readLong();
		m_nodeID = din.readInt();

		int identifierLength = din.readInt();
		byte[] identifierBytes = new byte[identifierLength];
		din.readFully(identifierBytes);
		IPAddress = new String(identifierBytes);

		portNumber = din.readInt();
		listenPortNumber = din.readInt();
		
		int identifierLengthNickName = din.readInt();
		byte[] identifierBytesNickName = new byte[identifierLengthNickName];
		din.readFully(identifierBytesNickName);
		m_nickName = new String(identifierBytesNickName);
		
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
		dout.writeInt(m_nodeID);

		byte[] identifierBytes = IPAddress.getBytes();
		int elementLength = identifierBytes.length;
		dout.writeInt(elementLength);
		dout.write(identifierBytes);

		dout.writeInt(portNumber);
		dout.writeInt(listenPortNumber);
		
		byte[] identifierBytesNickName = m_nickName.getBytes();
		int elementLengthNickName = identifierBytesNickName.length;
		dout.writeInt(elementLengthNickName);
		dout.write(identifierBytesNickName);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();
		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}

}
