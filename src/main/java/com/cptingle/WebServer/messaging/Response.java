package com.cptingle.WebServer.messaging;

import com.cptingle.WebServer.response.Status;
import com.cptingle.WebServer.server.ClientConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

public class Response {
    private ClientConnection conn;
    private Status status;
    private String headers;
    private byte[] body;


    public Response(ClientConnection conn, Status status) {
        this.conn = conn;
        this.status = status;
    }

    public Response(ClientConnection conn, Status status, File file) {
        this.conn = conn;
        this.status = status;
        try {
            this.body = fileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.headers = buildHeaders(conn, status, null, "text/html", body.length);
    }

    public Response(ClientConnection conn, Status status, File file, List<String> extraHeaders) {
        this.conn = conn;
        this.status = status;
        try {
            this.body = fileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.headers = buildHeaders(conn, status, extraHeaders, "text/html", body.length);
    }

    public String getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Status getStatus() {
        return status;
    }

    public void buildErrorPage() {
        buildErrorPage(null);
    }

    public void buildErrorPage(List<String> extraHeaders) {
        String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head><title>" + status.getReasonPhrase() + "</title></head>" +
                "<body>" +
                "<h1>" + status.getReasonPhrase() + "</h1>" +
                "<body>" +
                "</html>";
        body = html.getBytes();
        headers = buildHeaders(conn, status, extraHeaders, "text/html", body.length);
    }

    /**
     * Converts the provided file into an array of bytes to be send to the client using a BufferedOutputStream
     * @param file - File to be sent
     * @return - Array of bytes making up the file
     * @throws IOException
     */
    public byte[] fileToByteArray(File file) throws IOException {
        FileInputStream fis = null;
        byte[] data = new byte[(int) file.length()];

        try {
            fis = new FileInputStream(file);
            fis.read(data);
        } finally {
            if (fis != null)
                fis.close();
        }

        return data;
    }

    /**
     * Constructs a set-cookie header from the provided name, value, and arguments
     * @param name - cookie name
     * @param value - cookie value
     * @param args - any other cookie arguments such as expiration, site origins, etc
     * @return - the completed cookie in string form
     */
    public String buildCookie(String name, String value, String... args) {
        String cookie = "Set-Cookie: " + name + "=" + value;
        for (int i = 0; i < args.length; i++) {
            cookie += "; " + args[i];
        }
        return cookie;
    }

    /**
     * Helper method to build the header string
     * @param cc - client connection object, used to get certain com.cptingle.WebServer.server info
     * @param rc - the com.cptingle.WebServer.response code to send to the client (200, 404, etc.)
     * @param extraHeaders - a list of extra headers such as set-cookie etc.
     * @param mimeType - the mime type of the returned data
     * @param contentLength - the length (in bytes) of the content returned
     * @return - A string containing all of the headers delimited by a newline
     */
    public String buildHeaders(ClientConnection cc, Status rc, List<String> extraHeaders, String mimeType, int contentLength) {
        String ret = "";
        ret += "HTTP/1.1 " + rc.getStatusCode() + " " + rc.getReasonPhrase() + "\r\n";
        ret += "Server: cpt22 Web Server v" + cc.getServer().getVersion() + "\r\n";
        ret += "Date: " + new Date() + "\r\n";
        ret += "Content-type: " + mimeType + "\r\n";
        ret += "Content-length: " + contentLength + "\r\n";
        if (!cc.getServer().areConnectionsPersistent())
            ret += "Connection: close\r\n";
        if (extraHeaders != null) {
            for (int i = 0; i < extraHeaders.size(); i++) {
                ret += extraHeaders.get(i) + "\r\n";
            }
        }
        ret += "\r\n";
        return ret;
    }
}
