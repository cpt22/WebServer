package com.cptingle.WebServer.server;

public class Server {
    /**
     * Static methods to instantiate com.cptingle.WebServer.server
     */
    private static HTTPServer server;
    public static void main(String[] args) {
        server = new HTTPServer();
    }

    public static HTTPServer getServer() {
        return server;
    }
}
