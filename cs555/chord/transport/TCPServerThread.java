package cs555.chord.transport;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import cs555.chord.wireformats.EventFactory;



public class TCPServerThread implements Runnable{
	private int m_portNumber = -1;
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private String m_node = "";
	private EventFactory m_ef;
	private int ownServerPort = -1;
	private HashMap<String, Socket> clientSocketInfo = new HashMap<String, Socket>();
	private volatile boolean done = false;
	private TCPReceiverThread receiver;
	private Thread receiverThread;
	
	public void setDone()
	{
		done = true;
	}
	
	
	public TCPServerThread(int portNumber, String node, EventFactory ef)
	{
		this.m_portNumber = portNumber;
		this.m_node = node;
		this.m_ef = ef;
	}
	
	public int getOwnPort()
	{
		ownServerPort = serverSocket.getLocalPort();
		return ownServerPort;
	}
	
	public ServerSocket getServerSocket()
	{
		return serverSocket;
	}
	
	public Socket getClientSocket(String IPPortInfo)
	{
		Socket tempClientSocket = null;
		
		if(clientSocketInfo.containsKey(IPPortInfo))
		{
			tempClientSocket = clientSocketInfo.get(IPPortInfo);
		}
		return tempClientSocket;
	}
	
	public void removeClientSocketInfo(String IPPortInfo)
	{
		if(clientSocketInfo.containsKey(IPPortInfo))
		{
			clientSocketInfo.remove(IPPortInfo);
		}
	}

	@Override
	public void run() {
		try{
			serverSocket = new ServerSocket(m_portNumber); // binding socket to port
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("Server error");
		}
		
		while(!done){
			
			System.out.println("Server Started on Port Number " + getOwnPort());
			
			while(!serverSocket.isClosed())  //accepting the client infinitely
			{
				try {
					clientSocket = serverSocket.accept();
					synchronized (this){
						clientSocketInfo.put(clientSocket.getInetAddress().toString().split("/")[1] + ":" + clientSocket.getPort(), clientSocket);
					}
					receiver = new TCPReceiverThread(clientSocket);
					receiverThread = new Thread(receiver);
					receiverThread.start();
					receiver.setEventFactoryInstance(m_ef);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("-Server Socket closed-");
					break;
				}

			}
		}
		
	}
}
