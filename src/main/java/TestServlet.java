import com.cptingle.WebServer.messaging.Request;
import com.cptingle.WebServer.server.AbstractServlet;
import com.cptingle.WebServer.server.ClientConnection;
import com.cptingle.WebServer.server.HTTPServer;

public class TestServlet extends AbstractServlet {
    public TestServlet(HTTPServer server) {
        super(server, "/test.html");
    }
    public void get(ClientConnection conn, Request req) {
        System.err.println("YINNY");
    }
}
