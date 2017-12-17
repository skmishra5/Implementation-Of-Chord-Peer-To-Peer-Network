package cs555.chord.wireformats;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import cs555.chord.node.Node;



public class EventFactory {
	private static EventFactory instance = null;
	private Node node = null; 
	
	private EventFactory(){};
	
	public static EventFactory getInstance(){
		// Creating Singleton instance
		if(instance == null){
			instance = new EventFactory();
		}
		return instance;
	}
	
	public void setNodeInstance(Node n)
	{
		node = n;
	}
	
	public void processReceivedMessage(byte[] data) throws IOException
	{
		int messageType = getMessageType(data);
		System.out.println("Message Type: " + messageType);
		
		if(messageType == Protocol.REGISTER_REQUEST)
		{
			Event e = new RegisterRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.REGISTER_RESPONSE)
		{
			Event e = new RegisterResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.LOOKUP_REQUEST)
		{
			Event e = new LookUpRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.LOOKUP_RESPONSE)
		{
			Event e = new LookUpResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.ASK_PREDECESSOR_REQUEST)
		{
			Event e = new AskPredecessorRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.ASK_PREDECESSOR_RESPONSE)
		{
			Event e = new AskPredecessorResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.UPDATE_FINGER_TABLE_REQUEST)
		{
			Event e = new UpdateFingerTableRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.LOOKUP_FORWARD_REQUEST)
		{
			Event e = new LookUpForwardRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.RANDOMNODE_SUCCESSOR_RESPONSE)
		{
			Event e = new RandomNodeSuccessorResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.CHECK_FINGER_UPDATE)
		{
			Event e = new CheckFingerUpdate();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.CHECK_FINGER_UPDATE_RESPONSE)
		{
			Event e = new CheckFingerUpdateResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.UPDATE_LOOKUP_AFTER_ENTRY)
		{
			Event e = new UpdateLookUpAfterEntryRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.UPDATE_LOOKUP_AFTER_ENTRY_RESPONSE)
		{
			Event e = new UpdateLookUpAfterEntryResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.STORE_DATA_RANDOM_NODE_REQUEST)
		{
			Event e = new StoreDataRandomNodeRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.STORE_DATA_RANDOM_NODE_RESPONSE)
		{
			Event e = new StoreDataRandomNodeResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.FILE_STORE_REQUEST)
		{
			Event e = new FileStoreRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.FILE_TRANSFER_REQUEST)
		{
			Event e = new FileTransferRequest();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
		else if(messageType == Protocol.FILE_TRANSFER_RESPONSE)
		{
			Event e = new FileTransferResponse();
			e.getType(data);
			e.setMessageType(messageType);
			node.onEvent(e);
		}
	}
	
	private int getMessageType(byte[] data) throws IOException
	{
		int type;
		ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
		DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

		type = din.readInt();
		baInputStream.close();
		din.close();
		return type;
	}
}
