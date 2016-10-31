package com.brohkahn.pingserver;

public class Server {
	public int id;
	public String name;
	public int lastResult;
	public boolean active;

	public Server(int id, String name, int lastResult, boolean active) {
		this.id = id;
		this.name = name;
		this.lastResult = lastResult;
		this.active = active;
	}
}
