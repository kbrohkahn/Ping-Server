package com.brohkahn.pingserver;

class Server {
	public int id;
	public String name;
	public Ping lastPing;
	public boolean active;

	public Server(int id, String name, Ping lastPing, boolean active) {
		this.id = id;
		this.name = name;
		this.lastPing = lastPing;
		this.active = active;
	}
}
