package com.cptingle.WebServer.messaging;

import com.cptingle.WebServer.server.HTTPServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private HTTPServer server;

    private Method method;
    private String path;
    private Map<String, String> headers;
    private Map<String, String> cookies;

    public Request(HTTPServer server, Method method, String path, List<String> headerLines) {
        this.server = server;
        this.method = method;
        this.path = path;
        this.headers = parseHeaders(headerLines);
        this.cookies = parseCookies(headers.get("cookie"));
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHeaderValue(String key) {
        return headers.get(key.toLowerCase());

    }

    /**
     * Converts a list of header strings read in by the socket into a key value map of headers
     * @param headerLines - list to be converted
     * @return
     */
    public Map<String, String> parseHeaders(List<String> headerLines) {
        Map<String, String> temp = new HashMap<>();
        for (int i = 0; i < headerLines.size(); i++) {
            String[] arr = headerLines.get(i).split(": ");
            temp.put(arr[0].toLowerCase(), arr[1]);
        }
        return temp;
    }

    /**
     * The function traverses the list of headers to find each line that begins with "Cookie: ". Then it splits up that line into key value pairs which it puts into the cookie map.
     * @param cookieHeader - the header value for cookie
     * @return
     */
    protected Map<String, String> parseCookies(String cookieHeader) {
        Map<String, String> temp = new HashMap<>();
        if (cookieHeader == null)
            return temp;

        String[] allCookies = cookieHeader.split("; ");
        for (int i = 0; i < allCookies.length; i++) {
            String[] cookie = allCookies[i].split("=");
            temp.put(cookie[0], cookie.length > 1 ? cookie[1] : "");
        }

        return temp;
    }
}
