package cs555.chord.node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cs555.chord.core.GeneratingIdentifiers;
import cs555.chord.transport.CommandThread;
import cs555.chord.transport.TCPReceiverThread;
import cs555.chord.transport.TCPSender;
import cs555.chord.transport.TCPServerThread;
import cs555.chord.wireformats.AskPredecessorRequest;
import cs555.chord.wireformats.AskPredecessorResponse;
import cs555.chord.wireformats.CheckFingerUpdate;
import cs555.chord.wireformats.CheckFingerUpdateResponse;
import cs555.chord.wireformats.Event;
import cs555.chord.wireformats.EventFactory;
import cs555.chord.wireformats.FileTransferRequest;
import cs555.chord.wireformats.FileTransferResponse;
import cs555.chord.wireformats.LookUpForwardRequest;
import cs555.chord.wireformats.LookUpRequest;
import cs555.chord.wireformats.LookUpResponse;
import cs555.chord.wireformats.Protocol;
import cs555.chord.wireformats.RandomNodeSuccessorResponse;
import cs555.chord.wireformats.RegisterRequest;
import cs555.chord.wireformats.UpdateFingerTableRequest;
import cs555.chord.wireformats.UpdateLookUpAfterEntryRequest;
import cs555.chord.wireformats.UpdateLookUpAfterEntryResponse;

public class Peer implements Node{
	
	private static String ownIP = "";
	private InetAddress discoveryNodeHost = null;
	private int discoveryNodePort = -1;
	private String nickName = "";
	private EventFactory eventFactory;
	private TCPServerThread server;
	private Thread serverThread;
	private Socket discoveryNodeClientSocket = null;
	private int localPort = -1;
	private TCPReceiverThread clientReceiver = null;
	private Thread clientreceiverThread;
	private static int ownServerPort = -1;
	private int nodeID = -1;
	private static HashMap<Integer, String> fingerTable = new HashMap<Integer, String>();
	private static String predecessor = "";
	private static String successor = "";
	private boolean lookUpInitiate = false;
	private CommandThread commandInput;
	private String randomNodeInfo = "";
	private String joiningNewNodeIP = "";
	private int joiningNewNodePort = -1;
	private static HashMap<Integer, String> fileList = new HashMap<Integer, String>();
	
