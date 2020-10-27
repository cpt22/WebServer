package com.cptingle.WebServer.server;

import com.cptingle.WebServer.exceptions.YAMLConfigurationException;
import com.cptingle.WebServer.messaging.Method;
import com.cptingle.WebServer.messaging.Request;
import com.cptingle.WebServer.messaging.Response;
import com.cptingle.WebServer.response.Status;
import com.cptingle.WebServer.util.FileParser;
import com.cptingle.WebServer.util.Utility;
import com.cptingle.WebServer.util.configuration.Configuration;
import com.cptingle.WebServer.util.logging.LogLevel;
import com.cptingle.WebServer.util.logging.Logger;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientConnection implements Runnable {
    private static File WEB_ROOT;
    private HTTPServer server;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedOutputStream bytesOut;
    private Configuration config;

    private int connectionTimeout;


    public ClientConnection(Socket socket, HTTPServer server) {
        this.socket = socket;
        this.server = server;
        this.config = server.getConfig();
        try {
            WEB_ROOT = new File(config.getSection("files").getString("web-root", "."));
        } catch (YAMLConfigurationException e) {
            e.printStackTrace();
        }

        // If the config value for persistent connections is set, then set the socket timeout to a non-zero value as specified in the config.
        // Setting a timeout will cause the socket to throw a timeout exception after the specified amount of milliseconds.
        if (server.areConnectionsPersistent()) {
            try {
                socket.setSoTimeout(server.getPersistentConnectionTimeout());
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public HTTPServer getServer() {
        return server;
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public String getHost() {
        return socket.getInetAddress().getHostName();
    }

    public Logger getLogger() {
        return server.getLogger();
    }

    @Override
    public void run() {

        // While the socket is not timed out or closed, listen for and process HTTP requests
        while(true) {
            List<String> headers = new ArrayList<String>();
            Map<String, String> headerMap = new HashMap<String, String>();
            Map<String, String> cookies = new HashMap<String, String>();
            try {
                // Open input and output streams/writers to facilitate communication with the client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());
                bytesOut = new BufferedOutputStream(socket.getOutputStream());

                // Read all of the headers line by line into the list of headers
                String inputLine;
                while (!(inputLine = in.readLine()).equals("")) {
                    headers.add(inputLine);
                }

                getLogger().verbose("Request received from client", getAddress());

                // Debug headers and cookies
                if (server.isDebugging()) {
                    getLogger().debug("RECEIVED HEADERS\n", getAddress());
                    for (String header : headers) {
                        getLogger().debug(header, getAddress());
                    }
                }

                String firstLine = headers.remove(0);
                String method = firstLine.split(" ")[0];
                String filePath = firstLine.split(" ")[1];

                if (filePath.substring(filePath.length() - 1).equals("/")) {
                    filePath += config.getString("index-page", "index.html");
                }

                Request request = new Request(server, Method.valueOf(method), filePath, headers);

                HTTPServlet servlet = server.getServlet(request.getPath());

                // If the config value for persistent connections is set, then set the socket timeout to the value specified in the headers otherwise use com.cptingle.WebServer.server config val.
                if (server.areConnectionsPersistent() && request.getHeaderValue("keep-alive") != null) {
                    try {
                        String hdr = request.getHeaderValue("keep-alive");
                        String[] attrSplit = hdr.split(",");
                        Map<String, String> tempMap = new HashMap<String, String>();
                        for (String s : attrSplit) {
                            String[] tmp = s.split("=");
                            tempMap.put(tmp[0], tmp[1]);
                        }
                        socket.setSoTimeout(Integer.parseInt(tempMap.get("timeout")) * 1000);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        getLogger().verbose("Malformed headers received", getAddress());
                    }
                }

                // Switch the request method and pass the request to the appropriate function
                switch (request.getMethod()) {
                    case GET:
                        if (servlet != null) {
                            servlet.get(this, request);
                        } else {
                            if (Files.exists(Paths.get(WEB_ROOT + request.getPath()))) {
                                Response resp = new Response(this, Status.OK, new File(WEB_ROOT + request.getPath()));
                                sendResponse(resp);
                            } else {
                                Response resp = new Response(this, Status.NOT_FOUND);
                                resp.buildErrorPage();
                                sendResponse(resp);
                            }
                        }
                        break;
                    case HEAD:
                        if (servlet != null) {
                            servlet.head(this, request);
                        } else {

                        }
                        break;
                    case POST:
                        if (servlet != null) {
                            servlet.post(this, request);
                        } else {

                        }
                        break;
                    case PUT:
                        if (servlet != null) {
                            servlet.put(this, request);
                        } else {

                        }
                        break;
                    case DELETE:
                        if (servlet != null) {
                            servlet.delete(this, request);
                        } else {

                        }
                        break;
                    case PATCH:
                        if (servlet != null) {
                            servlet.patch(this, request);
                        } else {

                        }
                        break;
                }


                // If connections are not persistent, close all socket connections.
                if (!server.areConnectionsPersistent()) {
                    getLogger().verbose("Connection with a client closed (non-persistent)", getAddress());
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                    break;
                }
            } catch (FileNotFoundException ex) {
                try {
                    if(!sendResponse(Status.NOT_FOUND, "<html><head><title>Page not found</title></head><body>404 Page not found</body></html>")){
                        getLogger().info("Error sending response to client", getAddress());
                    }
                } catch (IOException ex1) {
                    getLogger().severe("An exception was encountered in generating the error page" + ex1.getMessage(), getAddress());
                    System.err.println("An exception was encountered in generating the error page: " + ex1.getMessage());
                }
            } catch (SocketTimeoutException ex) {
                // Catch the SocketTimeoutException and close all of the streams and socket.
                getLogger().verbose("Connection with a client timed out and was closed (persistent)", getAddress());
                try {
                    in.close();
                    bytesOut.close();
                    out.close();
                    socket.close();
                } catch (IOException ex1) {
                    getLogger().info(ex1.getMessage(), "");
                }
                break;
            } catch (IOException ex) {
                getLogger().info(ex.getMessage(), "");
                break;
            }
        }
    }

    /**
     * Sends a com.cptingle.WebServer.response to the client provided a com.cptingle.WebServer.response code and a file to send
     * @param code - Response code to send
     * @param file - The file to send
     * @throws IOException
     */
    private boolean sendResponse(Status code, Object file) throws IOException {
        return sendResponse(code, null, file);
    }

    public boolean sendResponse(Response rsp) throws IOException {
        out.print(rsp.getHeaders());
        out.flush();

        bytesOut.write(rsp.getBody(), 0, rsp.getBody().length);
        bytesOut.flush();
        return true;
    }

    /**
     * Sends a com.cptingle.WebServer.response to the client provided a com.cptingle.WebServer.response code, additional headers, and a file to send
     * @param code - Response code to send
     * @param extraHeaders - Extra headers such as cookies
     * @param file - The file to send
     * @throws IOException
     */
    private boolean sendResponse(Status code, List<String> extraHeaders, Object file) throws IOException {
        int fileLength;
        byte[] fileData;

        // Sometimes a file is provided (in the case of 404) but other times the file is provided in the form of a String (the result of the FileParser)
        if (file instanceof File) {
            fileLength = (int) ((File) file).length();
            fileData = Utility.fileToByteArray((File) file);
        } else if (file instanceof String) {
            fileData = ((String) file).getBytes("UTF-8");
            fileLength = fileData.length;
        } else {
            return false;
        }

        // Builds and sends the header using the helper method in the Utility class
        String headers = Utility.buildHeaders(this, code, extraHeaders, "text/html", fileLength);

        getLogger().debug("SENDING HEADERS\n" + headers, getAddress());

        out.print(headers);
        out.flush();

        // Writes the file/string data in byte array form
        bytesOut.write(fileData, 0, fileLength);
        bytesOut.flush();
        return true;
    }
}
