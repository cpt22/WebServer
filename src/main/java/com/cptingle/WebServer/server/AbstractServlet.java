package com.cptingle.WebServer.server;

import com.cptingle.WebServer.messaging.Request;
import com.cptingle.WebServer.messaging.Response;
import com.cptingle.WebServer.response.Status;
import jdk.net.SocketFlow;

import java.io.IOException;

public abstract class AbstractServlet implements HTTPServlet {
    protected String uri;
    protected HTTPServer server;

    public AbstractServlet(HTTPServer server, String uri) {
        this.server = server;
        this.uri = uri;
    }

    public String getURI() {
        return uri;
    }

    public void get(ClientConnection conn, Request req) {
        System.err.println("superpoop");
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void head(ClientConnection conn, Request req) {
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void post(ClientConnection conn, Request req) {
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void put(ClientConnection conn, Request req) {
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void patch(ClientConnection conn, Request req) {
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(ClientConnection conn, Request req) {
        Response resp = new Response(conn, Status.METHOD_NOT_ALLOWED);
        resp.buildErrorPage();
        try {
            conn.sendResponse(resp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
