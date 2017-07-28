# ChatWayChat

This was a simple project I created to get a feel for two things in Java:

1. Using sockets to connect machines
2. Creating a GUI with built-in Java functions

Once the server is running, it waits for clients to connect.  When there are multiple 
clients available, it will connect them for one-to-one chats, ostensibly between
strangers.  Once one client disconnects, it will offer to let the stranded client 
reconnect with a new stranger.