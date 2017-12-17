package cs555.chord.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import cs555.chord.transport.CommandThread;
import cs555.chord.transport.TCPSender;
import cs555.chord.transport.TCPServerThread;
import cs555.chord.wireformats.Event;
import cs555.chord.wireformats.EventFactory;
import cs555.chord.wireformats.FileStoreRequest;
import cs555.chord.wireformats.LookUpRequest;
import cs555.chord.wireformats.Protocol;
import cs555.chord.wireformats.StoreDataRandomNodeRequest;

public class StoreData implements Node{

	private static String ownIP = "";
	private static int ownListenport = -1;
	private EventFactory eventFactory;
	private static TCPServerThread server = null;
	private CommandThread commandInput;
	private static InetAddress discoveryNodeHost = null;
	private static int discoveryNodePort = -1;
	private static String m_filePath = "";
	private static String m_fileName = "";
	private static String m_fileID = "";
	
	// Initializing the Store Data Node
	public void Initialize(String[] args)
	{
		if(args.length != 3)
		{
			System.out.println("Enter Port Number, Discovery Node IP and Port");
			return;
		}
		
		//ownListenport = Integer.parseInt(args[0]);
		
		// Getting the Registry IP and port number from the command line
		try {
			discoveryNodeHost = InetAddress.getByName(args[1]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} 
		
		discoveryNodePort = Integer.parseInt(args[2]);

		try {
			ownIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		// Initializing Event Factory Singleton Instance
		eventFactory = EventFactory.getInstance();
		eventFactory.setNodeInstance(this);
		
		// Initializing the server thread
		server = new TCPServerThread(0,  StoreData.class.getSimpleName(), eventFactory);
		Thread serverThread = new Thread(server);
		serverThread.start();
		
		commandInput = new CommandThread(1);
		Thread commandThread = new Thread(commandInput);
		commandThread.start();
	}
	
	public void storeResource(String filePath, String fileID)
	{		
		m_filePath = filePath;
		m_fileName = m_filePath.split("/")[1];
		m_fileID = fileID;
		
		ownListenport = server.getOwnPort();
		// Sending a message to Discovery Node asking for random node
		sendMessageToDiscoveryNode();
	}
	
	public void sendMessageToDiscoveryNode()
	{
		
		Socket msgSocket = null;
		try {
			msgSocket = new Socket(discoveryNodeHost, discoveryNodePort);
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		//sending random node request message
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			StoreDataRandomNodeRequest req= new StoreDataRandomNodeRequest();
			byte[] dataToSend = req.storeDataRandomNodeRequestMessage(ownIP, ownListenport);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			msgSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Sending the random node a message to start the look up
	private void sendFindSuccessorMessage(String info) throws NumberFormatException, UnknownHostException
	{
		Socket msgSocket = null;
		String[] token = info.split(":");
			
		try {
			msgSocket = new Socket(InetAddress.getByName(token[0]), Integer.parseInt(token[2]));
		} catch (IOException e) {
			e.printStackTrace();
		};
			
		//sending lookup message
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			LookUpRequest lookUpReq= new LookUpRequest();
			byte[] dataToSend = lookUpReq.lookUpRequestMessage(Integer.parseInt(m_fileID), ownIP, ownListenport);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		try {
			msgSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendFileToSuccessorNode(String info)
	{
		Socket msgSocket = null;
		String[] token = info.split(":");
			
		try {
			msgSocket = new Socket(InetAddress.getByName(token[1]), Integer.parseInt(token[2]));
		} catch (IOException e) {
			e.printStackTrace();
		};
			
		//sending lookup message
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			FileStoreRequest fileStoreReq= new FileStoreRequest();
			byte[] dataToSend = fileStoreReq.fileStoreRequest(m_filePath, m_fileName, Integer.parseInt(m_fileID), ownIP, ownListenport);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		try {
			msgSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onEvent(Event e)
	{
		int messageType = e.getMessageType();
		System.out.println("OnEvent messageType " + messageType);
		
		if(messageType == Protocol.STORE_DATA_RANDOM_NODE_RESPONSE)
		{
			// Send the random node a message to start the look up operation
			try {
				sendFindSuccessorMessage(e.getInfo());
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if((messageType == Protocol.LOOKUP_RESPONSE) || (messageType == Protocol.RANDOMNODE_SUCCESSOR_RESPONSE))
		{
			System.out.println("The successor is: " + e.getInfo());
			sendFileToSuccessorNode(e.getInfo());
		}
	}
	
	public static void main(String[] args) {
		
		StoreData storeData =  new StoreData();
		// Initialize the Store Data Node
		storeData.Initialize(args);
	}

}