	// Initializing the peer node
	public void Initialize(String[] args)
	{
		// Checking number of command line arguments which is one
		if(args.length != 4)
		{
			System.out.println("Enter Registry IP, Port Number, a nick name for the node and node Id");
			return;
		}
		
		System.out.println("Initializing the Messaging Node");
		
		// Getting the Registry IP and port number from the command line
		try {
			discoveryNodeHost = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} 
		
		discoveryNodePort = Integer.parseInt(args[1]);
		nickName = args[2];
		try {
			ownIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		// Initializing Event Factory Singleton Instance
		eventFactory = EventFactory.getInstance();
		eventFactory.setNodeInstance(this);
		
		
		// Initializing the server thread to listen to other connections
		server = new TCPServerThread(0,  Peer.class.getSimpleName(), eventFactory);
		serverThread = new Thread(server);
		serverThread.start();
		
		// Getting the node's identifier
		if(!args[3].equals("NA".toString()))
		{
			nodeID = Integer.parseInt(args[3]);
		}
		else
		{
			nodeID = GeneratingIdentifiers.generateNodeIdentifier();
		}
		
		System.out.println("Node ID: " + nodeID);
		
		// Opening connection to the Discovery node
		openDiscoveryNodeConnection(discoveryNodeHost, discoveryNodePort);
		
		// Sending Register Message to Registry
		sendRegisterMessage(nodeID, nickName);
		
		commandInput = new CommandThread(0);
		Thread commandThread = new Thread(commandInput);
		commandThread.start();
		
	}
	
	// Opening connection to the Discovery node
	private void openDiscoveryNodeConnection(InetAddress host, int port)
	{			
		try {
			discoveryNodeClientSocket = new Socket(host, port);
			localPort = discoveryNodeClientSocket.getLocalPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		// Opening receiver thread on the same client socket to receive data			
		try {
			clientReceiver = new TCPReceiverThread(discoveryNodeClientSocket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientreceiverThread = new Thread(clientReceiver);
		clientreceiverThread.start();
		clientReceiver.setEventFactoryInstance(eventFactory);			
	}
	
	// Sending Register Message to Discovery Node
	private void sendRegisterMessage(int nodeID, String nickName)
	{	
		try {
			TCPSender sender =  new TCPSender(discoveryNodeClientSocket);
			RegisterRequest regReq= new RegisterRequest();
			ownServerPort = server.getOwnPort();
			byte[] dataToSend = regReq.registerRequestMessage(nodeID, ownIP, localPort, ownServerPort, nickName);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// This method sends message to the random node the new node's successor
	private void sendFindSuccessorMessage(String info) throws NumberFormatException, UnknownHostException
	{
		Socket msgSocket = null;
		String[] token = info.split(":");
		
		// Filling the finger table with its own ID for the first time it comes in the system
		String fingerTableValue = nodeID + ":" + ownIP + ":" + ownServerPort;
		for(int i = 1; i <=5; i++ )
		{
			fingerTable.put(i, fingerTableValue);
		}
		
		if(!(token[0].equals(ownIP) && Integer.parseInt(token[2]) == ownServerPort))
		{	
			try {
				msgSocket = new Socket(InetAddress.getByName(token[0]), Integer.parseInt(token[2]));
			} catch (IOException e) {
				e.printStackTrace();
			};
			
			//sending lookup message
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				LookUpRequest lookUpReq= new LookUpRequest();
				byte[] dataToSend = lookUpReq.lookUpRequestMessage(nodeID, ownIP, ownServerPort);
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
		else
		{
			predecessor = nodeID + ":" + ownIP + ":" + ownServerPort;
			successor = nodeID + ":" + ownIP + ":" + ownServerPort;
			System.out.println("Welcome the first node in Chord Ring.");
		}
	}
	
	private String lookUp(int lookUpNode, String newNodeIP, int newNodeListenPort, String randomNode)
	{
		String successorNodeID = "";
		String forwardNodeInfo = "";
		boolean flag = false;
		int previousEntry = Integer.MAX_VALUE;
		int allSameEntry = Integer.MAX_VALUE;
		boolean prevFlag = false;
		boolean allSameFlag = false;
		// Own node information will be sent in forwarding look up request.
		// So that the final node can directly send message back to random node.

		String newNodeInfo = lookUpNode + ":" + newNodeIP + ":" + newNodeListenPort;
		
				
		Iterator it = fingerTable.entrySet().iterator();
		Map.Entry firstEntry = (Map.Entry)it.next();
		
		String[] firstEntryValueToken = firstEntry.getValue().toString().split(":");
		
		if((Integer.parseInt(firstEntryValueToken[0]) > lookUpNode) && (lookUpInitiate != true))
		{
			System.out.println("Hello");
			successorNodeID = firstEntry.getValue().toString();
			return successorNodeID;
			
			// look up hop end (Response) message to random node
		}
		else if((Integer.parseInt(firstEntryValueToken[0]) > lookUpNode) && (lookUpInitiate == true))
		{
			flag = true;
			lookUpInitiate = false;
		}
		
		for(Map.Entry<Integer, String> entry: fingerTable.entrySet())
		{
			String[] token = entry.getValue().split(":");
			
					
			if((Integer.parseInt(token[0]) <= nodeID) && (forwardNodeInfo == ""))
			{
				System.out.println("Hello1");
				if(!flag){
					if(!(previousEntry < Integer.parseInt(token[0])))
					{
						successorNodeID = entry.getValue();
					}
				}
				else{
					forwardNodeInfo = entry.getValue().toString();
					
				}
				//return successorNodeID;
				//break;
//				// If the same node with same successor ID initiated the look up as a random node
//				if(lookUpInitiate ==  true)
//				{
//					
//					
//				}
//				else
//				{
//					
//				}
				
			}
			else if((Integer.parseInt(token[0]) <= nodeID) && (forwardNodeInfo != ""))
			{
				if(flag){
					forwardNodeInfo = entry.getValue().toString();
				}
//				else if(Integer.parseInt(firstEntryValueToken[0]) > Integer.parseInt(token[0]))
//				{
//					if(!(Integer.parseInt(token[0]) == nodeID))
//					{
//						forwardNodeInfo = entry.getValue().toString();
//					}
//				}
			}
			else if(Integer.parseInt(token[0])  <= lookUpNode)
			{
				System.out.println("Hello2");
				forwardNodeInfo = entry.getValue().toString();
			}
			
			
			if((Integer.parseInt(token[0]) < nodeID) && (Integer.parseInt(token[0])  <= lookUpNode))
			{
				if(Integer.parseInt(token[0]) > previousEntry)
				{
					prevFlag = true;
				}
			}
			
			if(Integer.parseInt(token[0]) == allSameEntry)
			{
				allSameFlag = true;
			}
			else
			{
				allSameFlag = false;
			}
			
			previousEntry = Integer.parseInt(token[0]);
			allSameEntry = Integer.parseInt(token[0]);
			
		}
		
		System.out.println("forwardNodeInfo: " + forwardNodeInfo);
		
//		if(forwardNodeInfo.equals(predecessor))
//		{
//			successorNodeID = forwardNodeInfo;
//			return successorNodeID;
//		}
		
//		if(prevFlag)
//		{
//			if(!(Integer.parseInt(firstEntryValueToken[0]) < nodeID))
//			{
//				if(!(Integer.parseInt(firstEntryValueToken[0]) > Integer.parseInt(forwardNodeInfo.split(":")[0])))
//				{
//					forwardNodeInfo = successorNodeID;
//					successorNodeID = "";
//				}
//			}
//		}
		
		if(allSameFlag)
		{
			if((lookUpNode < nodeID) && (lookUpNode > Integer.parseInt(predecessor.split(":")[0])))
			{
				forwardNodeInfo = successorNodeID;
				successorNodeID = "";
			}
		}
		
		if(forwardNodeInfo != "")
		{
			System.out.println("Hello3");
			// look up hop message forward with random node's information so that the final node can directly send back
			sendLookUpForwardMessage(forwardNodeInfo, randomNode, newNodeInfo);
		}
		
		if(successorNodeID != "")
			return successorNodeID;
		else
			return "";
	}
	
	public void sendLookUpForwardMessage(String forwardNodeInfo, String randomNode, String newNodeInfo)
	{
		
		System.out.println("Hello4");
		
		Socket msgSocket = null;
		int newNodeID = Integer.parseInt(newNodeInfo.split(":")[0]);
		String newNodeIPAddress = newNodeInfo.split(":")[1];
		int newNodePortNumber = Integer.parseInt(newNodeInfo.split(":")[2]);
		
		// look up forward message to the new node with successor information
		try {
				msgSocket = new Socket(InetAddress.getByName(forwardNodeInfo.split(":")[1]), Integer.parseInt(forwardNodeInfo.split(":")[2]));
			} catch (IOException e) {
				e.printStackTrace();
			};
				
			//sending lookup forward message
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				LookUpForwardRequest lookUpForwdReq= new LookUpForwardRequest();
				byte[] dataToSend = lookUpForwdReq.lookUpForwardRequestMessage(newNodeID, newNodeIPAddress, newNodePortNumber, randomNode);
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
	
	public void sendLookUpResponse(String successorNodeID,  String sendNodeIP, int sendNodeListenPort, boolean direct)
	{
		
		Socket msgSocket = null;
		
		// look up response message to the new node with successor information
		try {
				msgSocket = new Socket(InetAddress.getByName(sendNodeIP), sendNodeListenPort);
			} catch (IOException e) {
				e.printStackTrace();
			};
				
			//sending lookup response message
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				LookUpResponse lookUpRes= new LookUpResponse();
				byte[] dataToSend;
				if(direct)
				{
					dataToSend = lookUpRes.lookUpResponseMessage(successorNodeID, 1);
				}
				else
				{
					dataToSend = lookUpRes.lookUpResponseMessage(successorNodeID, 0);
				}
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
	
	public void sendSuccessorInfoFromRandomNode(String succ, String IP, int port)
	{
		Socket msgSocket = null;
		
		// look up response message to the new node with successor information
		try {
				msgSocket = new Socket(InetAddress.getByName(IP), port);
			} catch (IOException e) {
				e.printStackTrace();
			};
				
			//sending lookup response message
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				RandomNodeSuccessorResponse randSuccRes= new RandomNodeSuccessorResponse();
				byte[] dataToSend = randSuccRes.randomNodeSuccessorReponseMessage(succ);
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
	
	public void updateFingerTableAfterLookUp(String info)
	{
		int fingerTableEntry = -1;
		int initialEntry = -1;
		Socket msgSocket = null;
		
		String[] token = info.split(":");
				
		// updating successor variable
		successor = info;
		
		// If successor node id is less than new node id
		if(Integer.parseInt(token[0]) < nodeID)
		{
			for(int i = 1; i <= 5; i++)
			{
				initialEntry = (int) ((int) (nodeID + (Math.pow(2, i-1))) % 32);
				if(!((initialEntry < 32) && (initialEntry > nodeID)))
				{
					// send successor a message to find the correct entry
					try {
						msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
					} catch (IOException e) {
						e.printStackTrace();
					};
					
					try {
						TCPSender sender =  new TCPSender(msgSocket);
						UpdateLookUpAfterEntryRequest upFinAftReq= new UpdateLookUpAfterEntryRequest();
						byte[] dataToSend = upFinAftReq.updateLookUpAfterMessage(nodeID, ownIP, ownServerPort, initialEntry);
						sender.sendData(dataToSend);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						msgSocket.close();
						msgSocket = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else
				{
					fingerTable.put(i, info);
				}
			}
		}
		else{
			// Updating its own finger table
			for(int i = 1; i <= 5; i++)
			{
				fingerTableEntry = (int) ((int) (nodeID + (Math.pow(2, i-1))) % 32);
			
				if(fingerTableEntry <= Integer.parseInt(token[0]))
				{
					fingerTable.put(i, info);
				}
				else if(fingerTableEntry > Integer.parseInt(token[0]))
				{
					// Send message to successor
					try {
						msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
					} catch (IOException e) {
						e.printStackTrace();
					};
					
					try {
						TCPSender sender =  new TCPSender(msgSocket);
						CheckFingerUpdate chkFingUpdt= new CheckFingerUpdate();
						byte[] dataToSend = chkFingUpdt.checkFingerUpdateMessage(nodeID, ownIP, ownServerPort, fingerTableEntry);
						sender.sendData(dataToSend);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					try {
						msgSocket.close();
						msgSocket = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// Sending successor a message to ask its predecessor information
		try {
			msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			AskPredecessorRequest askPredReq= new AskPredecessorRequest();
			byte[] dataToSend = askPredReq.askPredRequestMessage(nodeID, ownIP, ownServerPort);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			msgSocket.close();
			msgSocket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Sending successor a message to transfer its files
		try {
			msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			FileTransferRequest fileTransReq= new FileTransferRequest();
			byte[] dataToSend = fileTransReq.fileTransferRequestMessage(nodeID, ownIP, ownServerPort, Integer.parseInt(predecessor.split(":")[0]));
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			msgSocket.close();
			msgSocket = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void initiateFileTransfer(int newNodeID, String newNodeIP, int newNodeListenPort, int predNodeID)
	{
		Socket msgSocket = null;
		
		for(Map.Entry<Integer, String> entry: fileList.entrySet())
		{
			if((entry.getKey() <= newNodeID) && (entry.getKey() > predNodeID))
			{
				// Transfer the file to the new node
				try {
					msgSocket = new Socket(InetAddress.getByName(newNodeIP), newNodeListenPort);
				} catch (IOException e) {
					e.printStackTrace();
				};
				
				try {
					TCPSender sender =  new TCPSender(msgSocket);
					FileTransferResponse fileTransRes= new FileTransferResponse();
					byte[] dataToSend = fileTransRes.fileTransferResponseMessage("/tmp/skmishra_" + entry.getValue(), entry.getValue(), entry.getKey());
					sender.sendData(dataToSend);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					msgSocket.close();
					msgSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void handleUpdateLookUpAfterMessage(int lookUpNode, String newNodeIP, int newNodeListenPort, int entry)
	{
		
		Socket msgSocket = null;
		String updationInfo = "";
		
		if(entry <= Integer.parseInt(successor.split(":")[0]))
		{			
			if(entry <= nodeID)
			{
				updationInfo = nodeID + ":" + ownIP + ":" + ownServerPort;
			}
			else
			{
				updationInfo = successor;
			}
			
			// send response back
			try {
				msgSocket = new Socket(InetAddress.getByName(newNodeIP), newNodeListenPort);
			} catch (IOException e) {
				e.printStackTrace();
			};
			
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				UpdateLookUpAfterEntryResponse upFinAftRes= new UpdateLookUpAfterEntryResponse();
				byte[] dataToSend = upFinAftRes.updateLookUpAfterEntryResponseMessage(updationInfo, entry);
				sender.sendData(dataToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				msgSocket.close();
				msgSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			// send successor a message to find the correct entry
			try {
				msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
			} catch (IOException e) {
				e.printStackTrace();
			};
			
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				UpdateLookUpAfterEntryRequest upFinAftReq= new UpdateLookUpAfterEntryRequest();
				byte[] dataToSend = upFinAftReq.updateLookUpAfterMessage(lookUpNode, newNodeIP, newNodeListenPort, entry);
				sender.sendData(dataToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				msgSocket.close();
				msgSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void handleCheckFingerUpdateMessage(String info)
	{
		Socket msgSocket = null;
		String[] token = info.split(":");
		
		Iterator it = fingerTable.entrySet().iterator();
		Map.Entry firstEntry = (Map.Entry)it.next();
		
		String[] firstEntryValueToken = firstEntry.getValue().toString().split(":");
		
		if(Integer.parseInt(firstEntryValueToken[0]) > Integer.parseInt(token[3]))
		{
			// send response back
			try {
				msgSocket = new Socket(InetAddress.getByName(token[1]), Integer.parseInt(token[2]));
			} catch (IOException e) {
				e.printStackTrace();
			};
			
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				CheckFingerUpdateResponse chkFingUpdRes= new CheckFingerUpdateResponse();
				byte[] dataToSend = chkFingUpdRes.checkFingerUpdateResponseMessage(firstEntry.getValue().toString(), Integer.parseInt(token[3]));
				sender.sendData(dataToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				msgSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(Integer.parseInt(firstEntryValueToken[0]) < Integer.parseInt(token[3]))
		{
			if((Integer.parseInt(firstEntryValueToken[0]) > nodeID) && (Integer.parseInt(firstEntryValueToken[0]) < 32))
			{
				// forward to the successor to check			
				try {
					msgSocket = new Socket(InetAddress.getByName(successor.split(":")[1]), Integer.parseInt(successor.split(":")[2]));
				} catch (IOException e) {
					e.printStackTrace();
				};
				
				try {
					TCPSender sender =  new TCPSender(msgSocket);
					CheckFingerUpdate chkFingUpdt= new CheckFingerUpdate();
					byte[] dataToSend = chkFingUpdt.checkFingerUpdateMessage(nodeID, ownIP, ownServerPort, Integer.parseInt(token[3]));
					sender.sendData(dataToSend);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					msgSocket.close();
					msgSocket = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
			{
				// Send response back
				try {
					msgSocket = new Socket(InetAddress.getByName(token[1]), Integer.parseInt(token[2]));
				} catch (IOException e) {
					e.printStackTrace();
				};
				
				try {
					TCPSender sender =  new TCPSender(msgSocket);
					CheckFingerUpdateResponse chkFingUpdRes= new CheckFingerUpdateResponse();
					byte[] dataToSend = chkFingUpdRes.checkFingerUpdateResponseMessage(firstEntry.getValue().toString(), Integer.parseInt(token[3]));
					sender.sendData(dataToSend);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					msgSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void fillFingerTable(String fillNodeInfo, int entry)
	{
		int fingerEntry = -1;
		
		for(int i = 1; i <= 5; i++)
		{
			fingerEntry = (int) ((int) (nodeID + (Math.pow(2, i-1))) % 32);			
			if(fingerEntry == entry)
			{ 	
				fingerTable.put(i, fillNodeInfo);
			}
		}
	}
	
	public void sendPredecessorInfoToNewNode(int newNodeID, String newNodeIP, int newNodeListenPort)
	{
		Socket msgSocket = null;
		
		try {
			msgSocket = new Socket(InetAddress.getByName(newNodeIP), newNodeListenPort);
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			AskPredecessorResponse askPredRes= new AskPredecessorResponse();
			byte[] dataToSend = askPredRes.askPredecessorResponse(predecessor);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			msgSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Updating the new node as predecessor
		predecessor = newNodeID + ":" + newNodeIP + ":" + newNodeListenPort;
	}
	
	public void startUpdatingFingerTables(String info)
	{
		String[] token = info.split(":");
		
		System.out.println("Info At: " + info);
		
		predecessor = info;
		
		System.out.println("Predecessor At: " + predecessor);
		
		// Sending predecessor a message to update its successor and start updating finger tables
		// because a new node has entered the ring
		
		Socket msgSocket = null;
		
		try {
			msgSocket = new Socket(InetAddress.getByName(token[1]), Integer.parseInt(token[2]));
		} catch (IOException e) {
			e.printStackTrace();
		};
		
		try {
			TCPSender sender =  new TCPSender(msgSocket);
			UpdateFingerTableRequest upFinTabReq= new UpdateFingerTableRequest();
			byte[] dataToSend = upFinTabReq.updateFingerTableMessage(nodeID, ownIP, ownServerPort, 0);
			sender.sendData(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			msgSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateFingerTable(int newNodeID, String newNodeIP, int newNodeListenPort, int forwardFlag)
	{
		int fingerTableEntry = -1;
		int withoutModEntry = -1;
		String fingerTableInfo = newNodeID + ":" + newNodeIP + ":" + newNodeListenPort;
		int previousEntry = Integer.MAX_VALUE;
		boolean prevFlag = false;
		int prevCount = 1;
		
		if(forwardFlag == 0)
		{
			successor = fingerTableInfo;
		}
		
		for(Map.Entry<Integer, String> entry: fingerTable.entrySet())
		{
			String[] token = entry.getValue().split(":");
			
			if(Integer.parseInt(token[0]) == previousEntry)
			{
				prevFlag = true;
				prevCount++;
			}
			else 
			{
				prevFlag = false;
			}
			
			previousEntry = Integer.parseInt(token[0]);
		}
		
		
		// Updating its own finger table
		for(int i = 1; i <= 5; i++)
		{
			withoutModEntry = (int) (int) (nodeID + (Math.pow(2, i-1)));
			fingerTableEntry = (int) ((int) (nodeID + (Math.pow(2, i-1))) % 32);
					
			if(fingerTableEntry <= newNodeID)
			{
				if(Integer.parseInt(getElementByIndex(fingerTable, i-1).split(":")[0]) != Integer.parseInt(successor.split(":")[0]))
				{
					if(!(Integer.parseInt(getElementByIndex(fingerTable, i-1).split(":")[0]) < newNodeID))
					{
						fingerTable.put(i, fingerTableInfo);
					}
					
					if(Integer.parseInt(getElementByIndex(fingerTable, i-1).split(":")[0]) <= nodeID)
					{
						if(withoutModEntry < 32)
						{
							fingerTable.put(i, fingerTableInfo);
						}
					}
					else if(prevCount == 4)
					{
						fingerTable.put(i, fingerTableInfo);
					}
				}
			}
		}
		
		// Forward the message to predecessor to update its finger table
		
		if(!(Integer.parseInt(predecessor.split(":")[0]) == newNodeID))
		{
			Socket msgSocket = null;
		
			try {
				msgSocket = new Socket(InetAddress.getByName(predecessor.split(":")[1]), Integer.parseInt(predecessor.split(":")[2]));
			} catch (IOException e) {
				e.printStackTrace();
			};
		
			try {
				TCPSender sender =  new TCPSender(msgSocket);
				UpdateFingerTableRequest upFinTabForReq= new UpdateFingerTableRequest();
				byte[] dataToSend = upFinTabForReq.updateFingerTableMessage(newNodeID, newNodeIP, newNodeListenPort, 1);
				sender.sendData(dataToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			try {
				msgSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getElementByIndex(HashMap map,int index){
	    return (String) map.get( (map.keySet().toArray())[index] );
	}
	
	public void printFingerTable()
	{
		Set set = fingerTable.entrySet();
		Iterator iterator = set.iterator();
		while(iterator.hasNext()) {
			Map.Entry mentry = (Map.Entry)iterator.next();
			System.out.println("key is: "+ mentry.getKey() + " & Value is: " + mentry.getValue());
		}
	}
	
	public void printSuccessorNode()
	{
		System.out.println("Successor Node: " + successor);
	}
	
	public void printPredecessorNode()
	{
		System.out.println("Predecessor Node: " + predecessor);
	}
	
	public void printFileList()
	{
		Set set = fileList.entrySet();
		Iterator iterator = set.iterator();
		while(iterator.hasNext()) {
			Map.Entry mentry = (Map.Entry)iterator.next();
			System.out.println("File ID: "+ mentry.getKey() + " & File Name: " + mentry.getValue());
		}
	}
	
	@Override
	public void onEvent(Event e) 
	{		
		int messageType = e.getMessageType();
		System.out.println("OnEvent messageType " + messageType);
		
		if(messageType == Protocol.REGISTER_RESPONSE)
		{
			System.out.println(e.getStatusCode() + ":" + e.getInfo());
			if(e.getStatusCode() == Protocol.SUCCESS)
			{
				try {
					sendFindSuccessorMessage(e.getInfo());
				} catch (NumberFormatException e1) {
					e1.printStackTrace();
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
			}
			else
			{
				System.exit(0);
			}
		}
		else if(messageType == Protocol.LOOKUP_REQUEST)
		{
			int lookUpNodeID = e.getNodeId();
			lookUpInitiate = true;
			randomNodeInfo = nodeID + ":" + ownIP + ":" + ownServerPort;
			joiningNewNodeIP = e.getIPAddress();
			joiningNewNodePort = e.getListenPortNumber();
			String successorNodeID = lookUp(lookUpNodeID, e.getIPAddress(), e.getListenPortNumber(), randomNodeInfo);
			if(successorNodeID != "")
			{
				sendLookUpResponse(successorNodeID, e.getIPAddress(), e.getListenPortNumber(), true);
			}
		}
		else if(messageType == Protocol.LOOKUP_RESPONSE)
		{
			if(e.getForwardFlag() == 1)
			{
				updateFingerTableAfterLookUp(e.getInfo());
			}
			else if(e.getForwardFlag() == 0)
			{
				sendSuccessorInfoFromRandomNode(e.getInfo(), joiningNewNodeIP, joiningNewNodePort);
			}
		}
		else if(messageType == Protocol.ASK_PREDECESSOR_REQUEST)
		{
			sendPredecessorInfoToNewNode(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber());
		}
		else if(messageType == Protocol.ASK_PREDECESSOR_RESPONSE)
		{
			startUpdatingFingerTables(e.getInfo());
		}
		else if(messageType == Protocol.UPDATE_FINGER_TABLE_REQUEST)
		{
			if(!(nodeID  == e.getNodeId()))
			{
				updateFingerTable(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber(), e.getForwardFlag());
			}
			else
			{
				System.out.println("Dropping packet because the same node has been reached.");
			}
		}
		else if(messageType == Protocol.LOOKUP_FORWARD_REQUEST)
		{
			String successorNodeID = lookUp(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber(), e.getInfo());
			if(successorNodeID != "")
			{
				// sending successor info to random node
				sendLookUpResponse(successorNodeID, e.getInfo().toString().split(":")[1], Integer.parseInt(e.getInfo().toString().split(":")[2]), false);
			}
		}
		else if(messageType == Protocol.RANDOMNODE_SUCCESSOR_RESPONSE)
		{
			updateFingerTableAfterLookUp(e.getInfo());
		}
		else if(messageType == Protocol.CHECK_FINGER_UPDATE)
		{
			String info = e.getNodeId() + ":" + e.getIPAddress() + ":" + e.getListenPortNumber() + ":" + e.getFingerTableEntry();
			handleCheckFingerUpdateMessage(info);
		}
		else if(messageType == Protocol.CHECK_FINGER_UPDATE_RESPONSE)
		{
			fillFingerTable(e.getInfo(), e.getFingerTableEntry());
		}
		else if(messageType == Protocol.UPDATE_LOOKUP_AFTER_ENTRY)
		{
			handleUpdateLookUpAfterMessage(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber(), e.getFingerTableEntry());
		}
		else if(messageType == Protocol.UPDATE_LOOKUP_AFTER_ENTRY)
		{
			handleUpdateLookUpAfterMessage(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber(), e.getFingerTableEntry());
		}
		else if(messageType == Protocol.UPDATE_LOOKUP_AFTER_ENTRY_RESPONSE)
		{
			fillFingerTable(e.getInfo(), e.getFingerTableEntry());
		}
		else if(messageType == Protocol.FILE_STORE_REQUEST)
		{
			System.out.println("File Stored.");
			fileList.put(Integer.parseInt(e.getInfo().toString().split(":")[0]), e.getInfo().toString().split(":")[1]);
		}
		else if(messageType == Protocol.FILE_TRANSFER_REQUEST)
		{
			initiateFileTransfer(e.getNodeId(), e.getIPAddress(), e.getListenPortNumber(), Integer.parseInt(e.getInfo()));
		}
		else if(messageType == Protocol.FILE_TRANSFER_RESPONSE)
		{
			System.out.println("Transfered File Stored.");
			fileList.put(Integer.parseInt(e.getInfo().toString().split(":")[0]), e.getInfo().toString().split(":")[1]);
		}
	}
	
	public static void main(String[] args) {
		
		// Starting the peer nodes
		Peer peer = new Peer();
		peer.Initialize(args);

	}

}
