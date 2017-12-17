package cs555.chord.transport;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TCPSender {
	
	private Socket m_socket;
	private DataOutputStream dout;
	
	public TCPSender(Socket socket) throws IOException {
		this.m_socket = socket;
		m_socket.setSendBufferSize(1000000);
	}
	
	public void sendData(byte[] dataToSend) throws IOException {
		dout = new DataOutputStream(m_socket.getOutputStream());
		System.out.println("Data sent");
		int dataLength = dataToSend.length;
		synchronized (m_socket){
			dout.writeInt(dataLength);
			dout.write(dataToSend, 0, dataLength);
			dout.flush();
		}
	}
}
