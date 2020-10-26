package com.cptingle.WebServer.server;

import com.cptingle.WebServer.messaging.Request;

public interface HTTPServlet {
    public void get(ClientConnection conn, Request req);

    public void head(ClientConnection conn, Request req);

    public void post(ClientConnection conn, Request req);

    public void put(ClientConnection conn, Request req);

    public void patch(ClientConnection conn, Request req);

    public void delete(ClientConnection conn, Request req);
}
