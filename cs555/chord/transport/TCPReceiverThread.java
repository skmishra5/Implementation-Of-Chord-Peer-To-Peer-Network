package cs555.chord.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import cs555.chord.node.Node;
import cs555.chord.wireformats.EventFactory;



public class TCPReceiverThread implements Runnable{
	private Socket m_socket;
	private DataInputStream din;
	private Node node;
	private EventFactory eventFactory;
	private volatile boolean done = false;
	
	public void setDone()
	{
		done = true;
	}
	
	public TCPReceiverThread(Socket socket) throws IOException {
		this.m_socket = socket;
		m_socket.setReceiveBufferSize(63999);
	}
	
	public void setEventFactoryInstance(EventFactory ef)
	{
		eventFactory = ef;
	}
	
	@Override
	public void run() {
		int dataLength;
		while (m_socket != null) {
			try {
				din = new DataInputStream(m_socket.getInputStream());
				dataLength = din.readInt();
				if(dataLength == -1){
					break;
				}
				byte[] data = new byte[dataLength];
				din.readFully(data, 0, dataLength);
				// Process received event
				eventFactory.processReceivedMessage(data); 
			}
			catch (SocketException se) {
				break;
			}
			catch (IOException ioe) {
				break;
			}
		}
	}
}
