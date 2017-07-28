package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatWayServer {
	
	private final static int PORT = 5555;
	private ServerSocket serverSocket;
	private ConcurrentHashMap<Integer,Boolean> clientPorts;
	private Integer waitingPort = 0;
	private ConcurrentHashMap<Integer,Integer> clientPairs;
	private ConcurrentHashMap<Integer,Socket> portToSocket;
	private ConcurrentHashMap<Integer,String> portToNickname;
	
	
	/**
	 * Creates a ChatWayServer, which listens for incoming ChatWayClients at port number "port"
	 * @param port number, usually set to 5555
	 */
	public ChatWayServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		clientPorts = new ConcurrentHashMap<Integer,Boolean>();
		clientPairs = new ConcurrentHashMap<Integer,Integer>();
		portToSocket = new ConcurrentHashMap<Integer,Socket>();
		portToNickname = new ConcurrentHashMap<Integer,String>();
		for (int i = 5600; i<5700; i++){   // Keep track of 100 initially open ports for clients.  This puts a limit on the number of concurrent users.
			clientPorts.put(i, false);
			clientPairs.put(i, 0);
		}
		
		
	}
	
	/**
	 * Runs the server, listening for client connections and handles them
	 * Never returns unless exception is thrown
	 * @throws IOException if the main socket is broken
	 * (IOExceptions from individual clients do *not* terminate serve()).
	 * 
	 */
	public void serve() throws IOException {
		while (true){
			//block until a client connections
			Socket socket = serverSocket.accept();
			
			try{
				PrintWriter localOut = new PrintWriter(socket.getOutputStream(), true);
				int freePort = 0;
				for (int i = 5600; i<5700; i++){   // try to find a free port
					if (clientPorts.get(i) == false){
						freePort = i;
						clientPorts.replace(i, true);
						break;
					}
				}
				if (freePort == 0 ){
					localOut.println("error full");
				} else{
					localOut.println("connectTo " + freePort);
					/*
					 * Now create a new connection at the free port and a new thread to
					 * handle the client at this new socket
					 */
					ServerSocket localServerSocket = new ServerSocket(freePort);
					Thread clientThread = new Thread(new ClientRunnable(freePort, localServerSocket));
					clientThread.start();
				}
				socket.close();
				
			} catch (IOException e){ e.printStackTrace(); /* but don't terminate server()*/} 
			finally { socket.close(); }
			
		}
	}

	/**
     * Handle a single client connection.  Returns when client disconnects.
     * @param socket  socket where client is connected
     * @throws IOException if connection has an error or terminates unexpectedly
     */
	public void handleConnection(Socket socket, int portNum) throws IOException{

		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		String status = "sitting";
		int partner;
		Socket partnerSocket;
		PrintWriter partnerOut = null;
		
		try {
			System.out.println("into try...");
			for (String line = in.readLine(); line != null; line = in.readLine()){
				System.out.println(line);
				String[] inputs = line.split(" ");
				if (inputs[0].equals("new")){
					if (status.equals("inConvo")){
						partnerOut.println("end");
						System.out.println("send end from " + portToNickname.get(portNum) + " because I got a 'new' from my client");
					} else if (status.equals("waiting")){
						continue;
					}
					if (inputs.length > 1){
						portToNickname.put(portNum, line.substring(4));
					} else {portToNickname.put(portNum, "Stranger");}
					
					int match = waitRequest(portNum);
					if (match != 0){
						status = "inConvo";
						partner = match;
						partnerSocket = portToSocket.get(partner);
						out.println("starting " + portToNickname.get(partner));
						partnerOut = new PrintWriter(partnerSocket.getOutputStream(), true);
						partnerOut.println("starting " + portToNickname.get(portNum)); //alert partner to start their end of the conversation
						System.out.println("matched " + portToNickname.get(portNum) +" with " + portToNickname.get(partner));
					} else{
						status = "waiting";
						out.println("wait");
						System.out.println("sent wait message");
					}
				} else if(inputs[0].equals("end")){
					clientPairs.put(portNum, 0);
					partnerOut.println("end");
					status = "sitting";
					partner = 0;
					partnerSocket = null;
				} else if (inputs[0].equals("started")){ // this is the client helpfully telling the server, 'a partner has arrived, update your status
					status = "inConvo";
					partner = clientPairs.get(portNum);
					partnerSocket = portToSocket.get(partner);
					partnerOut = new PrintWriter(partnerSocket.getOutputStream(), true);
				} else if (inputs[0].equals("message")){
					if (status.equals("inConvo")){
					partnerOut.println(line);
					}
				} else if(inputs[0].equals("ended")){
					clientPairs.put(portNum, 0);
					status = "sitting";
					partner = 0;
					partnerSocket = null;
				}
			}
		} finally {
			System.out.println("out of try");
			out.close();
			in.close();
			socket.close();
		}
	}
	
	
	/**
	 * If a client is waiting for a connection, matches requester with waiting client
	 * If no client is waiting, tells the server that requester is now waiting
	 * 
	 * If a connection between clients is made, it pairs them in clientPairs
	 * 
	 * @param requester the server port number, where the requesting client is connected
	 * @mutates waitingPort
	 * @mutates clientPairs
	 */
	public int waitRequest(int requester){
		
		synchronized(waitingPort){
			if (waitingPort == 0){
				waitingPort = requester;
				return 0;
			}
			else {
				if (waitingPort.equals(requester)){
					System.out.println("ERROR: Tried to match client with itself for Conversation!");
					return 0;
				}
				Integer waiter = waitingPort;
				clientPairs.replace(requester, waiter);
				clientPairs.replace(waiter, requester);
				waitingPort = 0;
				return waiter;
			}
		}
	}
	
	public static void main(String[] args) {

		try {
            ChatWayServer server = new ChatWayServer(PORT);
            server.serve();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	class ClientRunnable implements Runnable {
    	int portNum;
    	ServerSocket localSS;
    	ClientRunnable(int newPort, ServerSocket localServerSocket){
    		portNum = newPort;
    		localSS = localServerSocket;
    	}
    	public void run(){
			try{
				Socket clientSocket = localSS.accept();
				portToSocket.put(portNum, clientSocket);
				System.out.println("made it into 'run'");
				handleConnection(clientSocket, portNum);
				portToSocket.put(portNum, null);
				clientSocket.close();
			}
			
			catch (Exception ex){System.out.println("Lost client connection");}
			
			finally {
				clientPorts.replace(portNum, false);
				try{localSS.close();}
				catch (Exception ex){ex.printStackTrace();}
			}
		}
    }


}
