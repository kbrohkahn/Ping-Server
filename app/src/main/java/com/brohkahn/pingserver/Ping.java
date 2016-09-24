package com.brohkahn.pingserver;

public class Ping {
    public static final int PING_SUCCESS = 1;
    public static final int PING_FAIL = 0;
    public static final int PING_ERROR_HOST = 10;
    public static final int PING_ERROR_IO = 11;

    public String date;
    public String server;
    public int result;
}
