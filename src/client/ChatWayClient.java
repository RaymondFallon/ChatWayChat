package client;

import java.awt.*;
import java.io.*;
import java.net.*;

import java.awt.event.*;
import javax.swing.*;

public class ChatWayClient {

	private JFrame firstFrame;
	private JRadioButton manualB;
	private JFrame frame;
	private String nickname;
	private String partnerNickname;
	private JTextField usernameText;
	private JTextArea reader;
	private JTextArea writer;
	private Socket socket;
	private String hostname = "localhost";
	private Integer serverPort = 5555;
	private PrintWriter out;
	private BufferedReader in;
	private String convo = "";
	private String status = "sitting";
	
	
	
	public static void main(String[] args) {
		if (args.length != 2){
			System.out.println("Client must be run by src/client/ChatWayClient <hostname> <portnumber>");
		}
		else {
			ChatWayClient client = new ChatWayClient();
			client.begin();
			client.getConnection();
			client.runChat();
		}
		
	}
	
	public void begin() {
		

		firstFrame = new JFrame("ChatWay");
		
		ButtonGroup bg = new ButtonGroup();
		JPanel radioPanel = new JPanel(new GridLayout(0,1));
		JRadioButton strangerB = new JRadioButton("Stay Anonymous", true);
		bg.add(strangerB);
		radioPanel.add(strangerB);
		manualB = new JRadioButton("Username:");
		bg.add(manualB);
		radioPanel.add(manualB);
		
		
		JPanel middlePanel = new JPanel();
		middlePanel.add(radioPanel);
		
		usernameText = new JTextField("Choose a nickname...", 15);
		usernameText.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){
				if (usernameText.getText().equals("Choose a nickname...")){
					usernameText.setText("");
					manualB.setSelected(true);}}
			public void focusLost(FocusEvent e){
				if (usernameText.getText().equals("")){
					usernameText.setText("Choose a nickname...");}}});
		usernameText.getActionMap().put("startAction", new StartAction());
		usernameText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "startAction");
		middlePanel.add(usernameText);
		
		JPanel southPanel = new JPanel();
		JButton start = new JButton("Start New Chat");
		start.addActionListener(new StartListener());
		southPanel.add(start);
		
		firstFrame.getContentPane().add(BorderLayout.CENTER, middlePanel);
		firstFrame.getContentPane().add(BorderLayout.PAGE_END, southPanel);
		//frame.getContentPane().setBounds(20, 20, 20, 20);
		
		firstFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		firstFrame.setSize(300, 150);
		firstFrame.setVisible(true);
		
	}
	
	public void getConnection(){
		try{
			Socket getterSocket = new Socket(hostname, serverPort);
			BufferedReader in1 = new BufferedReader(new InputStreamReader(getterSocket.getInputStream()));
			
			String line = in1.readLine();
			String[] words = line.split(" ");
			if (words.length == 2 & words[0].equals("connectTo")){
				getterSocket.close();
				System.out.println(line);
				socket = new Socket(hostname, Integer.parseInt(words[1]));
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			}
			
		} catch (Exception ex){
			ex.printStackTrace();
		} 
	}
	
	
	public void startChat(){
		
		frame = new JFrame("ChatWay Chat Session");
		JPanel northPanel = new JPanel(new BorderLayout());
		JPanel southPanel = new JPanel(new BorderLayout());
		
		reader = new JTextArea("Hello, " + nickname+ "\nWaiting for another user...");
		JScrollPane readerPane = new JScrollPane(reader, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		reader.setEditable(false);
		reader.setColumns(50);
		reader.setRows(19);
		reader.setLineWrap(true);
		reader.setWrapStyleWord(true);
		ImageIcon image = new ImageIcon("../images/hibiscus.jpg");
		JLabel hibiscus = new JLabel("", image, JLabel.CENTER);

		northPanel.add(readerPane, BorderLayout.LINE_START);
		northPanel.add(hibiscus, BorderLayout.LINE_END);
		
		writer = new JTextArea("Type your message here...\n");
		JScrollPane writerPane = new JScrollPane(writer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		writer.setLineWrap(true);
		writer.setWrapStyleWord(true);
		writer.setColumns(50);
		writer.setRows(4);
		writer.addFocusListener(new FocusListener(){
			public void focusGained(FocusEvent e){
				if (writer.getText().equals("Type your message here...\n")){
					writer.setText("");}}
			public void focusLost(FocusEvent e){
				if (writer.getText().equals("")){
					System.out.println("focus lost");
					writer.setText("Type your message here...\n");}}});
		southPanel.add(writerPane, BorderLayout.LINE_START);
		
		writer.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "sendText");
		writer.getActionMap().put("sendText", new SendAction());
		
		JButton send = new JButton("SEND");
		JButton end = new JButton("End Chat");
		JButton newC = new JButton("New Chat");
		JButton exit = new JButton("Exit ChatWay");
		JPanel stopper = new JPanel(new BorderLayout());
		JPanel endOrNew = new JPanel(new BorderLayout());
		endOrNew.add(end, BorderLayout.LINE_START);
		endOrNew.add(newC, BorderLayout.LINE_END);
		stopper.add(endOrNew, BorderLayout.CENTER);
		stopper.add(exit, BorderLayout.PAGE_END);
		southPanel.add(stopper, BorderLayout.LINE_END);
		southPanel.add(send, BorderLayout.CENTER);
		
		exit.addActionListener(new ExitListener());
		send.addActionListener(new SendListener());
		end.addActionListener(new EndListener());
		newC.addActionListener(new NewListener());
		
		frame.getContentPane().add(northPanel, BorderLayout.PAGE_START);
		frame.getContentPane().add(southPanel, BorderLayout.PAGE_END);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//frame.setSize(900, 500);
		frame.pack();
		frame.setVisible(true);
		
		out.println("new "+nickname);
	}
	
	private void runChat(){
		Thread serverInput = new Thread(new ServerListenRunnable());
		serverInput.start();
		//reader.setText("\n\n**************************\n         Disconnected from Server \n\n*************************");
	}
	
	
	
	class ExitListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			out.println("end");
			System.exit(0);
		}
	}
	
	private void sendText(){
		String writerText = writer.getText();
		if (!writerText.equals("") & !writerText.equals("Type your message here...\n") & getStatus().equals("inConvo")){
			System.out.println("Alright, trying to send this 'ish'");
			String displayText = nickname + ": " + writerText;
			out.println("message " + writerText);
			addToConvo(displayText);
			writer.setText("");
		}
	}
	
	class SendListener implements ActionListener {     
		public void actionPerformed(ActionEvent e){
			sendText();			
		}
	}
	
	class SendAction extends AbstractAction {
		public void actionPerformed(ActionEvent e){
			sendText();
		}
	}
	
	private void startIt(){
		String givenText = usernameText.getText();
		if (manualB.isSelected() & !givenText.equals("") & !givenText.equals("Choose a nickname...")){ nickname = givenText;} 
		else{nickname = "You";}
		firstFrame.setVisible(false);
		firstFrame.dispose();
		startChat();
	}
	class StartAction extends AbstractAction {
		public void actionPerformed(ActionEvent e){
			startIt();
		}
	}
	
	class StartListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			startIt();
		}
	}
	
	private void endChat(){
		if (getStatus().equals("inConvo")){
			addToConvo("\n\nConversation with " +partnerNickname+ " ended.");	
			addToConvo("\nClick \"New\" to start a new chat!");
			System.out.print("telling server to end it with" + partnerNickname);
			out.println("end");
		} setStatus("sitting");
			
	}
	
	class EndListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			endChat();
		}
	}
	
	class NewListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			endChat();
			resetConvo();
			addToConvo("Waiting to connect...");
			setStatus("waiting");
			out.println("new " + nickname);
		}
	}
	
	/**
	 * Adds the string to the ongoing conversation
	 * @mutates convo
	 */
	private void addToConvo(String s){
		synchronized(convo){
			convo = convo.concat(s);
			reader.setText(convo);
		}
	}
	
	/**
	 * Deletes any past conversation, making way for a log of the new conversation
	 * @mutates convo
	 */
	private void resetConvo(){
		synchronized(convo){
			convo = "";
		}
	}
	
	private String getStatus(){
		synchronized(status){
			return status;
		}
	}
	
	/**
	 * returns the current chat status of this client, defined as:
	 * waiting: waiting for the server to connect user to a new partner
	 * inConvo: currently in conversation with another user
	 * sitting: not in a conversation, and not currently waiting for a user, must send server a "new" message to start a new chat
	 * 
	 * @param s
	 */
	private void setStatus(String s){
		synchronized(status){
			status = s;
		}
	}
	
	class ServerListenRunnable implements Runnable{
		public void run(){
			try{
				while (true){
					for (String line = in.readLine(); line != null; line = in.readLine()){
						System.out.println(line);
						String[] words = line.split(" ");
						if (words[0].equals("starting")){
							partnerNickname = line.substring(9);
							if (partnerNickname.equals("You")){
								partnerNickname = "Stranger";
							}
							setStatus("inConvo");
							resetConvo();
							addToConvo("Connected!\nStarted a new conversation with "+ partnerNickname + "\n\n\n");
							out.println("started");
						}
						if (words[0].equals("end")){
							System.out.println(nickname + " was told by server to END IT");
							addToConvo("\n\nConversation with " +partnerNickname+ " ended.");	
							addToConvo("\nClick \"New\" to start a new chat!");
							setStatus("sitting");
							out.println("ended");
						}
						if (words[0].equals("message")){
							addToConvo(partnerNickname + ": " + line.substring(8) +"\n");
							
						}
						if (words[0].equals("wait")){

						}
					}
				}
			} catch (Exception ex){ex.printStackTrace();}
		}
	}
}
