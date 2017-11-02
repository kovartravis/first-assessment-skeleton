package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	private ObjectMapper mapper;
	private BufferedReader reader;
	private PrintWriter writer;
	private Server serverReference;
	
	public ClientHandler(Socket socket, Server serverReference) {
		super();
		this.socket = socket;
		this.serverReference = serverReference;
	}
	
	public void receiveMessage(Message message) {
		String response = null;
		try {
			response = mapper.writeValueAsString(message);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		writer.write(response);
		writer.flush();
	}
	
	public void run() {
		try {

			mapper = new ObjectMapper();
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				String response = null;

				switch (message.getCommand()) {
					case "connect":
						if(!serverReference.initializeClient(message.getUsername(), this, "User exists, enter password")) {
							log.info("user <{}> already logged in, rejected connection attempt", message.getUsername());
							this.socket.close();
						}
						break;
					case "givepassword":
						if(serverReference.loginClient(message.getUsername(), message.getContents(), this)){
							log.info("user <{}> connected", message.getUsername());
							serverReference.connectClient(message.getUsername(), this);
							
							Message loggedin = new Message();
							loggedin.setCommand("loggedin");
							receiveMessage(loggedin);
						}
						break;
					case "givenewpassword":
						serverReference.loginNewClient(message.getUsername(), message.getContents(), this);
						log.info("user <{}> created, password <{}>", message.getUsername(), message.getContents());
						serverReference.connectClient(message.getUsername(), this);
						
						Message loggedin = new Message();
						loggedin.setCommand("loggedin");
						receiveMessage(loggedin);
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
					    serverReference.disconnectClient(message.getUsername());
						this.socket.close();
						break;
					case "echo":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					        response = mapper.writeValueAsString(message);
					    	writer.write(response);
					    	writer.flush();
						}
						break;
					case "broadcast":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
						    serverReference.broadcastMessage(message, message.getUsername());
						}
						break;
					case "whisper":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> whispered <{}> to <{}>", message.getUsername(), message.getContents(), message.getwUsername());
						    serverReference.whisperMessage(message, message.getwUsername());
						}
					    break;
					case "users":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> asked for list of users", message.getUsername());
						    message.setContents(serverReference.getUserList());
						    response = mapper.writeValueAsString(message);
						    writer.write(response);
						    writer.flush();
						}
						break;
					case "newgroup":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> created a group named <{}>", message.getUsername(), message.getContents());
						    if(!serverReference.createGroup(message.getContents(), message.getUsername())){
						    	message.setContents("Group already exists, failed to create group");
							    response = mapper.writeValueAsString(message);
							    writer.write(response);
							    writer.flush();
						    }
						}
						break;
					case "joingroup":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> joined a group named <{}>", message.getUsername(), message.getContents());
						    if(serverReference.joinGroup(message.getContents(), message.getUsername())){
							    message.setContents("Group joined successfully");
							    response = mapper.writeValueAsString(message);
							    writer.write(response);
							    writer.flush();
						    }else {
							    message.setContents("Group doesn't exist, failed to join group");
							    response = mapper.writeValueAsString(message);
							    writer.write(response);
							    writer.flush();
						    }
						}
						break;
					case "leavegroup":
						if(!serverReference.checkClientAuth(message.getUsername())) {
							log.info("unauthorized client <{}> sent command", message.getUsername());
						}else {
						    log.info("user <{}> left a group named <{}>", message.getUsername(), message.getContents());
						    if(serverReference.leaveGroup(message.getContents(), message.getUsername())) {
						    	message.setContents("Group left successfully");
							    response = mapper.writeValueAsString(message);
							    writer.write(response);
							    writer.flush();
						    }else {
							    message.setContents("Group doesn't exist, or you arent a part of that group");
							    response = mapper.writeValueAsString(message);
							    writer.write(response);
							    writer.flush();
						    }
						}
					default: log.info("user <{}> command not recognized", message.getUsername());
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
