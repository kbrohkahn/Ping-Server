package com.brohkahn.pingserver;

public class Ping {
    public String date;
    public String server;
    public int result;

    public Ping(String date, String server, int result) {
        this.date = date;
        this.server = server;
        this.result = result;
    }
}
