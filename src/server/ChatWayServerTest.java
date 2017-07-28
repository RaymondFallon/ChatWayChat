package server;

import org.junit.Test;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatWayServerTest {

	
	@Test 
	public void openTest() throws IOException {
		ChatWayServer server = new ChatWayServer(5555);
		assert (server != null);
		
	}
	
	
}
