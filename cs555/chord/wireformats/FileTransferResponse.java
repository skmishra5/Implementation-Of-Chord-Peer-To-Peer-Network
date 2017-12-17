package cs555.chord.wireformats;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class FileTransferResponse implements Event{
	private int type;
	private long timestamp;
	private String m_filePath;
	private String m_fileName;
	private int m_fileID;

	public void setMessageType(int messageType){ type = messageType; }
	public int getMessageType(){ return type; }
	public int getNodeId(){ return -1;}
	public String getIPAddress(){ return null; }
	public int getLocalPortNumber(){ return -1;}
	public int getListenPortNumber(){ return -1;}
	public String getNodeNickName(){ return null; }
	public byte getStatusCode(){ return 0; }
	public String getInfo(){ return m_fileID + ":" + m_fileName; }
	public int getForwardFlag(){ return -1; }
	public int getFingerTableEntry(){ return -1; }
	
	public byte[] fileTransferResponseMessage(String filePath, String fileName, int fileID) throws IOException
	{
		type = Protocol.FILE_TRANSFER_RESPONSE;
		Date dte=new Date();
	    timestamp = dte.getTime();
	    m_filePath = filePath;
	    m_fileName = fileName;
	    m_fileID = fileID;
		byte[] marshalledBytes = getBytes();
		return marshalledBytes;
	}
	
	@Override
	public void getType(byte[] marshalledBytes) throws IOException {
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(marshalledBytes);
		DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
		int bytesRead;
	    int current = 0;
	    FileOutputStream fos = null;
	    BufferedOutputStream bos = null;

		type = din.readInt();
		timestamp = din.readLong();
		
		m_fileID = din.readInt();
		
		int identifierLength1 = din.readInt();
		byte[] identifierBytes1 = new byte[identifierLength1];
		din.readFully(identifierBytes1);
		m_fileName = new String(identifierBytes1);
		
		int fileSize = din.readInt();

		byte [] mybytearray  = new byte [fileSize];
	    fos = new FileOutputStream("/tmp/" + "skmishra_" + m_fileName);
	    bos = new BufferedOutputStream(fos);
	    din.readFully(mybytearray);

	    bos.write(mybytearray, 0 , fileSize);
	    bos.flush();
		
		baInputStream.close();
		din.close();		
	}
	
	@Override
	public byte[] getBytes() throws IOException {		
		byte[] marshalledBytes = null;
		ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
		File myFile = new File(m_filePath);
		byte [] mybytearray  = new byte [(int)myFile.length()];

		dout.writeInt(type);
		dout.writeLong(timestamp);
		
		dout.writeInt(m_fileID);
		
		byte[] identifierBytes1 = m_fileName.getBytes();
		int elementLength1 = identifierBytes1.length;
		dout.writeInt(elementLength1);
		dout.write(identifierBytes1);
				
		FileInputStream fis = new FileInputStream(myFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		bis.read(mybytearray, 0, mybytearray.length);
		dout.writeInt(mybytearray.length);
		dout.write(mybytearray);

		dout.flush();
		marshalledBytes = baOutputStream.toByteArray();
		baOutputStream.close();
		dout.close();
		return marshalledBytes;
	}
}
