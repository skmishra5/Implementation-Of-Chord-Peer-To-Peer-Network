package cs555.chord.node;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

import cs555.chord.transport.TCPSender;
import cs555.chord.transport.TCPServerThread;
import cs555.chord.wireformats.Event;
import cs555.chord.wireformats.EventFactory;
import cs555.chord.wireformats.Protocol;
import cs555.chord.wireformats.RegisterResponse;
import cs555.chord.wireformats.StoreDataRandomNodeRequest;
import cs555.chord.wireformats.StoreDataRandomNodeResponse;


public class DiscoveryNode implements Node{

	private int portNumber = -1;
	private EventFactory eventFactory;
	private static TCPServerThread server = null;
	private HashMap<Integer, String> peerNodeInfo = new HashMap<Integer, String>();
	Random randomNode = new Random();
	
	// Initializing the Discover Node
	public void Initialize(String[] args)
	{	
		if(args.length != 1)
		{
			System.out.println("Enter Port Number");
			return;
		}
		
		portNumber = Integer.parseInt(args[0]);
		
		// Initializing Event Factory Singleton Instance
		eventFactory = EventFactory.getInstance();
		eventFactory.setNodeInstance(this);
		
		// Initializing the server thread
		server = new TCPServerThread(portNumber,  DiscoveryNode.class.getSimpleName(), eventFactory);
		Thread serverThread = new Thread(server);
		serverThread.start();
	}
	
	@Override
	public void onEvent(Event e) 
	{
		int messageType = e.getMessageType();
		
		if(messageType == Protocol.REGISTER_REQUEST)
		{
			System.out.println("Registration Successful for IP " + e.getIPAddress() + " and port number " + e.getLocalPortNumber() + ":" + e.getListenPortNumber());
			String IPPortInfo = e.getIPAddress() + ":" + e.getLocalPortNumber();
			String storeIPPortInfo = e.getIPAddress() + ":" + e.getLocalPortNumber() + ":" + e.getListenPortNumber() + ":" + e.getNodeNickName();
			int storeNodeID = e.getNodeId();
			System.out.println("Node ID: " + storeNodeID);
			Socket tempClientSocket = server.getClientSocket(IPPortInfo);
			
			if((!peerNodeInfo.containsKey(storeNodeID)) && (tempClientSocket != null))
			{
				TCPSender sender = null;
				try {
					sender = new TCPSender(tempClientSocket);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				RegisterResponse regRes= new RegisterResponse();
				String randomNodeInfo = "";
				if(peerNodeInfo.size() > 0)
				{
					Object[] values = peerNodeInfo.values().toArray();
					randomNodeInfo = (String) values[randomNode.nextInt(values.length)];
				}
				else
				{
					randomNodeInfo = storeIPPortInfo;
				}

				byte[] dataToSend = null;
				try {
					dataToSend = regRes.registerResponseMessage(Protocol.SUCCESS, randomNodeInfo);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					sender.sendData(dataToSend);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				peerNodeInfo.put(storeNodeID, storeIPPortInfo);
				
			}
			else if(peerNodeInfo.containsKey(storeNodeID))
			{
				TCPSender sender = null;
				try {
					sender = new TCPSender(tempClientSocket);
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				RegisterResponse regRes= new RegisterResponse();
				String tempInfo = "Collision Detected! Send a new ID.";
				byte[] dataToSend = null;
				try {
					dataToSend = regRes.registerResponseMessage(Protocol.FAILURE, tempInfo);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					sender.sendData(dataToSend);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			else
			{
				System.out.println("Mismatch in the IP sent");
			}
		}
		else if(messageType == Protocol.STORE_DATA_RANDOM_NODE_REQUEST)
		{
			if(peerNodeInfo != null)
			{
				Socket msgSocket = null;
				try {
					msgSocket = new Socket(InetAddress.getByName(e.getIPAddress()), e.getListenPortNumber());
				} catch (IOException e1) {
					e1.printStackTrace();
				};
			
				//sending random node request message
				try {
					TCPSender sender =  new TCPSender(msgSocket);
					
					String randomNodeInfo = "";
					if(peerNodeInfo.size() > 0)
					{
						Object[] values = peerNodeInfo.values().toArray();
						randomNodeInfo = (String) values[randomNode.nextInt(values.length)];
					}
					
					StoreDataRandomNodeResponse res= new StoreDataRandomNodeResponse();
					byte[] dataToSend = res.storeDataRandomNodeResponseMessage(randomNodeInfo);
					sender.sendData(dataToSend);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			
				try {
					msgSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
	}
	
	public static void main(String[] args) {
		
		DiscoveryNode discoveryNode =  new DiscoveryNode();
		// Initialize the Discovery node
		discoveryNode.Initialize(args);

	}


}
