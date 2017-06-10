package com.brohkahn.pingserver;

class Ping {
	String date;
	String server;
	int result;

	public Ping(String date, String server, int result) {
		this.date = date;
		this.server = server;
		this.result = result;
	}
}
