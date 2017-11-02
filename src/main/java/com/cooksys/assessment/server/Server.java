package com.cooksys.assessment.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.cooksys.assessment.model.User;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	private int port;
	private ExecutorService executor;
	
	private Map<String, ClientHandler> connectedClients = new HashMap<String, ClientHandler>();
	private Map<String, User> users = new HashMap<String, User>();
	private Map<String, HashSet<String>> groups = new HashMap<String, HashSet<String>>();
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
	}
	
	public Boolean checkClientAuth(String username) {
		if(connectedClients.containsKey(username)) return true;
		else return false;
		
	}
	
	public Boolean initializeClient(String username, ClientHandler client, String response) {
		if(connectedClients.containsKey(username)) return false;
		if(users.containsKey(username)) {
			Message message = new Message();
			message.setCommand("givepassword");
			message.setContents(response);
			client.receiveMessage(message);
		}else {
			users.put(username, new User(username));
			Message message = new Message();
			message.setCommand("givenewpassword");
			message.setContents("Created new user, enter new password");
			client.receiveMessage(message);
		}
		return true;
	}
	
	public Boolean loginClient(String username, String password, ClientHandler client) {
		if(!users.containsKey(username)) return false;
		if(users.get(username).login(password)) {
			return true;
		}else {
			initializeClient(username, client, "Wrong password, try again");
			return false;
		}
	}
	
	public Boolean loginNewClient(String username, String password, ClientHandler client) {
		if(!users.containsKey(username)) return false;
		users.get(username).setPassword(password);
		return true;
	}
	
	public Boolean connectClient(String username, ClientHandler client) {
		connectedClients.put(username, client);
		Message message = new Message();
		message.setUsername(username);
		message.setCommand("newuser");
		broadcastMessage(message, username);
		return true;
	}
	public void disconnectClient(String username) {
		connectedClients.remove(username);
		
		Message message = new Message();
		message.setUsername(username);
		message.setCommand("userleft");
		broadcastMessage(message, username);
	}
	
	public void broadcastMessage(Message message, String username) {
		for(Entry<String, ClientHandler> entry: connectedClients.entrySet()) {
			if(!(entry.getKey() == username)) {
				entry.getValue().receiveMessage(message);
			}
		}
	}
	
	public void whisperMessage(Message message, String username) {
		if(connectedClients.containsKey(username)) {
			connectedClients.get(username).receiveMessage(message);
		}else if(groups.containsKey(username)) {
			message.setCommand("groupchat");
			for(String user : groups.get(username)) {
				connectedClients.get(user).receiveMessage(message);
			}
		}
	}
	
    public String getUserList() {
		return connectedClients.keySet().toString();
    }
    
    public Boolean createGroup(String groupname, String username) {
    	if(connectedClients.containsKey(groupname)) return false;
    	if(groups.containsKey(groupname)) return false;
    	groups.put(groupname, new HashSet<String>());
    	groups.get(groupname).add(username);
    	
		Message message = new Message();
		message.setUsername(groupname);
		message.setCommand("createdgroup");
		broadcastMessage(message, username);
		return true;
    }
    
    public Boolean joinGroup(String groupname, String username) {
    	if(!groups.containsKey(groupname)) return false;
    	groups.get(groupname).add(username);
		return true;
    }
    
    public Boolean leaveGroup(String groupname, String username) {
    	if(!groups.containsKey(groupname)) return false;
    	if(!groups.get(groupname).contains(username)) return false;
    	groups.get(groupname).remove(username);
    	return true;
    }
	public void run() {
		log.info("server started");
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			while (true) {
				Socket socket = ss.accept();
				ClientHandler handler = new ClientHandler(socket, this);
				executor.execute(handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
