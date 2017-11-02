package com.cooksys.assessment.model;

public class User {
	
	private String username;
	private String password;
	
	public User(String username) {
		this.username = username;
		this.password = null;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Boolean login(String password) {
		if(this.password.equals(password)) return true;
		else return false;
	}
	
}
