package cs555.chord.transport;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import cs555.chord.node.Peer;
import cs555.chord.node.StoreData;

public class CommandThread implements Runnable{
	private Scanner scanner = new Scanner(System.in);
	private volatile boolean done = false;
	private Peer peer = new Peer();
	private StoreData storeData = new StoreData();
	private int m_whichThread = -1;
	
	public void setDone()
	{
		done = true;
	}
	
	public CommandThread(int whichThread)
	{
		m_whichThread = whichThread;
	}
	
	@Override
	public void run() {
		while(!done){
			if(m_whichThread == 0)
			{
				String command = scanner.nextLine();
				System.out.println("Command " + command);
				String[] token = command.split(" ");
				
				if(command.equals("ft"))
				{
					peer.printFingerTable();
				}
				else if(command.equals("sn"))
				{
					peer.printSuccessorNode();
				}
				else if(command.equals("pn"))
				{
					peer.printPredecessorNode();
				}
				else if(command.equals("fl"))
				{
					peer.printFileList();
				}
			}
			else if(m_whichThread == 1)
			{
				System.out.println("Enter File Name: ");
				String filePath = scanner.nextLine();
				System.out.println("Enter File ID: ");
				String fileID = scanner.nextLine();
				
				storeData.storeResource(filePath, fileID);
			}
		}
	}
}
